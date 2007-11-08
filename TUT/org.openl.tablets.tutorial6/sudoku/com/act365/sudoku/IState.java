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
 * The IState interface is implemented by all the state types
 * used within strategy types to track possible Su Doku solutions. 
 */

public interface IState {

    /**
     * Sets up the state grid and its thread.
     */
    
    public void setup( int boxesAcross , int boxesDown );
    
    /**
     * Adds a state grid to the thread.
     * @param nMoves thread position to which state should be written
     */
    
    public void pushState( int nMoves );

    /**
     * Unwinds a state grid from the thread.
     * @param nMoves thread position from which state should be read
     */    
    
    public void popState( int nMoves );
    
    /**
     * Updates the state grid to account for the move (x,y):=v.
     */
    
    public void addMove( int x , int y , int v ) throws MoveException ;
    
    /**
     * Updates the state grid to account for the fact that the 
     * move (x,y):=v has been eliminated as a possibility.
     */

    public void eliminateMove( int x , int y , int v );
    
    /**
     * Produces a string representation of the state grid.
     */
    
    public String toString();
}
