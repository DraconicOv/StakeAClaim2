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

package org.stakeaclaim.bukkit.commands;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

//import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.bukkit.WorldConfiguration;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;
import org.stakeaclaim.domains.DefaultDomain;
import org.stakeaclaim.stakes.ApplicableRequestSet;
import org.stakeaclaim.stakes.databases.ProtectionDatabaseException;
import org.stakeaclaim.stakes.databases.RequestDBUtil;
import org.stakeaclaim.stakes.databases.migrators.AbstractDatabaseMigrator;
import org.stakeaclaim.stakes.databases.migrators.MigrationException;
import org.stakeaclaim.stakes.databases.migrators.MigratorKey;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.flags.Flag;
//import org.stakeaclaim.stakes.flags.InvalidFlagFormat;
//import org.stakeaclaim.stakes.flags.RequestGroup;
//import org.stakeaclaim.stakes.flags.RequestGroupFlag;
import org.stakeaclaim.stakes.RequestManager;
//import org.stakeaclaim.stakes.requests.GlobalRequest;
//import org.stakeaclaim.stakes.requests.ProtectedCuboidRequest;
//import org.stakeaclaim.stakes.requests.ProtectedPolygonalRequest;
import org.stakeaclaim.stakes.StakeRequest;
//import org.stakeaclaim.stakes.requests.Request.CircularInheritanceException;

public class RequestCommands {
    private final StakeAClaimPlugin plugin;

    private MigratorKey migrateDBRequest;
    private Date migrateDBRequestDate;

