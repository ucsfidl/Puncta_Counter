/* PUNCTA COUNTER AUTOLINK
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
import java.util.Arrays;
import java.util.Properties;
//import.java.util.ArrayList;
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
import ij.io.OpenDialog;
import java.io.File;
import java.io.IOException;
import ij.io.SaveDialog;
import java.lang.*;

public class PunctaCounterAutolink extends JFrame implements ActionListener, ItemListener {

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
    private static final String DELMODE = "Delete 3D Puncta";
    private static final String AUTODETECT = "Autodetect";
    private static final String REMOVEINADEQUATE = "Remove Inadequate";
    private static final String REMOVEEDGES = "Remove Edges";
    private static final String AUTOGROUP = "Autogroup";
    private static final String MANUALGROUPMODE = "Manual Group 3D Puncta";
	private static final String AUTOLINK = "Autolink";
	private static final String AUTOLINK2 = "Autolink2";
    private static final String UNLINKMODE = "Manual Unlink 3D Puncta";
    private static final String MANUALLINKMODE = "Manual Link 3D Puncta";
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
	private JCheckBox manualgroupCheck;
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
    private JButton autogroupButton;
    private JButton removeinadequateButton;
    private JButton removeedgesButton;
	private JButton autolinkButton;
	private JButton autolink2Button;
	private JButton measureButton;
	private JButton resultsButton;
    
	//ic was used by CellCounter and is effectively the 'active' image canvas
	//while the two seperate canvases for comparison mode are icA and icB
	//respectively
	public ImagePlus activeImg = null;
	public ImageWindow activeImg5 = null;
	private ImagePlus img1 = null;
	private ImagePlus img2 = null;
    public PunctaCntrImageCanvasAutolink activeIC;
	private PunctaCntrImageCanvasAutolink ic1;
	private PunctaCntrImageCanvasAutolink ic2;
	private int nextCanvasID = 1;
    
    private boolean isJava14;
    
    static PunctaCounterAutolink instance;
	
	public int nextResultNum = 1; 
	private boolean delmode = false;
	private boolean autolink2 = false;
	public boolean PixelZero = false;
	public boolean CellBody = false;	
	public boolean unlinkmode = false;
	public boolean manuallinkmode = false;
	public boolean manualgroupmode = false;
    private boolean showNumbers = true;
	private boolean results_initialized = false;
	ResultsTable rt = ResultsTable.getResultsTable();
	public int[] wList = null;
	public boolean compareMode = true;
	public boolean measureImg2 = false;
	public double[] bkgrnd_avg;
	public double[] bkgrnd_stddev;
	public double[] bkgrnd_avg1;
	public double[] bkgrnd_stddev1;
	public int[] signal_picknumber1;
	public int[] signal_picknumber2;
	public double[] signal_signal1;
	public double[] signal_max1;
	public double[] signal_min1;
	public double[] signal_median1;
	public double[] signal_avg1;
	public double[] signal_stddev1;
	public double[] signalAroundPunctum_avg1;
	public double[] signalAroundPunctum_stddev1;
	public double[] bkgrnd_avg2;
	public double[] bkgrnd_stddev2;
	public double[] signal_signal2;
	public double[] signal_max2;
	public double[] signal_min2;
	public double[] signal_median2;
	public double[] signal_avg2;
	public double[] signal_stddev2;
	public double[] signalAroundPunctum_avg2;
	public double[] signalAroundPunctum_stddev2;
	
	//bkgrnd_initialized is 0 for non-allocated, 1 for allocated, 2 for set
	public int bkgrnd_initialized = 0;
	public PunctaCntrImageCanvasAutolink compareTo = null;

	//input parameters
	public int BackgroundGridSize;										//Grid size used to calculate background levels
	public int BackgroundGridSizeAutoDetect;		
	public int PixelsX,PixelsY,PixelsImg1Z,PixelsImg2Z;            						//Image pixels
	public float MicronsX,MicronsY,MicronsZ;       						//Image micrometers
 	public double MinRingRatio;	 										//Minimum ratio for finding ring
	public int MaxRadius;												//Largest allowabe ring radius
	public int LinkedXML, Blur, DayXML1, DayXML2,GorRXML, ManualXML, LoadXML1, LoadXML2;
	public String DayXML1S, DayXML2S;
	public int manualLinkingStep = 1;
   public int lastCanvasClickedForManualLink = -1;
	public float ArbitraryLocalBoundaryCutoff;							//Arbitrary boundary cutoff as a function of bkgrnd_stddev 
	public int RemoveSurface, RemoveBottom;														//Remove surface and bottom slices from stack	
	public int RemoveLeftEdge, RemoveRightEdge, RemoveBottomEdge, RemoveTopEdge;
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
       
    public PunctaCounterAutolink(){
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
		  MinRingRatio = 2; // NoteForUser: This is used for relink, suggest to keep the same as in autodetect. 
          ArbitraryLocalBoundaryCutoff = 10; // NoteForUser: This is used for relink, suggest to keep the same as in autodetect.
          PSD95orSynt = 1;  // NoteForUser: Please indicate synaptic structure size here: PSD95 (1) or Synt(2)          
		  
			
		  ximg1 = 0;
		  yimg1 = 0;
		  zimg1 = 0;
		  ximg2 = 0;
		  yimg2 = 0;
		  zimg2 = 0;	

		  Shiftx = ximg2 - ximg1;
		  Shifty = yimg2 - yimg1;
		  Shiftz = zimg2 - zimg1;

		  xLinkTol = 2;
		  yLinkTol = 2;
		  zLinkTol = 2;	
		  
		  Blur = 0;
		  LinkedXML = 0;	
		  
        typeVector = new Vector<PunctaCntrMarkerVector>();
        markerVector = new PunctaCntrMarkerVector(1);
        typeVector.add(markerVector);
        markerVector = new PunctaCntrMarkerVector(2);
        typeVector.add(markerVector);

        //IJ.showStatus("Initializing image 1");
        initializeImage();
	    int ind1 = img1.getTitle().indexOf("Day");
	    int ind2 = img1.getTitle().indexOf('x');
	    DayXML1S = img1.getTitle().substring(ind1+3,ind2-2);
        IJ.showStatus("image Day"+DayXML1S);


        initializeImage();
        int ind3 = img2.getTitle().indexOf("Day");
	    int ind4 = img2.getTitle().indexOf('x');
	    DayXML2S =img2.getTitle().substring(ind3+3,ind4-2);
        IJ.showStatus("image Day"+DayXML2S);


        IJ.showStatus("Loading markers image Day"+DayXML1S);        
        currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(0); 
        String filePath1 = myDirectory+img1.getTitle()+"_3.xml";


	IJ.showStatus("Loading markers image Day"+DayXML2S);
        currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(1);
	String filePath2 = myDirectory+img2.getTitle()+"_3.xml"; 
	loadMarkers2(filePath1, filePath2);

	IJ.showStatus("Autolink Day"+DayXML1S+"Day"+DayXML2S);
        autolink();
       IJ.showStatus("Autolink2 Day"+DayXML1S+"Day"+DayXML2S);
       autolink2();
	IJ.showStatus("Measure Day"+DayXML1S+"Day"+DayXML2S);
        measure();  
        IJ.showStatus("Exporting markers Day"+DayXML1S+"Day"+DayXML2S);
       String filePath3 = myDirectory+"Day"+DayXML1S+"Day"+DayXML2S+fileSeparator+img1.getTitle()+"_Day"+DayXML2S+".xml";

       exportMarkers2(filePath3,1);
       String filePath4 = myDirectory+"Day"+DayXML1S+"Day"+DayXML2S+fileSeparator+img2.getTitle()+"_Day"+DayXML1S+".xml";

        exportMarkers2(filePath4,2);
        IJ.showStatus("Report Day"+DayXML1S+"Day"+DayXML2S);
        report();
        //End autorun code
    }
    
    /* Show the GUI threadsafe */
    private static class GUIShower implements Runnable {
        final JFrame jFrame;
        public GUIShower(JFrame jFrame) {
            this.jFrame = jFrame;
        }
        public void run() {
            jFrame.pack();
            jFrame.setLocation(1000, 200);
            jFrame.setVisible(true);
        }
    }
    
