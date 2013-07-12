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

package com.nineteengiraffes.stakeaclaim.bukkit.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.bukkit.ConfigurationManager;
import com.nineteengiraffes.stakeaclaim.bukkit.FlagStateManager.PlayerFlagState;
import com.nineteengiraffes.stakeaclaim.bukkit.SACUtil;
import com.nineteengiraffes.stakeaclaim.bukkit.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.bukkit.WorldConfiguration;
import com.nineteengiraffes.stakeaclaim.stakes.RequestManager;
import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest;
import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest.Status;
import com.nineteengiraffes.stakeaclaim.stakes.databases.StakeDatabaseException;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
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

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ArrayList<StakeRequest> requestList;
        requestList = rqMgr.getStatusRequests(Status.PENDING);
        LinkedHashMap<Integer, Long> requests = new LinkedHashMap<Integer, Long>();
        PlayerFlagState state = plugin.getFlagStateManager().getState(player);

        int index = 0;
        for (StakeRequest request : requestList) {
            requests.put(index, request.getRequestID());
            index++;
        }

        final int totalSize = requests.size();
        
        if (totalSize < 1) {
            state.requestList = null;
            throw new CommandException(ChatColor.YELLOW + "There are no pending requests.");
        }
        state.requestList = requests;

        // Display the list
        int page = 0;
        if (args.argsLength() > 0) {
            page = Math.max(0, args.getInteger(0) - 1);
        }
        final int pageSize = 10;
        final int pages = (int) Math.ceil(totalSize / (float) pageSize);

        sender.sendMessage(ChatColor.RED
                + "Pending request list: (page "
                + (page + 1) + " of " + pages + ")");

        if (page < pages) {
            StakeRequest request;
            for (int i = page * pageSize; i < page * pageSize + pageSize; i++) {
                if (i >= totalSize) {
                    break;
                }
                request = rqMgr.getRequest(requests.get(i));
                sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + request.getRegionID() +
                        ", " + ChatColor.GREEN + request.getPlayerName());
            }
        }
    }

    @Command(aliases = {"claim", "l"},
            usage = "",
            desc = "Populates a list with one claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.tools.claim")
    public void claim(CommandContext args, CommandSender sender) throws CommandException {

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
        final String regionID = claim.getId();
        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        final PlayerFlagState state = plugin.getFlagStateManager().getState(player);

        LinkedHashMap<Integer, Long> requests = new LinkedHashMap<Integer, Long>();
        state.requestList = null;

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode == 0) {
                final StakeRequest pendingRequest = SACUtil.getRegionPendingRequest(rqMgr, regionID);
                if (pendingRequest != null) {
                    requests.put(0, pendingRequest.getRequestID());
                    state.requestList = requests;
                    sender.sendMessage(ChatColor.RED + "Pending request: ");
                    sender.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                            ", " + ChatColor.GREEN + pendingRequest.getPlayerName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Open claim: ");
                    sender.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                            ", " + ChatColor.GRAY + "Unclaimed");
                }
        } else if (ownedCode == 1) {
                final StakeRequest acceptedRequest = SACUtil.fixRegionsRequests(rqMgr, claim, wcfg.useReclaimed);
                if (acceptedRequest == null) {
                    saveRequests(world);
                    throw new CommandException(ChatColor.RED + "Error in " + ChatColor.WHITE + regionID + 
                            ChatColor.RED + ", please notify admin!");
                }
                requests.put(0, acceptedRequest.getRequestID());
                state.requestList = requests;
                sender.sendMessage(ChatColor.RED + "Owned claim: ");
                sender.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                        ", " + ChatColor.GREEN + acceptedRequest.getPlayerName());
        }

        saveRequests(world);
    }

    @Command(aliases = {"accept", "a"},
            usage = "<list entry #>",
            desc = "Accept a pending request",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.accept")
    public void accept(CommandContext args, CommandSender sender) throws CommandException {

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
        StakeRequest requestToAccept = getRequestListItem(args, sender);

        if (requestToAccept.getStatus() != Status.PENDING) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        // Get the region requested
        final ProtectedRegion claim = rgMgr.getRegion(requestToAccept.getRegionID());
        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 0) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Accept the request
        claim.getOwners().addPlayer(requestToAccept.getPlayerName());
        requestToAccept.setStatus(Status.ACCEPTED);

        sender.sendMessage(ChatColor.YELLOW + "You have accepted " + ChatColor.GREEN + requestToAccept.getPlayerName() +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + requestToAccept.getRegionID() + "!");

        saveRequests(world);
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

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        StakeRequest requestToDeny = getRequestListItem(args, sender);

        if (requestToDeny.getStatus() != Status.PENDING) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        // Deny the request
        requestToDeny.setStatus(Status.DENIED);

        sender.sendMessage(ChatColor.YELLOW + "You have denied " + ChatColor.GREEN + requestToDeny.getPlayerName() +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + requestToDeny.getRegionID() + "!");

        saveRequests(world);
    }

    @Command(aliases = {"cancel", "c"},
            usage = "<list entry #>",
            desc = "Cancel a pending request",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.cancel")
    public void cancel(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        StakeRequest requestToCancel = getRequestListItem(args, sender);

        if (requestToCancel.getStatus() != Status.PENDING) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this request is not pending.");
        }

        // Cancel the request
        requestToCancel.setStatus(Status.UNSTAKED);

        sender.sendMessage(ChatColor.YELLOW + "You have canceled " + ChatColor.GREEN + requestToCancel.getPlayerName() +
                ChatColor.YELLOW + "'s request for " + ChatColor.WHITE + requestToCancel.getRegionID() + "!");

        saveRequests(world);
    }

    @Command(aliases = {"reclaim", "r"},
            usage = "<list entry #>",
            desc = "Reclaim a claim",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.reclaim")
    public void reclaim(CommandContext args, CommandSender sender) throws CommandException {

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
        StakeRequest requestToReclaim = getRequestListItem(args, sender);

        final ProtectedRegion claim = rgMgr.getRegion(requestToReclaim.getRegionID());
        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        // Reclaim the request
        SACUtil.reclaim(requestToReclaim, claim, wcfg.useReclaimed);

        sender.sendMessage(ChatColor.YELLOW + "You have reclaimed " + ChatColor.WHITE + requestToReclaim.getRegionID() +
                ChatColor.YELLOW + " from " + ChatColor.GREEN + requestToReclaim.getPlayerName() + ChatColor.YELLOW + "!");

        saveRequests(world);
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
        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final PlayerFlagState state = plugin.getFlagStateManager().getState(activePlayer);
        if (state.unsubmittedRequest == null) {
            throw new CommandException(ChatColor.YELLOW + "No player to proxy for.");
        }
        final StakeRequest unsubmittedRequest = state.unsubmittedRequest;
        final String regionID = unsubmittedRequest.getRegionID();
        final String passivePlayer = unsubmittedRequest.getPlayerName();
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ArrayList<StakeRequest> requestList;

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode == 0) {
            // Reclaim all accepted requests for this region
            requestList = rqMgr.getRegionStatusRequests(regionID, Status.ACCEPTED);
            for (StakeRequest request : requestList) {
                SACUtil.reclaim(request, claim, wcfg.useReclaimed);
            }
            final StakeRequest pendingRequest = SACUtil.getRegionPendingRequest(rqMgr, regionID);
            if (pendingRequest != null) {
                if (!pendingRequest.getPlayerName().equals(passivePlayer)) {
                    saveRequests(world);
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "This claim is already requested by " +
                            ChatColor.GREEN + pendingRequest.getPlayerName() + ".");
                }
            }
        } else if (ownedCode == 1) {
            // Set all pending requests for this region to unstaked
            requestList = rqMgr.getRegionStatusRequests(regionID, Status.PENDING);
            for (StakeRequest request : requestList) {
                request.setStatus(Status.UNSTAKED);
            }
            final StakeRequest acceptedRequest = SACUtil.fixRegionsRequests(rqMgr, claim, wcfg.useReclaimed);
            saveRequests(world);
            if (acceptedRequest == null) {
                throw new CommandException(ChatColor.RED + "Error in " + ChatColor.WHITE + regionID + 
                        ChatColor.RED + ", please notify admin!");
            } else {
                if (acceptedRequest.getPlayerName().equals(passivePlayer)) {
                throw new CommandException(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + " already owns this claim.");
                }
                throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                        ChatColor.GREEN + acceptedRequest.getPlayerName() + ".");
            }
        } else {
                throw new CommandException(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                        ownedCode + "-" + regionID + ChatColor.RED + ", please notify admin!");
        }

        // Check if this would be over the proxyClaimMax
        final ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedRegions(rgMgr, passivePlayer);
