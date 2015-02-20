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

The StakeAClaim plugin requires WorldGuard and WorldEdit to function. SAC is an add-on/extension to WorldGuard.
The primary goal of SAC is to allow players to stake land claims from within the game, and for staff to manage these stakes.
SAC has user friendly commands related to claiming, claims, and land all in one place, removing the need for any other claiming tool or complicated WorldGuard commands.
With only one base command for users, and shorter versions of all commands for the people that hate typing, SAC is the goto for user friendly claims in WorldGuard.


Features and commands:

In chat ‘support’ as you enter a claim when holding the SAC wand (default: feather)

/claim [c]
    me - show player summary [m]
    info - displays info about this claim [i]
    stake - stake your claim here [s]
    confirm - accept your own stake [c]
    unstake - remove your stake [u]
    add <players> - adds member(s) to this claim [a]
    remove <players> - removes member(s) from this claim, do -a to remove all [r]
    private - toggle this claim to private/open (people can’t enter) [p]
        default - toggle this claim's default entry state [d]
        clear - clear all privacy settings from claim [c]
    warp [player], [list item #] - warp to a player’s claim [w]
    proxy - claim for someone else [x]
    set
        warp - set this claim's warp location [w]
        name <name> - set this claim's name [n]
    del
        warp - delete this claim's warp location [w]
        name - delete this claim's name [n]

/sac
    filters - display a list of all filters [f]
    search <filter(s)> - search claims using filter(s) [s]
    pending [page #] - quick search of pending claims [p]
    open [page #] - quick search of open claims [o]
    claim <list item #> or <claim id> [world] - view detailed info on one claim [c]
    user <list item #> or <player> [world] - view detailed info on one user [u]
    goto <list item #> or <claim id> [world] - go to a claim [g]
    spawn <list item #> or <claim id> [world] - go to a claim's spawn
    do - action commands on all or one item of the active claim list [d]
        accept <list item #> or <'all'> - accept stake(s) [a]
        deny <list item #> or <'all'> - deny stake(s) [d]
        reclaim <list item #> or <'all'> - reclaim/reset claim(s) [r]
        generate <list item #> or <'all'> - generate default spawn point for claim(s) [g]
        normal <list item #> or <'all'> - set claim(s) to anyone [n]
        vip <list item #> or <'all'> - set claim(s) to VIP only [v]
    write - [world] or ['all'] write stakes to file for world [w]
    load - [world] or ['all'] or ['config'] load stakes from file for world [l]

Hit a player with wand to lookup claims by that player
Hit a block with wand to lookup the claim the block is in
A claim can be set to automatically revert to the default entry state (private or open) when the owner logs off.


Install:
1) make sure you have WorldGuard and WorldEdit installed
2) copy StakeAClaim.jar to the plugins folder of your server
3) run the server to create config files
4) setup config and permissions
5) enjoy StakeAClaim on your server!!

You may view a full list of SAC permissions nodes at: <http://goo.gl/3q8TrP>

Sample config:
============================================================================================================

# Global only settings.
sac:

# Use the scheduler for tasks.
    use-scheduler: true

# Enable use of 'support' feature.
    use-player-move-event: true

# console output on startup.
summary-on-start: true

# SAC gives players with OP all SAC permissions.
op-permissions: true

# Enable SAC.
master-enable: true

# Enable featurs that use stakes.
stakes-enable: true

# Item (wand) to hold for 'support' and other features.
wand: FEATHER

# Regular expressions filter. Used to determine what regions are 'claims' by naming convention. Default: matches n12e3, S2W56, etc.
claim-name-regex-filter-string: ^[NSns]\d\d?[EWew]\d\d?$

# Mark claims 'reclaimed' when reclaimed.
remeber-reclaimed: true

# What you call your vip player.
what-you-call-your-vips: Donors

# Stake/Claim settings.
claiming:

# Disable player notifications for when they are added to claims and more.
    silent-claiming: false

# Players need to do '/claim confirm' for unassisted stakes.
    players-must-confirm-unassisted-stakes: true

# Use the 'max-volume' settings below as claiming limits. Default: false ('max-count' settings)
    use-volume-limits: false

# Count based limits.
    max-count:

# Number of stakes a player can place without staff approval. (-1 = no limit)
        unassisted-stakes: 1

# Number of stakes a player can place with staff approval. (-1 = no limit)
        total-stakes: 3

# Number of stakes someone else can place for a player. Bypasses the above. (-1 = no limit)
        proxy-can-stake: -1

# Volume based limits.
    max-volume:

# Total volume of stakes a player can place without staff approval, in blocks. (-1 = no limit)
        unassisted-stakes: 262144

# Total volume of stakes a player can place with staff approval, in blocks. (-1 = no limit)
        total-stakes: 1048576

# Total volume of stakes someone else can place for a player, in blocks. Bypasses the above. (-1 = no limit)
        proxy-can-stake: -1
============================================================================================================


Thanks to sk89q and WorldGuard for the use of WorldGuard code in the making of StakeAClaim.