//-----------------------------CREATES PANELS FOR COUNTER AND ACTION BUTTONS
	private void createPunctaCounterGUI(){
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GridBagLayout gb = new GridBagLayout();
        getContentPane().setLayout(gb);
        
        radioGrp = new ButtonGroup(); //to group the radiobuttons
        
        dynGrid = new GridLayout(5,1);
        dynGrid.setVgap(2);
        
		//this panel will keep the dynamic GUI parts
        dynPanel = new JPanel();
        dynPanel.setBorder(BorderFactory.createTitledBorder("Counter"));
        dynPanel.setLayout(gb);
        
		//CREATE A PANEL FOR RADIO BUTTONS
        dynButtonPanel = new JPanel();
        dynButtonPanel.setLayout(dynGrid);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynButtonPanel,gbc);
        dynPanel.add(dynButtonPanel);
        
		//CREATE A PANEL THAT KEEPS COUNT
        dynTxtPanel = new JPanel();
        dynTxtPanel.setLayout(dynGrid);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx=5;
        gb.setConstraints(dynTxtPanel,gbc);
        dynPanel.add(dynTxtPanel);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.ipadx=5;
        gb.setConstraints(dynPanel,gbc);
        getContentPane().add(dynPanel);
        
        dynButtonPanel.add(makeDynRadioButton(1));
        dynButtonPanel.add(makeDynRadioButton(2));
        dynButtonPanel.add(makeDynRadioButton(3));
        dynButtonPanel.add(makeDynRadioButton(4));
        dynButtonPanel.add(makeDynRadioButton(5));
      
		//CREATE A "STATIC" PANEL FOR ACTION BUTTONS 
        statButtonPanel = new JPanel();
        statButtonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        statButtonPanel.setLayout(gb);
                     
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        initializeButton = makeButton(INITIALIZE, "Initialize image to count");
        gb.setConstraints(initializeButton,gbc);
        statButtonPanel.add(initializeButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
		gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        loadButton = makeButton(LOADMARKERS, "Load markers from file");
        loadButton.setEnabled(false);
        gb.setConstraints(loadButton,gbc);
        statButtonPanel.add(loadButton);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        exportButton = makeButton(EXPORTMARKERS, "Save markers to file");
        exportButton.setEnabled(false);
        gb.setConstraints(exportButton,gbc);
        statButtonPanel.add(exportButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        delCheck = new JCheckBox(DELMODE);
        delCheck.setToolTipText("When selected\nclick on the marker\nyou want to remove");
        delCheck.setSelected(false);
        delCheck.addItemListener(this);
        delCheck.setEnabled(false);
        gb.setConstraints(delCheck,gbc);
        statButtonPanel.add(delCheck);
      
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        autodetectButton = makeButton(AUTODETECT, "Automatically detect puncta in stack(s)");
        autodetectButton.setEnabled(false);
        gb.setConstraints(autodetectButton,gbc);
        statButtonPanel.add(autodetectButton);

		  gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        removeinadequateButton = makeButton(REMOVEINADEQUATE, "Remove inadequate puncta in stack(s)");
        removeinadequateButton.setEnabled(false);
        gb.setConstraints(removeinadequateButton,gbc);
        statButtonPanel.add(removeinadequateButton);
        
                
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        autogroupButton = makeButton(AUTOGROUP, "Automatically group puncta in stack(s)");
        autogroupButton.setEnabled(false);
        gb.setConstraints(autogroupButton,gbc);
        statButtonPanel.add(autogroupButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        manualgroupCheck = new JCheckBox(MANUALGROUPMODE);
        manualgroupCheck.setToolTipText("When selected\nclick on the markers\nyou want to group");
        manualgroupCheck.setSelected(false);
        manualgroupCheck.addItemListener(this);
        manualgroupCheck.setEnabled(false);
        gb.setConstraints(manualgroupCheck,gbc);
        statButtonPanel.add(manualgroupCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        removeedgesButton = makeButton(REMOVEEDGES, "Remove edges of stack(s)");
        removeedgesButton.setEnabled(false);
        gb.setConstraints(removeedgesButton,gbc);
        statButtonPanel.add(removeedgesButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        autolinkButton = makeButton(AUTOLINK, "Automatically link puncta across two stacks");
        autolinkButton.setEnabled(false);
        gb.setConstraints(autolinkButton,gbc);
        statButtonPanel.add(autolinkButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        autolink2Button = makeButton(AUTOLINK2, "Add linked puncta across two stacks");
        autolink2Button.setEnabled(false);
        gb.setConstraints(autolink2Button,gbc);
        statButtonPanel.add(autolink2Button);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        unlinkCheck = new JCheckBox(UNLINKMODE);
        unlinkCheck.setToolTipText("When selected\nclick on the markers\nyou want to unlink");
        unlinkCheck.setSelected(false);
        unlinkCheck.addItemListener(this);
        unlinkCheck.setEnabled(false);
        gb.setConstraints(unlinkCheck,gbc);
        statButtonPanel.add(unlinkCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        manuallinkCheck = new JCheckBox(MANUALLINKMODE);
        manuallinkCheck.setToolTipText("When selected\nclick on the markers\nyou want to link\ndouble click same image for no linking");
        manuallinkCheck.setSelected(false);
        manuallinkCheck.addItemListener(this);
        manuallinkCheck.setEnabled(false);
        gb.setConstraints(manuallinkCheck,gbc);
        statButtonPanel.add(manuallinkCheck);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        measureButton = makeButton(MEASURE, "Measure puncta in stack(s)");
        measureButton.setEnabled(false);
        gb.setConstraints(measureButton,gbc);
        statButtonPanel.add(measureButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        resultsButton = makeButton(RESULTS, "Show results table");
        resultsButton.setEnabled(false);
        gb.setConstraints(resultsButton,gbc);
        statButtonPanel.add(resultsButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        addButton = makeButton(ADD, "Add a counter type");
        gb.setConstraints(addButton,gbc);
        statButtonPanel.add(addButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        removeButton = makeButton(REMOVE, "Remove last counter type");
        gb.setConstraints(removeButton,gbc);
        statButtonPanel.add(removeButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(3,0,3,0);
        separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setPreferredSize(new Dimension(1,1));
        gb.setConstraints(separator,gbc);
        statButtonPanel.add(separator);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx=0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        resetButton=makeButton(RESET, "Reset all counters");
        resetButton.setEnabled(false);
        gb.setConstraints(resetButton,gbc);
        statButtonPanel.add(resetButton);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.ipadx=5;
        gb.setConstraints(statButtonPanel,gbc);
        getContentPane().add(statButtonPanel);  
      
        Runnable runner = new GUIShower(this);
        EventQueue.invokeLater(runner);
    }

	private void createImageSizeGUI(){ // create the second GUI for user to input parameters 
        GenericDialog gd = new GenericDialog("Image Size");
		  
		gd.addNumericField("X (microns per x-line):", 200,2);
		gd.addNumericField("Y (microns per y-line):", 200,2);
		gd.addNumericField("Z (microns per z-step):", 1,2);
		gd.addNumericField("Grid Size of Background Level (pixels):", 16,0);

		gd.showDialog();
		  
		MicronsX = (float)gd.getNextNumber();
		MicronsY = (float)gd.getNextNumber();
		MicronsZ = (float)gd.getNextNumber();
		BackgroundGridSize = (int)gd.getNextNumber();
	}

	private void createAutodetectGUI(){ // create the second GUI for user to input parameters 
      	GenericDialog gd = new GenericDialog("Puncta Detection Criteria");
      	
      	//8 pixels for 1024x1024 1x image, 8 for 512x512 2x image, 16 for 512x512 4x image, 16 for 1024x1024 2x image
      	BackgroundGridSizeAutoDetect = (int)(0.5*(Math.round(Math.sqrt(3.125*(PixelsX/MicronsX)*3.125*(PixelsY/MicronsY)))));
      	//10 pixels for 1024x1024 1x image, 10 for 512x512 2x image, 20 for 512x512 4x image, 20 for 1024x1024 2x image
      	MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));
      	
			gd.addNumericField("Grid Size for Puncta Detection (pixels):", BackgroundGridSizeAutoDetect,0);
        gd.addNumericField("Min Ring Ratio:", 2,2);
        gd.addNumericField("Max Ring Radius (pixels):", MaxRadius,0);
        gd.addNumericField("Arbitrary Boundary Cutoff:", 10,2);

		gd.showDialog();

		BackgroundGridSizeAutoDetect = (int)gd.getNextNumber();
		MinRingRatio = (double)gd.getNextNumber();
		MaxRadius = (int)gd.getNextNumber();
        ArbitraryLocalBoundaryCutoff = (float)gd.getNextNumber();
	 }
	 
	 private void createLoadMarkersGUI(){ // create the second GUI for user to input parameters 
      	GenericDialog gd = new GenericDialog("Load Markers Criteria");

         gd.addNumericField("Load Unlinked (0) or Linked (1) XML:", LinkedXML,0);
         gd.addNumericField("No blur (0) or Yes blur (1) XML:", Blur,0);
      	
		gd.showDialog();
		LinkedXML = (int)gd.getNextNumber();
		Blur = (int)gd.getNextNumber();
	 }

	 private void createLoadMarkersGUI2(){ // create the second GUI for user to input parameters 
      	GenericDialog gd = new GenericDialog("Load Markers Criteria");

      	MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));
         
         if (LinkedXML == 0) {
         	gd.addNumericField("Manual (0) or Auto (1) Load:", 1,0);
	         gd.addNumericField("Load Red (1) or Green (2) XML:", 2,0);
	         
	      	if (!compareMode) {
					gd.addNumericField("Load XML (0,1,2,3):", 3,0);      
				} else {
					gd.addNumericField("Load XML Img1 (0,1,2,3):", 3,0); 
					gd.addNumericField("Load XML Img2 (0,1,2,3):", 3,0);     
				}
			} else {
				gd.addNumericField("Manual (0) or Auto (1) Load:", 1,0);
				gd.addNumericField("Load Red (1) or Green (2) XML:", 2,0);
	         
				gd.addNumericField("Img1 Day:", 0,0); 
				gd.addNumericField("Img2 Day:", 3,0);     		
			}
		
        gd.addNumericField("Min Ring Ratio:", 2,2);
        gd.addNumericField("Max Ring Radius (pixels):", MaxRadius,0);
        
        if (Blur == 0) {
        		gd.addNumericField("Arbitrary Boundary Cutoff:", 10,2);
        } else {
        		gd.addNumericField("Arbitrary Boundary Cutoff:", 20,2);
        }

		gd.showDialog();
		
		ManualXML = (int)gd.getNextNumber();
		GorRXML = (int)gd.getNextNumber();
		
		if (LinkedXML == 0) {
			if (!compareMode) {
				LoadXML1 = (int)gd.getNextNumber();
			} else {
				LoadXML1 = (int)gd.getNextNumber();
				LoadXML2 = (int)gd.getNextNumber();
			}
		} else {
				DayXML1 = (int)gd.getNextNumber();
				DayXML2 = (int)gd.getNextNumber();
		}
		
		MinRingRatio = (double)gd.getNextNumber();
		MaxRadius = (int)gd.getNextNumber();
      ArbitraryLocalBoundaryCutoff = (float)gd.getNextNumber();
	 }
	 
	 private void createRemoveInadequateGUI(){ // create the second GUI for user to input parameters 
        GenericDialog gd = new GenericDialog("Remove Inadequate Criteria");
		 
		 if (PSD95orSynt > 0){
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", PSD95orSynt,0);
		} else {
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", 1,0);
		}       
		gd.showDialog();
		  
		 PSD95orSynt = (int)gd.getNextNumber();		
	}
	
	 private void createRemoveInadequateGUI2(){ // create the second GUI for user to input parameters 
        GenericDialog gd = new GenericDialog("Remove Inadequate Criteria");
		 
        gd.addNumericField("Remove Surface (pixels):", 0,0);
        
        if (ximg1 == 0 && yimg1 == 0 && zimg1 == 0){
        	gd.addNumericField("X coordinate in Image 1:", 0,0);
			gd.addNumericField("Y coordinate in Image 1:", 0,0);
			gd.addNumericField("Z coordinate in Image 1:", 0,0);
			gd.addNumericField("X coordinate in Image 2:", 0,0);
			gd.addNumericField("Y coordinate in Image 2:", 0,0);
			gd.addNumericField("Z coordinate in Image 2:", 0,0);
        } else {
        	gd.addNumericField("X coordinate in Image 1:", ximg1,0);
			gd.addNumericField("Y coordinate in Image 1:", yimg1,0);
			gd.addNumericField("Z coordinate in Image 1:", zimg1,0);
			gd.addNumericField("X coordinate in Image 2:", ximg2,0);
			gd.addNumericField("Y coordinate in Image 2:", yimg2,0);
			gd.addNumericField("Z coordinate in Image 2:", zimg2,0);
        }
			
			        
		gd.addNumericField("Max Puncta Size in X-plane (pixels):", 15,0);
        gd.addNumericField("Max Puncta Size in Y-plane (pixels):", 15,0);
        gd.addNumericField("Max Puncta Size in Z-plane (pixels):", 20,0);
        
		if (PSD95orSynt == 1){        
        gd.addNumericField("X Distance Tolerance for PSD95 Density Calculation (microns):", 20,0);
		gd.addNumericField("Y Distance Tolerance for PSD95 Density Calculation (microns):", 20,0);
		gd.addNumericField("Z Distance Tolerance for PSD95 Density Calculation (microns):", 2,0);
		gd.addNumericField("Puncta Neighbor Density Cutoff for PSD95:", 5,0);
		} else {
		 gd.addNumericField("X Distance Tolerance for Synt Density Calculation (microns):", 100,0);
		gd.addNumericField("Y Distance Tolerance for Synt Density Calculation (microns):", 100,0);
		gd.addNumericField("Z Distance Tolerance for Synt Density Calculation (microns):", 4,0);
		gd.addNumericField("Puncta Neighbor Density Cutoff for Synt:", 5,0);
		}
		gd.addNumericField("Remove Pixels:", 1,0);
		gd.addNumericField("Remove MaxPunctaSizeinZ:", 1,0);
		gd.addNumericField("Remove Density:", 1,0);
		gd.addNumericField("Remove zCount = 1:", 0,0);

		gd.showDialog();

        RemoveSurface = (int)gd.getNextNumber();

        ximg1 = (int)gd.getNextNumber();
		yimg1 = (int)gd.getNextNumber();
		zimg1 = (int)gd.getNextNumber();
		ximg2 = (int)gd.getNextNumber();
		yimg2 = (int)gd.getNextNumber();
		zimg2 = (int)gd.getNextNumber();
						
	 //if (ShiftEstimateCalculated == 0){
			Shiftx = ximg2 - ximg1;
			Shifty = yimg2 - yimg1;
			Shiftz = zimg2 - zimg1;
		//}	
		
        MaxPunctaSizeinX = (int)gd.getNextNumber();
        MaxPunctaSizeinY = (int)gd.getNextNumber();
        MaxPunctaSizeinZ = (int)gd.getNextNumber();
        
        xDensityTol = (int)gd.getNextNumber();
		yDensityTol = (int)gd.getNextNumber();
		zDensityTol = (int)gd.getNextNumber();
		PunctaNeighborDensityCutoff = (int)gd.getNextNumber();
		
		RemovePixels = (int)gd.getNextNumber();
		RemoveMaxPunctaSizeinZ = (int)gd.getNextNumber();
		RemoveDensity = (int)gd.getNextNumber();
		RemoveZCount = (int)gd.getNextNumber();		
	}
	
	private void createRemoveEdgesGUI(){ // create the second GUI for user to input parameters 
        GenericDialog gd = new GenericDialog("Remove Puncta on Edges Criteria");
		 
        gd.addNumericField("Remove Surface (pixels):", 0,0);
        
        if (compareMode) {
	        if (PixelsImg1Z >= PixelsImg2Z) {
	        	gd.addNumericField("Remove Bottom (pixels):", PixelsImg1Z,0);
	        } else {
	        	gd.addNumericField("Remove Bottom (pixels):", PixelsImg2Z,0);
	        }
        } else {
        		gd.addNumericField("Remove Bottom (pixels):", PixelsImg1Z,0);
        }
        
        gd.addNumericField("Remove Left Edge (pixels):", 0,0);
        gd.addNumericField("Remove Right Edge (pixels):", PixelsX,0);
        gd.addNumericField("Remove Top Edge (pixels):", 0,0);
        gd.addNumericField("Remove Bottom Edge (pixels):", PixelsY,0);

		gd.showDialog();

      RemoveSurface = (int)gd.getNextNumber();
      RemoveBottom = (int)gd.getNextNumber();
		RemoveLeftEdge = (int)gd.getNextNumber();
		RemoveRightEdge = (int)gd.getNextNumber();
		RemoveTopEdge = (int)gd.getNextNumber();
		RemoveBottomEdge = (int)gd.getNextNumber();			
	}
	
	private void createAutoGroupMarkerGUI(){  
        GenericDialog gd = new GenericDialog("Auto Group Marker Criteria");
	
        if (PSD95orSynt > 0){
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", PSD95orSynt,0);
		} else {
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", 1,0);
		}

		gd.showDialog();
		  
		  PSD95orSynt = (int)gd.getNextNumber();
	}

	private void createAutolinkGUI(){ 
        GenericDialog gd = new GenericDialog("Auto-Link Criteria");

        		gd.addNumericField("Img1 Day:", 0,0);
        		gd.addNumericField("Img2 Day:", 3,0);
        		
      if (ximg1 == 0 && yimg1 == 0 && zimg1 == 0){
        		gd.addNumericField("X coordinate in Image 1:", 0,0);
			gd.addNumericField("Y coordinate in Image 1:", 0,0);
			gd.addNumericField("Z coordinate in Image 1:", 0,0);
			gd.addNumericField("X coordinate in Image 2:", 0,0);
			gd.addNumericField("Y coordinate in Image 2:", 0,0);
			gd.addNumericField("Z coordinate in Image 2:", 0,0);
        } else {
        		gd.addNumericField("X coordinate in Image 1:", ximg1,0);
			gd.addNumericField("Y coordinate in Image 1:", yimg1,0);
			gd.addNumericField("Z coordinate in Image 1:", zimg1,0);
			gd.addNumericField("X coordinate in Image 2:", ximg2,0);
			gd.addNumericField("Y coordinate in Image 2:", yimg2,0);
			gd.addNumericField("Z coordinate in Image 2:", zimg2,0);
        }
        
		gd.addNumericField("X Link Distance Tolerance (microns):", xLinkTol,0);
		gd.addNumericField("Y Link Distance Tolerance (microns):", yLinkTol,0);
		gd.addNumericField("Z Link Distance Tolerance (microns):", zLinkTol,0);

		gd.showDialog();
		
		DayXML1 = (int)gd.getNextNumber();
		DayXML2 = (int)gd.getNextNumber();
		
		ximg1 = (int)gd.getNextNumber();
		yimg1 = (int)gd.getNextNumber();
		zimg1 = (int)gd.getNextNumber();
		ximg2 = (int)gd.getNextNumber();
		yimg2 = (int)gd.getNextNumber();
		zimg2 = (int)gd.getNextNumber();	
		
		//if (ShiftEstimateCalculated == 0){	
			Shiftx = ximg2 - ximg1;
			Shifty = yimg2 - yimg1;
			Shiftz = zimg2 - zimg1;
		//}
		
		xLinkTol = (int)gd.getNextNumber();
		yLinkTol = (int)gd.getNextNumber();
		zLinkTol = (int)gd.getNextNumber();
	}
	
	private void createAutolink2GUI(){ 
        GenericDialog gd = new GenericDialog("Auto-Link Error");
        
        if (PSD95orSynt > 0){
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", PSD95orSynt,0);
		} else {
			gd.addNumericField("PSD95 (1) or Synaptophysin (2):", 1,0);
		}

			gd.showDialog();
		  
		  PSD95orSynt = (int)gd.getNextNumber();  		
	}
	 
    private JTextField makeDynamicTextArea(){
        JTextField txtFld = new JTextField();
        txtFld.setHorizontalAlignment(JTextField.CENTER);
        txtFld.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        txtFld.setEditable(false);
        txtFld.setText("0");
        txtFieldVector.add(txtFld);
        return txtFld;
    }
    
    private JRadioButton makeDynRadioButton(int id){
    	  if (id<3) {
    	  JRadioButton jrButton = new JRadioButton("Image "+ id);
    	     jrButton.addActionListener(this);
        dynRadioVector.add(jrButton);
        radioGrp.add(jrButton);
        markerVector = new PunctaCntrMarkerVector(id);
        typeVector.add(markerVector);
        dynTxtPanel.add(makeDynamicTextArea());
        return jrButton;
    	  }
    	  else {
        JRadioButton jrButton = new JRadioButton("Type "+ id);
        
        jrButton.addActionListener(this);
        dynRadioVector.add(jrButton);
        radioGrp.add(jrButton);
        markerVector = new PunctaCntrMarkerVector(id);
        typeVector.add(markerVector);
        dynTxtPanel.add(makeDynamicTextArea());
        return jrButton;
        }
        
    }
    
    private JButton makeButton(String name, String tooltip){
        JButton jButton = new JButton(name);
        jButton.setToolTipText(tooltip);
        jButton.addActionListener(this);
        return jButton;
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
		activeIC = new PunctaCntrImageCanvasAutolink(activeImg,this,nextCanvasID,displayList);
		
		if (nextCanvasID == 1) {
			activeImg = WindowManager.getImage(1);

			img1 = activeImg;
			ic1 = activeIC;
		   PixelsX = img1.getWidth();
		   PixelsY = img1.getHeight();
		   PixelsImg1Z = img1.getStackSize();
		   
		   //IJ.log("1030 img1 title = "+img1.getTitle());
		   
		   BackgroundGridSize = (int)Math.round(Math.sqrt(PixelsX*PixelsY))/64; //sampling is 3.125 microns per pixel, equals 16 for 1024 pixel, 1x 200 micron image
		   MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY))));	
		   
			compareMode = false;
			//createImageSizeGUI();
			
			//Begin set background level block 
         if (bkgrnd_initialized != 0)
           	IJ.error("Background is already initialized in initializeImage");
           	
         int [] picknumber;
         picknumber = new int[PixelsImg1Z+1]; //it is +1 because array starts at zero but we have bz starting at 1          		
			bkgrnd_avg1 = new double[PixelsImg1Z+1];
			bkgrnd_stddev1 = new double[PixelsImg1Z+1];
			signal_avg1 = new double[PixelsImg1Z+1];
			signal_stddev1 = new double[PixelsImg1Z+1];	
			signal_signal1 = new double[PixelsImg1Z+1];
			signal_max1 = new double[PixelsImg1Z+1];	
			signal_min1 = new double[PixelsImg1Z+1];
			signal_median1 = new double[PixelsImg1Z+1];	
			signal_picknumber1 = new int[PixelsImg1Z+1];	

			//Note: May break if stackSize == 1 (?)
	      ImageStack stack = img1.getStack();
			ImageProcessor ip;
			
			for (int bz = 1; bz <= PixelsImg1Z; bz++) {
				ip = stack.getProcessor(bz);
			   	ip.resetRoi();
			   	ip = ip.crop();
			   	
				MeasureData MD = findBackgroundLevel(ip);
				bkgrnd_avg1[bz] = MD.avg;
				bkgrnd_stddev1[bz] = MD.stddev;

				MeasureData MDsignal = findSignalLevel(ip, bz, picknumber);
				signal_picknumber1[bz] = MDsignal.picknumber;
				signal_signal1[bz] = MDsignal.signal;
				signal_avg1[bz] = MDsignal.avg;
				signal_stddev1[bz] = MDsignal.stddev;
				signal_max1[bz] = MDsignal.max;
				signal_min1[bz] = MDsignal.min;
				signal_median1[bz] = MDsignal.median;

				/*
				if (!results_initialized) {
					rt.reset();
					results_initialized = true;
				}
			
				rt.incrementCounter();
		
				rt.addValue("Img1 Background Noise per Slice Average",bkgrnd_avg1[bz]);
				rt.addValue("Img1 Background Noise per Slice Stddev",bkgrnd_stddev1[bz]);

				rt.addValue("Img1 Signal per Slice Average",signal_avg1[bz]);
				rt.addValue("Img1 ignal per Slice Stddev",signal_stddev1[bz]);	
				*/
			}
			bkgrnd_initialized = 1;
			//End set background level block 
		} else {
		   //Can compare img2.getWidth() and img2.getHeight() and produce error
		   //if no match
		   activeImg = WindowManager.getImage(2);
		   
			img2 = activeImg;
			ic2 = activeIC;
	   	int x2 = img2.getWidth();
	   	int y2 = img2.getHeight();
	   	PixelsImg2Z = img2.getStackSize();
	   	 //IJ.log("1103 img2 title = "+img2.getTitle());
	   	
	   	if (x2 != PixelsX || y2 != PixelsY) { 
		    	IJ.error("Images must be same size");
		  	}
		  	
			compareMode = true;
			int [] picknumber;
         picknumber = new int[PixelsImg2Z+1]; //it is +1 because array starts at zero but we have bz starting at 1 
			//Begin set background level block 
			bkgrnd_avg2 = new double[PixelsImg2Z+1];
			bkgrnd_stddev2 = new double[PixelsImg2Z+1];
			signal_avg2 = new double[PixelsImg2Z+1];
			signal_stddev2 = new double[PixelsImg2Z+1];	
			signal_signal2 = new double[PixelsImg2Z+1];
			signal_max2 = new double[PixelsImg2Z+1];	
			signal_min2 = new double[PixelsImg2Z+1];
			signal_median2 = new double[PixelsImg2Z+1];	
			signal_picknumber2 = new int[PixelsImg2Z+1];			
						
			//Note: May break if stackSize == 1 (?)
	      ImageStack stack = img2.getStack();
			ImageProcessor ip;
			for (int bz = 1; bz <= PixelsImg2Z; bz++) {
				ip = stack.getProcessor(bz);
			   	ip.resetRoi();
			   	//ip = ip.crop();
			   	
				MeasureData MD2 = findBackgroundLevel2(ip);
				bkgrnd_avg2[bz] = MD2.avg;
				bkgrnd_stddev2[bz] = MD2.stddev;
				
				MeasureData MDsignal2 = findSignalLevel2(ip, bz, picknumber);
				signal_picknumber2[bz] = MDsignal2.picknumber;
				signal_signal2[bz] = MDsignal2.signal;
				signal_avg2[bz] = MDsignal2.avg;
				signal_stddev2[bz] = MDsignal2.stddev;
				signal_max2[bz] = MDsignal2.max;
				signal_min2[bz] = MDsignal2.min;
				signal_median2[bz] = MDsignal2.median;
			}
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
        
		  if (command.compareTo(INITIALIZE) == 0){
            initializeImage();
        } else if (command.startsWith("Type")){ //COUNT
            if (activeIC == null){
                IJ.error("You need to initialize first");
                return;
            }
            int index = Integer.parseInt(command.substring(command.indexOf(" ")+1,command.length()));
            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index-1);
            setCurrentMarkerVector(currentMarkerVector);
        } else if (command.compareTo(LOADMARKERS) == 0){
            if (activeIC == null) {
                IJ.error("No active image in loadMarkers call"); //debug
                initializeImage();
            }
            if (compareMode) {
            	createLoadMarkersGUI();
            }
            createLoadMarkersGUI2();           
   
				if (!compareMode) {
	            if (ManualXML == 0 || LoadXML1 == 0) {
	            	loadMarkers();
	            } else  {
	             	String filePath = myDirectory+img1.getTitle()+"_"+LoadXML1+".xml";
	             	String filePath2 = filePath;
	            	loadMarkers2(filePath, filePath2); //loadmarkers2 does not actually use filePath2 when not in compare mode
	            }       
            } else {
            	if (LinkedXML == 0) {
	 	            if (ManualXML == 0 || LoadXML1 == 0 || LoadXML2 == 0) {
		            	loadMarkers();
		            } else  {
		             	String filePath = myDirectory+fileSeparator+img2.getTitle()+"_"+LoadXML2+".xml"; 
		             	String filePath2 = filePath;
		            	loadMarkers2(filePath, filePath2);
		            } 
	            } else {
	            	if (ManualXML == 0) {
		            	loadMarkers();
		            	relinkBasedOnResultNums();
		            } else {
		             	String filePath = myDirectory+img1.getTitle()+"_Day"+DayXML2+".xml";
		             	String filePath2 = myDirectory+img2.getTitle()+"_Day"+DayXML1+".xml";
		            	loadMarkers2(filePath, filePath2);
		            	relinkBasedOnResultNums();
		            }            
	            }
            }
            validateLayout();
        } else if (command.compareTo(EXPORTMARKERS) == 0){
            exportMarkers();
        } else if (command.compareTo(AUTODETECT) == 0){
            createAutodetectGUI();
            autoDetectButton();
        }  else if (command.compareTo(REMOVEINADEQUATE) == 0){	
        		createRemoveInadequateGUI();
        		createRemoveInadequateGUI2();
        		
        		if (compareMode && ShiftEstimateCalculated == 0) {
        			BestShiftEstimate();
        		}
            removeinadequate();
        } else if (command.compareTo(AUTOGROUP) == 0){
            createAutoGroupMarkerGUI();
            
            if (compareMode) {
            	autoGroupMarker(1);
            	autoGroupMarker(2);
            } else {
            	autoGroupMarker(1);
            }
        } else if (command.compareTo(REMOVEEDGES) == 0){
            createRemoveEdgesGUI();
            RemoveEdges();
        } else if (command.compareTo(AUTOLINK) == 0){
         createAutolinkGUI();
			if (ShiftEstimateCalculated == 0) {			
				BestShiftEstimate();
			}
			autolink();
			autolink2Button.setEnabled(true);
		} else if (command.compareTo(AUTOLINK2) == 0){
         createAutolink2GUI();
			if (LinkedXML == 0) {
				IJ.log("Need to autolink first");
			} else if (LinkedXML == 1) {
				autolink2();
			} else {
        		IJ.log("Cannot autolink2 because of manual linking");
        	}
		} else if (command.compareTo(MEASURE) == 0){
			measure();
		} else if (command.compareTo(RESULTS) == 0){
            report();
        } else if (command.compareTo(ADD) == 0) {
            int i = dynRadioVector.size() + 1;
            dynGrid.setRows(i);
            dynButtonPanel.add(makeDynRadioButton(i));
            validateLayout();
        } else if (command.compareTo(REMOVE) == 0) {
            if (dynRadioVector.size() > 1) {
                JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                dynButtonPanel.remove(rbutton);
                radioGrp.remove(rbutton);
                dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                dynGrid.setRows(dynRadioVector.size());
            }
            if (txtFieldVector.size() > 1) {
                JTextField field = (JTextField)txtFieldVector.lastElement();
                dynTxtPanel.remove(field);
                txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
            }
            if (typeVector.size() > 1) {
                typeVector.removeElementAt(typeVector.size() - 1);
            }
            validateLayout();
        } else if (command.compareTo(RESET) == 0){
            reset();
		  }
		  
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
        } else if (e.getItem().equals(manualgroupCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
                manualgroupmode = true;
                unlinkmode = false;
                manuallinkmode = false;
          } else {
          		 manualgroupmode = false;
          }
        } else if (e.getItem().equals(unlinkCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
                unlinkmode = true;
                manualgroupmode = false;
                manuallinkmode = false;
          } else {
          		 unlinkmode = false;
          }
        } else if (e.getItem().equals(manuallinkCheck)){
          if (e.getStateChange()==ItemEvent.SELECTED){
            	 manuallinkmode = true;
            	 manualgroupmode = false;
                unlinkmode = false;
          } else { 
          		 manuallinkmode = false;
          }
        }
    }

//-----------------------------LOAD MARKERS
	 public void loadMarkers2(String filePath, String filePath2){
	 		if (!compareMode) {  
	        ReadXML rxml = new ReadXML(filePath);
	        String storedfilename = rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
	        if (storedfilename.equals(img1.getTitle())){
	            rxml.readMarkerData(typeVector, 1);
	            int index = Integer.parseInt(rxml.readImgProperties(rxml.CURRENT_TYPE));
	            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
	            setCurrentMarkerVector(currentMarkerVector); //redundant?
	        }  
        } else {
         	ReadXML rxml1 = new ReadXML(filePath);
	        String storedfilename1 = rxml1.readImgProperties(rxml1.IMAGE_FILE_PATH);
	        if (storedfilename1.equals(img1.getTitle())){
	            rxml1.readMarkerData(typeVector, 1);
	            int index = Integer.parseInt(rxml1.readImgProperties(rxml1.CURRENT_TYPE));
	            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
	            setCurrentMarkerVector(currentMarkerVector); //redundant?
	        }        
	        ReadXML rxml2 = new ReadXML(filePath2);
	        String storedfilename2 = rxml2.readImgProperties(rxml2.IMAGE_FILE_PATH);
	        if (storedfilename2.equals(img2.getTitle())){
	            rxml2.readMarkerData(typeVector, 2);
	            int index = Integer.parseInt(rxml2.readImgProperties(rxml2.CURRENT_TYPE));
	            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
	            setCurrentMarkerVector(currentMarkerVector); //redundant?
	        }  
        }
	 }

    public void loadMarkers(){
      if (!compareMode) {  //This code runs when working with one image
		  String filePath = getFilePath(new JFrame(), "Marker File For "+activeImg.getTitle(), FileDialog.LOAD);
        ReadXML rxml = new ReadXML(filePath);
        String storedfilename = rxml.readImgProperties(rxml.IMAGE_FILE_PATH);
        if (storedfilename.equals(activeImg.getTitle())){
            rxml.readMarkerData(typeVector, 1);
            int index = Integer.parseInt(rxml.readImgProperties(rxml.CURRENT_TYPE));
            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
            setCurrentMarkerVector(currentMarkerVector); //redundant?
            
            while(dynRadioVector.size()>typeVector.size()){
                if (dynRadioVector.size() > 1){
                    JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                    dynButtonPanel.remove(rbutton);
                    radioGrp.remove(rbutton);
                    dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                    dynGrid.setRows(dynRadioVector.size());
                }
                if (txtFieldVector.size() > 1){
                    JTextField field = (JTextField)txtFieldVector.lastElement();
                    dynTxtPanel.remove(field);
                    txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
                }
            }
            JRadioButton butt = (JRadioButton)(dynRadioVector.get(index));
            butt.setSelected(true);
        } else {
            IJ.error("These Markers do not belong to the current image");
        }
	   } else { //This code runs when working with two images
		  String filePath1 = getFilePath(new JFrame(), "Marker File For "+img1.getTitle(), FileDialog.LOAD);
        ReadXML rxml1 = new ReadXML(filePath1);
        String storedfilename = rxml1.readImgProperties(rxml1.IMAGE_FILE_PATH);
        if (storedfilename.equals(img1.getTitle())){
            //Vector<PunctaCntrMarkerVector> typeVector = rxml1.readMarkerData();
            rxml1.readMarkerData(typeVector, 1);
            
            int index = Integer.parseInt(rxml1.readImgProperties(rxml1.CURRENT_TYPE));
            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
            setCurrentMarkerVector(currentMarkerVector);
            
            while(dynRadioVector.size()>typeVector.size()){
                if (dynRadioVector.size() > 1){
                    JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                    dynButtonPanel.remove(rbutton);
                    radioGrp.remove(rbutton);
                    dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                    dynGrid.setRows(dynRadioVector.size());
                }
                if (txtFieldVector.size() > 1){
                    JTextField field = (JTextField)txtFieldVector.lastElement();
                    dynTxtPanel.remove(field);
                    txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
                }
            }
            JRadioButton butt = (JRadioButton)(dynRadioVector.get(index));
            butt.setSelected(true);
        } else {
            IJ.error("These Markers do not belong to the current image");
        }
        String filePath2 = getFilePath(new JFrame(), "Marker File For "+img2.getTitle(), FileDialog.LOAD);
        ReadXML rxml2 = new ReadXML(filePath2);
        String storedfilename2 = rxml2.readImgProperties(rxml2.IMAGE_FILE_PATH);
        if (storedfilename2.equals(img2.getTitle())){
            //Vector<PunctaCntrMarkerVector> typeVector = rxml2.readMarkerData();;
            rxml2.readMarkerData(typeVector, 2);
            
            int index = Integer.parseInt(rxml2.readImgProperties(rxml2.CURRENT_TYPE));
            currentMarkerVector = (PunctaCntrMarkerVector)typeVector.get(index); 
            setCurrentMarkerVector(currentMarkerVector);
            
            while(dynRadioVector.size()>typeVector.size()){
                if (dynRadioVector.size() > 1){
                    JRadioButton rbutton = (JRadioButton)dynRadioVector.lastElement();
                    dynButtonPanel.remove(rbutton);
                    radioGrp.remove(rbutton);
                    dynRadioVector.removeElementAt(dynRadioVector.size() - 1);
                    dynGrid.setRows(dynRadioVector.size());
                }
                if (txtFieldVector.size() > 1){
                    JTextField field = (JTextField)txtFieldVector.lastElement();
                    dynTxtPanel.remove(field);
                    txtFieldVector.removeElementAt(txtFieldVector.size() - 1);
                }
            }
            JRadioButton butt = (JRadioButton)(dynRadioVector.get(index));
            butt.setSelected(true);
        } else {
            IJ.error("These Markers do not belong to the current image");
        }
		} 
	 }
  
    public Vector getTypeVector() {
        return typeVector;
    }
    
//-----------------------------EXPORT MARKERS  
	 public void exportMarkers2(String filePath, int n) {
      if (n == 1) {
      	WriteXML wxml1 = new WriteXML(filePath);
			wxml1.writeXML(img1.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), n);
		} else {
			WriteXML wxml2 = new WriteXML(filePath);
			wxml2.writeXML(img2.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), n);
		}
    }   
    
    public void exportMarkers(){
		  if (!compareMode) {  //This code runs when working with one image
		  		  String filePath = getFilePath(new JFrame(), "Marker File "+activeImg.getTitle(), FileDialog.SAVE);
				  if (!filePath.endsWith(".xml"))
                 filePath+=".xml";
				  WriteXML wxml = new WriteXML(filePath);
		        wxml.writeXML(activeImg.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), 0);
		  } else { //This code runs when working with two images
		  		  String filePath1 = getFilePath(new JFrame(), "Marker File "+img1.getTitle(), FileDialog.SAVE);
				  if (!filePath1.endsWith(".xml"))
                 filePath1+=".xml";
				  WriteXML wxml1 = new WriteXML(filePath1);
		        wxml1.writeXML(img1.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), 1);
		       
		        String filePath2 = getFilePath2(new JFrame(), "Marker File "+img2.getTitle(), FileDialog.SAVE);
				  if (!filePath2.endsWith(".xml"))
                 filePath2+=".xml";
		        WriteXML wxml2 = new WriteXML(filePath2); //why does it not update the filename?
		        wxml2.writeXML(img2.getTitle(), typeVector, typeVector.indexOf(currentMarkerVector), 2);
		  }
    }
    
    private String getFilePath(JFrame parent, String dialogMessage, int dialogType){
        
        switch(dialogType){
            case(FileDialog.SAVE):
                dialogMessage = "Save "+dialogMessage;
                break;
            case(FileDialog.LOAD):
                dialogMessage = "Open "+dialogMessage;
                break;
        }
        
        String[] filePathComponents = new String[2];
        int PATH = 0;
        int FILE = 1;
 
        FileDialog fd = new FileDialog(parent, dialogMessage, dialogType);
        
        switch(dialogType){
            case(FileDialog.SAVE):
		          if (LinkedXML == 0) {   
		          	String filename = img1.getTitle();	
		          	//fd.setFile(filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                	fd.setFile(filename+".xml");
                } else {
        		      String filename = img1.getTitle();
                	fd.setFile(filename+"_Day"+DayXML2+".xml");
                }
                break;
        }
        
      
        fd.setDirectory(myDirectory);
        fd.setVisible(true);
        
        filePathComponents[PATH] = fd.getDirectory();
        filePathComponents[FILE] = fd.getFile();
        return filePathComponents[PATH]+filePathComponents[FILE];
    }
    
    private String getFilePath2(JFrame parent, String dialogMessage, int dialogType){
        
        switch(dialogType){
            case(FileDialog.SAVE):
                dialogMessage = "Save "+dialogMessage;
                break;
            case(FileDialog.LOAD):
                dialogMessage = "Open "+dialogMessage;
                break;
        }
        
        String[] filePathComponents = new String[2];
        int PATH = 0;
        int FILE = 1;
 
        FileDialog fd = new FileDialog(parent, dialogMessage, dialogType);
        
        switch(dialogType){
            case(FileDialog.SAVE):
            	 if (LinkedXML == 0) {
                	String filename = img2.getTitle();
                	//fd.setFile(filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                	fd.setFile(filename+".xml");
                } else {
                	String filename = img2.getTitle();
                	//fd.setFile(filename.substring(0,filename.lastIndexOf(".")+1)+"xml");
                	fd.setFile(filename+"_Day"+DayXML1+".xml");
                }
                break;
        }
        
        
        fd.setDirectory(myDirectory);
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
		private double max;
		private double min;
		private double median;
		private double signal;
		private int picknumber;
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
	
	private void addSample2(MeasureData MD2, double newavg, double newstddev, int oldcontribs) {
		if (oldcontribs == 0) {
			MD2.avg = newavg;
			MD2.stddev = newstddev;
			return;
		}
		MD2.stddev = oldcontribs*(MD2.stddev*MD2.stddev + MD2.avg*MD2.avg);
		MD2.stddev += newavg*newavg + newstddev*newstddev;
		MD2.stddev /= (oldcontribs+1);
		MD2.avg = (MD2.avg*oldcontribs + newavg)/(oldcontribs+1);
		MD2.stddev -= MD2.avg*MD2.avg;
		MD2.stddev = Math.sqrt(MD2.stddev);
	}
	
	private void addSampleSignal(MeasureData MDsignal, int picknumber, double signal, double max, double min, double median, double avg, double stddev) {
		MDsignal.picknumber = picknumber;
		MDsignal.signal = signal;
		MDsignal.max = max;
		MDsignal.min = min;
		MDsignal.median = median;
		MDsignal.avg = avg;
		MDsignal.stddev = stddev;	
	}
	
	private void addSampleSignal2(MeasureData MDsignal2, int picknumber2, double signal, double max, double min, double median, double avg, double stddev) {
		MDsignal2.picknumber = picknumber2;
		MDsignal2.signal = signal;
		MDsignal2.max = max;
		MDsignal2.min = min;
		MDsignal2.median = median;
		MDsignal2.avg = avg;
		MDsignal2.stddev = stddev;	
	}	
	
	private void addSampleSignalAroundPunctum(MeasureData MDsignalAroundPunctum, double newavg, double newstddev, int oldcontribs) {
		if (oldcontribs == 0) {
			MDsignalAroundPunctum.avg = newavg;
			MDsignalAroundPunctum.stddev = newstddev;
			return;
		}
		MDsignalAroundPunctum.stddev = oldcontribs*(MDsignalAroundPunctum.stddev*MDsignalAroundPunctum.stddev + MDsignalAroundPunctum.avg*MDsignalAroundPunctum.avg);
		MDsignalAroundPunctum.stddev += newavg*newavg + newstddev*newstddev;
		MDsignalAroundPunctum.stddev /= (oldcontribs+1);
		MDsignalAroundPunctum.avg = (MDsignalAroundPunctum.avg*oldcontribs + newavg)/(oldcontribs+1);
		MDsignalAroundPunctum.stddev -= MDsignalAroundPunctum.avg*MDsignalAroundPunctum.avg;
		MDsignalAroundPunctum.stddev = Math.sqrt(MDsignalAroundPunctum.stddev);
	}
	
	private void addSampleSignalAroundPunctum2(MeasureData MDsignalAroundPunctum2, double newavg, double newstddev, int oldcontribs) {
		if (oldcontribs == 0) {
			MDsignalAroundPunctum2.avg = newavg;
			MDsignalAroundPunctum2.stddev = newstddev;
			return;
		}
		MDsignalAroundPunctum2.stddev = oldcontribs*(MDsignalAroundPunctum2.stddev*MDsignalAroundPunctum2.stddev + MDsignalAroundPunctum2.avg*MDsignalAroundPunctum2.avg);
		MDsignalAroundPunctum2.stddev += newavg*newavg + newstddev*newstddev;
		MDsignalAroundPunctum2.stddev /= (oldcontribs+1);
		MDsignalAroundPunctum2.avg = (MDsignalAroundPunctum2.avg*oldcontribs + newavg)/(oldcontribs+1);
		MDsignalAroundPunctum2.stddev -= MDsignalAroundPunctum2.avg*MDsignalAroundPunctum2.avg;
		MDsignalAroundPunctum2.stddev = Math.sqrt(MDsignalAroundPunctum2.stddev);
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
			   
			   //takes average and stdev of all grids less than or equal to first grid with the exception of grids that have a pixel = 0
			   //is this the best way to do it?  underestimates the background noise in many cases especially in the center since the edges
			   //tend to be more faint and should we make an array of slice or an array for each grid? 
				if (contribs == 0 || avg <= (MD.avg + MD.stddev)) {
					addSample(MD, avg, stddev, contribs);
					contribs++;
				}
			}
			
			PixelZero = false;
		}
		return MD;
	}
	
	private MeasureData findBackgroundLevel2(ImageProcessor ip) {
		MeasureData MD2 = new MeasureData();
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
			   
			   //takes average and stdev of all grids less than or equal to first grid with the exception of grids that have a pixel = 0
			   //is this the best way to do it?  underestimates the background noise in many cases especially in the center since the edges
			   //tend to be more faint and should we make an array of slice or an array for each grid? 
				if (contribs == 0 || avg <= (MD2.avg + MD2.stddev)) {
					addSample2(MD2, avg, stddev, contribs);
					contribs++;
				}
			}
			
			PixelZero = false;
		}
		return MD2;
	}
	
	private MeasureData findSignalLevel(ImageProcessor ip, int z, int [] picknumber) { //finds average signal level per slices excluding cell bodies 
		MeasureData MDsignal = new MeasureData();

		int v;
		int v2;
		int threshold = 65;
		int count = 0;
		int i = 0;
				
		//find cell bodies 
		for (int ii = 0; ii <= PixelsX; ii += BackgroundGridSize) { //this is generally 16 * 2 or 32
			for (int jj = 0; jj <= PixelsY; jj += BackgroundGridSize) {
				int i2 = 0;
				int j = 0;
				for (int x = 0; x < BackgroundGridSize; x++) {
					for (int y = 0; y < BackgroundGridSize; y++) {
						v = (int)ip.getPixelValue(ii+x,jj+y);
						
						//calculates the intensity level of all that is considered a signal and excludes background noise
		          	if (v >= threshold) { //3x background noise?
		          		i++;
		          		j++;
		          	}
		          	
		            if (v >= 100) { //replace with percentile value of entire slice 
		          		i2++;
		          	}	
		          	
		          	//if 90% of pixels of the grid size are intensity > 100 then it is a cell body
		          	if (i2 >= (0.9*BackgroundGridSize*BackgroundGridSize)) { 
		          		CellBody = true;
		          	}
		          	
		          	if (CellBody && x == (BackgroundGridSize - 1) && y == (BackgroundGridSize - 1)) {
		          		//IJ.log("before i = "+i);
		          		//IJ.log("ii = "+ii+" jj = "+jj+" z = "+z);
		          		i = i - j; 
		          		//IJ.log("after i = "+i);
		          		j = 0;
		          	}
					}
				}
				CellBody = false;
			}
		}
	
		//IJ.log("i = "+i);
		
		picknumber[z] = i - (int)Math.round(0.99*i); //one picknumber per z-section

		//IJ.log("picknumber img1 = "+picknumber[z]);
		
					
		if (i > 100 && i > picknumber[z]) { //minimum of 100 pixels above threshold per slice in order to calculate the red signal; i > picknumber is important for second image			
			int[] signalarray;
			signalarray = new int[i];
	
			i = 0;
	 
			for (int ii = 0; ii <= PixelsX; ii += BackgroundGridSize) { //this is generally 16 * 2 or 32
				for (int jj = 0; jj <= PixelsY; jj += BackgroundGridSize) {
					int i2 = 0;
					for (int x = 0; x < BackgroundGridSize; x++) {
						for (int y = 0; y < BackgroundGridSize; y++) {
							v = (int)ip.getPixelValue(ii+x,jj+y);
			          	
			            if (v >= 100) { //replace with percentile value of entire slice 
			          		i2++;
			          	}	
			          	
			          	//if 90% of pixels of the grid size are intensity > 100 then it is a cell body
			          	if (i2 >= (0.9*BackgroundGridSize*BackgroundGridSize)) { 
			          		CellBody = true;
			          	}
						}
					}
					
					if (!CellBody) {
						for (int x = 0; x < BackgroundGridSize; x++) {
							for (int y = 0; y < BackgroundGridSize; y++) {
								v2 = (int)ip.getPixelValue(ii+x,jj+y);
				          	
							   if (v2 >= threshold) {
							      signalarray[i++] = v2;
							   }
							}
						}
					}
					CellBody = false;
				}
			}
	
			Arrays.sort(signalarray);

			double min = signalarray[0];
			double max = signalarray[i-1];
		
			double avg = 0;
			int sum = 0;
			
			for (int xx = 0; xx < signalarray.length; xx++) {
				sum = sum + signalarray[xx];
			}
			
			avg = (double)sum/signalarray.length;

			
			double stddev = 0;
					
			double median = 0;
			
			if (signalarray.length %2 != 0) {
				median = signalarray[signalarray.length/2];
			} else {
				median = ((double)signalarray[(signalarray.length/2)-1] + (double)signalarray[signalarray.length/2]) / 2;
			}

			
			count = i - picknumber[z];
	
			//IJ.log("img1 z = "+z+" red signal = "+signalarray[count]);  
		 
			addSampleSignal(MDsignal, picknumber[z], signalarray[count], max, min, median, avg, stddev);
			
			Arrays.fill(signalarray,0);
		} else {
			addSampleSignal(MDsignal, -1, -1, -1, -1, -1, -1, -1);
		}
		
		return MDsignal;
	}
	
	private MeasureData findSignalLevel2(ImageProcessor ip, int z, int [] picknumber2) { //finds average signal level per slices excluding cell bodies 
		MeasureData MDsignal2 = new MeasureData();
		
		int v;
		int v2;
		int threshold = 65;
		int count = 0;
		int i = 0;
				
		//find cell bodies 
		for (int ii = 0; ii <= PixelsX; ii += BackgroundGridSize) { //this is generally 16 * 2 or 32
			for (int jj = 0; jj <= PixelsY; jj += BackgroundGridSize) {
				int i2 = 0;
				int j = 0;
				for (int x = 0; x < BackgroundGridSize; x++) {
					for (int y = 0; y < BackgroundGridSize; y++) {
						v = (int)ip.getPixelValue(ii+x,jj+y);
						
						//calculates the intensity level of all that is considered a signal and excludes background noise
		          	if (v >= threshold) { //3x background noise?
		          		i++;
		          		j++;
		          	}
		          	
		            if (v >= 100) { //replace with percentile value of entire slice 
		          		i2++;
		          	}	
		          	
		          	//if 90% of pixels of the grid size are intensity > 100 then it is a cell body
		          	if (i2 >= (0.9*BackgroundGridSize*BackgroundGridSize)) { 
		          		CellBody = true;
		          	}
		          	
		          	if (CellBody && x == (BackgroundGridSize - 1) && y == (BackgroundGridSize - 1)) {
		          		//IJ.log("before i = "+i);
		          		//IJ.log("ii = "+ii+" jj = "+jj+" z = "+z);
		          		i = i - j; 
		          		//IJ.log("after i = "+i);
		          		j = 0;
		          	}
					}
				}
				CellBody = false;
			}
		}
		
		//IJ.log("z = "+z+" picknumber img2 = "+picknumber2);
			
				picknumber2[z] = i - (int)Math.round(0.99*i); //one picknumber per z-section

		//IJ.log("picknumber img2 = "+picknumber2[z]);
		
					
		if (i > 100 && i > picknumber2[z]) { //minimum of 100 pixels above threshold per slice in order to calculate the red signal; i > picknumber is important for second image			
			int[] signalarray2;
			signalarray2 = new int[i];
	
			i = 0;
	 
			for (int ii = 0; ii <= PixelsX; ii += BackgroundGridSize) { //this is generally 16 * 2 or 32
				for (int jj = 0; jj <= PixelsY; jj += BackgroundGridSize) {
					int i2 = 0;
					for (int x = 0; x < BackgroundGridSize; x++) {
						for (int y = 0; y < BackgroundGridSize; y++) {
							v = (int)ip.getPixelValue(ii+x,jj+y);
			          	
			            if (v >= 100) { //replace with percentile value of entire slice 
			          		i2++;
			          	}	
			          	
			          	//if 90% of pixels of the grid size are intensity > 100 then it is a cell body
			          	if (i2 >= (0.9*BackgroundGridSize*BackgroundGridSize)) { 
			          		CellBody = true;
			          	}
						}
					}
					
					if (!CellBody) {
						for (int x = 0; x < BackgroundGridSize; x++) {
							for (int y = 0; y < BackgroundGridSize; y++) {
								v2 = (int)ip.getPixelValue(ii+x,jj+y);
				          	
							   if (v2 >= threshold) {
							      signalarray2[i++] = v2;
							   }
							}
						}
					}
					CellBody = false;
				}
			}
				
				
	
			Arrays.sort(signalarray2);

			double min = signalarray2[0];
			double max = signalarray2[i-1];
		
			double avg = 0;
			int sum = 0;
			
			for (int xx = 0; xx < signalarray2.length; xx++) {
				sum = sum + signalarray2[xx];
			}
			
			avg = (double)sum/signalarray2.length;

			
			double stddev = 0;
					
			double median = 0;
			
		
			if (signalarray2.length %2 != 0) {
				median = signalarray2[signalarray2.length/2];
			} else {
				median = ((double)signalarray2[(signalarray2.length/2)-1] + (double)signalarray2[signalarray2.length/2]) / 2;
			}
			
			count = i - picknumber2[z];
	
			//IJ.log("img1 z = "+z+" red signal = "+signalarray[count]);  
		 
			
			addSampleSignal2(MDsignal2, picknumber2[z], signalarray2[count], max, min, median, avg, stddev);
			
			Arrays.fill(signalarray2,0);
		} else {
			addSampleSignal2(MDsignal2, -1, -1, -1, -1, -1, -1, -1);
		}
		
		return MDsignal2;
	}
	
	private MeasureData findSignalLevelAroundPunctum(ImageProcessor ip, int PunctumCenterX, int PunctumCenterY) { //finds average signal level per slices excluding cell bodies 
		MeasureData MDsignalAroundPunctum = new MeasureData();
		int contribs = 0;
		int v;
		
		int ii = (PunctumCenterX - BackgroundGridSize);
		int jj = (PunctumCenterY - BackgroundGridSize);

		int total = 0;
		double avg = 0;
		double stddev = 0;
		int count = 0;
		for (int x = 0; x < BackgroundGridSize*2; x++)
		for (int y = 0; y < BackgroundGridSize*2; y++) {
			v = (int)ip.getPixelValue(ii+x,jj+y);
			//calculates the intensity level of all that is considered a signal and excludes background noise
			//signal includes where the punctum resides
       	if (v >= 50) { 
       		total += v;
       		count++;
       	}
		}
	
		if (count > 0) {							
			avg = ((double)total)/(count);			
		
			for (int x = 0; x < BackgroundGridSize*2; x++)
			for (int y = 0; y < BackgroundGridSize*2; y++) {
				v = (int)ip.getPixelValue(ii+x,jj+y);
				if (v >= 50) {
					stddev += (v-avg) * (v-avg);
				}
			}
		
			stddev /= count;				
			stddev = Math.sqrt(stddev);
		
			//takes average and stdev of all grids in z-plane except for those grids that have cell bodies
		   if (contribs >= 0) {
				addSampleSignalAroundPunctum(MDsignalAroundPunctum, avg, stddev, contribs);
				contribs++;
			}
		}
		
		return MDsignalAroundPunctum;
	}	

	private MeasureData findSignalLevelAroundPunctum2(ImageProcessor ip, int PunctumCenterX, int PunctumCenterY) { //finds average signal level per slices excluding cell bodies 
		MeasureData MDsignalAroundPunctum2 = new MeasureData();
		int contribs = 0;
		int v;
		
		int ii = (PunctumCenterX - BackgroundGridSize);
		int jj = (PunctumCenterY - BackgroundGridSize);

		int total = 0;
		double avg = 0;
		double stddev = 0;
		int count = 0;
		for (int x = 0; x < BackgroundGridSize*2; x++)
		for (int y = 0; y < BackgroundGridSize*2; y++) {
			v = (int)ip.getPixelValue(ii+x,jj+y);
			//calculates the intensity level of all that is considered a signal and excludes background noise
			//signal includes where the punctum resides
       	if (v >= 50) { 
       		total += v;
       		count++;
       	}
		}
	
		if (count > 0) {							
			avg = ((double)total)/(count);			
		
			for (int x = 0; x < BackgroundGridSize*2; x++)
			for (int y = 0; y < BackgroundGridSize*2; y++) {
				v = (int)ip.getPixelValue(ii+x,jj+y);
				if (v >= 50) {
					stddev += (v-avg) * (v-avg);
				}
			}
		
			stddev /= count;				
			stddev = Math.sqrt(stddev);
		
			//takes average and stdev of all grids in z-plane except for those grids that have cell bodies
		   if (contribs >= 0) {
				addSampleSignalAroundPunctum2(MDsignalAroundPunctum2, avg, stddev, contribs);
				contribs++;
			}
		}
		
		return MDsignalAroundPunctum2;
	}	
	
