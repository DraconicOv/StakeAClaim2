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
package com.nineteengiraffes.stakeaclaim;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.SACFlags.Status;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

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

        if (plugin.getGlobalManager().usePlayerMove) {
            pm.registerEvents(new PlayerMoveHandler(), plugin);
        }
    }

    class PlayerMoveHandler implements Listener {
        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.HIGH)
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            World world = player.getWorld();

            ConfigManager cfg = plugin.getGlobalManager();
            WorldConfig wcfg = cfg.get(world);

            if (wcfg.useRegions && wcfg.useSAC) {
                // Did we move a block?
                if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                        || event.getFrom().getBlockY() != event.getTo().getBlockY()
                        || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    PlayerState state = plugin.getPlayerStateManager().getState(player);

                    //Flush states in multiworld scenario
                    if (state.lastWorld != null && !state.lastWorld.equals(world)) {
                        plugin.getPlayerStateManager().forget(player);
                        state = plugin.getPlayerStateManager().getState(player);
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

                        final ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg,
                                new Vector(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ()));

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
                        state.regionList = null;
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

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Entity thingClicked = event.getRightClicked();
        Player activePlayer = event.getPlayer();

        if (thingClicked instanceof Player) {
            Player passivePlayer = (Player) thingClicked;
            World world = passivePlayer.getWorld();

            ConfigManager cfg = plugin.getGlobalManager();
            WorldConfig wcfg = cfg.get(world);

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

                final Location loc = passivePlayer.getLocation();
                final PlayerState state = plugin.getPlayerStateManager().getState(activePlayer);

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg, new Vector(loc.getX(), loc.getY(), loc.getZ()));

                if (claim != null) {
                    final String regionID = claim.getId();
                    activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName().toLowerCase() + 
                            ChatColor.YELLOW + " is in " + ChatColor.WHITE + regionID + ".");
                    activePlayer.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.WHITE + "/tools proxy" + 
                            ChatColor.YELLOW + " to stake that claim for them.");
                    state.unsubmittedRequest = new String[]{passivePlayer.getName().toLowerCase(),regionID};
                } else {
                    state.unsubmittedRequest = null;
                }

                LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();
                state.regionList = null;

                ArrayList<ProtectedRegion> regionList = SACUtil.getPendingRegions(rgMgr, passivePlayer);
                int index = 0;
                for (ProtectedRegion region : regionList) {
                    regions.put(index, region.getId());
                    index++;
                }

                regionList = SACUtil.getOwnedRegions(rgMgr, passivePlayer);
                for (ProtectedRegion region : regionList) {
                    regions.put(index, region.getId());
                    index++;
                }

                final int totalSize = regions.size();
                if (totalSize < 1) {
                    state.regionList = null;
                    activePlayer.sendMessage(ChatColor.YELLOW + "This player has no requests.");
                    return;
                }
                state.regionList = regions;

                // Display the list
                activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName().toLowerCase() +
                        ChatColor.RED + "'s request list:");

                for (int i = 0; i < totalSize; i++) {
                    claim = rgMgr.getRegion(regions.get(i));
                    activePlayer.sendMessage(ChatColor.YELLOW + "# " + (i + 1) + ": " + ChatColor.WHITE + claim.getId() +
                            ", " + ChatColor.GREEN + passivePlayer.getName().toLowerCase() +
                            ", " + ChatColor.YELLOW + claim.getFlag(SACFlags.PENDING) != null ? "Pending" : "Accepted");
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            World world = block.getWorld();
            Player player = event.getPlayer();
            ItemStack held = player.getItemInHand();
            ConfigManager cfg = plugin.getGlobalManager();
            WorldConfig wcfg = cfg.get(world);

            if (held.getTypeId() == wcfg.sacWand && plugin.hasPermission(player, "stakeaclaim.events.wand.claim")) {

                if (!wcfg.useRequests) {
                    player.sendMessage(ChatColor.YELLOW + "Requests are disabled in this world.");
                    return;
                }

                final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    player.sendMessage(ChatColor.YELLOW + "Regions are disabled in this world.");
                    return;
                }

                final Location loc = block.getLocation();

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg, new Vector(loc.getX(), loc.getY(), loc.getZ()));
                final String regionID = claim.getId();
                final PlayerState state = plugin.getPlayerStateManager().getState(player);

                LinkedHashMap<Integer, String> regions = new LinkedHashMap<Integer, String>();
                state.regionList = null;
                regions.put(0, regionID);
                state.regionList = regions;

                int ownedCode = SACUtil.isRegionOwned(claim);
                if (ownedCode <= 0) {
                    claim.getMembers().getPlayers().clear();
                    claim.getMembers().getGroups().clear();
                    if (claim.getFlag(SACFlags.PENDING) != null && claim.getFlag(SACFlags.PENDING) == true && claim.getFlag(SACFlags.REQUEST_NAME) != null) {
                        player.sendMessage(ChatColor.RED + "Pending request: ");
                        player.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                                ", " + ChatColor.GREEN + claim.getFlag(SACFlags.REQUEST_NAME));
                    } else {
                        claim.setFlag(SACFlags.REQUEST_NAME,null);
                        claim.setFlag(SACFlags.REQUEST_STATUS,null);
                        claim.setFlag(SACFlags.PENDING,null);
                        claim.setFlag(SACFlags.ENTRY_DEFAULT,null);
                        claim.setFlag(DefaultFlag.ENTRY,null);
                        player.sendMessage(ChatColor.RED + "Open claim: ");
                        player.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                                ", " + ChatColor.GRAY + "Unclaimed");
                    }
                } else if (ownedCode == 1) {
                    claim.setFlag(SACFlags.PENDING,null);
                    player.sendMessage(ChatColor.RED + "Owned claim: ");
                    player.sendMessage(ChatColor.YELLOW + "# 1: " + ChatColor.WHITE + regionID +
                            ", " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());
                } else {
                    player.sendMessage(ChatColor.RED + "Claim error: " + ChatColor.WHITE + 
                              claim.getId() + ChatColor.RED + " has multiple owners!");
                }

                try {
                    rgMgr.save();
                } catch (ProtectionDatabaseException e) {
                    player.sendMessage("Failed to write regions: " + e.getMessage());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World playersWorld = player.getWorld();

        ConfigManager cfg = plugin.getGlobalManager();
        WorldConfig wcfg = cfg.get(playersWorld);

        if (wcfg.useSAC) {
            PlayerState state = plugin.getPlayerStateManager().getState(player);
            Location loc = player.getLocation();
            state.lastWorld = loc.getWorld();
            state.lastBlockX = loc.getBlockX();
            state.lastBlockY = loc.getBlockY();
            state.lastBlockZ = loc.getBlockZ();
            state.lastSupport = null;
            state.regionList = null;
            state.unsubmittedRequest = null;
        }

        RegionManager rgMgr;

        for (World world : plugin.getServer().getWorlds()) {
            wcfg = cfg.get(world);
            rgMgr = WGBukkit.getRegionManager(world);
            if (!wcfg.useSAC || rgMgr == null) {
                continue;
            }

            ArrayList<ProtectedRegion> regionList = SACUtil.getStatusRegions(rgMgr, player);
            for (ProtectedRegion region : regionList) {
                Status status = region.getFlag(SACFlags.REQUEST_STATUS);
                StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Your request for " + ChatColor.WHITE + region.getId() + ChatColor.YELLOW + " in " + 
                        ChatColor.BLUE + world.getName() + ChatColor.YELLOW);
                switch (status) {
                    case PENDING:
                        message.append(" is still pending.");
                        break;
                    case ACCEPTED:
                        message.append(" has been " + ChatColor.DARK_GREEN + "accepted!");
                        region.setFlag(SACFlags.REQUEST_NAME,null);
                        region.setFlag(SACFlags.REQUEST_STATUS,null);
                        break;
                    case DENIED:
                        message.append(" has been " + ChatColor.DARK_RED + "denied!");
                        region.setFlag(SACFlags.REQUEST_NAME,null);
                        region.setFlag(SACFlags.REQUEST_STATUS,null);
                        break;
                    case CANCELED:
                        message.append(" has been " + ChatColor.GRAY + "canceled.");
                        region.setFlag(SACFlags.REQUEST_NAME,null);
                        region.setFlag(SACFlags.REQUEST_STATUS,null);
                        break;
                }
                player.sendMessage(message.toString());
            }

            if (plugin.hasPermission(player, "stakeaclaim.pending.notify")) {
                regionList = SACUtil.getStatusPendingRegions(rgMgr);
                if (regionList.size() == 1) {
                    player.sendMessage(ChatColor.YELLOW + "There is " + regionList.size() + " pending stake request in " + 
                            ChatColor.BLUE + world.getName() + ".");
                } else if (regionList.size() > 1) {
                    player.sendMessage(ChatColor.YELLOW + "There are " + regionList.size() + " pending stake requests in " + 
                            ChatColor.BLUE + world.getName() + ".");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getGlobalManager();

        if (plugin.hasPermission(player, "stakeaclaim.events.reset-entry")) {
            RegionManager rgMgr;
            WorldConfig wcfg;

            for (World world : plugin.getServer().getWorlds()) {
                wcfg = cfg.get(world);
                rgMgr = WGBukkit.getRegionManager(world);
                if (!wcfg.useSAC || rgMgr == null) {
                    continue;
                }

                SACUtil.resetEntryRegions(rgMgr, player);
            }
        }

        plugin.forgetPlayer(player);
    }
}
