package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.component.html.HtmlDataTable;
import javax.faces.model.SelectItem;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DatatypeTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.OpenClassDelegator;

/**
 * @author Andrei Astrouski
 */
public class DatatypeTableCreationWizard extends TableCreationWizard {

    @NotBlank(message="Can not be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = INVALID_NAME_MESSAGE)
    private String technicalName;

    @Valid
    private List<TypeNamePair> parameters = new ArrayList<TypeNamePair>();

    private DomainTree domainTree;
    private SelectItem[] definedDatatypes;
    private SelectItem[] domainTypes;
    private String parent;
    private int definedDatatypesLength;

    private HtmlDataTable parametersTable;

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

    public HtmlDataTable getParametersTable() {
        return parametersTable;
    }

    public void setParametersTable(HtmlDataTable parametersTable) {
        this.parametersTable = parametersTable;
    }

    public int getDefinedDatatypesLength() {
        return definedDatatypes.length;
    }

    public void setDefinedDatatypesLength(int definedDatatypesLength) {
        this.definedDatatypesLength = definedDatatypesLength;
    }

    @Override
    public String getName() {
        return "newDatatypeTable";
    }

    @Override
    protected void onStart() {
        reset();

        domainTree = DomainTree.buildTree(WizardUtils.getProjectOpenClass());
        
        List<IOpenClass> types = new ArrayList<IOpenClass>(WizardUtils.getProjectOpenClass().getTypes().values());
        Collection<IOpenClass> importedClasses = WizardUtils.getImportedClasses();
        types.addAll(importedClasses);
        
        List<String> datatypes = new ArrayList<String>(types.size());
        datatypes.add("");
        for (IOpenClass datatype : types) {
            if (Modifier.isFinal(datatype.getInstanceClass().getModifiers())) {
                // cannot inherit from final class
                continue;
            }

            if (!(datatype instanceof DomainOpenClass)) {
                datatypes.add(datatype.getDisplayName(INamedThing.SHORT));
            }
        }
        
        definedDatatypes = FacesUtils.createSelectItems(datatypes);
        Collection<String> allClasses = domainTree.getAllClasses();
        for (IOpenClass type : importedClasses) {
            if (type instanceof OpenClassDelegator) {
                allClasses.add(type.getName());
            } else {
                allClasses.add(type.getDisplayName(INamedThing.SHORT));
            }
        }
        domainTypes = FacesUtils.createSelectItems(allClasses);

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
            case 3:
                initWorkbooks();
                break;
        }
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void removeParameter(TypeNamePair parameter) {
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
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    public boolean containsRemoveLink(Map<String, String> params) {
        if (params == null)
            return false;
        for (String param : params.keySet()) {
            if (param.endsWith("removeLink")) {
                return true;
            }
        }
        return false;
    }

}
