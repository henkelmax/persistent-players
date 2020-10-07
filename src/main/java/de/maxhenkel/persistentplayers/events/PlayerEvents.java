package de.maxhenkel.persistentplayers.events;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.persistentplayers.Config;
import de.maxhenkel.persistentplayers.Log;
import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerFromFile(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.getServerWorld().getMinecraftServer().isSinglePlayer()) {
            return;
        }

        boolean foundPlayer = false;
        for (WorldServer world : player.getServerWorld().getMinecraftServer().worlds) {
            Optional<PersistentPlayerEntity> persistentPlayer = findPersistentPlayer(world, player.getUniqueID());

            if (persistentPlayer.isPresent()) {
                PersistentPlayerEntity p = persistentPlayer.get();
                p.toPlayer(player);
                p.setDead();
                foundPlayer = true;
                break;
            }
        }
        if (!foundPlayer) {
            Log.e("Failed to find persisted player. Defaulting to vanilla spawning.");
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void playerLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (!shouldPersist(player)) {
            return;
        }
        player.dismountRidingEntity();
        player.world.spawnEntity(PersistentPlayerEntity.fromPlayer(player));
    }

    public boolean shouldPersist(EntityPlayerMP player) {
        if (player.getServerWorld().getMinecraftServer().isSinglePlayer()) {
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (player.isCreative() && !Config.persistCreativePlayers) {
            return false;
        }
        return true;
    }

    public void updatePersistentPlayerLocation(PersistentPlayerEntity persistentPlayer, Consumer<EntityPlayerMP> additionalUpdateConsumer) {
        if (!(persistentPlayer.world instanceof WorldServer)) {
            return;
        }
        if (!persistentPlayer.getPlayerUUID().isPresent()) {
            return;
        }

        WorldServer world = (WorldServer) persistentPlayer.world;
        updateOfflinePlayer(world, persistentPlayer.getPlayerUUID().get(), serverPlayerEntity -> {
            Log.i("Updating offline player location " + persistentPlayer.getPlayerName() + " x: " + persistentPlayer.posX + " y: " + persistentPlayer.posY + " z: " + persistentPlayer.posZ);
            serverPlayerEntity.setPositionAndRotation(persistentPlayer.posX, persistentPlayer.posY, persistentPlayer.posZ, persistentPlayer.rotationYaw, persistentPlayer.rotationPitch);
            serverPlayerEntity.dimension = world.provider.getDimension();
            if (additionalUpdateConsumer != null) {
                additionalUpdateConsumer.accept(serverPlayerEntity);
            }
        });
    }

    public Optional<PersistentPlayerEntity> findPersistentPlayer(WorldServer world, UUID playerUUID) {
        return world.getEntities(PersistentPlayerEntity.class, p -> p.getPlayerUUID().isPresent() && p.getPlayerUUID().get().equals(playerUUID)).stream().findAny();
    }

    public static void updateOfflinePlayer(WorldServer world, UUID playerUUID, Consumer<EntityPlayerMP> playerConsumer) {
        if (world.getMinecraftServer().isSinglePlayer()) {
            return;
        }

        ISaveHandler playerData = world.getSaveHandler();
        EntityPlayerMP serverPlayerEntity = new EntityPlayerMP(world.getMinecraftServer(), world, new GameProfile(playerUUID, ""), new PlayerInteractionManager(world));

        if (playerData instanceof SaveHandler) {
            SaveHandler saveHandler = (SaveHandler) playerData;
            saveHandler.readPlayerData(serverPlayerEntity);
            playerConsumer.accept(serverPlayerEntity);
            saveHandler.writePlayerData(serverPlayerEntity);
        } else {
            Log.e("Failed to write player data (Wrong SaveHandler)");
        }
    }

}
