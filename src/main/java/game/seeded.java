package game;

import java.util.Random;

//A Random from a seed, for anything that makes one per thing from numbers next to each other (seed + i). Java's Random
//gives almost the same first numbers for seeds next to each other: new Random(s) and new Random(s + 1) draw the same
//nextInt(4), and their first nextDouble differs by about 0.0001. So the seed is scrambled first (SplitMix64's
//finaliser), and every seed gets numbers of its own
public class seeded {
    public static Random random (long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30))*0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27))*0x94D049BB133111EBL;
        return new Random(z ^ (z >>> 31));
    }
}
