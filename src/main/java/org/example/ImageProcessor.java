package org.example;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.*;

/**
 *  UTILITY CLASS for processing images to detect and analyse leaf clusters
 *
 *  - converts image to binary (leaf vs not-leaf)
 *  - denoise binary img
 *  - detects clusters using a union-find
 *  - render visualisations of clusters
 */

public class ImageProcessor {

    // cached union-find structure from last clustering
    private static UnionFind lastUnionFind;
    // cached binary img  from last processing step
    private static boolean[] lastBinary;
    // cached img dimensions
    private static int lastWidth;
    private static int lastHeight;

    /**
     * converts an image into a black/white representation of where leaf pixels are / arent
     * @param img input image
     * @param samples optionally sampled leaf colours
     * @param settings processing thresholds
     * @return binary image ( white is leaf, black is not leaf )
     */
    public static Image makeBinaryImage(Image img, List<Color> samples, ProcessingSettings settings) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();

        // computing binary rep
        boolean[] binary = convertToBinaryArray(img, samples, settings);

        WritableImage out = new WritableImage(width, height);
        PixelWriter pw = out.getPixelWriter();

        // render binary array into the image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isLeaf = binary[y * width + x];
                pw.setColor(x, y, isLeaf ? Color.WHITE : Color.BLACK);
            }
        }

        return out;
    }

    /**
     * converts an image into a binary array and indicates leaf pixels
     *      - classifies each pixel using colour rules
     *      - denoise filter
     *      - cache result for later visualisation
     * @param img input image
     * @param samples optionally sampled leaf colours
     * @param settings processing thresholds
     * @return boolean array ( true = leaf pixel )
     */
    public static boolean[] convertToBinaryArray(Image img, List<Color> samples, ProcessingSettings settings) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();

        boolean[] binary = new boolean[width * height];
        PixelReader pr = img.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel = pr.getColor(x, y);
                binary[y * width + x] = isLeafColor(pixel, samples, settings);
            }
        }

        binary = denoise(binary, width, height);

        lastBinary = binary;
        lastWidth = width;
        lastHeight = height;

        return binary;
    }

    /**
     * determines whether a picel should be classified as a leaf
     *      - minimum saturation/brightness thresholds
     *      - optional comparison to sampled colours
     */
    private static boolean isLeafColor(Color pixel, List<Color> samples, ProcessingSettings settings) {
        if (pixel.getSaturation() < settings.minSaturation) {
            return false;
        }

        if (pixel.getBrightness() < settings.minBrightness) {
            return false;
        }

        if (samples == null || samples.isEmpty()) {
            return defaultLeafRule(pixel);
        }

        double hue = pixel.getHue();
        double sat = pixel.getSaturation();
        double bright = pixel.getBrightness();

        for (Color sample : samples) {
            double hueDiff = hueDistance(hue, sample.getHue());
            double satDiff = Math.abs(sat - sample.getSaturation());
            double brightDiff = Math.abs(bright - sample.getBrightness());

            if (hueDiff <= settings.hueTolerance
                    && satDiff <= settings.saturationTolerance
                    && brightDiff <= settings.brightnessTolerance) {
                return true;
            }
        }

        return false;
    }

    /**
     * default for detecting leaf-like colours when no samples are provided
     *      - by default it targets autumn like hues ( standard colours when leaves are.... on the ground.....)
     */
    private static boolean defaultLeafRule(Color pixel) {
        double hue = pixel.getHue();
        double sat = pixel.getSaturation();
        double bright = pixel.getBrightness();

        boolean autumnHue =
                (hue >= 0 && hue <= 60) ||
                        (hue >= 330 && hue <= 360);

        return autumnHue && sat >= 0.25 && bright >= 0.20;
    }

    /**
     * computes a circular distance between two hue values
     */
    private static double hueDistance(double h1, double h2) {
        double diff = Math.abs(h1 - h2);
        return Math.min(diff, 360.0 - diff);
    }

    /**
     * applies simple 3x3 neighbourhood filter to remove noise from the b/w scan
     *      - a pixel is kept if at least 3 neighbours are white
     */
    public static boolean[] denoise(boolean[] binary, int width, int height) {
        boolean[] out = new boolean[binary.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int whiteCount = 0;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;

                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if (binary[ny * width + nx]) {
                                whiteCount++;
                            }
                        }
                    }
                }

                out[y * width + x] = whiteCount >= 3;
            }
        }

        return out;
    }

    /**
     * find connected clusters of leaf pixels using a union-find
     *      - two pixels are connected if they are adjacent to form a cluster
     * @return list of clusters filtered by size constraints
     */
    public static List<LeafCluster> findClusters(boolean[] binary, int width, int height, ProcessingSettings settings) {
        int total = width * height;
        UnionFind uf = new UnionFind(total);

        // first pass , union adjacent leaf pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                if (!binary[index]) {
                    continue;
                }

                // connect to the right neighbour
                if (x + 1 < width) {
                    int right = y * width + (x + 1);
                    if (binary[right]) {
                        uf.union(index, right);
                    }
                }

                // connect to bottom neighbour
                if (y + 1 < height) {
                    int down = (y + 1) * width + x;
                    if (binary[down]) {
                        uf.union(index, down);
                    }
                }
            }
        }

        // cache for visualisation
        lastUnionFind = uf;
        lastBinary = binary;
        lastWidth = width;
        lastHeight = height;

        Map<Integer, LeafCluster> clusterMap = new HashMap<>();

        // second pass : group pixels into clusters
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                if (!binary[index]) {
                    continue;
                }

                int root = uf.find(index);
                LeafCluster cluster = clusterMap.get(root);

                // get / create cluster
                if (cluster == null) {
                    cluster = new LeafCluster();
                    cluster.root = root;
                    cluster.minX = x;
                    cluster.maxX = x;
                    cluster.minY = y;
                    cluster.maxY = y;
                    cluster.pixelCount = 0;
                    clusterMap.put(root, cluster);
                }

                // update cluster stats
                cluster.pixelCount++;

                if (x < cluster.minX) cluster.minX = x;
                if (x > cluster.maxX) cluster.maxX = x;
                if (y < cluster.minY) cluster.minY = y;
                if (y > cluster.maxY) cluster.maxY = y;
            }
        }

        // filter clusters by size
        List<LeafCluster> clusters = new ArrayList<>();

        for (LeafCluster cluster : clusterMap.values()) {
            if (cluster.pixelCount >= settings.minClusterSize
                    && cluster.pixelCount <= settings.maxClusterSize) {
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * highlights a selected cluster in red
     */
    public static Image makeSelectedClusterImage(int selectedRoot) {
        if (lastUnionFind == null || lastBinary == null) {
            return null;
        }

        WritableImage out = new WritableImage(lastWidth, lastHeight);
        PixelWriter pw = out.getPixelWriter();

        for (int y = 0; y < lastHeight; y++) {
            for (int x = 0; x < lastWidth; x++) {
                int index = y * lastWidth + x;

                if (!lastBinary[index]) {
                    pw.setColor(x, y, Color.BLACK);
                } else {
                    int root = lastUnionFind.find(index);
                    if (root == selectedRoot) {
                        pw.setColor(x, y, Color.RED);
                    } else {
                        pw.setColor(x, y, Color.BLACK);
                    }
                }
            }
        }

        return out;
    }

    /**
     * generates an image where each cluster is assigned a random colour
     */
    public static Image makeRandomClusterImage(List<LeafCluster> clusters) {
        if (lastUnionFind == null || lastBinary == null) {
            return null;
        }

        WritableImage out = new WritableImage(lastWidth, lastHeight);
        PixelWriter pw = out.getPixelWriter();

        Map<Integer, Color> colorMap = new HashMap<>();
        Random random = new Random(42);

        // assigned random colours to colours
        for (LeafCluster cluster : clusters) {
            Color randomColor = Color.color(
                    0.2 + random.nextDouble() * 0.8,
                    0.2 + random.nextDouble() * 0.8,
                    0.2 + random.nextDouble() * 0.8
            );
            colorMap.put(cluster.root, randomColor);
        }

        // render image
        for (int y = 0; y < lastHeight; y++) {
            for (int x = 0; x < lastWidth; x++) {
                int index = y * lastWidth + x;

                if (!lastBinary[index]) {
                    pw.setColor(x, y, Color.BLACK);
                } else {
                    int root = lastUnionFind.find(index);
                    Color c = colorMap.get(root);
                    pw.setColor(x, y, c != null ? c : Color.WHITE);
                }
            }
        }

        return out;
    }
}