    public RequestCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

//    @Command(aliases = {"define", "def", "d"}, usage = "<id> [<owner1> [<owner2> [<owners...>]]]",
//            desc = "Defines a request", min = 1)
//    @CommandPermissions({"stakeaclaim.request.define"})
//    public void define(CommandContext args, CommandSender sender) throws CommandException {
//
//        Player player = plugin.checkPlayer(sender);
//        WorldEditPlugin worldEdit = plugin.getWorldEdit();
//        String id = args.getString(0);
//
//        if (!StakeRequest.isValidId(id)) {
//            throw new CommandException("Invalid request ID specified!");
//        }
//
//        if (id.equalsIgnoreCase("__global__")) {
//            throw new CommandException("A request cannot be named __global__");
//        }
//
//        // Attempt to get the player's selection from WorldEdit
//        Selection sel = worldEdit.getSelection(player);
//
//        if (sel == null) {
//            throw new CommandException("Select a request with WorldEdit first.");
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(sel.getWorld());
//        if (mgr.hasRequest(id)) {
//            throw new CommandException("That request is already defined. Use redefine instead.");
//        }
//
//        StakeRequest request;
//
//        // Detect the type of request from WorldEdit
//        if (sel instanceof Polygonal2DSelection) {
//            Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
//            int minY = polySel.getNativeMinimumPoint().getBlockY();
//            int maxY = polySel.getNativeMaximumPoint().getBlockY();
//            request = new ProtectedPolygonalRequest(id, polySel.getNativePoints(), minY, maxY);
//        } else if (sel instanceof CuboidSelection) {
//            BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
//            BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
//            request = new ProtectedCuboidRequest(id, min, max);
//        } else {
//            throw new CommandException(
//                    "The type of request selected in WorldEdit is unsupported in StakeAClaim!");
//        }
//
//        // Get the list of request owners
//        if (args.argsLength() > 1) {
//            request.setOwners(RequestDBUtil.parseDomainString(args.getSlice(1), 1));
//        }
//
//        mgr.addRequest(request);
//
//        try {
//            mgr.save();
//            sender.sendMessage(ChatColor.YELLOW + "Request saved as " + id + ".");
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"redefine", "update", "move"}, usage = "<id>",
//            desc = "Re-defines the shape of a request", min = 1, max = 1)
//    public void redefine(CommandContext args, CommandSender sender) throws CommandException {
//
//        Player player = plugin.checkPlayer(sender);
//        World world = player.getWorld();
//        WorldEditPlugin worldEdit = plugin.getWorldEdit();
//        LocalPlayer localPlayer = plugin.wrapPlayer(player);
//        String id = args.getString(0);
//
//        if (id.equalsIgnoreCase("__global__")) {
//            throw new CommandException("The request cannot be named __global__");
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest existing = mgr.getRequestExact(id);
//
//        if (existing == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        if (existing.isOwner(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.redefine.own");
//        } else if (existing.isMember(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.redefine.member");
//        } else {
//            plugin.checkPermission(sender, "stakeaclaim.request.redefine");
//        } 
//
//        // Attempt to get the player's selection from WorldEdit
//        Selection sel = worldEdit.getSelection(player);
//
//        if (sel == null) {
//            throw new CommandException("Select a request with WorldEdit first.");
//        }
//
//        StakeRequest request;
//
//        // Detect the type of request from WorldEdit
//        if (sel instanceof Polygonal2DSelection) {
//            Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
//            int minY = polySel.getNativeMinimumPoint().getBlockY();
//            int maxY = polySel.getNativeMaximumPoint().getBlockY();
//            request = new ProtectedPolygonalRequest(id, polySel.getNativePoints(), minY, maxY);
//        } else if (sel instanceof CuboidSelection) {
//            BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
//            BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
//            request = new ProtectedCuboidRequest(id, min, max);
//        } else {
//            throw new CommandException(
//                    "The type of request selected in WorldEdit is unsupported in StakeAClaim!");
//        }
//
//        request.setMembers(existing.getMembers());
//        request.setOwners(existing.getOwners());
//        request.setFlags(existing.getFlags());
//        request.setPriority(existing.getPriority());
//        try {
//            request.setParent(existing.getParent());
//        } catch (CircularInheritanceException ignore) {
//        }
//
//        mgr.addRequest(request);
//
//        sender.sendMessage(ChatColor.YELLOW + "Request updated with new area.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"claim"}, usage = "<id> [<owner1> [<owner2> [<owners...>]]]",
//            desc = "Claim a request", min = 1)
//    @CommandPermissions({"stakeaclaim.request.claim"})
//    public void claim(CommandContext args, CommandSender sender) throws CommandException {
//
//        Player player = plugin.checkPlayer(sender);
//        LocalPlayer localPlayer = plugin.wrapPlayer(player);
//        WorldEditPlugin worldEdit = plugin.getWorldEdit();
//        String id = args.getString(0);
//
//        if (!StakeRequest.isValidId(id)) {
//            throw new CommandException("Invalid request ID specified!");
//        }
//
//        if (id.equalsIgnoreCase("__global__")) {
//            throw new CommandException("A request cannot be named __global__");
//        }
//
//        // Attempt to get the player's selection from WorldEdit
//        Selection sel = worldEdit.getSelection(player);
//
//        if (sel == null) {
//            throw new CommandException("Select a request with WorldEdit first.");
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(sel.getWorld());
//
//        if (mgr.hasRequest(id)) {
//            throw new CommandException("That request already exists. Please choose a different name.");
//        }
//
//        StakeRequest request;
//
//        // Detect the type of request from WorldEdit
//        if (sel instanceof Polygonal2DSelection) {
//            Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
//            int minY = polySel.getNativeMinimumPoint().getBlockY();
//            int maxY = polySel.getNativeMaximumPoint().getBlockY();
//            request = new ProtectedPolygonalRequest(id, polySel.getNativePoints(), minY, maxY);
//        } else if (sel instanceof CuboidSelection) {
//            BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
//            BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
//            request = new ProtectedCuboidRequest(id, min, max);
//        } else {
//            throw new CommandException(
//                    "The type of request selected in WorldEdit is unsupported in StakeAClaim!");
//        }
//
//        // Get the list of request owners
//        if (args.argsLength() > 1) {
//            request.setOwners(RequestDBUtil.parseDomainString(args.getSlice(1), 1));
//        }
//
//        WorldConfiguration wcfg = plugin.getGlobalStateManager().get(player.getWorld());
//
//        if (!plugin.hasPermission(sender, "stakeaclaim.request.unlimited")) {
//            // Check whether the player has created too many requests 
//            int maxRequestCount = wcfg.getMaxRequestCount(player);
//            if (maxRequestCount >= 0
//                    && mgr.getRequestCountOfPlayer(localPlayer) >= maxRequestCount) {
//                throw new CommandException("You own too many requests, delete one first to claim a new one.");
//            }
//        }
//
//        StakeRequest existing = mgr.getRequestExact(id);
//
//        // Check for an existing request
//        if (existing != null) {
//            if (!existing.getOwners().contains(localPlayer)) {
//                throw new CommandException("This request already exists and you don't own it.");
//            }
//        }
//
//        ApplicableRequestSet requests = mgr.getApplicableRequests(request);
//
//        // Check if this request overlaps any other request
//        if (requests.size() > 0) {
//            if (!requests.isOwnerOfAll(localPlayer)) {
//                throw new CommandException("This request overlaps with someone else's request.");
//            }
//        } else {
//            if (wcfg.claimOnlyInsideExistingRequests) {
//                throw new CommandException("You may only claim requests inside " +
//                        "existing requests that you or your group own.");
//            }
//        }
//
//        /*if (plugin.getGlobalConfiguration().getiConomy() != null && wcfg.useiConomy && wcfg.buyOnClaim) {
//            if (iConomy.getBank().hasAccount(player.getName())) {
//                Account account = iConomy.getBank().getAccount(player.getName());
//                double balance = account.getBalance();
//                double requestCosts = request.countBlocks() * wcfg.buyOnClaimPrice;
//                if (balance >= requestCosts) {
//                    account.subtract(requestCosts);
//                    player.sendMessage(ChatColor.YELLOW + "You have bought that request for "
//                            + iConomy.getBank().format(requestCosts));
//                    account.save();
//                } else {
//                    player.sendMessage(ChatColor.RED + "You have not enough money.");
//                    player.sendMessage(ChatColor.RED + "The request you want to claim costs "
//                            + iConomy.getBank().format(requestCosts));
//                    player.sendMessage(ChatColor.RED + "You have " + iConomy.getBank().format(balance));
//                    return;
//                }
//            } else {
//                player.sendMessage(ChatColor.YELLOW + "You have not enough money.");
//                return;
//            }
//        }*/
//
//        if (!plugin.hasPermission(sender, "stakeaclaim.request.unlimited")) {
//            if (request.volume() > wcfg.maxClaimVolume) {
//                player.sendMessage(ChatColor.RED + "This request is too large to claim.");
//                player.sendMessage(ChatColor.RED +
//                        "Max. volume: " + wcfg.maxClaimVolume + ", your volume: " + request.volume());
//                return;
//            }
//        }
//
//        request.getOwners().addPlayer(player.getName());
//        mgr.addRequest(request);
//
//        try {
//            mgr.save();
//            sender.sendMessage(ChatColor.YELLOW + "Request saved as " + id + ".");
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"select", "sel", "s"}, usage = "[id]",
//            desc = "Load a request as a WorldEdit selection", min = 0, max = 1)
//    public void select(CommandContext args, CommandSender sender) throws CommandException {
//
//        final Player player = plugin.checkPlayer(sender);
//        final World world = player.getWorld();
//        final LocalPlayer localPlayer = plugin.wrapPlayer(player);
//
//        final RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//
//        final String id;
//        if (args.argsLength() == 0) {
//            final Vector pt = localPlayer.getPosition();
//            final ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//            if (set.size() == 0) {
//                throw new CommandException("No request ID specified and no request found at current location!");
//            }
//
//            id = set.iterator().next().getId();
//        }
//        else {
//            id = args.getString(0);
//        }
//
//        final StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        selectRequest(player, localPlayer, request);
//    }

