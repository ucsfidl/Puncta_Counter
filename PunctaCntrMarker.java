/*
 * Marker.java
 *
 * Adapted from the Cell Counter plugin written by Kurt De Vos (2005)
 * by Vito Cairone and Sebastian Espinosa for Dr. Michael Stryker's lab at the University of California San Francisco
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
	 public class PunctaCntrMarker {
    private int x;
    private int y;
    private int z;
	 private int rad;
	 private int owner;
	 private int uid;
	 public int flags; //for temporary use
	 public int linkedOwner;
	 public int canvasID;
	 public int resultNum;
    
    /** Creates a new instance of Marker */
    public PunctaCntrMarker() {
	
    }

    public PunctaCntrMarker(int x, int y, int z) {
        this.x=x;
        this.y=y;
        this.z=z;
		  this.rad = 30;
		  this.uid = -1;
		  this.owner = -1;
		  this.flags = 0;
		  this.canvasID = 0; //default
    }
    
    public PunctaCntrMarker(int x, int y, int z, int rad) {
        this.x=x;
        this.y=y;
        this.z=z;
		  this.rad = rad;
		  this.uid = -1;
		  this.owner = -1;
		  this.flags = 0;
		  this.canvasID = 0; //default
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }	

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
	
	public int getRad() {
        return rad;
    }

    public void setRad(int rad) {
        this.rad = rad;
    }
	
	public boolean isOwner() {
	     return (owner == uid);
	}
	
	public int getOwner() {
		  return owner;
	}
	
	public void setOwner(int owner) {
		  this.owner = owner;
	}
	
	public int getUID() {
		  return uid;
	}
	
	public void setUID(int uid) {
		  this.uid = uid;
	}
}
