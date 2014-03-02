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

package com.nineteengiraffes.stakeaclaim.stakes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLNode;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.flags.StateFlag.State;

public class StakeManager {

    private YAMLProcessor config;
    private Map<String, Stake> stakes;
    private final Logger logger;

    /**
     * Construct the object.
     */
    public StakeManager(File file, Logger logger) throws StakeDatabaseException, FileNotFoundException {
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

    /**
     * Load stakes from file
     * 
     * @throws StakeDatabaseException
     */
    public void load() throws StakeDatabaseException {
        try {
            config.load();
        } catch (IOException e) {
            throw new StakeDatabaseException(e);
        }
        
        Map<String, YAMLNode> stakeData = config.getNodes("regions");
        
        // File has no stakes
        if (stakeData == null) {
            this.stakes = new HashMap<String, Stake>();
            return;
        }

        Map<String, Stake> stakes = new HashMap<String, Stake>();

        for (Map.Entry<String, YAMLNode> entry : stakeData.entrySet()) {

            try {
                boolean hasData = false;

                // Get ID
                String id = entry.getKey().toLowerCase().replace(".", "");
                Stake stake = new Stake(id);

                YAMLNode node = entry.getValue();

                // Get stake status and player
                String statusString = node.getString("stake.status");
                String stakeName = node.getString("stake.player");
                if (statusString != null && stakeName != null) {
                    Status status = Status.valueOf(statusString.toUpperCase());
                    stake.setStatus(status);
                    stake.setStakeName(stakeName);
                    hasData = true;
                }

                // Get default entry
                String entryString = node.getString("default-entry");
                if (entryString != null) {
                    State defaultEntry = State.valueOf(entryString.toUpperCase());
                    stake.setDefaultEntry(defaultEntry);
                    hasData = true;
                }

                // Get reclaimed state
                Boolean reclaimed = node.getBoolean("reclaimed");
                if (reclaimed != null) {
                    stake.setRecalimed(reclaimed);
                    hasData = true;
                }

                // Get VIP state
                Boolean vip = node.getBoolean("vip");
                if (vip != null) {
                    stake.setVIP(vip);
                    hasData = true;
                }

                // Get custom name
                String claimName = node.getString("custom-name");
                if (claimName != null) {
                    stake.setClaimName(claimName);
                    hasData = true;
                }

                // Only use stakes that have data
                if (hasData) {
                    stakes.put(id, stake);
                }

            } catch (NullPointerException e) {
                logger.warning("Missing data for stake '" + entry.getKey() + '"' + e.getMessage());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid status for stake '" + entry.getKey() + '"');
            }
        }

        this.stakes = stakes;
    }

    /**
     * Commit stakes to file
     */
    public void save(){
        config.clear();
        
        for (Map.Entry<String, Stake> entry : stakes.entrySet()) {

            Stake stake = entry.getValue();

            // Skip if stake has nothing to save
            if (!(stake.getStatus() != null && stake.getStakeName() != null) &&
                    stake.getDefaultEntry() == null && stake.getClaimName() == null &&
                    !stake.getVIP() && !stake.getReclaimed()) {
                continue;
            }

            YAMLNode node = config.addNode("regions." + entry.getKey().toString());

            if (stake.getStatus() != null && stake.getStakeName() != null) {
                node.setProperty("stake.status", stake.getStatus().name());
                node.setProperty("stake.player", stake.getStakeName());
            }
            if (stake.getDefaultEntry() != null) {
                node.setProperty("default-entry", stake.getDefaultEntry().name());
            }
            if (stake.getReclaimed()) {
                node.setProperty("reclaimed", true);
            }
            if (stake.getVIP()) {
                node.setProperty("vip", true);
            }
            if (stake.getClaimName() != null) {
                node.setProperty("custom-name", stake.getClaimName());
            }
        }

        config.setHeader("#\r\n" +
                "# StakeAClaim stake file\r\n" +
                "#\r\n" +
                "# WARNING: THIS FILE IS AUTOMATICALLY GENERATED. If you modify this file by\r\n" +
                "# hand, be aware that A SINGLE MISTYPED CHARACTER CAN CORRUPT THE FILE. If\r\n" +
                "# StakeAClaim is unable to parse the file, your stakes will FAIL TO LOAD and\r\n" +
                "# the contents of this file will reset. Please use a YAML validator such as\r\n" +
                "# http://yaml-online-parser.appspot.com (for smaller files).\r\n" +
                "#\r\n" +
                "# REMEMBER TO KEEP PERIODICAL BACKUPS.\r\n" +
                "#");
        config.save();
    }

    /**
     * Get map of stakes
     * 
     * @return map of stakes
     */
    public Map<String, Stake> getStakes() {
        return this.stakes;
    }

    /**
     * Get a stake by its ID,
     * if it does not exists it will be created
     *
     * @param id id of the stake, can be mixed-case
     * @return stake
     */
    public Stake getStake(String id) {
        Stake stake = this.stakes.get(id.toLowerCase());
        if (stake == null) {
            stake = new Stake(id);
            this.stakes.put(id, stake);
        }
        return stake;
    }

}
