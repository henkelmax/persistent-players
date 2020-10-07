package de.maxhenkel.persistentplayers.entities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.UUID;

public class PlayerRenderer extends LivingRenderer<PersistentPlayerEntity, PlayerModel<PersistentPlayerEntity>> {

    private PlayerModel<PersistentPlayerEntity> playerModel;
    private PlayerModel<PersistentPlayerEntity> playerModelSmallArms;

    public PlayerRenderer(EntityRendererManager renderManager) {
        super(renderManager, null, 0.5F);
        playerModel = new PlayerModel<>(0F, false);
        playerModelSmallArms = new PlayerModel<>(0F, true);
        entityModel = playerModel;

        addLayer(new BipedArmorLayer<>(this, new BipedModel<>(0.5F), new BipedModel<>(1F)));
        addLayer(new HeldItemLayer<>(this));
        addLayer(new HeadLayer<>(this));
        addLayer(new ElytraLayer<>(this));
    }

    @Override
    protected void preRenderCallback(PersistentPlayerEntity entitylivingbaseIn, float partialTickTime) {
        float scale = 0.9375F;
        GlStateManager.scalef(scale, scale, scale);
    }

    @Override
    public ResourceLocation getEntityTexture(PersistentPlayerEntity entity) {
        return getSkin(new GameProfile(entity.getPlayerUUID().orElse(new UUID(0L, 0L)), entity.getPlayerName()));
    }

    @Override
    public void doRender(PersistentPlayerEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        if (isSlim(entity.getPlayerUUID().orElse(new UUID(0L, 0L)))) {
            entityModel = playerModelSmallArms;
        } else {
            entityModel = playerModel;
        }
        setModelVisibilities(entity);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.popMatrix();
    }

    public static ResourceLocation getSkin(GameProfile gameProfile) {
        Minecraft minecraft = Minecraft.getInstance();
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = minecraft.getSkinManager().loadSkinFromCache(gameProfile);

        if (map.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            return minecraft.getSkinManager().loadSkin(map.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
        } else {
            return DefaultPlayerSkin.getDefaultSkin(gameProfile.getId());
        }
    }

    public static boolean isSlim(UUID uuid) {
        NetworkPlayerInfo networkplayerinfo = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
        return networkplayerinfo == null ? (uuid.hashCode() & 1) == 1 : networkplayerinfo.getSkinType().equals("slim");
    }

    private void setModelVisibilities(PersistentPlayerEntity playerEntity) {
        entityModel.bipedHeadwear.showModel = playerEntity.isWearing(PlayerModelPart.HAT);
        entityModel.bipedBodyWear.showModel = playerEntity.isWearing(PlayerModelPart.JACKET);
        entityModel.bipedLeftLegwear.showModel = playerEntity.isWearing(PlayerModelPart.LEFT_PANTS_LEG);
        entityModel.bipedRightLegwear.showModel = playerEntity.isWearing(PlayerModelPart.RIGHT_PANTS_LEG);
        entityModel.bipedLeftArmwear.showModel = playerEntity.isWearing(PlayerModelPart.LEFT_SLEEVE);
        entityModel.bipedRightArmwear.showModel = playerEntity.isWearing(PlayerModelPart.RIGHT_SLEEVE);
    }

}
