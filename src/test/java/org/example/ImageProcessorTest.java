package org.example;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

public class ImageProcessorTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            // initialises javafx toolkit, needed form image processing, doesn't work without??
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // already started = ignore
        }
    }

    // tests binary conversion works by checking the output isn't null after changing modifier
    @Test
    void testBinaryConversionNotNull() {
        // load up test img
        Image img = new Image(
                getClass().getResource("/leaves2.png").toExternalForm()
        );

        // congfiguring processing settings
        ProcessingSettings settings = new ProcessingSettings();
        settings.minSaturation = 0.1;
        settings.minBrightness = 0.1;
        settings.hueTolerance = 20;
        settings.saturationTolerance = 0.5;
        settings.brightnessTolerance = 0.5;

        // convert img to binary array using colour filters
        boolean[] result = ImageProcessor.convertToBinaryArray(
                img,
                List.of(Color.ORANGE),
                settings
        );

        // check result is not null
        assertNotNull(result);
        // check array length matches pixel count
        assertEquals(
                (int)(img.getWidth() * img.getHeight()),
                result.length
        );
    }

    @Test
    void testFindClustersReturnsList() {
        // load up test img
        Image img = new Image(
                getClass().getResource("/leaves2.png").toExternalForm()
        );

       // configuring processing settings w/ cluster size limit
        ProcessingSettings settings = new ProcessingSettings();
        settings.minSaturation = 0.1;
        settings.minBrightness = 0.1;
        settings.hueTolerance = 20;
        settings.saturationTolerance = 0.5;
        settings.brightnessTolerance = 0.5;
        settings.minClusterSize = 1;
        settings.maxClusterSize = 100000;

        // convert image to binary array using colour filters
        boolean[] binary = ImageProcessor.convertToBinaryArray(
                img,
                List.of(Color.ORANGE),
                settings
        );

        // finding clusters in the binmary image
        var clusters = ImageProcessor.findClusters(
                binary,
                (int) img.getWidth(),
                (int) img.getHeight(),
                settings
        );

        // check clusters list is not null
        assertNotNull(clusters);
    }

    @Test
    void testDenoiseSizeUnchanged() {
        int width = 5;
        int height = 5;

        // empty boolean array
        boolean[] input = new boolean[width * height];

        // denoise array
        boolean[] output = ImageProcessor.denoise(input, width, height);

        // check that denoise doesn't change array size
        assertEquals(input.length, output.length);
    }

    @Test
    void testSelectedClusterImageNotNull() {
        // load up test img
        Image img = new Image(
                getClass().getResource("/leaves2.png").toExternalForm()
        );

        // configure processing settings with min + max cluster size
        ProcessingSettings settings = new ProcessingSettings();
        settings.minClusterSize = 1;
        settings.maxClusterSize = 100000;

        // convert to binary
        boolean[] binary = ImageProcessor.convertToBinaryArray(
                img,
                List.of(Color.ORANGE),
                settings
        );

        // find clusters
        var clusters = ImageProcessor.findClusters(
                binary,
                (int) img.getWidth(),
                (int) img.getHeight(),
                settings
        );

        // if clusters exist, create image from first cluster and check not null
        if (!clusters.isEmpty()) {
            Image result = ImageProcessor.makeSelectedClusterImage(clusters.get(0).root);
            assertNotNull(result);
        }
    }

    @Test
    void testDenoiseAllWhiteStaysWhite() {
        int width = 5;
        int height = 5;
        boolean[] input = new boolean[width * height];
        // fill all white
        java.util.Arrays.fill(input, true);

        boolean[] output = ImageProcessor.denoise(input, width, height);

        // centre pixels should stay white when surrounded by white
        assertTrue(output[2 * width + 2]);
    }

    @Test
    void testDenoiseIsolatedPixelRemoved() {
        int width = 5;
        int height = 5;
        boolean[] input = new boolean[width * height];
        // single isolated white pixel in centre
        input[2 * width + 2] = true;

        boolean[] output = ImageProcessor.denoise(input, width, height);

        // isolated pixel should be removed by denoise
        assertFalse(output[2 * width + 2]);
    }

    @Test
    void testFindClustersCountsCorrectly() {
        // 3x3 all white = one cluster of 9
        int width = 3;
        int height = 3;
        boolean[] binary = new boolean[width * height];
        java.util.Arrays.fill(binary, true);

        ProcessingSettings settings = new ProcessingSettings();
        settings.minClusterSize = 1;
        settings.maxClusterSize = 100000;

        List<LeafCluster> clusters = ImageProcessor.findClusters(binary, width, height, settings);

        assertEquals(1, clusters.size());
        assertEquals(9, clusters.get(0).pixelCount);
    }
}