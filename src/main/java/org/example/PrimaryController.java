package org.example;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PrimaryController {

    // ui elements
    @FXML
    private ImageView originalView; // original image display
    @FXML
    private ImageView binaryView; // binary img display
    @FXML
    private Pane overlayPane; // draws the overlay on the images (boxes, markers, etc)
    @FXML
    private Label statusLabel; // status messages for the user
    @FXML
    private CheckBox labelsCheckBox; // allows for toggle of cluster labels
    @FXML
    private Slider hueSlider;
    @FXML
    private Slider minSatSlider;
    @FXML
    private Slider minBrightSlider;
    @FXML
    private Slider minClusterSlider;
    @FXML
    private Slider maxClusterSlider;
    private Image originalImage;
    private Image binaryImage;
    private boolean[] binaryPixels;
    private List<LeafCluster> clusters = new ArrayList<>();
    // sample colours
    private List<Color> sampledLeafColors = new ArrayList<>();
    private List<SamplePoint> samplePoints = new ArrayList<>();
    private ProcessingSettings settings = new ProcessingSettings();
    // tsp related
    private LeafCluster selectedStartCluster;
    private Timeline tspTimeline;

    /**
     * called automatically when the ui is loaded
     * registers mouse clicking for sampling leaf colours
     */
    @FXML
    public void initialize() {
        originalView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::sampleLeafColor);
    }

    /**
     * opens an image file and loads it into the ui
     */
    @FXML
    public void openImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = chooser.showOpenDialog(getStage());
        if (file == null) {
            return;
        }

        try {
            BufferedImage buffered = ImageIO.read(file);

            // validating the image decoding
            if (buffered == null) {
                showError("Could not open image",
                        "This file could not be decoded as an image.\nTry opening it in Paint and saving it again as PNG.");
                return;
            }

            // converting to javafx image
            originalImage = SwingFXUtils.toFXImage(buffered, null);
            originalView.setImage(originalImage);

            // clear/reset all the processing states
            binaryImage = null;
            binaryPixels = null;
            clusters.clear();
            sampledLeafColors.clear();
            samplePoints.clear();
            overlayPane.getChildren().clear();
            binaryView.setImage(null);

            statusLabel.setText("Loaded image: " + file.getName() + ". Click leaves to sample colours.");
        } catch (Exception ex) {
            showError("Could not open image", ex.toString());
        }
    }

    /**
     * converts the original image into a binary (black and white) version using
     * thresholds , sampled leaf colours (if chosen)
     */
    @FXML
    public void convertToBinary() {
        if (originalImage == null) {
            statusLabel.setText("Please open an image first.");
            return;
        }

        updateSettingsFromUI();

        binaryPixels = ImageProcessor.convertToBinaryArray(originalImage, sampledLeafColors, settings);
        binaryImage = ImageProcessor.makeBinaryImage(originalImage, sampledLeafColors, settings);
        binaryView.setImage(binaryImage);

        statusLabel.setText("Converted to black/white using " + sampledLeafColors.size() + " sampled colour(s).");
    }

    /**
     * detects connected clusters from the binary img
     */
    @FXML
    public void detectLeaves() {
        if (originalImage == null) {
            statusLabel.setText("Please open an image first.");
            return;
        }

        updateSettingsFromUI();

        // ensures that binary img exists
        if (binaryPixels == null) {
            binaryPixels = ImageProcessor.convertToBinaryArray(originalImage, sampledLeafColors, settings);
            binaryImage = ImageProcessor.makeBinaryImage(originalImage, sampledLeafColors, settings);
            binaryView.setImage(binaryImage);
        }

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        // find connected clusters
        clusters = ImageProcessor.findClusters(binaryPixels, width, height, settings);
        // sort clusters by size ( largest -> smallest )
        clusters.sort(Comparator.comparingInt((LeafCluster c) -> c.pixelCount).reversed());
        // assigning  ids to clusters ( largest = 1 -> smallest = whatevaaaaa )
        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).id = i + 1;
        }

        drawClusterBoxes();
        statusLabel.setText("Detected " + clusters.size() + " leaf clusters.");
    }

    /**
     * displays each cluster in a random colour
     */
    @FXML
    public void showRandomClusters() {
        // if no clusters detected first, error msg
        if (clusters == null || clusters.isEmpty()) {
            statusLabel.setText("Detect leaves first.");
            return;
        }

        // if img detected, colour randomly from ImageProcessor + display new text
        Image randomImage = ImageProcessor.makeRandomClusterImage(clusters);
        if (randomImage != null) {
            binaryView.setImage(randomImage);
            statusLabel.setText("Showing all disjoint sets in random colours.");
        }
    }

    /**
     * restores the og binary ( black / white ) view
     */
    @FXML
    public void showBinaryAgain() {
        if (binaryImage != null) {
            binaryView.setImage(binaryImage);
            statusLabel.setText("Showing normal black/white image.");
        }
    }

    /**
     * drqws the bounding boxes around detected clusters
     */
    private void drawClusterBoxes() {
        overlayPane.getChildren().clear();

        if (originalImage == null) {
            return;
        }

        double imgWidth = originalImage.getWidth();
        double imgHeight = originalImage.getHeight();

        double viewWidth = originalView.getFitWidth();
        double viewHeight = originalView.getFitHeight();

        double scale = Math.min(viewWidth / imgWidth, viewHeight / imgHeight);
        double displayedWidth = imgWidth * scale;
        double displayedHeight = imgHeight * scale;

        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;

        if (clusters != null) {
            for (LeafCluster cluster : clusters) {
                Rectangle rect = new Rectangle(
                        offsetX + cluster.minX * scale,
                        offsetY + cluster.minY * scale,
                        Math.max(1, cluster.getBoxWidth() * scale),
                        Math.max(1, cluster.getBoxHeight() * scale)
                );
                rect.setFill(Color.TRANSPARENT);
                rect.setStroke(Color.BLUE);
                rect.setStrokeWidth(2);

                // clicking on the selected cluster
                LeafCluster clicked = cluster;
                rect.setOnMouseClicked(e -> {
                    selectedStartCluster = clicked;

                    statusLabel.setText(
                            "Cluster " + clicked.id +
                                    " selected | pixels: " + clicked.pixelCount +
                                    " | box: " + clicked.getBoxWidth() + "x" + clicked.getBoxHeight()
                    );

                    Image selected = ImageProcessor.makeSelectedClusterImage(clicked.root);
                    if (selected != null) {
                        binaryView.setImage(selected);
                    }

                    e.consume();
                });

                overlayPane.getChildren().add(rect);

                // optional label displayed with rectangled cluster
                if (labelsCheckBox.isSelected()) {
                    Text label = new Text(
                            offsetX + cluster.minX * scale,
                            Math.max(12, offsetY + cluster.minY * scale - 2),
                            String.valueOf(cluster.id)
                    );
                    label.setFill(Color.BLUE);
                    label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                    overlayPane.getChildren().add(label);
                }
            }
        }

        redrawSampleMarkers(offsetX, offsetY, scale);
    }

    /**
     * clears all sample colours and visual markers
     * keeps detected clusters and redraws them
     */
    @FXML
    public void clearSamples() {
        sampledLeafColors.clear();
        samplePoints.clear();
        overlayPane.getChildren().clear();

        // redraw cluster boxes if they exist
        if (clusters != null && !clusters.isEmpty()) {
            drawClusterBoxes();
        }

        statusLabel.setText("Cleared sampled colours.");
    }

    /**
     * fully resets the application to the initial launch
     * stops EVERYTHING.
     */
    @FXML
    public void resetView() {
        if (originalImage == null) {
            return;
        }
        // stops any animation running
        if (tspTimeline != null) {
            tspTimeline.stop();
        }

        // resets all processing data + uui overlays
        binaryImage = null;
        binaryPixels = null;
        clusters.clear();
        sampledLeafColors.clear();
        samplePoints.clear();
        selectedStartCluster = null;
        overlayPane.getChildren().clear();
        binaryView.setImage(null);
        // restores og img
        originalView.setImage(originalImage);

        statusLabel.setText("Reset view. Click leaves to sample colours again.");
    }

    /**
     * toggles cluster labels on/off
     * redraws clusters depending on state of the checkbox
     */
    @FXML
    public void toggleLabels() {
        drawClusterBoxes();
    }

    /**
     * animates a tsp ( travelling salesman ) path in between the clusters
     */
    @FXML
    public void animateTspPath() {
        if (clusters == null || clusters.isEmpty()) {
            statusLabel.setText("Detect leaves first.");
            return;
        }

        if (selectedStartCluster == null) {
            statusLabel.setText("Choose a start cluster first.");
            return;
        }

        drawClusterBoxes();

        List<LeafCluster> path = TspSolver.nearestNeighbourPath(clusters, selectedStartCluster);
        if (path.size() < 2) {
            statusLabel.setText("Not enough clusters to animate a path.");
            return;
        }

        if (tspTimeline != null) {
            tspTimeline.stop();
        }

        double totalMillis = 5000.0;
        double stepMillis = totalMillis / (path.size() - 1);

        tspTimeline = new Timeline();

        for (int i = 1; i < path.size(); i++) {
            LeafCluster from = path.get(i - 1);
            LeafCluster to = path.get(i);

            int stepIndex = i;

            KeyFrame frame = new KeyFrame(Duration.millis(stepIndex * stepMillis), e -> {
                drawAnimatedStep(from, to);
                statusLabel.setText("Animating path: " + stepIndex + " / " + (path.size() - 1));
            });

            tspTimeline.getKeyFrames().add(frame);
        }

        tspTimeline.setOnFinished(e ->
                statusLabel.setText("TSP animation finished from cluster " + selectedStartCluster.id + ".")
        );

        tspTimeline.play();
    }

    /**
     * draws one animated step / line between the clusters
     */
    private void drawAnimatedStep(LeafCluster from, LeafCluster to) {
        if (originalImage == null) {
            return;
        }

        double imgWidth = originalImage.getWidth();
        double imgHeight = originalImage.getHeight();

        double viewWidth = originalView.getFitWidth();
        double viewHeight = originalView.getFitHeight();

        double scale = Math.min(viewWidth / imgWidth, viewHeight / imgHeight);
        double displayedWidth = imgWidth * scale;
        double displayedHeight = imgHeight * scale;

        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;

        double x1 = offsetX + from.getCenterX() * scale;
        double y1 = offsetY + from.getCenterY() * scale;
        double x2 = offsetX + to.getCenterX() * scale;
        double y2 = offsetY + to.getCenterY() * scale;

        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.YELLOW);
        line.setStrokeWidth(3);

        Rectangle highlight = new Rectangle(
                offsetX + to.minX * scale,
                offsetY + to.minY * scale,
                Math.max(1, to.getBoxWidth() * scale),
                Math.max(1, to.getBoxHeight() * scale)
        );
        highlight.setFill(Color.TRANSPARENT);
        highlight.setStroke(Color.YELLOW);
        highlight.setStrokeWidth(3);

        overlayPane.getChildren().add(line);
        overlayPane.getChildren().add(highlight);
    }

    /**
     * updates the processing settings from the ui sliders ( brightness , saturation , etc )
     */
    private void updateSettingsFromUI() {
        settings.hueTolerance = hueSlider.getValue();
        settings.minSaturation = minSatSlider.getValue();
        settings.minBrightness = minBrightSlider.getValue();
        settings.minClusterSize = (int) minClusterSlider.getValue();
        settings.maxClusterSize = (int) maxClusterSlider.getValue();
    }

    /**
     * allow the user to click the image and sample a leaf colour
     */
    private void sampleLeafColor(MouseEvent event) {
        if (originalImage == null) {
            return;
        }

        // lots and lots...... and lots........... of maths........... brain melted BUT IT WORKS
        // converts mouse co-ords to img co-ords
        double imgWidth = originalImage.getWidth();
        double imgHeight = originalImage.getHeight();

        Bounds bounds = originalView.getBoundsInLocal();
        double viewWidth = bounds.getWidth();
        double viewHeight = bounds.getHeight();

        double scale = Math.min(viewWidth / imgWidth, viewHeight / imgHeight);
        double displayedWidth = imgWidth * scale;
        double displayedHeight = imgHeight * scale;

        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;

        Point2D point = originalView.sceneToLocal(event.getSceneX(), event.getSceneY());
        double mouseX = point.getX();
        double mouseY = point.getY();

        int imageX = (int) ((mouseX - offsetX) / scale);
        int imageY = (int) ((mouseY - offsetY) / scale);

        // bounds checking
        if (imageX < 0 || imageY < 0 || imageX >= imgWidth || imageY >= imgHeight) {
            return;
        }

        // getting that pixel colour
        Color picked = originalImage.getPixelReader().getColor(imageX, imageY);
        sampledLeafColors.add(picked);
        samplePoints.add(new SamplePoint(imageX, imageY));

        drawOverlayOnly();

        statusLabel.setText("Sampled colour " + sampledLeafColors.size()
                + " | hue=" + (int) picked.getHue()
                + " sat=" + String.format("%.2f", picked.getSaturation())
                + " bright=" + String.format("%.2f", picked.getBrightness()));
    }

    /**
     * refraws only the overlay layer which includes sample markers without drawing cluster boxes
     * used when the user is sampling colours so only the markers have to be redrawn, not everythingg
     */
    private void drawOverlayOnly() {
        // clears any existing overlay
        overlayPane.getChildren().clear();

        // checks if img is loaded
        if (originalImage == null) {
            return;
        }

        // og image dimensions
        double imgWidth = originalImage.getWidth();
        double imgHeight = originalImage.getHeight();
        // image view display size
        double viewWidth = originalView.getFitWidth();
        double viewHeight = originalView.getFitHeight();
        // compute the scale factor to fit inside the image view
        double scale = Math.min(viewWidth / imgWidth, viewHeight / imgHeight);
        // compute displayed img size after scaling
        double displayedWidth = imgWidth * scale;
        double displayedHeight = imgHeight * scale;
        // computing offsets for centering inside img view
        double offsetX = (viewWidth - displayedWidth) / 2.0;
        double offsetY = (viewHeight - displayedHeight) / 2.0;
        // draw sample markers at correct scaled positions
        redrawSampleMarkers(offsetX, offsetY, scale);
    }

    /**
     * draws red circle markers for each sample
     * @param offsetY vertical offset of disp image
     * @param offsetX horizontal '' ''
     * @param scale scale factor from img -> view coords
     */
    private void redrawSampleMarkers(double offsetX, double offsetY, double scale) {
        for (SamplePoint p : samplePoints) {
            // converting image coords -> screen coords
            Circle marker = new Circle(
                    offsetX + p.x * scale,
                    offsetY + p.y * scale,
                    5 // fixed radius for visibility
            );
            // style marker
            marker.setFill(Color.TRANSPARENT);
            marker.setStroke(Color.RED);
            marker.setStrokeWidth(2);
            // adding to overlay
            overlayPane.getChildren().add(marker);
        }
    }

    private Stage getStage() {
        return (Stage) originalView.getScene().getWindow();
    }

    /**
     * displays error dialog to the user
     * @param header short title / sum
     * @param content actual error msg
     */
    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Image Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * represents sampled pixel location in the og img
     * used to redraw markers and track selected colours
     */
    private static class SamplePoint {
        int x;
        int y;

        SamplePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

}