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
import org.stakeaclaim.protection.databases.ProtectionDatabase;
import org.stakeaclaim.protection.requests.Request;
//import org.stakeaclaim.protection.requests.RequestMBRConverter;
import org.khelekore.prtree.MBR;
import org.khelekore.prtree.MBRConverter;
import org.khelekore.prtree.PRTree;
import org.khelekore.prtree.SimpleMBR;

import java.util.*;

public class PRTreeRequestManager extends RequestManager {

    private static final int BRANCH_FACTOR = 30;
    /**
     * List of protected requests.
     */
    private Map<String, Request> requests;
//    /**
//     * Converter to get coordinates of the tree.
//     */
//    private MBRConverter<Request> converter = new RequestMBRConverter();
    /**
     * Priority R-tree.
     */
    private PRTree<Request> tree;

    /**
     * Construct the manager.
     *
     * @param requestLoader The request loader to use
     */
    public PRTreeRequestManager(ProtectionDatabase requestLoader) {
        super(requestLoader);
//        requests = new TreeMap<String, Request>();
//        tree = new PRTree<Request>(converter, BRANCH_FACTOR);
    }

    @Override
    public Map<String, Request> getRequests() {
        return requests;
    }

    @Override
    public void setRequests(Map<String, Request> requests) {
//        this.requests = new TreeMap<String, Request>(requests);
//        tree = new PRTree<Request>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
    }

    @Override
    public void addRequest(Request request) {
//        requests.put(request.getId().toLowerCase(), request);
//        tree = new PRTree<Request>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
    }

    @Override
    public boolean hasRequest(String id) {
        return requests.containsKey(id.toLowerCase());
    }

    @Override
    public void removeRequest(String id) {
//        Request request = requests.get(id.toLowerCase());
//
//        requests.remove(id.toLowerCase());
//
//        if (request != null) {
//            List<String> removeRequests = new ArrayList<String>();
//            for (Request curRequest : requests.values()) {
//                if (curRequest.getParent() == request) {
//                    removeRequests.add(curRequest.getId().toLowerCase());
//                }
//            }
//
//            for (String remId : removeRequests) {
//                removeRequest(remId);
//            }
//        }
//
//        tree = new PRTree<Request>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
    }

    @Override
    public ApplicableRequestSet getApplicableRequests(Vector pt) {
//
//        // Floor the vector to ensure we get accurate points
//        pt = pt.floor();
//
        List<Request> appRequests = new ArrayList<Request>();
//        MBR pointMBR = new SimpleMBR(pt.getX(), pt.getX(), pt.getY(), pt.getY(), pt.getZ(), pt.getZ());
//
//        for (Request request : tree.find(pointMBR)) {
//            if (request.contains(pt) && !appRequests.contains(request)) {
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
//
//        Collections.sort(appRequests);
//
        return new ApplicableRequestSet(appRequests, requests.get("__global__"));
    }

    @Override
    public ApplicableRequestSet getApplicableRequests(Request checkRequest) {
//        List<Request> appRequests = new ArrayList<Request>();
//        appRequests.addAll(requests.values());
//
//        List<Request> intersectRequests;
        List<Request> intersectRequests = null;
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (Exception e) {
//            intersectRequests = new ArrayList<Request>();
//        }
//
        return new ApplicableRequestSet(intersectRequests, requests.get("__global__"));
    }

    @Override
    public List<String> getApplicableRequestsIDs(Vector pt) {
//
//        // Floor the vector to ensure we get accurate points
//        pt = pt.floor();
//
        List<String> applicable = new ArrayList<String>();
//        MBR pointMBR = new SimpleMBR(pt.getX(), pt.getX(), pt.getY(), pt.getY(), pt.getZ(), pt.getZ());
//
//        for (Request request : tree.find(pointMBR)) {
//            if (request.contains(pt) && !applicable.contains(request.getId())) {
//                applicable.add(request.getId());
//
//                Request parent = request.getParent();
//
//                while (parent != null) {
//                    if (!applicable.contains(parent.getId())) {
//                        applicable.add(parent.getId());
//                    }
//
//                    parent = parent.getParent();
//                }
//            }
//        }
//
        return applicable;
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
//      List<Request> intersectRequests;
      List<Request> intersectRequests = null;
//      
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (Exception e) {
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
//        for (Map.Entry<String, Request> entry : requests.entrySet()) {
//            if (entry.getValue().getOwners().contains(player)) {
//                count++;
//            }
//        }
//
        return count;
    }
}
