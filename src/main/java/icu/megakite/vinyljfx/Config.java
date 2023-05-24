package icu.megakite.vinyljfx;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static java.lang.Double.parseDouble;

public class Config {
    private static Config INSTANCE = null;

    private double volume;
    private Mode mode;

    private Config() {
        try {
            var fileReader = new FileReader("config.txt", StandardCharsets.UTF_8);
            var bufferedReader = new BufferedReader(fileReader);
            for (;;) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;

                String key = line.substring(0, line.lastIndexOf('='));
                String value = line.substring(line.lastIndexOf('=') + 1);
                switch (key)
                {
                case "volume":
                    volume = parseDouble(value);
                    break;
                case "mode":
                    switch (value) {
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
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public void setVolume(double volume) {
        this.volume = volume;
        update();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        update();
    }

    private void update() {
        try {
            FileWriter fileWriter = new FileWriter("config.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("volume=");
            bufferedWriter.write(Double.toString(volume));
            bufferedWriter.newLine();
            bufferedWriter.write("mode=");
            bufferedWriter.write(mode.toString());

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
