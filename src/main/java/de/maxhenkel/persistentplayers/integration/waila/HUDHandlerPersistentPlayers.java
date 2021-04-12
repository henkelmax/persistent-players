package de.maxhenkel.persistentplayers.integration.waila;

import mcp.mobius.waila.Waila;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

public class HUDHandlerPersistentPlayers implements IEntityComponentProvider {

    static final String MINECRAFT = "Minecraft";

    static final HUDHandlerPersistentPlayers INSTANCE = new HUDHandlerPersistentPlayers();

    @Override
    public void appendTail(List<ITextComponent> tooltip, IEntityAccessor accessor, IPluginConfig config) {
        tooltip.clear();
        tooltip.add(new StringTextComponent(String.format(Waila.CONFIG.get().getFormatting().getModName(), MINECRAFT)));
    }

}