//    public void selectRequest(Player player, LocalPlayer localPlayer, StakeRequest request) throws CommandException, CommandPermissionsException {
//        final WorldEditPlugin worldEdit = plugin.getWorldEdit();
//        final String id = request.getId();
//
//        if (request.isOwner(localPlayer)) {
//            plugin.checkPermission(player, "stakeaclaim.request.select.own." + id.toLowerCase());
//        } else if (request.isMember(localPlayer)) {
//            plugin.checkPermission(player, "stakeaclaim.request.select.member." + id.toLowerCase());
//        } else {
//            plugin.checkPermission(player, "stakeaclaim.request.select." + id.toLowerCase());
//        }
//
//        final World world = player.getWorld();
//        if (request instanceof ProtectedCuboidRequest) {
//            final ProtectedCuboidRequest cuboid = (ProtectedCuboidRequest) request;
//            final Vector pt1 = cuboid.getMinimumPoint();
//            final Vector pt2 = cuboid.getMaximumPoint();
//            final CuboidSelection selection = new CuboidSelection(world, pt1, pt2);
//            worldEdit.setSelection(player, selection);
//            player.sendMessage(ChatColor.YELLOW + "Request selected as a cuboid.");
//        } else if (request instanceof ProtectedPolygonalRequest) {
//            final ProtectedPolygonalRequest poly2d = (ProtectedPolygonalRequest) request;
//            final Polygonal2DSelection selection = new Polygonal2DSelection(
//                    world, poly2d.getPoints(),
//                    poly2d.getMinimumPoint().getBlockY(),
//                    poly2d.getMaximumPoint().getBlockY()
//                    );
//            worldEdit.setSelection(player, selection);
//            player.sendMessage(ChatColor.YELLOW + "Request selected as a polygon.");
//        } else if (request instanceof GlobalRequest) {
//            throw new CommandException("Can't select global requests.");
//        } else {
//            throw new CommandException("Unknown request type: " + request.getClass().getCanonicalName());
//        }
//    }

