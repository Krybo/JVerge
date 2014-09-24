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
			{  active = truefalse;   }
		public void setVisible(boolean truefalse)
			{  visible = truefalse;   }
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
	
	public int getWriteLayer()
		{  return( this.currentLayer );  }
	
	public boolean setLayerVisibility(int layerNumber ) 
		{
		if( layerNumber <= 0 ) { return(false); }
		if( layerNumber >= this.vcl.size()  )  { return(false); }
		this.vcl.get(layerNumber).setVisible(true);
		return(true);
		}
	
	public boolean getLayerVisibility(int layerNumber)
		{	return( this.vcl.get(layerNumber).getVisible() );	}
	
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
			g2.setComposite(AlphaComposite.Src );
				// And pound down all layers that have drawing turned on.
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
	public BufferedImage joinBufferedImage( 
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

	
	public void JVCstring(int x, int y, String s)
		{
		if( x < 0 || y < 0 ) { return; }
		Graphics2D g2 = (Graphics2D) vcl.get(this.currentLayer).getImage().getGraphics();
		g2.setColor( Color.WHITE );
		g2.setComposite(AlphaComposite.Src );
		g2.setFont( this.nativefont );
		g2.drawString( s, x, y );
		g2.dispose();
		 // anything that writes to layers needs to set this ~ if successful
		this.requiresUpdate = true; 
		return;
		}
	
	public void JVCcircle()
		{
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
	
	}
	
	
