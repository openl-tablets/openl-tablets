package org.openl.studio.projects.service.tables;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;

import org.openl.rules.repository.api.Pageable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.projects.model.tables.TableInputView;
import org.openl.studio.projects.model.tables.TestCaseView;

/**
 * Describes the input a table takes to be executed.
 *
 * <p>A rule table is described by its declared parameters and by the runtime context when the project provides
 * one. A test table carries cases instead, which are read a page at a time.
 *
 * <p>A page of cases carries plain values only. A value with inner structure is read with its case, one case at a
 * time.
 */
public interface TableInputService {

    /**
     * Describes the input of a table.
     *
     * @param projectModel        model the table is compiled in
     * @param table               the table
     * @param currentOpenedModule whether the table is resolved within the opened module only
     * @param objectMapper        mapper configured for the project, writes the parameter values
     * @param schemaGenerator     generator that describes the parameter types
     * @return the input description
     * @throws NotFoundException when the table has no executable method
     */
    TableInputView describe(ProjectModel projectModel,
                            IOpenLTable table,
                            boolean currentOpenedModule,
                            ObjectMapper objectMapper,
                            SchemaGenerator schemaGenerator);

    /**
     * Lists a page of the cases of a test table, in the order the table writes them.
     *
     * <p>A value with inner structure is marked lazy and left out of the page. It is read with its case.
     *
     * @param projectModel        model the table is compiled in
     * @param table               the test table
     * @param currentOpenedModule whether the table is resolved within the opened module only
     * @param page                page to list
     * @param objectMapper        mapper configured for the project, writes the case values
     * @param schemaGenerator     generator that describes the parameter types
     * @return the page of cases, with the number of cases the table holds
     * @throws NotFoundException when the table is not a test table
     */
    PageResponse<TestCaseView> listTestCases(ProjectModel projectModel,
                                             IOpenLTable table,
                                             boolean currentOpenedModule,
                                             Pageable page,
                                             ObjectMapper objectMapper,
                                             SchemaGenerator schemaGenerator);

    /**
     * Describes one case of a test table with every value written in full.
     *
     * @param projectModel        model the table is compiled in
     * @param table               the test table
     * @param currentOpenedModule whether the table is resolved within the opened module only
     * @param caseId              id of the case, as the case list names it
     * @param objectMapper        mapper configured for the project, writes the case values
     * @param schemaGenerator     generator that describes the parameter types
     * @return the case
     * @throws NotFoundException when the table is not a test table or has no case with the id
     */
    TestCaseView describeTestCase(ProjectModel projectModel,
                                  IOpenLTable table,
                                  boolean currentOpenedModule,
                                  String caseId,
                                  ObjectMapper objectMapper,
                                  SchemaGenerator schemaGenerator);
}
