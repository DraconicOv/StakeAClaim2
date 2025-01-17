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
import java.util.UUID;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.nineteengiraffes.stakeaclaim.util.SACUtil;
import com.nineteengiraffes.stakeaclaim.util.SearchUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldguard.bukkit.BukkitWorldGuardPlatform;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
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

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        final Stake stake = sMgr.getStake(claim);

        SACUtil.checkPerm(plugin, player, "info", claim);

        SACUtil.displayClaim(wcfg, claim, stake, sender, plugin, world, null);
    }

    @Command(aliases = {"stake", "s"},
            usage = "",
            desc = "Stake your claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.stake")
    public void stake(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode <= 0) {
            claim.getMembers().clear();
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING) {
                if (stake.getStakeUUID() == null) {
                    stake.setStatus(null);
                    stake.setStakeUUID(null);
                    stake.setDefaultEntry(null);
                    claim.setFlag(WorldGuard.getInstance().getFlagRegistry().get("entry"),null);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "This claim is already staked by " + 
                            SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, stake.getStakeUUID())));
                    return;
                }
            }
        } else {
            if (claim.getOwners().contains(player.getUniqueId()) || claim.getOwners().contains(player.getName().toLowerCase())) {
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "This claim is already owned by");
            for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, oneOwner)));
            }

// remove for loop when names get removed entirely
            for (String oneOwner : claim.getOwners().getPlayers()) {
                message.append(" " + SACUtil.formatPlayer(oneOwner));
            }

            throw new CommandException(message.toString() + ".");
        }

        // Check if this would be over the claimMax
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        regionList.addAll(rgMgr.getRegions().values());
        regionList = SearchUtil.filterForClaims(plugin, world, regionList);
        regionList = SearchUtil.ownerFilter(regionList, player);
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
                throw new CommandException(ChatColor.YELLOW + "This claim would put you over the maximum claim volume.");
            }
        } else {
            if (regionList.size() < wcfg.unassistedMaxCount || wcfg.unassistedMaxCount == -1) {
                unassisted = true;
            }
            if (regionList.size() >= wcfg.totalMaxCount && wcfg.totalMaxCount != -1) {
                sMgr.save();
                throw new CommandException(ChatColor.YELLOW + "You have already claimed the maximum number of claims.");
            }
        }

        // VIP check
        if (stake.getVIP() && !SACUtil.hasPermission(plugin, player, "stakeaclaim.vip")) {
            sMgr.save();
            throw new CommandException(ChatColor.YELLOW + "Only " + wcfg.VIPs + " may stake this claim.");
        }

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, player.getUniqueId());
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeUUID(null);
        }

        // Submit stake
        stake.setStatus(Status.PENDING);
        stake.setStakeUUID(player.getUniqueId());

        if (unassisted && !wcfg.confirmUnassisted && !stake.getReclaimed()) {
            claim.getOwners().addPlayer(player.getUniqueId());
            stake.setStatus(null);
            stake.setStakeUUID(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + SACUtil.formatID(stake) + "!");
        } else {
            if (stake.getReclaimed()) {
                sender.sendMessage(ChatColor.RED + "note: " + SACUtil.formatID(stake) + 
                        ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
            }
            sender.sendMessage(ChatColor.YELLOW + "Your stake in " + SACUtil.formatID(stake) + 
                    ChatColor.YELLOW + " is pending.");

            if (!wcfg.silentNotify) {
                for (Player admin : plugin.getServer().getOnlinePlayers()) {
                    if (SACUtil.hasPermission(plugin, admin, "stakeaclaim.pending.notify")) {
                        StringBuilder message = new StringBuilder("tellraw " + admin.getName() + " {text:'New stake by',color:yellow,extra:[");
                        if (SACUtil.hasPermission(plugin, admin, "stakeaclaim.sac.user")) {
                            message.append(SACUtil.formatPlayerJSON(world, player));
                        } else {
                            message.append(SACUtil.formatPlayerJSON(null, player));
                        }
                        message.append(",' in',");
                        if (SACUtil.hasPermission(plugin, admin, "stakeaclaim.sac.claim")) {
                            message.append(SACUtil.formatIdJSON(stake, world));
                        } else {
                            message.append(SACUtil.formatIdJSON(stake, null));
                        }
                        if (!admin.getWorld().equals(world)) {
                            message.append(",' in',");
                            message.append("{text:' " + world.getName() + "',color:blue}");
                        }
                        message.append(",'!']}");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message.toString());
                    }
                }
            }
        }

        sMgr.save();
    }

    @Command(aliases = {"confirm", "c"},
            usage = "",
            desc = "Confirm your stake",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.confirm")
    public void confirm(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, player.getUniqueId());
        if (stake == null) {
            throw new CommandException(ChatColor.YELLOW + "There is no pending stake for you to confirm.");
        }

        ProtectedRegion claim = rgMgr.getRegion(stake.getId());

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            if (claim.getOwners().contains(player.getUniqueId()) || claim.getOwners().contains(player.getName().toLowerCase())) {
                stake.setStatus(null);
                stake.setStakeUUID(null);
                sMgr.save();
                throw new CommandException(ChatColor.YELLOW + "You already own this claim.");
            }
            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "This claim is already owned by");
            for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, oneOwner)));
            }