//-----------------------------CRITERIA FOR SELECTING 2-D PUNCTA
	private PunctaCntrMarker findBoundary(int x, int y, int z, int canvasID) {
	if (z <= activeImg.getStackSize()) {
	
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
		int[] rings_v;
		int threshold;
		
		if (canvasID == 1) {
			threshold = (int)(bkgrnd_avg1[z]); //avoid tabulating 0's
		} else {
			threshold = (int)(bkgrnd_avg2[z]);
		}		

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
		int canvasID = m.canvasID;
		int v;
		int local_cutoff;
		
		if (canvasID == 1) {
			local_cutoff = (int)(bkgrnd_avg1[z] + ArbitraryLocalBoundaryCutoff*bkgrnd_stddev1[z]); //critical parameter for boundary of each punctum
		} else {
			local_cutoff = (int)(bkgrnd_avg2[z] + ArbitraryLocalBoundaryCutoff*bkgrnd_stddev2[z]);
		}		

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

	//this codes suffers from inability to autogroup and groupandremoveredundant which isn't as sophisticated as autogroup fails in that
	//it will assign two puncta with the same number on the same plane.
	public void MeasureByClick_old(int x, int y, int z, int canvasID) { //not in use 
		MinRingRatio=1.1;	 //this is set lower (to 1.1 instead of 2) so that when you can detect fainter puncta																						
		ArbitraryLocalBoundaryCutoff=2; //this is set lower (to 2 instead of 10) so that when you can detect fainter puncta	
		
		if (autolink2) {		
			MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY)))); //this equals to 2.5 microns, which is 25 pixels for 512x512 pixels 50x50 microns or 12.8 pixels for 1024x1024 pixels 200x200 microns			
		} else {
			MaxRadius = (int)Math.round(3*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY)))); 
		}
		
		PunctaCntrMarker m = findBoundary(x, y, z, canvasID);
		
		int donotadd = 1;
		
		for (int n = 0; n < currentMarkerVector.size(); n++) {
			PunctaCntrMarker m1 = currentMarkerVector.get(n);
		
			if (m1.getZ() == z) {
				int dx = m1.getX() - m.getX();
				int dy = m1.getY() - m.getY();
				int r1 = m1.getRad();
				int r2 = m.getRad();

				if (dx*dx+dy*dy < r1*r1 || dx*dx+dy*dy < r2*r2) {
					donotadd = 0;
					IJ.log("do not add");
				}
			}  
		}	
		
		if (m.getRad() > MinRingRatio && donotadd == 1) {	//min radius applies for 1x 1024x1024 image... does it apply for others? 
			int uid = currentMarkerVector.addOwnerMarker(m);
			m.canvasID = activeIC.canvasID;
			//IJ.log("x = "+x+" y = "+y+" z = "+z);
			int thisSlice = z;
			int loX = x;
			int loY = y;
			int loZ = 0;
			boolean foundspot = true;
			while (foundspot && thisSlice-1 >= 1) {
			  thisSlice--;
			  m = findBoundary(loX, loY, thisSlice, canvasID);
			  if (m.getRad() > 2) {		   
				currentMarkerVector.addSubjectMarker(m);
				m.setOwner(uid);
				m.canvasID = activeIC.canvasID;
				loX = m.getX();
				loY = m.getY();
				loZ = m.getZ();
			  } else {
				foundspot = false;
			  } 
			}
			//IJ.log("lox = "+loX+" loy = "+loY+" loz = "+loZ);
			thisSlice = z;
			int hiX = x;			
			int hiY = y;
			int hiZ = 0;
			foundspot = true;
			while (foundspot && thisSlice+1 <= activeImg.getStackSize()) {
			  thisSlice++;
			  m = findBoundary(hiX, hiY, thisSlice, canvasID);
			  if (m.getRad() > 2) {
			   currentMarkerVector.addSubjectMarker(m);
			   m.setOwner(uid);
				m.canvasID = activeIC.canvasID;
				hiX = m.getX();
				hiY = m.getY();
				hiZ = m.getZ();
				IJ.log("mB x = "+m.getX()+" y = "+m.getY()+" z = "+m.getZ());
			  } else {
				foundspot = false;
			  }
			}
			
			//IJ.log("hix = "+hiX+" hiy = "+hiY+" hiz = "+hiZ);
			//IJ.log("mB x = "+m.getX()+" y = "+m.getY()+" z = "+m.getZ());
		}
	} 		
	
	//this measureclick makes sure that m increments sequentially with z plane, but the problem is that you get occasinal punctamarkers that are
	// 0,0,0 (x,y,z) and this breaks the autolink2 code unless you skip over z=0 
	public void MeasureByClick_new(int x, int y, int z, int canvasID) {
		//MinRingRatio=1.1;	 // JSUN： to keep consistency, use previous parameter 																						
		//ArbitraryLocalBoundaryCutoff=10; // JSUN： to keep consistency, use previous parameter   
		
		if (autolink2) {		
			MaxRadius = (int)Math.round(2*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY)))); //this equals to 2.5 microns, which is 25 pixels for 512x512 pixels 50x50 microns or 12.8 pixels for 1024x1024 pixels 200x200 microns			
		} else {
			MaxRadius = (int)Math.round(3*(Math.sqrt((PixelsX/MicronsX)*(PixelsY/MicronsY)))); 
		}
		//if (x >= 600 && x <= 610 && y >= 975 && y <= 985) {
			//IJ.log("x = "+x+" y = "+y+" z = "+z);
		//}
		PunctaCntrMarker m = findBoundary(x, y, z, canvasID);
		
		int donotadd = 1;
		
		for (int n = 0; n < currentMarkerVector.size(); n++) {
			PunctaCntrMarker m1 = currentMarkerVector.get(n);
		
			if (m1.getZ() == z) {
				int dx = m1.getX() - m.getX();
				int dy = m1.getY() - m.getY();
				int r1 = m1.getRad();
				int r2 = m.getRad();
				//IJ.log("2183");
				if (dx*dx+dy*dy < r1*r1 || dx*dx+dy*dy < r2*r2) {
					donotadd = 0;
					//IJ.log("do not add");
				}
			}  
		}	
		
		if (m.getRad() > 2 && donotadd == 1) {	//min radius applies for 1x 1024x1024 image... does it apply for others? 
			int thisSlice = z;
			int loX = x;
			int loY = y;
			int loZ = z;
			boolean foundspot = true;
			//IJ.log("x = "+loX+" y = "+loY+" z = "+loZ);
			while (foundspot && thisSlice-1 >= 1) {
			  thisSlice--;
			  m = findBoundary(loX, loY, thisSlice, canvasID);
			  if (m.getRad() > 2) {		   
				loX = m.getX();
				loY = m.getY();
				loZ = m.getZ();
			  } else {
				foundspot = false;
			  } 
			}
			
			m = findBoundary(loX, loY, loZ, canvasID);
			//IJ.log("lox = "+loX+" loy = "+loY+" loz = "+loZ);
			int uid = currentMarkerVector.addOwnerMarker(m);
			m.canvasID = activeIC.canvasID;		
					
			thisSlice = loZ-1;
			int hiX = loX;			
			int hiY = loY;
			int hiZ = 0;
			foundspot = true;
			while (foundspot && thisSlice+1 <= activeImg.getStackSize()) {
			  thisSlice++;
			  m = findBoundary(hiX, hiY, thisSlice, canvasID);
			  if (m.getRad() > 2) {
			   currentMarkerVector.addSubjectMarker(m);
			   m.setOwner(uid);
				m.canvasID = activeIC.canvasID;
				hiX = m.getX();
				hiY = m.getY();
				hiZ = m.getZ();
				//IJ.log("mB x = "+m.getX()+" y = "+m.getY()+" z = "+m.getZ());
			  } else {
				foundspot = false;
			  }
			}
			
			//IJ.log("hix = "+hiX+" hiy = "+hiY+" hiz = "+hiZ);
			//IJ.log("mB x = "+m.getX()+" y = "+m.getY()+" z = "+m.getZ());
		}
	} 				
			
