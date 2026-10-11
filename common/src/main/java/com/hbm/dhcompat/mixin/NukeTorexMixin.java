package com.hbm.dhcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server: the cloud isn't saved and holds no chunk, so once the player flies off its chunk drops below
 * FULL, the server stops tracking it and the client loses it; coming back respawns it and the
 * animation replays from the start. Keep its chunk loaded with a self-expiring ticket while it lives.
 *
 * Client: the mushroom cloud reads the heightmap for its stem base and shockwave ring. In an
 * unloaded chunk that is the world bottom, dragging the stem underground. Use the detonation height.
 */
@Mixin(targets = "com.hbm.entity.effect.EntityNukeTorex")
abstract class NukeTorexMixin {

    @WrapOperation(
            method = "simulateTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int ntmdh$height(Level level, Heightmap.Types type, int x, int z, Operation<Integer> original) {
        if (level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)))
            return original.call(level, type, x, z);
        return (int) ((Entity) (Object) this).getY();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ntmdh$holdChunk(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel level)
            level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, self.chunkPosition(), 2);
    }
}
