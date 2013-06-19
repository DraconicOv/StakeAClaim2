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

package org.stakeaclaim.bukkit.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
//import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
//import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.databases.RegionDBUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
//import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.stakes.ApplicableRequestSet;
import org.stakeaclaim.stakes.databases.StakeDatabaseException;
import org.stakeaclaim.stakes.RequestManager;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.StakeRequest.Access;
import org.stakeaclaim.stakes.StakeRequest.Status;

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
    public void stake(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

        checkPerm(player, "stake", claim);

        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ApplicableRequestSet rqSet;
        final RegionManager rgMgr = WGBukkit.getRegionManager(world); // need to make sure it is not null
        ProtectedRegion region;

        // Check if there are any pending requests for this claim
        rqSet = rqMgr.getApplicableRequests(regionID, Status.PENDING);
        final StakeRequest pendingRequest = rqSet.getPendingRequest();
        
        // Throw an exception for a pending claim by another player
        if (pendingRequest != null) {
            if (!pendingRequest.getPlayerName().equals(player.getName().toLowerCase())) {
                throw new CommandException(ChatColor.YELLOW + "This claim is already requested by " +
                        ChatColor.GREEN + pendingRequest.getPlayerName() + ".");
            }
        }

        // Check if there are any accepted requests for this claim
        rqSet = rqMgr.getApplicableRequests(regionID, Status.ACCEPTED);
        final StakeRequest acceptedRequest = rqSet.getAcceptedRequest(rgMgr);

        // Throw an exception for a pre-owned claim
        if (acceptedRequest != null) {
            if (acceptedRequest.getPlayerName().equals(player.getName().toLowerCase())) {
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + acceptedRequest.getPlayerName() + ".");
        }

        // Check if this would be over the claimMax
        rqSet = rqMgr.getApplicableRequests(player, Status.ACCEPTED);
        boolean selfClaimActive = false;
        
        boolean twoStepSelfClaim = false; // delete after testing, this is in place of config value
        boolean claimLimitIsArea = true; // delete after testing, this is in place of config value
        double selfClaimMax = 16; // delete after testing, this is in place of config value
        double claimMax = 36; // delete after testing, this is in place of config value
        
        if (claimLimitIsArea) {
            double area = getArea(claim);
            for (StakeRequest request : rqSet) {
                region = rgMgr.getRegion(request.getRegionID());
                area = area + getArea(region);
            }
            if (area <= selfClaimMax || selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (area > claimMax && claimMax != -1) {
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim area.");
            }
        } else {
            if (rqSet.size() < selfClaimMax || selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (rqSet.size() + 1 > claimMax && claimMax != -1) {
                throw new CommandException(ChatColor.YELLOW + "You have already claimed the maximum number of claims.");
            }
        }
        
        // Set all old pending requests for this player to unstaked
        rqSet = rqMgr.getApplicableRequests(player, Status.PENDING);
        for (StakeRequest request : rqSet) {
            request.setStatus(Status.UNSTAKED);
        }

        // Submit request
        final StakeRequest newRequest = new StakeRequest(regionID, player);
        newRequest.setStatus(Status.PENDING);
        rqMgr.addRequest(newRequest);

        // Do we use self claim?
        if (selfClaimActive && !twoStepSelfClaim) {
            final String[] owners = new String[1];
            region = rgMgr.getRegion(newRequest.getRegionID());
            owners[0] = newRequest.getPlayerName();

            RegionDBUtil.addToDomain(region.getOwners(), owners, 0);
            newRequest.setStatus(Status.ACCEPTED);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + newRequest.getRegionID() + "!");
        } else {
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
        
        final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
        ApplicableRequestSet rqSet;
        final RegionManager rgMgr = WGBukkit.getRegionManager(world); // need to make sure it is not null
        ProtectedRegion region;
        StakeRequest requestToConfirm = null;
        
        boolean twoStepSelfClaim = false; // delete after testing, this is in place of config value
        boolean claimLimitIsArea = true; // delete after testing, this is in place of config value
        double selfClaimMax = 16; // delete after testing, this is in place of config value

        // Check if two step self claim is active
        if (!twoStepSelfClaim) {
            throw new CommandException(ChatColor.YELLOW + " This command is disabled! You can't confirm your own pending requests.");
        }

        // Get the request the player will confirm
        rqSet = rqMgr.getApplicableRequests(player, Status.PENDING);
        if (rqSet.size() > 0) {
            for (StakeRequest request : rqSet) {
                requestToConfirm = request;
                break;  // this will get the first in the set and ignore any others, '/claim stake' deals with duplicates
            }
        } else {
            throw new CommandException(ChatColor.YELLOW + "There are no pending request for you to confirm.");
        }
        final ProtectedRegion claim = rgMgr.getRegion(requestToConfirm.getRegionID());

        checkPerm(player, "confirm", claim);

        // Check if this would be over the selfClaimMax
        rqSet = rqMgr.getApplicableRequests(player, Status.ACCEPTED);
        boolean selfClaimActive = false;
        if (claimLimitIsArea) {
            double area = getArea(claim);
            for (StakeRequest request : rqSet) {
                region = rgMgr.getRegion(request.getRegionID());
                area = area + getArea(region);
            }
            if (area <= selfClaimMax || selfClaimMax == -1) {
                selfClaimActive = true;
            }
        } else {
            if (rqSet.size() < selfClaimMax || selfClaimMax == -1) {
                selfClaimActive = true;
            }
        }

        // Do we use self claim?
        if (selfClaimActive) {
            final String[] owners = new String[1];
            owners[0] = requestToConfirm.getPlayerName();

            RegionDBUtil.addToDomain(claim.getOwners(), owners, 0);
            requestToConfirm.setStatus(Status.ACCEPTED);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + requestToConfirm.getRegionID() + "!");
        } else {
            throw new CommandException(ChatColor.YELLOW + "Limit reached! You can't confirm your own pending requests.");
        }

        saveRequests(world);
        saveRegions(world);
    }

    @Command(aliases = {"add", "addmember", "addmembers", "ad", "a"},
            usage = "<members...>",
            desc = "Add a member to a claim",
            min = 1)
    public void add(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

        checkPerm(player, "add", claim);

        RegionDBUtil.addToDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        sender.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.GREEN + args.getJoinedStrings(0) + ChatColor.YELLOW + " to claim: " + ChatColor.WHITE + regionID + ".");

        saveRegions(world);
    }

    @Command(aliases = {"remove", "removemember", "removemembers", "r"},
            usage = "<members...>",
//            flags = "a:",
            desc = "Remove a member from a claim",
//            min = 0)
            min = 1)
    public void remove(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

        checkPerm(player, "remove", claim);
        
//        if (args.hasFlag('a')) {
//            claim.getMembers().removeAll();
//        } else {
//            if (args.argsLength() < 1) {
//                throw new CommandException("List some names to remove, or use -a to remove all.");
//            }
            RegionDBUtil.removeFromDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);
