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

import org.bukkit.entity.Player;

/**
 * Represents a stake request.
 *
 */
public class StakeRequest implements Comparable<StakeRequest> {

    /**
    * Compares to another request.<br>
    *<br>
    * Orders by the id, ascending
    *
    * @param other The request to compare to
    */
   public int compareTo(StakeRequest other) {
       if (requestID < other.requestID) {
           return -1;
       } else if (requestID > other.requestID) {
           return 1;
       }
       return 0;
   }

    public enum Status {
        PENDING,
        ACCEPTED,
        DENIED,
        WITHDRAWN,
        RECLAIMED
    }
    
    public enum Access {
        ALLOW,
        DENY
    }

    /**
     * Holds the request's ID number. (epoch time stamp in mill/s)
     */
    private long requestID;

    /**
     * Holds the ID of the region the request is for.
     */
    private String regionID;

    /**
     * Holds the requester's name.
     */
    private String playerName;

    /**
     * Holds the status of the request.
     */
    private Status status;

    /**
     * Holds the offline access state.
     */
    private Access access;

    /**
     * Construct a new instance of this request.
     * playerName is a String
     *
     * @param regionID The ID of the region being requested.
     * @param playerName The requester's name.
     */
    public StakeRequest(String regionID, String playerName) {
        this.requestID = System.currentTimeMillis();
        this.regionID = regionID.toLowerCase();
        this.playerName = playerName.toLowerCase();
        this.access = null;
    }

    /**
     * Construct a new instance of this request.
     * player is a Player
     *
     * @param regionID The ID of the region being requested.
     * @param playerName The requester's name.
     */
    public StakeRequest(String regionID, Player player) {
        this.requestID = System.currentTimeMillis();
        this.regionID = regionID.toLowerCase();
        this.playerName = player.getName().toLowerCase();
        this.access = null;
    }

    /**
     * Gets the ID number of this request
     *
     * @return the requestID
     */
    public long getRequestID() {
        return requestID;
    }

    /**
     * Gets the ID of the region the request is for
     *
     * @return the regionID
     */
    public String getRegionID() {
        return regionID;
    }

    /**
     * Gets the requester's name
     *
     * @return the playerName
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Gets the status of the request
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of the request
     *
     * @param status 
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the offline access state
     *
     * @return the access
     */
    public Access getAccess() {
        return access;
    }

    /**
     * Set the offline access state
     *
     * @param access offline access state
     */
    public void setAccess(Access access) {
        this.access = access;
    }
}
