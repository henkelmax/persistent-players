package de.maxhenkel.persistentplayers;

import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import de.maxhenkel.persistentplayers.entities.PlayerRenderer;
import de.maxhenkel.persistentplayers.events.PlayerEvents;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
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

    public static PlayerEvents LOGIN_EVENTS = new PlayerEvents();

    public Main() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(EntityType.class, this::registerEntities);


        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_SPEC);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(Main.this::clientSetup));
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(LOGIN_EVENTS);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(PersistentPlayerEntity.class, PlayerRenderer::new);
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        PLAYER_ENTITY_TYPE = EntityType.Builder.<PersistentPlayerEntity>create(PersistentPlayerEntity::new, EntityClassification.MISC)
                .setTrackingRange(128)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true)
                .setCustomClientFactory((spawnEntity, world) -> new PersistentPlayerEntity(world))
                .build(Main.MODID + ":player");
        PLAYER_ENTITY_TYPE.setRegistryName(new ResourceLocation(Main.MODID, "player"));
        event.getRegistry().register(PLAYER_ENTITY_TYPE);
    }

}
