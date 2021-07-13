package org.openl.rules.webstudio.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.openl.CompiledOpenClass;
import org.openl.excel.parser.ExcelReader;
import org.openl.excel.parser.sax.SAXReader;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.model.Module;

// TODO: Remove this when https://bz.apache.org/bugzilla/show_bug.cgi?id=64473 will be fixed
public final class POIFixer {
    private static final Pattern ERROR_PATTERN = Pattern.compile(
        "Failed to load dependent module '(.+)': InvalidOperationException: You can't add a part with a part name derived from another part ! \\[M1\\.11]");

    private POIFixer() {
    }

    public static boolean isCorruptedModuleCanBeFixed(CompiledOpenClass compiledOpenClass, Module moduleInfo) {
        for (OpenLMessage message : compiledOpenClass.getMessages()) {
            final Matcher matcher = ERROR_PATTERN.matcher(message.getSummary());
            if (matcher.matches()) {
                // We have corrupted module.
                final String dependency = matcher.group(1);
                if (moduleInfo.getName().equals(dependency)) {
                    // Error message is for the module "moduleInfo".
                    // Check if it is fixable.
                    try (final ExcelReader reader = new SAXReader(moduleInfo.getRulesPath().toString())) {
                        reader.getSheets();
                        // Module is not corrupted. Skip it.
                        continue;
                    } catch (Exception ignored) {
                        // Verified that the module is corrupted.
                    }

                    // Check if it's the known issue
                    try (ZipFile zipFile = new ZipFile(moduleInfo.getRulesPath().toFile())) {
                        ZipEntry metadata = zipFile.getEntry("xl/metadata");
                        ZipEntry metadataXml = zipFile.getEntry("xl/metadata.xml");
                        return metadata != null && metadataXml != null;
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return false;
    }

    public static boolean fixCorruptedModule(CompiledOpenClass compiledOpenClass, Module moduleInfo) throws IOException {
       for (OpenLMessage message : compiledOpenClass.getMessages()) {
            final Matcher matcher = ERROR_PATTERN.matcher(message.getSummary());
            if (matcher.matches()) {
                // We have corrupted module.
                final String dependency = matcher.group(1);
                if (moduleInfo.getName().equals(dependency)) {
                    // Error message is for the module "moduleInfo".
                    // Try to fix it.
                    Map<String, String> props = new HashMap<>();
                    props.put("create", "false");

                    URI uri = URI.create("jar:" + moduleInfo.getRulesPath().toRealPath().toUri().toURL().toExternalForm());

                    try (FileSystem zipFs = FileSystems.newFileSystem(uri, props)) {
                        Files.delete(zipFs.getPath("xl/metadata"));
                    }

                    return true;
                }
            }
        }

       return false;
    }
}
