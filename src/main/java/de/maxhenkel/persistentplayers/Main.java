package de.maxhenkel.persistentplayers;

import de.maxhenkel.persistentplayers.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Main.MODID, version = Main.VERSION, acceptedMinecraftVersions = Main.MC_VERSION, updateJSON = Main.UPDATE_JSON)
public class Main {

    public static final String MODID = "persistent_players";
    public static final String VERSION = "1.12.2-1.0.0";
    public static final String MC_VERSION = "[1.12.2]";
    public static final String UPDATE_JSON = "https://maxhenkel.de/update/persistent_players.json";

    @Mod.Instance
    private static Main instance;

    @SidedProxy(clientSide = "de.maxhenkel.persistentplayers.proxy.ClientProxy", serverSide = "de.maxhenkel.persistentplayers.proxy.CommonProxy")
    public static CommonProxy proxy;

    public Main() {
        instance = this;
    }

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        proxy.preinit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {
        proxy.postinit(event);
    }

    public static Main instance() {
        return instance;
    }

}