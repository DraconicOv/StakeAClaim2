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

import com.sk89q.worldguard.protection.flags.StateFlag.State;

/**
 * Represents extended information for a claim .
 *
 */
public class Stake implements Comparable<Stake>{

    /**
    * Compares to another stake.<br>
    *<br>
    * Orders by the id, ascending
    *
    * @param other The stake to compare to
    */
   public int compareTo(Stake other) {
        return id.compareTo(other.id);
   }

    public enum Status {
        PENDING,
        ACCEPTED,
        DENIED
    }

    /**
     * Holds the region's ID.
     */
    private String id;

    /**
     * Holds the name on the stake for this claim.
     */
    private String stakeName = null;

    /**
     * Holds the status of the stake.
     */
    private Status status = null;

    /**
     * True if the claim has been reclaimed.
     */
    private boolean reclaimed = false;

    /**
     * True if the claim has been marked as VIP only.
     */
    private boolean vip = false;

    /**
     * Holds the offline/default entry state of this claim.
     */
    private State defaultEntry;

    /**
     * Holds a custom name for this claim.
     */
    private String claimName = null;

    /**
     * Construct a new claim stake instance.
     *
     * @param id The ID of the region being extended.
     */
    public Stake(String id) {
        this.id = id;
    }

    /**
     * Gets the id of this stake
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name on the stake
     *
     * @return stakeName
     */
    public String getStakeName() {
        return stakeName;
    }

    /**
     * Sets the name on the stake
     *
     * @param playerName the player's name to put on the stake
     */
    public void setStakeName(String playerName) {
         this.stakeName = playerName;
    }

    /**
     * Gets the status of the stake
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of the stake
     *
     * @param status 
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the reclaimed state of the claim
     *
     * @return reclaimed state
     */
    public boolean getReclaimed() {
        return reclaimed;
    }

    /**
     * Sets the reclaimed state of the claim
     *
     * @param reclaimed true to set as reclaimed
     */
    public void setRecalimed(boolean reclaimed) {
         this.reclaimed = reclaimed;
    }

    /**
     * Gets the VIP state of the claim
     *
     * @return vip state
     */
    public boolean getVIP() {
        return vip;
    }

    /**
     * Sets the VIP state of the claim
     *
     * @param vip true to mark a claim VIP only
     */
    public void setVIP(boolean vip) {
         this.vip = vip;
    }

    /**
     * Gets the offline/default entry state
     *
     * @return offline/default entry state
     */
    public State getDefaultEntry() {
        return defaultEntry;
    }

    /**
     * Set the offline/default entry state
     *
     * @param defaultEntry offline/default entry state
     */
    public void setDefaultEntry(State defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    /**
     * Gets the custom name on this claim
     *
     * @return claimName
     */
    public String getClaimName() {
        return claimName;
    }

    /**
     * Sets the custom name on this claim
     *
     * @param claimName the new custom name for this claim
     */
    public void setClaimName(String claimName) {
         this.claimName = claimName;
    }

}
