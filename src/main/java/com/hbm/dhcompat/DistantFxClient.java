package com.hbm.dhcompat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class DistantFxClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (DistantFx.lodDepth && FabricLoader.getInstance().isModLoaded("distanthorizons")) LodDepth.register();
    }
}
