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

import java.awt.* ;
import java.awt.event.* ;

/**
 * A SuDokuContainer contains two components - a GridContainer and
 * a ControlContainer.
 */

public class SuDokuContainer extends com.act365.awt.Container 
                             implements ComponentListener {

    GridContainer grid ;
    
    ControlContainer control ;
    
    /**
     * Creates a new SuDokuContainer to display the given grid and control.
     */

    public SuDokuContainer( GridContainer grid ,
                            ControlContainer control ){
        this.grid = grid ;
        this.control = control ;
        
        addComponent( grid , 0 , 0 , 1 , 1 , 1 , 1 );
        addComponent( control , 1 , 0 , 1 , 1 , 1 , 1 );
        
        grid.addComponentListener( this );
    }
                            
    /**
     * Returns the best display size for the container.
     */
    
	public Dimension getBestSize() {
		return new Dimension( grid.getBestSize().width + control.getBestSize().width ,
                              Math.max( grid.getBestSize().height , control.getBestSize().height ) );
	}
    
    /**
     * SuDokuContainer does nothing if the grid is hidden.
     */
    
    public void componentHidden( ComponentEvent evt ) {        
    }
    
    /**
     * SuDokuContainer does nothing if the grid is moved.
     */
    
    public void componentMoved( ComponentEvent evt ){        
    }

    /**
     * SuDokuContainer does nothing if the grid is resized.
     */
    
    public void componentResized( ComponentEvent evt ){        
    }
    
    /**
     * SuDokuContainer revalidates itself if the grid is revalidated.
     */
    
    public void componentShown( ComponentEvent evt ){
        validate();
    }
}
