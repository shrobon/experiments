package org.example;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.nio.charset.StandardCharsets;

public class TestSIMD {

    static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    static void main() {
        String baseJson = "{\n" +
                "    \"id\" : 12345678,\n" +
                "    \"event\" : \"click\",\n" +
                "    \"timestamp\" : 1790443584,\n" +
                "    \"metadata\" : {\n" +
                "        \"source\" : \"web\"\n" +
                "    }\n" +
                "},";


        byte[] baseBytes = baseJson.getBytes(StandardCharsets.UTF_8);
        int repeat = 1_000_000;

        byte[] massiveJsonPayload = new byte[baseBytes.length * repeat];

        for (int i = 0; i < repeat; i++) {
            System.arraycopy(baseBytes, 0, massiveJsonPayload, i * baseBytes.length, baseBytes.length);
        }

        System.out.println("Payload Size: " + (massiveJsonPayload.length / 1024 / 1024) + " MB");
        System.out.println("Vector Size: " + SPECIES.length());
        System.out.println("Warming up JVM...");


        int dummy = 0;
        for (int i = 0; i < 50; i++) {
            dummy += scanScalar(massiveJsonPayload);
            dummy += scanSIMD(massiveJsonPayload);
        }

        System.out.println("Warmup complete. Dummy: " + dummy + "\n");

        // 1. Run Scalar (Character by character)
        long startScalar = System.nanoTime();
        int countScalar = scanScalar(massiveJsonPayload);
        long timeScalar = System.nanoTime() - startScalar;

        // 2. Run SIMD (Chunk by chunk)
        long startSIMD = System.nanoTime();
        int countSIMD = scanSIMD(massiveJsonPayload);
        long timeSIMD = System.nanoTime() - startSIMD;

        System.out.printf("Quotes found: %d%n", countSIMD);
        System.out.printf("Scalar Time:  %d ms%n", timeScalar / 1_000_000);
        System.out.printf("SIMD Time:    %d ms%n", timeSIMD / 1_000_000);
        System.out.printf("Speedup:      %.2fx%n", (double) timeScalar / timeSIMD);
    }

    static int scanSIMD(byte[] payload) {
        int quoteCount = 0;
        int i = 0;
        int upperBound = SPECIES.loopBound(payload.length);
        final byte quoteChar = '"';

        for (; i < upperBound; i += SPECIES.length()) {
            ByteVector vec = ByteVector.fromArray(SPECIES, payload, i);
            VectorMask<Byte> isQuoteMask = vec.compare(VectorOperators.EQ, quoteChar);

            quoteCount += isQuoteMask.trueCount();
        }

        // trailing
        for (; i < payload.length; i++) {
            if (payload[i] == quoteChar) {
                quoteCount++;
            }
        }

        return quoteCount;
    }

    static int scanScalar(byte[] payload) {
        int quoteCount = 0;
        for (int i = 0; i < payload.length; i++) {
            if (payload[i] == '"') {
                quoteCount++;
            }
        }
        return quoteCount;
    }
}
