package de.maxhenkel.persistentplayers;

import de.maxhenkel.corelib.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig extends ConfigBase {

    public final ForgeConfigSpec.BooleanValue persistCreativePlayers;

    public ServerConfig(ForgeConfigSpec.Builder builder) {
        super(builder);
        persistCreativePlayers = builder
                .comment("If players in creative mode should persist")
                .define("persist_creative_players", true);
    }

}
