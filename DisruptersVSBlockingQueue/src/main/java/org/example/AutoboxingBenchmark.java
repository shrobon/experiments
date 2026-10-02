package org.example;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoboxingBenchmark {

    // 20M elements runs cleanly within standard default heap sizes (~2GB)
    static final int RECORDS = 20_000_000;

    public static void main(String[] args) {
        System.out.printf("Generating raw data for %,d elements...%n", RECORDS);
        Random rand = new Random(42);
        int[] rawData = new int[RECORDS];
        for (int i = 0; i < RECORDS; i++) {
            rawData[i] = rand.nextInt(1_000_000);
        }

        System.out.println("Starting benchmark...\n");

        // ========================================================
        // 1. STANDARD JAVA (Generics + Autoboxing)
        // ========================================================
        cleanMemory();
        long memBeforeBoxed = getUsedMemoryMB();

        // Measure Write Path (Allocation + Autoboxing)
        long startBoxedWrite = System.currentTimeMillis();
        List<Integer> boxedList = new ArrayList<>(RECORDS);
        for (int val : rawData) {
            // SILENT PENALTY: Allocates 20 million individual Integer objects
            boxedList.add(val);
        }
        long timeBoxedWrite = System.currentTimeMillis() - startBoxedWrite;

        long memAfterBoxed = getUsedMemoryMB();
        long boxedMemoryMB = memAfterBoxed - memBeforeBoxed;

        // Measure Read Path (Pointer Chasing + Unboxing)
        long startBoxedRead = System.currentTimeMillis();
        long sumBoxed = 0;
        for (int i = 0; i < RECORDS; i++) {
            sumBoxed += boxedList.get(i);
        }
        long timeBoxedRead = System.currentTimeMillis() - startBoxedRead;

        System.out.println("=== Standard List<Integer> ===");
        System.out.printf("  Memory Footprint : ~%,d MB%n", boxedMemoryMB);
        System.out.printf("  Write Time       : %,d ms%n", timeBoxedWrite);
        System.out.printf("  Read Time        : %,d ms%n%n", timeBoxedRead);

        // Free reference to allow GC before running the primitive test
        boxedList = null;
        cleanMemory();

        // ========================================================
        // 2. FASTUTIL (Primitive Specialized)
        // ========================================================
        long memBeforePrim = getUsedMemoryMB();

        // Measure Write Path (Direct flat array write)
        long startPrimWrite = System.currentTimeMillis();
        IntArrayList primList = new IntArrayList(RECORDS);
        for (int val : rawData) {
            // Direct write to backing int[] array
            primList.add(val);
        }
        long timePrimWrite = System.currentTimeMillis() - startPrimWrite;

        long memAfterPrim = getUsedMemoryMB();
        long primMemoryMB = memAfterPrim - memBeforePrim;

        // Measure Read Path (Sequential contiguous memory scan)
        long startPrimRead = System.currentTimeMillis();
        long sumPrim = 0;
        for (int i = 0; i < RECORDS; i++) {
            sumPrim += primList.getInt(i);
        }
        long timePrimRead = System.currentTimeMillis() - startPrimRead;

        System.out.println("=== Fastutil IntArrayList ===");
        System.out.printf("  Memory Footprint : ~%,d MB%n", primMemoryMB);
        System.out.printf("  Write Time       : %,d ms%n", timePrimWrite);
        System.out.printf("  Read Time        : %,d ms%n%n", timePrimRead);

        // ========================================================
        // 3. VERIFICATION & COMPARISON
        // ========================================================
        System.out.println("=== Comparison ===");

        double memRatio = (double) boxedMemoryMB / Math.max(1, primMemoryMB);
        double writeRatio = (double) timeBoxedWrite / Math.max(1, timePrimWrite);
        double readRatio = (double) timeBoxedRead / Math.max(1, timePrimRead);

        System.out.printf("  Memory Savings   : ~%.1fx less memory%n", memRatio);
        System.out.printf("  Write Speedup    : ~%.1fx faster%n", writeRatio);
        System.out.printf("  Read Speedup     : ~%.1fx faster%n", readRatio);

        if (sumBoxed != sumPrim) {
            System.err.println("Sum mismatch: " + sumBoxed + " vs " + sumPrim);
        }
    }

    static long getUsedMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    static void cleanMemory() {
        System.gc();
        try {
            Thread.sleep(250);
        } catch (InterruptedException ignored) {}
    }
}