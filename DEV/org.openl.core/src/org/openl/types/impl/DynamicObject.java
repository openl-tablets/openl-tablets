/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.util.print.NicePrinter;
import org.openl.util.print.NicePrinterAdaptor;

/**
 * @author snshor
 * 
 */
public class DynamicObject implements IDynamicObject {

    private IOpenClass type;

    private HashMap<String, Object> fieldValues = new HashMap<String, Object>();

    /*
     * Added to support deployment of OpenL project as web services
     */

    public static NicePrinterAdaptor getNicePrinterAdaptor() {
        return new DONIcePrinterAdaptor();
    }

    /*
     * Added to support deployment of OpenL project as web services
     */

    public DynamicObject() {
    }

    public DynamicObject(IOpenClass type) {
        this.type = type;
    }

    public boolean containsField(String name) {
        return fieldValues.containsKey(name);
    }

    @Override
    public Object getFieldValue(String name) {
        return fieldValues.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFieldValues() {
        return (HashMap<String, Object>) fieldValues.clone();
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    protected boolean isMyField(String name) {
        return type.getField(name) != null;
    }

    @Override
    public void setFieldValue(String name, Object value) {
        fieldValues.put(name, value);
    }

    public void setType(IOpenClass type) {
        this.type = type;
    }

    @Override
    public String toString() {
        NicePrinter printer = new NicePrinter();
        printer.print(this, getNicePrinterAdaptor());
        return printer.getBuffer().toString();
    }

    private static class DONIcePrinterAdaptor extends NicePrinterAdaptor {

        @Override
        protected String getTypeName(Object obj) {
            if (obj instanceof DynamicObject) {
                return ((DynamicObject) obj).getType().getName();
            }
            return super.getTypeName(obj);
        }

        @Override
        public void printObject(Object obj, int newID, NicePrinter printer) {
            if (obj instanceof IDynamicObject) {
                IDynamicObject dobj = (IDynamicObject) obj;
                printReference(dobj, newID, printer);
                // printer.getBuffer().append(shortTypeName(dobj.getType().getName()));
                printMap(dobj.getFieldValues(), null, printer);
                return;
            }

            super.printObject(obj, newID, printer);
        }
    }

}
