import subprocess

INPUT_FILE_SIZE_MB = 10000
SOLUTION_DIR = "ReadChunks"
THREAD_COUNTS = [1, 2, 8, 16]
CHUNK_SIZES = [32 * 1024*1024, 124 * 1024 * 1024]  # in bytes

INPUT_FILE = f"inputs/{INPUT_FILE_SIZE_MB}"
SOLUTION_MAIN = f"HuffmanParallel{SOLUTION_DIR}"
OUTPUT_FILE = f"{SOLUTION_DIR}/execution_times_{INPUT_FILE_SIZE_MB}MB.md"

JAVA_PATH = "C:/Program Files/Java/jdk-17/bin/java.exe"

execution_times = {}

# Function to run Java code with specified thread count and chunk size and get execution time
def run_java(thread_count, chunk_size):
    times = []
    for _ in range(1):
        result = subprocess.run(
            [
                JAVA_PATH,
                f"{SOLUTION_DIR}/{SOLUTION_MAIN}",
                "-f",
                INPUT_FILE,
                "-t",
                str(thread_count),
                "-c",
                str(chunk_size),
                "-q"
            ],
            capture_output=True,
            text=True,
        )
        output_lines = result.stdout.split("\n")
        time_line = [
            line
            for line in output_lines
            if "Total execution time for current run" in line
        ]
        if time_line:
            print(thread_count, chunk_size, time_line)
            execution_time = int(time_line[0].split(
                " ")[-1].replace("ms.", ""))
            times.append(execution_time)
    return times


# Run Java code with different thread counts and chunk sizes and store results
for chunk_size in CHUNK_SIZES:
    execution_times[chunk_size] = {}
    for count in THREAD_COUNTS:
        times = run_java(count, chunk_size)
        execution_times[chunk_size][count] = times

# Write results to a file
with open(OUTPUT_FILE, "w") as file:
    file.write(f"File: {INPUT_FILE}\n\n")
    file.write("| Chunk Size (KB) | # | p | G | Tp(1) | Tp(2) | Tp(3) | Tp(min) | Sp | Ep |\n")
    file.write("|-----------------|---|---|---|-------|-------|-------|---------|----|----|\n")

    # Iterate over each chunk size
    for chunk_size in CHUNK_SIZES:
        # Calculate granularity
        G = INPUT_FILE_SIZE_MB * 1024 * 1024 / chunk_size  # Input file size in bytes / chunk size
        chunk_size_kb = chunk_size // 1024

        # Base execution time for single thread for the current chunk size
        base_time = min(execution_times[chunk_size][1])

        # Calculate and write results for each thread count
        for i, count in enumerate(THREAD_COUNTS):
            times = execution_times[chunk_size][count]
            min_time = min(times)
            sp = base_time / min_time
            ep = sp / count
            data = (
                chunk_size_kb,
                i + 1,
                count,
                f"{G:.2f}",
                f"{times[0]}ms",
                f"{times[0]}ms",
                f"{times[0]}ms",
                f"{min_time}ms",
                f"{sp:.6f}",
                f"{ep:.6f}",
            )
            file.write(
                "| {:<17} | {:<2} | {:<2} | {:<5} | {:<6} | {:<6} | {:<6} | {:<7} | {:<8} | {:<8} |\n".format(
                    *data)
            )
