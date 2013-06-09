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

package org.stakeaclaim.protection.requests;

//import com.sk89q.worldedit.BlockVector;
//import com.sk89q.worldedit.BlockVector2D;
//import com.sk89q.worldedit.Vector;
import org.stakeaclaim.LocalPlayer;
//import org.stakeaclaim.domains.DefaultDomain;
//import org.stakeaclaim.protection.UnsupportedIntersectionException;
//import org.stakeaclaim.protection.flags.Flag;

//import java.awt.geom.Line2D;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;

/**
 * Represents a stake request.
 *
 * @author sk89q
 */
public abstract class Request implements Comparable<Request> {

    public enum Status {
        PENDING,
        DENIED,
        ACCEPTED,
        CANCELED
    }

//    protected BlockVector min;
//    protected BlockVector max;

//    private static final Pattern idPattern = Pattern.compile("^[A-Za-z0-9_,'\\-\\+/]{1,}$");

    /**
     * Holds the request's ID number. (epoch time stamp in mill/s)
     */
    private long requestID;

    /**
     * Holds the ID of the region the request is for.
     */
    private String regionID;

    /**
     * Holds the requester's name.
     */
    private String playerName;

    /**
     * Holds the status of the request.
     */
    private Status status;

//    /**
//     * Priority.
//     */
//    private int priority = 0;

//    /**
//     * Holds the curParent.
//     */
//    private Request parent;

//    /**
//     * List of owners.
//     */
//    private DefaultDomain owners = new DefaultDomain();

//    /**
//     * List of members.
//     */
//    private DefaultDomain members = new DefaultDomain();

//    /**
//     * List of flags.
//     */
//    private Map<Flag<?>, Object> flags = new HashMap<Flag<?>, Object>();

    /**
     * Construct a new instance of this request.
     *
     * @param regionID The ID of the region being requested.
     * @param playerName The requester's name.
     */
    public Request(String regionID, String playerName) {
        this.requestID = System.currentTimeMillis();
        this.regionID = regionID;
        this.playerName = playerName;
        this.status = Status.PENDING;
    }

//    /**
//     * Sets the minimum and maximum points of the bounding box for a request
//     *
//     * @param points The points to set. Must have at least one element.
//     */
//    protected void setMinMaxPoints(List<Vector> points) {
//        int minX = points.get(0).getBlockX();
//        int minY = points.get(0).getBlockY();
//        int minZ = points.get(0).getBlockZ();
//        int maxX = minX;
//        int maxY = minY;
//        int maxZ = minZ;
//
//        for (Vector v : points) {
//            int x = v.getBlockX();
//            int y = v.getBlockY();
//            int z = v.getBlockZ();
//
//            if (x < minX) minX = x;
//            if (y < minY) minY = y;
//            if (z < minZ) minZ = z;
//
//            if (x > maxX) maxX = x;
//            if (y > maxY) maxY = y;
//            if (z > maxZ) maxZ = z;
//        }
//        
//        min = new BlockVector(minX, minY, minZ);
//        max = new BlockVector(maxX, maxY, maxZ);
//    }

