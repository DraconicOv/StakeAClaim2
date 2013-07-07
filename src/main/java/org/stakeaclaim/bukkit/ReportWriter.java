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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.util.LogListBlock;

import org.stakeaclaim.stakes.GlobalRequestManager;
import org.stakeaclaim.stakes.RequestManager;

public class ReportWriter {

    private static final SimpleDateFormat dateFmt =
            new SimpleDateFormat("yyyy-MM-dd kk:mm Z");

    private Date date = new Date();
    private StringBuilder output = new StringBuilder();

    public ReportWriter(StakeAClaimPlugin plugin) {
        appendReportHeader(plugin);
        appendServerInformation(plugin.getServer());
        appendPluginInformation(plugin.getServer().getPluginManager().getPlugins());
        appendWorldInformation(plugin.getServer().getWorlds());
        appendGlobalConfiguration(plugin.getGlobalStateManager());
        appendWorldConfigurations(plugin, plugin.getServer().getWorlds(),
                plugin.getGlobalRequestManager(), plugin.getGlobalStateManager());
        appendln("-------------");
        appendln("END OF REPORT");
        appendln();
    }

    protected static String repeat(String str, int n) {
        if(str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(str);
        }

        return sb.toString();
    }

    protected void appendln(String text) {
        output.append(text);
        output.append("\r\n");
    }

    protected void appendln(String text, Object ... args) {
        output.append(String.format(text, args));
        output.append("\r\n");
    }

    protected void append(LogListBlock log) {
        output.append(log.toString());
    }

    protected void appendln() {
        output.append("\r\n");
    }

    protected void appendHeader(String text) {
        String rule = repeat("-", text.length());
        output.append(rule);
        output.append("\r\n");
        appendln(text);
        output.append(rule);
        output.append("\r\n");
        appendln();
    }

    private void appendReportHeader(StakeAClaimPlugin plugin) {
        appendln("StakeAClaim Configuration Report");
        appendln("Generated " + dateFmt.format(date));
        appendln();
        appendln("Version: " + plugin.getDescription().getVersion());
        appendln();
    }

    private void appendGlobalConfiguration(ConfigurationManager config) {
        appendHeader("Global Configuration");

        LogListBlock log = new LogListBlock();
        LogListBlock configLog = log.putChild("Configuration");

        Class<? extends ConfigurationManager> cls = config.getClass();
        for (Field field : cls.getFields()) {
            try {
                if (field.getName().equalsIgnoreCase("CONFIG_HEADER")) continue;
                Object val = field.get(config);
                configLog.put(field.getName(), val);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException ignore) {
            }
        }

        append(log);
        appendln();
    }

