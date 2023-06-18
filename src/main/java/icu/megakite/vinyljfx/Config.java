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
        var prop = new Properties();
        try {
            var etc = new File("etc");
            if (!configFile.exists()) {
                if (!etc.mkdir() && !configFile.createNewFile())
                    throw new IOException("Cannot create config file");

                volume = 1.0;
                mode = Mode.DEFAULT;
                update();

                return;
            }

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

    public void setVolume(double volume) {
        this.volume = volume;
        update();
    }

    private void update() {
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("volume=");
            bufferedWriter.write(Double.toString(volume));
            bufferedWriter.newLine();
            bufferedWriter.write("mode=");
            bufferedWriter.write(mode.toString());
            bufferedWriter.newLine();

            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
