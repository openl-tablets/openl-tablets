/**
 * 
 */
package com.exigen.le.evaluator.function.TypifiedRange;

import java.util.Iterator;

/**
 * @author vabramovs
 *
 */
public class VirtualRowIterator implements Iterator<VirtualRow>{
	   int index=0;  
	   TypifiedRange tRange;
	VirtualRowIterator(TypifiedRange tRange){
		this.tRange=tRange;
	}
	public boolean hasNext() {
		if(tRange.isHorizontal()){
			return this.index < tRange.range.getHeight();
		}
		else {
			return index < tRange.range.getWidth();
		}
	}

	public VirtualRow next() {
		VirtualRow result = tRange.getVirtualRow(index);
		index++;
		return result;
	}

	public void remove() {
		
	}
	
}
