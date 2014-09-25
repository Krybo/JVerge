package core;

/* core.JVCL
 * Java Verge C Layers 
 * Emulation of Verge 1's "VC layers"
 * 
 * Provides a persistant, resizable stack of graphics drawing 
 * layers and functions.   They are the last layer drawn to screen
 * On top of all the map layers
 */

import static java.awt.Font.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import domain.VImage;

import java.util.ArrayList;



public class JVCL
	{

	private class VCLayer extends VImage
		{
		private boolean visible;
		private boolean active;
		
		public VCLayer(int  sizeX, int sizeY )
			{
			super(sizeX,sizeY);
			visible = false;
			active = false;
			}
		
		public boolean getActive()
			{  return(active);  }
		public boolean getVisible()
			{  return(visible);  }
		public void setActive(boolean truefalse)
			{  this.active = truefalse;   }
		public void setVisible(boolean truefalse)
			{  this.visible = truefalse;   }
		}

	// Adds a student to the student array list.
	private ArrayList<VCLayer> vcl = new ArrayList<VCLayer>();
	private int currentLayer;
	private int standardX, standardY;
	private Font nativefont = new Font("Tahoma",PLAIN, 18);
	private boolean requiresUpdate = false;

	public JVCL(int numLayers, int xRes, int yRes )
		{
		this.standardX = xRes;
		this.standardY = yRes;
		if( standardX < 320 ) { this.standardX = 320; }
		if( standardY < 280 ) { this.standardY = 280; }
		
		// This is a base buffer layer, all must have it.
		this.vcl.add( new VCLayer(standardX,standardY) );
		this.vcl.get(0).setActive(true);
		
		for( int a = 0; a < numLayers; a++)
			{
			vcl.add( new VCLayer(standardX,standardY) );
			vcl.get(a+1).setActive(true);
			}

		JVCclearAllLayers();
		currentLayer = 1;
		}
	
	public void addLayer()
		{
		VCLayer L = new VCLayer(this.standardX,this.standardY); 
		this.vcl.add(L);
		}
	
	public void dropLayer()
		{
		this.vcl.remove( this.vcl.size() - 1 );
		}

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
		requiresUpdate = false;
		return;
		}
	
	public VImage getVImage()
		{
		this.flattenLayers();
		return( this.vcl.get(0) );
		}

	public BufferedImage getBufferedImage() 
		{
		this.flattenLayers();
		return( this.vcl.get(0).getImage() );
		}

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

	
	public boolean JVCstring(int x, int y, String s, Font fnt, Color c )
		{
		if( x < 0 || y < 0 ) { return(false); }
		
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
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

		y += (1 + Math.floorDiv(checkY, 2)); 
		
		g2.setColor( c );
		g2.setComposite(AlphaComposite.Src );
		g2.drawString( s, x, y );
		g2.dispose();
		 	// 	anything that writes to layers needs to set this ~ if successful
		this.requiresUpdate = true; 
		return(true);
		}

		// A "dumb" overload form of the above.
	public boolean JVCstring(int x, int y, String s )
		{
		return JVCstring(x,y,s, this.nativefont, Color.white );
		}
	
	public void JVCoval( int cx, int cy, int xRad, int yRad, Color c, boolean fillErIn )
		{
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

	public void JVCclear()
		{
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setComposite(AlphaComposite.Clear );
		g2.fillRect(0, 0, this.standardX, this.standardY );
		g2.dispose();
		this.requiresUpdate = true;
		return;
		}

	public void JVCclearAllLayers()
		{
		for( int a = 0; a < this.vcl.size(); a++ )
			{
			Graphics2D g2 = (Graphics2D) vcl.get(a).getImage().getGraphics();
			g2.setComposite(AlphaComposite.Clear );
			g2.fillRect(0, 0, this.standardX, this.standardY );
			g2.dispose();
			}
		this.requiresUpdate = true;
		return;
		}

	public void JVCrect( int x, int y, int w, int h, Color c )
		{
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

	public boolean JVCmenuPanel( int leftX, int topY, int totalWidth, int totalHeight, Color backgroundColor,
			int frameWidth, Color frameColor, boolean sunkenFrame )
		{
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
		requiresUpdate = true;
		return(true);
		}
	
	}
	
	
