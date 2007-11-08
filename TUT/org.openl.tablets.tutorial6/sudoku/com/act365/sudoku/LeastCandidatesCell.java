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
 * The LeastCandidatesCell strategy calculates the number of seemingly valid
 * candidates (of course, for a problem with a unique solution, there is only
 * one strictly valid candidate for each cell - a candidate is deemed
 * 'seemingly valid' if it isn't blatantly contradicted by another cell in
 * that row, column or subgrid) for each cell in the grid, and fills the cells
 * with the least number of possible candidates first.
 */

public class LeastCandidatesCell extends StrategyBase implements IStrategy {

    boolean findMany ;
    
    /**
     * Creates a new LeastCandidatesCell instance.
     * @param randomize whether the final candidate should be randomly chosen from the set of possibles
     * @param explain whether each move should be explained
     */
    
    public LeastCandidatesCell( boolean randomize , 
                                boolean explain ){
        this( randomize , randomize , explain );
    }
    
    /**
     * Creates a new LeastCandidatesCell instance.
     * @param findMany whether an entire set of possible values should be found
     * @param randomize whether the final candidate should be randomly chosen from the set of possibles
     * @param explain whether explanatory debug should be produced
     */
    
    public LeastCandidatesCell( boolean findMany , 
                                boolean randomize ,
                                boolean explain ){
        super( randomize , explain );
        this.findMany = findMany ;
        state = new CellState();
    }
    
    /**
     * Sets the state variables.
     */
    
    public void setup( Grid grid ) throws Exception {
        super.setup( grid );
    }
    
	/** 
	 * Finds the cells that have the least number of candidates. 
	 * @see com.act365.sudoku.IStrategy#findCandidates()
	 */
	
	public int findCandidates() {
        CellState cellState = (CellState) state ;
		// Find the unpopulated cells with the smallest number of candidates.		
		int i , j , k , maxEliminated = -1 ;
        StringBuilder sb ;
        nCandidates = 0 ;
		i = 0 ;
        findMaxEliminated:
		while( i < grid.cellsInRow ){
			j = 0 ;
			while( j < grid.cellsInRow ){
                if( cellState.nEliminated[i][j] == grid.cellsInRow ){
                    score = 0 ;
                    return ( nCandidates = 0 );
                } else if( grid.data[i][j] > 0 ){
                } else if( ! findMany && cellState.nEliminated[i][j] == grid.cellsInRow - 1 ){
                    nCandidates = 1 ;
                    maxEliminated = grid.cellsInRow - 1 ;
                    break findMaxEliminated ;
				} else if( cellState.nEliminated[i][j] > maxEliminated ){
					nCandidates = 1 ;
					maxEliminated = cellState.nEliminated[i][j];
				}
				++ j ;
			}
			++ i ;
		}
        score = grid.cellsInRow - maxEliminated ;
        if( nCandidates == 0 ){
            return 0 ;
        }
        nCandidates = 0 ;
		i = 0 ;
		while( i < grid.cellsInRow ){
			j = 0 ;
			while( j < grid.cellsInRow ){
				if( grid.data[i][j] == 0 && cellState.nEliminated[i][j] == maxEliminated ){
                    k = 0 ;
                    while( k < grid.cellsInRow ){
                        if( ! cellState.eliminated[i][j][k] ){
                            xCandidates[nCandidates] = i ;
                            yCandidates[nCandidates] = j ;
                            valueCandidates[nCandidates] = (byte)( k + 1 );
                            if( explain ){
                                sb = new StringBuilder();
                                sb.append("The value ");
                                SuDokuUtils.appendValue( sb , k );
                                sb.append(" is ");
                                if( score > 1 ){
                                    sb.append("one of ");
                                    sb.append( score );
                                    sb.append(" candidates ");
                                } else {
                                    sb.append("the only candidate ");
                                }
                                sb.append("for the cell ");
                                SuDokuUtils.appendCell( sb , i , j );
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
