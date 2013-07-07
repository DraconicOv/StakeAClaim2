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

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.entity.Player;

import org.stakeaclaim.stakes.StakeRequest;

/**
 * This processes per-player state information and is also meant to be used
 * as a scheduled task.
 */
public class FlagStateManager implements Runnable {

    public static final int RUN_DELAY = 20;

    private StakeAClaimPlugin plugin;
    private Map<String, PlayerFlagState> states;

    /**
     * Construct the object.
     *
     * @param plugin The plugin instance
     */
    public FlagStateManager(StakeAClaimPlugin plugin) {
        this.plugin = plugin;

        states = new HashMap<String, PlayerFlagState>();
    }

    /**
     * Run the task.
     */
    public void run() {
        Player[] players = plugin.getServer().getOnlinePlayers();
        ConfigurationManager config = plugin.getGlobalStateManager();

        for (Player player : players) {
            WorldConfiguration worldConfig = config.get(player.getWorld());

            if (!worldConfig.useSAC) {
                continue;
            }

            PlayerFlagState state;

            synchronized (this) {
                state = states.get(player.getName());

                if (state == null) {
                    state = new PlayerFlagState();
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
    public synchronized PlayerFlagState getState(Player player) {
        PlayerFlagState state = states.get(player.getName());

        if (state == null) {
            state = new PlayerFlagState();
            states.put(player.getName(), state);
        }

        return state;
    }

    /**
     * Keeps state per player.
     */
    public static class PlayerFlagState {
        public World lastWorld;
        public int lastBlockX;
        public int lastBlockY;
        public int lastBlockZ;
        public String lastSupport;
        public LinkedHashMap<Integer, Long> requestList;
        public StakeRequest unsubmittedRequest;
    }
}
