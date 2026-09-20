package simplexity.simplebucketmobs.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import simplexity.simplebucketmobs.SimpleBucketMobs;

import java.lang.reflect.Method;

public class CoreProtectHook {

    private static final Logger logger = SimpleBucketMobs.getPlugin().getSLF4JLogger();

    private static Object coreProtectAPI;
    private static Method logInteractionMethod;
    private static boolean initialized = false;
    private static boolean cpAvailable = false;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) {
            cpAvailable = false;
            return;
        }
        try {
            org.bukkit.plugin.Plugin cpPlugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (cpPlugin == null || !cpPlugin.isEnabled()) {
                cpAvailable = false;
                return;
            }

            Method getAPIMethod = cpPlugin.getClass().getMethod("getAPI");
            Object api = getAPIMethod.invoke(cpPlugin);
            if (api == null) {
                cpAvailable = false;
                return;
            }

            Method isEnabledMethod = api.getClass().getMethod("isEnabled");
            boolean enabled = (boolean) isEnabledMethod.invoke(api);
            if (!enabled) {
                cpAvailable = false;
                return;
            }

            Method apiVersionMethod = api.getClass().getMethod("APIVersion");
            int version = (int) apiVersionMethod.invoke(api);
            if (version < 9) {
                logger.warn("CoreProtect API version {} is unsupported (requires v9+)", version);
                cpAvailable = false;
                return;
            }

            coreProtectAPI = api;
            logInteractionMethod = api.getClass().getMethod("logInteraction", String.class, Location.class);
            cpAvailable = true;
        } catch (Exception e) {
            logger.warn("Failed to initialize CoreProtect integration hooks", e);
            cpAvailable = false;
        }
    }

    public static boolean isAvailable() {
        if (!initialized) init();
        return cpAvailable && Bukkit.getPluginManager().isPluginEnabled("CoreProtect");
    }

    /**
     * Logs mob capture action to CoreProtect.
     *
     * @param player the player who captured the mob
     * @param entity the captured entity
     */
    public static void logCapture(Player player, Entity entity) {
        if (!isAvailable()) return;
        try {
            Location loc = entity.getLocation();
            String mobName = entity.customName() != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(entity.customName()) : entity.getType().name();
            logInteractionMethod.invoke(coreProtectAPI, player.getName(), loc);
            logger.info("Logged CoreProtect capture of {} ({}) by {} at {}", entity.getType().name(), mobName, player.getName(), loc);
        } catch (Exception e) {
            logger.warn("Error logging mob capture to CoreProtect", e);
        }
    }

    /**
     * Logs mob release action to CoreProtect.
     *
     * @param player the player who released the mob
     * @param location the location where the mob was released
     * @param entityInfo the entity details
     */
    public static void logRelease(Player player, Location location, String entityInfo) {
        if (!isAvailable()) return;
        try {
            logInteractionMethod.invoke(coreProtectAPI, player.getName(), location);
            logger.info("Logged CoreProtect release of {} by {} at {}", entityInfo, player.getName(), location);
        } catch (Exception e) {
            logger.warn("Error logging mob release to CoreProtect", e);
        }
    }
}
