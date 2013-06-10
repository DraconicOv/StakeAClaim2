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


import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Vector;
import org.stakeaclaim.bukkit.ConfigurationManager;
import org.stakeaclaim.domains.DefaultDomain;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.flags.Flag;
//import org.stakeaclaim.stakes.requests.GlobalRequest;
//import org.stakeaclaim.stakes.requests.ProtectedCuboidRequest;
//import org.stakeaclaim.stakes.requests.ProtectedPolygonalRequest;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.requests.Request.CircularInheritanceException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySQLDatabase extends AbstractProtectionDatabase {
    private final Logger logger;

    private Yaml yaml;

    private Map<Long, StakeRequest> requests;

    private Map<Long, StakeRequest> cuboidRequests;
    private Map<Long, StakeRequest> poly2dRequests;
    private Map<Long, StakeRequest> globalRequests;
//    private Map<StakeRequest, String> parentSets;

    private final ConfigurationManager config;

    private Connection conn;
    private int worldDbId = -1; // The database will never have an id of -1;

    public MySQLDatabase(ConfigurationManager config, String world, Logger logger) throws ProtectionDatabaseException {
        this.config = config;
//        String world1 = world;
        this.logger = logger;
//
//        try {
//            connect();
//
//            try {
//            	// Test if the database is up to date, if not throw a critical error
//            	PreparedStatement verTest = this.conn.prepareStatement(
//            			"SELECT `world_id` FROM `request_cuboid` LIMIT 0,1;"
//            		);
//            	verTest.execute();
//            } catch (SQLException ex) {
//            	throw new InvalidTableFormatException(
//            			"request_storage_update_20110325.sql"
//            		);
//            }
//
//            PreparedStatement worldStmt = conn.prepareStatement(
//                    "SELECT `id` FROM " +
//                    "`world` " +
//                    "WHERE `name` = ? LIMIT 0,1"
//            );
//
//            worldStmt.setString(1, world1);
//            ResultSet worldResult = worldStmt.executeQuery();
//
//            if (worldResult.first()) {
//                this.worldDbId = worldResult.getInt("id");
//            } else {
//                PreparedStatement insertWorldStatement = this.conn.prepareStatement(
//                        "INSERT INTO " +
//                        "`world` " +
//                        "(`id`, `name`) VALUES (null, ?)",
//                        Statement.RETURN_GENERATED_KEYS
//                );
//
//                insertWorldStatement.setString(1, world);
//                insertWorldStatement.execute();
//                ResultSet generatedKeys = insertWorldStatement.getGeneratedKeys();
//                if (generatedKeys.first()) {
//                    this.worldDbId = generatedKeys.getInt(1);
//                }
//            }
//        } catch (SQLException ex) {
//            logger.log(Level.SEVERE, ex.getMessage(), ex);
//            // We havn't connected to the databases, or there was an error
//            // initialising the world record, so there is no point continuing
//            return;
//        }
//
//        if (this.worldDbId <= 0) {
//            logger.log(Level.SEVERE, "Could not find or create the world");
//            // There was an error initialising the world record, so there is
//            // no point continuing
//            return;
//        }
//
//        DumperOptions options = new DumperOptions();
//        options.setIndent(2);
//        options.setDefaultFlowStyle(FlowStyle.FLOW);
//        Representer representer = new Representer();
//        representer.setDefaultFlowStyle(FlowStyle.FLOW);
//
//        // We have to use this in order to properly save non-string values
//        yaml = new Yaml(new SafeConstructor(), new Representer(), options);
    }

