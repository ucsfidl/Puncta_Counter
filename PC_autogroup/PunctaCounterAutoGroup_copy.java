/* PUNCTA COUNTER AUTOGROUP
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
 * Last Update: 09/18/2019
 */

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.gui.Overlay;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.CompositeImage;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ListIterator;
import java.util.Vector;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.gui.Toolbar;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.event.MouseEvent;
import ij.measure.ResultsTable;
import java.lang.*;

public class PunctaCounterAutoGroup extends JFrame implements ActionListener, ItemListener {

    //DEVELOPMENT NOTES
    //This program contains a few redundant or inefficient paradigms developed
    //as new functions were added; to avoid frequent recursing of the list, it would
    //be ideal for subjects to point to owner objects rather than simply
    //refer for their ID, and similarly to have a faster way for owners to collect subjects.
    
    //Also, the resultNum and linkedOwner attributes are redundant and easily conflated;
    //resultNum is a number which is the same for all markers in two groups on different
    //lists/canvases, while linkedOwner is the owner number for an opposite group,
    //therefore two paired groups have linkedOwners which are reciprocal, NOT equal.
    //This is particularly evident in the manual linking code, and also requires
    //the user to manually unlink paired markers seperately, which will otherwise incorrectly
    //have green numbers. Ideally linkedOwner numbers should also be replaced
    //with pointers to reduce confusion with resultNum.
    
    //Also, much of the two-image code that was developed works only for type 1
    //and type 2 lists (typeVector(0) and typeVector(1) respectively) and relies
    //on the assumption that type 1 is for image 1 and type 2 is image 2;
    //if the other types above 2 are not going to be used than the whole type structure
    //is largely irrelevant and superfluous

    static final String pluginName = "Puncta_Counter";

	 private static final String INITIALIZE = "Initialize";
    private static final String LOADMARKERS = "Load Markers";
    private static final String EXPORTMARKERS = "Save Markers";
    private static final String DELMODE = "Delete Mode";
    private static final String AUTODETECT = "Autodetect";
	 private static final String AUTOLINK = "Autolink";
    private static final String UNLINKMODE = "Manual Unlink";
    private static final String MANUALLINKMODE = "Manual Link";
    private static final String MEASURE = "Measure";
    private static final String RESULTS = "Results";
    private static final String ADD = "Add Counter";
    private static final String REMOVE = "Remove Counter";
    private static final String RESET = "Reset";
    
    public Vector<PunctaCntrMarkerVector> typeVector;
    private Vector<JRadioButton> dynRadioVector;
    private Vector<JTextField> txtFieldVector;
    public PunctaCntrMarkerVector markerVector;
    public PunctaCntrMarkerVector currentMarkerVector;
    
    private GridLayout dynGrid;
    private JPanel dynPanel;
    private JPanel dynTxtPanel;
    private JPanel dynButtonPanel;    
    private JPanel statButtonPanel;
    
	 private JCheckBox delCheck;
	 private JCheckBox unlinkCheck;
	 private JCheckBox manuallinkCheck;
    private JCheckBox newCheck;
    private JCheckBox numbersCheck;
    private ButtonGroup radioGrp;
    private JSeparator separator;
    
    private JButton addButton;
    private JButton removeButton;
    private JButton initializeButton;
    private JButton resetButton;
    private JButton exportButton;
    private JButton loadButton;
    private JButton autodetectButton;
	 private JButton autolinkButton;
	 private JButton measureButton;
	 private JButton resultsButton;
    
	 //ic was used by CellCounter and is effectively the 'active' image canvas
	 //while the two seperate canvases for comparison mode are icA and icB
	 //respectively
	 public ImagePlus activeImg = null;
	 private ImagePlus img1 = null;
	 private ImagePlus img2 = null;
    public PunctaCntrImageCanvasAutoGroup activeIC;
	 private PunctaCntrImageCanvasAutoGroup ic1;
	 private PunctaCntrImageCanvasAutoGroup ic2;
	 private int nextCanvasID = 1;
    
    private boolean isJava14;
    
    static PunctaCounterAutoGroup instance;
	
	 public int nextResultNum = 1; 
	 private boolean delmode = false;
	 public boolean PixelZero = false;
	 public boolean unlinkmode = false;
	 public boolean manuallinkmode = false;
    private boolean showNumbers = true;
	 private boolean results_initialized = false;
	 ResultsTable rt = ResultsTable.getResultsTable();
	 public int[] wList = null;
	 public boolean compareMode = true;
	 public double[] bkgrnd_avg;
	 public double[] bkgrnd_stddev;
	 public int bkgrnd_initialized = 0;
	 public PunctaCntrImageCanvasAutoGroup compareTo = null;

	//input parameters
	public int BackgroundGridSize;										//Grid size used to calculate background levels
	//public int BackgroundGridSizeAutoDetect=16;		
	public int PixelsX,PixelsY,PixelsImg1Z,PixelsImg2Z;            						//Image pixels
	public float MicronsX,MicronsY,MicronsZ;       						//Image micrometers
 	public double MinRingRatio;	 										//Minimum ratio for finding ring
	public int MaxRadius;												//Largest allowabe ring radius
	public float ArbitraryLocalBoundaryCutoff;							//Arbitrary boundary cutoff as a function of bkgrnd_stddev 
	public int RemoveSurface;														//Remove surface slices from stack	
	public int MaxPunctaSizeinX, MaxPunctaSizeinY, MaxPunctaSizeinZ;	//Max puncta size in z-plane allowed
	public int PSD95NeighboringDensity, SyntNeighboringDensity;
	public int PSD95orSynt;
	public int Shiftx, Shifty, Shiftz;									//Shift in pixels for alignment of two stacks
	public int Group_Shiftx=0, Group_Shifty=0, Group_Shiftz=0;
	public int ximg1=0, yimg1=0, zimg1=0, ximg2=0, yimg2=0, zimg2=0;	
	public int xLinkTol=2,yLinkTol=2, zLinkTol=2;										//Tolerance for autolinking puncta
	public int xDensityTol, yDensityTol, zDensityTol;
	public int PunctaNeighborDensityCutoff;
	public int RemovePixels, RemoveMaxPunctaSizeinZ, RemoveDensity, RemoveZCount;
	public int ShiftEstimateCalculated=0;
    public String fileSeparator= System.getProperty("file.separator");   
    public String myDirectory= System.getProperty("user.home")+fileSeparator+"xml"+fileSeparator;    // NoteForUser: default direcotry is the image1 folder, but you can sepcify your own here 

    GenericDialog gd; 
       
