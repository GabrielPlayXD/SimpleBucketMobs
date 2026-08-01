package simplexity.simplebucketmobs.util;

import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.TrustTypes;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GriefDefenderHook {

    public static boolean canBucket(Player player, Entity entity) {
        User user = GriefDefender.getCore().getUser(player.getUniqueId());
        if (user == null) {
            return false;
        }
        return user.canInteractWithEntity(entity.getLocation(), entity, TrustTypes.ACCESSOR);
    }

    public static boolean canUnbucket(Player player, Location location, ItemStack bucket) {
        User user = GriefDefender.getCore().getUser(player.getUniqueId());
        if (user == null) {
            return false;
        }
        return user.canPlace(location, bucket);
    }
}
