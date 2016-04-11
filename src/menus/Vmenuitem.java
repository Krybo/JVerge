package menus;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;

import domain.VImage;
 

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

	// Change the position of the menuitem within the parent.
	public boolean reposition( int posX, int posY, int relPosX, int relPosY );
	// Sets the method called when the thing is used.
	public void setAction( Method action );
	public boolean doAction();
	// Method to draw the item.
	public void paint(VImage target);
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

	}
