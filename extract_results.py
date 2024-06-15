import subprocess

INPUT_FILE_SIZE = "300"
SOLUTION_DIR = "ReadAllAtOnce"
THREAD_COUNTS = [1, 2, 4, 8, 16, 32, 64, 128, 1024]

INPUT_FILE = f"inputs/{INPUT_FILE_SIZE}"
SOLUTION_MAIN = f"HuffmanParallel{SOLUTION_DIR}"
OUTPUT_FILE = f"{SOLUTION_DIR}/execution_times_{INPUT_FILE_SIZE}MB.md"

JAVA_PATH = "C:/Program Files/Java/jdk-17/bin/java.exe"

execution_times = {}

# Function to run Java code with specified thread count and get execution time
def run_java(thread_count):
    times = []
    for _ in range(3):
        result = subprocess.run(
            [
                JAVA_PATH,
                f"{SOLUTION_DIR}/{SOLUTION_MAIN}",
                "-f",
                INPUT_FILE,
                "-t",
                str(thread_count),
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
            print(thread_count, time_line)
            execution_time = int(time_line[0].split(
                " ")[-1].replace("ms.", ""))
            times.append(execution_time)
    return times


# Run Java code with different thread counts and store results
for count in THREAD_COUNTS:
    times = run_java(count)
    execution_times[count] = times

# Write results to a file
with open(OUTPUT_FILE, "w") as file:
    file.write(f"File: {INPUT_FILE}\n\n")
    file.write("| # | p | G | Tp(1) | Tp(2) | Tp(3) | Tp(min) | Sp | Ep |\n")
    file.write("|---|---|---|-------|-------|-------|----|----|----|\n")

    # Base execution time for single thread
    base_time = min(execution_times[1])

    # Calculate and write results for each thread count
    for i, count in enumerate(THREAD_COUNTS):
        times = execution_times[count]
        min_time = min(times)
        sp = base_time / min_time
        ep = sp / count
        data = (
            i + 1,
            count,
            1,
            f"{times[0]}ms",
            f"{times[1]}ms",
            f"{times[2]}ms",
            f"{min_time}ms",
            f"{sp:.6f}",
            f"{ep:.6f}",
        )
        file.write(
            "| {:<2} | {:<2} | {:<2} | {:<6} | {:<6} | {:<6} | {:<4} | {:<8} | {:<8} |\n".format(
                *data)
        )
