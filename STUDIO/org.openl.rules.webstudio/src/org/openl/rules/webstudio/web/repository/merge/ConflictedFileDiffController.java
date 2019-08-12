package org.openl.rules.webstudio.web.repository.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.validation.ValidationException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.webstudio.web.diff.ExcelDiffController;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@ManagedBean
@SessionScoped
public class ConflictedFileDiffController extends ExcelDiffController {
    private static final Pattern LINES_RANGE_PATTERN = Pattern
        .compile("@@\\s*-(\\d+)(,\\d+)?\\s+\\+(\\d+)(,(\\d+))?\\s*@@");
    private final Logger log = LoggerFactory.getLogger(ConflictedFileDiffController.class);

    @ManagedProperty(value = "#{workspaceManager}")
    private MultiUserWorkspaceManager workspaceManager;
    private String conflictedFile;

    public void setConflictedFile(String conflictedFile) {
        try {
            deleteTempFiles();
            setDiffTree(null);
            clearTreeSelection();

            this.conflictedFile = conflictedFile;

            if (StringUtils.isBlank(conflictedFile)) {
                return;
            }

            if (!isExcelFile(conflictedFile)) {
                return;
            }

            MergeConflictInfo mergeConflict = getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                WorkspaceUser user = new WorkspaceUserImpl(
                    SecurityContextHolder.getContext().getAuthentication().getName());
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

                FileItem their = userWorkspace.getDesignTimeRepository()
                    .getRepository()
                    .readHistory(conflictedFile, exception.getTheirCommit());
                File theirFile = createTempFile(their, conflictedFile);

                String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                String localName = conflictedFile.substring(rulesLocation.length());
                FileItem our = userWorkspace.getLocalWorkspace().getRepository().read(localName);
                File ourFile = createTempFile(our, localName);

                compare(Arrays.asList(ourFile, theirFile));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(e.getMessage(), e);
        }
    }

    private void clearTreeSelection() {
        UIComponent treeComponent = RichFunction.findComponent("newTree");
        if (treeComponent instanceof UITree) {
            ((UITree) treeComponent).setSelection(Collections.emptyList());
        }
    }

    private File createTempFile(FileItem item, String fullName) throws FileNotFoundException {
        if (item == null) {
            throw new FileNotFoundException("File " + fullName + " is not found");
        }
        File ourFile = FileTool.toTempFile(item.getStream(), FileUtils.getName(fullName));
        if (ourFile == null) {
            throw new FileNotFoundException("Can't create temp file for '" + fullName + "'");
        }
        addTempFile(ourFile);
        return ourFile;
    }

    @PreDestroy
    public void destroy() {
        deleteTempFiles();
        setDiffTree(null);
    }

    public void close() {
        setFilesToCompare(Collections.emptyList());
        deleteTempFiles();
        setDiffTree(null);
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public List<DiffSet> getDiffSets() {
        if (StringUtils.isBlank(conflictedFile) || isExcelFile(conflictedFile)) {
            return Collections.emptyList();
        }

        MergeConflictInfo mergeConflict = getMergeConflict();
        if (mergeConflict != null) {
            MergeConflictException exception = mergeConflict.getException();
            String diffText = exception.getDiffs().get(conflictedFile);
            if (diffText == null) {
                return Collections.emptyList();
            }

            List<DiffSet> result = new ArrayList<>();
            String[] lines = diffText.split("\\r?\\n");

            List<DiffLine> diffLines = new ArrayList<>();
            int theirLine = 0;
            int ourLine = 0;
            DiffType previousType = null;
            for (String line : lines) {
                Matcher matcher = LINES_RANGE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    diffLines = new ArrayList<>();
                    theirLine = Integer.parseInt(matcher.group(1)) - 1;
                    ourLine = Integer.parseInt(matcher.group(3)) - 1;
                    String ourLineCountStr = matcher.group(5);
                    int ourLineCount = ourLineCountStr.isEmpty() ? 1 : Integer.parseInt(ourLineCountStr);
                    result.add(new DiffSet(diffLines, ourLine + 1, ourLine + ourLineCount));
                } else {
                    if (line.isEmpty()) {
                        continue;
                    }
                    char c = line.charAt(0);

                    boolean noEndOfFile = false;
                    DiffType type;
                    switch (c) {
                        case '-':
                            type = DiffType.THEIR;
                            theirLine++;
                            previousType = type;
                            break;
                        case '+':
                            type = DiffType.OUR;
                            ourLine++;
                            previousType = type;
                            break;
                        case ' ':
                            type = DiffType.COMMON;
                            theirLine++;
                            ourLine++;
                            previousType = type;
                            break;
                        case '\\':
                            type = previousType;
                            if (type == DiffType.THEIR || type == DiffType.COMMON) {
                                theirLine++;
                            }
                            if (type == DiffType.OUR || type == DiffType.COMMON) {
                                ourLine++;
                            }
                            noEndOfFile = true;
                            break;
                        default:
                            log.warn("Line \"{}\" starts with unsupported character '{}'", line, c);
                            type = DiffType.COMMON;
                            break;
                    }
                    diffLines.add(new DiffLine(line.substring(1), theirLine, ourLine, type, noEndOfFile));
                }
            }

            return result;
        }

        return Collections.emptyList();
    }

    private MergeConflictInfo getMergeConflict() {
        return (MergeConflictInfo) FacesUtils.getSessionMap().get(Constants.SESSION_PARAM_MERGE_CONFLICT);
    }

    private boolean isExcelFile(String conflictedFile) {
        return conflictedFile.endsWith(".xls") || conflictedFile.endsWith(".xlsx");
    }
}
