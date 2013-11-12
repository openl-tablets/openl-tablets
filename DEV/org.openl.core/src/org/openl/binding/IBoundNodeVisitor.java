package org.openl.binding;

/** 
 * Not right implementation of Visitor pattern
 * see http://en.wikipedia.org/wiki/Visitor_pattern
 * @author DLiauchuk
 *
 */
@Deprecated
public interface IBoundNodeVisitor {
	
    boolean visit(IBoundNode node);
}
