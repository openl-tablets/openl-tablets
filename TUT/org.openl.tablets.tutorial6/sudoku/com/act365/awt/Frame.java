/*
 * act365.com Abstract Windowing Toolkit
 * 
 * Copyright (C) act365.com November 2004
 * 
 * Web site: http://act365.com
 * E-mail: developers@act365.com
 * 
 * The act365.com Abstract Windowing Toolkit defines standard GUI classes
 * common to all act365.com apps.
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

package com.act365.awt;

import java.awt.event.*;

/**
 * The Frame class differs from java.awt.Frame in that it closes if the 'X'
 * in the corner is clicked.
 * 
 * @see java.awt.Frame
 */

public class Frame extends java.awt.Frame {
    
    /**
     * Constructs a Frame with the chosen text as a title.
     */
    
    public Frame( String s ){
        super( s );
        addWindowListener( new WindowEventHandler() );
    }

    class WindowEventHandler extends WindowAdapter {
        @Override public void windowClosing( WindowEvent we ) {
            dispose();   
        }
    }
}
