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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.nineteengiraffes.stakeaclaim.commands.AllCommands;
import com.nineteengiraffes.stakeaclaim.stakes.GlobalStakeManager;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldguard.util.FatalConfigurationLoadingException;

/**
 * The main class for StakeAClaim as a Bukkit plugin.
 */
public class StakeAClaimPlugin extends JavaPlugin {

    /**
     * Manager for commands. This automatically handles nested commands,
     * permissions checking, and a number of other fancy command things.
     * We just set it up and register commands against it.
     */
    private final CommandsManager<CommandSender> commands;

    /**
     * Handles all config.
     */
    private final ConfigManager config;

    /**
     * Handles all stakes.
     */
    private final GlobalStakeManager globalStakes;

    /**
     * Used for scheduling flags.
     */
    private PlayerStateManager playerStateManager;

    /**
     * Construct objects. Actual loading occurs when the plugin is enabled, so
     * this merely instantiates the objects.
     */
    public StakeAClaimPlugin() {
        config = new ConfigManager(this);
        globalStakes = new GlobalStakeManager(this);

        final StakeAClaimPlugin plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return SACUtil.hasPermission(plugin, player, perm);
            }
        };
    }

    /**
     * Called on plugin enable.
     */
    @Override
    public void onEnable() {

        // Set the proper command injector
        commands.setInjector(new SimpleInjector(this));

        // Register command class
        final CommandsManagerRegistration reg = new CommandsManagerRegistration(this, commands);
        reg.register(AllCommands.class);

        // Need to create the plugins/StakeAClaim folder
        getDataFolder().mkdirs();

        PermissionsResolverManager.initialize(this);

        try {
            // Load the config
            config.load();
            globalStakes.load();
        } catch (FatalConfigurationLoadingException e) {
            e.printStackTrace();
            getServer().shutdown();
        }

        playerStateManager = new PlayerStateManager(this);

        if (config.useStakeScheduler) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, playerStateManager,
                    PlayerStateManager.RUN_DELAY, PlayerStateManager.RUN_DELAY);
        }

        // Register events
        (new PlayerListener(this)).registerEvents();

    }

    /**
     * Called on plugin disable.
     */
    @Override
    public void onDisable() {
        globalStakes.unload();
        config.unload();
        this.getServer().getScheduler().cancelTasks(this);
    }

    /**
     * Handle a command.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
            String[] args) {
        try {
            commands.execute(cmd.getName(), args, sender, sender);
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }

        return true;
    }

    /**
     * Gets the player state manager.
     *
     * @return The player state manager
     */
    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    /**
     * Get the global ConfigManager.
     * Use this to access global config values and per-world config values.
     * @return The global ConfigManager
     */
    public ConfigManager getGlobalManager() {
        return config;
    }

    /**
     * Get the global stake manager.
     * Use this to access all stakes.
     * @return The global stake manager
     */
    public GlobalStakeManager getGlobalStakeManager() {
        return globalStakes;
    }

    /**
     * Forgets a player.
     *
     * @param player The player to remove state information for
     */
    public void forgetPlayer(Player player) {
        playerStateManager.forget(player);
    }
}
