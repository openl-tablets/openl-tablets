package org.openl.codegen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Generates file in outFileLocation by inserting code into predefined places in input file inFileLocation. Insertion
 * places are defined by INSERT_TAG. The insertion logic is handled by ICodeGenAdaptor, there could be multiple
 * INSERT_TAGS in the code, calling class can redefine INSERT_TAG value
 *
 * @author snshor Created Jul 27, 2009
 *
 */

public class FileCodeGen {

    private static final String DEFAULT_INSERT_TAG = "<<< INSERT";
    private static final String DEFAULT_END_INSERT_TAG = "<<< END INSERT";

    private String inFileLocation;
    private String outFileLocation;
    private String insertTag;

    public FileCodeGen(String inFileLocation, String outFileLocation) {
        this.inFileLocation = inFileLocation;
        this.outFileLocation = outFileLocation == null ? inFileLocation : outFileLocation;
        this.insertTag = insertTag == null ? DEFAULT_INSERT_TAG : insertTag;

    }

    private String getEndInsertTag(String line) {
        return DEFAULT_END_INSERT_TAG;
    }

    public void processFile(ICodeGenAdaptor cga) throws IOException {
        if (inFileLocation.equals(outFileLocation)) {
            System.out.println("Processing " + inFileLocation);
        } else {
            System.out.println("Processing " + inFileLocation + " into " + outFileLocation);
        }

        StringBuilder sb = new StringBuilder(10000);
        try (BufferedReader br = new BufferedReader(new FileReader(inFileLocation))) {
            String line = null;

            Deque<String> endInsert = new LinkedList<>();

            while ((line = br.readLine()) != null) {

                if (line.contains(insertTag)) {
                    sb.append(line).append("\r\n");
                    cga.processInsertTag(line, sb);
                    endInsert.push(getEndInsertTag(line));
                }

                boolean skipTillEnd = !endInsert.isEmpty();

                if (skipTillEnd) {
                    String endTag = endInsert.peek();
                    if (line.contains(endTag)) {
                        cga.processEndInsertTag(line, sb);
                        sb.append(line.trim()).append("\r\n");
                        endInsert.pop();
                    }
                    continue;
                }
                sb.append(line).append("\r\n");

            }

            if (!endInsert.isEmpty()) {
                throw new IllegalStateException("Not processed " + endInsert);
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outFileLocation), StandardCharsets.UTF_8)) {
            bw.write(sb.toString());
        }
    }
}
