package org.example;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class LexerBenchmark {

    private String text;
    private Pattern pattern;

    @Setup
    public void setup() {
        // Generate a test string of random words
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("word").append(i).append(" ");
        }
        text = sb.toString();

        // Pre-compile the standard regex pattern
        pattern = Pattern.compile("[a-zA-Z0-9]+");
    }

    @Benchmark
    public int javaUtilRegex() {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @Benchmark
    public int re2cGeneratedDFA() {
        // Calls the output of our .re file
        return Re2cWordCounter.countWords(text);
    }
}