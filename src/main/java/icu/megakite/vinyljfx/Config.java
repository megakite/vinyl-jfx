package icu.megakite.vinyljfx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static java.lang.Double.parseDouble;

public class Config {
    private static Config INSTANCE = null;
    private static final File configFile = new File("etc", "config.txt");

    private double volume;
    private Mode mode;

    private Config() {
        try {
            var etc = new File("etc");
            if (!etc.isDirectory()) {
                boolean mkdir = etc.mkdir();
                if (!mkdir)
                    throw new IOException("Cannot create directory ./etc/");
            }

            boolean newFile = configFile.createNewFile();
            if (newFile) {
                volume = 1.0;
                mode = Mode.DEFAULT;
                update();

                return;
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        var prop = new Properties();
        try {
            var fileReader = new FileReader(configFile, StandardCharsets.UTF_8);
            var bufferedReader = new BufferedReader(fileReader);
            prop.load(bufferedReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        volume = parseDouble(prop.getProperty("volume"));
        switch (prop.getProperty("mode")) {
        case "DEFAULT":
            mode = Mode.DEFAULT;
            break;
        case "REPEAT_ALL":
            mode = Mode.REPEAT_ALL;
            break;
        case "REPEAT_ONE":
            mode = Mode.REPEAT_ONE;
            break;
        case "SHUFFLE":
            mode = Mode.SHUFFLE;
            break;
        }
    }

    public static Config getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Config();

        return INSTANCE;
    }

    public double getVolume() {
        return volume;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        update();
    }

    public void setVolume(double volume) {
        this.volume = volume;
        update();
    }

    private void update() {
        var prop = new Properties();
        prop.setProperty("volume", Double.toString(volume));
        prop.setProperty("mode", mode.toString());

        try {
            FileWriter fileWriter = new FileWriter(configFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            prop.store(bufferedWriter, "Config file of Vinyl");

            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
