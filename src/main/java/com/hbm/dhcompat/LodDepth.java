package com.hbm.dhcompat;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterRenderEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL33C.*;

/**
 * DH draws LOD terrain into its own depth texture and only copies color into Minecraft's frame, so
 * everything Minecraft draws afterwards (NTM mushroom clouds, missiles, particles) paints over LOD
 * mountains. Right after the LOD pass we re-project DH's depth into Minecraft's depth buffer, so
 * LOD terrain occludes like real blocks. Vanilla terrain still draws over it where it is closer.
 */
final class LodDepth {

    private static final String VERT = """
            #version 330 core
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """;
    private static final String FRAG = """
            #version 330 core
            uniform sampler2D DhDepth;
            uniform mat4 InvDhProj;
            uniform mat4 McProj;
            uniform float DhSky;
            uniform int ZeroToOne;
            uniform vec2 ScreenSize;
            void main() {
                vec2 uv = gl_FragCoord.xy / ScreenSize;
                float d = texelFetch(DhDepth, ivec2(uv * vec2(textureSize(DhDepth, 0))), 0).r;
                if (d == DhSky) discard;
                vec4 view = InvDhProj * vec4(uv * 2.0 - 1.0, ZeroToOne == 1 ? d : d * 2.0 - 1.0, 1.0);
                vec4 clip = McProj * vec4(view.xyz / view.w, 1.0);
                float z = clip.z / clip.w;
                gl_FragDepth = clamp(ZeroToOne == 1 ? z : z * 0.5 + 0.5, 0.0, 1.0);
            }
            """;

    private static int program, vao, fbo;
    private static int uInvDhProj, uMcProj, uDhSky, uZeroToOne, uScreenSize;
    private static boolean broken;
    /** Matrices of the LOD pass in flight; set before DH renders, consumed after it composited. */
    private static DhApiRenderParam pending;

    private LodDepth() {}

    static void register() {
        DhApi.events.bind(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent() {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
                pending = event.value.renderPass == EDhApiRenderPass.TRANSPARENT ? null : event.value.copy();
            }
        });
        // After, not before, DH's apply step: if that step looks at vanilla depth, our LOD depth
        // written earlier would make it skip the LOD color.
        DhApi.events.bind(DhApiAfterRenderEvent.class, new DhApiAfterRenderEvent() {
            @Override
            public void afterRender(DhApiEventParam<Void> event) {
                DhApiRenderParam param = pending;
                pending = null;
                if (param == null || broken) return;
                try {
                    copy(param);
                } catch (RuntimeException e) {
                    broken = true; // ponytail: one failure disables the pass for the session; no retry logic
                    DistantFx.LOG.error("LOD depth copy failed, disabled", e);
                }
            }
        });
    }

    private static void copy(DhApiRenderParam param) {
        DhApiResult<Integer> dhDepth = DhApi.Delayed.renderProxy.getDhDepthTextureGlId();
        GpuTexture mcDepth = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture();
        if (!dhDepth.success || dhDepth.payload == null || !(mcDepth instanceof GlTexture mcGl)) return;
        if (program == 0) init();

        Matrix4f invDhProj = joml(param.dhProjectionMatrix).invert();
        Matrix4f mcProj = joml(param.mcProjectionMatrix);
        int w = mcDepth.getWidth(0), h = mcDepth.getHeight(0);

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int drawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int readFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prog = glGetInteger(GL_CURRENT_PROGRAM);
        int vaoOld = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int activeTex = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int tex0 = glGetInteger(GL_TEXTURE_BINDING_2D);
        int sampler0 = glGetInteger(GL_SAMPLER_BINDING);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST), blend = glIsEnabled(GL_BLEND),
                cull = glIsEnabled(GL_CULL_FACE), scissor = glIsEnabled(GL_SCISSOR_TEST);
        int depthFunc = glGetInteger(GL_DEPTH_FUNC);
        boolean depthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
        boolean[] colorMask = new boolean[4];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buf = stack.malloc(4);
            glGetBooleanv(GL_COLOR_WRITEMASK, buf);
            for (int i = 0; i < 4; i++) colorMask[i] = buf.get(i) != 0;
        }

        try {
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, mcGl.glId(), 0);
            glViewport(0, 0, w, h);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_GREATER); // 26.2 renders reversed-Z: larger = closer
            glDepthMask(true);
            glColorMask(false, false, false, false);
            glDisable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            glDisable(GL_SCISSOR_TEST);

            glUseProgram(program);
            glBindVertexArray(vao);
            glBindTexture(GL_TEXTURE_2D, dhDepth.payload);
            glBindSampler(0, 0);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer m = stack.mallocFloat(16);
                glUniformMatrix4fv(uInvDhProj, false, invDhProj.get(m));
                glUniformMatrix4fv(uMcProj, false, mcProj.get(m));
            }
            glUniform1f(uDhSky, DhApi.Delayed.renderProxy.getDepthDirection().farDepth);
            glUniform1i(uZeroToOne, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1 : 0);
            glUniform2f(uScreenSize, w, h);
            glDrawArrays(GL_TRIANGLES, 0, 3);
        } finally {
            glBindSampler(0, sampler0);
            glBindTexture(GL_TEXTURE_2D, tex0);
            glActiveTexture(activeTex);
            glBindVertexArray(vaoOld);
            glUseProgram(prog);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFbo);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readFbo);
            glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            set(GL_DEPTH_TEST, depthTest);
            set(GL_BLEND, blend);
            set(GL_CULL_FACE, cull);
            set(GL_SCISSOR_TEST, scissor);
            glDepthFunc(depthFunc);
            glDepthMask(depthMask);
            glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
        }
    }

    private static void init() {
        int vs = shader(GL_VERTEX_SHADER, VERT), fs = shader(GL_FRAGMENT_SHADER, FRAG);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glDeleteShader(vs);
        glDeleteShader(fs);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new IllegalStateException(glGetProgramInfoLog(program));
        uInvDhProj = glGetUniformLocation(program, "InvDhProj");
        uMcProj = glGetUniformLocation(program, "McProj");
        uDhSky = glGetUniformLocation(program, "DhSky");
        uZeroToOne = glGetUniformLocation(program, "ZeroToOne");
        uScreenSize = glGetUniformLocation(program, "ScreenSize");
        int prog = glGetInteger(GL_CURRENT_PROGRAM);
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "DhDepth"), 0);
        glUseProgram(prog);

        vao = glGenVertexArrays();
        fbo = glGenFramebuffers();
        int old = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
        glDrawBuffer(GL_NONE);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, old);
    }

    private static int shader(int type, String src) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE) throw new IllegalStateException(glGetShaderInfoLog(s));
        return s;
    }

    private static void set(int cap, boolean on) {
        if (on) glEnable(cap);
        else glDisable(cap);
    }

    /**
     * DhApiMat4f's field naming is row-major or JOML-style depending on DH internals. A perspective
     * matrix has -1 at (row 3, col 2); JOML calls that m23. Detect which field holds it.
     */
    static Matrix4f joml(DhApiMat4f d) {
        Matrix4f m = new Matrix4f(
                d.m00, d.m01, d.m02, d.m03,
                d.m10, d.m11, d.m12, d.m13,
                d.m20, d.m21, d.m22, d.m23,
                d.m30, d.m31, d.m32, d.m33);
        return Math.abs(d.m32 + 1F) < 1e-4F ? m.transpose() : m;
    }
}
