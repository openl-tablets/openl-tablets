package org.openl.rules.ui.repository.beans;

public class XVersion {
    private String name;
    private String created;
    private XUser createdBy;
    private String comment;
    
    public XVersion(String name, String created, XUser createdBy, String comment) {
	super();
	this.name = name;
	this.created = created;
	this.createdBy = createdBy;
	this.comment = comment;
    }
    
    public String getComment() {
        return comment;
    }
    public String getCreated() {
        return created;
    }
    public XUser getCreatedBy() {
        return createdBy;
    }
    public String getName() {
        return name;
    }
}
