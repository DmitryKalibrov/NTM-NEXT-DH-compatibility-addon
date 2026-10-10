package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client: NTM's exhaust is additive flame that is gone in ~3 s, so a far missile left no track.
 * Lay a long-lived, large translucent smoke trail along its path that is readable at LOD range.
 */
@Mixin(targets = "com.hbm.entity.missile.EntityMissileBaseNT")
abstract class MissileContrailMixin {

    /** Blocks between puffs. */
    @Unique private static final double SPACING = 3;

    @Unique private Vec3 ntmdh$last;

    @Inject(method = "tick", at = @At("RETURN"))
    private void ntmdh$contrail(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!self.level().isClientSide() || DistantFx.contrailSeconds <= 0) return;
        Vec3 pos = self.position(), last = ntmdh$last;
        ntmdh$last = pos;
        if (last == null) return;
        double len = pos.distanceTo(last);
        if (len < 0.1) return;
        int n = (int) Math.min(Math.ceil(len / SPACING), 20);
        for (int i = 1; i <= n; i++) {
            Vec3 p = last.lerp(pos, (double) i / n);
            Particle puff = Minecraft.getInstance().particleEngine
                    .createParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x, p.y, p.z, 0, 0, 0);
            if (puff == null) continue;
            puff.scale(8F);
            puff.setLifetime(DistantFx.contrailSeconds * 20);
        }
    }
}