//    private void connect() throws SQLException {
//    	if (conn != null) {
//    		// Make a dummy query to check the connnection is alive.
//    		try {
//    			conn.prepareStatement("SELECT 1;").execute();
//    		} catch (SQLException ex) {
//                // Test if the dummy query failed because the connection is dead,
//                // and if it is mark the connection as closed (the MySQL Driver
//                // does not ensure that the connection is marked as closed unless
//                // the close() method has been called.
//    			if ("08S01".equals(ex.getSQLState())) {
//    				conn.close();
//    			}
//    		}
//    	}
//        if (conn == null || conn.isClosed()) {
//            conn = DriverManager.getConnection(config.sqlDsn, config.sqlUsername, config.sqlPassword);
//        }
//    }
//
//    private void loadFlags(StakeRequest request) {
//        // @TODO: Iterate _ONCE_
//        try {
//            PreparedStatement flagsStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`request_flag`.`flag`, " +
//                    "`request_flag`.`value` " +
//                    "FROM `request_flag` " +
//                    "WHERE `request_flag`.`request_id` = ? " +
//                    "AND `request_flag`.`world_id` = " + this.worldDbId
//            );
//
//            flagsStatement.setString(1, request.getId().toLowerCase());
//            ResultSet flagsResultSet = flagsStatement.executeQuery();
//
//            Map<String,Object> requestFlags = new HashMap<String,Object>();
//            while (flagsResultSet.next()) {
//                requestFlags.put(
//                        flagsResultSet.getString("flag"),
//                        sqlUnmarshal(flagsResultSet.getString("value"))
//                        );
//            }
//
//            // @TODO: Make this better
//            for (Flag<?> flag : DefaultFlag.getFlags()) {
//                Object o = requestFlags.get(flag.getName());
//                if (o != null) {
//                    setFlag(request, flag, o);
//                }
//            }
//        } catch (SQLException ex) {
//            logger.warning(
//                    "Unable to load flags for request "
//                    + request.getId().toLowerCase() + ": " + ex.getMessage()
//            );
//        }
//    }
//
//    private <T> void setFlag(StakeRequest request, Flag<T> flag, Object rawValue) {
//        T val = flag.unmarshal(rawValue);
//        if (val == null) {
//            logger.warning("Failed to parse flag '" + flag.getName()
//                    + "' with value '" + rawValue.toString() + "'");
//            return;
//        }
//        request.setFlag(flag, val);
//    }
//
//    private void loadOwnersAndMembers(StakeRequest request) {
//        DefaultDomain owners = new DefaultDomain();
//        DefaultDomain members = new DefaultDomain();
//
//        try {
//            PreparedStatement usersStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`user`.`name`, " +
//                    "`request_players`.`owner` " +
//                    "FROM `request_players` " +
//                    "LEFT JOIN `user` ON ( " +
//                    "`request_players`.`user_id` = " +
//                    "`user`.`id`) " +
//                    "WHERE `request_players`.`request_id` = ? " +
//                    "AND `request_players`.`world_id` = " + this.worldDbId
//            );
//
//            usersStatement.setString(1, request.getId().toLowerCase());
//            ResultSet userSet = usersStatement.executeQuery();
//            while(userSet.next()) {
//                if (userSet.getBoolean("owner")) {
//                    owners.addPlayer(userSet.getString("name"));
//                } else {
//                    members.addPlayer(userSet.getString("name"));
//                }
//            }
//        } catch (SQLException ex) {
//            logger.warning("Unable to load users for request " + request.getId().toLowerCase() + ": " + ex.getMessage());
//        }
//
//        try {
//            PreparedStatement groupsStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`group`.`name`, " +
//                    "`request_groups`.`owner` " +
//                    "FROM `request_groups` " +
//                    "LEFT JOIN `group` ON ( " +
//                    "`request_groups`.`group_id` = " +
//                    "`group`.`id`) " +
//                    "WHERE `request_groups`.`request_id` = ? " +
//                    "AND `request_groups`.`world_id` = " + this.worldDbId
//            );
//
//            groupsStatement.setString(1, request.getId().toLowerCase());
//            ResultSet groupSet = groupsStatement.executeQuery();
//            while(groupSet.next()) {
//                if (groupSet.getBoolean("owner")) {
//                    owners.addGroup(groupSet.getString("name"));
//                } else {
//                    members.addGroup(groupSet.getString("name"));
//                }
//            }
//        } catch (SQLException ex) {
//            logger.warning("Unable to load groups for request " + request.getId().toLowerCase() + ": " + ex.getMessage());
//        }
//
//        request.setOwners(owners);
//        request.setMembers(members);
//    }
//
//    private void loadGlobal() {
//        Map<Long, StakeRequest> requests =
//                new HashMap<Long, StakeRequest>();
//
//        try {
//            PreparedStatement globalRequestStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`request`.`id`, " +
//                    "`request`.`priority`, " +
//                    "`parent`.`id` AS `parent` " +
//                    "FROM `request` " +
//                    "LEFT JOIN `request` AS `parent` " +
//                    "ON (`request`.`parent` = `parent`.`id` " +
//                    "AND `request`.`world_id` = `parent`.`world_id`) " +
//                    "WHERE `request`.`type` = 'global' " +
//                    "AND `request`.`world_id` = ? "
//            );
//
//            globalRequestStatement.setInt(1, this.worldDbId);
//            ResultSet globalResultSet = globalRequestStatement.executeQuery();
//
//            while (globalResultSet.next()) {
//                StakeRequest request = new GlobalRequest(globalResultSet.getString("id"));
//
//                request.setPriority(globalResultSet.getInt("priority"));
//
//                this.loadFlags(request);
//                this.loadOwnersAndMembers(request);
//
//                requests.put(globalResultSet.getString("id"), request);
//
//                String parentId = globalResultSet.getString("parent");
//                if (parentId != null) {
//                    parentSets.put(request, parentId);
//                }
//            }
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            logger.warning("Unable to load requests from sql database: " + ex.getMessage());
//            Throwable t = ex.getCause();
//            while (t != null) {
//                logger.warning("\t\tCause: " + t.getMessage());
//                t = t.getCause();
//            }
//        }
//
//        globalRequests = requests;
//    }
//
//    private void loadCuboid() {
//        Map<Long, StakeRequest> requests =
//                new HashMap<Long, StakeRequest>();
//
//        try {
//            PreparedStatement cuboidRequestStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`request_cuboid`.`min_z`, " +
//                    "`request_cuboid`.`min_y`, " +
//                    "`request_cuboid`.`min_x`, " +
//                    "`request_cuboid`.`max_z`, " +
//                    "`request_cuboid`.`max_y`, " +
//                    "`request_cuboid`.`max_x`, " +
//                    "`request`.`id`, " +
//                    "`request`.`priority`, " +
//                    "`parent`.`id` AS `parent` " +
//                    "FROM `request_cuboid` " +
//                    "LEFT JOIN `request` " +
//                    "ON (`request_cuboid`.`request_id` = `request`.`id` " +
//                    "AND `request_cuboid`.`world_id` = `request`.`world_id`) " +
//                    "LEFT JOIN `request` AS `parent` " +
//                    "ON (`request`.`parent` = `parent`.`id` " +
//                    "AND `request`.`world_id` = `parent`.`world_id`) " +
//                    "WHERE `request`.`world_id` = ? "
//            );
//
//            cuboidRequestStatement.setInt(1, this.worldDbId);
//            ResultSet cuboidResultSet = cuboidRequestStatement.executeQuery();
//
//            while (cuboidResultSet.next()) {
//                Vector pt1 = new Vector(
//                        cuboidResultSet.getInt("min_x"),
//                        cuboidResultSet.getInt("min_y"),
//                        cuboidResultSet.getInt("min_z")
//                );
//                Vector pt2 = new Vector(
//                        cuboidResultSet.getInt("max_x"),
//                        cuboidResultSet.getInt("max_y"),
//                        cuboidResultSet.getInt("max_z")
//                );
//
//                BlockVector min = Vector.getMinimum(pt1, pt2).toBlockVector();
//                BlockVector max = Vector.getMaximum(pt1, pt2).toBlockVector();
//                StakeRequest request = new ProtectedCuboidRequest(
//                        cuboidResultSet.getString("id"),
//                        min,
//                        max
//                );
//
//                request.setPriority(cuboidResultSet.getInt("priority"));
//
//                this.loadFlags(request);
//                this.loadOwnersAndMembers(request);
//
//                requests.put(cuboidResultSet.getString("id"), request);
//
//                String parentId = cuboidResultSet.getString("parent");
//                if (parentId != null) {
//                    parentSets.put(request, parentId);
//                }
//            }
//
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            logger.warning("Unable to load requests from sql database: " + ex.getMessage());
//            Throwable t = ex.getCause();
//            while (t != null) {
//                logger.warning("\t\tCause: " + t.getMessage());
//                t = t.getCause();
//            }
//        }
//
//        cuboidRequests = requests;
//    }
//
//    private void loadPoly2d() {
//        Map<Long, StakeRequest> requests =
//                new HashMap<Long, StakeRequest>();
//
//        try {
//            PreparedStatement poly2dRequestStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`request_poly2d`.`min_y`, " +
//                    "`request_poly2d`.`max_y`, " +
//                    "`request`.`id`, " +
//                    "`request`.`priority`, " +
//                    "`parent`.`id` AS `parent` " +
//                    "FROM `request_poly2d` " +
//                    "LEFT JOIN `request` " +
//                    "ON (`request_poly2d`.`request_id` = `request`.`id` " +
//                    "AND `request_poly2d`.`world_id` = `request`.`world_id`) " +
//                    "LEFT JOIN `request` AS `parent` " +
//                    "ON (`request`.`parent` = `parent`.`id` " +
//                    "AND `request`.`world_id` = `parent`.`world_id`) " +
//                    "WHERE `request`.`world_id` = ? "
//            );
//
//            poly2dRequestStatement.setInt(1, this.worldDbId);
//            ResultSet poly2dResultSet = poly2dRequestStatement.executeQuery();
//
//            PreparedStatement poly2dVectorStatement = this.conn.prepareStatement(
//                    "SELECT " +
//                    "`request_poly2d_point`.`x`, " +
//                    "`request_poly2d_point`.`z` " +
//                    "FROM `request_poly2d_point` " +
//                    "WHERE `request_poly2d_point`.`request_id` = ? " +
//                    "AND `request_poly2d_point`.`world_id` = " + this.worldDbId
//            );
//
//            while (poly2dResultSet.next()) {
//                String id = poly2dResultSet.getString("id");
//
//                Integer minY = poly2dResultSet.getInt("min_y");
//                Integer maxY = poly2dResultSet.getInt("max_y");
//                List<BlockVector2D> points = new ArrayList<BlockVector2D>();
//
//                poly2dVectorStatement.setString(1, id);
//                ResultSet poly2dVectorResultSet = poly2dVectorStatement.executeQuery();
//
//                while(poly2dVectorResultSet.next()) {
//                    points.add(new BlockVector2D(
//                            poly2dVectorResultSet.getInt("x"),
//                            poly2dVectorResultSet.getInt("z")
//                    ));
//                }
//                StakeRequest request = new ProtectedPolygonalRequest(id, points, minY, maxY);
//
//                request.setPriority(poly2dResultSet.getInt("priority"));
//
//                this.loadFlags(request);
//                this.loadOwnersAndMembers(request);
//
//                requests.put(poly2dResultSet.getString("id"), request);
//
//                String parentId = poly2dResultSet.getString("parent");
//                if (parentId != null) {
//                    parentSets.put(request, parentId);
//                }
//            }
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            logger.warning("Unable to load requests from sql database: " + ex.getMessage());
//            Throwable t = ex.getCause();
//            while (t != null) {
//                logger.warning("\t\tCause: " + t.getMessage());
//                t = t.getCause();
//            }
//        }
//
//        poly2dRequests = requests;
//    }
//
    @Override
    public void load() throws ProtectionDatabaseException {
//        try {
//            connect();
//        } catch (SQLException ex) {
//            throw new ProtectionDatabaseException(ex);
//        }
//
//        parentSets = new HashMap<StakeRequest,String>();
//
//        // We load the cuboid requests first, as this is likely to be the
//        // largest dataset. This should save time in regards to the putAll()s
//        this.loadCuboid();
//        Map<Long, StakeRequest> requests = this.cuboidRequests;
//        this.cuboidRequests = null;
//
//        this.loadPoly2d();
//        requests.putAll(this.poly2dRequests);
//        this.poly2dRequests = null;
//
//        this.loadGlobal();
//        requests.putAll(this.globalRequests);
//        this.globalRequests = null;
//
//        // Relink parents // Taken verbatim from YAMLDatabase
//        for (Map.Entry<StakeRequest, String> entry : parentSets.entrySet()) {
//            StakeRequest parent = requests.get(entry.getValue());
//            if (parent != null) {
//                try {
//                    entry.getKey().setParent(parent);
//                } catch (CircularInheritanceException e) {
//                    logger.warning("Circular inheritance detect with '"
//                            + entry.getValue() + "' detected as a parent");
//                }
//            } else {
//                logger.warning("Unknown request parent: " + entry.getValue());
//            }
//        }
//
//        this.requests = requests;
    }

