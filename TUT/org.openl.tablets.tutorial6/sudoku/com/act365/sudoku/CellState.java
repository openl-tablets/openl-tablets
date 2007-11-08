/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com February 2005
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
 * CellState records the number of candidates remain for each
 * separate cell on the grid.
 */

public class CellState implements IState {

    // Grid size
    
    int boxesAcross ,
        boxesDown ,
        cellsInRow ;
        
    // State variables
    
    boolean[][][] eliminated ;
    
    byte[][] nEliminated ;
    
    // Thread
    
    boolean[][][][] threadEliminated ;
    
    byte[][][] threadNEliminated ;
    
	/**
     * Sets the state grid to the appropriate size.
	 * @see com.act365.sudoku.IState#setup(int,int)
	 */
     
	public void setup(int boxesAcross , int boxesDown ) {

        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;

        final boolean resize = cellsInRow != boxesAcross * boxesDown ;

        cellsInRow = boxesAcross * boxesDown ;
        
        int i , j , k ;
        if( resize ){
            eliminated = new boolean[cellsInRow][cellsInRow][cellsInRow];
            nEliminated = new byte[cellsInRow][cellsInRow];
    
            threadEliminated = new boolean[cellsInRow*cellsInRow][cellsInRow][cellsInRow][cellsInRow];
            threadNEliminated = new byte[cellsInRow*cellsInRow][cellsInRow][cellsInRow];
        } else {
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    nEliminated[i][j] = 0 ;
                    k = 0 ;
                    while( k < cellsInRow ){
                        eliminated[i][j][k] = false ;
                        ++ k ;
                    }
                    ++ j ;
                }
                ++ i ;
            }
        }
	}

	/**
     * Writes the state grid to the stack at the appropriate position.
	 * @see com.act365.sudoku.IState#pushState(int)
	 */
     
	public void pushState( int nMoves ) {
        int i, j , k ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                k = 0 ;
                while( k < cellsInRow ){
                    threadEliminated[nMoves][i][j][k] = eliminated[i][j][k];
                    ++ k ;
                }
                threadNEliminated[nMoves][i][j] = nEliminated[i][j];
                ++ j ;
            }
            ++ i ;
        }
	}

	/**
     * Reads the state gris from the stack at the appropriate position.
	 * @see com.act365.sudoku.IState#popState(int)
	 */
     
	public void popState( int nMoves ) {
        int i , j , k ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                k = 0 ;
                while( k < cellsInRow ){
                    eliminated[i][j][k] = threadEliminated[nMoves][i][j][k];
                    ++ k ;
                }
                nEliminated[i][j] = threadNEliminated[nMoves][i][j];
                ++ j ;
            }
            ++ i ;
        }
	}

	/**
     * Adds the move (x,y):=v to the state grid.
     * @param value is in the range [0,cellsInRow), not [1,cellsInRow]. 
	 * @see com.act365.sudoku.IState#addMove(int, int, int)
	 */
	
    public void addMove(int x, int y, int value ) throws MoveException {
        int i , j ;
        // Check that it's a valid candidate.
        if( eliminated[x][y][value] ){
            throw new MoveAlreadyEliminatedException( x , y , value );
        }
        // Eliminate other candidates for the current cell.
        i = 0 ;
        while( i < cellsInRow ){
            if( i != value && ! eliminated[x][y][i] ){
                eliminated[x][y][i] = true ;
                ++ nEliminated[x][y];
            }
            ++ i ;
        }
        if( nEliminated[x][y] != cellsInRow - 1 ){
            throw new MoveCantBeEliminatedException( x , y , value );
        }
        // Eliminate other candidates for the current row.
        j = 0 ;
        while( j < cellsInRow ){
            if( j != y && ! eliminated[x][j][value] ){
                eliminated[x][j][value] = true ;
                ++ nEliminated[x][j];
            }
            ++ j ;
        }
        // Eliminate other candidates for the current column.
        i = 0 ;
        while( i < cellsInRow ){
            if( i != x && ! eliminated[i][y][value] ){
                eliminated[i][y][value] = true ;
                ++ nEliminated[i][y];
            }
            ++ i ;
        }
        // Eliminate other candidates for the current subgrid.
        i = x / boxesAcross * boxesAcross - 1 ;
        while( ++ i < ( x / boxesAcross + 1 )* boxesAcross ){
            if( i == x ){
                continue ;
            }
            j = y / boxesDown * boxesDown - 1 ;
            while( ++ j < ( y / boxesDown + 1 )* boxesDown ){
                if( j == y ){
                    continue ;
                }
                if( ! eliminated[i][j][value] ){
                    eliminated[i][j][value] = true ;
                    ++ nEliminated[i][j];
                }
            }
        }
	}

	/**
     * Eliminates the move (x,y):=v from the current state grid.
     * @param value is in the range [0,cellsInRow), not [1,cellsInRow]. 
	 * @see com.act365.sudoku.IState#eliminateMove(int, int, int)
	 */
     
	public void eliminateMove(int x, int y, int value ) {
        eliminated[x][y][value] = true ;
        ++ nEliminated[x][y];
	}

    /**
     * Produces a string representation of the state grid.
     */
    
    @Override public String toString() {
        boolean multipleValues ;
        int i , j , v , length ;
        int[] maxLength = new int[cellsInRow];
        StringBuilder sb = new StringBuilder();
        String[][] candidates = new String[cellsInRow][cellsInRow];
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                sb.delete( 0 , sb.length() );
                multipleValues = false ;
                v = 0 ;
                while( v < cellsInRow ){
                    if( ! eliminated[i][j][v] ){
                        if( multipleValues ){
                            sb.append("|");
                        } else {
                            multipleValues = true ;
                        }
                        SuDokuUtils.appendValue( sb , v );
                    }
                    ++ v ;
                }
                if( ( length = sb.length() ) > maxLength[j] ){
                    maxLength[j] = length ;
                }
                candidates[i][j] = sb.toString();
                ++ j ;
            }
            ++ i ;
        }
        return SuDokuUtils.toString( candidates , boxesAcross , maxLength );
    }
}
