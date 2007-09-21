package org.openl.rules.ui.repository;

import java.util.LinkedList;
import java.util.List;

import org.openl.rules.ui.repository.beans.Element;

/**
 * Handler for Properties->Elements Tab
 * 
 * @author Aleh Bykhavets
 *
 */
public class ElementHandler {
	private List<Element> elements;

	public ElementHandler() {
		elements = new LinkedList<Element>();
		
		elements.add(new Element("bin"  , "1.2", "08/08/2007 10:32am", "John S."));
		elements.add(new Element("build", "1.1", "08/05/2007  9:40am", "Alex T."));
		elements.add(new Element("docs" , "1.1", "08/05/2007  9:40am", "Jonh S."));
		elements.add(new Element("rules", "1.4", "08/11/2007 11:07am", "Lee Vong"));
	}
	
	/**
	 * Gets list of elements for 'Elements' tab.
	 * 
	 * @return list of elements for a entity
	 */
	public List<Element> getElements() {
		return elements;
	}
}
