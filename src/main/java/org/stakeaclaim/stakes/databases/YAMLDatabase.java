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

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;

import org.stakeaclaim.domains.DefaultDomain;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.flags.Flag;
//import org.stakeaclaim.stakes.requests.GlobalRequest;
//import org.stakeaclaim.stakes.requests.ProtectedCuboidRequest;
//import org.stakeaclaim.stakes.requests.ProtectedPolygonalRequest;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.requests.Request.CircularInheritanceException;

public class YAMLDatabase extends AbstractProtectionDatabase {
    
    private YAMLProcessor config;
    private Map<Long, StakeRequest> requests;
    private final Logger logger;
    
    public YAMLDatabase(File file, Logger logger) throws ProtectionDatabaseException, FileNotFoundException {
        this.logger = logger;
        if (!file.exists()) { // shouldn't be necessary, but check anyways
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new FileNotFoundException(file.getAbsolutePath());
            }
        }
        config = new YAMLProcessor(file, false, YAMLFormat.COMPACT);
    }

    public void load() throws ProtectionDatabaseException {
        try {
            config.load();
        } catch (IOException e) {
            throw new ProtectionDatabaseException(e);
        }
        
        Map<String, YAMLNode> requestData = config.getNodes("requests");
        
        // No requests are even configured
        if (requestData == null) {
            this.requests = new HashMap<Long, StakeRequest>();
            return;
        }

        Map<Long, StakeRequest> requests =
            new HashMap<Long, StakeRequest>();
        Map<StakeRequest,String> parentSets =
            new LinkedHashMap<StakeRequest, String>();
        
        for (Map.Entry<String, YAMLNode> entry : requestData.entrySet()) {
            String id = entry.getKey().toLowerCase().replace(".", "");
            YAMLNode node = entry.getValue();
            
            String type = node.getString("type");
            StakeRequest request;
            
            try {
                if (type == null) {
                    logger.warning("Undefined request type for request '" + id + '"');
                    continue;
                } else if (type.equals("cuboid")) {
                    Vector pt1 = checkNonNull(node.getVector("min"));
                    Vector pt2 = checkNonNull(node.getVector("max"));
                    BlockVector min = Vector.getMinimum(pt1, pt2).toBlockVector();
                    BlockVector max = Vector.getMaximum(pt1, pt2).toBlockVector();
//                    request = new ProtectedCuboidRequest(id, min, max);
                } else if (type.equals("poly2d")) {
                    Integer minY = checkNonNull(node.getInt("min-y"));
                    Integer maxY = checkNonNull(node.getInt("max-y"));
                    List<BlockVector2D> points = node.getBlockVector2dList("points", null);
//                    request = new ProtectedPolygonalRequest(id, points, minY, maxY);
                } else if (type.equals("global")) {
//                    request = new GlobalRequest(id);
                } else {
                    logger.warning("Unknown request type for request '" + id + '"');
                    continue;
                }
                
                Integer priority = checkNonNull(node.getInt("priority"));
//                request.setPriority(priority);
//                setFlags(request, node.getNode("flags"));
//                request.setOwners(parseDomain(node.getNode("owners")));
//                request.setMembers(parseDomain(node.getNode("members")));
//                requests.put(id, request);
                
//                String parentId = node.getString("parent");
//                if (parentId != null) {
//                    parentSets.put(request, parentId);
//                }
            } catch (NullPointerException e) {
                logger.warning("Missing data for request '" + id + '"');
            }
        }
        
        // Relink parents
        for (Map.Entry<StakeRequest, String> entry : parentSets.entrySet()) {
            StakeRequest parent = requests.get(entry.getValue());
            if (parent != null) {
//                try {
//                    entry.getKey().setParent(parent);
//                } catch (CircularInheritanceException e) {
//                    logger.warning("Circular inheritance detect with '"
//                            + entry.getValue() + "' detected as a parent");
//                }
            } else {
                logger.warning("Unknown request parent: " + entry.getValue());
            }
        }
        
        this.requests = requests;
    }
    
    private <V> V checkNonNull(V val) throws NullPointerException {
        if (val == null) {
            throw new NullPointerException();
        }
        
        return val;
    }
    
//    private void setFlags(StakeRequest request, YAMLNode flagsData) {
//        if (flagsData == null) {
//            return;
//        }
//        
//        // @TODO: Make this better
//        for (Flag<?> flag : DefaultFlag.getFlags()) {
//            Object o = flagsData.getProperty(flag.getName());
//            if (o != null) {
//                setFlag(request, flag, o);
//            }
//            
//            if (flag.getRequestGroupFlag() != null) {
//            Object o2 = flagsData.getProperty(flag.getRequestGroupFlag().getName());
//                if (o2 != null) {
//                    setFlag(request, flag.getRequestGroupFlag(), o2);
//                }
//            }
//        }
//    }
    
