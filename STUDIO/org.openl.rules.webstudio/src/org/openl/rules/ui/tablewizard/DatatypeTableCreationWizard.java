package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DatatypeTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.util.StringUtils;

/**
 * @author Andrei Astrouski
 */
public class DatatypeTableCreationWizard extends TableCreationWizard {

    @NotBlank(message = "Cannot be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = INVALID_NAME_MESSAGE)
    private String technicalName;

    @Valid
    private List<TypeNamePair> parameters = new ArrayList<>();

    private List<String> definedDatatypes;
    private List<String> domainTypes;
    private String parent;

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

    public List<String> getDefinedDatatypes() {
        return definedDatatypes;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public List<String> getDomainTypes() {
        return domainTypes;
    }

    @Override
    public String getName() {
        return "newDatatypeTable";
    }

    @Override
    protected void onStart() {
        reset();

        definedDatatypes = new ArrayList<>(WizardUtils.declaredDatatypes());
        definedDatatypes.add("");
        WizardUtils.getImportedClasses().forEach(c -> {
            if (!Modifier.isFinal(c.getInstanceClass().getModifiers()))
                definedDatatypes.add(c.getDisplayName(INamedThing.SHORT));
        });

        domainTypes = new ArrayList<>(WizardUtils.predefinedTypes());
        domainTypes.add("");
        domainTypes.addAll(WizardUtils.declaredDatatypes());
        domainTypes.add("");
        domainTypes.addAll(WizardUtils.declaredAliases());
        domainTypes.add("");
        domainTypes.addAll(WizardUtils.importedClasses());

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
        parameters = new ArrayList<>();

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

    public void nameValidator(FacesContext context, UIComponent toValidate, Object value) {
        String text = (String) value;
        if (StringUtils.isBlank(text)) {
            throw new ValidatorException(new FacesMessage("Cannot be empty"));
        }

        String[] idParts = toValidate.getClientId().split(":");
        int inputNum = Integer.parseInt(idParts[idParts.length - 2]);
        parameters.get(inputNum).setSubmittedName(text);

        for (int i = 0; i < inputNum; i++) {
            if (text.equals(parameters.get(i).getSubmittedName())) {
                throw new ValidatorException(new FacesMessage("Parameter '" + text + "' already exists"));
            }
        }
    }
}
