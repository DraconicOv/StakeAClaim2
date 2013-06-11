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

package org.stakeaclaim.stakes;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bukkit.entity.Player;

//import org.stakeaclaim.LocalPlayer;
//import org.stakeaclaim.stakes.ApplicableRequestSet;
import org.stakeaclaim.stakes.databases.StakeDatabaseException;
import org.stakeaclaim.stakes.databases.StakeDatabase;
//import org.stakeaclaim.stakes.StakeRequest;
import org.stakeaclaim.stakes.StakeRequest.Access;
import org.stakeaclaim.stakes.StakeRequest.Status;

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
     * Get a set of requests for region {@code name) isRegion == true
     * Get a set of requests requested by player {@code name) isRegion == false
     *
     * @param name the name of the player or region whose requests to get
     * @return request set
     */
    public ApplicableRequestSet getApplicableRequests(String name, boolean isRegion) {
        TreeSet<StakeRequest> appRequests = new TreeSet<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (isRegion) {
                if (request.getRegionID() == name.toLowerCase()) {
                    appRequests.add(request);
                }
            } else {
                if (request.getPlayerName() == name.toLowerCase()) {
                    appRequests.add(request);
                }
            }
        }

        return new ApplicableRequestSet(appRequests);
    }

    /**
     * Get a set of requests requested by {@code player)
     *
     * @param player the player whose requests to get
     * @return request set
     */
    public ApplicableRequestSet getApplicableRequests(Player player) {
        TreeSet<StakeRequest> appRequests = new TreeSet<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getPlayerName() == player.getName().toLowerCase()) {
                appRequests.add(request);
            }
        }

        return new ApplicableRequestSet(appRequests);
    }

    /**
     * Get a set of requests with status {@code status)
     *
     * @param status the status to get requests with
     * @return request set
     */
    public ApplicableRequestSet getApplicableRequests(Status status) {
        TreeSet<StakeRequest> appRequests = new TreeSet<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getStatus() == status) {
                appRequests.add(request);
            }
        }

        return new ApplicableRequestSet(appRequests);
    }

    /**
     * Get a set of requests with access state of {@code status)
     *
     * @param status the access state to get requests with
     * @return request set
     */
    public ApplicableRequestSet getApplicableRequests(Access access) {
        TreeSet<StakeRequest> appRequests = new TreeSet<StakeRequest>();

        for (StakeRequest request : requests.values()) {
            if (request.getAccess() == access) {
                appRequests.add(request);
            }
        }

        return new ApplicableRequestSet(appRequests);
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
