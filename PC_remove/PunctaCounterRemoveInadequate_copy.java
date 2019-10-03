
/* PUNCTA COUNTER REMOVE INADEQUATE
 *
 * Adapted from the Cell Counter plugin written by Kurt De Vos (2005)
 * by Vito Cairone for Dr. Michael Stryker's lab at the University of California San Francisco
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

public class PunctaCounterRemoveInadequate extends JFrame implements ActionListener, ItemListener {

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
    public PunctaCntrImageCanvasRemoveInadequate activeIC;
	 private PunctaCntrImageCanvasRemoveInadequate ic1;
	 private PunctaCntrImageCanvasRemoveInadequate ic2;
	 private int nextCanvasID = 1;
    
    private boolean isJava14;
    
    static PunctaCounterRemoveInadequate instance;
	
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
	 //bkgrnd_initialized is 0 for non-allocated, 1 for allocated, 2 for set
	 public int bkgrnd_initialized = 0;
	 public PunctaCntrImageCanvasRemoveInadequate compareTo = null;
	 //public boolean arbitraryBoundaries = true;

	//input parameters
	public int BackgroundGridSize;										//Grid size used to calculate background levels
	//public int BackgroundGridSizeAutoDetect=16;		
	public int PixelsX,PixelsY,PixelsImg1Z,PixelsImg2Z;            						//Image pixels
	public float MicronsX,MicronsY,MicronsZ;       						//Image micrometers
 	public float MinRingRatio;	 										//Minimum ratio for finding ring
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

    GenericDialog gd; 
       
    public PunctaCounterRemoveInadequate(){
        super("Puncta Counter");

        isJava14 = IJ.isJava14(); 
        if(!isJava14){
            IJ.showMessage("You are using a pre 1.4 version of java, exporting and loading marker data is disabled");
        }
        setResizable(false);
        
        //Autorun code starts here
		  MicronsX = 200;
		  MicronsY = 200;
		  MicronsZ = 1; //number of microns per slice 
		  
		  MinRingRatio = 2;
        ArbitraryLocalBoundaryCutoff = 10;
        RemoveSurface = 0;

		  Shiftx = 0;
		  Shifty = 0;
		  Shiftz = 0;
		  
			PSD95orSynt = 1;			
			if (PSD95orSynt == 1){        
				xDensityTol = 20; //fine because it is being compared to a value converted into microns 
				yDensityTol = 20; //fine because it is being compared to a value converted into microns 
				zDensityTol = 2;  //fine because it is being compared to a value converted into microns 
				PunctaNeighborDensityCutoff = 5; //this is fine because it is count of puncta within tolerance range 
			} else {
		      xDensityTol = 100; //fine because it is being compared to a value converted into microns 
				yDensityTol = 100; //fine because it is being compared to a value converted into microns 
				zDensityTol = 4;   //fine because it is being compared to a value converted into microns 
				PunctaNeighborDensityCutoff = 5; //this is fine because it is count of puncta within tolerance range
			}		 

			RemovePixels = 1; 
			RemoveMaxPunctaSizeinZ = 1;
			RemoveDensity = 1;
			RemoveZCount = 0;	
		
        typeVector = new Vector<PunctaCntrMarkerVector>();
        markerVector = new PunctaCntrMarkerVector(1);
        typeVector.add(markerVector);
        initializeImage();
        currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(0);
        String filePath = "/nas/data/sebastian/structural/punctacounter/xml1/"+activeImg.getTitle()+".xml";
        loadMarkers2(filePath);
        removeinadequate();
        measure();    
        String filePath2 = "/nas/data/sebastian/structural/punctacounter/xml2/"+activeImg.getTitle()+"_2.xml";
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
		activeIC = new PunctaCntrImageCanvasRemoveInadequate(activeImg,this,nextCanvasID,displayList);
		if (nextCanvasID == 1) {
			img1 = activeImg;
			ic1 = activeIC;
		   PixelsX = img1.getWidth();
		   PixelsY = img1.getHeight();
		   PixelsImg1Z = img1.getStackSize();
		   
			BackgroundGridSize = (int)Math.round(Math.sqrt(PixelsX*PixelsY))/64; //sampling is 3.125 microns per pixel, equals 16 for 1024 pixel, 1x 200 micron image
		   MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));
         MaxPunctaSizeinX = (int)Math.round(3*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY)))); //3 microns or 15 pixels for 1x 200x200 micron 1024x1024 pixel image 
         MaxPunctaSizeinY = (int)Math.round(3*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));
         MaxPunctaSizeinZ = (int)Math.round(20*MicronsZ); //20 microns 
       	   
			compareMode = false;

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
			
		} else {
		   //Can compare img2.getWidth() and img2.getHeight() and produce error
		   //if no match
			img2 = activeImg;
			ic2 = activeIC;
		   int x2 = img2.getWidth();
		   int y2 = img2.getHeight();
		   PixelsImg2Z = img2.getStackSize();
		   if (x2 != PixelsX || y2 != PixelsY) { 
		     IJ.error("Images must be same size");
		   }
			compareMode = true;
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
					;//ic1.repaint();
					;//ic2.repaint();
		  		} else {
		  		  ;//activeIC.repaint();
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
            /*
            while(dynRadioVector.size()>typeVector.size()){
                if (dynRadioVector.size() > 1){
                    JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                    dynButtonPanel.remove(rbutton);
                    radioGrp.remove(rbutton);
                    dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                    dynGrid.setRows(dynRadioVector.size());
                };
                if (txtFieldVector.size() > 1){
                    JTextField field = (JTextField)txtFieldVector.lastElement();
                    dynTxtPanel.remove(field);
                    txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
                }
            }*/
            //JRadioButton butt = (JRadioButton)(dynRadioVector.get(index));
            //butt.setSelected(true);
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
		double minrat = MinRingRatio;  //critical parameter

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
			if (rat < prev_rat && prev_rat > minrat) {
			  best_rng = i;
			  best_rat = prev_rat;
			  rad_found = true;
			} else prev_rat = rat;
			inr_v += out_v;
			inr_c += out_c;
		}
		if (best_rat > minrat) {
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
					if (rat < prev_rat && prev_rat > minrat)
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
	
//-----------------------------CRITERIA FOR SELECTING 3-D PUNCTA - Groups together 2D circles into a 3D sphere, removes groups where z=1 or z>4
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
	
//-----------------------------Remove Markers on Boundaries or that are too small or too big
	public void removeinadequate() {
		if (compareMode) { //This code removes puncta when working with two images
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (mA.isOwner()) {
					int uid = mA.getOwner();
					int group_pixels = 0;
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;

					int PSD95NeighboringDensity = 0;
					int SyntNeighboringDensity = 0;

					if (PSD95NeighboringDensity == 0 && SyntNeighboringDensity == 0) {
						for (int nA = 0; nA < PCMVA.size(); nA++) { 
							PunctaCntrMarker Neighbor = PCMVA.get(nA);
							
							if (Neighbor.isOwner()){
								int xNeighbor = Neighbor.getX();
								int yNeighbor = Neighbor.getY();
								int zNeighbor = Neighbor.getZ();
					
								int xmA = mA.getX();
								int ymA = mA.getY();
								int zmA = mA.getZ();
					
								float dx = (xmA - xNeighbor)*(MicronsX/PixelsX);	//in microns
								float dy = (ymA - yNeighbor)*(MicronsY/PixelsY);	//in microns
								float dz = (zmA - zNeighbor)*(MicronsZ);			//in microns	
								
								IJ.showStatus("Processing remove inadequate density in image one in slice "+zmA+"/"+activeImg.getStackSize());
					
								if (PSD95orSynt == 1 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									PSD95NeighboringDensity++;
								}
								if (PSD95orSynt == 2 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									SyntNeighboringDensity++;
								}
							}
						}
					}
			
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
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
							
							IJ.showStatus("Processing remove inadequate in image one in slice "+z+"/"+activeImg.getStackSize());
				
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
				
							int pixels = 0;
							double max = 0;
							double v;
				
							for (int j = -rad; j <= rad; j++) {
								if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
								for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
									v = ip.getPixelValue(x+i, y+j);
									if (v > max) max = v;
									pixels++;						
								}
							}
	
							group_pixels += pixels;
						}	
					}

					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}
					
					//removes punctum that are on top edge or bottom edge or that do not overlap with second canvas or that are too small or too big
					//is z-shiftz < 1 correct? used to be zA
					if (z <= RemoveSurface || (z2 - MaxPunctaSizeinZ/2) < 1 || (z - Group_Shiftz) < 1 || (z2 - Group_Shiftz) < MaxPunctaSizeinZ/2 || (z2 + MaxPunctaSizeinZ/2) > PixelsImg1Z || (z - Group_Shiftz) > PixelsImg1Z || (z2 + MaxPunctaSizeinZ/2 - Group_Shiftz) > PixelsImg1Z) {
  						PCMVA.removeMarker(n);
						n--;
		 				//IJ.log("removesurface="+RemoveSurface+" ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shiftz="+Shiftz+" remove_z");
					} else if ((group_x/zCount + Group_Shiftx - MaxPunctaSizeinX/2) < 0 || (group_x/zCount + Group_Shiftx + MaxPunctaSizeinX/2) > PixelsX) {
						PCMVA.removeMarker(n);
						n--;
	  					//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shiftx="+Shiftx+" remove_x");
	  			 	} else if ((group_y/zCount + Group_Shifty - MaxPunctaSizeinX/2) < 0  || (group_y/zCount + Group_Shifty + MaxPunctaSizeinY/2) > PixelsY) {
						PCMVA.removeMarker(n);
						n--;
					 	//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shifty="+Shifty+" remove_y");
					} else if (RemovePixels == 1 && group_pixels < 15) {  //this would break down for higher pixel/micron images (e.g., 4x at 512) 
					 	PCMVA.removeMarker(n);
						n--;
					  	//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" group_pixelsA="+group_pixels+" remove_pixels");
					} else if (RemoveMaxPunctaSizeinZ == 1 && zCount > MaxPunctaSizeinZ) { //critical parameter
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" remove_MaxPunctaSize");
					} else if (RemoveDensity == 1 && PSD95orSynt == 1 && PSD95NeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+"PunctaNeighboringDensity="+PunctaNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveDensity == 1 && PSD95orSynt == 2 && SyntNeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+"PunctaNeighboringDensity="+SyntNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveZCount == 1 && zCount == 1) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" remove_zcount=1");
					} else { 
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" PunctaNeighboringDensity="+PunctaNeighboringDensity+" group_pixels="+group_pixels+" safe"); 
					} 
				}
			}
			
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
			
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);

			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
				if (mB.isOwner()) {
					int uid = mB.getOwner();
					int group_pixels = 0;
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;

					int PSD95NeighboringDensity = 0;
					int SyntNeighboringDensity = 0;
					
					if (PSD95NeighboringDensity == 0 && SyntNeighboringDensity == 0) {
						for (int nB = 0; nB < PCMVB.size(); nB++) { 
							PunctaCntrMarker Neighbor = PCMVB.get(nB);
							
							if (Neighbor.isOwner()){
								int xNeighbor = Neighbor.getX();
								int yNeighbor = Neighbor.getY();
								int zNeighbor = Neighbor.getZ();
						
								int xmB = mB.getX();
								int ymB = mB.getY();
								int zmB = mB.getZ();
					
								float dx = (xmB - xNeighbor)*(MicronsX/PixelsX);	//in microns
								float dy = (ymB - yNeighbor)*(MicronsY/PixelsY);	//in microns
								float dz = (zmB - zNeighbor)*(MicronsZ);			//in microns	
								
								IJ.showStatus("Processing density in image two in slice "+zmB+"/"+activeImg.getStackSize());
					
								if (PSD95orSynt == 1 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									PSD95NeighboringDensity++;
								}
								if (PSD95orSynt == 2 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									SyntNeighboringDensity++;
								}
							}
						}
					}
					
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
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
							
							IJ.showStatus("Processing remove inadequate image two in slice "+z+"/"+activeImg.getStackSize());
				
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
				
							int pixels = 0;
							double max = 0;
							double v;
				
							for (int j = -rad; j <= rad; j++) {
								if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
								for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
									v = ip.getPixelValue(x+i, y+j);
									if (v > max) max = v;
									pixels++;						
								}
							}
	
							group_pixels += pixels;
						}	
					}
					
					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}
					
					//removes punctum that are on top edge or bottom edge or that do not overlap with first canvas or that are too small or too big
					if (z <= (RemoveSurface + Group_Shiftz) || (z2 - MaxPunctaSizeinZ/2) < 1 || (z + Group_Shiftz) < 1 || (z2 + Group_Shiftz) < MaxPunctaSizeinZ/2 || (z2 + MaxPunctaSizeinZ/2) > PixelsImg2Z || (z + Group_Shiftz) > PixelsImg2Z || (z2 + MaxPunctaSizeinZ/2 + Group_Shiftz) > PixelsImg2Z) {
  						PCMVB.removeMarker(n);
  						n--;
		 				//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mA_z="+z2+" mB_groupzcount="+zCount+" Shiftz="+Shiftz+" remove_z");
					} else if ((group_x/zCount - Group_Shiftx - MaxPunctaSizeinX/2) < 0 || (group_x/zCount - Group_Shiftx + MaxPunctaSizeinX/2) > PixelsX) {
						PCMVB.removeMarker(n);
						n--;
	  					//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+" mB_groupzcount="+zCount+" Shiftx="+Shiftx+" remove_x");
	   			} else if ((group_y/zCount - Group_Shifty - MaxPunctaSizeinX/2) < 0 || (group_y/zCount - Group_Shifty + MaxPunctaSizeinY/2) > PixelsY) {
						PCMVB.removeMarker(n);
						n--;
					 	//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+" mB_groupzcount="+zCount+" Shifty="+Shifty+" remove_y");
					} else if (RemovePixels == 1 && group_pixels < 15) {
					 	PCMVB.removeMarker(n);
						n--;
					  	//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+" group_pixelsB="+group_pixels+" remove_pixels");
					} else if (RemoveMaxPunctaSizeinZ == 1 && zCount > MaxPunctaSizeinZ) { //critical parameter
						PCMVB.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+" remove_MaxPunctaSize");
					} else if (RemoveDensity == 1 && PSD95orSynt == 1 && PSD95NeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVB.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+"PunctaNeighboringDensity="+PunctaNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveDensity == 1 && PSD95orSynt == 2 && SyntNeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVB.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+"PunctaNeighboringDensity="+PunctaNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveZCount == 1 && zCount == 1) {
						PCMVB.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mB_z="+z2+" mB_groupzcount="+zCount+" remove_zcount=1");
					} else { 
						//IJ.log("ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z+" mA_z="+z2+" mB_groupzcount="+zCount+" group_pixels="+group_pixels+" safe"); 
					} 
				}
			}
			
			//start of renumbering puncta following any removal
			nextResultNum = 1;

			for (int n = 0; n < PCMVB.size(); n++) 
				((PunctaCntrMarker)PCMVB.get(n)).resultNum = 0;
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker m = PCMVB.get(n);
				if (!m.isOwner()) continue;
				m.resultNum = nextResultNum;
				nextResultNum++;
			}
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mOwn = (PunctaCntrMarker) PCMVB.get(n);
				if (!mOwn.isOwner()) continue;
				for (int n2 = 0; n2 < PCMVB.size(); n2++) {
					PunctaCntrMarker mSubj = (PunctaCntrMarker) PCMVB.get(n2);
					if (mSubj.getOwner() == mOwn.getOwner())
						mSubj.resultNum = mOwn.resultNum;
				}
			}
			//end of renumbering puncta following any removal
			
			currentMarkerVector = startingCMV;
			
		} else { //This code removes puncta when working with one image
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);

			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (mA.isOwner()) {
					int uid = mA.getOwner();
					int group_pixels = 0;
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;

					int PSD95NeighboringDensity = 0;
					int SyntNeighboringDensity = 0;

					if (PSD95NeighboringDensity == 0 && SyntNeighboringDensity == 0) {
						for (int nA = 0; nA < PCMVA.size(); nA++) { 
							PunctaCntrMarker Neighbor = PCMVA.get(nA);
							
							if (Neighbor.isOwner()){
								int xNeighbor = Neighbor.getX();
								int yNeighbor = Neighbor.getY();
								int zNeighbor = Neighbor.getZ();
					
								int xmA = mA.getX();
								int ymA = mA.getY();
								int zmA = mA.getZ();
					
								float dx = (xmA - xNeighbor)*(MicronsX/PixelsX);	//in microns
								float dy = (ymA - yNeighbor)*(MicronsY/PixelsY);	//in microns
								float dz = (zmA - zNeighbor)*(MicronsZ);			//in microns	
								
								IJ.showStatus("Processing remove inadequate density in slice "+zmA+"/"+activeImg.getStackSize());
					
								if (PSD95orSynt == 1 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									PSD95NeighboringDensity++;
									//IJ.log("ID= "+resultNum+"dx= "+dx+"dy= "+dy+"dz= "+dz+"punctaneighbordensity= "+PunctaNeighboringDensity);
								}
								if (PSD95orSynt == 2 && Math.abs(dx) <= xDensityTol && Math.abs(dy) <= yDensityTol && Math.abs(dz) <= zDensityTol) { //has to meet the minimum distance criteria
									SyntNeighboringDensity++;
									//IJ.log("ID= "+resultNum+"dx= "+dx+"dy= "+dy+"dz= "+dz+"punctaneighbordensity= "+PunctaNeighboringDensity);
								}
							}
						}
					}
			
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
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
							
							IJ.showStatus("Processing remove inadequate in slice "+z+"/"+activeImg.getStackSize());
				
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
				
							int pixels = 0;
							double max = 0;
							double v;
				
							for (int j = -rad; j <= rad; j++) {
								if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
								for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
									v = ip.getPixelValue(x+i, y+j);
									if (v > max) max = v;
									pixels++;						
								}
							}
	
							group_pixels += pixels;
						}	
					}

					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}

					//removes punctum that are on top edge or bottom edge or that are too small or too big, shift values are irrelevant b/c they should be set to zero.
					//is z-shiftz < 1 correct? used to be zA
					if (z <= RemoveSurface || (z2 - MaxPunctaSizeinZ/2) < 1 || (z - Group_Shiftz) < 1 || (z2 - Group_Shiftz) < MaxPunctaSizeinZ/2 || (z2 + MaxPunctaSizeinZ/2) > PixelsImg1Z || (z - Group_Shiftz) > PixelsImg1Z || (z2 + MaxPunctaSizeinZ/2 - Group_Shiftz) > PixelsImg1Z) {
  						PCMVA.removeMarker(n);
						n--;
		 				//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shiftz="+Shiftz+" remove_z");
					} else if ((group_x/zCount - Group_Shiftx) < MaxPunctaSizeinX/2 || (group_x/zCount - Group_Shiftx + MaxPunctaSizeinX/2) > PixelsX) {
						PCMVA.removeMarker(n);
						n--;
	  					//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shiftx="+Shiftx+" remove_x");
	  			 	} else if ((group_y/zCount - Group_Shifty) < MaxPunctaSizeinY/2 || (group_y/zCount - Group_Shifty + MaxPunctaSizeinY/2) > PixelsY) {
						PCMVA.removeMarker(n);
						n--;
					 	//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" Shifty="+Shifty+" remove_y");
					} else if (RemovePixels == 1 && group_pixels < 15) {
					 	PCMVA.removeMarker(n);
						n--;
					  	//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" group_pixelsA="+group_pixels+" remove_pixels");
					} else if (RemoveMaxPunctaSizeinZ == 1 && zCount > MaxPunctaSizeinZ) { //critical parameter
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" remove_MaxPunctaSize");
					} else if (RemoveDensity == 1 && PSD95orSynt == 1 && PSD95NeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+"PunctaNeighboringDensity="+PunctaNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveDensity == 1 && PSD95orSynt == 2 && SyntNeighboringDensity <= PunctaNeighborDensityCutoff) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+"PunctaNeighboringDensity="+PunctaNeighboringDensity+" remove_IsolatedPuncta");
					} else if (RemoveZCount == 1 && zCount == 1) {
						PCMVA.removeMarker(n);
						n--;
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" remove_zcount=1");
					} else { 
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+" mA_groupzcount="+zCount+" group_pixels="+group_pixels+" safe"); 
					} 
				}
			}

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
    
	 public void removeLastMarker(){
        currentMarkerVector.removeLastMarker();
        activeIC.repaint();
        populateTxtFields();
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
            activeIC.repaint();
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
