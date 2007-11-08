/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com May 2005
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
 * LinearSystemState stores the system of linear equations defined by
 * the Sudoku puzzle.
 */

public class LinearSystemState implements IState {

    int boxesAcross ,
        boxesDown ,
        cellsInRow ;
    
    byte[][][] a ;
    
    int[] nRows ;

    // Thread
    
    StateStack stack ;
    
//    byte[][][][] threadA ;
    
    int[][] threadNRows ;
    
    // Display formats
    
    public final static int EQUATIONS = 0 ,
                            MATRIX = 1 ;
                            
    public static int defaultDisplayFormat = EQUATIONS ;
        
	/**
	 * @see com.act365.sudoku.IState#setup(int, int)
	 */
     
	public void setup( int boxesAcross , int boxesDown ) {
    
        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;        
        cellsInRow = boxesAcross * boxesDown ;
        
        a = new byte[cellsInRow][3*cellsInRow][1+cellsInRow*cellsInRow];
        nRows = new int[cellsInRow];

        stack = new StateStack( cellsInRow * cellsInRow );
        threadNRows = new int[cellsInRow*cellsInRow][cellsInRow];
                
//        threadA = new byte[cellsInRow*cellsInRow][cellsInRow][3*cellsInRow][1+cellsInRow*cellsInRow];
                
        int i , j , k , v , col ;
        v = 0 ;
        while( v < cellsInRow ){
            i = 0 ;
            // Row constraints
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    a[v][i][i*cellsInRow+j] = 1 ;
                    ++ j ;
                }
                a[v][i][cellsInRow*cellsInRow] = 1 ;
                ++ i ;
            }
            // Column constraints
            while( i < 2 * cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    a[v][i][i+(j-1)*cellsInRow] = 1 ;
                    ++ j ;
                }
                a[v][i][cellsInRow*cellsInRow] = 1 ;
                ++ i ;            
            }
            // Box constraints
            while( i < 3 * cellsInRow ){
                col = ( i - 2 * cellsInRow )/ boxesAcross * boxesAcross * cellsInRow + ( i - 2 * cellsInRow )% boxesAcross * boxesDown ;
                j = 0 ;
                while( j < boxesAcross ){
                    k = 0 ;
                    while( k < boxesDown ){
                        a[v][i][col++] = 1 ;
                        ++ k ;
                    }
                    col += cellsInRow - boxesDown ;
                    ++ j ;
                }
                a[v][i][cellsInRow*cellsInRow] = 1 ;
                ++ i ;
            }
            //
            nRows[v] = 3 * cellsInRow ;
            try {
                nRows[v] = reduce( v );
            } catch ( Exception e ) {
                nRows[v] = 3 * cellsInRow ;
            }
            ++ v ;
        }
	}

	/**
	 * @see com.act365.sudoku.IState#pushState(int)
	 */
     
	public void pushState( int nMoves ) {
        
        int v , i , j ;
        byte[][][] aSlice = new byte[cellsInRow][][];
        v = 0 ;
        while( v < cellsInRow ){
            try {
                threadNRows[nMoves][v] = nRows[v] = reduce( v );
            } catch( Exception e ) {
                threadNRows[nMoves][v] = nRows[v] = 3 * cellsInRow * cellsInRow ;    
            }
            aSlice[v] = new byte[nRows[v]][1+cellsInRow*cellsInRow];
            i = 0 ;
            while( i < nRows[v] ){
                j = 0 ;
                while( j < 1 + cellsInRow * cellsInRow ){
                    aSlice[v][i][j] = a[v][i][j];
                    ++ j ;
                }
                ++ i ;
            }
            ++ v ;
        }   
        stack.pushState( aSlice , nMoves );
	}

    /**
     * @see com.act365.sudoku.IState#popState(int)
     */
     	
    public void popState( int nMoves ) {
        byte[][][] aSlice ;
        if( ( aSlice = (byte[][][]) stack.popState( nMoves ) ) != null ){  
            int v , i , j ;
            v = 0 ;
            while( v < cellsInRow ){
                nRows[v] = threadNRows[nMoves][v];
                i = 0 ;
                while( i < nRows[v] ){
                    j = 0 ;
                    while( j < 1 + cellsInRow * cellsInRow ){
                        a[v][i][j] = aSlice[v][i][j];
                        ++ j ;
                    }
                    ++ i ;
                }
                ++ v ;
            }        
        }
	}

	/** 
     * Adds the constraint (x,y):=v to the system and reduces. 
	 * @see com.act365.sudoku.IState#addMove(int, int, int)
	 */
    
	public void addMove( int x , int y , int v ) throws MoveException {
        int i , j , k ;
        // Eliminate other candidates for the current row.
        j = 0 ;
        while( j < cellsInRow ){
            if( j != y ){
                eliminateMove( x , j , v );
            }
            ++ j ;
        }
        // Eliminate other candidates for the current column.
        i = 0 ;
        while( i < cellsInRow ){
            if( i != x ){
                eliminateMove( i , y , v );
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
                eliminateMove( i , j , v );
            }
        }
        // Eliminate other values for the cell.
        k = 0 ;
        while( k < cellsInRow ){
            if( k != v ){
                eliminateMove( x , y , k );
            }
            ++ k ;
        }
        // Add a new constraint.
        final int col = x * cellsInRow + y ;
        j = 0 ;
        while( j < cellsInRow * cellsInRow ){
            if( j == col ){
                a[v][nRows[v]][j] = 1 ;
            } else {
                a[v][nRows[v]][j] = 0 ;
            }
            ++ j ;
        }
        a[v][nRows[v]][cellsInRow*cellsInRow] = 1 ;
        ++ nRows[v];
        // Reduce the system.
        nRows[v] = reduce( v );
	}

	/** 
	 * @see com.act365.sudoku.IState#eliminateMove(int, int, int)
	 */
     
    public void eliminateMove( int x , int y , int v ) {
        final int j = x * cellsInRow + y ;
        int i = 0 ;
        while( i < nRows[v] ){
            a[v][i++][j] = 0 ;
        }
	}

    /**
     * Reduces the linear system of Su Doku equations for a given value.
     * @param v value for which the system is to be reduced
     * @return the number of constraints in the reduced system
     */
    
    int reduce( int v ) {
        int c , c2 , c3 , r , pivotRow , pivotValue , previousPivotRow ;
        byte temp ;
        boolean divisible ;

        previousPivotRow = -1 ;
        pivotRow = 0 ;

        while( pivotRow < nRows[v] ){
            // Look for a pivot row.
            r = c = pivotRow ;
            findPivotRow:
            while( c < cellsInRow * cellsInRow ){
                r = pivotRow ;
                while( r < nRows[v] ){
                    if( a[v][r][c] != 0 ){
                        break findPivotRow ;
                    }
                    ++ r ;
                }
                ++ c ;
            }
            if( c == cellsInRow * cellsInRow ){
                return pivotRow ;
            } else {
                pivotRow = r ;
            }
            // Normalize the pivot row and check for an insoluble system.
            pivotValue = a[v][pivotRow][c];
            if( pivotValue == 1 ){
            } else if( pivotValue == -1 ) {
                c2 = c ;
                while( c2 < cellsInRow * cellsInRow + 1 ){
                    a[v][pivotRow][c2] = (byte) - a[v][pivotRow][c2];
                    ++ c2 ;
                }                            
            } else {
                c2 = c ;
                // Check whether it's possible to divide through by the pivot ratio.
                divisible = true ;
                while( divisible && c2 < cellsInRow * cellsInRow + 1 ){
                    if( a[v][pivotRow][c2] % pivotValue != 0 ){
                        divisible = false ;
                        break;
                    }
                    ++ c2 ;
                }
                // Divide through.
                c2 = c ;
                if( divisible ){
                    while( c2 < cellsInRow * cellsInRow + 1 ){
                        a[v][pivotRow][c2++] /= pivotValue ;
                    }      
                } else if( pivotValue < 0 ){
                    while( c2 < cellsInRow * cellsInRow + 1 ){
                        a[v][pivotRow][c2] = (byte) -a[v][pivotRow][c2];
                        ++ c2 ;
                    }      
                }
            }
            // Swap rows, if necessary.
            if( pivotRow != previousPivotRow + 1 ){
                c2 = c ;
                while( c2 < cellsInRow * cellsInRow + 1 ){
                    temp = a[v][previousPivotRow+1][c2] ;
                    a[v][previousPivotRow+1][c2] = a[v][pivotRow][c2];
                    a[v][pivotRow][c2] = temp ;
                    ++ c2 ;
                }
                pivotRow = previousPivotRow + 1 ;
            }
            // Subtract the pivot row from all others.
            r = 0 ;
            while( r < nRows[v] ){
                if( r != pivotRow && ( pivotValue = a[v][r][c] ) != 0 ){
                    c2 = c ;
                    while( c2 < cellsInRow * cellsInRow + 1 ){
                        a[v][r][c2] -= pivotValue * a[v][pivotRow][c2];
                        ++ c2 ;
                    }
                    // Normalize the new row.
                    c2 = 0 ;
                    while( c2 < cellsInRow * cellsInRow + 1 ){
                        if( a[v][r][c2] != 0 ){
                            pivotValue = a[v][r][c2];
                            break;
                        }
                        ++ c2 ;
                    }
                    if( c2 == cellsInRow * cellsInRow + 1 ){
                        // It's a row of zeros.
                        ++ r ;
                        continue ;
                    }
                    if( pivotValue == 1 ){
                    } else if( pivotValue == -1 ) {
                        while( c2 < cellsInRow * cellsInRow + 1 ){
                            a[v][r][c2] = (byte) - a[v][r][c2];
                            ++ c2 ;
                        }                            
                    } else {
                        c3 = c2 ;
                        // Check whether it's possible to divide through by the pivot ratio.
                        divisible = true ;
                        while( divisible && c3 < cellsInRow * cellsInRow + 1 ){
                            if( a[v][pivotRow][c3] % pivotValue != 0 ){
                                divisible = false ;
                                break;
                            }
                            ++ c3 ;
                        }
                        // Divide through.
                        c3 = c2 ;
                        if( divisible ){
                            while( c3 < cellsInRow * cellsInRow + 1 ){
                                a[v][pivotRow][c3++] /= pivotValue ;
                            }      
                        } else if( pivotValue < 0 ){
                            while( c3 < cellsInRow * cellsInRow + 1 ){
                                a[v][pivotRow][c3] = (byte) -a[v][pivotRow][c3];
                                ++ c3 ;
                            }      
                        }
                    }
                }
                ++ r ;
            }
            // Consider the next pivot row.
            previousPivotRow = pivotRow ++ ;
        }

        return pivotRow ;
    }

    /**
     * String representation
     */
     
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        int v = 0 ;
        while( v < cellsInRow ){
            sb.append( toString( v , defaultDisplayFormat ) );
            ++ v ;
        }
        
        return sb.toString();
    }
    
    /**
     * String representation of the equations for the 
     * chosen value in the chosen display format.
     */

    public String toString( int v , int displayFormat ) {

        StringBuilder sb = new StringBuilder();
                
        int i , j ; 
        
        switch( displayFormat ){
            
            case EQUATIONS:
            {
                boolean multipleValues ;
                int nValues ;        

                i = 0 ;
                while( i < nRows[v] ){
                    multipleValues = false ;
                    nValues = 0 ;
                    j = 0 ;
                    while( nValues < 2 && j < cellsInRow * cellsInRow ){
                        if( a[v][i][j] != 0 ){
                            ++ nValues ;
                        }
                        ++ j ;
                    }
                    j = 0 ;
                    while( j < cellsInRow * cellsInRow ){                
                        if( a[v][i][j] != 0 ){
                            if( multipleValues ){
                                if( a[v][i][j] > 0 ){
                                    sb.append("+");
                                }
                            } else {
                                multipleValues = true ;
                            }
                            if( a[v][i][j] == -1 ){
                                sb.append("-");
                            } else if( a[v][i][j] != 1 ){                        
                                sb.append( a[v][i][j] );
                            }
                            sb.append("d[");
                            sb.append( 1 + j / cellsInRow );
                            sb.append(",");
                            sb.append( 1 + j % cellsInRow );
                            sb.append(",");
                            SuDokuUtils.appendValue( sb , v );
                            sb.append("]");
                        }
                        ++ j ;
                    }
                    sb.append(" = ");
                    sb.append( a[v][i][cellsInRow*cellsInRow] );
                    sb.append("\n");
                    ++ i ;
                }
                sb.append("\n");
            }
            break;
            
            case MATRIX:
            {
                String[][] s = new String[3*cellsInRow][cellsInRow*cellsInRow+1];                
                int k , length , maxLength = 0 ;                
                i = 0 ;
                while( i < 3*cellsInRow ){
                    j = 0 ;
                    while( j < cellsInRow * cellsInRow + 1 ){
                        s[i][j] = Integer.toString( a[v][i][j] );
                        if( ( length = s[i][j].length() ) >  maxLength ){
                            maxLength = length ;
                        }
                        ++ j ;
                    }
                    ++ i ;
                }
                i = 0 ;
                while( i < 3*cellsInRow ){
                    j = 0 ;
                    while( j < cellsInRow * cellsInRow + 1 ){
                        length = s[i][j].length();
                        k = 0 ;
                        while( k < maxLength - length + 1 ){
                            sb.append(" ");
                            ++ k ;
                        }
                        sb.append( s[i][j] );
                        ++ j ;
                    }
                    sb.append("\n");
                    ++ i ;
                }
            }
            break;
        }
        
        return sb.toString();
    }
}
