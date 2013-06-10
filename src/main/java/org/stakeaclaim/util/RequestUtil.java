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

package org.stakeaclaim.util;

import org.stakeaclaim.domains.DefaultDomain;
import org.stakeaclaim.stakes.databases.RequestDBUtil;

/**
 * Various utility functions for requests.
 * 
 * @author sk89q
 */
@Deprecated
public class RequestUtil {
    
    private RequestUtil() {
    }

    /**
     * Parse a group/player DefaultDomain specification for areas.
     * 
     * @param domain The domain
     * @param split The arguments
     * @param startIndex The index to start at
     * @deprecated see {@link RequestDBUtil#addToDomain(org.stakeaclaim.domains.DefaultDomain, String[], int)}
     */
    @Deprecated
    public static void addToDomain(DefaultDomain domain, String[] split,
            int startIndex) {
        RequestDBUtil.addToDomain(domain, split, startIndex);
    }

    /**
     * Parse a group/player DefaultDomain specification for areas.
     * 
     * @param domain The domain to add to
     * @param split The arguments
     * @param startIndex The index to start at
     * @deprecated see {@link RequestDBUtil#removeFromDomain(org.stakeaclaim.domains.DefaultDomain, String[], int)}
     */
    @Deprecated
    public static void removeFromDomain(DefaultDomain domain, String[] split,
            int startIndex) {
        RequestDBUtil.removeFromDomain(domain, split, startIndex);
    }

    /**
     * Parse a group/player DefaultDomain specification for areas.
     *
     * @param split The arguments
     * @param startIndex The index to start at
     * @deprecated see {@link RequestDBUtil#parseDomainString(String[], int)}
     * @return the parsed domain
     */
    @Deprecated
    public static DefaultDomain parseDomainString(String[] split, int startIndex) {
        return RequestDBUtil.parseDomainString(split, startIndex);
    }
}
