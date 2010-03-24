/*
 * Created on Jul 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;

/**
 * @author snshor
 *
 */
public class ImportTypeLibrary implements ITypeLibrary {

    class ImportSelector extends ASelector<String> {

        public boolean select(String name) {
            int dotIndex = name.lastIndexOf('.');

            for (int i = 0; i < imports.length; i++) {
                if (!name.startsWith(imports[i])) {
                    continue;
                }

                if (dotIndex == imports[i].length()) {
                    return true;
                }
            }

            return false;
        }

    }
    protected ITypeLibrary library;
    String[] imports;

    String[] classes;

    Map<String, String> aliases;

    /**
     *
     */
    public ImportTypeLibrary(ITypeLibrary library, String[] imports, String[] classes) {
        this.library = library;
        this.imports = imports;
        aliases = new HashMap<String, String>();
        for (int i = 0; i < classes.length; i++) {
            int index = classes[i].lastIndexOf('.');
            if (index > 0) {
                aliases.put(classes[i].substring(index + 1), classes[i]);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.ITypeLibrary#getType(java.lang.String)
     */
    public IOpenClass getType(String typename) throws AmbiguousTypeException {
        IOpenClass type = null;
        String alias = aliases.get(typename);

        if (alias != null) {
            type = library.getType(alias);
        }

        if (type != null) {
            return type;
        }

        for (int i = 0; i < imports.length; i++) {
            alias = imports[i] + '.' + typename;
            type = library.getType(alias);
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    protected boolean isLongName(String typename) {
        return typename.indexOf('.') >= 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.ITypeLibrary#types()
     */
    public Iterator<String> typeNames() {
        return AOpenIterator.select(library.typeNames(), new ImportSelector());
    }

}
