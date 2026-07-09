package org.openl.studio.projects.model;

/**
 * Request to generate OpenL rules and datatype tables from an OpenAPI spec already committed to the
 * project ("tables generation" import). The spec at {@code specPath} is converted into two modules:
 * a rules module and a datatype module, written to the given paths.
 *
 * @author Yury Molchan
 */
public record OpenApiTablesRequest(
        String specPath,
        String rulesModuleName,
        String dataModuleName,
        String rulesModulePath,
        String dataModulePath) {
}
