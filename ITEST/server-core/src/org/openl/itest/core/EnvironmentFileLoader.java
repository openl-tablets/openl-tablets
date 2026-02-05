package org.openl.itest.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and merges hierarchical environment files (itest.env) from directory trees.
 * Uses a stack-based approach to avoid re-reading env files when navigating folders.
 * <p>
 * File format:
 * <ul>
 *   <li>KEY=VALUE pairs, one per line</li>
 *   <li>Lines starting with # are comments</li>
 *   <li>Empty value (KEY=) sets the variable to empty string</li>
 *   <li>Quoted values (KEY="value" or KEY='value') have quotes stripped</li>
 *   <li>Child folder values override parent folder values</li>
 * </ul>
 *
 * @author OpenL Tablets
 */
public final class EnvironmentFileLoader {

    public static final String ENV_FILE_NAME = "itest.env";

    private final Path rootPath;
    private final Deque<StackEntry> envStack = new ArrayDeque<>();

    /**
     * Creates a new loader for the given root path.
     *
     * @param rootPath the root test resources directory
     */
    public EnvironmentFileLoader(Path rootPath) {
        this.rootPath = rootPath.normalize();
        // Initialize with root folder environment
        Map<String, String> rootEnv = parseEnvFile(this.rootPath.resolve(ENV_FILE_NAME));
        envStack.push(new StackEntry(this.rootPath, rootEnv));
    }

    /**
     * Updates the environment stack to match the target folder and returns the merged environment.
     * Only reads new env files when entering deeper folders; reuses cached values otherwise.
     *
     * @param targetFolder the folder to navigate to
     * @return merged environment map (parent values overridden by child values)
     */
    public Map<String, String> navigateTo(Path targetFolder) {
        targetFolder = targetFolder.normalize();

        // Find common ancestor - pop stack until we find it
        while (envStack.size() > 1 && !targetFolder.startsWith(envStack.peek().folder)) {
            envStack.pop();
        }

        // Current top of stack is the common ancestor (or root)
        Path currentPath = envStack.peek().folder;

        // If target is the current path, we're done
        if (currentPath.equals(targetFolder)) {
            return getMergedEnvironment();
        }

        // Push new folders from current to target
        Path relativePath = currentPath.relativize(targetFolder);
        for (Path segment : relativePath) {
            currentPath = currentPath.resolve(segment);
            Map<String, String> folderEnv = parseEnvFile(currentPath.resolve(ENV_FILE_NAME));
            envStack.push(new StackEntry(currentPath, folderEnv));
        }

        return getMergedEnvironment();
    }

    /**
     * Returns the merged environment from all levels in the stack.
     */
    private Map<String, String> getMergedEnvironment() {
        Map<String, String> merged = new LinkedHashMap<>();
        // Iterate from bottom to top (root to current folder)
        for (var it = envStack.descendingIterator(); it.hasNext(); ) {
            merged.putAll(it.next().env);
        }
        return merged;
    }

    /**
     * Parses environment from a single file.
     *
     * @param envFile path to the env file
     * @return parsed environment map, or empty map if file doesn't exist
     */
    private static Map<String, String> parseEnvFile(Path envFile) {
        Map<String, String> env = new LinkedHashMap<>();
        if (!Files.isRegularFile(envFile)) {
            return env;
        }

        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = stripQuotes(line.substring(equalsIndex + 1));
                    if (!key.isEmpty()) {
                        env.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read env file: " + envFile, e);
        }
        return env;
    }

    /**
     * Strips surrounding quotes (single or double) from a value.
     * Quotes are syntax delimiters, not part of the value.
     */
    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Stack entry holding folder path and its environment variables.
     */
    private record StackEntry(Path folder, Map<String, String> env) {
    }
}
