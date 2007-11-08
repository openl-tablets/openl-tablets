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

import com.act365.awt.Frame;

import java.io.* ;

/**
 * The SuDoku app displays a Su Doku solver in a new window.
 */

public class SuDoku extends Frame {

    SuDoku( Grid grid ){
        super("Su Doku Solver");        
        GridContainer gc = new GridContainer( grid );
        ControlContainer control = new ControlContainer( gc , getClass() ); 
        SuDokuContainer suDoku = new SuDokuContainer( gc , control );
        add( suDoku );
        setSize( suDoku.getBestSize() );
    }
 
    /**
     * Starts a new app with a Su Doku grid of the given size.
     * <code>java com.act365.sudoku.SuDoku [-a boxesAcross] [-d boxesDown] [-i]</code>
     * <br><code>boxesAcross</code> is the number of boxes to appear across one row of the Su Doku grid - the default is 3.
     * <br><code>boxesDown</code> is the number of boxes to appear down one column of the Su Doku grid - the default is 3.
     * <br>The -i option indicates that an initial grid should be read from standard input
     */
    
	public static void main(String[] args) {
		
		int boxesAcross = 3 ,
		    boxesDown = 3 ;
		
        boolean standardInput = false ;
        
		int i = 0 ;
		
		while( i < args.length ){
			if( args[i].equals("-a") ){
				++ i ;
				if( i < args.length ){
					try {
						boxesAcross = Integer.parseInt( args[i] );
					} catch ( NumberFormatException e ) {
						System.err.println("boxesAcross should be an integer");
                        System.exit( 1 );
					}
				} else {
					System.err.println("-a requires an argument");
                    System.exit( 1 );
				}
			} else if( args[i].equals("-d") ){
				++ i ;
				if( i < args.length ){
					try {
						boxesDown = Integer.parseInt( args[i] );
					} catch ( NumberFormatException e ) {
						System.err.println("boxesDown should be an integer");
                        System.exit( 1 );
					}
				} else {
					System.err.println("-d requires an argument");
                    System.exit( 1 );
				}
            } else if( args[i].equals("-i") ){
                standardInput = true ;
			} else {
				System.err.println("Usage: SuDoku [-a boxesAcross] [-d boxesDown]");
                System.exit( 1 );
			}
			++ i ;
		}
        // Read a grid from standard input.
        Grid grid = null ;
        if( standardInput ){
            String text ;
            StringBuilder gridText = new StringBuilder();
            BufferedReader standardInputReader = new BufferedReader( new InputStreamReader( System.in ) );
            try {
                while( ( text = standardInputReader.readLine() ) != null ){
                    if( text.length() == 0 ){
                        break ;
                    }
                    gridText.append( text );
                    gridText.append('\n');
                }
                grid = new Grid();
                grid.populate( gridText.toString() );
            } catch ( IOException e ) {
                System.err.println( e.getMessage() );
                System.exit( 2 );               
            }            
        } else {
            grid = new Grid( boxesAcross , boxesDown );
        }
        // 
        new SuDoku( grid ).setVisible( true );
	}
}
