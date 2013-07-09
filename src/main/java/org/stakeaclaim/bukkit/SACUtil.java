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

package org.stakeaclaim.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.Player;

//import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.stakeaclaim.bukkit.ConfigurationManager;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.bukkit.WorldConfiguration;
import org.stakeaclaim.stakes.RequestManager;
import org.stakeaclaim.stakes.StakeRequest;
import org.stakeaclaim.stakes.StakeRequest.Status;
import org.stakeaclaim.stakes.databases.StakeDatabaseException;

public class SACUtil {

    private SACUtil()  {

    }

    /**
     * Fix requests for all worlds
     * Deals with players added/removed via WG
     * or manual edits to regions or requests
     */
    public static void fixRquests(StakeAClaimPlugin plugin) {

        final List<World> worlds = plugin.getServer().getWorlds();
        final ConfigurationManager cfg = plugin.getGlobalStateManager();

        WorldConfiguration wcfg;
        RegionManager rgMgr;
        RequestManager rqMgr;

        for (World world : worlds) {

            wcfg = cfg.get(world);
            rgMgr = WGBukkit.getRegionManager(world);
            if (wcfg.useRequests && rgMgr != null) {
                rqMgr = plugin.getGlobalRequestManager().get(world);

                checkRequests(rqMgr, rgMgr, wcfg.useReclaimed);
                checkRegions(rqMgr, rgMgr, wcfg.useReclaimed);

            }
        }
    }

    /**
     * Check all requests:
     * Fix for non existing regions
     * Fix for unowned regions
     * 
     * @param rqMgr the request manager to work with
     * @param rgMgr the region manager to work with
     * @param useReclaimed boolean config value
     */
    public static void checkRequests(RequestManager rqMgr, RegionManager rgMgr, boolean useReclaimed) {

        final Map<Long, StakeRequest> requests = rqMgr.getRequests();
        ProtectedRegion region;

        for (StakeRequest request : requests.values()) {
            if (request.getStatus() != Status.NOREGION) {
                if (!rgMgr.hasRegion(request.getRegionID())) {
                    request.setStatus(Status.NOREGION);
                } else {
                    if (request.getStatus() == Status.ACCEPTED) {
                        region = rgMgr.getRegion(request.getRegionID());
                        if (region.getOwners().size() < 1) {
                            reclaim(request, region, useReclaimed);
                        }
                    }
                }
            }
        }
        save(rqMgr);
        save(rgMgr);
    }

    /**
     * Reclaim request
     * 
     * @param request the request to reclaim
     * @param useReclaimed boolean config value
     */
    public static void reclaim(StakeRequest request, ProtectedRegion region, boolean useReclaimed) {
        if (useReclaimed) {
            request.setStatus(Status.RECLAIMED);
        } else {
            request.setStatus(Status.UNSTAKED);
        }
        request.setAccess(null);
        region.getOwners().getPlayers().clear();
        region.getOwners().getGroups().clear();
        region.getMembers().getPlayers().clear();
        region.getMembers().getGroups().clear();
        region.setFlag(DefaultFlag.ENTRY, null);
    }

    /**
     * Check all regions:
     * Flag if: has muiltiple owners
     * Flag if: has members but no owners
     * Add missing requests
     * Fix non matching requests
     * Fix duplicate requests
     * 
     * @param rqMgr the request manager to work with
     * @param rgMgr the region manager to work with
     * @param useReclaimed boolean config value
     */
    public static void checkRegions(RequestManager rqMgr, RegionManager rgMgr, boolean useReclaimed) {

        final Map<String, ProtectedRegion> regions = rgMgr.getRegions();
        int ownedCode;

        for (ProtectedRegion region : regions.values()) {
            ownedCode = isRegionOwned(region);
            if (ownedCode == 0) {
                continue;
            } else if (ownedCode == 1) {
                fixRegionsRequests(rqMgr, region, useReclaimed);
            } else if (ownedCode > 1) {
                // Muiltiple owners, add some catch
            } else if (ownedCode < 0) {
                // Members w/o owner, add some catch
            }
        }
        save(rqMgr);
    }

    /**
     * Check one region for owners
     * int > 1 has muiltiple owners
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
        ArrayList<StakeRequest> requests = rqMgr.getRegionStatusRequests(region.getId(), Status.ACCEPTED);

        // Remove requests by wrong owner
        for (StakeRequest request : requests) {
            if (!region.getOwners().contains(request.getPlayerName())) {
                if (useReclaimed) {
                    request.setStatus(Status.RECLAIMED);
                } else {
                    request.setStatus(Status.UNSTAKED);
                }
                request.setAccess(null);
            }
        }
        requests = rqMgr.getRegionStatusRequests(region.getId(), Status.ACCEPTED);

        // Add a missing request
        if (requests.size() == 0) {
            newRequest = new StakeRequest(region.getId(), region.getOwners().getPlayers().toArray(new String[0])[0]);
            newRequest.setStatus(Status.ACCEPTED);
            rqMgr.addRequest(newRequest);

        // Remove duplicate requests
        } else if (requests.size() > 1) {
            newRequest = requests.get(0);
            for (int i = 1; i < requests.size(); i++) {

                // Save oldest
                if (requests.get(i).getRequestID() < newRequest.getRequestID()) {
                    if (useReclaimed) {
                        newRequest.setStatus(Status.RECLAIMED);
                    } else {
                        newRequest.setStatus(Status.UNSTAKED);
                    }
                    newRequest.setAccess(null);
                    newRequest = requests.get(i);
                } else {
                    if (useReclaimed) {
                        requests.get(i).setStatus(Status.RECLAIMED);
                    } else {
                        requests.get(i).setStatus(Status.UNSTAKED);
                    }
                    requests.get(i).setAccess(null);
                }
            }
        }
        requests = rqMgr.getRegionStatusRequests(region.getId(), Status.ACCEPTED);

        // Did it get fixed?
        if (requests.size() != 1) {
            return null;
        }

        return requests.get(0);
    }

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
        save(rqMgr);
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
     * Get pending request for (@code regionID)
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
    
                // Save oldest
                if (requestList.get(i).getRequestID() < oldestRequest.getRequestID()) {
                    oldestRequest.setStatus(Status.UNSTAKED);
                    oldestRequest = requestList.get(i);
                } else {
                    requestList.get(i).setStatus(Status.UNSTAKED);
                }
            }
            save(rqMgr);
        }

        return oldestRequest;
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
    
                // Save oldest
                if (requestList.get(i).getRequestID() < oldestRequest.getRequestID()) {
//                    oldestRequest.setStatus(Status.UNSTAKED);
                    oldestRequest = requestList.get(i);
//                } else {
//                    requestList.get(i).setStatus(Status.UNSTAKED);
                }
            }
            save(rqMgr);
        }

        return oldestRequest;
    }

    public static void save(RegionManager rgMgr) {
        try {
            rgMgr.save();
        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write regions: " + e.getMessage());
        }
    }

    public static void save(RequestManager rqMgr) {
        try {
            rqMgr.save();
        } catch (StakeDatabaseException e) {
//            throw new CommandException("Failed to write requests: " + e.getMessage());
        }
    }
}