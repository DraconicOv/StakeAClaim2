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
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ToolsCommands {
    private final StakeAClaimPlugin plugin;

    public ToolsCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"pending", "p"},
            usage = "[page]",
            desc = "Populates a list of pending stakes",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.tools.pending")
    public void pending(CommandContext args, CommandSender sender) throws CommandException {

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

        ArrayList<Stake> stakeList = SACUtil.getPendingStakes(rgMgr, sMgr);
        LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();
        PlayerState state = plugin.getPlayerStateManager().getState(player);

        int index = 0;
        for (Stake stake : stakeList) {
            regions.put(index, stake.getId());
            index++;
        }

        final int totalSize = regions.size();
        
        if (totalSize < 1) {
            state.regionList = null;
            throw new CommandException(ChatColor.YELLOW + "There are no pending stakes.");
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
                + "Pending stake list: (page "
                + (page + 1) + " of " + pages + ")");

        if (page < pages) {
            for (int i = page * pageSize; i < page * pageSize + pageSize; i++) {
                if (i >= totalSize) {
                    break;
                }
                Stake stake = sMgr.getStake(regions.get(i));
                sender.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + regions.get(i) +
                        ", " + ChatColor.GREEN + stake.getStakeName());
            }
        }
    }

    @Command(aliases = {"accept", "a"},
            usage = "<list entry #>",
            desc = "Accept a pending stake",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.tools.accept")
    public void accept(CommandContext args, CommandSender sender) throws CommandException {

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

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim.getId());

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
                ChatColor.YELLOW + "'s stake in " + ChatColor.WHITE + regionID + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.ACCEPTED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " in " + 
                            ChatColor.BLUE + world.getName() + ChatColor.YELLOW + " has been " + ChatColor.DARK_GREEN + "accepted!");
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

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim.getId());

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
                ChatColor.YELLOW + "'s stake in " + ChatColor.WHITE + regionID + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.DENIED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " in " + 
                            ChatColor.BLUE + world.getName() + ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "denied!");
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

        final String regionID = getRegionIDFromList(args, player);
        final ProtectedRegion claim = rgMgr.getRegion(regionID);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim.getId());

        // Reclaim the claim
        final String owner = claim.getOwners().toUserFriendlyString();
        SACUtil.reclaim(stake, claim, wcfg.useReclaimed);

        sender.sendMessage(ChatColor.YELLOW + "You have reclaimed " + ChatColor.WHITE + regionID +
                ChatColor.YELLOW + " from " + ChatColor.GREEN + owner + ChatColor.YELLOW + "!");

        if (!wcfg.silentNotify) {
            for (Player claimHolder : plugin.getServer().getOnlinePlayers()) {
                if (claimHolder.getName().equalsIgnoreCase(owner)) {
                    claimHolder.sendMessage(ChatColor.YELLOW + "You no longer own " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " in " + 
                            ChatColor.BLUE + world.getName() + ". " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "reclaimed!");
                }
            }
        }

        sMgr.save();
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
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final PlayerState state = plugin.getPlayerStateManager().getState(activePlayer);
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
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "This claim is already staked by " +
                            ChatColor.GREEN + stake.getStakeName() + ".");
                }
            }
        } else if (ownedCode == 1) {
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
            sender.sendMessage(ChatColor.RED + "note: " + ChatColor.WHITE + claim.getId() + 
                    ChatColor.YELLOW + " was claimed in the past and may not be pristine.");
        }
        sender.sendMessage(ChatColor.GREEN + passivePlayer + ChatColor.YELLOW + "'s stake in " + 
                ChatColor.WHITE + regionID + ChatColor.YELLOW + " is pending.");

        if (!wcfg.silentNotify) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().equalsIgnoreCase(passivePlayer)) {
                    player.sendMessage(ChatColor.YELLOW + "Your stake in " + ChatColor.WHITE + regionID + ChatColor.YELLOW + " in " + 
                            ChatColor.BLUE + world.getName() + ChatColor.YELLOW + " is pending.");
                }
                if (plugin.hasPermission(player, "stakeaclaim.pending.notify")) {
                    player.sendMessage(ChatColor.YELLOW + "New stake by " + ChatColor.GREEN + player.getName() + ChatColor.YELLOW + " in " + 
                            ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName() + "!");
                }
            }
        }

        sMgr.save();
    }

    @Command(aliases = {"goto", "g", "go"},
            usage = "<list entry #> or <region ID> ['spawn']",
            desc = "Goto to a claim",
            min = 0, max = 2)
    @CommandPermissions("stakeaclaim.tools.goto")
    public void togo(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final PlayerState state = plugin.getPlayerStateManager().getState(player);
        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        if (args.argsLength() == 0) {
            if (state.lastWarp != null) {
                if (wcfg.useStakes) {
                    SACUtil.warpTo(state.lastWarp, state, player, false, sMgr.getStake(state.lastWarp.getId()).getClaimName());
                } else { 
                    SACUtil.warpTo(state.lastWarp, state, player, false, null);
                }
            }
            sender.sendMessage(ChatColor.RED + "Too few arguments.");
            throw new CommandException(ChatColor.RED + "/tools " + args.getCommand() + "<list entry #> or <region ID>");
        }

        final boolean forceSpawn = (args.argsLength() >= 2 && (args.getString(1).toLowerCase().equals("s") || args.getString(1).toLowerCase().equals("spawn")));
        String claimID = args.getString(0);
        ProtectedRegion claim = rgMgr.getRegion(claimID);
        if (claim == null) {
            claimID = getRegionIDFromList(args, player);
            claim = rgMgr.getRegion(claimID);
        }

        if (claim.getFlag(DefaultFlag.ENTRY) != null && claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
                sender.sendMessage(ChatColor.WHITE + claimID + ChatColor.YELLOW + " is set to " + ChatColor.RED + "Private!");
        }

        if (wcfg.useStakes) {
            SACUtil.warpTo(claim, state, player, forceSpawn, sMgr.getStake(claim.getId()).getClaimName());
        } else { 
            SACUtil.warpTo(claim, state, player, forceSpawn, null);
        }

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
            throw new CommandException(ChatColor.YELLOW + "The stake list is empty.");
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
