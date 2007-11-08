/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com July 2005
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
 * MoveException is the base class for all exceptions 
 * associated with the move (x,y):=value. 
 */

public abstract class MoveException extends Exception {
    
    int x , y , value ;
    
    protected MoveException( String s , int x , int y , int value ) {
        super( s );
        this.x = x ;
        this.y = y ;
        this.value = value ;    
    }
    
    public int getX() { return x ;}
    
    public int getY() { return y ;}
    
    public int getValue() { return value ;}
}
