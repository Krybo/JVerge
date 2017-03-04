package core;

/* core.JVCL
 * Java Verge C Layers 
 * Emulation of Verge 1's "VC layers"
 * 
 * Provides a persistant, resizable stack of graphics drawing 
 * layers and functions.   They are the last layer drawn to screen
 * On top of all the map layers
 */



import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.Font.*;
import domain.VImage;

import java.util.ArrayList;

import menus.Vmenu;
import menus.Vmenuitem;
//import static core.Script.currentLucent;
//import java.awt.image.ImageObserver;

public class JVCL
	{

	void destroy()
		{
		this.dialogBoxes.clear();
		this.vcl.clear();
		this.requiresUpdate = false;
		this.currentLayer = -1;
		}

	private class VCLayer extends VImage
		{
		private boolean visible;
		private boolean active;
		private int layerWidth;
		private int layerHeigth;
		// Each layer carries a sets of flags that may be used
		// by the programmer to track anything going on for that layer.
		private int layernum = -1;
		private final int flagArraySize = 100;
		private boolean[] boolFlags = new boolean[flagArraySize]; 
		private int[] intFlags = new int[flagArraySize];
		private double[] decFlags = new double[flagArraySize];
		
		private Vmenu vm = null; 
		
		private VCLayer(int  sizeX, int sizeY )
			{
			super(sizeX,sizeY);
			this.layerWidth = sizeX;
			this. layerHeigth = sizeY;
			this.layernum = -1;
			this.visible = false;
			this.active = false;
			this.clearAllFlags();
			this.warningSuppressor();
			}
		
		public boolean getActive()
			{  return(active);  }
		public boolean getVisible()
			{  return(visible);  }
		public void setActive(boolean truefalse)
			{  this.active = truefalse;   }
		public void setVisible(boolean truefalse)
			{
			this.visible = truefalse;   
			}
		
		private int flagBound(int n)
			{
			if( n < 0 )  { return 0; }
			if( n >= this.flagArraySize ) { return(flagArraySize - 1); }
			return(0);
			}

		public int getNum()	{ return(this.layernum); }
		public void setNum(int id)	{ this.layernum = id; }

		public boolean getBoolFlag(int flagNum)
			{
			flagNum = flagBound(flagNum);
			return( this.boolFlags[flagNum] );
			}
		public void setBoolFlag(int flagNum, boolean val )
			{
			flagNum = flagBound(flagNum);
			this.boolFlags[flagNum] = val;
			}
		public int getIntFlag(int flagNum)
			{
			flagNum = flagBound(flagNum);
			return( this.intFlags[flagNum] );
			}
		public void setIntFlag(int flagNum, int val )
			{
			flagNum = flagBound(flagNum);
			this.intFlags[flagNum] = val;
			}
		public double getDecFlag(int flagNum)
			{
			flagNum = flagBound(flagNum);
			return( this.decFlags[flagNum] );
			}
		public void setDecFlag(int flagNum, double val )
			{
			flagNum = flagBound(flagNum);
			this.decFlags[flagNum] = val;
			}

		public void clearAllFlags()
			{
			for( int n = 0; n < flagArraySize; n++ )
				{
				this.boolFlags[n] = false;
				this.intFlags[n] = 0;
				this.decFlags[n] = 0.0f;
				}
			return;
			}
		private void warningSuppressor()
			{
			setIntFlag( 0, getIntFlag(0) );
			setDecFlag( 0, getDecFlag(0) );
			setBoolFlag( 0, getBoolFlag(0) );
			return;
			}

			// Applies rotation to the entire layer
		public void rotate(float radians)
			{	super.rotateBlend( radians, 1.0f); }
					// apply an alpha blend
		public void alphaBlend( float blendFactor )
			{	super.rotateBlend( 0.0f, blendFactor );	}
			// Does both
		public void rotateBlend( float radians, float blendFactor )
			{	super.rotateBlend( radians, blendFactor );	}
		
		public void clear()
			{
			Graphics2D g2 = (Graphics2D) this.getImage().getGraphics();
			g2.setComposite(AlphaComposite.Clear );
			g2.fillRect(0, 0, this.layerWidth, this.layerHeigth );
			g2.dispose();
			return;
			}

		}

	// Holds information for tracking floating entity-following dialogs
	// Designed to only set the message, position and box characteristics.
	//     and be independant of what is going on with the Verge Map
	// Moving entities and players are handled externally, and this
	//     will draw the boxes using functions in JVCL.
	// Original plan was to use the flags in the VCLayers... --
	//      but a subclass is probably the better route.
	private class DialogTracker
		{
		private int mapEntityNumber;
		private boolean isOnScreen;
		private long fadeTimePercent;
		private long fadeTimeDuration;
		private long selfDestructTime;
		private int screenX;	// current position of the box on the screen
		private int screenY;
		private String theText;
		private Font fnt;
		private Color textColor;
		private Color outlineColor;
		private int paddingPx;
		private VImage imgBackground;
		private float alphaBackground;
		private float fadeAlphaBackground;
		private int resX, resY;

		public DialogTracker( int entityNum, String message, int durationMsec, 
				Font fnt, Color textColor, Color outlineColor, int boxWidth,
				VImage imgBackground, float alphaBackground, int resX, int resY )
			{
			this.mapEntityNumber = entityNum;
			this.screenX = -1;    this.screenY = -1;
			this.theText = message;
			this.isOnScreen = false;
			this.resX = resX;
			this.resY = resY;
			// Cast to long is important or integer overflow will occur. here
			long lifespan = ( (long) durationMsec) * 1000000;
			
			this.fnt = fnt;
			this.textColor = textColor;
			this.outlineColor = outlineColor;
			this.paddingPx = boxWidth;
			this.imgBackground = imgBackground;
			this.alphaBackground = alphaBackground;
			this.fadeAlphaBackground = alphaBackground;
			
			// Fade time in ns is 1/10th of total duration (at the end)
			this.selfDestructTime = lifespan + System.nanoTime();
			this.fadeTimePercent = durationMsec * 9 / 10;
			this.fadeTimeDuration = durationMsec * 100000;
			}
		
		public int getEntityNumber()
			{	return this.mapEntityNumber;	}
			
		public boolean hasExpired()
			{ 
			if( System.nanoTime() >= this.selfDestructTime )
				{ System.out.println("Dialog expired!"); return true; }
			return false;
			}
		
		public void calculateFadeAlpha()
			{
			long timeNow = System.nanoTime();
//			long timeRemaining = this.selfDestructTime - System.nanoTime();
//			System.out.println( "Time: "+Long.toString(timeNow)+"  Remaining : "+Long.toString(timeRemaining) );
			long fadeStart = this.selfDestructTime - this.fadeTimeDuration;
				// Fade has not yet begun, or textbox has expired.
			if( timeNow < fadeStart )  
				{ this.fadeAlphaBackground = 1.0f; return; }
			if( timeNow > this.selfDestructTime )  
				{ this.fadeAlphaBackground = 0.0f;  return; }
			
			float fadeFactor = 1.0f - ( (float) (timeNow - fadeStart ) / (float) this.fadeTimeDuration );
			System.out.println( "Fade Factor: "+Float.toString(fadeFactor) );
			this.fadeAlphaBackground = (float) this.alphaBackground * fadeFactor;
			if( this.fadeAlphaBackground > 1.0f )
				{ this.fadeAlphaBackground = 1.0f; }
			if( this.fadeAlphaBackground < 0.0f )
				{ this.fadeAlphaBackground = 0.0f; }
			return;
			}
		
		public int getX()
			{	return this.screenX; }
		public int getY()
			{	return this.screenY; }
		public String getText()
			{ return this.theText; }
		public Font getFont()
			{ return this.fnt; }
		public Color getTextColor()
			{ return this.textColor; }
		public Color getOutlineColor()
			{ return this.outlineColor; }
		public int getPaddingPx()
			{ return this.paddingPx; }
		public VImage getImgBackground()
			{ return this.imgBackground; }
		public BufferedImage getBufferedImage()
			{ return this.imgBackground.getImage(); }
		public float getAlphaBackground()
			{ return this.alphaBackground; }
		public float getAlphaBackgroundWithFade()
			{ 
			this.calculateFadeAlpha();
			return (float) this.fadeAlphaBackground; 
			}
		public long getFadeTimeDurationInNanoSeconds()
			{ return this.fadeTimePercent; }

		public void updatePositions( int playerX, int playerY, 
				int entityX, int entityY, byte zoomLevel )
			{
			int distX = entityX - playerX;
			int distY = entityY - playerY;
			int Xmargin = this.resX * 4 / 5;
			int Ymargin = this.resY * 4 / 5;
			int halfWidth = Math.floorDiv(this.resX, 2);
			int halfHeight = Math.floorDiv(this.resY, 2);
			int zoomedHalfWidth = Math.floorDiv(this.resX, zoomLevel * 2);
			int zoomedHalfHeight = Math.floorDiv(this.resY, zoomLevel * 2);
			
			if( Math.abs(distX) > halfWidth )
				{
				this.isOnScreen = false;
				return;
				}
			if( Math.abs(distY) > halfHeight )
				{
				this.isOnScreen = false;
				return;
				}

			this.isOnScreen = true;

			// Adjustments need made when player is close ot edges
			int mapScrollX = zoomedHalfWidth - playerX;
			int mapScrollY = zoomedHalfHeight - playerY;
			if( mapScrollX < 0 ) { mapScrollX = 0; }
			if( mapScrollY < 0 ) { mapScrollY = 0; }
			
//			this.screenX = screenWidth / 2 + distX;
//			this.screenY = screenHeight / 2 + distY;
//   	The +16 is an adjustment made so part of the box is not
//		directly over-top the character.
			this.screenX = (entityX - mapScrollX) * ((int) zoomLevel) + 16;
			this.screenY = (entityY - mapScrollY ) * ( (int) zoomLevel )  + 16;
			
			this.screenX = halfWidth  - (mapScrollX * (int)zoomLevel)  + ( (int) zoomLevel * distX);
			this.screenY = halfHeight  - (mapScrollY * (int)zoomLevel)  + ( (int) zoomLevel * distY);

			// MARGINS
			// There is a condition where boxes will degenerate if they
			//    are put too close ot the right & bottom edge of the screen.
			//     Protect vs. this by making a bound at 20% to right margin
			//  consider margin violations to be "off the screen"
			if( this.screenX > Xmargin )
				{	this.isOnScreen = false;	}
			if( this.screenY > Ymargin )
				{	this.isOnScreen = false;	}

			return;
			}

		public boolean isOnScreen()
			{	return isOnScreen;	}
		
		}

	
	// Adds a student to the student array list.
	private ArrayList<VCLayer> vcl = new ArrayList<VCLayer>();
	private ArrayList<DialogTracker> dialogBoxes = new ArrayList<DialogTracker>();
	private VCLayer JVCLdialoglayer;
	private int JVCLdialogZpos;
	private int currentLayer;
	private int standardX, standardY;
	private Font nativefont = new Font("Tahoma",PLAIN, 18);
	private boolean requiresUpdate = false;
	private float masterRotation = 0.0f;

	public JVCL(int numLayers, int xRes, int yRes )
		{
		this.standardX = xRes;
		this.standardY = yRes;
		if( this.standardX < 320 ) { this.standardX = 320; }
		if( this.standardY < 280 ) { this.standardY = 280; }
		
		// This is a base buffer layer, all must have it.
		this.vcl.add( new VCLayer(standardX,standardY) );
		this.vcl.get(0).setNum(0);
		this.vcl.get(0).setActive(true);
		
		for( int a = 0; a < numLayers; a++)
			{
			this.vcl.add( new VCLayer(standardX,standardY) );
			this.vcl.get(a+1).setNum(a+1);
			this.vcl.get(a+1).setActive(true);
			}
		
		this.JVCLdialoglayer = new VCLayer(standardX,standardY);

		this.JVCclearAllLayers();
		this.setJVCLdialogZpos(0);
		this.refresh();
		currentLayer = 1;
		}
	
	public int addLayer()
		{
		VCLayer L = new VCLayer(this.standardX,this.standardY); 
		this.vcl.add(L);
		L.setNum(this.vcl.size()-1);
		return( this.vcl.size() );
		}	
	public int dropLayer()
		{
		this.vcl.remove( this.vcl.size() - 1 );
		return(this.vcl.size() - 1);
		}

	/**  Returns the number of usable layers - counts all but layer 0
	 *   and other internal layers.
	 * @return	int number of usable layers.
	 */
	public int getLayerCount()
		{ return(this.vcl.size() - 1); }
	
	public boolean setWriteLayer( int layerNum )
		{
		if( layerNum <= 0 ) { return(false); }
		if( layerNum >= this.vcl.size() )  { return(false); }
		if( this.vcl.get(layerNum).getActive() == false )  
			{ return(false); }		// Layer is avbl, but is "write protected"
		this.currentLayer = layerNum;
		return(true);
		}
	/**
	 * Changes to a layer # and fully activates it - unlike setWriteLayer that
	 *    will switch layers but NOT activate it.
	 * @param layerNum	The target layer number to switch to
	 * @return  boolean - true if layer exists and switch successful.
	 */
	public boolean setWriteLayerAndEnable( int layerNum )
		{
		if( layerNum <= 0 ) { return(false); }
		if( layerNum >= this.vcl.size() )  { return(false); }
		this.vcl.get(layerNum).active = true;
		this.vcl.get(layerNum).visible = true;
		this.currentLayer = layerNum;
		return(true);
		}
	
		// Set layer write to layer X & turn it ON
		//   then also switches all the other layers OFF
	public boolean setWriteLayerExclusive( int layerNum )
		{
		if( layerNum <= 0 ) { return(false); }
		if( layerNum >= this.vcl.size() )  { return(false); }
		this.vcl.get(layerNum).setActive(true);
		this.vcl.get(layerNum).setVisible(true);
		this.currentLayer = layerNum;
		for( int ln = 1; ln < this.vcl.size(); ln++ )
			{
			if( ln == layerNum )  { continue; }
			this.vcl.get(ln).setActive(false);
			this.vcl.get(ln).setVisible(false);			
			}
		return(true);
		}
	
	public int getWriteLayer()
		{  return( this.currentLayer );  }
	
	public boolean setLayerVisible(int layerNumber ) 
		{
		if( layerNumber <= 0 ) { return(false); }
		if( layerNumber >= this.vcl.size()  )  { return(false); }
		this.vcl.get(layerNumber).setVisible(true);
		return(true);
		}
	public boolean setLayerInvisible(int layerNumber ) 
		{
		if( layerNumber <= 0 ) { return(false); }
		if( layerNumber >= this.vcl.size()  )  { return(false); }
		this.vcl.get(layerNumber).setVisible(false);
		return(true);
		}
	
	public void setLayerVisibility( boolean truefalse )
		{	this.vcl.get(currentLayer).setVisible(truefalse);	}
	
	/** Attempts to directly scale any given Vimage directly to a layer.
	 *   does not affect layer settings, and will not work if layer not writtable
	 *   Any previous drawing on the target layer will be destroyed.
	 * @param img	A VImage object
	 * @param layerNum		the target stack layer number
	 */
	public void setFullLayerImage(VImage img, int layerNum )
		{
		if( layerNum <= 0 ) 					{ return;  }
		if( layerNum >= this.vcl.size()  )  	{ return; }
		if( ! this.vcl.get(layerNum).active )	{ return; }
		this.vcl.get(layerNum).clear();
		this.vcl.get(layerNum).scaleblit(  0, 0, 
				this.vcl.get(layerNum).width, 
				this.vcl.get(layerNum).height, img );
		return;
		}

	public boolean toggleLayerVisible(int layerNumber ) 
		{
		if( layerNumber <= 0 ) { return(false); }
		if( layerNumber >= this.vcl.size()  )  { return(false); }
		this.refresh();
		if( this.vcl.get(layerNumber).getVisible() == true )
			{
			this.vcl.get(layerNumber).setVisible(false);
			return(false);
			}
		else
			{
			this.vcl.get(layerNumber).setVisible(true);
			return(true);
			}
		}
	
	public boolean toggleLayerActive( int layerNumber ) 
		{
		if( layerNumber <= 0 ) { return(false); }
		if( layerNumber >= this.vcl.size()  )  { return(false); }
		this.refresh();
		if( this.vcl.get(layerNumber).getActive() == true )
			{
			this.vcl.get(layerNumber).setActive(false);
			return(false);
			}
		else
			{
			this.vcl.get(layerNumber).setActive(true);
			return(true);
			}
		}
	
	public boolean getLayerVisibility(int layerNumber)
		{	return( this.vcl.get(layerNumber).getVisible() );	}
	
	public void setLayerVisibilityAll(boolean truefalse ) 
		{
		for( int ln = 1; ln < this.vcl.size(); ln++ )
			{	this.vcl.get(ln).setVisible(truefalse);	}
		}

	public void setLayerActiveAll(boolean truefalse ) 
		{
		for( int ln = 1; ln < this.vcl.size(); ln++ )
			{	this.vcl.get(ln).setActive(truefalse);	}
		}
	
		// This sets the rotation (if any) that is applied after flatten() is done.
	public float getMasterRotation()
		{	return masterRotation;	}
	public void setMasterRotation(float masterRotationValue )
		{  
		this.masterRotation = masterRotationValue;
		this.requiresUpdate = true;
		return;
		}
	public void adjustMasterRotation(float masterRotationAdjustment )
		{ 
		this.masterRotation += masterRotationAdjustment;
		while( this.masterRotation >= Math.PI*2 ) 
			{ this.masterRotation -= Math.PI*2; }
		while( this.masterRotation < Math.PI * -2 ) 
			{ this.masterRotation += Math.PI*2; }
		this.requiresUpdate = true;
		return;
		}

	public void refresh()
		{ 
		this.requiresUpdate = true;
		this.flattenLayers();
		return;
		}
	
	private void flattenLayers()
		{
		if( this.requiresUpdate == false ) 
			{ return; }
		
			// no sense doing anything if all layers are turned off.
		if( getNumVisibleLayers() == 0 )
			{ return; }

		Graphics2D g2 = (Graphics2D) vcl.get(0).getImage().getGraphics();
		
		try 
			{
				// Start with a clean slate ( all work done on protected layer # 0)
			g2.setComposite(AlphaComposite.Clear );
			g2.fillRect(0, 0, this.standardX, this.standardY );
				// Now set to overlay

				// And pound down all layers that have drawing turned on.
				// Sandwich from the top > down, using the right composite mode
			g2.setComposite(AlphaComposite.SrcOver);

			for( int ln = 1; ln < this.vcl.size(); ln++ )
				{
				if( vcl.get( ln ) == null )   { continue; }
				if( this.vcl.get(ln).getVisible() == false ) { continue; }

				g2.drawImage( vcl.get( ln ).getImage(), null, 0, 0 );
				}

			} 
		catch(Exception e) { e.printStackTrace(); }
		finally  { g2.dispose(); }
		
		if( masterRotation != 0.0f )
			{
			vcl.get(0).rotate(masterRotation);
			
			}

		requiresUpdate = false;
		return;
		}
	
	public VImage getVImage()
		{
		this.flattenLayers();
		return( this.vcl.get(0) );
		}

	public VImage getDialogVImage()
		{	return( this.JVCLdialoglayer );	}

	public BufferedImage getBufferedImage() 
		{
		this.flattenLayers();
		return( this.vcl.get(0).getImage() );
		}

	public BufferedImage getDialogBufferedImage() 
		{  return( this.JVCLdialoglayer.getImage() );	}

	public int getNumVisibleLayers()
		{
		int count = 0;
		for( int ln = 1; ln < this.vcl.size(); ln++ )
			{
			if( this.vcl.get(ln).getVisible() == true ) { count++; }
			}
		return(count);
		}
	
		// Creates a new BufferedImage from any two VCLayers.
		// returns null if anything went wrong, be sure to check.
	public BufferedImage joinLayersAsBufferedImage( 
		    int srcLayer1, int srcLayer2 ) 
		{
		if( srcLayer1 <= 0 || srcLayer2 <= 0 )   
				{ return(null); }
		if( srcLayer1 > vcl.size() || srcLayer2 > vcl.size() )  
				{ return(null); }
		if( vcl.get(srcLayer1) == null  || vcl.get(srcLayer2) == null )
				{ return(null); }
		
		BufferedImage finalImage = new BufferedImage(standardX, standardY,
				this.vcl.get(0).getImage().getType() );
		Graphics2D g2 = (Graphics2D) finalImage.createGraphics();
		g2.drawImage( vcl.get(srcLayer1).getImage(), null, 0, 0 );
		g2.drawImage( vcl.get(srcLayer2).getImage(), null, 0, 0 );
		g2.dispose();
	    return(finalImage);
		}

	//  ===========   DRAWING FUNCTIONS ============
	
	private boolean JVCstring( VImage destLayer, int x, int y, String s, Font fnt, Color c )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return(false); }
		if( x < 0 || y < 0 ) { return(false); }
		
		Graphics2D g2 = (Graphics2D) destLayer.getImage().getGraphics();
		g2.setComposite(AlphaComposite.Src );
		g2.setFont( fnt );
		
		int checkY = g2.getFontMetrics().getHeight();
		int checkX = g2.getFontMetrics().stringWidth(s);
		
		if( x+checkX > standardX )   
			{
			g2.dispose();
			return(false);
			}
		if( y+checkY > standardY )   
			{
			g2.dispose();
			return(false);
			}

		y += Math.floorDiv(checkY, 2); 
		
		g2.setColor( c );
		g2.setComposite(AlphaComposite.Src );
		g2.drawString( s, x, y );
		g2.dispose();
		 	// 	anything that writes to layers needs to set this ~ if successful
		this.requiresUpdate = true; 
		return(true);
		}
	
	public boolean JVCstring(int x, int y, String s, Font fnt, Color c )
		{
		// Default send to active layer.
		return JVCstring( vcl.get( this.currentLayer), x, y, 
				s, fnt, c );
		}

		// A "dumb" overload form of the above.
	public boolean JVCstring(int x, int y, String s )
		{
		return JVCstring(x,y,s, this.nativefont, Color.white );
		}
	
	public void JVCoval( int cx, int cy, int xRad, int yRad, Color c, boolean fillErIn )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
				{ return; }
		if( cx < 0 || cy < 0 ) { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor( c );
		g2.setComposite(AlphaComposite.Src );
		if( fillErIn == true )
			{ g2.fillOval( cx-xRad, cy-yRad, xRad*2, yRad*2 ); }
		else
			{ g2.drawOval( cx-xRad, cy-yRad, xRad*2, yRad*2 ); }
		g2.dispose();
		this.requiresUpdate = true;
		return;
		}
	
	public void JVCcircle(int cx, int cy, int radius, Color c, boolean fillErIn )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
				{ return; }
		if( cx < 0 || cy < 0 ) { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor( c );
		g2.setComposite(AlphaComposite.Src );
		if( fillErIn == true )
			{ 	g2.fillOval( cx-radius, cy-radius, radius*2, radius*2 );  }
		else
			{	g2.drawOval( cx-radius, cy-radius, radius*2, radius*2 );  }			
		g2.dispose();
		this.requiresUpdate = true;
		return;
		}

			// Simple solid Blit solid images
	public void JVCblitImage(int x, int y, VImage img )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
				{ return; }
		vcl.get(this.currentLayer).blit(x, y, img);
		return;
		}

		// Blits an image onto current layer, giving control of the alpha blending
	public void JVCblitImage(int x, int y, BufferedImage img, AlphaComposite mode )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x < 0 || y < 0 )   { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setComposite(mode);
		g2.drawImage( img, x, y, Color.BLACK, null);
		return;
		}

	public void JVCblitImage(int x, int y, BufferedImage img )
		{ JVCblitImage( x, y, img, AlphaComposite.Src );  }

	public void JVCblitBlendImage(int x, int y, BufferedImage img, float blendValue )
		{ 
		JVCblitImage( x, y, img, AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, blendValue )  );  
		}
	
	public void JVCblitBlendImage(int x, int y, VImage img, float blendValue )
		{ JVCblitBlendImage(  x,  y, img.getImage(),  blendValue ); }

	public void JVCblitFullscreenImage( BufferedImage img, AlphaComposite mode )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		double srcW = img.getWidth();
		double srcH = img.getHeight();
		double factorX = (double) this.standardX / srcW;
		double factorY = (double) this.standardY / srcH;

		Graphics2D g2d = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2d.setComposite(mode);
		g2d.drawImage( img, 0, 0, Color.BLACK, null);		
		AffineTransform at = new AffineTransform();
		at.scale( factorX,  factorY );
		g2d.drawImage( img, at, null );
		g2d.dispose();
		
		return;
		}

	public void JVCblitFullscreenImage( VImage img, AlphaComposite mode )
		{ JVCblitFullscreenImage(  img.getImage(), mode ); return; }

		// Scales and blits an image of arbituary size over the entire screen.   
		//   While also applying a translucency factor (0-1.0f)
	public void JVCblitBlendFullscreenImage( VImage img, float alphaValue )
		{ 
		JVCblitFullscreenImage( img, AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, alphaValue )  );
		return;
		}
	
	public void JVCblitBlendFullscreenImage( BufferedImage img, float alphaValue )
		{  
		JVCblitFullscreenImage( img, AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, alphaValue )  );
		return; 
		}

	/**	(krybo feb.2016)
	 * Blits a single Vmenuitem object onto the current VC layer.
	 * @param vmi	the implemented Vmenuitems to draw
	 */
	public void JVCmenuPaintItems( Vmenuitem vmi )
		{
		if( ! vcl.get(this.currentLayer).active )	{ return; }
		vmi.paint( vcl.get(this.currentLayer) );
		this.requiresUpdate = true;
		return;
		}
	
	/**	
	 * Blits multiple Vmenuitem objects onto the current VC layer.
	 * (krybo feb.2016)
	 * @param vmi	ArrayList of Vmenuitems
	 */
	public void JVCmenuPaintItems( ArrayList<Vmenuitem> vmi )
		{
		if( ! vcl.get(this.currentLayer).active )	{ return; }
		for( Vmenuitem mi : vmi )
			{	mi.paint( vcl.get(this.currentLayer) );	   }
		this.requiresUpdate = true;
		return;
		}
	/**
	 * Blits a external Vmenu object onto the current VC layer.
	 * @param vm	Fully built Jmenu object.
	 */
	public void JVCmenuPaint( Vmenu vm )
		{
		if( ! vcl.get(this.currentLayer).active )	{ return; }
		vm.paint( vcl.get(this.currentLayer) );
		this.requiresUpdate = true;
		return;
		}

	/**  Draws the Vmenu object attached to a given layer --
	 * onto the given layer.   Does nothing if there is no attachment.
	 * Usually handled by VmenuManager 
	 * 
	 * @param vcLayerNum	The VClayer number
	 */
	public void JVCmenuPaintAttached( int vcLayerNum )
		{
		if( ! this.vcl.get( vcLayerNum).active )	
			{ return; }
		if( this.vcl.get( vcLayerNum ).vm == null )
			{ return; }
		this.vcl.get( vcLayerNum ).vm.paint( 
			this.vcl.get( vcLayerNum ) );
		this.requiresUpdate = true;
		return;
		}

	/** Run Vmenu animation routine (once) that is 
	 * attached to a specified layer.  Usually handled by VmenuManager
	 * @param vcLayerNum	The VClayer number
	 * */
	public void JVCmenuAnimateAttached( int vcLayerNum )
		{
		if( ! this.vcl.get( vcLayerNum).active )	
			{ return; }
		if( this.vcl.get( vcLayerNum ).vm == null )
			{ return; }
		this.vcl.get( vcLayerNum ).vm.animate( 
			this.vcl.get( vcLayerNum ) );
		this.requiresUpdate = true;
		return;
		}

	/**
	 * Draws the attached menu
	 * If there is no menu attached, it returns having done nothing.
	 */
	public void JVCmenuPaint( )
		{
		if( ! vcl.get(this.currentLayer).active )	{ return; }
		if( vcl.get( this.currentLayer ).vm == null ) 
			{ return; }
		vcl.get( this.currentLayer ).vm.paint( 
			vcl.get(this.currentLayer) );
		this.requiresUpdate = true;
		return;
		}

	/**
	 * Passes through all available layers and prints any menus 
	 *     that are in a visible state.
	 * @param reverseOrder	set true to Reverse the blit order.
	 * @return	Count of the menus actually drawn
	 */
	public Integer JVCmenuPaintAll( boolean reverseOrder )
		{
		Integer rtn = 0;
//		System.out.println(" *** menu paint all called.");
		int nMax = this.vcl.size();
		if( nMax <= 1 )  { return(0); }
		if( reverseOrder  == false )
			{
			for( int n = 1; n < nMax; n++ )
				{
				if( ! this.vcl.get(n).active )	
					{ continue; }
				if( this.vcl.get(n).vm == null )	
					{ continue; }
				this.vcl.get(n).vm.paint( vcl.get(n) );
				rtn++;
				}
			}
		else
			{
			for( int x = nMax-1; x > 1; x-- )
				{
				if( ! this.vcl.get(x).active )	
					{ continue; }
				if( this.vcl.get(x).vm == null )	
					{ continue; }
				this.vcl.get(x).vm.paint( vcl.get(x) );
				rtn++;
				}			
			}
		return(rtn);
		}
	
	
	/**
	 * Associates a Vmenu object to the currently selected VC layer.
	 *  - it can then be drawn on-demand with JVCmenuPaint()
	 * @param menus  The Fully built Vmenu object to attach. 
	 */
	public void JVCmenuAttach( Vmenu menus )
		{
		if( ! vcl.get( this.currentLayer ).active )	{ return; }
		this.vcl.get( this.currentLayer ).vm = menus;	  
		return; 
		}
	/**
	 * Attaches a Vmenu to a particular layer.
	 * @param menus		The constructed menu object
	 * @param layerNum		the layer
	 * @param enableLayer	true Will enable the target layer if needed
	 * @return	true on success, false if aborted
	 */
	public boolean JVCmenuAttachToLayer( Vmenu menus,
			int layerNum, boolean enableLayer )
		{
		if( layerNum <= 0 || layerNum > (this.vcl.size()-1) )
			{ return(false); }
		if( enableLayer == true )
			{
			this.vcl.get( layerNum ).setActive( true );
			this.vcl.get( layerNum ).setVisible( true );
			}
		if( ! this.vcl.get( layerNum ).active )	{ return(false); }		
		vcl.get( layerNum ).vm = menus;	
		return(true); 
		}
	/**
	 * Dissociates the attached menu object from the current layer.
	 * @return	Vmenu ref. that was removed.
	 */
	public Vmenu JVCmenuDetach( )
		{
		if( ! vcl.get( this.currentLayer ).active )	{ return(null); }
		Vmenu rtn = this.vcl.get( this.currentLayer ).vm;
		vcl.get( this.currentLayer ).vm = null;
		vcl.get( this.currentLayer ).clear();
		return(rtn);
		}

	/** Gets the menu focus id of the Vmenu object attached
	 *    to a specified VClayer.
	 * @param laternum	The target VClayer number
	 * @return	The menu id, else -1
	 */
	public Long JVCmenuGetMenuFocusID( int laternum )
		{
		if( this.vcl.get(laternum).vm == null )   
			{ return( new Long(-1) ); }
		return( this.vcl.get(laternum).vm.getFocusId() );
		}

	/** Gets the menu focus id of the Vmenu object attached
	 *    to the current layer.
	 * @return	The menu id, else -1
	 */
	public Long JVCmenuGetMenuFocusID( )
		{
		if( this.vcl.get( this.currentLayer ).vm == null )   
			{ return( new Long(-1) ); }
		return( this.vcl.get( this.currentLayer ).vm.getFocusId() );
		}

	public void JVCclear()
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setComposite(AlphaComposite.Clear );
		g2.fillRect(0, 0, this.standardX, this.standardY );
		g2.dispose();
		this.requiresUpdate = true;
		return;
		}
	// Clears active layer to a solid color, rather than transparent.
	public void JVCclear( Color floodColor )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		Graphics2D g2 = (Graphics2D) vcl.get(
				this.currentLayer).getImage().getGraphics();
		g2.setComposite(AlphaComposite.Clear );
		g2.fillRect(0, 0, this.standardX, this.standardY );

		g2.setColor(floodColor);
		g2.setComposite(AlphaComposite.Src );
		g2.fillRect( 0, 0, this.standardX, this.standardY );

		g2.dispose();
		this.requiresUpdate = true;
		return;
		}
	
		// Clears the dialog layer
	public void JVCclearDialog()
		{
		Graphics2D g2 = (Graphics2D) this.JVCLdialoglayer.getImage().getGraphics();
		g2.setComposite(AlphaComposite.Clear );
		g2.fillRect(0, 0, this.standardX, this.standardY );
		g2.dispose();
		return;
		}

	public void JVCclearAllLayers()
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }

		for( int a = 0; a < this.vcl.size(); a++ )
			{
			Graphics2D g2 = (Graphics2D) vcl.get(a).getImage().getGraphics();
			g2.setComposite(AlphaComposite.Clear );
			g2.setColor( new Color(0.0f, 0.0f, 0.0f, 0.0f ) );
			g2.fillRect(0, 0, this.standardX, this.standardY );
			g2.dispose();
			}
		this.requiresUpdate = true;
		return;
		}

	public void JVCrect( int x, int y, int w, int h, Color c )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x < 0 || y < 0 ) { return; }
		if( x+w > this.standardX )  { return; }
		if( y+h > this.standardY )  { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor(c);
		g2.setComposite(AlphaComposite.Src );
		g2.drawRect( x, y, w, h );
		g2.dispose();
		requiresUpdate = true;
		return;
		}
	
	public void JVCrectfill( int x, int y, int w, int h, Color c )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x < 0 || y < 0 ) { return; }
		if( x+w > this.standardX )  { return; }
		if( y+h > this.standardY )  { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor(c);
		g2.setComposite(AlphaComposite.Src );
		g2.fillRect( x, y, w, h );
		g2.dispose();
		requiresUpdate = true;
		return;
		}
	
	public void JVCline(int x1, int y1, int x2, int y2, Color c )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0 ) { return; }

		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor(c);
		g2.setComposite(AlphaComposite.Src );
		g2.drawLine(x1, y1, x2, y2 );
		g2.dispose();
		requiresUpdate = true;
		return;
		}
	
		// Pixel.. basically a degenerate line
	public void JVCpixel(int x, int y, Color c )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x < 0 || y < 0 ) { return; }

		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor(c);
		g2.setComposite(AlphaComposite.Src );
		g2.drawLine( x, y, x, y );
		g2.dispose();
		requiresUpdate = true;
		return;
		}

