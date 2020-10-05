package de.maxhenkel.persistentplayers;

import de.maxhenkel.corelib.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig extends ConfigBase {

    public final ForgeConfigSpec.BooleanValue persistCreativePlayers;
    public final ForgeConfigSpec.BooleanValue offlinePlayersSleep;

    public ServerConfig(ForgeConfigSpec.Builder builder) {
        super(builder);
        persistCreativePlayers = builder
                .comment("If players in creative mode should persist")
                .define("persist_creative_players", true);

        offlinePlayersSleep = builder
                .comment("If players that are offline should be sleeping")
                .worldRestart()
                .define("offline_players_sleep", false);
    }

}
