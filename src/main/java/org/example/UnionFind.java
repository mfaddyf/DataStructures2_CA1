package org.example;

public class UnionFind {

    // parent pointer
    private final int[] parent;
    // size of each component
    private final int[] size;

    /**
     * constructs union find structure with n elements ( 0 to n-1 )
     * at the start, each element is in its own set
     * @param n
     */
    public UnionFind(int n) {
        parent = new int[n];
        size = new int[n];
        // start each element as its own parent
        for (int i = 0; i < n; i++) {
            parent[i] = i; // self root
            size[i] = 1; // each set has size of 1 at the start
        }
    }

    /**
     * finds the root of the set that has x
     * uses path compression:
     *  - flattens tree by making nodes to point to their grandparent
     *  - greatly improves performance for future
     * @param x said element
     * @return root of the set containing x
     */
    public int find(int x) {
        while (x != parent[x]) {
            // path compression ( points to the grandparent )
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    /**
     * unites the sets containing a and b
     * uses union by size:
     *  - attach smaller tree under larger
     *  - keeps tree shallow to improve performance!!!! took a LOOOONG time to figure this out
     * @param a first element
     * @param b second element
     */
    public void union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        // if in the same set already -> nothing to do
        if (ra == rb) {
            return;
        }
        // attach smaller tree to the larger one
        if (size[ra] < size[rb]) {
            parent[ra] = rb;
            size[rb] += size[ra];
        } else {
            parent[rb] = ra;
            size[ra] += size[rb];
        }
    }

    /**
     * returns the size of connected component containing x
     * @param x said element
     * @return size of its set
     */
    public int componentSize(int x) {
        return size[find(x)];
    }
}