//    @Command(aliases = {"info", "i"}, usage = "[world] [id]", flags = "s",
//            desc = "Get information about a request", min = 0, max = 2)
//    public void info(CommandContext args, CommandSender sender) throws CommandException {
//
//        final LocalPlayer localPlayer;
//        final World world;
//        if (sender instanceof Player) {
//            final Player player = (Player) sender;
//            localPlayer = plugin.wrapPlayer(player);
//            world = player.getWorld();
//        } else if (args.argsLength() < 2) {
//            throw new CommandException("A player is expected.");
//        } else {
//            localPlayer = null;
//            world = plugin.matchWorld(sender, args.getString(0));
//        }
//
//        final RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//
//        final String id;
//
//        // Get different values based on provided arguments
//        switch (args.argsLength()) {
//        case 0:
//            if (localPlayer == null) {
//                throw new CommandException("A player is expected.");
//            }
//
//            final Vector pt = localPlayer.getPosition();
//            final ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//            if (set.size() == 0) {
//                throw new CommandException("No request ID specified and no request found at current location!");
//            }
//
//            id = set.iterator().next().getId();
//            break;
//
//        case 1:
//            id = args.getString(0).toLowerCase();
//            break;
//
//        default:
//            id = args.getString(1).toLowerCase();
//        }
//
//        final StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            if (!StakeRequest.isValidId(id)) {
//                throw new CommandException("Invalid request ID specified!");
//            }
//            throw new CommandException("A request with ID '" + id + "' doesn't exist.");
//        }
//
//        displayRequestInfo(sender, localPlayer, request);
//
//        if (args.hasFlag('s')) {
//            selectRequest(plugin.checkPlayer(sender), localPlayer, request);
//        }
//    }

//    public void displayRequestInfo(CommandSender sender, final LocalPlayer localPlayer, StakeRequest request) throws CommandPermissionsException {
//        if (localPlayer == null) {
//            plugin.checkPermission(sender, "stakeaclaim.request.info");
//        } else if (request.isOwner(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.info.own");
//        } else if (request.isMember(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.info.member");
//        } else {
//            plugin.checkPermission(sender, "stakeaclaim.request.info");
//        }
//
//        final String id = request.getId();
//
//        sender.sendMessage(ChatColor.YELLOW + "Request: " + id + ChatColor.GRAY + ", type: " + request.getTypeName() + ", " + ChatColor.BLUE + "Priority: " + request.getPriority());
//
//        boolean hasFlags = false;
//        final StringBuilder s = new StringBuilder(ChatColor.BLUE + "Flags: ");
//        for (Flag<?> flag : DefaultFlag.getFlags()) {
//            Object val = request.getFlag(flag), group = null;
//
//            if (val == null) {
//                continue;
//            }
//
//            if (hasFlags) {
//                s.append(", ");
//            }
//
//            RequestGroupFlag groupFlag = flag.getRequestGroupFlag();
//            if (groupFlag != null) {
//                group = request.getFlag(groupFlag);
//            }
//
//            if(group == null) {
//                s.append(flag.getName()).append(": ").append(String.valueOf(val));
//            } else {
//                s.append(flag.getName()).append(" -g ").append(String.valueOf(group)).append(": ").append(String.valueOf(val));
//            }
//
//            hasFlags = true;
//        }
//        if (hasFlags) {
//            sender.sendMessage(s.toString());
//        }
//
//        if (request.getParent() != null) {
//            sender.sendMessage(ChatColor.BLUE + "Parent: " + request.getParent().getId());
//        }
//
//        final DefaultDomain owners = request.getOwners();
//        if (owners.size() != 0) {
//            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Owners: " + owners.toUserFriendlyString());
//        }
//
//        final DefaultDomain members = request.getMembers();
//        if (members.size() != 0) {
//            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Members: " + members.toUserFriendlyString());
//        }
//
//        final BlockVector min = request.getMinimumPoint();
//        final BlockVector max = request.getMaximumPoint();
//        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Bounds:"
//                + " (" + min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ() + ")"
//                + " (" + max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ() + ")"
//                );
//    }

//    public class RequestEntry implements Comparable<RequestEntry>{
//        private final String id;
//        private final int index;
//        private boolean isOwner;
//        private boolean isMember;
//
//        public RequestEntry(String id, int index) {
//            this.id = id;
//            this.index = index;
//        }
//
//        @Override
//        public int compareTo(RequestEntry o) {
//            if (isOwner != o.isOwner) {
//                return isOwner ? 1 : -1;
//            }
//            if (isMember != o.isMember) {
//                return isMember ? 1 : -1;
//            }
//            return id.compareTo(o.id);
//        }
//
//        @Override
//        public String toString() {
//            if (isOwner) {
//                return (index + 1) + ". +" + id;
//            } else if (isMember) {
//                return (index + 1) + ". -" + id;
//            } else {
//                return (index + 1) + ". " + id;
//            }
//        }
//    }

