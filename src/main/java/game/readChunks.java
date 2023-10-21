package game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class readChunks {
    public static void read (gameHandler handler) {
        System.out.println("Reading chunks");
        String data = "";

        //Lag ny fil i tilfelle den ikke eksisterer
        File obj = new File(getPath() + "/chunks.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader( getPath() + "/chunks.txt"))) {
            data = readAllLines(reader);
        } catch (IOException e) {
            System.out.println(e + " eeeeee");
        }

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
                    handler.getWorld(tempWorld.get(n)).generateSpecialChunk(tempX.get(n), tempY.get(n), chunkInfo);
                    System.out.println(tempWorld.get(n) + ", " + tempX.get(n) + ", " + tempY.get(n) + " loading");
                }
            }

            

            else {i++;}
        }
         
    }

    private static String getPath () {
        String resourceFolder = "GemstoneGuardian/src/main/resources/game/reading/";
        String absolutePath = new File(resourceFolder).getAbsolutePath();
        return absolutePath;
    }

    private static String readAllLines(BufferedReader reader) {
        return reader.lines()
          .collect(Collectors.joining(System.lineSeparator()));
    }
}
