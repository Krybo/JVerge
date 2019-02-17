package menus;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Method;
import java.util.HashMap;

import domain.VImage;
import menus.VmiButton.enumMenuButtonCOLORS;
 

/**	Vmenuitem: 
 * intended to interface Classes for menu content
 * E.x.  Simple text item, fill-statBar, Input box 
 * NOT intended for Classes the control overall menus mechanics.
 * 
 * The paint method is the meat, it decides how to use any of 
 * the data provied to the menu item, be it images, strings, colors.. etc.
 * 
 * There is only one action that can be proformed when the 
 * item is activated.   This is provided by any java Method pointer.
 * 
 * Setting and Getting various constituents can be highly unpredictable.
 * Hence this is left up to individual implementations to add them.l
 * 
 * @author Krybo
 */

public interface Vmenuitem
	{

//	public Vmenuitem();
			
	public static enum enumMenuItemSTATE
		{
		NORMAL (0),
		SELECTED (1),
		ACTIVATED (2),
		DISABLED (3),
		STATELESS(4);
		
		private final Integer index;
		enumMenuItemSTATE( Integer n )
			{	this.index = n;	}
		public Integer value()
			{ return(this.index); }
		public String getName()
			{ return(this.name()); }
		public int val()
			{ return(this.index.intValue()); }
		}

	/** Directly Change the position of the menuitem within the parent. */
	public boolean reposition( int posX, int posY, int relPosX, int relPosY );
	/** Nudge existing position(s) of the menuitem within the parent. */
	public boolean repositionDelta( 
			int deltaX, int deltaY, int drelPosX, int drelPosY );
	// Sets the method called when the thing is used.
	public void setAction( Method action );
	public Method getAction();
	public boolean doAction();
	public void setActionArgs( Object[] actionArgs );
	public Object[] getActionArgs();
	
	// Method to draw the item.
	public void paint(VImage target);
	
	/** Special animated drawing content.   Is called on a pre-specified 
	 * Frequency by the VmenuManager.  Do not make animation too
	 * intricate as blocks that take significantly long will 
	 * quickly impact performance, depending on frequency and intricacy.
	 * Animated items can be expected to "draw over" what is done
	 *   by the lower frequency paint() method.
	 * @param target  a VImage reference to draw on.
	 * @return boolean success or fail.
	 */
	public boolean animate( VImage target );
	
	// mode # For internal use
	public boolean changeMode(Integer theModeNumber );

	
	// Gets the position / total width & height this item consumes.
	public Double getX();
	public Double getY();
	public Double getDX();
	public Double getDY();
		// Lets menus enforce a certain width/height for their Items
	public void setExtendX( int itemWidth, boolean onlyIfGreater );
	public void setExtendY( int itemHeight, boolean onlyIfGreater );

	// Control and change the menu content.
	public void setTextContent( HashMap<Integer,String> textItems );
	public void setIconContent( HashMap<Integer,VImage> imageItems );
	public void setColorContent( HashMap<Integer,Color> basicColors );
	public void setImageContent( HashMap<Integer,VImage> imageItems );
	public void setFont( Font newFont );

	// Methods to get hashed data.
	public Color getColorComponent( Integer hashKeyColor );

	// Enable different components of possible item.
	// All implemented instances won't have all of them, so use skeletons.
	public void enableActivation();	// disable menu action. greyout.
	public void enableText( boolean enableText );
	public void enableIcons( boolean enable );
	public void enableBackdrop( boolean enable );
	public void enableFrame( boolean enable );
	public void setState( Integer itemState );
	public Integer getState();
	
	public boolean isActive();
	public boolean isVisible();
	
	/* Enables blinking highlighting in menuitems
	 * Vmenuitem implementations display the blinking using background colors 
	 *    along with trig. and the system clock.
	 * Vmenu implementations must control the blinking amongst their items.
	 * Proper animation needs to be enabled in Vmenu for this to be shown.
	 * Blinking image backgrounds could be implemented but thats not 
	 *    recommended due to performance concerns.. 
	 */
	public void enableOscillation( Integer freqMsec );
	public void disableOscillation();
	public boolean isOscillating();
	public Integer getOscillationFrequency();

	//  To attach a menuitem to a keystroke.
	public Integer getKeycode();
	public void setKeycode(Integer vergeKeyCode );
	public void setFrameThicknessPx(int thick);

	/**
	 * This typically can do nothing.
	 * Otherwise, use it for your own inscrutable purposes
	 * @param argv  a number that can be used to change behavior
	 * @return  a number code as the result
	 */
	public Integer debug_function( Integer argv );

	// Default action Method, safely does nothing.
	public static void nullAction()
		{
		System.out.println("NULL ACTION called.");
		return; 
		}

	public void setParentID( Long id );
	public void setChildID( Long id );
	public Long getParentID();
	public Long getId();
	public Long getChildID();

	public  void goParent();
	public  void goChild();
	
	public void processInput( String input );

	public static Long getRandomID()
		{
		return( new Double(
				Math.random() * Long.MAX_VALUE ).longValue());
		}
	
	public String[] getTip();
	public void setTip( String[] descriptions );
	public void setTip( String helpCaption );

	/** Varies a color between white > color > black on a 
	 * cycle (trig) variable that varies between -1 and 1 */
	public static Color oscillateColor( Color in, Double cycle, boolean invert)
		{
		int r = in.getRed();
		int g = in.getGreen();
		int b = in.getBlue();
		int dr = 255 - r;
		int dg = 255 - g;
		int db = 255 - b;
		if( cycle == 0.0d )   { return( in ); }
		if( ! invert )
			{
			if( cycle < 0.0d )
				{
				r = r - new Double( r * Math.abs( cycle ) ).intValue();
				g = g - new Double( g * Math.abs( cycle ) ).intValue();
				b = b - new Double( b * Math.abs( cycle ) ).intValue();
				}
			else {
				r += ( dr * cycle);
				g += ( dg * cycle);
				b += ( db * cycle);
				}
			}
		else
			{
			if( cycle > 0.0d )
				{
				r = r - new Double( r * Math.abs( cycle ) ).intValue();
				g = g - new Double( g * Math.abs( cycle ) ).intValue();
				b = b - new Double( b * Math.abs( cycle ) ).intValue();
				}
			else {
				r += ( dr * cycle);
				g += ( dg * cycle);
				b += ( db * cycle);
				}
			}
		return( new Color( r,g,b ) );
		}		
	
	/** Returns a value between one and negative one depending
	 * on the current system time and a given frequency. 
	 * To be used by blinking animations and similar. 
	 * Frequency must be a positive integer, internally enforced.  */
	public static Double generateTimeCycler( Long frequencyMsec )
		{
		if( frequencyMsec <= 0 )   { return( 0.0d ); }
		Long nsCycle = frequencyMsec * new Long( 1000000 );
		Long t = System.nanoTime();
		Long nsTime = Long.remainderUnsigned( t, nsCycle ); // t % nsCycle;
//		Long nsTime = Math.floorMod( t, nsCycle );  
		if( nsTime <= 0 )   {  nsTime = new Long(1); }  // not allowed to be 0
		Double rads = (nsTime.doubleValue() / nsCycle.doubleValue()) *
				Math.PI * 2.0d;
		return( Math.sin( rads ) );
		}
	// a simple type cast overload.
	public static Double generateTimeCycler( Integer freqencyMsec )
		{ return( Vmenuitem.generateTimeCycler( new Long( freqencyMsec ))); }
	
	}
