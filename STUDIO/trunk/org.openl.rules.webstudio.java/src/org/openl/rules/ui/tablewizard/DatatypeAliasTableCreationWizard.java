package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.domaintree.DomainTree;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DatatypeAliasTableBuilder;
import org.richfaces.component.html.HtmlDataTable;

/**
 * @author Andrei Astrouski
 */
public class DatatypeAliasTableCreationWizard extends WizardBase {

    @NotEmpty(message="Table name can not be empty")
    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9]*)?", message="Invalid table name")
    private String tableName;

    private String aliasType;

    @Valid
    private List<AliasValue> values = new ArrayList<AliasValue>();

    private DomainTree domainTree;
    private SelectItem[] domainTypes;

    private HtmlDataTable valuesTable;

    public DatatypeAliasTableCreationWizard() {
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getAliasType() {
        return aliasType;
    }

    public void setAliasType(String aliasType) {
        this.aliasType = aliasType;
    }

    public List<AliasValue> getValues() {
        return values;
    }

    public void setValues(List<AliasValue> values) {
        this.values = values;
    }

    public DomainTree getDomainTree() {
        return domainTree;
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    public HtmlDataTable getValuesTable() {
        return valuesTable;
    }

    public void setValuesTable(HtmlDataTable valuesTable) {
        this.valuesTable = valuesTable;
    }

    @Override
    public String getName() {
        return "newDatatypeAliasTable";
    }

    @Override
    protected void onStart() {
        reset();

        domainTree = DomainTree.buildTree(WizardUtils.getProjectOpenClass(), false);
        domainTypes = FacesUtils.createSelectItems(domainTree.getAllClasses(true));

        addValue();
    }

    @Override
    protected void onCancel() {
        reset();
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        DatatypeAliasTableBuilder builder = new DatatypeAliasTableBuilder(gridModel);

        builder.beginTable(values.size() + 1); // values + header

        builder.writeHeader(tableName, aliasType);

        for (AliasValue value : values) {
            builder.writeValue(value.getValue());
        }

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return uri;
    }

    @Override
    protected void onStepFirstVisit(int step) {
        switch (step) {
            case 3:
                initWorkbooks();
                break;
        }
    }

    public void addValue() {
        values.add(new AliasValue());
    }

    public void removeValue() {
        AliasValue value = (AliasValue) valuesTable.getRowData();
        values.remove(value);
    }

    @Override
    protected void reset() {
        tableName = null;
        values = new ArrayList<AliasValue>();

        domainTree = null;
        domainTypes = null;

        super.reset();
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableUri(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

}
