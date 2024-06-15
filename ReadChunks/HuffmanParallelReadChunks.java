package ReadChunks;

import java.io.IOException;
import java.io.RandomAccessFile;
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
import java.util.concurrent.atomic.AtomicLong;

public class HuffmanParallelReadChunks implements Runnable {
    private static String inputFile;
    private static int numThreads;
    private static int chunkSize;
    private static boolean quietMode = false;
    private static ConcurrentHashMap<Character, AtomicInteger> frequencyTable = new ConcurrentHashMap<>();
    private static AtomicLong filePointer = new AtomicLong(0);
    private int threadId;
    private RandomAccessFile file;

    private static String outputFile;

    public HuffmanParallelReadChunks(int threadId, RandomAccessFile file) {
        this.threadId = threadId;
        this.file = file;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        if (!quietMode) {
            System.out.println("Thread-" + threadId + " started.");
        }

        try {
            while (true) {
                long currentPointer = filePointer.getAndAdd(chunkSize);
                if (currentPointer >= file.length()) {
                    break;
                }

                byte[] buffer = new byte[chunkSize];
                synchronized (file) {
                    file.seek(currentPointer);
                    int bytesRead = file.read(buffer);

                    if (bytesRead == -1) {
                        break;
                    }

                    for (int i = 0; i < bytesRead; i++) {
                        char character = (char) buffer[i];
                        frequencyTable.computeIfAbsent(character, k -> new AtomicInteger()).incrementAndGet();
                    }
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

        try (RandomAccessFile file = new RandomAccessFile(inputFile, "r")) {
            filePointer.set(0);
            if (numThreads == 1) {
                runSingleThreaded(file);
            } else {
                runMultiThreaded(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-f")) {
                inputFile = args[++i];
            } else if (args[i].equals("-t") || args[i].equals("-tasks")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-c") || args[i].equals("-chunk")) {
                chunkSize = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-q") || args[i].equals("-quiet")) {
                quietMode = true;
            } else if (args[i].equals("-w")) {
                outputFile = args[++i];
            }
        }
        if (inputFile == null || numThreads <= 0 || chunkSize <= 0) {
            System.err.println(
                    "Usage: java HuffmanParallelReadChunks -f <inputfile> -t <threads> -c <chunksize> [-q] [-w <outputfile>]");
            System.exit(1);
        }
    }

    private static void runSingleThreaded(RandomAccessFile file) {
        long startTime = System.currentTimeMillis();
        HuffmanParallelReadChunks task = new HuffmanParallelReadChunks(1, file);
        task.run();
        long endTime = System.currentTimeMillis();
        System.out.println("Threads used in current run: 1");
        System.out.println("Total execution time for current run (millis): " + (endTime - startTime));
    }

    private static void runMultiThreaded(RandomAccessFile file) {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            HuffmanParallelReadChunks task = new HuffmanParallelReadChunks(i + 1, file);
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

        if (outputFile != null) {
            writeFrequencyTableToFile(frequencyTable, outputFile);
        }
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