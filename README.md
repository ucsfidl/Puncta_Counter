# ImageJ Plugin Puncta_Counter documentation

Puncta_Counter is ImageJ Plugin an imaging processing pipeline which includes a main GUI and 4 customizable modules:

-  Main visualization GUI: Puncta_Counter                  
-  1.Puncta detection: Puncta_CounterAutodetect
-  2.Puncta 3D group: Puncta_CounterAutoGroup         
-  3.Removal of inadequate puncta: Puncta_CounterRemoveInadequate   
-  4.Link puncta over different images: Puncta_CounterAutolink          

* Installation
  Please download Java 6 ImageJ (2013 July 15 version) so you can complie the customizable plugin: https://imagej.net/Fiji/Downloads 

* Image Registration
  We recommend using CMTK toolkit to register images to correct misalignment before running puncta detection, more inforatmion about the toolkit and downloadable link: https://www.nitrc.org/projects/cmtkto 

* Run Plugins
  1. Clone/download the whole package and put everything in Fiji \ plugin folder
  2. Everything is complied and ready to run, so start Fiji, under [Plugins] tab, there should be Puncta_Counter and other options. You can also custermize each plugins to suit sepecifc need. More details in next section.
  3. Run Puncta_CounterAutodetect,  Puncta_CounterAutoGroup, Puncta_CounterRemoveInadequate for each image.
  4. Run Puncta_CounterAutolink to link over different images (currently it's set to batch progess all corresponding images in 2 designated folders)
  
* Customizable Plugins 
  1. In each subfolder, there is one java file PunctaCounter*_copy for the specific module *. You can custermize by changing the working folder, the size of processing image, the intensity and size threshold for the detected puncta, etc. After making the changes, you should rename the files to PunctaCounter* by removing the '_copy' and move one level to replace the working ones in the main folder. 
  2. Start Fiji, under [Plugins] tab, use [Compile and Run] to compile the specific module by running Puncta_Counter * . If you make changes to the main GUI Puncta_Counter, it's recommended to complile it first, since other functions (although unlikely) might be affected. 
  3. Restart Fiji and it should work using your custermized parameters. Know more about plugins and compilation at https://imagej.nih.gov/ij/docs/menus/plugins.html and https://imagejdocu.tudor.lu/howto/plugins/how_to_install_a_plugin
