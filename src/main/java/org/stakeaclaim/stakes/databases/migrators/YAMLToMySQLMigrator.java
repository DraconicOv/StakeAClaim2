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

package org.stakeaclaim.stakes.databases.migrators;

import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.stakes.databases.MySQLDatabase;
import org.stakeaclaim.stakes.databases.StakeDatabase;
import org.stakeaclaim.stakes.databases.StakeDatabaseException;
import org.stakeaclaim.stakes.databases.YAMLDatabase;
import org.stakeaclaim.stakes.StakeRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class YAMLToMySQLMigrator extends AbstractDatabaseMigrator {

    private StakeAClaimPlugin plugin;
    private HashMap<String,File> requestYamlFiles;

    public YAMLToMySQLMigrator(StakeAClaimPlugin plugin) {
        this.plugin = plugin;

        this.requestYamlFiles = new HashMap<String,File>();

        File files[] = new File(plugin.getDataFolder(), "worlds" + File.separator).listFiles();
        for (File item : files) {
            if (item.isDirectory()) {
                for (File subItem : item.listFiles()) { 
                    if (subItem.getName().equals("requests.yml")) {
                        this.requestYamlFiles.put(item.getName(), subItem);
                    }
                }
            }
        }
    }

    @Override
    protected Set<String> getWorldsFromOld() {
        return this.requestYamlFiles.keySet();
    }

    @Override
    protected Map<Long, StakeRequest> getRequestsForWorldFromOld(String world) throws MigrationException {
        StakeDatabase oldDatabase;
        try {
            oldDatabase = new YAMLDatabase(this.requestYamlFiles.get(world), plugin.getLogger());
            oldDatabase.load();
        } catch (FileNotFoundException e) {
            throw new MigrationException(e);
        } catch (StakeDatabaseException e) {
            throw new MigrationException(e);
        }

        return oldDatabase.getRequests();
    }

    @Override
    protected StakeDatabase getNewWorldStorage(String world) throws MigrationException {
        try {
            return new MySQLDatabase(plugin.getGlobalStateManager(), world, plugin.getLogger());
        } catch (StakeDatabaseException e) {
            throw new MigrationException(e);
        }
    }

}
