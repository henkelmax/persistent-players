package de.maxhenkel.persistentplayers;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        Pair<ServerConfig, ForgeConfigSpec> specPairServer = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPairServer.getRight();
        SERVER = specPairServer.getLeft();
    }

    public final ForgeConfigSpec.BooleanValue persistCreativePlayers;
    public final ForgeConfigSpec.BooleanValue offlinePlayersSleep;

    public ServerConfig(ForgeConfigSpec.Builder builder) {
        persistCreativePlayers = builder
                .comment("If players in creative mode should persist")
                .define("persist_creative_players", true);

        offlinePlayersSleep = builder
                .comment("If players that are offline should be sleeping")
                .worldRestart()
                .define("offline_players_sleep", false);
    }

}
