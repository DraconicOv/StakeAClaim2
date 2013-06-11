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

//import static org.stakeaclaim.bukkit.BukkitUtil.toVector;

//import java.util.Iterator;
//import java.util.Map; /* MCA add */
//import java.util.Set;
//import java.util.regex.Pattern;

//import org.bukkit.ChatColor;
//import org.bukkit.GameMode;
import org.bukkit.Location;
//import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
//import org.bukkit.block.BlockFace;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
//import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
//import org.bukkit.event.block.Action;
//import org.bukkit.event.player.AsyncPlayerChatEvent;
//import org.bukkit.event.player.PlayerBedEnterEvent;
//import org.bukkit.event.player.PlayerBucketEmptyEvent;
//import org.bukkit.event.player.PlayerBucketFillEvent;
//import org.bukkit.event.player.PlayerCommandPreprocessEvent;
//import org.bukkit.event.player.PlayerDropItemEvent;
//import org.bukkit.event.player.PlayerFishEvent;
//import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
//import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
//import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
//import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
//import org.bukkit.potion.Potion;
//import org.bukkit.potion.PotionEffect;

import com.sk89q.worldedit.Vector;
//import com.sk89q.worldedit.blocks.BlockID;
//import com.sk89q.worldedit.blocks.BlockType;
//import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;

//import org.stakeaclaim.LocalPlayer;
//import org.stakeaclaim.blacklist.events.BlockBreakBlacklistEvent;
//import org.stakeaclaim.blacklist.events.BlockInteractBlacklistEvent;
//import org.stakeaclaim.blacklist.events.BlockPlaceBlacklistEvent;
//import org.stakeaclaim.blacklist.events.ItemAcquireBlacklistEvent;
//import org.stakeaclaim.blacklist.events.ItemDropBlacklistEvent;
//import org.stakeaclaim.blacklist.events.ItemUseBlacklistEvent;
import org.stakeaclaim.bukkit.FlagStateManager.PlayerFlagState;
//import org.stakeaclaim.domains.DefaultDomain; /* MCA add */
//import org.stakeaclaim.stakes.ApplicableRequestSet;
//import org.stakeaclaim.stakes.flags.DefaultFlag;
//import org.stakeaclaim.stakes.RequestManager;
//import org.stakeaclaim.stakes.StakeRequest;

/**
 * Handles all events thrown in relation to a player.
 */
public class PlayerListener implements Listener {

//    private Pattern opPattern = Pattern.compile("^/op(?:\\s.*)?$", Pattern.CASE_INSENSITIVE);
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

