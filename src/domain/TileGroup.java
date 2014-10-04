package domain;

import java.util.ArrayList;

import domain.Map;


public class TileGroup
	{

	private class CmpTile
		{
		public int tx,ty,tz,tn;
		public int I,J;
		private String name; 
		public CmpTile(int xMapPos, int yMapPos, int mapLayerNum, int tileVspNumber )
			{
			setName(new String(""));
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
		public CmpTile(String tileName, int xMapPos, int yMapPos, int mapLayerNum, int tileVspNumber )
			{
			setName(new String(tileName));
			tx = xMapPos;
			ty = yMapPos;
			tz = mapLayerNum;
			tn = tileVspNumber;
			I = 0;  J=0;		
			}
		public String getName()
			{  return name;	}
		public void setName(String name)
			{	this.name = name;	}
		public void WarningKiller()
			{
			// Warning killer - do not use
			String tmp1 = getName();
			String tmp2 = new String("bleh");
			tmp2 = tmp1;
			tmp1 = tmp2;
			return;
			}
		}
	
	// Most basic verge map tile unit
	// We also use an internal grid that holds tiles locations 
	// relative to each other (i,j) -- this is optional, but will override
	//  the tiles x/y values when relative placement is requested.
	
//	private ArrayList<CmpTile> tiles = new ArrayList<CmpTile>();
	private ArrayList<CmpTile> tiles;
	private String groupName;
	
	public TileGroup primeTerrain;
	public boolean autotiling;
	
	public TileGroup()
		{  
		tiles = new ArrayList<CmpTile>();
		setGroupName(new String("Undesignated Tile Group"));
		}
	
	// creates a functionally "dumb" tile group that is basically just "tn"s  with a name
	public TileGroup( Integer[] tileIdx, String GroupName )
		{
		tiles = new ArrayList<CmpTile>();
		for ( Integer t : tileIdx )
			{
			tiles.add( new CmpTile( GroupName+"Component" , 
					0, 0, 0, t ) );
			}
		setGroupName(new String( GroupName ));
		tiles.get(0).WarningKiller();
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

	
	public String getGroupName()
		{	return groupName; }

	public void setGroupName(String groupName)
		{	this.groupName = groupName;	}
	
	public int getComponentTileVspIndex(int n)
		{
		if( n > tiles.size() ) { return -1; }
		if( n < 0 ) { return -1; }
		return( tiles.get(n).tn );
		}
	
		// "diffs" a tile group vs. another, keeping the higher vsp index of both
	public TileGroup max(TileGroup other)
		{
		if( other == null ) { return(null); }
		for( int n = 0; n < this.tiles.size(); n++ )
			{
			if( n > (other.size()-1) ) { continue; }
			int a = this.getComponentTileVspIndex(n);
			int b = other.getComponentTileVspIndex(n);
			if( b > a ) 
				{ this.tiles.get(n).tn = b; }
			}
		return this;
		}

	
		// "diffs" a tile group vs. another, keeping the lower vsp index of both
	public TileGroup min(TileGroup other)
		{
		if( other == null ) { return(null); }
		for( int n = 0; n < this.tiles.size(); n++ )
			{
			if( n > (other.size()-1) ) { continue; }
			int a = this.getComponentTileVspIndex(n);
			int b = other.getComponentTileVspIndex(n);
			if( b < a ) 
				{ this.tiles.get(n).tn = b; }
			}
		return this;
		}
	
	}

