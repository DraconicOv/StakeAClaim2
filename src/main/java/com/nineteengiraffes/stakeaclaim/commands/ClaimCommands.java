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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigurationManager;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.SACFlags;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfiguration;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.databases.RegionDBUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ClaimCommands {
    private final StakeAClaimPlugin plugin;

    public ClaimCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // Commands
    @Command(aliases = {"info", "i"},
            usage = "",
            desc = "Get information about a claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.info.*")
    public void info(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "info", claim);

        if (claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + regionID + ChatColor.RED + " Private!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + regionID);
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

    @Command(aliases = {"stake", "s"},
            usage = "",
            desc = "Stake your claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.stake")
    public void stake(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode <= 0) {
            claim.getMembers().getPlayers().clear();
            claim.getMembers().getGroups().clear();
            if (claim.getFlag(SACFlags.PENDING) != null && claim.getFlag(SACFlags.PENDING) == true) {
                if (claim.getFlag(SACFlags.REQUEST_NAME) == null) {
                    claim.setFlag(SACFlags.REQUEST_STATUS,null);
                    claim.setFlag(SACFlags.PENDING,null);
                    claim.setFlag(SACFlags.ENTRY_DEFAULT,null);
                    claim.setFlag(DefaultFlag.ENTRY,null);
                } else if (!claim.getFlag(SACFlags.REQUEST_NAME).equals(player.getName().toLowerCase())) {
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "This claim is already requested by " +
                            ChatColor.GREEN + claim.getFlag(SACFlags.REQUEST_NAME) + ".");
                }
            }
        } else if (ownedCode == 1) {
            // Remove pending flag
            claim.setFlag(SACFlags.PENDING,null);
            if (claim.getFlag(SACFlags.REQUEST_NAME) != null && claim.getOwners().equals(claim.getFlag(SACFlags.REQUEST_NAME))) {
                claim.setFlag(SACFlags.REQUEST_NAME,null);
                claim.setFlag(SACFlags.REQUEST_STATUS,null);
            }
            saveRegions(world);

            if (claim.getOwners().equals(player.getName().toLowerCase())) {
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        } else {
                throw new CommandException(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                          claim.getId() + ChatColor.RED + " already has multiple owners!");
        }

        // Check if this would be over the claimMax
        ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedRegions(rgMgr, player);
        boolean selfClaimActive = false;

        if (wcfg.claimLimitsAreArea) {
            double area = getArea(claim);
            for (ProtectedRegion region : regionList) {
                area = area + getArea(region);
            }
            if (area <= wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (area > wcfg.claimMax && wcfg.claimMax != -1) {
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim area.");
            }
        } else {
            if (regionList.size() < wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (regionList.size() >= wcfg.claimMax && wcfg.claimMax != -1) {
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "You have already claimed the maximum number of claims.");
            }
        }

        // Remove pending flag on any/all other claims
        regionList = SACUtil.getPendingRegions(rgMgr, player);
        for (ProtectedRegion region : regionList) {
            region.setFlag(SACFlags.REQUEST_STATUS,null);
            region.setFlag(SACFlags.REQUEST_NAME,null);
            region.setFlag(SACFlags.PENDING,null);
            region.setFlag(SACFlags.ENTRY_DEFAULT,null);
            region.setFlag(DefaultFlag.ENTRY,null);
        }

        // Submit request
        claim.setFlag(SACFlags.PENDING,true);
        claim.setFlag(SACFlags.REQUEST_NAME,player.getName().toLowerCase());
        claim.setFlag(SACFlags.REQUEST_STATUS,"Pending");

        boolean useReclaimed = false;
        if (claim.getFlag(SACFlags.RECLAIMED) != null && claim.getFlag(SACFlags.RECLAIMED) == true && wcfg.showReclaimOnStake) {
                useReclaimed = true;
        }
        if (selfClaimActive && !wcfg.twoStepSelfClaim && useReclaimed == false) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            claim.setFlag(SACFlags.PENDING,null);
            claim.setFlag(SACFlags.REQUEST_NAME,null);
            claim.setFlag(SACFlags.REQUEST_STATUS,null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + claim.getId() + "!");
        } else {
            if (useReclaimed == true) {
                sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + claim.getId() + 
                        ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Your stake request for " + ChatColor.WHITE + claim.getId() + 
                    ChatColor.YELLOW + " is pending.");
        }

        saveRegions(world);
    }

    @Command(aliases = {"confirm", "c"},
            usage = "",
            desc = "Confirm your claim request",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.confirm")
    public void confirm(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        // Get pending request(s) by the player
        ArrayList<ProtectedRegion> regionList = SACUtil.getPendingRegions(rgMgr, player);
        if (regionList.isEmpty()) {
            throw new CommandException(ChatColor.YELLOW + "There is no pending request for you to confirm.");
        }
        ProtectedRegion claim = null;
        ProtectedRegion thisClaim = SACUtil.getClaimStandingIn(player, plugin);
        if (regionList.size() == 1) {
            claim = regionList.get(0);
        } else {
            for (ProtectedRegion region : regionList) {
                if (thisClaim == region) {
                    claim = region;
                    break;
                }
                region.setFlag(SACFlags.REQUEST_STATUS,null);
                region.setFlag(SACFlags.REQUEST_NAME,null);
                region.setFlag(SACFlags.PENDING,null);
                region.setFlag(SACFlags.ENTRY_DEFAULT,null);
                region.setFlag(DefaultFlag.ENTRY,null);
            }
        }
        if (claim == null) {
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "There was an error with your request, please stake again!");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 0) {
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Check if this would be over the selfClaimMax
        regionList = SACUtil.getOwnedRegions(rgMgr, player);
        boolean selfClaimActive = false;

        if (wcfg.claimLimitsAreArea) {
            double area = getArea(claim);
            for (ProtectedRegion region : regionList) {
                area = area + getArea(region);
            }
            if (area <= wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
        } else {
            if (regionList.size() < wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
        }

        // Do we use self claim?
        if (selfClaimActive) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            claim.setFlag(SACFlags.PENDING,null);
            claim.setFlag(SACFlags.REQUEST_NAME,null);
            claim.setFlag(SACFlags.REQUEST_STATUS,null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + claim.getId() + "!");

            saveRegions(world);
        } else {
            throw new CommandException(ChatColor.YELLOW + "You can't confirm your own pending requests!");
        }
    }

    @Command(aliases = {"unstake", "u"},
            usage = "",
            desc = "Cancel your stake request.",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.unstake")
    public void unstake(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        // Remove all pending requests for this player
        ArrayList<ProtectedRegion> regionList = SACUtil.getPendingRegions(rgMgr, player);
        for (ProtectedRegion region : regionList) {
            region.setFlag(SACFlags.REQUEST_STATUS,null);
            region.setFlag(SACFlags.REQUEST_NAME,null);
            region.setFlag(SACFlags.PENDING,null);
            region.setFlag(SACFlags.ENTRY_DEFAULT,null);
            region.setFlag(DefaultFlag.ENTRY,null);
        }

        if (regionList.size() == 0) {
            sender.sendMessage(ChatColor.YELLOW + "You had no pending stake request.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Your stake request has been canceled.");
            saveRegions(world);
        }
    }

    @Command(aliases = {"add", "a"},
            usage = "<members...>",
            desc = "Add a member to a claim",
            min = 1)
    @CommandPermissions("stakeaclaim.claim.add.*")
    public void add(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "add", claim);

        RegionDBUtil.addToDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        sender.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.GREEN + args.getJoinedStrings(0) + 
                ChatColor.YELLOW + " to claim: " + ChatColor.WHITE + regionID + ".");

        saveRegions(world);
    }

    @Command(aliases = {"remove", "r"},
            usage = "<members...>",
            flags = "a:",
            desc = "Remove a member from a claim",
            min = 1)
    @CommandPermissions("stakeaclaim.claim.remove.*")
    public void remove(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "remove", claim);

        if (args.hasFlag('a')) {
            claim.getMembers().getPlayers().clear();
            claim.getMembers().getGroups().clear();
            sender.sendMessage(ChatColor.YELLOW + "Removed all members from claim: " + ChatColor.WHITE + regionID + ".");
        } else {
            if (args.argsLength() < 1) {
                throw new CommandException("List some names to remove, or use -a to remove all.");
            }
            RegionDBUtil.removeFromDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

            sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.GREEN + args.getJoinedStrings(0) + 
                    ChatColor.YELLOW + " from claim: " + ChatColor.WHITE + regionID + ".");
        }

        saveRegions(world);
    }

    @Command(aliases = {"private", "v"},
            usage = "",
            desc = "Set a claim to private",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.private.*")
    public void setprivate(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "private", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }
        claim.setFlag(DefaultFlag.ENTRY, State.DENY);

        sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.RED + "private.");

        saveRegions(world);
    }

    @Command(aliases = {"open", "o"},
            usage = "",
            desc = "Set a claim to open",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.open.*")
    public void open(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "open", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }
        claim.setFlag(DefaultFlag.ENTRY, null);

        sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.GRAY + "open.");

        saveRegions(world);
    }

    @Command(aliases = {"warp", "w"},
            usage = "<player> [list entry #]",
            desc = "Accept a pending request",
            min = 0, max = 2)
    @CommandPermissions("stakeaclaim.claim.warp.*")
    public void warp(CommandContext args, CommandSender sender) throws CommandException {

        final Player travelPlayer = plugin.checkPlayer(sender);
        final World world = travelPlayer.getWorld();
        final PlayerState state = plugin.getPlayerStateManager().getState(travelPlayer);

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        if (args.argsLength() == 0) {
            if (state.lastWarp != null) {
                SACUtil.warpTo(state.lastWarp, state, travelPlayer, false);
            }
            sender.sendMessage(ChatColor.RED + "Too few arguments.");
            throw new CommandException(ChatColor.RED + "/claim " + args.getCommand() + " <player> [list entry #]");
        }

        final String targetPlayer = args.getString(0);
        ArrayList<ProtectedRegion> tempList = SACUtil.getOwnedRegions(rgMgr, targetPlayer);
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>(tempList);
        LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();

        for (ProtectedRegion region : tempList) {
            if (region.getFlag(DefaultFlag.ENTRY) != null && region.getFlag(DefaultFlag.ENTRY) == State.DENY) {
                regionList.remove(region);
            } else if (!hasPerm(travelPlayer, "warp", region)) {
                regionList.remove(region);
            }
        }
        if (regionList.isEmpty()) {
            if (plugin.getServer().getOfflinePlayer(targetPlayer).hasPlayedBefore() == false) {
                throw new CommandException(ChatColor.GREEN + targetPlayer + ChatColor.YELLOW + " has not played on this server.");
            }
            throw new CommandException(ChatColor.GREEN + targetPlayer + ChatColor.YELLOW + " has no claims for you to warp to.");
        } else if (regionList.size() == 1) {
            SACUtil.warpTo(regionList.get(0), state, travelPlayer, false);
        } else {
            int index = 0;
            for (ProtectedRegion region : regionList) {
                regions.put(index, region.getId());
                index++;
            }
        }
        int listNumber = 0;
        if (args.argsLength() == 2) {
            listNumber = args.getInteger(1) - 1;
            if (!regions.containsKey(listNumber)) {
                throw new CommandException(ChatColor.YELLOW + "That is not a valid list entry number.");
            }
            SACUtil.warpTo(rgMgr.getRegion(regions.get(listNumber)), state, travelPlayer, false);
        }

        // Display the list
        sender.sendMessage(ChatColor.GREEN + targetPlayer + ChatColor.RED + "'s claim warp list:");

        ProtectedRegion region;
        for (int i = 0; i < regions.size(); i++) {
            region = rgMgr.getRegion(regions.get(i));
            sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + region.getId());
        }
    }

    // Other methods
    public double getArea(ProtectedRegion region) throws CommandException {
        final BlockVector min = region.getMinimumPoint();
        final BlockVector max = region.getMaximumPoint();

        return (max.getBlockZ() - min.getBlockZ() + 1) * (max.getBlockX() - min.getBlockX() + 1);
    }

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

    public boolean hasPerm(Player player, String command, ProtectedRegion claim) {

        final String playerName = player.getName();
        final String id = claim.getId();

        if (claim.isOwner(playerName)) {
            return plugin.hasPermission(player, "stakeaclaim.claim." + command + ".own." + id.toLowerCase());
        } else if (claim.isMember(playerName)) {
            return plugin.hasPermission(player, "stakeaclaim.claim." + command + ".member." + id.toLowerCase());
        } else {
            return plugin.hasPermission(player, "stakeaclaim.claim." + command + "." + id.toLowerCase());
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
