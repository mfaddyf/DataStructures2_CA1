package org.example;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class AdjustWindow {

    private final Image original;
    private final ImageView targetView;

    public AdjustWindow(Image img, ImageView target) {
        this.original = img;
        this.targetView = target;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Adjust Image");

        Slider hue = new Slider(-1, 1, 0);
        Slider sat = new Slider(-1, 1, 0);
        Slider bright = new Slider(-1, 1, 0);

        VBox box = new VBox(10,
                new Label("Hue"), hue,
                new Label("Saturation"), sat,
                new Label("Brightness"), bright
        );
        box.setPadding(new Insets(10));

        // Update image whenever sliders move
        hue.valueProperty().addListener((a, b, c) -> apply(hue, sat, bright));
        sat.valueProperty().addListener((a, b, c) -> apply(hue, sat, bright));
        bright.valueProperty().addListener((a, b, c) -> apply(hue, sat, bright));

        stage.setScene(new Scene(box, 300, 300));
        stage.show();
    }

    private void apply(Slider hue, Slider sat, Slider bright) {
        int w = (int) original.getWidth();
        int h = (int) original.getHeight();

        WritableImage out = new WritableImage(w, h);
        PixelReader pr = original.getPixelReader();
        PixelWriter pw = out.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = pr.getColor(x, y);

                Color adj = c.deriveColor(
                        hue.getValue() * 360,   // hue shift
                        1 + sat.getValue(),     // saturation scale
                        1 + bright.getValue(),  // brightness scale
                        1                       // opacity
                );

                pw.setColor(x, y, adj);
            }
        }

        targetView.setImage(out);
    }
}
