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

package com.nineteengiraffes.stakeaclaim.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nineteengiraffes.stakeaclaim.StakeAClaimPlugin;
import com.nineteengiraffes.stakeaclaim.WorldConfig;
import com.nineteengiraffes.stakeaclaim.stakes.Stake.Status;
import com.nineteengiraffes.stakeaclaim.stakes.StakeManager;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class SearchUtil {

    // Filters
    /**
     * Filters {@code fullList} for claims as defined by regex filter in config
     * 
     * @param plugin the SAC plugin
     * @param world the world the list is for
     * @param fullList the list of regions to filter
     * @return {@code filtered} list
     */
    public static ArrayList<ProtectedRegion> filterForClaims(StakeAClaimPlugin plugin, World world, ArrayList<ProtectedRegion> fullList) {

            final WorldConfig wcfg = plugin.getGlobalManager().get(world);
            final Pattern regexPat = Pattern.compile(wcfg.claimNameFilter);
            Matcher regexMat;
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                regexMat = regexPat.matcher(claim.getId());
                if (regexMat.find()) {
                    filtered.add(claim);
                }
            }

        return filtered;
    }

    /**
     * Filters {@code fullList} with {@code id} filter
     * 
     * @param fullList the list of regions to filter
     * @param id filter
     * @return {@code filtered} list or {@code fullList} if {@code id} is null
     */
    public static ArrayList<ProtectedRegion> idFilter(ArrayList<ProtectedRegion> fullList, String id) {

        if (id != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (claim.getId().equalsIgnoreCase(id)) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }

    }

    /**
     * Filters {@code fullList} with {@code owner} filter
     * 
     * @param fullList the list of regions to filter
     * @param owner filter
     * @return {@code filtered} list or {@code fullList} if {@code owner} is null
     */
    public static ArrayList<ProtectedRegion> ownerFilter(ArrayList<ProtectedRegion> fullList, OfflinePlayer owner) {

        if (owner != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (claim.getOwners().contains(owner.getUniqueId()) || owner.getName() != null && 
                        claim.getOwners().contains(owner.getName().toLowerCase())) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code member} filter
     * 
     * @param fullList the list of regions to filter
     * @param member filter
     * @return {@code filtered} list or {@code fullList} if {@code member} is null
     */
    public static ArrayList<ProtectedRegion> memberFilter(ArrayList<ProtectedRegion> fullList, OfflinePlayer member) {

        if (member != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (claim.getMembers().contains(member.getUniqueId()) || member.getName() != null && 
                        claim.getMembers().contains(member.getName().toLowerCase())) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code pending} filter
     * 
     * @param plugin the SAC plugin
     * @param world the world the list is for
     * @param fullList the list of regions to filter
     * @param pending filter
     * @return {@code filtered} list or {@code fullList} if {@code pending} is null
     */
    public static ArrayList<ProtectedRegion> pendingFilter(StakeAClaimPlugin plugin, World world, ArrayList<ProtectedRegion> fullList, Boolean pending) {

        if (pending != null) {
            final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (sMgr.getStake(claim).getStatus() == Status.PENDING && pending) {
                    filtered.add(claim);
                } else if (sMgr.getStake(claim).getStatus() != Status.PENDING && !pending) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code hasMembers} filter
     * 
     * @param fullList the list of regions to filter
     * @param hasMembers filter
     * @return {@code filtered} list or {@code fullList} if {@code hasMembers} is null
     */
    public static ArrayList<ProtectedRegion> hasMembersFilter(ArrayList<ProtectedRegion> fullList, Boolean hasMembers) {

        if (hasMembers != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (claim.getMembers().size() > 0 && hasMembers) {
                    filtered.add(claim);
                } else if (claim.getMembers().size() <= 0 && !hasMembers) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code claimed} filter
     * 
     * @param fullList the list of regions to filter
     * @param claimed filter
     * @return {@code filtered} list or {@code fullList} if {@code claimed} is null
     */
    public static ArrayList<ProtectedRegion> claimedFilter(ArrayList<ProtectedRegion> fullList, Boolean claimed) {

        if (claimed != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (claim.getOwners().size() > 0 && claimed) {
                    filtered.add(claim);
                } else if (claim.getOwners().size() <= 0 && !claimed) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code vip} filter
     * 
     * @param plugin the SAC plugin
     * @param world the world the list is for
     * @param fullList the list of regions to filter
     * @param vip filter
     * @return {@code filtered} list or {@code fullList} if {@code vip} is null
     */
    public static ArrayList<ProtectedRegion> vipFilter(StakeAClaimPlugin plugin, World world, ArrayList<ProtectedRegion> fullList, Boolean vip) {

        if (vip != null) {
            final StakeManager sMgr = plugin.getGlobalStakeManager().get(world);
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                if (sMgr.getStake(claim).getVIP() == vip) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code absent} filter
     * 
     * @param plugin the SAC plugin
     * @param fullList the list of regions to filter
     * @param absent filter
     * @return {@code filtered} list or {@code fullList} if {@code absent} is null
     */
    public static ArrayList<ProtectedRegion> absentFilter(StakeAClaimPlugin plugin, ArrayList<ProtectedRegion> fullList, Long absent) {

        if (absent != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                List<Long> times = new ArrayList<Long>();
                for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                    Long online = SACUtil.getOfflinePlayer(plugin, oneOwner).getLastPlayed();
                    if (online > 0) {
                        times.add(online);
                    }
                }
                for (String oneOwner : claim.getOwners().getPlayers()) {
                    UUID uuid = SACUtil.uuidLookup(plugin, oneOwner);
                    if (uuid != null) {
                        Long online = SACUtil.getOfflinePlayer(plugin, uuid).getLastPlayed();
                        if (online > 0) {
                            times.add(online);
                        }
                    }
                }

                if (!times.isEmpty() && Collections.max(times) < absent) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code seen} filter
     * 
     * @param plugin the SAC plugin
     * @param fullList the list of regions to filter
     * @param seen filter
     * @return {@code filtered} list or {@code fullList} if {@code seen} is null
     */
    public static ArrayList<ProtectedRegion> seenFilter(StakeAClaimPlugin plugin, ArrayList<ProtectedRegion> fullList, Long seen) {

        if (seen != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                List<Long> times = new ArrayList<Long>();
                for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                    Long online = SACUtil.getOfflinePlayer(plugin, oneOwner).getLastPlayed();
                    if (online > 0) {
                        times.add(online);
                    }
                }
                for (String oneOwner : claim.getOwners().getPlayers()) {
                    UUID uuid = SACUtil.uuidLookup(plugin, oneOwner);
                    if (uuid != null) {
                        Long online = SACUtil.getOfflinePlayer(plugin, uuid).getLastPlayed();
                        if (online > 0) {
                            times.add(online);
                        }
                    }
                }

                if (!times.isEmpty() && Collections.max(times) > seen) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code typo} filter
     * 
     * @param plugin the SAC plugin
     * @param fullList the list of regions to filter
     * @param typo filter
     * @return {@code filtered} list or {@code fullList} if {@code typo} is null
     */
    public static ArrayList<ProtectedRegion> typoFilter(StakeAClaimPlugin plugin, ArrayList<ProtectedRegion> fullList, Boolean typo) {

        if (typo != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                boolean hasTypo = false;
                for (UUID oneMember : claim.getMembers().getUniqueIds()) {
                    if (!SACUtil.getOfflinePlayer(plugin, oneMember).hasPlayedBefore()) {
                        hasTypo = true;
                        break;
                    }
                }
                if (!hasTypo) {
                    for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                        if (!SACUtil.getOfflinePlayer(plugin, oneOwner).hasPlayedBefore()) {
                            hasTypo = true;
                            break;
                        }
                    }
                }
                if (!hasTypo) {
                    for (String oneMember : claim.getMembers().getPlayers()) {
                        UUID uuid = SACUtil.uuidLookup(plugin, oneMember);
                        if (uuid != null && SACUtil.getOfflinePlayer(plugin, uuid).hasPlayedBefore()) {
                            hasTypo = true;
                            break;
                        }
                    }
                }
                if (!hasTypo) {
                    for (String oneOwner : claim.getOwners().getPlayers()) {
                        UUID uuid = SACUtil.uuidLookup(plugin, oneOwner);
                        if (uuid != null && SACUtil.getOfflinePlayer(plugin, uuid).hasPlayedBefore()) {
                            hasTypo = true;
                            break;
                        }
                    }
                }

                if (hasTypo == typo) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} with {@code banned} filter
     * 
     * @param plugin the SAC plugin
     * @param fullList the list of regions to filter
     * @param banned filter
     * @return {@code filtered} list or {@code fullList} if {@code banned} is null
     */
    public static ArrayList<ProtectedRegion> bannedFilter(StakeAClaimPlugin plugin, ArrayList<ProtectedRegion> fullList, Boolean banned) {

        if (banned != null) {
            ArrayList<ProtectedRegion> filtered = new ArrayList<ProtectedRegion>();
            for (ProtectedRegion claim : fullList) {
                boolean isBanned = false;
                for (UUID oneMember : claim.getMembers().getUniqueIds()) {
                    if (SACUtil.getOfflinePlayer(plugin, oneMember).isBanned()) {
                        isBanned = true;
                        break;
                    }
                }
                if (!isBanned) {
                    for (UUID oneOwner : claim.getOwners().getUniqueIds()) {
                        if (SACUtil.getOfflinePlayer(plugin, oneOwner).isBanned()) {
                            isBanned = true;
                            break;
                        }
                    }
                }
                if (!isBanned) {
                    for (String oneMember : claim.getMembers().getPlayers()) {
                        UUID uuid = SACUtil.uuidLookup(plugin, oneMember);
                        if (uuid != null && SACUtil.getOfflinePlayer(plugin, uuid).isBanned()) {
                            isBanned = true;
                            break;
                        }
                    }
                }
                if (!isBanned) {
                    for (String oneOwner : claim.getOwners().getPlayers()) {
                        UUID uuid = SACUtil.uuidLookup(plugin, oneOwner);
                        if (uuid != null && SACUtil.getOfflinePlayer(plugin, uuid).isBanned()) {
                            isBanned = true;
                            break;
                        }
                    }
                }

                if (isBanned == banned) {
                    filtered.add(claim);
                }
            }
            return filtered;
        } else {
            return fullList;
        }
    }

    /**
     * Filters {@code fullList} for regions {@code player} can warp to
     * 
     * @param fullList the list of regions to filter
     * @param player the player that can warp
     * @return filtered list
     */
    public static ArrayList<ProtectedRegion> canWarpTo(StakeAClaimPlugin plugin, ArrayList<ProtectedRegion> fullList, Player player) {
        StateFlag entryFlag = (StateFlag)WorldGuard.getInstance().getFlagRegistry().get("entry");
        for (ProtectedRegion claim : fullList) {
            if (claim.getFlag(entryFlag) != null && claim.getFlag(entryFlag) == State.DENY) {
                fullList.remove(claim);
            } else if (!SACUtil.hasPerm(plugin, player, "warp", claim)) {
                fullList.remove(claim);
            }
        }

        return fullList;
    }

    // Convert list to map
    /**
     * Joins {@code fullList} with {@code joinList} as a map
     * 
     * @param fullList list of regions
     * @param joinList map of regions
     * @return joined lists as a map
     */
    public static LinkedHashMap<Integer, ProtectedRegion> joinLists(ArrayList<ProtectedRegion> fullList, LinkedHashMap<Integer, ProtectedRegion> joinList) {

            LinkedHashMap<Integer, ProtectedRegion> regionList = new LinkedHashMap<Integer, ProtectedRegion>();
            if (joinList != null) {
                regionList = joinList;
            }

            int index = regionList.size();
            for (ProtectedRegion region : fullList) {
                if (!regionList.containsValue(region)) {
                    regionList.put(index, region);
                    index++;
                }
            }

            return regionList;
    }


}
