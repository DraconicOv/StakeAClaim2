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
package org.stakeaclaim.protection.managers;

import com.sk89q.worldedit.Vector;
import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.protection.ApplicableRequestSet;
import org.stakeaclaim.protection.UnsupportedIntersectionException;
import org.stakeaclaim.protection.databases.ProtectionDatabase;
import org.stakeaclaim.protection.requests.Request;

import java.util.*;

/**
 * A very simple implementation of the request manager that uses a flat list
 * and iterates through the list to identify applicable requests. This method
 * is not very efficient.
 *
 * @author sk89q
 */
public class FlatRequestManager extends RequestManager {

    /**
     * List of protected requests.
     */
    private Map<String, Request> requests;

    /**
     * Construct the manager.
     *
     * @param requestLoader The loader for requests
     */
    public FlatRequestManager(ProtectionDatabase requestLoader) {
        super(requestLoader);
        requests = new TreeMap<String, Request>();
    }

    @Override
    public Map<String, Request> getRequests() {
        return requests;
    }

    @Override
    public void setRequests(Map<String, Request> requests) {
        this.requests = new TreeMap<String, Request>(requests);
    }

    @Override
    public void addRequest(Request request) {
//        requests.put(request.getId().toLowerCase(), request);
    }

    @Override
    public void removeRequest(String id) {
        Request request = requests.get(id.toLowerCase());
        requests.remove(id.toLowerCase());

        if (request != null) {
            List<String> removeRequests = new ArrayList<String>();
            for (Request curRequest : requests.values()) {
//                if (curRequest.getParent() == request) {
//                    removeRequests.add(curRequest.getId().toLowerCase());
//                }
            }

            for (String remId : removeRequests) {
                removeRequest(remId);
            }
        }
    }

    @Override
    public boolean hasRequest(String id) {
        return requests.containsKey(id.toLowerCase());
    }

    @Override
    public ApplicableRequestSet getApplicableRequests(Vector pt) {
        TreeSet<Request> appRequests =
                new TreeSet<Request>();

//        for (Request request : requests.values()) {
//            if (request.contains(pt)) {
//                appRequests.add(request);
//
//                Request parent = request.getParent();
//
//                while (parent != null) {
//                    if (!appRequests.contains(parent)) {
//                        appRequests.add(parent);
//                    }
//
//                    parent = parent.getParent();
//                }
//            }
//        }

        return new ApplicableRequestSet(appRequests, requests.get("__global__"));
    }

    /**
     * Get an object for a request for rules to be applied with.
     *
     * @return
     */
    /*@Override
    public ApplicableRequestSet getApplicableRequests(Request checkRequest) {

        List<Request> appRequests = new ArrayList<Request>();
        appRequests.addAll(requests.values());

        List<Request> intersectRequests;
        try {
            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
        } catch (Exception e) {
            intersectRequests = new ArrayList<Request>();
        }

        return new ApplicableRequestSet(intersectRequests, requests.get("__global__"));
    }*/

    @Override
    public List<String> getApplicableRequestsIDs(Vector pt) {
        List<String> applicable = new ArrayList<String>();
//
//        for (Map.Entry<String, Request> entry : requests.entrySet()) {
//            if (entry.getValue().contains(pt)) {
//                applicable.add(entry.getKey());
//            }
//        }
//
        return applicable;
    }

    @Override
    public ApplicableRequestSet getApplicableRequests(Request checkRequest) {
//
//        List<Request> appRequests = new ArrayList<Request>();
//        appRequests.addAll(requests.values());
//
//        List<Request> intersectRequests;
        List<Request> intersectRequests = null;
//
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (Exception e) {
//            intersectRequests = new ArrayList<Request>();
//        }
//
        return new ApplicableRequestSet(intersectRequests, requests.get("__global__"));
    }

    @Override
    public boolean overlapsUnownedRequest(Request checkRequest, LocalPlayer player) {
//        List<Request> appRequests = new ArrayList<Request>();
//
//        for (Request other : requests.values()) {
//            if (other.getOwners().contains(player)) {
//                continue;
//            }
//
//            appRequests.add(other);
//        }
//
//        List<Request> intersectRequests;
        List<Request> intersectRequests = null;
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (UnsupportedIntersectionException e) {
//            intersectRequests = new ArrayList<Request>();
//        }
//
        return intersectRequests.size() > 0;
    }

    @Override
    public int size() {
        return requests.size();
    }

    @Override
    public int getRequestCountOfPlayer(LocalPlayer player) {
        int count = 0;
//
//        for (Request request : requests.values()) {
//            if (request.getOwners().contains(player)) {
//                count++;
//            }
//        }
//
        return count;
    }
}
