package domain;

import java.util.ArrayList;
import domain.Map;


public class TileGroup
	{

	private class CmpTile
		{
		public int tx,ty,tz,tn;
		public int I,J;
		public CmpTile(int xMapPos, int yMapPos, int mapLayerNum, int tileVspNumber )
			{
			tx = xMapPos;
			ty = yMapPos;
			tz = mapLayerNum;
			tn = tileVspNumber;
			I = 0;  J=0;
			}
		public CmpTile(int xMapPos, int yMapPos, int mapLayerNum, int tileVspNumber, int i, int j)
			{
			tx = xMapPos;
			ty = yMapPos;
			tz = mapLayerNum;
			tn = tileVspNumber;
			I = i;    J = j;
			}
		}
	
	// Most basic verge map tile unit
	// We also use an internal grid that holds tiles locations 
	// relative to each other (i,j) -- this is optional, but will override
	//  the tiles x/y values when relative placement is requested.
	
//	private ArrayList<CmpTile> tiles = new ArrayList<CmpTile>();
	private ArrayList<CmpTile> tiles;

	public TileGroup()
		{  
		tiles = new ArrayList<CmpTile>();
		}

	public void pushTile( int mx, int my, int layer, int tileNumber )
		{
		CmpTile me = new CmpTile(mx, my, layer, tileNumber);
		tiles.add(me);
		}
	
	public void pushTile( int mx, int my, int layer, int tileNumber, int i, int j )
		{
		CmpTile me = new CmpTile(mx, my, layer, tileNumber, i, j );
		tiles.add(me);
		}
	
		// Push tile using I/J instead of X/Y
	public void pushTileRelative( int layer, int tileNumber, int i, int j )
		{
		
		try {
		CmpTile me = new CmpTile( 0, 0, layer, tileNumber, i, j );
		tiles.add(me);
		} catch(Exception e ) { e.printStackTrace(); }
		
		}
	
	public boolean popTile()
		{
		if( tiles.isEmpty() ) { return(false); }
		tiles.remove( tiles.size() );
		return(true);
		}

	public boolean removeTile( int stackindex )
		{
		if( tiles.isEmpty() ) { return(false); }
		if( stackindex < 0 )  { return(false); }
		if( tiles.get(stackindex) == null ) { return(false); }
		tiles.remove( stackindex );
		return(true);
		}

	public boolean isEmpty()
		{ return( tiles.isEmpty() ); }

	public int size()
		{  return( tiles.size() ); }
	
	public void putTileSetAbsolute(Map map)
		{
		if( tiles.isEmpty() )  { return; }
		for (CmpTile t : tiles )
			{
			map.settile( t.tx, t.ty, t.tz, t.tn );
			}
		return;
		}

	//  The advantage of this is you can ignore the individual tiles map-x 
	//  map-y coordinates if you have set relative positions when building the set.
	//   it feeds off the map-x and map-y you give here in ARGS
	//	This also allows the same tile group to be used in multiple areas
	//  Warning: if you didn't declare i/j's when adding tiles, they
	//        will all be stacked ontop each other at (0,0)
	
	public void putTileSetRelative(Map map, int mapTX, int mapTY )
		{
		if( tiles.isEmpty() )  { return; }
		if( mapTX < 0 || mapTY < 0 )  { return; }
		if( mapTX > map.getWidth() )   { return; }
		if( mapTY > map.getHeight() )   { return; }

		for (CmpTile t : tiles )
			{
			int rx = mapTX + t.I;
			int ry = mapTY + t.J;
			if( rx < 0 || rx > map.getWidth() )  { continue; }
			if( ry < 0 || ry > map.getHeight() )  { continue; }
			map.settile( mapTX + t.I, mapTY+t.J , t.tz, t.tn );
			}
		return;
		}
	
		// Similar to putTileSetRelative, except it overrides the layer number defined at each tile
		// Effectively forcing the entire tile set to be drawn to the specified map layer.
	public void putTileSetRelativeToLayer(Map map, int mapTX, int mapTY, int mapLayer )
		{
		if( tiles.isEmpty() )  { return; }
		if( mapTX < 0 || mapTY < 0 )  { return; }
		if( mapTX > map.getWidth() )   { return; }
		if( mapTY > map.getHeight() )   { return; }
		if( mapLayer < 0 || mapLayer >  map.getNumLayers() )
			{ return; }
		
		for (CmpTile t : tiles )
			{
			int rx = mapTX + t.I;
			int ry = mapTY + t.J;
			if( rx < 0 || rx > map.getWidth() )  { continue; }
			if( ry < 0 || ry > map.getHeight() )  { continue; }
			map.settile( mapTX + t.I, mapTY+t.J , mapLayer, t.tn );
			}
		return;
		}
	
	}

