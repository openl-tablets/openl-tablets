package com.exigen.le.smodel;


/**
 * LE Range 
 * @author vabramovs
 *
 */
public  class Range implements ExcelSpace {
// Excel visual addressing: <highest-left cell>:<lowest-right cell> 
private Cell from;
private Cell to;

public Range init(String rangeAddress){
	String[] part= rangeAddress.split(":") ;
	from = new Cell().init(part[0]) ;
	to= new Cell().init(part[1]);
	return this;
}

public boolean isArea() {
	return true;
}

public Cell from() {
	return from;
}

public Cell to() {
	return to;
}

public void setFrom(Cell from) {
	this.from = from;
}

public void setTo(Cell to) {
	this.to = to;
}

public String toString(){
	return from.toString()+":"+to.toString();
}

public int getHeight() {
	return to.getRow()-from.getRow()+1;
}

public int getWidth() {
	return to.getColumnIndex()-from.getColumnIndex()+1;
}

}
