/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com November 2004
 * 
 * Web site: http://act365.com/sudoku
 * E-mail: developers@act365.com
 * 
 * The Su Doku Solver solves Su Doku problems - see http://www.sudoku.com.
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *  
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package com.act365.sudoku;

import java.applet.Applet;

/**
 * Creates an applet that displays a Su Doku solver. The following PARAM names
 * are supported within the APPLET tag:
 * <br>ACROSS - the number of boxes to appear across one row of the Su Doku grid - the default is 3.
 * <br>DOWN - the number of boxes to appear down one column of the Su Doku grid - the default is 3.
 */

public class SuDokuApplet extends Applet {

    int boxesAcross = 3 ,
        boxesDown = 3 ;
        
    /**
     * Initiates the applet.
     */
    
    public void init(){
    	
    	String acrossString = getParameter("ACROSS"),
    	       downString = getParameter("DOWN");
    	       
		if( acrossString instanceof String ){
			try{
				boxesAcross = Integer.parseInt( acrossString );
			} catch ( NumberFormatException e ) {
				System.err.println("Illegal ACROSS vaue: " + acrossString );
			}
		}
		if( downString instanceof String ){
			try{
				boxesDown = Integer.parseInt( downString );
			} catch ( NumberFormatException e ) {
				System.err.println("Illegal DOWN vaue: " + downString );
			}
		}
    	
		GridContainer grid = new GridContainer( new Grid( boxesAcross , boxesDown ) );
		ControlContainer control = new ControlContainer( grid , getClass() ); 
		SuDokuContainer suDoku = new SuDokuContainer( grid , control );
		add( suDoku );
		setSize( suDoku.getBestSize() );
    }
}
