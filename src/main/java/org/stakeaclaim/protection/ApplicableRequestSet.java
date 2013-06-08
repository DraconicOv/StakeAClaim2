// $Id$
/*
 * StakeAClaim
 * Copyright (C) 2010 sk89q <http://www.sk89q.com>
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
package org.stakeaclaim.protection;

import org.stakeaclaim.LocalPlayer;
import org.stakeaclaim.protection.flags.*;
import org.stakeaclaim.protection.flags.StateFlag.State;
import org.stakeaclaim.protection.requests.ProtectedRequest;

import java.util.*;

/**
 * Represents a set of requests for a particular point or area and the rules
 * that are represented by that set. An instance of this can be used to
 * query the value of a flag or check if a player can build in the respective
 * request or point. This object contains the list of applicable requests and so
 * the expensive search of requests that are in the desired area has already
 * been completed.
 * 
 * @author sk89q
 */
public class ApplicableRequestSet implements Iterable<ProtectedRequest> {

    private Collection<ProtectedRequest> applicable;
    private ProtectedRequest globalRequest;

    /**
     * Construct the object.
     * 
     * @param applicable The requests contained in this set
     * @param globalRequest The global request, set aside for special handling.
     */
    public ApplicableRequestSet(Collection<ProtectedRequest> applicable,
            ProtectedRequest globalRequest) {
        this.applicable = applicable;
        this.globalRequest = globalRequest;
    }

    /* MCA add start */
    /**
     * Gets the request that is a claim.
     * 
     * @return the request that is a claim, if there are more or less than 1, null.
     */
    public ProtectedRequest getClaim() {
        ProtectedRequest claim = null;
        ProtectedRequest parent = null;
        for (ProtectedRequest request : this) {
            parent = request.getParent();
            if (parent != null) {
                if (!parent.getId().equals("world")) {
                    if (claim == null) {
                        claim = request;
                    } else {
                        return null;
                    }
                }
            }
        }
        return claim;
    }
    /* MCA add end */

    /**
     * Checks if a player can build in an area.
     * 
     * @param player The player to chec
     * @return build ability
     */
    public boolean canBuild(LocalPlayer player) {
        return internalGetState(DefaultFlag.BUILD, player, null);
    }

    public boolean canConstruct(LocalPlayer player) {
        final RequestGroup flag = getFlag(DefaultFlag.CONSTRUCT, player);
        return RequestGroupFlag.isMember(this, flag, player);
    }

    /**
     * Checks if a player can use buttons and such in an area.
     * 
     * @param player The player to check
     * @return able to use items
     * @deprecated This method seems to be the opposite of its name
     */
    @Deprecated
    public boolean canUse(LocalPlayer player) {
        return !allows(DefaultFlag.USE, player)
                && !canBuild(player);
    }

    /**
     * Gets the state of a state flag. This cannot be used for the build flag.
     *
     * @param flag flag to check
     * @return whether it is allowed
     * @throws IllegalArgumentException if the build flag is given
     */
    public boolean allows(StateFlag flag) {
        if (flag == DefaultFlag.BUILD) {
            throw new IllegalArgumentException("Can't use build flag with allows()");
        }
        return internalGetState(flag, null, null);
    }
    
    /**
     * Gets the state of a state flag. This cannot be used for the build flag.
     * 
     * @param flag flag to check
     * @param player player (used by some flags)
     * @return whether the state is allows for it
     * @throws IllegalArgumentException if the build flag is given
     */
    public boolean allows(StateFlag flag, LocalPlayer player) {
        if (flag == DefaultFlag.BUILD) {
            throw new IllegalArgumentException("Can't use build flag with allows()");
        }
        return internalGetState(flag, null, player);
    }
    
