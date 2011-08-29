package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotEmpty;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.domaintree.DomainTree;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DatatypeTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.richfaces.component.UIDataTable;

/**
 * @author Andrei Astrouski
 */
public class DatatypeTableCreationWizard extends BusinessTableCreationWizard {

    @NotEmpty(message="Technical name can not be empty")
    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9]*)?", message="Invalid table name")
    private String technicalName;

    @Valid
    private List<TypeNamePair> parameters = new ArrayList<TypeNamePair>();

    private DomainTree domainTree;
    private SelectItem[] definedDatatypes;
    private SelectItem[] domainTypes;
    private String parent;

    private UIDataTable parametersTable;

    public DatatypeTableCreationWizard() {
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public void setTechnicalName(String technicalName) {
        this.technicalName = technicalName;
    }

    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public DomainTree getDomainTree() {
        return domainTree;
    }

    public SelectItem[] getDefinedDatatypes() {
        return definedDatatypes;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    public UIDataTable getParametersTable() {
        return parametersTable;
    }

    public void setParametersTable(UIDataTable parametersTable) {
        this.parametersTable = parametersTable;
    }

    @Override
    public String getName() {
        return "newDatatypeTable";
    }

    @Override
    protected void onStart() {
        reset();

        domainTree = DomainTree.buildTree(WizardUtils.getProjectOpenClass());
        List<String> datatypes = new ArrayList<String>(WizardUtils.getProjectOpenClass().getTypes().size());
        datatypes.add("");
        for(IOpenClass datatype : WizardUtils.getProjectOpenClass().getTypes().values()){
        	
        	if (!(datatype instanceof DomainOpenClass)) {
				datatypes.add(datatype.getName());
			}

        }
        definedDatatypes = FacesUtils.createSelectItems(datatypes);
        domainTypes = FacesUtils.createSelectItems(domainTree.getAllClasses(true));

        addParameter();
    }

    @Override
    protected void onCancel() {
        reset();
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        DatatypeTableBuilder builder = new DatatypeTableBuilder(gridModel);

        Map<String, Object> properties = buildProperties();

        int width = DatatypeTableBuilder.MIN_WIDTH;
        if (!properties.isEmpty()) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }
        int height = TableBuilder.HEADER_HEIGHT + properties.size() + parameters.size();

        builder.beginTable(width, height);

        builder.writeHeader(technicalName, parent);
        builder.writeProperties(properties, null);

        for (TypeNamePair parameter : parameters) {
            String paramType = parameter.getType();
            if (parameter.isIterable()) {
                paramType += "[]";
            }
            builder.writeParameter(paramType, parameter.getName());
        }

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return uri;
    }

    @Override
    protected void onStepFirstVisit(int step) {
        switch (step) {
            case 4:
                initWorkbooks();
                break;
        }
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void removeParameter() {
        TypeNamePair parameter = (TypeNamePair) parametersTable.getRowData();
        parameters.remove(parameter);
    }

    @Override
    protected void reset() {
        technicalName = null;
        parameters = new ArrayList<TypeNamePair>();

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
