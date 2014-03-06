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

package com.nineteengiraffes.stakeaclaim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SACUtil {

    public enum ListType {
        PENDING,
        USER,
        CLAIM,
        OWN,
        FREE,
        VIP
    }

    // Get regions
    /**
     * Get a list of pending claims
     * 
     * @param rgMgr the region manager to work with
     * @param sMgr the stake manager to work with
     * @param wcfg the world config to work with
     * @return list of pending claims
     */
    public static ArrayList<ProtectedRegion> getPendingClaims(RegionManager rgMgr, StakeManager sMgr, WorldConfig wcfg) {

        final Map<String, Stake> stakes = sMgr.getStakes();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        ArrayList<Stake> removeList = new ArrayList<Stake>();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;
        ProtectedRegion claim;
        boolean save = false;

        for (Stake stake : stakes.values()) {
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
                regexMat = regexPat.matcher(stake.getId());
                if (regexMat.find()) {
                    claim = rgMgr.getRegion(stake.getId());
                    if (claim == null) {
                        removeList.add(stake);
                        continue;
                    }
                    int ownedCode = isRegionOwned(claim);
                    if (ownedCode > 0) {
                        stake.setStatus(null);
                        stake.setStakeName(null);
                        save = true;
                    } else {
                        regionList.add(claim);
                    }
                }
            }
        }

        for (Stake stake : removeList) {
            stakes.remove(stake.getId());
            save = true;
        }
        if (save) sMgr.save();
        return regionList;
    }

    /**
     * Get a list of regions owned by (@code player)
     * 
     * @param rgMgr the region manager to work with
     * @param wcfg the world config to work with
     * @param player the player to get the regions for
     * @return list of regions owned by the player
     */
    public static ArrayList<ProtectedRegion> getOwnedClaims(RegionManager rgMgr, WorldConfig wcfg, Player player) {
        return getOwnedClaims(rgMgr, wcfg, player.getName().toLowerCase());
    }

    /**
     * Get a list of regions owned by (@code playerName)
     * 
     * @param rgMgr the region manager to work with
     * @param wcfg the world config to work with
     * @param playerName the name of the player to get the regions for
     * @return list of regions owned by the player
     */
    public static ArrayList<ProtectedRegion> getOwnedClaims(RegionManager rgMgr, WorldConfig wcfg, String playerName) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;

        for (ProtectedRegion region : regions.values()) {
            if (isRegionOwned(region) > 0 && region.getOwners().contains(playerName)) {
                regexMat = regexPat.matcher(region.getId());
                if (regexMat.find()) {
                    regionList.add(region);
                }
            }
        }
        return regionList;
    }

    /**
     * Get a list of owned claims
     * 
     * @param rgMgr the region manager to work with
     * @param wcfg the world config to work with
     * @return list of owned claims
     */
    public static ArrayList<ProtectedRegion> getOwnedClaims(RegionManager rgMgr, WorldConfig wcfg) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;

        for (ProtectedRegion region : regions.values()) {
            if (isRegionOwned(region) > 0) {
                regexMat = regexPat.matcher(region.getId());
                if (regexMat.find()) {
                    regionList.add(region);
                }
            }
        }
        return regionList;
    }

    /**
     * Get a list of unclaimed claims
     * 
     * @param rgMgr the region manager to work with
     * @param wcfg the world config to work with
     * @return list of unclaimed claims
     */
    public static ArrayList<ProtectedRegion> getUnclaimed(RegionManager rgMgr, WorldConfig wcfg) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;

        for (ProtectedRegion region : regions.values()) {
            if (isRegionOwned(region) <= 0) {
                regexMat = regexPat.matcher(region.getId());
                if (regexMat.find()) {
                    regionList.add(region);
                }
            }
        }
        return regionList;
    }

    /**
     * Get a list of VIP claims
     * 
     * @param rgMgr the region manager to work with
     * @param wcfg the world config to work with
     * @return list of VIP claims
     */
    public static ArrayList<ProtectedRegion> getVIPClaims(RegionManager rgMgr, StakeManager sMgr, WorldConfig wcfg) {

        final Map<String, Stake> stakes = sMgr.getStakes();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;
        ProtectedRegion region;

        for (Stake stake : stakes.values()) {
            if (stake.getVIP()) {
                regexMat = regexPat.matcher(stake.getId());
                if (regexMat.find()) {
                    region = rgMgr.getRegion(stake.getId());
                    if (region != null) {
                        regionList.add(region);
                    }
                }
            }
        }
        return regionList;
    }

    /**
     * Get the single 'claim' the player is in
     * 
     * @param player the player to get the location from
     * @param plugin the SAC plugin
     * @return the 'claim' the player is standing in
     * @throws CommandException no regions or not in a single 'claim'
     */
    public static ProtectedRegion getClaimStandingIn(Player player, StakeAClaimPlugin plugin) throws CommandException {

        final World world = player.getWorld();
        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        final Location loc = player.getLocation();

        ProtectedRegion claim = getClaimAtPoint(rgMgr, wcfg, new Vector(loc.getX(), loc.getY(), loc.getZ()));
 
        if (claim == null) {
            throw new CommandException("You are not in a single valid claim!");
        }

        return claim;
    }

    /**
     * Get the single 'claim' at a given point
     * 
     * @param rgMgr the region manager to look for the claim in
     * @param wcfg the world config to work with
     * @param vector the location to look for the claim
     * @return the claim at {@code vector}, returns null if there are no claims there, or more than one
     */
    public static ProtectedRegion getClaimAtPoint(RegionManager rgMgr, WorldConfig wcfg, Vector vector) {

        final ApplicableRegionSet rgSet = rgMgr.getApplicableRegions(vector);
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

        return claim;
    }


    // Get Stakes
    /**
     * Get the pending stake for (@code player) if any
     * 
     * @param rgMgr the region manager to work with
     * @param sMgr the stake manager to work with
     * @param player the player to get the regions for
     * @return pending stake for the player, or null
     */
    public static Stake getPendingStake(RegionManager rgMgr, StakeManager sMgr, Player player) {
        return getPendingStake(rgMgr, sMgr, player.getName().toLowerCase());
    }

    /**
     * Get the pending stake for (@code playerName) if any
     * 
     * @param rgMgr the region manager to work with
     * @param sMgr the stake manager to work with
     * @param player the player to get the regions for
     * @return pending stake for the player, or null
     */
    public static Stake getPendingStake(RegionManager rgMgr, StakeManager sMgr, String playerName) {

        final Map<String, Stake> stakes = sMgr.getStakes();
        ArrayList<Stake> stakeList = new ArrayList<Stake>();
        ArrayList<Stake> removeList = new ArrayList<Stake>();
        ProtectedRegion claim;
        boolean save = false;

        for (Stake stake : stakes.values()) {
            if (stake.getStakeName() != null && stake.getStakeName().equals(playerName) &&
                    stake.getStatus() != null && stake.getStatus() == Status.PENDING) {
                claim = rgMgr.getRegion(stake.getId());
                if (claim == null) {
                    removeList.add(stake);
                    continue;
                }
                int ownedCode = isRegionOwned(claim);
                if (ownedCode > 0) {
                    stake.setStatus(null);
                    stake.setStakeName(null);
                    save = true;
                } else {
                    stakeList.add(stake);
                }
            }
        }

        for (Stake stake : removeList) {
            stakes.remove(stake.getId());
            save = true;
        }

        if (stakeList.size() == 0) {
            if (save) sMgr.save();
            return null;
        } else {
            for (int i = 1; i < stakeList.size(); i++) {
                stakeList.get(i).setStatus(null);
                stakeList.get(i).setStakeName(null);
                save = true;
            }
            if (save) sMgr.save();
            return stakeList.get(0);
        }
    }


    // Utilities
    /**
     * Displays one {@code claim}
     * 
     * @param index the index to show for the claim
     * @param claim the claim to display
     * @param stake the stake for the claim
     * @param sender the sender to display the claim to
     * @return true if the claim is unclaimed
     */
    public static boolean displayClaim(String index, ProtectedRegion claim, Stake stake, CommandSender sender) {

        boolean open = false;
        StringBuilder message = new StringBuilder(ChatColor.YELLOW + "# " + index + ": ");
        message.append((stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId());
        if (stake.getClaimName() != null) {
            message.append(ChatColor.LIGHT_PURPLE + " '" + stake.getClaimName() + "'");
        }

        int ownedCode = isRegionOwned(claim);
        if (ownedCode <= 0) {
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
                message.append(ChatColor.YELLOW + " pending for " + ChatColor.GREEN + stake.getStakeName());
            } else {
                message.append(" " + ChatColor.GRAY + "Unclaimed");
                open = true;
            }
        } else {
            message.append(" " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());
        }

        if (claim.getFlag(DefaultFlag.ENTRY) != null && claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
            message.append(ChatColor.RED + " Private!");
        }

        sender.sendMessage(message.toString());
        return open;
    }

    /**
     * Displays one {@code claim}
     * 
     * @param index the index to show for the claim
     * @param claim the claim to display
     * @param player the player to display the claim to
     * @return true if the claim is unclaimed
     */
    public static boolean displayClaim(String index, ProtectedRegion claim, CommandSender sender) {

        boolean open = false;
        StringBuilder message = new StringBuilder(ChatColor.YELLOW + "# " + index + ": ");
        message.append(ChatColor.WHITE + claim.getId() + " ");

        int ownedCode = isRegionOwned(claim);
        if (ownedCode <= 0) {
            message.append(ChatColor.GRAY + "Unclaimed");
            open = true;
        } else {
            message.append(ChatColor.GREEN + claim.getOwners().toUserFriendlyString());
        }

        if (claim.getFlag(DefaultFlag.ENTRY) != null && claim.getFlag(DefaultFlag.ENTRY) == State.DENY) {
            message.append(ChatColor.RED + " Private!");
        }

        sender.sendMessage(message.toString());
        return open;
    }

    public static void doList(StakeAClaimPlugin plugin, CommandContext args, CommandSender sender, int worldIndex, ListType type) throws CommandException {

        World world = getWorld(plugin, args, sender, worldIndex);
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        ArrayList<ProtectedRegion> regions = new ArrayList<ProtectedRegion>();
        StringBuilder errorMessage = new StringBuilder();

        switch (type) {
            case CLAIM:
                String claimID = args.getString(0);
                ProtectedRegion claim = rgMgr.getRegion(claimID);
                if (claim == null) {
                    world = state.listWorld;
                    rgMgr = WGBukkit.getRegionManager(world);
                    claimID = SACUtil.getRegionIDFromList(args, state, 0);
                    claim = rgMgr.getRegion(claimID);
                }
                regions.add(claim);
                errorMessage.append(ChatColor.YELLOW + "There is no claim.");
                break;
            case FREE:
                regions = getUnclaimed(rgMgr, wcfg);
                errorMessage.append(ChatColor.YELLOW + "There are no free claims.");
                break;
            case OWN:
                regions = getOwnedClaims(rgMgr, wcfg);
                errorMessage.append(ChatColor.YELLOW + "There are no owned claims.");
                break;
            case PENDING:
                regions = getPendingClaims(rgMgr, sMgr, wcfg);
                errorMessage.append(ChatColor.YELLOW + "There are no pending stakes.");
                break;
            case USER:
                final String user = args.getString(0);
                if (hasBeenOnServer(plugin, user)) {
                    sender.sendMessage(ChatColor.GREEN + user + ChatColor.YELLOW + " has not played on this server.");
                }
                regions = SACUtil.getOwnedClaims(rgMgr, wcfg, user);
                Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, user);
                if (stake != null) {
                    regions.add(rgMgr.getRegion(stake.getId()));
                }
                errorMessage.append(ChatColor.GREEN + user + ChatColor.YELLOW + " does not have any claims!");
                break;
            case VIP:
                regions = getVIPClaims(rgMgr, sMgr, wcfg);
                errorMessage.append(ChatColor.YELLOW + "There are no claims for " + wcfg.VIPs);
                break;
        }

        LinkedHashMap<Integer, String> regionList = new LinkedHashMap<Integer, String>();

        if (regions.size() < 1) {
            state.regionList = null;
            state.listWorld = null;
            throw new CommandException(errorMessage.toString());
        }

        if (sender instanceof Player && ((Player) sender).getWorld() == world) {
            sender.sendMessage(ChatColor.YELLOW + "Claim list:");
        } else {
            sender.sendMessage(ChatColor.BLUE + world.getName() + ChatColor.YELLOW + " claim list:");
        }
        Integer index = 0;
        for (ProtectedRegion region : regions) {
            regionList.put(index, region.getId());
            index++;
            if (index < 10) {
                SACUtil.displayClaim(index.toString(), region, sMgr.getStake(region), sender);
            }
        }
        if (index > 9) {
            sender.sendMessage(ChatColor.YELLOW + "Showing first 9 claims of " + regions.size() +
                    ". Do " + ChatColor.WHITE + "/tools list" + ChatColor.YELLOW + " to see full list.");
        }
        state.regionList = regionList;
        state.listWorld = world;
    }

    /**
     * Gets the world from args, or falling back to the the current player
     * if the sender is a player, otherwise reporting an error.
     * 
     * @param args the arguments
     * @param sender the sender
     * @param index the index of worldName in {@code args}
     * @return a world
     * @throws CommandException on error
     */
    private static World getWorld(StakeAClaimPlugin plugin, CommandContext args, CommandSender sender, int index) throws CommandException {

        if (args.argsLength() > index) {
            final String worldName = args.getString(index);
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase().equals(worldName.toLowerCase())) {
                    return world;
                }
            }
            throw new CommandException("No world by the exact name '" + worldName + "'");
        } else {
            if (sender instanceof Player) {
                return ((Player) sender).getWorld();
            } else {
                throw new CommandException("Please specify the world.");
            }
        }
    }

    public static String getRegionIDFromList(CommandContext args, PlayerState state, int argsIndex) throws CommandException {
        String regionID = null;
        LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();

        if (state.regionList != null && !state.regionList.isEmpty()) {
            regions = state.regionList;
            int listNumber;
            if (args.argsLength() > argsIndex) {
                listNumber = args.getInteger(argsIndex) - 1;
                if (!regions.containsKey(listNumber)) {
                    throw new CommandException(ChatColor.YELLOW + "That is not a valid list entry number.");
                }
            } else {
                throw new CommandException(ChatColor.YELLOW + "Please include the list entry number.");
            }
            regionID = regions.get(listNumber);
        } else {
            throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
        }

        return regionID;
    }

    /**
     * Resets regions with default entry flag owned by (@code player)
     * 
     * @param rgMgr the region manager to work with
     * @param sMgr the stake manager to work with
     * @param player the player to reset entries for
     */
    public static void resetEntryRegions(RegionManager rgMgr, StakeManager sMgr, Player player) {

        final Map<String, Stake> stakes = sMgr.getStakes();
        ProtectedRegion region;
        boolean save = false;

        for (Stake stake : stakes.values()) {
            if (stake.getDefaultEntry() != null) {
                region = rgMgr.getRegion(stake.getId());
                if (region.getOwners().contains(player.getName().toLowerCase())) {
                    if (region.getFlag(DefaultFlag.ENTRY) != stake.getDefaultEntry()) {
                        region.setFlag(DefaultFlag.ENTRY, stake.getDefaultEntry());
                        save = true;
                    }
                }
            }
        }

        if (save) {
            try {
                rgMgr.save();
            } catch (ProtectionDatabaseException e) {
            }
        }
    }

    /**
     * Check one region for owners. 
     * int > 1, has multiple owners. 
     * int < 0, has members but no owners.
     * 
     * @param region the region to check owners of
     * @return int error code / modified owners count
     */
    public static int isRegionOwned(ProtectedRegion region) {

        int owners = region.getOwners().size();
        if (owners == 1) {
            return owners + region.getOwners().getGroups().size();
        }
        if (owners == 0) {
            return owners - region.getMembers().size();
        }
        return owners;
    }

    public static boolean hasBeenOnServer(StakeAClaimPlugin plugin, String playerName) {
        boolean onlinePlayer = false;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (playerName.equalsIgnoreCase(player.getName())) {
                onlinePlayer = true;
            }
        }
        boolean offlinePlayer = plugin.getServer().getOfflinePlayer(playerName).hasPlayedBefore();

        return (!onlinePlayer && !offlinePlayer);
    }

    /**
     *  Warp {@code player} to {@code claim}
     * 
     * @param claim the claim to warp to
     * @param stake stake for the claim
     * @param player the player to warp
     * @param forceSpawn toggle spawn only warp
     * @return claim warped to, will be null if you did not warp
     */
    public static ProtectedRegion warpTo(ProtectedRegion claim, Stake stake, Player player, boolean forceSpawn){

        if (claim.getFlag(DefaultFlag.TELE_LOC)!= null && !forceSpawn) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.TELE_LOC)));
            player.sendMessage(ChatColor.YELLOW + "Gone to:");
            displayClaim("", claim, stake, player);
        } else if (claim.getFlag(DefaultFlag.SPAWN_LOC)!= null) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.SPAWN_LOC)));
            player.sendMessage(ChatColor.YELLOW + "Gone to spawn of:");
            displayClaim("", claim, stake, player);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No warp set for " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId() + "!");
            return null;
        }

        return claim;
    }

    public static void doGoto(StakeAClaimPlugin plugin, CommandContext args, CommandSender sender, boolean spawn) throws CommandException {

        if (args.argsLength() == 0) {
            if (gotoRememberedWarp(plugin, sender, spawn)) {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                sender.sendMessage(ChatColor.RED + "/tools " + args.getCommand() + " <list entry #> or <region ID>");
            }
            return;
        }

        final Player player = plugin.checkPlayer(sender);
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();

        final World world = player.getWorld();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        String claimID = args.getString(0);
        ProtectedRegion claim = rgMgr.getRegion(claimID);
        if (claim == null) {
            claimID = getRegionIDFromList(args, state, 0);
            claim = rgMgr.getRegion(claimID);
        }

        state.lastWarp = SACUtil.warpTo(claim, sMgr.getStake(claim), player, spawn);
        if (state.lastWarp == null) {
            state.warpWorld = null;
        } else {
            state.warpWorld = world;
        }

    }

    public static boolean gotoRememberedWarp(StakeAClaimPlugin plugin, CommandSender sender, boolean spawn) throws CommandException {

        final Player player = plugin.checkPlayer(sender);
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();

        if (state.lastWarp != null && state.warpWorld != null) {
            final WorldConfig warpwcfg = cfg.get(state.warpWorld);
            if (!warpwcfg.useStakes) {
                throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
            }
            final StakeManager warpsMgr = plugin.getGlobalStakeManager().get(state.warpWorld);

            state.lastWarp = SACUtil.warpTo(state.lastWarp, warpsMgr.getStake(state.lastWarp), player, spawn);
            if (state.lastWarp == null) {
                state.warpWorld = null;
            }
            return false;
        }
        throw new CommandException(ChatColor.RED + "Too few arguments.");
    }

    /**
     *  Create spawn locations for all claims in {@code world}
     * 
     * @param plugin the SAC plugin
     * @param world the world the claim is in
     * @return int count of spawns made, -1 if there is no rgMgr for this world
     */
    public static int makeSpawns(StakeAClaimPlugin plugin, World world){
        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            return -1;
        }

        ConfigManager cfg = plugin.getGlobalManager();
        WorldConfig wcfg = cfg.get(world);

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        Matcher regexMat;
        int claims = 0;

        for (ProtectedRegion region : regions.values()) {
            regexMat = regexPat.matcher(region.getId());
            if (regexMat.find()) {
                makeSpawn(region, world);
                claims++;
            }
        }
        
        return claims;
    }

    /**
     *  Create spawn location for {@code claim} in {@code world}
     * 
     * @param claim the claim to create a spawn for
     * @param world the world the claim is in
     * @throws CommandException 
     */
    @SuppressWarnings("deprecation")
    public static void makeSpawn(ProtectedRegion claim, World world){
        Vector center = BlockVector.getMidpoint(claim.getMaximumPoint(),claim.getMinimumPoint());
        for (int i = 255; i >= 0; i--) {
            if (world.getBlockTypeIdAt(center.getBlockX(), i, center.getBlockZ()) != 0) {
                center = new Vector(center.getBlockX()+.5, i+1, center.getBlockZ()+.5);
                break;
            }
            if (i == 0) {
                center = new Vector(center.getBlockX()+.5, 100, center.getBlockZ()+.5);
            }
        }

        claim.setFlag(DefaultFlag.SPAWN_LOC, new com.sk89q.worldedit.Location(BukkitUtil.getLocalWorld(world), center));
        
    }

}