    /**
     * Indicates whether a player is an owner of all requests in this set.
     * 
     * @param player player
     * @return whether the player is an owner of all requests
     */
    public boolean isOwnerOfAll(LocalPlayer player) {
        for (ProtectedRequest request : applicable) {
            if (!request.isOwner(player)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Indicates whether a player is an owner or member of all requests in
     * this set.
     * 
     * @param player player
     * @return whether the player is a member of all requests
     */
    public boolean isMemberOfAll(LocalPlayer player) {
        for (ProtectedRequest request : applicable) {
            if (!request.isMember(player)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks to see if a flag is permitted.
     * 
     * @param flag flag to check
     * @param player null to not check owners and members
     * @param groupPlayer player to use for the group flag check
     * @return the allow/deny state for the flag
     */
    private boolean internalGetState(StateFlag flag, LocalPlayer player,
                                     LocalPlayer groupPlayer) {
        boolean found = false;
        boolean hasFlagDefined = false;
        boolean allowed = false; // Used for ALLOW override
        boolean def = flag.getDefault();
        
        // Handle defaults
        if (globalRequest != null) {
            State globalState = globalRequest.getFlag(flag);

            // The global request has this flag set
            if (globalState != null) {
                // Build flag is very special
                if (player != null && globalRequest.hasMembersOrOwners()) {
                    def = globalRequest.isMember(player) && (globalState == State.ALLOW);
                } else {
                    def = (globalState == State.ALLOW);
                }
            } else {
                // Build flag is very special
                if (player != null && globalRequest.hasMembersOrOwners()) {
                    def = globalRequest.isMember(player);
                }
            }
        }
        
        // The player argument is used if and only if the flag is the build
        // flag -- in which case, if there are any requests in this area, we
        // default to FALSE, otherwise true if there are no defined requests.
        // However, other flags are different -- if there are requests defined,
        // we default to the global request value. 
        if (player == null) {
            allowed = def; 
        }
        
        int lastPriority = Integer.MIN_VALUE;

        // The algorithm is as follows:
        // While iterating through the list of requests, if an entry disallows
        // the flag, then put it into the needsClear set. If an entry allows
        // the flag and it has a parent, then its parent is put into hasCleared.
        // In the situation that the child is reached before the parent, upon
        // the parent being reached, even if the parent disallows, because the
        // parent will be in hasCleared, permission will be allowed. In the
        // other case, where the parent is reached first, if it does not allow
        // permissions, it will be placed into needsClear. If a child of
        // the parent is reached later, the parent will be removed from
        // needsClear. At the end, if needsClear is not empty, that means that
        // permission should not be given. If a parent has multiple children
        // and one child does not allow permissions, then it will be placed into
        // needsClear just like as if was a parent.

        Set<ProtectedRequest> needsClear = new HashSet<ProtectedRequest>();
        Set<ProtectedRequest> hasCleared = new HashSet<ProtectedRequest>();

        for (ProtectedRequest request : applicable) {
            // Ignore lower priority requests
            if (hasFlagDefined && request.getPriority() < lastPriority) {
                break;
            }

            lastPriority = request.getPriority();

            // Ignore non-build requests
            if (player != null
                    && request.getFlag(DefaultFlag.PASSTHROUGH) == State.ALLOW) {
                continue;
            }

            // Check group permissions
            if (groupPlayer != null && flag.getRequestGroupFlag() != null) {
                RequestGroup group = request.getFlag(flag.getRequestGroupFlag());
                if (group == null) {
                    group = flag.getRequestGroupFlag().getDefault();
                }
                if (!RequestGroupFlag.isMember(request, group, groupPlayer)) {
                    continue;
                }
            }

            State v = request.getFlag(flag);

            // Allow DENY to override everything
            if (v == State.DENY) {
                return false;
            }

            // Forget about requests that allow it, although make sure the
            // default state is now to allow
            if (v == State.ALLOW) {
                allowed = true;
                found = true;
                hasFlagDefined = true;
                continue;
            }

            // For the build flag, the flags are conditional and are based
            // on membership, so we have to check for parent-child
            // relationships
            if (player != null) {
                hasFlagDefined = true;

                if (hasCleared.contains(request)) {
                    // Already cleared, so do nothing
                } else {
                    if (!request.isMember(player)) {
                        needsClear.add(request);
                    } else {
                        // Need to clear all parents
                        clearParents(needsClear, hasCleared, request);
                    }
                }
            }

            found = true;
        }

        return !found ? def :
                (allowed || (player != null && needsClear.size() == 0));
    }

    /**
     * Clear a request's parents for isFlagAllowed().
     * 
     * @param needsClear The requests that should be cleared
     * @param hasCleared The requests already cleared
     * @param request The request to start from
     */
    private void clearParents(Set<ProtectedRequest> needsClear,
            Set<ProtectedRequest> hasCleared, ProtectedRequest request) {
        ProtectedRequest parent = request.getParent();

        while (parent != null) {
            if (!needsClear.remove(parent)) {
                hasCleared.add(parent);
            }

            parent = parent.getParent();
        }
    }

    /**
     * @see #getFlag(org.stakeaclaim.protection.flags.Flag, org.stakeaclaim.LocalPlayer)
     * @param flag flag to check
     * @return value of the flag
     */
    public <T extends Flag<V>, V> V getFlag(T flag) {
        return getFlag(flag, null);
    }

    /**
     * Gets the value of a flag. Do not use this for state flags
     * (use {@link #allows(StateFlag, LocalPlayer)} for that).
     * 
     * @param flag flag to check
     * @param groupPlayer player to check {@link RequestGroup}s against
     * @return value of the flag
     * @throws IllegalArgumentException if a StateFlag is given
     */
    public <T extends Flag<V>, V> V getFlag(T flag, LocalPlayer groupPlayer) {
        /*
        if (flag instanceof StateFlag) {
            throw new IllegalArgumentException("Cannot use StateFlag with getFlag()");
        }
        */

        int lastPriority = 0;
        boolean found = false;

        Map<ProtectedRequest, V> needsClear = new HashMap<ProtectedRequest, V>();
        Set<ProtectedRequest> hasCleared = new HashSet<ProtectedRequest>();

        for (ProtectedRequest request : applicable) {
            // Ignore lower priority requests
            if (found && request.getPriority() < lastPriority) {
                break;
            }

            // Check group permissions
            if (groupPlayer != null && flag.getRequestGroupFlag() != null) {
                RequestGroup group = request.getFlag(flag.getRequestGroupFlag());
                if (group == null) {
                    group = flag.getRequestGroupFlag().getDefault();
                }
                if (!RequestGroupFlag.isMember(request, group, groupPlayer)) {
                    continue;
                }
            }

            if (hasCleared.contains(request)) {
                // Already cleared, so do nothing
            } else if (request.getFlag(flag) != null) {
                clearParents(needsClear, hasCleared, request);

                needsClear.put(request, request.getFlag(flag));

                found = true;
            }

            lastPriority = request.getPriority();
        }
        
        try {
            return needsClear.values().iterator().next();
        } catch (NoSuchElementException e) {
            if (globalRequest != null) {
                V gFlag = globalRequest.getFlag(flag);
                if (gFlag != null) return gFlag;
            }
            return null;
        }
    }

    /**
     * Clear a request's parents for getFlag().
     * 
     * @param needsClear The requests that should be cleared
     * @param hasCleared The requests already cleared
     * @param request The request to start from
     */
    private void clearParents(Map<ProtectedRequest, ?> needsClear,
            Set<ProtectedRequest> hasCleared, ProtectedRequest request) {
        ProtectedRequest parent = request.getParent();

        while (parent != null) {
            if (needsClear.remove(parent) == null) {
                hasCleared.add(parent);
            }

            parent = parent.getParent();
        }
    }
    
    /**
     * Get the number of requests that are included.
     * 
     * @return the size of this ApplicbleRequestSet
     */
    public int size() {
        return applicable.size();
    }
    
    /**
     * Get an iterator of affected requests.
     */
    public Iterator<ProtectedRequest> iterator() {
        return applicable.iterator();
    }
}
