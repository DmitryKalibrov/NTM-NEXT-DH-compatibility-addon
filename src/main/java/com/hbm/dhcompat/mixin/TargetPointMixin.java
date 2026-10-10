package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server: NTM sends explosion effect packets (muke clouds, large explosions) to players inside
 * TargetPoint.radius(); its only reader is PacketWire.playersAround. Big radii = explosions.
 */
@Mixin(targets = "com.hbm.handler.threading.TargetPoint")
abstract class TargetPointMixin {

    @Inject(method = "radius", at = @At("RETURN"), cancellable = true)
    private void ntmdh$radius(CallbackInfoReturnable<Double> cir) {
        double r = cir.getReturnValueD();
        if (r >= DistantFx.minEffectRadius && r < DistantFx.range) cir.setReturnValue((double) DistantFx.range);
    }
}
