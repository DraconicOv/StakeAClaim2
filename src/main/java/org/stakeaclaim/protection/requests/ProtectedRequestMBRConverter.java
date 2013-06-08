// $Id$
/*
 * StakeAClaim
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
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

package org.stakeaclaim.protection.requests;

import org.khelekore.prtree.MBRConverter;

public class ProtectedRequestMBRConverter implements MBRConverter<ProtectedRequest> {

    @Override
    public int getDimensions() {
        return 3;
    }

    @Override
    public double getMax(int dimension, ProtectedRequest request) {
        switch (dimension) {
            case 0:
                return request.getMaximumPoint().getBlockX();
            case 1:
                return request.getMaximumPoint().getBlockY();
            case 2:
                return request.getMaximumPoint().getBlockZ();
        }
        return 0;
    }

    @Override
    public double getMin(int dimension, ProtectedRequest request) {
        switch (dimension) {
            case 0:
                return request.getMinimumPoint().getBlockX();
            case 1:
                return request.getMinimumPoint().getBlockY();
            case 2:
                return request.getMinimumPoint().getBlockZ();
        }
        return 0;
    }
}
