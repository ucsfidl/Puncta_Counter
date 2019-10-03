/*
 * MarkerVector.java
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
import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D;
import java.util.ListIterator;
import java.util.Vector;
import ij.IJ;

public class PunctaCntrMarkerVector extends Vector<PunctaCntrMarker> {
	 private int next_uid;
	 public int assocWindowID;
	// On addition to the vector, each marker receives a unique ID (uid) which is
	//	used to track ownership; list position is not a stable reference because
	//	markers can decrease position when those before them are removed.
    private int type;
	//private int ownercount;
    private Color color;
    /** Creates a new instance of MarkerVector */
    public PunctaCntrMarkerVector(int type) {
      super();
      this.type=type;
		this.next_uid = 0;
      color = createColor(type);
    }
    public int addOwnerMarker(PunctaCntrMarker marker) {
		marker.setUID(next_uid);
		marker.linkedOwner = -1;
		this.next_uid++;
        add(marker);
		marker.setOwner(marker.getUID());
		return marker.getUID();
    }
    public void addSubjectMarker(PunctaCntrMarker marker) {
		marker.setOwner(-1);
		marker.linkedOwner = -1;
		marker.setUID(next_uid);
		this.next_uid++;
        add(marker);
    }
	public void addMarkerFromXML(PunctaCntrMarker marker) {
		if (marker.getUID() > this.next_uid) this.next_uid = marker.getUID()+1;
		add(marker);
	}
    
    public PunctaCntrMarker getMarker(int n){
        return (PunctaCntrMarker)get(n);
    }
	
    public int getVectorIndex(PunctaCntrMarker marker){
        return indexOf(marker);
    }
    
	public void removeSingle(int n) {
		this.remove(n);
	}
	
	public PunctaCntrMarker getMarkerByUID(int uid) {
		ListIterator it = this.listIterator();
        while(it.hasNext()){
            PunctaCntrMarker m = (PunctaCntrMarker)it.next();
            if (m.getUID() == uid)
                return m;
        }
		return null;
	}
	
	public void removeMarker(PunctaCntrMarker m) {
		int remove_group = m.getOwner();
        ListIterator it = this.listIterator();
		  int n;
        while(it.hasNext()){
		    n = it.nextIndex();
            PunctaCntrMarker m2 = (PunctaCntrMarker)it.next();
            if (m2.getOwner() == remove_group) {
				it.remove();
			}
		}
	}
	
	  //This function takes position in the internal list, which is not the same as a UID
    public void removeMarker(int n){
	    removeMarker(getMarker(n));
    }
	
    public void removeLastMarker(){
		removeMarker(size()-1);
    }
    
    private Color createColor(int typeID){
        switch(typeID){
            case(1):
                return Color.green;
            case(2):
                return Color.green;
            case(3):
                return Color.magenta;
            case(4):
                return Color.cyan;
            case(5):
                return Color.orange;
            case(6):
                return Color.pink;
            case(7):
                return Color.red;
            case(8):
                return Color.yellow;
            default:
                Color c = new Color((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()));
                while(c.equals(Color.green) | 
                        c.equals(Color.cyan) | 
                        c.equals(Color.blue) | 
                        c.equals(Color.magenta) | 
                        c.equals(Color.orange) | 
                        c.equals(Color.pink) |
                        c.equals(Color.red) |
                        c.equals(Color.yellow)){
                    c = new Color((int)(255*Math.random()),(int)(255*Math.random()),(int)(255*Math.random()));
                }
                return c;
        }
    }
    
    private boolean isCloser(PunctaCntrMarker m1,PunctaCntrMarker m2, Point p){
        Point2D p1 = new Point2D.Double(m1.getX(), m1.getY());
        Point2D p2 = new Point2D.Double(m1.getX(), m2.getY());
        System.out.println("px = "+p.x+ " py = "+p.y);
        System.out.println(Math.abs(p1.distance(p)) + " < "+ Math.abs(p2.distance(p)));
        return (Math.abs(p1.distance(p)) < Math.abs(p2.distance(p)));
    }

    public PunctaCntrMarker getMarkerFromPosition(Point p, int sliceIndex, int thisCanvasID){
        Vector<PunctaCntrMarker> v = new Vector<PunctaCntrMarker>();
        ListIterator it = this.listIterator();
        while(it.hasNext()){
            PunctaCntrMarker m = (PunctaCntrMarker)it.next();
            if (m.getZ()==sliceIndex && (m.canvasID == thisCanvasID || thisCanvasID == 0)){
                v.add(m);
            }
        }
        PunctaCntrMarker currentsmallest = (PunctaCntrMarker)v.get(0);
        Point p0 = new Point(currentsmallest.getX(),currentsmallest.getY());
        double currentLeastDist = Math.abs(p.distance(p0));
        for (int i=1; i<v.size(); i++) {
            PunctaCntrMarker m2 = (PunctaCntrMarker)v.get(i);
            Point p1 = new Point(currentsmallest.getX(),currentsmallest.getY());
            Point p2 = new Point(m2.getX(),m2.getY());
            boolean closer = Math.abs(p1.distance(p)) > Math.abs(p2.distance(p));
            if (closer) {
                currentsmallest = m2;
                currentLeastDist = p2.distance(p);
            }
        }
        if (currentLeastDist <= currentsmallest.getRad() + 10 ) //tolerance in pixels
          return currentsmallest;
        PunctaCntrMarker mEmpty = new PunctaCntrMarker(0, 0, 0, 0);
        return mEmpty;
    }
    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
	
	public int getOwnercount() {
		int c = 0;
		ListIterator it = this.listIterator();
        while(it.hasNext()){
            PunctaCntrMarker m = (PunctaCntrMarker)it.next();
            if (m.isOwner()) c++;
        }
		return c;
	}
}
