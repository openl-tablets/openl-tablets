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
 * Reflect8 represents a mask with reflective symmetry in
 * the vertical, horizontal and both diagonal axes. 
 */

public class Reflect8 extends MaskFactory {

    public Reflect8( int cellsInRow ) {
        super( cellsInRow );
    }
    
    /**
     * There are potentially sectors:
     * sector[0] represents everything apart from the axes.
     * sector[1] represents the diagonal axes.
     * sector[2] potentially represents the vertical and horizontal axes.
     * sector[3] potentially represents the centre cell.
     */
    
    public int countSectors(){
        return cellsInRow % 2 == 1 ? 4 : 2 ;    
    }
    
    public int allocateSectors( int filledCells ){
        if( cellsInRow % 2 == 1 ){
            if( filledCells % 4 > 1 ){
                filledCells +=  4 - filledCells % 4 ;
            }
            sectorMin[3] = sectorMax[3] = filledCells % 2 ;
            sectorSlots[3] = 1 ;
            sectorMin[2] = sectorMin[1] = 0 ;
            sectorMax[2] = sectorMax[1] = 
            sectorSlots[2] = sectorSlots[1] = ( cellsInRow - 1 )/ 2 ;
            sectorMin[0]= 0 ; 
            sectorMax[0]= ( filledCells - filledCells % 2 )/ 8 ;
            sectorSlots[0] = ( cellsInRow - 1 )*( cellsInRow - 3 )/ 8 ; 
        } else {
            if( filledCells % 4 != 0 ){
                filledCells += 4 - filledCells % 4 ; 
            }
            sectorMin[1] = 0 ;
            sectorMax[1] = sectorSlots[1] = cellsInRow / 2 ;
            sectorMin[0]= 0 ; 
            sectorMax[0]= filledCells / 8 ;
            sectorSlots[0] = cellsInRow *( cellsInRow - 2 )/ 8 ;            
        }
        
        return filledCells ;
    }   

    public boolean areSectorsValid(){
        if( nSectors == 4 ){
            return 8 * sectorBalls[0] + 4 *( sectorBalls[1] + sectorBalls[2] ) + sectorBalls[3] == filledCells ;   
        } else {
            return 4 * sectorBalls[0] + 4 * sectorBalls[1] == filledCells ;   
        }
    }
    
    public void populateMask(){
        int i , j , k ;
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
            while( j > i ){
                j -= i ++ ;
            }
            mask[i][j] = mask[j][i] =
            mask[cellsInRow-1-i][j] = mask[j][cellsInRow-1-i] =
            mask[cellsInRow-1-j][i] = mask[i][cellsInRow-1-j] =
            mask[cellsInRow-1-j][cellsInRow-1-i] = mask[cellsInRow-i-1][cellsInRow-j-1] = true ; 
            ++ j ; 
        }        
        i = k = 0 ;
        while( k < sectorBalls[1] ){
            i += g[1][k++];
            mask[i][i] = mask[cellsInRow-1-i][cellsInRow-1-i] = 
            mask[cellsInRow-1-i][i] = mask[i][cellsInRow-1-i] = true ; 
            ++ i ; 
        }
        if( nSectors == 4 ){
            i = k = 0 ;
            while( k < sectorBalls[2] ){
                i += g[2][k++];
                mask[i][cellsInRow/2] = mask[cellsInRow/2][i] = 
                mask[cellsInRow/2][cellsInRow-1-i] = mask[cellsInRow-1-i][cellsInRow/2] = true ; 
                ++ i ; 
            }
            mask[cellsInRow/2][cellsInRow/2] = filledCells % 2 == 1 ;
        }
    }
}
