package de.maxhenkel.persistentplayers.events;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.persistentplayers.Main;
import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.PlayerData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerFromFile(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        if (player.getLevel().getServer().isSingleplayer()) {
            return;
        }

        boolean foundPlayer = false;
        for (ServerWorld world : player.getLevel().getServer().getAllLevels()) {
            Optional<PersistentPlayerEntity> persistentPlayer = findPersistentPlayer(world, player.getUUID());

            if (persistentPlayer.isPresent()) {
                PersistentPlayerEntity p = persistentPlayer.get();
                p.toPlayer(player);
                p.remove();
                foundPlayer = true;
                break;
            }
        }
        if (!foundPlayer) {
            Main.LOGGER.error("Failed to find persisted player. Defaulting to vanilla spawning.");
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        if (!shouldPersist(player)) {
            return;
        }
        player.stopRiding();
        player.level.addFreshEntity(PersistentPlayerEntity.fromPlayer(player));
    }

    public boolean shouldPersist(ServerPlayerEntity player) {
        if (player.getLevel().getServer().isSingleplayer()) {
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (player.isCreative() && !Main.SERVER_CONFIG.persistCreativePlayers.get()) {
            return false;
        }
        return true;
    }

    public void updatePersistentPlayerLocation(PersistentPlayerEntity persistentPlayer, Consumer<ServerPlayerEntity> additionalUpdateConsumer) {
        if (!(persistentPlayer.level instanceof ServerWorld)) {
            return;
        }
        if (!persistentPlayer.getPlayerUUID().isPresent()) {
            return;
        }

        ServerWorld world = (ServerWorld) persistentPlayer.level;
        updateOfflinePlayer(world, persistentPlayer.getPlayerUUID().get(), serverPlayerEntity -> {
            Main.LOGGER.info("Updating offline player location {} x:{} y: {} z: {}", persistentPlayer.getPlayerName(), persistentPlayer.getX(), persistentPlayer.getY(), persistentPlayer.getZ());
            serverPlayerEntity.absMoveTo(persistentPlayer.getX(), persistentPlayer.getY(), persistentPlayer.getZ(), persistentPlayer.yRot, persistentPlayer.xRot);
            serverPlayerEntity.setLevel(world);
            if (additionalUpdateConsumer != null) {
                additionalUpdateConsumer.accept(serverPlayerEntity);
            }
        });
    }

    public Optional<PersistentPlayerEntity> findPersistentPlayer(ServerWorld world, UUID playerUUID) {
        return world.getEntities()
                .filter(PersistentPlayerEntity.class::isInstance)
                .map(PersistentPlayerEntity.class::cast)
                .filter(p -> p.getPlayerUUID().isPresent())
                .filter(p -> p.getPlayerUUID().get().equals(playerUUID))
                .findAny();
    }

    public static void updateOfflinePlayer(ServerWorld world, UUID playerUUID, Consumer<ServerPlayerEntity> playerConsumer) {
        if (world.getServer().isSingleplayer()) {
            return;
        }

        PlayerData playerData = getPlayerData(world);
        ServerPlayerEntity serverPlayerEntity = new ServerPlayerEntity(world.getServer(), world, new GameProfile(playerUUID, ""), new PlayerInteractionManager(world));
        playerData.load(serverPlayerEntity);
        playerConsumer.accept(serverPlayerEntity);
        playerData.save(serverPlayerEntity);
    }

    public static PlayerData getPlayerData(ServerWorld world) {
        try {
            Field field = ObfuscationReflectionHelper.findField(MinecraftServer.class, "field_240766_e_");
            return (PlayerData) field.get(world.getServer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
