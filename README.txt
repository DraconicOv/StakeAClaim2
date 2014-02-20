StakeAClaim
Copyright (C) 2013 NineteenGiraffes <http://www.NineteenGiraffes.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.


About Stake A Claim

The StakeAClaim plugin requires WorldGuard and WorldEdit to function. SAC is an ads-on/extension to WorldGuard.
The primary goal of SAC is to allow players to request land claims from within the game, and for staff to manage these requests.
SAC has user friendly commands related to claiming, claims, and land all in one place, removing the need for any other claiming tool or complicated WorldGuard commands.
With only one base command for users, and shorter versions of all commands for the people that hate typing, SAC is the goto for user friendly claims in WorldGuard.


Features and commands:

In chat ‘support’ as you enter a claim when holding the SAC wand (default: feather)

/claim [c]
    me - show player summary [m]
    info - displays info about this claim [i]
    stake - stake your claim here [s]
    confirm - accept your own stake request [c]
    unstake - cancel your stake request) [u]
    add <players> - adds member(s) to this claim [a]
    remove <players> - removes member(s) from this claim, do -a to remove all [r]
    private - toggle this claim to private/open (people can’t enter) [p]
        default - toggle this claim's default entry state [d]
        clear - clear all privacy settings from claim [c]
    warp [player], [list item #] - warp to a player’s claim [w]
    set
        warp - set this claim's warp location [w]
        name <name> - set this claim's name [n]
    del
        warp - delete this claim's warp location [w]
        name - delete this claim's name [n]

/tools [t]
    pending - show a list of pending stake requests [p]
    accept <list item #> - accept someone's stake request [a]
    deny <list item #> - deny someone's stake request [d]
    cancel <list item #> - cancel someone's stake request [c]
    reclaim <list item #> - unclaim someone's claim > [r]
    proxy - claim for someone else [x]
    goto <list item #> or <region ID> ['spawn'] - go to a claim [g]
    spawns - create default spawn points for all claims in this world [s]

Hit a player with wand to lookup claims by that player
Hit a block with wand to lookup the claim the block is in
A claim can be set to automatically revert to the default entry state (private or open) when the owner logs off.


Install:
1) make sure you have WorldGuard and WorldEdit installed
2) copy StakeAClaim.jar to the plugins folder of your server
3) run the server to create config files
4) set up config and permissions
5) enjoy StakeAClaim on your server!!


Thanks to sk89q and WorldGuard for the use of WorldGuard code in the making of StakeAClaim.