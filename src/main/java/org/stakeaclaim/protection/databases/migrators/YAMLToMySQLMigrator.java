// $Id$
/*
 * MySQL WordGuard Region Database
 * Copyright (C) 2011 Nicholas Steicke <http://narthollis.net>
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

package org.stakeaclaim.protection.databases.migrators;

import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.protection.databases.MySQLDatabase;
import org.stakeaclaim.protection.databases.ProtectionDatabase;
import org.stakeaclaim.protection.databases.ProtectionDatabaseException;
import org.stakeaclaim.protection.databases.YAMLDatabase;
import org.stakeaclaim.protection.regions.ProtectedRegion;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class YAMLToMySQLMigrator extends AbstractDatabaseMigrator {

    private StakeAClaimPlugin plugin;
    private HashMap<String,File> regionYamlFiles;

    public YAMLToMySQLMigrator(StakeAClaimPlugin plugin) {
        this.plugin = plugin;

        this.regionYamlFiles = new HashMap<String,File>();

        File files[] = new File(plugin.getDataFolder(), "worlds" + File.separator).listFiles();
        for (File item : files) {
            if (item.isDirectory()) {
                for (File subItem : item.listFiles()) { 
                    if (subItem.getName().equals("regions.yml")) {
                        this.regionYamlFiles.put(item.getName(), subItem);
                    }
                }
            }
        }
    }

    @Override
    protected Set<String> getWorldsFromOld() {
        return this.regionYamlFiles.keySet();
    }

    @Override
    protected Map<String, ProtectedRegion> getRegionsForWorldFromOld(String world) throws MigrationException {
        ProtectionDatabase oldDatabase;
        try {
            oldDatabase = new YAMLDatabase(this.regionYamlFiles.get(world), plugin.getLogger());
            oldDatabase.load();
        } catch (FileNotFoundException e) {
            throw new MigrationException(e);
        } catch (ProtectionDatabaseException e) {
            throw new MigrationException(e);
        }

        return oldDatabase.getRegions();
    }

    @Override
    protected ProtectionDatabase getNewWorldStorage(String world) throws MigrationException {
        try {
            return new MySQLDatabase(plugin.getGlobalStateManager(), world, plugin.getLogger());
        } catch (ProtectionDatabaseException e) {
            throw new MigrationException(e);
        }
    }

}