//    private <T> void setFlag(StakeRequest request, Flag<T> flag, Object rawValue) {
//        T val = flag.unmarshal(rawValue);
//        if (val == null) {
//            logger.warning("Failed to parse flag '" + flag.getName()
//                    + "' with value '" + rawValue.toString() + "'");
//            return;
//        }
//        request.setFlag(flag, val);
//    }
    
    private DefaultDomain parseDomain(YAMLNode node) {
        if (node == null) {
            return new DefaultDomain();
        }
        
        DefaultDomain domain = new DefaultDomain();
        
        for (String name : node.getStringList("players", null)) {
            domain.addPlayer(name);
        }
        
        for (String name : node.getStringList("groups", null)) {
            domain.addGroup(name);
        }
        
        return domain;
    }

    public void save() throws ProtectionDatabaseException {
        config.clear();
        
        for (Map.Entry<Long, StakeRequest> entry : requests.entrySet()) {
            StakeRequest request = entry.getValue();
            YAMLNode node = config.addNode("requests." + entry.getKey());
            
//            if (request instanceof ProtectedCuboidRequest) {
//                ProtectedCuboidRequest cuboid = (ProtectedCuboidRequest) request;
//                node.setProperty("type", "cuboid");
//                node.setProperty("min", cuboid.getMinimumPoint());
//                node.setProperty("max", cuboid.getMaximumPoint());
//            } else if (request instanceof ProtectedPolygonalRequest) {
//                ProtectedPolygonalRequest poly = (ProtectedPolygonalRequest) request;
//                node.setProperty("type", "poly2d");
//                node.setProperty("min-y", poly.getMinimumPoint().getBlockY());
//                node.setProperty("max-y", poly.getMaximumPoint().getBlockY());
//                
//                List<Map<String, Object>> points = new ArrayList<Map<String,Object>>();
//                for (BlockVector2D point : poly.getPoints()) {
//                    Map<String, Object> data = new HashMap<String, Object>();
//                    data.put("x", point.getBlockX());
//                    data.put("z", point.getBlockZ());
//                    points.add(data);
//                }
//                
//                node.setProperty("points", points);
//            } else if (request instanceof GlobalRequest) {
//                node.setProperty("type", "global");
//            } else {
//                node.setProperty("type", request.getClass().getCanonicalName());
//            }

//            node.setProperty("priority", request.getPriority());
            node.setProperty("flags", getFlagData(request));
//            node.setProperty("owners", getDomainData(request.getOwners()));
//            node.setProperty("members", getDomainData(request.getMembers()));
//            StakeRequest parent = request.getParent();
//            if (parent != null) {
//                node.setProperty("parent", parent.getId());
//            }
        }
        
        config.setHeader("#\r\n" +
                "# StakeAClaim requests file\r\n" +
                "#\r\n" +
                "# WARNING: THIS FILE IS AUTOMATICALLY GENERATED. If you modify this file by\r\n" +
                "# hand, be aware that A SINGLE MISTYPED CHARACTER CAN CORRUPT THE FILE. If\r\n" +
                "# StakeAClaim is unable to parse the file, your requests will FAIL TO LOAD and\r\n" +
                "# the contents of this file will reset. Please use a YAML validator such as\r\n" +
                "# http://yaml-online-parser.appspot.com (for smaller files).\r\n" +
                "#\r\n" +
                "# REMEMBER TO KEEP PERIODICAL BACKUPS.\r\n" +
                "#");
        config.save();
    }
    
    private Map<String, Object> getFlagData(StakeRequest request) {
        Map<String, Object> flagData = new HashMap<String, Object>();
        
//        for (Map.Entry<Flag<?>, Object> entry : request.getFlags().entrySet()) {
//            Flag<?> flag = entry.getKey();
//            addMarshalledFlag(flagData, flag, entry.getValue());
//        }
        
        return flagData;
    }
    
//    @SuppressWarnings("unchecked")
//    private <V> void addMarshalledFlag(Map<String, Object> flagData,
//            Flag<V> flag, Object val) {
//        if (val == null) {
//            return;
//        }
//        flagData.put(flag.getName(), flag.marshal((V) val));
//    }
    
//    private Map<String, Object> getDomainData(DefaultDomain domain) {
//        Map<String, Object> domainData = new HashMap<String, Object>();
//
//        setDomainData(domainData, "players", domain.getPlayers());
//        setDomainData(domainData, "groups", domain.getGroups());
//        
//        return domainData;
//    }
    
    private void setDomainData(Map<String, Object> domainData,
            String key, Set<String> domain) {
        if (domain.size() == 0) {
            return;
        }
        
        List<String> list = new ArrayList<String>();
        
        for (String str : domain) {
            list.add(str);
        }
        
        domainData.put(key, list);
    }

    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

    public void setRequests(Map<Long, StakeRequest> requests) {
        this.requests = requests;
    }
    
}