//        boolean claimWasReclaimed = false;

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

        // Set all old pending requests for passivePlayer to unstaked
        requestList = rqMgr.getPlayerStatusRequests(passivePlayer, Status.PENDING);
        for (StakeRequest request : requestList) {
            request.setStatus(Status.UNSTAKED);
        }

        // Submit request
        rqMgr.addRequest(unsubmittedRequest);

        requestList = rqMgr.getRegionStatusRequests(regionID, Status.RECLAIMED);
        if (requestList.size() > 0 && wcfg.showReclaimOnStake) {
            sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + regionID + 
                    ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
        }
        sender.sendMessage(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + "'s stake request for " + 
                ChatColor.WHITE + regionID + ChatColor.YELLOW + " is pending.");

        saveRequests(world);
        saveRegions(world);
    }

    @Command(aliases = {"load", "reload"},
            usage = "",
            desc = "Reload requests from file",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.tools.load")
    public void load(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        RequestManager mgr = plugin.getGlobalRequestManager().get(world);

        try {
            mgr.load();
            sender.sendMessage(ChatColor.YELLOW
                    + "Requests for '" + world.getName() + "' loaded.");
        } catch (StakeDatabaseException e) {
            throw new CommandException("Failed to read requests: "
                    + e.getMessage());
        }
    }

    @Command(aliases = {"save", "write"},
            usage = "",
            desc = "Re-save requests to file",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.tools.save")
    public void save(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        if (!wcfg.useRequests) {
            throw new CommandException(ChatColor.YELLOW + "Requests are disabled in this world.");
        }

        saveRequests(world);
        sender.sendMessage(ChatColor.YELLOW + "Requests for '" + world.getName() + "' saved.");
    }

    // Other methods
    public double getArea(ProtectedRegion region) throws CommandException {
        final BlockVector min = region.getMinimumPoint();
        final BlockVector max = region.getMaximumPoint();

        return (max.getBlockZ() - min.getBlockZ() + 1) * (max.getBlockX() - min.getBlockX() + 1);
    }

    public StakeRequest getRequestListItem(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        StakeRequest requestListItem = null;
        
        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        LinkedHashMap<Integer, Long> requests = new LinkedHashMap<Integer, Long>();
        PlayerFlagState state = plugin.getFlagStateManager().getState(player);

        if (state.requestList != null) {

            requests = state.requestList;
            int listNumber = 0;
            if (args.argsLength() == 1) {
                listNumber = args.getInteger(0) - 1;
                if (!requests.containsKey(listNumber)) {
                    throw new CommandException(ChatColor.YELLOW + "That is not a valid list entry number.");
                }
            } else {
                throw new CommandException(ChatColor.YELLOW + "Please include the list entry number.");
            }
            requestListItem = rqMgr.getRequest(requests.get(listNumber));
        } else {
            throw new CommandException(ChatColor.YELLOW + "The request list is empty.");
        }

        return requestListItem;
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

    public void saveRequests(World world) throws CommandException {

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);

        try {
            rqMgr.save();
        } catch (StakeDatabaseException e) {
            throw new CommandException("Failed to write requests: " + e.getMessage());
        }
    }
}
