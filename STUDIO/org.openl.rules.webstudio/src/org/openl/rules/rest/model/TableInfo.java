/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.model;

import java.util.Map;
import java.util.Optional;

import org.openl.rules.lang.xls.XlsNodeTypes;

/**
 * Table info model.
 *
 * @author Vladyslav Pikus
 */
public class TableInfo {

    public final String id;
    public final String kind;
    public final String tableType;
    public final String name;
    public final String returnType;
    public final String signature;
    public final String file;
    public final String pos;
    public final Map<String, Object> properties;

    private TableInfo(Builder builder) {
        this.id = builder.id;
        this.kind = builder.kind;
        this.tableType = builder.tableType;
        this.name = builder.name;
        this.returnType = builder.returnType;
        this.signature = builder.signature;
        this.file = builder.file;
        this.pos = builder.pos;
        this.properties = Optional.ofNullable(builder.properties)
                .map(Map::copyOf)
                .orElse(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String kind;
        private String tableType;
        private String name;
        private String returnType;
        private String signature;
        private String file;
        private String pos;
        private Map<String, Object> properties;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder tableType(String tableType) {
            this.tableType = tableType;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder signature(String signature) {
            this.signature = signature;
            return this;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder pos(String pos) {
            this.pos = pos;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public TableInfo build() {
            return new TableInfo(this);
        }
    }

}
