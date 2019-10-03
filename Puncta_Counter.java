/*
 * Puncta_Counter.java
 *
 * Adapted from the Cell Counter plugin written by Kurt De Vos (2005)
 * by Vito Cairone, Sebastian Espinosa, and Y. Jennifer Sun
 * for Dr. Michael Stryker's lab at the University of California San Francisco
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 *
 */

/**
 *
 * @author Kurt De Vos
 */

import ij.plugin.frame.PlugInFrame;

public class Puncta_Counter extends PlugInFrame {
    
    /** Creates a new instance of Puncta_Counter */
    public Puncta_Counter() {
         super("Puncta Counter");
         new PunctaCounter();
    }
    
    public void run(String arg){
    }
}
