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

import java.util.Random ;

/**
 * StrategyBase handles several thread-related function common to
 * most implementors of IStrategy.
 * @see IStrategy
 */

public abstract class StrategyBase {

    int size ;
    
    protected Grid grid ;
    
    // Thread variables
    
    protected boolean[] stateWrite ;
    
    protected int[] xMoves ,
                    yMoves ,
                    values ;
    
    protected StringBuilder[] reasons ;
          
    protected int nMoves ;
    
    // Candidates selected by findCandidates()
    
    protected int[] xCandidates ,
                    yCandidates ;
                    
    protected byte[] valueCandidates ;
    
    protected StringBuilder[] reasonCandidates ;
                    
    protected int nCandidates ;
    
    // Whether the selection should be random.
    
    protected boolean randomize ;
    
    Random generator ;
    
    // Whether explanatory debug should be produced.
    
    protected boolean explain ;
    
    // Score
    
    protected int score ;
    
    // Best candidate selected by selectCandidate()
    
    protected int bestX ,
                  bestY ;
                  
    protected byte bestValue ;
    
    protected String bestReason ;
    
    // State variables
    
    protected IState state ;
     
    // Whether the underlying grid has been resized.
    
    transient protected boolean resize ;
    
    /**
     * Creates a new base class with an optional random number generator.
     */
    
    protected StrategyBase( boolean randomize , boolean explain ){
        this.randomize = randomize ;                  
        this.explain = explain ;
        if( randomize ){
            generator = new Random();
        }
    }
    
    /**
     * Sets up the thread.
     */
    
    protected void setup( Grid grid ) throws Exception {
    
        resize = this.grid == null || grid.cellsInRow != size ;
            
        this.grid = grid ;
        size = grid.cellsInRow ;
        
        if( resize ){
            xMoves = new int[grid.cellsInRow*grid.cellsInRow];
            yMoves = new int[grid.cellsInRow*grid.cellsInRow];
            values = new int[grid.cellsInRow*grid.cellsInRow];
            reasons = new StringBuilder[grid.cellsInRow*grid.cellsInRow];
            stateWrite = new boolean[grid.cellsInRow*grid.cellsInRow];
            xCandidates = new int[grid.cellsInRow*grid.cellsInRow*grid.cellsInRow];
            yCandidates = new int[grid.cellsInRow*grid.cellsInRow*grid.cellsInRow];
            valueCandidates = new byte[grid.cellsInRow*grid.cellsInRow*grid.cellsInRow];
            reasonCandidates = new StringBuilder[grid.cellsInRow*grid.cellsInRow*grid.cellsInRow];
        }

        nMoves = 0 ;
        nCandidates = 0 ;    
        score = 0 ;
            
        bestX = grid.cellsInRow ; 
        bestY = grid.cellsInRow ;
        
        bestValue = (byte) grid.cellsInRow ;
        
        int i ;
        if( explain ){
            i = 0 ;
            while( i < grid.cellsInRow * grid.cellsInRow ){
                reasons[i++] = new StringBuilder();
            }
        }        
        if( state instanceof IState ){
            state.setup( grid.boxesAcross , grid.boxesDown );        
            int j ;
            i = 0 ;
            while( i < grid.cellsInRow ){
                j = 0 ;
                while( j < grid.cellsInRow ){
                    if( grid.data[i][j] > 0 ){
                        state.addMove( i , j , grid.data[i][j] - 1 );
                    }
                    ++ j ;
                }
                ++ i ;
            }
/*            
            if( explain ){
                state.pushState( 0 );
            }
*/            
        }
    }
    
    /**
     * Selects a single candidate from the available list.
     */
    
    public void selectCandidate() {
        int pick = randomize && nCandidates > 1 ? Math.abs( generator.nextInt() % nCandidates ) : 0 ;
        bestX = xCandidates[pick];
        bestY = yCandidates[pick];
        bestValue = valueCandidates[pick];  
        if( explain ){
            bestReason = reasonCandidates[pick].toString();   
        }
    }
    
    /**
     * Sets the value chosen by findCandidates().
     * @see com.act365.sudoku.IStrategy#setCandidate()
     */

    public void setCandidate() {
        grid.data[bestX][bestY] = bestValue ;
    }
    

    /**
     * Updates state variables.
     * @see com.act365.sudoku.IStrategy#updateState(int,int,int,String,boolean)
     */    
    
    public boolean updateState( int x , int y , int value , String reason , boolean writeState ) throws Exception {
        if( nMoves == -1 ){
            return false ;
        }
        // Store current state variables on thread.
        if( writeState ){
            state.pushState( nMoves );
            stateWrite[nMoves] = true ;
        } else {
            stateWrite[nMoves] = false ;
        }        
        // Store move to thread
        xMoves[nMoves] = x ;
        yMoves[nMoves] = y ;
        values[nMoves] = value - 1 ;
        if( explain ){
            reasons[nMoves].append( reason );
        }
        ++ nMoves ;
        // Update state variables
        state.addMove( x , y , value - 1 );
        return true ;
    }

    /**
     * Unwinds the the thread and reinstates state variables.
     * @see com.act365.sudoku.IStrategy#unwind(int,boolean,boolean)
     */
    
