package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.CraterLod;
import com.hbm.dhcompat.DistantFx;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server: fallout finished scorching the land and setting crater biomes; refresh the DH LODs over it. */
@Mixin(targets = "com.hbm.entity.effect.EntityFalloutRain")
abstract class FalloutRainMixin {

    @Shadow public abstract int getScale();

    @Inject(method = "remove", at = @At("HEAD"))
    private void ntmdh$fallout(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason == Entity.RemovalReason.DISCARDED && DistantFx.dh) CraterLod.queue((Entity) (Object) this, getScale());
    }
}
