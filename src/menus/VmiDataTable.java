package menus;

import static core.Script.setMenuFocus;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import domain.VImage;
import menus.VmiButton.enumMenuButtonCOLORS;

/**  VmiDataTable - a VERGE menu item element intended for displaying 
 *    a lot of tabular data at once, intended to help inventory display or shops.
 * It operates in an "inactive" state, which means Vmenu that attach it
 * will not allow it to be selected nor will it conform boundries to it.
 * Thus, parent menus are responsible for sending controls to it.
 * Any data taken in must be turned into a String type first.
 * The size of the table row and columns is externally fixed, as 
 *     the data table will not automatically react to addition of data values.
 * 
 * Working Features :  
 * 		Background images
 * 		Dynamic number of columns and rows
 * 		Image Icon or Text Labels 
 * 		Optional Caption with a different font
 * 		Changable main & caption fonts.
 * 		Custom width border and main-block padding
 * 		Custom (translucent) colors when images are not used.
 * 		Smart column sizing
 * 		Scrolling with optional scroll bars
 * 
 * TODO What is NOT done.   low-pri features:
 * 		inter-cellular padding.
 * 		translucent image backgrounds
 * 		Multi-line text within cells - currently truncates long entries
 * 		Gridline thickness and styles
 * 		Inter-cell content alignment
 * 
 * @author Krybo
 *
 */
