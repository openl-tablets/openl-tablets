/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com August 2005
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

package com.act365.sudoku.masks ;

import com.act365.sudoku.MaskFactory ;

/**
 * UserDefined represents a user-defined mask. 
 */

public class UserDefined extends MaskFactory {

    boolean[][] userMask ;
        
    public UserDefined( boolean[][] mask ) {
        super( mask.length );
        
        userMask = new boolean[cellsInRow][cellsInRow];
        
        int i , j ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                userMask[i][j] = mask[i][j];
                ++ j ;
            }
            ++ i ;
        }
    }
    
    /**
     * There is a sector for each cell.
     */
    
    public int countSectors(){
        return cellsInRow * cellsInRow ;    
    }
    
    public int allocateSectors( int filledCells ){
        filledCells = 0 ;
        
        int i = 0 ;
        while( i < cellsInRow * cellsInRow ){
            filledCells += sectorMin[i] = sectorMax[i] = userMask[i/cellsInRow][i%cellsInRow] ? 1 : 0 ;
            sectorSlots[i] = 1 ;
            ++ i ;
        }
                
        return filledCells ;
    }   

    public boolean areSectorsValid(){
        return true ;   
    }
    
    public void populateMask(){
        int i , j ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                mask[i][j] = userMask[i][j];
                ++ j ;
            }
            ++ i ;
        }
    }
}
