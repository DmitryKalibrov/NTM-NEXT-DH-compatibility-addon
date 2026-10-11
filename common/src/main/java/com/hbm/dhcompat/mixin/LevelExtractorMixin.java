package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Client: vanilla only draws entities standing in a compiled chunk section; DH-range ones never are. */
@Mixin(LevelExtractor.class)
abstract class LevelExtractorMixin {

    @ModifyExpressionValue(
            method = "isEntityVisible",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean ntmdh$compiled(boolean original, @Local(argsOnly = true) Entity entity) {
        return original || DistantFx.isDistant(entity);
    }
}
