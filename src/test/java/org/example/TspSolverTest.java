package org.example;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TspSolverTest {

    private LeafCluster makeCluster(int id, int x, int y) {
        LeafCluster c = new LeafCluster();
        c.id = id;
        c.minX = x - 5;
        c.maxX = x + 5;
        c.minY = y - 5;
        c.maxY = y + 5;
        c.pixelCount = 100;
        return c;
    }

    @Test
    void testPathIncludesAllClusters() {
        LeafCluster a = makeCluster(1, 0, 0);
        LeafCluster b = makeCluster(2, 10, 0);
        LeafCluster c = makeCluster(3, 20, 0);
        List<LeafCluster> clusters = List.of(a, b, c);

        List<LeafCluster> path = TspSolver.nearestNeighbourPath(clusters, a);

        assertEquals(3, path.size());
        assertTrue(path.containsAll(clusters));
    }

    @Test
    void testPathStartsAtChosenCluster() {
        LeafCluster a = makeCluster(1, 0, 0);
        LeafCluster b = makeCluster(2, 100, 0);
        List<LeafCluster> clusters = List.of(a, b);

        List<LeafCluster> path = TspSolver.nearestNeighbourPath(clusters, b);

        assertEquals(b, path.get(0));
    }

    @Test
    void testEmptyInput() {
        List<LeafCluster> path = TspSolver.nearestNeighbourPath(List.of(), null);
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    void testSingleCluster() {
        LeafCluster a = makeCluster(1, 0, 0);
        List<LeafCluster> path = TspSolver.nearestNeighbourPath(List.of(a), a);
        assertEquals(1, path.size());
    }

    @Test
    void testNearestNeighbourPicksClosest() {
        // a is start, b is close, c is far - b should come before c
        LeafCluster a = makeCluster(1, 0, 0);
        LeafCluster b = makeCluster(2, 10, 0);
        LeafCluster c = makeCluster(3, 1000, 0);
        List<LeafCluster> clusters = List.of(a, b, c);

        List<LeafCluster> path = TspSolver.nearestNeighbourPath(clusters, a);

        assertEquals(a, path.get(0));
        assertEquals(b, path.get(1));
        assertEquals(c, path.get(2));
    }
}