//    @Command(aliases = {"list"}, usage = "[.player] [page] [world]",
//            desc = "Get a list of requests", max = 3)
//    //@CommandPermissions({"stakeaclaim.request.list"})
//    public void list(CommandContext args, CommandSender sender) throws CommandException {
//
//        World world;
//        int page = 0;
//        int argOffset = 0;
//        String name = "";
//        boolean own = false;
//        LocalPlayer localPlayer = null;
//
//        final String senderName = sender.getName().toLowerCase();
//        if (args.argsLength() > 0 && args.getString(0).startsWith(".")) {
//            name = args.getString(0).substring(1).toLowerCase();
//            argOffset = 1;
//
//            if (name.equals("me") || name.isEmpty() || name.equals(senderName)) {
//                own = true;
//            }
//        }
//
//        // Make /rg list default to "own" mode if the "stakeaclaim.request.list" permission is not given
//        if (!own && !plugin.hasPermission(sender, "stakeaclaim.request.list")) {
//            own = true;
//        }
//
//        if (own) {
//            plugin.checkPermission(sender, "stakeaclaim.request.list.own");
//            name = senderName;
//            localPlayer = plugin.wrapPlayer(plugin.checkPlayer(sender));
//        }
//
//        if (args.argsLength() > argOffset) {
//            page = Math.max(0, args.getInteger(argOffset) - 1);
//        }
//
//        if (args.argsLength() > 1 + argOffset) {
//            world = plugin.matchWorld(sender, args.getString(1 + argOffset));
//        } else {
//            world = plugin.checkPlayer(sender).getWorld();
//        }
//
//        final RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        final Map<Long, StakeRequest> requests = mgr.getRequests();
//
//        List<RequestEntry> requestEntries = new ArrayList<RequestEntry>();
//        int index = 0;
//        for (String id : requests.keySet()) {
//            RequestEntry entry = new RequestEntry(id, index++);
//            if (!name.isEmpty()) {
//                if (own) {
//                    entry.isOwner = requests.get(id).isOwner(localPlayer);
//                    entry.isMember = requests.get(id).isMember(localPlayer);
//                }
//                else {
//                    entry.isOwner = requests.get(id).isOwner(name);
//                    entry.isMember = requests.get(id).isMember(name);
//                }
//
//                if (!entry.isOwner && !entry.isMember) {
//                    continue;
//                }
//            }
//
//            requestEntries.add(entry);
//        }
//
//        Collections.sort(requestEntries);
//
//        final int totalSize = requestEntries.size();
//        final int pageSize = 10;
//        final int pages = (int) Math.ceil(totalSize / (float) pageSize);
//
//        sender.sendMessage(ChatColor.RED
//                + (name.equals("") ? "Requests (page " : "Requests for " + name + " (page ")
//                + (page + 1) + " of " + pages + "):");
//
//        if (page < pages) {
//            for (int i = page * pageSize; i < page * pageSize + pageSize; i++) {
//                if (i >= totalSize) {
//                    break;
//                }
//                sender.sendMessage(ChatColor.YELLOW.toString() + requestEntries.get(i));
//            }
//        }
//    }

