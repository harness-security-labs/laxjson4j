package benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Single-threaded parse-only benchmark comparing JSON parsers.
 *
 * <p>Measures average wall-clock time per {@code String → tree} parse call.
 * All benchmark methods return their result to prevent dead-code elimination.
 *
 * <p>Run:
 * <pre>
 *   ./gradlew :docs:benchmarks:jmh \
 *       -Pjmh.includes='ParserBenchmarkingSingleThread'
 * </pre>
 *
 * <p>For JSON output (useful for charting / comparison):
 * <pre>
 *   ./gradlew :docs:benchmarks:jmh \
 *       -Pjmh.includes='ParserBenchmarkingSingleThread' \
 *       -Pjmh.resultFormat='JSON' \
 *       -Pjmh.resultsFile='benchmark-results.json'
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = {"-Xms512m", "-Xmx512m"})
@Threads(1)
@State(Scope.Thread)
public class ParserBenchmarkingSingleThread {

    @Param({"small.json", "medium.json", "large.json"})
    String inputFile;

    private String json;
    private ai.traceable.laxjson4j.Json laxjson4j;
    private ObjectMapper jacksonMapper;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(inputFile)) {
            if (is == null) throw new IOException("Resource not found: " + inputFile);
            json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        laxjson4j = new ai.traceable.laxjson4j.Json();
        jacksonMapper = new ObjectMapper();
    }

    // laxjson4j

    @Benchmark
    public ai.traceable.laxjson4j.node.JsonNode laxjson4j() {
        return laxjson4j.parse(json);
    }

    // Third-party parsers

    @Benchmark
    public com.fasterxml.jackson.databind.JsonNode jackson() throws IOException {
        return jacksonMapper.readTree(json);
    }

    @Benchmark
    public com.google.gson.JsonElement gson() {
        return com.google.gson.JsonParser.parseString(json);
    }

    @Benchmark
    public Object fastjson2() {
        return com.alibaba.fastjson2.JSON.parse(json);
    }

    @Benchmark
    public org.hjson.JsonValue hjson() {
        return org.hjson.JsonValue.readHjson(json);
    }
}