//    /*
//     * Returns the database id for the user
//     * If it doesn't exits it adds the user and returns the id.
//     */
//    private Map<String,Integer> getUserIds(String... usernames) {
//        Map<String,Integer> users = new HashMap<String,Integer>();
//
//        if (usernames.length < 1) return users;
//
//        try {
//            PreparedStatement findUsersStatement = this.conn.prepareStatement(
//                    String.format(
//                            "SELECT " +
//                            "`user`.`id`, " +
//                            "`user`.`name` " +
//                            "FROM `user` " +
//                            "WHERE `name` IN (%s)",
//                            RequestDBUtil.preparePlaceHolders(usernames.length)
//                    )
//            );
//
//            RequestDBUtil.setValues(findUsersStatement, usernames);
//
//            ResultSet findUsersResults = findUsersStatement.executeQuery();
//
//            while(findUsersResults.next()) {
//                users.put(findUsersResults.getString("name"), findUsersResults.getInt("id"));
//            }
//
//            PreparedStatement insertUserStatement = this.conn.prepareStatement(
//                    "INSERT INTO " +
//                    "`user` ( " +
//                    "`id`, " +
//                    "`name`" +
//                    ") VALUES (null, ?)",
//                    Statement.RETURN_GENERATED_KEYS
//            );
//
//            for (String username : usernames) {
//                if (!users.containsKey(username)) {
//                    insertUserStatement.setString(1, username);
//                    insertUserStatement.execute();
//                    ResultSet generatedKeys = insertUserStatement.getGeneratedKeys();
//                    if (generatedKeys.first()) {
//                        users.put(username, generatedKeys.getInt(1));
//                    } else {
//                        logger.warning("Could not get the database id for user " + username);
//                    }
//                }
//            }
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//            logger.warning("Could not get the database id for the users " + usernames.toString() + "\n\t" + ex.getMessage());
//            Throwable t = ex.getCause();
//            while (t != null) {
//                logger.warning(t.getMessage());
//                t = t.getCause();
//            }
//        }
//
//        return users;
//    }
//
//
//    /*
//     * Returns the database id for the groups
//     * If it doesn't exits it adds the group and returns the id.
//     */
//    private Map<String,Integer> getGroupIds(String... groupnames) {
//        Map<String,Integer> groups = new HashMap<String,Integer>();
//
//        if (groupnames.length < 1) return groups;
//
//        try {
//            PreparedStatement findGroupsStatement = this.conn.prepareStatement(
//                    String.format(
//                            "SELECT " +
//                            "`group`.`id`, " +
//                            "`group`.`name` " +
//                            "FROM `group` " +
//                            "WHERE `name` IN (%s)",
//                            RequestDBUtil.preparePlaceHolders(groupnames.length)
//                    )
//            );
//
//            RequestDBUtil.setValues(findGroupsStatement, groupnames);
//
//            ResultSet findGroupsResults = findGroupsStatement.executeQuery();
//
//            while(findGroupsResults.next()) {
//                groups.put(findGroupsResults.getString("name"), findGroupsResults.getInt("id"));
//            }
//
//            PreparedStatement insertGroupStatement = this.conn.prepareStatement(
//                    "INSERT INTO " +
//                    "`group` ( " +
//                    "`id`, " +
//                    "`name`" +
//                    ") VALUES (null, ?)",
//                    Statement.RETURN_GENERATED_KEYS
//            );
//
//            for (String groupname : groupnames) {
//                if (!groups.containsKey(groupname)) {
//                    insertGroupStatement.setString(1, groupname);
//                    insertGroupStatement.execute();
//                    ResultSet generatedKeys = insertGroupStatement.getGeneratedKeys();
//                    if (generatedKeys.first()) {
//                        groups.put(groupname, generatedKeys.getInt(1));
//                    } else {
//                        logger.warning("Could not get the database id for user " + groupname);
//                    }
//                }
//            }
//        } catch (SQLException ex) {
//            logger.warning("Could not get the database id for the groups " + groupnames.toString() + ex.getMessage());
//        }
//
//        return groups;
//    }
//
//    /*
//     * As we don't get notified on the creation/removal of requests:
//     *  1) We get a list of all of the in-database requests
//     *  2) We iterate over all of the in-memory requests
//     *  2a) If the request is in the database, we update the database and
//     *      remove the request from the in-database list
//     *   b) If the request is not in the database, we insert it
//     *  3) We iterate over what remains of the in-database list and remove
//     *     them from the database
//     *
//     * TODO: Look at adding/removing/updating the database when the in
//     *       memory request is created/remove/updated
//     *
//     * @see org.stakeaclaim.stakes.databases.ProtectionDatabase#save()
//     */
    @Override
    public void save() throws ProtectionDatabaseException {
//        try {
//            connect();
//        } catch (SQLException ex) {
//            throw new ProtectionDatabaseException(ex);
//        }
//
//        List<String> requestsInDatabase = new ArrayList<String>();
//
//        try {
//            PreparedStatement getAllRequestsStatement = this.conn.prepareStatement(
//                    "SELECT `request`.`id` FROM " +
//                    "`request` " +
//                    "WHERE `world_id` = ? "
//            );
//
//            getAllRequestsStatement.setInt(1, this.worldDbId);
//            ResultSet getAllRequestsResult = getAllRequestsStatement.executeQuery();
//
//            while(getAllRequestsResult.next()) {
//                requestsInDatabase.add(getAllRequestsResult.getString("id"));
//            }
//        } catch (SQLException ex) {
//            logger.warning("Could not get request list for save comparison: " + ex.getMessage());
//        }
//
//        for (Map.Entry<long, StakeRequest> entry : requests.entrySet()) {
//            String name = entry.getKey();
//            StakeRequest request = entry.getValue();
//
//            try {
//                if (requestsInDatabase.contains(name)) {
//                    requestsInDatabase.remove(name);
//
//                    if (request instanceof ProtectedCuboidRequest) {
//                        updateRequestCuboid( (ProtectedCuboidRequest) request );
//                    } else if (request instanceof ProtectedPolygonalRequest) {
//                        updateRequestPoly2D( (ProtectedPolygonalRequest) request );
//                    } else if (request instanceof GlobalRequest) {
//                        updateRequestGlobal( (GlobalRequest) request );
//                    } else {
//                        this.updateRequest(request, request.getClass().getCanonicalName());
//                    }
//                } else {
//                    if (request instanceof ProtectedCuboidRequest) {
//                        insertRequestCuboid( (ProtectedCuboidRequest) request );
//                    } else if (request instanceof ProtectedPolygonalRequest) {
//                        insertRequestPoly2D( (ProtectedPolygonalRequest) request );
//                    } else if (request instanceof GlobalRequest) {
//                        insertRequestGlobal( (GlobalRequest) request );
//                    } else {
//                        this.insertRequest(request, request.getClass().getCanonicalName());
//                    }
//                }
//            } catch (SQLException ex) {
//                logger.warning("Could not save request " + request.getId().toLowerCase() + ": " + ex.getMessage());
//                throw new ProtectionDatabaseException(ex);
//            }
//        }
//
//        for (Map.Entry<long, StakeRequest> entry : requests.entrySet()) {
//            try {
//                if (entry.getValue().getParent() == null) continue;
//
//                PreparedStatement setParentStatement = this.conn.prepareStatement(
//                        "UPDATE `request` SET " +
//                        "`parent` = ? " +
//                        "WHERE `id` = ? AND `world_id` = " + this.worldDbId
//                );
//
//                setParentStatement.setString(1, entry.getValue().getParent().getId().toLowerCase());
//                setParentStatement.setString(2, entry.getValue().getId().toLowerCase());
//
//                setParentStatement.execute();
//            } catch (SQLException ex) {
//                logger.warning("Could not save request parents " + entry.getValue().getId().toLowerCase() + ": " + ex.getMessage());
//                throw new ProtectionDatabaseException(ex);
//            }
//        }
//
//        for (String name : requestsInDatabase) {
//            try {
//                PreparedStatement removeRequest = this.conn.prepareStatement(
//                        "DELETE FROM `request` WHERE `id` = ? "
//                );
//
//                removeRequest.setString(1, name);
//                removeRequest.execute();
//            } catch (SQLException ex) {
//                logger.warning("Could not remove request from database " + name + ": " + ex.getMessage());
//            }
//        }
//
    }

