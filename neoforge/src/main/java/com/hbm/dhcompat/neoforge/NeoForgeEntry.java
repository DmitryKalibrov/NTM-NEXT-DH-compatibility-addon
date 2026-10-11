package com.hbm.dhcompat.neoforge;

import com.hbm.dhcompat.CraterLod;
import com.hbm.dhcompat.DistantFx;
import com.hbm.dhcompat.LodDepth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("ntm_dh_compat")
public final class NeoForgeEntry {

    public NeoForgeEntry(Dist dist) {
        DistantFx.init(FMLPaths.CONFIGDIR.get(), ModList.get().isLoaded("distanthorizons"));
        if (DistantFx.dh) NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> CraterLod.tick());
        if (dist.isClient() && DistantFx.lodDepth && DistantFx.dh) LodDepth.register();
    }
}
