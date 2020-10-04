package de.maxhenkel.persistentplayers.integration.waila;

import mcp.mobius.waila.Waila;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.utils.ModIdentification;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;

public class HUDHandlerPersistentPlayers implements IEntityComponentProvider {

    static final ModIdentification.Info MINECRAFT = ModIdentification.getModInfo("minecraft");

    static final HUDHandlerPersistentPlayers INSTANCE = new HUDHandlerPersistentPlayers();

    @Override
    public void appendTail(List<ITextComponent> tooltip, IEntityAccessor accessor, IPluginConfig config) {
        tooltip.clear();
        tooltip.add(new StringTextComponent(String.format(Waila.CONFIG.get().getFormatting().getModName(), MINECRAFT.getName())));
    }

}