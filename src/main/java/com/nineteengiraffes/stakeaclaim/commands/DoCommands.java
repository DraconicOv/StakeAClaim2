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

package com.nineteengiraffes.stakeaclaim.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.PlayerStateManager.PlayerState;
import com.nineteengiraffes.stakeaclaim.SACUtil;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

    enum Action {
        ACCEPT,
        DENY,
        RECLAIM,
        GENERATE,
        NORMAL,
        VIP
    }

public class DoCommands {
    private final StakeAClaimPlugin plugin;

    public DoCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // Action commands
    @Command(aliases = {"accept", "a"},
            usage = "<list item #>",
            desc = "Accept a pending stake",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.accept")
    public void accept(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.ACCEPT);
    }

    @Command(aliases = {"deny", "d"},
            usage = "<list item #>",
            desc = "Deny a pending stake",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.deny")
    public void deny(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.DENY);
    }

    @Command(aliases = {"reclaim", "r"},
            usage = "<list item #>",
            desc = "Reclaim/reset a claim",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.reclaim")
    public void reclaim(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.RECLAIM);
    }

    @Command(aliases = {"generate", "gen", "spawn", "g"},
            usage = "<list item #>",
            desc = "Generate spawnpoint for a claim",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.generate")
    public void generate(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.GENERATE);
    }

    @Command(aliases = {"normal", "n"},
            usage = "<list item #>",
            desc = "Set a claim to anyone",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.normal")
    public void normal(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.NORMAL);
    }

    @Command(aliases = {"vip", "v"},
            usage = "<list item #>",
            desc = "Set a claim to VIP only",
            min = 1, max = 1)
    @CommandPermissions("stakeaclaim.sac.do.vip")
    public void vip(CommandContext args, CommandSender sender) throws CommandException {
        doAction(args, sender, Action.VIP);
    }


