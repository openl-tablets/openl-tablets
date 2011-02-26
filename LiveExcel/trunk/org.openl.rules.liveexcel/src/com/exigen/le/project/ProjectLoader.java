package com.exigen.le.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PathUtils;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.project.ProjectElement.ElementType;
import com.exigen.le.project.cache.Cache;
import com.exigen.le.project.cache.SimpleCache;
import com.exigen.le.usermodel.LiveExcelWorkbookFactory;

public class ProjectLoader {
    private static final Log LOG = LogFactory.getLog(ProjectLoader.class);

    private static HashMap<ProjectElement.ElementType, ElementFactory> creators = initCreators();

    private static ThreadLocal<Cache<String, ProjectElement>> elementsCache = new ThreadLocal<Cache<String, ProjectElement>>() {
        protected Cache<String, ProjectElement> initialValue() {
            return new SimpleCache<String, ProjectElement>();
        };
    };

    private static ThreadLocal<File> tempDir = new ThreadLocal<File>() {
        protected File initialValue() {
            return new File(createTempDir());
        };
        
        protected void finalize() throws Throwable {
            try {
                FileUtils.deleteDirectory(get());
            } catch (IOException e) {
                LOG.warn(String.format("Failed to clean temporary directory \"%s\"", get().getAbsolutePath()));
            }
        };
    };

    public static File getTempDir() {
        if(tempDir.get() == null){
            tempDir.set(new File(createTempDir()));
        }
        if(!tempDir.get().exists()){
            tempDir.get().mkdirs();
        }
        return tempDir.get();
    }

    private static HashMap<ElementType, ElementFactory> initCreators() {
        HashMap<ElementType, ElementFactory> result = new HashMap<ElementType, ElementFactory>();
        for (ElementType type : ProjectElement.ElementType.values()) {
            result.put(type, getDefaultCreators(type));
        }
        return result;
    }

    private static ElementFactory getDefaultCreators(ElementType type) {
        ElementFactory result = null;
        switch (type) {
            case WORKBOOK:
                result = LiveExcelWorkbookFactory.getInstance();
                break;
            case MAPPING:
                result = null;
                break;
            case TABLE:
                result = new LETableFactory();
                break;
            default:
                result = null;
                break;
        }
        return result;
    }

    /**
     * Register new Project Element creator
     * 
     * @param type
     * @param creator
     */
    public static void registerElementFactory(ProjectElement.ElementType type, ElementFactory creator) {
        creators.put(type, creator);

    }

    /**
     * Unregister creator for Element type
     * 
     * @param type
     */
    public static void unRegisterElementFactory(ProjectElement.ElementType type) {
        creators.put(type, getDefaultCreators(type));
    }

    public static List<ProjectElement> retrieveElementList(File projectLocation) {
        List<ProjectElement> result = new ArrayList<ProjectElement>();
        File[] elems = projectLocation.listFiles();
        if (elems != null && elems.length > 0) { // project not empty and
            for (File elem : elems) {
                try {
                    int ind = elem.getName().indexOf(".");
                    if (ind != (-1)) {
                        String ext = elem.getName().substring(ind);
                        ElementType type = ProjectElement.ElementType.getByExtension(ext);
                        ProjectElement element = new ProjectElement(elem.getName(), type);
                        result.add(element);
                    }
                } catch (Exception e) {
                }
            }

        }
        return result;

    }

    public static ProjectElement getFullElement(File projectLocation, String fileName, ElementType type) {
        if (elementsCache.get().getKeys().contains(fileName)) {
            return elementsCache.get().get(fileName);
        }
        File[] elems = projectLocation.listFiles();
        if (elems != null && elems.length > 0) { // project not empty and
            for (File elem : elems) {
                if (elem.getName().equalsIgnoreCase(fileName)) {
                    try {
                        int ind = elem.getName().indexOf(".");
                        if (ind != (-1)) {
                            String ext = elem.getName().substring(ind);
                            ElementType elementType = ProjectElement.ElementType.getByExtension(ext);
                            if (elementType == type) {
                                ProjectElement element = creators.get(type).create(elem.getName(),
                                        new FileInputStream(elem), ThreadEvaluationContext.getServiceModel());
                                elementsCache.get().put(fileName, element);
                                return element;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }

        }
        return null;

    }

    public static InputStream getExcelFile(File projectLocation, String workbookName) {
        String excelFileName = getExcelFileName(projectLocation, workbookName);
        try {
            return new FileInputStream(new File(projectLocation, excelFileName));
        } catch (Exception e) {
            LOG.warn("Failed to get excel file.", e);
            return null;
        }
    }

    public static String getExcelFileName(File projectLocation, String workbookName) {
        try {
            File xlsFile = new File(projectLocation, workbookName);
            if (xlsFile.exists()) {
                return xlsFile.getName();
            }
            xlsFile = new File(projectLocation, workbookName + ".xlsm");
            if (xlsFile.exists()) {
                return xlsFile.getName();
            }
            xlsFile = new File(projectLocation, workbookName + ".xlsx");
            if (xlsFile.exists()) {
                return xlsFile.getName();
            }
            xlsFile = new File(projectLocation, workbookName + ".xls");
            if (xlsFile.exists()) {
                return xlsFile.getName();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Workbook getWorkbook(File projectLocation, String name) {
        String workbookName = PathUtils.extractFile(name);
        ProjectElement workbookProjectElement = getFullElement(projectLocation,
                getExcelFileName(projectLocation, workbookName), ElementType.WORKBOOK);
        if (workbookProjectElement != null) {
            return (Workbook) workbookProjectElement.getElement();
        } else {
            return null;
        }
    }

    public static String createTempDir() {
        try {
            File dir = File.createTempFile("LEtmp", ".dir");
            dir.delete();
            boolean success = dir.mkdir();
            if (success) {
                dir.deleteOnExit();
                return dir.getAbsolutePath();
            } else {
                String msg = "Could not create tmp directory";
                throw new RuntimeException(msg);
            }
        } catch (Exception e) {
            String msg = "Could not create tmp directory";
            throw new RuntimeException(msg, e);
        }
    }

    public static void reset() {
        creators = initCreators();
        for (ProjectElement element : elementsCache.get().getValues()) {
            element.dispose();
        }
        elementsCache.get().removeAll();
        File currentTempDir = getTempDir();
        try {
            FileUtils.cleanDirectory(currentTempDir);
        } catch (IOException e) {
            LOG.warn(String.format("Failed to clean temporary directory \"%s\"", currentTempDir.getAbsolutePath()));
        }
    }
}
