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
import com.nineteengiraffes.stakeaclaim.SACUtil.ListType;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
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

    // list commands
    @Command(aliases = {"list", "l"},
            usage = "[page]",
            desc = "Show the current claim list",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.list")
    public void list(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final World world = state.listWorld;
        LinkedHashMap<Integer, String> regions = state.regionList;
        if (regions == null || regions.isEmpty() || world == null) {
            throw new CommandException(ChatColor.YELLOW + "The Claim list is empty.");
        }

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

        int page = 0;
        if (args.argsLength() > 0) {
            page = Math.max(0, args.getInteger(0) - 1);
        }
        final int totalSize = regions.size();
        final int pageSize = 10;
        final int pages = (int) Math.ceil(totalSize / (float) pageSize);
        if (page + 1 > pages) {
            page = pages - 1;
        }

        if (sender instanceof Player && ((Player) sender).getWorld() == world) {
            sender.sendMessage(ChatColor.YELLOW + "Claim list: (page " + (page + 1) + " of " + pages + ")");
        } else {
            sender.sendMessage(ChatColor.BLUE + world.getName() + ChatColor.YELLOW + " claim list: (page " + (page + 1) + " of " + pages + ")");
        }

        if (page < pages) {
            for (int i = page * pageSize; i < page * pageSize + pageSize; i++) {
                if (i >= totalSize) {
                    break;
                }
                SACUtil.displayClaim(String.valueOf(i + 1), rgMgr.getRegion(regions.get(i)), sMgr.getStake(regions.get(i)), sender);
            }
        }
    }

    @Command(aliases = {"pending", "p"},
            usage = "[world]",
            desc = "Populates a list of pending stakes",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.pending")
    public void pending(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 0, ListType.PENDING);
    }

    @Command(aliases = {"user", "u"},
            usage = "<username> [world]",
            desc = "Populates a list of claims for a user",
            min = 1, max = 2)
    @CommandPermissions("stakeaclaim.tools.user")
    public void user(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 1, ListType.USER);
    }

    @Command(aliases = {"claim", "c"},
            usage = "<list entry #> or <claim id> [world]",
            desc = "Populates a single item list",
            min = 1, max = 2)
    @CommandPermissions("stakeaclaim.tools.claim")
    public void claim(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 1, ListType.CLAIM);
    }

    @Command(aliases = {"own", "owned", "o"},
            usage = "[world]",
            desc = "Populates a list of owned claims",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.own")
    public void own(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 0, ListType.OWN);
    }

    @Command(aliases = {"free", "f"},
            usage = "[world]",
            desc = "Populates a list of unclaimed claims",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.free")
    public void free(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 0, ListType.FREE);
    }

    @Command(aliases = {"vip", "v"},
            usage = "[world]",
            desc = "Populates a list of VIP claims",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.vip")
    public void vip(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doList(plugin, args, sender, 0, ListType.VIP);
    }


    // action commands
    @Command(aliases = {"accept", "a"},
            usage = "<list entry #>",
            desc = "Accept a pending stake",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.accept")
    public void accept(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final World world = state.listWorld;
        if (world == null) {
            throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
        }

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = SACUtil.getRegionIDFromList(args, state, 0);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);

        if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
        } else {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this stake is not pending.");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            stake.setStatus(null);
            stake.setStakeName(null);
            sMgr.save();
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        // Accept the stake
        claim.getOwners().addPlayer(stake.getStakeName());

        sender.sendMessage(ChatColor.YELLOW + "You have accepted " + ChatColor.GREEN + stake.getStakeName() +
                ChatColor.YELLOW + "'s stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.ACCEPTED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                            (stakeHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " has been " + ChatColor.DARK_GREEN + "accepted!");
                    stake.setStatus(null);
                    stake.setStakeName(null);
                }
            }
        }

        sMgr.save();
        saveRegions(world);
    }

    @Command(aliases = {"deny", "d"},
            usage = "<list entry #>",
            desc = "Deny a pending stake",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.deny")
    public void deny(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final World world = state.listWorld;
        if (world == null) {
            throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
        }

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = SACUtil.getRegionIDFromList(args, state, 0);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);

        if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
        } else {
            throw new CommandException(ChatColor.YELLOW + "Sorry, this stake is not pending.");
        }

        int ownedCode = SACUtil.isRegionOwned(claim);
        if (ownedCode > 0) {
            stake.setStatus(null);
            stake.setStakeName(null);
            sMgr.save();
            throw new CommandException(ChatColor.YELLOW + "Sorry, this claim is not open.");
        }

        sender.sendMessage(ChatColor.YELLOW + "You have denied " + ChatColor.GREEN + stake.getStakeName() +
                ChatColor.YELLOW + "'s stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.DENIED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                            (stakeHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "denied!");
                    stake.setStatus(null);
                    stake.setStakeName(null);
                }
            }
        }

        sMgr.save();
    }

    @Command(aliases = {"reclaim", "r"},
            usage = "<list entry #>",
            desc = "Reclaim/reset a claim",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.reclaim")
    public void reclaim(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final World world = state.listWorld;
        if (world == null) {
            throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
        }

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final String regionID = SACUtil.getRegionIDFromList(args, state, 0);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);

        sender.sendMessage(ChatColor.YELLOW + "You have reclaimed " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID +
                ChatColor.YELLOW + " from " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ChatColor.YELLOW + "!");

        if (!wcfg.silentNotify) {
            for (Player claimHolder : plugin.getServer().getOnlinePlayers()) {
                if (claim.getOwners().contains(claimHolder.getName())) {
                    claimHolder.sendMessage(ChatColor.YELLOW + "You no longer own " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                            (claimHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ", it has been " + ChatColor.DARK_RED + "reclaimed!");
                }
            }
        }

        claim.getOwners().getPlayers().clear();
        claim.getOwners().getGroups().clear();
        claim.getMembers().getPlayers().clear();
        claim.getMembers().getGroups().clear();
        claim.setFlag(DefaultFlag.TELE_LOC,null);
        claim.setFlag(DefaultFlag.ENTRY,null);
        stake.setStatus(null);
        stake.setStakeName(null);
        stake.setDefaultEntry(null);
        stake.setClaimName(null);
        stake.setVIP(false);
        stake.setRecalimed(wcfg.useReclaimed);

        sMgr.save();
        saveRegions(world);
    }

    @Command(aliases = {"goto", "g", "go"},
            usage = "<list entry #> or <claim id>",
            desc = "Goto to a claim",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.goto")
    public void togo(CommandContext args, CommandSender sender) throws CommandException{
        SACUtil.doGoto(plugin, args, sender, false);
    }

    @Command(aliases = {"spawn", "s"},
            usage = "<list entry #> or <claim id>",
            desc = "Goto to a claim's spawn",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.spawn")
    public void spawn(CommandContext args, CommandSender sender) throws CommandException {
        SACUtil.doGoto(plugin, args, sender, true);
    }

    @Command(aliases = {"proxy", "x"},
            usage = "",
            desc = "Stake their claim",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.tools.proxy")
    public void proxy(CommandContext args, CommandSender sender) throws CommandException {

        final Player activePlayer = plugin.checkPlayer(sender);
        final World world = activePlayer.getWorld();
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        if (state.unsubmittedStake == null) {
            throw new CommandException(ChatColor.YELLOW + "No player to proxy for.");
        }
        final String[] unsubmittedStake = state.unsubmittedStake;
        final String regionID = unsubmittedStake[1];
        final String passivePlayer = unsubmittedStake[0];
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);
        final Stake stake = sMgr.getStake(regionID);
        
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
                } else if (!stake.getStakeName().equals(passivePlayer)) {
                    sMgr.save();
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "This claim is already staked by " +
                            ChatColor.GREEN + stake.getStakeName() + ".");
                }
            }
        } else {
            if (claim.getOwners().contains(passivePlayer)) {
                throw new CommandException(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + " already owns this claim.");
            }
            throw new CommandException(ChatColor.YELLOW + "This claim is already owned by " + 
                    ChatColor.GREEN + claim.getOwners().toUserFriendlyString() + ".");
        }

        // Check if this would be over the proxyClaimMax
         ArrayList<ProtectedRegion> regionList = SACUtil.getOwnedClaims(rgMgr, wcfg, passivePlayer);

        if (wcfg.useVolumeLimits) {
            double volume = claim.volume();
            for (ProtectedRegion region : regionList) {
                volume = volume + region.volume();
            }
            if (volume > wcfg.proxyMaxVolume && wcfg.proxyMaxVolume != -1) {
                sMgr.save();
                saveRegions(world);
                throw new CommandException(ChatColor.YELLOW + "This claim would put " + ChatColor.GREEN + passivePlayer + 
                        ChatColor.YELLOW + " over the maximum claim volume.");
            }
        } else {
            if (regionList.size() >= wcfg.proxyMaxCount && wcfg.proxyMaxCount != -1) {
                sMgr.save();
                saveRegions(world);
                throw new CommandException(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + 
                        " has already claimed the maximum number of claims.");
            }
        }

        // Cancel old stakes
        Stake oldStake = SACUtil.getPendingStake(rgMgr, sMgr, passivePlayer);
        if (oldStake != null) {
            oldStake.setStatus(null);
            oldStake.setStakeName(null);
        }

        // Submit stake
        stake.setStatus(Status.PENDING);
        stake.setStakeName(passivePlayer);

        if (stake.getReclaimed()) {
            sender.sendMessage(ChatColor.RED + "note: " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                    ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
        }
        sender.sendMessage(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + "'s stake in " + 
                (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + ChatColor.YELLOW + " is pending.");

        if (!wcfg.silentNotify) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().equalsIgnoreCase(passivePlayer)) {
                    player.sendMessage(ChatColor.YELLOW + "Your stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                            (player.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " is pending.");
                }
                if (plugin.hasPermission(player, "stakeaclaim.pending.notify")) {
                    player.sendMessage(ChatColor.YELLOW + "New stake by " + ChatColor.GREEN + passivePlayer + 
                            ChatColor.YELLOW + " in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + regionID + 
                            (player.getWorld().equals(world) ? "!" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName() + "!"));
                }
            }
        }

        sMgr.save();
        saveRegions(world);
    }

    // Other methods
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
