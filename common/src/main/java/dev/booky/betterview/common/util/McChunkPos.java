package dev.booky.betterview.common.util;
// Created by booky10 in BetterView (14:12 03.06.2025)

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class McChunkPos {

    public static final McChunkPos ZERO = new McChunkPos(0, 0);

    private final int posX;
    private final int posZ;
    private final long key;

    public McChunkPos(int posX, int posZ, long key) {
        this.posX = posX;
        this.posZ = posZ;
        this.key = key;
    }

    public McChunkPos(int posX, int posZ) {
        this.posX = posX;
        this.posZ = posZ;
        this.key = getChunkKey(posX, posZ);
    }

    public McChunkPos(long key) {
        this.posX = getChunkX(key);
        this.posZ = getChunkZ(key);
        this.key = key;
    }

    public static long getChunkKey(int posX, int posZ) {
        return ((long) posZ << Integer.SIZE) | (posX & 0xFFFFFFFFL);
    }

    public static int getChunkX(long key) {
        return (int) key;
    }

    public static int getChunkZ(long key) {
        return (int) (key >>> Integer.SIZE);
    }

    public int distanceSquared(McChunkPos other) {
        int diffX = this.posX - other.posX;
        int diffZ = this.posZ - other.posZ;
        return diffX * diffX + diffZ * diffZ;
    }

    public int getX() {
        return this.posX;
    }

    public int getZ() {
        return this.posZ;
    }

    public long getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof McChunkPos that)) return false;
        return this.key == that.key;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.posX, this.posZ);
    }

    @Override
    public String toString() {
        return "Chunk[" + this.posX + ";" + this.posZ + ']';
    }
}
