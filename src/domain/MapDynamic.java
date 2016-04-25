package domain;

import static core.Script.callfunction;
import static core.Script.current_map;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import core.Script;
import domain.TileGroup;

public class MapDynamic extends MapAbstract implements Map {

	private VImage[] layers;
	private String mapName;
//	private HashMap<Integer, Integer> mappings;
	private int scaleFactor = 1;
	int NULL_TILE = 0;
	private HashMap<String, Integer[]> autoTileMappings;
	private boolean autoTiling;
	private HashMap<Integer, TileGroup> pixelMappings;
	
	public MapDynamic(String strFilename) {
		this(Script.load(strFilename));
	}
	
	public MapDynamic(URL properties) {
		Properties mapProperties = new Properties();
		try {
			this.scaleFactor = 1;
			mapProperties.load(properties.openStream());
			this.renderstring = mapProperties.getProperty("RenderString");
			this.startX = Integer.parseInt(mapProperties.getProperty("StartX"));
			this.startY = Integer.parseInt(mapProperties.getProperty("StartY"));
			
			String strTileset = mapProperties.getProperty("Tileset");
			this.tileset = new Vsp(Script.load(strTileset));

			// Load Layers as VImages
			String strLayers[] = mapProperties.getProperty("Layers").split(",");
			this.layers = new VImage[strLayers.length];
			for(int i=0; i<layers.length; i++) {
				layers[i] = new VImage(Script.load(strLayers[i].trim()));
			}
			


			this.pixelMappings = new HashMap<Integer, TileGroup>();
			String[] strMappings = mapProperties.getProperty("Tile.Mappings").split(",");

			String strAutoTiles[] = null;
			String strAutotiling = mapProperties.getProperty("Autotiling.tiles");
			if( strAutotiling!=null ) 
				{
				this.autoTiling = true;
				strAutoTiles = strAutotiling.split(",");
				}
//
//			this.mappings = new HashMap<Integer, Integer>() 
//				{
//				private static final long serialVersionUID = -8076555731725126570L;
//					{ // Associates each pixel color to a tile from the given tileset
//				put(new Color(0, 150, 0).getRGB(), 106);
//				put(new Color(30, 100, 30).getRGB(), 37);
//				put(new Color(0, 200, 0).getRGB(), 51);
//				put(new Color(0, 255, 0).getRGB(), 58);
//				put(new Color(0, 0, 255).getRGB(), 7);
//				put(new Color(240, 240, 0).getRGB(), 32);
//				put(new Color(150, 150, 50).getRGB(), 35);
//				put(new Color(200, 200, 240).getRGB(), 76);
//				put(new Color(200, 0, 0).getRGB(), 103);
//				put(new Color(201, 0, 0).getRGB(), 104);
//			}};

			
			
			

			// Associates each pixel color to a tile object from the given tileset
			for(String strPixelName: strMappings) 
				{
				String strMapping = mapProperties.getProperty(
						"Tile.PixelColor." + strPixelName.trim()  );
				String strColors[] = strMapping.split(",");

				String strIdx[] = mapProperties.getProperty(
						"Tile.TilesetIdx." + strPixelName.trim()).split(",");
				Integer[] tileIdx = new Integer[strIdx.length];
				if( Math.sqrt(tileIdx.length) > scaleFactor ) 
					{
					//scaleFactor = (int) Math.sqrt(tileIdx.length);
					}
				for(int i=0; i<tileIdx.length;i++) 
					{
					tileIdx[i] = Integer.parseInt(strIdx[i].trim());
					}

				TileGroup tileObj = new TileGroup( tileIdx , strPixelName.trim() );
//				tileObj.tiles = tileIdx;
//				tileObj.name = strPixelName.trim();

				// Finds primeTerrain TileObject
				String primeTerrain = mapProperties.getProperty(
						"Tile.PrimeTerrain." + strPixelName.trim() );
				if(primeTerrain != null) 
					{
					for( TileGroup tg: pixelMappings.values() ) 
						{
						if( tg.getGroupName().equals(primeTerrain) ) 
							{
								// Krybo ???
							tileObj.primeTerrain = tg;
							break;
							}
						}
					}
				
				//tileObj.primeTerrain = primeTerrain != null ? primeTerrain.trim() : strPixelName.trim(); 
				
				for(String s: strAutoTiles) 
					{
					if( s.trim().equals(strPixelName.trim()) ) 
						{
						tileObj.autotiling = true;
						}
					}

				pixelMappings.put(
						new Color(
							Integer.parseInt(strColors[0].trim()),
							Integer.parseInt(strColors[1].trim()), 
							Integer.parseInt(strColors[2].trim())).getRGB(),
						tileObj);
				}

			/* Auto-tiling */
			if(this.autoTiling == true) 
				{
				autoTileMappings = new HashMap<String, Integer[]>();
				for(int i=0; i<strAutoTiles.length; i++) 
					{	
					for(int j=0; j<strAutoTiles.length; j++) 
						{
						if(i==j)		{ continue; }
						String in = strAutoTiles[i].trim();
						String out = strAutoTiles[j].trim();
						String autoTiling = mapProperties.getProperty("Autotiling." + in + "." + out);
						System.out.println("Autotiling." + in + "." + out);
						if( autoTiling != null )
							{
							String strTiles[] = autoTiling.split(",");
							Integer intTiles[] = new Integer[strTiles.length];
							for(int k=0; k<strTiles.length;k++) 
								{
								intTiles[k] = Integer.parseInt(strTiles[k].trim());
								}
							autoTileMappings.put(in+"/"+out, intTiles);
							
							Integer[] revIntTiles = new Integer[intTiles.length];
							for(int p=0;p<intTiles.length;p++) 
								{
								revIntTiles[p] = intTiles[intTiles.length-p-1];
								}
							autoTileMappings.put(out+"/"+in, revIntTiles);
							}
						}	
					}
				scaleFactor *=2;
				}

			for(TileGroup t: pixelMappings.values()) 
				{
				System.out.println(t.getGroupName());
				}
			for(String s: autoTileMappings.keySet()) 
				{
				System.out.println(s + ": " + Arrays.toString(autoTileMappings.get(s)));
				}

			this.startX*=scaleFactor;
			this.startY*=scaleFactor;
			
			current_map = this;
			callfunction("mapinit");			
		} catch (IOException e) 
			{	e.printStackTrace();	}

	}