public class VmiDataTable implements Vmenuitem
	{
	/**  Great resource for font centering.  
	 * Which is a real chore for this class.
	 * http://stackoverflow.com/questions/1055851/how-do-you-draw-a-string-centered-vertically-in-java
	 */
	private int x = 0, y = 0, w = 0, h = 0;
	private int rownum = 0;
	private int colnum = 0;
	private Double captionHeight = new Double(0.0d);
	private int captionTextWidth = 0;
	private int captionTextHeight = 0;
	private Double captionHeightSave = new Double(0.0d);
	private int borderWidthPx = 3;
	private int borderPaddingPx = 10;
	private Integer hotkeyCode = -1;
	private boolean needsRefresh = true;
	private boolean active = false;		// not selectable, ever.
	private boolean visible = true;
	private boolean useLabels = false;
	private boolean useImagesAsLabels = false;
	private boolean scanMode = false;
	private boolean enableGrid = true;
	private boolean enableBorder = true;
	private boolean enableImageBackdrop = false;
	private boolean enableCaption = false;
	private boolean scaleVoid = false;
	private String labelTerminator	= new String(": ");
	private String theCaption 		= new String("");
	private String decimalFormatter = new String( "%.3f" );
	private Font fnt = core.Script.fntMASTER;
	private Font theCaptionFont = this.fnt;

	// A separate image containing the main body of the table.
	private VImage virtualBody = new VImage( 24,24 );

		// For scrolling
	private boolean enableScroll = false;
	private boolean enableScrollBars = false; 
	private Double scrollSpeed = 1.0d;
	private Double scrollFractionX = 0.0d;
	private Double scrollFractionY = 0.0d;
	private Integer scrollMaxX = 0;		// max allowable conditions.
	private Integer scrollMaxY = 0;
	private Integer scrollX = 0;
	private Integer scrollY = 0;
	private Integer scrollXitems = 0;
	private Integer scrollYitems = 0;
	private int textAccent = 0;	// Font Accent (in px) for the current Text
	
		// These are interface requirements for all menu items.
	private Long parentID = new Long(-1);
	private Long id = new Long(-1);
	private Long childID = new Long(-1);

	private VImage bkgImg = null;

		// Key data members.
	private HashMap<Integer,Color> clrSettings;
	private HashMap<Integer,TableCell> cells;
	private String desc;
	private String tip;

	//  This allows a string-pixel limit to be placed on each column
	//    zero or null means unlimited.
	private HashMap<Integer,Integer> cellTruncationBounds;
	private HashMap<Integer,Integer> calcColumnWidth;
	// TODO:  implement blinking by enabling it on a cell based property
	private boolean oscillatingSelection;
	private Integer oscillatingFreq;

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


	/** -------------------- Constructor -------------------------------------- */

	public VmiDataTable( int pinX, int pinY, int pixelWidth, int pixelHeight,
			int numColumns, int numRows, 
			LinkedHashMap<String,String> entries )
		{
		this.init( pinX, pinY, pixelWidth, pixelHeight, numColumns, numRows );
		this.insertEntries( entries );
		this.setDefaultColors();
		this.needsRefresh = true;
		return;
		}

	/** Constructor helpers */

	/**  This handles an arraylist of various types as Data input to build
	 *      a verge menu item data table.
	 * 
	 * @param pinX	The absolute x pixel screen position.
	 * @param pinY	The absolute y pixel screen position.
	 * @param pixelWidth	Horizontal space (px) allocated to the table.
	 * @param pixelHeight	Vertical space (px) allocated to the table.
	 * @param numColumns	Horizontal Columns in the resulting table.
	 * @param numRows		Vertical Rows in the resulting table.
	 * @param dataEntries	An ArrayList of some type.
	 * @throws Exception	throws if the arraylist<type> cannot be used
	 */
	public VmiDataTable( int pinX, int pinY, int pixelWidth, int pixelHeight,
			int numColumns, int numRows, 
			ArrayList<?> dataEntries ) throws Exception
		{
		this.init(pinX, pinY, pixelWidth, pixelHeight, numColumns, numRows);
		this.setDefaultColors();
		
		boolean keepgoing = false;

		// Proceed with a empty table.
		if( dataEntries == null || dataEntries.isEmpty() )
			{	return;	}

		// Parse differently depending on arraylist class.
		if( dataEntries.get(0).getClass().equals( String.class ) ) 
			{ 
			for( int n = 0; n < dataEntries.size(); n++ )
				{
				String o = (String) dataEntries.get(n);
//				this.theData.put( n, o );
				this.cells.get(n).setText( o );
				}
			keepgoing = true;
			}
		
		if( dataEntries.get(0).getClass().equals( Integer.class ) )
			{ 
			for( int n = 0; n < dataEntries.size(); n++ )
				{
				Integer o = (Integer) dataEntries.get(n);
//				this.theData.put( n, o.toString() );
				this.cells.get(n).setText( o.toString() );
				}
			keepgoing = true;
			}

		if( dataEntries.get(0).getClass().equals( Float.class ) )
			{ 
			for( int n = 0; n < dataEntries.size(); n++ )
				{
				Float o = (Float) dataEntries.get(n);
//				this.theData.put( n, String.format(	
//						this.decimalFormatter, o ) );
				this.cells.get(n).setText( 
						String.format(this.decimalFormatter, o ) );
				}
			keepgoing = true;
			}
		
		if( dataEntries.get(0).getClass().equals( Double.class ) )
			{ 
			for( int n = 0; n < dataEntries.size(); n++ )
				{
				Double o = (Double) dataEntries.get(n);
//				this.theData.put( n, String.format(	
//						this.decimalFormatter, o ) );
				this.cells.get(n).setText( String.format(	
						this.decimalFormatter, o ) );
				}
			keepgoing = true;
			}

		//  ** Other ArrayList<> types can be added here as needed.
		
		if( keepgoing == false )
			{
			System.err.println(" VmiDataTable:  got Unworkable data.");
			Exception e = new Exception("Incompatible arraylist type."+
				"new Data Table will be empty.");
			throw(e);
			}

		this.needsRefresh = true;
		return;
		}
	
		// Sets only essentials. - simplifies constructors.
	public void init( int pinX, int pinY, int pixelWidth, int pixelHeight,
			int numColumns, int numRows )
		{
		this.id = Vmenuitem.getRandomID();
		this.x = pinX;
		this.y = pinY;
		this.w = pixelWidth;
		this.h = pixelHeight;
		this.colnum = numColumns;
		this.rownum = numRows;
		this.hotkeyCode = -1;
		this.captionTextWidth = 0;
		this.captionTextHeight = 0;
		this.oscillatingSelection = false;
		this.oscillatingFreq = 0;
		this.captionHeight = new Double( 0.0d );
		this.cells = new HashMap<Integer,TableCell>();
		
		//  The Virtual Body will be resized dynamically -- later on.
		this.virtualBody = new VImage( this.w+24, this.h+24 );
		
		// Init inner cells.
		for( Integer n = 0; n < this.getCellCapacity(); n++ )
			{  this.cells.put( n, new TableCell( n,-1,-1,"", this.fnt) );  }
		
		this.clrSettings 	= new HashMap<Integer,Color>();
		
		//  Shut off truncation on default.
		this.cellTruncationBounds = new HashMap<Integer,Integer>();
		this.calcColumnWidth = new HashMap<Integer,Integer>();
		for( int c = 0; c < numColumns; c++ )
			{
			this.cellTruncationBounds.put( c, 0 );
			this.calcColumnWidth.put( c, 0 );
			}

		this.theCaption = new String("");
		this.theCaptionFont = this.fnt;
		this.enableCaption = false;
		this.scaleVoid = false;

		this.active = false;
		this.visible = true;
		this.scanMode = false;	// Fill across columns, then rows, by default.
		this.enableImageBackdrop = false;

		//	Inits all scrolling vars to disabled values - 
		//      the user decides to turn it on after init. 
		this.disableScroll();
		
		this.needsRefresh = true;
		return;
		}

	
	private class TableCell
		{
		private Integer myX,myY,id;
		private VImage icon = null;
		private String myText,myLabel;
		private String labelTerminator;
		private Integer pxW, pxH, pxBorder, pxPadding;
		private Integer calcW, calcH;
		private Boolean showGrid, showLabel, showImageAsLabel;
		private Boolean needsUpdate;
		private Boolean showVoidSpace;
		private Boolean isBlinking;
		private Font myFont;
		private Color clrGridX, clrGridY, clrText, clrLabel;
		private Integer bareAccent;		// Text accent value of font
		private Integer imgW, imgH;
		private Integer xVoidSpace, yVoidSpace;
		
		public TableCell( int idNum, int x, int y, String text, Font fnt )
			{
			this.id = idNum;
			this.myX = x;
			this.myY = y;
			this.myText = text;
				// Defaults
			this.labelTerminator = new String(": ");
			this.showLabel = true;
			this.showImageAsLabel = false;
			this.myFont = fnt;
			this.needsUpdate = true;
			this.isBlinking = false;
			this.yVoidSpace = 0;
			this.xVoidSpace = 0;
			this.pxW = 0;
			this.pxH = 0;
			this.imgW = 0;
			this.imgH = 0;
			this.pxBorder = 1;
			this.pxPadding = 3;
			this.clrGridX = Color.gray;
			this.clrGridY = Color.gray;
			this.clrText = Color.gray;
			this.clrLabel = Color.gray; 
			return;
			}
		
		public void setColors( Color gridX, Color gridY, Color text, Color label )
			{
			if( gridX != null )
				{ this.clrGridX = gridX; }
			if( gridY != null )
				{ this.clrGridY = gridY; }
			if( text != null )
				{ this.clrText = text; }
			if( label != null )
				{ this.clrLabel = label; }			
			return;
			}
			
		public void reposition( int x, int y )
			{
			this.myX = x;
			this.myY = y;
			return;
			}
		
		public void setLabel( String lab )
			{ 
			this.myLabel = lab;
			this.needsUpdate = true;
			return; 
			}
			
		public void setFont( Font newFont )
			{
			this.myFont = newFont;
			this.needsUpdate = true;
			return;
			}
		
		public Font getFont( )
			{ return( this.myFont ); }
		
		public void setShowGrid( boolean onOff )
			{
			this.showGrid = onOff;
			return;
			}
		public boolean getShowGrid()
			{  return( this.showGrid );  }
		
		public void setIcon( VImage vimg )
			{
			this.icon = vimg;
			this.needsUpdate = true;
			return; 
			}
		
		public void setBorderPx( Integer cellBorderWidthPx )
			{
			this.pxBorder = cellBorderWidthPx;
			this.needsUpdate = true; 
			return;
			}
		
		public void setPaddingPx( Integer cellPaddingPx )
			{
			this.pxPadding = cellPaddingPx;
			this.needsUpdate = true; 
			return;
			}
		
		public Integer getPaddingPx()
			{ return( this.pxPadding ); }
		public Integer getBorderPx()
			{ return( this.pxBorder ); }
		
		public void setLabelTerminator( String joiner )
			{
			this.labelTerminator = joiner;
			this.needsUpdate = true;
			return;  
			}
			
		public String getText( )
			{ return( this.myText ); }
		public String getLabel( )
			{ return( this.myLabel ); }
		public Integer getX( )
			{ return( this.myX ); }
		public Integer getY( )
			{ return( this.myY ); }
		
		public void setShowLabel( Boolean onOff )
			{
			this.showLabel = onOff;
			this.needsUpdate = true;
			return; 
			}
		public void setImageForLabel( Boolean onOff )
			{
			this.showImageAsLabel = onOff;
			this.needsUpdate = true;
			return;  
			}
			
		public void setFixedWidth( Integer widthPx )
			{
			this.pxW = widthPx;
			this.needsUpdate = true;
			return;
			}
		public void setFixedHeight( Integer hgtPx )
			{
			this.pxH = hgtPx;
			this.needsUpdate = true;
			return;
			}
			
		public void setText( String text )
			{
			this.myText = text;
			this.needsUpdate = true;
			return;
			}
		
		public Integer getTextWidth( )
			{
			return( this.calcW );
			}
		public Integer getTextHeight( )
			{
			return( this.calcH );
			}

		/** returns the space the border and padding consume within a cell */
		public Integer getInsetSpacePx()
			{ return( this.pxBorder + (this.pxPadding * 2) ); }
		
		public Boolean setVoidSpace( Boolean onOff )
			{
			this.showVoidSpace = onOff;
			this.needsUpdate = true;
			return( this.showVoidSpace );
			}
		
		public void setXvoid( Integer voidXpx )
			{
			this.xVoidSpace = voidXpx;
			return;
			}
		public void setYvoid( Integer voidYpx )
			{
			this.yVoidSpace = voidYpx;
			return;
			}
		
		public Integer getTotalWidth()
			{ return( this.pxW + this.getInsetSpacePx() ); }
					
		public Integer getTotalHeight()
			{ return( this.pxH + this.getInsetSpacePx() ); }

		public void update( VImage target )
			{
			String fullText = this.myText;
			if( this.showLabel )
				{
				fullText = this.myLabel + this.labelTerminator + this.myText;
				if( this.showImageAsLabel == true )
					{  fullText = " " + this.myText; }
				}
			Rectangle metrics = 
				target.getStringPixelBounds( this.myFont, fullText );
			this.calcW = metrics.width;
			this.calcH = metrics.height;

			int realH = this.calcH;
			if( this.pxH > 0  &&  this.pxH > this.calcH )
				{ realH = this.pxH; }
				
			// Calculated Width exceeds permitted width.  Truncate.
			if( this.showImageAsLabel  &&  this.icon != null )
				{
				Integer scaledWidth = new Double( 
					this.icon.width / this.icon.height * realH ).intValue();
				this.imgH = realH;
				this.imgW = scaledWidth;
				this.calcW += scaledWidth; 
				}
			
			//  Handle cell Overflow.
			if(  this.pxW > 0  &&  this.calcW > this.pxW )
				{
				String oversized = fullText;
				String tmpStr = new String(oversized); 
				int shorten = this.calcW;
				int breaker = 0;
				while( (shorten >= this.pxW) &&  (tmpStr.length() > 2)  
						&& breaker < 500 ) 
					{
					breaker++;
					tmpStr = oversized.substring( 0, oversized.length()-1 );
					shorten = target.getStringPixelBounds( 
						this.myFont, tmpStr ).width + this.imgW;
//		System.out.println("DEBUG: "+Integer.toString(shorten));
					oversized = new String( tmpStr );
					}
//				System.out.println("Need to shorten # "+Integer.toString( this.id ) +
//						" by " + Integer.toString(breaker) + " of " +
//						Integer.toString(fullText.length()));
				if( (fullText.length() > breaker) &&
					( this.myText.length() > (fullText.length() - breaker))  )
					{
					this.myText = this.myText.substring( 0, 
						fullText.length() - breaker);
					this.calcW = this.pxW - 1;
					}
				else { 
					this.myText = new String("|||");
					this.calcW = new VImage(50,50).getStringPixelBounds(
							this.myFont, this.myText).width;
					}	
				}
			
			// Need a hgt measurement of non-descending chars
			//     for proper vertical alignment 
			this.bareAccent =	target.getStringPixelBounds( 
					this.myFont, "0123456789YASZ").height + 1;
			
			this.needsUpdate = false;
			}
		
		public void paint( VImage target )
			{
			if( this.needsUpdate )  	{ this.update( target ); }
			
			// Padding & border adjustments
			Integer inset = this.pxPadding + this.pxBorder;
			Integer outset = this.pxPadding * 2 + this.pxBorder;
			
			if( this.isBlinking )
				{
//  TODO : implement this by blitting a colored rectangle first.
//              Use the bounds set below for the gridlines
//				bodyColor = Vmenuitem.oscillateColor( this.hmColorItems.get(
//					enumMenuButtonCOLORS.BODY_SELECTED.value()), 
//					Vmenuitem.generateTimeCycler( this.oscillatingFreq ) );
				}
			
			String fullText = new String( myText );
			int imgAdj = 0;
			if( this.showImageAsLabel == true )
				{
				target.scaleblit( inset + this.myX, inset + this.myY,
						this.imgW, this.imgH, this.icon );
				imgAdj = this.imgW + 1;
				}
			else if( this.showLabel )
				{
				fullText = this.myLabel + this.labelTerminator + this.myText;
				}
			
			target.printString( inset + this.myX + imgAdj, 
				inset + this.myY + this.bareAccent, 
				this.myFont, this.clrText, fullText );
			
			if( this.showGrid == true )
				{
				target.line( this.myX, this.myY, 
					this.myX+this.pxW+outset+this.xVoidSpace,
					this.myY, this.clrGridX );
	//			target.line(  this.myX, 
	//				this.myY + this.pxH + outset + this.yVoidSpace - this.pxBorder,
	//				this.myX + this.pxW + outset + this.xVoidSpace,
	//				this.myY + this.pxH + outset + this.yVoidSpace - this.pxBorder, 
	//				this.clrGridX );
				target.line(  this.myX, this.myY, 
					this.myX, this.myY + this.pxH + outset + this.yVoidSpace,
					this.clrGridY );
	//			target.line( this.myX + this.pxW + outset + this.xVoidSpace, 
	//				this.myY, 
	//				this.myX + this.pxW + outset + this.xVoidSpace,
	//				this.myY + this.pxH + outset + this.yVoidSpace, this.clrGridY );
				}
			return;
			}

		}		// END SUBCLASS TABLECELL


	private void setDefaultColors()
		{
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
		}

	/** Searching in order from the beginning, Returns the ffirst base zero
	 * index value of the entry matching the argument conditions contiguously.  
	* otherwise: Finds the first unused cell within the table.   */
	private Integer getLastEntry( Boolean hasData, Boolean hasLabel )
		{
		if( hasData == false  &&  hasLabel == false )
			{ return(-1); }		// nonsense argument values
				// This loop will simply kick out upon the first violation.
		for( Integer x : this.cells.keySet() )
			{
			String lab = this.cells.get(x).getLabel();
			String dat = this.cells.get(x).getText();
			if( hasData == true  &&  dat.isEmpty() )
				{ return(x-1); }
			if( hasLabel == true  &&  lab.isEmpty() )
				{ return(x-1); }
			if( hasData == true  &&  dat.length() < 1 )
				{ return(x-1); }
			if( hasLabel == true  &&  lab.length() < 1 )
				{ return(x-1); }
			}
		return( this.cells.size() );
		}
	
	/** Given a key value string hashmap, inserts values and labels
	 * starting from the first blank cell found.   All data following the first
	 * blank cell will be overwritten! */
	private void insertEntries( HashMap<String,String> entries )
		{
		if( entries == null || entries.isEmpty() )
			{ return; }
		// Inject new values at the end of known.
		Integer n = this.getLastEntry( true, true );
		for(  String key : entries.keySet() )
			{
			n++;
			if( this.cells.containsKey(n) )
				{
				this.cells.get( n ).setText(  new String( entries.get(key) ) );
				this.cells.get( n ).setLabel( new String( key ) );
				}
			else {
				this.cells.put( n, new TableCell( n, 0,0, "new cell",this.fnt) );
				this.cells.get( n ).setText(  new String( entries.get(key) ) );
				this.cells.get( n ).setLabel( new String( key ) );				
				}
			}
		this.needsRefresh = true;
		return;
		}

	public boolean reposition( int posX, int posY, int relPosX, int relPosY)
		{
		if( (posX+relPosX < 0) || (posY+relPosY < 0) )
			{ return(false); }
		this.x = posX+relPosX;
		this.y = posY+relPosY;
		this.needsRefresh = true;
		return true;
		}

	/** DataTable implementation will ignore relative deltas
	 * it is there simply to satisfy interface..  */
	public boolean repositionDelta( 
			int deltaX, int deltaY, int drelPosX, int drelPosY )
		{
			// refuse to send the item off the upper left of the screen.
		if( (this.x + deltaX) < 0 || (this.y + deltaY) < 0 )
			{ return(false); }
		this.x += deltaX;
		this.y += deltaY;
		this.needsRefresh = true;
		return true;
		}

	/**  This Vmenutem is for display only.   No actions attached.	 */
	public void setAction(Method action)
		{	return;	}
	public Method getAction()
		{ 
		return( core.Script.getFunction(Vmenuitem.class, "nullAction" ) ); 
		}
	/**  This Vmenutem is for display only.   No actions attached.	 */
	public boolean doAction()
		{	return(false);	}

	/** only to satisfy interface. */
	public Object[] getActionArgs()
		{ return( new Object[]{} );	}
	public void setActionArgs(Object[] actionArgs)
		{ return; }

	/** Force a recalculation of all internal placements and sizing.
	 * Just calling paint() will automatically do this if something in the table's
	 * display properties has changed - before drawing it. */
	public void refresh()
		{
		this.needsRefresh = true;
		this.redrawVirtualTable();
		return;
		}

	public boolean animate( VImage target )
		{ this.paint( target );  return( true ); }
	
	/**  Draws the contents of this menuitem Data table onto 
	 *    the provided VImage
	 */
	public void paint( VImage target )
		{
		if( this.visible == false ) { return; }
		if( this.needsRefresh == true )
			{ this.redrawVirtualTable(); }
		// For convenience. - actual start of data area.
		// calc the corners for box and its margin.
		int x0 = this.x;
		int y0 = this.y;
		//  x1 & y1 are the main body top-left position after all adjustments.
		int x1 = x0 + this.borderWidthPx;
		int y1 = y0 + this.borderWidthPx;
		int x2 = x0 + this.w;
		int y2 = y0 + this.h;
		
		int bodyWidth =  this.w - (this.borderWidthPx * 2) + 1;
		int bodyHeight =  this.h - (this.borderWidthPx * 2) + 1;

		//  -- Caption Pre-calculation --
		Double captionStartX = new Double( 0.0d );
		Double captionStartY = new Double( 0.0d );
		if( this.enableCaption == true  &&  ! this.theCaption.isEmpty() )
			{
			captionStartX = new Double( this.w - this.captionTextWidth ) / 2;
			captionStartX += new Double( x0 );
			captionStartY = new Double( y0 + this.borderWidthPx + 
					this.borderPaddingPx + this.captionTextHeight );

			y1 = y0 + this.captionHeight.intValue();
			bodyHeight -= this.captionHeight.intValue();
			bodyHeight += this.borderWidthPx;
			// The Caption backdrop
			target.rectfill( x0, y0,	x0 + this.w, 
				y0 + this.captionHeight.intValue(), this.clrSettings.get(
				enumMenuDataTableCOLORS.BKG_CAPTION.value()) );
			}
		else if( this.captionHeight != 0.0d )
			{
			this.captionHeight = 0.0d;
			captionStartX = 0.0d;
			captionStartY = 0.0d;
			this.needsRefresh = true;
			this.redrawVirtualTable();
			}
		
		// -- Draw the main body through a virtual canvas --
		//   the virtual canvas contains the backdrop, main text, and gridlines
		if( this.enableScroll == true )
			{
			int sBarAdjust = 0; 
			if( this.enableScrollBars ) 
				{ sBarAdjust = 5; }
			 // Inset part of the virtual body depending on scroll vars
			target.blitSubRegion( this.virtualBody, 
				this.scrollX, this.scrollY, bodyWidth - sBarAdjust, 
				bodyHeight - sBarAdjust, x0 + this.borderWidthPx, 
				y0 + this.borderWidthPx + this.captionHeight.intValue() );
			}
		else   // Otherwise, just plop the entire virtualbody - scaled.
			{
			target.scaleblit( x1,  y1,
				bodyWidth, bodyHeight, this.virtualBody );	
			}

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
				if( this.enableCaption == true && ! this.theCaption.isEmpty() )
					{
					target.rect( x0+fn, y0+fn,	x2 - fn, 
						y0 + this.captionHeight.intValue() - fn, fc );	
					}
				}
			}
		
		// 	Draw the caption text
		if( this.enableCaption == true && ! this.theCaption.isEmpty() )
			{
			target.printString( captionStartX.intValue(),  captionStartY.intValue(),  
				this.theCaptionFont, 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ),
				this.theCaption );
			}			
		
		
		// Draws the scroll bars
		if( this.enableScrollBars )
			{
			int subX = x2 - this.borderWidthPx;
			int subY = y2 - this.borderWidthPx;
			int sBarVX = subX - 5;
			int sBarVY = y1;
			int sBarHX = x1;
			int sBarHY = subY - 5;
			Double scrX = new Double( new Double(bodyWidth) / 
					new Double( this.virtualBody.getWidth() ) );
			Double scrY = new Double( new Double( bodyHeight ) /
					new Double( this.virtualBody.getHeight() ) );
			int barW = new Double( scrX * bodyWidth + 0.5d ).intValue();
			int barH = new Double( scrY * bodyHeight + 0.5d ).intValue();
			int scrollXoffset = new Double( this.scrollFractionX *
					new Double( bodyWidth - barW ) ).intValue();
			int scrollYoffset = new Double( this.scrollFractionY *
					new Double( bodyHeight - barH ) ).intValue();

			Color cOut = this.clrSettings.get(
					enumMenuDataTableCOLORS.FRAME_OUTER.value());
			Color cFill = this.clrSettings.get(
					enumMenuDataTableCOLORS.FRAME_INNER.value());
			
//			System.out.println("DEBUG SCROLL: " + 
//				Integer.toString(this.virtualBody.getWidth() )  + " w, " +  
//				Integer.toString(this.virtualBody.getHeight() )  + " h, " +
//				Integer.toString( scrollXoffset ) + " xt " + 
//				Integer.toString(scrollYoffset) + " yt "  );
			
			target.rectfill( sBarVX, sBarVY, subX, subY, cFill ); 
			target.rectfill( sBarHX, sBarHY, subX, subY, cFill ); 

			target.rect( sBarVX, sBarVY, subX, subY,  cOut );
			target.rect( sBarHX, sBarHY, subX, subY, cOut );
			
			target.rectfill( sBarHX + scrollXoffset, sBarHY, 
					sBarHX + scrollXoffset + barW,  sBarHY+5, cOut );
			target.rectfill( sBarVX, sBarVY + scrollYoffset, sBarVX+5, 
					sBarVY + scrollYoffset + barH, cOut );
			}
			
		return;
		}		// END main paint() method.


	/**  For data tables, this acts to toggle the scan mode.
	 *   argument = 0 data will fill across rows first, else fill down columns first.
	 */
	public boolean changeMode( Integer theModeNumber )
		{
		if( theModeNumber == 0 )
			{ this.scanMode = false; }
		else
			{ this.scanMode = true; }
		this.needsRefresh = true;
		return( this.scanMode );
		}

	public Double getX()
		{	return( new Double(this.x));	}
	public Double getY()
		{	return( new Double(this.y));	}
	public Double getDX()
		{	return( new Double(this.w));	}
	public Double getDY()
		{	return( new Double(this.h));	}

	/** setExtendX Not available for this Vmenuitem type */
	public void setExtendX(int itemWidth, boolean onlyIfGreater)
		{	return;	}
	/** setExtendY Not available for this Vmenuitem type */
	public void setExtendY(int itemHeight, boolean onlyIfGreater)
		{	return;	}

	// TODO, the VMI interface should be modified to use Int instead of void RT.
	//  Doing a cheap workaround delegator here that chucks the return type.
	public void setTextContent( HashMap<Integer, String> textItems )
		{ this.setTextContent(textItems); return; }

	/**   Sets a series of data cell values.
	 *    This method will NOT set or alter existing cell labels.
	 *    If cell number does not exist in table, input entry is ignored.
	 *    Returns an Integer representing the number actually set.
	 */
	public Integer setTextContents( HashMap<Integer, String> textItems )
		{
		Integer rslt = 0;
		if( textItems == null  ||  textItems.isEmpty() ) 
			{ return( rslt ); }
		for( Integer x : textItems.keySet() )
			{
			if( textItems.get(x) == null  ||  textItems.get(x).length() < 1 ) 
				{ continue; }
			if( ! this.cells.containsKey(x) )  { continue; }
			this.cells.get(x).setText( new String( textItems.get(x)) );
			rslt++;
			}
		this.needsRefresh = true;
		return( rslt );
		}
	
	/**  Import a HashMap containing the Labels for the corresponding cell #..
	 * It will NOT set nor alter existing cell values.
	 * Returns an Integer of the number actually set.
	 */
	public Integer setTextLabels( HashMap<Integer, String> textLabels )
		{
		Integer n = 0;
		if( textLabels == null  ||  textLabels.isEmpty() ) 
			{ return( n ); }  
		for( Integer x : textLabels.keySet() )
			{
			if( textLabels.get(x) == null  ||  textLabels.get(x).length() < 1 ) 
				{ continue; }
			if(  ! this.cells.containsKey(x)  )   { continue; }
			this.cells.get(x).setLabel( textLabels.get(x) );
			n++;
			}
		return(n);
		}

	/**  It is possible to use VImages (icons) as labels instead of text 
	 */
	public void setIconContent( HashMap<Integer, VImage> imageItems )
		{ this.setIconContent( imageItems ,false ); return; }
	public void setIconContent( HashMap<Integer, VImage> imageItems,
			Boolean turnOnImages )
		{	
		for( Integer x : imageItems.keySet() )
			{
			if( ! this.cells.containsKey(x) )   { continue; }
			this.cells.get(x).setIcon( imageItems.get(x) );
			if( turnOnImages == true )
				{ this.cells.get(x).setImageForLabel( true ); }
			}
		this.needsRefresh = true;
		return;
		}

	/**  in array form, sends VImage icon data into the object.
	 *    one per data cell.   Excess images are discarded.
	 * @param images  Array of Vimage, size is scaled to fit.
	 */
	public void setIconContent( VImage[] images )
		{
		int max = this.cells.size();
		for( Integer x = 0; x < images.length; x++ )
			{
			if( x > max )	{ continue; }
			if( ! this.cells.containsKey(x) ) 
				{
				System.out.println("WARN:  Icon load failed,  Uninit object # "+
				   Integer.toString(x) ); 
				continue; 
				}
			this.cells.get(x).setIcon( images[x] );
			}
		this.needsRefresh = true;
		return;
		}

	public void setColorContent( HashMap<Integer, Color> basicColors )
		{
		if( basicColors == null )	{ return; }
		for( Integer cn : basicColors.keySet() )
			{ this.clrSettings.put(cn, basicColors.get(cn) ); }
		return;
		}
	
	public Color getColorComponent( Integer hashKeyColor )
		{ return( this.clrSettings.get(hashKeyColor)); }

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
		{
		this.useLabels = enable;
		this.needsRefresh = true;
		return; 
		}

	/** Toggles if icons are used instead of String labels */
	public void enableIcons(boolean enable)
		{
		if( this.useImagesAsLabels != enable )
			{ this.needsRefresh = true; }
		this.useImagesAsLabels = enable;
		return;
		}

	public void enableBackdrop( boolean enable)
		{	this.enableImageBackdrop = enable;	}

	public void enableFrame( boolean enable)
		{
		this.enableBorder = enable;
		this.needsRefresh = true;
		return; 
		}

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
		{	return( this.hotkeyCode );	}
	/** Cannot set a keycode for VmiDataTable. */
	public void setKeycode(Integer vergeKeyCode )
		{
		this.hotkeyCode = vergeKeyCode;
		return;
		}

	public void setFrameThicknessPx( int thick)
		{
		if( thick < 1 )	{ return; }
		this.borderWidthPx = thick;
		this.needsRefresh = true;
		return;
		}

	public void setFramePaddingPx( int thick )
		{
		if( thick < 1 )	{ return; }
		this.borderPaddingPx = thick;
		this.needsRefresh = true;
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

	// If this happens to receive input -- it shouldn't -- 
	//   hide it away in a negative index.
	public void processInput(String input)
		{
		this.cells.put(-1, new TableCell(-1,0,0,input,this.fnt) );
		return;
		}


	public String[] getTip()
		{
		String[] rtn = new String[2];
		rtn[0] = this.tip;
		rtn[1] = this.desc;
		return(rtn);
		}
	
	public void setTip( String helpCaption )
		{
		this.tip = helpCaption;
		this.desc = helpCaption;
		return;
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
	
	
	public void enableOscillation( Integer freqMsec )
		{
		this.oscillatingSelection = true;
		this.oscillatingFreq = freqMsec;
		return;
		}
	public void disableOscillation( )
		{
		this.oscillatingSelection = false;
		this.oscillatingFreq = 0;
		return;
		}
	public boolean isOscillating()
		{
		return( this.oscillatingSelection );	
		}
	public Integer getOscillationFrequency()
		{ return( this.oscillatingFreq); }

	
	// ----------------------------- end interface funcs ----------------------------
	
	/** Returns the column number from an index number, 
	 *    taking into account the scan mode.  */
	private Integer determineColumn( int index )
		{
		if( this.scanMode == true )
			{ return( index / this.rownum ); }
		return( index % this.colnum );
		}
	
	/**  --   Drawing and Scroll related support methods ----------  */

	public void setScrollable( Integer scrollRows, Integer scrollCols, 
			Double speed, boolean showScrollBars )
		{
		this.enableScroll = true;
		this.enableScrollBars = showScrollBars; 
		this.scrollSpeed = speed;
		this.scrollX = 0;
		this.scrollY = 0;
		this.scrollXitems = scrollCols;
		this.scrollYitems = scrollRows;
		this.needsRefresh = true;
		return;
		}

	public void doScrollReset()
		{
		this.scrollX = 0;
		this.scrollY = 0;
		this.scrollXitems = 0;
		this.scrollYitems = 0;
		return;
		}

	public void disableScroll()
		{
		this.enableScroll = false;
		this.enableScrollBars = false; 
		this.scrollSpeed = 1.0d;
		this.scrollX = 0;
		this.scrollY = 0;
		this.scrollXitems = 0;
		this.scrollYitems = 0;
		this.scrollMaxX = 0;
		this.scrollMaxY = 0;
		this.needsRefresh = true;
		return;		
		}
	
	/** returns the y position of a row within the virtual body. */
	private Integer getRowBaseY( Integer rowNum )
		{
		Integer rslt = 0;
		if( rowNum == 0  ||  this.rownum < 2 )  
			{ return( rslt ); }		// Easy cases.
		int stop = 0;
		for( Integer x : this.cells.keySet() )
			{
			int cNum = x % this.colnum;
			if( stop >= rowNum )  { continue; }
			if( cNum != 0 )  { continue; }
			else { 
				rslt += this.cells.get(x).getTotalHeight();
				stop++;
				}
			}
		return( rslt );
		}
	
	/** Return the x coordinate of the target column within the virtual body */
	private Integer getColumnBaseX( Integer colNum )
		{
		Integer rslt = 0;
		//  nonsensical for a 1-column table, and the first column is always zero
		if( colNum == 0  ||  this.colnum < 2 ) 
			{ return( rslt ); }
		for( Integer x : this.cells.keySet() )
			{
			int cNum = x % this.colnum;
			if( cNum > 0 )  { continue; }
			if( cNum <= colNum )
				{ rslt += this.cells.get(x).getTotalWidth(); }
			}
		return( rslt );
		}
	
	/** Return the column number corresponding to scrollMaxX */
	private Integer getScrollMaxXcolumn( )
		{
		Integer rslt = 0;
		if( this.colnum < 2 )   { return( rslt ); }
		int count = 0;
		for( Integer x : this.cells.keySet() )
			{
			if( x >= this.colnum ) 
				{ return( this.colnum - 1 ); }
			count += this.cells.get(x).getTotalWidth();
			if( count >= this.scrollMaxX )  { return(x+1); }
			}
		return( this.colnum - 1 );
		}
	
	/** Return the row number corresponding to scrollMaxY */
	private Integer getScrollMaxYrow( )
		{
		Integer rslt = -1;
		if( this.rownum < 2 )  { return( 0 ); }
		int count = 0;
		for( Integer x : this.cells.keySet() )
			{
			int cNum = x % this.colnum;
			if( cNum != 0 )  { continue; }
			rslt++;
			count += this.cells.get(x).getTotalHeight();
			if( count >= this.scrollMaxY )  { return( rslt + 1 ); }
			}
		return( this.rownum - 1 );
		}
	
	/** Examine data and determine metrics needed for cols & rows. */
	private void analyzeRowsCols()
		{
		if( ! this.needsRefresh )   { return; }
		Integer heightMax = 0;
		
		if( this.cells == null  ||  this.cells.isEmpty()  ||  this.cells.size() == 0 )
			{
			System.out.println("WARN:  VmiDataTable has no data!"); 
			return; 
			}
		
		//  Ensure all global settings are passed into the internal table cells
		for( Integer x : this.cells.keySet() )
			{
			TableCell tc = this.cells.get(x);
			tc.setLabelTerminator( this.labelTerminator );
			tc.setShowLabel( this.useLabels );
			tc.setPaddingPx( this.borderPaddingPx );
			tc.setBorderPx( this.borderPaddingPx );
			tc.setFont( this.fnt );
			tc.setShowGrid( this.enableGrid );
			tc.update( this.virtualBody );
			}


		//  Save off Text space for the caption.		
		Rectangle captionMetrics = this.virtualBody.getStringPixelBounds(
				this.theCaptionFont, this.theCaption );
		this.captionTextWidth = captionMetrics.width;
		this.captionTextHeight =  captionMetrics.height;
		// Total space for the caption item
		this.captionHeight = new Double( (this.getFramePaddingPx() * 2) +
				this.captionTextHeight + (this.borderPaddingPx*2) );  
		
		//  This measures the font accent by using a non-descent char
		this.textAccent = 
			this.virtualBody.getStringPixelBounds( this.fnt, "8" ).height;
		
		int[] colWidths = new int[ this.colnum ];
		for( Integer c = 0; c < this.colnum; c++ )
			{ colWidths[c] = 0; }
		
		// For each cell... gather hgt metrics so that fixed column and heigth
		//    requirements can be gathered.   Also insert color info.
		for( Integer x : this.cells.keySet() )
			{
			this.cells.get(x).update( this.virtualBody );
			this.cells.get(x).setColors( this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_X.value() ),
				this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_Y.value() ), 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ), 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_LABELS.value() ) );
			
			int heightTotalCalc = this.cells.get(x).getTextHeight();
			if( heightTotalCalc > heightMax )
				{ heightMax = heightTotalCalc; }			
			}
		 
		//  Calculate the Y void space for each cell
		// The scaleVoid var is false when scaling happens, this is 
		//      due to how the virtualBody is sized.
		Integer yXS = this.h - ( heightMax * this.rownum );
		Integer yVoid = 0;  
