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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.ConfigManager;
import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.nineteengiraffes.stakeaclaim.util.SACUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SetCommands {
    private final StakeAClaimPlugin plugin;

    public SetCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    // Commands
    @Command(aliases = {"warp", "w"},
            usage = "",
            desc = "Set this claim's warp",
            min = 0, max = 0)
    @CommandPermissions({"stakeaclaim.claim.set.warp", "stakeaclaim.claim.set.warp.own.*", "stakeaclaim.claim.set.warp.member.*", "stakeaclaim.claim.set.warp.*"})
    public void warp(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        if (!wcfg.useRegions) {
            throw new CommandException(ChatColor.YELLOW + "Regions are disabled in this world.");
        }

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        SACUtil.checkPerm(plugin, player, "set.warp", claim);
        LocationFlag teleportFlag = (LocationFlag) WorldGuard.getInstance().getFlagRegistry().get("teleport");
        claim.setFlag(teleportFlag, BukkitAdapter.adapt(player.getLocation()));
        sender.sendMessage(SACUtil.formatID(sMgr.getStake(claim)) + ChatColor.YELLOW + "'s warp set.");

    }

    @Command(aliases = {"name", "n"},
            usage = "<name>",
            desc = "Set this claim's name",
            min = 1)
    @CommandPermissions({"stakeaclaim.claim.set.name", "stakeaclaim.claim.set.name.own.*", "stakeaclaim.claim.set.name.member.*", "stakeaclaim.claim.set.name.*"})
    public void name(CommandContext args, CommandSender sender) throws CommandException {

        final Player player = SACUtil.checkPlayer(sender);
        final World world = player.getWorld();

        final ConfigManager cfg = plugin.getGlobalManager();
        final WorldConfig wcfg = cfg.get(world);
        if (!wcfg.useStakes) {
            throw new CommandException(ChatColor.YELLOW + "Stakes are disabled in this world.");
        }

        final ProtectedRegion claim = SACUtil.getClaimStandingIn(player, plugin);
        SACUtil.checkPerm(plugin, player, "set.name", claim);

        final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
        Stake stake = sMgr.getStake(claim);

        stake.setClaimName(args.getJoinedStrings(0));
        sender.sendMessage(SACUtil.formatID(stake) + ChatColor.YELLOW + "'s name set to: " + ChatColor.LIGHT_PURPLE + args.getJoinedStrings(0));

        sMgr.save();
    }

}
