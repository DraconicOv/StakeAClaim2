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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.StakeDatabaseException;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SACCommands {
    private final StakeAClaimPlugin plugin;

    public SACCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"filters", "filter", "f"},
            usage = "",
            desc = "Display a list of all filters",
            min = 0, max = 0)
    @CommandPermissions("stakeaclaim.sac.filters")
    public void filters(CommandContext args, CommandSender sender) throws CommandException {

        sender.sendMessage(ChatColor.GRAY + 
                "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550" + 
                " Filters " + 
                "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");

        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "refine" + ChatColor.WHITE + 
                "' - search within last claim list. " + ChatColor.GRAY + "[r]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "join" + ChatColor.WHITE + 
                "' - add results to last claim list. " + ChatColor.GRAY + "[j]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "item" + ChatColor.WHITE + 
                "' - one item from last claim list. " + ChatColor.GRAY + "[i] <list item #>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "world" + ChatColor.WHITE + 
                "' - search world, default: player's world. " + ChatColor.GRAY + "[w] <name>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "id" + ChatColor.WHITE + 
                "' - one claim's ID. " + ChatColor.GRAY + "[id] <regionID>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "owner" + ChatColor.WHITE + 
                "' - claim owner or claimed. " + ChatColor.GRAY + "[o] <player>" + ChatColor.WHITE + " or " + ChatColor.GRAY + "<yes/no>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "member" + ChatColor.WHITE + 
                "' - claim member or members. " + ChatColor.GRAY + "[m] <player>" + ChatColor.WHITE + " or " + ChatColor.GRAY + "<yes/no>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "typo" + ChatColor.WHITE + 
                "' - mistyped players, default: yes. " + ChatColor.GRAY + "[t] [yes/no]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "banned" + ChatColor.WHITE + 
                "' - banned players, default: yes. " + ChatColor.GRAY + "[b] [yes/no]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "pending" + ChatColor.WHITE + 
                "' - pending stakes, default: yes. " + ChatColor.GRAY + "[p] [yes/no]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "vip" + ChatColor.WHITE + 
                "' - VIP claims, default: yes. " + ChatColor.GRAY + "[v] [yes/no]");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "absent" + ChatColor.WHITE + 
                "' - absent at least # of days. " + ChatColor.GRAY + "[a] <# of days>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "seen" + ChatColor.WHITE + 
                "' - seen within # of days. " + ChatColor.GRAY + "[s] <# of days>");
        sender.sendMessage(ChatColor.WHITE + "'" + ChatColor.GOLD + "page" + ChatColor.WHITE + 
                "' - results page # " + ChatColor.GRAY + "[p#]" + ChatColor.WHITE + " or " + ChatColor.GRAY + "[page] <page #>");

    }

    @Command(aliases = {"search", "s"},
            usage = "[filter(s)..]",
            desc = "Search all claims with filter(s)",
            min = 0)
    @CommandPermissions("stakeaclaim.sac.search")
    public void search(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();
        WorldConfig wcfg;
        
        ArrayList<ProtectedRegion> fullList = new ArrayList<ProtectedRegion>();
        World listWorld = null;

        // filters
        LinkedHashMap<Integer, ProtectedRegion> joinList = null;
        World argWorld = null;
        String id = null;
        String owner = null;
        String member = null;
        Boolean typo = null;
        Boolean banned = null;
        Boolean pending = null;
        Boolean vip = null;
        Boolean hasMembers = null;
        Boolean claimed = null;
        Long absent = null;
        Long seen = null;
        Integer page = null;

        LinkedHashMap<Integer, ProtectedRegion> claimList = new LinkedHashMap<Integer, ProtectedRegion>();

        if (args.argsLength() > 0) {
            String filter;

            // loops through all args and looks for filter key words
            for (int i = 0; i < args.argsLength(); i++) {
                filter = args.getString(i);

                // refine
                if (filter.equalsIgnoreCase("refine") || filter.equalsIgnoreCase("r")) {
                    if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
                        listWorld = state.listWorld;
                        fullList = new ArrayList<ProtectedRegion>(state.regionList.values());
                    } else {
                        throw new CommandException(ChatColor.YELLOW + "Claim list is empty. '" + ChatColor.WHITE + "refine" + 
                                ChatColor.YELLOW + "' filter can't be used.");
                    }

                // join
                } else if (filter.equalsIgnoreCase("join") || filter.equalsIgnoreCase("j")) {
                    if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
                        joinList = state.regionList;
                    } else {
                        throw new CommandException(ChatColor.YELLOW + "Claim list is empty. '" + ChatColor.WHITE + "join" + 
                                ChatColor.YELLOW + "' filter can't be used.");
                    }

                // item
                } else if (filter.equalsIgnoreCase("item") || filter.equalsIgnoreCase("i")) {
                    if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
                        i++;
                        if (i >= args.argsLength()) {
                            throw new CommandException(ChatColor.YELLOW + "Please include a list item number for filter '" + ChatColor.WHITE + "item" + 
                                    ChatColor.YELLOW + "'");
                        }
                        filter = args.getString(i);
                        ProtectedRegion claim;
                        try {
                            claim = state.regionList.get(Integer.parseInt(filter) - 1);
                        } catch (NumberFormatException e) {
                            throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                    ChatColor.WHITE + "item" + ChatColor.YELLOW + "' is not a valid number.");
                        }
                        if (claim == null) {
                            throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                    ChatColor.WHITE + "item" + ChatColor.YELLOW + "' is not a valid list item number.");
                        } else {
                            wcfg = cfg.get(state.listWorld);
                            if (!wcfg.useStakes) {
                                throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
                            }
                            StakeManager sMgr = plugin.getGlobalStakeManager().get(state.listWorld);
                            claimList.put(0, claim);
                            if (page == null) {
                                page = 0;
                            }

                            SACUtil.displayList(plugin, sender, claimList, sMgr, state.listWorld, page);
                            return;
                        }
                    } else {
                        throw new CommandException(ChatColor.YELLOW + "Claim list is empty. '" + ChatColor.WHITE + "item" + 
                                ChatColor.YELLOW + "' filter can't be used.");
                    }

                // world
                } else if (filter.equalsIgnoreCase("world") || filter.equalsIgnoreCase("w")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a world for filter '" + ChatColor.WHITE + "world" + 
                                ChatColor.YELLOW + "'");
                    }
                    filter = args.getString(i);
                    for (World serverWorld : plugin.getServer().getWorlds()) {
                        if (serverWorld.getName().equalsIgnoreCase(filter)) {
                            argWorld = serverWorld;
                        }
                    }
                    if (argWorld == null) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "world" + ChatColor.YELLOW + "' is not a valid world.");
                    }

                // id
                } else if (filter.equalsIgnoreCase("id")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a claim ID for filter '" + ChatColor.WHITE + "id" + 
                                ChatColor.YELLOW + "'");
                    }
                    id = args.getString(i);

                // owner
                } else if (filter.equalsIgnoreCase("owner") || filter.equalsIgnoreCase("o")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a player or booean for filter '" + ChatColor.WHITE + "owner" + 
                                ChatColor.YELLOW + "'");
                    } else {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            claimed = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            claimed = true;
                        } else {
                            owner = args.getString(i);
                        }
                    }

                // member
                } else if (filter.equalsIgnoreCase("member") || filter.equalsIgnoreCase("m")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a player or booean for filter '" + ChatColor.WHITE + "member" + 
                                ChatColor.YELLOW + "'");
                    } else {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            hasMembers = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            hasMembers = true;
                        } else {
                            member = args.getString(i);
                        }
                    }

                // typo
                } else if (filter.equalsIgnoreCase("typo") || filter.equalsIgnoreCase("t")) {
                    i++;
                    if (i < args.argsLength()) {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            typo = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            typo = true;
                        } else {
                            typo = true;
                            i--;
                        }
                    } else {
                        typo = true;
                    }

                // banned
                } else if (filter.equalsIgnoreCase("banned") || filter.equalsIgnoreCase("ban") || filter.equalsIgnoreCase("b")) {
                    i++;
                    if (i < args.argsLength()) {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            banned = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            banned = true;
                        } else {
                            banned = true;
                            i--;
                        }
                    } else {
                        banned = true;
                    }

                // pending
                } else if (filter.equalsIgnoreCase("pending") || filter.equalsIgnoreCase("p")) {
                    i++;
                    if (i < args.argsLength()) {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            pending = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            pending = true;
                        } else {
                            pending = true;
                            i--;
                        }
                    } else {
                        pending = true;
                    }

                // vip
                } else if (filter.equalsIgnoreCase("vip") || filter.equalsIgnoreCase("v")) {
                    i++;
                    if (i < args.argsLength()) {
                        filter = args.getString(i);
                        if (filter.equalsIgnoreCase("no") || filter.equalsIgnoreCase("n") || 
                                filter.equalsIgnoreCase("false")) {
                            vip = false;
                        } else if (filter.equalsIgnoreCase("yes") || filter.equalsIgnoreCase("y") || 
                                filter.equalsIgnoreCase("true")) {
                            vip = true;
                        } else {
                            vip = true;
                            i--;
                        }
                    } else {
                        vip = true;
                    }

                // absent
                } else if (filter.equalsIgnoreCase("absent") || filter.equalsIgnoreCase("a")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a number of days for filter '" + ChatColor.WHITE + "absent" + 
                                ChatColor.YELLOW + "'");
                    }
                    filter = args.getString(i);
                    try {
                        absent = Math.round(Double.parseDouble(filter) * 1000 * 60 * 60 * 24);
                    } catch (NumberFormatException e) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "absent" + ChatColor.YELLOW + "' is not a valid number of days.");
                    }
                    if (absent < 0) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "absent" + ChatColor.YELLOW + "' must be a positive number.");
                    }
                    absent = System.currentTimeMillis() - absent;

                // seen
                } else if (filter.equalsIgnoreCase("seen") || filter.equalsIgnoreCase("s")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a number of days for filter '" + ChatColor.WHITE + "seen" + 
                                ChatColor.YELLOW + "'");
                    }
                    filter = args.getString(i);
                    try {
                        seen = Math.round(Double.parseDouble(filter) * 1000 * 60 * 60 * 24);
                    } catch (NumberFormatException e) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "seen" + ChatColor.YELLOW + "' is not a valid number of days.");
                    }
                    if (seen < 0) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "seen" + ChatColor.YELLOW + "' must be a positive number.");
                    }
                    seen = System.currentTimeMillis() - seen;

                // page
                } else if (filter.equalsIgnoreCase("page")) {
                    i++;
                    if (i >= args.argsLength()) {
                        throw new CommandException(ChatColor.YELLOW + "Please include a page number for filter '" + ChatColor.WHITE + "page" + 
                                ChatColor.YELLOW + "'");
                    }
                    filter = args.getString(i);
                    try {
                        page = Integer.parseInt(filter) - 1;
                    } catch (NumberFormatException e) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' is not a valid number.");
                    }
                    if (page < 0) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' must be at least 1.");
                    }

                // page#
                } else if (filter.toLowerCase().startsWith("page") && filter.length() > 4) {
                    try {
                        page = Integer.parseInt(filter.substring(4)) - 1;
                    } catch (NumberFormatException e) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' is not a valid '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' filter");
                    }
                    if (page < 0) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' must be at least 1.");
                    }

                // p#
                } else if (filter.toLowerCase().startsWith("p") && filter.length() > 1) {
                    try {
                        page = Integer.parseInt(filter.substring(1)) - 1;
                    } catch (NumberFormatException e) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' is not a valid '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' filter");
                    }
                    if (page < 0) {
                        throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + filter + ChatColor.YELLOW + "' for filter '" + 
                                ChatColor.WHITE + "page" + ChatColor.YELLOW + "' must be at least 1.");
                    }
                }

            // end args loop
            }

            World world;
            if (listWorld != null && fullList != null && !fullList.isEmpty()) {
                if (joinList != null) {
                    throw new CommandException(ChatColor.YELLOW + "Filters '" + ChatColor.WHITE + "refine" + ChatColor.YELLOW + "' and '" + 
                            ChatColor.WHITE + "join" + ChatColor.YELLOW + "' can't be used together.");
                }
                world = listWorld;
            } else if (joinList != null) {
                world = state.listWorld;
            } else if (argWorld != null) {
                world = argWorld;
            } else {
                Player player;
                if (sender instanceof Player) {
                    player = (Player) sender;
                    world = player.getWorld();
                } else {
                    throw new CommandException(ChatColor.YELLOW + "Please include filter '" + ChatColor.WHITE + "world" + 
                            ChatColor.YELLOW + "'");
                }
            }

            wcfg = cfg.get(world);
            if (!wcfg.useStakes) {
                throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
            }

            if (fullList.isEmpty()) {
                final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
                }
                fullList.addAll(rgMgr.getRegions().values());
            }

            if (page == null) {
                page = 0;
            }

            StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
            LinkedHashMap<Integer, ProtectedRegion> regionList = SACUtil.filterList(plugin, fullList, world, 
                    joinList, id, owner, member, typo, banned, pending, hasMembers, claimed, vip, absent, seen);

            state.regionList = regionList;
            state.listWorld = world;

            SACUtil.displayList(plugin, sender, regionList, sMgr, world, page);

        // if no args
        } else {
            throw new CommandException(ChatColor.YELLOW + "Please include some filters. Do " + ChatColor.WHITE + "/sac filters" + 
                    ChatColor.YELLOW + " to see a full list.");
        }
    }

    @Command(aliases = {"pending", "p"},
            usage = "[page #]",
            desc = "Populates a list of pending stakes",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.pending")
    public void pending(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }
        ArrayList<ProtectedRegion> fullList = new ArrayList<ProtectedRegion>();
        fullList.addAll(rgMgr.getRegions().values());

        LinkedHashMap<Integer, ProtectedRegion> regionList = SACUtil.filterList(plugin, fullList, world, 
                null, null, null, null, null, null, true, null, null, null, null, null);

        state.regionList = regionList;
        state.listWorld = world;

        int page = 0;
        if (args.argsLength() == 1) {
            try {
                page = args.getInteger(0) - 1;
            } catch (NumberFormatException e) {
            }
        }

        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        SACUtil.displayList(plugin, sender, regionList, sMgr, world, page);
    }

    @Command(aliases = {"open", "o"},
            usage = "[page #]",
            desc = "Populates a list of open claims",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.open")
    public void open(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }
        ArrayList<ProtectedRegion> fullList = new ArrayList<ProtectedRegion>();
        fullList.addAll(rgMgr.getRegions().values());

        LinkedHashMap<Integer, ProtectedRegion> regionList = SACUtil.filterList(plugin, fullList, world, 
                null, null, null, null, null, null, false, null, false, null, null, null);

        state.regionList = regionList;
        state.listWorld = world;

        int page = 0;
        if (args.argsLength() == 1) {
            try {
                page = args.getInteger(0) - 1;
            } catch (NumberFormatException e) {
            }
        }

        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        SACUtil.displayList(plugin, sender, regionList, sMgr, world, page);
    }

    @Command(aliases = {"claim", "c"},
            usage = "<list item #> or <claim id> [world]",
            desc = "View detailed info on one claim",
            min = 1, max = 2)
    @CommandPermissions("stakeaclaim.sac.claim")
    public void claim(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();
        String argWorld = null;
        World world = null;
        ProtectedRegion claim = null;
        LinkedHashMap<Integer, ProtectedRegion> fullList;
        WorldConfig wcfg;

        if (args.argsLength() == 2) {
            argWorld = args.getString(1);
            for (World oneWorld : plugin.getServer().getWorlds()) {
                if (oneWorld.getName().equalsIgnoreCase(argWorld)) {
                    world = oneWorld;
                    break;
                }
            }
            if (world == null) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + argWorld + ChatColor.YELLOW + "' is not a valid world.");
            }
        }

        if (world == null && sender instanceof Player) {
            world = ((Player) sender).getWorld();
        }

        if (world == null && state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
             world = state.listWorld;
        }

        if (world == null) {
            throw new CommandException(ChatColor.YELLOW + "Please include a world.");
        }

        wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        String regionID = args.getString(0);
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        final Matcher regexMat = regexPat.matcher(regionID);
        if (regexMat.find()) {
            claim = rgMgr.getRegion(regionID);
        }

        Integer item = null;
        if (claim == null) {
            try {
                item = args.getInteger(0) - 1;
            } catch (NumberFormatException e) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + regionID + 
                        ChatColor.YELLOW + "' is not a valid list item number or claim id.");
            }

            if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
                world = state.listWorld;
                fullList = state.regionList;
            } else {
                throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
            }

            if (!fullList.containsKey(item)) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + regionID + ChatColor.YELLOW + "' is not a valid list item number.");
            }
            claim = fullList.get(item);
        }

        wcfg = cfg.get(world);
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        final Stake stake = sMgr.getStake(claim);

        SACUtil.displayClaim(wcfg, claim, stake, sender, plugin, world, item);
    }

    @Command(aliases = {"user", "u"},
            usage = "<list item #> or <player> [world]",
            desc = "View detailed info on one user",
            min = 1, max = 2)
    @CommandPermissions("stakeaclaim.sac.user")
    public void user(CommandContext args, CommandSender sender) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();
        String argWorld = null;
        World world = null;
        ProtectedRegion claim = null;
        String playerName = null;
        LinkedHashMap<Integer, ProtectedRegion> fullList;
        WorldConfig wcfg;

        if (args.argsLength() == 2) {
            argWorld = args.getString(1);
            for (World oneWorld : plugin.getServer().getWorlds()) {
                if (oneWorld.getName().equalsIgnoreCase(argWorld)) {
                    world = oneWorld;
                    break;
                }
            }
            if (world == null) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + argWorld + ChatColor.YELLOW + "' is not a valid world.");
            }
        }

        if (world == null && sender instanceof Player) {
            world = ((Player) sender).getWorld();
        }

        if (world == null && state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
             world = state.listWorld;
        }

        if (world == null) {
            throw new CommandException(ChatColor.YELLOW + "Please include a world.");
        }

        wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
            fullList = state.regionList;
            try {
                int item = args.getInteger(0) - 1;
                claim = fullList.get(item);
            } catch (NumberFormatException e) {
            }
            if (claim != null) {
                world = state.listWorld;
                final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                final Stake stake = sMgr.getStake(claim);
                for (String owner : claim.getOwners().getPlayers()) {
                    playerName = owner;
                    break;
                }
                if (playerName == null && stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
                    playerName = stake.getStakeName();
                }
            }
        }

        if (playerName == null) {
            playerName = args.getString(0);
        }

        RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        SACUtil.displayPlayer(plugin, sender, rgMgr, world, playerName);
    }

    @Command(aliases = {"goto", "g", "go"},
            usage = "<list item #> or <claim id> [world]",
            desc = "Goto to a claim",
            min = 0, max = 2)
    @CommandPermissions("stakeaclaim.sac.goto")
    public void togo(CommandContext args, CommandSender sender) throws CommandException{
        doGoto(args, sender, false);
    }

    @Command(aliases = {"spawn"},
            usage = "<list item #> or <claim id> [world]",
            desc = "Goto to a claim's spawn",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.spawn")
    public void spawn(CommandContext args, CommandSender sender) throws CommandException {
        doGoto(args, sender, true);
    }

    @Command(aliases = {"do", "d"},
        desc = "Do action on all or part of the list")
    @NestedCommand(DoCommands.class)
    @CommandPermissions("stakeaclaim.sac.do")
    public void doAction(CommandContext args, CommandSender sender) {}

    @Command(aliases = {"write", "save", "w"},
            usage = "[world] or ['all']",
            desc = "Write stakes to file",
            min = 0, max = 1)
    @CommandPermissions("stakeaclaim.sac.write")
    public void write(CommandContext args, CommandSender sender) throws CommandException {
        if (args.argsLength() == 1) {
            String in = args.getString(0);

            // save one world
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getName().equalsIgnoreCase(in)) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
                    }
                    sMgr.save();
                    throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes saved for '" + world.getName() + "'");
                }
            }
    
            // save all worlds
            if (in.equalsIgnoreCase("all") || in.equalsIgnoreCase("a")) {
                for (World world : plugin.getServer().getWorlds()) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        continue;
                    }
                    sMgr.save();
                    sender.sendMessage(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes saved for '" + world.getName() + "'");
                }
                throw new CommandException(ChatColor.YELLOW + "All worlds saved.");
            }
        }

        // save the world the player is in
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            throw new CommandException("A world is expected.");
        }
        final World world = player.getWorld();
        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        if (sMgr == null) {
            throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
        }
        sMgr.save();
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
                if (world.getName().equalsIgnoreCase(in)) {
                    StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                    if (sMgr == null) {
                        throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
                    }
                    read(sMgr);
                    throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes loaded for '" + world.getName() + "'");
                }
            }
    
            // load all worlds
            if (in.equalsIgnoreCase("all") || in.equalsIgnoreCase("a")) {
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
            if (in.equalsIgnoreCase("config") || in.equalsIgnoreCase("c")) {
                ConfigManager config = plugin.getGlobalManager();
                config.load();
                throw new CommandException(ChatColor.YELLOW + "SAC config reloaded.");
            }
        }

        // load the world the player is in
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            throw new CommandException("A world is expected.");
        }
        final World world = player.getWorld();
        StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        if (sMgr == null) {
            throw new CommandException("No stake manager exists for world '" + world.getName() + "'.");
        }
        read(sMgr);
        throw new CommandException(ChatColor.YELLOW + "" + sMgr.getStakes().size() + " stakes loaded for '" + world.getName() + "'");

    }

    // other methods
    private void doGoto(CommandContext args, CommandSender sender, boolean spawn) throws CommandException {

        if (args.argsLength() == 0) {
            if (!SACUtil.gotoRememberedWarp(plugin, args, sender, spawn)) {
                sender.sendMessage(ChatColor.RED + "Too few arguments.");
                sender.sendMessage(ChatColor.RED + "/sac " + args.getCommand() + " <list item #> or <claim id> [world]");
            }
            return;
        }

        final Player player = SACUtil.checkPlayer(sender);
        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        final ConfigManager cfg = plugin.getGlobalManager();
        World world = null;
        ProtectedRegion claim = null;
        LinkedHashMap<Integer, ProtectedRegion> fullList;

        if (args.argsLength() == 2) {
            String argWorld = args.getString(1);
            for (World oneWorld : plugin.getServer().getWorlds()) {
                if (oneWorld.getName().equalsIgnoreCase(argWorld)) {
                    world = oneWorld;
                    break;
                }
            }
            if (world == null) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + argWorld + ChatColor.YELLOW + "' is not a valid world.");
            }
        }

        if (world == null) {
            world = player.getWorld();
        }

        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
        if (rgMgr == null) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        String regionID = args.getString(0);
        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
        final Matcher regexMat = regexPat.matcher(regionID);
        if (regexMat.find()) {
            claim = rgMgr.getRegion(regionID);
        }

        if (claim == null) {
            int item;
            try {
                item = args.getInteger(0) - 1;
            } catch (NumberFormatException e) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + regionID + 
                        ChatColor.YELLOW + "' is not a valid list item number or claim id.");
            }

            if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
                world = state.listWorld;
                fullList = state.regionList;
            } else {
                throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
            }

            if (!fullList.containsKey(item)) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + regionID + ChatColor.YELLOW + "' is not a valid list item number.");
            }
            claim = fullList.get(item);
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        state.lastWarp = SACUtil.warpTo(plugin, world, claim, sMgr.getStake(claim), player, spawn);
        if (state.lastWarp == null) {
            state.warpWorld = null;
        } else {
            state.warpWorld = world;
        }

    }

    private void read(StakeManager sMgr) throws CommandException {
        try {
            sMgr.load();
        } catch (StakeDatabaseException e) {
            throw new CommandException("Uh oh, stakes did not load: " + e.getMessage());
        }
    }

}
