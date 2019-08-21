/*
 * PunctaCntrImageCanvasAutolink.java
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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.gui.Overlay;
import ij.process.ImageProcessor;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ListIterator;
import java.util.Vector;
import ij.measure.ResultsTable;
import ij.WindowManager;
import java.awt.Color;

public class PunctaCntrImageCanvasAutolink extends ImageCanvas {
    private PunctaCounterAutolink pc;
    private ImagePlus img;
    //private Font font = new Font("SansSerif", Font.PLAIN, 10);
    private Font font = new Font("Arial", Font.PLAIN, 11);
	 public int canvasID;
	//canvasID is either 1 or 2, for the first and second image, respectively.
	//Markers also have a canvasID; the marker canvasID will match the image it belongs to,
	//or it will be equal to 0 if it goes with both.
    /** Creates a new instance of PunctaCntrImageCanvas */
    public PunctaCntrImageCanvasAutolink(ImagePlus img, PunctaCounterAutolink pc, int canvasID, Overlay displayList) {
        super(img);
        this.img=img;
        this.pc = pc;
		  this.canvasID = canvasID;
        if (displayList!=null)
            this.setOverlay(displayList);
    }
	
    public void mousePressed(MouseEvent e) {
		  pc.activeImg = img;
		  pc.activeIC = this;
        if (IJ.spaceBarDown() || Toolbar.getToolId()==Toolbar.MAGNIFIER || Toolbar.getToolId()==Toolbar.HAND) {
            super.mousePressed(e);
            return;
        }
        
        if (pc.currentMarkerVector==null){
            IJ.error("Select a counter type first!");
            return;
        }
        
        int x = super.offScreenX(e.getX());
        int y = super.offScreenY(e.getY());
        
        //Critical
        //This code currently forces type 1 to go with image 1 and type 2 to go with image 2
        PunctaCntrMarkerVector originalPCMV = pc.currentMarkerVector;
        //This is < 2 because index 1 is type 2, so index 2 is type 3
        if (pc.typeVector.indexOf(pc.currentMarkerVector) < 2) {
          if (canvasID == 1) {
            pc.currentMarkerVector = (PunctaCntrMarkerVector)pc.typeVector.get(0); //type 1
          } else {
            pc.currentMarkerVector = (PunctaCntrMarkerVector)pc.typeVector.get(1); //type 2    
          }
        }
        if (pc.isDelmode()) {
           PunctaCntrMarker m = pc.currentMarkerVector.getMarkerFromPosition(new Point(x,y),
             img.getCurrentSlice(), pc.compareMode ? canvasID : 0);
           //The drawn # of this marker is m.resultNum
           if (m.getRad() != 0)
             pc.currentMarkerVector.removeMarker(m);
        } else if (pc.manualgroupmode) {
           PunctaCntrMarker m = pc.currentMarkerVector.getMarkerFromPosition(new Point(x,y),
             img.getCurrentSlice(), pc.compareMode ? canvasID : 0);
           if (m.getRad() != 0)
             pc.manualGroupMarker(m, canvasID);
        } else if (pc.unlinkmode) {
           PunctaCntrMarker m = pc.currentMarkerVector.getMarkerFromPosition(new Point(x,y),
             img.getCurrentSlice(), pc.compareMode ? canvasID : 0);
           if (m.getRad() != 0)
             pc.unlinkMarker(m);
        } else if (pc.manuallinkmode) {
           PunctaCntrMarker m = pc.currentMarkerVector.getMarkerFromPosition(new Point(x,y),
             img.getCurrentSlice(), pc.compareMode ? canvasID : 0);
           if (m.getRad() != 0)
             pc.manualLinkMarker(m, canvasID);
        } else { //default - manual adding of new puncta
			 pc.MeasureByClick_new(x, y, img.getCurrentSlice(), canvasID);
			 pc.GroupAndRemoveRedundant(canvasID);
        }
        pc.currentMarkerVector = originalPCMV;
        repaint();
        pc.populateTxtFields();
    }
    
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
    }
    
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
    }
    
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
    }
    
    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        if (!IJ.spaceBarDown() | Toolbar.getToolId()!=Toolbar.MAGNIFIER | Toolbar.getToolId()!=Toolbar.HAND)
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }
    
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
    }
    
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
    }
	
    public void paint(Graphics g){
		pc.activeIC = this;
		pc.activeImg = img;
	
      //super.paint() calls the parent class to paint things we don't care about,
		//i.e. the frame around our image
		super.paint(g);
		
        srcRect = getSrcRect();
        Roi roi = img.getRoi();
        double xM=0;
        double yM=0;

		//For drawing arbitrary boundaries
		ImageProcessor ip;
		if (img.getStackSize() == 1) {
			ip = img.getProcessor();
		} else {
			ImageStack stack = img.getStack();
			ip = stack.getProcessor(img.getCurrentSlice());
		}
		ip.resetRoi();
		ip = ip.crop();
		//end for drawing arbitrary boundaries
        
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(1.25f));
        g2.setFont(font);    
      
		double mag = super.getMagnification();
		
        ListIterator it = pc.typeVector.listIterator();
        while(it.hasNext()){
            PunctaCntrMarkerVector mv = (PunctaCntrMarkerVector)it.next();
            int typeID = mv.getType();
            g2.setColor(mv.getColor());
            ListIterator mit = mv.listIterator();
			   int local_cutoff = 0;
            while(mit.hasNext()){
                PunctaCntrMarker m = (PunctaCntrMarker)mit.next();
                int z = m.getZ();
                boolean sameSlice = (m.getZ()==img.getCurrentSlice());
                if ((m.canvasID == 0 || m.canvasID == canvasID) && sameSlice) {
                    xM = ((m.getX()-srcRect.x)*magnification);
                    yM = ((m.getY()-srcRect.y)*magnification);
					int rad = m.getRad();
				
					if (pc.bkgrnd_avg1[z] >= 0) { 
					//used to have pc.arbitraryBoundaries as part of if statement
					//which was set to true in punctacounter.java
						if (m.resultNum != -1) {
					   	if (m.resultNum == 0) {
					     		g2.setColor(Color.yellow);
					   	} else if (m.linkedOwner == -1) {
						  		if (m.canvasID == 1)
						    		g2.setColor(Color.pink);
						  		else
						    		g2.setColor(Color.pink);
							} else {
						  		g2.setColor(Color.green);
							}
						}
						int[][] endpts = new int[rad*2+1][2];
						
						pc.restrictBoundary(m, endpts, ip);
						int j1 = 0;
						while (j1 < rad*2+1 && endpts[j1][0] == -1000) j1++;
						int orig_j1 = j1;
						int j2 = j1+1;
						while (j2 < rad*2+1 && endpts[j2][0] == -1000) j2++;
						if (j2 < rad*2+1) {
							g2.drawLine((int)(xM+endpts[j1][0]*mag),
										(int)(yM+(j1-rad)*mag),
										(int)(xM+endpts[j2][0]*mag),
										(int)(yM+(j2-rad)*mag));									
						}
						while (j2 < rad*2+1) {
							j1 = j2;
							j2 = j1+1;
							while (j2 < rad*2+1 && endpts[j2][0] == -1000) j2++;
							if (j2 < rad*2+1)
								g2.drawLine((int)(xM+endpts[j1][0]*mag),
											(int)(yM+(j1-rad)*mag),
											(int)(xM+endpts[j2][0]*mag),
											(int)(yM+(j2-rad)*mag));
						}
						//j1 is now already correct, do not set = j2
						j2 = rad*2;
						while (j2 >= 0 && endpts[j2][1] == -1000) j2--;
						if (j2 >= 0) {
							g2.drawLine((int)(xM+endpts[j1][0]*mag),
										(int)(yM+(j1-rad)*mag),
										(int)(xM+endpts[j2][1]*mag),
										(int)(yM+(j2-rad)*mag));
						}
						while (j2 >= 0) {
							j1 = j2;
							j2 = j1-1;
							while (j2 >= 0 && endpts[j2][1] == -1000) j2--;
							if (j2 >= 0)
								g2.drawLine((int)(xM+endpts[j1][1]*mag),
											(int)(yM+(j1-rad)*mag),
											(int)(xM+endpts[j2][1]*mag),
											(int)(yM+(j2-rad)*mag));
						}
						if (orig_j1 < rad*2+1 && j1 >= 0) 
							g2.drawLine((int)(xM+endpts[j1][1]*mag),
											(int)(yM+(j1-rad)*mag),
											(int)(xM+endpts[orig_j1][0]*mag),
											(int)(yM+(orig_j1-rad)*mag));
					} else  {
						if (m.resultNum != -1) {
					   	if (m.resultNum == 0) {
					     		g2.setColor(Color.yellow);
					   	} else if (m.linkedOwner == -1) {
						  		if (m.canvasID == 1)
						    		g2.setColor(Color.pink);
						  		else
						    		g2.setColor(Color.pink);
							} else {
						  		g2.setColor(Color.green);
							}
						}
						g2.drawOval((int)(xM-rad*mag), (int)(yM-rad*mag), (int)(rad*2*mag), (int)(rad*2*mag));
					}
					
					if (m.resultNum != -1) {
					   if (m.resultNum == 0) {
					     g2.setColor(Color.yellow);
					   }
						else if (m.linkedOwner == -1) {
						  if (m.canvasID == 1)
						    g2.setColor(Color.pink);
						  else
						    g2.setColor(Color.pink);
						} else {
						  g2.setColor(Color.green);
						}
						
						if (xM > img.getWidth()*.9)
							g2.drawString(Integer.toString(m.resultNum), (int)(xM-rad*mag-2), (int)(yM-rad*mag-2));
						else
						  g2.drawString(Integer.toString(m.resultNum), (int)(xM+rad*mag+2), (int)(yM-rad*mag-2));
					}
               }
            }
        }
    }
}