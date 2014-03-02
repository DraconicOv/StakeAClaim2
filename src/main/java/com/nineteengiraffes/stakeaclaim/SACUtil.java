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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SACUtil {

    // Get regions
    /**
     * Get a list of regions owned by (@code player)
     * 
     * @param rgMgr the region manager to work with
     * @param player the player to get the regions for
     * @return list of regions owned by the player
     */
    public static ArrayList<ProtectedRegion> getOwnedRegions(RegionManager rgMgr, Player player) {
        return getOwnedRegions(rgMgr, player.getName().toLowerCase());
    }

    /**
     * Get a list of regions owned by (@code playerName)
     * 
     * @param rgMgr the region manager to work with
     * @param playerName the name of the player to get the regions for
     * @return list of regions owned by the player
     */
    public static ArrayList<ProtectedRegion> getOwnedRegions(RegionManager rgMgr, String playerName) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();

        for (ProtectedRegion region : regions.values()) {
            if (isRegionOwned(region) == 1 && region.getOwners().contains(playerName)) {
                regionList.add(region);
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
     * @return the claim at ({@code vector}, returns null if there are no claims there, or more than one
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
     * Get a list of pending stakes
     * 
     * @param rgMgr the region manager to work with
     * @param sMgr the stake manager to work with
     * @return list of pending stakes
     */
    public static ArrayList<Stake> getPendingStakes(RegionManager rgMgr, StakeManager sMgr) {

        final Map<String, Stake> stakes = sMgr.getStakes();
        ArrayList<Stake> stakeList = new ArrayList<Stake>();
        ProtectedRegion claim;
        boolean save = false;

        for (Stake stake : stakes.values()) {
            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
                claim = rgMgr.getRegion(stake.getId());
                if (claim == null) {
                    stakes.remove(stake.getId());
                    save = true;
                    continue;
                }
                int ownedCode = SACUtil.isRegionOwned(claim);
                if (ownedCode >= 1) {
                    stake.setStatus(null);
                    stake.setStakeName(null);
                    save = true;
                } else {
                    stakeList.add(stake);
                }
            }
        }
        if (save) sMgr.save();
        return stakeList;
    }

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
        ProtectedRegion claim;
        boolean save = false;

        for (Stake stake : stakes.values()) {
            if (stake.getStakeName() != null && stake.getStakeName().equals(playerName) &&
                    stake.getStatus() != null && stake.getStatus() == Status.PENDING) {
                claim = rgMgr.getRegion(stake.getId());
                if (claim == null) {
                    stakes.remove(stake.getId());
                    save = true;
                    continue;
                }
                int ownedCode = SACUtil.isRegionOwned(claim);
                if (ownedCode >= 1) {
                    stake.setStatus(null);
                    stake.setStakeName(null);
                    save = true;
                } else {
                    stakeList.add(stake);
                }
            }
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
     * Reclaim a region and reset its stake
     * 
     * @param stake the stake to reset
     * @param region the region to reclaim
     * @param useReclaimed boolean config value
     */
    public static void reclaim(Stake stake, ProtectedRegion region, boolean useReclaimed) {
        region.getOwners().getPlayers().clear();
        region.getOwners().getGroups().clear();
        region.getMembers().getPlayers().clear();
        region.getMembers().getGroups().clear();
        region.setFlag(DefaultFlag.TELE_LOC,null);
        region.setFlag(DefaultFlag.ENTRY,null);
        stake.setStatus(null);
        stake.setStakeName(null);
        stake.setDefaultEntry(null);
        stake.setClaimName(null);
        stake.setVIP(false);
        stake.setRecalimed(useReclaimed);
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

    /**
     *  Warp {@code player} to {@code claim}
     * 
     * @param claim the claim to warp to
     * @param state players state so save last warp
     * @param player the player to warp
     * @param forceSpawn toggle spawn only warp
     * @param claimName the custom name of the destination claim
     * @throws CommandException
     */
    public static void warpTo(ProtectedRegion claim, PlayerState state, Player player, boolean forceSpawn, String claimName) throws CommandException {

        if (claim.getFlag(DefaultFlag.TELE_LOC)!= null && !forceSpawn) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.TELE_LOC)));
            state.lastWarp = claim;
            throw new CommandException(ChatColor.YELLOW + "Gone to " + 
                    (claimName == null ? (ChatColor.WHITE + claim.getId()) : (ChatColor.LIGHT_PURPLE + claimName)) + 
                    ChatColor.YELLOW + " By: " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());

        } else if (claim.getFlag(DefaultFlag.SPAWN_LOC)!= null) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.SPAWN_LOC)));
            state.lastWarp = claim;
            if (claim.getOwners().size() == 0) {
                throw new CommandException(ChatColor.YELLOW + "Gone to " + ChatColor.WHITE + claim.getId() + 
                        ChatColor.YELLOW + "'s spawn. By: " + ChatColor.GRAY + "Unclaimed");
            } else {
                throw new CommandException(ChatColor.YELLOW + "Gone to " + ChatColor.WHITE + claim.getId() + 
                        ChatColor.YELLOW + "'s spawn. By: " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());
            }

        } else {
            state.lastWarp = null;
            throw new CommandException("No warp set for this claim!");
        }
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
