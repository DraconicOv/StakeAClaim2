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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.stakeaclaim.bukkit.SACUtil;
import org.stakeaclaim.bukkit.FlagStateManager.PlayerFlagState;
import org.stakeaclaim.stakes.RequestManager;
import org.stakeaclaim.stakes.StakeRequest;
import org.stakeaclaim.stakes.StakeRequest.Access;
import org.stakeaclaim.stakes.StakeRequest.Status;
import org.stakeaclaim.stakes.databases.StakeDatabaseException;

/**
 * Handles all events thrown in relation to a player.
 */
public class PlayerListener implements Listener {

    private StakeAClaimPlugin plugin;

    /**
     * Construct the object;
     *
     * @param plugin
     */
    public PlayerListener(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register events.
     */
    public void registerEvents() {
        final PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(this, plugin);

        if (plugin.getGlobalStateManager().usePlayerMove) {
            pm.registerEvents(new PlayerMoveHandler(), plugin);
        }
    }

    class PlayerMoveHandler implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            World world = player.getWorld();

            ConfigurationManager cfg = plugin.getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(world);

            if (wcfg.useRegions && wcfg.useSAC) {
                // Did we move a block?
                if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                        || event.getFrom().getBlockY() != event.getTo().getBlockY()
                        || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    PlayerFlagState state = plugin.getFlagStateManager().getState(player);

                    //Flush states in multiworld scenario
                    if (state.lastWorld != null && !state.lastWorld.equals(world)) {
                        plugin.getFlagStateManager().forget(player);
                        state = plugin.getFlagStateManager().getState(player);
                    }

                    // If wand is in hand, displays claim name and owner(s) as you enter
                    final ItemStack item = player.getItemInHand();
                    String support = null;

                    if (item.getTypeId() == wcfg.sacWand && plugin.hasPermission(player, "stakeaclaim.events.wand")) {

                        // Get a single valid claim.
                        final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                        if (rgMgr == null) {
                            return;
                        }

                        final Vector pt = new Vector(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());
                        final ApplicableRegionSet rgSet = rgMgr.getApplicableRegions(pt);
                        final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
                        Matcher regexMat;
                        ProtectedRegion claim = null;

                        for (ProtectedRegion region : rgSet) {
                            regexMat = regexPat.matcher(region.getId());
                            if (regexMat.find()) {
                                if (claim == null) {
                                    claim = region;
                                } else {
                                    claim = null;
                                    break;
                                }
                            }
                        }

                        if (claim != null) {
                            // Display info for claim.
                            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + claim.getId());
                            final DefaultDomain owners = claim.getOwners();
                            if (owners.size() == 0) {
                                message.append(ChatColor.GRAY + " Unclaimed");
                            } else {
                                message.append(" " + ChatColor.GREEN + owners.toUserFriendlyString());
                            }

                            support = message.toString();
                            if (support != null && (state.lastSupport == null
                                    || !state.lastSupport.equals(support))) {
                                player.sendMessage(support);
                            }
                        }
                    }

                    // save state
                    if (state.lastWorld != event.getTo().getWorld()) {
                        state.requestList = null;
                        state.unsubmittedRequest = null;
                        state.lastWorld = event.getTo().getWorld();
                    }
                    state.lastBlockX = event.getTo().getBlockX();
                    state.lastBlockY = event.getTo().getBlockY();
                    state.lastBlockZ = event.getTo().getBlockZ();
                    state.lastSupport = support;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Entity thingClicked = event.getRightClicked();
        Player activePlayer = event.getPlayer();

        if (thingClicked instanceof Player) {
            Player passivePlayer = (Player) thingClicked;
            World world = passivePlayer.getWorld();

            ConfigurationManager cfg = plugin.getGlobalStateManager();
            WorldConfiguration wcfg = cfg.get(world);

            ItemStack held = activePlayer.getItemInHand();

            if (held.getTypeId() == wcfg.sacWand && plugin.hasPermission(activePlayer, "stakeaclaim.events.wand.player")) {

                if (!wcfg.useRequests) {
                    activePlayer.sendMessage(ChatColor.YELLOW + "Requests are disabled in this world.");
                    return;
                }

                final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    activePlayer.sendMessage(ChatColor.YELLOW + "Regions are disabled in this world.");
                    return;
                }

                final RequestManager rqMgr = plugin.getGlobalRequestManager().get(world);
                final PlayerFlagState state = plugin.getFlagStateManager().getState(activePlayer);
                final Location loc = passivePlayer.getLocation();
                final Vector pt = new Vector(loc.getX(), loc.getY(), loc.getZ());
                final ApplicableRegionSet rgSet = rgMgr.getApplicableRegions(pt);
                final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
                Matcher regexMat;

                ProtectedRegion claim = null;
                for (ProtectedRegion region : rgSet) {
                    regexMat = regexPat.matcher(region.getId());
                    if (regexMat.find()) {
                        if (claim == null) {
                            claim = region;
                        } else {
                            claim = null;
                            break;
                        }
                    }
                }

                if (claim != null) {
                    final String regionID = claim.getId();
                    activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName().toLowerCase() + 
                            ChatColor.YELLOW + " is in " + ChatColor.WHITE + regionID + ".");
                    activePlayer.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.WHITE + "/tools proxy" + 
                            ChatColor.YELLOW + " to stake that claim for them.");
                    state.unsubmittedRequest = new StakeRequest(regionID, passivePlayer);
                    state.unsubmittedRequest.setStatus(Status.PENDING);
                } else {
                    state.unsubmittedRequest = null;
                }