//-----------------------------AUTO DETECT PUNCTA FOR 1 OR 2 STACKS	
    public void autoDetectButton() {
		if (compareMode) { //This code runs when working with two images
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			//Note: .get(0) returns type 1, .get(1) returns type 2
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			setCurrentMarkerVector(PCMVA);
			autoDetect(1);
			setCurrentMarkerVector(PCMVB);
			autoDetect(2);
			setCurrentMarkerVector(startingCMV);
		} else { //This code runs when working with one image
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			autoDetect(1);
			setCurrentMarkerVector(startingCMV);
		}
    }
    
	 private void autoDetect(int canvasID) {
		if (canvasID == 1) {
			activeImg = img1;
			activeIC = ic1;
		} else {
			activeImg = img2;
			activeIC = ic2;
		}
		
		ImageProcessor ip;
		ImageStack stack = activeImg.getStack();
		
		if (bkgrnd_initialized == 0) {
			IJ.error("Background not initialized in autoDetect");
		}
		
		for (int z = 1; z <= activeImg.getStackSize(); z++) {
			IJ.showStatus("Processing slice "+z+"/"+activeImg.getStackSize());
			IJ.showProgress((double)z/(double)activeImg.getStackSize());
			
			ip = stack.getProcessor(z);
			ip.resetRoi();
			ip = ip.crop();
			
			if (canvasID == 1) {
				MeasureData MD = findBackgroundLevel(ip);
				bkgrnd_avg1[z] = MD.avg;
				bkgrnd_stddev1[z] = MD.stddev;
				bkgrnd_avg[z] = bkgrnd_avg1[z];
				bkgrnd_stddev[z] = bkgrnd_stddev1[z];
			} else {
				MeasureData MD2 = findBackgroundLevel2(ip);
				bkgrnd_avg2[z] = MD2.avg;
				bkgrnd_stddev2[z] = MD2.stddev;
				bkgrnd_avg[z] = bkgrnd_avg2[z];
				bkgrnd_stddev[z] = bkgrnd_stddev2[z];
			}			
			
			bkgrnd_initialized = 1;
			int horiz[] = new int[BackgroundGridSizeAutoDetect]; //critical parameter, BackgroundGridSize
			int vert[] = new int[BackgroundGridSizeAutoDetect];
			double hereavg;
			
			for (int jj = 0; jj < PixelsY; jj += BackgroundGridSizeAutoDetect/2) //critical PixelsX,PixelsY
			for (int ii = 0; ii < PixelsX; ii += BackgroundGridSizeAutoDetect/2) {
				hereavg = 0;
				for (int j = 0; j < BackgroundGridSizeAutoDetect; j++)
				for (int i = 0; i < BackgroundGridSizeAutoDetect; i++) {
					hereavg += (int)ip.getPixelValue(ii+i,jj+j);
				}
				hereavg /= BackgroundGridSizeAutoDetect*BackgroundGridSizeAutoDetect;
				if ((hereavg - bkgrnd_avg[z]) > bkgrnd_stddev[z]) {
					//setCurrentMarkerVector((PunctaCntrMarkerVector)typeVector.get(1));
					//PunctaCntrMarker mIdent = new PunctaCntrMarker(ii,jj,z,-1);
					//currentMarkerVector.addSubjectMarker(mIdent);
					//setCurrentMarkerVector((PunctaCntrMarkerVector)typeVector.get(0));
					//Nonbackground spot found. Locate the local brightness maxima.
					
					for (int i = 0; i < BackgroundGridSizeAutoDetect; i++) {
						horiz[i] = 0;
						vert[i] = 0;
					}
					for (int j = 0; j < BackgroundGridSizeAutoDetect; j++)
					for (int i = 0; i < BackgroundGridSizeAutoDetect; i++) {
						int v2 = (int)ip.getPixelValue(ii+i,jj+j);
						horiz[i] += v2;
						vert[j] += v2;
					}
					int besti = 0;
					int bestj = 0;
					int maxH = 0;
					int maxV = 0;
					for (int i = 1; i < BackgroundGridSizeAutoDetect-1; i++)
						if (horiz[i]+horiz[i-1]+horiz[i+1] > maxH) {
						maxH = horiz[i]+horiz[i-1]+horiz[i+1];
						besti = i;
					}
					for (int j = 1; j < BackgroundGridSizeAutoDetect-1; j++)
						if (vert[j]+vert[j-1]+vert[j+1] > maxV) {
						maxV = vert[j]+vert[j-1]+vert[j+1];
						bestj = j;
					}
					
					//If the spot is on the far right edge it'll be captured in the next half-increment
					if (besti < BackgroundGridSizeAutoDetect-2 && bestj < BackgroundGridSizeAutoDetect-2) {
						PunctaCntrMarker m = findBoundary(ii+besti, jj+bestj, z, canvasID);
						if (m.getRad() > 0) {
							currentMarkerVector.addOwnerMarker(m);
							if (m.getRad() >= BackgroundGridSizeAutoDetect/2)
								ii += BackgroundGridSizeAutoDetect/2;
							m.canvasID = activeIC.canvasID;
						}
					}
					
					for (int i = 0; i < BackgroundGridSizeAutoDetect; i+=2)
					for (int j = 0; j < BackgroundGridSizeAutoDetect; j+=2) {
						continue;
					}
					
					//Reclaim stack since MeasureByClick mucks with it  (?) (unsure if this is necessary)
					ip = stack.getProcessor(z);
					ip.resetRoi();
					ip = ip.crop();	
				}
			}
		} //end for z

		//Now that all the markers have been added, properly group them 
		//NYI: Special exclusion for multiple distinct markers falling within the radius of one large one above
		//boolean overlap_found = false;
		GroupAndRemoveRedundant(canvasID);
		
		//activeIC.repaint(); // don't need this because in action performed it already calls repaint
		//populateTxtFields(); // don't need this because in action performed it already calls populatetxtfields
	 }

