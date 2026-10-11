package com.hbm.dhcompat.neoforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge only: DH runs its vanilla-fade passes on RenderLevelStageEvent.AfterOpaqueFeatures and
 * AfterLevel, i.e. after entities and particles (on Fabric: after opaque / translucent terrain, before
 * them). Clouds made of particles write no depth, so those passes paint LOD terrain over NTM mushroom
 * clouds wherever LOD ground is behind them. Skip both: on NeoForge the fade mode acts as NONE.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.api.internal.ClientApi", remap = false)
abstract class DhFadeMixin {

    @Inject(method = "renderFadeOpaque", at = @At("HEAD"), cancellable = true, require = 0)
    private void ntmdh$skipOpaqueFade(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderFadeTransparent", at = @At("HEAD"), cancellable = true, require = 0)
    private void ntmdh$skipTransparentFade(CallbackInfo ci) {
        ci.cancel();
    }
}
