package game;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class readChunks {
    //The map file as last read, and what it was parsed into (world, x, y and the chunk's numbers, one per special
    //chunk). The file is still read for every new game, so edits to it show up, but it is only parsed again when it
    //changed: a switch between games (the zoom through space) is then not held up by it
    private static String parsedText;
    private static List<Object[]> parsed;

    public static void read (gameHandler handler) {
        String data = "";

        try (BufferedReader reader = gameFiles.read("chunks.txt")) {
            data = readAllLines(reader);
        } catch (IOException e) {
            System.out.println(e + " eeeeee");
        }

        if (!data.equals(parsedText)) {
            parsed = parse(data);
            parsedText = data;
        }
        for (Object[] c : parsed) {
            @SuppressWarnings("unchecked")
            List<Integer> chunkInfo = (List<Integer>) c[3];
            handler.getWorld((Integer) c[0]).generateSpecialChunk((Integer) c[1], (Integer) c[2], chunkInfo);
        }
    }

    private static List<Object[]> parse (String data) {
        List<Object[]> out = new ArrayList<Object[]>();
        String[] lines = data.replaceAll(" ", "").split("\\R");

        int i = 0;
        while (i < lines.length) {

            if (lines[i] != "") {
                List<Integer> tempWorld = new ArrayList<Integer>();
                List<Integer> tempX = new ArrayList<Integer>();
                List<Integer> tempY = new ArrayList<Integer>();

                while (lines[i].split(",").length < 9) {
                    String[] info = lines[i].split(",");
                    tempWorld.add(Integer.valueOf(info[0]));
                    tempX.add(Integer.valueOf(info[1]));
                    tempY.add(Integer.valueOf(info[2]));
                    i++;
                }

                List<Integer> chunkInfo = new ArrayList<Integer>();
                for (int n = 0; n <= 8; n++) {
                    String[] tempLine = lines[i].split(",");
                    for (String str : tempLine) {
                        chunkInfo.add(Integer.valueOf(str));
                    }
                    i++;
                }

                for (int n = 0; n < tempWorld.size(); n++) {
                    out.add(new Object[] {tempWorld.get(n), tempX.get(n), tempY.get(n), chunkInfo});
                }
            }

            else {i++;}
        }
        return out;
    }

    private static String readAllLines(BufferedReader reader) {
        return reader.lines()
          .collect(Collectors.joining(System.lineSeparator()));
    }
}
