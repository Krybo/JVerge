package menus;

import static core.Script.setMenuFocus;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;

import domain.VImage;

/** A button that can execute a function.  Features:
 * stateful Image backgrounds
 * circular or square default appearance.
 * Menus cannot adjust dimensions.  width and height are constant.
 *      but they can adjust the anchor points through relative x/y
 * Minimum dimension: 10 pixels.   smaller values will be auto-raised.
 * Center or edge focused on anchor point.
 * structure:  padding, colored border with thickness control, main body
 * (Krybo Apr.2016)
 * @author Krybo
 *
 */

public class VmiButton implements Vmenuitem
	{
	private final static int MIN_DIMENSION_PX = 10;
	private int ax,ay = 0;		// Anchor points
	private int rx,ry;				// relative x/y.
	private int w,h = 1;			// Total allocated space.
	private int keycode = -1;	// key stroke used to activate
	private int FrameThicknessPx = 1;	// programmatic shell thickness.
	private int shadowThicknessPx = 1;	// basically, same as padding.

	private Integer state;

	private boolean isImage = false;
	private boolean shadow = false;
	private boolean circular = false;
	private boolean centered = false;
	private boolean active = false;
	private boolean visible = false;

	private Long parentID = new Long(-1);
	private Long id = new Long(-1);
	private Long childID = new Long(-1);
	
	private String tip = new String("");
	private String desc = new String("");
	
	private Color highlighter = new Color(1.0f, 1.0f, 1.0f, 0.36f );
	private Color highlighter2 = new Color(1.0f, 1.0f, 0.2f, 0.50f );
	
		// Used when circular mode is on to mask out corners.
	private VImage circleMask = null;
	private boolean circleMaskUpdateRequired = false;
	
	// images for various states
	private HashMap<Integer, VImage> hmImageItems = 
		new HashMap<Integer, VImage>();
	// Colors for various states and components.
	private HashMap<Integer, Color> hmColorItems = 
		new HashMap<Integer, Color>();

	// Da buziness end.
	private Method myAction = null;
	private Object[] actionArgs;
	
	public static enum enumMenuButtonCOLORS
		{
		BODY_ACTIVE (0),
		BODY_INACTIVE (1),
		BODY_SELECTED (2),
		FRAME_OUTER (3),
		FRAME_INNER (4),
		FRAME_INACTIVE (5),
		FRAME_SHADOW (6);

		private final Integer index;
		enumMenuButtonCOLORS( Integer n )
			{	this.index = n;	}
		public Integer value()
			{ return(this.index); }
		public int val()
			{ return(this.index.intValue()); }
		}

	
/** --------------------- Constructs ------------------------------------------- */

	/** Null constructor : 
	 * makes a default-o button with the smallest size possible
	 * */
	public VmiButton( )
		{
		this( getMinDimensionPx(), getMinDimensionPx() );
		}
	
	/**  Defaultor constructor	 */
	public VmiButton( int width, int height )
		{
		this( 1, 1, 0, 0, width, height, 2, 
				false, false, false, false, true );		
		return;
		}
	
	/**  Full constructor	 */
	public VmiButton( int posX, int posY, int relX, int relY, 
		int width, int height, int frameThickness, 
		boolean useImages, boolean isCircular, boolean anchoredCenter,
		boolean isShadowed, boolean visibility )
		{
		this.id = Vmenuitem.getRandomID();
		
		this.rx = relX;		this.ry = relY;
		this.ax = posX;	this.ay = posY;
		this.w = width;	this.h = height;
		this.keycode = -1;
		this.state = new Integer(0);
		this.FrameThicknessPx = frameThickness;
		
		// Enforce minimum dimensions.
		if( this.w < VmiButton.getMinDimensionPx() )	
			{
			System.err.println(
				"VmiButton : constructor WTH smaller then allowed.");
			this.w = VmiButton.getMinDimensionPx(); 
			}
		if( this.h < VmiButton.getMinDimensionPx() )	
			{
			System.err.println(
				"VmiButton : constructor HGT smaller then allowed.");
			this.h = VmiButton.getMinDimensionPx(); 
			}

		this.circleMask = new VImage(this.w,this.h,
				core.Script.Color_DEATH_MAGENTA );
		this.circleMaskUpdateRequired = isCircular;

		this.isImage = useImages;
		this.setCircular(isCircular);
		this.visible = visibility;
		this.setCentered(anchoredCenter);
		this.shadow = isShadowed;
		
		this.active = true;
		this.setDefaultHashMapSettings();
				
		this.myAction = core.Script.getFunction(Vmenuitem.class, 
				"nullAction" );
		this.actionArgs = new Object[]{};

		return;
		}
	

/** --------------------- Methods --------------------------------------------- */	

	private void setDefaultHashMapSettings()
		{
		// Load default.. simple images
		this.hmImageItems.put(enumMenuItemSTATE.ACTIVATED.value(), 
			new VImage( this.w,this.h,Color.GREEN ) );
		this.hmImageItems.put(enumMenuItemSTATE.DISABLED.value(), 
			new VImage( this.w,this.h,Color.GRAY ) );
		this.hmImageItems.put(enumMenuItemSTATE.NORMAL.value(), 
			new VImage( this.w,this.h,Color.BLUE ) );
		this.hmImageItems.put(enumMenuItemSTATE.SELECTED.value(), 
			new VImage( this.w,this.h,Color.YELLOW ) );

		this.hmColorItems.put(
				enumMenuButtonCOLORS.BODY_ACTIVE.value(),
				new Color( 0.6f,0.6f,0.6f,1.0f ) );
		this.hmColorItems.put(
				enumMenuButtonCOLORS.BODY_INACTIVE.value(), 
				new Color(0.33f,0.33f,0.33f,1.0f) );
		this.hmColorItems.put(
				enumMenuButtonCOLORS.BODY_SELECTED.value(), 
				Color.GREEN );
		this.hmColorItems.put(
				enumMenuButtonCOLORS.FRAME_INACTIVE.value(), 
				new Color(0.63f,0.63f,0.63f,1.0f) );
		this.hmColorItems.put(
				enumMenuButtonCOLORS.FRAME_INNER.value(), 
				Color.WHITE );
		this.hmColorItems.put(
				enumMenuButtonCOLORS.FRAME_OUTER.value(), 
				Color.WHITE );
			// Shadow should be a translucent color.
		this.hmColorItems.put(
				enumMenuButtonCOLORS.FRAME_SHADOW.value(), 
				new Color( 0.13f,0.13f,0.13f,0.3f ) );

		return;
		}
	
	public boolean reposition(int anchorX, int anchorY, 
			int relPosX, int relPosY)
		{
			// refuse to send the item off the upper left of the screen.
		if( anchorX < 0 || anchorY < 0 )
			{ return(false); }
		if( (anchorX+relPosX) < 0 || (anchorY+relPosY) < 0 )
			{ return(false); }
		this.ax = anchorX;
		this.ay = anchorY;
		this.rx = relPosX;
		this.ry = relPosY;
		return true;
		}

	public boolean repositionDelta( 
			int deltaX, int deltaY, int drelPosX, int drelPosY )
		{
			// refuse to send the item off the upper left of the screen.
		if( (this.ax + deltaX) < 0 || (this.ay + deltaY) < 0 )
			{ return(false); }
		if( 		(this.ax + deltaX + drelPosX) < 0 || 
				(this.ay + deltaY + drelPosY ) < 0 )
			{ return(false); }
		this.ax += deltaX;
		this.ay += deltaY;
		this.rx += drelPosX;
		this.ry += drelPosY;
		return true;
		}

	public void setAction(Method action)
		{	this.myAction = action;	}
	public Method getAction()
		{ return(this.myAction); }
	
	/** no font involved.   this does nothing but furfill interface. */
	public void setFont( Font newFont )
		{ return; }

	/** Get the argument(s) passed when doAction is called.
	 * @return the actionArgs
	 */
	public Object[] getActionArgs()
		{	return actionArgs;	}
	/**  Set the argument(s) to pass when doAction is called.
	 * @param actionArgs the actionArgs to set
	 */
	public void setActionArgs(Object[] actionArgs)
		{	this.actionArgs = actionArgs;	 return; }

	// Runs the attached Method - returns true if it was successfully invoked.
	public boolean doAction()
		{
		if( this.active == false )		{ return(false); }
		if( this.myAction == null ) 	{ return(false); }

 		Type[] pType = myAction.getGenericParameterTypes();
 			// arguments are definately not right.  Stop.
 		if( pType.length != this.getActionArgs().length )
 			{
 			System.err.println("Prevented activation of method : "+
				this.myAction.getName() + " due to argument mismatch!" );
 			return(false); 
 			}
		
		System.out.println("DEBUG : Attempting to activate method : "+
				this.myAction.getName() + " with " + 
				Integer.toString( this.getActionArgs().length ) + " args (" +
				this.myAction.getModifiers() + ")."  );
		if( this.getActionArgs().length > 0 )
			{
			Integer n = -1;
			for( Object c : this.getActionArgs() )
				{
				n++;
				System.out.println(" ARG "+n.toString()+
					": Taken Type " + c.getClass().getTypeName()  +
					" :: Given Type "+c.getClass().getName() );
				if( pType[n].equals( c.getClass() ) == false ) 
					{
					System.err.println("doAction: Prevented type mismatch.");
					}
				}
			}

		try { 			// 	call the method. and prey
//			this.myAction.invoke(null);

	        if( (this.myAction.getModifiers() & Modifier.STATIC)
	     		   == Modifier.STATIC  )
	     	   { this.myAction.invoke( null, (Object[])this.getActionArgs() ); }
	        else
	     	   {
//	     	   this.myAction.invoke( this.getClass().getInterfaces(), this.actionArgs );
	     	   
	     	   boolean localTest = false;
	     	   for( Method mTest : this.getClass().getDeclaredMethods() )
	     		   {
	     		   if( mTest.getName().equals(myAction.getName()))
	     			   { localTest = true; }
	     		   }
	     	   if( localTest == false )		
	     		   {
	     		   System.err.println("Invoking instanced method not"+
     				   "belonging to the local Class is unsupported!  "+
     				   "Either make it a static method or add it to this Class");
	     		   return(false); 
	     		   }
	     	   else
	     		   {
	     		   this.myAction.invoke( this, this.getActionArgs() );
//	     		   this.myAction.invoke( myAction.getClass().newInstance(), 
//	     			   new Object[]{} );
	     		   }
	     	   }
			}
		catch(Exception e)
			{
			e.printStackTrace();
			System.err.println( e.getMessage() );
			return(false); 
			}

		return(true);
		}
	


	public void paint(VImage target)
		{
		if( this.visible == false ) { return; }

		int x1 = (this.ax + this.rx) - (this.w / 2);
		int y1 = (this.ay + this.ry)  - (this.h / 2);
		int x2 = x1 + this.w - 1;
		int y2 = y1 + this.h - 1;

		if( this.isCentered() == false ) 
			{
			x1 = this.ax + this.rx;
			y1 = this.ay + this.ry;
			x2 = x1 + this.w;
			y2 = y1 + this.h;
			}

		if( x1 < 0 ) { x1 = 0; x2 = this.w; }
		if( y1 < 0 ) { y1 = 0; y2 = this.h; }
		
//		System.out.println("Paint button @ "+
//				Integer.toString(x1)+" / "+
//				Integer.toString(y1)+" :: "+
//				Integer.toString(this.rx)+" / "+
//				Integer.toString(this.ry)	);

			// Allocate room for shadows.
		if( this.shadow == true )
			{
			x1 += 1;
			y1 += 1;
			x2 -= this.shadowThicknessPx;
			y2 -= this.shadowThicknessPx;
			}

			// We're drawing a circle/oval, not a square.
		int radX = 0;
		int radY = 0;
		int xC = x1 - 1 + (this.w / 2);
		int yC = y1 - 1 + (this.h / 2);
		if( this.isCircular() == true )
			{
			radX = ((x2 - x1) / 2) - 2;
			radY = ((y2 - y1) / 2) - 2;
			if( this.shadow == true && this.shadowThicknessPx > 0 )
				{
				xC -= this.shadowThicknessPx ;
				yC -= this.shadowThicknessPx ;
				radX = ((x2 - x1 - this.shadowThicknessPx) / 2) - 2;
				radY = ((y2 - y1- this.shadowThicknessPx) / 2) - 2;
				}
			if( this.circleMaskUpdateRequired == true )
				{
				this.circleMask = 
						new VImage( this.hmImageItems.get(this.state) );
				int nix = (radX+radY)/2*3;
				for( Integer clearer = 1; clearer < nix; clearer++ )
					{
//					this.circleMask.line(0, clearer, 20, clearer, Color.WHITE );
					int tmpXc = (this.circleMask.width / 2) - 1;
					int tmpYc = (this.circleMask.height / 2) - 1;
					this.circleMask.circleTrans( tmpXc, tmpYc, 
							tmpXc+clearer, tmpYc+clearer, 
							core.Script.Color_CLEAR,
//							new Color(255,0,255),
							this.circleMask );
					this.circleMask.circleTrans( tmpXc+1, tmpYc, 
							tmpXc+clearer, tmpYc+clearer, 
							core.Script.Color_CLEAR,
							this.circleMask );
					}

				this.circleMaskUpdateRequired = false;
				}
			}
		
		Color bodyColor;
		Color shellColor;
		Color shadowColor = new Color(0.0f,0.0f,0.0f,0.0f);

		switch( state.intValue() )
			{
			case 1: 		// selected
				bodyColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.BODY_SELECTED.value() );
				shellColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.FRAME_OUTER.value() );
				break;
			case 2:		// activated
				bodyColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.BODY_ACTIVE.value() );
				shellColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.FRAME_OUTER.value() );
				break;
			case 3:		// disabled.
				bodyColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.BODY_INACTIVE.value() );
				shellColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.FRAME_INACTIVE.value() );
				break;
			default:
				bodyColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.BODY_ACTIVE.value() );
				shellColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.FRAME_OUTER.value() );
				break;
			}
		
		int tmpX1, tmpY1, tmpX2, tmpY2;
		
		if( this.shadow )
			{
			shadowColor = this.hmColorItems.get(
					enumMenuButtonCOLORS.FRAME_SHADOW.value() );

			// need to draw a circle-mode shadow here before other parts.
			if( this.circular == true )
				{
				for( int fsn = 0; fsn < this.shadowThicknessPx; fsn++ )
					{
					target.circleTrans( xC+1+fsn, yC+1+fsn, 
							radX, radY, shadowColor,  target );
					}
				}
			}
		
		if( this.isImage == true )
			{
			// Circular Image body
			if( this.isCircular() == true )
				{
				target.scaleblit( x1, y1, x2-x1, y2-y1, 
					 this.circleMask );
				}
			else		// Square Image body
				{
				target.scaleblit( x1+1, y1+1,
					x2-x1, 	y2-y1, 
					this.hmImageItems.get( state ) );
				}
			}
		else			// Draw shapes, not images.
			{
			if( this.isCircular() == true )
				{
				target.circlefill( xC, yC, radX-1, radY-1, bodyColor );
				}
			else
				{
				target.rectfill( x1, y1, x2-1, y2-1, bodyColor );
				}
			}
		
		if( this.isCircular() == true && this.FrameThicknessPx > 0 )
			{
			if( this.shadow == true )
				{
				target.circleTrans( xC, yC, radX+1, radY+1, 
						shadowColor, target );
				target.circleTrans( xC+1, yC, radX+1, radY+1, 
						shadowColor, target );
				}
			for( int fn = 0; fn < this.FrameThicknessPx; fn++ )
				{
			// need to draw two of these or sparse pixeling will ruin appearance
				target.circle( xC, yC, radX-fn, radY-fn, shellColor, target );
				target.circle( xC+1, yC, radX-fn, radY-fn, shellColor, target );
				}
			}
		if( this.isCircular() == false && this.FrameThicknessPx > 0 )
			{
			for( int fn = 0; fn < this.FrameThicknessPx; fn++ )
				{
				tmpX1 = x1 + fn;
				tmpY1 = y1 + fn;
				tmpX2 = x2 - fn;
				tmpY2 = y2 - fn;
					// Do shadowing here.
				if( fn == 0 && this.shadow == true )
					{
					target.rectTrans( tmpX1-1, tmpY1-1, tmpX2+1, tmpY2+1, 
							shadowColor );
					for( int fs = 1; fs < this.shadowThicknessPx; fs++ )		
						{
						target.lineTrans( tmpX1+fs-1, tmpY2+fs, 
							tmpX2+fs, tmpY2+fs,  shadowColor );
						target.lineTrans( tmpX2+fs, tmpY1+fs-1, 
							tmpX2+fs, tmpY2+fs, 	shadowColor );
						}
					}
				target.rect( tmpX1, tmpY1, tmpX2, tmpY2, shellColor );
				}
			}
		
		// If its selected, highlight the background
		if( this.state == enumMenuItemSTATE.SELECTED.value() && 
				this.isImage == false )
			{	target.rectfill(x1, y1, x2-1, y2-1, this.highlighter );	}
		if( this.state == enumMenuItemSTATE.ACTIVATED.value() && 
				this.isImage == false  )
			{	target.rectfill(x1, y1, x2-1, y2-1, this.highlighter2 );	}
		
		return;
		}

	// nada - buttons are "mode-less"
	public boolean changeMode(Integer theModeNumber)
		{	return false;	}

	public Double getX()
		{	return( new Double( this.ax) );	}

	public Double getY()
		{	return( new Double( this.ay) );	}

	public Double getDX()
		{	return( new Double(this.w) );	}

	public Double getDY()
		{	return( new Double(this.h) );	}

	/** Buttons cannot be extended  */
	public void setExtendX(int itemWidth, boolean onlyIfGreater)
		{	return;	}
	public void setExtendY(int itemHeight, boolean onlyIfGreater)
		{  return;  }

	/** Buttons have no text or icons. */
	public void setTextContent(HashMap<Integer, String> textItems)
		{	return;	}
	public void setIconContent(HashMap<Integer, VImage> imageItems)
		{	return;	}

	public void setColorContent(
			HashMap<Integer, Color> basicColors)
		{
		if( basicColors == null )	{ return; }
		for( Integer cn : basicColors.keySet() )
			{
			this.hmColorItems.put(cn, basicColors.get(cn) );
			}
//		this.hmColorItems = basicColors;
		return;
		}
	
	public Color getColorComponent( Integer hashKeyColor )
		{	return( this.hmColorItems.get(hashKeyColor) );	}

	public void setImageContent(
			HashMap<Integer, VImage> imageItems)
		{
		this.hmImageItems = imageItems;
		this.isImage = true;
		this.circleMaskUpdateRequired = true;
		return;
		}

	public void enableActivation()
		{
		this.active = true;
		return;
		}

	public void enableText(boolean enableText)
		{	return;	}

	public void enableIcons(boolean enable)
		{	return;	}

	// There is no backdrop.
	public void enableBackdrop(boolean enable)
		{	return;	}

	// Frame cannot be disabled.
	public void enableFrame(boolean enable)
		{	return;	}

	public void setState(Integer itemState)
		{
		if( this.state != itemState )
			{ this.circleMaskUpdateRequired = true; }
		this.state = itemState;
		return;
		}

	public Integer getState()
		{	return( this.state );	}

	public boolean isActive()
		{	return( this.active );	}

	public boolean isVisible()
		{	return( this.visible );	}

	public Integer getKeycode()
		{	return( this.keycode );	}

	public void setKeycode(Integer vergeKeyCode)
		{
		this.keycode = vergeKeyCode;
		return;
		}

	public void setFrameThicknessPx(int thick)
		{
		this.FrameThicknessPx = thick;
		if( this.FrameThicknessPx < 0  )
			{ this.FrameThicknessPx = 0; }
		return;
		}

	public Integer debug_function(Integer argv)
		{
		return null;
		}
	

	public void setParentID( Long id )
		{	this.parentID = id;	}
	public void setChildID( Long id )
		{ 	this.childID = id;	}
	public Long getParentID()
		{	return(this.parentID);	}
	public Long getChildID()
		{	return(this.childID);	}
	public Long getId()
		{	return( this.id );	}

	// Buttons do not take input.
	public void processInput(String input)
		{	return;	}
	
	/**		transfers control to the menu that is linked by childID	 */
	public void goChild()
		{
		if( this.childID < 0 ) { return; }
		setMenuFocus( 0, this.childID );
		return;
		}

	/**		transfers control to the menu that is linked by parentID	 */	
	public void goParent()
		{
		if( this.parentID < 0 ) { return; }
		setMenuFocus( 0, this.parentID );
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

/**  ------------- Non-interfaced special methods --------------------------- */
	
	/**  Sets the image for one state of the button.
	 * The image sent should be relatively high resolution to avoid odd
	 * looking artifacts around the border-shadow-image area.  Especially
	 * when the button is in circular mode.   The corners of the image will
	 * also be clipped away in circular mode.
	 * 
	 * @param e	The state, use {@link enumMenuButtonCOLORS}
	 * @param img The image to use when button is in this state.
	 */
	public void setImageComponent( enumMenuItemSTATE e, 
			VImage img )
		{	
		this.hmImageItems.put( e.value(), img);
		this.circleMaskUpdateRequired = true;
		return;
		}

	public void setColorComponent( enumMenuButtonCOLORS e, 
			Color newColor )
		{	
		this.hmColorItems.put( e.value(), newColor );
		return;
		}
	
	public void setUseImages( boolean setting )
		{
		this.isImage = setting;
		if( setting == true )
			{ this.circleMaskUpdateRequired = true; }
		return;
		}

	public int getShadowThickness()
		{	return shadowThicknessPx;	}

	public void setShadowThicknessPx(int shadowThicknessPx,
			boolean enableShadow )
		{
			// Max Shadow is only allowed to be 10% of the average 
			//  width & height average.
		int uLimit  = (this.w + this.h) / 20;
		if( uLimit < 1 )   { uLimit = 1; }
		if( shadowThicknessPx < 1 )
			{
			this.shadowThicknessPx = 0;	
			this.shadow = false;
			return;
			}

		if( shadowThicknessPx >= uLimit )
			{
			this.shadowThicknessPx = uLimit;	
			this.shadow = enableShadow;	
			return;
			}
		
		this.shadowThicknessPx = shadowThicknessPx;
		this.shadow = enableShadow;
		return;
		}

	public boolean isCentered()
		{	return(this.centered);	}
	public void setCentered(boolean centered)
		{
		this.centered = centered;
		return;
		}

	public boolean isCircular()
		{	return( this.circular);	}

	/**  Set up this button to draw a circular shape instead of a square.
	 * @param circular  true to draw circlular button.
	 */

	public void setCircular( boolean circular )
		{
		this.circular = circular;
		}
	
	/**  Resizes the buttons width/height.
	 * 
	 * @param newWidth	new width in pixels
	 * @param newHeight	new height in pixels
	 * @return	true if resize was 100% successful, else false
	 */
	public boolean resize( int newWidth, int newHeight )
		{
		if( this.w == newWidth  && this.h == newHeight )
			{ return(false); }

		boolean rtn = true;
		this.w = newWidth;
		this.h = newHeight;
		
		if( this.w < VmiButton.getMinDimensionPx() )	
			{
			System.err.println(
				"VmiButton : resize WTH smaller then allowed.");
			this.w = VmiButton.getMinDimensionPx();
			rtn = false;
			}
		if( this.h < VmiButton.getMinDimensionPx() )	
			{
			System.err.println(
				"VmiButton : resize HGT smaller then allowed.");
			this.h = VmiButton.getMinDimensionPx();
			rtn = false;
			}

		this.circleMaskUpdateRequired = true;
		return( rtn );
		}

	public static int getMinDimensionPx()
		{	return MIN_DIMENSION_PX;	}

	public void setTip( String helpCaption )
		{
		this.tip = helpCaption;
		this.desc = helpCaption;
		return;
		}

	/**  Get relative X pixel position. 
	 * Typicall zero if it is not referenced by anything. */
	public int getXrel()
		{ return(this.rx); }
	/**  Get relative Y pixel position. 
	 * Typicall zero if it is not referenced by anything. */
	public int getYrel()
		{ return(this.ry); }
	
	}
