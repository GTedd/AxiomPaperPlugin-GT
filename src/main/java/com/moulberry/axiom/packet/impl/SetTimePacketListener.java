package com.moulberry.axiom.packet.impl;

import com.moulberry.axiom.AxiomPaper;
import com.moulberry.axiom.event.AxiomTimeChangeEvent;
import com.moulberry.axiom.integration.plotsquared.PlotSquaredIntegration;
import com.moulberry.axiom.packet.PacketHandler;
import io.netty.buffer.Unpooled;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class SetTimePacketListener implements PacketHandler {

    private final AxiomPaper plugin;
    public SetTimePacketListener(AxiomPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReceive(Player player, RegistryFriendlyByteBuf friendlyByteBuf) {
        if (!this.plugin.canUseAxiom(player, "axiom.world.time")) {
            return;
        }

        ResourceKey<Level> key = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
        Integer time = friendlyByteBuf.readNullable(FriendlyByteBuf::readInt);
        Boolean freezeTime = friendlyByteBuf.readNullable(FriendlyByteBuf::readBoolean);

        if (time == null && freezeTime == null) return;

        ServerLevel level = ((CraftWorld)player.getWorld()).getHandle();
        if (!level.dimension().equals(key)) return;

        // Call modify world
        if (!this.plugin.canModifyWorld(player, player.getWorld())) {
            return;
        }

        // Don't allow on plot worlds
        if (PlotSquaredIntegration.isPlotWorld(player.getWorld())) {
            return;
        }

        // Call time change event
        AxiomTimeChangeEvent timeChangeEvent = new AxiomTimeChangeEvent(player, time, freezeTime);
        Bukkit.getPluginManager().callEvent(timeChangeEvent);
        if (timeChangeEvent.isCancelled()) return;

        // Change time
        if (time != null) level.setDayTime(time);
        if (freezeTime != null) level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(!freezeTime, null);
    }

}
