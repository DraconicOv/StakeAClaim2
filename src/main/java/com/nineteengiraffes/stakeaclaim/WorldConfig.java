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

package com.nineteengiraffes.stakeaclaim;

import java.io.File;
import java.io.IOException;

import org.bukkit.Material;

import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.bukkit.WGBukkit;

/**
 * Holds the configuration for individual worlds.
 */
public class WorldConfig {

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
    public boolean useStakes;
    public boolean useRegions;
    public Material sacWand;
    public String claimNameFilter;
    public boolean useReclaimed;
    public boolean useVolumeLimits;
    public int unassistedMaxCount;
    public int totalMaxCount;
    public int proxyMaxCount;
    public int unassistedMaxVolume;
    public int totalMaxVolume;
    public int proxyMaxVolume;
    public boolean confirmUnassisted;
    public String VIPs;
    public boolean silentNotify;
    /* Configuration data end */

    /**
     * Construct the object.
     *
     * @param plugin The StakeAClaimPlugin instance
     * @param worldName The world name that this WorldConfiguration is for.
     * @param parentConfig The parent configuration to read defaults from
     */
    public WorldConfig(StakeAClaimPlugin plugin, String worldName, YAMLProcessor parentConfig) {
        File configFile = new File(plugin.getDataFolder(), "worlds" + File.separator + worldName + File.separator + "config.yml");

        this.plugin = plugin;
        this.worldName = worldName;
        this.parentConfig = parentConfig;

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

    /**
     * Load the configuration.
     */
    private void loadConfiguration() {
        //TODO make default config generation include comments
        try {
            config.load();
        } catch (IOException e) {
            plugin.getLogger().info("No configuration for world '" + worldName + "' found, using default.");
        }

        summaryOnStart = getBoolean("summary-on-start", true);
        opPermissions = getBoolean("op-permissions", true);
        useSAC = getBoolean("master-enable", true);
        useStakes = getBoolean("stakes-enable", true);
        sacWand = Material.matchMaterial(getString("wand", "FEATHER"));
        claimNameFilter = getString("claim-name-regex-filter-string", "^[NSns]\\d\\d?[EWew]\\d\\d?$"); // match eg. s2w45
        useReclaimed = getBoolean("remeber-reclaimed", true);
        VIPs = getString("what-you-call-your-vips", "Donors");
        silentNotify = getBoolean("claiming.silent-claiming", false);
        confirmUnassisted = getBoolean("claiming.players-must-confirm-unassisted-stakes", true);
        useVolumeLimits = getBoolean("claiming.use-volume-limits", false);
        unassistedMaxCount = getInt("claiming.max-count.unassisted-stakes", 1);
        totalMaxCount = getInt("claiming.max-count.total-stakes", 3);
        proxyMaxCount = getInt("claiming.max-count.proxy-can-stake", -1);
        unassistedMaxVolume = getInt("claiming.max-volume.unassisted-stakes", 262144);
        totalMaxVolume = getInt("claiming.max-volume.total-stakes", 1048576);
        proxyMaxVolume = getInt("claiming.max-volume.proxy-can-stake", -1);

        useRegions = WGBukkit.getPlugin().getGlobalStateManager().get(plugin.getServer().getWorld(worldName)).useRegions;

        if (!useRegions || !useSAC) {
            useStakes = false;
        }

        config.setHeader(CONFIG_HEADER);

        config.save();
    }

    public String getWorldName() {
        return this.worldName;
    }
}
