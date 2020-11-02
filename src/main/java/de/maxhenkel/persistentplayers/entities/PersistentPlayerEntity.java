package de.maxhenkel.persistentplayers.entities;

import com.mojang.authlib.GameProfile;
import de.maxhenkel.corelib.player.PlayerUtils;
import de.maxhenkel.persistentplayers.Main;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class PersistentPlayerEntity extends MobEntity {

    private static final DataParameter<Optional<UUID>> ID = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<String> NAME = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> PLAYER_MODEL = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.BYTE);

    public PersistentPlayerEntity(EntityType type, World world) {
        super(type, world);
        Arrays.fill(inventoryArmorDropChances, 0F);
        Arrays.fill(inventoryHandsDropChances, 0F);

        if (Main.SERVER_CONFIG.offlinePlayersSleep.get()) {
            setPose(Pose.SLEEPING);
        }
    }

    public PersistentPlayerEntity(World world) {
        this(Main.PLAYER_ENTITY_TYPE, world);
    }

    public static PersistentPlayerEntity fromPlayer(PlayerEntity player) {
        PersistentPlayerEntity persistentPlayer = Main.PLAYER_ENTITY_TYPE.create(player.world);
        persistentPlayer.setPlayerName(player.getName().getString());
        persistentPlayer.setPlayerUUID(player.getUniqueID());
        for (EquipmentSlotType equipmentSlot : EquipmentSlotType.values()) {
            persistentPlayer.setItemStackToSlot(equipmentSlot, player.getItemStackFromSlot(equipmentSlot).copy());
        }
        persistentPlayer.setPosition(player.getPosX(), player.getPosY(), player.getPosZ());
        persistentPlayer.rotationYaw = player.rotationYaw;
        persistentPlayer.prevRotationYaw = player.prevRotationYaw;
        persistentPlayer.rotationPitch = player.rotationPitch;
        persistentPlayer.prevRotationPitch = player.prevRotationPitch;
        persistentPlayer.rotationYawHead = player.rotationYawHead;
        persistentPlayer.prevRotationYawHead = player.prevRotationYawHead;
        persistentPlayer.setHealth(player.getHealth());
        persistentPlayer.setAir(player.getAir());
        persistentPlayer.setFire(player.getFireTimer());
        player.getActivePotionEffects().forEach(persistentPlayer::addPotionEffect);
        persistentPlayer.setInvulnerable(player.isCreative());
        persistentPlayer.setPlayerModel(PlayerUtils.getModel(player));
        return persistentPlayer;
    }

    public void toPlayer(ServerPlayerEntity player) {
        player.setHealth(getHealth());
        player.setAir(getAir());
        player.setFire(getFireTimer());
        getActivePotionEffects().forEach(player::addPotionEffect);
        player.teleport((ServerWorld) world, getPosX(), getPosY(), getPosZ(), rotationYaw, rotationPitch);
        player.rotationYaw = rotationYaw;
        player.prevRotationYaw = prevRotationYaw;
        player.rotationPitch = rotationPitch;
        player.prevRotationPitch = prevRotationPitch;
        player.rotationYawHead = rotationYawHead;
        player.prevRotationYawHead = prevRotationYawHead;
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        Main.LOGIN_EVENTS.updatePersistentPlayerLocation(this, p -> {
            p.setHealth(0F);
            for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
                ItemStack stackInSlot = p.inventory.getStackInSlot(i);
                p.inventory.removeStackFromSlot(i);
                entityDropItem(stackInSlot);
            }
        });
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (!isInvulnerable()) {
            return super.isInvulnerableTo(source);
        }
        return source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new SwimGoal(this));
        if (!Main.SERVER_CONFIG.offlinePlayersSleep.get()) {
            goalSelector.addGoal(1, new LookAtGoal(this, MobEntity.class, 8F));
            goalSelector.addGoal(2, new LookRandomlyGoal(this));
        }

    }

    public static AttributeModifierMap.MutableAttribute getAttributes() {
        return MonsterEntity.func_234295_eP_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 20D);
    }

    @Override
    public ITextComponent getDisplayName() {
        String name = getPlayerName();
        if (name == null || name.trim().isEmpty()) {
            return super.getDisplayName();
        } else {
            return new StringTextComponent(getPlayerName());
        }
    }

    public Optional<UUID> getPlayerUUID() {
        return dataManager.get(ID);
    }

    public void setPlayerUUID(UUID uuid) {
        if (uuid == null) {
            dataManager.set(ID, Optional.empty());
        } else {
            dataManager.set(ID, Optional.of(uuid));
        }
    }

    public String getPlayerName() {
        return dataManager.get(NAME);
    }

    public void setPlayerName(String name) {
        dataManager.set(NAME, name);
    }

    public GameProfile getGameProfile() {
        return new GameProfile(getPlayerUUID().orElse(new UUID(0L, 0L)), getPlayerName());
    }

    @Override
    protected void registerData() {
        super.registerData();
        dataManager.register(ID, Optional.empty());
        dataManager.register(NAME, "");
        dataManager.register(PLAYER_MODEL, (byte) 0);
    }

    public boolean isWearing(PlayerModelPart part) {
        return (getPlayerModel() & part.getPartMask()) == part.getPartMask();
    }

    protected byte getPlayerModel() {
        return dataManager.get(PLAYER_MODEL);
    }

    protected void setPlayerModel(byte b) {
        dataManager.set(PLAYER_MODEL, b);
    }

    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);

        getPlayerUUID().ifPresent(uuid -> {
            compound.putUniqueId("playerUUID", uuid);
        });

        compound.putString("playerName", getPlayerName());

        compound.putByte("model", getPlayerModel());

        Main.LOGIN_EVENTS.updatePersistentPlayerLocation(this, null);
    }

    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);

        if (compound.contains("playerUUID")) {
            setPlayerUUID(compound.getUniqueId("playerUUID"));
        }

        setPlayerName(compound.getString("playerName"));

        setPlayerModel(compound.getByte("model"));
    }

}