//    @Command(aliases = {"flag", "f"}, usage = "<id> <flag> [-g group] [value]", flags = "g:w:",
//            desc = "Set flags", min = 2)
//    public void flag(CommandContext args, CommandSender sender) throws CommandException {
//
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
//        String flagName = args.getString(1);
//        String value = null;
//        RequestGroup groupValue = null;
//
//        if (args.argsLength() >= 3) {
//            value = args.getJoinedStrings(2);
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//
//        if (request == null) {
//            if (id.equalsIgnoreCase("__global__")) {
//                request = new GlobalRequest(id);
//                mgr.addRequest(request);
//            } else {
//                throw new CommandException("Could not find a request by that ID.");
//            }
//        }
//
//        // @TODO deprecate "flag.[own./member./blank]"
//        boolean hasPerm = false;
//        if (localPlayer == null) {
//            hasPerm = true;
//        } else {
//            if (request.isOwner(localPlayer)) {
//                if (plugin.hasPermission(sender, "stakeaclaim.request.flag.own." + id.toLowerCase())) hasPerm = true;
//                else if (plugin.hasPermission(sender, "stakeaclaim.request.flag.requests.own." + id.toLowerCase())) hasPerm = true;
//            } else if (request.isMember(localPlayer)) {
//                if (plugin.hasPermission(sender, "stakeaclaim.request.flag.member." + id.toLowerCase())) hasPerm = true;
//                else if (plugin.hasPermission(sender, "stakeaclaim.request.flag.requests.member." + id.toLowerCase())) hasPerm = true;
//            } else {
//                if (plugin.hasPermission(sender, "stakeaclaim.request.flag." + id.toLowerCase())) hasPerm = true;
//                else if (plugin.hasPermission(sender, "stakeaclaim.request.flag.requests." + id.toLowerCase())) hasPerm = true;
//            }
//        }
//        if (!hasPerm) throw new CommandPermissionsException();
//
//        Flag<?> foundFlag = null;
//
//        // Now time to find the flag!
//        for (Flag<?> flag : DefaultFlag.getFlags()) {
//            // Try to detect the flag
//            if (flag.getName().replace("-", "").equalsIgnoreCase(flagName.replace("-", ""))) {
//                foundFlag = flag;
//                break;
//            }
//        }
//
//        if (foundFlag == null) {
//            StringBuilder list = new StringBuilder();
//
//            // Need to build a list
//            for (Flag<?> flag : DefaultFlag.getFlags()) {
//
//                // @TODO deprecate inconsistant "owner" permission
//                if (localPlayer != null) {
//                    if (request.isOwner(localPlayer)) {
//                        if (!plugin.hasPermission(sender, "stakeaclaim.request.flag.flags."
//                                + flag.getName() + ".owner." + id.toLowerCase())
//                                && !plugin.hasPermission(sender, "stakeaclaim.request.flag.flags."
//                                        + flag.getName() + ".own." + id.toLowerCase())) {
//                            continue;
//                        }
//                    } else if (request.isMember(localPlayer)) {
//                        if (!plugin.hasPermission(sender, "stakeaclaim.request.flag.flags."
//                                + flag.getName() + ".member." + id.toLowerCase())) {
//                            continue;
//                        }
//                    } else {
//                        if (!plugin.hasPermission(sender, "stakeaclaim.request.flag.flags."
//                                + flag.getName() + "." + id.toLowerCase())) {
//                            continue;
//                        }
//                    }
//                }
//
//                if (list.length() > 0) {
//                    list.append(", ");
//                }
//                list.append(flag.getName());
//            }
//
//            sender.sendMessage(ChatColor.RED + "Unknown flag specified: " + flagName);
//            sender.sendMessage(ChatColor.RED + "Available flags: " + list);
//            return;
//        }
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.flag.flags."
//                        + foundFlag.getName() + ".owner." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.flag.flags."
//                        + foundFlag.getName() + ".member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.flag.flags."
//                        + foundFlag.getName() + "." + id.toLowerCase());
//            }
//        }
//
//        if (args.hasFlag('g')) {
//            String group = args.getFlag('g');
//            RequestGroupFlag groupFlag = foundFlag.getRequestGroupFlag();
//            if (groupFlag == null) {
//                throw new CommandException("Request flag '" + foundFlag.getName()
//                        + "' does not have a group flag!");
//            }
//
//            // Parse the [-g group] separately so entire command can abort if parsing
//            // the [value] part throws an error.
//            try {
//                groupValue = groupFlag.parseInput(plugin, sender, group);
//            } catch (InvalidFlagFormat e) {
//                throw new CommandException(e.getMessage());
//            }
//
//        }
//
//        if (value != null) {
//            // Set the flag if [value] was given even if [-g group] was given as well
//            try {
//                setFlag(request, foundFlag, sender, value);
//            } catch (InvalidFlagFormat e) {
//                throw new CommandException(e.getMessage());
//            }
//
//            sender.sendMessage(ChatColor.YELLOW
//                    + "Request flag '" + foundFlag.getName() + "' set.");
//        }
//
//        if (value == null && !args.hasFlag('g')) {
//            // Clear the flag only if neither [value] nor [-g group] was given
//            request.setFlag(foundFlag, null);
//
//            // Also clear the associated group flag if one exists
//            RequestGroupFlag groupFlag = foundFlag.getRequestGroupFlag();
//            if (groupFlag != null) {
//                request.setFlag(groupFlag, null);
//            }
//
//            sender.sendMessage(ChatColor.YELLOW
//                    + "Request flag '" + foundFlag.getName() + "' cleared.");
//        }
//
//        if (groupValue != null) {
//            RequestGroupFlag groupFlag = foundFlag.getRequestGroupFlag();
//
//            // If group set to the default, then clear the group flag
//            if (groupValue == groupFlag.getDefault()) {
//                request.setFlag(groupFlag, null);
//                sender.sendMessage(ChatColor.YELLOW
//                        + "Request group flag for '" + foundFlag.getName() + "' reset to default.");
//            } else {
//                request.setFlag(groupFlag, groupValue);
//                sender.sendMessage(ChatColor.YELLOW
//                        + "Request group flag for '" + foundFlag.getName() + "' set.");
//            }
//        }
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    public <V> void setFlag(StakeRequest request,
//            Flag<V> flag, CommandSender sender, String value)
//                    throws InvalidFlagFormat {
//        request.setFlag(flag, flag.parseInput(plugin, sender, value));
//    }

//    @Command(aliases = {"setpriority", "priority", "pri"},
//            usage = "<id> <priority>",
//            flags = "w:",
//            desc = "Set the priority of a request",
//            min = 2, max = 2)
//    public void setPriority(CommandContext args, CommandSender sender) throws CommandException {
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
//        int priority = args.getInteger(1);
//
//        if (id.equalsIgnoreCase("__global__")) {
//            throw new CommandException("The request cannot be named __global__");
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        id = request.getId();
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.setpriority.own." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.setpriority.member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.setpriority." + id.toLowerCase());
//            }
//        }
//
//        request.setPriority(priority);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Priority of '" + request.getId() + "' set to "
//                + priority + ".");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"setparent", "parent", "par"}, 
//            usage = "<id> [parent-id]",
//            flags = "w:",
//            desc = "Set the parent of a request",
//            min = 1, max = 2)
//    public void setParent(CommandContext args, CommandSender sender) throws CommandException {
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
//        if (id.equalsIgnoreCase("__global__")) {
//            throw new CommandException("The request cannot be named __global__");
//        }
//
//        RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//        StakeRequest request = mgr.getRequest(id);
//        if (request == null) {
//            throw new CommandException("Could not find a target request by that ID.");
//        }
//
//        id = request.getId();
//
//        if (args.argsLength() == 1) {
//            try {
//                request.setParent(null);
//            } catch (CircularInheritanceException ignore) {
//            }
//
//            sender.sendMessage(ChatColor.YELLOW
//                    + "Parent of '" + request.getId() + "' cleared.");
//        } else {
//            String parentId = args.getString(1);
//            StakeRequest parent = mgr.getRequest(parentId);
//
//            if (parent == null) {
//                throw new CommandException("Could not find the parent request by that ID.");
//            }
//
//            if (localPlayer != null) {
//                if (request.isOwner(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent.own." + id.toLowerCase());
//                } else if (request.isMember(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent.member." + id.toLowerCase());
//                } else {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent." + id.toLowerCase());
//                } 
//
//                if (parent.isOwner(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent.own." + parentId.toLowerCase());
//                } else if (parent.isMember(localPlayer)) {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent.member." + parentId.toLowerCase());
//                } else {
//                    plugin.checkPermission(sender, "stakeaclaim.request.setparent." + parentId.toLowerCase());
//                }
//            }
//            try {
//                request.setParent(parent);
//            } catch (CircularInheritanceException e) {
//                throw new CommandException("Circular inheritance detected!");
//            }
//
//            sender.sendMessage(ChatColor.YELLOW
//                    + "Parent of '" + request.getId() + "' set to '"
//                    + parent.getId() + "'.");
//        }
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

