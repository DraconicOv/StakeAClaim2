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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
import java.util.List;
//import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.ChatColor;
//import org.bukkit.Location;
import org.bukkit.World;
//import org.bukkit.World.Environment;
//import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
//import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.SimpleInjector;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.sk89q.wepif.PermissionsResolverManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
//import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.util.FatalConfigurationLoadingException;

//import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.bukkit.commands.AllCommands;
import org.stakeaclaim.stakes.GlobalRequestManager;
import org.stakeaclaim.stakes.RequestManager;

/**
 * The main class for StakeAClaim as a Bukkit plugin.
 *
 * @author sk89q
 */
public class StakeAClaimPlugin extends JavaPlugin {

    /**
     * Manager for commands. This automatically handles nested commands,
     * permissions checking, and a number of other fancy command things.
     * We just set it up and register commands against it.
     */
    private final CommandsManager<CommandSender> commands;

    /**
     * Handles the request databases for all worlds.
     */
    private final GlobalRequestManager globalRequestManager;

    /**
     * Handles all configuration.
     */
    private final ConfigurationManager configuration;

    /**
     * Used for scheduling flags.
     */
    private FlagStateManager flagStateManager;

//    /**
//     * Used to avoid duplicate request IDs.
//     */
//    private long lastRequestID;

    /**
     * Construct objects. Actual loading occurs when the plugin is enabled, so
     * this merely instantiates the objects.
     */
    public StakeAClaimPlugin() {
        configuration = new ConfigurationManager(this);
        globalRequestManager = new GlobalRequestManager(this);

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
//    @SuppressWarnings("deprecation")
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
            globalRequestManager.preload();
        } catch (FatalConfigurationLoadingException e) {
            e.printStackTrace();
            getServer().shutdown();
        }

        flagStateManager = new FlagStateManager(this);

        if (configuration.useRequestsScheduler) {
            getServer().getScheduler().scheduleSyncRepeatingTask(this, flagStateManager,
                    FlagStateManager.RUN_DELAY, FlagStateManager.RUN_DELAY);
        }

