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

package com.nineteengiraffes.stakeaclaim.bukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.nineteengiraffes.stakeaclaim.bukkit.commands.AllCommands;
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
     * Handles all configuration.
     */
    private final ConfigurationManager configuration;

    /**
     * Used for scheduling flags.
     */
    private PlayerStateManager playerStateManager;

    /**
     * Construct objects. Actual loading occurs when the plugin is enabled, so
     * this merely instantiates the objects.
     */
    public StakeAClaimPlugin() {
        configuration = new ConfigurationManager(this);

        final StakeAClaimPlugin plugin = this;
        commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender player, String perm) {
                return plugin.hasPermission(player, perm);
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
            // Load the configuration
            configuration.load();
        } catch (FatalConfigurationLoadingException e) {
            e.printStackTrace();
            getServer().shutdown();
        }

        playerStateManager = new PlayerStateManager(this);

        if (configuration.useRequestsScheduler) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, playerStateManager,
                    PlayerStateManager.RUN_DELAY, PlayerStateManager.RUN_DELAY);
        }

        // Register events
        (new PlayerListener(this)).registerEvents();

        // Add in custom flags
        SACUtil.addFlags();
    }

    /**
     * Called on plugin disable.
     */
    @Override
    public void onDisable() {
        configuration.unload();
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
     * Get the global ConfigurationManager.
     * USe this to access global configuration values and per-world configuration values.
     * @return The global ConfigurationManager
     */
    public ConfigurationManager getGlobalStateManager() {
        return configuration;
    }

    /**
     * Checks permissions.
     *
     * @param sender The sender to check the permission on.
     * @param perm The permission to check the permission on.
     * @return whether {@code sender} has {@code perm}
     */
    public boolean hasPermission(CommandSender sender, String perm) {
        if (sender.isOp()) {
            if (sender instanceof Player) {
                if (this.getGlobalStateManager().get(((Player) sender).getWorld()).opPermissions) {
                    return true;
                }
            } else {
                return true;
            }
        }

        // Invoke the permissions resolver
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return PermissionsResolverManager.getInstance().hasPermission(player.getWorld().getName(), player.getName(), perm);
        }

        return false;
    }

    /**
     * Checks permissions and throws an exception if permission is not met.
     *
     * @param sender The sender to check the permission on.
     * @param perm The permission to check the permission on.
     * @throws CommandPermissionsException if {@code sender} doesn't have {@code perm}
     */
    public void checkPermission(CommandSender sender, String perm)
            throws CommandPermissionsException {
        if (!hasPermission(sender, perm)) {
            throw new CommandPermissionsException();
        }
    }

    /**
     * Checks to see if the sender is a player, otherwise throw an exception.
     *
     * @param sender The {@link CommandSender} to check
     * @return {@code sender} casted to a player
     * @throws CommandException if {@code sender} isn't a {@link Player}
     */
    public Player checkPlayer(CommandSender sender)
            throws CommandException {
        if (sender instanceof Player) {
            return (Player) sender;
        } else {
            throw new CommandException("A player is expected.");
        }
    }

    /**
     * Create a default configuration file from the .jar.
     *
     * @param actual The destination file
     * @param defaultName The name of the file inside the jar's defaults folder
     */
    @SuppressWarnings("resource")
    public void createDefaultConfiguration(File actual, String defaultName) {

        // Make parent directories
        File parent = actual.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        if (actual.exists()) {
            return;
        }

        InputStream input =
                    null;
            try {
                JarFile file = new JarFile(getFile());
                ZipEntry copy = file.getEntry("defaults/" + defaultName);
                if (copy == null) throw new FileNotFoundException();
                input = file.getInputStream(copy);
            } catch (IOException e) {
                getLogger().severe("Unable to read default configuration: " + defaultName);
            }

        if (input != null) {
            FileOutputStream output = null;

            try {
                output = new FileOutputStream(actual);
                byte[] buf = new byte[8192];
                int length = 0;
                while ((length = input.read(buf)) > 0) {
                    output.write(buf, 0, length);
                }

                getLogger().info("Default configuration file written: "
                        + actual.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignore) {
                }

                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }
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
