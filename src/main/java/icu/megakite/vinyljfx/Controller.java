package icu.megakite.vinyljfx;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class Controller {
    @FXML
    private Label labelTitle;
    @FXML
    private Label labelArtist;
    @FXML
    private Label labelTimeNow;
    @FXML
    private Label labelTimeRemaining;
    @FXML
    private RadioButton radioButtonList;
    @FXML
    private RadioButton radioButtonLyrics;
    @FXML
    private ListView<Song> listViewSong;
    @FXML
    private ImageView imageViewBackground;
    @FXML
    private ImageView imageViewCover;
    @FXML
    private StackPane stackPaneRoot;
    @FXML
    private StackPane stackPaneAlbumArt;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Slider slider;
    @FXML
    private ProgressBar volumeProgressBar;
    @FXML
    private Slider volumeSlider;
    @FXML
    private Pane paneShadow;
    @FXML
    private Pane paneLyrics;
    @FXML
    private ToggleButton toggleButtonPlayPause;
    @FXML
    private CheckBox checkBoxRandom;
    @FXML
    private CheckBox checkBoxRepeat;
    @FXML
    private VBox vBoxLyrics;
    @FXML
    private Button buttonClose;
    @FXML
    private Button buttonMinimize;
    @FXML
    private ToggleButton toggleButtonFullscreen;
    @FXML
    private final ToggleGroup toggleGroup = new ToggleGroup();

    private static final Interpolator cubicEaseOut = new Interpolator() {
        @Override
        protected double curve(double v) {
            return (pow(v - 1, 3) + 1);
        }
    };
    private static final Interpolator fourierBounce = new Interpolator() {
        @Override
        protected double curve(double v) {
            return ((9.0 / 7.0)
                    * (sin(v * PI / 2)
                    + sin(v * 3 * PI / 2) / 3
                    + sin(v * 5 * PI / 2) / 9));
        }
    };

    private static final Image defaultCover = new Image(new File("defaultCover.png").toURI().toString());
    private static final Config config = Config.getInstance();

    private MediaPlayer mediaPlayer;

    public void initialize() {
        radioButtonList.setToggleGroup(toggleGroup);
        radioButtonLyrics.setToggleGroup(toggleGroup);

        radioButtonList.setSelected(true);
        paneLyrics.setVisible(false);
        listViewSong.setVisible(true);

        vBoxLyrics.prefWidthProperty().bind(paneLyrics.widthProperty().multiply(0.9));
        var clipVBoxLyrics = new Rectangle(stackPaneRoot.getWidth(), stackPaneRoot.getHeight());
        clipVBoxLyrics.widthProperty().bind(paneLyrics.widthProperty());
        clipVBoxLyrics.heightProperty().bind(stackPaneRoot.heightProperty().subtract(100));
        clipVBoxLyrics.yProperty().bind(paneLyrics.translateYProperty().negate().subtract(40 + 36));
        vBoxLyrics.setClip(clipVBoxLyrics);

        imageViewBackground.fitWidthProperty().bind(stackPaneRoot.widthProperty().add(127 * 2));
        imageViewBackground.fitHeightProperty().bind(stackPaneRoot.heightProperty().add(127 * 2));
        imageViewBackground.imageProperty().bind(imageViewCover.imageProperty());
        updateClip();

        paneShadow.prefHeightProperty().bind(paneShadow.widthProperty());
        imageViewCover.fitWidthProperty().bind(paneShadow.widthProperty());

        progressBar.progressProperty().bind(slider.valueProperty().divide(slider.maxProperty()));

        volumeProgressBar.progressProperty().bind(volumeSlider.valueProperty().divide(volumeSlider.maxProperty()));
        volumeSlider.setValue(config.getVolume());

        final var playlistFile = new File("playlist.txt");
        var songs = new ArrayList<Song>();
        try {
            var fileReader = new FileReader(playlistFile, StandardCharsets.UTF_8);
            var bufferedReader = new BufferedReader(fileReader);
            for (;;) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;

                songs.add(new Song(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (volumeSlider.isValueChanging()) {
                mediaPlayer.setVolume(newVal.doubleValue());
                config.setVolume(newVal.doubleValue());
            }
        });

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (slider.isValueChanging())
                mediaPlayer.seek(Duration.millis(newVal.doubleValue()));
        });

        var playlist = FXCollections.observableArrayList(songs);
        listViewSong.setItems(playlist);
        listViewSong.setCellFactory(lv -> new SongCell());
        listViewSong.setOnDragOver(e -> {
            if (e.getGestureSource() != listViewSong)
                e.acceptTransferModes(TransferMode.ANY);

            e.consume();
        });
        listViewSong.setOnDragDropped(e -> {
            var dragboard = e.getDragboard();
            listViewSong
                    .getItems()
                    .addAll(dragboard
                            .getFiles()
                            .stream()
                            .map(f -> new Song(f.toURI().toString()))
                            .collect(Collectors.toList()));
            e.setDropCompleted(true);
            e.consume();

            try {
                FileWriter fileWriter = new FileWriter("playlist.txt", StandardCharsets.UTF_8);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                for (var song : listViewSong.getItems()) {
                    bufferedWriter.write(song.getUri().toString());
                    bufferedWriter.newLine();
                }
                bufferedWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        listViewSong.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.DELETE)
                return;

            listViewSong.getItems().remove(listViewSong.getSelectionModel().getSelectedItem());

            e.consume();
        });

        listViewSong.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null)
                mediaPlayer.dispose();

            var song = listViewSong.getSelectionModel().getSelectedItem();
            var media = new Media(song.getUri().toString());
            labelTitle.setText(song.getTitle());
            labelArtist.setText(song.getArtist());
            imageViewCover.setImage(defaultCover);
            media.getMetadata().addListener((MapChangeListener<String, Object>) c -> {
                if (c.wasAdded()) {
                    if (c.getKey() == "image") {
                        imageViewCover.setImage((Image) c.getValueAdded());
                    }
                }
            });

            vBoxLyrics.getChildren().clear();
            var lyrics =
                    LrcParser.parse(media.getSource().substring(0, media.getSource().lastIndexOf('.')) + ".lrc");
            for (var lyric : lyrics) {
                var label = new Label(lyric.getValue());
                label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));
                label.setTextFill(Paint.valueOf("#ffffff"));
                label.setOpacity(0.5);
                label.setWrapText(true);

                vBoxLyrics.getChildren().add(label);
            }

            toggleButtonPlayPause.setSelected(true);

            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.currentTimeProperty().addListener((obs1, oldVal1, newVal1) -> {
                if (!slider.isValueChanging())
                    slider.setValue(newVal1.toMillis());

                for (int i = 0; i < lyrics.size(); ++i) {
                    var time = lyrics.get(i).getKey();

                    if (oldVal1.lessThan(time) && newVal1.greaterThan(time)) {
                        highlightOfIndex(i);
                        if (i != 0)
                            highlightRevertOfIndex(i);
                    } else if (oldVal1.greaterThan(time) && newVal1.lessThan(time)) {
                        highlightOfIndex(i);
                        if (i != lyrics.size() - 1)
                            highlightRevertOfIndex(i);
                    }
                }

                var remaining = newVal1.subtract(media.getDuration());
                labelTimeNow.setText(toMinutesSeconds(newVal1));
                labelTimeRemaining.setText(toMinutesSeconds(remaining));
            });
            mediaPlayer.statusProperty().addListener((obs1, oldVal1, newVal1) -> {
                if (newVal1 == MediaPlayer.Status.READY)
                    slider.setMax(mediaPlayer.getTotalDuration().toMillis());
            });

            mediaPlayer.setOnEndOfMedia(this::onMediaPlayerEndOfMedia);
            mediaPlayer.setOnPlaying(this::onMediaPlayerPlaying);
            mediaPlayer.setOnPaused(this::onMediaPlayerPaused);
            mediaPlayer.setOnStopped(this::onMediaPlayerStopped);

            mediaPlayer.setVolume(volumeSlider.getValue());

            mediaPlayer.play();
        });
    }

    private String toMinutesSeconds(Duration d) {
        int nowMinutes = (int) d.toMinutes();
        int nowSeconds = (int) abs(d.toSeconds() - nowMinutes * 60);

        var now = new StringBuilder();
        if (nowMinutes > -1 && d.lessThan(Duration.ZERO))
            now.append('-');
        now.append(nowMinutes);
        now.append(':');
        if (nowSeconds < 10)
            now.append(0);
        now.append(nowSeconds);

        return now.toString();
    }

    private void highlightOfIndex(int i) {
        var label = (Label) vBoxLyrics.getChildren().get(i);

        var translatePane = new TranslateTransition(Duration.millis(700), paneLyrics);
        translatePane.setToY(paneLyrics.getHeight() / 2 - label.getLayoutY() - label.getHeight() / 2);
        translatePane.setInterpolator(cubicEaseOut);
        translatePane.play();

        var translate = new TranslateTransition(Duration.millis(700), label);
        translate.setToX(label.getLayoutBounds().getWidth() * 0.05);
        translate.setInterpolator(cubicEaseOut);
        translate.play();

        var scale = new ScaleTransition(Duration.millis(700), label);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setInterpolator(cubicEaseOut);
        scale.play();

        var fade = new FadeTransition(Duration.millis(350), label);
        fade.setToValue(1);
        fade.play();
    }

    private void highlightRevertOfIndex(int i) {
        var label = (Label) vBoxLyrics.getChildren().get(i - 1);

        var translate = new TranslateTransition(Duration.millis(700), label);
        translate.setToX(0);
        translate.setInterpolator(cubicEaseOut);
        translate.play();

        var scale = new ScaleTransition(Duration.millis(700), label);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(cubicEaseOut);
        scale.play();

        var fade = new FadeTransition(Duration.millis(350), label);
        fade.setToValue(0.5);
        fade.play();
    }

    public void onRadioButtonListAction() {
        paneLyrics.setVisible(false);
        listViewSong.setVisible(true);
        listViewSong.refresh();
    }

    public void onRadioButtonLyricsAction() {
        paneLyrics.setVisible(true);
        listViewSong.setVisible(false);
    }

    public void onToggleButtonPlayPauseAction() {
        if (toggleButtonPlayPause.isSelected()) {
            if (mediaPlayer == null)
                listViewSong.getSelectionModel().selectFirst();

            mediaPlayer.play();
        } else {
            mediaPlayer.pause();
        }
    }

    public void onButtonNextAction() {
        listViewSong.getSelectionModel().selectNext();
    }

    public void onButtonPrevAction() {
        listViewSong.getSelectionModel().selectPrevious();
    }

    public void onButtonCloseAction() {
        ((Stage) buttonClose.getScene().getWindow()).close();
    }

    public void onButtonMinimizeAction() {
        ((Stage) buttonMinimize.getScene().getWindow()).setIconified(true);
    }

    public void onToggleButtonFullscreenAction() {
        if (toggleButtonFullscreen.isSelected()) {
            stackPaneRoot.setId("root-pane-maximized");
            stackPaneRoot.setPadding(new Insets(0));
            ((Stage) toggleButtonFullscreen.getScene().getWindow()).setMaximized(true);
            imageViewBackground.setClip(null);
        } else {
            stackPaneRoot.setId("root-pane");
            stackPaneRoot.setPadding(new Insets(32, 50, 68, 50));
            ((Stage) toggleButtonFullscreen.getScene().getWindow()).setMaximized(false);
            updateClip();
        }
    }

    private void updateClip() {
        var clipImageViewBackground = new Rectangle(127 + 50, 127 + 50, 800, 600);
        clipImageViewBackground.widthProperty().bind(stackPaneRoot.widthProperty().subtract(100));
        clipImageViewBackground.heightProperty().bind(stackPaneRoot.heightProperty().subtract(100));
        clipImageViewBackground.setArcHeight(9 * 2);
        clipImageViewBackground.setArcWidth(9 * 2);
        imageViewBackground.setClip(clipImageViewBackground);
    }

    public void onMediaPlayerPaused() {
        var scale = new ScaleTransition(Duration.millis(500), stackPaneAlbumArt);
        scale.setInterpolator(cubicEaseOut);
        scale.setToX(0.7);
        scale.setToY(0.7);
        scale.play();
    }

    public void onMediaPlayerPlaying() {
        var scale = new ScaleTransition(Duration.millis(700), stackPaneAlbumArt);
        scale.setInterpolator(fourierBounce);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    public void onMediaPlayerStopped() {
        imageViewCover.setImage(defaultCover);

        var scale = new ScaleTransition(Duration.millis(500), stackPaneAlbumArt);
        scale.setInterpolator(cubicEaseOut);
        scale.setToX(0.7);
        scale.setToY(0.7);
        scale.play();
    }

    public void onMediaPlayerEndOfMedia() {
        if (checkBoxRepeat.isSelected()) {
            mediaPlayer.seek(Duration.ZERO);
        } else if (checkBoxRepeat.isIndeterminate()) {
            if (listViewSong.getSelectionModel().getSelectedIndex() == listViewSong.getItems().size() - 1) {
                listViewSong.getSelectionModel().selectFirst();
            } else {
                listViewSong.getSelectionModel().selectNext();
            }
        } else if (checkBoxRandom.isSelected()) {
            int size = listViewSong.getItems().size();
            int rndIdx = (int) (Math.random() * size);
            listViewSong.getSelectionModel().select(rndIdx);
        } else {
            if (listViewSong.getSelectionModel().getSelectedIndex() == listViewSong.getItems().size() - 1) {
                listViewSong.getSelectionModel().selectFirst();
                mediaPlayer.dispose();
                toggleButtonPlayPause.setSelected(false);
                var scale = new ScaleTransition(Duration.millis(500), stackPaneAlbumArt);
                scale.setInterpolator(cubicEaseOut);
                scale.setToX(0.7);
                scale.setToY(0.7);
                scale.play();
            } else {
                listViewSong.getSelectionModel().selectNext();
            }
        }
    }
}
