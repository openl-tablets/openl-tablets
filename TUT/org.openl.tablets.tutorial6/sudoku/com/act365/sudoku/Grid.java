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

package com.act365.sudoku ;

import java.io.* ;
import java.util.* ;

/**
 * A Grid object represents a partially-filled Su Doku grid.
 */

public class Grid implements Cloneable , Serializable {

    // Constants that define the grid size. The nomenclature is taken from
    // the Sudoku XML schema.
    
    int cellsInRow ,
        boxesAcross ,
        boxesDown ;
    
    // Grid data
    
    byte[][] data ;

    // Transient data
    
    transient int nUnwinds ,
                  complexity ; 

    transient Solver solver ;
                      
    /**
     * Creates a Su Doku grid with the given number of boxes
     * (aka subgrids) in each dimension.
     */
    
    public Grid( int boxesAcross , int boxesDown ){
        resize( boxesAcross , boxesDown );
    }

    /**
     * Creates a dimensionless Su Doku grid. The grid will have to be
     * redimensioned before it stores a puzzle.
     */    
    
    public Grid(){
    }
    
    /**
     * Clones the grid.
     */
    
    public Object clone() {
        
        Grid copy = new Grid( boxesAcross , boxesDown );
        
        int i, j ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                copy.data[i][j] = data[i][j];
                ++ j ;
            }
            ++ i ;
        }
        
        return copy ;
    }
    
    /**
     * Sets the number of boxes in the Su Doku grid.
     */
    
    public void resize( int boxesAcross ,
                        int boxesDown ){
        
        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;
        
	    cellsInRow = boxesAcross * boxesDown ;

	    data = new byte[cellsInRow][cellsInRow];
	  
	    nUnwinds = 0 ;                	
    }    
    
    /**
     * Populates the grid from a string.
     * @param s string in the format created by <code>toString()</code>
     */
    
    public Grid populate( String s ){

        StringTokenizer st = new StringTokenizer( s , " ");
        
        // Attempt to determine the dimensions of the grid.
        
        int boxesDown = 0 ,
            boxesAcross = 1 ;
        
        String token = st.nextToken();
            
        while( token.indexOf('\n') == -1 ){
            if( token.equals("*") || token.equals("|") || token.equals("¦") ){
                ++ boxesAcross ;
            } else if( boxesAcross == 1 ){
                ++ boxesDown ;
            }
            token = st.nextToken();
        }
        if( boxesAcross > 1 && boxesDown > 1 ){
            resize( boxesAcross , boxesDown );            
        }
        SuDokuUtils.populate( data , s );        
        return this ;
    }
    
    /**
     * Counts the number of filled cells in the grid.
     */
    
    public int countFilledCells(){
        int i , j , count = 0 ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                if( data[i][j] > 0 ){
                    ++ count ;
                }
                ++ j ;
            }
            ++ i ;
        }
        
        return count ;
    }

    /**
     * Solves the grid.
     * @param strategy strategy to be used
     * @param maxSolns the maximum number of solutions to be sought
     * @return number of solutions found
     */
        
    public int solve( IStrategy strategy ,
                      int maxSolns ){
        return solve( strategy , null , maxSolns );
    }
                      
    synchronized int solve( IStrategy strategy ,
                            IStrategy composeSolver ,
                            int maxSolns ){

        solver = new Solver( this , strategy , composeSolver , 0 , maxSolns , null );        
        solver.start();  
        int nSolns ;
        try {
            solver.join();            
            nUnwinds = solver.getNumberOfUnwinds();
            nSolns = solver.getNumberOfSolutions();
            complexity = solver.getComplexity();                      
        } catch( InterruptedException e ) {
            nSolns = nUnwinds = complexity = 0 ;
        }
        return nSolns ;
    }
    
    /**
     * Resets the value of each grid square.
     */
    
    public void reset(){
        int i , j ;
        
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                data[i][j] = 0 ;
                ++ j ;
            }
            ++ i ;
        }
        nUnwinds = 0 ;
    }
    
    /**
     * Produces a string representation of the grid.
     */
    
    public String toString() {
        return SuDokuUtils.defaultCopyType == SuDokuUtils.PLAIN_TEXT ? SuDokuUtils.toString( data , boxesAcross ) : toXML( 1 , SuDokuUtils.featuredGrades[0] );
    }
    
    /**
     * Tests for equality.
     */
    
    public boolean equals( Object obj ){
        if( !( obj instanceof Grid ) ){
            return false ;
        }
        Grid grid = (Grid) obj ;
        if( boxesAcross != grid.boxesAcross || boxesDown != grid.boxesDown ){
            return false ;
        }
        int r , c ;
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                if( data[r][c] != grid.data[r][c] ){
                    return false ;
                }
                ++ c ;
            }
            ++ r ;
        }
        return true ;
    }
    
    /**
     * Determines whether this grid precedes the given grid. The
     * value true will be returned if the two grids match.
     */

    boolean precedes( Grid grid ){
        if( boxesAcross != grid.boxesAcross || boxesDown != grid.boxesDown ){
            return false ;
        }
        int i ,j ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                if( grid.data[i][j] < data[i][j] ){
                    return false ;
                } else if( grid.data[i][j] > data[i][j] ){
                    return true ;
                }
                ++ j ;
            }
            ++ i ;
        }
        return true ;
    }

    /**
     * Rotates the grid through a half-turn.
     */    
    
    Grid halfRotate(){
        int i , j ;
        byte tmp ;
        i = 0 ;
        while( i < cellsInRow / 2 ){
            j = 0 ;
            while( j < cellsInRow ){
                tmp = data[cellsInRow-1-i][cellsInRow-1-j];
                data[cellsInRow-1-i][cellsInRow-1-j] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
            ++ i ;
        }
        if( cellsInRow % 2 == 1 ){
            j = 0 ;
            while( j < cellsInRow / 2 ){
                tmp = data[cellsInRow-1-i][cellsInRow-1-j];
                data[cellsInRow-1-i][cellsInRow-1-j] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
        }
        return this ;
    }
    
    /**
     * Rotates the grid through a quarter-turn anticlockwise.
     * Nothing happens if the number of boxes across the grid
     * does not match the number of boxes down.
     */

    Grid quarterRotate(){
        if( boxesAcross != boxesDown ){
            return this ;
        }
        int i , j ;
        byte tmp ;
        final int jMax = cellsInRow / 2 + ( cellsInRow % 2 == 1 ? 1 : 0 ); 
        i = 0 ;
        while( i < cellsInRow / 2 ){
            j = 0 ;
            while( j < jMax ){
                tmp = data[i][j];
                data[i][j] = data[j][cellsInRow-1-i];
                data[j][cellsInRow-1-i] = data[cellsInRow-1-i][cellsInRow-1-j];
                data[cellsInRow-1-i][cellsInRow-1-j] = data[cellsInRow-1-j][i];
                data[cellsInRow-1-j][i] = tmp ;
                ++ j ;
            }
            ++ i ;
        }
        return this ;
    }
    
    /**
     * Reflects the grid data in the line that runs from
     * the left centre of the grid to the right centre.
     */

    Grid reflectLeftRight(){
        int i , j ;
        byte tmp ;
        i = 0 ;
        while( i < cellsInRow / 2 ){
            j = 0 ;
            while( j < cellsInRow ){
                tmp = data[cellsInRow-1-i][j];
                data[cellsInRow-1-i][j] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
            ++ i ;
        }
        if( cellsInRow % 2 == 1 ){
            j = 0 ;
            while( j < cellsInRow / 2 ){
                tmp = data[cellsInRow-1-i][j];
                data[cellsInRow-1-i][j] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
        }
        return this ;
    }
    
    /**
     * Reflects the grid data in the line that runs from
     * the top centre of the grid to the bottom centre.
     */

    Grid reflectTopBottom(){
        int i , j ;
        byte tmp ;
        j = 0 ;
        while( j < cellsInRow / 2 ){
            i = 0 ;
            while( i < cellsInRow ){
                tmp = data[i][cellsInRow-1-j];
                data[i][cellsInRow-1-j] = data[i][j];
                data[i][j] = tmp ;
                ++ i ;
            }
            ++ j ;
        }
        if( cellsInRow % 2 == 1 ){
            i = 0 ;
            while( i < cellsInRow / 2 ){
                tmp = data[i][cellsInRow-1-j];
                data[i][cellsInRow-1-j] = data[i][j];
                data[i][j] = tmp ;
                ++ i ;
            }
        }
        return this ;
    }
    
    /**
     * Reflects the grid data in the diagonal that runs from
     * the top-left corner of the grid to the bottom-right.
     * Nothing happens if the number of boxes across the grid
     * does not match the number of boxes down.
     */
    
    Grid reflectTopLeftBottomRight(){
        if( boxesAcross != boxesDown ){
            return this ;
        }
        int i , j ;
        byte tmp ;
        i = 0 ;
        while( i < cellsInRow ){
            j = i + 1 ;
            while( j < cellsInRow ){
                tmp = data[j][i];
                data[j][i] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
            ++ i ;
        }
        return this ;
    }
    
    /**
     * Reflects the grid data in the diagonal that runs from
     * the top-right corner of the grid to the bottom-left.
     * Nothing happens if the number of boxes across the grid
     * does not match the number of boxes down.
     */
    
    Grid reflectTopRightBottomLeft(){
        if( boxesAcross != boxesDown ){
            return this ;
        }
        int i , j ;
        byte tmp ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow - 1 - i ){
                tmp = data[cellsInRow-1-j][cellsInRow-1-i];
                data[cellsInRow-1-j][cellsInRow-1-i] = data[i][j];
                data[i][j] = tmp ;
                ++ j ;
            }
            ++ i ;
        }
        return this ;
    }
    
    /**
     * Reflects, rotates and rearranges the grid as necessary in order
     * to reduce it to its lowest form.
     */
    
    public Grid rectify( boolean[][] mask ){
        int i , j ;
        boolean precedesGrid ;
        rearrangeData();
        Grid grid = (Grid) clone();
        grid.halfRotate().rearrangeData();
        precedesGrid = precedes( grid );        
        i = 0 ;
        while( i < grid.cellsInRow ){
            j = 0 ;
            while( j < grid.cellsInRow ){
                if( precedesGrid ){
                    grid.data[i][j] = data[i][j];
                } else {
                    data[i][j] = grid.data[i][j];
                }
                ++ j ;
            }
            ++ i ;
        }            
        if( MaskFactory.isSymmetricLeftRight( mask ) ){
            grid.reflectLeftRight().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
        }
        if( MaskFactory.isSymmetricTopBottom( mask ) ){
            grid.reflectTopBottom().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
        }
        if( MaskFactory.isSymmetricTopLeftBottomRight( mask ) ){
            grid.reflectTopLeftBottomRight().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
        }
        if( MaskFactory.isSymmetricTopRightBottomLeft( mask ) ){
            grid.reflectTopRightBottomLeft().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
        }
        if( MaskFactory.isSymmetricOrder4( mask ) ){
            // Anticlockwise
            grid.quarterRotate().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
            // Clockwise
            grid.halfRotate().rearrangeData();
            precedesGrid = precedes( grid );        
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( precedesGrid ){
                        grid.data[i][j] = data[i][j];
                    } else {
                        data[i][j] = grid.data[i][j];
                    }
                    ++ j ;
                }
                ++ i ;
            }            
        }
        return this ;
    }
    
    /**
     * Sets the contents of the given grid to be a rearranged form of
     * this grid such that, when read from left-to-right and
     * top-to-bottom, the numbers appear in increasing order. 
     */
    
    Grid rearrangeData(){
        int i , r , c , subSize = 0 ;
        int[] substitute = new int[cellsInRow];
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                if( data[r][c] > 0 ){
                    i = 0 ;
                    while( i < subSize ){
                        if( data[r][c] == substitute[i] ){
                            break ;
                        }
                        ++ i ;
                    }
                    if( i == subSize ){
                        substitute[subSize++] = data[r][c];
                    }
                }
                ++ c ;
            }
            ++ r ;
        }
        r = 0 ;
        while( r < cellsInRow ){
            c = 0 ;
            while( c < cellsInRow ){
                if( data[r][c] > 0 ){
                    i = 0 ;
                    while( i < subSize ){
                        if( data[r][c] == substitute[i] ){
                            data[r][c] = (byte)( i + 1 );
                            break ;
                        }
                        ++ i ;
                    }
                }
                ++ c ;
            }
            ++ r ;
        }
        return this ;        
    }
    
    /**
     * Randomly shuffles the current grid. 
     */

    public Grid shuffle(){
        return shuffle( true , true , true );    
    }
    
    public Grid shuffle( boolean rearrange , boolean rotate , boolean reflect ){
        int pick ;
        Random generator = new Random();
        // Rearrange the data within the grid.
        if( rearrange ){
            int i , j , size = cellsInRow ;
            byte[] substitute = new byte[cellsInRow];
            while( size > 0 ){
                i = -1 ;
                pick = Math.abs( generator.nextInt() % size );
                while( pick -- >= 0 ){
                    while( substitute[++i] > 0 );
                }
                substitute[i] = (byte) size -- ;
            }
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    if( data[i][j] > 0 ){
                        data[i][j] = substitute[data[i][j]-1]; 
                    }
                    ++ j ;
                }
                ++ i ;
            }
        }
        // Rotate
        if( rotate ){
            pick = Math.abs( generator.nextInt() % 4 );
            switch( pick ){
                case 0 :
                break;
            
                case 1:
                quarterRotate();
                break;
            
                case 2:
                halfRotate();
                break;
            
                case 3:
                halfRotate().quarterRotate();
                break;
            }
        }
        // Reflect
        if( reflect ){
            pick = Math.abs( generator.nextInt() % 16 );
            if( ( pick & 1 ) == 1 ){
                reflectLeftRight(); 
            }
            if( ( pick & 2 ) == 2 ){
                reflectTopBottom(); 
            }
            if( ( pick & 4 ) == 4 ){
                reflectTopLeftBottomRight(); 
            }
            if( ( pick & 8 ) == 8 ){
                reflectTopRightBottomLeft(); 
            }
        }
        //
        return this ;
    }
    
    /**
     * Writes the current grid with its associated labels in XML format to a string.
     */

    public String toXML( int serial , String grade ) {
        
        StringBuilder sb = new StringBuilder();
        
        int i , j ;
        sb.append("<puzzle>\n");
        sb.append("<serial>" + serial + "</serial>\n");
        sb.append("<grade>" + grade + "</grade>\n");
        sb.append("<solvers>0000</solvers>\n");
        sb.append("<question>\n");       
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                if( data[i][j] > 0 ){
                    if( cellsInRow >= 10 && data[i][j] < 10 ){
                        sb.append("0");
                    }
                    sb.append( data[i][j] );                    
                } else {
                    sb.append(".");
                }
                ++ j ; 
            }
            sb.append("\n");
            ++ i ;
        }
        sb.append("</question>\n");
        sb.append("</puzzle>\n");    
        
        return sb.toString();            
    }
}
