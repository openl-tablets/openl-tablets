package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.upload.RootFolderExtractor;
import org.openl.rules.workspace.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tries to detect charset for zips compressed with non-UTF-8 encoding. First of all it iterates all entries
 * to check that they can be opened with UTF-8. If it was unsuccessful, it uses rules.xml module paths to check
 * correctness of chosen encoding. If rules.xml is absent it compares file paths in the zip with existing project files
 * (when updating existing project with zip).
 */
public class ZipCharsetDetector {
    private final Logger log = LoggerFactory.getLogger(ZipCharsetDetector.class);
    private final Charset[] charsets;
    private final PathFilter zipFilter;
    private final IProjectDescriptorSerializer projectDescriptorSerializer = new XmlProjectDescriptorSerializer(false);

    /**
     * Create zip charset detector.
     *
     * @param charsetNames additional charsets to check.
     * @param zipFilter path filter to filter out technical folders. If null, all files in the zip will be accepted.
     */
    public ZipCharsetDetector(String[] charsetNames, PathFilter zipFilter) {
        this.charsets = getAvailableCharsets(charsetNames);
        this.zipFilter = zipFilter;
    }

    /**
     * Detect charset for the given zip. File names will be compared with rules.xml if it exists.
     *
     * @param source source for zip.
     * @return Detected encoding. If null is returned then it means that charset isn't UTF-8 but charset can't be
     *         detected
     */
    public Charset detectCharset(ZipSource source) {
        return detectCharset(source, null);
    }

    /**
     * Detect charset for given zip. File names will be compared with rules.xml if it exists. If it absents zip files
     * will be compared with <code>existingFiles</code>
     *
     * @param source source for zip.
     * @param existingFiles Existing file names to check. Can be null.
     * @return Detected encoding. If null is returned then it means that charset isn't UTF-8 but charset can't be
     *         detected
     */
    public Charset detectCharset(ZipSource source, Collection<String> existingFiles) {
        // Check if zip stream can be opened with UTF-8 without error
        try (ZipInputStream zipInputStream = new ZipInputStream(source.createStream())) {
            // Just iterate all entries to check that there is no any error
            while (true) {
                if (zipInputStream.getNextEntry() == null) {
                    break;
                }
            }
            // If there is no any error, zip can be decompressed using UTF-8
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            log.debug("UTF-8 charset can't be used for zip decoding: {}", e.getMessage(), e);
        }

        // Check other charsets
        if (charsets.length > 0) {
            Charset defaultCharset = charsets[0];
            final List<String> defaultEntryNames = getEntryNames(source, defaultCharset);

            Collection<String> projectDescriptorFiles = getRulesXmlFiles(source,
                defaultCharset,
                new RootFolderExtractor(new HashSet<>(defaultEntryNames), zipFilter));

            Collection<String> filesToCompare = new HashSet<>();
            if (projectDescriptorFiles != null) {
                filesToCompare.addAll(projectDescriptorFiles);
            }
            if (existingFiles != null) {
                filesToCompare.addAll(existingFiles);
            }

            Charset bestCharset = null;
            double bestRatio = 0;
            for (Charset charset : charsets) {
                try {
                    Collection<String> fileNames = convertEntryNames(defaultEntryNames, defaultCharset, charset);
                    if (!checkNames(fileNames)) {
                        continue;
                    }

                    if (bestCharset == null) {
                        bestCharset = charset;
                    }

                    if (filesToCompare.isEmpty()) {
                        // Can't figure out best charset. Use first applicable.
                        break;
                    }

                    double ratio = calcRatio(fileNames, filesToCompare);

                    if (ratio > bestRatio) {
                        bestCharset = charset;
                        bestRatio = ratio;
                    }

                    if (Math.abs(1 - bestRatio) < 0.001) {
                        // Found full match. No need to iterate further.
                        break;
                    }

                } catch (Exception e) {
                    log.debug("Charset '{}' can't be used for zip decoding: {}", charset.name(), e.getMessage(), e);
                }
            }

            log.debug("Best charset: '{}'", bestCharset);
            return bestCharset;
        }

        return null;
    }

