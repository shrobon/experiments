package org.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickBenchmark {
    public static void main(String[] args) {
        System.out.println("Generating test data...");
        // Create a massive string (approx 5 million words / 45 MB of text)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5_000_000; i++) {
            sb.append("word").append(i).append(" ");
        }
        String text = sb.toString();
        System.out.printf("Payload ready: %.2f MB\n", text.length() / 1024.0 / 1024.0);

        // --- 1. WARMUP THE JVM ---
        // Run both once so the JVM loads the classes before we start timing
        Pattern.compile("[a-zA-Z0-9]+").matcher(text).find();
        Re2cWordCounter.countWords("warmup");

        // --- 2. TIME STANDARD REGEX ---
        System.out.println("\nRunning Standard java.util.regex...");
        Pattern pattern = Pattern.compile("[a-zA-Z0-9]+");

        long startRegex = System.nanoTime();
        Matcher matcher = pattern.matcher(text);
        int regexCount = 0;
        while (matcher.find()) {
            regexCount++;
        }
        long endRegex = System.nanoTime();
        double regexMs = (endRegex - startRegex) / 1_000_000.0;

        System.out.printf("Regex finished: %d words in %.2f ms\n", regexCount, regexMs);

        // --- 3. TIME RE2C ---
        System.out.println("\nRunning re2c native DFA...");

        long startRe2c = System.nanoTime();
        int re2cCount = Re2cWordCounter.countWords(text);
        long endRe2c = System.nanoTime();
        double re2cMs = (endRe2c - startRe2c) / 1_000_000.0;

        System.out.printf("re2c finished:  %d words in %.2f ms\n", re2cCount, re2cMs);

        // --- 4. RESULTS ---
        System.out.println("\n==================================");
        System.out.printf("re2c was %.2f TIMES FASTER\n", (regexMs / re2cMs));
        System.out.println("==================================");
    }
}