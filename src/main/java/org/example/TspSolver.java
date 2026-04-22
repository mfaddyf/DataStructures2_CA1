package org.example;

import java.util.ArrayList;
import java.util.List;

public class TspSolver {

    /**
     * builds a path through all clusters using the nearest neighbour
     *
     * 1. start at chosen cluster
     * 2. repeatedly go to the closest unvisited cluster
     * 3. stop when all clusters are counted for
     *
     * is it optimal path??? not too sure, but closest i could get it
     * it seems to be the fastest + most accurate ver i could write without loosing my mind
     *
     * @param clusters list of all clusters / nodes
     * @param start starting cluster
     * @return ordered list representing the path
     */
    public static List<LeafCluster> nearestNeighbourPath(List<LeafCluster> clusters, LeafCluster start) {
        List<LeafCluster> path = new ArrayList<>();
        // invalid input / null input wont run
        if (clusters == null || clusters.isEmpty() || start == null) {
            return path;
        }
        // copy of the leaf clusters to track the nodes
        List<LeafCluster> unvisited = new ArrayList<>(clusters);
        // starting from the chosen cluster
        LeafCluster current = start;

        path.add(current);
        unvisited.remove(current);
        // continuing until all clusters are found!
        while (!unvisited.isEmpty()) {
            LeafCluster nearest = null;
            double bestDistance = Double.MAX_VALUE;
            //  find the closest cluster to current one
            for (LeafCluster candidate : unvisited) {
                double d = distance(current, candidate);
                if (d < bestDistance) {
                    bestDistance = d;
                    nearest = candidate;
                }
            }
            // move to the nearest cluster
            current = nearest;
            path.add(current);
            unvisited.remove(current);
        }

        return path;
    }

    /**
     * computes distance between the centers of the two clusters
     * @param a first cluster
     * @param b second cluster
     * @return distance between the cluster centres
     */
    private static double distance(LeafCluster a, LeafCluster b) {
        double dx = a.getCenterX() - b.getCenterX();
        double dy = a.getCenterY() - b.getCenterY();
        // distance formula !!
        return Math.sqrt(dx * dx + dy * dy);
    }
}