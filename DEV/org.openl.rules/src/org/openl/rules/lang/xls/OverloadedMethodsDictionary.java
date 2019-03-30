package org.openl.rules.lang.xls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * Dictionary of IOpenMethod instances. Categorizes methods using their signatures.
 */
public class OverloadedMethodsDictionary {

    /**
     * Internal map of groups.
     */
    private Map<MethodKey, Set<TableSyntaxNodeKey>> internalMap = new HashMap<>();

    /**
     * Checks that method already in dictionary.
     * 
     * @param method IOpenMethod instance
     * @return <code>true</code> if method already exists in dictionary; <code>false</code> - otherwise
     */
    public boolean contains(IOpenMethod method) {
        MethodKey key = buildKey(method);

        return contains(key);
    }

    /**
     * Adds TableSyntaxNode instance to dictionary. If method(s) with same signature already exists in dictionary new
     * one will be added to its group; otherwise - new entry will be created.
     * 
     * @param table executable table
     */
    public void add(TableSyntaxNode table) {

        IOpenMethod method = (IOpenMethod) table.getMember();
        MethodKey key = buildKey(method);

        if (contains(key)) {
            Set<TableSyntaxNodeKey> value = internalMap.get(key);
            value.add(buildKey(table));
        } else {
            Set<TableSyntaxNodeKey> value = new HashSet<>();
            value.add(buildKey(table));

            internalMap.put(key, value);
        }
    }

    /**
     * Adds all nodes to dictionary.
     * 
     * @param tables list of executable nodes
     */
    public void addAll(List<TableSyntaxNode> tables) {

        for (TableSyntaxNode table : tables) {
            add(table);
        }
    }

    /**
     * Gets group of all possible overloads for specified method.
     * 
     * @param method IOpenMethod instance
     * @return group of methods
     */
    public Set<TableSyntaxNodeKey> getAllMethodOverloads(IOpenMethod method) {
        MethodKey key = buildKey(method);

        return internalMap.get(key);
    }

    /**
     * Checks that entry with passed key already exists.
     * 
     * @param key key
     * @return <code>true</code> if entry already exists; <code>false</code> - otherwise
     */
    private boolean contains(MethodKey key) {
        return internalMap.containsKey(key);
    }

    /**
     * Build key for IOpenMethod instance.
     * 
     * @param method IOpenMethod instance
     * @return builded key object
     */
    private MethodKey buildKey(IOpenMethod method) {
        return new MethodKey(method);
    }

    /**
     * Build key for TableSyntaxNode.
     * 
     * @param table Table for key generation.
     * @return builded key object
     */
    private TableSyntaxNodeKey buildKey(TableSyntaxNode table) {
        return new TableSyntaxNodeKey(table);
    }
}