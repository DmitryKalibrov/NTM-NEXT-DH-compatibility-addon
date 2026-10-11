package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client: vanilla far plane is 4x render distance, so anything past it is clipped and frustum-culled
 * (entities, particles, Flywheel visuals). Push it out to DH range. Depth precision is governed by
 * the 0.05 near plane, so this costs nothing measurable up close.
 */
@Mixin(Camera.class)
abstract class CameraMixin {

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float ntmdh$depthFar(float original) {
        return Math.max(original, DistantFx.range * 1.25F);
    }
}
