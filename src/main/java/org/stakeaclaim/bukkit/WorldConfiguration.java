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

package org.stakeaclaim.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;

import com.sk89q.worldguard.bukkit.WGBukkit;

/**
 * Holds the configuration for individual worlds.
 */
public class WorldConfiguration {

    public static final String CONFIG_HEADER = "#\r\n" +
            "# StakeAClaim's world configuration file\r\n" +
            "#\r\n" +
            "# This is a world configuration file. Anything placed into here will only\r\n" +
            "# affect this world. If you don't put anything in this file, then the\r\n" +
            "# settings will be inherited from the main configuration file.\r\n" +
            "#\r\n" +
            "# If you see {} below, that means that there are NO entries in this file.\r\n" +
            "# Remove the {} and add your own entries.\r\n" +
            "#\r\n";

    private StakeAClaimPlugin plugin;

    private String worldName;
    private YAMLProcessor parentConfig;
    private YAMLProcessor config;

    /* Configuration data start */
    public boolean summaryOnStart;
    public boolean opPermissions;
    public boolean useSAC;
    public boolean useRequests;
    public boolean useRegions;
    public int sacWand;
    public String claimNameFilter;
    public boolean useReclaimed;
    public boolean claimLimitsAreArea;
    public double selfClaimMax;
    public double claimMax;
    public double proxyClaimMax;
    public boolean showReclaimOnStake;
    public boolean twoStepSelfClaim;
    public boolean createRequest;
    public boolean addOwner;
    /* Configuration data end */

    /**
     * Construct the object.
     *
     * @param plugin The StakeAClaimPlugin instance
     * @param worldName The world name that this WorldConfiguration is for.
     * @param parentConfig The parent configuration to read defaults from
     */
    public WorldConfiguration(StakeAClaimPlugin plugin, String worldName, YAMLProcessor parentConfig) {
        File baseFolder = new File(plugin.getDataFolder(), "worlds/" + worldName);
        File configFile = new File(baseFolder, "config.yml");

        this.plugin = plugin;
        this.worldName = worldName;
        this.parentConfig = parentConfig;

        plugin.createDefaultConfiguration(configFile, "config_world.yml");

        config = new YAMLProcessor(configFile, true, YAMLFormat.EXTENDED);
        loadConfiguration();

        if (summaryOnStart) {
            plugin.getLogger().info("Loaded configuration for world '" + worldName + "'");
        }
    }

    private boolean getBoolean(String node, boolean def) {
        boolean val = parentConfig.getBoolean(node, def);

        if (config.getProperty(node) != null) {
            return config.getBoolean(node, def);
        } else {
            return val;
        }
    }

    private String getString(String node, String def) {
        String val = parentConfig.getString(node, def);

        if (config.getProperty(node) != null) {
            return config.getString(node, def);
        } else {
            return val;
        }
    }

    private int getInt(String node, int def) {
        int val = parentConfig.getInt(node, def);

        if (config.getProperty(node) != null) {
            return config.getInt(node, def);
        } else {
            return val;
        }
    }

    private double getDouble(String node, double def) {
        double val = parentConfig.getDouble(node, def);

        if (config.getProperty(node) != null) {
            return config.getDouble(node, def);
        } else {
            return val;
        }
    }

    private Object getProperty(String node) {
        Object res = parentConfig.getProperty(node);

        if (config.getProperty(node) != null) {
            res = config.getProperty(node);
        }

        return res;
    }

    /**
     * Load the configuration.
     */
    private void loadConfiguration() {
        try {
            config.load();
        } catch (IOException e) {
            plugin.getLogger().severe("Error reading configuration for world " + worldName + ": ");
            e.printStackTrace();
        }

        summaryOnStart = getBoolean("summary-on-start", true);
        opPermissions = getBoolean("op-permissions", true);
        useSAC = getBoolean("master-enable", true);
        useRequests = getBoolean("requests-enable", true);
        sacWand = getInt("wand", 288); // Feather
        claimNameFilter = getString("claim-name-regex-filter-string", "^[NSns]\\d\\d?[EWew]\\d\\d?$"); // match eg. s2w45
        useReclaimed = getBoolean("set-status-to-reclaimed-on-reclaim", true);
        claimLimitsAreArea = getBoolean("claiming.claim-max-is-in-area-of-claims", false);
        selfClaimMax = getDouble("claiming.max-claims-a-player-can-stake-on-their-own", 1);
        claimMax = getDouble("claiming.max-claims-a-player-can-request", 3);
        proxyClaimMax = getDouble("claiming.max-claims-a-proxy-can-request", -1);
        showReclaimOnStake = getBoolean("claiming.show-past-reclaimed-note", true);
        twoStepSelfClaim = getBoolean("claiming.players-need-to-confirm-when-self-accepting", true);
        createRequest = getBoolean("error-handling.create-request-for-claims-with-an-owner", true);
        addOwner = getBoolean("error-handling.add-owner-to-claims-with-an-accepted-request", true);

        useRegions = WGBukkit.getPlugin().getGlobalStateManager().get(plugin.getServer().getWorld(worldName)).useRegions;

        if (!useRegions || !useSAC) {
            useRequests = false;
        }

        config.setHeader(CONFIG_HEADER);

        config.save();
    }

    public String getWorldName() {
        return this.worldName;
    }
}