//    @Command(aliases = {"remove", "delete", "del", "rem"},
//            usage = "<id>",
//            flags = "w:",
//            desc = "Remove a request",
//            min = 1, max = 1)
//    public void remove(CommandContext args, CommandSender sender) throws CommandException {
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
//        StakeRequest request = mgr.getRequestExact(id);
//
//        if (request == null) {
//            throw new CommandException("Could not find a request by that ID.");
//        }
//
//        if (localPlayer != null) {
//            if (request.isOwner(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.remove.own." + id.toLowerCase());
//            } else if (request.isMember(localPlayer)) {
//                plugin.checkPermission(sender, "stakeaclaim.request.remove.member." + id.toLowerCase());
//            } else {
//                plugin.checkPermission(sender, "stakeaclaim.request.remove." + id.toLowerCase());
//            }
//        }
//
//        mgr.removeRequest(id);
//
//        sender.sendMessage(ChatColor.YELLOW
//                + "Request '" + id + "' removed.");
//
//        try {
//            mgr.save();
//        } catch (ProtectionDatabaseException e) {
//            throw new CommandException("Failed to write requests: "
//                    + e.getMessage());
//        }
//    }

    @Command(aliases = {"load", "reload"}, usage = "[world]",
            desc = "Reload requests from file", max = 1)
    @CommandPermissions({"stakeaclaim.request.load"})
    public void load(CommandContext args, CommandSender sender) throws CommandException {

        World world = null;

        if (args.argsLength() > 0) {
            world = plugin.matchWorld(sender, args.getString(0));
        }

        if (world != null) {
            RequestManager mgr = plugin.getGlobalRequestManager().get(world);

            try {
                mgr.load();
                sender.sendMessage(ChatColor.YELLOW
                        + "Requests for '" + world.getName() + "' load.");
            } catch (ProtectionDatabaseException e) {
                throw new CommandException("Failed to read requests: "
                        + e.getMessage());
            }
        } else {
            for (World w : plugin.getServer().getWorlds()) {
                RequestManager mgr = plugin.getGlobalRequestManager().get(w);

                try {
                    mgr.load();
                } catch (ProtectionDatabaseException e) {
                    throw new CommandException("Failed to read requests: "
                            + e.getMessage());
                }
            }

            sender.sendMessage(ChatColor.YELLOW
                    + "Request databases loaded.");
        }
    }

    @Command(aliases = {"save", "write"}, usage = "[world]",
            desc = "Re-save requests to file", max = 1)
    @CommandPermissions({"stakeaclaim.request.save"})
    public void save(CommandContext args, CommandSender sender) throws CommandException {

        World world = null;

        if (args.argsLength() > 0) {
            world = plugin.matchWorld(sender, args.getString(0));
        }

        if (world != null) {
            RequestManager mgr = plugin.getGlobalRequestManager().get(world);

            try {
                mgr.save();
                sender.sendMessage(ChatColor.YELLOW
                        + "Requests for '" + world.getName() + "' saved.");
            } catch (ProtectionDatabaseException e) {
                throw new CommandException("Failed to write requests: "
                        + e.getMessage());
            }
        } else {
            for (World w : plugin.getServer().getWorlds()) {
                RequestManager mgr = plugin.getGlobalRequestManager().get(w);

                try {
                    mgr.save();
                } catch (ProtectionDatabaseException e) {
                    throw new CommandException("Failed to write requests: "
                            + e.getMessage());
                }
            }

            sender.sendMessage(ChatColor.YELLOW
                    + "Request databases saved.");
        }
    }