// remove for loop when names get removed entirely
            for (String oneOwner : claim.getOwners().getPlayers()) {
                message.append(" " + SACUtil.formatPlayer(oneOwner));
            }

            sender.sendMessage(message.toString() + ".");
            return;
        }

        // Check if this would be over the selfClaimMax
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        regionList.addAll(rgMgr.getRegions().values());
        regionList = SearchUtil.filterForClaims(plugin, world, regionList);
        regionList = SearchUtil.ownerFilter(regionList, player);
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
            claim.getOwners().addPlayer(player.getUniqueId());
            stake.setStatus(null);
            stake.setStakeUUID(null);

            sender.sendMessage(ChatColor.YELLOW + "You have staked your claim in " + SACUtil.formatID(stake) + "!");

            sMgr.save();
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

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, player.getUniqueId());
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeUUID(null);
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

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useSAC) {
            throw new CommandException(ChatColor.YELLOW + "SAC is disabled in this world.");
        }

        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        SACUtil.checkPerm(plugin, player, "add", claim);

        DefaultDomain addedMembers = SACUtil.newDomain(plugin, args.getParsedPaddedSlice(0, 0));
        claim.getMembers().addAll(addedMembers);
        
        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (!wcfg.silentNotify) {
            for (Player member : plugin.getServer().getOnlinePlayers()) {
                for (UUID added : addedMembers.getUniqueIds()) {
                    if (member.getUniqueId() == added) {
                        member.sendMessage(SACUtil.formatPlayer(player) + ChatColor.YELLOW + " has added you to " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + "!");
                    }
                }
            }
        }

        StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Added");
        for (UUID added : addedMembers.getUniqueIds()) {
            message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, added)));
        }
        sender.sendMessage(message.toString() + ChatColor.YELLOW + " to claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");

    }

    @Command(aliases = {"remove", "r"},
            usage = "<members...> or <'all'>",
            flags = "",
            desc = "Remove a member from a claim",
            min = 1)
    @CommandPermissions({"stakeaclaim.claim.remove", "stakeaclaim.claim.remove.own.*", "stakeaclaim.claim.remove.member.*", "stakeaclaim.claim.remove.*"})
    public void remove(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useSAC) {
            throw new CommandException(ChatColor.YELLOW + "SAC is disabled in this world.");
        }

        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        SACUtil.checkPerm(plugin, player, "remove", claim);

        String in = args.getString(0);

        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (in.equalsIgnoreCase("all") || in.equalsIgnoreCase("a")) {
            claim.getMembers().removeAll();

            sender.sendMessage(ChatColor.YELLOW + "Removed all members from claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");
        } else {
            DefaultDomain removedMembers = SACUtil.newDomain(plugin, args.getParsedPaddedSlice(0, 0));
            claim.getMembers().removeAll(removedMembers);

            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Removed");
            for (UUID removed : removedMembers.getUniqueIds()) {
                message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, removed)));
            }
            sender.sendMessage(message.toString() + ChatColor.YELLOW + " from claim: " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ".");

        }

    }

    @Command(aliases = {"private", "p"},
            usage = "",
            desc = "Toggle a claim to private/open",
            min = 0, max = 0)
    @NestedCommand(value=PrivateCommands.class, executeBody=true)
    @CommandPermissions({"stakeaclaim.claim.private", "stakeaclaim.claim.private.own.*", "stakeaclaim.claim.private.member.*", "stakeaclaim.claim.private.*"})
    public void setprivate(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useSAC) {
            throw new CommandException(ChatColor.YELLOW + "SAC is disabled in this world.");
        }

        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);

        SACUtil.checkPerm(plugin, player, "private", claim);

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode < 1) {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not owned.");
        }

        boolean isVIP = false;
        if (wcfg.useStakes) {
            isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
        }

        if (claim.getFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry")) == null || (claim.getFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry")) != null && claim.getFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry")) == State.ALLOW)) {
            claim.setFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry"), State.DENY);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + " to " + ChatColor.RED + "private.");
        } else {
            claim.setFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry"), null);
            sender.sendMessage(ChatColor.YELLOW + "Set " + (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + ChatColor.YELLOW + " to " + ChatColor.GRAY + "open.");
        }

    }

    @Command(aliases = {"warp", "w"},
            usage = "<player> [list item #]",
            desc = "Warp to a players claim",
            min = 0, max = 2)
    @CommandPermissions({"stakeaclaim.claim.warp", "stakeaclaim.claim.warp.own.*", "stakeaclaim.claim.warp.member.*", "stakeaclaim.claim.warp.*"})
    public void warp(CommandContext args, CommandSender sender) throws CommandException {

        if (args.argsLength() == 0) {
            if (!SACUtil.gotoRememberedWarp(plugin, sender, false)) {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                sender.sendMessage(ChatColor.RED + "/claim " + args.getCommand() + " <player> [list item #]");
            }
            return;
        }

        final Player travelPlayer = SACUtil.checkPlayer(sender);
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();

        final World world = travelPlayer.getWorld();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        final OfflinePlayer targetPlayer = SACUtil.getOfflinePlayer(plugin, SACUtil.uuidLookupWithEx(plugin, args.getString(0)));

        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        regionList.addAll(rgMgr.getRegions().values());
        regionList = SearchUtil.filterForClaims(plugin, world, regionList);
        regionList = SearchUtil.ownerFilter(regionList, targetPlayer);
        LinkedHashMap<Integer, ProtectedRegion> regions = new LinkedHashMap<Integer, ProtectedRegion>();

        regionList = SearchUtil.canWarpTo(plugin, regionList, travelPlayer);
        if (regionList.isEmpty()) {
            sender.sendMessage(SACUtil.formatPlayer(targetPlayer) + ChatColor.YELLOW + " has no claims for you to warp to.");
            return;

        } else if (regionList.size() == 1) {
            ProtectedRegion claim = regionList.get(0);
            state.lastWarp = SACUtil.warpTo(plugin, world, claim, sMgr.getStake(claim), travelPlayer, false);
            if (state.lastWarp == null) {
                state.warpWorld = null;
            } else {
                state.warpWorld = world;
            }
            return;

        } else {
            regions = SearchUtil.joinLists(regionList, null);
        }

        state.regionList = regions;
        state.listWorld = world;

        if (args.argsLength() == 2) {
            ProtectedRegion claim = null;

            int item;
            try {
                item = args.getInteger(1) - 1;
            } catch (NumberFormatException e) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + args.getString(1) + 
                        ChatColor.YELLOW + "' is not a valid number.");
            }

            if (!regions.containsKey(item)) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + args.getString(1) + 
                        ChatColor.YELLOW + "' is not a valid list item number.");
            }
            claim = regions.get(item);


            state.lastWarp = SACUtil.warpTo(plugin, world, claim, sMgr.getStake(claim), travelPlayer, false);
            if (state.lastWarp == null) {
                state.warpWorld = null;
            } else {
                state.warpWorld = world;
            }
            return;
        }

        // Display the list
        sender.sendMessage(SACUtil.formatPlayer(targetPlayer) + ChatColor.YELLOW + "'s warps!");

        SACUtil.displayList(plugin, sender, regions, sMgr, world, 0);

    }

    @Command(aliases = {"me", "m"},
            usage = "",
            desc = "Your claims",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.me")
    public void me(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        SACUtil.displayPlayer(plugin, sender, rgMgr, world, player);
    }

    @Command(aliases = {"proxy", "x"},
            usage = "",
            desc = "Stake their claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.claim.proxy")
    public void proxy(CommandContext args, CommandSender sender) throws CommandException {

        final Player activePlayer = SACUtil.checkPlayer(sender);
        final World world = activePlayer.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        if (state.unsubmittedStake == null) {
            throw new CommandException(ChatColor.YELLOW + "No player to proxy for.");
        }

        final String regionID = state.unsubmittedStake.getId();
        final UUID passiveUUID = state.unsubmittedStake.getStakeUUID();
        final OfflinePlayer passivePlayer = SACUtil.getOfflinePlayer(plugin, state.unsubmittedStake.getStakeUUID());
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        final Stake stake = sMgr.getStake(claim);
        
        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode <= 0) {
            claim.getMembers().clear();
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING) {
                if (stake.getStakeUUID() == null) {
                    stake.setStatus(null);
                    stake.setStakeUUID(null);
                    stake.setDefaultEntry(null);
                    claim.setFlag((StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry"),null);
                } else if (stake.getStakeUUID() != passiveUUID) {
                    sMgr.save();
                    sender.sendMessage(ChatColor.YELLOW + "This claim is already staked by " + 
                            SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, stake.getStakeUUID())));
                    return;
                }
            }
        } else {
            if (claim.getOwners().contains(passivePlayer.getUniqueId()) || claim.getOwners().contains(passivePlayer.getName().toLowerCase())) {
                sender.sendMessage(SACUtil.formatPlayer(passivePlayer) + ChatColor.YELLOW + " already owns this claim.");
                return;
            }
            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "This claim is already owned by");
            for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, oneOwner)));
            }

