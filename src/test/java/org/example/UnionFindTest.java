package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UnionFindTest {

    @Test
    void testUnionAndFind() {
        UnionFind uf = new UnionFind(10);

        uf.union(1, 2);
        uf.union(2, 3);

        assertEquals(uf.find(1), uf.find(3));
    }

    @Test
    void testSeparateSets() {
        UnionFind uf = new UnionFind(10);

        uf.union(1, 2);

        assertNotEquals(uf.find(1), uf.find(5));
    }

    @Test
    void testComponentSize() {
        UnionFind uf = new UnionFind(10);
        uf.union(1, 2);
        uf.union(2, 3);
        assertEquals(3, uf.componentSize(1));
    }

    @Test
    void testSelfFind() {
        UnionFind uf = new UnionFind(5);
        // each element should be its own root initially
        for (int i = 0; i < 5; i++) {
            assertEquals(i, uf.find(i));
        }
    }

    @Test
    void testUnionIsSymmetric() {
        UnionFind uf = new UnionFind(10);
        uf.union(3, 7);
        assertEquals(uf.find(3), uf.find(7));
        assertEquals(uf.find(7), uf.find(3));
    }

    @Test
    void testMultipleMerges() {
        UnionFind uf = new UnionFind(10);
        uf.union(0, 1);
        uf.union(2, 3);
        uf.union(1, 2);
        // all four should be in same set
        assertEquals(uf.find(0), uf.find(3));
    }
}