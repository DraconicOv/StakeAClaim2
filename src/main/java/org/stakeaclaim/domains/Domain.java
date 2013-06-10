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

package org.stakeaclaim.domains;

import org.stakeaclaim.LocalPlayer;

public interface Domain {
    /**
     * Returns true if a domain contains a player.
     *
     * @param player The player to check
     * @return whether this domain contains {@code player}
     */
    boolean contains(LocalPlayer player);

    /**
     * Returns true if a domain contains a player.<br />
     * This method doesn't check for groups!
     *
     * @param playerName The name of the player to check
     * @return whether this domain contains a player by that name
     */
    boolean contains(String playerName);
}
