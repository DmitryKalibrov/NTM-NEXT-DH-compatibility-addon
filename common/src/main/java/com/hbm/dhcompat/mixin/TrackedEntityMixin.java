package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Server: send NTM clouds/missiles to players far beyond view distance and outside their loaded chunks. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class TrackedEntityMixin {

    @Shadow @Final private Entity entity;

    @ModifyExpressionValue(method = "updatePlayer", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int ntmdh$range(int original) {
        return DistantFx.isDistant(entity) ? Math.max(original, DistantFx.range) : original;
    }

    @ModifyExpressionValue(
            method = "updatePlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"))
    private boolean ntmdh$tracked(boolean original) {
        return original || DistantFx.isDistant(entity);
    }
}
