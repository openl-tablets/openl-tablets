package org.openl.studio.telemetry;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * The OpenTelemetry settings of the environment OpenL Studio runs in: the {@code OTEL_*} variables the OpenTelemetry
 * Java agent of the Docker image reads.
 *
 * <p>Telemetry is on when {@code OTEL_JAVAAGENT_ENABLED} is {@code true} or any {@code OTEL_EXPORTER_*} variable is
 * defined, as the image turns the agent on. {@code OTEL_JAVAAGENT_ENABLED=false} or {@code OTEL_SDK_DISABLED=true}
 * turns it off.
 *
 * <p>A signal reaches the collector over OTLP/HTTP only: its exporter ({@code OTEL_TRACES_EXPORTER},
 * {@code OTEL_METRICS_EXPORTER}) must name {@code otlp}, the default, and its protocol must be {@code http/protobuf},
 * the default, or {@code http/json}. A signal sent over gRPC is not exported by the components outside the Java agent.
 *
 * <p>A value the agent would refuse never stops OpenL Studio: a malformed, a zero or a too long timeout is taken as the
 * default one, and a malformed percent-encoding of a list value is kept as it is written.
 *
 * <p>It holds the whole environment, so it never prints it.
 *
 * @author Yury Molchan
 */
@Slf4j
@RequiredArgsConstructor
public final class OtelEnvironment {

    /** The name of the service when the environment names none, as the Docker image does. */
    private static final String DEFAULT_SERVICE_NAME = "OpenL";
    private static final String DEFAULT_ENDPOINT = "http://localhost:4318";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    /** The resource attribute that names the service. */
    public static final String SERVICE_NAME = "service.name";
    /** The prefix of the variables of the OTLP exporter. */
    private static final String OTLP = "OTEL_EXPORTER_OTLP_";
    /** A duration of the agent: a number of milliseconds, or a number with a unit. */
    private static final Pattern DURATION = Pattern.compile("(\\d++)(ms|[smhd])?");

    private final Map<String, String> env;

    /** A kind of telemetry data the collector receives at a path of its own. */
    public enum Signal {
        TRACES,
        METRICS;

        /** The part of the names of the variables and of the path of the collector, such as {@code traces}. */
        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The signal of the given {@link #id()}, in any letter case, or {@code null} for an unknown one. */
        public static @Nullable Signal of(String id) {
            return Arrays.stream(values()).filter(signal -> signal.id().equalsIgnoreCase(id)).findFirst()
                    .orElse(null);
        }
    }

    /** The settings of the environment of the running OpenL Studio. */
    public static OtelEnvironment current() {
        return new OtelEnvironment(System.getenv());
    }

    /** Whether OpenL Studio sends telemetry at all. */
    boolean enabled() {
        if (is("OTEL_JAVAAGENT_ENABLED", "false") || is("OTEL_SDK_DISABLED", "true")) {
            return false;
        }
        return is("OTEL_JAVAAGENT_ENABLED", "true")
                || env.keySet().stream().anyMatch(name -> name.startsWith("OTEL_EXPORTER_"));
    }

    /**
     * The name of a component of OpenL Studio: the name of the service with the given suffix, such as {@code -mcp}.
     * The name of the service is {@code OTEL_SERVICE_NAME}, else the {@code service.name} of
     * {@code OTEL_RESOURCE_ATTRIBUTES}, else {@code OpenL}.
     */
    public String serviceName(String suffix) {
        var name = value("OTEL_SERVICE_NAME");
        if (name == null) {
            name = pairs(value("OTEL_RESOURCE_ATTRIBUTES")).getOrDefault(SERVICE_NAME, DEFAULT_SERVICE_NAME);
        }
        return name + suffix;
    }

    /** The attributes of {@code OTEL_RESOURCE_ATTRIBUTES} but the name of the service. */
    public Map<String, String> resourceAttributes() {
        var attributes = pairs(value("OTEL_RESOURCE_ATTRIBUTES"));
        attributes.remove(SERVICE_NAME);
        return attributes;
    }

    /** Whether the signal is sent anywhere: telemetry is on and its exporter is not {@code none}. */
    public boolean sends(Signal signal) {
        return enabled() && exporters(signal).stream().anyMatch(exporter -> !"none".equals(exporter));
    }