    public PunctaCounterAutoGroup(){
        super("Puncta Counter");

        isJava14 = IJ.isJava14(); 
        if(!isJava14){
            IJ.showMessage("You are using a pre 1.4 version of java, exporting and loading marker data is disabled");
        }
        setResizable(false);
        
        //Autorun code starts here
        MicronsX = 200; // NoteForUser: Please indicate image size in micrometers dimension X
        MicronsY = 200; // NoteForUser: Please indicate image size in micrometers dimension Y
        MicronsZ = 1;   // NoteForUser: Please indicate image size in micrometers dimension Z
        //MinRingRatio = 2;
        //ArbitraryLocalBoundaryCutoff = 10;
        PSD95orSynt = 1;    // NoteForUser: Please indicate synaptic structure size here: PSD95 (1) or Synt(2)
		   
        typeVector = new Vector<PunctaCntrMarkerVector>();
        markerVector = new PunctaCntrMarkerVector(1);
        typeVector.add(markerVector);
        
        initializeImage();
        
        currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(0);
        String filePath = myDirectory+activeImg.getTitle()+"_2.xml";       
        loadMarkers2(filePath);
        autoGroupMarker();
        measure();    
        String filePath2 = myDirectory+activeImg.getTitle()+"_3.xml";
        exportMarkers2(filePath2);
        //End autorun code
        
    }
    
    void populateTxtFields(){
        ListIterator it = typeVector.listIterator();
        while (it.hasNext()){
            int index = it.nextIndex();
            PunctaCntrMarkerVector markerVector = (PunctaCntrMarkerVector)it.next();
            int count = markerVector.getOwnercount();
            JTextField tArea = (JTextField)txtFieldVector.get(index);
            tArea.setText(""+count);
        }
        validateLayout();
    }
    
    void validateLayout(){
        dynPanel.validate();
        dynButtonPanel.validate();
        dynTxtPanel.validate();
        statButtonPanel.validate();
        validate();
        pack();
    }
    
//-----------------------------INITIALIZE STACK(S)  
    private void initializeImage() {
     	if (nextCanvasID == 1) reset();
		if (nextCanvasID == 3) {
		  IJ.log("PunctaCounter currently supports only one or two images.");
		  return;
		}
      activeImg = WindowManager.getCurrentImage();
      if (activeImg==null){
            IJ.noImage();
      } else if (activeImg.getStackSize() == 1) {
            ImageProcessor ip = activeImg.getProcessor();
            ip.resetRoi();
            ip = ip.crop();
            
      } else {
            ImageStack stack = activeImg.getStack();
            int size = stack.getSize();
            for (int i = 1; i <= size; i++){
                ImageProcessor ip = stack.getProcessor(i);
                ip.resetRoi();
                ip = ip.crop();
            }  
      }
		Overlay displayList = activeImg.getOverlay();
		activeIC = new PunctaCntrImageCanvasAutoGroup(activeImg,this,nextCanvasID,displayList);
		if (nextCanvasID == 1) {
			img1 = activeImg;
			ic1 = activeIC;
		   PixelsX = img1.getWidth();
		   PixelsY = img1.getHeight();
		   PixelsImg1Z = img1.getStackSize();
			compareMode = false;
			BackgroundGridSize = (int)Math.round(Math.sqrt(PixelsX*PixelsY))/64; //sampling is 3.125 microns per pixel, equals 16 for 1024 pixel, 1x 200 micron image
			MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));

			//Begin set background level block 
         if (bkgrnd_initialized != 0)
           IJ.error("Background is already initialized in initializeImage");
			bkgrnd_avg = new double[img1.getStackSize()+1];
			bkgrnd_stddev = new double[img1.getStackSize()+1];
			//Note: May break if stackSize == 1 (?)
	      ImageStack stack = img1.getStack();
			ImageProcessor ip;
			for (int bz = 1; bz <= img1.getStackSize(); bz++) {
			   ip = stack.getProcessor(bz);
			   ip.resetRoi();
			   ip = ip.crop();
				MeasureData MD = findBackgroundLevel(ip);
				bkgrnd_avg[bz] = MD.avg;
				bkgrnd_stddev[bz] = MD.stddev;
			}
			bkgrnd_initialized = 2;
			//End set background level block 
			
		} 
		nextCanvasID++;
		
		if (activeImg.getStackSize() == 1)
			new ImageWindow(activeImg, activeIC);
		else
			new StackWindow(activeImg, activeIC);
    
      setType("1");
     
    }
    
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
  
		  if (activeIC != null) {
				if (ic2 != null) {
					ic1.repaint();
					ic2.repaint();
		  		} else {
		  		  activeIC.repaint();
		  		}
		  }
        populateTxtFields();
    }
    
    public void itemStateChanged(ItemEvent e){
        if (e.getItem().equals(delCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
                setDelmode(true);
          } else {
          		 setDelmode(false);
          }
        } else if (e.getItem().equals(unlinkCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
                unlinkmode = true;
          } else {
          		 unlinkmode = false;
          }
        } else if (e.getItem().equals(manuallinkCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
            	 manuallinkmode = true;
          } else { 
          		 manuallinkmode = false;
          }
        }
    }

//-----------------------------LOAD MARKERS
	 public void loadMarkers2(String filePath){  
        ReadXML rxml = new ReadXML(filePath);
        String storedfilename = rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
        if (storedfilename.equals(activeImg.getTitle())){
            rxml.readMarkerData(typeVector, 1);
            int index = Integer.parseInt(rxml.readImgProperties(rxml.CURRENT_TYPE));
            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
            setCurrentMarkerVector(currentMarkerVector); //redundant?
        }  
	 }
  
    public Vector getTypeVector() {
        return typeVector;
    } 
    
