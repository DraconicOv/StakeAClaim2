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

import java.util.Collection;
import java.util.Iterator;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;

import org.stakeaclaim.stakes.StakeRequest;
import org.stakeaclaim.stakes.StakeRequest.Status;

/**
 * Represents a set of requests
 */
public class ApplicableRequestSet implements Iterable<StakeRequest> {

    private Collection<StakeRequest> applicable;

    /**
     * Construct the object.
     * 
     * @param applicable The requests contained in this set
     */
    public ApplicableRequestSet(Collection<StakeRequest> applicable) {
        this.applicable = applicable;
    }

    /**
     * Get the number of requests that are included.
     * 
     * @return the size of this ApplicbleRequestSet
     */
    public int size() {
        return applicable.size();
    }

    /**
     * Get an iterator of affected requests.
     */
    public Iterator<StakeRequest> iterator() {
        return applicable.iterator();
    }

    /**
     * Get a single request, this is for sets made with getRegionStatusRequests(<regionID>, Status.PENDING)
     * This will mark all but the oldest request unstaked, leaving one valid request
     * The changes are done to the requests not to the set
     * 
     * this will return null if:
     *      the set is empty
     *      all request are not pending and for the same region
     *      
     * @return the one valid request, or null
     */
    public StakeRequest getPendingRegionRequest() {
        if (applicable.isEmpty()) {
            return null;
        }

        StakeRequest oldestRequest = null;

        for (StakeRequest request : applicable) {
            if (oldestRequest == null) {
                oldestRequest = request;
            } else {
                // Error check
                if (!oldestRequest.getRegionID().equals(request.getRegionID())
                        || oldestRequest.getStatus() != Status.PENDING
                        || request.getStatus() != Status.PENDING) {
                    return null;
                }

                // Keep the oldest request, set the other request to unstaked
                if (oldestRequest.getRequestID() > request.getRequestID()) {
                    oldestRequest.setStatus(Status.UNSTAKED);
                    oldestRequest = request;
                } else {
                    request.setStatus(Status.UNSTAKED);
                }
            }
        }

        return oldestRequest;
    }

    /**
     * Get a single request, this is for sets made with getPlayerStatusRequests(<player>, Status.PENDING)
     * This will mark all but the newest request unstaked, leaving one valid request
     * The changes are done to the requests not to the set
     * 
     * this will return null if:
     *      the set is empty
     *      all request are not pending and by the same player
     *      
     * @return the one valid request, or null
     */
    public StakeRequest getPendingPlayerRequest() {
        if (applicable.isEmpty()) {
            return null;
        }

        StakeRequest newestRequest = null;

        for (StakeRequest request : applicable) {
            if (newestRequest == null) {
                newestRequest = request;
            } else {
                // Error check
                if (!newestRequest.getRegionID().equals(request.getRegionID())
                        || newestRequest.getStatus() != Status.PENDING
                        || request.getStatus() != Status.PENDING) {
                    return null;
                }

                // Keep the oldest request, set the other request to unstaked
                if (newestRequest.getRequestID() < request.getRequestID()) {
                    newestRequest.setStatus(Status.UNSTAKED);
                    newestRequest = request;
                } else {
                    request.setStatus(Status.UNSTAKED);
                }
            }
        }

        return newestRequest;
    }

    /**
     * Get a single request, this is for sets made with getRegionStatusRequests(<regionID>, Status.ACCEPTED)
     * This will fix all requests of the set based on date and owners, leaving one valid request
     * The changes are done to the requests not to the set
     * 
     * this will return null if:
     *      the set is empty
     *      all request are not accepted and for the same region
     *      the set can't fixed down to one valid request
     *      
     * @param rgMgr the WG RegionManager that has the region(s) the requests in the set pair up to
     * @return the one valid request, or null
     */
    public StakeRequest getAcceptedRequest(RegionManager rgMgr) {
        if (applicable.isEmpty()) {
            return null;
        }

        StakeRequest otherRequest = null;
        DefaultDomain owners;

        for (StakeRequest request : applicable) {
            owners = rgMgr.getRegion(request.getRegionID()).getOwners();
            if (otherRequest == null) {
                otherRequest = request;
            } else {
                // Error check
                if (!otherRequest.getRegionID().equals(request.getRegionID())
                        || otherRequest.getStatus() != Status.ACCEPTED
                        || request.getStatus() != Status.ACCEPTED 
                        || owners.size() != 1) {
                    return null;
                }

                // Both requesters and the region owner match, save the older request
                if (otherRequest.getPlayerName().equals(request.getPlayerName()) &&
                        owners.contains(request.getPlayerName())) {
                    if (otherRequest.getRequestID() > request.getRequestID()) {
                        otherRequest.setStatus(Status.RECLAIMED);
                        otherRequest = request;
                    } else {
                        request.setStatus(Status.RECLAIMED);
                    }
                // request matches the region owner, keep it and remove otherRequest
                } else if (owners.contains(request.getPlayerName())) {
                        otherRequest.setStatus(Status.RECLAIMED);
                        otherRequest = request;
                // otherRequest matches the region owner, keep it and remove request
                } else if (owners.contains(otherRequest.getPlayerName())) {
                        request.setStatus(Status.RECLAIMED);
                } else {
                    // Neither requester matches the region owner, can't fix
                    return null;
                }
            }
        }

        return otherRequest;
    }
}
