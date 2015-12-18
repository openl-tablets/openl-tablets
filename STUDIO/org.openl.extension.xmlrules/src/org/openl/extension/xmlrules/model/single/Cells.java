package org.openl.extension.xmlrules.model.single;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.openl.extension.xmlrules.model.single.node.NamedRange;

@XmlRootElement(name="cells")
public class Cells {
    private List<Cell> cells = new ArrayList<Cell>();
    private List<NamedRange> namedRanges = new ArrayList<NamedRange>();

    @XmlElement(name = "cell", type = Cell.class, required = true)
    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    @XmlElementWrapper(name="named-ranges")
    @XmlElement(name = "named-range")
    public List<NamedRange> getNamedRanges() {
        return namedRanges;
    }

    public void setNamedRanges(List<NamedRange> namedRanges) {
        this.namedRanges = namedRanges;
    }
}
