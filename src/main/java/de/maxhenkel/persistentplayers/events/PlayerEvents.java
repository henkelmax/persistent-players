package de.maxhenkel.persistentplayers.events;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.persistentplayers.Main;
import de.maxhenkel.persistentplayers.ServerConfig;
import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        if (player.getServerWorld().getServer().isSinglePlayer()) {
            return;
        }

        boolean foundPlayer = false;
        for (ServerWorld world : player.getServerWorld().getServer().getWorlds()) {
            Optional<PersistentPlayerEntity> persistentPlayer = findPersistentPlayer(world, player.getUniqueID());

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
        player.world.addEntity(PersistentPlayerEntity.fromPlayer(player));
    }

    public boolean shouldPersist(ServerPlayerEntity player) {
        if (player.getServerWorld().getServer().isSinglePlayer()) {
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (player.isCreative() && !ServerConfig.SERVER.persistCreativePlayers.get()) {
            return false;
        }
        return true;
    }

    public void updatePersistentPlayerLocation(PersistentPlayerEntity persistentPlayer, Consumer<ServerPlayerEntity> additionalUpdateConsumer) {
        if (!(persistentPlayer.world instanceof ServerWorld)) {
            return;
        }
        if (!persistentPlayer.getPlayerUUID().isPresent()) {
            return;
        }

        ServerWorld world = (ServerWorld) persistentPlayer.world;
        updateOfflinePlayer(world, persistentPlayer.getPlayerUUID().get(), serverPlayerEntity -> {
            Main.LOGGER.info("Updating offline player location {} x:{} y: {} z: {}", persistentPlayer.getPlayerName(), persistentPlayer.getPosX(), persistentPlayer.getPosY(), persistentPlayer.getPosZ());
            serverPlayerEntity.setPositionAndRotation(persistentPlayer.getPosX(), persistentPlayer.getPosY(), persistentPlayer.getPosZ(), persistentPlayer.rotationYaw, persistentPlayer.rotationPitch);
            serverPlayerEntity.setWorld(world);
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
        if (world.getServer().isSinglePlayer()) {
            return;
        }

        SaveHandler playerData = world.getSaveHandler();
        ServerPlayerEntity serverPlayerEntity = new ServerPlayerEntity(world.getServer(), world, new GameProfile(playerUUID, ""), new PlayerInteractionManager(world));
        playerData.readPlayerData(serverPlayerEntity);
        playerConsumer.accept(serverPlayerEntity);
        playerData.writePlayerData(serverPlayerEntity);
    }

}
