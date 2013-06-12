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
//import java.lang.NumberFormatException;
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

//import org.stakeaclaim.domains.DefaultDomain;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.flags.Flag;
//import org.stakeaclaim.stakes.requests.GlobalRequest;
//import org.stakeaclaim.stakes.requests.ProtectedCuboidRequest;
//import org.stakeaclaim.stakes.requests.ProtectedPolygonalRequest;
import org.stakeaclaim.stakes.StakeRequest;
import org.stakeaclaim.stakes.StakeRequest.Access;
import org.stakeaclaim.stakes.StakeRequest.Status;

public class YAMLDatabase extends AbstractStakeDatabase {

    private YAMLProcessor config;
    private Map<Long, StakeRequest> requests;
    private final Logger logger;

    public YAMLDatabase(File file, Logger logger) throws StakeDatabaseException, FileNotFoundException {
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

    public void load() throws StakeDatabaseException {
        try {
            config.load();
        } catch (IOException e) {
            throw new StakeDatabaseException(e);
        }

        Map<String, YAMLNode> requestData = config.getNodes("requests");

        // No requests are even configured
        if (requestData == null) {
            this.requests = new HashMap<Long, StakeRequest>();
            return;
        }

        Map<Long, StakeRequest> requests = new HashMap<Long, StakeRequest>();

        for (Map.Entry<String, YAMLNode> entry : requestData.entrySet()) {

            try {
                StakeRequest request;

                long requestID = Long.parseLong(entry.getKey());
                YAMLNode node = entry.getValue();

                String regionID = checkNonNull(node.getString("region"));
                String playerName = checkNonNull(node.getString("player"));

                request = new StakeRequest(requestID, regionID, playerName);

                request.setStatus(unmarshalStatus(checkNonNull(node.getString("status"))));
                request.setAccess(unmarshalAccess(node.getString("access")));

                requests.put(requestID, request);
                
            } catch (NullPointerException e) {
                logger.warning("Missing data for request '" + entry.getKey() + '"');
            } catch (NumberFormatException e) {
                logger.warning("'" + entry.getKey() + "' is not a valid request ID.");
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

    private Status unmarshalStatus(String status) {
        try {
            return Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Access unmarshalAccess(String access) {
        if (access == null) {
            return null;
        }
        try {
            return Access.valueOf(access.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void save() throws StakeDatabaseException {
        config.clear();

        for (Map.Entry<Long, StakeRequest> entry : requests.entrySet()) {
            StakeRequest request = entry.getValue();
            YAMLNode node = config.addNode("requests." + entry.getKey().toString());

            node.setProperty("region", request.getRegionID());
            node.setProperty("player", request.getPlayerName());
            node.setProperty("status", request.getStatus().name());
            if (request.getAccess() != null) {
                node.setProperty("access", request.getAccess().name());
            }
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

    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

    public void setRequests(Map<Long, StakeRequest> requests) {
        this.requests = requests;
    }
    
}
