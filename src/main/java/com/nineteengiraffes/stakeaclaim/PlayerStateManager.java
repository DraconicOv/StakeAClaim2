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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * This processes per-player state information and is also meant to be used
 * as a scheduled task.
 */
public class PlayerStateManager implements Runnable {

    public static final int RUN_DELAY = 20;

    private StakeAClaimPlugin plugin;
    private Map<String, PlayerState> states;

    /**
     * Construct the object.
     *
     * @param plugin The plugin instance
     */
    public PlayerStateManager(StakeAClaimPlugin plugin) {
        this.plugin = plugin;

        states = new HashMap<String, PlayerState>();
    }

    /**
     * Run the task.
     */
    public void run() {
        Player[] players = plugin.getServer().getOnlinePlayers();
        ConfigManager config = plugin.getGlobalManager();

        for (Player player : players) {
            WorldConfig worldConfig = config.get(player.getWorld());

            if (!worldConfig.useSAC) {
                continue;
            }

            PlayerState state;

            synchronized (this) {
                state = states.get(player.getName());

                if (state == null) {
                    state = new PlayerState();
                    states.put(player.getName(), state);
                }
            }
        }
    }

    /**
     * Forget a player.
     *
     * @param player The player to forget
     */
    public synchronized void forget(Player player) {
        states.remove(player.getName());
    }

    /**
     * Forget all managed players. Use with caution.
     */
    public synchronized void forgetAll() {
        states.clear();
    }

    /**
     * Get a player's flag state. A new state will be created if there is no existing
     * state for the player.
     *
     * @param player The player to get a state for
     * @return The {@code player}'s state
     */
    public synchronized PlayerState getState(Player player) {
        PlayerState state = states.get(player.getName());

        if (state == null) {
            state = new PlayerState();
            states.put(player.getName(), state);
        }

        return state;
    }

    /**
     * Keeps state per player.
     */
    public static class PlayerState {
        public World lastWorld;
        public int lastBlockX;
        public int lastBlockY;
        public int lastBlockZ;
        public String lastSupport;
        public LinkedHashMap<Integer, String> regionList;
        public String[] unsubmittedRequest;
        public ProtectedRegion lastWarp;
    }
}