//-----------------------------EXPORT MARKERS  

    public void exportMarkers2(String filePath2) {
      //For one image only!!
      WriteXML wxml = new WriteXML(filePath2);
		wxml.writeXML(activeImg.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), 0);
    }   
         
    public static final int SAVE=FileDialog.SAVE, OPEN=FileDialog.LOAD;
    
    private String getFilePath(JFrame parent, String dialogMessage, int dialogType){
        switch(dialogType){
            case(SAVE):
                dialogMessage = "Save "+dialogMessage;
                break;
            case(OPEN):
                dialogMessage = "Open "+dialogMessage;
                break;
        }
        FileDialog fd ;
        String[] filePathComponents = new String[2];
        int PATH = 0;
        int FILE = 1;
        fd = new FileDialog(parent, dialogMessage, dialogType);
        switch(dialogType){
            case(SAVE):
                String filename = activeImg.getTitle();
                fd.setFile("PunctaCounter_"+filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                break;
        }
        fd.setVisible(true);
        filePathComponents[PATH] = fd.getDirectory();
        filePathComponents[FILE] = fd.getFile();
        return filePathComponents[PATH]+filePathComponents[FILE];
    }
    
//-----------------------------DELETE MODE
    public boolean isDelmode(){
        return delmode;
    }
    
    public void setDelmode(boolean delmode){
        this.delmode = delmode;
    }

//-----------------------------MEASURE BACKGROUND		
	private class MeasureData {
		private int total;
		private int pixels;
		private double avg;
		private double stddev;
	}
	
	private void addSample(MeasureData MD, double newavg, double newstddev, int oldcontribs) {
		if (oldcontribs == 0) {
			MD.avg = newavg;
			MD.stddev = newstddev;
			return;
		}
		MD.stddev = oldcontribs*(MD.stddev*MD.stddev + MD.avg*MD.avg);
		MD.stddev += newavg*newavg + newstddev*newstddev;
		MD.stddev /= (oldcontribs+1);
		MD.avg = (MD.avg*oldcontribs + newavg)/(oldcontribs+1);
		MD.stddev -= MD.avg*MD.avg;
		MD.stddev = Math.sqrt(MD.stddev);
	}
	
	private MeasureData findBackgroundLevel(ImageProcessor ip) {
		MeasureData MD = new MeasureData();
		int contribs = 0;
		int v;
		for (int ii = 0; ii < PixelsX; ii += BackgroundGridSize)
		for (int jj = 0; jj < PixelsY; jj += BackgroundGridSize) {
			int total = 0;
			double avg = 0;
			double stddev = 0;
			for (int x = 0; x < BackgroundGridSize; x++)
			for (int y = 0; y < BackgroundGridSize; y++) {
				v = (int)ip.getPixelValue(ii+x,jj+y);
				total += v;
				if (v == 0){ //one could put total instead of zero, but for warped images i think this is more accurate because there are cases where
			   				//the grid falls at the edge of real signal and black signal and no point to count this; difference between total or v is minor
                PixelZero = true;
          	}
			}
			
			if (!PixelZero) {
				avg = ((double)total)/(BackgroundGridSize*BackgroundGridSize);
				for (int x = 0; x < BackgroundGridSize; x++)
				for (int y = 0; y < BackgroundGridSize; y++) {
					v = (int)ip.getPixelValue(ii+x,jj+y);
					stddev += (v-avg) * (v-avg);
				}
				stddev /= BackgroundGridSize*BackgroundGridSize;
				stddev = Math.sqrt(stddev);
				if (contribs == 0 || avg <= MD.avg + MD.stddev) {
					addSample(MD, avg, stddev, contribs);
					contribs++;
				}
			}
			
			PixelZero = false;
		}
		return MD;
	}
	
//-----------------------------CRITERIA FOR SELECTING 2-D PUNCTA
	private PunctaCntrMarker findBoundary(int x, int y, int z) {
		ImageProcessor ip;
		if (activeImg.getStackSize() == 1) {
			ip = activeImg.getProcessor();
		} else {
			ImageStack stack = activeImg.getStack();
			ip = stack.getProcessor(z);
		}
		ip.resetRoi();
		ip = ip.crop();
		if (bkgrnd_initialized == 0) {
			IJ.error("Background not initialized");
		}
		int rad;
		int threshold = (int)(bkgrnd_avg[z]); //avoid tabulating 0's
		int[] rings_v;
	
		//30 is arbitrary here; it just needs to be sufficient
		//larger than max so that ring, calculated below,
		//is always less than the number allocated, approximately
		//max * root(2)
		rings_v = new int[30];
		int[] rings_c;
		rings_c = new int[30];
		for (int i = 0; i < 30; i++) {
			rings_c[i] = 0;
			rings_v[i] = 0;
		}
		      
		float v, d2;
		for (int i = -MaxRadius; i <= MaxRadius; i++) //critical parameter - MaxRadius ensure allocation above (30s) accomodate
		for (int j = -MaxRadius; j <= MaxRadius; j++) {
			v = ip.getPixelValue(x+i,y+j);
			d2 = i*i+j*j;
			int ring = (int)(Math.floor(Math.sqrt(d2)));
			if (v > 2*threshold) rings_v[ring] += v;
			else rings_v[ring] += 2*threshold;
			rings_c[ring] ++;
		}
		int best_rng = 0;
		double best_rat = 0;
		int inr_v = rings_v[0];
		int inr_c = rings_c[0];
		boolean rad_found = false;
		double prev_rat = 0;
		for (int i = 1; i < MaxRadius && rings_v[i] > 0 && !rad_found; i++) {
			int out_v = rings_v[i];
			int out_c = rings_c[i];
			double rat = ((double)inr_v/(double)inr_c)/((double)out_v/(double)out_c);
			if (rat < prev_rat && prev_rat > MinRingRatio) {
			  best_rng = i;
			  best_rat = prev_rat;
			  rad_found = true;
			} else prev_rat = rat;
			inr_v += out_v;
			inr_c += out_c;
		}
		if (best_rat > MinRingRatio) {
			rad = best_rng-1;
		
			//ring found, begin recenter
			int xLo = x-rad;
			int yLo = y-rad;
			int wid = rad*2+1; //do not change ever
			int[] xV;	xV = new int[wid];
			int[] yV;	yV = new int[wid];				
			for (int i = 0; i < wid; i++) {
			  xV[i] = 0;
			  yV[i] = 0;
			}
			for (int i = 0; i < wid; i++) {
				for (int j = 0; j < wid; j++) {
					v = ip.getPixelValue(xLo+i, yLo+j);
					xV[i] += v;
					yV[j] += v;
				}
			}
			int maxi = 0;
			int maxX = 0;
			int maxj = 0;
			int maxY = 0;
			for (int i = 1; i < wid-1; i++)
			  if (xV[i]+xV[i-1]+xV[i+1] > maxX) {
				maxX = xV[i]+xV[i-1]+xV[i+1];
				maxi = i;
			  }
			for (int j = 1; j < wid-1; j++)
			  if (yV[j]+yV[j-1]+yV[j+1] > maxY) {
				maxY = yV[j]+yV[j-1]+yV[j+1];
				maxj = j;
			  }
			int newX = xLo+maxi;
			int newY = yLo+maxj;
			//end recenter
			
			//find ring size again based on revised center
			if ((maxi != rad || maxj != rad) && rad != 0) {
				for (int i = 0; i < 30; i++) {
					rings_c[i] = 0;
					rings_v[i] = 0;
				}
				for (int i = -MaxRadius; i <= MaxRadius; i++)
				for (int j = -MaxRadius; j <= MaxRadius; j++) {
					v = ip.getPixelValue(newX+i,newY+j);
					d2 = i*i+j*j;
					int ring = (int)(Math.floor(Math.sqrt(d2)));
					if (v > 2*threshold) rings_v[ring] += v;
					else rings_v[ring] += 2*threshold;
					rings_c[ring] ++;
				}
				best_rng = 0;
				best_rat = 0;
				inr_v = rings_v[0];
				inr_c = rings_c[0];
				rad_found = false;
				prev_rat = 0;
				for (int i = 1; i < MaxRadius && rings_v[i] > 0 && !rad_found; i++) {
					int out_v = rings_v[i]; //v = value (total)
					int out_c = rings_c[i]; //c = count, i.e., # of pixels
					double test_inravg = (double)inr_v/(double)inr_c;
					double rat = test_inravg/((double)out_v/(double)out_c);
					if (rat < prev_rat && prev_rat > MinRingRatio)
					{
						best_rng = i;
						best_rat = prev_rat;
						rad_found = true;
					} else prev_rat = rat;
					inr_v += out_v;
					inr_c += out_c;
				}
				rad = best_rng-1;
			}
			if (rad > 1) {
				PunctaCntrMarker m = new PunctaCntrMarker(newX, newY, z, rad);
				return m;
			} else {
				PunctaCntrMarker m = new PunctaCntrMarker(0, 0, 0, 0);
				return m; //Spots need to be > 4 pixels and non-overlapping
			}
		} else {
			PunctaCntrMarker m = new PunctaCntrMarker(0, 0, 0, 0);
			return m; //No spot detected or diameter > MaxRadius
		}
	}

	private Rectangle srcRect = new Rectangle(0, 0, 0, 0);
	
	private Point point;
	
	public void restrictBoundary(PunctaCntrMarker m, int[][] endpts, ImageProcessor ip) {
		int rad = m.getRad();
		int z = m.getZ();
		boolean brightFound;
		int local_cutoff = (int)(bkgrnd_avg[z] + ArbitraryLocalBoundaryCutoff*bkgrnd_stddev[z]); //critical parameter for boundary of each punctum
		int v;
        
		//The endpts[] array stores values from 0 to 2*rad+1; values for [0] corresponds
		//to the line getY-rad and values at [2*rad+1] correspond to getY+rad
		for (int j = -rad; j <= rad; j++) {
			endpts[j+rad][0] = -1000; 
			endpts[j+rad][1] = -1000; 
			//The counter i is decreasing and we're checking the value of pixel x-1
			//while it exists in the image (>=0); we continue while a bright
			//spot is found, i.e. moving x- of center, and when a bright spot
			//is not found we don't reach the continue, therefor we quit
			int i;
			for (i = 0; m.getX()+i >= 0 && i >= -rad; i--) {
				v = (int) ip.getPixel(m.getX()+i, m.getY()+j);
				if (v <= local_cutoff) break;
			}
         if (i < 0) endpts[j+rad][0] = i;
			for (i = 0; m.getX()+i < activeImg.getWidth() && i <= rad; i++) {
				v = (int) ip.getPixel(m.getX()+i, m.getY()+j);
				if (v <= local_cutoff) break;
			}
			if (i > 0) endpts[j+rad][1] = i; 
		}
		
		//Smooth over jagged spines... need to add criteria so the arbitary drawing is >2 pixels
		//since smallest punctum has a diameter of 5 pixels. we don't want something drawn that is too small
		//but that still meets the criteria because the circle is bigger.
		for (int j = -rad+1; j < rad; j++) {
			if (endpts[j+rad-1][0] != -1000 && endpts[j+rad][0] != -1000 && endpts[j+rad+1][0] != -1000
				&& endpts[j+rad][0] < endpts[j-1+rad][0]-2
				&& endpts[j+rad][0] < endpts[j+1+rad][0]-2)
				endpts[j+rad][0] = (endpts[j-1+rad][0]+endpts[j+1+rad][0])/2-2;
			if (endpts[j+rad-1][1] != -1000 && endpts[j+rad][1] != -1000 && endpts[j+rad+1][1] != -1000
				&& endpts[j+rad][1] > endpts[j-1+rad][1]+2
				&& endpts[j+rad][1] > endpts[j+1+rad][1]+2)
				endpts[j+rad][1] = (endpts[j-1+rad][1]+endpts[j+1+rad][1])/2+2;
		}
	 }
	
//-----------------------------MANUAL DETECT PUNCTA IN 1 OR 2 STACKS
	public void MeasureByClick(int x, int y, int z) {
		int startSlice = z;
		PunctaCntrMarker m = findBoundary(x, y, startSlice);
		if (m.getRad() > 0) {
			PunctaCntrMarker m_orig = m;
			int group_intensity = 0;
			int group_stddev = 0;
			int group_max = 0;
			int group_pixels = 0;
			
			int thisSlice = startSlice;
			int uid = currentMarkerVector.addOwnerMarker(m);
			m.canvasID = activeIC.canvasID;
			int loZ = startSlice;
			int hiZ = startSlice;
			boolean foundspot = true;
			while (foundspot && thisSlice+1 <= activeImg.getStackSize()) {
			  thisSlice++;
			  m = findBoundary(x, y, thisSlice);
			  if (m.getRad() > 0) {
				m.setOwner(uid);
				currentMarkerVector.addSubjectMarker(m);
				m.canvasID = activeIC.canvasID;
				x = m.getX();
				y = m.getY();
			  } else {
				foundspot = false;
				hiZ = thisSlice-1;
			  }
			}
			foundspot = true;
			thisSlice = startSlice;
			while (foundspot && thisSlice-1 >= 1) {
			  thisSlice--;
			  m = findBoundary(x, y, thisSlice);
			  if (m.getRad() > 0) {
				m.setOwner(uid);
				currentMarkerVector.addSubjectMarker(m);
				m.canvasID = activeIC.canvasID;
				x = m.getX();
				y = m.getY();
			 } else {
				foundspot = false;
				loZ = thisSlice+1;
			  }
			}
		} // end if m.getRad() > 0
	}
		 
//-----------------------------CRITERIA FOR SELECTING 3-D PUNCTA - Groups together 2D circles into a 3D sphere, removes groups where z=1 or z>MaxPunctaSizeinZ
	public void GroupAndRemoveRedundant() {
		PunctaCntrMarker m1;
		PunctaCntrMarker m2;
		 
		for (int n = 0; n < currentMarkerVector.size(); n++) {
			m1 = (PunctaCntrMarker)currentMarkerVector.get(n);
			for (int n2 = n+1; n2 < currentMarkerVector.size(); n2++) {
				m2 = (PunctaCntrMarker)currentMarkerVector.get(n2);
				//deletes redundant markers
				if (m1.getZ() == m2.getZ() && m1.canvasID != 0 && m1.canvasID == m2.canvasID) {
					int x = m1.getX() - m2.getX();
					int y = m1.getY() - m2.getY();
					int r1 = m1.getRad();
					int r2 = m2.getRad();
					if (x*x+y*y < r1*r1 || x*x+y*y < r2*r2) {
						currentMarkerVector.removeSingle(n2);
						n2--;
					}
				//groups markers that are on different slices, but whos x and y coordinates fall within the its
				//2-dimensional radius
				} else if (m2.getZ() == m1.getZ() + 1) {
					int x = m1.getX() - m2.getX();
					int y = m1.getY() - m2.getY();
					int r1 = m1.getRad();
					int r2 = m2.getRad();
					
					if (x*x+y*y < r1*r1 || x*x+y*y < r2*r2) {	
							
						m2.setOwner(m1.getOwner());
					}					
				}
			}
		}	
	}	
		 
	 public void autoGroupMarker() {
   	if (PSD95orSynt == 2) {
   		if (compareMode) {
 	     		PunctaCntrMarkerVector startingCMV = currentMarkerVector;
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
				PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
				activeImg = img1;
				activeIC = ic1;
				currentMarkerVector = typeVector.get(0);
		
				int MaxMovementXNeg = 0;
				int MaxMovementYNeg = 0;
				int MaxMovementXPos = 0;
				int MaxMovementYPos = 0;
				int currentZ = 0;
				int count = 0;
				int temp1 = 0;
				int temp2 = 0;
				int temp3 = 0;
				int temp4 = 0;
				int BestMatch = 1000;
				int BestMatch2 = 1000;
				boolean ignore = false;
			 
				for (int n1 = 0; n1 < PCMVA.size(); n1++) {
					PunctaCntrMarker m1 = PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					//what happens when there are none that maintain same resultnum? 
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = PCMVA.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVA.size(); n10++) {
							PunctaCntrMarker m10 = PCMVA.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVA.size(); n4++) {
						PunctaCntrMarker m4 = PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVA.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(0);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				}
									
				activeImg = img2;
				activeIC = ic2;
				currentMarkerVector = typeVector.get(1);
								
				MaxMovementXNeg = 0;
				MaxMovementYNeg = 0;
				MaxMovementXPos = 0;
				MaxMovementYPos = 0;
				currentZ = 0;
				count = 0;
				temp1 = 0;
				temp2 = 0;
				temp3 = 0;
				temp4 = 0;
				BestMatch = 1000;
				BestMatch2 = 1000;
				ignore = false;
			 
				for (int n1 = 0; n1 < PCMVB.size(); n1++) {
					PunctaCntrMarker m1 = PCMVB.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVB.size(); n2++) {
						PunctaCntrMarker m2 = PCMVB.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVB.size(); n3++) {
								PunctaCntrMarker m3 = PCMVB.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVB.size(); n10++) {
							PunctaCntrMarker m10 = PCMVB.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVB.size(); n4++) {
						PunctaCntrMarker m4 = PCMVB.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVB.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVB.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVB.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVB.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVB.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVB.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVB.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(1); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(1);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(1); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				} 	
			} else {
				PunctaCntrMarkerVector startingCMV = currentMarkerVector;
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		
				activeImg = img1;
				activeIC = ic1;
				currentMarkerVector = typeVector.get(0);
		
				int MaxMovementXNeg = 0;
				int MaxMovementYNeg = 0;
				int MaxMovementXPos = 0;
				int MaxMovementYPos = 0;
				int currentZ = 0;
				int count = 0;
				int temp1 = 0;
				int temp2 = 0;
				int temp3 = 0;
				int temp4 = 0;
				int BestMatch = 1000;
				int BestMatch2 = 1000;
				boolean ignore = false;
			 
				for (int n1 = 0; n1 < PCMVA.size(); n1++) {
					PunctaCntrMarker m1 = PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = PCMVA.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVA.size(); n10++) {
							PunctaCntrMarker m10 = PCMVA.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVA.size(); n4++) {
						PunctaCntrMarker m4 = PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVA.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(0);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				}
			} 			
		} else {
			if (compareMode) {
 	     		PunctaCntrMarkerVector startingCMV = currentMarkerVector;
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
				PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
				activeImg = img1;
				activeIC = ic1;
				currentMarkerVector = typeVector.get(0);
		
				int MaxMovementXNeg = 0;
				int MaxMovementYNeg = 0;
				int MaxMovementXPos = 0;
				int MaxMovementYPos = 0;
				int currentZ = 0;
				int count = 0;
				int temp1 = 0;
				int temp2 = 0;
				int temp3 = 0;
				int temp4 = 0;
				int BestMatch = 1000;
				int BestMatch2 = 1000;
				boolean ignore = false;
			 
				for (int n1 = 0; n1 < PCMVA.size(); n1++) {
					PunctaCntrMarker m1 = PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					//what happens when there are none that maintain same resultnum? 
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = PCMVA.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVA.size(); n10++) {
							PunctaCntrMarker m10 = PCMVA.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVA.size(); n4++) {
						PunctaCntrMarker m4 = PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVA.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(0);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				}
									
				activeImg = img2;
				activeIC = ic2;
				currentMarkerVector = typeVector.get(1);
								
				MaxMovementXNeg = 0;
				MaxMovementYNeg = 0;
				MaxMovementXPos = 0;
				MaxMovementYPos = 0;
				currentZ = 0;
				count = 0;
				temp1 = 0;
				temp2 = 0;
				temp3 = 0;
				temp4 = 0;
				BestMatch = 1000;
				BestMatch2 = 1000;
				ignore = false;
			 
				for (int n1 = 0; n1 < PCMVB.size(); n1++) {
					PunctaCntrMarker m1 = PCMVB.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVB.size(); n2++) {
						PunctaCntrMarker m2 = PCMVB.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVB.size(); n3++) {
								PunctaCntrMarker m3 = PCMVB.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVB.size(); n10++) {
							PunctaCntrMarker m10 = PCMVB.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVB.size(); n4++) {
						PunctaCntrMarker m4 = PCMVB.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVB.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVB.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVB.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVB.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVB.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVB.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVB.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(1); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(1);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(1); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				}
		}	else {
				PunctaCntrMarkerVector startingCMV = currentMarkerVector;
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		
				activeImg = img1;
				activeIC = ic1;
				currentMarkerVector = typeVector.get(0);
		
				int MaxMovementXNeg = 0;
				int MaxMovementYNeg = 0;
				int MaxMovementXPos = 0;
				int MaxMovementYPos = 0;
				int currentZ = 0;
				int count = 0;
				int temp1 = 0;
				int temp2 = 0;
				int temp3 = 0;
				int temp4 = 0;
				int BestMatch = 1000;
				int BestMatch2 = 1000;
				boolean ignore = false;
			 
				for (int n1 = 0; n1 < PCMVA.size(); n1++) {
					PunctaCntrMarker m1 = PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = PCMVA.get(n3);
								
								if (m3.resultNum == m2.resultNum && m3.getZ() == (m2.getZ() + 1)) {
									int dx = m3.getX() - m2.getX();
									int dy = m3.getY() - m2.getY();		
									
									if (dx < MaxMovementXNeg) {
										MaxMovementXNeg = dx;
									} else if (dx > MaxMovementXPos) {
										MaxMovementXPos = dx;
									}
									
									if (dy < MaxMovementYNeg) {
										MaxMovementYNeg = dy;
									} else if (dy > MaxMovementYPos){
										MaxMovementYPos = dy;
									}				
								}	
							}
						}
					}
				
					if (MaxMovementXPos == 0 && MaxMovementXNeg == 0 && MaxMovementYPos == 0 && MaxMovementYNeg == 0) { //this is for when the movement is so great that all puncta are displaced from one slice to the next								
						for (int n10 = n1; n10 < PCMVA.size(); n10++) {
							PunctaCntrMarker m10 = PCMVA.get(n10);
							
							if (m10.getZ() == (m1.getZ() + 1)) {
								int dx = m10.getX() - m1.getX();
								int dy = m10.getY() - m1.getY();
								count++;
										
								if (dx < 0 && MaxMovementXNeg == 0) {
									MaxMovementXNeg = dx;
								} else if (dx < 0 && dx > MaxMovementXNeg) {
									MaxMovementXNeg = dx;
								} 
													
								if (dx > 0 && MaxMovementXPos == 0) {
									MaxMovementXPos = dx;
								} else if (dx > 0 && dx < MaxMovementXPos) {
									MaxMovementXPos = dx;
								}
									
								if (dy < 0 && MaxMovementYNeg == 0) {
									MaxMovementYNeg = dy;
								} else if (dy < 0 && dy > MaxMovementYNeg) {
									MaxMovementYNeg = dy;
								} 
										
								if (dy > 0 && MaxMovementYPos == 0) {
									MaxMovementYPos = dy;
								} else if (dy > 0 && dy < MaxMovementYPos) {
									MaxMovementYPos = dy;
								}					
							}	
						}
						
						if ((MaxMovementXPos + MaxMovementYPos) > (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXPos = Math.abs(MaxMovementXNeg);
							MaxMovementYPos = Math.abs(MaxMovementYNeg);
						} else if ((MaxMovementXPos + MaxMovementYPos) < (Math.abs(MaxMovementXNeg) + Math.abs(MaxMovementYNeg))) {
							MaxMovementXNeg = MaxMovementXPos;
							MaxMovementYNeg = MaxMovementYPos;
						}
						
						if (((MaxMovementXPos + MaxMovementYPos) > 20) && count > 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						} else if (((MaxMovementXPos + MaxMovementYPos) > 10) && count <= 10){
							MaxMovementXNeg = 0;
							MaxMovementYNeg = 0;
							MaxMovementXPos = 0;
							MaxMovementYPos = 0;
						}
						
						count = 0;												
					}
					
					PunctaCntrMarker BestMatchNum = null;
					PunctaCntrMarker BestMatchNum2 = null;
										
					for (int n4 = n1; n4 < PCMVA.size(); n4++) {
						PunctaCntrMarker m4 = PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = PCMVA.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
								}
							}			
																				
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
																
								int r1 = m1.getRad();
								int r4 = m4.getRad();
									
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch)) {	//before assigning make sure that there isn't another puncta that meets this criteria and where it is closer											
									BestMatch = dx1*dx1+dy1*dy1;
									BestMatchNum = m4;									
								}	
							}				
						} 
					
						if (BestMatchNum == null && m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 2) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane
								PunctaCntrMarker m5 = PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = PCMVA.get(n6);
									if (m6.resultNum == m1.resultNum && m6.getZ() == m4.getZ()) {
										ignore = true;
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m4.getZ() - 1)) { //m4.getZ() - 1 = m1.getZ() + 1 
										ignore = true; //wait to group in next slice when distance between grouping puncta is only 1 slice and not 2 slices								
									}
									
									if (m6.resultNum == m1.resultNum && m6.getZ() == (m1.getZ() - 1)) {
										temp4 = 1;
									}
								}
							}
							
							if (ignore != true && temp3 == 1 && temp4 == 1) { 
								ignore = true;
							}
												
													
							if (ignore != true) {
								int dx1 = m4.getX() - m1.getX();
								int dx2 = m4.getX() - m1.getX();
								if (dx1 < 0) {
									dx1 = dx1 - MaxMovementXNeg;
									dx2 = dx2 + MaxMovementXPos;
								} else {
									dx1 = dx1 + MaxMovementXNeg;
									dx2 = dx2 - MaxMovementXPos;
								}
					
								int dy1 = m4.getY() - m1.getY();
								int dy2 = m4.getY() - m1.getY();
								if (dy1 < 0) {								
									dy1 = dy1 - MaxMovementYNeg;
									dy2 = dy2 + MaxMovementYPos;
								} else {
									dy1 = dy1 + MaxMovementYNeg;
									dy2 = dy2 - MaxMovementYPos;
								}
								
								if (Math.abs(dx2) < Math.abs(dx1)) {
									dx1 = dx2;
								} 
								
								if (Math.abs(dy2) < Math.abs(dy1)) {
									dy1 = dy2;
								} 
								
								//makes sure that the next two puncta in the same slice as m1 is not closer to m4
								for (int n7 = n1+1; n7 < n1+3; n7++) {
									PunctaCntrMarker m1next = PCMVA.get(n7);
	
									int dxm1next1 = m4.getX() - m1next.getX();
									int dxm1next2 = m4.getX() - m1next.getX();
									if (dxm1next1 < 0) {								
										dxm1next1 = dxm1next1 - MaxMovementXNeg;
										dxm1next2 = dxm1next2 + MaxMovementXPos;
									} else {
										dxm1next1 = dxm1next1 + MaxMovementXNeg;
										dxm1next2 = dxm1next2 - MaxMovementXPos;
									}
								
									int dym1next1 = m4.getY() - m1next.getY();
									int dym1next2 = m4.getY() - m1next.getY();
									if (dym1next1 < 0) {								
										dym1next1 = dym1next1 - MaxMovementYNeg;
										dym1next2 = dym1next2 + MaxMovementYPos;
									} else {
										dym1next1 = dym1next1 + MaxMovementYNeg;
										dym1next2 = dym1next2 - MaxMovementYPos;
									}
									
									if (Math.abs(dxm1next2) < Math.abs(dxm1next1)) {
										dxm1next1 = dxm1next2;
									} 
									
									if (Math.abs(dym1next2) < Math.abs(dym1next1)) {
										dym1next1 = dym1next2;
									} 
								
									if ((Math.abs(dxm1next1) + Math.abs(dym1next1)) < (Math.abs(dx1) + Math.abs(dy1))){
										ignore = true;
									} 
								}
							
								int r1 = m1.getRad();
								int r4 = m4.getRad();	
							
								if (ignore != true && (dx1*dx1+dy1*dy1 <= r1*r1 || dx1*dx1+dy1*dy1 <= r4*r4) && (dx1*dx1+dy1*dy1 <= BestMatch2)) {
									BestMatch2 = dx1*dx1+dy1*dy1;
									BestMatchNum2 = m4;						
								}	
							}	
						} 
						
						ignore = false;
						temp1 = 0;
						temp2 = 0;
						temp3 = 0;
						temp4 = 0;
					}

					if (BestMatchNum != null || BestMatchNum2 != null) {	
						if ((BestMatchNum != null) && (BestMatchNum2 == null)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						} else if ((BestMatchNum == null) && (BestMatchNum2 != null)) {
							setGroupResultNum(BestMatchNum2, m1.resultNum); 
							GroupOwnerByResultNum(0);
						} else if ((BestMatchNum != null) && (BestMatchNum2 != null) && (BestMatchNum != BestMatchNum2) && (BestMatch < BestMatch2)) {
							setGroupResultNum(BestMatchNum, m1.resultNum);
							GroupOwnerByResultNum(0); 
						}	
					}
					
					BestMatch = 1000;
					BestMatch2 = 1000;
						
					currentZ = m1.getZ();
					IJ.showStatus("Processing autogroup in slice "+currentZ+"/"+activeImg.getStackSize());
				}
			}
		}	
	}	
	
	int manualGroupStep = 1;
	int lastCanvasClickedForGroupLink = -1;
	
	public void manualGroupMarker(PunctaCntrMarker m, int canvasID) { 	
		if (manualGroupStep == 1) {   
		   PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = PCMVA.get(n);
				if (!mA.isOwner()) continue;
				nextResultNum++;
			}	
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = PCMVB.get(n);
				if (!mB.isOwner()) continue;
				nextResultNum++;
			}	     	
         setGroupResultNum(m, nextResultNum); 
        	manualGroupStep = 2;
        	lastCanvasClickedForGroupLink = canvasID;
      } else if (manualGroupStep == 2 && canvasID == lastCanvasClickedForGroupLink) {
        	 setGroupResultNum(m, nextResultNum);
        	 nextResultNum++;
          manualGroupStep = 3;
      } else if (manualGroupStep == 3) {
         setGroupResultNum(m, nextResultNum); 
        	manualGroupStep = 4;
        	lastCanvasClickedForGroupLink = canvasID;
      } else if (manualGroupStep == 4 && canvasID == lastCanvasClickedForGroupLink) {
        	 setGroupResultNum(m, nextResultNum);
        	 nextResultNum++;
          manualGroupStep = 3;
      } else {
        	 IJ.log("Error: Grouping is only done for puncta on same canvas");
      }
      GroupOwnerByResultNum(0);
      GroupOwnerByResultNum(1);
    }
    
	public void GroupOwnerByResultNum(int v) {     
		if (v == 0) {
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
			   int resultNumOwn = 0;
				PunctaCntrMarker mOwn = (PunctaCntrMarker) PCMVA.get(n);
				if (!mOwn.isOwner()) continue;
				resultNumOwn = mOwn.resultNum;
				for (int n2 = 0; n2 < PCMVA.size(); n2++) {
				   int resultNumSubj = 0;
					PunctaCntrMarker mSubj = (PunctaCntrMarker) PCMVA.get(n2);
					resultNumSubj = mSubj.resultNum;
					if (resultNumSubj == resultNumOwn){
						mSubj.setOwner(mOwn.getOwner());
					}
				}
			}
		} else {
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);
			
			for (int n = 0; n < PCMVB.size(); n++) {
			   int resultNumOwn = 0;
				PunctaCntrMarker mOwn = (PunctaCntrMarker) PCMVB.get(n);
				if (!mOwn.isOwner()) continue;
				resultNumOwn = mOwn.resultNum;
				for (int n2 = 0; n2 < PCMVB.size(); n2++) {
			   	int resultNumSubj = 0;
					PunctaCntrMarker mSubj = (PunctaCntrMarker) PCMVB.get(n2);
					resultNumSubj = mSubj.resultNum;
					if (resultNumSubj == resultNumOwn){
						mSubj.setOwner(mOwn.getOwner());
					}
				}
			}
		}
	}
		