    /** Whether the signal is sent to the collector over OTLP/HTTP. */
    public boolean exports(Signal signal) {
        var otlp = exporters(signal).contains("otlp");
        var protocol = signalValue(signal, "PROTOCOL");
        return enabled() && otlp && (protocol == null || protocol.startsWith("http/"));
    }

    /**
     * The address the collector receives the signal at: {@code OTEL_EXPORTER_OTLP_<SIGNAL>_ENDPOINT} as it is, else
     * {@code OTEL_EXPORTER_OTLP_ENDPOINT} followed by {@code /v1/<signal>}, else the collector on the local host.
     *
     * @throws IllegalArgumentException when the variable is not a URI
     */
    public URI endpoint(Signal signal) {
        var endpoint = value(OTLP + signal.name() + "_ENDPOINT");
        if (endpoint != null) {
            return URI.create(endpoint);
        }
        var base = value(OTLP + "ENDPOINT");
        base = base == null ? DEFAULT_ENDPOINT : StringUtils.stripEnd(base, "/");
        return URI.create(base + "/v1/" + signal.id());
    }

    /**
     * The headers sent to the collector with the signal: {@code OTEL_EXPORTER_OTLP_HEADERS}, a header of
     * {@code OTEL_EXPORTER_OTLP_<SIGNAL>_HEADERS} replacing the one of the same name.
     */
    public Map<String, String> headers(Signal signal) {
        var headers = pairs(value(OTLP + "HEADERS"));
        headers.putAll(pairs(value(OTLP + signal.name() + "_HEADERS")));
        return headers;
    }

    /**
     * How long the collector may take to receive the signal: {@code OTEL_EXPORTER_OTLP_TIMEOUT}, in milliseconds or
     * with a unit ({@code ms}, {@code s}, {@code m}, {@code h}, {@code d}), 10 seconds by default and in place of a
     * malformed, a zero or a too long one.
     */
    public Duration timeout(Signal signal) {
        var timeout = signalValue(signal, "TIMEOUT");
        if (timeout == null) {
            return DEFAULT_TIMEOUT;
        }
        var duration = durationOf(timeout);
        if (duration == null || duration.isZero()) {
            log.warn("The OpenTelemetry timeout '{}' is not a positive duration: 10 seconds are taken.", timeout);
            return DEFAULT_TIMEOUT;
        }
        return duration;
    }

    /** The duration a value names, or {@code null} when it names none or one longer than a duration holds. */
    private static @Nullable Duration durationOf(String value) {
        var matcher = DURATION.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        try {
            var amount = Long.parseLong(matcher.group(1));
            return switch (matcher.group(2) == null ? "ms" : matcher.group(2)) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> Duration.ofMillis(amount);
            };
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    /** The exporters of the signal, {@code otlp} by default. */
    private List<String> exporters(Signal signal) {
        var exporters = value("OTEL_" + signal.name() + "_EXPORTER");
        return exporters == null ? List.of("otlp") : Arrays.stream(exporters.split(",")).map(String::strip).toList();
    }

    /** The variable of the signal, such as {@code OTEL_EXPORTER_OTLP_TRACES_TIMEOUT}, else the one of every signal. */
    private @Nullable String signalValue(Signal signal, String setting) {
        var value = value(OTLP + signal.name() + "_" + setting);
        return value != null ? value : value(OTLP + setting);
    }

    private boolean is(String name, String expected) {
        return expected.equalsIgnoreCase(value(name));
    }

    /** The value of a variable, or {@code null} when it is not defined or blank. */
    private @Nullable String value(String name) {
        var value = env.get(name);
        return value == null || value.isBlank() ? null : value.strip();
    }

    /** The {@code key=value} pairs of a comma-separated list, with their values percent-decoded. */
    private static Map<String, String> pairs(@Nullable String list) {
        var pairs = new LinkedHashMap<String, String>();
        if (list != null) {
            for (var pair : list.split(",")) {
                var eq = pair.indexOf('=');
                if (eq > 0) {
                    pairs.put(pair.substring(0, eq).strip(), decoded(pair.substring(eq + 1).strip()));
                }
            }
        }
        return pairs;
    }

    /** A percent-decoded value, or the value as it is written when its percent-encoding is malformed. */
    private static String decoded(String value) {
        try {
            // Percent-decoding only: a '+' stays itself, as in the Base64 of a credential
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