//	System.out.println( "heightMax is "+Integer.toString(heightMax) + 
//		" Extra H space = " + Integer.toString( yXS ) );
		if( ! this.scaleVoid  &&  yXS > this.rownum )
			{ yVoid = yXS / this.rownum; }
			
		// Now that the tallest text is known, 
		//   Scale the image icons as they might affect the width
		// Then calculate the set of column widths 

		for( Integer x : this.cells.keySet() )
			{
			int cNum = this.determineColumn(x);
			this.cells.get(x).setFixedHeight( heightMax );

			// This should never happen... but guard against it anyway.
			if( cNum > this.colnum )   { continue; }

			if( this.useImagesAsLabels == true )
				{ this.cells.get(x).setImageForLabel( true ); }
			
			this.cells.get(x).update( this.virtualBody );

			if( this.cells.get(x).getTextWidth() > colWidths[cNum]  )
				{ colWidths[cNum] = this.cells.get(x).getTextWidth(); }
			// If set - Column width cannot exceed the user imposed limit.
			if( this.cellTruncationBounds.get(cNum)  > 0  &&
				colWidths[cNum] > this.cellTruncationBounds.get( cNum ) )
				{ colWidths[cNum] = this.cellTruncationBounds.get(cNum); }
			
			// Check for text that is too long and truncate them.
			
//			System.out.println( "Test: # "+Integer.toString(s) + ":" + val + " = " + lab +
//					" :len: "  + Integer.toString(lenVal) + " - " + Integer.toString(lenLab) + 
//					" COL # " + Integer.toString( cNum ) );
			}
		
		//  Compute x void.  -- Equally divided filler space
		Integer wVoidCalc = 0;
		Integer xVoid  =  0;
		for( int a = 0; a < this.colnum; a++ )
			{  wVoidCalc += colWidths[a]; }
		if( ! this.scaleVoid  &&  wVoidCalc + this.colnum < this.w )	// Width void space available
			{ xVoid = (this.w - wVoidCalc) / this.colnum; }
		
		// Record Width results and finish setting up cells.
		for( Integer x : this.cells.keySet() )
			{
			int cNum = this.determineColumn( x );
			if( cNum > this.colnum )   { continue; }
			if( x < this.colnum )		// Record column widths 
				{ this.calcColumnWidth.put( x, colWidths[cNum] ); }
			this.cells.get(x).setFixedWidth( colWidths[cNum] );
			this.cells.get(x).setXvoid( xVoid );
			this.cells.get(x).setYvoid( yVoid );
			}
		
		// Assembles the cells in the final row/column matrix.
		//  Also save off maximum extents for resizing virtualBody below.
		Integer cx = 0, cy = 0;
		Integer virtualX = 0, virtualY = 0;
		for( Integer x : this.cells.keySet() )
			{
			if( this.scanMode == false )
				{
				this.cells.get(x).reposition( cx, cy );
				cx += (this.cells.get(x).getTotalWidth() + xVoid);
				if( cx > virtualX )  { virtualX = cx; }
				if( this.determineColumn( x ) == (this.colnum-1) )
					{	// End of a row
					cx = 0;
					cy += this.cells.get(x).getTotalHeight() + yVoid;
					}
				if( cy > virtualY )  { virtualY = cy; }
				}
			else
				{
				this.cells.get(x).reposition( cx, cy );
				cy += (this.cells.get(x).getTotalHeight() + yVoid);
				if( cx > virtualX )  { virtualX = cx; }
				if( cy > virtualY )  { virtualY = cy; }
				if( this.determineColumn(x)  != this.determineColumn(x+1) )
					{	// time to move over to next column
					cx += (this.cells.get(x).getTotalWidth() + xVoid);
					cy = 0; 
					}
				}
			}

		// Resize the the virtual body to its final dimensions
		if(		virtualX != this.virtualBody.getWidth() ||  
				virtualY != this.virtualBody.getHeight() )
			{
			this.virtualBody = new VImage( virtualX, virtualY );
			}
		
		// With the VB finalized, relevant scroll bounds can be set.
		//   If negative, scroll for that dimension is essentially disabled. 
		this.scrollMaxX = this.virtualBody.getWidth() - this.w;
		this.scrollMaxY = this.virtualBody.getHeight() - this.h;
		
