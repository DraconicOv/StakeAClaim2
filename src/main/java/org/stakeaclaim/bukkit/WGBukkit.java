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

import org.bukkit.Bukkit;
import org.bukkit.World;

import org.stakeaclaim.stakes.RequestManager;

/**
 * Helper class to get a reference to StakeAClaim and its components.
 */
public class WGBukkit {
    private static StakeAClaimPlugin cachedPlugin = null;

    private WGBukkit() {
    }

    /**
     * Get the StakeAClaim plugin. If StakeAClaim isn't loaded yet, then this will
     * return null.
     * <p>
     * If you are depending on StakeAClaim in your plugin, you should place
     * <code>softdepend: [StakeAClaim]</code> or <code>depend: [StakeAClaim]</code>
     * in your plugin.yml so that this won't return null for you.
     *
     * @return the StakeAClaim plugin or null
     */
    public static StakeAClaimPlugin getPlugin() {
        if (cachedPlugin == null) {
            cachedPlugin = (StakeAClaimPlugin) Bukkit.getServer().getPluginManager().getPlugin("StakeAClaim");
        }
        return cachedPlugin;
    }

    /**
     * Set cache to null for reload StakeAClaimPlugin
     */
    public static void cleanCache() {
        cachedPlugin = null;
    }

    /**
     * Returns the request manager for a given world. May return null if StakeAClaim
     * is not loaded or request protection is not enabled for the given world.
     *
     * @param world world
     * @return a request manager or null
     */
    public static RequestManager getRequestManager(World world) {
        if (getPlugin() == null) {
            return null;
        }
        return cachedPlugin.getRequestManager(world);
    }

}
