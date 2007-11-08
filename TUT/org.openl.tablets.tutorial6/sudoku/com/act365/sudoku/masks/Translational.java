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
 * Translational represents a mask with translational symmetry. 
 */

public class Translational extends MaskFactory {

    int boxesAcross ,
        boxesDown ;
        
    public Translational( int boxesAcross , int boxesDown ) {
        super( boxesAcross * boxesDown );
        this.boxesAcross = boxesAcross ;
        this.boxesDown = boxesDown ;
    }
    
    /**
     * There is a single sector.
     */
    
    public int countSectors(){
        return 1 ;    
    }
    
    public int allocateSectors( int filledCells ){
        if( filledCells % boxesAcross != 0 ){
            filledCells += boxesAcross - filledCells % boxesAcross ;
        }
        sectorMin[0] = sectorMax[0] = filledCells / boxesAcross ;
        sectorSlots[0] = boxesDown *( cellsInRow - boxesAcross + 1 );
        
        return filledCells ;
    }   

    public boolean areSectorsValid(){
        return true ;   
    }
    
    public void populateMask(){
        int i , j , k , l ;
        i = 0 ;
        while( i < cellsInRow ){
            j = 0 ;
            while( j < cellsInRow ){
                mask[i][j] = false ;
                ++ j ;
            }
            ++ i ;
        }
        i = j = k = 0 ;
        while( k < sectorBalls[0] ){
            j += g[0][k++];
            i += j / boxesDown ;
            j = j % boxesDown ;
            l = 0 ;
            while( l < boxesAcross ){
                mask[i+l][j+l*boxesDown] = true ;
                ++ l ;
            }
            ++ j ; 
        }
    }
}