//-----------------------------CRITERIA FOR SELECTING 3-D PUNCTA - Groups together 2D circles into a 3D sphere, removes groups where z=1 or z>MaxPunctaSizeinZ	
	public void GroupAndRemoveRedundant_old() {	//not being used 
		for (int n = 0; n < currentMarkerVector.size(); n++) {
			PunctaCntrMarker m1 = (PunctaCntrMarker)currentMarkerVector.get(n);
			for (int n2 = n+1; n2 < currentMarkerVector.size(); n2++) {
				PunctaCntrMarker m2 = (PunctaCntrMarker)currentMarkerVector.get(n2);
		
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
	
	public void GroupAndRemoveRedundant(int canvasID) {
		LinkedXML = 0; //this is not necessary but being cautious because of renumber(), currently function is called only by autolink2 which then calls autolink and sets linkedxml back to 1 
		renumber();
		if (canvasID == 1) {
			activeImg = img1;
			activeIC = ic1;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVA.get(n);
				for (int n2 = n+1; n2 < PCMVA.size(); n2++) {
					PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVA.get(n2);
					
					//deletes redundant markers
					if (m1.getZ() == m2.getZ() && m1.canvasID != 0 && m1.canvasID == m2.canvasID) {
						int x = m1.getX() - m2.getX();
						int y = m1.getY() - m2.getY();
						int r1 = m1.getRad();
						int r2 = m2.getRad();
						if (x*x+y*y < r1*r1 || x*x+y*y < r2*r2) {
							PCMVA.removeSingle(n2);
							n2--;
						} 
						
					//groups markers that are on different slices, but whos x and y coordinates fall within the its
					//2-dimensional radius
					} else if (m2.getZ() == m1.getZ() - 1 || m2.getZ() == m1.getZ() + 1) {
						int x = m1.getX() - m2.getX();
						int y = m1.getY() - m2.getY();
						int r1 = m1.getRad();
						int r2 = m2.getRad();
						if (x*x+y*y < r1*r1 || x*x+y*y < r2*r2) {	
							setGroupResultNum(m2, m1.resultNum);
											
							for (int n4 = n2; n4 < PCMVA.size(); n4++) {
						   	int resultNumSubj = 0;
								PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVA.get(n4);
								if (mSubj.resultNum == m1.resultNum){
									mSubj.setOwner(m1.getOwner());
								}
							}	
						}			
					}
				}
			}

			renumber();
	
			for (int n = 0; n < PCMVA.size(); n++) {
				if (((PunctaCntrMarker)PCMVA.get(n)).resultNum == 0) {
					PCMVA.removeSingle(n);
					n--;
				}
			}	
		} else {
			activeImg = img2;
			activeIC = ic2;
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVB.get(n);
				for (int n2 = n+1; n2 < currentMarkerVector.size(); n2++) {
					PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVB.get(n2);
					
					//deletes redundant markers
					if (m1.getZ() == m2.getZ() && m1.canvasID != 0 && m1.canvasID == m2.canvasID) {
						int x = m1.getX() - m2.getX();
						int y = m1.getY() - m2.getY();
						int r1 = m1.getRad();
						int r2 = m2.getRad();
						if (x*x+y*y < r1*r1 || x*x+y*y < r2*r2) {
							PCMVB.removeSingle(n2);
							n2--;
						} 
					
					//groups markers that are on different slices, but whos x and y coordinates fall within the its
					//2-dimensional radius
					} else if (m2.resultNum != m1.resultNum && (m2.getZ() == m1.getZ() - 1 || m2.getZ() == m1.getZ() + 1)) {
						int x = m1.getX() - m2.getX();
						int y = m1.getY() - m2.getY();
						int r1 = m1.getRad();
						int r2 = m2.getRad();
						
						boolean ignore = false;
						
						if (ignore != true && (x*x+y*y < r1*r1 || x*x+y*y < r2*r2)) {	
							setGroupResultNum(m2, m1.resultNum);
											
							for (int n4 = n2; n4 < currentMarkerVector.size(); n4++) {
						   	int resultNumSubj = 0;
								PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVB.get(n4);
								if (mSubj.resultNum == m1.resultNum){
									mSubj.setOwner(m1.getOwner());
								}
							}	
						}		
					}				
				}
			}
			renumber();
	
			for (int n = 0; n < PCMVB.size(); n++) {
				if (((PunctaCntrMarker)PCMVB.get(n)).resultNum == 0) {
					PCMVB.removeSingle(n);
					n--;
				}
			}
		}	
	}	

	private void removeinadequate2(int canvasID) {
		if (canvasID == 1) { //This code removes puncta when working with two images
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (mA.isOwner()) {
					int uid = mA.getOwner();
					int group_rad = 0;
					double group_max = 0;
					int group_pixels = 0;
					int zCount = 0;
			
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
						if (m.getOwner() == uid) {	
							int rad = m.getRad();
							group_rad += rad;
							int x = m.getX();
							int y = m.getY();
							int z = m.getZ();
							zCount++;
							
							if (m.getZ() > 0) {
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
					
								double max = 0;
								double v;
								int pixels = 0;
					
								for (int j = -rad; j <= rad; j++) {
									if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
									for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
										v = ip.getPixelValue(x+i, y+j);
										if (v > max) max = v;	
										pixels++;				
									}
								}
								
								group_pixels += pixels;
		
								if (max > group_max) {
									group_max = max;
								}
							}
						}	
					}
				
					//removes punctum that are on top edge or bottom edge or that do not overlap with second canvas or that are too small or too big
					//is z-shiftz < 1 correct? used to be zA
					if (group_rad/zCount <= 2 || group_max == 0 || group_pixels/zCount <= 7) {
						PCMVA.removeMarker(n);
						n--;
					} 
				}
			}
			renumber();
		} else {	
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
	
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);
			//IJ.log("2636");
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
				if (mB.isOwner()) {
					int uid = mB.getOwner();
					int group_rad = 0;
					double group_max = 0;
					int zCount = 0;
					int group_pixels = 0;
					
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
						if (m.getOwner() == uid) {	
							int rad = m.getRad();
							group_rad += rad;
							int x = m.getX();
							int y = m.getY();
							int z = m.getZ();
							zCount++;
							
							if (m.getZ() > 0) {
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
					
								double max = 0;
								double v;
								int pixels = 0;
					
								for (int j = -rad; j <= rad; j++) {
									if (endpts[j+rad][0] == -1000 || endpts[j+rad][1] == -1000) continue;
									for (int i = endpts[j+rad][0]+1; i <= endpts[j+rad][1]-1; i++) {
										v = ip.getPixelValue(x+i, y+j);
										if (v > max) max = v;
										pixels++;					
									}
								}
		
								group_pixels += pixels;
								
								if (max > group_max) {
									group_max = max;
								}
							}
						}	
					}
					
					if (group_rad/zCount <= 2 || group_max == 0 || group_pixels/zCount <= 7) {
						PCMVB.removeMarker(n);
						n--;
					} 
				}
			}
			renumber();
		}
	}		
	
	private void removeinadequate3() {
		if (compareMode) { //This code removes puncta when working with two images
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			//IJ.log("2865");
			activeImg = img1;
			activeIC = ic1;
				   
	      for (int n = 0; n < PCMVB.size(); n++)
	        		PCMVB.get(n).flags = 0;
        		
			for (int n = 0; n < PCMVA.size(); n++) {
				boolean pairing_found = false;
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				
				if (mA.isOwner()) {
					for (int n2 = 0; n2 < PCMVB.size() && !pairing_found; n2++) { 
						PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n2);
						if (!mB.isOwner()) continue;
						if (mB.resultNum == mA.resultNum) {
						   mB.flags = 1; //signifies a B was used
							pairing_found = true;		
						}
					}
					if (!pairing_found) { //found unlinked punctum in canvas A
						int zCount = 0;
						for (int n3 = n; n3 < PCMVA.size(); n3++) { 
			  				PunctaCntrMarker m = (PunctaCntrMarker)PCMVA.get(n3);			  				
							if (m.getOwner() == mA.getOwner()) {
								zCount++;	
							}
						}
						if (zCount == 1) {						
							PCMVA.removeMarker(n);
							n--;
						}
					}
				}
			}			
			//IJ.log("2899");
			activeImg = img2;
			activeIC = ic2;
			
			for (int n = 0; n < PCMVB.size(); n++) {  
			  	PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
			  	//IJ.log("n2 = "+n);
			  	if (mB.isOwner()) {
			    	if (mB.flags != 1) { //found unliked punctum in canvas B
			    		int zCount = 0;
						for (int n2 = n; n2 < PCMVB.size(); n2++) { 
			  				PunctaCntrMarker m = (PunctaCntrMarker)PCMVB.get(n2);
							if (m.getOwner() == mB.getOwner()) {
								zCount++;
							}
						}
						if (zCount == 1) {
							PCMVB.removeMarker(n);
							n--;
							//IJ.log("n2 #2 = "+n);
						}
			    	}
			  	}	        	
	      }	
	      
	      for (int n = 0; n < PCMVB.size(); n++) //AFTER being checked, reset to 0
	        		PCMVB.get(n).flags = 0;
	      //IJ.log("2922");		
		} 
	}						
	
