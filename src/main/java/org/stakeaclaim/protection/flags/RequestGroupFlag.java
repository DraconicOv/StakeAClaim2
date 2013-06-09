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
package org.stakeaclaim.protection.flags;

import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.protection.ApplicableRequestSet;
import org.stakeaclaim.protection.requests.Request;

/**
 *
 * @author sk89q
 */
public class RequestGroupFlag extends EnumFlag<RequestGroup> {

    private RequestGroup def;

    public RequestGroupFlag(String name, RequestGroup def) {
        super(name, RequestGroup.class, null);
        this.def = def;
    }

    public RequestGroup getDefault() {
        return def;
    }

    @Override
    public RequestGroup detectValue(String input) {
        input = input.trim();

        if (input.equalsIgnoreCase("members") || input.equalsIgnoreCase("member")) {
            return RequestGroup.MEMBERS;
        } else if (input.equalsIgnoreCase("owners") || input.equalsIgnoreCase("owner")) {
            return RequestGroup.OWNERS;
        } else if (input.equalsIgnoreCase("nonowners") || input.equalsIgnoreCase("nonowner")) {
            return RequestGroup.NON_OWNERS;
        } else if (input.equalsIgnoreCase("nonmembers") || input.equalsIgnoreCase("nonmember")) {
            return RequestGroup.NON_MEMBERS;
        } else if (input.equalsIgnoreCase("everyone") || input.equalsIgnoreCase("anyone") || input.equalsIgnoreCase("all")) {
            return RequestGroup.ALL;
        } else if (input.equalsIgnoreCase("none") || input.equalsIgnoreCase("noone") || input.equalsIgnoreCase("deny")) {
            return RequestGroup.NONE;
        } else {
            return null;
        }
    }

//    public static boolean isMember(Request request, RequestGroup group, LocalPlayer player) {
//        if (group == null || group == RequestGroup.ALL) {
//            return true;
//        } else if (group == RequestGroup.OWNERS) {
//            if (request.isOwner(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.MEMBERS) {
//            if (request.isMember(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.NON_OWNERS) {
//            if (!request.isOwner(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.NON_MEMBERS) {
//            if (!request.isMember(player)) {
//                return true;
//            }
//        }
//
//        return false;
//    }

//    public static boolean isMember(ApplicableRequestSet set,
//                                   RequestGroup group, LocalPlayer player) {
//        if (group == null || group == RequestGroup.ALL) {
//            return true;
//        } else if (group == RequestGroup.OWNERS) {
//            if (set.isOwnerOfAll(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.MEMBERS) {
//            if (set.isMemberOfAll(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.NON_OWNERS) {
//            if (!set.isOwnerOfAll(player)) {
//                return true;
//            }
//        } else if (group == RequestGroup.NON_MEMBERS) {
//            if (!set.isMemberOfAll(player)) {
//                return true;
//            }
//        }
//
//        return false;
//    }

}
