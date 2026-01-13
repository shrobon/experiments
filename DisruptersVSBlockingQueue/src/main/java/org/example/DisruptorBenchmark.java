package org.example;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class DisruptorBenchmark {

    // Number of messages to pass: 10 Million
    private static final int ITERATIONS = 10_000_000;

    public static void main(String[] args) throws Exception {
        System.out.println("⚠️ APP STARTED. Waiting to connect VisualVM...");
        Thread.sleep(15000);

        System.out.println("--- Starting Benchmark ---");
        System.out.println("--- Starting Benchmark (10 Million Events) ---");

        // Run Queue Test
        runQueueBenchmark();

        // Run Disruptor Test
        runDisruptorBenchmark();
    }

    // ---------------------------------------------------------------
    // SCENARIO 1: Standard ArrayBlockingQueue
    // ---------------------------------------------------------------
    private static void runQueueBenchmark() throws InterruptedException {
        // A queue with capacity 1024
        final ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<>(1024);
        final CountDownLatch latch = new CountDownLatch(1);

        // Consumer Thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < ITERATIONS; i++) {
                    // TAKE: Blocks if empty
                    queue.take();
                }
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        long startTime = System.currentTimeMillis();
        consumer.start();

        // Producer (Main Thread)
        for (int i = 0; i < ITERATIONS; i++) {
            // PUT: Blocks if full.
            // MEMORY COST: Autoboxing 'i' (int) to 'Long' (Object) creates garbage!
            queue.put(Long.valueOf(i));
        }

        latch.await(); // Wait for consumer to finish
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("BlockingQueue Time: " + duration + " ms");
        System.out.println("  -> Locks used? YES");
        System.out.println("  -> Garbage Created? YES (10 Million Long objects)");
    }

    // ---------------------------------------------------------------
    // SCENARIO 2: LMAX Disruptor
    // ---------------------------------------------------------------

    // 1. The Event (Mutable, reused)
    public static class LongEvent {
        private long value;
        public void set(long value) { this.value = value; }
    }

    // 2. The Factory (Called once at startup)
    public static class LongEventFactory implements EventFactory<LongEvent> {
        public LongEvent newInstance() { return new LongEvent(); }
    }

    // 3. The Handler (Consumer)
    public static class LongEventHandler implements EventHandler<LongEvent> {
        private final CountDownLatch latch;
        private int count = 0;

        public LongEventHandler(CountDownLatch latch) { this.latch = latch; }

        @Override
        public void onEvent(LongEvent event, long sequence, boolean endOfBatch) {
            count++;
            if (count == ITERATIONS) {
                latch.countDown();
            }
        }
    }

    private static void runDisruptorBenchmark() throws InterruptedException {
        int bufferSize = 1024; // Power of 2
        LongEventFactory factory = new LongEventFactory();

        // Create Disruptor
        Disruptor<LongEvent> disruptor = new Disruptor<>(
                factory,
                bufferSize,
                DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE, // Optimize for single producer
                new com.lmax.disruptor.BusySpinWaitStrategy()
        );

        CountDownLatch latch = new CountDownLatch(1);
        disruptor.handleEventsWith(new LongEventHandler(latch));
        disruptor.start();

        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        long startTime = System.currentTimeMillis();

        // Producer (Main Thread)
        for (long i = 0; i < ITERATIONS; i++) {
            long sequence = ringBuffer.next();
            try {
                // MEMORY BENEFIT: No new objects. Just setting a primitive long.
                ringBuffer.get(sequence).set(i);
            } finally {
                ringBuffer.publish(sequence);
            }
        }

        latch.await(); // Wait for consumer to finish
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Disruptor Time:     " + duration + " ms");
        System.out.println("  -> Locks used? NO (CAS only)");
        System.out.println("  -> Garbage Created? ZERO (Reused 1024 objects)");
    }
}
