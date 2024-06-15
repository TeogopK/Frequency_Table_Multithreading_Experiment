package ReadOneByOne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HuffmanParallelReadOneByOne implements Runnable {
    private static String inputFile;
    private static String outputFile;
    private static int numThreads;
    private static boolean quietMode = false;
    private static Map<Character, Integer> frequencyTable = new HashMap<>();
    private int threadId;

    private static final Object fileLock = new Object();
    private static BufferedReader reader;

    public HuffmanParallelReadOneByOne(int threadId) {
        this.threadId = threadId;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        if (!quietMode) {
            System.out.println("Thread-" + threadId + " started.");
        }

        try {
            while (true) {
                int c;
                synchronized (fileLock) {
                    c = reader.read();
                }
                if (c == -1) {
                    break;
                }

                char character = (char) c;
                synchronized (frequencyTable) {
                    frequencyTable.put(character, frequencyTable.getOrDefault(character, 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        if (!quietMode) {
            System.out.println("Thread-" + threadId + " stopped.");
            System.out.println("Thread-" + threadId + " execution time was (millis): " + (endTime - startTime));
        }
    }

    public static void main(String[] args) {
        parseArguments(args);
        try {
            reader = new BufferedReader(new FileReader(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (numThreads == 1) {
            runSingleThreaded();
        } else {
            runMultiThreaded();
        }

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (outputFile != null) {
            writeFrequencyTableToFile(outputFile);
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
            System.err.println(
                    "Usage: java HuffmanParallelReadOneByOne -f <inputfile> -t <threads> [-q] [-w <outputfile>]");
            System.exit(1);
        }
    }

    private static void runSingleThreaded() {
        long startTime = System.currentTimeMillis();
        HuffmanParallelReadOneByOne task = new HuffmanParallelReadOneByOne(1);
        task.run();
        long endTime = System.currentTimeMillis();
        System.out.println("Threads used in current run: 1");
        System.out.println("Total execution time for current run (millis): " + (endTime - startTime));
    }

    private static void runMultiThreaded() {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            HuffmanParallelReadOneByOne task = new HuffmanParallelReadOneByOne(i + 1);
            futures.add(executor.submit(task));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        long endTime = System.currentTimeMillis();
        System.out.println("Threads used in current run: " + numThreads);
        System.out.println("Total execution time for current run (millis): " + (endTime - startTime));
    }

    private static void writeFrequencyTableToFile(String outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (Map.Entry<Character, Integer> entry : frequencyTable.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
