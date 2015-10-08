package org.openl.rules.dt.storage;

import java.util.HashMap;
import java.util.Map;

public class StorageInfo {

	int firstFormulaIndex = -1, firstSpaceIndex = -1;
	int numberOfSpaces = 0;
	Object min, max;
	int numberOfFormulas = 0;
	private int numberOfElses = 0;
	private int firstElseIndex = -1;
	
	Map<Object, Integer> uniqueIndex = new HashMap<Object, Integer>();

	public int getFirstFormulaindex() {
		return firstFormulaIndex;
	}

	public void setFirstFormulaindex(int firstFormulaindex) {
		this.firstFormulaIndex = firstFormulaindex;
	}

	public int getFirstSpaceindex() {
		return firstSpaceIndex;
	}

	public void setFirstSpaceindex(int firstSpaceindex) {
		this.firstSpaceIndex = firstSpaceindex;
	}


	public int getNumberOfSpaces() {
		return numberOfSpaces;
	}

	public void setNumberOfSpaces(int numberOfSpaces) {
		this.numberOfSpaces = numberOfSpaces;
	}

	public Object getMin() {
		return min;
	}

	public void setMin(Object min) {
		this.min = min;
	}

	public Object getMax() {
		return max;
	}

	public void setMax(Object max) {
		this.max = max;
	}

	public int getNumberOfFormulas() {
		return numberOfFormulas;
	}

	public void setNumberOfFormulas(int numberOfFormulas) {
		this.numberOfFormulas = numberOfFormulas;
	}

	public Map<Object, Integer> getUniqueIndex() {
		return uniqueIndex;
	}

	public void setUniqueIndex(Map<Object, Integer> uniqueIndex) {
		this.uniqueIndex = uniqueIndex;
	}
	
	
	public int getTotalNumberOfUniqueValues()
	{
		return  uniqueIndex.size()   + numberOfFormulas + (numberOfSpaces > 0 ? 1 : 0) + (numberOfElses  > 0 ? 1 : 0);
	}

	public int getNumberOfElses() {
		return numberOfElses;
	}

	public void setNumberOfElses(int numberOfElses) {
		this.numberOfElses = numberOfElses;
	}

	public void addSpaceIndex(int index) {
		if (numberOfSpaces++ == 0)
			firstSpaceIndex = index;
	}

	public void addElseIndex(int index) {
		if (numberOfElses++ == 0)
			firstElseIndex  = index;
	}
	
	public void addFormulaIndex(int index) {
		if (numberOfFormulas++ == 0)
			firstFormulaIndex  = index;
	}

	public int getFirstFormulaIndex() {
		return firstFormulaIndex;
	}

	public void setFirstFormulaIndex(int firstFormulaIndex) {
		this.firstFormulaIndex = firstFormulaIndex;
	}

	public int getFirstSpaceIndex() {
		return firstSpaceIndex;
	}

	public void setFirstSpaceIndex(int firstSpaceIndex) {
		this.firstSpaceIndex = firstSpaceIndex;
	}

	public int getFirstElseIndex() {
		return firstElseIndex;
	}

	public void setFirstElseIndex(int firstElseIndex) {
		this.firstElseIndex = firstElseIndex;
	}
	
	
	
	
}
