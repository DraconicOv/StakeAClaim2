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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
//import org.stakeaclaim.domains.DefaultDomain;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.flags.StateFlag;
//import org.stakeaclaim.stakes.flags.StateFlag.State;
//import org.stakeaclaim.stakes.requests.ProtectedCuboidRequest;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.requests.Request.CircularInheritanceException;
//import org.stakeaclaim.util.ArrayReader;

/**
 * Represents a protected area database that uses CSV files.
 *
 * @author sk89q
 */
public class CSVDatabase extends AbstractStakeDatabase {
    
//    private static final Map<String, StateFlag> legacyFlagCodes = new HashMap<String, StateFlag>();
//    static {
//        legacyFlagCodes.put("z", DefaultFlag.PASSTHROUGH);
//        legacyFlagCodes.put("b", DefaultFlag.BUILD);
//        legacyFlagCodes.put("p", DefaultFlag.PVP);
//        legacyFlagCodes.put("m", DefaultFlag.MOB_DAMAGE);
//        legacyFlagCodes.put("c", DefaultFlag.CREEPER_EXPLOSION);
//        legacyFlagCodes.put("t", DefaultFlag.TNT);
//        legacyFlagCodes.put("l", DefaultFlag.LIGHTER);
//        legacyFlagCodes.put("f", DefaultFlag.FIRE_SPREAD);
//        legacyFlagCodes.put("F", DefaultFlag.LAVA_FIRE);
//        legacyFlagCodes.put("C", DefaultFlag.CHEST_ACCESS);
//    }

    private final Logger logger;

    /**
     * References the CSV file.
     */
    private final File file;
    
    /**
     * Holds the list of requests.
     */
    private Map<Long, StakeRequest> requests;

