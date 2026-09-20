package simplexity.simplebucketmobs.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import simplexity.simplebucketmobs.SimpleBucketMobs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GriefDefenderHook {

    private static final Logger logger = SimpleBucketMobs.getPlugin().getSLF4JLogger();

    private static Class<?> gdClass;
    private static Method getCoreMethod;
    private static Method getClaimAtMethod;
    private static Method isWildernessMethod;
    private static Method getUserMethod;
    private static Method isUserTrustedMethod;
    private static Method canInteractWithEntityMethod;
    private static Method canUseBlockMethod;

    private static Object containerTrust;
    private static boolean initialized = false;
    private static boolean gdAvailable = false;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        if (!Bukkit.getPluginManager().isPluginEnabled("GriefDefender")) {
            gdAvailable = false;
            return;
        }
        try {
            gdClass = Class.forName("com.griefdefender.api.GriefDefender");
            getCoreMethod = gdClass.getMethod("getCore");

            Class<?> coreClass = Class.forName("com.griefdefender.api.Core");
            getClaimAtMethod = coreClass.getMethod("getClaimAt", Object.class);
            getUserMethod = coreClass.getMethod("getUser", java.util.UUID.class);

            Class<?> claimClass = Class.forName("com.griefdefender.api.claim.Claim");
            isWildernessMethod = claimClass.getMethod("isWilderness");

            Class<?> userClass = Class.forName("com.griefdefender.api.User");
            Class<?> trustTypeClass = Class.forName("com.griefdefender.api.claim.TrustType");

            isUserTrustedMethod = claimClass.getMethod("isUserTrusted", java.util.UUID.class, trustTypeClass);

            try {
                canInteractWithEntityMethod = userClass.getMethod("canInteractWithEntity", Object.class, Object.class, trustTypeClass);
            } catch (NoSuchMethodException ignored) {}

            try {
                canUseBlockMethod = userClass.getMethod("canUseBlock", Object.class, trustTypeClass, boolean.class, boolean.class);
            } catch (NoSuchMethodException ignored) {}

            Class<?> trustTypesClass = Class.forName("com.griefdefender.api.claim.TrustTypes");
            Field containerField = trustTypesClass.getField("CONTAINER");
            containerTrust = containerField.get(null);

            gdAvailable = true;
        } catch (Exception e) {
            logger.warn("Failed to initialize GriefDefender integration hooks", e);
            gdAvailable = false;
        }
    }

    public static boolean isAvailable() {
        if (!initialized) init();
        return gdAvailable && Bukkit.getPluginManager().isPluginEnabled("GriefDefender");
    }

    /**
     * Checks if a player is allowed to interact with/capture an entity in a claim.
     *
     * @param player the player interacting
     * @param entity the target entity
     * @return true if allowed or GriefDefender is not present; false if denied
     */
    public static boolean canInteractWithEntity(Player player, Entity entity) {
        if (!isAvailable()) return true;
        try {
            Object core = getCoreMethod.invoke(null);
            if (core == null) return true;

            Location loc = entity.getLocation();
            Object claim = getClaimAtMethod.invoke(core, loc);
            if (claim == null) return true;

            boolean isWild = (boolean) isWildernessMethod.invoke(claim);
            if (isWild) return true;

            Object user = getUserMethod.invoke(core, player.getUniqueId());
            if (user != null && canInteractWithEntityMethod != null) {
                Object itemInHand = player.getInventory().getItemInMainHand();
                boolean allowed = (boolean) canInteractWithEntityMethod.invoke(user, itemInHand, entity, containerTrust);
                if (!allowed) {
                    player.sendRichMessage("<red>You don't have permission to interact with entities in this claim.</red>");
                    return false;
                }
                return true;
            }

            boolean isTrusted = (boolean) isUserTrustedMethod.invoke(claim, player.getUniqueId(), containerTrust);
            if (!isTrusted) {
                player.sendRichMessage("<red>You don't have permission to interact with entities in this claim.</red>");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Error executing GriefDefender permission check for entity interaction", e);
            return true;
        }
    }

    /**
     * Checks if a player is allowed to release/spawn a mob at a location in a claim.
     * Releasing uses entity interaction / block use check in GD claims.
     *
     * @param player the player releasing the mob
     * @param location the location where the mob will spawn
     * @return true if allowed or GriefDefender is not present; false if denied
     */
    public static boolean canReleaseAt(Player player, Location location) {
        if (!isAvailable()) return true;
        try {
            Object core = getCoreMethod.invoke(null);
            if (core == null) return true;

            Object claim = getClaimAtMethod.invoke(core, location);
            if (claim == null) return true;

            boolean isWild = (boolean) isWildernessMethod.invoke(claim);
            if (isWild) return true;

            Object user = getUserMethod.invoke(core, player.getUniqueId());
            if (user != null && canUseBlockMethod != null) {
                boolean allowed = (boolean) canUseBlockMethod.invoke(user, location, containerTrust, false, false);
                if (!allowed) {
                    player.sendRichMessage("<red>You don't have permission to release mobs in this claim.</red>");
                    return false;
                }
                return true;
            }

            boolean isTrusted = (boolean) isUserTrustedMethod.invoke(claim, player.getUniqueId(), containerTrust);
            if (!isTrusted) {
                player.sendRichMessage("<red>You don't have permission to release mobs in this claim.</red>");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Error executing GriefDefender permission check for mob release", e);
            return true;
        }
    }
}
