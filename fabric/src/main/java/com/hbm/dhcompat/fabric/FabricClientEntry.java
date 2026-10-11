package com.hbm.dhcompat.fabric;

import com.hbm.dhcompat.DistantFx;
import com.hbm.dhcompat.LodDepth;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClientEntry implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (DistantFx.lodDepth && DistantFx.dh) LodDepth.register();
    }
}
