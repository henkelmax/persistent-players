package de.maxhenkel.persistentplayers.proxy;

import de.maxhenkel.persistentplayers.Config;
import de.maxhenkel.persistentplayers.Log;
import de.maxhenkel.persistentplayers.Main;
import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import de.maxhenkel.persistentplayers.events.PlayerEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class CommonProxy {

    public static PlayerEvents PLAYER_EVENTS = new PlayerEvents();

    public void preinit(FMLPreInitializationEvent event) {
        Configuration c;
        try {
            c = new Configuration(event.getSuggestedConfigurationFile());
            Config.init(c);
        } catch (Exception e) {
            Log.w("Could not create config file: " + e.getMessage());
        }

        Log.setLogger(event.getModLog());

    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(PLAYER_EVENTS);
        EntityRegistry.registerModEntity(new ResourceLocation(Main.MODID, "player"), PersistentPlayerEntity.class, "player", 133704, Main.instance(), 128, 1, true);
    }

    public void postinit(FMLPostInitializationEvent event) {

    }

}
