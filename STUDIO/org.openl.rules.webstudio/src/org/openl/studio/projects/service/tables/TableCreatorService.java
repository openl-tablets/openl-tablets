package org.openl.studio.projects.service.tables;

import java.util.Optional;

import org.springframework.stereotype.Service;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.rest.exception.ConflictException;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.CreateNewTableRequest;
import org.openl.studio.projects.model.tables.TableView;
import org.openl.studio.projects.service.tables.write.TableWriterExecutor;
import org.openl.studio.projects.service.tables.write.TableWritersFactory;
import org.openl.util.StringUtils;

@Service
public class TableCreatorService {

    private final TableWritersFactory tableWritersFactory;
    private final TableWriterExecutor tableWriterExecutor;

    public TableCreatorService(TableWritersFactory tableWritersFactory,
                               TableWriterExecutor tableWriterExecutor) {
        this.tableWritersFactory = tableWritersFactory;
        this.tableWriterExecutor = tableWriterExecutor;
    }


    public void createTable(CreateNewTableRequest createTableRequest, ProjectModel projectModel) {
        var table = (TableView) createTableRequest.table();
        if (!validateUniqueness(projectModel, table.name)) {
            throw new ConflictException("table.exists.message", table.name);
        }

        var gridModel = getXlsSheetGridModel(createTableRequest, projectModel);
        var tableWriter = tableWritersFactory.getNewTableWriter(table, gridModel);
        tableWriterExecutor.executeWrite(tableWriter, createTableRequest.table());
    }

    private static boolean validateUniqueness(ProjectModel model, String tableName) {
        return model.getAllTableSyntaxNodes().stream()
                .noneMatch(node -> Optional.ofNullable(node.getMember())
                        .map(INamedThing::getName)
                        .map(name -> name.equalsIgnoreCase(tableName))
                        .orElse(false));
    }

    private static XlsSheetGridModel getXlsSheetGridModel(CreateNewTableRequest createTableRequest, ProjectModel projectModel) {
        var currentWorkbook = projectModel.getXlsModuleNode().getWorkbookSyntaxNodes()[0]
                .getWorkbookSourceCodeModule();

        var excelWorkbook = currentWorkbook.getWorkbook();
        var sheetName = Optional.ofNullable(createTableRequest.sheetName())
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> ((TableView) createTableRequest.table()).name);
        var sheet = excelWorkbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = excelWorkbook.createSheet(sheetName);
        }

        var sourceCodeModule = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), currentWorkbook);
        return new XlsSheetGridModel(sourceCodeModule);
    }
}
