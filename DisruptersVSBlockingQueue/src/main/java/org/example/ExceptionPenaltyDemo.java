package org.example;

public class ExceptionPenaltyDemo {

    private static final int DATA_SIZE = 1_000_000;
    private static final double ERROR_RATE = 0.30; // 30% of data will be unparseable

    public static void main(String[] args) {
        String[] data = generateData();

        System.out.println("Warming up JVM (triggering JIT compilation)...");
        for (int i = 0; i < 5; i++) {
            parseWithExceptions(data);
            parseWithValidation(data);
        }

        System.out.println("\n--- Starting Benchmark ---");

        // 1. The Slow Way: Exceptions for Control Flow
        long startExceptions = System.nanoTime();
        int count1 = parseWithExceptions(data);
        long timeExceptions = System.nanoTime() - startExceptions;

        // 2. The Fast Way: Pre-validation
        long startValidation = System.nanoTime();
        int count2 = parseWithValidation(data);
        long timeValidation = System.nanoTime() - startValidation;

        System.out.printf("Parsed %d items.\n", count1);
        System.out.printf("Exception Method:  %,d ms\n", timeExceptions / 1_000_000);
        System.out.printf("Validation Method: %,d ms\n", timeValidation / 1_000_000);
        System.out.printf("Magnitude Difference: %.2fx faster\n", (double) timeExceptions / timeValidation);
    }

    private static int parseWithExceptions(String[] data) {
        int validCount = 0;
        for (String s : data) {
            try {
                Integer.parseInt(s);
                validCount++;
            } catch (NumberFormatException e) {
                // Ignore and continue - this is the expensive part!
            }
        }
        return validCount;
    }

    private static int parseWithValidation(String[] data) {
        int validCount = 0;
        for (String s : data) {
            if (isNumeric(s)) {
                Integer.parseInt(s);
                validCount++;
            }
        }
        return validCount;
    }

    // A fast, allocation-free check (mechanical sympathy in action)
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) < '0' || str.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static String[] generateData() {
        String[] data = new String[DATA_SIZE];
        for (int i = 0; i < DATA_SIZE; i++) {
            // Mix in 10% bad data ("invalid_123")
            if (Math.random() < ERROR_RATE) {
                data[i] = "invalid_" + i;
            } else {
                data[i] = String.valueOf(i);
            }
        }
        return data;
    }
}