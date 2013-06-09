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

package org.stakeaclaim.bukkit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
import org.stakeaclaim.stakes.ApplicableRequestSet;
import org.stakeaclaim.stakes.flags.DefaultFlag;
import org.stakeaclaim.stakes.flags.StateFlag;
import org.stakeaclaim.stakes.managers.RequestManager;

public class RequestQueryUtil {

    public static boolean isInvincible(StakeAClaimPlugin plugin, Player player) {
        return isInvincible(plugin, player, null);
    }

    public static boolean isInvincible(StakeAClaimPlugin plugin, Player player,
                                       ApplicableRequestSet set) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        FlagStateManager.PlayerFlagState state = plugin.getFlagStateManager().getState(player);

        if (state.lastInvincibleWorld == null ||
                !state.lastInvincibleWorld.equals(world) ||
                state.lastInvincibleX != loc.getBlockX() ||
                state.lastInvincibleY != loc.getBlockY() ||
                state.lastInvincibleZ != loc.getBlockZ()) {
            state.lastInvincibleX = loc.getBlockX();
            state.lastInvincibleY = loc.getBlockY();
            state.lastInvincibleZ = loc.getBlockZ();
            state.lastInvincibleWorld = world;

            if (set == null) {
                Vector vec = new Vector(state.lastInvincibleX,
                        state.lastInvincibleY, state.lastInvincibleZ);
                RequestManager mgr = plugin.getGlobalRequestManager().get(world);
                set = mgr.getApplicableRequests(vec);
            }

            state.wasInvincible = set.allows(DefaultFlag.INVINCIBILITY, plugin.wrapPlayer(player));
        }

        return state.wasInvincible;
    }

    public static Boolean isAllowedInvinciblity(StakeAClaimPlugin plugin, Player player) {
        World world = player.getWorld();
        FlagStateManager.PlayerFlagState state = plugin.getFlagStateManager().getState(player);
        Vector vec = new Vector(state.lastInvincibleX, state.lastInvincibleY, state.lastInvincibleZ);

        StateFlag.State requestState = plugin.getGlobalRequestManager().get(world).
                getApplicableRequests(vec).getFlag(DefaultFlag.INVINCIBILITY, plugin.wrapPlayer(player));
        if (requestState == StateFlag.State.ALLOW) {
            return Boolean.TRUE;
        } else if (requestState == StateFlag.State.DENY) {
            return Boolean.FALSE;
        } else {
            return null;
        }
    }
}