/**   Not an actual menu, calls drawing function to draw
 * 			what only looks like a menu, but has no function.
* 		  @deprecated Use {@link #JVCpaintMenu} after JVCmenuAttach 
* for a real menus
 */
	public boolean JVCmenuPanel( int leftX, int topY, int totalWidth, int totalHeight, Color backgroundColor,
			int frameWidth, Color frameColor, boolean sunkenFrame )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return(false); }
		
			// minimum possible size for a sunken frame is 3
		if( (frameWidth < 3) && (sunkenFrame == true) )
			{ frameWidth = 3; }
	
		if( leftX < 0 || topY < 0 ) 				{ return(false); }
		if( frameWidth*3 > totalWidth ) 	{ return(false); }
		if( frameWidth*3 > totalHeight ) 	{ return(false); }
		if( leftX+totalWidth > this.standardX )  { return(false); }
		if( topY+totalHeight > this.standardY )  { return(false); }

		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor( backgroundColor );
		g2.setComposite(AlphaComposite.Src );
		g2.fillRect( leftX, topY, totalWidth, totalHeight );
		g2.setColor( frameColor );
		int fx,fw,fy,fh;
		for( int a = 0; a < frameWidth; a++ )
			{
			if( sunkenFrame == true && (a > 0 && a < (frameWidth - 1) ) )
				{ continue; }
			fx = leftX + a;
			fw = totalWidth - (a*2) - 1;
			fy = topY + a;
			fh = totalHeight - (a*2) - 1;
			
			g2.drawRect( fx, fy, fw, fh );
			}
		
		g2.dispose();
		this.requiresUpdate = true;
		return(true);
		}

	/**   Not an actual menu, calls drawing function to draw
	 * 			what only looks like a menu, but has no function.
	* 		  @deprecated Use {@link #JVCpaintMenu} for a real menus
	 */
	private boolean JVCmenuImage( VCLayer layer, 
			int leftX, int topY, int totalWidth, int totalHeight, 
			BufferedImage imgBackground, float imgBkgBlend,
			int frameWidth, Color frameColor, boolean sunkenFrame )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return(false); }
		
			// minimum possible size for a sunken frame is 3
		if( (frameWidth < 3) && (sunkenFrame == true) )
			{ frameWidth = 3; }
	
		if( leftX < 0 || topY < 0 ) 				{ return(false); }
		if( frameWidth*3 > totalWidth ) 	{ return(false); }
		if( frameWidth*3 > totalHeight ) 	{ return(false); }
		if( leftX+totalWidth > this.standardX )  { return(false); }
		if( topY+totalHeight > this.standardY )  { return(false); }

		Graphics2D g2 = (Graphics2D) layer.getImage().getGraphics();

		this.JVCblitScaleBlendImage(layer, leftX, topY, totalWidth, totalHeight, 
				imgBackground, imgBkgBlend );
		
