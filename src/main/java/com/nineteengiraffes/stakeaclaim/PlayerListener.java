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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
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

import com.nineteengiraffes.stakeaclaim.PlayerListener.PlayerMoveHandler;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.nineteengiraffes.stakeaclaim.util.SACUtil;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.BukkitWorldGuardPlatform;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
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
                    final ItemStack item = player.getInventory().getItemInMainHand();

                    if (item.getType() == wcfg.sacWand && SACUtil.hasPermission(plugin, player, "stakeaclaim.events.wand")) {

                        // Get a single valid claim.
                        final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                        if (rgMgr == null) {
                            return;
                        }

                        final ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg,
                                 BlockVector3.at(event.getTo().getBlockX(), event.getTo().getBlockY(), event.getTo().getBlockZ()));

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
                                for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                                    message.append(" " + SACUtil.formatPlayer(SACUtil.getOfflinePlayer(plugin, oneOwner)));
                                }

// remove for loop when names get removed entirely
                                for (String oneOwner : claim.getOwners().getPlayers()) {
                                    message.append(" " + SACUtil.formatPlayer(oneOwner));
                                }

                            }

                            String support = message.toString();
                            if (support != null && (state.lastSupport == null
                                    || !state.lastSupport.equals(support))) {
                                player.sendMessage(support);
                            }
                            state.lastSupport = support;
                        }
                    } else {
                        state.lastSupport = null;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getPlayerStateManager().getState(event.getPlayer()).unsubmittedStake = null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity thingClicked = event.getRightClicked();
        Player activePlayer = event.getPlayer();

        if (thingClicked instanceof Player) {
            Player passivePlayer = (Player) thingClicked;

            World world = passivePlayer.getWorld();

            ConfigManager cfg = plugin.getGlobalManager();
            WorldConfig wcfg = cfg.get(world);

            ItemStack held = activePlayer.getInventory().getItemInMainHand();
            PlayerState state = plugin.getPlayerStateManager().getState(activePlayer);

            if (held.getType() == wcfg.sacWand && SACUtil.hasPermission(plugin, activePlayer, "stakeaclaim.events.wand.player")) {

                if (!wcfg.useStakes) {
                    return;
                }

                final RegionManager rgMgr = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (rgMgr == null) {
                    return;
                }

                final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                final Block block = passivePlayer.getLocation().getBlock();
                if (state.lastPlayer != null && state.lastPlayer.equals(passivePlayer)) {
                    if (state.lastBlock != null && state.lastBlock.equals(block)) {
                        return;
                    }
                }

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg,  BlockVector3.at(block.getX(), block.getY(), block.getZ()));
                state.unsubmittedStake = null;
                if (claim != null) {
                    Stake stake = sMgr.getStake(claim);

                    activePlayer.sendMessage(SACUtil.formatPlayer(passivePlayer) + ChatColor.YELLOW + " is in:");
                    boolean open = SACUtil.displayClaim("", claim, stake, activePlayer, plugin, world);

                    if (open && SACUtil.hasPermission(plugin, activePlayer, "stakeaclaim.claim.proxy")) {
                        activePlayer.sendMessage(ChatColor.YELLOW + "Do " + ChatColor.WHITE + "/claim proxy" + 
                                ChatColor.YELLOW + " to stake that claim for them.");
                        state.unsubmittedStake = new Stake(claim.getId());
                        state.unsubmittedStake.setStakeUUID(passivePlayer.getUniqueId());
                    }
                }

                SACUtil.displayPlayer(plugin, activePlayer, rgMgr, world, passivePlayer);
                state.lastPlayer = passivePlayer ;
                state.lastBlock = block;

                event.setCancelled(true);
            } else {
                state.lastPlayer = null;
                state.lastBlock = null;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            World world = block.getWorld();
            Player player = event.getPlayer();
            PlayerState state = plugin.getPlayerStateManager().getState(player);

            if (state.lastBlock != null && state.lastBlock.equals(block)) {
                return;
            }

            ItemStack held = player.getInventory().getItemInMainHand();
            ConfigManager cfg = plugin.getGlobalManager();
            WorldConfig wcfg = cfg.get(world);

            if (held.getType() == wcfg.sacWand && SACUtil.hasPermission(plugin, player, "stakeaclaim.events.wand.claim")) {

                state.lastBlock = block;
                if (!wcfg.useStakes) {
                    return;
                }

                final RegionManager rgMgr =  WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (rgMgr == null) {
                    return;
                }

                ProtectedRegion claim = SACUtil.getClaimAtPoint(rgMgr, wcfg, BlockVector3.at(block.getX(), block.getY(), block.getZ()));
                if (claim == null) {
                    player.sendMessage(ChatColor.WHITE + block.getType().toString() + ChatColor.YELLOW + " at " + ChatColor.GOLD + "(" + block.getX() + "," + block.getY() + "," + block.getZ() + ")" + ChatColor.YELLOW + " is not in a claim!");
                    return;
                }

                final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
                Stake stake = sMgr.getStake(claim);

                player.sendMessage(ChatColor.WHITE + block.getType().toString() + ChatColor.YELLOW + " at " + ChatColor.GOLD + "(" + block.getX() + "," + block.getY() + "," + block.getZ() + ")" + ChatColor.YELLOW + " is in:");
                SACUtil.displayClaim(wcfg, claim, stake, player, plugin, world, null);
                LinkedHashMap<Integer, ProtectedRegion> regionList = new LinkedHashMap<Integer, ProtectedRegion>();
                regionList.put(0, claim);
                state.regionList = regionList;
                state.listWorld = world;

                event.setCancelled(true);
            } else {
                state.lastBlock = null;
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
                if (stake.getStakeUUID() != null && stake.getStakeUUID() == player.getUniqueId() && stake.getStatus() != null) {
                    StringBuilder message = new StringBuilder(ChatColor.YELLOW + "Your stake in " + SACUtil.formatID(stake) + 
                            (player.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()));
                    switch (stake.getStatus()) {
                        case PENDING:
                            message.append(ChatColor.YELLOW + " is still pending.");
                            break;
                        case ACCEPTED:
                            message.append(ChatColor.YELLOW + " has been " + ChatColor.DARK_GREEN + "accepted!");
                            stake.setStatus(null);
                            stake.setStakeUUID(null);
                            save = true;
                            break;
                        case DENIED:
                            message.append(ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "denied!");
                            stake.setStatus(null);
                            stake.setStakeUUID(null);
                            save = true;
                            break;
                    }
                    player.sendMessage(message.toString());
                }
                if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeUUID() != null) {
                    pendingCount++;
                }
            }

            if (SACUtil.hasPermission(plugin, player, "stakeaclaim.pending.notify") && pendingCount > 0) {
                if (SACUtil.hasPermission(plugin, player, "stakeaclaim.sac.search")) {
                    StringBuilder message = new StringBuilder("tellraw " + player.getName() + " {text:'There ");
                    if (pendingCount == 1) {
                        message.append("is 1 pending stake");
                    } else {
                        message.append("are " + pendingCount + " pending stakes");
                    }
                    if (!player.getWorld().equals(world)) {
                        message.append("',extra:[' in',{text:' " + world.getName() + "',color:blue},'.']");
                    } else {
                        message.append(".'");
                    }
                    message.append(",clickEvent:{action:run_command,value:'/sac search pending world " + world.getName() + "'},color:yellow");
                    message.append("}");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message.toString());
                } else {
                    if (pendingCount == 1) {
                        player.sendMessage(ChatColor.YELLOW + "There is 1 pending stake" + 
                                (player.getWorld().equals(world) ? "." : " in " + ChatColor.BLUE + world.getName() + "."));
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "There are " + pendingCount + " pending stakes" + 
                                (player.getWorld().equals(world) ? "." : " in " + ChatColor.BLUE + world.getName() + "."));
                    }
                }
            }

            if (save) sMgr.save();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ConfigManager cfg = plugin.getGlobalManager();

        if (SACUtil.hasPermission(plugin, player, "stakeaclaim.events.reset-entry")) {
            RegionManager rgMgr;
            StakeManager sMgr;
            WorldConfig wcfg;

            for (World world : plugin.getServer().getWorlds()) {
                wcfg = cfg.get(world);
                rgMgr =  WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (!wcfg.useStakes || rgMgr == null) {
                    continue;
                }

                sMgr = plugin.getGlobalStakeManager().get(world);
                final Map<String, Stake> stakes = sMgr.getStakes();
                ProtectedRegion region;
                for (Stake stake : stakes.values()) {
                    if (stake.getDefaultEntry() != null) {
                        region = rgMgr.getRegion(stake.getId());
                        StateFlag entryFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get("entry");
                        if (region.getOwners().contains(player.getUniqueId()) || region.getOwners().contains(player.getName().toLowerCase())) {
                            if (region.getFlag(entryFlag) != stake.getDefaultEntry()) {
                                region.setFlag(entryFlag, stake.getDefaultEntry());
                            }
                        }
                    }
                }
            }
        }

        plugin.forgetPlayer(player);
    }
}
