/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com March 2005
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

/**
 * MaskState is broadly similar to InvulnerableState except that it calculates 
 * the state for a mask - no values are involved. Unlike the other state classes,
 * MaskState does not implement IState.
 * @see InvulnerableState
 * @see IState
 */

public class MaskState {

    // Grid size
    
    int boxesAcross ,
        boxesDown ,
        cellsInRow ;
        
    byte maxScore ;
        
    // State variables
    
    byte[][] nInvulnerable ;

    boolean[][] mask ;
        
    /**
     * Sets the state grid to the appropriate size.
     * @see com.act365.sudoku.IState#setup(int,int)
     */

	public void setup(int boxesAcross, int boxesDown ) {

        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;

        final boolean resize = cellsInRow != boxesAcross * boxesDown ;

        cellsInRow = boxesAcross * boxesDown ;
        maxScore = (byte)( 3 * cellsInRow - boxesAcross - boxesDown );
        
        int i , j ;
        if( resize ){
            nInvulnerable = new byte[cellsInRow][cellsInRow];
            mask = new boolean[cellsInRow][cellsInRow];
        } else {
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    nInvulnerable[i][j] = 0 ;
                    mask[i][j] = false ;
                    ++ j ;
                }
                ++ i ;
            }
        }
	}

    /**
     * Adds the cell (x,y) to the state grid.
     */

	public void addCell( int x, int y ) throws Exception {
        int i , j ;
        // Check that it's a valid candidate.
        if( mask[x][y] ){
            throw new Exception("The cell (" + ( 1 + x ) + "," + ( 1 + y ) + ") has already been filled");
        }
        // Calc temp values.
        int lowerX = ( x / boxesAcross )* boxesAcross ;
        int upperX = ( x / boxesAcross + 1 )* boxesAcross ;
        int lowerY = ( y / boxesDown )* boxesDown ;
        int upperY = ( y / boxesDown + 1 )* boxesDown ;
        // Update nInvulnerable for (x,y).
        nInvulnerable[x][y] = maxScore ;
        // Update nInvulnerable for the domain of (x,y).
        // Shared column
        i = -1 ;
        while( ++ i < cellsInRow ){
            if( i == x || mask[i][y] ){
                continue ;
            }
            ++ nInvulnerable[i][y];
        }
        // Shared row
        j = -1 ;
        while( ++ j < cellsInRow ){
            if( j == y || mask[x][j] ){
                continue ;
            }
            ++ nInvulnerable[x][j];
        }
        // Shared subgrid
        i = lowerX - 1 ;
        while( ++ i < upperX ){
            if( i == x ){
                continue ;
            }
            j = lowerY - 1 ;
            while( ++ j < upperY ){
                if( j == y || mask[i][j] ){
                    continue ;
                }
                ++ nInvulnerable[i][j] ;
            }
        }
        // Set the cell.
        mask[x][y] = true ;      
	}
    
    /**
     * Produces a string representation of the state grid.
     */
    
    public String toString() {
        return SuDokuUtils.toString( nInvulnerable , boxesAcross , maxScore );
    }
}
