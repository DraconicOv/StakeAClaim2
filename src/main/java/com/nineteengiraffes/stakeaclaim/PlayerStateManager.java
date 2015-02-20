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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.stakes.Stake;
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

        PlayerState state;

        for (Player player : players) {
            WorldConfig wcfg = config.get(player.getWorld());

            if (!wcfg.useSAC) {
                continue;
            }

            synchronized (this) {
                state = states.get(player.getName());

                if (state == null) {
                    state = new PlayerState();
                    states.put(player.getName(), state);
                }
            }
        }

        synchronized (this) {
            state = states.get(plugin.getServer().getConsoleSender().getName());

            if (state == null) {
                state = new PlayerState();
                states.put(plugin.getServer().getConsoleSender().getName(), state);
            }
        }
    }

    /**
     * Forget a player.
     *
     * @param sender The player to forget
     */
    public synchronized void forget(CommandSender sender) {
        states.remove(sender.getName());
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
     * @param sender The player to get a state for
     * @return The {@code player}'s state
     */
    public synchronized PlayerState getState(CommandSender sender) {
        PlayerState state = states.get(sender.getName());

        if (state == null) {
            state = new PlayerState();
            states.put(sender.getName(), state);
        }

        return state;
    }

    /**
     * Keeps state per player.
     */
    public static class PlayerState {

        // move event
        public String lastSupport;

        // for proxy
        public Stake unsubmittedStake;

        // list
        public LinkedHashMap<Integer, ProtectedRegion> regionList;
        public World listWorld;

        // warp
        public ProtectedRegion lastWarp;
        public World warpWorld;
    }
}
