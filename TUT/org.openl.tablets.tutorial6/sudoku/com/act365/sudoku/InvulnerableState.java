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
 * InvulnerableState records the number of cells that would remain
 * unresolved for each given move (ie each cell/value pair).
 */

public class InvulnerableState implements IState {

    // Grid size
    
    int boxesAcross ,
        boxesDown ,
        cellsInRow ;
        
    byte maxScore ;
        
    // State variables
    
    boolean[][][] eliminated ;
    
    byte[][][] nInvulnerable ;
    
    // Thread
    
    boolean[][][][] threadEliminated ;
    
    byte[][][][] threadNInvulnerable ;
    
    // Temporary vars used to store partially-calculated 
    // values for efficiency reasons.
    
    transient int lowerX , upperX , lowerY , upperY ;
    
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
        
        int i , j , k ;
        if( resize ){
            eliminated = new boolean[cellsInRow][cellsInRow][cellsInRow];
            nInvulnerable = new byte[cellsInRow][cellsInRow][cellsInRow];
    
            threadEliminated = new boolean[cellsInRow*cellsInRow][cellsInRow][cellsInRow][cellsInRow];
            threadNInvulnerable = new byte[cellsInRow*cellsInRow][cellsInRow][cellsInRow][cellsInRow];
        } else {
            i = 0 ;
            while( i < cellsInRow ){
                j = 0 ;
                while( j < cellsInRow ){
                    k = 0 ;
                    while( k < cellsInRow ){
                        eliminated[i][j][k] = false ;
                        nInvulnerable[i][j][k] = 0 ;
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
        int i, j , v ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                v = 0 ;
                while( v < cellsInRow ){
                    threadEliminated[nMoves][v][i][j] = eliminated[v][i][j];
                    threadNInvulnerable[nMoves][v][i][j] = nInvulnerable[v][i][j];
                    ++ v ;
                }
                ++ j ;
            }
            ++ i ;
        }
	}

    /**
     * Reads the state gris from the stack at the appropriate position.
     * @see com.act365.sudoku.IState#popState(int)
     */
          
	public void popState(int nMoves ) {
        int i , j , v ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                v = 0 ;
                while( v < cellsInRow ){
                    eliminated[v][i][j] = threadEliminated[nMoves][v][i][j];
                    nInvulnerable[v][i][j] = threadNInvulnerable[nMoves][v][i][j];
                    ++ v ;
                }
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
        int i , j , v , cx , cy ;
        // Check that it's a valid candidate.
        if( eliminated[value][x][y] ){
            throw new MoveAlreadyEliminatedException( x , y , value );
        }
        // Calc temp values.
        lowerX = ( x / boxesAcross )* boxesAcross ;
        upperX = ( x / boxesAcross + 1 )* boxesAcross ;
        lowerY = ( y / boxesDown )* boxesDown ;
        upperY = ( y / boxesDown + 1 )* boxesDown ;
        // Update nInvulnerable for (x,y).
        v = 0 ;
        while( v < cellsInRow ){
            nInvulnerable[v][x][y] = maxScore ;
            ++ v ; 
        }
        // Update nInvulnerable for the domain of (x,y).
        v = 0 ;
        while( v < cellsInRow ){
            if( eliminated[v][x][y] ){
                ++ v ;
                continue ;
            }
            // Shared column
            i = -1 ;
            while( ++ i < cellsInRow ){
                if( i == x || eliminated[v][i][y] ){
                    continue ;
                }
                if( v == value ){
                    nInvulnerable[v][i][y] = maxScore ;
                } else {
                    ++ nInvulnerable[v][i][y];
                }
            }
            // Shared row
            j = -1 ;
            while( ++ j < cellsInRow ){
                if( j == y || eliminated[v][x][j] ){
                    continue ;
                }
                if( v == value ){
                    nInvulnerable[v][x][j] = maxScore ;
                } else {
                    ++ nInvulnerable[v][x][j];
                }
            }
            // Shared subgrid
            i = lowerX - 1 ;
            while( ++ i < upperX ){
                if( i == x ){
                    continue ;
                }
                j = lowerY - 1 ;
                while( ++ j < upperY ){
                    if( j == y || eliminated[v][i][j] ){
                        continue ;
                    }
                    if( v == value ){
                        nInvulnerable[v][i][j] = maxScore ;
                    } else {
                        ++ nInvulnerable[v][i][j];
                    }
                }
            }
            ++ v ;
        }
        // Update nInvulnerable for the entire grid.
        int lowerCX , upperCX , lowerCY , upperCY ;
        cx = 0 ;
        while( cx < cellsInRow ){
            if( cx == x ){
                ++ cx ;
                continue ;
            }
            cy = 0 ;
            while( cy < cellsInRow ){
                if( eliminated[value][cx][cy] || cy == y || lowerX <= cx && cx < upperX && lowerY <= cy && cy < upperY ){
                    ++ cy ;
                    continue ;
                }
                lowerCX = cx / boxesAcross * boxesAcross ;
                upperCX = ( cx / boxesAcross + 1 )* boxesAcross ;
                lowerCY = cy / boxesDown * boxesDown ;
                upperCY = ( cy / boxesDown + 1 )* boxesDown ;
                i = 0 ;
                while( i < cellsInRow ){
                    if( i == x ){
                        j = 0 ;
                        while( j < cellsInRow ){
                            if( ! eliminated[value][i][j] ){
                                if( i == cx || j == cy || lowerCX <= i && i < upperCX && lowerCY <= j && j < upperCY ){
                                    ++ nInvulnerable[value][cx][cy];
                                }
                            }
                            ++ j ;
                        }
                    } else if( lowerX <= i && i < upperX ){
                        j = lowerY ;
                        while( j < upperY ){
                            if( ! eliminated[value][i][j] ){
                                if( i == cx || j == cy || lowerCX <= i && i < upperCX && lowerCY <= j && j < upperCY ){
                                    ++ nInvulnerable[value][cx][cy];
                                }
                            }
                            ++ j ;
                        }
                    } else if( ! eliminated[value][i][y] ){
                        if( i == cx || y == cy || lowerCX <= i && i < upperCX && lowerCY <= y && y < upperCY ){
                            ++ nInvulnerable[value][cx][cy];
                        }
                    }
                    ++ i ;
                }                
                ++ cy ;
            }
            ++ cx ;
        }
        // Update eliminated.        
        // Eliminate other candidates for the current cell.
        i = 0 ;
        while( i < cellsInRow ){
            if( i != value && ! eliminated[i][x][y] ){
                eliminated[i][x][y] = true ;
            }
            ++ i ;
        }
        // Eliminate other candidates for the current row.
        j = 0 ;
        while( j < cellsInRow ){
            if( j != y && ! eliminated[value][x][j] ){
                eliminated[value][x][j] = true ;
            }
            ++ j ;
        }
        // Eliminate other candidates for the current column.
        i = 0 ;
        while( i < cellsInRow ){
            if( i != x && ! eliminated[value][i][y] ){
                eliminated[value][i][y] = true ;
            }
            ++ i ;
        }
        // Eliminate other candidates for the current subgrid.
        i = lowerX - 1 ;
        while( ++ i < upperX ){
            if( i == x ){
                continue ;
            }
            j = lowerY - 1 ;
            while( ++ j < upperY ){
                if( j == y ){
                    continue ;
                }
                if( ! eliminated[value][i][j] ){
                    eliminated[value][i][j] = true ;
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
        int i , j , partial ;
        // Calc temp values.
        lowerX = ( x / boxesAcross )* boxesAcross ;
        upperX = ( x / boxesAcross + 1 )* boxesAcross ;
        lowerY = ( y / boxesDown )* boxesDown ;
        upperY = ( y / boxesDown + 1 )* boxesDown ;
        i = 0 ;
        while ( i < cellsInRow ){
            partial = inDomainPartial( x , i );
            j = 0 ;
            while( j < cellsInRow ){
                if( i == x && j == y ){
                    eliminated[value][x][y] = true ;
                    nInvulnerable[value][i][j] = maxScore ;
                } else if( ! eliminated[value][i][j] && inDomain( partial , y , j ) ){
                    ++ nInvulnerable[value][i][j];
                }
                ++ j ;
            }
            ++ i ;
        }
	}
    
    /** 
     * The next two functions split inDomain(), which calculates whether 
     * (p,q) is in the domain of (x,y), i.e. whether it shares a column, 
     * row or subgrid, in order to allow more efficient calculation.
     */ 
    
    int inDomainPartial( int x , int p ){
        if( x == p ){
            return 2 ;
        } else if( p >= lowerX && p < upperX ) {
            return 1 ;
        } else {
            return 0 ;
        }
    }
    
    boolean inDomain( int partial , int y , int q ){
        switch( partial ){
            case 2 :
            return true ;

            case 1 :
            return q >= lowerY && q < upperY ;
            
            case 0 :
            return q == y ;
            
            default:
            return false ;
        }
    }

    /**
     * Produces a string representation of the state grid.
     */
    
    @Override public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        int v = 0 ;
        while( v < cellsInRow ){
            sb.append("Value ");
            SuDokuUtils.appendValue( sb , v );
            sb.append(":\n\n");
            sb.append( SuDokuUtils.toString( nInvulnerable[v] , boxesAcross , maxScore , SuDokuUtils.ValueFormat.NUMERIC ) );
            sb.append("\n");
            ++ v ;
        }
        
        return sb.toString();
    }
}
