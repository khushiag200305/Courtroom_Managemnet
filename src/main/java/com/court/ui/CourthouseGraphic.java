package com.court.ui;

import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * A courthouse front (pediment, six pillars and steps) drawn with JavaFX shapes,
 * so the app needs no image files. Colours come from CSS (.courthouse-shape, .courthouse-seal).
 * <p>
 * Usable from FXML: {@code <CourthouseGraphic size="300"/>}.
 */
public class CourthouseGraphic extends Group {

    /** Drawing is designed on a 300 x 120 grid and scaled to {@link #size}. */
    private static final double BASE_WIDTH = 300;

    private double size = BASE_WIDTH;

    public CourthouseGraphic() {
        draw();
    }

    public double getSize() {
        return size;
    }

    /** Width in pixels; the height follows (about 40% of the width). */
    public void setSize(double size) {
        this.size = size;
        draw();
    }

    private void draw() {
        double k = size / BASE_WIDTH;
        getChildren().clear();

        // Roof (pediment) and the beam under it
        add(new Polygon(150 * k, 6 * k, 38 * k, 42 * k, 262 * k, 42 * k));
        add(new Rectangle(32 * k, 42 * k, 236 * k, 8 * k));

        // Six pillars
        for (int i = 0; i < 6; i++) {
            double x = (52 + i * 36.4) * k;
            add(new Rectangle(x - 2 * k, 53 * k, 18 * k, 4 * k));    // capital
            add(new Rectangle(x, 57 * k, 14 * k, 42 * k));           // shaft
        }

        // Steps
        add(new Rectangle(40 * k, 99 * k, 220 * k, 6 * k));
        add(new Rectangle(28 * k, 105 * k, 244 * k, 6 * k));
        add(new Rectangle(16 * k, 111 * k, 268 * k, 6 * k));

        // Round seal in the roof
        Circle seal = new Circle(150 * k, 29 * k, 7 * k);
        seal.getStyleClass().add("courthouse-seal");
        getChildren().add(seal);
    }

    private void add(Shape shape) {
        shape.getStyleClass().add("courthouse-shape");
        getChildren().add(shape);
    }
}
