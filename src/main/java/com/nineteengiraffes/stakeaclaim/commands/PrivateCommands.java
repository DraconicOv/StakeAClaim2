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
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class PrivateCommands {
    private final StakeAClaimPlugin plugin;

    public PrivateCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // Commands
    @Command(aliases = {"default", "d"},
            usage = "",
            desc = "Toggle a claim's default to private/open",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.default.private", "stakeaclaim.claim.default.private.own.*", "stakeaclaim.claim.default.private.member.*", "stakeaclaim.claim.default.private.*"})
    public void setdefault(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        checkPerm(player, "default.private", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode < 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);
        if (stake.getDefaultEntry() == null || stake.getDefaultEntry() == State.ALLOW) {
            stake.setDefaultEntry(State.DENY);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + "'s default to " + ChatColor.RED + "private.");
        } else {
            stake.setDefaultEntry(State.ALLOW);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + "'s default to " + ChatColor.GRAY + "open.");
        }

        sMgr.save();
    }

    @Command(aliases = {"clear", "c"},
            usage = "",
            desc = "Clear all privacy settings from claim",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.clear.private", "stakeaclaim.claim.clear.private.own.*", "stakeaclaim.claim.clear.private.member.*", "stakeaclaim.claim.clear.private.*"})
    public void clear(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        checkPerm(player, "clear.private", claim);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);
        stake.setDefaultEntry(null);
        claim.setFlag(DefaultFlag.ENTRY, null);
        sender.sendMessage(ChatColor.YELLOW + "Cleared " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + "'s privacy settings.");

        sMgr.save();
        saveRegions(world);
    }

    // Other methods
    private void checkPerm(Player player, String command, ProtectedRegion claim) throws CommandPermissionsException {

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

    private void saveRegions(World world) throws CommandException {

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
