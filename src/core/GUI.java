package core;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.SystemColor;			// Krybo (2014-09-17)
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;

import static core.VergeEngine.*;
import core.JVCL;
import static core.Controls.KeyF6;
import static core.Controls.clearKey;
import static core.Script.*;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import domain.Map;

public class GUI extends JFrame implements ActionListener, ItemListener, ComponentListener 
	{
	private static final long serialVersionUID = 5487850478802622502L;
	Canvas canvas = new Canvas();
	Controls control = new Controls();
	static VergeEngine gameThread; 
	static BufferStrategy strategy;
	
	private int winwidth, winheight;
	private static int curwidth;
	private static int curheight;
	boolean win_decoration = false;
	private static float alpha = 1f;

	public static long cycleTime;
	private static int frameDelay = 20; // 20ms. implies 50fps (1000/20) = 50
	private static byte GUIzoom = 1;
	private static boolean showFPS = true;

	JMenuBar menuBar;
	private JCheckBoxMenuItem cbMenuItemSound;
	private JCheckBoxMenuItem cbMenuItemFullScreen;
	private JCheckBoxMenuItem cbMenuItemDoubleScreen;
	private JCheckBoxMenuItem cbMenuItemshowFPS;
	private JMenuItem menuItemIncreaseFPS;
	private JMenuItem menuItemDecreaseFPS;
	private JMenuItem menuItemTerminate;	// Krybo
	private JMenuItem menuItemZoomIn;		// Krybo
	private JMenuItem menuItemZoomOut;	// Krybo

	
	
	public GUI(int w, int h) {
		// build and display your GUI

		addComponentListener(this);
		VergeEngine.gui = this;
		
		canvas.setBackground(Color.black);
		control.clearKeymap();
		canvas.addMouseListener(control);
		canvas.addMouseMotionListener(control);
		canvas.addFocusListener(control);
		canvas.addKeyListener(control);
		
		
		/* Krybo (2014-09-17) : Since I dispise ALL white backgrounds
		  mine shall now be to my prefs., while most others will stay white
		  needs tested on systems other than windows  */
		try {
			// Initialize the windows-level menu
		UIManager.put("Menu.foreground", 
				java.awt.SystemColor.menuText );
		UIManager.put("Menu.background", 
				java.awt.SystemColor.menu );
		UIManager.put("Menu.selectionForeground", 
				java.awt.SystemColor.textHighlightText );
		UIManager.put("Menu.selectionBackground", 
				java.awt.SystemColor.textHighlight );
		
		UIManager.put("MenuItem.foreground", 
				java.awt.SystemColor.menuText );
		UIManager.put("MenuItem.background", 
				java.awt.SystemColor.menu );
		UIManager.put("MenuItem.selectionForeground", 
				java.awt.SystemColor.textHighlightText );
		UIManager.put("MenuItem.selectionBackground", 
				java.awt.SystemColor.textHighlight );
		
		UIManager.put("MenuBar.foreground", 
				java.awt.SystemColor.menuText );
		UIManager.put("MenuBar.background", 
			java.awt.SystemColor.menu );
		UIManager.put("MenuBar.selectionForeground", 
				java.awt.SystemColor.textHighlightText );
		UIManager.put("MenuBar.selectionBackground", 
				java.awt.SystemColor.textHighlight );
		
		UIManager.put("CheckBoxMenuItem.foreground", 
				java.awt.SystemColor.menuText );
		UIManager.put("CheckBoxMenuItem.background", 
				java.awt.SystemColor.menu );
		UIManager.put("CheckBoxMenuItem.selectionForeground", 
				java.awt.SystemColor.textHighlightText );
		UIManager.put("CheckBoxMenuItem.selectionBackground", 
				java.awt.SystemColor.textHighlight );

		} catch (Exception e) { }  // Whatever ~ noone will care.
		// 	END Krybo (2014-09-17)
	
		
		// Menus
		menuBar = new JMenuBar();
		JMenu menu = new JMenu("Settings");
		menuBar.add(menu);
		
		cbMenuItemSound = new JCheckBoxMenuItem("Enable Sound", true);
		cbMenuItemSound.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, ActionEvent.CTRL_MASK));
		cbMenuItemSound.addItemListener(this);
		menu.add(cbMenuItemSound);
		
		cbMenuItemFullScreen = new JCheckBoxMenuItem("Full Screen mode", !config.isWindowmode());
		cbMenuItemFullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, ActionEvent.CTRL_MASK));
		cbMenuItemFullScreen.addItemListener(this);
		menu.add(cbMenuItemFullScreen);

		cbMenuItemDoubleScreen = new JCheckBoxMenuItem("Double Screen mode", config.isDoubleWindowmode());
		cbMenuItemDoubleScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, ActionEvent.CTRL_MASK));
		cbMenuItemDoubleScreen.addItemListener(this);
		menu.add(cbMenuItemDoubleScreen);

		cbMenuItemshowFPS = new JCheckBoxMenuItem("Show FPS", true);
		cbMenuItemshowFPS.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, ActionEvent.CTRL_MASK));
		cbMenuItemshowFPS.addItemListener(this);
		menu.add(cbMenuItemshowFPS);
		
		menuItemDecreaseFPS = new JMenuItem("Decrease FPS");
		menuItemDecreaseFPS.setActionCommand("decreaseFPS");
		menuItemDecreaseFPS.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, ActionEvent.CTRL_MASK));
		menuItemDecreaseFPS.addActionListener(this);
		menu.add(menuItemDecreaseFPS);

		menuItemIncreaseFPS = new JMenuItem("Increase FPS");
		menuItemIncreaseFPS.setActionCommand("increaseFPS");
		menuItemIncreaseFPS.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, ActionEvent.CTRL_MASK));
		menuItemIncreaseFPS.addActionListener(this);
		menu.add(menuItemIncreaseFPS);
		
			// Krybo (2014-09-17   Zoom and Exit GUI functions
			//     zooming is done at the very last moment possible
			//     before the render hits the final destination.  It can
			// 	only be in solid integers, cause scaling may be too burndensome
		
		menuItemZoomIn = new JMenuItem("Zoom-In Map");
		menuItemZoomIn.setActionCommand("MapZoomIn");
		menuItemZoomIn.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_UP, ActionEvent.SHIFT_MASK ) );
		menuItemZoomIn.addActionListener( this );
		menu.add(menuItemZoomIn);
		
		menuItemZoomOut = new JMenuItem("Zoom-Out Map");
		menuItemZoomOut.setActionCommand("MapZoomOut");
		menuItemZoomOut.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_DOWN, ActionEvent.SHIFT_MASK  ));
		menuItemZoomOut.addActionListener( this );
		menu.add( menuItemZoomOut );
		
		menuItemTerminate = new JMenuItem("Exit");
		menuItemTerminate.setActionCommand("TerminateFromGUI");
		menuItemTerminate.setAccelerator(KeyStroke.getKeyStroke(
					KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		menuItemTerminate.addActionListener( this );
		menu.add(menuItemTerminate);
		
				// END Krybo edits.
		
		this.setJMenuBar(menuBar);

		this.add(canvas);
		if(!config.isDoubleWindowmode()) {
			setDimensions(this, w, h);
		} else {
			setDimensions(this, w*2, h*2);
		}
		this.addWindowListener(control);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		cycleTime = System.currentTimeMillis();
			
		System.out.println("GUI Initialized.");
		gameThread = new VergeEngine();
		gameThread.setPriority(Thread.MIN_PRIORITY);
		gameThread.start(); // start Game processing.

	}
	
	void setDimensions(GUI gui, int w, int h) {

		setVisible(false);
		dispose();

		if (w==0) { // Full screen
			Dimension scrsize = Toolkit.getDefaultToolkit().getScreenSize();
			winwidth = scrsize.width;
			winheight = scrsize.height;
			win_decoration=false;
			menuBar.setPreferredSize(new java.awt.Dimension());
			
			this.setUndecorated(true);
			this.setResizable(false);
			this.setSize(winwidth, winheight);
			//try {
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(gui);
			//} finally {
			//	GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
			//}
			
		} else { // Window mode
			winwidth = w;
			winheight = h;
			win_decoration=true;
			menuBar.setPreferredSize(null);

			// setting the size of the canvas or applet has no effect
			// we need to add the height of the title bar to the height
			// We use the insets now. Originally, we used:
			// 24 is the empirically determined height in WinXP
			// 48 enables us to have the whole window with title bar on-screen
			// 8 is the empirically determined width in win and linux
			
			//[Rafael, the Esper] Does not work
			//Insets insets = super.getInsets();
			//super.setSize(winwidth+insets.left+insets.right, winheight+insets.top+insets.bottom);
			//super.setSize(winwidth, winheight);
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
			this.setUndecorated(false);
			this.setResizable(true);
			this.setVisible(true);
			System.out.println("Winwidth: " + winwidth + ", Winheight: " + winheight + " I: " + super.getInsets());

			this.setSize(winwidth+super.getInsets().left+super.getInsets().right,
					winheight+super.getInsets().top+super.getInsets().bottom+menuBar.getHeight());
			System.out.println(super.getBounds());
		}

		this.setVisible(true);
		
		canvas.requestFocus();
		
		/*try {
			this.createBufferStrategy(2, new BufferCapabilities(new ImageCapabilities(true), new ImageCapabilities(true), FlipContents.UNDEFINED));
			} catch (AWTException e){
			// flip is unavailable, create the best you have
				this.createBufferStrategy(2);
			}*/		

		canvas.createBufferStrategy(2);
		strategy = this.canvas.getBufferStrategy();
	}

		// Krybo (2014-09-20)  Does the work for map zooming.
	public static void processZoom()
		{
		try {
			ZoomScreenSubset.blackOut();
			
			Integer xCenter = screenHalfWidth;		
			Integer yCenter = screenHalfHeight;
			Integer pX = screenHalfWidth + 1;
			Integer pY = screenHalfHeight + 1;
			Integer offX = 0;
			Integer offY = 0;
			Integer sectionDeltaX = Math.floorDiv(
					screenHalfWidth, GUIzoom );
			Integer sectionDeltaY = Math.floorDiv(
					screenHalfHeight, GUIzoom );
			
				//  The offset helps keep the view on the char as 
				//             they approach the edge of the screen.
	
			if( playerIsSet() )
				{
				pX = playerGetMapPixelX();
				pY = playerGetMapPixelY();
				if( pX < screenHalfWidth )
					{  offX = (Integer) (screenHalfWidth - pX); }
				if( pY < screenHalfHeight )
					{  offY = (Integer) (screenHalfHeight - pY); }
						// Right and Bottom edge -- lots of math, can likely be optimized
				 if( pX > ((current_map.getWidth()*16) - screenHalfWidth) )
					 {  offX = (Integer) (-1)*(screenHalfWidth - pX); }
				 if( pY > ((current_map.getHeight()*16) - (screenHalfHeight)) )
					 {  offY = (Integer) (-1)*(screenHalfHeight - pY); }	
				
				// Bound the Offsets ~ cannot push the region off the image
				if( (Math.abs( offX ) > (xCenter - sectionDeltaX)) && offX < 0 )
					{ offX = (xCenter - sectionDeltaX) * -1; }
				if( (Math.abs( offX ) > (xCenter - sectionDeltaX)) && offX > 0 )
					{ offX = (xCenter - sectionDeltaX); }		
				if( (Math.abs( offY ) > (yCenter - sectionDeltaY)) && offY < 0 )
					{ offY = (yCenter - sectionDeltaY) * -1; }
				if( (Math.abs( offY ) > (yCenter - sectionDeltaY)) && (offY > 0) )
					{ offY = (yCenter - sectionDeltaY); }
				}

				// This sets a virtual screen (screenZOOM) which is a blank copy of screen,
				//  To a cropped section of the real screen, that is thus immediately scaled to screen x/y
				// Probably not the best way to do zooming, but it works.
				// screenZOOM is then fed to the physical screen instead of screen, which continues
				// to update as usual.
			screenZOOM.setImage(
					scaleImage(  screen.getImage().getSubimage(
							xCenter - sectionDeltaX - offX, 
							yCenter - sectionDeltaY - offY,
							sectionDeltaX*2, sectionDeltaY*2 ),
					java.awt.image.BufferedImage.TYPE_INT_RGB,
					GUIzoom,GUIzoom)
				);
			}
		catch(Exception e) 
			{
			System.err.println("Unable to zoom map");
			log( " WARNING error in map zoom function : " + e.getMessage() );
			e.printStackTrace();
			}

		}

	public static void paintFrame() 
		{
		//GUI.cycleTime = System.currentTimeMillis(); // Keep a steady FPS
		if( GUIzoom > 1 )	
			{
			processZoom();
			updateGUI( screenZOOM );
			}
		else	{  updateGUI();  }
		synchFramerate();
		updateFPS();
		}

		// Krybo (2014-09-18) inserted code dealign with map zone to this routine.
		// This can be used to "hijack" the original [screen] draw process
		//  and draw a different image to the screen.
	public static void updateGUI( domain.VImage theSource) 
		{
		if(Script.TEST_SIMULATION)
			{ return;	}
		if( theSource.width != screen.width  ||  theSource.height != screen.height )
			{
			log( " WARNING  UpdateGUI got VImage incompatible with screen size!");
			log("DEBUG:  X: "+Integer.toString(theSource.width)+" vs "+Integer.toString(screen.width)+
					"  :-:  Y: "+Integer.toString(theSource.height)+
					" vs "+Integer.toString(screen.height) );
			return; 
			}

		try {
			Graphics g = strategy.getDrawGraphics();
			if(alpha != 1f) 
				{
				Graphics2D g2d = (Graphics2D) g;
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, alpha));
				g2d.drawImage( theSource.getImage(), 0, 0, 
						curwidth, curheight, null);
				}
			else {
				g.drawImage( theSource.getImage(), 0, 0, 
						curwidth, curheight, null);			
				}
	
					// This displays all the VC layers
//			g.setComposite(AlphaComposite.SRC );
			g.drawImage( jvcl.getBufferedImage(), 0, 0, 
					curwidth, curheight, null);
			
			/* Do this to rotate 180 
			Graphics2D g2d = (Graphics2D) g;
			g2d.rotate(Math.PI, curwidth/2, curheight/2);
			g2d.drawImage(screen.getImage(), 0, 0, curwidth, curheight, null);*/

			
			// Show FPS
			if(showFPS) 
				{
				g.setFont(fps_font);
				g.setColor(Color.WHITE);
				g.drawString("FPS: " + Float.toString(frameInLastSecond), 10, 20);
				g.drawString(Integer.toString(GUIzoom)+"x", 15, 40);
				if( ! playerIsSet()  )
					{  g.drawString(" !!! UNSET PLAYER", 15, 60);  } 
				}
				
			g.dispose();
			strategy.show();
			}
		catch(Exception e) 
			{
			System.err.println("Unable to draw screen");
			log( " WARNING Exception in main screen drawing function : " + e.getMessage() );
			e.printStackTrace();
			}

	}
	
		// Passthrough to keep any existing code happy.
	public static void updateGUI( ) 
		{ updateGUI(screen); }
	
			// END Krybo (2014-09-18) edits
	
	
	public static void synchFramerate() {
		cycleTime = cycleTime + frameDelay;
		long difference = cycleTime - System.currentTimeMillis();
		if(difference > 0) {
			try {
				Thread.sleep(difference);
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}

	}	
	

	void closeWindow() {
		super.setVisible(false);
		System.exit(0);
	}
	
	public Canvas getCanvas() {
		return this.canvas;
	}

	public void componentResized(ComponentEvent e)
	{
		Dimension scrsize = Toolkit.getDefaultToolkit().getScreenSize();
		winwidth = scrsize.width;
		winheight = scrsize.height;		
		//System.out.println(getWidth());	
		updateCanvasSize();
		//VergeEngine.scaledBI = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);//this.createVolatileImage(this.getWidth(), this.getHeight());
		//VergeEngine.g = VergeEngine.scaledBI.createGraphics();
	}
	
	public void updateCanvasSize() 
		{
		this.setCurwidth(this.getWidth()-super.getInsets().left - 
				super.getInsets().right );
		this.setCurheight(this.getHeight()-super.getInsets().top-super.getInsets().bottom-menuBar.getHeight());
		//canvas.updateSize(
			//	this.getWidth()-super.getInsets().left-super.getInsets().right,
				//this.getHeight()-super.getInsets().top-super.getInsets().bottom);
		}

	
	@Override
	public void componentHidden(ComponentEvent arg0) {	}

	@Override
	public void componentMoved(ComponentEvent arg0) {	}

	@Override
	public void componentShown(ComponentEvent arg0) {	}

		// Krybo : compiler disapproved of static access
	public void setAlpha(float f) 
		{
		setAlphaStatic(f);
//		this.alpha = f;		
		}	
	public static void setAlphaStatic( float f )
		{	alpha = f;	}
	
	public static void incFrameDelay(int i) {
		if(frameDelay <=1)
			return;
		if(frameDelay <= 5)
			i = -1;
		
		frameDelay = frameDelay + i;
		
		if(frameDelay > 100)
			frameDelay = 100;
	}

	
	protected final static Font fps_font = new Font("Monospaced", Font.PLAIN, 12);
	static long nextSecond = System.currentTimeMillis() + 1000;
	static int frameInLastSecond = 0;
	static int framesInCurrentSecond = 0;
	static void updateFPS() {
		long currentTime = System.currentTimeMillis();
	    if (currentTime > nextSecond) {
	        nextSecond += 1000;
	        frameInLastSecond = framesInCurrentSecond;
	        framesInCurrentSecond = 0;
	    }
	    framesInCurrentSecond++;	
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		if(source==cbMenuItemSound) {
			config.setNosound(!config.isNosound());
			stopmusic();
		} else
		if(source==cbMenuItemFullScreen) {
			config.setWindowmode(!config.isWindowmode());
			if(	config.isWindowmode()) {
				this.setDimensions(this, config.getV3_xres(), config.getV3_yres());
			}
			else {
				this.setDimensions(this, 0, 0);
			}
		} else
		if(source==cbMenuItemDoubleScreen) {
				config.setWindowmode(true);
				config.setDoubleWindowmode(!config.isDoubleWindowmode());
				if(	config.isDoubleWindowmode()) {
					this.setDimensions(this, config.getV3_xres()*2, config.getV3_yres()*2);
				}
				else {
					this.setDimensions(this, config.getV3_xres(), config.getV3_yres());
				}
			} else
		if(source==cbMenuItemshowFPS) {
			showFPS = cbMenuItemshowFPS.isSelected();
		}
				
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if( e.getActionCommand().equals("increaseFPS") ) 
			{	GUI.incFrameDelay(-5);	} 
		else if(e.getActionCommand().equals("decreaseFPS")) 
			{	GUI.incFrameDelay(5); }
					// Krybo (2014-09-17  added some basic functions to GUI
		else if( e.getActionCommand().equals("TerminateFromGUI") ) 
			{ 	core.Script.exit(" User Terminated from GUI (Bye) "); }
		else if( e.getActionCommand().equals("MapZoomIn") ) 
			{
			if( GUIzoom == 4 ) { GUIzoom = 8; }
			if( GUIzoom == 2 ) { GUIzoom = 4; }
			if( GUIzoom == 1 ) { GUIzoom = 2; }
			ZoomScreenSubset = new domain.VImage(
					screen.width / GUIzoom, screen.height / GUIzoom );
			log( " Map Zoom IN to level x" + Integer.toString(GUIzoom) ); 
			}
		else if( e.getActionCommand().equals("MapZoomOut") ) 
			{
			if( GUIzoom == 2 ) { GUIzoom = 1; }
			if( GUIzoom == 4 ) { GUIzoom = 2; }
			if( GUIzoom == 8 ) { GUIzoom = 4; }
			ZoomScreenSubset = new domain.VImage(
					screen.width / GUIzoom, screen.height / GUIzoom );
			log(" Map Zoom OUT to level x"+Integer.toString(GUIzoom) ); 
			}
					// END Krybo Edits
	}

	public int getCurwidth()
		{  return curwidth;   }

	public void setCurwidth(int curwidth)
		{	GUI.curwidth = curwidth;   }

	public int getCurheight()
		{	return curheight;	}

	public void setCurheight( int curheight )
		{	GUI.curheight = curheight;   }
	
}
