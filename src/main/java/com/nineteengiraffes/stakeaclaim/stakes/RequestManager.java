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

package com.nineteengiraffes.stakeaclaim.stakes;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest.Access;
import com.nineteengiraffes.stakeaclaim.stakes.StakeRequest.Status;
import com.nineteengiraffes.stakeaclaim.stakes.databases.StakeDatabase;
import com.nineteengiraffes.stakeaclaim.stakes.databases.StakeDatabaseException;

/**
 * An class for getting, setting, and looking up requests.
 */
public class RequestManager {

    /**
     * The request loader to use.
     */
    protected StakeDatabase loader;

    /**
     * List of stake requests.
     */
    private Map<Long, StakeRequest> requests;

    /**
     * Construct the object.
     *
     * @param loader The loader for this request
     */
    public RequestManager(StakeDatabase loader) {
        this.loader = loader;
        requests = new TreeMap<Long, StakeRequest>();
    }

    /**
     * Load the list of requests. If the requests do not load properly, then
     * the existing list should be used (as stored previously).
     *
     * @throws StakeDatabaseException when an error occurs
     */
    public void load() throws StakeDatabaseException {
        loader.load(this);
    }

    /**
     * Save the list of requests.
     *
     * @throws StakeDatabaseException when an error occurs while saving
     */
    public void save() throws StakeDatabaseException {
        loader.save(this);
    }

    /**
     * Get a map of protected requests. Use one of the request manager methods
     * if possible if working with requests.
     *
     * @return map of requests, with keys being request ID numbers 
     */
    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

    /**
     * Set a list of requests. Keys should be request IDs
     *
     * @param requests map of requests
     */
     public void setRequests(Map<Long, StakeRequest> requests) {
         this.requests = new TreeMap<Long, StakeRequest>(requests);
     }

    /**
     * Adds a request.
     *
     * @param request request to add
     */
     public void addRequest(StakeRequest request) {
         requests.put(request.getRequestID(), request);
     }

    /**
     * Get a request by its ID number.
     *
     * @param requestID id number of the request
     * @return request or null if it doesn't exist
     */
    public StakeRequest getRequest(long requestID) {
        return getRequests().get(requestID);
    }

    /**
     * Removes a request.
     *
     * @param requestID id number of the request
     */
    public void removeRequest(long requestID) {
        requests.remove(requestID);
    }

    /**
     * Get a set of requests for {@code regionID) that are (@code status)
     * 
     * @param regionID the name of the region whose requests to get
     * @param status the status of the requests
     * @return list of StakeRequest
     */
    public ArrayList<StakeRequest> getRegionStatusRequests(String regionID, Status status) {
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getRegionID().equals(regionID.toLowerCase()) && request.getStatus() == status) {
                requestList.add(request);
            }
        }

        return requestList;
    }

    /**
     * Get a set of requests by {@code player) that are (@code status)
     * 
     * @param player the player whose requests to get
     * @param status the status of the requests
     * @return list of StakeRequest
     */
    public ArrayList<StakeRequest> getPlayerStatusRequests(Player player, Status status) {
        return getPlayerStatusRequests(player.getName().toLowerCase(), status);
    }

    /**
     * Get a set of requests by {@code playerName) that are (@code status)
     * 
     * @param playerName the name of the player whose requests to get
     * @param status the status of the requests
     * @return list of StakeRequest
     */
    public ArrayList<StakeRequest> getPlayerStatusRequests(String playerName, Status status) {
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getPlayerName().equals(playerName) && request.getStatus() == status) {
                requestList.add(request);
            }
        }

        return requestList;
    }

    /**
     * Get a set of accepted requests by {@code player) that are (@code access)
     * 
     * @param player the player whose requests to get
     * @param access the access of the requests
     * @return request set
     */
    public ArrayList<StakeRequest> getPlayerAccessRequests(Player player, Access access) {
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getPlayerName().equals(player.getName().toLowerCase()) && 
                    request.getStatus() == Status.ACCEPTED &&
                    request.getAccess() == access) {
                requestList.add(request);
            }
        }

        return requestList;
    }

    /**
     * Get a set of accepted requests by {@code player) that have an access node
     * 
     * @param player the player whose requests to get
     * @return list of StakeRequest
     */
    public ArrayList<StakeRequest> getPlayerAccessRequests(Player player) {
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getPlayerName().equals(player.getName().toLowerCase()) && 
                    request.getStatus() == Status.ACCEPTED &&
                    request.getAccess() != null) {
                requestList.add(request);
            }
        }

        return requestList;
    }

    /**
     * Get a set of requests with status {@code status)
     *
     * @param status the status to get requests with
     * @return list of StakeRequest
     */
    public ArrayList<StakeRequest> getStatusRequests(Status status) {
        ArrayList<StakeRequest> requestList = new ArrayList<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getStatus() == status) {
                requestList.add(request);
            }
        }

        return requestList;
    }

    /**
     * Get the number of requests.
     *
     * @return number of requests
     */
    public int size() {
        return requests.size();
    }
}
