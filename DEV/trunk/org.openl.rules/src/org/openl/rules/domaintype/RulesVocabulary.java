package org.openl.rules.domaintype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.exception.FieldNotFoundException;
import org.openl.meta.IVocabulary;
import org.openl.meta.StringValue;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.StringTool;

public class RulesVocabulary implements IVocabulary {

    HashMap<String, IOpenClass> newTypes = new HashMap<String, IOpenClass>();

    public DomainAttribute[] getAttributes() {
        return new DomainAttribute[] {};
    }

    /**
     * Needs to be overridden by subclasses
     *
     * @return
     */
    public DomainCreator[] getDomains() {
        return new DomainCreator[] {};
    }

    public IOpenClass[] getVocabularyTypes() throws SyntaxNodeException {
        ArrayList<IOpenClass> list = makeBaseTypes();
        makeDomains(list);

        for (Iterator<IOpenClass> iterator = list.iterator(); iterator.hasNext();) {
            IOpenClass ioc = iterator.next();
            newTypes.put(ioc.getName(), ioc);

        }

        makeDomainAttributes(list);

        return list.toArray(IOpenClass.EMPTY);
    }

    ArrayList<IOpenClass> makeBaseTypes() {

        ArrayList<IOpenClass> list = new ArrayList<IOpenClass>();
        DomainCreator[] dc = getDomains();

        for (int i = 0; i < dc.length; i++) {
            IOpenClass newType = dc[i].makeDomain();
            list.add(newType);
        }

        return list;
    }

    void makeDomainAttributes(ArrayList<IOpenClass> list) throws SyntaxNodeException {
        DomainAttribute[] attributes = getAttributes();

        for (int i = 0; i < attributes.length; i++) {

            StringValue fieldName = attributes[i].getName();
            IOpenField field = attributes[i].getBase().getField(fieldName.getValue(), true);
            if (field == null) {
                try {
                    throw new FieldNotFoundException("Can not find attribute", fieldName.getValue(), null);
                } catch (FieldNotFoundException e) {
                    throw SyntaxNodeExceptionUtils.createError(null, e, null, fieldName.asSourceCodeModule());
                }
            }

            StringValue typeName = attributes[i].getNewType();

            IOpenClass newType = newTypes.get(typeName.getValue());

            // TODO add ability to access external types, not only the ones
            // defined in here (Excel), for example by adding another IOpenClass
            // field
            if (newType == null) {
                try {
                    throw new Exception("Type not found: " + typeName);
                } catch (Exception e) {
                    throw SyntaxNodeExceptionUtils.createError(null, e, null, typeName.asSourceCodeModule());
                }

            }

            // TODO check field type correctness

            String baseName = attributes[i].getBase().getName();

            // TODO change all this

            String xname = StringTool.lastToken(baseName, ".");

            ModifiableOpenClass modifiedBase = (ModifiableOpenClass) newTypes.get(xname);

            if (modifiedBase == null) {
                modifiedBase = new ModifiableOpenClass(attributes[i].getBase(), xname);
                newTypes.put(xname, modifiedBase);
                list.add(modifiedBase);
            }

            ModifiedField mf = new ModifiedField(field, newType);

            modifiedBase.addField(mf);

        }
    }

    // public DomainOpenClass[] makeDomains()
    // {
    // List<DomainOpenClass> list = new ArrayList<DomainOpenClass>();
    // makeDomains(list);
    // return list.toArray(new DomainOpenClass[0]);
    // }

    public void makeDomains(
    @SuppressWarnings("unused")
    List<IOpenClass> list) {

    }

}
