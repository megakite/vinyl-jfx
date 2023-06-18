package icu.megakite.vinyljfx;

import javafx.collections.MapChangeListener;
import javafx.scene.image.Image;
import javafx.scene.media.Media;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class Song implements Serializable {
    private String title;
    private String artist;
    private Image image;
    private final URI uri;

    Song(String s) {
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        var path = uri.getPath();

        title = path.substring(path.lastIndexOf('/') + 1);
        artist = "<Unknown Artist>";
        label: try {
            var url = uri.toURL().toString();
            var partialPath = url.substring(0, url.lastIndexOf('/') + 1) + "cover";

            image = new Image(partialPath + ".jpg");
            if (!image.isError())
                break label;
            image = new Image(partialPath + ".png");
            if (!image.isError())
                break label;
            image = new Image(partialPath + ".bmp");
            if (!image.isError())
                break label;

            image = null;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        var file = new File(path);
        if (file.exists()) {
            getMetadata(uri.toString());
        }
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public Image getImage() {
        return image;
    }

    public URI getUri() {
        return uri;
    }

    private void getMetadata(String s) {
        var metadata = new Media(s).getMetadata();
        metadata.addListener((MapChangeListener<String, Object>) c -> {
            if (c.wasAdded()) {
                var key = c.getKey();
                var value = c.getValueAdded();

                switch (key) {
                case "title":
                    title = (String) value;
                    break;
                case "artist":
                    artist = (String) value;
                    break;
                case "image":
                    image = (Image) value;
                }
            }
        });
    }
}
