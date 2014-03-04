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

package com.nineteengiraffes.stakeaclaim.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.stakes.StakeDatabaseException;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;

public class SACCommands {
    private final StakeAClaimPlugin plugin;

    public SACCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"gen", "generate", "spawn", "g"},
            usage = "[world] or ['all']",
            desc = "Generate spawns for all claims",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.generate")
    public void generate(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String in = args.getString(0);

            // generate one world
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase().equals(in.toLowerCase())) {
                    int claims = SACUtil.makeSpawns(plugin, world);
                    if (claims == -1) {
                        throw new CommandException("No region manager exists for world '" + world.getName() + "'.");
                    }
                    saveRegions(world);
                    throw new CommandException(ChatColor.YELLOW + "Generated " + claims + " claim spawns in '" + world.getName() + "'");
                }
            }
    
            // generate all worlds
            if (in.toLowerCase().equals("all") || in.toLowerCase().equals("a")) {
                for (World world : plugin.getServer().getWorlds()) {
                    int claims = SACUtil.makeSpawns(plugin, world);
                    if (claims == -1) continue;
                    saveRegions(world);
                    sender.sendMessage(ChatColor.YELLOW + "Generated " + claims + " claim spawns in '" + world.getName() + "'");
                }
                throw new CommandException(ChatColor.YELLOW + "Generated in all worlds.");
            }
        }

        // generate the world the player is in
        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        int claims = SACUtil.makeSpawns(plugin, world);
        if (claims == -1) {
            throw new CommandException("No region manager exists for world '" + world.getName() + "'.");
        }
        saveRegions(world);
        throw new CommandException(ChatColor.YELLOW + "Generated " + claims + " claim spawns in '" + world.getName() + "'");

    }

    @Command(aliases = {"save", "write", "s"},
            usage = "[world] or ['all']",
            desc = "Save stakes to file",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.save")
    public void save(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String in = args.getString(0);

            // save one world
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase().equals(in.toLowerCase())) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
                    }
                    write(sMgr);
                    throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes saved for '" + world.getName() + "'");
                }
            }
    
            // save all worlds
            if (in.toLowerCase().equals("all") || in.toLowerCase().equals("a")) {
                for (World world : plugin.getServer().getWorlds()) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        continue;
                    }
                    write(sMgr);
                    sender.sendMessage(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes saved for '" + world.getName() + "'");
                }
                throw new CommandException(ChatColor.YELLOW + "All worlds saved.");
            }
        }

        // save the world the player is in
        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        if (sMgr == null) {
            throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
        }
        write(sMgr);
        throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes saved for '" + world.getName() + "'");

    }

    @Command(aliases = {"load", "reload", "l"},
            usage = "[world] or ['all'] or ['config']",
            desc = "Reload stakes from file",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.load")
    public void load(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String in = args.getString(0);

            // load one world
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().toLowerCase().equals(in.toLowerCase())) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
                    }
                    read(sMgr);
                    throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes loaded for '" + world.getName() + "'");
                }
            }
    
            // load all worlds
            if (in.toLowerCase().equals("all") || in.toLowerCase().equals("a")) {
                for (World world : plugin.getServer().getWorlds()) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        continue;
                    }
                    read(sMgr);
                    sender.sendMessage(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes loaded for '" + world.getName() + "'");
                }
                throw new CommandException(ChatColor.YELLOW + "All worlds reloaded.");
            }
    
            // load config
            if (in.toLowerCase().equals("config") || in.toLowerCase().equals("c")) {
                ConfigManager config = plugin.getGlobalManager();
                config.load();
                throw new CommandException(ChatColor.YELLOW + "SAC config reloaded.");
            }
        }

        // load the world the player is in
        final Player player = plugin.checkPlayer(sender);
        final World world = player.getWorld();
        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        if (sMgr == null) {
            throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
        }
        read(sMgr);
        throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes loaded for '" + world.getName() + "'");

    }

    // other methods
    private void read(StakeManager sMgr) throws CommandException {
        try {
            sMgr.load();
        } catch (StakeDatabaseException e) {
            throw new CommandException("Uh oh, stakes did not load: " + e.getMessage());
        }
    }

    private void write(StakeManager sMgr) {
        sMgr.save();
    }

    private void saveRegions(World world) throws CommandException {

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        try {
            rgMgr.save();
        } catch (ProtectionDatabaseException e) {
            throw new CommandException("Failed to write regions: " + e.getMessage());
        }
    }

}
