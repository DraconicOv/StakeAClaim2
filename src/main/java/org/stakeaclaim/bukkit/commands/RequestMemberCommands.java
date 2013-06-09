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

package org.stakeaclaim.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.domains.DefaultDomain;
import org.stakeaclaim.stakes.databases.ProtectionDatabaseException;
import org.stakeaclaim.stakes.databases.RequestDBUtil;
import org.stakeaclaim.stakes.flags.DefaultFlag;
import org.stakeaclaim.stakes.managers.RequestManager;
import org.stakeaclaim.stakes.StakeRequest;

// @TODO: A lot of code duplication here! Need to fix.

public class RequestMemberCommands {
    private final StakeAClaimPlugin plugin;

    public RequestMemberCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

//    @Command(aliases = {"addmember", "addmember"},
//            usage = "<id> <members...>",
//            flags = "w:",
//            desc = "Add a member to a request",
//            min = 2)
//    public void addMember(CommandContext args, CommandSender sender) throws CommandException {
//        final World world;
//        Player player;
//        LocalPlayer localPlayer = null;
//        if (args.hasFlag('w')) {
//            world = plugin.matchWorld(sender, args.getFlag('w'));
//        } else {
//            player = plugin.checkPlayer(sender);
//            localPlayer = plugin.wrapPlayer(player);
//            world = player.getWorld();
//        }
//
//        String id = args.getString(0);
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        id = request.getId();
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.addmember.own." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.addmember.member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.addmember." + id.toLowerCase());
//            }
//        }
//
//        RequestDBUtil.addToDomain(request.getMembers(), args.getPaddedSlice(2, 0), 0);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Request '" + id + "' updated.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"addowner", "addowner"},
//            usage = "<id> <owners...>",
//            flags = "w:",
//            desc = "Add an owner to a request",
//            min = 2)
//    public void addOwner(CommandContext args, CommandSender sender) throws CommandException {
//        final World world;
//        Player player = null;
//        LocalPlayer localPlayer = null;
//        if (args.hasFlag('w')) {
//            world = plugin.matchWorld(sender, args.getFlag('w'));
//        } else {
//            player = plugin.checkPlayer(sender);
//            localPlayer = plugin.wrapPlayer(player);
//            world = player.getWorld();
//        }
//
//        String id = args.getString(0);
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        id = request.getId();
//
//        Boolean flag = request.getFlag(DefaultFlag.BUYABLE);
//        DefaultDomain owners = request.getOwners();
//        if (localPlayer != null) {
//            if (flag != null && flag && owners != null && owners.size() == 0) {
//                if (!plugin.hasPermission(player, "stakeaclaim.request.unlimited")) {
//                    int maxRequestCount = plugin.getGlobalStateManager().get(world).getMaxRequestCount(player);
//                    if (maxRequestCount >= 0 && mgr.getRequestCountOfPlayer(localPlayer)
//                            >= maxRequestCount) {
//                        throw new CommandException("You already own the maximum allowed amount of requests.");
//                    }
//                }
//                plugin.checkPermission(sender, "stakeaclaim.request.addowner.unclaimed." + id.toLowerCase());
//            } else {
//                if (request.isOwner(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.addowner.own." + id.toLowerCase());
//                } else if (request.isMember(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.addowner.member." + id.toLowerCase());
//                } else {
//                    plugin.checkPermission(sender, "stakeaclaim.request.addowner." + id.toLowerCase());
//                }
//            }
//        }
//
//        RequestDBUtil.addToDomain(request.getOwners(), args.getPaddedSlice(2, 0), 0);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Request '" + id + "' updated.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"removemember", "remmember", "removemem", "remmem"},
//            usage = "<id> <owners...>",
//            flags = "w:",
//            desc = "Remove an owner to a request",
//            min = 2)
//    public void removeMember(CommandContext args, CommandSender sender) throws CommandException {
//        final World world;
//        Player player;
//        LocalPlayer localPlayer = null;
//        if (args.hasFlag('w')) {
//            world = plugin.matchWorld(sender, args.getFlag('w'));
//        } else {
//            player = plugin.checkPlayer(sender);
//            localPlayer = plugin.wrapPlayer(player);
//            world = player.getWorld();
//        }
//
//        String id = args.getString(0);
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        id = request.getId();
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.removemember.own." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.removemember.member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.removemember." + id.toLowerCase());
//            }
//        }
//
//        RequestDBUtil.removeFromDomain(request.getMembers(), args.getPaddedSlice(2, 0), 0);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Request '" + id + "' updated.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"removeowner", "remowner"},
//            usage = "<id> <owners...>",
//            flags = "w:",
//            desc = "Remove an owner to a request",
//            min = 2)
//    public void removeOwner(CommandContext args,
//            CommandSender sender) throws CommandException {
//        final World world;
//        Player player;
//        LocalPlayer localPlayer = null;
//        if (args.hasFlag('w')) {
//            world = plugin.matchWorld(sender, args.getFlag('w'));
//        } else {
//            player = plugin.checkPlayer(sender);
//            localPlayer = plugin.wrapPlayer(player);
//            world = player.getWorld();
//        }
//
//        String id = args.getString(0);
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        id = request.getId();
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.removeowner.own." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.removeowner.member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.removeowner." + id.toLowerCase());
//            }
//        }
//
//        RequestDBUtil.removeFromDomain(request.getOwners(), args.getPaddedSlice(2, 0), 0);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Request '" + id + "' updated.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }
}
