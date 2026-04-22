package org.example;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.openjdk.jmh.annotations.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.scene.image.Image;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class LeafBenchmark {

    private boolean[] binary;
    private int width;
    private int height;
    private ProcessingSettings settings;
    private List<LeafCluster> clusters;
    private Image testImage;

    @Setup
    public void setup() throws Exception {
        // init JavaFX toolkit
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException e) {
            // already running
        }

        settings = new ProcessingSettings();
        settings.minSaturation = 0.1;
        settings.minBrightness = 0.1;
        settings.hueTolerance = 20;
        settings.saturationTolerance = 0.5;
        settings.brightnessTolerance = 0.5;
        settings.minClusterSize = 1;
        settings.maxClusterSize = 200000;

        // load test image
        testImage = new Image(
                getClass().getResourceAsStream("/leaves1.png")
        );

        width = (int) testImage.getWidth();
        height = (int) testImage.getHeight();

        // pre-compute binary array for benchmarks that don't need to test conversion
        binary = ImageProcessor.convertToBinaryArray(
                testImage,
                List.of(Color.ORANGE),
                settings
        );

        // pre-compute clusters for TSP benchmark
        clusters = ImageProcessor.findClusters(binary, width, height, settings);
        clusters.sort((a, b) -> Integer.compare(b.pixelCount, a.pixelCount));
    }

    //
    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.exit(); // request Javafx to shutdown to avoid looooong waits between

        // letting it actually terminate
        new Thread(() -> {
            try {
                Thread.sleep(100); // small delay
            } catch (InterruptedException ignored) {}
            latch.countDown();
        }).start();

        latch.await();
    }

    // benchmark 1 - how long does binary conversion take?
    @Benchmark
    public boolean[] benchmarkConvertToBinary() {
        return ImageProcessor.convertToBinaryArray(
                testImage,
                List.of(Color.ORANGE),
                settings
        );
    }

    // benchmark 2 - how long does union-find cluster detection take?
    @Benchmark
    public List<LeafCluster> benchmarkFindClusters() {
        return ImageProcessor.findClusters(binary, width, height, settings);
    }

    // benchmark 3 - how long does denoise take?
    @Benchmark
    public boolean[] benchmarkDenoise() {
        return ImageProcessor.denoise(binary, width, height);
    }

    // benchmark 4 - how long does TSP nearest neighbour take?
    @Benchmark
    public List<LeafCluster> benchmarkTspSolver() {
        if (clusters == null || clusters.isEmpty()) {
            return List.of();
        }
        return TspSolver.nearestNeighbourPath(clusters, clusters.get(0));
    }

    // benchmark 5 - how long does union find alone take on a large array?
    @Benchmark
    public UnionFind benchmarkUnionFindRaw() {
        int total = width * height;
        UnionFind uf = new UnionFind(total);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (!binary[index]) continue;

                if (x + 1 < width && binary[y * width + (x + 1)]) {
                    uf.union(index, y * width + (x + 1));
                }
                if (y + 1 < height && binary[(y + 1) * width + x]) {
                    uf.union(index, (y + 1) * width + x);
                }
            }
        }
        return uf;
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}