//-----------------------------Remove Markers on Boundaries or that are too small or too big
	public void removeinadequate() {
		if (compareMode) { //This code removes puncta when working with two images
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
							PunctaCntrMarker Neighbor = (PunctaCntrMarker)PCMVA.get(nA);
							
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
						//IJ.log("ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z+" mA_z="+z2+"PunctaNeighboringDensity="+PSD95NeighboringDensity+" remove_IsolatedPuncta");
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
							PunctaCntrMarker Neighbor = (PunctaCntrMarker)PCMVB.get(nB);
							
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
			renumber();
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
							PunctaCntrMarker Neighbor = (PunctaCntrMarker)PCMVA.get(nA);
							
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
			renumber();
		}
	}
	
	
//-----------------------------Remove Markers on Edges
	public void RemoveEdges() {
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
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;
					int rad = 0;
			
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
						if (m.getOwner() == uid) {
							if (m.isOwner()) {
								resultNum = m.resultNum;
								markerImage = m.canvasID;
							}
				
							rad = m.getRad();
							int x = m.getX();
							int y = m.getY();
							int z = m.getZ();
			   			group_x += x;
							group_y += y;
							group_z += z;
							zCount++;
							ImageProcessor ip;
							
							IJ.showStatus("Processing remove edges in image one in slice "+z+"/"+activeImg.getStackSize());
				
							if (activeImg.getStackSize() == 1) {
								ip = activeImg.getProcessor();
							} else {
								ImageStack stack = activeImg.getStack();
								ip = stack.getProcessor(z);
							}
				
							ip.resetRoi();
							ip = ip.crop();
						}	
					}

					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}
					
					//removes punctum that are on edges
					if (z <= RemoveSurface || z2 >= RemoveBottom || (group_x/zCount - rad) <= RemoveLeftEdge || (group_x/zCount + rad) >= RemoveRightEdge || (group_y/zCount - rad) <= RemoveTopEdge || (group_y/zCount + rad) >= RemoveBottomEdge) {
  						PCMVA.removeMarker(n);
						n--;
		 				//IJ.log("removed edge ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z2+" rad="+rad);
					}
				}
			}
			
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);

			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
				if (mB.isOwner()) {
					int uid = mB.getOwner();
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;
					int rad = 0;
					
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
						if (m.getOwner() == uid) {
							if (m.isOwner()) {
								resultNum = m.resultNum;
								markerImage = m.canvasID;
							}
				
							rad = m.getRad();
							int x = m.getX();
							int y = m.getY();
							int z = m.getZ();
			   				group_x += x;
							group_y += y;
							group_z += z;
							zCount++;
							ImageProcessor ip;
							
							IJ.showStatus("Processing remove edges in image two in slice "+z+"/"+activeImg.getStackSize());
				
							if (activeImg.getStackSize() == 1) {
								ip = activeImg.getProcessor();
							} else {
								ImageStack stack = activeImg.getStack();
								ip = stack.getProcessor(z);
							}
				
							ip.resetRoi();
							ip = ip.crop();
						}	
					}
					
					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}
					
					//removes punctum that are on edges
					if (z <= RemoveSurface || z2 >= RemoveBottom || (group_x/zCount - rad) <= RemoveLeftEdge || (group_x/zCount + rad) >= RemoveRightEdge || (group_y/zCount - rad) <= RemoveTopEdge || (group_y/zCount + rad) >= RemoveBottomEdge) {
  						PCMVB.removeMarker(n);
						n--;
		 				//IJ.log("removed edge ID="+resultNum+" xB="+group_x/zCount+" yB="+group_y/zCount+" zB="+z2+" rad ="+rad);
					}
				}
			}						
			renumber();
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
					int group_x = 0;
					int group_y = 0;
					int group_z = 0;
					int zCount = 0;
					int resultNum = 0;
					int markerImage = 0;
					int rad = 0;
			
					ListIterator it = currentMarkerVector.listIterator();
		
					while (it.hasNext()) {
						PunctaCntrMarker m = (PunctaCntrMarker)it.next();
						if (m.getOwner() == uid) {
							if (m.isOwner()) {
								resultNum = m.resultNum;
								markerImage = m.canvasID;
							}
				
							rad = m.getRad();
							int x = m.getX();
							int y = m.getY();
							int z = m.getZ();
			   			group_x += x;
							group_y += y;
							group_z += z;
							zCount++;
							ImageProcessor ip;
							
							IJ.showStatus("Processing remove edges in image one in slice "+z+"/"+activeImg.getStackSize());
				
							if (activeImg.getStackSize() == 1) {
								ip = activeImg.getProcessor();
							} else {
								ImageStack stack = activeImg.getStack();
								ip = stack.getProcessor(z);
							}
				
							ip.resetRoi();
							ip = ip.crop();
						}	
					}

					int z = (int)(group_z/zCount);
					double z2 = (int)(z);
		
					if (zCount % 2 == 0) {
						z2 = z + 0.5;
					}
					
					//removes punctum that are on edges
					if (z <= RemoveSurface || z2 >= RemoveBottom || (group_x/zCount - rad) <= RemoveLeftEdge || (group_x/zCount + rad) >= RemoveRightEdge || (group_y/zCount - rad) <= RemoveTopEdge || (group_y/zCount + rad) >= RemoveBottomEdge) {
  						PCMVA.removeMarker(n);
						n--;
		 				//IJ.log("removed edge ID="+resultNum+" xA="+group_x/zCount+" yA="+group_y/zCount+" zA="+z2+" rad="+rad);
					}
				}
			}
			renumber();
		}
	}