	public String getMapname() {
		return "Dynamic map: " + getFilename();
	}
	
	public int getWidth() {
		if(layers!=null && layers[0]!=null) {
			return layers[0].width * scaleFactor;
		}
		return 0;
	}

	public int getHeight() {
		if(layers!=null && layers[0]!=null) {
			return layers[0].height * scaleFactor;
		}
		return 0;
	}

	

	public int gettile(int x, int y, int l)
		{
		if( x<0 || y<0 || x >= this.getWidth() || y >= this.getHeight() ) 
			{	return 0;	}
		if(layers[l] == null) { return 0; }
		
		// Start insertion

		if(layers[l] != null) 
			{
			if(autoTiling && l==0) { // terrain
				
				//TileObject tobj = pixelMappings.get(layers[l].getImage().getRGB(x/scaleFactor, y/scaleFactor));
				//if(tobj!=null) {
					//String strTile = tobj.name;
				//}

				if(x%2==0 && y%2==0)   // Central tile
					{	return getDirectTileFrom(x, y, x, y, l); 	}
				else if (x%2==1 && y%2==0) 
					{ // Horizontal Neighbors
					TileGroup c1 = getTileObjectFrom(x-1, y, l);
					TileGroup c2 = getTileObjectFrom(x+1, y, l);
					
					if(c1==c2) {
						return getDirectTileFrom(x-1, y, l);
					} else {
						return (getAutoTile(c1, c2, c1, c2));
					}					
					
				}
				else if (x%2==0 && y%2==1) { // Vertical Neighbors
					TileGroup c1 = getTileObjectFrom(x, y-1, l);
					TileGroup c2 = getTileObjectFrom(x, y+1, l);
					
					if(c1==c2) {
						return getDirectTileFrom(x, y-1, l);
					} else {
						return (getAutoTile(c1, c1, c2, c2));
					}
				}
				else { // All Directions Neighbors
					TileGroup c1 = getTileObjectFrom(x-1, y-1, l);
					TileGroup c2 = getTileObjectFrom(x+1, y-1, l);
					TileGroup c3 = getTileObjectFrom(x-1, y+1, l);
					TileGroup c4 = getTileObjectFrom(x+1, y+1, l);
					
					if(c1==c2 && c2==c3 && c3==c4) {
						return getDirectTileFrom(x-1, y-1, l);
					} else {
						return (getAutoTile(c1, c2, c3, c4));
					}
					
				}
				
				
				//return NULL_TILE;
				
			}
			
			// Not using autotiling, or if it's using, Even X and Even Y
			return getDirectTileFrom(x, y, x, y, l);
			
		}
		
//		Old form
//		if(layers[i] != null) {
//			Integer c = mappings.get(layers[i].getImage().getRGB(x/scaleFactor, y/scaleFactor));
//			if(c!=null)
//				return c;
//		}
		
		return 0;
		}

