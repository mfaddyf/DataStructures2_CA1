package org.example;

/**
 * represents a connected cluster of leaf pixels in a binary img
 *      - cluster is identified by root in the union-find structure
 *      and stores basic geometric and size info
 */

public class LeafCluster {
    public int id;
    public int root;
    public int pixelCount;
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;

    public double getCenterX() {
        return (minX + maxX) / 2.0;
    }

    public double getCenterY() {
        return (minY + maxY) / 2.0;
    }

    public int getBoxWidth() {
        return maxX - minX + 1;
    }

    public int getBoxHeight() {
        return maxY - minY + 1;
    }
}