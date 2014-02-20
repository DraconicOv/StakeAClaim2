// $Id$
/*
 * StakeAClaim
 * Copyright (C) 2013 NineteenGiraffes <http://www.NineteenGiraffes.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.nineteengiraffes.stakeaclaim.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.SACFlags;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class DeleteCommands {
    private final StakeAClaimPlugin plugin;

    public DeleteCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // Commands
    @Command(aliases = {"warp", "w"},
            usage = "",
            desc = "Delete this claim's warp",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.del.warp", "stakeaclaim.claim.del.warp.own.*", "stakeaclaim.claim.del.warp.member.*", "stakeaclaim.claim.del.warp.*"})
    public void warp(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        checkPerm(player, "del.warp", claim);

        claim.setFlag(DefaultFlag.TELE_LOC, null);
        sender.sendMessage(ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + "'s warp deleted.");

        saveRegions(world);
    }

    @Command(aliases = {"name", "n"},
            usage = "",
            desc = "Delete this claim's name",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.del.name", "stakeaclaim.claim.del.name.own.*", "stakeaclaim.claim.del.name.member.*", "stakeaclaim.claim.del.name.*"})
    public void name(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        checkPerm(player, "del.name", claim);

        claim.setFlag(SACFlags.CLAIM_NAME, null);
        sender.sendMessage(ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + "'s name deleted.");

        saveRegions(world);
    }

    @Command(aliases = {"vip", "v"},
            usage = "",
            desc = "Mark this claim anyone",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.del.vip", "stakeaclaim.claim.del.vip.own.*", "stakeaclaim.claim.del.vip.member.*", "stakeaclaim.claim.del.vip.*"})
    public void vip(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        checkPerm(player, "del.vip", claim);

        claim.setFlag(SACFlags.VIP,null);
        sender.sendMessage(ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + " set to anyone.");

        saveRegions(world);
    }

    // Other methods
    public void checkPerm(Player player, String command, ProtectedRegion claim) throws CommandPermissionsException {

        final String playerName = player.getName();
        final String id = claim.getId();

        if (claim.isOwner(playerName)) {
            plugin.checkPermission(player, "stakeaclaim.claim." + command + ".own." + id.toLowerCase());
        } else if (claim.isMember(playerName)) {
            plugin.checkPermission(player, "stakeaclaim.claim." + command + ".member." + id.toLowerCase());
        } else {
            plugin.checkPermission(player, "stakeaclaim.claim." + command + "." + id.toLowerCase());
        }
    }

    public void saveRegions(World world) throws CommandException {

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        try {
            rgMgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regions: " + e.getMessage());
        }
    }

}