//		System.out.println( " --- R&C Final: " + Integer.toString(virtualX) +
//			" x "+Integer.toString(virtualY) + " :: " +
//			 Integer.toString( this.colnum ) + "x" + Integer.toString(this.rownum) );
		
		return;
		}

	/**  Re-allocates a full size VImage containing all data.
	 *    For simplicity, it has no border, caption, etc. 
	 *    The virtual body is then scaled into the confines of the main pane  
	 *    All other aspects are handled within the paint() method	*/
	private void redrawVirtualTable()
		{
		if( ! this.needsRefresh )   { return; }
		this.analyzeRowsCols();
		
		Integer vbw = this.virtualBody.getWidth();
		Integer vbh = this.virtualBody.getHeight();
		Integer cellMax = this.getCellCapacity();
		
			// Background bounding box (or image)
		if( this.enableImageBackdrop == true && this.bkgImg != null )
			{
				// Scale desired image to fit the box.
			this.virtualBody.scaleblit( 0, 0, vbw, vbh, this.bkgImg ); 
			}
		else	
			{
			this.virtualBody.rectfill( 0, 0, vbw, vbh, 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.BKG_MAIN.value()) );
			}
			
		// Due to the way the grid lines draw inside cells, 
		//   we need a line at the right and bottom edge here.
		if( this.enableGrid )
			{
			this.virtualBody.line( vbw-1, 0, vbw-1, vbh-1, 
				this.clrSettings.get(
				enumMenuDataTableCOLORS.GRIDLINES_X.value()) );
			this.virtualBody.line( 0, vbh-1, vbw-1, vbh-1, 
				this.clrSettings.get(
				enumMenuDataTableCOLORS.GRIDLINES_Y.value()) );
			}
			
		// Paint all the cells using their internal settings
		for( Integer tc : this.cells.keySet() )
			{  this.cells.get(tc).paint( this.virtualBody ); }

		this.needsRefresh = false;
		return;
		}
	
	// External methods for manipulating the scrolling
	//  Returns the current scrolled value
	public int doScrollHorizontalByPixel( int amount )
		{
		if( this.virtualBody.getWidth() < this.w )		// scrolling not possible 
			{ this.scrollX = 0;   return( 0 ); }
		this.scrollX += new Double( amount * this.scrollSpeed).intValue();
		if( this.scrollX < 0 )   { this.scrollX = 0; }
		if( this.scrollX > this.scrollMaxX )
				{ this.scrollX = this.scrollMaxX; }
		this.scrollFractionX = new Double( this.scrollX ) / 
				new Double( this.scrollMaxX );
//		System.out.println("Scroll X = "+Integer.toString(this.scrollX) + " " +
//				Double.toString(this.scrollFractionX) + " %   cell : " + 
//				Integer.toString(this.scrollXitems) );
		return( this.scrollX );
		}
	public int doScrollVerticalByPixel( double amount )
		{
		if( this.virtualBody.getHeight() < this.h )		// scrolling not possible 
			{ this.scrollY = 0;   return( 0 ); }
		this.scrollY += new Double( amount * this.scrollSpeed).intValue();
		if( this.scrollY < 0 )   { this.scrollY = 0; }
		if( this.scrollY > this.scrollMaxY )
				{ this.scrollY = this.scrollMaxY; }
		this.scrollFractionY = new Double( this.scrollY ) / 
				new Double( this.scrollMaxY );
//		System.out.println("Scroll Y = "+Integer.toString(this.scrollY) + " " +
//				Double.toString(this.scrollFractionY) + " %   Cell : " +
//				Integer.toString(this.scrollYitems) );
		return( this.scrollY );
		}
	
	// External methods for manipulating the scrolling
	//  Returns the current scrolled value
	public int doScrollHorizontalByCell( int amount )
		{
		if( this.scrollMaxX < 1 )		// scrolling not possible 
			{ this.scrollX = 0;   return( 0 ); }
		
		this.scrollXitems += amount;
		if( this.scrollXitems > this.getScrollMaxXcolumn() )
			{ this.scrollXitems = this.getScrollMaxXcolumn(); }
		if( this.scrollXitems < 0 )
			{ this.scrollXitems = 0; }
 
		this.scrollX = this.getColumnBaseX( this.scrollXitems );
		if( this.scrollX < 0 )   { this.scrollX = 0; }
		if( this.scrollX > this.scrollMaxX )
			{ this.scrollX = this.scrollMaxX; }
		
		this.scrollFractionX = 
			new Double( this.scrollX ) / new Double( this.scrollMaxX );
//		System.out.println("Scroll X = "+Integer.toString(this.scrollX) + " " +
//				Double.toString(this.scrollFractionX) + " %   cell : " + 
//				Integer.toString(this.scrollXitems) );
		return( this.scrollX );
		}
	public int doScrollVerticalByCell( int amount )
		{
		if( this.scrollMaxY < 1 )		// scrolling not possible 
			{ this.scrollY = 0;   return( 0 ); }

		this.scrollYitems += amount;
		if( this.scrollYitems > this.getScrollMaxYrow() )
			{ this.scrollYitems = this.getScrollMaxYrow(); }
		if( this.scrollYitems < 0 )
			{ this.scrollYitems = 0; }

		this.scrollY = this.getRowBaseY( this.scrollYitems );
		if( this.scrollY < 0 )   { this.scrollY = 0; }
		if( this.scrollY > this.scrollMaxY )
			{ this.scrollY = this.scrollMaxY; }
		
		this.scrollFractionY = new Double( this.scrollY / 
			new Double( this.virtualBody.getHeight() - this.h) );
//		System.out.println("Scroll Y = "+Integer.toString(this.scrollY) + " " +
//				Double.toString(this.scrollFractionY) + " %   Cell : " +
//				Integer.toString(this.scrollYitems) );
		return( this.scrollY );
		}
	
	public void doScrollByPixel( int x, int y)  // Operates both at once.
		{
		this.doScrollHorizontalByPixel( x );
		this.doScrollVerticalByPixel( y );
		return;
		}
	public void doScrollByCell( int x, int y)  // Operates both at once.
		{
		this.doScrollHorizontalByCell( x );
		this.doScrollVerticalByCell( y );
		return;
		}
	
	
	/** ----------------------- non-interface methods ---------------------- **/	


	public boolean isEnableGrid()
		{	return( this.enableGrid ); }
	public void setEnableGrid( boolean enableGrid )
		{	this.enableGrid = enableGrid;	 return; }
	
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
	public void addEntry( Integer cellNum, String label, String data )
		{
		if( ! this.cells.containsKey(cellNum) )
			{
			this.cells.put( cellNum, 
					new TableCell(cellNum, 0,0, "new cell", this.fnt ) );
			}
		this.cells.get(cellNum).setText( data );
		this.cells.get(cellNum).setLabel( label );
		this.needsRefresh = true;
		return;
		}

	public void setFont( Font newFnt )
		{
		this.fnt = newFnt;
		this.needsRefresh = true;
		return;
		}

	/** Returns the predefined rows * columns */
	public int getCellCapacity()
		{ return( this.colnum * this.rownum );	}
	/** Returns the actual count of internal cell objects.
	 * This can be different then a simple rows * columns calculation.  */
	public Integer getCellCount()
		{ return( this.cells.size()); }
	
	public void setBackgroundImage( VImage img )
		{
		if( img == null  )   { return; }
		this.bkgImg = img;
		this.needsRefresh = true;
		return;
		}
	public void setBackgroundImage( VImage img, boolean enable )
		{
		if( img == null )  { return; }
		this.bkgImg = img;
		if( enable == true )
			{  this.enableImageBackdrop = true; }
		this.needsRefresh = true;
		return;
		}
	
	/** Dynamically change the number of columns allocated.
	 * Data is not cleared, and will be rearranged automatically 
	 * depending on the current scanmode  and may cause undesired affects. */
	public boolean setNumColumns( int columns )
		{
		if( columns == this.colnum )	 { return(false); }
		if( columns < 1 )	 { return(false); }
		this.colnum = columns;
		this.needsRefresh = true;
		return(true);
		}
	/** Dynamically change the number of rows allocated.
	 * Data is not cleared, and will be rearranged automatically 
	 * depending on the current scanmode and may cause undesired affects. */
	public boolean setNumRows( int rows )
		{
		if( rows == this.rownum )	 { return(false); }
		if( rows < 1 )	 { return(false); }
		this.rownum = rows;
		this.needsRefresh = true;
		return(true);
		}
	
	public void enableCaption( boolean onOff )
		{
		if( this.enableCaption != onOff )
			{ this.needsRefresh = true; }
		this.enableCaption = onOff;
		return;
		}

	public boolean toggleCaption()
		{
		if( this.theCaption.isEmpty() )  { return(this.enableCaption); }
		this.enableCaption = ! this.enableCaption;
		this.needsRefresh = true;
		return(this.enableCaption);
		}

	/** Sets the caption, with its own font */
	public void setCaption( String caption, Font capFont, boolean enable)
		{
		if( caption.length() < 1 )  { return; }
		this.theCaption = caption;
		this.theCaptionFont = capFont;
		this.enableCaption(enable);
		this.needsRefresh = true;
		return;
		}
	/** Sets the caption using the same font as the rest of the table.  */
	public void setCaption( String caption, boolean enable )
		{ this.setCaption( caption, this.fnt, enable);   return; }

	/** Change the caption font */
	public void setCaptionFont( Font capfont )
		{
		this.theCaptionFont = capfont;
		this.needsRefresh = true;
		return;
		}

	public boolean isEmpty()
		{	return(this.cells.isEmpty());	}
	
	/** If there is more space allocated by the tables width & height
	 * then the data contents need, the content will scale into the 
	 * void space when this is ON. */	
	public void setScaleVoid( Boolean onOff )
		{
		if( this.scaleVoid != onOff )  
			{ this.needsRefresh = true; }
		this.scaleVoid = onOff;
		return;
		}

	public String getDecimalFormatter()
		{	return decimalFormatter;	}

	public void setDecimalFormatter(String decimalFormatter)
		{	this.decimalFormatter = decimalFormatter;	}
	
		/** Assignes integer numeric labels to data */
	public void autoNumericLabels( )
		{
		for( Integer x :  this.cells.keySet() )
			{
			if( ! this.cells.containsKey(x) )  { continue; }
			this.cells.get(x).setLabel( x.toString() );
			}
		this.needsRefresh = true;
		return;
		}

	/** When the internal inspection routine runs, This setting 
	 * will truncate cell content longer then the given pixel-length in set column 
	 * Returns the previous value for the column, 0 if none.  */
	public Integer setColumnWidthLimitsByColumn( 
			Integer columnNum, Integer maxWidthPx )
		{
		if( this.cellTruncationBounds == null )
			{ this.cellTruncationBounds = new HashMap<Integer,Integer>(); }
		Integer previous = 0;
		if( this.cellTruncationBounds.containsKey(columnNum) )
			{  previous = this.cellTruncationBounds.get( columnNum ); }
		this.cellTruncationBounds.put( columnNum, maxWidthPx );
		this.needsRefresh = true;
		return( previous );
		}

	/** When the internal inspection routine runs, This setting 
	 * will truncate cell content longer then the given pixel-width in ALL columns 
	 * Returns the Number of columns changed.  */
	public Integer setColumnWidthLimits( Integer maxWidthsPx )
		{
		Integer setCount = 0;
		for( Integer a = 0;  a < this.colnum;  a++ )
			{
			if( this.setColumnWidthLimitsByColumn(a, maxWidthsPx) > 0 )
				{ setCount++; }
			}
		return( setCount );
		}

	/** adds a column to the table.  Returns the total column count. */
	public Integer addColumn( )
		{
		this.colnum++;
		this.needsRefresh = true;
		for( int a = 0; a < this.colnum; a++ )
			{
			if( ! this.cellTruncationBounds.containsKey(a) )
				{ this.cellTruncationBounds.put( a, 0 ); }
			}
		return( this.colnum );
		}
	/** adds a row to the table.  Returns the total row count. */
	public Integer addRow( )
		{
		this.rownum++;
		this.needsRefresh = true;
		return( this.rownum );
		}
	/** adds a column to the table.  Returns the total column count. */
	public Integer removeColumn( )
		{
		if( this.colnum <= 1 )   { return(1); }
		this.colnum--;
		this.needsRefresh = true;
		return( this.colnum );
		}
	/** adds a row to the table.  Returns the total row count. */
	public Integer removeRow( )
		{
		if( this.rownum <= 1 )   { return(1); }
		this.rownum--;
		this.needsRefresh = true;
		return( this.rownum );
		}	

	public boolean toggleScaleVoid()
		{
		this.scaleVoid = ! this.scaleVoid;
		this.needsRefresh = true;
		return( this.scaleVoid );
		}

	public boolean toggleLabels( )
		{
		this.useLabels = ! this.useLabels;
		this.needsRefresh = true;
		return( this.useLabels );		
		}
	
	public boolean toggleFrame()
		{
		this.enableBorder = ! this.enableBorder;
		this.needsRefresh = true;
		return( this.enableBorder );		
		}

	public boolean toggleGridlines()
		{
		this.enableGrid = ! this.enableGrid;
		this.needsRefresh = true;
		return( this.enableGrid );
		}

	}			// END class  VmiDataTable.