//		g2.fillRect( leftX, topY, totalWidth, totalHeight );
		
		g2.setColor( frameColor );
		int fx,fw,fy,fh;
		for( int a = 0; a < frameWidth; a++ )
			{
			if( sunkenFrame == true && (a > 0 && a < (frameWidth - 1) ) )
				{ continue; }
			fx = leftX + a;
			fw = totalWidth - (a*2) - 1;
			fy = topY + a;
			fh = totalHeight - (a*2) - 1;
			
			g2.drawRect( fx, fy, fw, fh );
			}
		
		g2.dispose();
		this.requiresUpdate = true;
		return(true);
		}

	/**   Not an actual menu, calls drawing function to draw
	 * 			what only looks like a menu, but has no function.
	* 		  @deprecated Use {@link #JVCpaintMenu} for a real menus
	 */
	public boolean JVCmenuImage( int leftX, int topY, int totalWidth, int totalHeight, 
			BufferedImage imgBackground, float imgBkgBlend,
			int frameWidth, Color frameColor, boolean sunkenFrame )
		{
		return this.JVCmenuImage( this.vcl.get(currentLayer),
			leftX, topY, totalWidth, totalHeight,  imgBackground, imgBkgBlend,
			frameWidth, frameColor, sunkenFrame );
		}

	/**   Not an actual menu, calls drawing function to draw
	 * 			what only looks like a menu, but has no function.
	* 		  @deprecated Use {@link #JVCpaintMenu} for a real menus
	 */
	public boolean JVCmenuImage( int leftX, int topY, int totalWidth, int totalHeight, 
			VImage imgBackground, float imgBkgBlend,
			int frameWidth, Color frameColor, boolean sunkenFrame )
		{
		return( JVCmenuImage( leftX, topY, totalWidth, totalHeight, 
				imgBackground.getImage(), imgBkgBlend,
				frameWidth, frameColor, sunkenFrame ) );
		}

		// Just a simpler form of a menuPanel
	public boolean JVCborderedBox( int leftX, int topY, int totalWidth, int totalHeight, Color backgroundColor,	int frameWidth )
		{
		return JVCmenuPanel( leftX,  topY,  totalWidth,  totalHeight, backgroundColor,				frameWidth, Color.WHITE, false );
		}


		// Combines several functions to create a textbox
		// Height and Width are calculated fronm text matrics
	public boolean JVCtextOutline( int x, int y, String s, 
			Font fnt, Color textColor, Color outlineColor, int paddingPx,
			Color backgroundColor )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return(false); }
		if( x < 0 || y < 0 ) { return(false); }
		if( paddingPx < 1 )  { paddingPx = 1; }
		if( paddingPx > 20 ) { paddingPx = 20; }

		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setFont( fnt );

		int singleLineHeight = g2.getFontMetrics().getHeight();
		int checkY = singleLineHeight + (paddingPx*2);
		int checkX = g2.getFontMetrics().stringWidth(s);

		String[] sParts = new String[10];
		int numLines = 1;

		String tmp = new String(s);
		int substrLen = checkX;
			// Attempt to get x+checkX within the bounds.   if not ~ must bail
		while( ((x+checkX+(paddingPx*2)+4) >= standardX) && numLines < 5 ) 
			{
			numLines++;
			substrLen = Math.floorDiv( s.length() , numLines );
			tmp = tmp.substring(0, substrLen );
			checkX = g2.getFontMetrics().stringWidth(tmp);
				//  Getting dis-perportionate, stop now.
			if( numLines > substrLen ) { numLines=11; }
			}
		
			// The string is just too long / font too big.
		if( numLines == 5 )
			{
			g2.dispose();
			return(false);
			}
		else
			{
			for( int ln = 0; ln < numLines; ln++ )
				{
				sParts[ln] = new String(s.substring( substrLen*ln, substrLen*(ln+1) ));
				}
			}
		
		// Normalize spaces
	for( int ln = 1; ln < numLines; ln++ )
		{
		int lastSpace = sParts[ln-1].lastIndexOf(" ");
		int thisLen = sParts[ln-1].length();
		
		if( thisLen < 15 )   			{ continue; }
		if( lastSpace == thisLen ) 	{ continue; }
		if( lastSpace < 6 )				{ continue; }

		String snip = new String( 
				sParts[ln-1].substring(lastSpace + 1, thisLen  ) );
		sParts[ln-1] = sParts[ln-1].substring(0,lastSpace );
		sParts[ln] = new String( snip + sParts[ln] );
		}

			// Now check for box running off the bottom of screen
		checkY = (singleLineHeight * numLines ) + (paddingPx*2);
		if( y+checkY > standardY )   
			{
			g2.dispose();
			return(false);
			}

		int longestLine = 0;
		for( int ln = 0; ln < numLines; ln++ )
			{ 
			int thislen = g2.getFontMetrics().stringWidth( sParts[ln] );
			if( thislen > longestLine )  { longestLine = thislen; }
			}
		
			// Now, we can finally calculate the outline bounds of the box.
		int x0 = x;    int y0 = y;
		int w0 = longestLine + (paddingPx*2) + 4;
		int h0 = (numLines * singleLineHeight ) + (paddingPx * 2) + 4;

		g2.setComposite(AlphaComposite.Src );
		g2.setColor(backgroundColor);
		g2.fillRect( x+1, y+1, w0-2, h0-2 );
		g2.setColor( outlineColor );
		g2.drawRect( x, y, w0, h0 );
		g2.drawRect( x+1, y+1, w0-2, h0-2 ); 
		g2.setColor( outlineColor.darker() );
		g2.drawRect( x+1, y+1, w0-2, h0-2 );
		g2.dispose();

		x0 = x+2+paddingPx;
		y0 = y+2+paddingPx;

		for( int ln = 0; ln < numLines; ln++, y0 += (singleLineHeight+2) )
			{
			this.JVCstring( x0, y0, sParts[ln], fnt, textColor );
			}

		this.requiresUpdate = true;
		return(true);
		}

	// By default the public function draws to the active JVC layer.
	public boolean JVCtextImageBox( int x, int y, String s, 
			Font fnt, Color textColor, Color outlineColor, int paddingPx,
			BufferedImage imgBackground, float alphaBackground )
		{
		return JVCtextImageBox( vcl.get(this.currentLayer) , x, y, s, 
			fnt, textColor, outlineColor, paddingPx,
			imgBackground, alphaBackground );
		}
	
	private boolean JVCtextImageBox( VCLayer targetLayer, int x, int y, String s, 
			Font fnt, Color textColor, Color outlineColor, int paddingPx,
			BufferedImage imgBackground, float alphaBackground )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return(false); }
		if( x < 0 || y < 0 ) { return(false); }
		if( paddingPx < 1 )  { paddingPx = 1; }
		if( paddingPx > 20 ) { paddingPx = 20; }

		Graphics2D g2 = (Graphics2D) targetLayer.getImage().getGraphics();
		g2.setFont( fnt );

		int singleLineHeight = g2.getFontMetrics().getHeight();
		int checkY = singleLineHeight + (paddingPx*2);
		int checkX = g2.getFontMetrics().stringWidth(s);

		String[] sParts = new String[10];
		int numLines = 1;

		String tmp = new String(s);
		int origlen = s.length();
		int substrLen = origlen;
		
		
			// Attempt to get x+checkX within the bounds.   if not ~ must bail
		while( ((x+checkX+(paddingPx*2)+4) >= standardX) && numLines < 5 ) 
			{
			numLines++;
			
			substrLen = Math.floorDiv( origlen , numLines );
			tmp = tmp.substring(0, substrLen );
			checkX = g2.getFontMetrics().stringWidth(tmp);
				//  Getting dis-perportionate, stop now.
			if( numLines > substrLen ) { numLines=11; }
			}
		
			// The string is just too long / font too big.
		if( numLines == 5 )
			{
			g2.dispose();
			return(false);
			}
		else
			{
			for( int ln = 0; ln < numLines; ln++ )
				{
				sParts[ln] = new String(s.substring( substrLen*ln, substrLen*(ln+1) ));
				}
			}
		
		// Normalize spaces
	for( int ln = 1; ln < numLines; ln++ )
		{
		int lastSpace = sParts[ln-1].lastIndexOf(" ");
		int thisLen = sParts[ln-1].length();
		
		if( thisLen < 15 )   			{ continue; }
		if( lastSpace == thisLen ) 	{ continue; }
		if( lastSpace < 6 )				{ continue; }

		String snip = new String( 
				sParts[ln-1].substring(lastSpace + 1, thisLen  ) );
		sParts[ln-1] = sParts[ln-1].substring(0,lastSpace );
		sParts[ln] = new String( snip + sParts[ln] );
		}

			// Now check for box running off the bottom of screen
		checkY = (singleLineHeight * numLines ) + (paddingPx*2);
		if( y+checkY > standardY )   
			{
			g2.dispose();
			return(false);
			}

		int longestLine = 0;
		for( int ln = 0; ln < numLines; ln++ )
			{
			int thislen = g2.getFontMetrics().stringWidth( sParts[ln] );
			if( thislen > longestLine )  { longestLine = thislen; }
			}

			// Now, we can finally calculate the outline bounds of the box.
		int x0 = x;    int y0 = y;

