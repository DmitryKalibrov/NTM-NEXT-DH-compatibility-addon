package com.hbm.dhcompat.fabric;

import com.hbm.dhcompat.CraterLod;
import com.hbm.dhcompat.DistantFx;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntry implements ModInitializer {

    @Override
    public void onInitialize() {
        DistantFx.init(FabricLoader.getInstance().getConfigDir(), FabricLoader.getInstance().isModLoaded("distanthorizons"));
        if (DistantFx.dh) ServerTickEvents.END_SERVER_TICK.register(server -> CraterLod.tick());
    }
}
