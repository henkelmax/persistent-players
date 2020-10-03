package de.maxhenkel.persistentplayers;

import de.maxhenkel.corelib.CommonRegistry;
import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import de.maxhenkel.persistentplayers.entities.PlayerRenderer;
import de.maxhenkel.persistentplayers.events.PlayerEvents;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Main.MODID)
@Mod.EventBusSubscriber
public class Main {

    public static final String MODID = "persistent_players";

    public static final Logger LOGGER = LogManager.getLogger(Main.MODID);

    public static EntityType<PersistentPlayerEntity> PLAYER_ENTITY_TYPE;
    public static ServerConfig SERVER_CONFIG;

    public static PlayerEvents LOGIN_EVENTS = new PlayerEvents();

    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(EntityType.class, this::registerEntities);

        SERVER_CONFIG = CommonRegistry.registerConfig(ModConfig.Type.SERVER, ServerConfig.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(LOGIN_EVENTS);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(PLAYER_ENTITY_TYPE, PlayerRenderer::new);
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        PLAYER_ENTITY_TYPE = CommonRegistry.registerEntity(Main.MODID, "player", EntityClassification.CREATURE, PersistentPlayerEntity.class, playerEntityBuilder -> {
            playerEntityBuilder
                    .setTrackingRange(128)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .setCustomClientFactory((spawnEntity, world) -> new PersistentPlayerEntity(world));
        });
        event.getRegistry().register(PLAYER_ENTITY_TYPE);
        GlobalEntityTypeAttributes.put(PLAYER_ENTITY_TYPE, PersistentPlayerEntity.getAttributes().func_233813_a_());
    }

}
