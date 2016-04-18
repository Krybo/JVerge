package menus;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import domain.VImage;
import static core.Script.setMenuFocus;
//import static core.Script.Color_DEATH_MAGENTA;

/*
 * Colors:
 * 	0:  Active Text color
 * 	1:	Inactive Text color
 * 	2:  Frame Color Major
 * 	3:  Frame Color Minor
 * 	4:  non-image active background Color (with alpha)
 * 	5:  non-image in-active background Color (with alpha)
 */

public class VmiTextSimple implements Vmenuitem
	{
	private int ulx,uly = 0;		// Upper-left absolute x & y coords
	private int sx,sy = 0;			// String pixel space consumption
	private int w,h = 1;			// Total calculated width and height
	private int extX=0,extY=0;	// empty buffer space width/height
	private int keycode = -1;	// key stroke used to activate
	private int rx,ry;				// relative x/y.
	private int boundX, boundY;	// Externally imposed clipping dimensions
	private int FrameThicknessPx = 1;
	private Long parentID = new Long(-1);
	private Long id = new Long(-1);
	private Long childID = new Long(-1);

	private Color highlighter = new Color(1.0f, 1.0f, 1.0f, 0.36f );
	private Color highlighter2 = new Color(1.0f, 1.0f, 0.2f, 0.50f );

	private Integer state;
	private Integer mode;
	// Active means it is shown but interaction is disabled, it is greyed out.
	//  visible sets drawing of the entire object on or off.
	private boolean active  = false; 
	private boolean visible = false;
		// Controls what is displayed.
	private boolean showText 			= true; 
	private boolean showBG 			= false;
	private boolean showIcon  			= false;
	private boolean showColorful  		= true; 
	private boolean showFrame 		= true;
	
		// Holds text according to mode #
	private HashMap<Integer, String> textItems = 
			new HashMap<Integer, String>();
		// Full box background images.
	private HashMap<Integer, VImage> imageItems = 
			new HashMap<Integer, VImage>();
		// Small "pointer-indicator" type images 
	private HashMap<Integer, VImage> iconItems = 
			new HashMap<Integer, VImage>();
		// Colors used in this item
	private HashMap<Integer, Color> colorItems = 
			new HashMap<Integer, Color>();
	
	private Font fnt = core.Script.fntMASTER;
	private Method myAction = null;
	private String desc;
	private String tip;
	
	public static enum enumMenuStxtCOLORS
		{
		TEXT_ACTIVE (0),
		TEXT_INACTIVE (1),
		FRAME_OUTER (2),
		FRAME_INNER (3),
		BKG_ACTIVE (4),
		BKG_INACTIVE (5);
		
		private final Integer index;
		enumMenuStxtCOLORS( Integer n )
			{	this.index = n;	}
		public Integer value()
			{ return(this.index); }
		public int val()
			{ return(this.index.intValue()); }
		}


	
// ===========================================
	
		// Most basic Constructor:  
	public VmiTextSimple( String text )
		{
		this.textItems.put(0, text );
		this.id = Vmenuitem.getRandomID();
		this.mode = 0;
		this.rx = 0;	this.ry = 0;
		this.ulx = 1;	this.uly = 1;
		this.extX = 0; this.extY = 0;
		this.boundX = 9999;
		this.boundY = 9999;
		this.keycode = -1;
		this.state = new Integer(0);
		this.FrameThicknessPx = 1;
//		this.setAllColorIcon( 16,Color_DEATH_MAGENTA );
		this.setAllColorIcon( 16,Color.red  );
		this.calcDims();

		this.fnt = core.Script.fntMASTER;
		this.active = true;
		this.visible = true;
		this.showBG = false;
		this.showColorful = true;
		this.showFrame = true;
		this.showIcon = false;
		this.showText = true;

		this.imageItems.put(enumMenuItemSTATE.ACTIVATED.value(), 
				new VImage(this.w,this.h,Color.GREEN ) );
		this.imageItems.put(enumMenuItemSTATE.NORMAL.value(),
				new VImage(this.w,this.h,
				core.Script.Color_DEATH_MAGENTA ) );
//				new VImage(this.w,this.h,Color.BLACK) );
		this.imageItems.put(enumMenuItemSTATE.SELECTED.value(), 
				new VImage(this.w,this.h,Color.BLACK ) );
		this.imageItems.put(enumMenuItemSTATE.DISABLED.value(), 
				new VImage(this.w,this.h,Color.DARK_GRAY ) );

		this.colorItems.put(enumMenuStxtCOLORS.BKG_ACTIVE.value(), 
				Color.BLUE );
		this.colorItems.put(enumMenuStxtCOLORS.BKG_INACTIVE.value(), 
				new Color( 0.0f, 0.1f, 0.5f, 1.0f ) );
		this.colorItems.put(enumMenuStxtCOLORS.FRAME_INNER.value(), 
				Color.GRAY );
		this.colorItems.put(enumMenuStxtCOLORS.FRAME_OUTER.value(),
				Color.WHITE );
		this.colorItems.put(enumMenuStxtCOLORS.TEXT_ACTIVE.value(),
				Color.WHITE );
		this.colorItems.put(enumMenuStxtCOLORS.TEXT_INACTIVE.value(),
				new Color( 0.62f, 0.62f, 0.62f, 1.0f )  );
		
		// Set the 4 boring solid-box icons.
		this.iconItems.clear();
		this.iconItems.put(0, new VImage(16,16,
				new Color(1.0f,1.0f,1.0f,1.0f)) );
		this.iconItems.put(1, new VImage(16,16,
				new Color(0.0f,1.0f,0.0f,1.0f)) );
		this.iconItems.put(2, new VImage(16,16,
				new Color(1.0f,0.0f,0.0f,1.0f)) );
		this.iconItems.put(3, new VImage(16,16,
				new Color(0.1f,0.1f,0.1f,1.0f)) );

//		this.highlighter = new VImage(this.w*10, this.h*10);
//		highlighter.rectfill(0, 0, this.w*10, this.h*10, 
//			new Color(1.0f, 1.0f, 1.0f, 0.25f ) );

		this.myAction = core.Script.getFunction(Vmenuitem.class, 
				"nullAction" );

		return;
		}

	public boolean changeMode(Integer theModeNumber )
		{
			// in this type of menuitem, the mode is linked to what text string
			//  is shown,  so check for the data.
		if( theModeNumber > this.textItems.size() || theModeNumber < 0 )
			{ this.mode = 0;  return(false); }
		this.mode = theModeNumber;
		this.calcDims();
		return(true);
		}
	
	public VmiTextSimple( String text, int relX, int relY )
		{
		this(text);
		this.ulx = 0;
		this.uly = 0;
		this.rx = relX;
		this.ry = relY;
		return;
		}
	


	
	
	public void setFont( Font f )
		{
		this.fnt = f;		// easy enogh.

		// But - Changing fonts will change dimensions of box.  
		//   and that needs to be dealt with here.
		this.calcDims();

		return;
		}
	
	protected void calcTextArea()
		{
	// Thank you : http://stackoverflow.com/questions/258486/calculate-the-display-width-of-a-string-in-java
		String text = this.textItems.get(this.mode);
		if( text == null || text.length() == 0 || text.isEmpty() )
			{
			this.sx = 0;   this.sy = 0;
			return;
			}
		AffineTransform affinetransform = new AffineTransform();     
		FontRenderContext frc = 
				new FontRenderContext(affinetransform,true,true);

		this.sx = (int)( this.fnt.getStringBounds(text, frc).getWidth());
//		this.sx = Math.abs( (int)( this.fnt.getStringBounds(text, frc).getMaxX()) );
//		this.sy = (int)( this.fnt.getStringBounds(text, frc).getHeight());
		this.sy = Math.abs( (int) this.fnt.getStringBounds(text, frc).getMinY() );
		}
	
	protected void calcDims()
		{
		this.calcTextArea();
		int iconX = 0 ,iconY = 0;
		if( this.iconItems.get( 0 ) != null )	// Assume all icons the same size.
			{
			iconX = this.iconItems.get( 0 ).width;
			iconY = this.iconItems.get( 0 ).height;
			}
		// only tallest of string vs. icon matters.
		if( this.sy > iconY )  { iconY = 0; }
		else	{ iconY -= this.sy; }
		
		this.w = this.sx + (this.FrameThicknessPx*2)+iconX+9;
		this.h = this.sy + (this.FrameThicknessPx*2)+iconY+6;
		}
	
	public boolean reposition(int anchorX, int anchorY, int relPosX, int relPosY)
		{
			// refuse to send the item off the upper left of the screen.
		if( anchorX < 0 || anchorY < 0 )
			{ return(false); }
		if( (anchorX+relPosX) < 0 || (anchorY+relPosY) < 0 )
			{ return(false); }
		this.ulx = anchorX;
		this.uly = anchorY;
		this.rx = relPosX;
		this.ry = relPosY;
		return true;
		}
	
	public boolean repositionDelta( 
			int deltaX, int deltaY, int drelPosX, int drelPosY )
		{
			// refuse to send the item off the upper left of the screen.
		if( (this.ulx + deltaX) < 0 || (this.uly + deltaY) < 0 )
			{ return(false); }
		if( 	(this.ulx + deltaX + drelPosX) < 0 || 
			(this.uly + deltaY + drelPosY ) < 0 )
			{ return(false); }
		this.ulx += deltaX;
		this.uly += deltaY;
		this.rx += drelPosX;
		this.ry += drelPosY;
		return true;
		}

	public void setAction(Method action)
		{	this.myAction = action;	}

	public void paint( VImage target )
		{	this.paint( target, this.state ); 	}	 // paint in normal state.

	public void paint( VImage target, Integer state )
		{

		if( this.visible == false ) { return; }
		int x1 = this.ulx + this.rx;
		int y1 = this.uly + this.ry;
		int x2 = x1 + this.w;
		if( this.extX > this.w )		{ x2 = x1 + this.extX; }
		int y2 = y1 + this.h;
		if( this.extY > this.h )		{ y2 = y1 + this.extY; }
		
//		System.out.println("Paint menu item state : "+state.toString()+
//			" this ("+Integer.toString(this.state)+" )  @ "+
//			Integer.toString(x1)+" . "+
//			Integer.toString(y1)+" . "+
//			Integer.toString(x2)+" . "+
//			Integer.toString(y2)+" :: "+ 
//			Integer.toString(this.w)+" - "+
//			Integer.toString(this.extX)+" | "+
//			Integer.toString(this.h)+" - "+
//			Integer.toString(this.extY)
//			);

		int tmpX1, tmpY1, tmpX2, tmpY2;

		Color bgFlatColor;
		Color tColor;
		switch( state.intValue() )
			{
			case 1: 		// selected
				bgFlatColor = this.colorItems.get(
					enumMenuStxtCOLORS.BKG_ACTIVE.value() );
				tColor = this.colorItems.get(
					enumMenuStxtCOLORS.TEXT_ACTIVE.value() );
				break;
			case 2:		// activated
				bgFlatColor = this.colorItems.get(
					enumMenuStxtCOLORS.BKG_ACTIVE.value() );
				tColor = this.colorItems.get(
					enumMenuStxtCOLORS.TEXT_ACTIVE.value() );
				break;
			case 3:		// disabled.
				bgFlatColor = this.colorItems.get(
					enumMenuStxtCOLORS.BKG_INACTIVE.value() );
				tColor = this.colorItems.get(
					enumMenuStxtCOLORS.TEXT_INACTIVE.value() );
				break;
			default:	// Normal , or 0
				bgFlatColor = this.colorItems.get(
					enumMenuStxtCOLORS.BKG_ACTIVE.value() );
				tColor = this.colorItems.get(
					enumMenuStxtCOLORS.TEXT_ACTIVE.value() );
				break;
			}
		
		if( this.showBG == true && this.imageItems.get(state) != null )
			{
				// Scale desired image to fit the box.
			target.scaleblit(x1, y1, x2-x1,y2-y1, 
					this.imageItems.get(state) );
			}
		else		// Use a flat color rather than image.
			{
			if( this.showColorful == false )
				{ bgFlatColor = new Color( 0.0f, 0.1f, 0.0f, 0.0f ); }
			target.rectfill(x1, y1, x2-1, y2-1, bgFlatColor );
			}

		if( this.showIcon )
			{
			tmpX1 = x1 + 3 + this.FrameThicknessPx;
			tmpY1 = y1 + 3;
			target.blit(tmpX1, tmpY1, this.iconItems.get(this.state) );
			}

		if( this.showFrame )
			{
			Color fc;
			for( int fn = 0; fn < this.FrameThicknessPx; fn++ )
				{
				tmpX1 = x1 + fn;
				tmpY1 = y1 + fn;
				tmpX2 = x2 - fn;
				tmpY2 = y2 - fn;
				fc = this.colorItems.get(
					enumMenuStxtCOLORS.FRAME_INNER.value() );
				if( fn == 0 || fn == this.FrameThicknessPx-1 )
					{ 
					fc = this.colorItems.get(
						enumMenuStxtCOLORS.FRAME_OUTER.value() );
					}
				if( this.showColorful == false )
					{  fc = Color.WHITE; }

				if( fc == null )  {  fc = Color.WHITE; } 
				
				target.rect(tmpX1, tmpY1, tmpX2, tmpY2, fc );
				}
			}
		
		
		if( this.showText )
			{
			// Strings draw upward from a base at Y1.. making 
			//   this a bit awkward given we know where the upper left
			// 		 should be but are presented with the lower left.
			tmpX1 = x1+this.FrameThicknessPx+6+this.iconItems.get(state).width;
			tmpY1 = y1+3+this.FrameThicknessPx+this.sy;
			if( this.showColorful == false )
				{ tColor = Color.WHITE; }

			target.printString( tmpX1, tmpY1, this.fnt, tColor, 
				this.textItems.get( this.mode ) );
//		 show the "string rectangle"
//			target.rect(tmpX1, y1+3, tmpX1+this.sx, tmpY1, Color.WHITE );
			}

		// If its selected, highlight the background
		if( this.state == enumMenuItemSTATE.SELECTED.value() )
			{	target.rectfill(x1, y1, x2-1, y2-1, highlighter );	}
		if( this.state == enumMenuItemSTATE.ACTIVATED.value() )
			{	target.rectfill(x1, y1, x2-1, y2-1, highlighter2 );	}		

		return;
		}

	public Double getX()
		{	return new Double(this.ulx+this.rx);	}

	public Double getY()
		{	return new Double(this.uly+this.ry);	}

	public Double getDX()
		{
		this.calcDims();
		return new Double(this.w);		
		}

	public Double getDY()
		{
		this.calcDims();
		return new Double(this.h);
		}

	public void setTextContent( HashMap<Integer, String> textItems )
		{
		this.textItems = textItems;
		this.calcDims();
		return;
		}

	public void setIconContent(HashMap<Integer, VImage> imageItems)
		{	
		this.iconItems = imageItems;
		this.calcDims();
		return;
		}

	public void setColorContent( HashMap<Integer, Color> basicColors)
		{	this.colorItems = basicColors;	}

	public void setImageContent(HashMap<Integer, VImage> imageItems)
		{	this.imageItems = imageItems;	}

	public void enableActivation()
		{	this.active = true;	}

	public void enableText(boolean enableText)
		{	this.showText = enableText;	}

	public void enableIcons( boolean enable )
		{	this.showIcon = enable;	}

	public void enableBackdrop( boolean enable )
		{	this.showBG = enable;	}

	public void enableFrame(boolean enable)
		{	this.showFrame = enable;	}

	// Switches if object will be drawn, returns previous state.
	//  if this is false, then the paint() method will do nothing.
	public boolean setVisibility(boolean isVisible )
		{ 
		boolean save = this.visible; 
		this.visible = isVisible;
		return(save);
		}
	
	public Integer getKeycode()
		{	return this.keycode;	}

	public void setKeycode(Integer vergeKeyCode)
		{	this.keycode = vergeKeyCode.intValue();	}

	// Change the text under the current mode.
	public void setText( String newText ) 
		{
		this.textItems.put(this.mode, newText );
		this.calcDims();
		return;
		}

	/** Sets clipping bounds when drawing
	 *  !! not yet implemented !!
	 * @param xBound	full screen x integer pixel bound
	 * @param yBound	full screen x integer pixel bound
	 */
	public void setClipping(int xBound, int yBound )
		{
		this.boundX = xBound;
		this.boundY = yBound;
		System.out.println(" Bound set to : "+
				Integer.toString(this.boundX) + " / "+
				Integer.toString(this.boundY) ); 
		
		if( xBound < 1 )
			{ this.boundX = 1; }
		if( yBound < 1 )
			{ this.boundX = 1; }		
		return;
		}
	
	// Runs the attached Method - returns true if it was successfully invoked.
	public boolean doAction()
		{
		if( this.active == false )		{ return(false); }
		if( this.myAction == null ) 	{ return(false); }

//  This doesn't do what one may first think. - its not necessary.
//		if( ! this.myAction.isAccessible() )  
//			{ return(false); }

		try { 			// 	call the method. and prey
//			this.myAction.invoke(null);
	        if( this.myAction.getModifiers() == Modifier.STATIC )
	     	   { this.myAction.invoke( null ); }
	        else
	     	   { this.myAction.invoke( this ); }
			}
		catch(Exception e)
			{	
			e.printStackTrace();
			System.out.println( e.getMessage() );
			return(false); 
			}

		return(true);
		}

	public int getFrameThicknessPx()
		{	return FrameThicknessPx;	}

	public void setFrameThicknessPx(int frameThicknessPx)
		{ 
		if( frameThicknessPx <= 0 )
			{ 
			this.showFrame = false;
			frameThicknessPx = 0;
			}
		this.FrameThicknessPx = frameThicknessPx;
		this.calcDims();
		return;
		}

	// Sets icons for all states to a simple colored square of pixel size
	private void setAllColorIcon( int size, Color c )
		{
		for( enumMenuItemSTATE s : enumMenuItemSTATE.values() )
			{
			this.iconItems.put(s.value(), new VImage(size,size,c) );
			}
		return;
		}
	
	public  void setColor(enumMenuStxtCOLORS n, Color c )
		{
		if( c == null )  { return; }
		this.colorItems.put(n.value(), c );	
		}
	public void setState( Integer theState )
		{
		// this method is ineffective if the item is in a disabled state.
		//    you will need to reenable it first.
		if( this.state == enumMenuItemSTATE.DISABLED.value() )
			{ return; }
		this.state = theState;
//		System.out.println("menuitem set state => "+Integer.toString(this.state));
		this.calcDims();
		}
	public void setStateImages(enumMenuItemSTATE n, 
			VImage theIcon, VImage theBackGround )
		{
		this.imageItems.put(n.value(), theBackGround );
		this.iconItems.put(n.value(), theIcon );
		this.calcDims();
		return;
		}

	public String getText()
		{ return(this.textItems.get(this.mode)); }
	public VImage getBackground( enumMenuItemSTATE n )
		{ return(this.imageItems.get( n.value() )); }
	public VImage getIcon( enumMenuItemSTATE n )
		{ return(this.iconItems.get( n.value() )); }

	public boolean isActive()
		{ return(this.active ); }
	public boolean isVisible()
		{ return( this.visible ); }
	public Integer getState()
		{ return( this.state ); }
	
	/**
	 * These allow Vmenu objects control the width and height of 
	 * an arrangement of Vmenuitems. by adding blank space
	 */
	public void setExtendX( int desiredWidth, boolean onlyIfGreater )
		{
		if( desiredWidth < this.w )
			{ this.extX = 0;   return; }
		if( onlyIfGreater == true && (desiredWidth <= this.extX  ) )
			{	return;	}
		this.extX = desiredWidth;
		return;
		}
	public void setExtendY( int desiredHeight, boolean onlyIfGreater )
		{
		if( desiredHeight < this.h )
			{ this.extY = 0;   return; }
		if( onlyIfGreater == true && (desiredHeight <= this.extY  ) )
			{ return; }
		this.extY = desiredHeight;
		return;
		}

	public int getExtendX()
		{ return(this.extX); }
	public int getExtendY()
		{ return(this.extY); }

	private String getStateString()
		{
		String rslt = "UNKNOWN";
		
		for( enumMenuItemSTATE e : enumMenuItemSTATE.values() )
			{
			if( e.ordinal() == this.state )
				{ rslt = e.getName(); }
			}

		return(rslt);
		}
	
	public void setParentID( Long id )
		{	this.parentID = id;	}
	public void setChildID( Long id )
		{ 	this.childID = id;	}
	public Long getParentID()
		{	return(this.parentID);	}
	public Long getChildID()
		{	return(this.childID);	}

	public  Integer debug_function( Integer ignore )
		{
		this.textItems.put(this.mode, this.getStateString() );
		this.calcDims();
		return(0);
		}

	/**		transfers control to the menu that is linked by parentID
	 */	
	public void goParent()
		{
//		System.out.println("focusing parent menu # "+
//				this.parentID.toString() );
		if( this.parentID < 0 ) { return; }
		setMenuFocus( 0, this.parentID );
		return;
		}
	/**		transfers control to the menu that is linked by childID
	 */
	public void goChild()
		{
//		System.out.println("Switching to child menu # "+
//				this.childID.toString() );
		if( this.childID < 0 ) { return; }
		setMenuFocus( 0, this.childID );
//		MENU_FOCUS[0] = this.childID;
		return;
		}

	public Long getId()
		{ return(this.id); }

	/**  Takes in an external string
	 *   by default, replaces the current text with it.
	 */
	public void processInput( String input )
		{
		if( this.textItems.containsKey( this.mode ) )
			{
			String dat = this.textItems.get( this.mode );
			this.textItems.put( this.mode * -1, dat );
			}
		this.textItems.put( this.mode, input );
		return;
		}
	
	
	public String[] getTip()
		{
		String[] rtn = new String[2];
		rtn[0] = this.tip;
		rtn[1] = this.desc;
		return(rtn);
		}
	
	public void setTip( String[] descriptions )
		{
		if( descriptions == null )	
			{ return; }
		if( descriptions.length >= 1 )	
			{ this.tip  = descriptions[0]; }
		if( descriptions.length >= 2 )	
			{ this.desc  = descriptions[1]; }
		return;
		}
	
	public void setTip( String helpCaption )
		{
		this.tip = helpCaption;
		this.desc = helpCaption;
		return;
		}
	
	public boolean isTextEnabled()
		{ return( this.showText ); }
	public boolean isBackgroundEnabled()
		{ return( this.showBG ); }
	public boolean isIconEnabled()
		{ return( this.showIcon ); }
	public boolean isColorful()
		{ return( this.showColorful ); }
	public boolean isBorderEnabled()
		{ return( this.showFrame ); }

	}

