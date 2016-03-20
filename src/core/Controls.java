package core;

import static core.Script.*;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import menus.VMenuManager;
import domain.VImage;

public class Controls implements 
				MouseListener, MouseMotionListener, FocusListener, 
				KeyListener, WindowListener
	{

	// The VERGE 3 Project is originally by Ben Eirich and is made available via
	//  the BSD License.
	//
	// Please see LICENSE in the project's root directory for the text of the
	// licensing agreement.  The CREDITS file in the same directory enumerates the
	// folks involved in this public build.
	//
	// If you have altered this source file, please log your name, date, and what
	// changes you made below this line.

	/***************************** data *****************************/
	
	public static boolean kill_up, kill_down, kill_left, kill_right;
	public static boolean kill_b1, kill_b2, kill_b3, kill_b4;

	public static String bindbutton[] = new String[4];
	public static String bindarray[] = new String [128];
		// Krybo : added in Script.nicedHookkey
	public static long bindarrayDelay[] = new long [128];
	public static long bindarrayCounter[] = new long [128];

	public static void UnUp() { kill_up = true; up = false; }
	public static void UnDown() { kill_down = true; down = false; }
	public static void  UnLeft() { kill_left = true; left = false; }
	public static void  UnRight() { kill_right = true; right = false; }
	public static void  UnB1() { kill_b1 = true; b1 = false; }
	public static void  UnB2() { kill_b2 = true; b2 = false; }
	public static void  UnB3() { kill_b3 = true; b3 = false; }
	public static void  UnB4() { kill_b4 = true; b4 = false; }
	
	/*[Rafael, the Esper] Use interface instead 
	 * boolean k_b1 = SCAN_ENTER,
	     k_b2 = SCAN_ALT,
		 k_b3 = SCAN_ESC,
		 k_b4 = SCAN_SPACE;

	// Overkill (2006-06-25): Customizable directionals on the keyboard.
	byte k_up = SCAN_UP,
		 k_down = SCAN_DOWN,
		 k_left = SCAN_LEFT,
		 k_right = SCAN_RIGHT;*/

	byte j_b1=0, j_b2=1, j_b3=2, j_b4=3;

	/***************************** menu *****************************/
	// Krybo (Feb.2016)  Adding a low level menu toggle variable.
	public static boolean MENU_OPEN;
	public static Long MENU_TIMER = new Long(0);
	public static int MENU_MASTERKEY = -1; 
	// Keeps a stack of button keypress codes intended to be fed to menus
	//   the int keeps track of how many items were sent out to process.
	protected static ArrayList<Integer> menusKeyStack = new ArrayList<Integer>();
	protected static int menusKeyStackSent = 0;
	// A shameless counter of the number of pieces of keystrokes
	//		sucessfully sent to menus.
	public static long menuKeyCount = 0;

	/***************************** input ****************************/
	private static boolean INPUT_MODE = false;
	private static HashMap<Long,String> INPUT = new HashMap<Long,String>();
	private static StringBuilder inputbuffer = new StringBuilder();
	private static Integer inputCursor = 0;
	private static String inputMsg = "Input:";
	private static Long inputID = new Long( -1 );
	private static int inputX = 10, inputY = 10;
	private static boolean inputAcceptNumbers = true;
	private static boolean inputAcceptDecimal = true;
	private static boolean inputAcceptLetter = true;
	private static boolean inputAcceptSpecial = true;
	private static boolean inputAcceptHighChar = true;
	public static AffineTransform generic_AffTransf = 
			new AffineTransform();

		// 	Regular expressions for input filtering
	public static final String regExpFilterInt = 
			new String( "[0-9]" );
	public static final String regExpFilterDec = 
			new String( "[0-9.]" );
	public static final String regExpFilterAlpha = 
			new String( "[a-zA-Z]" );
	public static final String regExpFilterAlphaNum = 
			new String( "[a-zA-Z0-9]" );
	public static final String regExpFilterSpecial = 
			new String( "[\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\,\\_\\-\\\\\\/ ]" );
	public static final String regExpFilterHighChar = 
			new String( "[\\x7F-\\xFF]" );
	public static final String regExpFilterLowChar = 
			new String( "[\\x00-\\x20]" );


	/***************************** code *****************************/
	//int _input_killswitch;
	
	public static void UpdateControls()
	{
		//[Rafael, the Esper] HandleMessages();

		/*[Rafael, the Esper] if( _input_killswitch ) {
			b4 = b3 = b2 = b1 = right = left = down = up = false;
			return;
		}*/

		/* [Rafael, the Esper] Use JGame implementation
		joy_Update();
		mouse_Update();
		UpdateKeyboard();
*/

		boolean oldb1 = b1,
			 oldb2 = b2,
		     oldb3 = b3,
			 oldb4 = b4;

		// Overkill (2006-06-25):
		// The following four ifs have been altered to allow custom directional keys.
		if (getKey(KeyUp)) up = true; else up = false;
		if (getKey(KeyLeft)) left = true; else left = false;
		if (getKey(KeyDown)) down = true; else down = false;
		if (getKey(KeyRight)) right = true; else right = false;

		if (getKey(KeyEnter)) b1 = true; else b1 = false;
		if (getKey(KeyAlt)) b2 = true; else b2 = false;
		if (getKey(KeyEsc)) b3 = true; else b3 = false;
		if (getKey(KeyFire)) b4 = true; else b4 = false;

		if (!up && kill_up) 
			{ kill_up = false; log("UP Key released."); }
		if (!down && kill_down) kill_down = false;
		if (!left && kill_left) kill_left = false;
		if (!right && kill_right) kill_right = false;

		if (!b1 && kill_b1) kill_b1 = false;
		if (!b2 && kill_b2) kill_b2 = false;
		if (!b3 && kill_b3) kill_b3 = false;
		if (!b4 && kill_b4) kill_b4 = false;

		if (up && kill_up) up = false;
		if (down && kill_down) down = false;
		if (left && kill_left) left = false;
		if (right && kill_right) right = false;

		if (b1 && kill_b1) b1 = false;
		if (b2 && kill_b2) b2 = false;
		if (b3 && kill_b3) b3 = false;
		if (b4 && kill_b4) b4 = false;

		//mbg 9/5/05 todo removed for psp
		// TODO LUA
		if (b1 && !oldb1) callfunction(bindbutton[0]);
		if (b2 && !oldb2) callfunction(bindbutton[1]);
		if (b3 && !oldb3) callfunction(bindbutton[2]);
		if (b4 && !oldb4) callfunction(bindbutton[3]);
		
		long hkNow = System.nanoTime();
		
		// Rafael, the Esper (2014: new)
		for(int i=0; i<bindarray.length; i++)
			{
			
//			if( getKey(i) ) 
//				{ log("key # "+Integer.toString(i)+" is down"); } 
			
			if(getKey(i) && bindarray[i] != null && !bindarray[i].isEmpty()) 
				{
				
				// Krybo June-2015: 
				//    If the user specified delay has NOT passed since
				//    the last time it was executed, skip it.
				//	If nicedHookkey was not used, this will have no effect
				//   - purpose is to prevent a hookkey method from
				//    executing as fast as the machine cycles this.
				
				if( hkNow < (bindarrayCounter[i] + bindarrayDelay[i] ) )
					{ continue; }
				else { bindarrayCounter[i] = hkNow;  }
				
				callfunction(bindarray[i]);
				}
			}
		
		if( MENU_OPEN )	{ UpdateMenusControls( new Long(90000000) ); }
	}

	/**
	 * Determine the menu instances that have focus
	 *    and call their .controls() method.
	 *  Several menus can be controlled at once.   This is the purpose
	 *      behind MENU_FOCUS[] to hold the focusID of each operable
	 *      Vmenu object.      For the initial implementation, all Vmenu's
	 *      are confined to the JVCL (core.Script.jvclMenu) so this
	 *      method knows where the menus are.   Since, JVCL has
	 *      been done away with in favor of the VMenuManager Class,
	 *      which better handles inter-menus communicationss.
	 *      
	 *      The arg controls how frequently this function is allowed  
	 *         to execute.   a value of  100 000 000 is ~ 10 x a second.
	 *  
	 *      Krybo (Mar.2016)
	 */
	public static void UpdateMenusControls( Long minTimingNanoseconds )
		{
		Long delta = System.nanoTime() - MENU_TIMER;
		if( delta < minTimingNanoseconds )  { return; }		// Timing control.
		MENU_TIMER = System.nanoTime();
//		log("Menu controls read @ "+
//			Long.toString(MENU_TIMER) + " delta " + delta.toString() );

		// First, see if there is any input to send.
		if( Controls.menusKeyStack.isEmpty() ) { return; }
		int nstrokes = Controls.menusKeyStack.size();
//		System.out.println(Integer.toString(nstrokes)+" new keystrokes VS "+Integer.toString( Controls.menusKeyStackSent ));
		while( nstrokes > Controls.menusKeyStackSent )
			{
//	System.out.println(Integer.toString(nstrokes)+" DEBUG new keystrokes VS "
//			+Integer.toString( Controls.menusKeyStackSent ));
			Controls.menusKeyStackSent++;
				// If no menus have focus.. we have a problem to resolve.
			boolean focusedMenuCheck = false;
				// now send it to all menus with focus.

			if( VergeEngine.Vmm.delegateControl(
					Controls.menusKeyStack.get(
						Controls.menusKeyStackSent-1 ),
					true) > 0 )
				{
				Controls.menuKeyCount++;
				focusedMenuCheck = true;
				VergeEngine.Vmm.refreshGraphics();
				}
			
			if( focusedMenuCheck == false )
				{
				System.err.println( "WARNING : ALL MENU FOCUS LOST."
					+ ".- Re-focusing system menus. "
					+ " Probable menu link problem" );
				VergeEngine.Vmm.restoreSystemMenuFocus();
//				MENU_FOCUS[0] = 
//						VergeEngine.Vmm.getSystemMenuFocusID();

				}
			
			}
		
		return;
		}
	
	/**
	 * Utility statics to help parse the extended menu codes passed to 
	 *   menu objects doControls() method
	 *   (Krybo Mar.2016)
	 * @param ext_code   keycode that includes high bits for cntl shift alt
	 * @return  --various--
	 */
	public static Integer extcodeGetBasecode(Integer ext_code )
		{ return( new Integer(ext_code >> 3));  }
	public static Byte extcodeGetExtentionByte( Integer ext_code )
		{
		return( new Byte(  new Integer( 
			ext_code - Controls.extcodeGetBasecode(ext_code) ).byteValue()
			 ));
		}
	public static int extcodeGetExtention( Integer ext_code )
		{
		return(  new Integer( 
			ext_code - (( ext_code >> 3 ) << 3)  ));
		}
	public static boolean extcodeGetSHIFT( Integer ext_code )
		{
		int tmp = Controls.extcodeGetExtention(ext_code); 
		if( (tmp & 1) == 1 )
			{ return(true); }
		return(false);
		}
	public static boolean extcodeGetCNTL( Integer ext_code )
		{
		int tmp = Controls.extcodeGetExtention(ext_code);
		if( (tmp & 2) == 2 )
			{ return(true); }
		return(false);
		}
	public static boolean extcodeGetALT( Integer ext_code )
		{
		int tmp = Controls.extcodeGetExtention(ext_code);
		if( (tmp & 4) == 4 )
			{ return(true); }
		return(false);
		}

	// JGAME STUFF **** /////////////////////////////////////////////
	
	void updateMouse(MouseEvent e,boolean pressed, boolean released, boolean inside) {
				mousepos = e.getPoint();
				/* [Rafael, the Esper] mousepos.x = (int)(mousepos.x/el.x_scale_fac);
				mousepos.y = (int)(mousepos.y/el.y_scale_fac); */
				mouseinside=inside;
				int button=0;
				if ((e.getModifiers()&InputEvent.BUTTON1_MASK)!=0) button=1;
				if ((e.getModifiers()&InputEvent.BUTTON2_MASK)!=0) button=2;
				if ((e.getModifiers()&InputEvent.BUTTON3_MASK)!=0) button=3;
				if (button==0) return;
				if (pressed)  {
					mousebutton[button]=true;
					keymap[255+button]=true;
					/* [Rafael, the Esper] if (wakeup_key==-1 || wakeup_key==255+button) {
						if (!eng.isRunning()) {
							eng.start();
							// mouse button is cleared when it is used as wakeup key
							mousebutton[button]=false;
							keymap[255+button]=false;
						}
					}*/
				}
				if (released) {
					mousebutton[button]=false;
					keymap[255+button]=false;
				}
			}

			public void mouseClicked(MouseEvent e) {
				// part of the "official" method of handling keyboard focus
				// some people think it's a bug.
				// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4362074
				if (!has_focus) VergeEngine.getGUI().getCanvas().requestFocus();
				updateMouse(e,false,false,true); 
			}
			public void mouseEntered(MouseEvent e) {
				updateMouse(e,false,false,true); 
			}
			public void mouseExited(MouseEvent e) {
				updateMouse(e,false,false,false); 
			}
			public void mousePressed(MouseEvent e) {
				updateMouse(e,true,false,true); 
			}
			public void mouseReleased(MouseEvent e) {
				updateMouse(e,false,true,true); 
			}
			public void mouseDragged(MouseEvent e) {
				updateMouse(e,false,false,true); 
			}
			public void mouseMoved(MouseEvent e) {
				updateMouse(e,false,false,true);
				//VergeEngine.getGUI().menuBar.setVisible(VergeEngine.getGUI().menuBar.isVisible() || (e.getY() < 50 && VergeEngine.getGUI().isUndecorated()));
			}
			public void focusGained(FocusEvent e) {
				has_focus=true;
			}
			public void focusLost(FocusEvent e) {
				has_focus=false;
			}

			/* Standard Wimp event handlers */
			public void keyPressed(KeyEvent e) 
				{
				char keychar = e.getKeyChar();
				int keycode = e.getKeyCode();				
				if (keycode>=0 && keycode < 256) {
					keymap[keycode]=true;
					lastkey=keycode;
					lastkeychar=keychar;
				/* [Rafael, the Esper]	if (wakeup_key==-1 || wakeup_key==keycode) {
						if (!eng.isRunning()) {
							eng.start();
							// key is cleared when it is used as wakeup key
							keymap[keycode]=false;
						}
					}*/
				}
				/* shift escape = exit */
				if (e.isShiftDown () 
				&& e.getKeyCode () == KeyEvent.VK_ESCAPE) {
				// [Rafael, the Esper]&& !eng.isApplet()) {
					System.exit(0);
				}
				
				// Krybo (Feb.2016)  menu mode toggle
				
				if( MENU_OPEN && keycode != MENU_MASTERKEY &&
						INPUT_MODE != true &&
						keycode != 16 && keycode != 17 && keycode != 18 )
					{
					
					Integer MKEYCODE = new Integer( keycode << 3);
					if( e.isShiftDown() ) 
						{ MKEYCODE += 1; }
					if( e.isControlDown() ) 	
						{ MKEYCODE += 2; }
					if( e.isAltDown() ) 
						{ MKEYCODE += 4; } 
					menusKeyStack.add(MKEYCODE);
					}

				// Menu open <> close toggle.

				if( e.getKeyCode() == MENU_MASTERKEY 
						&& INPUT_MODE == false )
					{
					if( MENU_OPEN == false )	
						{ 
						MENU_OPEN = true;
						// This ensures the menu redraws immediated
						// The fake-keystroke of -1 does nothing

						VergeEngine.Vmm.refreshGraphics();
						log("           --< MENU MODE >--");
						}
					else { 
						MENU_OPEN = false;
						log("           --< STANDARD MODE >--");
						}
					}

				if( INPUT_MODE )
					{
					// This switch is to handle control characters only
					switch( keycode )
						{
						case 10:		// Enter
							Controls.finish_input(true);
							break;
						case 27:		// Esc - Discard
							Controls.finish_input( false ); 
							break;
						case 32:		// left
							Controls.inputbuffer.insert(
								Controls.inputCursor.intValue(),
								" " );
							Controls.inputCursor++;
							break;
						case 36:		// END
							Controls.inputCursor = 0;
							break;
						case 35:		// HOME
							Controls.inputCursor = 
								Controls.inputbuffer.length();
							break;
						case 37:		// left
							Controls.inputCursor--;
							break;
						case 39:		// right
							Controls.inputCursor++;
							break;
						case 127:		// Forward delete
							if( Controls.inputCursor >= 
									Controls.inputbuffer.length() )
								{  break;  }
							Controls.inputbuffer.delete(
								Controls.inputCursor.intValue(),
								Controls.inputCursor.intValue()+1 );							
							break;
						case 8:	// Backspace
							if( Controls.inputbuffer.length() == 0 )   
								{ break; }
							Controls.inputbuffer.delete(
								Controls.inputCursor.intValue() - 1,
								Controls.inputCursor.intValue() );
							Controls.inputCursor--;
							break;
						case 16:	// Kill Cntl Alt Shift
						case 17:
						case 18:
							break;
						default:

							// Do fancy Filtering input here.
//							log("INPUT MODE:  got : "+
//									Integer.toString(keycode));	
							Controls.inputbuffer.insert(
								Controls.inputCursor.intValue(),
								lastkeychar );
							Controls.inputCursor++;
							break;
						}

					int b4 = Controls.inputbuffer.length();
					
					// Filtering
					if( ! Controls.inputAcceptNumbers )
						{
						// numbers are not allowed.
						Controls.inputbuffer = new StringBuilder(
							Controls.inputbuffer.toString().replaceAll(
								Controls.regExpFilterInt, "" )
							);
						}
					if( ! Controls.inputAcceptDecimal )
						{
						// numbers & period are not allowed.
						Controls.inputbuffer = new StringBuilder(
							Controls.inputbuffer.toString().replaceAll(
								Controls.regExpFilterDec, "" )
							);
						}
					if( ! Controls.inputAcceptLetter )
						{
						// letters are not allowed.
						Controls.inputbuffer = new StringBuilder(
							Controls.inputbuffer.toString().replaceAll(
								Controls.regExpFilterAlpha, "" )
							);
						}
					if( ! Controls.inputAcceptSpecial  )
						{
						// numbers are not allowed.
						Controls.inputbuffer = new StringBuilder(
							Controls.inputbuffer.toString().replaceAll(
								Controls.regExpFilterSpecial, "" )
							);
						}
					if( ! Controls.inputAcceptHighChar )
						{
						// hgh ascii chars are not allowed.
						Controls.inputbuffer = new StringBuilder(
							Controls.inputbuffer.toString().replaceAll(
								Controls.regExpFilterHighChar, "" )
							);
						}
					
					int b5 = Controls.inputbuffer.length();
					
					// Ensure cursor bounds.
					if( Controls.inputCursor > Controls.inputbuffer.length() )
						{
						Controls.inputCursor = 
							Controls.inputbuffer.length();
						}

					if( Controls.inputCursor < 0 )
						{ Controls.inputCursor=0; }
					
					// If characters have been filters, adjust the cursor.
					if( b4 != b5 )
						{	Controls.inputCursor += (b5-b4);	}

					}
				
				//System.out.println(e+" keychar"+e.getKeyChar());
			}

			/* handle keys, shift-escape patch by Jeff Friesen */
			public void keyReleased (KeyEvent e) 
				{
//				log("KeyEvent released: "+Integer.toString(e.getKeyCode() ));
//				char keychar = e.getKeyChar ();   --  Krybo: b/c its not used.
				int keycode = e.getKeyCode ();   
				if (keycode >= 0 && keycode < 256) {
					keymap [keycode] = false;
				return;
				}
			}
			public void keyTyped (KeyEvent e) { }
	
			/* WindowListener handlers */

			public void windowActivated(WindowEvent e) {}
			public void windowClosed(WindowEvent e) {
				System.out.println("Window closed");
			}
			public void windowClosing(WindowEvent e) {
				System.out.println("Window closed; exiting.");
				VergeEngine.getGUI().closeWindow();
			}
			public void windowDeactivated(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowOpened(WindowEvent e) {}
			
			

	/** Cursor keys for both regular and mobile keyboard. */
	public static final int KeyUp=38,KeyDown=40,KeyLeft=37,KeyRight=39;
	/** On a mobile, the cursor control Fire is the same as shift. */
	public static final int KeyShift=16;
	/** Fire stands for a mobile key, indicating the fire button of the cursor
	 * controls.  It is equivalent to KeyShift. */
	public static final int KeyFire=16;
	public static final int KeyCtrl=17;
	public static final int KeyAlt=18;
	public static final int KeyEsc=27;
	/** On a mobile, pressing "*" also triggers KeyEnter. */
	public static final int KeyEnter=10;
	/** The mobile Star key, equal to '*'. */
	public static final int KeyStar='*';
	/** The mobile Pound key, equal to '#'. */
	public static final int KeyPound='#';
	public static final int KeyBackspace=8; /* is it different sometimes? */
	public static final int KeyTab=9;
	/** Keymap equivalent of mouse button. */
	public static final int KeyMouse1=256, KeyMouse2=257, KeyMouse3=258;
	
	public static final int KeyF1=112;
	public static final int KeyF2=113;
	public static final int KeyF3=114;
	public static final int KeyF4=115;
	public static final int KeyF5=116;
	public static final int KeyF6=117;
	public static final int KeyF7=118;
	public static final int KeyF8=119;
	public static final int KeyF9=120;
	public static final int KeyF10=121;
	public static final int KeyF11=122;
	public static final int KeyF12=123;
	
	
	
	/* mouse */

	boolean has_focus=false;
	Point mousepos = new Point(0,0);
	boolean [] mousebutton = new boolean[] {false,false,false,false};
	boolean mouseinside=false;

	/* keyboard */

	/** The codes 256-258 are the mouse buttons */
	static boolean [] keymap = new boolean [256+3];
	static int lastkey=0;
	static char lastkeychar=0;
	int wakeup_key=0;

	public void clearKeymap() {
		for (int i=0; i<256+3; i++) keymap[i]=false;
	}

	public void wakeUpOnKey(int key) { wakeup_key=key; }

	/* input */

	// get methods unnecessary, variables accessed directly from JGEngine

	public int getMousePosX() { return mousepos.x; }
	public int getMousePosY() { return mousepos.y; }
	public int getMouseX() { return mousepos.x; }
	public int getMouseY() { return mousepos.y; }

	public boolean getMouseButton(int nr) { return mousebutton[nr]; }
	public void clearMouseButton(int nr) { mousebutton[nr]=false; }
	public void setMouseButton(int nr) { mousebutton[nr]=true; }
	public boolean getMouseInside() { return mouseinside; }

	public static boolean getKey(int key) { return keymap[key]; }
	public static void clearKey(int key) { keymap[key]=false; }
	public static void setKey(int key) { keymap[key]=true; }

	public static int getLastKey() { return lastkey; }
	public static char getLastKeyChar() { return lastkeychar; }


	public static void clearLastKey() {
		lastkey=0;
		lastkeychar=0;
	}

	public static String getKeyDescStatic(int key) {
		if (key==32) return "space";
		if (key==0) return "(none)";
		if (key==KeyEnter) return "enter";
		if (key==KeyEsc) return "escape";
		if (key==KeyUp) return "cursor up";
		if (key==KeyDown) return "cursor down";
		if (key==KeyLeft) return "cursor left";
		if (key==KeyRight) return "cursor right";
		if (key==KeyShift) return "shift";
		if (key==KeyAlt) return "alt";
		if (key==KeyCtrl) return "control";
		if (key==KeyMouse1) return "left mouse button";
		if (key==KeyMouse2) return "middle mouse button";
		if (key==KeyMouse3) return "right mouse button";
		if (key==27) return "escape";
		if (key >= 33 && key <= 95)
			return new String(new char[] {(char)key});
		return "keycode "+key;
		}

	public static int getKeyCodeStatic(String keydesc) {
		// tab, enter, backspace, insert, delete, home, end, pageup, pagedown
		// escape
		keydesc = keydesc.toLowerCase().trim();
		if (keydesc.equals("space")) {
			return 32;
		} else if (keydesc.equals("escape")) {
			return KeyEsc;
		} else if (keydesc.equals("(none)")) {
			return 0;
		} else if (keydesc.equals("enter")) {
			return KeyEnter;
		} else if (keydesc.equals("cursor up")) {
			return KeyUp;
		} else if (keydesc.equals("cursor down")) {
			return KeyDown;
		} else if (keydesc.equals("cursor left")) {
			return KeyLeft;
		} else if (keydesc.equals("cursor right")) {
			return KeyRight;
		} else if (keydesc.equals("shift")) {
			return KeyShift;
		} else if (keydesc.equals("alt")) {
			return KeyAlt;
		} else if (keydesc.equals("control")) {
			return KeyCtrl;
		} else if (keydesc.equals("left mouse button")) {
			return KeyMouse1;
		} else if (keydesc.equals("middle mouse button")) {
			return KeyMouse2;
		} else if (keydesc.equals("right mouse button")) {
			return KeyMouse3;
		} else if (keydesc.startsWith("keycode")) {
			return Integer.parseInt(keydesc.substring(7));
		} else if (keydesc.length() == 1) {
			return keydesc.charAt(0);
		}
		return 0;
	}
	
	
	// Krybo (Mar.2016) : utility to open & close menu mode.
	public static boolean changeMenuMode()	// Toggle
		{
		if( MENU_OPEN ) 	{ MENU_OPEN = false; }
		else 				
			{
			MENU_OPEN = true;
			VergeEngine.Vmm.refreshGraphics();
			}
		Controls.menusKeyStack.add(-1);
		return(MENU_OPEN);
		}
	public static boolean changeMenuMode(boolean onOff)
		{
		MENU_OPEN = onOff;
		if( onOff = true )
			{ 
			VergeEngine.Vmm.refreshGraphics();
			}
		
//	Controls.menusKeyStack.add(-1);
		return(MENU_OPEN);
		}
	
	/**  Switches engine controls into Input mode. 
	 * Prepares variables for a new input.
	 */
	public static void begin_input( String theCaption, Long id,
			int x, int y, boolean acceptNumbers,
			boolean acceptDecimal ,	boolean acceptLetter, 
			boolean acceptSpecial, boolean acceptHighChar )
		{
		if( Controls.INPUT_MODE == true )
			{ return; }
		Controls.INPUT_MODE = true;
		Controls.inputID = id;
		Controls.inputbuffer = new StringBuilder();
		Controls.inputCursor = 0;
		Controls.inputX = x;
		Controls.inputY = y;
		Controls.inputAcceptNumbers = acceptNumbers;
		Controls.inputAcceptDecimal = acceptDecimal;
		Controls.inputAcceptLetter = acceptLetter;
		Controls.inputAcceptSpecial = acceptSpecial;
		Controls.inputAcceptHighChar = acceptHighChar;
		Controls.inputMsg = theCaption;
		return;
		}
	
	/**  This is called when an input dialog is finished and closed.
	 * 
	 * @param keep true to retain the input, false to throw it out (cancel)
	 */
	private synchronized static void finish_input( boolean keep )
		{
		if( Controls.INPUT_MODE == false )
			{ return; }
		Controls.INPUT_MODE = false;
		if( keep )
			{
//			System.out.println(" saved user INPUT : "+
//					Controls.inputbuffer.toString() );

			Controls.INPUT.put( Controls.inputID,
					Controls.inputbuffer.toString() );
			}
		return;
		}
	
		/**	This may be used to return String-type input back to 
		 *     the originating menuitem.
		 * @param menusId the menu id
		 */
	public synchronized static String obtain_input( Long menusId )
		{
		if( ! Controls.INPUT.containsKey( menusId )  )
			{	return(new String(""));	}
		return( Controls.INPUT.remove(menusId) );
		}

	/**	Export the Long id keyset from the input stash.
	 *		null if there is no input. 
	 * @return	Set<Long> of menu ids that initiated a furfilled input
	 */
	public synchronized static Set<Long> obtain_input_keys()
		{
		if( Controls.INPUT.isEmpty() ) 
			{ return( null ); }
		return( Controls.INPUT.keySet() );
		}

	public synchronized static BufferedImage getInputBImage(
			VImage screensizeImage )
		{
//		System.out.println("DEBUG: Input layer visual draw called.");
		int myX = Controls.inputX;
		int myY = Controls.inputY;
		
		FontMetrics fm =
			screensizeImage.getImage().getGraphics().getFontMetrics(
			VMenuManager.getInputFont() );

		FontRenderContext frc = fm.getFontRenderContext();

//		TextLayout layout = new TextLayout(
//			Controls.inputbuffer.toString().substring(0, Controls.inputCursor) , 
//			VMenuManager.getInputFont(), frc );

		int sx0 = new Double( 
			VMenuManager.getInputFont().getStringBounds(
			Controls.inputbuffer.toString(), frc ).getWidth() ).intValue();

		int sx1 = new Double( 
			VMenuManager.getInputFont().getStringBounds(
			Controls.inputbuffer.toString().substring(0, Controls.inputCursor), 
			frc).getWidth() ).intValue();
		
		int sx2 = 2;
		if( Controls.inputbuffer.toString().length() != 0 )
			{ 
			sx2 = (sx0 / Controls.inputbuffer.toString().length());
			}
		
		int sy1 = Math.abs( new Double( VMenuManager.getInputFont().getStringBounds(
			Controls.inputbuffer.toString(), frc).getMinY() ).intValue() );

			// Nudge if too close to edge.
		if( myX < (screensizeImage.width >> 4)  )		// 1-16th of screen
			{ myX = (screensizeImage.width >> 4); }
		if( myY < (screensizeImage.height >> 4)  )
			{ myY = (screensizeImage.height >> 4); }
		
		if( myX > (screensizeImage.width - (screensizeImage.width >> 2))  )
			{ myX = (screensizeImage.width - (screensizeImage.width >> 2)); }
		if( myY > (screensizeImage.height - (screensizeImage.height >> 2))  )
			{ myY = (screensizeImage.height - (screensizeImage.height >> 2)); }

			// 	Input Carrot.
		screensizeImage.rectfill( myX+sx1, myY+20, 
				myX+sx1+sx2,  myY+20-sy1, 
				Color.YELLOW );

//		screensizeImage.line( myX+sx1, myY+20, 
//				myX+sx1, myY+20-sy1, 
//				Color.YELLOW );
//		screensizeImage.line( myX+sx1+1, myY+20, 
//				myX+sx1+1, myY+20-sy1, 
//				Color.YELLOW );
		
		screensizeImage.rectfill( 0, 0, screensizeImage.width, 
				screensizeImage.height, new Color( 0.0f,0.0f,0.0f,0.50f ) );
		screensizeImage.printString( myX, myY, 
				VMenuManager.getInputFont(), 
				Controls.inputMsg );
		screensizeImage.line(myX-10, myY+5, myX+100, myY+5, 
				Color.WHITE );
		screensizeImage.line(myX-10, myY+6, myX+100, myY+6, 
				Color.WHITE );

		screensizeImage.printString( myX, myY+20, 
				VMenuManager.getInputFont(), 
				Controls.inputbuffer.toString()  );
		

		
		return( screensizeImage.getImage() );
		}
	
	public static boolean isInInputMode()
		{	return( Controls.INPUT_MODE );	}
	public static boolean isInMenuMode()
		{	return( Controls.MENU_OPEN );		}
	public synchronized static boolean hasInput()
		{ return( ! Controls.INPUT.isEmpty() ); }

}
