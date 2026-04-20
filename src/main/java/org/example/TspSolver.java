package org.example;

import java.util.ArrayList;
import java.util.List;

public class TspSolver {

    public static List<LeafCluster> nearestNeighbourPath(List<LeafCluster> clusters, LeafCluster start) {
        List<LeafCluster> path = new ArrayList<>();

        if (clusters == null || clusters.isEmpty() || start == null) {
            return path;
        }

        List<LeafCluster> unvisited = new ArrayList<>(clusters);
        LeafCluster current = start;

        path.add(current);
        unvisited.remove(current);

        while (!unvisited.isEmpty()) {
            LeafCluster nearest = null;
            double bestDistance = Double.MAX_VALUE;

            for (LeafCluster candidate : unvisited) {
                double d = distance(current, candidate);
                if (d < bestDistance) {
                    bestDistance = d;
                    nearest = candidate;
                }
            }

            current = nearest;
            path.add(current);
            unvisited.remove(current);
        }

        return path;
    }

    private static double distance(LeafCluster a, LeafCluster b) {
        double dx = a.getCenterX() - b.getCenterX();
        double dy = a.getCenterY() - b.getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}