    /**
     * Gets the ID number of this request
     *
     * @return the requestID
     */
    public long getRequestID() {
        return requestID;
    }

//    /**
//     * Get the lower point of the cuboid.
//     *
//     * @return min point
//     */
//    public BlockVector getMinimumPoint() {
//        return min;
//    }

//    /**
//     * Get the upper point of the cuboid.
//     *
//     * @return max point
//     */
//    public BlockVector getMaximumPoint() {
//        return max;
//    }

//    /**
//     * @return the priority
//     */
//    public int getPriority() {
//        return priority;
//    }

//    /**
//     * @param priority the priority to setFlag
//     */
//    public void setPriority(int priority) {
//        this.priority = priority;
//    }

//    /**
//     * @return the curParent
//     */
//    public Request getParent() {
//        return parent;
//    }

//    /**
//     * Set the curParent. This checks to make sure that it will not result
//     * in circular inheritance.
//     *
//     * @param parent the curParent to setFlag
//     * @throws CircularInheritanceException when circular inheritance is detected
//     */
//    public void setParent(Request parent) throws CircularInheritanceException {
//        if (parent == null) {
//            this.parent = null;
//            return;
//        }
//
//        if (parent == this) {
//            throw new CircularInheritanceException();
//        }
//
//        Request p = parent.getParent();
//        while (p != null) {
//            if (p == this) {
//                throw new CircularInheritanceException();
//            }
//            p = p.getParent();
//        }
//
//        this.parent = parent;
//    }


//    /**
//     * @return the owners
//     */
//    public DefaultDomain getOwners() {
//
//        return owners;
//    }

//    /**
//     * @param owners the owners to setFlag
//     */
//    public void setOwners(DefaultDomain owners) {
//        this.owners = owners;
//    }

//    /**
//     * @return the members
//     */
//    public DefaultDomain getMembers() {
//        return members;
//    }

//    /**
//     * @param members the members to setFlag
//     */
//    public void setMembers(DefaultDomain members) {
//        this.members = members;
//    }

//    /**
//     * Checks whether a request has members or owners.
//     *
//     * @return whether there are members or owners
//     */
//    public boolean hasMembersOrOwners() {
//        return owners.size() > 0 || members.size() > 0;
//    }

//    /**
//     * Checks whether a player is an owner of request or any of its parents.
//     *
//     * @param player player to check
//     * @return whether an owner
//     */
//    public boolean isOwner(LocalPlayer player) {
//        if (owners.contains(player)) {
//            return true;
//        }
//
//        Request curParent = getParent();
//        while (curParent != null) {
//            if (curParent.getOwners().contains(player)) {
//                return true;
//            }
//
//            curParent = curParent.getParent();
//        }
//
//        return false;
//    }

//    /**
//     * Checks whether a player is an owner of request or any of its parents.
//     *
//     * @param playerName player name to check
//     * @return whether an owner
//     */
//    public boolean isOwner(String playerName) {
//        if (owners.contains(playerName)) {
//            return true;
//        }
//
//        Request curParent = getParent();
//        while (curParent != null) {
//            if (curParent.getOwners().contains(playerName)) {
//                return true;
//            }
//
//            curParent = curParent.getParent();
//        }
//
//        return false;
//    }

//    /**
//     * Checks whether a player is a member OR OWNER of the request
//     * or any of its parents.
//     *
//     * @param player player to check
//     * @return whether an owner or member
//     */
//    public boolean isMember(LocalPlayer player) {
//        if (isOwner(player)) {
//            return true;
//        }
//
//        if (members.contains(player)) {
//            return true;
//        }
//
//        Request curParent = getParent();
//        while (curParent != null) {
//            if (curParent.getMembers().contains(player)) {
//                return true;
//            }
//
//            curParent = curParent.getParent();
//        }
//
//        return false;
//    }

//    /**
//     * Checks whether a player is a member OR OWNER of the request
//     * or any of its parents.
//     *
//     * @param playerName player name to check
//     * @return whether an owner or member
//     */
//    public boolean isMember(String playerName) {
//        if (isOwner(playerName)) {
//            return true;
//        }
//
//        if (members.contains(playerName)) {
//            return true;
//        }
//
//        Request curParent = getParent();
//        while (curParent != null) {
//            if (curParent.getMembers().contains(playerName)) {
//                return true;
//            }
//
//            curParent = curParent.getParent();
//        }
//
//        return false;
//    }

//    /**
//     * Checks whether a player is a member of the request
//     * or any of its parents.
//     *
//     * @param player player to check
//     * @return whether an member
//     */
//    public boolean isMemberOnly(LocalPlayer player) {
//        if (members.contains(player)) {
//            return true;
//        }
//
//        Request curParent = getParent();
//        while (curParent != null) {
//            if (curParent.getMembers().contains(player)) {
//                return true;
//            }
//
//            curParent = curParent.getParent();
//        }
//
//        return false;
//    }

//    /**
//     * Get a flag's value.
//     *
//     * @param <T> The flag type
//     * @param <V> The type of the flag's value
//     * @param flag The flag to check
//     * @return value or null if isn't defined
//     */
//    @SuppressWarnings("unchecked")
//    public <T extends Flag<V>, V> V getFlag(T flag) {
//        Object obj = flags.get(flag);
//        V val;
//        if (obj != null) {
//            val = (V) obj;
//        } else {
//            return null;
//        }
//        return val;
//    }

//    /**
//     * Set a flag's value.
//     *
//     * @param <T> The flag type
//     * @param <V> The type of the flag's value
//     * @param flag The flag to check
//     * @param val The value to set
//     */
//    public <T extends Flag<V>, V> void setFlag(T flag, V val) {
//        if (val == null) {
//            flags.remove(flag);
//        } else {
//            flags.put(flag, val);
//        }
//    }

//    /**
//     * Get the map of flags.
//     *
//     * @return The map of flags currently used for this request
//     */
//    public Map<Flag<?>, Object> getFlags() {
//        return flags;
//    }

//    /**
//     * Get the map of flags.
//     *
//     * @param flags The flags to set
//     */
//    public void setFlags(Map<Flag<?>, Object> flags) {
//        this.flags = flags;
//    }

//    /**
//     * Gets the 2D points for this request
//     *
//     * @return The points for this request as (x, z) coordinates
//     */
//    public abstract List<BlockVector2D> getPoints();

//    /**
//     * Get the number of blocks in this request
//     *
//     * @return the volume of this request in blocks
//     */
//    public abstract int volume();

//    /**
//     * Check to see if a point is inside this request.
//     *
//     * @param pt The point to check
//     * @return Whether {@code pt} is in this request
//     */
//    public abstract boolean contains(Vector pt);

//    /**
//     * Check to see if a point is inside this request.
//     *
//     * @param pt The point to check
//     * @return Whether {@code pt} is in this request
//     */
//    public boolean contains(BlockVector2D pt) {
//        return contains(new Vector(pt.getBlockX(), min.getBlockY(), pt.getBlockZ()));
//    }

//    /**
//     * Check to see if a point is inside this request.
//     *
//     * @param x The x coordinate to check
//     * @param y The y coordinate to check
//     * @param z The z coordinate to check
//     * @return Whether this request contains the points at the given coordinate
//     */
//    public boolean contains(int x, int y, int z) {
//        return contains(new Vector(x, y, z));
//    }

//    /**
//     * Check to see if any of the 2D points are inside this request.
//     *
//     * @param pts
//     * @return
//     */
//    public boolean containsAny(List<BlockVector2D> pts) {
//        for (BlockVector2D pt : pts) {
//            if (contains(pt)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    /**
//     * Compares to another request.<br>
//     *<br>
//     * Orders primarily by the priority, descending<br>
//     * Orders secondarily by the id, ascending
//     *
//     * @param other The request to compare to
//     */
//    public int compareTo(Request other) {
//        if (priority > other.priority) {
//            return -1;
//        } else if (priority < other.priority) {
//            return 1;
//        }
//
//        return id.compareTo(other.id);
//    }

//    /**
//     * Return the type of request as a user-friendly, lowercase name.
//     *
//     * @return type of request
//     */
//    public abstract String getTypeName();

//    /**
//     * Get a list of intersecting requests.
//     *
//     * @param requests The list of requests to source from
//     * @return The elements of {@code requests} that intersect with this request
//     * @throws UnsupportedIntersectionException if an invalid intersection is detected
//     */
//    public abstract List<Request> getIntersectingRequests(
//            List<Request> requests)
//            throws UnsupportedIntersectionException;

//    /**
//     * Checks if the bounding box of a request intersects with with the bounding
//     * box of this request
//     *
//     * @param request The request to check
//     * @return whether the given request intersects
//     */
//    protected boolean intersectsBoundingBox(Request request) {
//        BlockVector rMaxPoint = request.getMaximumPoint();
//        BlockVector min = getMinimumPoint();
//
//        if (rMaxPoint.getBlockX() < min.getBlockX()) return false;
//        if (rMaxPoint.getBlockY() < min.getBlockY()) return false;
//        if (rMaxPoint.getBlockZ() < min.getBlockZ()) return false;
//
//        BlockVector rMinPoint = request.getMinimumPoint();
//        BlockVector max = getMaximumPoint();
//
//        if (rMinPoint.getBlockX() > max.getBlockX()) return false;
//        if (rMinPoint.getBlockY() > max.getBlockY()) return false;
//        if (rMinPoint.getBlockZ() > max.getBlockZ()) return false;
//
//        return true;
//    }

//    /**
//     * Compares all edges of two requests to see if any of them intersect
//     *
//     * @param request The request to check
//     * @return whether any edges of a request intersect
//     */
//    protected boolean intersectsEdges(Request request) {
//        List<BlockVector2D> pts1 = getPoints();
//        List<BlockVector2D> pts2 = request.getPoints();
//        BlockVector2D lastPt1 = pts1.get(pts1.size() - 1);
//        BlockVector2D lastPt2 = pts2.get(pts2.size() - 1);
//        for (BlockVector2D aPts1 : pts1) {
//            for (BlockVector2D aPts2 : pts2) {
//
//                Line2D line1 = new Line2D.Double(
//                        lastPt1.getBlockX(),
//                        lastPt1.getBlockZ(),
//                        aPts1.getBlockX(),
//                        aPts1.getBlockZ());
//
//                if (line1.intersectsLine(
//                        lastPt2.getBlockX(),
//                        lastPt2.getBlockZ(),
//                        aPts2.getBlockX(),
//                        aPts2.getBlockZ())) {
//                    return true;
//                }
//                lastPt2 = aPts2;
//            }
//            lastPt1 = aPts1;
//        }
//        return false;
//    }

//    /**
//     * Checks to see if the given ID is accurate.
//     *
//     * @param id The id to check
//     * @see #idPattern
//     * @return Whether the request id given is valid
//     */
//    public static boolean isValidId(String id) {
//        return idPattern.matcher(id).matches();
//    }

//    @Override
//    public int hashCode(){
//        return id.hashCode();
//    }

//    /**
//     * Returns whether this request has the same ID as another request.
//     */
//    @Override
//    public boolean equals(Object obj) {
//        if (!(obj instanceof Request)) {
//            return false;
//        }
//
//        Request other = (Request) obj;
//        return other.getId().equals(getId());
//    }

//    /**
//     * Thrown when setting a curParent would create a circular inheritance
//     * situation.
//     */
//    public static class CircularInheritanceException extends Exception {
//        private static final long serialVersionUID = 7479613488496776022L;
//    }
}
