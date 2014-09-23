package core;

/* core.JVCL
 * Java Verge C Layers 
 * Emulation of Verge 1's "VC layers"
 * 
 * Provides a persistant, resizable stack of graphics drawing 
 * layers and functions.   They are the last layer drawn to screen
 * On top of all the map layers
 */

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
		}
	
	public void addLayer()
		{
		VCLayer L = new VCLayer(this.standardX,this.standardY); 
		this.vcl.add(L);
		}
	
	public void dropLayer()
		{
		this.vcl.remove( this.vcl.size() );
		}

	public int getLayerCount()
		{ return(this.vcl.size()); }
	
	public boolean setWriteLayer( int layerNum )
		{
		if( layerNum <= 0 ) { return(false); }
		if( layerNum > this.vcl.size() )  { return(false); }
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
		if( layerNumber > this.vcl.size() )  { return(false); }
		this.vcl.get(layerNumber).setVisible(true);
		return(true);
		}
	
	public boolean getLayerVisibility(int layerNumber)
		{	return( this.vcl.get(layerNumber).getVisible() );	}
	
	private void flattenLayers()
		{
		// Flattens all "visible" layers into layer 0
		//   In preparation for export.
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

	}
