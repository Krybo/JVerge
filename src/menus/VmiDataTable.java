package menus;

import static core.Script.setMenuFocus;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;

import core.Controls;
import domain.VImage;

public class VmiDataTable implements Vmenuitem
	{
	
	private int x = 0, y = 0, w = 0, h = 0;
	private int rownum = 0;
	private int colnum = 0;
	private Double colMaxW = new Double(0);
	private Double rowMaxH = new Double(0);
	private int borderWidthPx = 3;
	private int borderPaddingPx = 10;
	private boolean active = false;		// not selectable, ever.
	private boolean visible = true;
	private boolean useLabels = false;
	private boolean useImagesAsLabels = false;
	private boolean scanMode = true;
	private boolean enableGrid = true;
	private boolean enableBorder = true;
	private boolean enableImageBackdrop = false;
	private String labelTerminator = ":";
	
		// These are required by all menu items.
	private Long parentID = new Long(-1);
	private Long id = new Long(-1);
	private Long childID = new Long(-1);
	
	private VImage bkgImg = null;

		// Key data members.
	private HashMap<Integer,String> theData;
	private HashMap<Integer,String> theLabels;
	private HashMap<Integer,VImage> imgLabels;
	private HashMap<Integer,Integer> theXpos;
	private HashMap<Integer,Integer> theYpos;
	private HashMap<Integer,Color> clrSettings;

	private Font fnt = core.Script.fntMASTER;
	
	public static enum enumMenuDataTableCOLORS
		{
		TEXT_DATA (0),
		TEXT_LABELS (1),
		FRAME_OUTER (2),
		FRAME_INNER (3),
		BKG_MAIN (4),
		BKG_CAPTION (5),
		GRIDLINES_X (6),
		GRIDLINES_Y (7);
		
		private final Integer index;
		enumMenuDataTableCOLORS( Integer n )
			{	this.index = n;	}
		public Integer value()
			{ return(this.index); }
		public int val()
			{ return(this.index.intValue()); }
		}


	/** --------------------------------------------------------------------------- **/


	public VmiDataTable(int pinX, int pinY, int pixelWidth, int pixelHeight,
			int numColumns, int numRows, LinkedHashMap<String,String> entries )
		{
		this.x = pinX;
		this.y = pinY;
		this.w = pixelWidth;
		this.h = pixelHeight;
		this.colnum = numColumns;
		this.rownum = numRows;
		
			// Critical: Init data arrays 
		this.theData 		= new HashMap<Integer,String>();
		this.theLabels 	= new HashMap<Integer,String>();
		this.imgLabels		= new HashMap<Integer,VImage>();
		this.theXpos		= new HashMap<Integer,Integer>();
		this.theYpos 		= new HashMap<Integer,Integer>();
		this.clrSettings 	= new HashMap<Integer,Color>();
		
		this.active = false;
		this.visible = true;
		this.enableImageBackdrop = false;

		this.resolvePositions( entries );
			
			// Define Default colors.
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.BKG_CAPTION.value(), 
				new Color(0.0f,0.33f,0.0f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.BKG_MAIN.value(), 
				new Color(0.05f,0.05f,0.05f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.FRAME_INNER.value(), 
				new Color(0.5f,0.5f,0.5f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.FRAME_OUTER.value(), 
				new Color( 1.0f,1.0f,1.0f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.GRIDLINES_X.value(), 
				new Color( 0.8f,0.8f,0.8f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.GRIDLINES_Y.value(), 
				new Color( 0.8f,0.8f,0.8f,1.0f) );			
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.TEXT_DATA.value(), 
				new Color( 1.0f,1.0f,1.0f,1.0f) );
		this.clrSettings.put( 
				enumMenuDataTableCOLORS.TEXT_LABELS.value(), 
				new Color( 1.0f,1.0f,0.77f,1.0f) );
		
		return;
		}

	private void resolvePositions( HashMap<String,String> entries )
		{
		int indent = this.borderPaddingPx+this.borderWidthPx; 
			// Pixel space to allocate to each column
		this.colMaxW = new Double( this.w - (2*indent)) / this.colnum;
		this.rowMaxH = new Double( this.h - (2*indent)) / this.rownum;		
	
		int td = 1, tr = 1;
		Integer n = 0;
		for(  String key : entries.keySet() )
			{
			Double tdw = new Double( td - 1 ) * this.colMaxW;
	//		Double trw = new Double( tr - 1 ) * this.colMaxW;
	//		Double tdh = new Double( td - 1 ) * this.rowMaxH;
			Double trh = new Double( tr - 1 ) * this.rowMaxH;
	
			this.theData.put( n, entries.get(key) );
			this.theLabels.put( n, key );
			this.theXpos.put(n,  tdw.intValue() + indent );
			this.theYpos.put(n, trh.intValue() + indent );
	
			n++;
			
				// Layout the table according to scanmode.
			if( this.scanMode == true )
				{
				td++;
				if( td > this.colnum )
					{
					td = 1;
					tr++;
					}
				}
			else		// Fill table by Rows.
				{
				tr++;
				if( tr > this.rownum )
					{
					tr = 1;
					td++;
					}
				}
			}
		return;
		}
	
	//  This one just resolves the positions of existing data and labels.
	private void resolvePositions( )
		{
		int indent = this.borderPaddingPx+this.borderWidthPx; 
			// Pixel space to allocate to each column
		this.colMaxW = new Double( this.w - (2*indent)) / this.colnum;
		this.rowMaxH = new Double( this.h - (2*indent)) / this.rownum;		
	
		int td = 1, tr = 1;
		Integer n = 0;
		for( Integer keyint : this.theData.keySet() )
			{
			String key = this.theLabels.get(keyint);
			Double tdw = new Double( td - 1 ) * this.colMaxW;
			Double trh = new Double( tr - 1 ) * this.rowMaxH;
	
			System.out.println("DEBUG  tdw = "+tdw.toString());
			
			this.theXpos.put(n,  tdw.intValue() + indent );
			this.theYpos.put(n, trh.intValue() + indent );
	
			n++;
			
				// Layout the table according to scanmode.
			if( this.scanMode == true )
				{
				td++;
				if( td > this.colnum )
					{
					td = 1;
					tr++;
					}
				}
			else		// Fill table by Rows.
				{
				tr++;
				if( tr > this.rownum )
					{
					tr = 1;
					td++;
					}
				}
			}
		return;
		}


	
	public boolean reposition(int posX, int posY, int relPosX, int relPosY)
		{
		if( (posX+relPosX < 0) || (posY+relPosY < 0) )
			{ return(false); }
		this.x = posX+relPosX;
		this.y = posY+relPosY;
		this.resolvePositions();
		return true;
		}

	/**  This Vmenutem is for display only.   No actions attached.	 */
	public void setAction(Method action)
		{	return;	}
	/**  This Vmenutem is for display only.   No actions attached.	 */
	public boolean doAction()
		{	return false;	}

	/**  Draws the contents of this menuitem Data table onto 
	 *    the provided VImage
	 */
	public void paint( VImage target )
		{
		if( this.visible == false ) { return; }
		// For convenience. - actual start of data area.
		// calc the corners for box and its margin.
		int x0 = this.x;
		int y0 = this.y;
		int x1 = x0 + this.borderWidthPx + this.borderPaddingPx;
		int y1 = y0 + this.borderWidthPx + this.borderPaddingPx;
		int x2 = x0 + this.w;
		int y2 = y0 + this.h;
		int xw = x2 - this.borderWidthPx - this.borderPaddingPx;
		int yh = y2 - this.borderWidthPx - this.borderPaddingPx;
		Double tmp;

			// recalculate this in case margins have changed.
		this.colMaxW = new Double( xw - x1) / this.colnum;
		this.rowMaxH = new Double( yh - y1) / this.rownum;

			// Background bounding box (or image)
		if( this.enableImageBackdrop == true )
			{
				// Scale desired image to fit the box.
			target.scaleblit( x0, y0, this.w, this.h, this.bkgImg ); 
			}
		else
			{
			target.rectfill( x0, y0, x2, y2, 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.BKG_MAIN.value()) );
			}

			// Draw gridlines
		if( this.enableGrid == true )
			{

			for( double a = 1.0d; a < this.colnum; a += 1.0d )
				{
				tmp = a * this.colMaxW;
//				System.out.println(" Column "+Double.toString(a)+ " = "+tmp.toString() 
//						+ " { " + this.colMaxW.toString() + " } " );
				target.line( x1+tmp.intValue(), y0, 
					x1+tmp.intValue(), y2, this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_Y.value()));
				}
			for( double b = 1.0d; b < this.rownum; b += 1.0d )
				{
				tmp = b * this.rowMaxH;
//				System.out.println(" Row "+Double.toString(b)+ " = "+tmp.toString()
//						+ " { " + this.rowMaxH.toString() + " } " );
				target.line( x0, y1+tmp.intValue(), x2, 
					y1+tmp.intValue(),  this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_X.value()) );
				}

			}		// Done IF-gridlines
		
		// the bordering frame
		if( this.enableBorder == true )
			{
			Color fc; 			// Which color to use?
			for( int fn = 0; fn < this.borderWidthPx; fn++ )
				{

				fc = this.clrSettings.get(
					enumMenuDataTableCOLORS.FRAME_INNER.value() );
				if( fn == 0 || fn == this.borderWidthPx-1 )
					{		// Outer frame 
					fc = this.clrSettings.get(
						enumMenuDataTableCOLORS.FRAME_OUTER.value()
						);
					}
				if( fc == null )  {  fc = Color.WHITE; } 
				target.rect( x0+fn, y0+fn, x2 - fn, y2 - fn, fc );
				}
			}
		
		System.out.println( " Data # " + Integer.toString( this.theData.size() ) );

		FontMetrics fm =
				target.getImage().getGraphics().getFontMetrics(
				this.fnt );
		FontRenderContext frc = fm.getFontRenderContext();

		// Finally, print all the labels & values.
		String label = new String("");
		String tmpLabel = new String("");
		tmpLabel.trim();
		String tmpData = new String("");
		tmpData.trim();

		for( Integer s = 0; s < this.theData.size(); s++ )
			{
			tmpData = this.theData.get(s);
			tmpLabel = this.theLabels.get(s)+this.labelTerminator;

			int dw0 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getWidth() ).intValue();
			int dy0 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getMaxY() ).intValue();
			int dy1 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getMinY() ).intValue();
			int dy2 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getHeight() ).intValue();

			int shorten = dw0;
				// This thing is longer then the space allocated.
				//   will need to truncate it.
			while( (shorten > this.colMaxW) && ! tmpData.isEmpty() )
				{
				tmpData = tmpData.substring(0, tmpData.length()-2 );
				shorten = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getWidth() ).intValue();
				}

			if( tmpData.isEmpty() )	{ continue; }
			
			// note: indentation is built into : this.theXpos.get(s) 			
			int Xadj = x0 + this.theXpos.get(s).intValue() 
					+ this.colMaxW.intValue() - shorten - 1;
			int Yadj = y0 + this.theYpos.get(s).intValue()
					+ (this.rowMaxH.intValue() / 2) + dy0;
			
			shorten = new Double( this.fnt.getStringBounds(
					tmpLabel, frc ).getWidth() ).intValue();
			while( (shorten > this.colMaxW) && ! tmpLabel.isEmpty() )
				{
				tmpLabel = tmpLabel.substring(0, tmpLabel.length()-2 );
				shorten = new Double( this.fnt.getStringBounds(
					tmpLabel, frc ).getWidth() ).intValue();
				}			



			if( this.useLabels == true )
				{
				if( s < this.theLabels.size() )
					{ label = this.theLabels.get(s) + this.labelTerminator; }
				else	{ label = ""; }

				if( this.useImagesAsLabels == true)
					{
					// TODO  : implement icon-labels
					}
				}

			System.out.println( " string x/y " + 
				Integer.toString( Xadj ) + " / " +  
				Integer.toString( Yadj ) );

			target.printString( 1 + x0 + this.theXpos.get(s).intValue(),  
				y0 - dy1 + this.theYpos.get(s).intValue(), 
				this.fnt, this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ),
				tmpLabel	);
			
			target.printString( Xadj, Yadj, 
				this.fnt, this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ),
				tmpData	);
			
			}

		return;
		}


	/**  For data tables, this acts to toggle the scan mode.
	 *   argument = 0 will display data fills columns first, else rows first.
	 */
	public boolean changeMode(Integer theModeNumber)
		{
		if( theModeNumber == 0 )
			{ this.scanMode = false; }
		else
			{ this.scanMode = true; }
		this.resolvePositions();
		return(this.scanMode);
		}

	public Double getX()
		{	return( new Double(this.x));	}
	public Double getY()
		{	return( new Double(this.y));	}
	public Double getDX()
		{	return( new Double(this.w));	}
	public Double getDY()
		{	return( new Double(this.h));	}

	/** Not available for this Vmenuitem type */
	public void setExtendX(int itemWidth, boolean onlyIfGreater)
		{	return;	}
	/** Not available for this Vmenuitem type */
	public void setExtendY(int itemHeight, boolean onlyIfGreater)
		{	return;	}

	/**   Sets all data cell values.
	 *    may lead to strangeness if applicable labels are accompanied.
	 */
	public void setTextContent( HashMap<Integer, String> textItems )
		{	this.theData = textItems;	}
	
	/**  Import a HashMap containing the Labels for the values..
	 */
	public void setTextLabels( HashMap<Integer, String> textLabels )
		{	this.theLabels = textLabels;	}

	/**  It is possible to use VImages (icons) as labels instead of text 
	 */
	public void setIconContent(HashMap<Integer, VImage> imageItems)
		{	this.imgLabels = imageItems;	}

	public void setColorContent(HashMap<Integer, Color> basicColors)
		{	this.clrSettings = basicColors;	 }

	/** Not used in this VMI type */
	public void setImageContent(HashMap<Integer, VImage> imageItems)
		{	return;	}

	/** VmiDataTable Cannot be selected in menus-thus always inactive */
	public void enableActivation()
		{	return;	}

	/** VmiDataTable with no text is non-sense */
	public void enableText(boolean enableText)
		{	return;	}

	/**   Turns displayed text labels on or off
	 * 
	 * @param enable true displays labels - false hides them.
	 */
	public void enableLabels( boolean enable )
		{	this.useLabels = enable;	}

	/** Toggles if icons are used instead of String labels */
	public void enableIcons(boolean enable)
		{	this.useImagesAsLabels = enable;	}

	public void enableBackdrop(boolean enable)
		{	this.enableImageBackdrop = enable;	}

	public void enableFrame(boolean enable)
		{	this.enableBorder = enable;	}

	/** VmiDataTable is state-less .. not selectable. */
	public void setState(Integer itemState)
		{	return;	}

	public Integer getState()
		{	return(enumMenuItemSTATE.STATELESS.value());	}

	public boolean isActive()
		{	return(this.active);	} 

	public boolean isVisible()
		{	return(this.visible);	}

	/** Cannot set a keycode for VmiDataTable. */
	public Integer getKeycode()
		{	return -1;	}
	/** Cannot set a keycode for VmiDataTable. */
	public void setKeycode(Integer vergeKeyCode)
		{	return;	}

	public void setFrameThicknessPx(int thick)
		{
		if( thick < 1 )	{ return; }
		this.borderWidthPx = thick;
		this.resolvePositions();
		return;
		}

	public void setFramePaddingPx( int thick )
		{
		if( thick < 1 )	{ return; }
		this.borderPaddingPx = thick;
		this.resolvePositions();
		return;
		}
	
	public Integer getFramePaddingPx()
		{ return(this.borderPaddingPx); }
	
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
		{	return(this.id);	}


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
		if( this.childID < 0 ) { return; }
		setMenuFocus( 0, this.childID );
		return;
		}

	// If this happens to recieve input -- it shouldn't -- 
	//   hide it away in a negative index.
	public void processInput(String input)
		{
		this.theData.put(-1, input);
		return;
		}



	/** ----------------------- non-interface methods ---------------------- **/	


	public boolean isEnableGrid()
		{	return enableGrid;	}
	public void setEnableGrid( boolean enableGrid )
		{	this.enableGrid = enableGrid;		}
	
		/**  Customize a single specific color, see enumMenuDataTableCOLORS
		 *       for potential keys.
		 * @param e  an enumMenuDataTableCOLORS key
		 * @param clr	Color object
		 */
	public void setColor( enumMenuDataTableCOLORS e, Color clr )
		{
		this.clrSettings.put(e.value(), clr );
		return;
		}
	
	/**  Retrieves a single color setting */
	public Color getColor( enumMenuDataTableCOLORS e )
		{
		return( this.clrSettings.get(e.value()) );
		}

	/** Directly adds a value to the table in most direct form.  */
	public void addEntry( String label, String data )
		{
		int newone = this.theData.size();
		this.theLabels.put(newone, label );
		this.theData.put(newone, data );
		this.resolvePositions();
		return;
		}

	public void setFont(Font newFnt )
		{
		this.fnt = newFnt;
		this.resolvePositions();
		return;
		}
	
	}			// END class  VmiDataTable.