//-----------------------------COMPARE AND MATCH SELECTED 3D PUNCTA BETWEEN TWO STACKS
	public void BestShiftEstimate(){
		
		PunctaCntrMarkerVector startingCMV = currentMarkerVector;
		PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
		int EstimateB = 0;
		PunctaCntrMarker bestEstimateB = null;
		
		int Shiftx2 = 0;
		int Shifty2 = 0;
		int Shiftz2 = 0;
		int Count = 0;
		
		for (int nA = 0; nA < PCMVA.size(); nA++) {
			PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(nA);
		
		   double bestShiftEstimateSqrd = (xLinkTol*xLinkTol) + (yLinkTol*yLinkTol) + (zLinkTol*zLinkTol) + 0.04;
		   
		   for (int nB = 0; nB < PCMVB.size(); nB++) { 
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(nB);
				int xA = mA.getX();
				int yA = mA.getY();
				int zA = mA.getZ();
				int xB = mB.getX();
				int yB = mB.getY();
				int zB = mB.getZ();
				float dx = (xB - (xA + Shiftx))*(MicronsX/PixelsX); 	//in microns
				float dy = (yB - (yA + Shifty))*(MicronsY/PixelsY); 	//in microns
				float dz = (zB - (zA + Shiftz))*(MicronsZ);				//in microns

				if (Math.abs(dx) <= xLinkTol && Math.abs(dy) <= yLinkTol && Math.abs(dz) <= zLinkTol) { //has to meet the minimum distance criteria
					float dSqrd = (dx*dx) + (dy*dy) + (dz*dz); //in microns
					if (dSqrd < bestShiftEstimateSqrd) { 
						bestEstimateB = mB;
						EstimateB = 1;
						bestShiftEstimateSqrd = dSqrd; //keeps resetting the criteria to be more strict to narrow down the closest punctum
					}
				}
			}
			
			int EstimateB2 = 0;
			
			if (bestEstimateB != null) {
				for (int nA2 = 0; nA2 < PCMVA.size(); nA2++) { //make sure there isn't a punctum in canvas 1 that isn't closer to the selected bestMatchB
					PunctaCntrMarker mA2 = (PunctaCntrMarker)PCMVA.get(nA2);			
					int xA2 = mA2.getX();
					int yA2 = mA2.getY();
					int zA2 = mA2.getZ();
					int xbestEstimateB = bestEstimateB.getX();
					int ybestEstimateB = bestEstimateB.getY();
					int zbestEstimateB = bestEstimateB.getZ();
					float dx2 = (xbestEstimateB - (xA2 + Shiftx))*(MicronsX/PixelsX);	//in microns
					float dy2 = (ybestEstimateB - (yA2 + Shifty))*(MicronsY/PixelsY);	//in microns
					float dz2 = (zbestEstimateB - (zA2 + Shiftz))*(MicronsZ);			//in microns	
					
					if (Math.abs(dx2) <= xLinkTol && Math.abs(dy2) <= yLinkTol && Math.abs(dz2) <= zLinkTol) { //has to meet the minimum distance criteria
						float dSqrd2 = (dx2*dx2) + (dy2*dy2) + (dz2*dz2); //in microns					
						if (dSqrd2 < bestShiftEstimateSqrd) { 
							//bestEstimateB2 = null; //there is another mA that is closer to BestMatchB
							EstimateB2 = 1;
						}
					}
				}
			}
			
			if (EstimateB == 1 && EstimateB2 == 0) { //if a matching punctum was found it links them
					Shiftx2 = bestEstimateB.getX() - mA.getX();
					Shifty2 = bestEstimateB.getY() - mA.getY();
					Shiftz2 = bestEstimateB.getZ() - mA.getZ();	
					Group_Shiftx += Shiftx2;
					Group_Shifty += Shifty2;
					Group_Shiftz += Shiftz2;
					Count++;
					//IJ.log("Shiftx = "+Shiftx2+" Shifty = "+Shifty2+" Shiftz = "+Shiftz2);
					//IJ.log("Group_Shiftx = "+Group_Shiftx+" Group_Shifty = "+Group_Shifty+" Group_Shiftz = "+Group_Shiftz);
					//IJ.log("Count = "+Count);					
					//IJ.log("mA_x = "+mA.getX()+" mA_y = "+mA.getY()+" mA_z = "+mA.getZ()+" mB_x = "+bestEstimateB.getX()+" mB_y = "+bestEstimateB.getY()+" mB_z = "+bestEstimateB.getZ());
			}
		}
		if (Count > 0) {
			Group_Shiftx = Group_Shiftx/Count;
			Group_Shifty = Group_Shifty/Count;
			Group_Shiftz = Group_Shiftz/Count;
		} else {
			Group_Shiftx = Shiftx;
			Group_Shifty = Shifty;
			Group_Shiftz = Shiftz;
		}
		//IJ.log("Avg Shiftx = "+Group_Shiftx+" Avg Shifty = "+Group_Shifty+" Avg Shiftz = "+Group_Shiftz);
		
		ShiftEstimateCalculated = 1;
	}
		
	 public void autolink(){
	 	if (LinkedXML == 0 || LinkedXML == 1) {
		 	PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
			
			int Count = 0;
			for (int nB = 0; nB < PCMVB.size(); nB++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(nB);
				mB.linkedOwner = -1;
			}	
			
			int MatchB = 0;
			PunctaCntrMarker bestMatchB = null;
			
			for (int nA = 0; nA < PCMVA.size(); nA++) {

		
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(nA); 
				mA.linkedOwner = -1;
				//IJ.log("3718 na = "+nA+" mA = "+mA.resultNum+" z = "+mA.getZ());
				double bestMatchDSqrd = (xLinkTol*xLinkTol) + (yLinkTol*yLinkTol) + (zLinkTol*zLinkTol) + 0.04; // in microns
			   //IJ.log("3722");
				if (PCMVA.getMarkerByUID(mA.getOwner()).linkedOwner != -1) continue;
				//IJ.log("3724");
				for (int nB = 0; nB < PCMVB.size(); nB++) { //finds the closest grabbed punctum in canvas 2
					PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(nB);
	
					int xA = mA.getX();
					int yA = mA.getY();
					int zA = mA.getZ();
					int xB = mB.getX();
					int yB = mB.getY();
					int zB = mB.getZ();
					
					float dx = (xB - (xA + Group_Shiftx))*(MicronsX/PixelsX); 	//in microns
					float dy = (yB - (yA + Group_Shifty))*(MicronsY/PixelsY); 	//in microns
					float dz = (zB - (zA + Group_Shiftz))*(MicronsZ);				//in microns

					if (Math.abs(dx) <= xLinkTol && Math.abs(dy) <= yLinkTol && Math.abs(dz) <= zLinkTol) { //has to meet the minimum distance criteria
						float dSqrd = (dx*dx) + (dy*dy) + (dz*dz); //in microns
						if (dSqrd < bestMatchDSqrd) { 
							bestMatchB = mB;
							MatchB = 1;
							bestMatchDSqrd = dSqrd; //keeps resetting the criteria to be more strict to narrow down the closest punctum
						}
					}
				}
				//IJ.log("3748");
				int MatchB2 = 0;
				
				if (bestMatchB != null) {
					for (int nA2 = 0; nA2 < PCMVA.size(); nA2++) { //make sure there isn't a punctum in canvas 1 that isn't closer to the selected bestMatchB
						PunctaCntrMarker mA2 = (PunctaCntrMarker)PCMVA.get(nA2);
						int xA2 = mA2.getX();
						int yA2 = mA2.getY();
						int zA2 = mA2.getZ();
						int xbestMatchB = bestMatchB.getX();
						int ybestMatchB = bestMatchB.getY();
						int zbestMatchB = bestMatchB.getZ();
						float dx2 = (xbestMatchB - (xA2 + Group_Shiftx))*(MicronsX/PixelsX);	//in microns
						float dy2 = (ybestMatchB - (yA2 + Group_Shifty))*(MicronsY/PixelsY);	//in microns
						float dz2 = (zbestMatchB - (zA2 + Group_Shiftz))*(MicronsZ);			//in microns	
						
						if (Math.abs(dx2) <= xLinkTol && Math.abs(dy2) <= yLinkTol && Math.abs(dz2) <= zLinkTol) { //has to meet the minimum distance criteria
							float dSqrd2 = (dx2*dx2)+(dy2*dy2)+(dz2*dz2); //in microns
							if (dSqrd2 < bestMatchDSqrd) { 
								MatchB2 = 1; //there is another mA that is closer to BestMatchB
							}
						}
					}
				}
				//IJ.log("3772");	
				if (MatchB == 1 && MatchB2 == 0) { //if a matching punctum was found it links them
						PCMVA.getMarkerByUID(mA.getOwner()).linkedOwner = bestMatchB.getOwner();
						PCMVB.getMarkerByUID(bestMatchB.getOwner()).linkedOwner = mA.getOwner();
						Count++;
				}		
				//IJ.log("3778");
			}
			//IJ.log("3779");
			for (int nA = 0; nA < PCMVA.size(); nA++) { //takes care of unmatched punctum in canvas 1
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(nA);
				if (mA.isOwner()) continue;
				mA.linkedOwner = PCMVA.getMarkerByUID(mA.getOwner()).linkedOwner;
			}
			for (int nB = 0; nB < PCMVB.size(); nB++) { //takes care of unmatched punctum in canvas 2
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(nB);
				if (mB.isOwner()) continue;
				mB.linkedOwner = PCMVB.getMarkerByUID(mB.getOwner()).linkedOwner;
			}
			//IJ.log("3790");
			nextResultNum = 1;
		
			for (int n = 0; n < PCMVA.size(); n++)
				((PunctaCntrMarker)PCMVA.get(n)).resultNum = 0;
				
			for (int n = 0; n < PCMVB.size(); n++)
				((PunctaCntrMarker)PCMVB.get(n)).resultNum = 0;
				
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker m = (PunctaCntrMarker)PCMVA.get(n);
				if (!m.isOwner()) continue;
				m.resultNum = nextResultNum;
				nextResultNum++;
			}
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker m = (PunctaCntrMarker)PCMVB.get(n);
				if (!m.isOwner()) continue;
				if (m.linkedOwner == -1) {
					m.resultNum = nextResultNum;
					nextResultNum++;
				} else {
					m.resultNum = (PCMVA.getMarkerByUID(m.linkedOwner)).resultNum;
				}
			}	
			
			for (int n = 0; n < PCMVA.size(); n++) {
				PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVA.get(n);
				if (!mOwn.isOwner()) continue;
				for (int n2 = 0; n2 < PCMVA.size(); n2++) {
					PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVA.get(n2);
					if (mSubj.getOwner() == mOwn.getOwner())
						mSubj.resultNum = mOwn.resultNum;
				}
			}
			
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVB.get(n);
				if (!mOwn.isOwner()) continue;
				for (int n2 = 0; n2 < PCMVB.size(); n2++) {
					PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVB.get(n2);
					if (mSubj.getOwner() == mOwn.getOwner())
						mSubj.resultNum = mOwn.resultNum;
				}
			}
		} else if (LinkedXML == 2) {
			IJ.log("cannot autolink because of manual linking");
		}
		//IJ.log("3839");
		relinkBasedOnResultNums();
		
		if (LinkedXML == 0) {
			LinkedXML = 1;
		}
	}

	public void autolink2(){
		PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
	   autolink2 = true;
		//IJ.log("3838");	  
	   //removeinadequate2(1);  JSUN SKIP THIS
	   //removeinadequate2(2);  JSUN SKIP THIS
	   //IJ.log("3841");
	   int canvasID = 2;
	   
		//compare list of type 2 to list of type 1
     		 for (int n = 0; n < PCMVB.size(); n++)
        		PCMVB.get(n).flags = 0;
		
		for (int n = 0; n < PCMVA.size(); n++) 
			{ //compares unlinked punctum in canvas A to undetected puncta in canvas B
			boolean pairing_found = false;
			PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
			if (mA.isOwner()) { 
				for (int n2 = 0; n2 < PCMVB.size() && !pairing_found; n2++) {
					PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n2);
					if (!mB.isOwner()) continue;
					if (mB.resultNum == mA.resultNum) {
					   mB.flags = 1; //signifies a B was used
						pairing_found = true;		
					}
				}
				
				if (!pairing_found) { //found unlinked punctum in canvas A
					for (int n3 = n; n3 < PCMVA.size(); n3++) { //compares unlinked punctum in canvas B to undetected puncta in canvas A 
		  				PunctaCntrMarker m = (PunctaCntrMarker)PCMVA.get(n3);
						if (m.getOwner() == mA.getOwner()) {
							int xA = m.getX();
							int yA = m.getY();
							int zA = m.getZ();
							
							activeImg = img2;
							activeIC = ic2;
							currentMarkerVector = PCMVB;

							//IJ.log("mA = "+mA.resultNum+"x = "+xA+" y = "+yA+" z = "+zA);
							if (zA > 0){
								MeasureByClick_new(xA,yA,zA,canvasID); //try to detect punctum in canvas B for same location as unlinked punctum in canvas A						  
							}
						}
					}
				}
			}
		}		
		
		//IJ.log("3882");
		GroupAndRemoveRedundant(canvasID);
		//IJ.log("3884");
		removeinadequate2(canvasID);
		//IJ.log("3886");
		autolink();

		canvasID = 1;

		for (int n = 0; n < PCMVB.size(); n++) { //compares unlinked punctum in canvas B to undetected puncta in canvas A 
		  	PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
		  	if (mB.isOwner()) {
		    	if (mB.flags != 1) { //found unliked punctum in canvas B
					for (int n2 = n; n2 < PCMVB.size(); n2++) { //compares unlinked punctum in canvas B to undetected puncta in canvas A 
		  				PunctaCntrMarker m = (PunctaCntrMarker)PCMVB.get(n2);
						if (m.getOwner() == mB.getOwner()) {
							int xB = m.getX();
							int yB = m.getY();
							int zB = m.getZ();
							
							activeImg = img1;
							activeIC = ic1;
							currentMarkerVector = PCMVA;
							
							//IJ.log("id mb = "+m.resultNum+" x = "+m.getX()+" y = "+m.getY()+" z = "+m.getZ());
							if (zB > 0){
								MeasureByClick_new(xB,yB,zB,canvasID); //try to detect punctum in canvas B for same location as unlinked punctum in canvas A					  
							}						
						}
					}
		    	}
		  	}
      }
      
      for (int n = 0; n < PCMVB.size(); n++) //AFTER being checked, reset to 0
        		PCMVB.get(n).flags = 0;
      
		//IJ.log("3916");
		GroupAndRemoveRedundant(canvasID); //if sequential with z plane puncta do only remove and use autogroupmarker?		//JSUN: skip removing for image1			
		//IJ.log("3934");	
		removeinadequate2(canvasID); 
		//IJ.log("3936");
		autolink(); 
		//IJ.log("3920");
		//removeinadequate3(); //JSUN: this causes the issue that the same day might have different puncta numbers depending on different paring images over different days, skip it 
		//autolink(); 
		//IJ.log("3923");
		autoGroupMarker(1);
		autoGroupMarker(2);
		//IJ.log("3926");
		autolink();

		autolink2 = false;
	}							
			
