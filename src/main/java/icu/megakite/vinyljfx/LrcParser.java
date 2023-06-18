package icu.megakite.vinyljfx;

import javafx.util.Duration;
import javafx.util.Pair;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class LrcParser {
    private static final Pattern patternLine = Pattern.compile("\\[(.*?)](.*)");
    private static final Pattern patternTime = Pattern.compile("(.*):(.*)\\.(.*)");
    private static final Pair<Duration, String> defaultLyric = new Pair<>(Duration.ONE, "<Lyrics file not found>");

    public static List<Pair<Duration, String>> parse(String s) {
        List<Pair<Duration, String>> list = new ArrayList<>();

        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            return List.of(defaultLyric);
        }

        FileReader fileReader;
        try {
            fileReader = new FileReader(uri.getPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of(defaultLyric);
        }

        var reader = new BufferedReader(fileReader);
        for (;;) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                return List.of(defaultLyric);
            }

            if (line == null)
                break;

            var matcherLine = patternLine.matcher(line);
            if (!matcherLine.find())
                continue;

            var time = matcherLine.group(1);
            var lyric = matcherLine.group(2);

            var matcherTime = patternTime.matcher(time);
            if (!matcherTime.find())
                continue;

            try {
                int minutes = Integer.parseInt(matcherTime.group(1));
                int seconds = Integer.parseInt(matcherTime.group(2));
                int hundredths = Integer.parseInt(matcherTime.group(3));

                var timestamp = new Duration(minutes * 60 * 1000 + seconds * 1000 + hundredths * 10);

                list.add(new Pair<>(timestamp, lyric));
            } catch (NumberFormatException ignored) {
                // continue
            }
        }

        return list;
    }
}
