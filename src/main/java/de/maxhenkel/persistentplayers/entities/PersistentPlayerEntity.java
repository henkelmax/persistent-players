package de.maxhenkel.persistentplayers.entities;

import com.google.common.base.Optional;
import com.mojang.authlib.GameProfile;
import de.maxhenkel.persistentplayers.Config;
import de.maxhenkel.persistentplayers.proxy.CommonProxy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

public class PersistentPlayerEntity extends EntityMob {

    private static final DataParameter<Optional<UUID>> ID = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<String> NAME = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.STRING);
    private static final DataParameter<Byte> PLAYER_MODEL = EntityDataManager.createKey(PersistentPlayerEntity.class, DataSerializers.BYTE);

    public PersistentPlayerEntity(World world) {
        super(world);
        Arrays.fill(inventoryArmorDropChances, 0F);
        Arrays.fill(inventoryHandsDropChances, 0F);

        if (Config.offlinePlayersSleep) {
            width = 0.25F;
            height = 0.25F;
        }
    }

    @Override
    public boolean isPlayerSleeping() {
        return Config.offlinePlayersSleep;
    }

    public static PersistentPlayerEntity fromPlayer(EntityPlayer player) {
        PersistentPlayerEntity persistentPlayer = new PersistentPlayerEntity(player.world);
        persistentPlayer.setPlayerName(player.getName());
        persistentPlayer.setPlayerUUID(player.getUniqueID());
        for (EntityEquipmentSlot equipmentSlot : EntityEquipmentSlot.values()) {
            persistentPlayer.setItemStackToSlot(equipmentSlot, player.getItemStackFromSlot(equipmentSlot).copy());
        }
        persistentPlayer.setPosition(player.posX, player.posY, player.posZ);
        persistentPlayer.rotationYaw = player.rotationYaw;
        persistentPlayer.prevRotationYaw = player.prevRotationYaw;
        persistentPlayer.rotationPitch = player.rotationPitch;
        persistentPlayer.prevRotationPitch = player.prevRotationPitch;
        persistentPlayer.rotationYawHead = player.rotationYawHead;
        persistentPlayer.prevRotationYawHead = player.prevRotationYawHead;
        persistentPlayer.setHealth(player.getHealth());
        persistentPlayer.setAir(player.getAir());
        try {
            Field fire = ObfuscationReflectionHelper.findField(Entity.class, "field_190534_ay");
            persistentPlayer.setFire((Integer) fire.get(player));
        } catch (Exception e) {
            e.printStackTrace();
        }
        player.getActivePotionEffects().forEach(persistentPlayer::addPotionEffect);
        persistentPlayer.setEntityInvulnerable(player.isCreative());
        persistentPlayer.setPlayerModel(getModel(player));
        return persistentPlayer;
    }

    public void toPlayer(EntityPlayerMP player) {
        player.setHealth(getHealth());
        player.setAir(getAir());
        try {
            Field fire = ObfuscationReflectionHelper.findField(Entity.class, "field_190534_ay");
            setFire((Integer) fire.get(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
        getActivePotionEffects().forEach(player::addPotionEffect);
        player.setPositionAndRotation(posX, posY, posZ, rotationYaw, rotationPitch);
        player.rotationYaw = rotationYaw;
        player.prevRotationYaw = prevRotationYaw;
        player.rotationPitch = rotationPitch;
        player.prevRotationPitch = prevRotationPitch;
        player.rotationYawHead = rotationYawHead;
        player.prevRotationYawHead = prevRotationYawHead;
    }

    public static byte getModel(EntityPlayer player) {
        try {
            Field flag = ObfuscationReflectionHelper.findField(EntityPlayer.class, "field_184827_bp");
            DataParameter<Byte> dataParameter = (DataParameter<Byte>) flag.get(null);
            return player.getDataManager().get(dataParameter);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        CommonProxy.PLAYER_EVENTS.updatePersistentPlayerLocation(this, p -> {
            p.setHealth(0F);
            for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
                ItemStack stackInSlot = p.inventory.getStackInSlot(i);
                p.inventory.removeStackFromSlot(i);
                entityDropItem(stackInSlot, 0F);
            }
        });
    }

    @Override
    public Entity changeDimension(int dimensionIn, ITeleporter teleporter) {
        Entity entity = super.changeDimension(dimensionIn, teleporter);
        if (entity instanceof PersistentPlayerEntity) {
            CommonProxy.PLAYER_EVENTS.updatePersistentPlayerLocation((PersistentPlayerEntity) entity, null);
        }
        return entity;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        if (!getIsInvulnerable()) {
            return super.isEntityInvulnerable(source);
        }
        return source != DamageSource.OUT_OF_WORLD;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        if (!Config.offlinePlayersSleep) {
            tasks.addTask(1, new EntityAILookIdle(this));
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20D);
    }

    @Override
    public ITextComponent getDisplayName() {
        String name = getPlayerName();
        if (name == null || name.trim().isEmpty()) {
            return new TextComponentString("Player");
        } else {
            return new TextComponentString(getPlayerName());
        }
    }

    public Optional<UUID> getPlayerUUID() {
        return dataManager.get(ID);
    }

    public void setPlayerUUID(UUID uuid) {
        if (uuid == null) {
            dataManager.set(ID, Optional.absent());
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
        return new GameProfile(getPlayerUUID().or(new UUID(0L, 0L)), getPlayerName());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ID, Optional.absent());
        dataManager.register(NAME, "");
        dataManager.register(PLAYER_MODEL, (byte) 0);
    }

    public boolean isWearing(EnumPlayerModelParts part) {
        return (getPlayerModel() & part.getPartMask()) == part.getPartMask();
    }

    protected byte getPlayerModel() {
        return dataManager.get(PLAYER_MODEL);
    }

    protected void setPlayerModel(byte b) {
        dataManager.set(PLAYER_MODEL, b);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (getPlayerUUID().isPresent()) {
            compound.setUniqueId("playerUUID", getPlayerUUID().get());
        }

        compound.setString("playerName", getPlayerName());

        compound.setByte("model", getPlayerModel());

        CommonProxy.PLAYER_EVENTS.updatePersistentPlayerLocation(this, null);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("playerUUIDMost")) {
            setPlayerUUID(compound.getUniqueId("playerUUID"));
        }

        setPlayerName(compound.getString("playerName"));

        setPlayerModel(compound.getByte("model"));
    }

}
