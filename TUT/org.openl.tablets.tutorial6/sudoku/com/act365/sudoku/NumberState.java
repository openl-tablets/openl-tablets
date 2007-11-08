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
 * NumberState records the number of candidates remain for each
 * value/sector on the grid, where a sector is any row, column
 * or subgrid.
 */

public class NumberState implements IState {

    // Grid size
    
    int boxesAcross ,
        boxesDown ,
        cellsInRow ;
        
    // State variables
    
    boolean[][][] eliminated ;
    
    byte[][] nEliminated ;
    
    boolean[][] isFilled ;
    
    // Thread
    
    boolean[][][][] threadEliminated ;
    
    byte[][][] threadNEliminated ;
    
    boolean[][][] threadIsFilled ;
    
    /**
     * Sets the state grid to the appropriate size.
     * @see com.act365.sudoku.IState#setup(int,int)
     */

	public void setup(int boxesAcross, int boxesDown) {

        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;

        final boolean resize = cellsInRow != boxesAcross * boxesDown ;

        cellsInRow = boxesAcross * boxesDown ;
        
        int i , j , k ;
        if( resize ){
            eliminated = new boolean[cellsInRow][3*cellsInRow][cellsInRow];
            nEliminated = new byte[cellsInRow][3*cellsInRow];
            isFilled = new boolean[cellsInRow][3*cellsInRow];
        
            threadEliminated = new boolean[cellsInRow*cellsInRow][cellsInRow][3*cellsInRow][cellsInRow];
            threadNEliminated = new byte[cellsInRow*cellsInRow][cellsInRow][3*cellsInRow];
            threadIsFilled = new boolean[cellsInRow*cellsInRow][cellsInRow][3*cellsInRow];
        } else {
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < 3 * cellsInRow ){
                    nEliminated[i][j] = 0 ;
                    isFilled[i][j] = false ;
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
          
	public void pushState(int nMoves ) {
        int i, j , k ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < 3 * cellsInRow ){
                k = 0 ;
                while( k < cellsInRow ){
                    threadEliminated[nMoves][i][j][k] = eliminated[i][j][k];
                    ++ k ;
                }
                threadNEliminated[nMoves][i][j] = nEliminated[i][j];
                threadIsFilled[nMoves][i][j] = isFilled[i][j];
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
            while( j < 3 * cellsInRow ){
                k = 0 ;
                while( k < cellsInRow ){
                    eliminated[i][j][k] = threadEliminated[nMoves][i][j][k];
                    ++ k ;
                }
                nEliminated[i][j] = threadNEliminated[nMoves][i][j];
                isFilled[i][j] = threadIsFilled[nMoves][i][j];
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
        int boxSector = 2 * cellsInRow + x / boxesAcross * boxesAcross + y / boxesDown ,
            boxPosition = x % boxesAcross * boxesDown + y % boxesDown ;
        // Check that it's a valid candidate.
        if( eliminated[value][x][y] || 
            eliminated[value][cellsInRow+y][x] || 
            eliminated[value][boxSector][boxPosition] ){
                throw new MoveAlreadyEliminatedException( x , y , value );
        }
        // Note which sectors have been filled.
        isFilled[value][x] = true ;
        isFilled[value][cellsInRow+y] = true ;
        isFilled[value][boxSector] = true ;
        // Eliminate the current value from other cells in its 
        // ... row (x,i)
        boxSector = 2 * cellsInRow + x / boxesAcross * boxesAcross ;
        boxPosition = x % boxesAcross * boxesDown ;
        i = -1 ;
        while( ++ i < cellsInRow ){
            if( i == y ){
                continue ;
            }
            if( ! eliminated[value][x][i] ){
                eliminated[value][x][i] = true ;
                ++ nEliminated[value][x];
            }
            if( ! eliminated[value][cellsInRow+i][x] ){
                eliminated[value][cellsInRow+i][x] = true ;
                ++ nEliminated[value][cellsInRow+i];
            }
            if( ! eliminated[value][boxSector+i/boxesDown][boxPosition+i%boxesDown] ){
                eliminated[value][boxSector+i/boxesDown][boxPosition+i%boxesDown] = true ;
                ++ nEliminated[value][boxSector+i/boxesDown];
            }
        }
        if( nEliminated[value][x] != cellsInRow - 1 ){
            throw new MoveCantBeEliminatedException( x , y , value );
        }
        // ... column (i,y) 
        i = -1 ;
        boxSector = 2 * cellsInRow + y / boxesDown ;
        boxPosition = y % boxesDown ;
        while( ++ i < cellsInRow ){
            if( i == x ){
                continue ;
            }
            if( ! eliminated[value][i][y] ){
                eliminated[value][i][y] = true ;
                ++ nEliminated[value][i];
            }
            if( ! eliminated[value][cellsInRow+y][i] ){
                eliminated[value][cellsInRow+y][i] = true ;
                ++ nEliminated[value][cellsInRow+y];
            }
            if( ! eliminated[value][boxSector+i/boxesAcross*boxesAcross][boxPosition+i%boxesAcross*boxesDown] ){
                eliminated[value][boxSector+i/boxesAcross*boxesAcross][boxPosition+i%boxesAcross*boxesDown] = true ;
                ++ nEliminated[value][boxSector+i/boxesAcross*boxesAcross];
            }
        }
        if( nEliminated[value][cellsInRow+y] != cellsInRow - 1 ){
            throw new MoveCantBeEliminatedException( x , y , value );
        }
        // ... subgrid
        i = x / boxesAcross * boxesAcross - 1 ;
        while( ++ i < ( x / boxesAcross + 1 )* boxesAcross ){
            j = y / boxesDown * boxesDown - 1 ;
            while( ++ j < ( y / boxesDown + 1 )* boxesDown ){
                if( i == x && j == y ){
                    continue ;
                }
                if( ! eliminated[value][i][j] ){
                    eliminated[value][i][j] = true ;
                    ++ nEliminated[value][i];
                }
                if( ! eliminated[value][cellsInRow+j][i] ){
                    eliminated[value][cellsInRow+j][i] = true ;
                    ++ nEliminated[value][cellsInRow+j];
                }
            }
        }
        i = -1 ;
        boxSector = 2 * cellsInRow + x / boxesAcross * boxesAcross + y / boxesDown ;
        boxPosition = x % boxesAcross * boxesDown + y % boxesDown ;
        while( ++ i < cellsInRow ){
            if( i == x % boxesAcross * boxesDown + y % boxesDown ){
                continue ;    
            }
            if( ! eliminated[value][boxSector][i] ){
                eliminated[value][boxSector][i] = true ;
                ++ nEliminated[value][boxSector];
            }
        }
        if( nEliminated[value][boxSector] != cellsInRow - 1 ){
            throw new MoveCantBeEliminatedException( boxSector / boxesAcross , boxSector % boxesAcross , value );
        }
        // Eliminate other values as candidates for the current row.
        i = -1 ;
        while( ++ i < cellsInRow ){
            if( i != value && ! eliminated[i][x][y] ){
                eliminated[i][x][y] = true ;
                ++ nEliminated[i][x];
            }
        }
        // Eliminate other values as candidates for the current column.
        i = -1 ;
        while( ++ i < cellsInRow ){
            if( i != value && ! eliminated[i][cellsInRow+y][x] ){
                eliminated[i][cellsInRow+y][x] = true ;
                ++ nEliminated[i][cellsInRow+y];
            }
        }
        // Eliminate other values as candidates for the current subgrid.
        i = -1 ;
        while( ++ i < cellsInRow ){
            if( i != value && ! eliminated[i][boxSector][boxPosition] ){
                eliminated[i][boxSector][boxPosition] = true ;
                ++ nEliminated[i][boxSector];
            }
        }
	}

    /**
     * Eliminates the move (x,y):=v from the current state grid.
     * @param value is in the range [0,cellsInRow), not [1,cellsInRow]. 
     * @see com.act365.sudoku.IState#eliminateMove(int, int, int)
     */
     
	public void eliminateMove(int x, int y, int value ) {
        final int boxSector = 2 * cellsInRow + x / boxesAcross * boxesAcross + y / boxesDown ;
        eliminated[value][x][y] = true ;
        ++ nEliminated[value][x];
        eliminated[value][cellsInRow+y][x] = true ;
        ++ nEliminated[value][cellsInRow+y];
        eliminated[value][boxSector][x%boxesAcross*boxesDown+y%boxesDown] = true ;
        ++ nEliminated[value][boxSector];
        isFilled[value][x] = false ;
        isFilled[value][cellsInRow+y] = false ;
        isFilled[value][boxSector] = false ;
	}

    /**
     * Produces a string representation of the state grid.
     */
    
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        String[][] data = new String[cellsInRow][cellsInRow];
        int[] maxLength = new int[cellsInRow];
        int i , j , v , length ;
        v = 0 ;
        while( v < cellsInRow ){
            sb.append("Value ");
            SuDokuUtils.appendValue( sb , v );
            sb.append(":\n\n");
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    if( eliminated[v][i][j] ){
                        data[i][j] = ".";
                    } else if( nEliminated[v][i] == cellsInRow - 1 ){
                        data[i][j] = SuDokuUtils.valueToString( v );
                    } else {
                        data[i][j] = "?";
                    }
                    if( ( length = data[i][j].length() ) > maxLength[j] ){
                        maxLength[j] = length ;
                    }
                    ++ j ;
                }
                ++ i ;
            }
            sb.append( SuDokuUtils.toString( data , boxesAcross , maxLength ) );
            sb.append("\n");
            ++ v ;
        }
        
        return sb.toString();
    }
}