// remove for loop when names get removed entirely
            for (String oneOwner : claim.getOwners().getPlayers()) {
                message.append(" " + SACUtil.formatPlayer(oneOwner));
            }

            sender.sendMessage(message.toString() + ".");
            return;
        }

        // Check if this would be over the proxyClaimMax
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        regionList.addAll(rgMgr.getRegions().values());
        regionList = SearchUtil.filterForClaims(plugin, world, regionList);
        regionList = SearchUtil.ownerFilter(regionList, passivePlayer);

        if (wcfg.useVolumeLimits) {
            double volume = claim.volume();
            for (ProtectedRegion region : regionList) {
                volume = volume + region.volume();
            }
            if (volume > wcfg.proxyMaxVolume && wcfg.proxyMaxVolume != -1) {
                sMgr.save();
                sender.sendMessage(ChatColor.YELLOW + "This claim would put " + SACUtil.formatPlayer(passivePlayer) + 
                        ChatColor.YELLOW + " over the maximum claim volume.");
                return;
            }
        } else {
            if (regionList.size() >= wcfg.proxyMaxCount && wcfg.proxyMaxCount != -1) {
                sMgr.save();
                sender.sendMessage(SACUtil.formatPlayer(passivePlayer) + ChatColor.YELLOW + 
                        " has already claimed the maximum number of claims.");
                return;
            }
        }

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, passiveUUID);
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeUUID(null);
        }

        // Submit stake
        stake.setStatus(Status.PENDING);
        stake.setStakeUUID(passiveUUID);

        if (stake.getReclaimed()) {
            sender.sendMessage(ChatColor.RED + "note: " + SACUtil.formatID(stake) + 
                    ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
        }
        sender.sendMessage(SACUtil.formatPlayer(passivePlayer) + ChatColor.YELLOW + "'s stake in " + SACUtil.formatID(stake) + 
                ChatColor.YELLOW + " is pending.");

        if (!wcfg.silentNotify) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getUniqueId() == passiveUUID) {
                    player.sendMessage(ChatColor.YELLOW + "Your stake in " + SACUtil.formatID(stake) + 
                            (player.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " is pending.");
                }
                if (SACUtil.hasPermission(plugin, player, "stakeaclaim.pending.notify")) {
                    StringBuilder message = new StringBuilder("tellraw " + player.getName() + " {text:'New stake by',color:yellow,extra:[");
                    if (SACUtil.hasPermission(plugin, player, "stakeaclaim.sac.user")) {
                        message.append(SACUtil.formatPlayerJSON(world, passivePlayer));
                    } else {
                        message.append(SACUtil.formatPlayerJSON(null, passivePlayer));
                    }
                    message.append(",' in',");
                    if (SACUtil.hasPermission(plugin, player, "stakeaclaim.sac.claim")) {
                        message.append(SACUtil.formatIdJSON(stake, world));
                    } else {
                        message.append(SACUtil.formatIdJSON(stake, null));
                    }
                    if (!player.getWorld().equals(world)) {
                        message.append(",' in',");
                        message.append("{text:' " + world.getName() + "',color:blue}");
                    }
                    message.append(",'!']}");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message.toString());
                }
            }
        }

        sMgr.save();
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

}
