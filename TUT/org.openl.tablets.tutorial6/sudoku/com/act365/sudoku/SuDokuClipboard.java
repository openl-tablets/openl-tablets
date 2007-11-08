/*
 * Su Doku Solver
 * 
 * Copyright (C) act365.com March 2005
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

/**
 * The SuDokuClipboard implements its own clipboard in a seperate window in order
 * to work around the security restrictions that affect access to the system clipboard
 * from an applet.
 */

public class SuDokuClipboard extends com.act365.awt.Frame {

    TextArea textArea ;
    
    /**
     * Creates an empty clipboard window.
     */
    
    public SuDokuClipboard() {
        super("Su Doku Clipboard");
        textArea = new TextArea();
        add( textArea );
        setSize( 400 , 400 );
        setFont( Font.decode("Monospaced") );
    }
    
    /**
     * Sets the text in the window.
     */
    
    public void setText( String s ){
        textArea.setText( s );
    }
    
    /**
     * Reads the text from the window.
     */
    
    public String getText(){
        return textArea.getText();
    }
}
