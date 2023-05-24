package icu.megakite.vinyljfx;

import javafx.collections.MapChangeListener;
import javafx.scene.media.Media;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class Song implements Serializable {
    private String title;
    private String artist;
    private String album;
    private URI uri;

    public Song(String s) {
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        var path = uri.getPath();
        title = path.substring(path.lastIndexOf('/') + 1);
        artist = "<Unknown Artist>";
        album = "<Unknown Album>";

        var file = new File(path);
        if (file.exists()) {
            getMetadata(uri.toString());
        } else {
            title = "<File not found>";
        }
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
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
                    case "album":
                        album = (String) value;
                        break;
                }
            }
        });
    }
}