    /**
     * Construct the database with a path to a file. No file is read or
     * written at this time.
     *
     * @param file The file in CSV format containing the request database
     * @param logger The logger to log errors to
     */
    public CSVDatabase(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /**
     * Saves the database.
     */
    public void save() throws StakeDatabaseException {
        throw new UnsupportedOperationException("CSV format is no longer implemented");
    }

    public void load() throws StakeDatabaseException {
//        Map<Long, StakeRequest> requests =
//                new HashMap<Long, StakeRequest>();
//        Map<StakeRequest,String> parentSets =
//                new LinkedHashMap<StakeRequest, String>();
//
//        CSVReader reader = null;
//        try {
//            reader = new CSVReader(new FileReader(file));
//
//            String[] line;
//
//            while ((line = reader.readNext()) != null) {
//                if (line.length < 2) {
//                    logger.warning("Invalid request definition: " + line);
//                    continue;
//                }
//
//                String id = line[0].toLowerCase().replace(".", "");
//                String type = line[1];
//                ArrayReader<String> entries = new ArrayReader<String>(line);
//
//                if (type.equalsIgnoreCase("cuboid")) {
//                    if (line.length < 8) {
//                        logger.warning("Invalid request definition: " + line);
//                        continue;
//                    }
//
//                    Vector pt1 = new Vector(
//                            Integer.parseInt(line[2]),
//                            Integer.parseInt(line[3]),
//                            Integer.parseInt(line[4]));
//                    Vector pt2 = new Vector(
//                            Integer.parseInt(line[5]),
//                            Integer.parseInt(line[6]),
//                            Integer.parseInt(line[7]));
//
//                    BlockVector min = Vector.getMinimum(pt1, pt2).toBlockVector();
//                    BlockVector max = Vector.getMaximum(pt1, pt2).toBlockVector();
//
//                    int priority = entries.get(8) == null ? 0 : Integer.parseInt(entries.get(8));
//                    String ownersData = entries.get(9);
//                    String flagsData = entries.get(10);
//                    //String enterMessage = nullEmptyString(entries.get(11));
//
//                    StakeRequest request = new ProtectedCuboidRequest(id, min, max);
//                    request.setPriority(priority);
//                    parseFlags(request, flagsData);
//                    request.setOwners(this.parseDomains(ownersData));
//                    requests.put(id, request);
//                } else if (type.equalsIgnoreCase("cuboid.2")) {
//                    Vector pt1 = new Vector(
//                            Integer.parseInt(line[2]),
//                            Integer.parseInt(line[3]),
//                            Integer.parseInt(line[4]));
//                    Vector pt2 = new Vector(
//                            Integer.parseInt(line[5]),
//                            Integer.parseInt(line[6]),
//                            Integer.parseInt(line[7]));
//
//                    BlockVector min = Vector.getMinimum(pt1, pt2).toBlockVector();
//                    BlockVector max = Vector.getMaximum(pt1, pt2).toBlockVector();
//
//                    int priority = entries.get(8) == null ? 0 : Integer.parseInt(entries.get(8));
//                    String parentId = entries.get(9);
//                    String ownersData = entries.get(10);
//                    String membersData = entries.get(11);
//                    String flagsData = entries.get(12);
//                    //String enterMessage = nullEmptyString(entries.get(13));
//                    //String leaveMessage = nullEmptyString(entries.get(14));
//
//                    StakeRequest request = new ProtectedCuboidRequest(id, min, max);
//                    request.setPriority(priority);
//                    parseFlags(request, flagsData);
//                    request.setOwners(this.parseDomains(ownersData));
//                    request.setMembers(this.parseDomains(membersData));
//                    requests.put(id, request);
//
//                    // Link children to parents later
//                    if (parentId.length() > 0) {
//                        parentSets.put(request, parentId);
//                    }
//                }
//            }
//        } catch (IOException e) {
//            throw new StakeDatabaseException(e);
//        } finally {
//            try {
//                reader.close();
//            } catch (IOException ignored) {
//            }
//        }
//
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

//    /**
//     * Used to parse the specified domain in the CSV file.
//     *
//     * @param data The domain data as a string
//     * @return The domain data as a DefaultDomain
//     */
//    private DefaultDomain parseDomains(String data) {
//        if (data == null) {
//            return new DefaultDomain();
//        }
//
//        DefaultDomain domain = new DefaultDomain();
//        Pattern pattern = Pattern.compile("^([A-Za-z]):(.*)$");
//
//        String[] parts = data.split(",");
//
//        for (String part : parts) {
//            if (part.trim().length() == 0) {
//                continue;
//            }
//
//            Matcher matcher = pattern.matcher(part);
//
//            if (!matcher.matches()) {
//                logger.warning("Invalid owner specification: " + part);
//                continue;
//            }
//
//            String type = matcher.group(1);
//            String id = matcher.group(2);
//
//            if (type.equals("u")) {
//                domain.addPlayer(id);
//            } else if (type.equals("g")) {
//                domain.addGroup(id);
//            } else {
//                logger.warning("Unknown owner specification: " + type);
//            }
//        }
//
//        return domain;
//    }

//    /**
//     * Used to parse the list of flags.
//     *
//     * @param data The flag data in string format
//     */
//    private void parseFlags(StakeRequest request, String data) {
//        if (data == null) {
//            return;
//        }
//
//        State curState = State.ALLOW;
//
//        for (int i = 0; i < data.length(); i++) {
//            char k = data.charAt(i);
//            if (k == '+') {
//                curState = State.ALLOW;
//            } else if (k == '-') {
//                curState = State.DENY;
//            } else {
//                String flagStr;
//                if (k == '_') {
//                    if (i == data.length() - 1) {
//                        logger.warning("_ read ahead fail");
//                        break;
//                    }
//                    flagStr = "_" + data.charAt(i + 1);
//                    i++;
//
//                    logger.warning("_? custom flags are no longer supported");
//                    continue;
//                } else {
//                    flagStr = String.valueOf(k);
//                }
//
//                StateFlag flag = legacyFlagCodes.get(flagStr);
//                if (flag != null) {
//                    request.setFlag(flag, curState);
//                } else {
//                    logger.warning("Legacy flag '" + flagStr + "' is unsupported");
//                }
//            }
//        }
//    }

    /**
     * Used to write the list of domains.
     *
     * @param domain
     * @return
     */
/*    private String writeDomains(DefaultDomain domain) {
        StringBuilder str = new StringBuilder();

        for (String player : domain.getPlayers()) {
            str.append("u:" + player + ",");
        }

        for (String group : domain.getGroups()) {
            str.append("g:" + group + ",");
        }

        return str.length() > 0 ?
                str.toString().substring(0, str.length() - 1) : "";
    }*/

    /**
     * Helper method to prepend '+' or '-' in front of a flag according
     * to the flag's state.
     *
     * @param state
     * @param flag
     * @return
     */
/*
    private String writeFlag(State state, String flag) {
        if (state == State.ALLOW) {
            return "+" + flag;
        } else if (state == State.DENY) {
            return "-" + flag;
        }

        return "";
    }
*/

    /**
     * Returns a null if a string is null or empty.
     *
     * @param str The string to format
     * @return null if the string is empty or null, otherwise the provided string
     */
    protected String nullEmptyString(String str) {
        if (str == null) {
            return null;
        } else if (str.length() == 0) {
            return null;
        } else {
            return str;
        }
    }

    public Map<Long, StakeRequest> getRequests() {
        return requests;
    }

    public void setRequests(Map<Long, StakeRequest> requests) {
        this.requests = requests;
    }
}