//-----------------------------GET PUNCTA MEASUREMENTS FOR 1 OR 2 STACKS   
	public void renumber() { 
		if (LinkedXML == 0) {	
			if (compareMode) {	
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);

				activeImg = img1;
				activeIC = ic1;
				
				//start of numbering puncta 
				nextResultNum = 1;
	
				for (int n = 0; n < PCMVA.size(); n++) 
					((PunctaCntrMarker)PCMVA.get(n)).resultNum = 0;
				
				for (int n = 0; n < PCMVA.size(); n++) {
					PunctaCntrMarker m = (PunctaCntrMarker)PCMVA.get(n);
					if (!m.isOwner()) continue;
					m.resultNum = nextResultNum;
					nextResultNum++;
				}
				
				for (int n = 0; n < PCMVA.size(); n++) {
					PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVA.get(n);
					if (!mOwn.isOwner()) continue;
					for (int n2 = 0; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVA.get(n2);
						if (mSubj.getOwner() == mOwn.getOwner())
							mSubj.resultNum = mOwn.resultNum;
					}
				}
				//end of numbering puncta
				
				PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
				activeImg = img2;
				activeIC = ic2;
				
				//start of numbering puncta 
				nextResultNum = 1;
	
				for (int n = 0; n < PCMVB.size(); n++) 
					((PunctaCntrMarker)PCMVB.get(n)).resultNum = 0;
				
				for (int n = 0; n < PCMVB.size(); n++) {
					PunctaCntrMarker m = (PunctaCntrMarker)PCMVB.get(n);
					if (!m.isOwner()) continue;
					m.resultNum = nextResultNum;
					nextResultNum++;
				}
								
				for (int n = 0; n < PCMVB.size(); n++) {
					PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVB.get(n);
					if (!mOwn.isOwner()) continue;
					for (int n2 = 0; n2 < PCMVB.size(); n2++) {
						PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVB.get(n2);
						if (mSubj.getOwner() == mOwn.getOwner())
							mSubj.resultNum = mOwn.resultNum;
					}
				}
				//end of numbering puncta		
			} else {
				PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
				
				activeImg = img1;
				activeIC = ic1;
	
				//start of numbering puncta 
				nextResultNum = 1;
	
				for (int n = 0; n < PCMVA.size(); n++) 
					((PunctaCntrMarker)PCMVA.get(n)).resultNum = 0;
				
				for (int n = 0; n < PCMVA.size(); n++) {
					PunctaCntrMarker m = (PunctaCntrMarker)PCMVA.get(n);
					if (!m.isOwner()) continue;
					m.resultNum = nextResultNum;
					nextResultNum++;
				}
				
				for (int n = 0; n < PCMVA.size(); n++) {
					PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVA.get(n);
					if (!mOwn.isOwner()) continue;
					for (int n2 = 0; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVA.get(n2);
						if (mSubj.getOwner() == mOwn.getOwner())
							mSubj.resultNum = mOwn.resultNum;
					}
				}
				//end of numbering puncta
			}
		}
	}


	public void measure() {
		if (compareMode) { //This code measures puncta when working with two images; note: autolink already removes puncta in canvas 1 and 2 that are on edges of canvas boundaries
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);	
					
			if (LinkedXML == 0) {
				renumber();
			} 
			
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
			
			if (LinkedXML == 1 || LinkedXML == 2) {
	         relinkBasedOnResultNums();			
				
				//compare list of type 2 to list of type 1
				int nochange = 0;
				int gained2 = 0;
				int lost2 = 0;
				
	         	for (int n2 = 0; n2 < PCMVB.size(); n2++)
	           		PCMVB.get(n2).flags = 0;
				
				for (int n = 0; n < PCMVA.size(); n++) {
					boolean pairing_found = false;
					PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
					if (mA.isOwner()) { 
						for (int n2 = 0; n2 < PCMVB.size() && !pairing_found; n2++) {
							PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n2);
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
	         nochange = 2*nochange;
	         	//IJ.log("No change = "+nochange+" Gained = "+gained2+" Lost = "+lost2);
         }
			currentMarkerVector = startingCMV;
		} else { //This code measures puncta when working with one image
			PunctaCntrMarkerVector startingCMV = currentMarkerVector;
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);

			renumber();

			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);

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
		int group_rad = 0;
		int group_x = 0;
		int group_y = 0;
		int group_z = 0;
		int zCount = 0;
		int resultNum = 0;
		int markerImage = 0;
		int maxpixelarea = 0;
		
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
						if (v > max) {
							max = v;
						}
						pixels++;						
					}
				}
				
				if (pixels > 0 && pixels > maxpixelarea) {
					maxpixelarea = pixels;
				}				
				
				if (pixels > 0) { //this can happen if you load markers with a different arbitraryboundarycutoff. also measurebyclick seems to add sections that have pixels = 0 because they are not circles, but rather lines of rad>2
					group_rad += rad;
			   	group_x += x;
					group_y += y;
					group_z += z;
					
					zCount++;
							
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
					
					if (max > group_max) {
						group_max = max;
					}
				}
			}
		}
		
		//get average signal intensity around punctum for all the slices it resides or 3 slices if it resides in < 3 slices
		//assumption is that grid around punctum is large enough to not be affected by the changes in red signal intensity where the punctum is located 
		
		/*
		if (activeImg == img1) {
			ImageProcessor ip;
				
			signalAroundPunctum_avg1 = new double[activeImg.getStackSize()+1];
			signalAroundPunctum_stddev1 = new double[activeImg.getStackSize()+1];			
					
			double count = 0;
			double total_avg = 0;
			double total_stddev = 0;
			int rad = group_rad/zCount;
			int x = group_x/zCount;
			int y = group_y/zCount;
			int zCount2 = zCount;
			
			if (zCount2 < 3) {
				zCount2 = 3;
			}
	
			for (int bz = z; bz <= z+zCount2; bz++) {	
				ImageStack stack = activeImg.getStack();
				ip = stack.getProcessor(bz);
			   ip.resetRoi();
			   ip = ip.crop();
				MeasureData MDsignalAroundPunctum = findSignalLevelAroundPunctum(ip,x,y);
				signalAroundPunctum_avg1[bz] = MDsignalAroundPunctum.avg;
				signalAroundPunctum_stddev1[bz] = MDsignalAroundPunctum.stddev;	
				total_avg += signalAroundPunctum_avg1[bz];
				total_stddev += signalAroundPunctum_stddev1[bz];
				count++;		
			}
	
			signalAroundPunctum_avg1[z] = (total_avg)/(count);
			signalAroundPunctum_stddev1[z] = (total_stddev)/(count);
		} else {
			ImageProcessor ip;
				
			signalAroundPunctum_avg2 = new double[activeImg.getStackSize()+1];
			signalAroundPunctum_stddev2 = new double[activeImg.getStackSize()+1];			
					
			double count = 0;
			double total_avg = 0;
			double total_stddev = 0;
			int rad = group_rad/zCount;
			int x = group_x/zCount;
			int y = group_y/zCount;
			int zCount2 = zCount;
			
			if (zCount2 < 3) {
				zCount2 = 3;
			}
	
			for (int bz = z; bz <= z+zCount2; bz++) {	
				ImageStack stack = activeImg.getStack();
				ip = stack.getProcessor(bz);
			   ip.resetRoi();
			   ip = ip.crop();
				MeasureData MDsignalAroundPunctum2 = findSignalLevelAroundPunctum2(ip,x,y);
				signalAroundPunctum_avg2[bz] = MDsignalAroundPunctum2.avg;
				signalAroundPunctum_stddev2[bz] = MDsignalAroundPunctum2.stddev;	
				total_avg += signalAroundPunctum_avg2[bz];
				total_stddev += signalAroundPunctum_stddev2[bz];
				count++;		
			}
	
			signalAroundPunctum_avg2[z] = (total_avg)/(count);
			signalAroundPunctum_stddev2[z] = (total_stddev)/(count);
			
			measureImg2 = true;
		}						
			*/	
			
		rt.incrementCounter();

		rt.addValue("ID",resultNum);
		rt.addValue("Image",markerImage);
			
		if (zCount > 0) {
			int z = (int)(group_z/zCount);
			double z2 = (int)(z);
			
			if (zCount % 2 == 0){
				z2 = z + 0.5;
			}	
			
			double radius = group_rad/zCount;
			double avgpixelintensity = group_intensity/group_pixels;
			double avgpixelarea = group_pixels/zCount;			
			
			rt.addValue("X", group_x/zCount);
			rt.addValue("Y", group_y/zCount);
			rt.addValue("Z", z2);
			rt.addValue("Radius", radius);
			rt.addValue("Intensity Max",group_max); //in pixels, conduct gaussian blur at radius 0.8 microns
			rt.addValue("Intensity Total",group_intensity); //in pixels
			rt.addValue("Intensity Avg",avgpixelintensity); //in pixels, this can be a problem since for an identical punctum you might select an extra z-section which has more very faint pixels thus bringing down the average pixel intensity signifcantly
			rt.addValue("Intensity Stddev",group_stddev);
			rt.addValue("Max Area",maxpixelarea); //in pixels
			rt.addValue("Avg Area",avgpixelarea); //in pixels, again this can be affected by the number of z-sections the boundary covers
			rt.addValue("Volume",group_pixels); //in pixels, again this can be affected by the number of z-sections the boundary covers											
			//rt.addValue("Volume in Microns",group_pixels*(MicronsX/PixelsX*MicronsY/PixelsY*MicronsZ)); //Micronz Z already has unit um/slice
			rt.addValue("# of Slices",zCount);
			if (z <= img1.getStackSize()) {
			rt.addValue("Noise Avg v0"+GorRXML,bkgrnd_avg1[z]); //avg background noise per slice
			rt.addValue("Noise Stddev v0"+GorRXML,bkgrnd_stddev1[z]);
			}
			if (measureImg2) {	
			if (z <= img2.getStackSize()) {
				rt.addValue("Noise Avg v0"+GorRXML,bkgrnd_avg2[z]);  //avg background noise per slice
				rt.addValue("Noise Stddev v0"+GorRXML,bkgrnd_stddev2[z]);	
				}
			}
			if (z <= img1.getStackSize()) {
			rt.addValue("Signal Value v0"+GorRXML,signal_signal1[z]);
			rt.addValue("Signal Median v0"+GorRXML,signal_median1[z]);
			rt.addValue("Signal Max v0"+GorRXML,signal_max1[z]);
			rt.addValue("Signal Min v0"+GorRXML,signal_min1[z]);
			rt.addValue("Signal Avg v0"+GorRXML,signal_avg1[z]);
			rt.addValue("Signal Stddev v0"+GorRXML,signal_stddev1[z]);
			}
			if (measureImg2) {	
			if (z <= img2.getStackSize()) {
				rt.addValue("Signal Value v0"+GorRXML,signal_signal2[z]);
				rt.addValue("Signal Median v0"+GorRXML,signal_median2[z]);
				rt.addValue("Signal Max v0"+GorRXML,signal_max2[z]);
				rt.addValue("Signal Min v0"+GorRXML,signal_min2[z]);
				rt.addValue("Signal Avg v0"+GorRXML,signal_avg2[z]);
				rt.addValue("Signal Stddev v0"+GorRXML,signal_stddev2[z]);
				}
			}
			/*		
			rt.addValue("Img1 Signal Around Punctum Average",signalAroundPunctum_avg1[z]);
			rt.addValue("Img1 Signal Around Punctum Stddev",signalAroundPunctum_stddev1[z]);
			if (measureImg2) {	
				rt.addValue("Img2 Signal Around Punctum Average",signalAroundPunctum_avg2[z]);
				rt.addValue("Img2 Signal Around Punctum Stddev",signalAroundPunctum_stddev2[z]);	
			}		
			*/
			int changeCode = 0;
			if (marker.linkedOwner > -1) changeCode = 0;
			else if (marker.canvasID == 1) changeCode = -1;
			else changeCode = 1;
	      	rt.addValue("Change",changeCode);
      }	
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
	
//-----------------------------SHOW RESULTS TABLE
	 public void report(){
		if (results_initialized)
			rt.show("Results");
			String path = myDirectory +"Day"+DayXML1S+"Day"+DayXML2S+fileSeparator+img2.getTitle()+"_Day"+DayXML1S+".xls";
            String path2 = myDirectory +"Day"+DayXML1S+"Day"+DayXML2S+fileSeparator+img2.getTitle()+"_Day"+DayXML1S+".txt";
			try {
				rt.saveAs(path);
                rt.saveAs(path2);
			} catch (IOException e) {
				IJ.error(""+e);
			}
			WindowManager.closeAllWindows();
    }
	
		/* Updates the Results window. */
	/*public void updateResults() {
		TextPanel textPanel = IJ.getTextPanel();
		if (textPanel!=null) {
			textPanel.updateColumnHeadings(getColumnHeadings());		
			textPanel.updateDisplay();
		}
	}*/

	
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
        //if (activeIC!=null)
          //  activeIC.repaint();
    }

//-----------------------------MANUAL UNLINK AND RELINK
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
    
   public void autoGroupMarker(int canvasID) {
   //currently autogroupmarker is the same for psd95 and synt 10/19/2011 
		if (PSD95orSynt == 1) {
   		if (canvasID == 1) { 
			   IJ.showStatus("Processing autogroup in image "+canvasID);
			   
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
					PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					//what happens when there are none that maintain same resultnum? 
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = (PunctaCntrMarker)PCMVA.get(n3);
								
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
							PunctaCntrMarker m10 = (PunctaCntrMarker)PCMVA.get(n10);
							
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
						PunctaCntrMarker m4 = (PunctaCntrMarker)PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVA.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = (PunctaCntrMarker)PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVA.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVA.get(n7);
	
									if (m1next.resultNum != m4.resultNum) {
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
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVA.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVA.get(n7);
	
									if (m1next.resultNum != m4.resultNum) {
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
				}
			} else {
				IJ.showStatus("Processing autogroup in image "+canvasID);
				PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
									
				activeImg = img2;
				activeIC = ic2;
				currentMarkerVector = typeVector.get(1);
			 
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
			 
				for (int n1 = 0; n1 < PCMVB.size(); n1++) {
					PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVB.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVB.size(); n2++) {
						PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVB.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVB.size(); n3++) {
								PunctaCntrMarker m3 = (PunctaCntrMarker)PCMVB.get(n3);
								
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
							PunctaCntrMarker m10 = (PunctaCntrMarker)PCMVB.get(n10);
							
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
										
					for (int n4 = n1; n4 < PCMVB.size(); n4++) { //it can probably be n1+1 
						PunctaCntrMarker m4 = (PunctaCntrMarker)PCMVB.get(n4);			
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVB.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true; 
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																				
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
						
								PunctaCntrMarker m9 = (PunctaCntrMarker)PCMVB.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVB.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVB.get(n7);
									
									if (m1next.resultNum != m4.resultNum) {
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
							
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVB.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVB.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVB.get(n7);
									
									if (m1next.resultNum != m4.resultNum) {
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
				} 	
			} 			
		} else { 
			if (canvasID == 1) {
				IJ.showStatus("Processing autogroup in image "+canvasID);
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
					PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVA.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					//what happens when there are none that maintain same resultnum? 
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVA.size(); n2++) {
						PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVA.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVA.size(); n3++) {
								PunctaCntrMarker m3 = (PunctaCntrMarker)PCMVA.get(n3);
								
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
							PunctaCntrMarker m10 = (PunctaCntrMarker)PCMVA.get(n10);
							
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
						PunctaCntrMarker m4 = (PunctaCntrMarker)PCMVA.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVA.get(n5);
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;	
									//IJ.log("m5 = "+m5.resultNum+" m4 = "+m4.resultNum);																				
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = (PunctaCntrMarker)PCMVA.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
									//IJ.log("m9 = "+m9.resultNum+" m1 = "+m1.resultNum);
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVA.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVA.get(n7);
	
									if (m1next.resultNum != m4.resultNum) {
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
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVA.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVA.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVA.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVA.get(n7);
	
									if (m1next.resultNum != m4.resultNum) {
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
				}
			} else {
				IJ.showStatus("Processing autogroup in image "+canvasID);
				PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);	
				
				activeImg = img2;
				activeIC = ic2;
				currentMarkerVector = typeVector.get(1);
								
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
			 
				for (int n1 = 0; n1 < PCMVB.size(); n1++) {
					PunctaCntrMarker m1 = (PunctaCntrMarker)PCMVB.get(n1);
			
					if (currentZ != m1.getZ()) {
						MaxMovementXNeg = 0;
						MaxMovementYNeg = 0;
						MaxMovementXPos = 0;
						MaxMovementYPos = 0;
					}
					
					//find MaxMovementX and MaxMovementY among all punctum across two consecutive z planes
					for (int n2 = n1; n2 < PCMVB.size(); n2++) {
						PunctaCntrMarker m2 = (PunctaCntrMarker)PCMVB.get(n2);

						if (m2.getZ() == m1.getZ()) { 
							for (int n3 = n2; n3 < PCMVB.size(); n3++) {
								PunctaCntrMarker m3 = (PunctaCntrMarker)PCMVB.get(n3);
								
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
							PunctaCntrMarker m10 = (PunctaCntrMarker)PCMVB.get(n10);
							
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
						PunctaCntrMarker m4 = (PunctaCntrMarker)PCMVB.get(n4);
						
						//for those 2D punctum that are considered separate, evaluate again following movement calculation
						if (m4.resultNum != m1.resultNum && m4.getZ() == (m1.getZ() + 1) && (MaxMovementXPos != 0 || MaxMovementYPos != 0 || MaxMovementXNeg != 0 || MaxMovementYNeg != 0)) {
							for (int n5 = 0; n5 < n4; n5++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 1) 
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVB.get(n5);

								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}						
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) {
									temp1 = 1;																					
								}
							}
							
							for (int n9 = 0; n9 < n1; n9++) { //makes sure that the potential grouping is not with a puncta that already spans the same z plane (distance 2)
								PunctaCntrMarker m9 = (PunctaCntrMarker)PCMVB.get(n9);
								if (m9.resultNum == m1.resultNum && m9.getZ() == (m1.getZ() - 1)) {
									temp2 = 1; 
								}	
							}
							
							if (ignore != true && temp1 == 1 && temp2 == 1) {
								ignore = true;
							}	
														
							if (ignore != true) {
								for (int n6 = n1; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVB.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVB.get(n7);
	
									if (m1next.resultNum != m4.resultNum) {
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
								PunctaCntrMarker m5 = (PunctaCntrMarker)PCMVB.get(n5);
							
								if (m5.resultNum == m4.resultNum && m5.getZ() == m1.getZ()) {
									ignore = true;
								}
								
								if (m5.resultNum == m4.resultNum && m5.getZ() == (m1.getZ() - 1)) { //was used to prevent multiple of same number 
									temp3 = 1;
								}	
							}
							
							if (ignore != true) {
								for (int n6 = 0; n6 < PCMVB.size(); n6++) { //makes sure that the potential grouping is not done if puncta already spans z plane
									PunctaCntrMarker m6 = (PunctaCntrMarker)PCMVB.get(n6);
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
									PunctaCntrMarker m1next = (PunctaCntrMarker)PCMVB.get(n7);
									
									if (m1next.resultNum != m4.resultNum) {
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
				PunctaCntrMarker mA = (PunctaCntrMarker)PCMVA.get(n);
				if (!mA.isOwner()) continue;
				nextResultNum++;
			}	
			for (int n = 0; n < PCMVB.size(); n++) {
				PunctaCntrMarker mB = (PunctaCntrMarker)PCMVB.get(n);
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
			PunctaCntrMarkerVector PCMVA = (PunctaCntrMarkerVector)typeVector.get(0);
		
			activeImg = img1;
			activeIC = ic1;
			currentMarkerVector = typeVector.get(0);
			
			for (int n = 0; n < PCMVA.size(); n++) {
			   int resultNumOwn = 0;
				PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVA.get(n);
				if (!mOwn.isOwner()) continue;
				resultNumOwn = mOwn.resultNum;
				for (int n2 = 0; n2 < PCMVA.size(); n2++) {
				   int resultNumSubj = 0;
					PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVA.get(n2);
					resultNumSubj = mSubj.resultNum;
					if (resultNumSubj == resultNumOwn){
						mSubj.setOwner(mOwn.getOwner());
					}
				}
			}
		} else {
			PunctaCntrMarkerVector PCMVB = (PunctaCntrMarkerVector)typeVector.get(1);
		
			activeImg = img2;
			activeIC = ic2;
			currentMarkerVector = typeVector.get(1);
			
			for (int n = 0; n < PCMVB.size(); n++) {
			   int resultNumOwn = 0;
				PunctaCntrMarker mOwn = (PunctaCntrMarker)PCMVB.get(n);
				if (!mOwn.isOwner()) continue;
				resultNumOwn = mOwn.resultNum;
				for (int n2 = 0; n2 < PCMVB.size(); n2++) {
			   	int resultNumSubj = 0;
					PunctaCntrMarker mSubj = (PunctaCntrMarker)PCMVB.get(n2);
					resultNumSubj = mSubj.resultNum;
					if (resultNumSubj == resultNumOwn){
						mSubj.setOwner(mOwn.getOwner());
					}
				}
			}
		} 
	}
    
    public void unlinkMarker(PunctaCntrMarker m) {
      setGroupResultNum(m, -1);
	   setGroupLinkedOwner(m, -1);
	   LinkedXML = 2;
    }
         
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
      relinkBasedOnResultNums();
      ic1.repaint();
		ic2.repaint();
		LinkedXML = 2;
    }
}