    private Collection<String> getRulesXmlFiles(ZipSource source, Charset charset, RootFolderExtractor extractor) {
        ProjectDescriptor projectDescriptor = getProjectDescriptor(source, charset, extractor);
        if (projectDescriptor != null) {
            Set<String> files = new HashSet<>();
            for (Module module : projectDescriptor.getModules()) {
                String path = module.getRulesRootPath().getPath();
                if (!path.contains("*") && !path.contains("?")) {
                    // Modules with wildcard aren't supported for now
                    files.add(path);
                }
            }

            return files;
        }

        return null;
    }

    private ProjectDescriptor getProjectDescriptor(ZipSource source, Charset charset, RootFolderExtractor extractor) {
        try (ZipInputStream zipInputStream = new ZipInputStream(source.createStream(), charset)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String fileName = extractor.extractFromRootFolder(entryName);
                if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(fileName)) {
                    return projectDescriptorSerializer.deserialize(zipInputStream);
                }
            }
        } catch (Exception e) {
            log.debug("Can't read project descriptor. Skip it. Cause: {}", e.getMessage(), e);
        }
        return null;
    }

    private boolean checkNames(Collection<String> entryNames) {
        for (String entryName : entryNames) {
            for (String name : entryName.split("/")) {
                if (!NameChecker.checkName(name)) {
                    return false;
                }
            }
        }

        return true;
    }

    private double calcRatio(Collection<String> files1, Collection<String> files2) {
        if (files2.size() < files1.size()) {
            Collection<String> temp = files1;
            files1 = files2;
            files2 = temp;
        }

        int total = files1.size();

        int found = 0;
        for (String file : files1) {
            if (files2.contains(file)) {
                found++;
            }
        }

        return (double) found / total;
    }

    /**
     * 1) Convert entry names from one charset to another. 2) Extract folder names from root (when all files are inside
     * one folder) 3) Skip filtered out files
     *
     * @param entryNames all entry names
     * @param from source charset
     * @param to target charset
     * @return folder names as they must be located in the project from the root
     */
    private Collection<String> convertEntryNames(List<String> entryNames, Charset from, Charset to) {
        Set<String> converted = new HashSet<>(entryNames.size());
        for (String entryName : entryNames) {
            // Convert entry name to another charset
            converted.add(from == to ? entryName : new String(entryName.getBytes(from), to));
        }

        RootFolderExtractor folderExtractor = new RootFolderExtractor(converted, zipFilter);

        Set<String> result = new HashSet<>();
        for (String name : converted) {
            String extracted = folderExtractor.extractFromRootFolder(name);
            // Can be null if it was filtered out
            if (extracted != null) {
                result.add(extracted);
            }
        }

        return result;
    }

    private List<String> getEntryNames(ZipSource source, Charset charset) {
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(source.createStream(), charset)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // Only files are useful
                    entryNames.add(entry.getName());
                }
            }
        } catch (Exception e) {
            log.debug("Charset '{}' can't be used for zip decoding: {}", charset.name(), e.getMessage(), e);
        }
        return entryNames;
    }

    private Charset[] getAvailableCharsets(String[] charsetNames) {
        LinkedHashSet<Charset> available = new LinkedHashSet<>();

        if (charsetNames != null) {
            for (String charsetName : charsetNames) {
                try {
                    available.add(Charset.forName(charsetName));
                } catch (UnsupportedCharsetException e) {
                    log.debug("Charset '{}' isn't supported: {}", charsetName, e.getMessage(), e);
                }
            }

        }

        if (available.isEmpty()) {
            log.warn("Can't detect zip charset if it's compressed with any non-UTF-8 encoding");
        }

        return available.toArray(new Charset[0]);
    }

    public interface ZipSource {
        /**
         * Create new input stream for zip. Can be invoked several times.
         *
         * @return new input stream.
         */
        InputStream createStream() throws FileNotFoundException;
    }
}
