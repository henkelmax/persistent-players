package de.maxhenkel.persistentplayers.integration.waila;

import de.maxhenkel.persistentplayers.entities.PersistentPlayerEntity;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;

@WailaPlugin
public class PluginPersistentPlayers implements IWailaPlugin {

    @Override
    public void register(IRegistrar registrar) {
        registrar.registerComponentProvider(HUDHandlerPersistentPlayers.INSTANCE, TooltipPosition.TAIL, PersistentPlayerEntity.class);
    }

}