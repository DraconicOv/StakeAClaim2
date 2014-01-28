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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
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
     * Get a list of pending regions for (@code player)
     * 
     * @param rgMgr the region manager to work with
     * @param player the player to get the regions for
     * @return list of pending regions for the player
     */
    public static ArrayList<ProtectedRegion> getPendingRegions(RegionManager rgMgr, Player player) {
        return getPendingRegions(rgMgr, player.getName().toLowerCase());
    }

    /**
     * Get a list of pending regions for (@code playerName)
     * 
     * @param rgMgr the region manager to work with
     * @param playerName the name of the player to get the regions for
     * @return list of pending regions for the player
     */
    public static ArrayList<ProtectedRegion> getPendingRegions(RegionManager rgMgr, String playerName) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();

        for (ProtectedRegion region : regions.values()) {
            if (region.getFlag(SACFlags.REQUEST_NAME) != null && region.getFlag(SACFlags.REQUEST_NAME).equals(playerName) &&
                    region.getFlag(SACFlags.PENDING) != null && region.getFlag(SACFlags.PENDING) == true) {
                regionList.add(region);
            }
        }
        return regionList;
    }

    /**
     * Get a list of pending regions
     * 
     * @param rgMgr the region manager to work with
     * @return list of pending regions
     */
    public static ArrayList<ProtectedRegion> getPendingRegions(RegionManager rgMgr) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<ProtectedRegion> regionList = new ArrayList<ProtectedRegion>();

        for (ProtectedRegion region : regions.values()) {
            if (region.getFlag(SACFlags.PENDING) != null && region.getFlag(SACFlags.REQUEST_NAME) != null && region.getFlag(SACFlags.PENDING) == true) {
                regionList.add(region);
            }
        }
        return regionList;
    }

    // Utilities
    /**
     * Resets regions with default entry flag owned by (@code player)
     * 
     * @param rgMgr the region manager to work with
     * @param player the player to reset entries for
     * @return list of entry regions for the player
     */
    public static void resetEntryRegions(RegionManager rgMgr, Player player) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();

        for (ProtectedRegion region : regions.values()) {
            if (region.getOwners().contains(player.getName().toLowerCase()) && region.getFlag(SACFlags.ENTRY_DEFAULT) != null && isRegionOwned(region) == 1) {
                region.setFlag(DefaultFlag.ENTRY, region.getFlag(SACFlags.ENTRY_DEFAULT));
            }
        }
    }

    /**
     * Reclaim a region
     * 
     * @param region the region to reclaim
     * @param useReclaimed boolean config value
     */
    public static void reclaim(ProtectedRegion region, boolean useReclaimed) {
        region.getOwners().getPlayers().clear();
        region.getOwners().getGroups().clear();
        region.getMembers().getPlayers().clear();
        region.getMembers().getGroups().clear();
        region.setFlag(SACFlags.REQUEST_STATUS,null);
        region.setFlag(SACFlags.REQUEST_NAME,null);
        region.setFlag(SACFlags.PENDING,null);
        region.setFlag(SACFlags.ENTRY_DEFAULT,null);
        region.setFlag(DefaultFlag.ENTRY,null);
        if (useReclaimed) {
            region.setFlag(SACFlags.RECLAIMED,true);
        }
    }

    /**
     * Check one region for owners
     * int > 1 has multiple owners
     * int < 0 has members but no owners
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

        final ConfigurationManager cfg = plugin.getGlobalStateManager();
        final WorldConfiguration wcfg = cfg.get(world);
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
    public static ProtectedRegion getClaimAtPoint(RegionManager rgMgr, WorldConfiguration wcfg, Vector vector) {

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

    /**
     * Add in SAC's custom flags
     * 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addFlags() {
        try {
            Field field = DefaultFlag.class.getDeclaredField("flagsList");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
            field.setAccessible(true);

            List wgFlags = new ArrayList(Arrays.asList(DefaultFlag.getFlags()));

            // Flags
            wgFlags.add(SACFlags.RECLAIMED);
            wgFlags.add(SACFlags.PENDING);
            wgFlags.add(SACFlags.REQUEST_NAME);
            wgFlags.add(SACFlags.REQUEST_STATUS);
            wgFlags.add(SACFlags.ENTRY_DEFAULT);
            wgFlags.add(SACFlags.CLAIM_WARP_NAME);

            Flag<?>[] newFlags = new Flag[wgFlags.size()];
            wgFlags.toArray(newFlags);
            field.set(null, newFlags);

            Field grmField = WorldGuardPlugin.class.getDeclaredField("globalRegionManager");
            grmField.setAccessible(true);
            GlobalRegionManager gRgMr = (GlobalRegionManager) grmField.get(Bukkit.getPluginManager().getPlugin("WorldGuard"));
            gRgMr.preload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Warp {@code player} to {@code claim}
     * 
     * @param claim the claim to warp to
     * @param state players state so save last warp
     * @param player the player to warp
     * @param forceSpawn toggle spawn only warp
     * @throws CommandException
     */
    public static void warpTo(ProtectedRegion claim, PlayerState state, Player player, boolean forceSpawn) throws CommandException {

        if (claim.getFlag(DefaultFlag.TELE_LOC)!= null && !forceSpawn) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.TELE_LOC)));
            state.lastWarp = claim;
            throw new CommandException(ChatColor.YELLOW + "Gone to " + 
                    (claim.getFlag(SACFlags.CLAIM_WARP_NAME) == null ? (ChatColor.WHITE + claim.getId()) : (ChatColor.LIGHT_PURPLE + claim.getFlag(SACFlags.CLAIM_WARP_NAME))) + 
                    ChatColor.YELLOW + " By: " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());

        } else if (claim.getFlag(DefaultFlag.SPAWN_LOC)!= null) {
            player.teleport(BukkitUtil.toLocation(claim.getFlag(DefaultFlag.SPAWN_LOC)));
            state.lastWarp = claim;
            throw new CommandException(ChatColor.YELLOW + "Gone to " + ChatColor.WHITE + claim.getId() + 
                    ChatColor.YELLOW + "'s spawn. By: " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());

        } else {
            state.lastWarp = null;
            throw new CommandException("No warp set for this claim!");
        }
    }

}
