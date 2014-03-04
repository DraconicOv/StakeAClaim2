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
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        final Stake stake = sMgr.getStake(claim);

        checkPerm(player, "info", claim);

        if (claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.RED + " Private!");
        } else if (stake.getVIP()) {
                sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + claim.getId() + ChatColor.AQUA +" " + wcfg.VIPs + " claim!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + claim.getId());
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
        Stake stake = sMgr.getStake(claim);

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
        } else {
            if (claim.getOwners().contains(player.getName().toLowerCase())) {
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        }

        // Check if this would be over the claimMax
        ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedClaims(rgMgr, wcfg, player);
        boolean unassisted = false;

        if (wcfg.useVolumeLimits) {
            double volume = claim.volume();
            for (ProtectedRegion region : regionList) {
                volume = volume + region.volume();
            }
            if (volume <= wcfg.unassistedMaxVolume || wcfg.unassistedMaxVolume == -1) {
                unassisted = true;
            }
            if (volume > wcfg.totalMaxVolume && wcfg.totalMaxVolume != -1) {
                sMgr.save();
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim volume.");
            }
        } else {
            if (regionList.size() < wcfg.unassistedMaxCount || wcfg.unassistedMaxCount == -1) {
                unassisted = true;
            }
            if (regionList.size() >= wcfg.totalMaxCount && wcfg.totalMaxCount != -1) {
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

        if (unassisted && !wcfg.confirmUnassisted && !stake.getReclaimed()) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            stake.setStatus(null);
            stake.setStakeName(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + "!");
        } else {
            if (stake.getReclaimed()) {
                sender.sendMessage(ChatColor.RED + "note: " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + 
                        ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Your stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + 
                    ChatColor.YELLOW + " is pending.");

            if (!wcfg.silentNotify) {
                for (Player admin : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.hasPermission(admin, "stakeaclaim.pending.notify")) {
                        admin.sendMessage(ChatColor.YELLOW + "New stake by " + ChatColor.GREEN + player.getName() + 
                                ChatColor.YELLOW + " in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + 
                                (admin.getWorld().equals(world) ? "!" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName() + "!"));
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
        if (ownedCode > 0) {
            if (claim.getOwners().contains(player.getName().toLowerCase())) {
                stake.setStatus(null);
                stake.setStakeName(null);
                sMgr.save();
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        }

        // Check if this would be over the selfClaimMax
        ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedClaims(rgMgr, wcfg, player);
        boolean unassisted = false;

        if (wcfg.useVolumeLimits) {
            double volume = claim.volume();
            for (ProtectedRegion region : regionList) {
                volume = volume + region.volume();
            }
            if (volume <= wcfg.unassistedMaxVolume || wcfg.unassistedMaxVolume == -1) {
                unassisted = true;
            }
        } else {
            if (regionList.size() < wcfg.unassistedMaxCount || wcfg.unassistedMaxCount == -1) {
                unassisted = true;
            }
        }

        // Do we claim unassisted?
        if (unassisted) {
            claim.getOwners().addPlayer(player.getName().toLowerCase());
            stake.setStatus(null);
            stake.setStakeName(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + "!");

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

        checkPerm(player, "add", claim);

        RegionDBUtil.addToDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (!wcfg.silentNotify) {
            for (Player member : plugin.getServer().getOnlinePlayers()) {
                for (String added : args.getPaddedSlice(1, 0)) {
                    if (member.getName().equalsIgnoreCase(added)) {
                        member.sendMessage(ChatColor.GREEN + player.getName().toLowerCase() + ChatColor.YELLOW + " has added you to " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + "!");
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.GREEN + args.getJoinedStrings(0) + 
                ChatColor.YELLOW + " to claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");

        saveRegions(world);
    }

    @Command(aliases = {"remove", "r"},
            usage = "<members...> or <'all'>",
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

        checkPerm(player, "remove", claim);

        String in = args.getString(0);

        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (in.toLowerCase().equals("all") || in.toLowerCase().equals("a")) {
            claim.getMembers().getPlayers().clear();
            claim.getMembers().getGroups().clear();
            sender.sendMessage(ChatColor.YELLOW + "Removed all members from claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");
        } else {
            RegionDBUtil.removeFromDomain(claim.getMembers(), args.getPaddedSlice(1, 0), 0);

            sender.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.GREEN + args.getJoinedStrings(0) + 
                    ChatColor.YELLOW + " from claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");
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

        checkPerm(player, "private", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode < 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (claim.getFlag(DefaultFlag.ENTRY) == null || (claim.getFlag(DefaultFlag.ENTRY) != null && claim.getFlag(DefaultFlag.ENTRY) == State.ALLOW)) {
            claim.setFlag(DefaultFlag.ENTRY, State.DENY);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + " to " + ChatColor.RED + "private.");
        } else {
            claim.setFlag(DefaultFlag.ENTRY, null);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + " to " + ChatColor.GRAY + "open.");
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
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        if (args.argsLength() == 0) {
            if (state.lastWarp != null) {
                state.lastWarp = SACUtil.warpTo(state.lastWarp, sMgr.getStake(state.lastWarp), travelPlayer, false);
                return;
            } else {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                throw new CommandException(ChatColor.RED + "/claim " + args.getCommand() + " <player> [list entry #]");
            }
        }

        final String targetPlayer = args.getString(0);

        ArrayList<ProtectedRegion> tempList = SACUtil.getOwnedClaims(rgMgr, wcfg, targetPlayer);
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
            state.lastWarp = SACUtil.warpTo(claim, sMgr.getStake(claim), travelPlayer, false);
            return;
        } else {
            int index = 0;
            for (ProtectedRegion region : regionList) {
                regions.put(index, region.getId());
                index++;
            }
        }
        state.regionList = regions;
        
        int listNumber = 0;
        if (args.argsLength() == 2) {
            listNumber = args.getInteger(1) - 1;
            if (!regions.containsKey(listNumber)) {
                throw new CommandException(ChatColor.YELLOW + "That is not a valid list entry number.");
            }
            ProtectedRegion claim = rgMgr.getRegion(regions.get(listNumber));
            state.lastWarp = SACUtil.warpTo(claim, sMgr.getStake(claim), travelPlayer, false);
            return;
        }

        // Display the list
        sender.sendMessage(ChatColor.GREEN + targetPlayer + ChatColor.YELLOW + "'s claim warp list:");

        for (int i = 0; i < regions.size(); i++) {
            SACUtil.displayClaim(String.valueOf(i + 1), rgMgr.getRegion(regions.get(i)), sMgr.getStake(regions.get(i)), travelPlayer);
        }
    }

    @Command(aliases = {"me", "m"},
            usage = "",
            desc = "Your claims",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.me")
    public void me(CommandContext args, CommandSender sender) throws CommandException {

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
        ArrayList<ProtectedRegion> regions = SACUtil.getOwnedClaims(rgMgr, wcfg, player);
        LinkedHashMap<Integer, String> regionList = new LinkedHashMap<Integer, String>();
        PlayerState state = plugin.getPlayerStateManager().getState(player);

        Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, player);
        if (stake != null) {
            regions.add(rgMgr.getRegion(stake.getId()));
        }

        if (regions.size() < 1) {
            state.regionList = null;
            throw new CommandException(ChatColor.YELLOW + "You do not have any claims!");
        }

        sender.sendMessage(ChatColor.YELLOW + "Stake list:");
        Integer index = 0;
        for (ProtectedRegion region : regions) {
            regionList.put(index, region.getId());
            index++;
            if (index < 10) {
                SACUtil.displayClaim(index.toString(), region, sMgr.getStake(region), player);
            }
        }
        if (index > 9) {
            sender.sendMessage(ChatColor.YELLOW + "Showing first 9 stakes of " + regions.size() + ". Do " + ChatColor.WHITE + "/tools list" + ChatColor.YELLOW + " to see full list.");
        }
        state.regionList = regionList;
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

    private boolean hasPerm(Player player, String command, ProtectedRegion claim) {

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
