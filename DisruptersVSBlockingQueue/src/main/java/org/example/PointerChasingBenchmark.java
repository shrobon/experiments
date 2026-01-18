package org.example;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"}) // consistent heap to avoid GC noise
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class PointerChasingBenchmark {
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @Param({"10000000"}) // 10 Million elements
    private int size;

    private int[] primitiveArray;
    private List<Integer> objectList;

    @Setup(Level.Trial)
    public void setup() {
        primitiveArray = new int[size];
        objectList = new ArrayList<>(size);
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            int value = random.nextInt();
            primitiveArray[i] = value;
            objectList.add(value); // Autoboxing creates new Integer(value)
        }
        // 🔥 THE FIX: Shuffle the list to simulate memory fragmentation.
        // Now, iterating the list forces the CPU to jump to random heap addresses.
        java.util.Collections.shuffle(objectList);
    }

    // SCENARIO A: The "Primitive" Way (Contiguous Memory)
    // The CPU can pre-fetch data easily here.
    @Benchmark
    public long sumPrimitiveArray() {
        long sum = 0;
        for (int i : primitiveArray) {
            sum += i;
        }
        return sum;
    }

    // SCENARIO B: The "Enterprise" Way (Pointer Chasing)
    // CPU must fetch address -> jump to heap -> read value.
    @Benchmark
    public long sumWrapperList() {
        long sum = 0;
        for (Integer i : objectList) {
            sum += i; // Unboxing happens here
        }
        return sum;
    }
}