//        }

        sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.GREEN + args.getJoinedStrings(0) + ChatColor.YELLOW + " from claim: " + ChatColor.WHITE + regionID + ".");

        saveRegions(world);
    }

    @Command(aliases = {"private", "p"},
            usage = "",
            desc = "Set a claim to private",
            min = 0, max = 0)
    public void setprivate(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

        checkPerm(player, "private", claim);

        claim.setFlag(DefaultFlag.ENTRY, State.DENY);

        sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.RED + "private.");

        saveRegions(world);
    }

    @Command(aliases = {"open", "o", "public"},
            usage = "",
            desc = "Set a claim to open",
            min = 0, max = 0)
    public void open(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        final ProtectedRegion claim = getClaimStandingIn(player);
        final String regionID = claim.getId();

        checkPerm(player, "open", claim);

        claim.setFlag(DefaultFlag.ENTRY, null);

        sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.GRAY + "open.");

        saveRegions(world);
    }

    // Other methods
    public ProtectedRegion getClaimStandingIn(Player player) throws CommandException {

        final World world = player.getWorld();
        final RegionManager rgMgr = WGBukkit.getRegionManager(world); // need to make sure it is not null
        final Location loc = player.getLocation();
        final Vector pt = new Vector(loc.getX(), loc.getY(), loc.getZ());
        final ApplicableRegionSet rgSet = rgMgr.getApplicableRegions(pt);

        ProtectedRegion claim = null;
        final Pattern regxPat = Pattern.compile("^[ns]\\d\\d?[ew]\\d\\d?$"); // matches n1w23 or s51e2 etc.
        Matcher regxMat;

        for (ProtectedRegion region : rgSet) {
            regxMat = regxPat.matcher(region.getId());
            if (regxMat.find()) {
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

        final RegionManager rgMgr = WGBukkit.getRegionManager(world); // need to make sure it is not null

        try {
            rgMgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regionss: " + e.getMessage());
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
    
    {//    /**
//     * Gets the world from the given flag, or falling back to the the current player
//     * if the sender is a player, otherwise reporting an error.
//     * 
//     * @param args the arguments
//     * @param sender the sender
//     * @param flag the flag (such as 'w')
//     * @return a world
//     * @throws CommandException on error
//     */
//    private World getWorld(CommandContext args, CommandSender sender, char flag)
//            throws CommandException {
//        if (args.hasFlag(flag)) {
//            return plugin.matchWorld(sender, args.getFlag(flag));
//        } else {
//            if (sender instanceof Player) {
//                return plugin.checkPlayer(sender).getWorld();
//            } else {
//                throw new CommandException("Please specify " +
//                        "the world with -" + flag + " world_name.");
//            }
//        }
//    }
    }
}
