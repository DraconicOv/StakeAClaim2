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

import org.stakeaclaim.stakes.StakeRequest;

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
}
