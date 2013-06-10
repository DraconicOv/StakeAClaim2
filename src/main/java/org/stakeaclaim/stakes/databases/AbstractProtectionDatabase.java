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

import org.stakeaclaim.stakes.RequestManager;

public abstract class AbstractProtectionDatabase implements ProtectionDatabase {

    /**
     * Load the list of requests into a request manager.
     * 
     * @throws ProtectionDatabaseException
     */
    public void load(RequestManager manager) throws ProtectionDatabaseException {
        load();
//        manager.setRequests(getRequests());
    }
    
    /**
     * Save the list of requests from a request manager.
     * 
     * @throws ProtectionDatabaseException
     */
    public void save(RequestManager manager) throws ProtectionDatabaseException {
        setRequests(manager.getRequests());
        save();
    }
    
}