//		int w0 = longestLine + (paddingPx*2) + 4;
//		int h0 = (numLines * singleLineHeight ) + (paddingPx * 2) + 4;
//		this.JVCblitScaleBlendImage( targetLayer, x0,y0,w0,h0,
//				imgBackground, alphaBackground );
//		
//		g2.setColor(Color.black);
//		g2.fillRect( x, y, w0, h0 );
//		g2.setColor( outlineColor );
//		g2.drawRect( x, y, w0, h0 );
//		g2.drawRect( x+1, y+1, w0-2, h0-2 ); 
//		g2.setColor( outlineColor.darker() );
//		g2.drawRect( x+1, y+1, w0-2, h0-2 );
//		g2.dispose();
		
//		boolean tester = this.JVCmenuImage(targetLayer, x, y, w0, h0, imgBackground, 
//			alphaBackground, paddingPx, outlineColor,true );

		x0 = x+2+paddingPx;
		y0 = y+2+paddingPx;

		for( int ln = 0; ln < numLines; ln++, y0 += (singleLineHeight+2) )
			{
			this.JVCstring( targetLayer, x0, y0, sParts[ln], fnt, textColor );
			}

		
		this.requiresUpdate = true;
		return(true);
		}
	
	public boolean JVCtextImageBox( int x, int y, String s, 
			Font fnt, Color textColor, Color outlineColor, int paddingPx,
			VImage imgBackground, float alphaBackground )
		{
		return( JVCtextImageBox( x, y, s, fnt, textColor, outlineColor,
				paddingPx, imgBackground.getImage(), alphaBackground ) );
		}

	private boolean JVCtextImageBox( VCLayer targetLayer, int x, int y, String s, 
			Font fnt, Color textColor, Color outlineColor, int paddingPx,
			VImage imgBackground, float alphaBackground )
		{
		return( JVCtextImageBox( targetLayer, x, y, s, fnt, textColor, outlineColor,
				paddingPx, imgBackground.getImage(), alphaBackground ) );
		}


		// Blits an image onto current layer, giving control of the alpha blending
		// Constructs new intermediate image so this may be slow if abused.
		//  This was delegated because of the need to draw to dialog layer
	public void JVCblitScaleBlendImage( int x, int y, int w, int h, BufferedImage img, float blendValue )
		{
		JVCblitScaleBlendImage( this.vcl.get(this.currentLayer), 
				x, y, w, h, img, blendValue );
		return;
		}

		// Blits an image onto current layer, giving control of the alpha blending
		// Constructs new intermediate image so this may be slow if abused.
	private void JVCblitScaleBlendImage( VImage destLayer, int x, int y, int w, int h, BufferedImage img, float blendValue )
		{
		if( this.vcl.get(currentLayer).getActive() == false )  
			{ return; }
		if( x < 0 || y < 0 )   { return; }
		if( x+w > standardX )  { w -= (x+w-standardX); }
		if( y+h > standardY )  { h -= (y+h-standardY); }
		if( w < 1 ) { return; }	// prevent degenerate image.
		if( h < 1 ) { return; }
		
		double xFactor = (double) w / (double) img.getWidth();
		double yFactor = (double) h / (double) img.getHeight();
		AffineTransform at = new AffineTransform();
		at.scale( xFactor,  yFactor );
			// Scale the image to blit
		VImage interImg = new VImage(w,h);
		Graphics2D g2i = (Graphics2D) interImg.getImage().getGraphics();
//		g2i.setComposite( AlphaComposite.Src );
		g2i.drawImage( img, at, null );
		g2i.dispose();
			// Blend and blit it over the current layer at given position.
		Graphics2D g2d = (Graphics2D) destLayer.getImage().getGraphics();
		g2d.setComposite( AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, blendValue )  );
		g2d.drawImage( interImg.getImage(), x, y, Color.BLACK, null);		
		g2d.dispose();
		this.requiresUpdate = true;
		return;
		}

	public void JVCblitScaleBlendImage( int x, int y, int w, int h, VImage img, float blendValue )
		{ 
		this.JVCblitScaleBlendImage( x, y, w, h,img.getImage(), blendValue );
		return;
		}
	
	public void JVCrotateLayer(float rotationRadians)
		{
		this.vcl.get(this.currentLayer).rotate(rotationRadians);
		this.requiresUpdate = true;
		}
	public void JVCblendLayer(float blendFactor )
		{
		this.vcl.get(this.currentLayer).alphaBlend(blendFactor);
		this.requiresUpdate = true;
		}

		// Takes a VImage and rotates adn scales it onto the current layer
	public void JVCrotscaleBlendBlit( VImage img, int x, int y, 
			float rotationRadians, float sX, float sY, float alphaValue )
		{
		this.vcl.get(this.currentLayer).rotScaleBlendBlit(img, x, y, 
				rotationRadians, sX, sY, alphaValue );
		this.requiresUpdate = true;
		}

	/*
	 * External dialog processor shall: (on each frame)
	 * 
	 * Expire any old dialogs
	 * Query JVCL for entity numbers of active boxes
	 * Use these entity numbers to update current map positions
	 * Call routine to draw all boxes (done in this.flattenLayers )
	 * 
	 * The methods below provide the facility to work this process.
	 */

	// This adds a entity-tracking dialog to the dialog Layer
	//  The coordinates of the player<=>entity must be sent from 
	//       external sources whenever one of them moves.
	public void JVCdialogAdd( int entityNum, String message, 
			int durationMsec, Font fnt, Color textColor, 
			Color outlineColor, int frameWidth, VImage imgBackground,
			float alphaBackground )
		{
		DialogTracker iDialog = new DialogTracker(entityNum, message, 
			durationMsec, fnt, textColor, outlineColor, frameWidth, imgBackground, 
			alphaBackground, this.standardX, this.standardY );
		this.dialogBoxes.add(iDialog);
		this.requiresUpdate = true;
		return;
		}
	
	// makes a pass over all dialogs and removes any expired ones from the stack
	public int JVCdialogExpire()
		{
		int removed = 0;
		int dialogIndex = -1;
		int dialogTotal = this.dialogBoxes.size();
//		System.out.println(Integer.toString(dialogTotal)+" Dialogs to check");
		boolean[] deathFlag = new boolean[dialogTotal];
		for( DialogTracker dt : this.dialogBoxes )
			{
			dialogIndex++;
			deathFlag[dialogIndex] = dt.hasExpired();
			}

		for( int idx = (dialogTotal - 1); idx >= 0; idx-- )
			{
			if( deathFlag[idx] == true )
				{
				System.out.println(" Dialog # "+Integer.toString(idx)+" has expired.");
				this.dialogBoxes.remove(idx);
				removed++;
				}
			}

		if( removed > 0 )		// reflatten 
			{
			this.JVCclearDialog();
			this.requiresUpdate = true; 
			}
		return removed;
		}
	
	public int[] JVCdialogGetEntityNumbers()
		{
		int[] ents = new int[this.dialogBoxes.size()];
		int idx = -1;
		for( DialogTracker dt : this.dialogBoxes )
			{
			idx++;
			ents[idx] = dt.getEntityNumber();
			}
		return ents;
		}

	public void JVCdialogSetCoordinates(int index, int playerMapX,
			int playerMapY, int entityMapX, int entityMapY,	byte zoom )
		{
		DialogTracker dt = this.dialogBoxes.get(index);
		dt.updatePositions( playerMapX, playerMapY, 
				entityMapX, entityMapY, zoom );
		this.requiresUpdate = true;
		return;
		}
	// Essentially same as above, but finds a particular entity number first.
	//   returns false if that entity does not have an active dialog
	public boolean JVCdialogSetCoordinatesViaEntityNumber(
			int entityIndex, int playerMapX,
			int playerMapY, int entityMapX, int entityMapY,	byte zoom )
		{
		DialogTracker dt = null;
		boolean foundIt = false;
		for( DialogTracker idt : this.dialogBoxes )
			{
			if( idt.getEntityNumber() == entityIndex )
				{ dt = idt;  foundIt = true; }
			}
		
		if( foundIt == false )  { return false; }
		if( dt == null )  { return false; }
		
		dt.updatePositions( playerMapX, playerMapY, 
				entityMapX, entityMapY, zoom );
		
		this.requiresUpdate = true;
		return true;
		}
	
	public int JVCdialogDraw()
		{
		int dialogsDrawn = 0;
		if( this.dialogBoxes.size() <= 0 )   
			{   return(0);   }
		this.JVCclearDialog();
		for( DialogTracker dt : this.dialogBoxes )
			{
			if( dt.isOnScreen() == false ) 	{ continue; }

			this.JVCtextImageBox( this.JVCLdialoglayer, 
					dt.getX(), dt.getY(), dt.getText(), 
					dt.getFont(), dt.getTextColor(), dt.getOutlineColor(), 
					dt.getPaddingPx(), dt.getBufferedImage(),
					dt.getAlphaBackgroundWithFade() );

			}
		return(dialogsDrawn);
		}

	public int getJVCLdialogZpos()
		{	return JVCLdialogZpos;	}

	public void setJVCLdialogZpos(int jVCLdialogZpos)
		{	
		JVCLdialogZpos = jVCLdialogZpos;
		this.requiresUpdate = true;
		return;
		}

	/** Used only to stop frivolous compiler warnings
	 * @deprecated
	 */
	protected void _warning_stopper_do_not_use()
		{
		this.dialogBoxes.get(0).getAlphaBackground();
		this.dialogBoxes.get(0).getFadeTimeDurationInNanoSeconds();
		this.dialogBoxes.get(0).getImgBackground();
		this.vcl.get(0).getNum();
		this.JVCtextImageBox(this.vcl.get(0), 0, 0, "dat", 
				core.Script.fntCONSOLE, 
				core.Script.Color_DEATH_MAGENTA, 
				core.Script.Color_DEATH_MAGENTA,
				1, this.vcl.get(0), 1.0f );
		
		this._warning_stopper_do_not_use_2();
		return;
		}
	/**
	 * @deprecated
	 */
	protected void _warning_stopper_do_not_use_2()
		{
		this._warning_stopper_do_not_use();
		return;
		}
	
	}
	
