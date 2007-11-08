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

/**
 * The LeastCandidatesNumber strategy calculates, for each combination of
 * number and sector (where a sector is a generic term that covers rows,
 * columns and subgrids), the number of valid candidate cells and fills 
 * the the sectors with the least number of possible candidate cells first.
 */

public class LeastCandidatesNumber extends StrategyBase implements IStrategy {

    boolean findMany ;
    
    boolean[][][] considered ;
    
    /**
     * Creates a new LeastCandidatesNumber instance to solve the given grid.
     * @param randomize whether the final candidate should be randomly chosen from the set of possibles
     * @param explain whether each move should be explained
     */
    
    public LeastCandidatesNumber( boolean randomize , boolean explain ){
        this( randomize , randomize , explain );
    }
    
    /**
     * Creates a new LeastCandidatesNumber instance to solve the given grid.
     * @param findMany whether an entire set of possible values should be found
     * @param randomize whether the final candidate should be randomly chosen from the set of possibles
     * @param explain whether explanatory debug should be produced
     */
    
    public LeastCandidatesNumber( boolean findMany , 
                                  boolean randomize ,
                                  boolean explain ){
        super( randomize , explain );
        this.findMany = findMany ;
        state = new NumberState();
    }
    
    /**
     * Sets the state variables.
     */
    
    public void setup( Grid grid ) throws Exception {

        super.setup( grid );

        if( findMany ){
            int i , j , k ;
            if( resize ){
                considered = new boolean[grid.cellsInRow][grid.cellsInRow][grid.cellsInRow];
            } else {
                i = 0 ;
                while( i < grid.cellsInRow ){
                    j = 0 ;
                    while( j < grid.cellsInRow ){
                        k = 0 ;
                        while( k < grid.cellsInRow ){
                            considered[i][j][k] = false ;
                            ++ k ;
                        }
                        ++ j ;
                    }
                
                    ++ i ;
                }
            }
        }
    }
    
    /** 
     * Find the values and sectors that have the least number of candidates.
     * As the code stands, there might well be duplicates. 
     * @see com.act365.sudoku.IStrategy#findCandidates()
     */
    
    public int findCandidates() {
        NumberState numberState = (NumberState) state ;
        // Find the unpopulated cells with the smallest number of candidates.       
        int i , j , k , x , y , maxEliminated = -1 ;
        StringBuilder sb ;
        nCandidates = 0 ;
        i = 0 ;
        findMaxEliminated:
        while( i < grid.cellsInRow ){
            j = 0 ;
            while( j < 3 * grid.cellsInRow ){
                if( numberState.nEliminated[i][j] == grid.cellsInRow ){
                    score = 0 ;
                    return ( nCandidates = 0 );
                } else if( numberState.isFilled[i][j] ){
                } else if( ! findMany && numberState.nEliminated[i][j] == grid.cellsInRow - 1 ){
                    nCandidates = 1 ;
                    maxEliminated = grid.cellsInRow - 1 ;
                    break findMaxEliminated ;
                } else if( numberState.nEliminated[i][j] > maxEliminated ){
                    nCandidates = 1 ;
                    maxEliminated = numberState.nEliminated[i][j];
                }
                ++ j ;
            }
            ++ i ;
        }
        if( nCandidates == 0 ){
            return 0 ;
        }
        score = grid.cellsInRow - maxEliminated ;
        nCandidates = 0 ;
        // Blank out the grid of considered values.
        if( findMany ){
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    k = 0 ;
                    while( k < grid.cellsInRow ){
                        considered[i][j][k] = false ;
                        ++ k ;
                    }
                    ++ j ;
                }
                ++ i ;
            }
        }
        // Convert into standard x,y:=value coordinate system
        i = 0 ;
        while( i < grid.cellsInRow ){
            j = 0 ;
            while( j < 3 * grid.cellsInRow ){
                if( ! numberState.isFilled[i][j] && numberState.nEliminated[i][j] == maxEliminated ){
                    k = 0 ;
                    while( k < grid.cellsInRow ){
                        if( ! numberState.eliminated[i][j][k] ){
                            if( j < grid.cellsInRow ){
                                x = j ;
                                y = k ;
                            } else if( j < 2 * grid.cellsInRow ){
                                x = k ;
                                y = j - grid.cellsInRow ;
                            } else {
                                x = ( j - 2 * grid.cellsInRow )/ grid.boxesAcross * grid.boxesAcross + k / grid.boxesDown ;
                                y = ( j - 2 * grid.cellsInRow )% grid.boxesAcross * grid.boxesDown + k % grid.boxesDown ;
                            }
                            if( findMany ){
                                if( considered[x][y][i] ){
                                    ++ k ;
                                    continue;
                                }
                                considered[x][y][i] = true ;
                            }
                            xCandidates[nCandidates] = x ;
                            yCandidates[nCandidates] = y ;
                            valueCandidates[nCandidates] = (byte)( i + 1 );
                            if( explain ){
                                sb = new StringBuilder();
                                sb.append("The cell ");
                                SuDokuUtils.appendCell( sb , x , y );
                                sb.append(" is ");
                                if( score > 1 ){
                                    sb.append("one of ");
                                    sb.append( score );
                                    sb.append(" candidates ");
                                } else {
                                    sb.append("the only candidate ");
                                }                                                             
                                sb.append("for the value ");
                                SuDokuUtils.appendValue( sb , i );
                                sb.append(" in ");
                                SuDokuUtils.appendSector( sb , grid.cellsInRow , grid.boxesAcross , j );
                                sb.append(".\n");
                                reasonCandidates[nCandidates] = sb ;
                            }
                            ++ nCandidates ;
                            if( ! findMany ){
                                return nCandidates ;
                            }
                        }
                        ++ k ;
                    }
                }
                ++ j ;
            }
            ++ i ;  
        }
        
        return nCandidates ;
    }
}
