package de.maxhenkel.persistentplayers.entities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.UUID;

public class PlayerRenderer extends RenderLivingBase<PersistentPlayerEntity> {

    private ModelPlayer playerModel;
    private ModelPlayer playerModelSmallArms;

    public PlayerRenderer(RenderManager renderManager) {
        super(renderManager, null, 0.5F);

        playerModel = new ModelPlayer(0F, false);
        playerModelSmallArms = new ModelPlayer(0F, true);
        mainModel = playerModel;
    }

    @Override
    protected void preRenderCallback(PersistentPlayerEntity entitylivingbaseIn, float partialTickTime) {
        float scale = 0.9375F;
        GlStateManager.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getEntityTexture(PersistentPlayerEntity entity) {
        return getSkin(new GameProfile(entity.getPlayerUUID().or(new UUID(0L, 0L)), entity.getPlayerName()));
    }

    @Override
    public void doRender(PersistentPlayerEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        if (isSlim(entity.getPlayerUUID().or(new UUID(0L, 0L)))) {
            mainModel = playerModelSmallArms;
            setModelVisibilities(entity, playerModelSmallArms);
            initLayers(playerModelSmallArms);
        } else {
            mainModel = playerModel;
            setModelVisibilities(entity, playerModel);
            initLayers(playerModel);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.popMatrix();
    }

    public static ResourceLocation getSkin(GameProfile gameProfile) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().loadSkinFromCache(gameProfile);

        if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            return minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
        } else {
            return DefaultPlayerSkin.getDefaultSkin(gameProfile.getId());
        }
    }

    public static boolean isSlim(UUID uuid) {
        NetworkPlayerInfo networkplayerinfo = Minecraft.getMinecraft().getConnection().getPlayerInfo(uuid);
        return networkplayerinfo == null ? (uuid.hashCode() & 1) == 1 : networkplayerinfo.getSkinType().equals("slim");
    }

    private void setModelVisibilities(PersistentPlayerEntity playerEntity, ModelPlayer modelPlayer) {
        modelPlayer.bipedHeadwear.showModel = playerEntity.isWearing(EnumPlayerModelParts.HAT);
        modelPlayer.bipedBodyWear.showModel = playerEntity.isWearing(EnumPlayerModelParts.JACKET);
        modelPlayer.bipedLeftLegwear.showModel = playerEntity.isWearing(EnumPlayerModelParts.LEFT_PANTS_LEG);
        modelPlayer.bipedRightLegwear.showModel = playerEntity.isWearing(EnumPlayerModelParts.RIGHT_PANTS_LEG);
        modelPlayer.bipedLeftArmwear.showModel = playerEntity.isWearing(EnumPlayerModelParts.LEFT_SLEEVE);
        modelPlayer.bipedRightArmwear.showModel = playerEntity.isWearing(EnumPlayerModelParts.RIGHT_SLEEVE);
    }

    private void initLayers(ModelPlayer modelPlayer) {
        layerRenderers.clear();
        addLayer(new LayerBipedArmor(this));
        addLayer(new LayerHeldItem(this));
        addLayer(new LayerElytra(this));
        addLayer(new LayerCustomHead(modelPlayer.bipedHead));
    }

    @Override
    protected void applyRotations(PersistentPlayerEntity entityLiving, float f, float rotationYaw, float partialTicks) {
        if (entityLiving.isEntityAlive() && entityLiving.isPlayerSleeping()) {
            GlStateManager.translate(1.8F, 0F, 0F);
            GlStateManager.rotate(this.getDeathMaxRotation(entityLiving), 0F, 0F, 1F);
            GlStateManager.rotate(270F, 0F, 1F, 0F);
        } else {
            super.applyRotations(entityLiving, f, rotationYaw, partialTicks);
        }
    }
}