//-----------------------------GET PUNCTA MEASUREMENTS FOR 1 OR 2 STACKS   
	public void measure() {
		if (compareMode) { //This code measures puncta when working with two images; note: autolink already removes puncta in canvas 1 and 2 that are on edges of canvas boundaries
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (mA.isOwner()) {
					measureMarker3D(mA);
				}
			}
			
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
				if (mB.isOwner()) {
					measureMarker3D(mB);
				}
			}
			
         	relinkBasedOnResultNums();			
			
			//compare list of type 2 to list of type 1
			int nochange = 0;
			int gained2 = 0;
			int lost2 = 0;
			
         	for (int n2 = 0; n2 < PCMVB.size(); n2++)
           		PCMVB.get(n2).flags = 0;
			
			for (int n = 0; n < PCMVA.size(); n++) {
				boolean pairing_found = false;
				PunctaCntrMarker mA = (PunctaCntrMarker) PCMVA.get(n);
				if (mA.isOwner()) { 
					for (int n2 = 0; n2 < PCMVB.size() && !pairing_found; n2++) {
						PunctaCntrMarker mB = (PunctaCntrMarker) PCMVB.get(n2);
						if (!mB.isOwner()) continue;
						if (mB.resultNum == mA.resultNum) {
						   	mB.flags = 1; //signifies a B was used
							pairing_found = true;
							nochange++;
						}
					}
					if (!pairing_found) {
					  	lost2++;
					}
				}
			}
			
			for (int n2 = 0; n2 < PCMVB.size(); n2++) {
			  	PunctaCntrMarker mB = PCMVB.get(n2);
			  	if (mB.isOwner()) {
			    	if (mB.flags != 1) {
			      		gained2++;
			    	}
			  	}
           		PCMVB.get(n2).flags = 0;	//AFTER being checked, reset to 0
         	}
         	//end of gained/lost logic
         
         	IJ.log("No change = "+nochange+" Gained = "+gained2+" Lost = "+lost2);
			currentMarkerVector = startingCMV;
		} else { //This code measures puncta when working with one image
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);

			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);

			//start of renumbering puncta following any removal
			nextResultNum = 1;

			for (int n = 0; n < PCMVA.size(); n++) 
				((PunctaCntrMarker)PCMVA.get(n)).resultNum = 0;
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker m = PCMVA.get(n);
				if (!m.isOwner()) continue;
				m.resultNum = nextResultNum;
				nextResultNum++;
			}
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mOwn = (PunctaCntrMarker) PCMVA.get(n);
				if (!mOwn.isOwner()) continue;
				for (int n2 = 0; n2 < PCMVA.size(); n2++) {
					PunctaCntrMarker mSubj = (PunctaCntrMarker) PCMVA.get(n2);
					if (mSubj.getOwner() == mOwn.getOwner())
						mSubj.resultNum = mOwn.resultNum;
				}
			}
			//end of renumbering puncta following any removal

			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (mA.isOwner()) {
					measureMarker3D(mA);
				}
			}
		}
	}
	
	private void measureMarker3D(PunctaCntrMarker marker) {
		int uid = marker.getOwner();
		PunctaCntrMarker m;
		ListIterator it = currentMarkerVector.listIterator();
		
		if (!results_initialized) {
			rt.reset();
			results_initialized = true;
		}
		
		double group_intensity = 0;
		int group_pixels = 0;
		double group_stddev = 0;
		double group_max = 0;
		int group_x = 0;
		int group_y = 0;
		int group_z = 0;
		int zCount = 0;
		int resultNum = 0;
		int markerImage = 0;
		
		while (it.hasNext()) {
			m = (PunctaCntrMarker)it.next();
			if (m.getOwner() == uid) {
				if (m.isOwner()) {
					resultNum = m.resultNum;
					markerImage = m.canvasID;
				}
				int rad = m.getRad();
				int x = m.getX();
				int y = m.getY();
				int z = m.getZ();
			   	group_x += x;
				group_y += y;
				group_z += z;
				zCount++;
				ImageProcessor ip;
				if (activeImg.getStackSize() == 1) {
					ip = activeImg.getProcessor();
				} else {
					ImageStack stack = activeImg.getStack();
					ip = stack.getProcessor(z);
				}
				ip.resetRoi();
				ip = ip.crop();
				
				int[][] endpts = new int[rad*2+1][2];
				restrictBoundary(m, endpts, ip);
				double intensity = 0;
				int pixels = 0;
				double max = 0;
				int d2;
				double v;
				
				for (int j = -rad; j <= rad; j++) {
					if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
					for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
						v = ip.getPixelValue(x+i, y+j);
						intensity += v;
						if (v > max) max = v;
						pixels++;						
					}
				}
				
				double average = intensity/pixels;
				double stddev = 0;
				
				for (int j = -rad; j <= rad; j++) {
					if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
					for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
						v = ip.getPixelValue(x+i, y+j);
						stddev += ((v-average)*(v-average));
					}
				}
				
				stddev /= pixels;
				stddev = Math.sqrt(stddev);
				
				//The new stddev must be calculated BEFORE intensity and pixels are absorbed
				if (group_pixels == 0) {
					group_stddev = stddev;
				} else {
					int total_pixels = group_pixels + pixels;
					double newaverage = (group_intensity+intensity)/(group_pixels+pixels);
					group_stddev = ((double)pixels/(double)total_pixels)*(average*average+stddev*stddev)
						+ ((double)group_pixels/(double)total_pixels)*(group_intensity*group_intensity/(group_pixels*group_pixels)
						+ group_stddev*group_stddev) - newaverage*newaverage;
					group_stddev = Math.sqrt(group_stddev);
				}
				
				group_intensity += intensity;
				group_pixels += pixels;
				
				if (max > group_max) group_max = max;
			}
		}
		
		rt.incrementCounter();

		int z = (int)(group_z/zCount);
		double z2 = (int)(z);
		
		if (zCount % 2 == 0){
			z2 = z + 0.5;
		}
		
		rt.addValue("ID",resultNum);
		rt.addValue("Image",markerImage);
		rt.addValue("X", group_x/zCount);
		rt.addValue("Y", group_y/zCount);
		rt.addValue("Z", z2);
		rt.addValue("Max Intensity",group_max);
		rt.addValue("Average Intensity",group_intensity/group_pixels);
		rt.addValue("Stddev",group_stddev);
		rt.addValue("Volume in Pixels",group_pixels);														
		rt.addValue("Volume in Microns",group_pixels*(MicronsX/PixelsX*MicronsY/PixelsY*MicronsZ)); //Micronz Z already has unit um/slice
		rt.addValue("Background_Avg",bkgrnd_avg[z]);
		rt.addValue("Background_Stddev",bkgrnd_stddev[z]);		
		rt.addValue("Number of Slices",zCount);
		int changeCode = 0;
		if (marker.linkedOwner > -1) changeCode = 0;
		else if (marker.canvasID == 1) changeCode = -1;
		else changeCode = 1;
      	rt.addValue("Change",changeCode);
	}

	public void relinkBasedOnResultNums() {
     	//Note: This function does NOT call repaint by itself
     	
		PunctaCntrMarkerVector startingCMV = currentMarkerVector;
		PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
		PunctaCntrMarker m;
		PunctaCntrMarker m2;
		
		int owner1;
		int owner2;
		
		for (int n2 = 0; n2 < PCMVB.size(); n2++) {
		  m2 = PCMVB.get(n2);
		  m2.linkedOwner = -1;
		}
      	
      	for (int n = 0; n < PCMVA.size(); n++) {
        	m = PCMVA.get(n);
        	m.linkedOwner = -1;
        	owner1 = m.getOwner();
        	for (int n2 = 0; n2 < PCMVB.size(); n2++) {
          		m2 = PCMVB.get(n2);
          		if (m2.resultNum == m.resultNum && m2.resultNum > 0) {
            		owner2 = m2.getOwner();
            		//Both of these may be called redundantly several times,
            		//probably not really worth worrying about
            		m.linkedOwner = owner2;
            		m2.linkedOwner = owner1;
          		}
        	}
      	}
      	
		currentMarkerVector = startingCMV;
	}   
	
