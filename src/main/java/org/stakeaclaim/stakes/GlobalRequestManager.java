// $Id$
/*
 * StakeAClaim
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
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
package org.stakeaclaim.stakes;

import static org.stakeaclaim.bukkit.BukkitUtil.toVector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.bukkit.BukkitUtil;
import org.stakeaclaim.bukkit.ConfigurationManager;
import org.stakeaclaim.bukkit.WorldConfiguration;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.stakes.databases.MySQLDatabase;
import org.stakeaclaim.stakes.databases.ProtectionDatabase;
import org.stakeaclaim.stakes.databases.ProtectionDatabaseException;
import org.stakeaclaim.stakes.databases.YAMLDatabase;
import org.stakeaclaim.stakes.flags.StateFlag;
import org.stakeaclaim.stakes.managers.PRTreeRequestManager;
import org.stakeaclaim.stakes.managers.RequestManager;

/**
 * This class keeps track of request information for every world. It loads
 * world request information as needed.
 *
 * @author sk89q
 * @author Redecouverte
 */
public class GlobalRequestManager {

    /**
     * Reference to the plugin.
     */
    private StakeAClaimPlugin plugin;

    /**
     * Reference to the global configuration.
     */
    private ConfigurationManager config;

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
        config = plugin.getGlobalStateManager();
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
        boolean sql = config.useSqlDatabase;
        ProtectionDatabase database;
        File file = null;

        try {
            if (!sql) {
                file = getPath(name);
                database = new YAMLDatabase(file, plugin.getLogger());

                // Store the last modification date so we can track changes
                lastModified.put(name, file.lastModified());
            } else {
                database = new MySQLDatabase(config, name, plugin.getLogger());
            }

            // Create a manager
            RequestManager manager = new PRTreeRequestManager(database);
            manager.load();

            if (plugin.getGlobalStateManager().get(world).summaryOnStart) {
                plugin.getLogger().info(manager.getRequests().size()
                        + " requests loaded for '" + name + "'");
            }

            return manager;
        } catch (ProtectionDatabaseException e) {
            String logStr = "Failed to load requests from ";
            if (sql) {
                logStr += "SQL Database <" + config.sqlDsn + "> ";
            } else {
                logStr += "file \"" + file + "\" ";
            }

            plugin.getLogger().log(Level.SEVERE, logStr + " : " + e.getMessage());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading requests for world \""
                    + name + "\": " + e.toString() + "\n\t" + e.getMessage());
            e.printStackTrace();
        }

        // @TODO: THIS CREATES PROBLEMS!!one!!1!!eleven!!1!!!
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
        if (config.useSqlDatabase) return;

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

    /**
     * Returns whether the player can bypass.
     *
     * @param player The player to check
     * @param world The world to check for
     * @return Whether {@code player} has bypass permission for {@code world}
     */
    public boolean hasBypass(LocalPlayer player, World world) {
        return player.hasPermission("stakeaclaim.request.bypass."
                        + world.getName());
    }

    /**
     * Returns whether the player can bypass.
     *
     * @param player The player to check
     * @param world The world to check
     * @return Whether {@code player} has bypass permission for {@code world}
     */
    public boolean hasBypass(Player player, World world) {
        return plugin.hasPermission(player, "stakeaclaim.request.bypass."
                + world.getName());
    }

    /**
     * Check if a player has permission to build at a block.
     *
     * @param player The player to check
     * @param block The block to check at
     * @return Whether {@code player} can build at {@code block}'s location
     */
    public boolean canBuild(Player player, Block block) {
        return canBuild(player, block.getLocation());
    }

    /**
     * Check if a player has permission to build at a location.
     *
     * @param player The player to check
     * @param loc The location to check
     * @return Whether {@code player} can build at {@code loc}
     */
    public boolean canBuild(Player player, Location loc) {
        World world = loc.getWorld();
        WorldConfiguration worldConfig = config.get(world);

        if (!worldConfig.useRequests) {
            return true;
        }

        LocalPlayer localPlayer = plugin.wrapPlayer(player);

        if (!hasBypass(player, world)) {
            RequestManager mgr = get(world);

            if (!mgr.getApplicableRequests(BukkitUtil.toVector(loc))
                    .canBuild(localPlayer)) {
                return false;
            }
        }

        return true;
    }

    public boolean canConstruct(Player player, Block block) {
        return canConstruct(player, block.getLocation());
    }

    public boolean canConstruct(Player player, Location loc) {
        World world = loc.getWorld();
        WorldConfiguration worldConfig = config.get(world);

        if (!worldConfig.useRequests) {
            return true;
        }

        LocalPlayer localPlayer = plugin.wrapPlayer(player);

        if (!hasBypass(player, world)) {
            RequestManager mgr = get(world);

            final ApplicableRequestSet applicableRequests = mgr.getApplicableRequests(BukkitUtil.toVector(loc));
            if (!applicableRequests.canBuild(localPlayer)) {
                return false;
            }
            if (!applicableRequests.canConstruct(localPlayer)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks to see whether a flag is allowed.
     *
     * @see #allows(org.stakeaclaim.stakes.flags.StateFlag, org.bukkit.Location, org.stakeaclaim.LocalPlayer)
     * @param flag The flag to check
     * @param loc The location to check the flag at
     * @return Whether the flag is allowed
     */
    public boolean allows(StateFlag flag, Location loc) {
        return allows(flag, loc, null);
    }

    /**
     * Checks to see whether a flag is allowed.
     *
     * @param flag The flag to check
     * @param loc The location to check the flag at
     * @param player The player to check for the flag's {@link org.stakeaclaim.stakes.flags.RequestGroup}
     * @return Whether the flag is allowed
     */
    public boolean allows(StateFlag flag, Location loc, LocalPlayer player) {
        World world = loc.getWorld();
        WorldConfiguration worldConfig = config.get(world);

        if (!worldConfig.useRequests) {
            return true;
        }

        RequestManager mgr = get(world);
        return mgr.getApplicableRequests(toVector(loc)).allows(flag, player);
    }
}
