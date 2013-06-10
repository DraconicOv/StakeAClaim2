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

package org.stakeaclaim.bukkit.commands;

import org.bukkit.command.CommandSender;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import org.stakeaclaim.bukkit.StakeAClaimPlugin;

public class AllCommands {
    @SuppressWarnings("unused")
    private final StakeAClaimPlugin plugin;

    public AllCommands(StakeAClaimPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(aliases = {"request", "requests", "rg"}, desc = "Request management commands")
    @NestedCommand({RequestCommands.class, RequestMemberCommands.class})
    public void request(CommandContext args, CommandSender sender) {}

//    @Command(aliases = {"worldguard", "wg"}, desc = "StakeAClaim commands")
//    @NestedCommand({StakeAClaimCommands.class})
//    public void worldGuard(CommandContext args, CommandSender sender) {}
    
    /* MCA add start */
    @Command(aliases = {"plot", "p"}, desc = "Plot commands")
    @NestedCommand({PlotCommands.class})
    public void plot(CommandContext args, CommandSender sender) {}
    
//    @Command(aliases = {"claim", "c"}, desc = "Claim tool commands")
//    @NestedCommand({ClaimCommands.class})
//    public void claim(CommandContext args, CommandSender sender) {}
    /* MCA add end */
}
