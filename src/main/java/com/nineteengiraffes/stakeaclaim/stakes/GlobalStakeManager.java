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
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.World;

import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;

/**
 * This class keeps track of stake information for every world. It loads
 * world stake information as needed.
 */
public class GlobalStakeManager {

    /**
     * Reference to the plugin.
     */
    private StakeAClaimPlugin plugin;

    /**
     * Map of managers per-world.
     */
    private ConcurrentHashMap<String, StakeManager> managers;

    /**
     * Stores the list of modification dates for the world files. This allows
     * StakeAClaim to reload files as needed.
     */
    private HashMap<String, Long> lastModified;

    /**
     * Construct the object.
     *
     * @param plugin The plugin instance
     */
    public GlobalStakeManager(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
        managers = new ConcurrentHashMap<String, StakeManager>();
        lastModified = new HashMap<String, Long>();
    }

    /**
     * Unload stake information.
     */
    public void unload() {
        managers.clear();
        lastModified.clear();
    }

    /**
     * Get the path for a world's stake file.
     *
     * @param name The name of the world
     * @return The stake file path for a world's stake file
     */
    protected File getPath(String name) {
        return new File(plugin.getDataFolder(), "worlds" + File.separator + name + File.separator + "stakes.yml");
    }

    /**
     * Unload stake information for a world.
     *
     * @param name The name of the world to unload
     */
    public void unload(String name) {
        StakeManager manager = managers.remove(name);

        if (manager != null) {
            lastModified.remove(name);
        }
    }

    /**
     * Unload all stake information.
     */
    public void unloadAll() {
        managers.clear();
        lastModified.clear();
    }

    /**
     * Load stake information for a world.
     *
     * @param world The world to load a StakeManager for
     * @return the loaded StakeManager
     */
    public StakeManager loadWorld(World world) {
        String name = world.getName();
        StakeManager manager;
        File file = null;

        try {
            file = getPath(name);
            manager = new StakeManager(file, plugin.getLogger());
            lastModified.put(name, file.lastModified());

            manager.load();

            if (plugin.getGlobalManager().get(world).summaryOnStart) {
                plugin.getLogger().info(manager.getStakes().size()
                        + " stakes loaded for '" + name + "'");
            }

            managers.putIfAbsent(world.getName(), manager);
            return manager;
        } catch (StakeDatabaseException e) {
            String logStr = "Failed to load stakes from ";
            logStr += "file \"" + file + "\" ";

            plugin.getLogger().log(Level.SEVERE, logStr + " : " + e.getMessage());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading stakes for world \""
                    + name + "\": " + e.toString() + "\n\t" + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Loads stake managers for all worlds.
     */
    public void load() {
        for (World world : plugin.getServer().getWorlds()) {
            loadWorld(world);
        }
    }

    /**
     * Reloads the stake information from file when stake databases have changed.
     */
    public void reloadChanged() {

        for (String name : managers.keySet()) {
            File file = getPath(name);

            Long oldDate = lastModified.get(name);

            if (oldDate == null) {
                oldDate = 0L;
            }

            try {
                if (file.lastModified() > oldDate) {
                    World world = plugin.getServer().getWorld(name);

                    if (world != null) {
                        loadWorld(world);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Get the stake manager for a particular world.
     *
     * @param world The world to get a stake manager for
     * @return The stake manager.
     */
    public StakeManager get(World world) {
        StakeManager manager = managers.get(world.getName());

        if (manager == null) {
                manager = loadWorld(world);
        }

        return manager;
    }
}
