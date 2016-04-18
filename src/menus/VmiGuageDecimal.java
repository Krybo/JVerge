package menus;

import java.awt.Color;
import java.util.HashMap;

import domain.VImage;
import domain.VImageGradient;

/**  Decimal Guage
 * Holds a current / Maximum / Minimum value of Double type.
 * Can be constructed using Float or Double, but will use Double internally.
 * A fixed width and Height are allocated.  Orientation automatically
 * changes from horizontal to Vertical depending on how much width/height
 * is allowed.     Images and Gradients may be optionally used to 
 * improve drawn appearance.
 * (Krybo Apr.2016)
 * @author Krybo
 *
 */

public class VmiGuageDecimal extends VmiTextSimple 
		implements Vmenuitem
	{

	private int myWidth = 0;
	private int myHgt = 0;
	private Double capacity = 0.0d;
	private Double currentValue = 0.0d;
	private Double maxValue = 0.0d;
	private Double minValue = 0.0d;

	private Color coreColor;
	private Color edgeColor;
	
	private VImage gradientBody;
	private VImage guageHeart;
		// heart is essentially the same as gradientbody with a truncation
		// due to the guage's value.
	private VImage imageBorder = null;
	private VImage guageShell = null;
		// when using image borders - this is the amount of pixels along
		//  the 4 edges that will be cleared before the border is blit.
	private int borderObscure = 0;
		// 	true = horizontal  :: false for vertical.
	private boolean orientation = true;
	
	/**  Integer value Guage full constructor. 
	 *   Sets dimensions, values, and colors.
	 * @param relX	screen pixel position.
	 * @param relY	screen pixel position.
	 * @param width	fixed pixel-width of the guage
	 * @param hgt	fixed pixel-height of the guage
	 * @param startValue	the initial value of the guage
	 * @param valMax		maximum value "full guage" if value reaches  
	 * @param valMin		minimum value "empty guage" if value reaches
	 * @param clrCore		Color in the core of the guage fill
	 * @param clrEdge		Edge color the core grades into.
	 */
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Double startValue, Double valMax, Double valMin,
			Color clrCore, Color clrEdge )
		{
		super( ".", relX, relY);
		this.currentValue = startValue;
		
		if( valMin > valMax )
			{
			this.minValue = valMax;
			this.maxValue = valMin;			
			}
		else
			{
			this.minValue = valMin;
			this.maxValue = valMax;
			}
		if( valMin == valMax )
			{
			this.minValue = valMin;
			this.maxValue = valMin+1.0d;
			}
		
		this.imageBorder = null;
		this.guageShell = null;
		this.setBorderObscure(0);
		super.setExtendX( width, false);
		super.setExtendY( hgt, false);
		this.myWidth = width;
		this.myHgt = hgt;

//			super.enableActivation();
		super.enableBackdrop(false);
		super.enableIcons(false);
		super.enableText(false);
		
		// set horizontal or vertical depending on w/h 
		if( width >= hgt )
			{	this.orientation = true;	}
		else {	this.orientation = false;	}

		if( clrCore == null || clrEdge == null )
			{
			this.coreColor = Color.WHITE;
			this.edgeColor = Color.BLACK;
			this.gradientBody = defaultGradient();
			}
		else
			{	this.setTwoColorGradient(clrCore,clrEdge);	}
		this.recalculate();

		return;
		}


	/** Resets the colors used to draw the heart (bar) of the guage.
	*    this will overwrite any previous appearance set. 
	*    
	 * @param coreColor  Color in the center of the bar.
	 * @param edgeColor  Color core grades into near the edges.
	 */
	public void setBarColors( Color coreColor, Color edgeColor )
		{
		this.setTwoColorGradient( coreColor,  edgeColor );
		this.recalculate();
		return;
		}
	
	private void setTwoColorGradient(Color coreColor, Color edgeColor )
		{
		this.coreColor = coreColor;
		this.edgeColor = edgeColor;
		HashMap<Double,Color> hmTmp = 
				new HashMap<Double,Color>();	
		hmTmp.put( 0.00d, this.edgeColor );
		hmTmp.put( 0.40d, this.coreColor );
		hmTmp.put( 0.60d, this.coreColor );
		hmTmp.put( 1.00d, this.edgeColor );
		this.gradientBody = new VImageGradient( 300, hmTmp, null);
		}

	/** Up-scales Float to Double and delegates that constructor */
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
		Float startValue, Float valMax, Float valMin,
		Color clrCore, Color clrEdge )
			{
			this( relX, relY, width, hgt, startValue.doubleValue(), 
				valMax.doubleValue(), valMin.doubleValue(),
				clrCore, clrEdge );
			return;
			}
	
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Double startValue, Double valMax, Double valMin )
		{
		this( relX , relY, width, hgt, startValue, valMax, valMin, 
				new Color(1.0f,0.0f,0.0f), new Color(0.0f,0.0f,0.0f) );
		this.gradientBody = defaultGradient();
		this.recalculate();
		return;
		}

	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Float startValue, Float valMax, Float valMin )
			{
			this( relX, relY, width, hgt,startValue.doubleValue(), 
				valMax.doubleValue(), valMin.doubleValue() );
			return;
			}
	
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Double startValue, Double valMax, Double valMin, 
			VImageGradient colors )
		{
		this( relX , relY, width, hgt, startValue, valMax, valMin );
		this.gradientBody = colors;
		this.recalculate();
		return;
		}

	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
		Float startValue, Float valMax, Float valMin, 
		VImageGradient colors )
			{
			this( relX, relY, width, hgt, startValue.doubleValue(), 
				valMax.doubleValue(), valMin.doubleValue(), 	colors );
			return;
			}
	
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Double startValue, Double valMax, Double valMin, 
			VImageGradient colors, VImage borderImage )
		{
		this( relX , relY, width, hgt, startValue, valMax, valMin );
		this.gradientBody = colors;
		this.recalculate();
		this.imageBorder = borderImage;
		return;
		}
	
	public VmiGuageDecimal( int relX, int relY, int width, int hgt,
			Float startValue, Float valMax, Float valMin, 
			VImageGradient colors, VImage borderImage )
			{
			this( relX, relY, width, hgt, startValue.doubleValue(), 
				valMax.doubleValue(), valMin.doubleValue(), 
				colors, borderImage );
			return;
			}

	// add constructor that has min/max/cur value.
	
	public void paint( VImage target, Integer state )
		{
		if( super.isVisible() == false ) { return; }
		super.setExtendX( this.myWidth, false);
		super.setExtendY( this.myHgt, false);
		super.enableText(false);
//			super.paint(target, state );

		int padding = super.getFrameThicknessPx();
		int x1 = super.getX().intValue();
		int y1 = super.getY().intValue();



			// Find minor axis.
		int barThickness = this.myHgt - (padding*2);
		if( this.orientation == false )
			{ barThickness = this.myWidth - (padding*2); }

		int fillX = barThickness;
		int fillY = barThickness;
		int baseX = x1+padding;
		int baseY = y1+padding;
		Double widthAdj = new Double( this.myWidth - (2*padding) );
		Double hgtAdj = new Double( this.myHgt - (2*padding) );

			// HORIZONTAL
		if( this.orientation == true )
			{
			// 	Draw filler
//				fillX = new Double( fill * widthAdj + 1.0d ).intValue();
			fillX = new Double(widthAdj).intValue() + 1;
			}
		else			// VERTICAL
			{
//				fillY = new Double((-1.0d * fill * hgtAdj) - 1.0d ).intValue();
			fillY = new Double( (hgtAdj * -1.0d) - 1.0d).intValue();
			baseY = y1+this.myHgt-1;
			}

			// skip the heart completely if value is below the scale.
		if( this.currentValue > this.minValue )
			{
			target.scaleblit( baseX, baseY, fillX, fillY, 
				this.guageHeart );
			}

		// BORDER - in either form.
		if( super.isBorderEnabled() == true )
			{
			if( this.guageShell == null )
				{
				target.rect( x1, y1+1, x1+this.myWidth-1, y1+this.myHgt-2, 
					Color.BLACK );
				target.rect( x1, y1, x1+this.myWidth-1, y1+this.myHgt-1, 
					Color.WHITE );
				}
			else
				{
				int x2 = x1+this.myWidth-1;
				int y2 = y1+this.myHgt;
				for( int bx = 0; bx < this.getBorderObscure(); bx++ )
					{		// Clip the edges if so desired.
				// This seemed to be doing more harm then good. disabled it
	//					target.rect(x1+bx, y1+bx,
	//						x2-bx, y2-bx,  Color.TRANSLUCENT );
					}
				target.scaleblit( x1, y1, x2-x1, y2-y1,	this.guageShell );
	//				target.blit( x1+50, y1+50, this.guageShell );
				}
			}		// END border

//			System.out.println("DEBUG :: "+ Integer.toString(fillX)+ " / " + 
//					Integer.toString(fillY)+ "  :: " + 
//					Integer.toString(this.myWidth)+ " / " + 
//					Integer.toString( this.myHgt)		);
		
		return;
		}

	/** Justifies values and sets image-to-value 
	* use whenever a current/max/min value changes.
	* This assumes that current/max/min values are set to desired values.
	*/
	private void recalculate()
		{
		if( this.currentValue > this.maxValue )
			{ this.currentValue = this.maxValue; }
		if( this.currentValue < this.minValue )
			{ this.currentValue = this.minValue; }

		if( this.maxValue == this.minValue )
			{
			this.currentValue = this.minValue;
			this.capacity = 1.0d;
			}
		else
			{
			this.capacity =  ( this.currentValue - this.minValue) /
				(this.maxValue - this.minValue);
			}
		if( this.capacity < 0.0d )	{ this.capacity = 0.0d; }
		if( this.capacity > 1.0d )	{ this.capacity = 1.0d; }

			// This adjusts the guage image to match the value.
		this.guageHeart = new VImage( this.gradientBody.getWidth(),
			this.gradientBody.getHeight(), 
			core.Script.Color_DEATH_MAGENTA );
		this.guageHeart.blit(0, 0,  VImage.truncateX(this.gradientBody, 
			this.capacity ) );

		if( this.imageBorder != null )
			{ this.guageShell = new VImage( this.imageBorder ); }
 
		// Rotate core vertically if needed.
		if( this.orientation == false )
			{ 
			this.guageHeart.rotateBlend( 
				new Double(Math.PI / 2.0d).floatValue(), 1.0f );
			
			// Need to rotate the shell as well.
			if( this.imageBorder != null )
				{		
				this.guageShell = VImage.rotateDegreesIntoNewImage(
					this.imageBorder, 90.000d);
				}
			}

		return;
		}

	/* overloaded for convieniance */
	
	public void setValue( Double x )
		{
		this.currentValue = x;
		this.recalculate();
		return; 
		}
	/**   Fills the guage to a given percentage.
	 * 
	 * @param pct   percentage in form 0.0d to 1.0d
	 */
	public void setToPercentage( Double pct )
		{
		if( this.maxValue == this.minValue ) { return; }
		if( pct > 1.0d )   { pct = 1.0d; }
		if( pct < 0.0d )   { pct = 0.0d; }
		this.currentValue = (this.maxValue - this.minValue) * pct;
		this.recalculate();
		return;
		}
	public void setToMax()
		{ 
		this.currentValue = this.maxValue;
		this.recalculate();
		return;
		}
	public void setToMin()
		{ 
		this.currentValue = this.minValue;
		this.recalculate();
		return;
		}
	public void setValue( Float x )
		{ 
		this.currentValue = x.doubleValue();
		this.recalculate();
		return; 
		}
	public void setValueRelative( Double x , boolean wrap )
		{
		this.currentValue += x;
		if( this.maxValue == this.minValue )	{ return; }
		if( wrap == true )
			{
			while( this.currentValue > this.maxValue )
				{  this.currentValue -= (this.maxValue - this.minValue); }
			while( this.currentValue < this.minValue )
				{  this.currentValue += (this.maxValue - this.minValue); }
			}
		else
			{
			if( this.currentValue > this.maxValue )
				{  this.currentValue = this.maxValue; }
			if( this.currentValue < this.minValue )
				{  this.currentValue = this.minValue; }
			}
		this.recalculate();
		return; 
		}
	public void setValueRelative( Float x , boolean wrap )
		{	this.setValueRelative( x.doubleValue() , wrap );	}
	public void setValueRelativePercent( Double pct , boolean wrap )
		{
		if( this.maxValue == this.minValue ) 
			{ return; }
		if( pct == 0.0d )
			{ return; }
		if( pct <= -1.0d ) 
			{
			this.setToMin(); 
			return;
			}
		if( pct >= 1.0d )     
			{
			this.setToMax();
			return;
			}
		Double adj = (this.maxValue - this.minValue) * pct;
		this.setValueRelative( adj , wrap );
		return;
		}
	public void setValue( double x )
		{ 
		this.currentValue = new Double(x);
		this.recalculate();
		return; 
		}
	public void setValue( float x )
		{ 
		this.currentValue = new Double(x);
		this.recalculate();
		return; 
		}

	public void setValueMax( Double x )
		{ 
		this.maxValue = x; 
		this.recalculate();
		return;
		}
	public void setValueMin( Double x )
		{ 
		this.minValue = x;
		this.recalculate();
		return; 
		}
	public Double getValue()
		{ return(this.currentValue); }
	public Double getValueMin()
		{ return(this.minValue); }
	public Double getValueMax()
		{ return(this.maxValue); }

	// Characteristic of this class... cannot change width or height
	//   once the object is constructed.
	public void setExtendX( int desiredWidth, boolean onlyIfGreater )
		{ return; }
	public void setExtendY( int desiredWidth, boolean onlyIfGreater )
		{ return; }

	// Text is disabled in this item.  So also disable this function.
	//     as it may interfere with drawing the guage.
	protected void calcTextArea()
		{	return; 	}

	// These get less complicted b/c external extention is dis-allowed.
	public Double getDX()
		{	return( new Double(this.myWidth) );	}
	public Double getDY()
		{	return( new Double(this.myHgt) );	}
	
	// Guages are typically only for show or are controlled by
	//  separate buttons inside a Vmenu.   So they are inactive.
	public boolean isActive()
		{ return( false );  }
	
	public void setImageContent( HashMap<Integer,VImage> imageItems )
		{
		if( imageItems == null )	{ return; }
		if( imageItems.size() == 1 && imageItems.get(0) != null ) 
			{ 
			this.gradientBody = imageItems.get(0);
			}
		if( imageItems.size() == 2 && imageItems.get(1) != null )
			{
			if( imageItems.get(0) != null ) 
				{ this.gradientBody = imageItems.get(0); }
			this.imageBorder = imageItems.get(1);
			this.setBorderObscure(1);
			}
		this.recalculate();
		return;
		}
	
	/** Hardcoded default - somewhat fancy - gradient */
	private static VImageGradient defaultGradient()
		{
		HashMap<Double,Color> hmGradHorz = 
			new HashMap<Double,Color>();
		hmGradHorz.put( 0.00d, new Color( 0.50f, 0.0f, 0.0f, 1.0f) );
		hmGradHorz.put( 0.20d, new Color( 0.90f, 0.0f, 0.0f, 1.0f) );
		hmGradHorz.put( 0.40d, new Color( 0.86f, 0.25f, 0.0f, 1.0f) );
		hmGradHorz.put( 0.60d, new Color( 0.76f, 0.50f, 0.0f, 1.0f) );
		hmGradHorz.put( 0.80d, new Color( 0.66f, 0.66f, 0.66f, 1.0f)  );
		hmGradHorz.put( 1.00d, new Color( 0.70f, 0.70f, 0.70f, 1.0f)  );

		HashMap<Double,Color> hmGradVert = 
			new HashMap<Double,Color>();
		hmGradVert.put( 0.00d, new Color( 0.00f, 0.00f, 0.00f, 1.0f)   );
		hmGradVert.put( 0.10d, new Color( 0.00f, 0.00f, 0.00f, 1.0f) );
		hmGradVert.put( 0.20d, new Color( 0.20f, 0.20f, 0.20f, 1.0f) );
		hmGradVert.put( 0.50d, new Color( 0.40f, 0.40f, 0.40f, 1.0f) );
		hmGradVert.put( 0.80d, new Color( 0.20f, 0.20f, 0.20f, 1.0f) );
		hmGradVert.put( 0.90d, new Color( 0.00f, 0.00f, 0.00f, 1.0f) );
		hmGradVert.put( 1.00d, new Color( 0.00f, 0.00f, 0.00f, 1.0f) );

		return( new VImageGradient( 300, hmGradVert, hmGradHorz ) );
		}

	/**  When image borders are set, this is the width of pixels to 
	 *    clear around the edges to make for a smooth look.
	 * @return The current integer setting
	 */
	public int getBorderObscure()
		{	return borderObscure;	}

	/**  When image borders are set, this is the width of pixels to 
	 *    clear around the edges to make for a smooth look.
	  * @param borderObscure  number of pixels to clear around the edge
	  */
	public void setBorderObscure(int borderObscure)
		{	this.borderObscure = borderObscure;	}
	
	/** Resets the colors used to draw the heart (bar) of the guage.
	*  this uses the same color for the core and edge.
	*    
	 * @param barColor   the color.
	 */
	public void setBarSolidColor( Color barColor )
		{
		this.setTwoColorGradient( barColor,  barColor );
		this.recalculate();
		return;
		}

	}