	// Combines 4 tiles to return 1 frontier tile
	private int getAutoTile(TileGroup c1, TileGroup c2, TileGroup c3, TileGroup c4) 
		{
		if(c1==null || c2==null || c3==null || c4==null)
			{ 
			System.err.println("Tile not found");
			return NULL_TILE;
			}

		// Tile becomes the primeTerrain, if it exists 
		c1 = c1.primeTerrain==null ? c1 : c1.primeTerrain;
		c2 = c2.primeTerrain==null ? c2 : c2.primeTerrain;
		c3 = c3.primeTerrain==null ? c3 : c3.primeTerrain;
		c4 = c4.primeTerrain==null ? c4 : c4.primeTerrain;
		
		int count = 1;
		if(c1!=c2) count++;
		if(c3!=c1 && c3!=c2) count++;
		if(c4!=c1 && c4!=c2 && c4!=c3) count++;
		
		//If primeTerrain is equal (ex: grass/grass), return it (ex: grass)
		if( count == 1 ) 
			{
//			return c1.tiles[0];
			// Krybo:  *shrug* this is my best guess
			return c1.getComponentTileVspIndex(0);
			}

		TileGroup minTO = null;
		TileGroup maxTO = null;

		if(count==2) 
			{
			// Krybo: yikes!! do not know how exactly these ops
			//    are supposed to be calced. so guessing.
			minTO = c1.min(c2.min(c3.min(c4)));
			maxTO = c1.max(c2.max(c3.max(c4)));
			}
		else if(count == 3 || count == 4 ) 
			{

			//System.out.println("BE" + c1 + " " + c2 + " " + c3 + " " + c4);			
			if(c1.getGroupName().equals("water")) minTO = c1;
			else if(c2.getGroupName().equals("water")) minTO = c2;
			else if(c3.getGroupName().equals("water")) minTO = c3;
			else if(c4.getGroupName().equals("water")) minTO = c4;
			
			if( minTO  != null ) 			// has water 
				{ 
				maxTO = c1;
				if(minTO == maxTO) maxTO = c2;
				if(minTO == maxTO) maxTO = c3;
				if(minTO == maxTO) maxTO = c4;
				}
			else { // does not have water
				minTO = c1;
				maxTO = c2;
				if(minTO == maxTO) maxTO = c3;
				if(minTO == maxTO) maxTO = c4;
				}

			// All tiles become minTO or maxTO
			if(c1 != minTO && c1!=maxTO) c1=maxTO;
			if(c2 != minTO && c2!=maxTO) c2=maxTO;
			if(c3 != minTO && c3!=maxTO) c3=maxTO;
			if(c4 != minTO && c4!=maxTO) c4=maxTO;

			// Swap minTO and maxTO, for correct Autotiling ordering
//			if(minTO.tiles[0] > maxTO.tiles[0])
			if(minTO.getComponentTileVspIndex(0) > maxTO.getComponentTileVspIndex(0) )
				{
				TileGroup temp = minTO;
				minTO = maxTO;
				maxTO = temp;
				}
			//System.out.println("AF" + c1 + " " + c2 + " " + c3 + " " + c4);
			//return NULL_TILE;
			}
		
		int result = NULL_TILE;
		int b1 = c1.equals(minTO) ? 0: 1;
		int b2 = c2.equals(minTO) ? 0: 1;
		int b3 = c3.equals(minTO) ? 0: 1;
		int b4 = c4.equals(minTO) ? 0: 1;
		try {
			result = autoTileMappings.get(minTO.getGroupName() + "/" +
				maxTO.getGroupName() )[b1 + b2*2 + b3*4 + b4*8];
		} catch(Exception e) {
			System.out.println(minTO + " " + maxTO);
			}

		return result;
		}

