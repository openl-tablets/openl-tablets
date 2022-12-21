package org.openl.util;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Install JMH plugin for running this benchmark.
 * Define {@code  -prof gc } argument to collect memory stats.
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 3, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class StringUtilsBenchmark {

    @State(Scope.Thread)
    public static class IN {
        @Param({"simple", "trim", "one", "two", "heavy"})
        private String type;
        @Setup
        public void setup() {
            switch (type) {
                case "simple":
                    p = "No separator in";
                    break;
                case "trim":
                    p = "  whitespaces  ";
                    break;
                case "one":
                    p = "  white-space  ";
                    break;
                case "two":
                    p = " two -- dashes ";
                    break;
                case "heavy":
                    p = " - -f -f-ff - g";
                    break;
                default:
                    throw new IllegalStateException("Unknown type: " + type);
            }
        }
        public String p = "abc-def - fff- - ---ff f ff --";
        public char separator = '-';
    }

    @Benchmark
    public Object split(IN in) {
        return StringUtils.split(in.p, in.separator);
    }
}
