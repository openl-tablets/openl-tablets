package org.openl.studio.compare.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.compare.model.ComparisonSideRequest;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

/**
 * The Excel files of a project as a comparison reads them.
 *
 * <p>A file is read either from the working copy - the project as its own user has it now - or from a
 * revision the repository holds, on the branch that revision belongs to.
 */
@Slf4j
@Service
public class ProjectComparisonService {

    @Lookup
    public UserWorkspace getUserWorkspace() {
        // Spring overrides this method with a lookup of the bean; the stub itself never runs.
        throw new UnsupportedOperationException("Overridden by the Spring @Lookup container");
    }

    /**
     * Reads the file one side of a comparison names.
     *
     * <p>An absent side is an empty workbook of the format its name tells, and nothing is read for it.
     *
     * @param project the project the file belongs to
     * @param side    which file, and where it is read from
     * @return the content, under the name its format is read by
     * @throws BadRequestException when the file named is not an Excel file
     * @throws NotFoundException   when the project holds no such file there
     */
    public ComparisonContent read(RulesProject project, ComparisonSideRequest side) {
        if (!FileTypeHelper.isExcelFile(side.path())) {
            throw new BadRequestException("compare.file.not-excel.message");
        }
        if (side.absent()) {
            return emptyWorkbook(FileUtils.getName(side.path()));
        }
        var artefact = excelArtefacts(sideOf(project, side.branch(), side.revision())).get(side.path());
        if (artefact == null) {
            throw new NotFoundException("file.not.found.message");
        }
        try {
            return new ComparisonContent(artefact.getName(), ((AProjectResource) artefact).getContent());
        } catch (ProjectException e) {
            // The repository listed the file and then could not open it; the caller is told which one.
            throw RuntimeExceptionWrapper.wrap("Cannot read '" + side.path() + "'", e);
        }
    }

    /**
     * The project one side of a comparison reads: the working copy, or what the repository holds at a
     * revision.
     *
     * <p>A revision is read by the folder the repository keeps the project in rather than by its name,
     * because the name a user knows it by can be one a rename has not published yet.
     *
     * @throws NotFoundException when the repository has no such revision of the project
     */
    private AProject sideOf(RulesProject project, @Nullable String branch, @Nullable String revision) {
        if (StringUtils.isBlank(branch) && StringUtils.isBlank(revision)) {
            return project;
        }
        try {
            return getUserWorkspace().getDesignTimeRepository()
                    .getProjectByPath(project.getDesignRepository().getId(),
                            StringUtils.trimToNull(branch),
                            project.getRealPath(),
                            StringUtils.trimToNull(revision));
        } catch (Exception e) {
            // Whatever stopped the read - the revision is gone, the repository refused it - the user is
            // told the same thing, so what it really was is left in the log.
            log.warn("Cannot read revision '{}' of '{}' on branch '{}'", revision, project.getName(), branch, e);
            throw new NotFoundException("file.version.not.found.message");
        }
    }

    /** A workbook without sheets, in the format of the file name: the old one for {@code .xls}, else the new one. */
    private static ComparisonContent emptyWorkbook(String name) {
        try (var workbook = WorkbookFactory.create(!name.toLowerCase(Locale.ROOT).endsWith(".xls"));
             var out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new ComparisonContent(name, new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Every Excel file of the project, wherever in it it lies, by its path inside the project.
     *
     * <p>The path is the one the project writes the file under. A working copy and a revision read from a repository
     * with mapped folders name the project by different folders, so the path a file carries is not cut by either.
     */
    private static Map<String, AProjectArtefact> excelArtefacts(AProject project) {
        var excelArtefacts = new HashMap<String, AProjectArtefact>();
        collectExcelArtefacts(project.getArtefacts(), excelArtefacts);
        return excelArtefacts;
    }

    private static void collectExcelArtefacts(Collection<? extends AProjectArtefact> artefacts,
                                              Map<String, AProjectArtefact> excelArtefacts) {
        for (AProjectArtefact artefact : artefacts) {
            if (artefact instanceof AProjectFolder folder) {
                collectExcelArtefacts(folder.getArtefacts(), excelArtefacts);
            } else if (FileTypeHelper.isExcelFile(artefact.getName())) {
                excelArtefacts.put(artefact.getInternalPath(), artefact);
            }
        }
    }
}
