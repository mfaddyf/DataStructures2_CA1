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

    @FXML
    private ImageView originalView;

    @FXML
    private ImageView binaryView;

    @FXML
    private Pane overlayPane;

    @FXML
    private Label statusLabel;

    @FXML
    private CheckBox labelsCheckBox;

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
    private final List<Color> sampledLeafColors = new ArrayList<>();
    private final List<SamplePoint> samplePoints = new ArrayList<>();
    private final ProcessingSettings settings = new ProcessingSettings();
    private LeafCluster selectedStartCluster;
    private Timeline tspTimeline;

    @FXML
    public void initialize() {
        originalView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::sampleLeafColor);
    }

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

            if (buffered == null) {
                showError("Could not open image",
                        "This file could not be decoded as an image.\nTry opening it in Paint and saving it again as PNG.");
                return;
            }

            originalImage = SwingFXUtils.toFXImage(buffered, null);
            originalView.setImage(originalImage);

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

    @FXML
    public void detectLeaves() {
        if (originalImage == null) {
            statusLabel.setText("Please open an image first.");
            return;
        }

        updateSettingsFromUI();

        if (binaryPixels == null) {
            binaryPixels = ImageProcessor.convertToBinaryArray(originalImage, sampledLeafColors, settings);
            binaryImage = ImageProcessor.makeBinaryImage(originalImage, sampledLeafColors, settings);
            binaryView.setImage(binaryImage);
        }

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        clusters = ImageProcessor.findClusters(binaryPixels, width, height, settings);
        clusters.sort(Comparator.comparingInt((LeafCluster c) -> c.pixelCount).reversed());

        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).id = i + 1;
        }

        drawClusterBoxes();
        statusLabel.setText("Detected " + clusters.size() + " leaf clusters.");
    }

    @FXML
    public void showRandomClusters() {
        if (clusters == null || clusters.isEmpty()) {
            statusLabel.setText("Detect leaves first.");
            return;
        }

        Image randomImage = ImageProcessor.makeRandomClusterImage(clusters);
        if (randomImage != null) {
            binaryView.setImage(randomImage);
            statusLabel.setText("Showing all disjoint sets in random colours.");
        }
    }

    @FXML
    public void showBinaryAgain() {
        if (binaryImage != null) {
            binaryView.setImage(binaryImage);
            statusLabel.setText("Showing normal black/white image.");
        }
    }

    @FXML
    public void clearSamples() {
        sampledLeafColors.clear();
        samplePoints.clear();
        overlayPane.getChildren().clear();

        if (clusters != null && !clusters.isEmpty()) {
            drawClusterBoxes();
        }

        statusLabel.setText("Cleared sampled colours.");
    }

    @FXML
    public void resetView() {
        if (originalImage == null) {
            return;
        }

        if (tspTimeline != null) {
            tspTimeline.stop();
        }

        binaryImage = null;
        binaryPixels = null;
        clusters.clear();
        sampledLeafColors.clear();
        samplePoints.clear();
        selectedStartCluster = null;
        overlayPane.getChildren().clear();
        binaryView.setImage(null);
        originalView.setImage(originalImage);

        statusLabel.setText("Reset view. Click leaves to sample colours again.");
    }

    @FXML
    public void toggleLabels() {
        drawClusterBoxes();
    }

    @FXML
    public void chooseStartCluster() {
        if (clusters == null || clusters.isEmpty()) {
            statusLabel.setText("Detect leaves first.");
            return;
        }

        statusLabel.setText("Click a cluster rectangle to choose the TSP start cluster.");
    }

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

    private void updateSettingsFromUI() {
        settings.hueTolerance = hueSlider.getValue();
        settings.minSaturation = minSatSlider.getValue();
        settings.minBrightness = minBrightSlider.getValue();
        settings.minClusterSize = (int) minClusterSlider.getValue();
        settings.maxClusterSize = (int) maxClusterSlider.getValue();
    }

    private void sampleLeafColor(MouseEvent event) {
        if (originalImage == null) {
            return;
        }

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

        if (mouseX < offsetX || mouseX > offsetX + displayedWidth ||
                mouseY < offsetY || mouseY > offsetY + displayedHeight) {
            return;
        }

        int imageX = (int) ((mouseX - offsetX) / scale);
        int imageY = (int) ((mouseY - offsetY) / scale);

        if (imageX < 0 || imageY < 0 || imageX >= imgWidth || imageY >= imgHeight) {
            return;
        }

        Color picked = originalImage.getPixelReader().getColor(imageX, imageY);
        sampledLeafColors.add(picked);
        samplePoints.add(new SamplePoint(imageX, imageY));

        drawOverlayOnly();

        statusLabel.setText("Sampled colour " + sampledLeafColors.size()
                + " | hue=" + (int) picked.getHue()
                + " sat=" + String.format("%.2f", picked.getSaturation())
                + " bright=" + String.format("%.2f", picked.getBrightness()));
    }

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

    private void drawOverlayOnly() {
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

        redrawSampleMarkers(offsetX, offsetY, scale);
    }

    private void redrawSampleMarkers(double offsetX, double offsetY, double scale) {
        for (SamplePoint p : samplePoints) {
            Circle marker = new Circle(
                    offsetX + p.x * scale,
                    offsetY + p.y * scale,
                    5
            );
            marker.setFill(Color.TRANSPARENT);
            marker.setStroke(Color.RED);
            marker.setStrokeWidth(2);
            overlayPane.getChildren().add(marker);
        }
    }

    private Stage getStage() {
        return (Stage) originalView.getScene().getWindow();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Image Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static class SamplePoint {
        int x;
        int y;

        SamplePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // ---------------------------
    // HELPER METHODS
    // ---------------------------
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
}