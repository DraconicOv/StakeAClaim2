// $Id$
/*
 * Copyright (C) 2013 NineteenGiraffes <http://www.NineteenGiraffes.com>
 * All rights reserved.
*/
package org.stakeaclaim.bukkit;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class StakeAClaimWorldListener implements Listener {

    private StakeAClaimPlugin plugin;

    /**
     * Construct the object;
     *
     * @param plugin The plugin instance
     */
    public StakeAClaimWorldListener(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register events.
     */
    public void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

//    @EventHandler
//    public void onChunkLoad(ChunkLoadEvent event) {
//        ConfigurationManager cfg = plugin.getGlobalStateManager();
//
//        if (cfg.activityHaltToggle) {
//            int removed = 0;
//
//            for (Entity entity : event.getChunk().getEntities()) {
//                if (BukkitUtil.isIntensiveEntity(entity)) {
//                    entity.remove();
//                    removed++;
//                }
//            }
//
//            if (removed > 50) {
//                plugin.getLogger().info("Halt-Act: " + removed + " entities (>50) auto-removed from "
//                        + event.getChunk().toString());
//            }
//        }
//    }

//    @EventHandler
//    public void onWorldLoad(WorldLoadEvent event) {
//        initWorld(event.getWorld());
//    }

//    /**
//     * Initialize the settings for the specified world
//     * @see WorldConfiguration#alwaysRaining
//     * @see WorldConfiguration#disableWeather
//     * @see WorldConfiguration#alwaysThundering
//     * @see WorldConfiguration#disableThunder
//     * @param world The specified world
//     */
//    public void initWorld(World world) {
//        ConfigurationManager cfg = plugin.getGlobalStateManager();
//        WorldConfiguration wcfg = cfg.get(world);
//        if (wcfg.alwaysRaining && !wcfg.disableWeather) {
//            world.setStorm(true);
//        } else if (wcfg.disableWeather && !wcfg.alwaysRaining) {
//            world.setStorm(false);
//        }
//        if (wcfg.alwaysThundering && !wcfg.disableThunder) {
//            world.setThundering(true);
//        } else if (wcfg.disableThunder && !wcfg.alwaysThundering) {
//            world.setStorm(false);
//        }
//    }
}
