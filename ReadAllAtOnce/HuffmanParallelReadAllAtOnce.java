package ReadAllAtOnce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class HuffmanParallelReadAllAtOnce implements Runnable {
    private static String inputFile;
    private static int numThreads;
    private static boolean quietMode = false;
    private static String outputFile;
    private static ConcurrentHashMap<Character, AtomicInteger> frequencyTable = new ConcurrentHashMap<>();
    private static AtomicInteger threadCounter = new AtomicInteger(1);
    private String dataChunk;

    public HuffmanParallelReadAllAtOnce(String dataChunk) {
        this.dataChunk = dataChunk;
    }

    @Override
    public void run() {
        int threadId = threadCounter.getAndIncrement();
        long startTime = System.currentTimeMillis();
        if (!quietMode) {
            System.out.println("Thread-" + threadId + " started.");
        }
        calculateFrequency(dataChunk, frequencyTable);
        long endTime = System.currentTimeMillis();
        if (!quietMode) {
            System.out.println("Thread-" + threadId + " stopped.");
            System.out.println("Thread-" + threadId + " execution time was (millis): " + (endTime - startTime));
        }
    }

    private static void calculateFrequency(String data, ConcurrentHashMap<Character, AtomicInteger> frequencyTable) {
        for (char c : data.toCharArray()) {
            frequencyTable.computeIfAbsent(c, k -> new AtomicInteger()).incrementAndGet();
        }
    }

    public static void main(String[] args) {
        parseArguments(args);
        String data = readFile(inputFile);
        if (data == null) {
            System.err.println("Error reading input file.");
            return;
        }
        if (numThreads == 1) {
            runSingleThreaded(data);
        } else {
            runMultiThreaded(data);
        }

        if (outputFile != null) {
            writeFrequencyTableToFile(frequencyTable, outputFile);
        }
    }

    private static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-f")) {
                inputFile = args[++i];
            } else if (args[i].equals("-t") || args[i].equals("-tasks")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-q") || args[i].equals("-quiet")) {
                quietMode = true;
            } else if (args[i].equals("-w")) {
                outputFile = args[++i];
            }
        }
        if (inputFile == null || numThreads <= 0) {
            System.err.println("Usage: java HuffmanParallel -f <inputfile> -t <threads> [-q] [-w <outputfile>]");
            System.exit(1);
        }
    }

    private static String readFile(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void runSingleThreaded(String data) {
        long startTime = System.currentTimeMillis();
        HuffmanParallelReadAllAtOnce task = new HuffmanParallelReadAllAtOnce(data);
        task.run();
        long endTime = System.currentTimeMillis();
        System.out.println("Threads used in current run: 1");
        System.out.println("Total execution time for current run (millis): " + (endTime - startTime));
    }

    private static void runMultiThreaded(String data) {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        int chunkSize = data.length() / numThreads;
        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numThreads - 1) ? data.length() : (i + 1) * chunkSize;
            HuffmanParallelReadAllAtOnce task = new HuffmanParallelReadAllAtOnce(data.substring(start, end));
            futures.add(executor.submit(task));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        long endTime = System.currentTimeMillis();
        System.out.println("Threads used in current run: " + numThreads);
        System.out.println("Total execution time for current run (millis): " + (endTime - startTime));
    }

    private static void writeFrequencyTableToFile(ConcurrentHashMap<Character, AtomicInteger> frequencyTable,
            String filename) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Character, AtomicInteger> entry : frequencyTable.entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue().get()).append("\n");
        }
        writeToFile(filename, builder.toString());
    }

    private static void writeToFile(String filename, String data) {
        try {
            Files.write(Paths.get(filename), data.getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
