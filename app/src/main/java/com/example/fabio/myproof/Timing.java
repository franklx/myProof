package com.example.fabio.myproof;

/**
 * Created by fabio on 21/06/2017.
 */

public class Timing {
    private static long startTime;
    private static long endTime;
    public static long duration;

    public Timing() {
        startTime = System.currentTimeMillis();
        endTime = startTime;
        duration = endTime-startTime;
    }
    public static void time() {
        startTime = endTime;
        endTime = System.currentTimeMillis(); //System.nanoTime();
        duration = endTime-startTime;
    }
    public static String getDuration() {
        return String.valueOf(duration);
    }
}
