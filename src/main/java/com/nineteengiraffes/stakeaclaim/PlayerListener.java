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
import java.util.Map;

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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WGBukkit;
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
                            boolean isVIP = false;
                            if (wcfg.useStakes) {
                                isVIP = plugin.getGlobalStakeManager().get(world).getStake(claim).getVIP();
                            }

                            // Display info for claim.
                            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Location: " + 
                                    (isVIP ? ChatColor.AQUA : ChatColor.WHITE) + claim.getId());
                            if (SACUtil.isRegionOwned(claim) <= 0) {
                                message.append(ChatColor.GRAY + " Unclaimed");
                            } else {
                                message.append(" " + ChatColor.GREEN + claim.getOwners().toUserFriendlyString());
                            }

                            support = message.toString();
                            if (support != null && (state.lastSupport == null
                                    || !state.lastSupport.equals(support))) {
                                player.sendMessage(support);
                            }
                        }
                    }

                    // save state
                    state.lastSupport = support;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getPlayerStateManager().getState(event.getPlayer()).unsubmittedStake = null;
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

                if (!wcfg.useStakes) {
                    activePlayer.sendMessage(ChatColor.YELLOW + "Stakes are disabled in this world.");
                    return;
                }

                final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    activePlayer.sendMessage(ChatColor.YELLOW + "Regions are disabled in this world.");
                    return;
                }

                final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                LinkedHashMap<Integer, String> regionList = new LinkedHashMap<Integer, String>();
                PlayerState state = plugin.getPlayerStateManager().getState(activePlayer);
                final Location loc = passivePlayer.getLocation();

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg, new Vector(loc.getX(), loc.getY(), loc.getZ()));
                state.unsubmittedStake = null;
                Integer index = 0;
                if (claim != null) {
                    Stake stake = sMgr.getStake(claim);

                    activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName() + ChatColor.YELLOW + " is in:");
                    regionList.put(index, claim.getId());
                    index++;
                    boolean open = SACUtil.displayClaim(index.toString(), claim, stake, activePlayer);

                    if (open) {
                        activePlayer.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.WHITE + "/tools proxy" + 
                                ChatColor.YELLOW + " to stake that claim for them.");
                        state.unsubmittedStake = new String[]{passivePlayer.getName().toLowerCase(), claim.getId()};
                    }
                }

                ArrayList<ProtectedRegion> regions = SACUtil.getOwnedClaims(rgMgr, wcfg, passivePlayer);

                Stake stake = SACUtil.getPendingStake(rgMgr, sMgr, passivePlayer);
                if (stake != null) {
                    regions.add(rgMgr.getRegion(stake.getId()));
                }

                if (regions.size() < 1) {
                    state.regionList = regionList;
                    state.listWorld = world;
                    activePlayer.sendMessage(ChatColor.GREEN + passivePlayer.getName() + ChatColor.YELLOW + " does not have any claims!");
                    return;
                }

                activePlayer.sendMessage(ChatColor.YELLOW + "Stake list:");
                for (ProtectedRegion region : regions) {
                    regionList.put(index, region.getId());
                    index++;
                    if (index < 10) {
                        SACUtil.displayClaim(index.toString(), region, sMgr.getStake(region), activePlayer);
                    }
                }
                if (index > 9) {
                    activePlayer.sendMessage(ChatColor.YELLOW + "Showing first 9 stakes of " + regions.size() + ". Do " + ChatColor.WHITE + "/tools list" + ChatColor.YELLOW + " to see full list.");
                }
                state.regionList = regionList;
                state.listWorld = world;
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

                if (!wcfg.useStakes) {
                    player.sendMessage(ChatColor.YELLOW + "Stakes are disabled in this world.");
                    return;
                }

                final RegionManager rgMgr = WGBukkit.getRegionManager(world);
                if (rgMgr == null) {
                    player.sendMessage(ChatColor.YELLOW + "Regions are disabled in this world.");
                    return;
                }

                final Location loc = block.getLocation();

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg, new Vector(loc.getX(), loc.getY(), loc.getZ()));
                if (claim == null) {
                    player.sendMessage(ChatColor.YELLOW + "That block is not in a claim!");
                    return;
                }
                final PlayerState state = plugin.getPlayerStateManager().getState(player);

                final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                Stake stake = sMgr.getStake(claim);

                player.sendMessage(ChatColor.YELLOW + "That block is in:");
                SACUtil.displayClaim("1", claim, stake, player);
                LinkedHashMap<Integer, String> regionList = new LinkedHashMap<Integer, String>();
                regionList.put(0, claim.getId());
                state.regionList = regionList;
                state.listWorld = world;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ConfigManager cfg = plugin.getGlobalManager();
        WorldConfig wcfg;
        StakeManager sMgr;

        for (World world : plugin.getServer().getWorlds()) {
            wcfg = cfg.get(world);
            if (!wcfg.useStakes || wcfg.silentNotify) {
                continue;
            }

            sMgr = plugin.getGlobalStakeManager().get(world);
            final Map<String, Stake> stakes = sMgr.getStakes();
            int pendingCount = 0;
            boolean save = false;

            for (Stake stake : stakes.values()) {
                if (stake.getStakeName() != null && stake.getStakeName().equals(player.getName().toLowerCase()) && stake.getStatus() != null) {
                    StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Your stake in " + (stake.getVIP() ? ChatColor.AQUA : ChatColor.WHITE) + stake.getId() + 
                            (player.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()));
                    switch (stake.getStatus()) {
                        case PENDING:
                            message.append(ChatColor.YELLOW + " is still pending.");
                            break;
                        case ACCEPTED:
                            message.append(ChatColor.YELLOW + " has been " + ChatColor.DARK_GREEN + "accepted!");
                            stake.setStatus(null);
                            stake.setStakeName(null);
                            save = true;
                            break;
                        case DENIED:
                            message.append(ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "denied!");
                            stake.setStatus(null);
                            stake.setStakeName(null);
                            save = true;
                            break;
                    }
                    player.sendMessage(message.toString());
                }
                if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
                    pendingCount++;
                }
            }

            if (plugin.hasPermission(player, "stakeaclaim.pending.notify")) {
                if (pendingCount == 1) {
                    player.sendMessage(ChatColor.YELLOW + "There is 1 pending stake" + 
                            (player.getWorld().equals(world) ? "." : " in " + ChatColor.BLUE + world.getName() + "."));
                } else if (pendingCount > 1) {
                    player.sendMessage(ChatColor.YELLOW + "There are " + pendingCount + " pending stakes" + 
                            (player.getWorld().equals(world) ? "." : " in " + ChatColor.BLUE + world.getName() + "."));
                }
            }

            if (save) sMgr.save();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getGlobalManager();

        if (plugin.hasPermission(player, "stakeaclaim.events.reset-entry")) {
            RegionManager rgMgr;
            StakeManager sMgr;
            WorldConfig wcfg;

            for (World world : plugin.getServer().getWorlds()) {
                wcfg = cfg.get(world);
                rgMgr = WGBukkit.getRegionManager(world);
                if (!wcfg.useStakes || rgMgr == null) {
                    continue;
                }

                sMgr = plugin.getGlobalStakeManager().get(world);
                SACUtil.resetEntryRegions(rgMgr, sMgr, player);
                
            }
        }

        plugin.forgetPlayer(player);
    }
}
