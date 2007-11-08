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
 * The 'FirstAvailable' strategy always selects the first valid choice
 * it finds in any situation - it doesn't make any attempt to rank the
 * various alternatives. It is the only strategy implemented in 
 * Releases 1.0 and 1.1.
 */

public class FirstAvailable extends StrategyBase implements IStrategy {

    boolean lastMoveSuccessful ;
    
    // Cursor position
    
    int x , y ;
    byte value ;
    
    /**
     * Creates a new FirstAvailable instance.
     */
    
    public FirstAvailable() {
        super( false , false );
        lastMoveSuccessful = true ;
    }
    
    /**
     * Prepares the strategy to solve the given grid..
     */
    
    public void setup( Grid grid ) throws Exception {

        super.setup( grid );

        xCandidates = new int[1];
        yCandidates = new int[1];
        valueCandidates = new byte[1];

        x = 0 ;
        y = 0 ;
        value = 0 ;
        
        lastMoveSuccessful = true ;
        
        bestX = 0 ;
        bestY = 0 ;
        bestValue = 0 ;
    }
    
    /**
     * Locates the nearest empty cell, finds its lowest valid value
     * and stores the result.
     * @see com.act365.sudoku.IStrategy#findCandidates()
     */
    
	public int findCandidates() {
        if( lastMoveSuccessful ){        
            // Find the next empty cell.
            while( x < grid.cellsInRow && grid.data[x][y] > 0 ){
                ++ y ;
                while( y < grid.cellsInRow && grid.data[x][y] > 0 ){
                    ++ y ;
                }
                if( y == grid.cellsInRow ){
                    ++ x ;
                    y = 0 ;
                }
            }
        }
        // Find the smallest valid increase in value for the cell.
        byte originalValue = grid.data[x][y] ;
        while( ( value = ++ grid.data[x][y] ) <= grid.cellsInRow && ! isSound() );
        grid.data[x][y] = originalValue ;
        if( value <= grid.cellsInRow ){
            xCandidates[0] = x ;
            yCandidates[0] = y ;
            valueCandidates[0] = value ;
            score = 1 ;
            return ( nCandidates = 1 );
        } else {
            score = 0 ;
            return ( nCandidates = 0 );
        }
	}

    /**
     * Selects the single available candidate.
     * @see com.act365.sudoku#selectCandidate()
     */
    
    public void selectCandidate(){
        bestX = xCandidates[0];
        bestY = yCandidates[0];
        bestValue = valueCandidates[0];
    }
    
    /**
     * Updates state variables. 
     * @see com.act365.sudoku.IStrategy#updateState(int,int,int,String,boolean)
     * @param writeState is ignored
     */    
    
    public boolean updateState( int x , int y , int value , String reason , boolean writeState ){
        if( nMoves == -1 ){
            return false ;
        }
        lastMoveSuccessful = true ;
        // Store move to thread
        xMoves[nMoves] = x ;
        yMoves[nMoves] = y ;
        values[nMoves] = value - 1 ;
        stateWrite[nMoves] = true ;
        ++ nMoves ;
        return true ;
    }
    
    /**
     * Removes the current cell coordinates from the thread.
     * @see com.act365.sudoku.IStrategy#unwind(int,boolean,boolean)
     */
    
	public boolean unwind( int newNMoves , boolean reset , boolean eliminate ) {
        if( newNMoves < 0 ){
            return false ;
        } 
        -- nMoves ;
        lastMoveSuccessful = false ;
        if( grid.countFilledCells() == grid.cellsInRow * grid.cellsInRow ){
            return true ;
        }
        if( reset ){
            grid.data[x][y] = 0 ; 
        }
        x = xMoves[nMoves];
        y = yMoves[nMoves];
		return true ;
	}
	
	/**
	 * Determines whether a given column in the grid is sound, i.e. whether
	 * it contains no duplicates.
	 *  
	 * @param i - column to be tested
	 * @return true if the column is sound
	 */
    
	boolean isColumnSound( int i ){
        
	 boolean[] check = new boolean[grid.cellsInRow];
  
	 int j = 0 ;
     
	 while( j < grid.cellsInRow ){
	  if( grid.data[i][j] > 0 ){
	   if( check[grid.data[i][j]-1] ){
		return false ;
	   } else {
		check[grid.data[i][j]-1] = true ;
	   }
	  }
	  ++ j ;
	 }
     
	 return true ;
	}

	/**
	 * Determines whether a given row in the grid is sound, i.e. whether
	 * it contains no duplicates.
	 *  
	 * @param j - row to be tested
	 * @return true if the row is sound
	 */
    
	boolean isRowSound( int j ){
        
	 boolean[] check = new boolean[grid.cellsInRow];
  
	 int i = 0 ;
     
	 while( i < grid.cellsInRow ){
	  if( grid.data[i][j] > 0 ){
	   if( check[grid.data[i][j]-1] ){
		return false ;
	   } else {
		check[grid.data[i][j]-1] = true ;
	   }
	  }
	  ++ i ;
	 }
     
	 return true ;
	}
    
	/**
	 * Determines whether a given subgrid is sound, i.e. whether
	 * it contains no duplicates.
	 *  
	 * @param i - row coordinate of subgrid to be tested
	 * @param j - column coordinate of subgrid to be tested
	 * @return true if the subgrid is sound
	 */
    
	boolean isSubgridSound( int i , int j ){
     
		boolean[] check = new boolean[grid.cellsInRow];
        
		int k = 0 ;
        
		while( k < grid.cellsInRow ){
			if( grid.data[i*grid.boxesAcross+k%grid.boxesAcross][j*grid.boxesDown+k/grid.boxesAcross] > 0 ){
				if( check[grid.data[i*grid.boxesAcross+k%grid.boxesAcross][j*grid.boxesDown+k/grid.boxesAcross]-1] ){
					return false ;
				} else {
					check[grid.data[i*grid.boxesAcross+k%grid.boxesAcross][j*grid.boxesDown+k/grid.boxesAcross]-1] = true ;   
				}
			 }
			 ++ k ;
		}
        
		return true ;    
	}

	/**
	 * Determines whether the grid is sound - i.e. whether each row, column
	 * and subgrid within the grid is itself sound.
	 * 
	 * @return true if the grid is sound
	 */
    
	boolean isSound(){
     
		int i = 0 ;
        
		while( i < grid.cellsInRow ){
		   if( ! isColumnSound( i ) ){
			   return false ;  
		   } else if( ! isRowSound( i ) ) {
			   return false ;  
		   } else if( ! isSubgridSound( i % grid.boxesDown , i / grid.boxesDown ) ){
			   return false ;  
		   }
		   ++ i ;
		}
        
		return true ;
	}
}
