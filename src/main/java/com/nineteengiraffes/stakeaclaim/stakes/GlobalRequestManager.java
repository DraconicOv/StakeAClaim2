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

import com.nineteengiraffes.stakeaclaim.bukkit.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.stakes.databases.StakeDatabase;
import com.nineteengiraffes.stakeaclaim.stakes.databases.StakeDatabaseException;
import com.nineteengiraffes.stakeaclaim.stakes.databases.YAMLDatabase;

/**
 * This class keeps track of request information for every world. It loads
 * world request information as needed.
 */
public class GlobalRequestManager {

    /**
     * Reference to the plugin.
     */
    private StakeAClaimPlugin plugin;

//    /**
//     * Reference to the global configuration.
//     */
//    private ConfigurationManager config;
//
    /**
     * Map of managers per-world.
     */
    private ConcurrentHashMap<String, RequestManager> managers;

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
    public GlobalRequestManager(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
//        config = plugin.getGlobalStateManager();
        managers = new ConcurrentHashMap<String, RequestManager>();
        lastModified = new HashMap<String, Long>();
    }

    /**
     * Unload request information.
     */
    public void unload() {
        managers.clear();
        lastModified.clear();
    }

    /**
     * Get the path for a world's requests file.
     *
     * @param name The name of the world
     * @return The request file path for a world's request file
     */
    protected File getPath(String name) {
        return new File(plugin.getDataFolder(),
                "worlds" + File.separator + name + File.separator + "requests.yml");
    }

    /**
     * Unload request information for a world.
     *
     * @param name The name of the world to unload
     */
    public void unload(String name) {
        RequestManager manager = managers.remove(name);

        if (manager != null) {
            lastModified.remove(name);
        }
    }

    /**
     * Unload all request information.
     */
    public void unloadAll() {
        managers.clear();
        lastModified.clear();
    }

    public RequestManager load(World world) {
        RequestManager manager = create(world);
        managers.put(world.getName(), manager);
        return manager;
    }

    /**
     * Load request information for a world.
     *
     * @param world The world to load a RequestManager for
     * @return the loaded RequestManager
     */
    public RequestManager create(World world) {
        String name = world.getName();
        StakeDatabase database;
        File file = null;

        try {
            file = getPath(name);
            database = new YAMLDatabase(file, plugin.getLogger());

            lastModified.put(name, file.lastModified());

            // Create a manager
            RequestManager manager = new RequestManager(database);
            manager.load();

//            final String[] results = SACUtil.fixRquests(plugin, manager, world);

            if (plugin.getGlobalStateManager().get(world).summaryOnStart) {
                plugin.getLogger().info(manager.getRequests().size()
                        + " requests loaded for '" + name + "'");
//                if (!results[0].equals("")) {
//                    plugin.getLogger().info(results[0]);
//                }
//                if (!results[1].equals("")) {
//                    plugin.getLogger().info(results[1]);
//                }

            }

            return manager;
        } catch (StakeDatabaseException e) {
            String logStr = "Failed to load requests from ";
            logStr += "file \"" + file + "\" ";

            plugin.getLogger().log(Level.SEVERE, logStr + " : " + e.getMessage());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading requests for world \""
                    + name + "\": " + e.toString() + "\n\t" + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Preloads request managers for all worlds.
     */
    public void preload() {
        // Load requests
        for (World world : plugin.getServer().getWorlds()) {
            load(world);
        }
    }

    /**
     * Reloads the request information from file when request databases
     * have changed.
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
                        load(world);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Get the request manager for a particular world.
     *
     * @param world The world to get a RequestManager for
     * @return The request manager.
     */
    public RequestManager get(World world) {
        RequestManager manager = managers.get(world.getName());
        RequestManager newManager = null;

        while (manager == null) {
            if (newManager == null) {
                newManager = create(world);
            }
            managers.putIfAbsent(world.getName(), newManager);
            manager = managers.get(world.getName());
        }

        return manager;
    }
}
