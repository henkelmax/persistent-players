package de.maxhenkel.persistentplayers;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean persistCreativePlayers = true;
    public static boolean offlinePlayersSleep = false;

    public static void init(Configuration config) {
        config.load();
        persistCreativePlayers = config.getBoolean("persist_creative_players", "persistent_players", true, "If players in creative mode should persist");
        offlinePlayersSleep = config.getBoolean("offline_players_sleep", "persistent_players", false, "If players that are offline should be sleeping");
        config.save();
    }
}