        // Register events
        (new PlayerListener(this)).registerEvents();
        
//        lastRequestID = 0;
    }

    /**
     * Called on plugin disable.
     */
    @Override
    public void onDisable() {
        globalRequestManager.unload();
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
     * Get the GlobalRequestManager.
     *
     * @return The plugin's global request manager
     */
    public GlobalRequestManager getGlobalRequestManager() {
        return globalRequestManager;
    }

    /**
     * Gets the flag state manager.
     *
     * @return The flag state manager
     */
    public FlagStateManager getFlagStateManager() {
        return flagStateManager;
    }

    /**
     * Get the global ConfigurationManager.
     * USe this to access global configuration values and per-world configuration values.
     * @return The global ConfigurationManager
     */
    public ConfigurationManager getGlobalStateManager() {
        return configuration;
    }

//    /**
//     * Check whether a player is in a group.
//     * This calls the corresponding method in PermissionsResolverManager
//     *
//     * @param player The player to check
//     * @param group The group
//     * @return whether {@code player} is in {@code group}
//     */
//    public boolean inGroup(Player player, String group) {
//        try {
//            return PermissionsResolverManager.getInstance().inGroup(player, group);
//        } catch (Throwable t) {
//            t.printStackTrace();
//            return false;
//        }
//    }

//    /**
//     * Get the groups of a player.
//     * This calls the corresponding method in PermissionsResolverManager.
//     * @param player The player to check
//     * @return The names of each group the player is in.
//     */
//    public String[] getGroups(Player player) {
//        try {
//            return PermissionsResolverManager.getInstance().getGroups(player);
//        } catch (Throwable t) {
//            t.printStackTrace();
//            return new String[0];
//        }
//    }

//    /**
//     * Gets the name of a command sender. This is a unique name and this
//     * method should never return a "display name".
//     *
//     * @param sender The sender to get the name of
//     * @return The unique name of the sender.
//     */
//    public String toUniqueName(CommandSender sender) {
//        if (sender instanceof ConsoleCommandSender) {
//            return "*Console*";
//        } else {
//            return sender.getName();
//        }
//    }

//    /**
//     * Gets the name of a command sender. This may be a display name.
//     *
//     * @param sender The CommandSender to get the name of.
//     * @return The name of the given sender
//     */
//    public String toName(CommandSender sender) {
//        if (sender instanceof ConsoleCommandSender) {
//            return "*Console*";
//        } else if (sender instanceof Player) {
//            return ((Player) sender).getDisplayName();
//        } else {
//            return sender.getName();
//        }
//    }

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

//    /**
//     * Checks to see if the sender is a player, otherwise throw an exception.
//     *
//     * @param sender The {@link CommandSender} to check
//     * @return {@code sender} casted to a player
//     * @throws CommandException if {@code sender} isn't a {@link Player}
//     */
//    public Player checkPlayer(CommandSender sender)
//            throws CommandException {
//        if (sender instanceof Player) {
//            return (Player) sender;
//        } else {
//            throw new CommandException("A player is expected.");
//        }
//    }

    /**
     * Match a world.
     *
     * @param sender The sender requesting a match
     * @param filter The filter string
     * @return The resulting world
     * @throws CommandException if no world matches
     */
    public World matchWorld(CommandSender sender, String filter) throws CommandException {
        List<World> worlds = getServer().getWorlds();

        for (World world : worlds) {
            if (world.getName().equals(filter)) {
                return world;
            }
        }

        throw new CommandException("No world by that exact name found.");
    }

//    /**
//     * Gets a copy of the WorldGuard plugin.
//     *
//     * @return The WorldGuardPlugin instance
//     * @throws CommandException If there is no WorldGuardPlugin available
//     */
//    public WorldGuardPlugin getWorldGuard() throws CommandException {
//        Plugin worldGuard = getServer().getPluginManager().getPlugin("WorldGuard");
//        if (worldGuard == null) {
//            throw new CommandException("WorldGuard does not appear to be installed.");
//        }
//
//        if (worldGuard instanceof WorldGuardPlugin) {
//            return (WorldGuardPlugin) worldGuard;
//        } else {
//            throw new CommandException("WorldGuard detection failed (report error).");
//        }
//    }

    /**
     * Gets a copy of the WorldEdit plugin.
     *
     * @return The WorldEditPlugin instance
     * @throws CommandException If there is no WorldEditPlugin available
     */
    public WorldEditPlugin getWorldEdit() throws CommandException {
        Plugin worldEdit = getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            throw new CommandException("WorldEdit does not appear to be installed.");
        }

        if (worldEdit instanceof WorldEditPlugin) {
            return (WorldEditPlugin) worldEdit;
        } else {
            throw new CommandException("WorldEdit detection failed (report error).");
        }
    }

//    /**
//     * Wrap a player as a LocalPlayer.
//     *
//     * @param player The player to wrap
//     * @return The wrapped player
//     */
//    public LocalPlayer wrapPlayer(Player player) {
//        return new BukkitPlayer(this, player);
//    }

    /**
     * Create a default configuration file from the .jar.
     *
     * @param actual The destination file
     * @param defaultName The name of the file inside the jar's defaults folder
     */
    public void createDefaultConfiguration(File actual,
            String defaultName) {

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
        flagStateManager.forget(player);
    }

    /**
     * Gets the request manager for a world.
     *
     * @param world world to get the request manager for
     * @return the request manager or null if requests are not enabled
     */
    public RequestManager getRequestManager(World world) {
        if (!getGlobalStateManager().get(world).useRequests) {
            return null;
        }

        return getGlobalRequestManager().get(world);
    }

    /**
     * Replace macros in the text.
     *
     * The macros replaced are as follows:
     * %name%: The name of {@code sender}. See {@link #toName(org.bukkit.command.CommandSender)}
     * %id%: The unique name of the sender. See {@link #toUniqueName(org.bukkit.command.CommandSender)}
     * %online%: The number of players currently online on the server
     * If {@code sender} is a Player:
     * %world%: The name of the world {@code sender} is located in
     * %health%: The health of {@code sender}. See {@link org.bukkit.entity.Player#getHealth()}
     *
     * @param sender The sender to check
     * @param message The message to replace macros in
     * @return The message with macros replaced
     */
    public String replaceMacros(CommandSender sender, String message) {
        Player[] online = getServer().getOnlinePlayers();

//        message = message.replace("%name%", toName(sender));
//        message = message.replace("%id%", toUniqueName(sender));
        message = message.replace("%online%", String.valueOf(online.length));

        if (sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();

            message = message.replace("%world%", world.getName());
            message = message.replace("%health%", String.valueOf(player.getHealth()));
        }

        return message;
    }
}
