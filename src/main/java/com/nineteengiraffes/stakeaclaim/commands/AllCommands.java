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

import org.bukkit.command.CommandSender;

import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;

public class AllCommands {

    public AllCommands(StakeAClaimPlugin plugin) {}

    @Command(aliases = {"tools", "o", "t"},
        desc = "SAC tool commands")
    @NestedCommand(ToolsCommands.class)
    @CommandPermissions("stakeaclaim.tools")
    public void tools(CommandContext args, CommandSender sender) {}

    @Command(aliases = {"claim", "c"},
            desc = "SAC claim commands")
    @NestedCommand(ClaimCommands.class)
    @CommandPermissions("stakeaclaim.claim")
    public void claim(CommandContext args, CommandSender sender) {}

    @Command(aliases = {"sac"},
        desc = "SAC mangement commands")
    @NestedCommand(SACCommands.class)
    @CommandPermissions("stakeaclaim.sac")
    public void sac(CommandContext args, CommandSender sender) {}
}