                LinkedHashMap<Integer, Long> requests = new LinkedHashMap<Integer, Long>();
                state.requestList = null;

                final StakeRequest pendingRequest = SACUtil.getPlayerPendingRequest(rqMgr, passivePlayer);
                final ArrayList<StakeRequest> requestList = SACUtil.getAcceptedRequests(rqMgr, rgMgr, passivePlayer, wcfg.useReclaimed);
                try {
                    rqMgr.save();
                } catch (StakeDatabaseException e) {
                    activePlayer.sendMessage(ChatColor.RED + "Failed to write requests: " + e.getMessage());
                    return;
                }

                int index = 0;
                if (pendingRequest != null) {
                    requests.put(index, pendingRequest.getRequestID());
                    index = 1;
                }

                for (StakeRequest request : requestList) {
                    requests.put(index, request.getRequestID());
                    index++;
                }

                final int totalSize = requests.size();
                if (totalSize < 1) {
                    state.requestList = null;
                    activePlayer.sendMessage(ChatColor.YELLOW + "This player has no requests.");
                    return;
                }
                state.requestList = requests;

                // Display the list
                activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName().toLowerCase() +
                        ChatColor.RED + "'s request list:");

                StakeRequest request;
                for (int i = 0; i < totalSize; i++) {
                    request = rqMgr.getRequest(requests.get(i));
                    activePlayer.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + request.getRegionID() +
                            ", " + ChatColor.GREEN + request.getPlayerName() +
                            ", " + ChatColor.YELLOW + request.getStatus().name().toLowerCase());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

        if (wcfg.useSAC) {
            PlayerFlagState state = plugin.getFlagStateManager().getState(player);
            Location loc = player.getLocation();
            state.lastWorld = loc.getWorld();
            state.lastBlockX = loc.getBlockX();
            state.lastBlockY = loc.getBlockY();
            state.lastBlockZ = loc.getBlockZ();
            state.lastSupport = null;
            state.requestList = null;
            state.unsubmittedRequest = null;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.hasPermission(player, "stakeaclaim.events.access")) {
            RequestManager rqMgr;
            ArrayList<StakeRequest> requestList;
            RegionManager rgMgr;
            ProtectedRegion claim;

            for (World world : plugin.getServer().getWorlds()) {
                rqMgr = plugin.getGlobalRequestManager().get(world);
                requestList = rqMgr.getPlayerAccessRequests(player);

                rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    continue;
                }

                for (StakeRequest request : requestList) {
                    claim = rgMgr.getRegion(request.getRegionID());
                    if (request.getAccess() == Access.ALLOW) {
                        claim.setFlag(DefaultFlag.ENTRY, State.ALLOW);
                    } else if (request.getAccess() == Access.DENY) {
                        claim.setFlag(DefaultFlag.ENTRY, State.DENY);
                    }
                }
            }
        }

        plugin.forgetPlayer(player);
    }
}
