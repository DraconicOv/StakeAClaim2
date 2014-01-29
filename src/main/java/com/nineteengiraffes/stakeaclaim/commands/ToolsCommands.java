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

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.SACFlags;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ToolsCommands {
    private final StakeAClaimPlugin plugin;

    public ToolsCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"pending", "p"},
            usage = "[page]",
            desc = "Populates a list of pending requests",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.pending")
    public void pending(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        ArrayList<ProtectedRegion> regionList = SACUtil.getPendingRegions(rgMgr);
        LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();
        PlayerState state = plugin.getPlayerStateManager().getState(player);

        int index = 0;
        for (ProtectedRegion region : regionList) {
            regions.put(index, region.getId());
            index++;
        }

        final int totalSize = regions.size();
        
        if (totalSize < 1) {
            state.regionList = null;
            throw new CommandException(ChatColor.YELLOW + "There are no pending requests.");
        }
        state.regionList = regions;

        // Display the list
        int page = 0;
        if (args.argsLength() > 0) {
            page = Math.max(0, args.getInteger(0) - 1);
        }
        final int pageSize = 10;
        final int pages = (int) Math.ceil(totalSize / (float) pageSize);
        if (page + 1 > pages) {
            page = pages - 1;
        }

        sender.sendMessage(ChatColor.RED
                + "Pending request list: (page "
                + (page + 1) + " of " + pages + ")");

        if (page < pages) {
            ProtectedRegion region;
            for (int i = page * pageSize; i < page * pageSize + pageSize; i++) {
                if (i >= totalSize) {
                    break;
                }
                region = rgMgr.getRegion(regions.get(i));
                sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + region.getId() +
                        ", " + ChatColor.GREEN + region.getFlag(SACFlags.REQUEST_NAME));
            }
        }
    }

    @Command(aliases = {"accept", "a"},
            usage = "<list entry #>",
            desc = "Accept a pending request",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.accept")
    public void accept(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        if (claim.getFlag(SACFlags.PENDING) != null && claim.getFlag(SACFlags.PENDING) == true && claim.getFlag(SACFlags.REQUEST_NAME) != null) {
        } else {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            claim.setFlag(SACFlags.REQUEST_NAME,null);
            claim.setFlag(SACFlags.REQUEST_STATUS,null);
            claim.setFlag(SACFlags.PENDING,null);
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Accept the request
        claim.getOwners().addPlayer(claim.getFlag(SACFlags.REQUEST_NAME));
        claim.setFlag(SACFlags.PENDING,null);
        claim.setFlag(SACFlags.REQUEST_STATUS,"accepted");

        sender.sendMessage(ChatColor.YELLOW + "You have accepted " + ChatColor.GREEN + claim.getFlag(SACFlags.REQUEST_NAME) +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + regionID + "!");

        saveRegions(world);
    }

    @Command(aliases = {"deny", "d"},
            usage = "<list entry #>",
            desc = "Deny a pending request",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.deny")
    public void deny(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        if (claim.getFlag(SACFlags.PENDING) != null && claim.getFlag(SACFlags.PENDING) == true && claim.getFlag(SACFlags.REQUEST_NAME) != null) {
        } else {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            claim.setFlag(SACFlags.REQUEST_NAME,null);
            claim.setFlag(SACFlags.REQUEST_STATUS,null);
            claim.setFlag(SACFlags.PENDING,null);
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Deny the request
        claim.setFlag(SACFlags.PENDING,null);
        claim.setFlag(SACFlags.REQUEST_STATUS,"denied");

        sender.sendMessage(ChatColor.YELLOW + "You have denied " + ChatColor.GREEN + claim.getFlag(SACFlags.REQUEST_NAME) +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + regionID + "!");

        saveRegions(world);
    }

    @Command(aliases = {"cancel", "c"},
            usage = "<list entry #>",
            desc = "Cancel a pending request",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.cancel")
    public void cancel(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        if (claim.getFlag(SACFlags.PENDING) != null && claim.getFlag(SACFlags.PENDING) == true && claim.getFlag(SACFlags.REQUEST_NAME) != null) {
        } else {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            claim.setFlag(SACFlags.REQUEST_NAME,null);
            claim.setFlag(SACFlags.REQUEST_STATUS,null);
            claim.setFlag(SACFlags.PENDING,null);
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Cancel the request
        claim.setFlag(SACFlags.PENDING,null);
        claim.setFlag(SACFlags.REQUEST_STATUS,"canceled");

        sender.sendMessage(ChatColor.YELLOW + "You have canceled " + ChatColor.GREEN + claim.getFlag(SACFlags.REQUEST_NAME) +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + regionID + "!");

        saveRegions(world);
    }

    @Command(aliases = {"reclaim", "r"},
            usage = "<list entry #>",
            desc = "Reclaim a claim",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.reclaim")
    public void reclaim(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode < 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        // Reclaim the claim
        final String owner = claim.getOwners().toUserFriendlyString();
        SACUtil.reclaim(claim, wcfg.useReclaimed);

        sender.sendMessage(ChatColor.YELLOW + "You have reclaimed " + ChatColor.WHITE + regionID +
                ChatColor.YELLOW + " from " + ChatColor.GREEN + owner + ChatColor.YELLOW + "!");

        saveRegions(world);
    }

    @Command(aliases = {"proxy", "x"},
            usage = "",
            desc = "Stake their claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.tools.proxy")
    public void proxy(CommandContext args, CommandSender sender) throws CommandException {

        final Player activePlayer = plugin.checkPlayer(sender);
        final World world = activePlayer.getWorld();
        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final PlayerState state = plugin.getPlayerStateManager().getState(activePlayer);
        if (state.unsubmittedRequest == null) {
            throw new CommandException(ChatColor.YELLOW + "No player to proxy for.");
        }
        final String[] unsubmittedRequest = state.unsubmittedRequest;
        final String regionID = unsubmittedRequest[1];
        final String passivePlayer = unsubmittedRequest[0];
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        
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
                } else if (!claim.getFlag(SACFlags.REQUEST_NAME).equals(passivePlayer)) {
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

            if (claim.getOwners().equals(passivePlayer)) {
                throw new CommandException(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + " already owns this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        } else {
                throw new CommandException(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                          claim.getId() + ChatColor.RED + " already has multiple owners!");
        }

        // Check if this would be over the proxyClaimMax
         ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedRegions(rgMgr, passivePlayer);

        if (wcfg.claimLimitsAreArea) {
            double area = getArea(claim);
            for (ProtectedRegion region : regionList) {
                area = area + getArea(region);
            }
            if (area > wcfg.proxyClaimMax && wcfg.proxyClaimMax != -1) {
                throw new CommandException(ChatColor.YELLOW + "This claim would put " + ChatColor.GREEN + passivePlayer + 
                        ChatColor.YELLOW + " over the maximum claim area.");
            }
        } else {
            if (regionList.size() >= wcfg.proxyClaimMax && wcfg.proxyClaimMax != -1) {
                throw new CommandException(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + 
                        " has already claimed the maximum number of claims.");
            }
        }

        // Remove pending flag on any/all other claims
        regionList = SACUtil.getPendingRegions(rgMgr, passivePlayer);
        for (ProtectedRegion region : regionList) {
            region.setFlag(SACFlags.REQUEST_STATUS,null);
            region.setFlag(SACFlags.REQUEST_NAME,null);
            region.setFlag(SACFlags.PENDING,null);
            region.setFlag(SACFlags.ENTRY_DEFAULT,null);
            region.setFlag(DefaultFlag.ENTRY,null);
        }

        // Submit request
        claim.setFlag(SACFlags.PENDING,true);
        claim.setFlag(SACFlags.REQUEST_NAME,passivePlayer);
        claim.setFlag(SACFlags.REQUEST_STATUS,"Pending");

        if (claim.getFlag(SACFlags.RECLAIMED) != null && claim.getFlag(SACFlags.RECLAIMED) == true) {
            sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + regionID + 
                    ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
        }
        sender.sendMessage(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + "'s stake request for " + 
                ChatColor.WHITE + regionID + ChatColor.YELLOW + " is pending.");

        saveRegions(world);
    }

    @Command(aliases = {"entry", "e"},
            usage = "<list entry #>",
            desc = "Remove claim entry default",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.entry")
    public void entry(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        claim.setFlag(SACFlags.ENTRY_DEFAULT, null);

        sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " 's default entry setting.");

        saveRegions(world);
    }
    
    // Other methods
    public double getArea(ProtectedRegion region) throws CommandException {
        final BlockVector min = region.getMinimumPoint();
        final BlockVector max = region.getMaximumPoint();

        return (max.getBlockZ() - min.getBlockZ() + 1) * (max.getBlockX() - min.getBlockX() + 1);
    }

    public String getRegionIDFromList(CommandContext args, Player player) throws CommandException {
        String regionID = null;
        LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();
        PlayerState state = plugin.getPlayerStateManager().getState(player);

        if (state.regionList != null) {
            regions = state.regionList;
            int listNumber = 0;
            if (args.argsLength() == 1) {
                listNumber = args.getInteger(0) - 1;
                if (!regions.containsKey(listNumber)) {
                    throw new CommandException(ChatColor.YELLOW + "That is not a valid list entry number.");
                }
            } else {
                throw new CommandException(ChatColor.YELLOW + "Please include the list entry number.");
            }
            regionID = regions.get(listNumber);
        } else {
            throw new CommandException(ChatColor.YELLOW + "The request list is empty.");
        }

        return regionID;
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