//    @Command(aliases = {"migratedb"}, usage = "<from> <to>",
//            desc = "Migrate from one Protection Database to another.", min = 1)
//    @CommandPermissions({"stakeaclaim.request.migratedb"})
//    public void migratedb(CommandContext args, CommandSender sender) throws CommandException {
//        String from = args.getString(0).toLowerCase().trim();
//        String to = args.getString(1).toLowerCase().trim();
//
//        if (from.equals(to)) {
//            throw new CommandException("Will not migrate with common source and target.");
//        }
//
//        Map<MigratorKey, Class<? extends AbstractDatabaseMigrator>> migrators = AbstractDatabaseMigrator.getMigrators();
//        MigratorKey key = new MigratorKey(from,to);
//
//        if (!migrators.containsKey(key)) {
//            throw new CommandException("No migrator found for that combination and direction.");
//        }
//
//        long lastRequest = 10000000;
//        if (this.migrateDBRequestDate != null) { 
//            lastRequest = new Date().getTime() - this.migrateDBRequestDate.getTime();
//        }
//        if (this.migrateDBRequest == null || lastRequest > 60000) {
//            this.migrateDBRequest = key;
//            this.migrateDBRequestDate = new Date();
//
//            throw new CommandException("This command is potentially dangerous.\n" + 
//                    "Please ensure you have made a backup of your data, and then re-enter the command exactly to procede.");
//        }
//
//        Class<? extends AbstractDatabaseMigrator> cls = migrators.get(key);
//
//        try {
//            AbstractDatabaseMigrator migrator = cls.getConstructor(StakeAClaimPlugin.class).newInstance(plugin);
//
//            migrator.migrate();
//        } catch (IllegalArgumentException ignore) {
//        } catch (SecurityException ignore) {
//        } catch (InstantiationException ignore) {
//        } catch (IllegalAccessException ignore) {
//        } catch (InvocationTargetException ignore) {
//        } catch (NoSuchMethodException ignore) {
//        } catch (MigrationException e) {
//            throw new CommandException("Error migrating database: " + e.getMessage());
//        }
//
//        sender.sendMessage(ChatColor.YELLOW + "Requests have been migrated successfully.\n" +
//                "If you wish to use the destination format as your new backend, please update your config and reload StakeAClaim.");
//    }

//    @Command(aliases = {"teleport", "tp"}, usage = "<id>", flags = "s",
//            desc = "Teleports you to the location associated with the request.", min = 1, max = 1)
//    public void teleport(CommandContext args, CommandSender sender) throws CommandException {
//        final Player player = plugin.checkPlayer(sender);
//
//        final RequestManager mgr = plugin.getGlobalRequestManager().get(player.getWorld());
//        String id = args.getString(0);
//
//        final StakeRequest request = mgr.getRequest(id);
//        if (request == null) {
//            if (!StakeRequest.isValidId(id)) {
//                throw new CommandException("Invalid request ID specified!");
//            }
//            throw new CommandException("A request with ID '" + id + "' doesn't exist.");
//        }
//
//        id = request.getId();
//
//        LocalPlayer localPlayer = plugin.wrapPlayer(player);
//        if (request.isOwner(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.teleport.own." + id.toLowerCase());
//        } else if (request.isMember(localPlayer)) {
//            plugin.checkPermission(sender, "stakeaclaim.request.teleport.member." + id.toLowerCase());
//        } else {
//            plugin.checkPermission(sender, "stakeaclaim.request.teleport." + id.toLowerCase());
//        }
//
//        final Location teleportLocation;
//        if (args.hasFlag('s')) {
//            teleportLocation = request.getFlag(DefaultFlag.SPAWN_LOC);
//            if (teleportLocation == null) {
//                throw new CommandException("The request has no spawn point associated.");
//            }
//        } else {
//            teleportLocation = request.getFlag(DefaultFlag.TELE_LOC);
//            if (teleportLocation == null) {
//                throw new CommandException("The request has no teleport point associated.");
//            }
//        }
//
//        player.teleport(BukkitUtil.toLocation(teleportLocation));
//
//        sender.sendMessage("Teleported you to the request '" + id + "'.");
//    }
}