            if (player.getVehicle() != null) {
                return; // handled in vehicle listener
            }
            if (wcfg.useRequests) {
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

                    RegionManager mgr = WGBukkit.getRegionManager(world);
                    Vector pt = new Vector(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());
                    ApplicableRegionSet set = mgr.getApplicableRegions(pt);

                    // If wand is in hand, displays claim name and owner(s) as you enter
                    final ItemStack item = player.getItemInHand();
                    String support = null;

                    if (item.getTypeId() == wcfg.requestWand && plugin.hasPermission(player, "stakeaclaim.request.wand")) {

//                        final StakeRequest claim = set.getClaim();
//                        if (claim != null) {
//                            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + claim.getId());
//                            final DefaultDomain owners = claim.getOwners();
//                            if (owners.size() == 0) {
//                                message.append(ChatColor.GRAY + " Unclaimed");
//                            } else {
//                                message.append(" " + ChatColor.GREEN + owners.toUserFriendlyString());
//                            }
//
//                            support = message.toString();
//                            if (support != null && (state.lastSupport == null
//                                    || !state.lastSupport.equals(support))) {
//                                player.sendMessage(support);
//                            }
//                        }
                    }

                    state.lastWorld = event.getTo().getWorld();
                    state.lastBlockX = event.getTo().getBlockX();
                    state.lastBlockY = event.getTo().getBlockY();
                    state.lastBlockZ = event.getTo().getBlockZ();
                    state.lastSupport = support;
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

        if (wcfg.useRequests) {
            PlayerFlagState state = plugin.getFlagStateManager().getState(player);
            Location loc = player.getLocation();
            state.lastWorld = loc.getWorld();
            state.lastBlockX = loc.getBlockX();
            state.lastBlockY = loc.getBlockY();
            state.lastBlockZ = loc.getBlockZ();
            state.lastSupport = null;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//        World world = player.getWorld();
//
//        /* MCA add start */
//        // auto removes keep-out flag(s), if player has perms to do so
//        
//        if (plugin.hasPermission(player, "stakeaclaim.plot.public")) {
//            final LocalPlayer localPlayer2 = plugin.wrapPlayer(player);
//            final RequestManager mgr2 = plugin.getGlobalRequestManager().get(world);
//            final Map<Long, StakeRequest> requests = mgr2.getRequests();
//
//            for (StakeRequest claim : requests.values()) {
//                if (claim.isOwner(localPlayer2)) {
//                    claim.setFlag(DefaultFlag.ENTRY, null);
//                }
//            }
//        }
//        /* MCA add end */
//
//        ConfigurationManager cfg = plugin.getGlobalStateManager();
//        WorldConfiguration wcfg = cfg.get(world);
//
//        // This is to make the enter/exit flags accurate -- move events are not
//        // sent constantly, so it is possible to move just a little enough to
//        // not trigger the event and then rejoin so that you are then considered
//        // outside the border. This should work around that.
//        if (wcfg.useRequests) {
//            boolean hasBypass = plugin.getGlobalRequestManager().hasBypass(player, world);
//            PlayerFlagState state = plugin.getFlagStateManager().getState(player);
//
//            if (state.lastWorld != null && !hasBypass) {
//                LocalPlayer localPlayer = plugin.wrapPlayer(player);
//                RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//                Location loc = player.getLocation();
//                Vector pt = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
//                ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//
//                if (state.lastExitAllowed == null) {
//                    state.lastExitAllowed = set.allows(DefaultFlag.EXIT, localPlayer);
//                }
//
//                if (!state.lastExitAllowed || !set.allows(DefaultFlag.ENTRY, localPlayer)) {
//                    // Only if we have the last location cached
//                    if (state.lastWorld.equals(world)) {
//                        Location newLoc = new Location(world, state.lastBlockX + 0.5,
//                                state.lastBlockY, state.lastBlockZ + 0.5);
//                        player.teleport(newLoc);
//                    }
//                }
//            }
//        }
//
//        //cfg.forgetPlayer(plugin.wrapPlayer(player));
//        plugin.forgetPlayer(player);
    }

    /**
     * Called when a player right clicks a block.
     *
     * @param event Thrown event
     */
    private void handleBlockRightClick(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getClickedBlock();
        World world = block.getWorld();
        int type = block.getTypeId();
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        ConfigurationManager cfg = plugin.getGlobalStateManager();
        WorldConfiguration wcfg = cfg.get(world);

//        if (wcfg.useRequests) {
//            Vector pt = toVector(block);
//            RequestManager mgr = plugin.getGlobalRequestManager().get(world);
//            Block placedIn = block.getRelative(event.getBlockFace());
//            ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//            ApplicableRequestSet placedInSet = mgr.getApplicableRequests(placedIn.getLocation());
//            LocalPlayer localPlayer = plugin.wrapPlayer(player);
//
//            if (item.getTypeId() == wcfg.requestWand && plugin.hasPermission(player, "stakeaclaim.request.wand")) {
//                if (set.size() > 0) {
//                    player.sendMessage(ChatColor.YELLOW + "Can you build? "
//                            + (set.canBuild(localPlayer) ? "Yes" : "No"));
//
//                    StringBuilder str = new StringBuilder();
//                    for (Iterator<StakeRequest> it = set.iterator(); it.hasNext();) {
//                        str.append(it.next().getId());
//                        if (it.hasNext()) {
//                            str.append(", ");
//                        }
//                    }
//
//                    player.sendMessage(ChatColor.YELLOW + "Applicable requests: " + str.toString());
//                } else {
//                    player.sendMessage(ChatColor.YELLOW + "StakeAClaim: No defined requests here!");
//                }
//
//                event.setCancelled(true);
//                return;
//            }
//        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
//        Player player = event.getPlayer();
//        Location location = player.getLocation();
//
//        ConfigurationManager cfg = plugin.getGlobalStateManager();
//        WorldConfiguration wcfg = cfg.get(player.getWorld());
//
//        if (wcfg.useRequests) {
//            Vector pt = toVector(location);
//            RequestManager mgr = plugin.getGlobalRequestManager().get(player.getWorld());
//            ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//
//            LocalPlayer localPlayer = plugin.wrapPlayer(player);
//            com.sk89q.worldedit.Location spawn = set.getFlag(DefaultFlag.SPAWN_LOC, localPlayer);
//
//            if (spawn != null) {
//                event.setRespawnLocation(com.sk89q.worldedit.bukkit.BukkitUtil.toLocation(spawn));
//            }
//        }
    }


    @EventHandler(priority= EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
//        World world = event.getFrom().getWorld();
//        ConfigurationManager cfg = plugin.getGlobalStateManager();
//        WorldConfiguration wcfg = cfg.get(world);
//
//        if (wcfg.useRequests) {
//            if (event.getCause() == TeleportCause.ENDER_PEARL) {
//                RequestManager mgr = plugin.getGlobalRequestManager().get(event.getFrom().getWorld());
//                Vector pt = new Vector(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ());
//                Vector ptFrom = new Vector(event.getFrom().getBlockX(), event.getFrom().getBlockY(), event.getFrom().getBlockZ());
//                ApplicableRequestSet set = mgr.getApplicableRequests(pt);
//                ApplicableRequestSet setFrom = mgr.getApplicableRequests(ptFrom);
//                LocalPlayer localPlayer = plugin.wrapPlayer(event.getPlayer());
//
//                if (!plugin.getGlobalRequestManager().hasBypass(localPlayer, world)
//                        && !(set.allows(DefaultFlag.ENTRY, localPlayer)
//                                && setFrom.allows(DefaultFlag.EXIT, localPlayer))) {
//                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You're not allowed to go there.");
//                    event.setCancelled(true);
//                    return;
//                }
//                if (!plugin.getGlobalRequestManager().hasBypass(localPlayer, world)
//                        && !(set.allows(DefaultFlag.ENDERPEARL, localPlayer)
//                                && setFrom.allows(DefaultFlag.ENDERPEARL, localPlayer))) {
//                    event.getPlayer().sendMessage(ChatColor.DARK_RED + "You're not allowed to go there.");
//                    event.setCancelled(true);
//                    return;
//                }
//            }
//        }
    }
}
