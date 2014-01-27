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

package com.nineteengiraffes.stakeaclaim.bukkit;

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

import com.nineteengiraffes.stakeaclaim.stakes.RequestManager;
import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest;
import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest.Status;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.GlobalRegionManager;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SACUtil {

//    private SACUtil() {
//
//    }

    // Get request, fix inconsistencies if needed
    /**
     * Fix one region's requests:
     * Add missing requests
     * Fix non matching requests
     * Fix duplicate requests
     * 
     * Must have just one owner!
     * 
     * @param rqMgr the request manager to work with
     * @param region the region to check
     * @param useReclaimed boolean config value
     * @return request for the region, will return null on error
     */
    public static StakeRequest fixRegionsRequests(RequestManager rqMgr, ProtectedRegion region, boolean useReclaimed) {

        if (region.getOwners().getPlayers().size() != 1) {
            return null;
        }

        StakeRequest newRequest;
        final ArrayList<StakeRequest> requests = rqMgr.getRegionStatusRequests(region.getId(), Status.ACCEPTED);

        // Remove requests by wrong owner
        for (int i = requests.size() - 1; i >= 0; i--) {
            if (!region.getOwners().contains(requests.get(i).getPlayerName())) {
                reclaim(requests.get(i), useReclaimed);
                requests.remove(i);
            }
        }

        // Add a missing request
        if (requests.size() == 0) {
            newRequest = new StakeRequest(region.getId(), region.getOwners().getPlayers().toArray(new String[0])[0]);
            newRequest.setStatus(Status.ACCEPTED);
            rqMgr.addRequest(newRequest);
            requests.add(newRequest);

        // Remove duplicate requests
        } else if (requests.size() > 1) {
            newRequest = requests.get(requests.size() - 1);
            for (int i = requests.size() - 2; i >= 0; i--) {

                // Save oldest
                if (requests.get(i).getRequestID() < newRequest.getRequestID()) {
                    reclaim(newRequest, useReclaimed);
                    requests.remove(requests.indexOf(newRequest));
                    newRequest = requests.get(i);
                } else {
                    reclaim(requests.get(i), useReclaimed);
                    requests.remove(i);
                }
            }
        }

        // Did it get fixed?
        if (requests.size() != 1) {
            return null;
        }

        return requests.get(0);
    }

    /**
     * Get pending request for (@code regionID)
     * fixes duplicate requests
     * 
     * @param rqMgr the request manager to work with
     * @param regionID the region to get the request for
     * @return pending request, will return null if there is none
     */
    public static StakeRequest getRegionPendingRequest(RequestManager rqMgr, String regionID) {

        ArrayList<StakeRequest> requestList = rqMgr.getRegionStatusRequests(regionID, Status.PENDING);
        StakeRequest oldestRequest;

        if (requestList.size() < 1) {
            return null;
        } else {
            oldestRequest = requestList.get(0);
            for (int i = 1; i < requestList.size(); i++) {

                if (requestList.get(i).getRequestID() < oldestRequest.getRequestID()) {
                    oldestRequest.setStatus(Status.UNSTAKED);
                    oldestRequest = requestList.get(i);
                } else {
                    requestList.get(i).setStatus(Status.UNSTAKED);
                }
            }
        }

        return oldestRequest;
    }

    // Get request(s) or regions, query only
    /**
     * Get a list of requests for regions owned by (@code player)
     * 
     * @param rqMgr the request manager to work with
     * @param rgMgr the region manager to work with
     * @param player the to get the requests for
     * @param useReclaimed boolean config value
     * @return list of accepted requests for the player
     */
    public static ArrayList<StakeRequest> getAcceptedRequests(RequestManager rqMgr, RegionManager rgMgr, Player player, boolean useReclaimed) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();
        StakeRequest request;

        for (ProtectedRegion region : regions.values()) {
            if (isRegionOwned(region) == 1) {
                if (region.getOwners().contains(player.getName().toLowerCase())) {
                    request = fixRegionsRequests(rqMgr, region, useReclaimed);
                    if (request != null) {
                        requestList.add(request);
                    }
                }
            }
        }

        return requestList;
    }

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
            if (isRegionOwned(region) == 1) {
                if (region.getOwners().contains(playerName)) {
                    regionList.add(region);
                }
            }
        }
        return regionList;
    }

    /**
     * Get pending request for (@code player)
     * 
     * @param rqMgr the request manager to work with
     * @param player the player to get the request for
     * @return pending request, will return null if there is none
     */
    public static StakeRequest getPlayerPendingRequest(RequestManager rqMgr, Player player) {
        return getPlayerPendingRequest(rqMgr, player.getName().toLowerCase());
    }

    /**
     * Get pending request for (@code playerName)
     * 
     * @param rqMgr the request manager to work with
     * @param playerName name of the player to get the request for
     * @return pending request, will return null if there is none
     */
    public static StakeRequest getPlayerPendingRequest(RequestManager rqMgr, String playerName) {

        ArrayList<StakeRequest> requestList = rqMgr.getPlayerStatusRequests(playerName, Status.PENDING);
        StakeRequest oldestRequest;

        if (requestList.size() < 1) {
            return null;
        } else {
            oldestRequest = requestList.get(0);
            for (int i = 1; i < requestList.size(); i++) {

                if (requestList.get(i).getRequestID() < oldestRequest.getRequestID()) {
                    oldestRequest = requestList.get(i);
                }
            }
        }

        return oldestRequest;
    }

    // Utilities
    /**
     * Reclaim region and request
     * 
     * @param request the request to reclaim
     * @param region the region to reclaim
     * @param useReclaimed boolean config value
     */
    public static void reclaim(StakeRequest request, ProtectedRegion region, boolean useReclaimed) {
        reclaim(request, useReclaimed);
        region.getOwners().getPlayers().clear();
        region.getOwners().getGroups().clear();
        region.getMembers().getPlayers().clear();
        region.getMembers().getGroups().clear();
        region.setFlag(DefaultFlag.ENTRY, null);
    }

    /**
     * Reclaim request
     * 
     * @param request the request to reclaim
     * @param useReclaimed boolean config value
     */
    public static void reclaim(StakeRequest request, boolean useReclaimed) {
        if (useReclaimed) {
            request.setStatus(Status.RECLAIMED);
        } else {
            request.setStatus(Status.UNSTAKED);
        }
        request.setAccess(null);
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
     * Get the single claim the player is in
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
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void addFlags() {
        try {
            Field field = DefaultFlag.class.getDeclaredField("flagsList");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
            field.setAccessible(true);

            List wgFlags = new ArrayList(Arrays.asList(DefaultFlag.getFlags()));
            wgFlags.add(SACFlags.RECLAIMED);
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
    
}
