/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com January 2005
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
 * The MostCandidates strategy makes the move at any timestep that
 * will eliminate the greatest number of remaining candidates.
 * It is intended to be used in order to compose puzzles.
 */

public class MostCandidates extends StrategyBase implements IStrategy {

    boolean[][] mask ;

    /**
     * 
     * @param mask
     * @param randomize
     */
    
    public MostCandidates( boolean[][] mask , boolean randomize ){
        super( randomize , false );
        this.randomize = randomize ;
        this.mask = mask ;
        state = new InvulnerableState();
    }
    
    /**
     * Sets the state variables.
     */
    
    public void setup( Grid grid ) throws Exception {
        super.setup( grid );
    }

	/**
     * Finds the candidates for which nInvunerable is lowest.
	 * @see com.act365.sudoku.IStrategy#findCandidates()
	 */
    
	public int findCandidates(){
        InvulnerableState invulnerableState = (InvulnerableState) state ;
        // Find the unpopulated cells with the smallest number of candidates.       
        int i , j , v , minEliminated = Integer.MAX_VALUE ;
        nCandidates = 0 ;
        i = 0 ;
        while( i < grid.cellsInRow ){
            j = 0 ;
            while( j < grid.cellsInRow ){
                if( grid.data[i][j] > 0 || mask != null && ! mask[i][j] ){
                    ++ j ;
                    continue ;
                } 
                v = 0 ;
                while( v < grid.cellsInRow ){
                    if( ! invulnerableState.eliminated[v][i][j] && invulnerableState.nInvulnerable[v][i][j] < minEliminated ){
                        nCandidates = 1 ;
                        minEliminated = invulnerableState.nInvulnerable[v][i][j];
                    }
                    ++ v ;
                }
                ++ j ;
            }
            ++ i ;
        }
        if( nCandidates == 0 ){
            return 0 ;
        }
        score = 3 * grid.cellsInRow - grid.boxesAcross - grid.boxesDown - minEliminated ;
        nCandidates = 0 ;
        i = 0 ;
        while( i < grid.cellsInRow ){
            j = 0 ;
            while( j < grid.cellsInRow ){
                if( grid.data[i][j] > 0 || mask != null && ! mask[i][j] ){
                    ++ j ;
                    continue ;
                }
                v = 0 ;
                while( v < grid.cellsInRow ){
                    if( ! invulnerableState.eliminated[v][i][j] && invulnerableState.nInvulnerable[v][i][j] == minEliminated ){
                        xCandidates[nCandidates] = i ;
                        yCandidates[nCandidates] = j ;
                        valueCandidates[nCandidates] = (byte)( v + 1 );
                        ++ nCandidates ;
                        if( ! randomize ){
                            return nCandidates ;
                        }
                    }
                    ++ v ;
                }
                ++ j ;
            }
            ++ i ;  
        }           
        
        return nCandidates ;
	}

    /**
     * Unwinds the the thread and reinstates state variables.
     * Note that when a puzzle is created, the first value
     * is set without loss of generality. Therefore the thread 
     * is only ever unwound until a single move remains.
     * @see com.act365.sudoku.IStrategy#unwind(int,boolean,boolean)
     */
    
    public boolean unwind( int newNMoves , boolean reset , boolean eliminate ) {
        if( newNMoves < ( mask == null ? 0 : 1 ) ){
            return false ;
        }
        // Unwind thread.
        state.popState( newNMoves );
        if( eliminate ){
            state.eliminateMove( xMoves[newNMoves] , yMoves[newNMoves] , values[newNMoves] );
        }
        if( reset ){
            int i = newNMoves ;
            while( i < nMoves ){
                grid.data[xMoves[i]][yMoves[i]] = 0 ;
                ++ i ;
            }
        }
        nMoves = newNMoves ;
        return true ;
    }
}
