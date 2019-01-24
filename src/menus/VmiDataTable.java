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
	private Double colMaxW = new Double(0);
	private Double rowMaxH = new Double(0);
	private Double captionHeight = new Double(0.0d);
	private Double captionHeightSave = new Double(0.0d);
	private int borderWidthPx = 3;
	private int borderPaddingPx = 10;
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

		// For scrolling
	private VImage virtualBody = new VImage( 24,24 );
	private boolean enableScroll = false;
	private boolean enableScrollBars = false; 
	private Double scrollSpeed = 1.0d;
	private Double scrollFractionX = 0.0d;
	private Double scrollFractionY = 0.0d;
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
		this.analyzeRowsCols();		
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

		this.analyzeRowsCols();
		return;
		}
	
		// Sets only essentials. - simplifies constructors.
	public void init( int pinX, int pinY, int pixelWidth, int pixelHeight,
			int numColumns, int numRows )
		{
		this.x = pinX;
		this.y = pinY;
		this.w = pixelWidth;
		this.h = pixelHeight;
		this.colnum = numColumns;
		this.rownum = numRows;
		this.cells = new HashMap<Integer,TableCell>();
		
		this.virtualBody = new VImage( this.w+24, this.h+24 );
		
		// Init inner cells.
		for( Integer n = 0; n < this.getDataCellCount(); n++ )
			{  this.cells.put( n, new TableCell( n,-1,-1,"", this.fnt) );  }
		
		this.clrSettings 	= new HashMap<Integer,Color>();
		
		//  Shut off truncation on default.
		this.cellTruncationBounds = new HashMap<Integer,Integer>();
		for( int c = 0; c < numColumns; c++ )
			{ this.cellTruncationBounds.put( c, 0); }

		this.theCaption = new String("");
		this.theCaptionFont = this.fnt;
		this.enableCaption = false;
		this.scaleVoid = false;

		this.active = false;
		this.visible = true;
		this.scanMode = false;	// Fill across columns, then rows, by default.
		this.enableImageBackdrop = false;
		}

	
	private class TableCell
		{
		private Integer myX,myY,id;
		private VImage icon = null;
		private String myText,myLabel;
		private String labelTerminator;
		private Integer pxW, pxH, pxBorder, pxPadding;
		private Integer calcW, calcH;
		private Boolean showLabel, showImageAsLabel;
		private Boolean needsUpdate;
		private Boolean showVoidSpace;
		private Font myFont;
		private Color clrGridX, clrGridY, clrText, clrLabel;
		private Integer bareAccent;		// Text accent value of font
		private Integer imgW, imgH;
		private Integer xVoidSpace, yVoidSpace;
		
		public TableCell( int idNum, int x, int y, String text, Font fnt )
			{
				// Essentials
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
			
		public void setIcon( VImage vimg )
			{
			this.icon = vimg;
			this.needsUpdate = true;
			return; 
			}
		
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
				System.out.println("Need to shorten # "+Integer.toString( this.id ) +
						" by " + Integer.toString(breaker) + " of " +
						Integer.toString(fullText.length()));
				if( fullText.length() > breaker )
					{
					this.myText = this.myText.substring( 0, 
						fullText.length() - breaker);
					this.calcW = this.pxW - 1;
					}
				else { 
					this.myText = new String("|||");
					this.calcW = 20;	// TODO - get this right. 
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
			
			int imgAdj = 0;
			if( this.showImageAsLabel == true )
				{
				target.scaleblit( inset + this.myX, inset + this.myY,
						this.imgW, this.imgH, this.icon );
				imgAdj = this.imgW + 1;
				}
			
			target.printString( inset + this.myX + imgAdj, 
				inset + this.myY + this.bareAccent, 
				this.myFont, this.clrText, this.myText );

			
			target.line( this.myX, this.myY, 
				this.myX+this.pxW+outset+this.xVoidSpace,
				this.myY, this.clrGridX );
			target.line(  this.myX, 
				this.myY + this.pxH + outset + this.yVoidSpace,
				this.myX + this.pxW + outset + this.xVoidSpace,
				this.myY + this.pxH + outset + this.yVoidSpace, 
				this.clrGridX );
			target.line(  this.myX, this.myY, 
				this.myX, this.myY + this.pxH + outset + this.yVoidSpace,
				this.clrGridY );
			target.line( this.myX + this.pxW + outset + this.xVoidSpace, 
				this.myY, 
				this.myX + this.pxW + outset + this.xVoidSpace,
				this.myY + this.pxH + outset + this.yVoidSpace, this.clrGridY );
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

	/** Searching in order from the beginning, Returns the last index value
	* of the entry matching the argument conditions contiguously.  
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
	
	private void insertEntries( HashMap<String,String> entries )
		{
		if( entries == null || entries.isEmpty() )
			{ return; }
		// Inject new values at the end of known.
		Integer n = this.getLastEntry( true, true );
		for(  String key : entries.keySet() )
			{
			n++;
			this.cells.get( n ).setText(  new String( entries.get(key) ) );
			this.cells.get( n ).setLabel( new String( key ) );
//			this.theData.put( n, new String( entries.get(key) ) );
//			this.theLabels.put( n, new String( key ) );
			}
		return;
		}

	// Adjusts the vertical positioning associated with introducing a caption
	//  Factor is a Double that scales the height of the caption.
	private void justifyCaption( Double factor )
		{
		this.rowMaxH = new Double( this.h - 
			(2*this.borderPaddingPx+this.borderWidthPx)) / this.rownum;

		if( ! this.enableCaption )  { return; }

		if( this.enableCaption == true && ! this.theCaption.isEmpty() )
			{
			Double indent = new Double(
				2*this.borderPaddingPx+this.borderWidthPx );
			// Do not calculate this unless we need to.
			if( this.captionHeight <= 0.0d )
				{
				this.captionHeight = new Double(this.rowMaxH * factor );
				}
			// The caption reduces space available for the normal table cells.
			this.rowMaxH = 
				(new Double( this.h ) - indent - this.captionHeight) / 
				new Double( this.rownum );
			}
		return;
		}
	
	public boolean reposition(int posX, int posY, int relPosX, int relPosY)
		{
		if( (posX+relPosX < 0) || (posY+relPosY < 0) )
			{ return(false); }
		this.x = posX+relPosX;
		this.y = posY+relPosY;
		this.analyzeRowsCols();
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


	public boolean animate( VImage target )
		{ return false; }
	
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
		//  x1 & y1 are the main body top-left position after all adjustments.
		int x1 = x0 + this.borderWidthPx;
		int y1 = y0 + this.borderWidthPx;
		int x2 = x0 + this.w;
		int y2 = y0 + this.h;

		int xw = x2 - this.borderWidthPx - this.borderPaddingPx;
		int yh = y2 - this.borderWidthPx - this.borderPaddingPx;
		Double tmp;
		
		this.redrawVirtualTable();

		// recalculate this in case margins have changed.
		this.justifyCaption( 1.2d );
		
		int bodyWidth =  this.w - (this.borderWidthPx * 2) + 1;
		int bodyHeight =  this.h - (this.borderWidthPx * 2) + 1;

		//  -- Caption Calculations --
		Double captionStartX = new Double( 0.0d );
		Double captionStartY = new Double( 0.0d );
		if( this.enableCaption == true  &&  ! this.theCaption.isEmpty() )
			{
			Rectangle captionMetrics = target.getStringPixelBounds(
					this.theCaptionFont, this.theCaption );

			//  Text space for the caption.
			int capWidth    = captionMetrics.width;
			int captHeight =  captionMetrics.height;
			
			this.captionHeight = new Double( (this.getFramePaddingPx() * 2) +
					captHeight + (this.borderPaddingPx*2) ); 

			// Analysis is expensive.  Only do so if a change is detected.
			if( this.captionHeight != this.captionHeightSave )
				{
				this.analyzeRowsCols();
				this.captionHeightSave = this.captionHeight;
				}

			captionStartX = new Double( this.w - capWidth ) / 2;
			captionStartX += new Double( x0 );
			captionStartY = new Double( y0 + this.borderWidthPx + 
					this.borderPaddingPx + captHeight );

			y1 = y0 + this.captionHeight.intValue();
			bodyHeight -= this.captionHeight.intValue();
			}
		else if( this.captionHeight != 0.0d )
			{
			this.captionHeight = 0.0d;
			captionStartX = 0.0d;
			captionStartY = 0.0d;
//			y1 = y0 + this.borderWidthPx + this.borderPaddingPx;
			this.analyzeRowsCols();
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
		
		// Draws the scrollbars
		if( this.enableScrollBars )
			{
			int subX = x2 - this.borderWidthPx;
			int subY = y2 - this.borderWidthPx;
			// First need the amount of space that is possibly scrollable.
			Double xScrollable = 
				new Double( this.virtualBody.getWidth() - bodyWidth );
			Double yScrollable = 
				new Double( this.virtualBody.getHeight() - bodyHeight );				
			int sBarVX = subX - 5;
			int sBarVY = y1;
			int sBarHX = x1;
			int sBarHY = subY - 5;
			Double scrX = new Double( new Double(bodyWidth) / this.virtualBody.getWidth() );
			Double scrY = new Double( new Double( bodyHeight ) / this.virtualBody.getHeight() );
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

//		System.out.println( " Data # " + Integer.toString( this.theData.size() ) );

		// Write the Caption.
		if( this.enableCaption == true && ! this.theCaption.isEmpty() )
			{
			target.printString( captionStartX.intValue(),  captionStartY.intValue(),  
				this.theCaptionFont, 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ),
				this.theCaption );
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
		this.analyzeRowsCols();
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
		{
		for( Integer x : textItems.keySet() )
			{
			this.cells.get(x).setText(
					new String( textItems.get(x)) );
			}
		return;
		}
	
	/**  Import a HashMap containing the Labels for the corresponding cell #..
	 * Returns an Integer of the number actually set.
	 */
	public Integer setTextLabels( HashMap<Integer, String> textLabels )
		{
		Integer n = 0;
		for( Integer x : textLabels.keySet() )
			{
			if( textLabels.get(x) == null  ||  textLabels.get(x).length() < 1 ) 
				{ continue; }
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
			this.cells.get(x).setIcon( imageItems.get(x) );
			if( turnOnImages == true )
				{ this.cells.get(x).setImageForLabel( true ); }
			}
		return;
		}

	/**  in array form, sends VImage icon data into the object.
	 *    one per data cell.
	 *    will ignore if more icons then there are data values. 
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
		return;
		}

	public void setColorContent(HashMap<Integer, Color> basicColors)
		{
		if( basicColors == null )	{ return; }
		for( Integer cn : basicColors.keySet() )
			{
			this.clrSettings.put(cn, basicColors.get(cn) );
			}
//		this.clrSettings = basicColors;
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
		{	this.useLabels = enable;	}

	/** Toggles if icons are used instead of String labels */
	public void enableIcons(boolean enable)
		{	this.useImagesAsLabels = enable;	}

	public void enableBackdrop(boolean enable)
		{	this.enableImageBackdrop = enable;	}

	public void enableFrame( boolean enable)
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

	public void setFrameThicknessPx( int thick)
		{
		if( thick < 1 )	{ return; }
		this.borderWidthPx = thick;
		this.analyzeRowsCols();
		return;
		}

	public void setFramePaddingPx( int thick )
		{
		if( thick < 1 )	{ return; }
		this.borderPaddingPx = thick;
		this.analyzeRowsCols();
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
	
	private Integer determineColumn( int index )
		{
		if( this.scanMode == true )
			{ return( index / this.rownum ); }
		return( index % this.colnum );
		}
	
	/**  --   Drawing and Scroll related support methods ----------  */

	public void setScrollable( Integer dispRows, Integer dispCols, 
			Double speed, boolean showScrollBars )
		{
		this.enableScroll = true;
		this.enableScrollBars = showScrollBars; 
		this.scrollSpeed = speed;
		this.scrollX = 0;
		this.scrollY = 0;
		this.scrollXitems = dispCols;
		this.scrollYitems = dispRows;
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
		return;		
		}
	
	/** Examine data and determine metrics needed for cols & rows. */
	private void analyzeRowsCols()
		{
		Integer widthTotal = 0;
		Integer heightMax = 0;
		
		if( this.cells == null  ||  this.cells.isEmpty()  ||  this.cells.size() == 0 )
			{ System.out.println("WARN:  VmiDataTable has no data!");  return; } 
		
		this.justifyCaption( 1.2d );
		
		//  This measures the font accent by using a non-descent char
		this.textAccent = 
			this.virtualBody.getStringPixelBounds( this.fnt, "8" ).height;
		
		int[] colWidths = new int[ this.colnum ];
		for( Integer c = 0; c < this.colnum; c++ )
			{ colWidths[c] = 0; }
		
		// For each cell... gather hgt metrics so that column and height
		//    requirements can be gathered.   Also insert color info.
		for( Integer x : this.cells.keySet() )
			{
			this.cells.get(x).update( this.virtualBody );
			this.cells.get(x).setColors( this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_X),
				this.clrSettings.get(
					enumMenuDataTableCOLORS.GRIDLINES_Y), 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA), 
				this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_LABELS));
			
			int heightTotalCalc = this.cells.get(x).getTextHeight();
			if( heightTotalCalc > heightMax )
				{ heightMax = heightTotalCalc; }			
			}
		
		//  Calculate the Y void space for each cell
		// The scaleVoid var is false when scaling happens, this is 
		//      due to how the virtualBody is sized.
		Integer yXS = this.h - ( heightMax * this.rownum );
		Integer yVoid = 0;  
		System.out.println( "heightMax is "+Integer.toString(heightMax) + 
				" Extra H space = " + Integer.toString( yXS ) );
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
		
		for( Integer x : this.cells.keySet() )
			{
			int cNum = this.determineColumn( x );
			if( cNum > this.colnum )   { continue; }
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
		
		this.rowMaxH = new Double( heightMax );

		// Resize the the virtual body to its final dimensions
		if(		virtualX != this.virtualBody.width ||  
				virtualY != this.virtualBody.height )
			{	// If the VI is resized, it must also be promptly redrawn
			this.virtualBody = new VImage( virtualX, virtualY );
			this.redrawVirtualTable();
			}
		
		System.out.println( " --- Final: " + Integer.toString(virtualX) +
			" x "+Integer.toString(virtualY) + " :: " +
			 Integer.toString( this.colnum ) + "x" + Integer.toString(this.rownum) );
		
		return;
		}

	/**  Re-allocates a full size VImage containing all data.
	 *    For simplicity, it has no border, caption, etc. 
	 *    The virtual body is then scaled into the confines of the main pane  
	 *    All other aspects are handled within the paint() method	*/
	private void redrawVirtualTable()
		{
		this.analyzeRowsCols();
		Double tmp;
		Integer indent = this.borderPaddingPx + this.borderWidthPx;
		
		Integer vbw = this.virtualBody.getWidth();
		Integer vbh = this.virtualBody.getHeight();
		Integer cellMax = this.getDataCellCount();
		
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
			
		// Paint all the cells using their internal settings
		for( Integer tc : this.cells.keySet() )
			{  this.cells.get(tc).paint( this.virtualBody ); }

		if( this.useImagesAsLabels == true )
			{  this.virtualBody.copyImageToClipboard(); }

		return;
		}
	
	// External methods for manipulating the scrolling
	//  Returns the current scrolled value
	//  TODO   enforce bounds
	public int doScrollHorizontal( int amount )
		{
		if( this.virtualBody.width < this.w )		// scrolling not possible 
			{ this.scrollX = 0;   return( 0 ); }
		this.scrollX += new Double( amount * this.scrollSpeed).intValue();
		if( this.scrollX < 0 )   { this.scrollX = 0; }
		if( (this.scrollX + this.w) > this.virtualBody.width )
			{ this.scrollX = this.virtualBody.getWidth() - this.w; }
		this.scrollFractionX = new Double( this.scrollX / 
			new Double(this.virtualBody.getWidth() - this.w) );
		System.out.println("Scroll X = "+Integer.toString(this.scrollX) + " " +
				Double.toString(this.scrollFractionX) + " % " );
		return( this.scrollX );
		}
	public int doScrollVertical( double amount )
		{
		if( this.virtualBody.height < this.h )		// scrolling not possible 
			{ this.scrollY = 0;   return( 0 ); }
		this.scrollY += new Double( amount * this.scrollSpeed).intValue();
		if( this.scrollY < 0 )   { this.scrollY = 0; }
		if( (this.scrollY + this.h) > this.virtualBody.height )
			{ this.scrollY = this.virtualBody.getHeight() - this.h; }
		this.scrollFractionY = new Double( this.scrollY / 
			new Double( this.virtualBody.getHeight() - this.h) );
		System.out.println("Scroll Y = "+Integer.toString(this.scrollY) + " " +
				Double.toString(this.scrollFractionY) + " % " );
		return( this.scrollY );
		}
	public void doScroll( int x, int y)  // Operates both at once.
		{
		this.doScrollHorizontal( x );
		this.doScrollVertical( y );
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
	public void addEntry( Integer cellNum, String label, String data )
		{
		this.cells.get(cellNum).setText( data );
		this.cells.get(cellNum).setLabel( label );
		this.analyzeRowsCols();
		return;
		}

	public void setFont(Font newFnt )
		{
		this.fnt = newFnt;
		this.analyzeRowsCols();
		return;
		}

	public int getDataCellCount()
		{	return( this.colnum * this.rownum );	}
	
	public void setBackgroundImage( VImage img )
		{	this.bkgImg = img;	}
	public void setBackgroundImage( VImage img, boolean enable )
		{	
		this.bkgImg = img;
		if( enable == true )
			{  this.enableImageBackdrop = true; }
		return;
		}
	
	public boolean setNumColumns( int columns )
		{
		if( columns == this.colnum )	 { return(false); }
		if( columns < 1 )	 { return(false); }
		this.colnum = columns;
		this.analyzeRowsCols();
		return(true);
		}

	public boolean setNumRows( int rows )
		{
		if( rows == this.rownum )	 { return(false); }
		if( rows < 1 )	 { return(false); }
		this.rownum = rows;
		this.analyzeRowsCols();
		return(true);
		}
	
	public void enableCaption( boolean onOff )
		{
		this.enableCaption = onOff;
		return;
		}

	public boolean toggleCaption()
		{
		if( this.theCaption.isEmpty() )  { return(this.enableCaption); }
		this.enableCaption = ! this.enableCaption;
		return(this.enableCaption);
		}

	public void setCaption(String caption, Font capFont, boolean enable)
		{
		this.theCaption = caption;
		this.theCaptionFont = capFont;
		this.enableCaption(enable);
		this.analyzeRowsCols();
		return;
		}

	public boolean isEmpty()
		{	return(this.cells.isEmpty());	}
		
	/** If there is more space allocated by the tables width & height
	 * then the data contents need, the content will scale into the 
	 * void space when this is ON. */	
	public void setScaleVoid( Boolean onOff )
		{
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
			{ this.cells.get(x).setLabel( x.toString() ); }
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

	/** Reduce a string to the given pixel-width or less by 
	 *     repeatedly chopping characters off the end of it. 
	 *     Used to ensure text content fits within table cells.  */
	private String shortenOverbounds( String oversized, Integer bound )
		{
		String tmpStr = new String("");
		int shorten = bound;
		int breaker = 0;
		while( (shorten >= bound) && tmpStr.length() > 2 
				&& breaker < 500 ) 
			{
			breaker++;
			tmpStr = oversized.substring( 0, tmpStr.length()-1 );
			shorten = this.virtualBody.getStringPixelBounds(
						this.fnt, tmpStr ).width;
			}
		return( tmpStr );
		}

	}			// END class  VmiDataTable.







/*
 * 		// Finally, print all the labels & values.
//		String label = new String("");
		String tmpLabel = new String("");
		tmpLabel.trim();
		String tmpData = new String("");
		tmpData.trim();
		
		for( Integer s = 0; s < this.theData.size(); s++ )
			{
			if( s+1 > this.getDataCellCount() )		{ continue; }
			tmpData = this.theData.get(s);
			tmpLabel = this.theLabels.get(s)+this.labelTerminator;
			
			int dw0 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getWidth() ).intValue();
//			int dy0 = new Double( this.fnt.getStringBounds(
//					tmpData, frc ).getMaxY() ).intValue();
			int dy1 = new Double( this.fnt.getStringBounds(
					tmpData, frc ).getMinY() ).intValue();
//			int dy2 = new Double( this.fnt.getStringBounds(
//					tmpData, frc ).getHeight() ).intValue();

			double dy3 = this.fnt.getStringBounds( tmpData, frc ).getY();
			// half of unutilized space
//			int dy4 = new Double((this.rowMaxH + dy3) / 2.0d ).intValue();
			int dy4 = new Double( Math.abs(dy3) / 2.0d ).intValue();
			
//			int dy3 = 	Math.abs( new Double( 
//		this.fnt.getStringBounds( tmpData, frc ).getY() +
//		this.fnt.getStringBounds( tmpData, frc ).getMinY() ).intValue() )+1;
			
			int shorten = dw0;
				// This thing is longer then the space allocated.
				//   will need to truncate it.
	while( (shorten > this.colMaxW) && tmpData.length() > 2 )
		{
		tmpData = tmpData.substring(0, tmpData.length()-2 );
		shorten = new Double( this.fnt.getStringBounds(
			tmpData, frc ).getWidth() ).intValue();
		}

			if( tmpData.isEmpty() )	{ continue; }
			
			// note: indentation is built into : this.theXpos.get(s) 			
			int Xadj = x0 + this.theXpos.get(s).intValue() 
					+ this.colMaxW.intValue() - shorten - 1;
			// Bumps text baseline down to vertical center of cell 
			int Yadj = y0 + this.theYpos.get(s).intValue()
					+ (this.rowMaxH.intValue() / 2) + dy4;
//					+ (dy3 / 2);
//					+ (this.rowMaxH.intValue() / 2) + dy0;

			if( this.useLabels == true )
				{
				// Image icons used when turned on + the image
				//  is present, and the cell hgt allowcated is > 6 pixels.
				if( 		this.useImagesAsLabels == true &&  
						this.imgLabels.get(s) != null && 
						this.rowMaxH > 6.0d )
					{
					
					// Scale to the height of the cell.
					Double scFct = new Double(
						this.imgLabels.get(s).height) / (this.rowMaxH-4.0d);
					target.scaleblit( 2 + x0 + this.theXpos.get(s).intValue(), 
						2 + y0 + this.theYpos.get(s).intValue(), 
						new Double(this.imgLabels.get(s).width / scFct).intValue(), 
						this.rowMaxH.intValue() - 4, 
						this.imgLabels.get(s) );
					}
				else
					{
					shorten = new Double( this.fnt.getStringBounds(
							tmpLabel, frc ).getWidth() ).intValue();
					while( (shorten > this.colMaxW) && tmpLabel.length() > 2 ) 
						{
						tmpLabel = tmpLabel.substring(0, tmpLabel.length()-2 );
						shorten = new Double( this.fnt.getStringBounds(
							tmpLabel, frc ).getWidth() ).intValue();
						}

					target.printString( 1 + x0 + this.theXpos.get(s).intValue(),  
						y0 - dy1 + this.theYpos.get(s).intValue(), 
						this.fnt, this.clrSettings.get(
							enumMenuDataTableCOLORS.TEXT_DATA.value() ),
						tmpLabel	);
					}
				}

//			System.out.println( " string x/y " + 
//				Integer.toString( Xadj ) + " / " +  
//				Integer.toString( Yadj ) );

			target.printString( Xadj, Yadj, 
				this.fnt, this.clrSettings.get(
					enumMenuDataTableCOLORS.TEXT_DATA.value() ),
				tmpData	);
			
			}

 * */
