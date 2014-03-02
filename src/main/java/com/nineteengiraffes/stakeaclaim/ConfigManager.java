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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.World;

import com.sk89q.util.yaml.YAMLFormat;
import com.sk89q.util.yaml.YAMLProcessor;

/**
 * Represents the global configuration and also delegates configuration
 * for individual worlds.
 */
public class ConfigManager {

    private static final String CONFIG_HEADER = "#\r\n" +
            "# StakeAClaim's main configuration file\r\n" +
            "#\r\n" +
            "# This is the global configuration file. Anything placed into here will\r\n" +
            "# be applied to all worlds. However, each world has its own configuration\r\n" +
            "# file to allow you to replace most settings in here for that world only.\r\n" +
            "#\r\n" +
            "# About editing this file:\r\n" +
            "# - DO NOT USE TABS. You MUST use spaces or Bukkit will complain. If\r\n" +
            "#   you use an editor like Notepad++ (recommended for Windows users), you\r\n" +
            "#   must configure it to \"replace tabs with spaces.\" In Notepad++, this can\r\n" +
            "#   be changed in Settings > Preferences > Language Menu.\r\n" +
            "# - Don't get rid of the indents. They are indented so some entries are\r\n" +
            "#   in categories (like \"enforce-single-session\" is in the \"protection\"\r\n" +
            "#   category.\r\n" +
            "# - If you want to check the format of this file before putting it\r\n" +
            "#   into StakeAClaim, paste it into http://yaml-online-parser.appspot.com/\r\n" +
            "#   and see if it gives \"ERROR:\".\r\n" +
            "# - Lines starting with # are comments and so they are ignored.\r\n" +
            "#\r\n";

    /**
     * Reference to the plugin.
     */
    private StakeAClaimPlugin plugin;

    /**
     * Holds configurations for different worlds.
     */
    private ConcurrentMap<String, WorldConfig> worlds;

    /**
     * The global configuration for use when loading worlds
     */
    private YAMLProcessor config;

    /* Configuration data start */
    public boolean useStakeScheduler;
    public boolean usePlayerMove;
    /* Configuration data end */

    /**
     * Construct the object.
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
        this.worlds = new ConcurrentHashMap<String, WorldConfig>();
    }

    /**
     * Load the configuration.
     */
    public void load() {

        config = new YAMLProcessor(new File(plugin.getDataFolder(), "config.yml"), true, YAMLFormat.EXTENDED);
        try {
            config.load();
        } catch (IOException e) {
            plugin.getLogger().info("No global configuration found, using default.");
        }

        useStakeScheduler = config.getBoolean("sac.use-scheduler", true);
        usePlayerMove = config.getBoolean("sac.use-player-move-event", true);

        // Load configurations for each world
        for (World world : plugin.getServer().getWorlds()) {
            get(world);
        }

        config.setHeader(CONFIG_HEADER);

        if (!config.save()) {
            plugin.getLogger().severe("Error saving configuration!");
        }
    }

    /**
     * Unload the configuration.
     */
    public void unload() {
        worlds.clear();
    }

    /**
     * Get the configuration for a world.
     *
     * @param world The world to get the configuration for
     * @return {@code world}'s configuration
     */
    public WorldConfig get(World world) {
        String worldName = world.getName();
        WorldConfig config = worlds.get(worldName);
        WorldConfig newConfig = null;

        while (config == null) {
            if (newConfig == null) {
                newConfig = new WorldConfig(plugin, worldName, this.config);
            }
            worlds.putIfAbsent(worldName, newConfig);
            config = worlds.get(worldName);
        }

        return config;
    }
}
