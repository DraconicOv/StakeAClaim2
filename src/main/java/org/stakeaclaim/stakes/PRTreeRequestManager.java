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

import com.sk89q.worldedit.Vector;
import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.stakes.ApplicableRequestSet;
import org.stakeaclaim.stakes.databases.ProtectionDatabase;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.RequestMBRConverter;
import org.khelekore.prtree.MBR;
import org.khelekore.prtree.MBRConverter;
import org.khelekore.prtree.PRTree;
import org.khelekore.prtree.SimpleMBR;

import java.util.*;

public class PRTreeRequestManager extends RequestManager {

    private static final int BRANCH_FACTOR = 30;
    
    /**
     * List of stake requests.
     */
        private Map<Long, StakeRequest> requests;
    
//    /**
//     * Converter to get coordinates of the tree.
//     */
//    private MBRConverter<StakeRequest> converter = new RequestMBRConverter();
    
//    /**
//     * Priority R-tree.
//     */
//    private PRTree<StakeRequest> tree;

    /**
     * Construct the manager.
     *
     * @param requestLoader The request loader to use
     */
    public PRTreeRequestManager(ProtectionDatabase requestLoader) {
        super(requestLoader);
        requests = new TreeMap<Long, StakeRequest>();
//        tree = new PRTree<StakeRequest>(converter, BRANCH_FACTOR);
    }

    @Override
    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

//    @Override
//    public void setRequests(Map<Long, StakeRequest> requests) {
//        this.requests = new TreeMap<Long, StakeRequest>(requests);
//        tree = new PRTree<StakeRequest>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
//    }

    @Override
    public void addRequest(StakeRequest request) {
//        requests.put(request.getId().toLowerCase(), request);
//        tree = new PRTree<StakeRequest>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
    }

//    @Override
//    public boolean hasRequest(String id) {
//        return requests.containsKey(id.toLowerCase());
//    }

    @Override
    public void removeRequest(long id) {
//        StakeRequest request = requests.get(id.toLowerCase());
//
//        requests.remove(id.toLowerCase());
//
//        if (request != null) {
//            List<String> removeRequests = new ArrayList<String>();
//            for (StakeRequest curRequest : requests.values()) {
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
//        tree = new PRTree<StakeRequest>(converter, BRANCH_FACTOR);
//        tree.load(requests.values());
    }

//    @Override
//    public ApplicableRequestSet getApplicableRequests(Vector pt) {
//
//        // Floor the vector to ensure we get accurate points
//        pt = pt.floor();
//
//        List<StakeRequest> appRequests = new ArrayList<StakeRequest>();
//        MBR pointMBR = new SimpleMBR(pt.getX(), pt.getX(), pt.getY(), pt.getY(), pt.getZ(), pt.getZ());
//
//        for (StakeRequest request : tree.find(pointMBR)) {
//            if (request.contains(pt) && !appRequests.contains(request)) {
//                appRequests.add(request);
//
//                StakeRequest parent = request.getParent();
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
//        return new ApplicableRequestSet(appRequests, requests.get("__global__"));
//    }

//    @Override
//    public ApplicableRequestSet getApplicableRequests(StakeRequest checkRequest) {
//        List<StakeRequest> appRequests = new ArrayList<StakeRequest>();
//        appRequests.addAll(requests.values());
//
//        List<StakeRequest> intersectRequests;
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (Exception e) {
//            intersectRequests = new ArrayList<StakeRequest>();
//        }
//
//        return new ApplicableRequestSet(intersectRequests, requests.get("__global__"));
//    }

//    @Override
//    public List<String> getApplicableRequestsIDs(Vector pt) {
//
//        // Floor the vector to ensure we get accurate points
//        pt = pt.floor();
//
//        List<String> applicable = new ArrayList<String>();
//        MBR pointMBR = new SimpleMBR(pt.getX(), pt.getX(), pt.getY(), pt.getY(), pt.getZ(), pt.getZ());
//
//        for (StakeRequest request : tree.find(pointMBR)) {
//            if (request.contains(pt) && !applicable.contains(request.getId())) {
//                applicable.add(request.getId());
//
//                StakeRequest parent = request.getParent();
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
//        return applicable;
//    }

//    @Override
//    public boolean overlapsUnownedRequest(StakeRequest checkRequest, LocalPlayer player) {
//        List<StakeRequest> appRequests = new ArrayList<StakeRequest>();
//
//        for (StakeRequest other : requests.values()) {
//            if (other.getOwners().contains(player)) {
//                continue;
//            }
//
//            appRequests.add(other);
//        }
//
//      List<StakeRequest> intersectRequests;
//      List<StakeRequest> intersectRequests = null;
//      
//        try {
//            intersectRequests = checkRequest.getIntersectingRequests(appRequests);
//        } catch (Exception e) {
//            intersectRequests = new ArrayList<StakeRequest>();
//        }
//
//        return intersectRequests.size() > 0;
//    }

    @Override
    public int size() {
        return requests.size();
    }

    @Override
    public int getRequestCountOfPlayer(LocalPlayer player) {
        int count = 0;

        for (Map.Entry<Long, StakeRequest> entry : requests.entrySet()) {
//            if (entry.getValue().getOwners().contains(player)) {
//                count++;
//            }
        }

        return count;
    }
}
