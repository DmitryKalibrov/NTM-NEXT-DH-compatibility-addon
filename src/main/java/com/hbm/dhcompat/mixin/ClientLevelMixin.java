package com.hbm.dhcompat.mixin;

import com.hbm.dhcompat.DistantFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client: entities in chunks the client hasn't loaded are stored but never ticked, so a far
 * mushroom cloud would freeze at frame one and missiles would not move. Tick those ourselves.
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {

    @Inject(method = "tickEntities", at = @At("TAIL"))
    private void ntmdh$tickFar(CallbackInfo ci) {
        ClientLevel self = (ClientLevel) (Object) this;
        List<Entity> far = new ArrayList<>();
        for (Entity e : self.entitiesForRendering()) {
            if (!e.isRemoved() && !e.isPassenger() && !self.isTickingEntity(e) && DistantFx.isDistant(e)) far.add(e);
        }
        for (Entity e : far) self.guardEntityTick(self::tickNonPassenger, e);
    }
}
