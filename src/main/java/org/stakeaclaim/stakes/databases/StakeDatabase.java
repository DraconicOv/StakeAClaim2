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

package org.stakeaclaim.stakes.databases;

import java.util.Map;

import org.stakeaclaim.stakes.RequestManager;
import org.stakeaclaim.stakes.StakeRequest;

/**
 * Represents a database to read and write lists of requests from and to.
 */
public interface StakeDatabase {
    /**
     * Load the list of requests. The method should not modify the list returned
     * by getRequests() unless the load finishes successfully.
     *
     * @throws StakeDatabaseException when an error occurs
     */
    public void load() throws StakeDatabaseException;
    /**
     * Save the list of requests.
     *
     * @throws StakeDatabaseException when an error occurs
     */
    public void save() throws StakeDatabaseException;
    /**
     * Load the list of requests into a request manager.
     *
     * @param manager The manager to load requests into
     * @throws StakeDatabaseException when an error occurs
     */
    public void load(RequestManager manager) throws StakeDatabaseException;
    /**
     * Save the list of requests from a request manager.
     *
     * @param manager The manager to load requests into
     * @throws StakeDatabaseException when an error occurs
     */
    public void save(RequestManager manager) throws StakeDatabaseException;
    /**
     * Get a list of requests.
     *
     * @return the requests loaded by this StakeDatabase
     */
    public Map<Long, StakeRequest> getRequests();
    /**
     * Set the list of requests.
     *
     * @param requests The requests to be applied to this StakeDatabase
     */
    public void setRequests(Map<Long, StakeRequest> requests);
}
