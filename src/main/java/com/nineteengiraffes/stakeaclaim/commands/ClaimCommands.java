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
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.NestedCommand;
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
    @CommandPermissions({"stakeaclaim.claim.info", "stakeaclaim.claim.info.own.*", "stakeaclaim.claim.info.member.*", "stakeaclaim.claim.info.*"})
    public void info(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "info", claim);

        final World world = player.getWorld();
        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);

        if (claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + regionID + ChatColor.RED + " Private!");
        } else if (wcfg.useStakes) {
            final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
            Stake stake = sMgr.getStake(claim.getId());
            if (stake.getVIP()) {
                sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + regionID + ChatColor.AQUA +" " + wcfg.VIPs + " claim!");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + regionID);
            }
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

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim.getId());

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode <= 0) {
            claim.getMembers().getPlayers().clear();
            claim.getMembers().getGroups().clear();
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING) {
                if (stake.getStakeName() == null) {
                    stake.setStatus(null);
                    stake.setStakeName(null);
                    stake.setDefaultEntry(null);
                    claim.setFlag(DefaultFlag.ENTRY,null);
                } else if (!stake.getStakeName().equals(player.getName().toLowerCase())) {
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "This claim is already staked by " +
                            ChatColor.GREEN + stake.getStakeName() + ".");
                }
            }
        } else if (ownedCode == 1) {
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
                sMgr.save();
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim area.");
            }
        } else {
            if (regionList.size() < wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
            if (regionList.size() >= wcfg.claimMax && wcfg.claimMax != -1) {
                sMgr.save();
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "You have already claimed the maximum number of claims.");
            }
        }

        // VIP check
        if (stake.getVIP() && !plugin.hasPermission(player, "stakeaclaim.vip")) {
            sMgr.save();
            saveRegions(world);
            throw new CommandException(ChatColor.YELLOW + "Only " + wcfg.VIPs + " may stake this claim.");
        }

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, player);
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeName(null);
        }

        // Submit stake
        stake.setStatus(Status.PENDING);
        stake.setStakeName(player.getName().toLowerCase());

        if (selfClaimActive && !wcfg.twoStepSelfClaim && !stake.getReclaimed()) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            stake.setStatus(null);
            stake.setStakeName(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + claim.getId() + "!");
        } else {
            if (stake.getReclaimed()) {
                sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + claim.getId() + 
                        ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Your stake in " + ChatColor.WHITE + claim.getId() + 
                    ChatColor.YELLOW + " is pending.");

            if (!wcfg.silentNotify) {
                for (Player admin : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.hasPermission(admin, "stakeaclaim.pending.notify")) {
                        admin.sendMessage(ChatColor.YELLOW + "New stake by " + ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " in " + ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + " in " + 
                            ChatColor.BLUE + world.getName() + "!");
                    }
                }
            }
        }

        sMgr.save();
        saveRegions(world);
    }

    @Command(aliases = {"confirm", "c"},
            usage = "",
            desc = "Confirm your stake",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.confirm")
    public void confirm(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, player);
        if (stake == null) {
            throw new CommandException(ChatColor.YELLOW + "There is no pending stake for you to confirm.");
        }

        ProtectedRegion claim = rgMgr.getRegion(stake.getId());

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode == 1) {
            if (claim.getOwners().equals(player.getName().toLowerCase())) {
                stake.setStatus(null);
                stake.setStakeName(null);
                sMgr.save();
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        } else if (ownedCode > 1) {
            throw new CommandException(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                    claim.getId() + ChatColor.RED + " already has multiple owners!");
        }
        

        // Check if this would be over the selfClaimMax
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
        } else {
            if (regionList.size() < wcfg.selfClaimMax || wcfg.selfClaimMax == -1) {
                selfClaimActive = true;
            }
        }

        // Do we use self claim?
        if (selfClaimActive) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            stake.setStatus(null);
            stake.setStakeName(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + ChatColor.WHITE + claim.getId() + "!");

            sMgr.save();
            saveRegions(world);
        } else {
            throw new CommandException(ChatColor.YELLOW + "You can't confirm your own stake!");
        }
    }

    @Command(aliases = {"unstake", "u"},
            usage = "",
            desc = "Remove your stake.",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.unstake")
    public void unstake(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, player);
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeName(null);
            sender.sendMessage(ChatColor.YELLOW + "Your stake has been removed.");
            sMgr.save();
        } else {
            sender.sendMessage(ChatColor.YELLOW + "You had no pending stake.");
        }
    }

    @Command(aliases = {"add", "a"},
            usage = "<members...>",
            desc = "Add a member to a claim",
            min = 1)
    @CommandPermissions({"stakeaclaim.claim.add", "stakeaclaim.claim.add.own.*", "stakeaclaim.claim.add.member.*", "stakeaclaim.claim.add.*"})
    public void add(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "add", claim);

        RegionDBUtil.addToDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        if (!wcfg.silentNotify) {
            for (Player member : plugin.getServer().getOnlinePlayers()) {
                for (String added : args.getPaddedSlice(1, 0)) {
                    if (member.getName().equalsIgnoreCase(added)) {
                        member.sendMessage(ChatColor.GREEN + player.getName().toLowerCase() + ChatColor.YELLOW + " has added you to " + ChatColor.WHITE + claim.getId() + "!");
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.GREEN + args.getJoinedStrings(0) + 
                ChatColor.YELLOW + " to claim: " + ChatColor.WHITE + regionID + ".");

        saveRegions(world);
    }

    @Command(aliases = {"remove", "r"},
            usage = "<members...>",
            flags = "a:",
            desc = "Remove a member from a claim",
            min = 1)
    @CommandPermissions({"stakeaclaim.claim.remove", "stakeaclaim.claim.remove.own.*", "stakeaclaim.claim.remove.member.*", "stakeaclaim.claim.remove.*"})
    public void remove(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
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

    @Command(aliases = {"private", "p"},
            usage = "",
            desc = "Toggle a claim to private/open",
            min = 0, max = 0)
    @NestedCommand(value=PrivateCommands.class, executeBody=true)
    @CommandPermissions({"stakeaclaim.claim.private", "stakeaclaim.claim.private.own.*", "stakeaclaim.claim.private.member.*", "stakeaclaim.claim.private.*"})
    public void setprivate(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final String regionID = claim.getId();

        checkPerm(player, "private", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode < 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        if (claim.getFlag(DefaultFlag.ENTRY) == null || (claim.getFlag(DefaultFlag.ENTRY) != null && claim.getFlag(DefaultFlag.ENTRY) == State.ALLOW)) {
            claim.setFlag(DefaultFlag.ENTRY, State.DENY);
            sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.RED + "private.");
        } else {
            claim.setFlag(DefaultFlag.ENTRY, null);
            sender.sendMessage(ChatColor.YELLOW + "Set " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " to " + ChatColor.GRAY + "open.");
        }

        saveRegions(world);
    }

    @Command(aliases = {"warp", "w"},
            usage = "<player> [list entry #]",
            desc = "Warp to a players claim",
            min = 0, max = 2)
    @CommandPermissions({"stakeaclaim.claim.warp", "stakeaclaim.claim.warp.own.*", "stakeaclaim.claim.warp.member.*", "stakeaclaim.claim.warp.*"})
    public void warp(CommandContext args, CommandSender sender) throws CommandException {

        final Player travelPlayer = plugin.checkPlayer(sender);
        final World world = travelPlayer.getWorld();
        final PlayerState state = plugin.getPlayerStateManager().getState(travelPlayer);

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        if (args.argsLength() == 0) {
            if (state.lastWarp != null) {
                if (wcfg.useStakes) {
                    SACUtil.warpTo(state.lastWarp, state, travelPlayer, false, sMgr.getStake(state.lastWarp.getId()).getClaimName());
                } else { 
                    SACUtil.warpTo(state.lastWarp, state, travelPlayer, false, null);
                }
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
            boolean onlinePlayer = false;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (targetPlayer.equalsIgnoreCase(player.getName())) {
                    onlinePlayer = true;
                }
            }
            boolean offlinePlayer = plugin.getServer().getOfflinePlayer(targetPlayer).hasPlayedBefore();

            if (!onlinePlayer && !offlinePlayer) {
                throw new CommandException(ChatColor.GREEN + targetPlayer + ChatColor.YELLOW + " has not played on this server.");
            }
            throw new CommandException(ChatColor.GREEN + targetPlayer + ChatColor.YELLOW + " has no claims for you to warp to.");
        } else if (regionList.size() == 1) {
            ProtectedRegion claim = regionList.get(0);
            if (wcfg.useStakes) {
                SACUtil.warpTo(claim, state, travelPlayer, false, sMgr.getStake(claim.getId()).getClaimName());
            } else { 
                SACUtil.warpTo(claim, state, travelPlayer, false, null);
            }
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
            ProtectedRegion claim = rgMgr.getRegion(regions.get(listNumber));
            if (wcfg.useStakes) {
                SACUtil.warpTo(claim, state, travelPlayer, false, sMgr.getStake(claim.getId()).getClaimName());
            } else { 
                SACUtil.warpTo(claim, state, travelPlayer, false, null);
            }
        }

        // Display the list
        sender.sendMessage(ChatColor.GREEN + targetPlayer + ChatColor.RED + "'s claim warp list:");

        for (int i = 0; i < regions.size(); i++) {
            if (wcfg.useStakes) {
                Stake stake = sMgr.getStake(regions.get(i));
                if (stake.getClaimName() != null) {
                    sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + regions.get(i) + " " + ChatColor.LIGHT_PURPLE + stake.getClaimName());
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + regions.get(i));
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + regions.get(i));
            }
        }
    }

    @Command(aliases = {"me", "m"},
            usage = "",
            desc = "Your claims",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.me")
    public void me(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        LinkedHashMap<World, LinkedHashMap<Integer, String>> allClaims = new LinkedHashMap<World, LinkedHashMap<Integer, String>>();
        RegionManager rgMgr;
        int index = 0;

        final ConfigManager cfg = plugin.getGlobalManager();

        for (World world : plugin.getServer().getWorlds()) {
            rgMgr = WGBukkit.getRegionManager(world);
            if (rgMgr == null) {
                continue;
            }
            final WorldConfig wcfg = cfg.get(world);
            final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

            LinkedHashMap<Integer, String> someClaims = new LinkedHashMap<Integer, String>();
            ArrayList<ProtectedRegion> regions = SACUtil.getOwnedRegions(rgMgr, player);
            for (ProtectedRegion region : regions) {
                if (wcfg.useStakes) {
                    Stake stake = sMgr.getStake(region.getId());
                    if (stake.getClaimName() != null) {
                        someClaims.put(index, region.getId() + " " + ChatColor.LIGHT_PURPLE + stake.getClaimName());
                    } else {
                        someClaims.put(index, region.getId());
                    }
                } else {
                    someClaims.put(index, region.getId());
                }
                index++;
            }
            Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, player);
            if (stake != null) {
                someClaims.put(index, stake.getId() + ChatColor.YELLOW + " Pending");
                index++;
            }
            if (!someClaims.isEmpty()) {
                allClaims.put(world, someClaims);
            }
        }

        if (!allClaims.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Summery for " + ChatColor.GREEN + player.getName().toLowerCase());
            for (World claimWorld : allClaims.keySet()) {
                if (allClaims.size() == 1 && allClaims.containsKey(player.getWorld())) {
                    sender.sendMessage(ChatColor.YELLOW + "Claims:");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Claims in " + ChatColor.BLUE + claimWorld.getName() + ChatColor.YELLOW + ":");
                }
                LinkedHashMap<Integer, String> someClaims = allClaims.get(claimWorld);
                for (Integer i : someClaims.keySet()) {
                    sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + someClaims.get(i));
                }
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "You do not have any claims!");
        }
    }

    @Command(aliases = {"set"},
        desc = "Set things about this claim")
    @NestedCommand(SetCommands.class)
    @CommandPermissions("stakeaclaim.claim.set")
    public void set(CommandContext args, CommandSender sender) {}

    @Command(aliases = {"del", "delete"},
            desc = "Delete things about this claim")
    @NestedCommand(DeleteCommands.class)
    @CommandPermissions("stakeaclaim.claim.del")
    public void del(CommandContext args, CommandSender sender) {}

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
