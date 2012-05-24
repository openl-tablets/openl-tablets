package org.openl.rules.project.resolving.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tries to find the rules source file for the appropriate interface.
 * 
 * @author DLiauchuk
 *
 */
public class RuleFinderInXML {
    
    private final Log log = LogFactory.getLog(RuleFinderInXML.class);
    
    private File destinationOfSeacrh;
    private SAXParser parser;
    private RuleSearcher searcherHandler;
    
    private boolean alreadyParsed = false;
    
    public RuleFinderInXML(File destinationOfSeacrh) {
        if (destinationOfSeacrh == null) {
            throw new IllegalArgumentException("Destination file for search cannot be null");
        }
        this.destinationOfSeacrh = destinationOfSeacrh;
        this.parser = initParser();
        this.searcherHandler = initSearcher();
    }

    private RuleSearcher initSearcher() {
        return new GenerateJavaBuildXmlSearcher();
    }

    private SAXParser initParser() {
        SAXParser parser = null;
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        try {
            parser = parserFactory.newSAXParser();
        } catch (Exception e) {
            log.debug("Cannot create parser", e);
        }
        return parser;
    }

    public String getRulePath(String interfaceName) {
        String result = null;
        if (StringUtils.isNotBlank(interfaceName)) {
            if (alreadyParsed) {
                result = searcherHandler.getRuleSource(interfaceName);
            } else {
                if (parser != null) {
                    try {                
                        parser.parse(destinationOfSeacrh, searcherHandler);
                        alreadyParsed = true;
                        result = searcherHandler.getRuleSource(interfaceName);                
                    } catch (Exception e) {
                        if (log.isErrorEnabled()) {
                            log.error(String.format("Cannot find rule path for interface: %s", interfaceName), e);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public static abstract class RuleSearcher extends DefaultHandler {
       public abstract String getRuleSource(String javaName);
    }
    
    /**
     * Handler for parsing {@link EclipseBasedInterfaceResolvingStrategy.GENERATE_JAVA_INTERFACE_BUILD_XML} file
     * @author DLiauchuk
     *
     */
    public static class GenerateJavaBuildXmlSearcher extends RuleSearcher {
        private static final String TASK_NAME = "openlgen";
        private static final String SRC_FILE_ATTRIBUTE = "srcFile";
        private static final String TARGET_CLASS_ATTRIBUTE = "targetClass";
        
        private Map<String, String> srcFiles = new HashMap<String, String>();
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (TASK_NAME.equalsIgnoreCase(qName)) {                    
                String sourceFile = attributes.getValue(SRC_FILE_ATTRIBUTE);
                String targetClass = attributes.getValue(TARGET_CLASS_ATTRIBUTE);
                if (StringUtils.isNotEmpty(targetClass) && StringUtils.isNotEmpty(sourceFile)) {
                    srcFiles.put(targetClass, sourceFile);
                }
            }
        }
        @Override
        public String getRuleSource(String javaName) {            
            return srcFiles.get(javaName);
        }
    }
}
