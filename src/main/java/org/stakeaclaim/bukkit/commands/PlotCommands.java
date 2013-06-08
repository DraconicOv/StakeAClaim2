// $Id$
/*
 * StakeAClaim
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
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

package org.stakeaclaim.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import org.stakeaclaim.LocalPlayer;
//import org.stakeaclaim.bukkit.commands.RegionCommands;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.domains.DefaultDomain;
import org.stakeaclaim.protection.ApplicableRegionSet;
import org.stakeaclaim.protection.databases.ProtectionDatabaseException;
import org.stakeaclaim.protection.databases.RegionDBUtil;
import org.stakeaclaim.protection.flags.DefaultFlag;
import org.stakeaclaim.protection.flags.StateFlag;
import org.stakeaclaim.protection.managers.RegionManager;
import org.stakeaclaim.protection.regions.ProtectedRegion;

public class PlotCommands {
    private final StakeAClaimPlugin plugin;

    public PlotCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"info", "i"},
            usage = "",
            desc = "Get information about a claim",
            min = 0, max = 0)
    public void info(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = claimPlayerIsIn(player);
        final String id = claim.getId();

        checkPerm(player, "info", claim);

        if (claim.getFlag(DefaultFlag.ENTRY) == StateFlag.State.DENY) {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + id + ChatColor.RED + " Private!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + id);
        }

        if (claim.getParent() != null) {
            sender.sendMessage(ChatColor.BLUE + "Region: " + ChatColor.WHITE + claim.getParent().getId());
        }

        final DefaultDomain owners = claim.getOwners();
        if (owners.size() != 0) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Owner: " + ChatColor.GREEN + owners.toUserFriendlyString());
        }

        final DefaultDomain members = claim.getMembers();
        if (members.size() != 0) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Members: " + ChatColor.GREEN + members.toUserFriendlyString());
        }

        final BlockVector min = claim.getMinimumPoint();
        final BlockVector max = claim.getMaximumPoint();
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Bounds:"
            + " (" + min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ() + ")"
            + " (" + max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ() + ")"
            );

    }

    @Command(aliases = {"add", "addmember", "addmembers", "ad", "a"},
            usage = "<members...>",
            desc = "Add a member to a claim",
            min = 1)
    public void add(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = claimPlayerIsIn(player);
        final String id = claim.getId();
        final World world = player.getWorld();

        checkPerm(player, "add", claim);

        RegionDBUtil.addToDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        sender.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.GREEN + args.getJoinedStrings(0) + ChatColor.YELLOW + " to claim: " + ChatColor.WHITE + id);

        saveRegions(world);
    }

    @Command(aliases = {"remove", "removemember", "removemembers", "r"},
            usage = "<members...>",
            desc = "Remove a member from a claim",
            min = 1)
    public void remove(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = claimPlayerIsIn(player);
        final String id = claim.getId();
        final World world = player.getWorld();

        checkPerm(player, "remove", claim);

        RegionDBUtil.removeFromDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.GREEN + args.getJoinedStrings(0) + ChatColor.YELLOW + " from claim: " + ChatColor.WHITE + id);

        saveRegions(world);
    }

    @Command(aliases = {"keepout", "out"},
            usage = "",
            desc = "Mark a claim as a private plot",
            min = 0, max = 0)
    public void keepout(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = claimPlayerIsIn(player);
        final String id = claim.getId();
        final World world = player.getWorld();

        checkPerm(player, "keepout", claim);

        claim.setFlag(DefaultFlag.ENTRY, StateFlag.State.DENY);

        sender.sendMessage(ChatColor.WHITE + id + ChatColor.YELLOW + " set as private plot");

        saveRegions(world);
    }

    @Command(aliases = {"letin", "in"},
            usage = "",
            desc = "Unmark a claim as a private plot",
            min = 0, max = 0)
    public void letin(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = claimPlayerIsIn(player);
        final String id = claim.getId();
        final World world = player.getWorld();

        checkPerm(player, "letin", claim);

        claim.setFlag(DefaultFlag.ENTRY, null);

        sender.sendMessage(ChatColor.WHITE + id + ChatColor.YELLOW + " set as open access");

        saveRegions(world);
    }

    public ProtectedRegion claimPlayerIsIn(Player player) throws CommandException {

        final LocalPlayer localPlayer = plugin.wrapPlayer(player);
        final World world = player.getWorld();
        final RegionManager mgr = plugin.getGlobalRegionManager().get(world);
        final Vector pt = localPlayer.getPosition();
        final ApplicableRegionSet set = mgr.getApplicableRegions(pt);
        final ProtectedRegion claim = set.getClaim();

        if (claim == null) {
            throw new CommandException("You are not in a valid claim!");
        }

        return claim;
    }

    public void checkPerm(Player player, String command, ProtectedRegion claim) throws CommandPermissionsException {

        final LocalPlayer localPlayer = plugin.wrapPlayer(player);
        final String id = claim.getId();

        if (claim.isOwner(localPlayer)) {
            plugin.checkPermission(player, "worldguard.plot." + command + ".own." + id.toLowerCase());
        } else if (claim.isMember(localPlayer)) {
            plugin.checkPermission(player, "worldguard.plot." + command + ".member." + id.toLowerCase());
        } else {
            plugin.checkPermission(player, "worldguard.plot." + command + "." + id.toLowerCase());
        }
    }

    public void saveRegions(World world) throws CommandException {

        final RegionManager mgr = plugin.getGlobalRegionManager().get(world);

        try {
            mgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regions: "
                    + e.getMessage());
        }
    }
}