    private void appendServerInformation(Server server) {
        appendHeader("Server Information");

        LogListBlock log = new LogListBlock();

        Runtime runtime = Runtime.getRuntime();

        log.put("Java", "%s %s (%s)",
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.vendor.url"));
        log.put("Operating system", "%s %s (%s)",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        log.put("Available processors", runtime.availableProcessors());
        log.put("Free memory", runtime.freeMemory() / 1024 / 1024 + " MB");
        log.put("Max memory", runtime.maxMemory() / 1024 / 1024 + " MB");
        log.put("Total memory", runtime.totalMemory() / 1024 / 1024 + " MB");
        log.put("Server ID", server.getServerId());
        log.put("Server name", server.getServerName());
        log.put("Implementation", server.getVersion());
        //log.put("Address", server.getIp(), server.getPort());
        log.put("Player count", "%d/%d",
                server.getOnlinePlayers().length, server.getMaxPlayers());

        append(log);
        appendln();
    }

    private void appendPluginInformation(Plugin[] plugins) {
        appendHeader("Plugins (" + plugins.length + ")");

        LogListBlock log = new LogListBlock();

        for (Plugin plugin : plugins) {
            log.put(plugin.getDescription().getName(), plugin.getDescription().getVersion());
        }

        append(log);
        appendln();

        /*appendHeader("Plugin Information");

        log = new LogListBlock();

        for (Plugin plugin : plugins) {
            log.putChild(plugin.getDescription().getName())
                .put("Data folder", plugin.getDataFolder())
                .put("Website", plugin.getDescription().getWebsite())
                .put("Entry point", plugin.getDescription().getMain());
        }

        append(log);
        appendln();*/
    }

    private void appendWorldInformation(List<World> worlds) {
        appendHeader("Worlds");

        LogListBlock log = new LogListBlock();

        int i = 0;
        for (World world : worlds) {
            int loadedChunkCount = world.getLoadedChunks().length;

            LogListBlock worldLog = log.putChild(world.getName() + " (" +  i + ")");
            LogListBlock infoLog = worldLog.putChild("Information");
            LogListBlock entitiesLog = worldLog.putChild("Entities");

            infoLog.put("Seed", world.getSeed());
            infoLog.put("Environment", world.getEnvironment().toString());
            infoLog.put("Player count", world.getPlayers().size());
            infoLog.put("Entity count", world.getEntities().size());
            infoLog.put("Loaded chunk count", loadedChunkCount);
            infoLog.put("Spawn location", world.getSpawnLocation());
            infoLog.put("Raw time", world.getFullTime());

            Map<Class<? extends Entity>, Integer> entityCounts =
                    new HashMap<Class<? extends Entity>, Integer>();

            // Collect entities
            for (Entity entity : world.getEntities()) {
                Class<? extends Entity> cls = entity.getClass();

                if (entityCounts.containsKey(cls)) {
                    entityCounts.put(cls, entityCounts.get(cls) + 1);
                } else {
                    entityCounts.put(cls, 1);
                }
            }

            // Print entities
            for (Map.Entry<Class<? extends Entity>, Integer> entry
                    : entityCounts.entrySet()) {
                entitiesLog.put(entry.getKey().getSimpleName(),
                        "%d [%f]",
                        entry.getValue(),
                        (float) (entry.getValue() / (double) loadedChunkCount));
            }

            i++;
        }

        append(log);
        appendln();
    }

    private void appendWorldConfigurations(StakeAClaimPlugin plugin, List<World> worlds,
            GlobalRequestManager requestMgr, ConfigurationManager mgr) {
        appendHeader("World Configurations");

        LogListBlock log = new LogListBlock();

        int i = 0;
        for (World world : worlds) {
            LogListBlock worldLog = log.putChild(world.getName() + " (" +  i + ")");
            LogListBlock infoLog = worldLog.putChild("Information");
            LogListBlock configLog = worldLog.putChild("Configuration");
            LogListBlock requestsLog = worldLog.putChild("Request manager");

            infoLog.put("Configuration file", (new File(plugin.getDataFolder(), "worlds/"
                    + world.getName() + "/config.yml")).getAbsoluteFile());

            infoLog.put("Requests file", (new File(plugin.getDataFolder(), "worlds/"
                    + world.getName() + "/requests.yml")).getAbsoluteFile());

            WorldConfiguration config = mgr.get(world);

            Class<? extends WorldConfiguration> cls = config.getClass();
            for (Field field : cls.getFields()) {
                try {
                    Object val = field.get(config);
                    configLog.put(field.getName(), String.valueOf(val));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException ignore) {
                }
            }

            RequestManager worldRequests = requestMgr.get(world);

            requestsLog.put("Type", worldRequests.getClass().getCanonicalName());
            requestsLog.put("Number of requests", worldRequests.getRequests().size());
            LogListBlock globalRequestLog = requestsLog.putChild("Global request");

        }

        append(log);
        appendln();
    }

    public void write(File file) throws IOException {
        FileWriter writer = null;
        BufferedWriter out;

        try {
            writer = new FileWriter(file);
            out = new BufferedWriter(writer);
            out.write(output.toString());
            out.close();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Override
    public String toString() {
        return output.toString();
    }
}
