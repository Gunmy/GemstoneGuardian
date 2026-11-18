package game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class highscore {
    public static double getCurrentScore () {
        //Create file if it doesnt exist
        File obj = getFile();

        double score = -1;
        try {
			BufferedReader reader = new BufferedReader(new FileReader(obj));
			String scoretxt = reader.readLine();
            if (scoretxt == "") score = Double.valueOf(scoretxt);
            reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        return score;
    }

    private static File getFile () {
        return gameFiles.writable("highscore.txt");
    }

    private static void write (double time) {
        try {
            FileWriter myWriter = new FileWriter(getFile());
            myWriter.write("" + time);
            myWriter.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
    }

    private static boolean newHighscore (double score) {
        return score > getCurrentScore();
    }

    public static void reset () {
        write(-1);
    }

    public static String endGame (double score) {
        String msg = "Your time was " + score/60 + " minutes\n";

        if (newHighscore(score) || getCurrentScore() == -1) {
            msg += "NEW HIGHSCORE!";
            write(score);
        } else {
            msg += "You did not beat the time of " + getCurrentScore();
        }

        return msg;
    }

}