		// Krybo: this gets the tilegroup index based on
		// the Color of a VImage (layer) of the map at (x,y,z)
	private TileGroup getTileObjectFrom(int x, int y, int l) 
		{
		TileGroup to = pixelMappings.get(
				layers[l].getImage().getRGB(x/scaleFactor, y/scaleFactor) );
		if(to==null) 
			{
			System.err.println(layers[l].getImage().getRGB(x/scaleFactor, y/scaleFactor));
			Color c = new Color(layers[l].getImage().getRGB(x/scaleFactor, y/scaleFactor));
			System.out.println(c.getRed() + "," + c.getGreen() + ","+c.getBlue());
			}
		return to;
		}
	
	private int getDirectTileFrom(int x, int y, int l) {
		return getDirectTileFrom(x,y,x,y,l);
	}
	
	private int getDirectTileFrom(int x, int y, int mappingx, int mappingy, int l) 
		{
		TileGroup tobj = pixelMappings.get(layers[l].getImage().getRGB(
				mappingx/scaleFactor, mappingy/scaleFactor));
		if(tobj!=null) 
			{
			if( tobj.size() ==1)
				{ return tobj.getComponentTileVspIndex(0); }
			else if( tobj.size() == 4 ) 
				{
				return tobj.getComponentTileVspIndex( 2*(y%2==0?0:1) + (x%2==0?0:1) );
				}
			}
		return 0;
		}
	
	
	// END insertion point
	
	public void settile(int x, int y, int i, int z) {
		if(layers[i] != null) {
			layers[i].getImage().setRGB(x * scaleFactor, y * scaleFactor, z);
		}
	}

	
	public boolean getobs(int x, int y) {
		if(x<0 || y<0 || x >= this.getWidth() || y >= this.getHeight())
			return true;
		
		return false;
	}

	public void setobs(int x, int y, int t) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean getobspixel(int x, int y) {
		if(x<0 || y<0 || x>>4 >= this.getWidth() || y>>4 >= this.getHeight())
			return true;
		
		return false;
	}
	
	public String getFilename() {
		return mapName;
	}
	
	public Entity[] getEntities() 
		{
		return null;
		}
	
	public Entity getEntity(int entNumber) 
		{
		return null;
		}
	
	public String getRenderstring() {
		return this.renderstring;
	}
	
	public void setRenderstring(String string) {
		this.renderstring = string;
	}

	public int getzone(int x, int y) 
		{
		return layers[1].getImage().getRGB(x/scaleFactor, y/scaleFactor)==-3670016 ? 1 : 0;
		}

	public void setzone(int x, int y, int z) {
		// TODO Auto-generated method stub
	}

	public String getScriptZone(int zone) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getPercentZone(int zone) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public int getMethodZone(int zone) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getNumLayers() {
		return layers.length;
	}

	public int getLayerLucent(int layer) {
		return 0;
	}
	
	public void refresh()
		{ MapAbstract.refreshCache(); }

}