//-----------------------------ADD/REMOVE TYPE COUNTERS
	 public Vector getButtonVector() {
        return dynRadioVector;
    }
    
	 public void setButtonVector(Vector<JRadioButton> buttonVector) {
        this.dynRadioVector = buttonVector;
    }
    
    public void setCurrentMarkerVector(PunctaCntrMarkerVector currentMarkerVector) {
        this.currentMarkerVector = currentMarkerVector;
    }
    
    public static void setType(String type) {
		if (instance==null || instance.activeIC==null || type==null)
    		return;
    	int index = Integer.parseInt(type)-1;
    	int buttons = instance.dynRadioVector.size();
    	if (index<0 || index>=buttons)
    		return;
    	JRadioButton rbutton = (JRadioButton)instance.dynRadioVector.elementAt(index);
    	instance.radioGrp.setSelected(rbutton.getModel(), true);
		instance.currentMarkerVector = (PunctaCntrMarkerVector)instance.typeVector.get(index);
		instance.setCurrentMarkerVector(instance.currentMarkerVector);
    }
    
    public PunctaCntrMarkerVector getCurrentMarkerVector() {
        return currentMarkerVector;
    }

    
//-----------------------------RESET ALL NUMBERS
    public void reset(){
        if (typeVector.size()<1){
            return;
        }
        ListIterator mit = typeVector.listIterator();
        while (mit.hasNext()){
            PunctaCntrMarkerVector mv = (PunctaCntrMarkerVector)mit.next();
            mv.clear();
        }
        if (activeIC!=null)
            ;//activeIC.repaint();
    }

    public void setGroupResultNum(PunctaCntrMarker m, int newNum) {
      int ownerUID = m.getOwner();
      PunctaCntrMarker m2;
	   for (int n = 0; n < currentMarkerVector.size(); n++) {
	     m2 = currentMarkerVector.get(n);
	     if (m2.getOwner() == ownerUID) {
	       m2.resultNum = newNum;
	     }
	   }
    }
    
    public void setGroupLinkedOwner(PunctaCntrMarker m, int newLO) {
      int ownerUID = m.getOwner();
      PunctaCntrMarker m2;
	   for (int n = 0; n < currentMarkerVector.size(); n++) {
	     m2 = currentMarkerVector.get(n);
	     if (m2.getOwner() == ownerUID) {
	       m2.linkedOwner = newLO;
	     }
	   }
    }    
    
    public void unlinkMarker(PunctaCntrMarker m) {
      setGroupResultNum(m, -1);
	   setGroupLinkedOwner(m, -1);
    }   
    
    int manualLinkingStep = 1;
    int lastCanvasClickedForManualLink = -1;
    
    public void manualLinkMarker(PunctaCntrMarker m, int canvasClicked) {
      if (manualLinkingStep == 1) {
        setGroupResultNum(m, nextResultNum);
        nextResultNum++;
        manualLinkingStep = 2;
        lastCanvasClickedForManualLink = canvasClicked;
      } else { //manualLinkingStep must == 2 here
        if (canvasClicked == lastCanvasClickedForManualLink) {
          manualLinkingStep = 1;
        } else {
          setGroupResultNum(m, nextResultNum-1);
          manualLinkingStep = 1;
        }
      }
    }
}
