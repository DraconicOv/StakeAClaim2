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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.bukkit.ConfigurationManager;
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
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
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
        final ProtectedRegion claim = getClaimStandingIn(player);
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

        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

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
                if (!pendingRequest.getPlayerName().equals(player.getName().toLowerCase())) {
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
                if (acceptedRequest.getPlayerName().equals(player.getName().toLowerCase())) {
                    throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
                }
                throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                        ChatColor.GREEN + acceptedRequest.getPlayerName() + ".");
            }
        } else {
                throw new CommandException(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                        ownedCode + "-" + regionID + ChatColor.RED + ", please notify admin!");
        }

        // Check if this would be over the claimMax
        final ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedRegions(rgMgr, player);
        boolean selfClaimActive = false;
        boolean claimWasReclaimed = false;

        if (wcfg.claimLimitsAreArea) {
            double area = getArea(claim);
            for (ProtectedRegion region : regionList) {
                area = area + getArea(region);
            }
            if (area <= wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (area > wcfg.claimMax && wcfg.claimMax != -1) {
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim area.");
            }
        } else {
            if (regionList.size() < wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (regionList.size() >= wcfg.claimMax && wcfg.claimMax != -1) {
                throw new CommandException(ChatColor.YELLOW + "You have already claimed the maximum number of claims.");
            }
        }

        // Set all old pending requests for this player to unstaked
        requestList = rqMgr.getPlayerStatusRequests(player, Status.PENDING);
        for (StakeRequest request : requestList) {
            request.setStatus(Status.UNSTAKED);
        }

        // Submit request
        final StakeRequest newRequest = new StakeRequest(regionID, player);
        newRequest.setStatus(Status.PENDING);
        rqMgr.addRequest(newRequest);

        // Check if we need to show a reclaimed notification
        requestList = rqMgr.getRegionStatusRequests(regionID, Status.RECLAIMED);
        if (requestList.size() > 0 && wcfg.showReclaimOnStake) {
            claimWasReclaimed = true;
        }

        // Do we use self claim?
        if (selfClaimActive && !wcfg.twoStepSelfClaim && !claimWasReclaimed) {
            claim.getOwners().addPlayer(newRequest.getPlayerName());
            newRequest.setStatus(Status.ACCEPTED);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + newRequest.getRegionID() + "!");
        } else {
            if (claimWasReclaimed) {
                sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + regionID + 
                        ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Your stake request for " + ChatColor.WHITE + regionID + 
                    ChatColor.YELLOW + " is pending.");
        }

        saveRequests(world);
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

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ArrayList<StakeRequest> requestList;

        // Get the request the player will confirm
        final StakeRequest requestToConfirm = SACUtil.getPlayerPendingRequest(rqMgr, player);
        if (requestToConfirm == null) {
            throw new CommandException(ChatColor.YELLOW + "There is no pending request for you to confirm.");
        }

        final ProtectedRegion claim = rgMgr.getRegion(requestToConfirm.getRegionID());
        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode != 0) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Check if this would be over the selfClaimMax
        final ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedRegions(rgMgr, player);
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
            claim.getOwners().addPlayer(requestToConfirm.getPlayerName());
            requestToConfirm.setStatus(Status.ACCEPTED);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + requestToConfirm.getRegionID() + "!");

            saveRegions(world);
            saveRequests(world);
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

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ArrayList<StakeRequest> requestList;

        // Set all pending requests for this player to unstaked
        requestList = rqMgr.getPlayerStatusRequests(player, Status.PENDING);
        for (StakeRequest request : requestList) {
            request.setStatus(Status.UNSTAKED);
        }

        if (requestList.size() == 0) {
            sender.sendMessage(ChatColor.YELLOW + "You had no pending stake request.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Your stake request has been canceled.");
            saveRequests(world);
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

        final ProtectedRegion claim = getClaimStandingIn(player);
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

        final ProtectedRegion claim = getClaimStandingIn(player);
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

        final ProtectedRegion claim = getClaimStandingIn(player);
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

        final ProtectedRegion claim = getClaimStandingIn(player);
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

    // Other methods
    public ProtectedRegion getClaimStandingIn(Player player) throws CommandException {

        final World world = player.getWorld();
        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }
        final Location loc = player.getLocation();
        final Vector pt = new Vector(loc.getX(), loc.getY(), loc.getZ());
        final ApplicableRegionSet rgSet = rgMgr.getApplicableRegions(pt);
        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;
        ProtectedRegion claim = null;

        for (ProtectedRegion region : rgSet) {
            regexMat = regexPat.matcher(region.getId());
            if (regexMat.find()) {
                if (claim == null) {
                    claim = region;
                } else {
                    claim = null;
                    break;
                }
            }
        }

        if (claim == null) {
            throw new CommandException("You are not in a single valid claim!");
        }
        return claim;
    }

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
