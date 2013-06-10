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

import java.util.LinkedHashSet;
import java.util.Set;

import org.stakeaclaim.LocalPlayer;

public class DomainCollection implements Domain {
    private Set<Domain> domains;

    public DomainCollection() {
        domains = new LinkedHashSet<Domain>();
    }

    public void add(Domain domain) {
        domains.add(domain);
    }

    public void remove(Domain domain) {
        domains.remove(domain);
    }

    public int size() {
        return domains.size();
    }

    @Override
    public boolean contains(LocalPlayer player) {
        for (Domain domain : domains) {
            if (domain.contains(player)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean contains(String playerName) {
        for (Domain domain : domains) {
            if (domain.contains(playerName)) {
                return true;
            }
        }

        return false;
    }
}