//    private void updateFlags(StakeRequest request) throws SQLException {
//        PreparedStatement clearCurrentFlagStatement = this.conn.prepareStatement(
//                "DELETE FROM `request_flag` " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId
//        );
//
//        clearCurrentFlagStatement.setString(1, request.getId().toLowerCase());
//        clearCurrentFlagStatement.execute();
//
//        for (Map.Entry<Flag<?>, Object> entry : request.getFlags().entrySet()) {
//            if (entry.getValue() == null) continue;
//
//            Object flag = sqlMarshal(marshalFlag(entry.getKey(), entry.getValue()));
//
//            PreparedStatement insertFlagStatement = this.conn.prepareStatement(
//                    "INSERT INTO `request_flag` ( " +
//                    "`id`, " +
//                    "`request_id`, " +
//                    "`world_id`, " +
//                    "`flag`, " +
//                    "`value` " +
//                    ") VALUES (null, ?, " + this.worldDbId + ", ?, ?)"
//            );
//
//            insertFlagStatement.setString(1, request.getId().toLowerCase());
//            insertFlagStatement.setString(2, entry.getKey().getName());
//            insertFlagStatement.setObject(3, flag);
//
//            insertFlagStatement.execute();
//        }
//    }
//
//    private void updatePlayerAndGroups(StakeRequest request, Boolean owners) throws SQLException {
//        DefaultDomain domain;
//
//        if (owners) {
//            domain = request.getOwners();
//        } else {
//            domain = request.getMembers();
//        }
//
//        PreparedStatement deleteUsersForRequest = this.conn.prepareStatement(
//                "DELETE FROM `request_players` " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId + " " +
//                "AND `owner` = ?"
//        );
//
//        deleteUsersForRequest.setString(1, request.getId().toLowerCase());
//        deleteUsersForRequest.setBoolean(2, owners);
//        deleteUsersForRequest.execute();
//
//        PreparedStatement insertUsersForRequest = this.conn.prepareStatement(
//                "INSERT INTO `request_players` " +
//                "(`request_id`, `world_id`, `user_id`, `owner`) " +
//                "VALUES (?, " + this.worldDbId + ",  ?, ?)"
//        );
//
//        Set<String> var = domain.getPlayers();
//
//        for (Integer player : getUserIds(var.toArray(new String[var.size()])).values()) {
//            insertUsersForRequest.setString(1, request.getId().toLowerCase());
//            insertUsersForRequest.setInt(2, player);
//            insertUsersForRequest.setBoolean(3, owners);
//
//            insertUsersForRequest.execute();
//        }
//
//        PreparedStatement deleteGroupsForRequest = this.conn.prepareStatement(
//                "DELETE FROM `request_groups` " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId + " " +
//                "AND `owner` = ?"
//        );
//
//        deleteGroupsForRequest.setString(1, request.getId().toLowerCase());
//        deleteGroupsForRequest.setBoolean(2, owners);
//        deleteGroupsForRequest.execute();
//
//        PreparedStatement insertGroupsForRequest = this.conn.prepareStatement(
//                "INSERT INTO `request_groups` " +
//                "(`request_id`, `world_id`, `group_id`, `owner`) " +
//                "VALUES (?, " + this.worldDbId + ",  ?, ?)"
//        );
//
//        Set<String> groupVar = domain.getGroups();
//        for (Integer group : getGroupIds(groupVar.toArray(new String[groupVar.size()])).values()) {
//            insertGroupsForRequest.setString(1, request.getId().toLowerCase());
//            insertGroupsForRequest.setInt(2, group);
//            insertGroupsForRequest.setBoolean(3, owners);
//
//            insertGroupsForRequest.execute();
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private <V> Object marshalFlag(Flag<V> flag, Object val) {
//        return flag.marshal( (V) val );
//    }
//
//    private void insertRequest(StakeRequest request, String type) throws SQLException {
//        PreparedStatement insertRequestStatement = this.conn.prepareStatement(
//                "INSERT INTO `request` (" +
//                "`id`, " +
//                "`world_id`, " +
//                "`type`, " +
//                "`priority`, " +
//                "`parent` " +
//                ") VALUES (?, ?, ?, ?, null)"
//        );
//
//        insertRequestStatement.setString(1, request.getId().toLowerCase());
//        insertRequestStatement.setInt(2, this.worldDbId);
//        insertRequestStatement.setString(3, type);
//        insertRequestStatement.setInt(4, request.getPriority());
//
//        insertRequestStatement.execute();
//
//        updateFlags(request);
//
//        updatePlayerAndGroups(request, false);
//        updatePlayerAndGroups(request, true);
//    }
//
//    private void insertRequestCuboid(ProtectedCuboidRequest request) throws SQLException {
//        insertRequest(request, "cuboid");
//
//        PreparedStatement insertCuboidRequestStatement = this.conn.prepareStatement(
//                "INSERT INTO `request_cuboid` (" +
//                "`request_id`, " +
//                "`world_id`, " +
//                "`min_z`, " +
//                "`min_y`, " +
//                "`min_x`, " +
//                "`max_z`, " +
//                "`max_y`, " +
//                "`max_x` " +
//                ") VALUES (?, " + this.worldDbId + ", ?, ?, ?, ?, ?, ?)"
//        );
//
//        BlockVector min = request.getMinimumPoint();
//        BlockVector max = request.getMaximumPoint();
//
//        insertCuboidRequestStatement.setString(1, request.getId().toLowerCase());
//        insertCuboidRequestStatement.setInt(2, min.getBlockZ());
//        insertCuboidRequestStatement.setInt(3, min.getBlockY());
//        insertCuboidRequestStatement.setInt(4, min.getBlockX());
//        insertCuboidRequestStatement.setInt(5, max.getBlockZ());
//        insertCuboidRequestStatement.setInt(6, max.getBlockY());
//        insertCuboidRequestStatement.setInt(7, max.getBlockX());
//
//        insertCuboidRequestStatement.execute();
//    }
//
//    private void insertRequestPoly2D(ProtectedPolygonalRequest request) throws SQLException {
//        insertRequest(request, "poly2d");
//
//        PreparedStatement insertPoly2dRequestStatement = this.conn.prepareStatement(
//                "INSERT INTO `request_poly2d` (" +
//                "`request_id`, " +
//                "`world_id`, " +
//                "`max_y`, " +
//                "`min_y` " +
//                ") VALUES (?, " + this.worldDbId + ", ?, ?)"
//        );
//
//        insertPoly2dRequestStatement.setString(1, request.getId().toLowerCase());
//        insertPoly2dRequestStatement.setInt(2, request.getMaximumPoint().getBlockY());
//        insertPoly2dRequestStatement.setInt(3, request.getMinimumPoint().getBlockY());
//
//        insertPoly2dRequestStatement.execute();
//
//        updatePoly2dPoints(request);
//    }
//
//    private void updatePoly2dPoints(ProtectedPolygonalRequest request) throws SQLException {
//        PreparedStatement clearPoly2dPointsForRequestStatement = this.conn.prepareStatement(
//                "DELETE FROM `request_poly2d_point` " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId
//        );
//
//        clearPoly2dPointsForRequestStatement.setString(1, request.getId().toLowerCase());
//
//        clearPoly2dPointsForRequestStatement.execute();
//
//        PreparedStatement insertPoly2dPointStatement = this.conn.prepareStatement(
//                "INSERT INTO `request_poly2d_point` (" +
//                "`id`, " +
//                "`request_id`, " +
//                "`world_id`, " +
//                "`z`, " +
//                "`x` " +
//                ") VALUES (null, ?, " + this.worldDbId + ", ?, ?)"
//        );
//
//        String lowerId = request.getId();
//        for (BlockVector2D point : request.getPoints()) {
//            insertPoly2dPointStatement.setString(1, lowerId);
//            insertPoly2dPointStatement.setInt(2, point.getBlockZ());
//            insertPoly2dPointStatement.setInt(3, point.getBlockX());
//
//            insertPoly2dPointStatement.execute();
//        }
//    }
//
//    private void insertRequestGlobal(GlobalRequest request) throws SQLException {
//        insertRequest(request, "global");
//    }
//
//    private void updateRequest(StakeRequest request, String type) throws SQLException  {
//        PreparedStatement updateRequestStatement = this.conn.prepareStatement(
//                "UPDATE `request` SET " +
//                "`priority` = ? WHERE `id` = ? AND `world_id` = " + this.worldDbId
//        );
//
//        updateRequestStatement.setInt(1, request.getPriority());
//        updateRequestStatement.setString(2, request.getId().toLowerCase());
//
//        updateRequestStatement.execute();
//
//        updateFlags(request);
//
//        updatePlayerAndGroups(request, false);
//        updatePlayerAndGroups(request, true);
//    }
//
//    private void updateRequestCuboid(ProtectedCuboidRequest request) throws SQLException  {
//        updateRequest(request, "cuboid");
//
//        PreparedStatement updateCuboidRequestStatement = this.conn.prepareStatement(
//                "UPDATE `request_cuboid` SET " +
//                "`min_z` = ?, " +
//                "`min_y` = ?, " +
//                "`min_x` = ?, " +
//                "`max_z` = ?, " +
//                "`max_y` = ?, " +
//                "`max_x` = ? " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId
//        );
//
//        BlockVector min = request.getMinimumPoint();
//        BlockVector max = request.getMaximumPoint();
//
//        updateCuboidRequestStatement.setInt(1, min.getBlockZ());
//        updateCuboidRequestStatement.setInt(2, min.getBlockY());
//        updateCuboidRequestStatement.setInt(3, min.getBlockX());
//        updateCuboidRequestStatement.setInt(4, max.getBlockZ());
//        updateCuboidRequestStatement.setInt(5, max.getBlockY());
//        updateCuboidRequestStatement.setInt(6, max.getBlockX());
//        updateCuboidRequestStatement.setString(7, request.getId().toLowerCase());
//
//        updateCuboidRequestStatement.execute();
//    }
//
//    private void updateRequestPoly2D(ProtectedPolygonalRequest request) throws SQLException  {
//        updateRequest(request, "poly2d");
//
//        PreparedStatement updatePoly2dRequestStatement = this.conn.prepareStatement(
//                "UPDATE `request_poly2d` SET " +
//                "`max_y` = ?, " +
//                "`min_y` = ? " +
//                "WHERE `request_id` = ? " +
//                "AND `world_id` = " + this.worldDbId
//        );
//
//        updatePoly2dRequestStatement.setInt(1, request.getMaximumPoint().getBlockY());
//        updatePoly2dRequestStatement.setInt(2, request.getMinimumPoint().getBlockY());
//        updatePoly2dRequestStatement.setString(3, request.getId().toLowerCase());
//
//        updatePoly2dRequestStatement.execute();
//
//        updatePoly2dPoints(request);
//    }
//
//    private void updateRequestGlobal(GlobalRequest request) throws SQLException {
//        updateRequest(request, "global");
//    }
//
    @Override
    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

    @Override
    public void setRequests(Map<Long, StakeRequest> requests) {
        this.requests = requests;
    }
    
//    protected Object sqlUnmarshal(String rawValue) {
//        try {
//            return yaml.load(rawValue);
//        } catch (YAMLException e) {
//            return String.valueOf(rawValue);
//        }
//    }
//    
//    protected String sqlMarshal(Object rawObject) {
//        return yaml.dump(rawObject);
//    }
}
