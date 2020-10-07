package de.maxhenkel.persistentplayers.proxy;

import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import de.maxhenkel.persistentplayers.entities.PlayerRenderer;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    public void preinit(FMLPreInitializationEvent event) {
        super.preinit(event);

        RenderingRegistry.registerEntityRenderingHandler(PersistentPlayerEntity.class, PlayerRenderer::new);
    }

    public void init(FMLInitializationEvent event) {
        super.init(event);

    }

    public void postinit(FMLPostInitializationEvent event) {
        super.postinit(event);
    }

}