    // Do the action
    private void doAction(CommandContext args, CommandSender sender, Action action) throws CommandException {

        final PlayerState state = plugin.getPlayerStateManager().getState(sender);
        LinkedHashMap<Integer, ProtectedRegion> fullList;
        LinkedHashMap<Integer, ProtectedRegion> tempList = new LinkedHashMap<Integer, ProtectedRegion>();
        World world;
        if (state.regionList != null && !state.regionList.isEmpty() && state.listWorld != null) {
             world = state.listWorld;
             fullList = state.regionList;
        } else {
            throw new CommandException(ChatColor.YELLOW + "The claim list is empty.");
        }

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }
        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        String all = args.getString(0);
        if (!all.equalsIgnoreCase("all") && !all.equalsIgnoreCase("a")) {
            int item;
            try {
                item = args.getInteger(0) - 1;
            } catch (NumberFormatException e) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + all + ChatColor.YELLOW + "' is not a valid number.");
            }
            if (!fullList.containsKey(item)) {
                throw new CommandException(ChatColor.YELLOW + "'" + ChatColor.WHITE + all + ChatColor.YELLOW + "' is not a valid list item number.");
            }
            tempList.put(0, fullList.get(item));
        } else {
            if (SACUtil.hasPermission(plugin, sender, "stakeaclaim.sac.do.bulk")) {
                tempList = fullList;
            } else {
                throw new CommandPermissionsException();
            }
        }

        for (ProtectedRegion claim : tempList.values()) {
            Stake stake = sMgr.getStake(claim);

            switch (action) {
            case RECLAIM:
                reclaimClaim(sender, claim, stake, world, wcfg);
                continue;
            case GENERATE:
                generateClaimSpawn(sender, claim, stake, world);
                continue;
            case NORMAL:
                setNormalClaim(sender, claim, stake);
                continue;
            case VIP:
                setVIPClaim(sender, claim, stake, wcfg);
                continue;
            default:
                break;
            }

            if (stake.getStatus() != null && stake.getStatus() == Status.PENDING && stake.getStakeName() != null) {
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Sorry, " + SACUtil.formatID(stake) + 
                        ChatColor.YELLOW + " is not pending.");
                continue;
            }
            int ownedCode = SACUtil.isRegionOwned(claim);
            if (ownedCode > 0) {
                stake.setStatus(null);
                stake.setStakeName(null);
                sender.sendMessage(ChatColor.YELLOW + "Sorry, " + SACUtil.formatID(stake) + 
                        ChatColor.YELLOW + " is not open.");
                continue;
            }

            switch (action) {
            case ACCEPT:
                acceptClaim(sender, claim, stake, world, wcfg);
                continue;
            case DENY:
                denyClaim(sender, claim, stake, world, wcfg);
                continue;
            default:
                break;
            }
        }

        sMgr.save();
        SACUtil.saveRegions(world);
    }


    // Actions
    private void acceptClaim(CommandSender sender, ProtectedRegion claim, Stake stake, World world, WorldConfig wcfg) {
        claim.getOwners().addPlayer(stake.getStakeName());

        sender.sendMessage(ChatColor.YELLOW + "You have accepted " + SACUtil.formatPlayer(sender, stake.getStakeName()) +
                ChatColor.YELLOW + "'s stake in " + SACUtil.formatID(stake) + 
                ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName() + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.ACCEPTED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + SACUtil.formatID(stake) + 
                            (stakeHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " has been " + ChatColor.DARK_GREEN + "accepted!");
                    stake.setStatus(null);
                    stake.setStakeName(null);
                }
            }
        }
    }

    private void denyClaim(CommandSender sender, ProtectedRegion claim, Stake stake, World world, WorldConfig wcfg) {
        sender.sendMessage(ChatColor.YELLOW + "You have denied " + SACUtil.formatPlayer(sender, stake.getStakeName()) +
                ChatColor.YELLOW + "'s stake in " + SACUtil.formatID(stake) + 
                        ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName() + "!");

        if (wcfg.silentNotify) {
            stake.setStatus(null);
            stake.setStakeName(null);
        } else {
            stake.setStatus(Status.DENIED);
            for (Player stakeHolder : plugin.getServer().getOnlinePlayers()) {
                if (stakeHolder.getName().equalsIgnoreCase(stake.getStakeName())) {
                    stakeHolder.sendMessage(ChatColor.YELLOW + "Your stake in " + SACUtil.formatID(stake) + 
                            (stakeHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) +
                            ChatColor.YELLOW + " has been " + ChatColor.DARK_RED + "denied!");
                    stake.setStatus(null);
                    stake.setStakeName(null);
                }
            }
        }
    }

    private void reclaimClaim(CommandSender sender, ProtectedRegion claim, Stake stake, World world, WorldConfig wcfg) {
            StringBuilder message = new StringBuilder(ChatColor.YELLOW + "You have reclaimed " + SACUtil.formatID(stake) +
                    ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName());
            if (claim.getOwners().getPlayers().size() > 0) {
                message.append(ChatColor.YELLOW + " from");
                for (String oneOwner : claim.getOwners().getPlayers()) {
                    message.append(" " + SACUtil.formatPlayer(sender, oneOwner));
                }
            }
            sender.sendMessage(message.toString() + ChatColor.YELLOW + "!");

        if (!wcfg.silentNotify) {
            for (Player claimHolder : plugin.getServer().getOnlinePlayers()) {
                if (claim.getOwners().contains(claimHolder.getName())) {
                    claimHolder.sendMessage(ChatColor.YELLOW + "You no longer own " + SACUtil.formatID(stake) + 
                            (claimHolder.getWorld().equals(world) ? "" : ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName()) + 
                            ChatColor.YELLOW + ", it has been " + ChatColor.DARK_RED + "reclaimed!");
                }
            }
        }

        claim.getOwners().getPlayers().clear();
        claim.getOwners().getGroups().clear();
        claim.getMembers().getPlayers().clear();
        claim.getMembers().getGroups().clear();
        claim.setFlag(DefaultFlag.TELE_LOC,null);
        claim.setFlag(DefaultFlag.ENTRY,null);
        stake.setStatus(null);
        stake.setStakeName(null);
        stake.setDefaultEntry(null);
        stake.setClaimName(null);
        stake.setRecalimed(wcfg.useReclaimed);
    }

    private void generateClaimSpawn(CommandSender sender, ProtectedRegion claim, Stake stake, World world){
        Vector center = BlockVector.getMidpoint(claim.getMaximumPoint(),claim.getMinimumPoint());
        int x = center.getBlockX();
        int y;
        int z = center.getBlockZ();
        int[] offset = {0, 1, -1, 2, -2};
        for (int xOffset : offset) {
            for (int zOffset : offset) {
                y = world.getHighestBlockYAt(x + xOffset, z + zOffset);
                if (y != 0) {
                    claim.setFlag(DefaultFlag.SPAWN_LOC, new Location(BukkitUtil.getLocalWorld(world), new Vector(x + xOffset + .5, y, z + zOffset + .5)));
                    sender.sendMessage(ChatColor.YELLOW + "Generated spawnpoint for: " + SACUtil.formatID(stake));
                    return;
                }
            }
        }
        sender.sendMessage(ChatColor.RED + "Spawnpoint could not be generated for: " + SACUtil.formatID(stake));
    }

    private void setNormalClaim(CommandSender sender, ProtectedRegion claim, Stake stake) {
        stake.setVIP(false);
        sender.sendMessage(ChatColor.WHITE + claim.getId() + ChatColor.YELLOW + " set to anyone.");
    }

    private void setVIPClaim(CommandSender sender, ProtectedRegion claim, Stake stake, WorldConfig wcfg) {
        stake.setVIP(true);
        sender.sendMessage(ChatColor.AQUA + claim.getId() + ChatColor.YELLOW + " set to " + wcfg.VIPs + " only.");
    }

}
