package icu.megakite.vinyljfx;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
    private Pane paneImageView;
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
            return ((9 / 7.)
                    * (sin(v * PI / 2)
                    + sin(v * 3 * PI / 2) / 3
                    + sin(v * 5 * PI / 2) / 9));
        }
    };
    private static final Interpolator bounceToOriginal = new Interpolator() {
        @Override
        protected double curve(double v) {
            return -sin(PI * v) * exp(-5 * v);
        }
    };

    @SuppressWarnings("ConstantConditions")
    private static final Image defaultCover = new Image(VinylJFX.class.getResourceAsStream("images/defaultCover.png"));
    private static final Config config = Config.getInstance();

    private MediaPlayer mediaPlayer;

    public void initialize() {
        // Setup toggle group
        radioButtonList.setToggleGroup(toggleGroup);
        radioButtonLyrics.setToggleGroup(toggleGroup);

        // Default startup screen to lyrics view
        radioButtonLyrics.setSelected(true);
        listViewSong.setVisible(false);

        // Setup clip for lyrics box & (blurred) background image
        vBoxLyrics.prefWidthProperty().bind(paneLyrics.widthProperty().multiply(0.9));
        imageViewBackground.xProperty().set(-128);
        imageViewBackground.yProperty().set(-128);
        imageViewBackground.fitWidthProperty().bind(paneImageView.widthProperty().add(128 * 2));
        imageViewBackground.fitHeightProperty().bind(paneImageView.heightProperty().add(128 * 2));
        imageViewBackground.imageProperty().bind(imageViewCover.imageProperty());
        updateClip();

        // Setup shadow for cover image
        paneShadow.prefHeightProperty().bind(paneShadow.widthProperty());
        imageViewCover.fitWidthProperty().bind(paneShadow.widthProperty());

        // Normalize progress bar & volume bar
        progressBar.progressProperty().bind(slider.valueProperty().divide(slider.maxProperty()));
        volumeProgressBar.progressProperty().bind(volumeSlider.valueProperty().divide(volumeSlider.maxProperty()));

        // Get volume from config file
        volumeSlider.setValue(config.getVolume());

        // Set behaviors for volume & progress slider
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer == null)
                return;

            if (volumeSlider.isValueChanging()) {
                mediaPlayer.setVolume(newVal.doubleValue());
                config.setVolume(newVal.doubleValue());
            }
        });
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer == null)
                return;

            if (slider.isValueChanging())
                mediaPlayer.seek(Duration.millis(newVal.doubleValue()));
        });

        // Initialize playlist collection from file
        var songs = new ArrayList<Song>();
        try {
            readFromPlaylist(songs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Initialize items & behaviors for playlist view
        var playlist = FXCollections.observableArrayList(songs);
        listViewSong.setItems(playlist);
        listViewSong.setCellFactory(lv -> new SongCell());
        listViewSong.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> play(newVal));
    }

    @FXML
    protected void onListViewSongDragOver(DragEvent e) {
        if (e.getGestureSource() != listViewSong)
            e.acceptTransferModes(TransferMode.ANY);

        e.consume();
    }

    @FXML
    protected void onListViewSongDragDropped(DragEvent e) {
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
            updatePlaylist();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    protected void onListViewSongKeyPressed(KeyEvent e) {
        if (e.getCode() != KeyCode.DELETE)
            return;

        listViewSong.getItems().remove(listViewSong.getSelectionModel().getSelectedItem());

        e.consume();
    }

    @FXML
    protected void onStackPaneProgressMouseEntered() {
        scaleTo(progressBar, 1.02, 1.6);
        scaleTo(slider, 1.02, 1.6);
    }

    @FXML
    protected void onStackPaneProgressMouseExited() {
        scaleTo(progressBar, 1, 1);
        scaleTo(slider, 1, 1);
    }

    @FXML
    protected void onStackPaneVolumeMouseEntered() {
        scaleTo(volumeProgressBar, 1.02, 1.6);
        scaleTo(volumeSlider, 1.02, 1.6);
    }

    @FXML
    protected void onStackPaneVolumeMouseExited() {
        scaleTo(volumeProgressBar, 1, 1);
        scaleTo(volumeSlider, 1, 1);
    }

    @FXML
    protected void onArbitraryNodeMouseEntered(MouseEvent e) {
        scaleTo((Node) e.getSource(), 1.16, 1.16);

        e.consume();
    }

    @FXML
    protected void onArbitraryNodeMouseExited(MouseEvent e) {
        scaleTo((Node) e.getSource(), 1, 1);

        e.consume();
    }

    @FXML
    protected void onArbitraryNodeMouseClicked(MouseEvent e) {
        var scale = new ScaleTransition(Duration.millis(500), (Node) e.getSource());
        scale.setInterpolator(bounceToOriginal);
        scale.setByX(1);
        scale.setByY(1);
        scale.play();

        e.consume();
    }

    @FXML
    protected void onRadioButtonListAction() {
        paneLyrics.setVisible(false);
        listViewSong.setVisible(true);
        listViewSong.refresh();
    }

    @FXML
    protected void onRadioButtonLyricsAction() {
        paneLyrics.setVisible(true);
        listViewSong.setVisible(false);
    }

    @FXML
    protected void onToggleButtonPlayPauseAction() {
        if (toggleButtonPlayPause.isSelected()) {
            if (mediaPlayer == null)
                listViewSong.getSelectionModel().selectFirst();

            mediaPlayer.play();
        } else {
            mediaPlayer.pause();
        }
    }

    @FXML
    protected void onButtonNextAction() {
        listViewSong.getSelectionModel().selectNext();
    }

    @FXML
    protected void onButtonPrevAction() {
        listViewSong.getSelectionModel().selectPrevious();
    }

    @FXML
    protected void onButtonCloseAction() {
        ((Stage) buttonClose.getScene().getWindow()).close();
    }

    @FXML
    protected void onButtonMinimizeAction() {
        ((Stage) buttonMinimize.getScene().getWindow()).setIconified(true);
    }

    @FXML
    protected void onToggleButtonFullscreenAction() {
        if (toggleButtonFullscreen.isSelected()) {
            stackPaneRoot.setId("root-pane-maximized");
            stackPaneRoot.setPadding(Insets.EMPTY);
            ((Stage) toggleButtonFullscreen.getScene().getWindow()).setMaximized(true);

            paneImageView.setClip(null);
            vBoxLyrics.setClip(null);
        } else {
            stackPaneRoot.setId("root-pane");
            stackPaneRoot.setPadding(new Insets(32, 50, 68, 50));
            ((Stage) toggleButtonFullscreen.getScene().getWindow()).setMaximized(false);

            updateClip();
        }
    }

    private void readFromPlaylist(ArrayList<Song> songs) throws IOException {
        var etc = new File("etc");
        var playlistFile = new File("etc", "playlist.txt");
        if (!playlistFile.exists() && !etc.mkdir() && !playlistFile.createNewFile())
            throw new IOException("Cannot create playlist file");

        var fileReader = new FileReader(playlistFile, StandardCharsets.UTF_8);
        var bufferedReader = new BufferedReader(fileReader);
        for (;;) {
            String line = bufferedReader.readLine();
            if (line == null)
                break;

            songs.add(new Song(line));
        }
    }

    private void updatePlaylist() throws IOException {
        var playlistFile = new File("etc", "playlist.txt");
        FileWriter fileWriter = new FileWriter(playlistFile, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        for (var song : listViewSong.getItems()) {
            bufferedWriter.write(song.getUri().toString());
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
    }

    private void scaleTo(Node n, double x, double y) {
        var scale = new ScaleTransition(Duration.millis(350), n);
        scale.setInterpolator(cubicEaseOut);
        scale.setToX(x);
        scale.setToY(y);
        scale.play();
    }

    private void onMediaPlayerPaused() {
        var scale = new ScaleTransition(Duration.millis(500), stackPaneAlbumArt);
        scale.setInterpolator(cubicEaseOut);
        scale.setToX(0.7);
        scale.setToY(0.7);
        scale.play();
    }

    private void onMediaPlayerPlaying() {
        var scale = new ScaleTransition(Duration.millis(700), stackPaneAlbumArt);
        scale.setInterpolator(fourierBounce);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void onMediaPlayerStopped() {
        imageViewCover.setImage(defaultCover);

        var scale = new ScaleTransition(Duration.millis(350), stackPaneAlbumArt);
        scale.setInterpolator(cubicEaseOut);
        scale.setToX(0.7);
        scale.setToY(0.7);
        scale.play();
    }

    private void onMediaPlayerEndOfMedia() {
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

    /**
     * Play the specified song.
     * @param s Song
     */
    private void play(Song s) {
        if (s == null)
            return;

        if (mediaPlayer != null)
            mediaPlayer.dispose();

        // Get metadata
        labelTitle.setText(s.getTitle());
        labelArtist.setText(s.getArtist());
        if (s.getImage() == null) {
            imageViewCover.setImage(defaultCover);
        } else {
            imageViewCover.setImage(s.getImage());
        }

        // Fill up lyrics view
        var rawPath = s.getUri().getRawPath();
        var lyrics = LrcParser.parse(rawPath.substring(0, rawPath.lastIndexOf('.')) + ".lrc");
        vBoxLyrics.getChildren().clear();
        for (var line : lyrics) {
            var label = new Label(line.getValue());
            label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 28));
            label.setTextFill(Paint.valueOf("#ffffff"));
            label.setOpacity(0.5);
            label.setWrapText(true);

            vBoxLyrics.getChildren().add(label);
        }

        // Toggle playing state
        toggleButtonPlayPause.setSelected(true);

        // Setup media player behaviours
        var media = new Media(s.getUri().toString());
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

        // Play the song!
        mediaPlayer.play();
    }

    /**
     * Convert from duration to its natural representation in String.
     *
     * @param d Arbitrary duration (negative allowed)
     * @return A string in the form of {@code h:mm:ss}
     */
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

    private void updateClip() {
        var clipVBoxLyrics = new Rectangle(stackPaneRoot.getWidth(), stackPaneRoot.getHeight());
        clipVBoxLyrics.widthProperty().bind(paneLyrics.widthProperty());
        clipVBoxLyrics.heightProperty().bind(stackPaneRoot.heightProperty().subtract(100));
        clipVBoxLyrics.yProperty().bind(paneLyrics.translateYProperty().negate().subtract(40 + 40));
        vBoxLyrics.setClip(clipVBoxLyrics);

        var clipImageViewBackground = new Rectangle();
        clipImageViewBackground.widthProperty().bind(stackPaneRoot.widthProperty().subtract(100));
        clipImageViewBackground.heightProperty().bind(stackPaneRoot.heightProperty().subtract(100));
        clipImageViewBackground.setArcHeight(9 * 2);
        clipImageViewBackground.setArcWidth(9 * 2);
        paneImageView.setClip(clipImageViewBackground);
    }
}
