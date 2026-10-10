package com.hbm.dhcompat;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Config + the "is this an NTM entity that must be seen from far away" test. */
public final class DistantFx implements ModInitializer {

    static final Logger LOG = LoggerFactory.getLogger("ntm_dh_compat");

    /** Blocks. Server tracking radius, effect packet radius and client far plane. */
    public static int range = 4096;
    /** NTM effect packets with at least this radius are explosions; they get sent to {@link #range}. */
    public static double minEffectRadius = 150;
    /** Write DH LOD depth into the vanilla depth buffer so LOD terrain occludes clouds/missiles/particles. */
    public static boolean lodDepth = true;
    /** Seconds a missile contrail puff lives; 0 disables the trail. */
    public static int contrailSeconds = 30;
    /** Distant Horizons is installed. */
    public static boolean dh;
    /** Registry paths in the "hbm" namespace that are tracked/rendered at {@link #range}. */
    public static Pattern entities = Pattern.compile(
            "nuke_torex|cloud_.*|missile_.*|entity_.*missile.*|entity_black_hole|entity_emp_blast|entity_laser_blast");

    private static final Map<EntityType<?>, Boolean> CACHE = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("ntm_dh_compat.properties");
        Properties p = new Properties();
        p.setProperty("range", String.valueOf(range));
        p.setProperty("minEffectRadius", String.valueOf(minEffectRadius));
        p.setProperty("entities", entities.pattern());
        p.setProperty("lodDepth", String.valueOf(lodDepth));
        p.setProperty("contrailSeconds", String.valueOf(contrailSeconds));
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
            }
            range = Integer.parseInt(p.getProperty("range").trim());
            minEffectRadius = Double.parseDouble(p.getProperty("minEffectRadius").trim());
            entities = Pattern.compile(p.getProperty("entities").trim());
            lodDepth = Boolean.parseBoolean(p.getProperty("lodDepth").trim());
            contrailSeconds = Integer.parseInt(p.getProperty("contrailSeconds").trim());
            try (Writer w = Files.newBufferedWriter(file)) {
                p.store(w, "NTM x Distant Horizons. range: blocks (server tracking, effect packets, client far plane)");
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Bad config " + file, e);
        }
        dh = FabricLoader.getInstance().isModLoaded("distanthorizons");
        if (dh) CraterLod.register();
    }

    public static boolean isDistant(Entity entity) {
        return CACHE.computeIfAbsent(entity.getType(), t -> {
            var key = entity.typeHolder().unwrapKey();
            if (key.isEmpty()) return false;
            var id = key.get().identifier();
            return id.getNamespace().equals("hbm") && entities.matcher(id.getPath()).matches();
        });
    }
}