    public boolean unwind( int newNMoves , boolean reset , boolean eliminate ) {
        if( newNMoves < 0 ){
            return false ;
        }
        // Unwind thread.
        if( explain && reset ){
            reasons[newNMoves].append("The move (");
            reasons[newNMoves].append( 1 + xMoves[newNMoves] );
            reasons[newNMoves].append(",");
            reasons[newNMoves].append( 1 + yMoves[newNMoves] );
            reasons[newNMoves].append("):=");
            reasons[newNMoves].append( 1 + values[newNMoves] );
            reasons[newNMoves].append(" leads to a contradiction.\n");
            int i = newNMoves + 1 ;
            while( i < nMoves ){
                reasons[i].delete( 0 , reasons[i].length() );
                ++ i ;
            }
        }
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
    
    /**
     * Resets each cell that appears on the thread.
     * @see com.act365.sudoku.IStrategy#reset()
     */
    
    public void reset() {
        reset( 0 ); 
    }
    
    /**
     * Resets each cell that appears on the thread after the given move.
     * @see com.act365.sudoku.IStrategy#reset(int)
     */
    
    public void reset( int move ) {
        while( nMoves > move && nMoves >= 0 && nMoves <= grid.cellsInRow * grid.cellsInRow ){
            -- nMoves ;   
            grid.data[xMoves[nMoves]][yMoves[nMoves]] = 0 ;
        }       
        while( 0 <= nMoves && nMoves < move && nMoves < grid.cellsInRow * grid.cellsInRow ){
            grid.data[xMoves[nMoves]][yMoves[nMoves]] = (byte)( 1 + values[nMoves] );
            ++ nMoves ;   
        }   
    }
   
    /**
     * Returns the x-coordinate of the best candidate move.
     * @see com.act365.sudoku.IStrategy#getBestX()
     */
    
    public int getBestX(){
        return bestX ;
    }
    
    /**
     * Returns the y-coordinate of the best candidate move.
     * @see com.act365.sudoku.IStrategy#getBestY()
     */
    
    public int getBestY(){
        return bestY ;
    }
    
    /**
     * Returns the value of the best candidate move.
     * @see com.act365.sudoku.IStrategy#getBestValue()
     */
    
    public byte getBestValue(){
        return bestValue ;
    }
    
    /**
     * Returns the reason for the best move.
     * @see com.act365.sudoku.IStrategy#getBestReason()
     */
    
    public String getBestReason(){
        return bestReason ;
    }
    
    /**
     * Returns the x-coordinate of the given candidate.
     */
    
    public int getXCandidate( int index ){
        return xCandidates[ index ];    
    }

    /**
     * Returns the y-coordinate of the given candidate.
     */
    
    public int getYCandidate( int index ){
        return yCandidates[ index ];    
    }

    /**
     * Returns the value-coordinate of the given candidate.
     */
    
    public byte getValueCandidate( int index ){
        return valueCandidates[ index ];    
    }

    /**
     * Reasons the reason behind the given candidate.
     */

    public String getReasonCandidate( int index ){
        return reasonCandidates[index].toString();
    }
    
    /**
     * Returns the umber of candidates.
     */    
    
    public int getNumberOfCandidates(){
        return nCandidates ;
    }
    
    /**
     * Returns thread length.
     * @see com.act365.sudoku.IStrategy#getThreadLength()
     */
    
    public int getThreadLength(){
        return nMoves ;
    }
    
    /**
     * Returns x-coordinate of move at given thread position.
     * @see com.act365.sudoku.IStrategy#getThreadX(int)
     */
    
    public int getThreadX( int move ){
        return xMoves[ move ];
    }
    
    /**
     * Returns y-coordinate of move at given thread position.
     * @see com.act365.sudoku.IStrategy#getThreadY(int)
     */
    
    public int getThreadY( int move ){
        return yMoves[ move ];    
    }

    /**
     * Returns the reasoning behind the given move.
     * @see com.act365.sudoku.IStrategy#getReason(int)
     */

    public String getReason( int move ){
        return reasons[move].toString();    
    }
    
    /**
     * Indicates whether the strategy explains its reasoning.
     */
    
    public boolean explainsReasoning(){
        return explain ;
    }
    
    /**
     * Returns a measure of the confidence the strategy holds
     * in its candidates.
     * @see IStrategy#getScore()
     */
    
    public int getScore(){
        return score ;
    }
    
    /**
     * Returns the number of moves that had been made at the
     * last point where two alternative moves existed.
     */
    
    public int getLastWrittenMove(){
        int i = nMoves ; 
        while( -- i >= 0 ){
            if( stateWrite[i] ){
                break ;
            }
        }
        return i ;
    }
    
    /**
     * Dumps the thread to the given output stream.
     */
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        int i = 0 ;
        while( i < nMoves ){
            sb.append( ( 1 + i ) + ". (" + ( 1 + xMoves[i] ) + "," + ( 1 + yMoves[i] ) + "):=" + grid.data[xMoves[i]][yMoves[i]] + "\n");
            ++ i ;
        }
        sb.append("\n");
        sb.append( state.toString() );
        return sb.toString(); 
    }  
}
