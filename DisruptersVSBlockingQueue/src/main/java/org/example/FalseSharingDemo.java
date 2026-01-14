package org.example;

public class FalseSharingDemo {
    private static final long ITERATIONS = 500_000_000L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- False Sharing Benchmark (Fixed Layout) ---");

        // 1. Run the "Bad" layout
        runBadPad();

        // 2. Run the "Good" layout
        runGoodPad();
    }

    // ----------------------------------------------------------------
    // TEST 1: The "Bad" Object (Fields are neighbors)
    // ----------------------------------------------------------------
    static class BadPad {
        volatile long x;
        volatile long y;
    }

    private static void runBadPad() throws InterruptedException {
        BadPad t = new BadPad();
        long start = System.currentTimeMillis();

        Thread t1 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) t.x++;
        });

        Thread t2 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) t.y++;
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        long duration = System.currentTimeMillis() - start;
        System.out.println("Adjacent Fields (False Sharing): " + duration + " ms");
    }

    // ----------------------------------------------------------------
    // TEST 2: The "Good" Object (The Inheritance Sandwich)
    // ----------------------------------------------------------------

    // Top Bun (Holds X)
    static class L1 {
        volatile long x;
    }

    // The Meat (Holds 120 bytes of Padding)
    // Forces 'y' to start far away from 'x' in memory
    static class L2 extends L1 {
        volatile long p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
    }

    // Bottom Bun (Holds Y)
    static class GoodPad extends L2 {
        volatile long y;
    }

    private static void runGoodPad() throws InterruptedException {
        GoodPad t = new GoodPad();
        long start = System.currentTimeMillis();

        Thread t1 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) t.x++;
        });

        Thread t2 = new Thread(() -> {
            for (long i = 0; i < ITERATIONS; i++) t.y++;
        });

        t1.start(); t2.start();
        t1.join(); t2.join();

        long duration = System.currentTimeMillis() - start;
        System.out.println("Padded Fields (No Sharing):    " + duration + " ms");
    }
}