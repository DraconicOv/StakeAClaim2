// $Id$
/*
 * MySQL WordGuard Request Database
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

package org.stakeaclaim.stakes.databases.migrators;

import org.stakeaclaim.bukkit.ConfigurationManager;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.stakes.databases.MySQLDatabase;
import org.stakeaclaim.stakes.databases.ProtectionDatabase;
import org.stakeaclaim.stakes.databases.ProtectionDatabaseException;
import org.stakeaclaim.stakes.databases.YAMLDatabase;
import org.stakeaclaim.stakes.StakeRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MySQLToYAMLMigrator extends AbstractDatabaseMigrator {

    private StakeAClaimPlugin plugin;
    private Set<String> worlds;

    public MySQLToYAMLMigrator(StakeAClaimPlugin plugin) throws MigrationException {
        this.plugin = plugin;
        this.worlds = new HashSet<String>();

        ConfigurationManager config = plugin.getGlobalStateManager();

        try {
            Connection conn = DriverManager.getConnection(config.sqlDsn, config.sqlUsername, config.sqlPassword);

            ResultSet worlds = conn.prepareStatement("SELECT `name` FROM `world`;").executeQuery();

            while(worlds.next()) {
                this.worlds.add(worlds.getString(1));
            }

            conn.close();
        } catch (SQLException e) {
            throw new MigrationException(e);
        }
    }

    @Override
    protected Set<String> getWorldsFromOld() {
        return this.worlds;
    }

    @Override
    protected Map<long, StakeRequest> getRequestsForWorldFromOld(String world) throws MigrationException {
        ProtectionDatabase oldDatabase;
        try {
            oldDatabase = new MySQLDatabase(plugin.getGlobalStateManager(), world, plugin.getLogger());
            oldDatabase.load();
        } catch (ProtectionDatabaseException e) {
            throw new MigrationException(e);
        }

        return oldDatabase.getRequests();
    }

    @Override
    protected ProtectionDatabase getNewWorldStorage(String world) throws MigrationException {
        try {
            File file = new File(plugin.getDataFolder(),
                    "worlds" + File.separator + world + File.separator + "requests.yml");

            return new YAMLDatabase(file, plugin.getLogger());
        } catch (FileNotFoundException e) {
            throw new MigrationException(e);
        } catch (ProtectionDatabaseException e) {
            throw new MigrationException(e);
        }
    }
}
