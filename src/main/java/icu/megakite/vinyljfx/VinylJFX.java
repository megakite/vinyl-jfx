package icu.megakite.vinyljfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class VinylJFX extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    @SuppressWarnings("ConstantConditions")
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(VinylJFX.class.getResource("vinyl-main.fxml"));
        Parent root = fxmlLoader.load();

        // Make the window draggable
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            if (stage.isMaximized())
                return;

            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root, 800 + 50 * 2, 600 + 50 * 2);
        scene.setFill(Color.TRANSPARENT);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Vinyl JFX v0.1.0");
        stage.getIcons().addAll(
                new Image(VinylJFX.class.getResourceAsStream("icons/vinyl-jfx-16.png")),
                new Image(VinylJFX.class.getResourceAsStream("icons/vinyl-jfx-24.png")),
                new Image(VinylJFX.class.getResourceAsStream("icons/vinyl-jfx-32.png")),
                new Image(VinylJFX.class.getResourceAsStream("icons/vinyl-jfx-48.png")),
                new Image(VinylJFX.class.getResourceAsStream("icons/vinyl-jfx-256.png")));
        stage.setScene(scene);
        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
    }

    public static void main(String[] args) {
        launch(args);
    }
}