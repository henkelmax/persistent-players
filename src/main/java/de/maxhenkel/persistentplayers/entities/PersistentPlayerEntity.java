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

    private static final DataParameter<Optional<UUID>> ID = EntityDataManager.defineId(PersistentPlayerEntity.class, DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<String> NAME = EntityDataManager.defineId(PersistentPlayerEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> PLAYER_MODEL = EntityDataManager.defineId(PersistentPlayerEntity.class, DataSerializers.BYTE);

    public PersistentPlayerEntity(EntityType type, World world) {
        super(type, world);
        Arrays.fill(armorDropChances, 0F);
        Arrays.fill(handDropChances, 0F);

        if (Main.SERVER_CONFIG.offlinePlayersSleep.get()) {
            setPose(Pose.SLEEPING);
        }
    }

    public PersistentPlayerEntity(World world) {
        this(Main.PLAYER_ENTITY_TYPE, world);
    }

    public static PersistentPlayerEntity fromPlayer(PlayerEntity player) {
        PersistentPlayerEntity persistentPlayer = Main.PLAYER_ENTITY_TYPE.create(player.level);
        persistentPlayer.setPlayerName(player.getName().getString());
        persistentPlayer.setPlayerUUID(player.getUUID());
        for (EquipmentSlotType equipmentSlot : EquipmentSlotType.values()) {
            persistentPlayer.setItemSlot(equipmentSlot, player.getItemBySlot(equipmentSlot).copy());
        }
        persistentPlayer.setPos(player.getX(), player.getY(), player.getZ());
        persistentPlayer.yRot = player.yRot;
        persistentPlayer.yRotO = player.yRotO;
        persistentPlayer.xRot = player.xRot;
        persistentPlayer.xRotO = player.xRotO;
        persistentPlayer.yHeadRot = player.yHeadRot;
        persistentPlayer.yHeadRotO = player.yHeadRotO;
        persistentPlayer.setHealth(player.getHealth());
        persistentPlayer.setAirSupply(player.getAirSupply());
        persistentPlayer.setSecondsOnFire(player.getRemainingFireTicks());
        player.getActiveEffects().forEach(persistentPlayer::addEffect);
        persistentPlayer.setInvulnerable(player.isCreative());
        persistentPlayer.setPlayerModel(PlayerUtils.getModel(player));
        return persistentPlayer;
    }

    public void toPlayer(ServerPlayerEntity player) {
        player.setHealth(getHealth());
        player.setAirSupply(getAirSupply());
        player.setSecondsOnFire(getRemainingFireTicks());
        getActiveEffects().forEach(player::addEffect);
        player.teleportTo((ServerWorld) level, getX(), getY(), getZ(), yRot, xRot);
        player.yRot = yRot;
        player.yRotO = yRotO;
        player.xRot = xRot;
        player.xRotO = xRotO;
        player.yHeadRot = yHeadRot;
        player.yHeadRotO = yHeadRotO;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        Main.LOGIN_EVENTS.updatePersistentPlayerLocation(this, p -> {
            p.setHealth(0F);
            for (int i = 0; i < p.inventory.getContainerSize(); i++) {
                ItemStack stackInSlot = p.inventory.getItem(i);
                p.inventory.removeItemNoUpdate(i);
                spawnAtLocation(stackInSlot);
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

    public static AttributeModifierMap.MutableAttribute getPlayerAttributes() {
        return MonsterEntity.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20D);
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
        return entityData.get(ID);
    }

    public void setPlayerUUID(UUID uuid) {
        if (uuid == null) {
            entityData.set(ID, Optional.empty());
        } else {
            entityData.set(ID, Optional.of(uuid));
        }
    }

    public String getPlayerName() {
        return entityData.get(NAME);
    }

    public void setPlayerName(String name) {
        entityData.set(NAME, name);
    }

    public GameProfile getGameProfile() {
        return new GameProfile(getPlayerUUID().orElse(new UUID(0L, 0L)), getPlayerName());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ID, Optional.empty());
        entityData.define(NAME, "");
        entityData.define(PLAYER_MODEL, (byte) 0);
    }

    public boolean isWearing(PlayerModelPart part) {
        return (getPlayerModel() & part.getMask()) == part.getMask();
    }

    protected byte getPlayerModel() {
        return entityData.get(PLAYER_MODEL);
    }

    protected void setPlayerModel(byte b) {
        entityData.set(PLAYER_MODEL, b);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);

        getPlayerUUID().ifPresent(uuid -> {
            compound.putUUID("playerUUID", uuid);
        });

        compound.putString("playerName", getPlayerName());

        compound.putByte("model", getPlayerModel());

        Main.LOGIN_EVENTS.updatePersistentPlayerLocation(this, null);
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);

        if (compound.contains("playerUUID")) {
            setPlayerUUID(compound.getUUID("playerUUID"));
        }

        setPlayerName(compound.getString("playerName"));

        setPlayerModel(compound.getByte("model"));
    }

}
