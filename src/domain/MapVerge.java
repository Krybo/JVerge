package domain;

import static core.Script.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;

import core.Script;

import persist.ExtendedDataInputStream;
import persist.ExtendedDataOutputStream;

import domain.Entity;

public class MapVerge extends MapAbstract implements Map {

	// .map format: https://github.com/Bananattack/v3tiled/wiki/.map-file

	public static final String MAP_SIGNATURE = "V3MAP";
	public static final int MAP_VERSION = 2;

	private String filename = "";
	private String mapname = "dummy";
	private String vspname = "";
	private String musicname= "";
	private String startupscript = "startmap";

	private Layer[] layers;
	private byte[] obsLayer; 
	private int[] zoneLayer; 									// width * height, Unsigned shorts!

	private Zone[] zones;
	private Entity[] entities;

	
	public String toString() {
		return "Mapname: " + filename + "; vspFile:" + vspname + "; music:"
				+ musicname + "; render:" + renderstring + "; startEvent: "
				+ startupscript + "; start:" + startX + "," + startY;
	}

	public MapVerge(String strFilename) {
		this(Script.load(strFilename));
	}
	
	public MapVerge(URL url) {
		try {
			if(url==null)
				throw new IOException();
			
			this.filename =  url.getFile().substring( url.getFile().lastIndexOf('/')+1);
				
			this.load(url.openStream());
			//FileInputStream fis = new FileInputStream(path + "\\" + filename);
			//this.load(fis);
			
			// Load the vsp (map URL minus the map file plus the vsp file)
			//this.tileset = new Vsp(url.getFile().substring(0, url.getFile().lastIndexOf('/')+1) + this.vspname);
			this.tileset = new Vsp(Script.load(this.vspname));
			
			// Diassociated with loading the map
			startMap();
			
		} catch (IOException ioe) {
			System.err.println("MAP::IOException (" + filename + "), url = " + url);
		}
	}

	public MapVerge(URL mapUrl, URL vspUrl) {
		try {
			this.load(mapUrl.openStream());
		} catch (IOException e) {
			System.err.println("MAP::IOException (" + filename + "), mapurl = " + mapUrl);
		}
		this.tileset = new Vsp(vspUrl);
		
	}

	/**
	 * Loads a Map from an InputStream
	 * 
	 */
	private void load(InputStream is) {

		try {

			ExtendedDataInputStream f = new ExtendedDataInputStream(is);

			// Begin to read
			String mapSignature = f.readFixedString(6);
			if (!mapSignature.equals(MAP_SIGNATURE)) {
				throw new IOException("Map doesn't contain V3MAP signature: " + mapSignature);
			}

			int mapVersion = f.readSignedIntegerLittleEndian();
			int vcOffset = f.readSignedIntegerLittleEndian();
			//System.out.println("Map version:" + mapVersion + "; Vcoffset: "	+ vcOffset);

			// Map information
			this.mapname = f.readFixedString(256);
			this.vspname = f.readFixedString(256);
			this.musicname = f.readFixedString(256);
			this.renderstring = f.readFixedString(256);
			String mapScriptName = f.readFixedString(256);
			if( ! mapScriptName.isEmpty() ) 
				{
				// If this is set, override from default (Krybo 2014-09-14)
				this.startupscript = f.readFixedString(256);
				}
			this.startX = f.readUnsignedShortLittleEndian();
			this.startY = f.readUnsignedShortLittleEndian();

			int numLayers = f.readSignedIntegerLittleEndian(); // layers.length
			this.layers = new Layer[numLayers];

			for (int i = 0; i < numLayers; i++) {
				Layer l = new Layer();
				l.name = f.readFixedString(256);
				l.parallax_x = f.readDoubleLittleEndian();
				l.parallax_y = f.readDoubleLittleEndian();
				l.width = f.readUnsignedShortLittleEndian();
				l.height = f.readUnsignedShortLittleEndian();
				l.lucent = f.readUnsignedByte();
				l.tiledata = f.readCompressedUnsignedShorts();

				this.layers[i] = l;
			}

			// Read compressed (oLayer)
			this.obsLayer = f.readCompressedBytes();

			// Read compressed (zLayer)
			this.zoneLayer = f.readCompressedUnsignedShorts();

			int numZones = f.readSignedIntegerLittleEndian(); // zones.length
			this.zones = new Zone[numZones];
			//System.out.println("numZones = " + numZones);

			for (int i = 0; i < numZones; i++) {
				Zone z = new Zone();
				z.name = f.readFixedString(256);
				z.script = f.readFixedString(256);
				z.percent = f.readUnsignedByte();
				z.delay = f.readUnsignedByte();
				z.method = f.readUnsignedByte();

				this.zones[i] = z;
			}

			int numEntities = f.readSignedIntegerLittleEndian(); // entities.length
			this.entities = new Entity[numEntities];
			//System.out.println("numEntities = " + numEntities);

			for (int i = 0; i < numEntities; i++) {
				//Entity e = new Entity();
				int x = f.readUnsignedShortLittleEndian();
				int y = f.readUnsignedShortLittleEndian();
				Entity e = new Entity(x*16, y*16, null);
				e.face = f.readByte();
				e.obstructable = f.readByte() == 0 ? false : true;
				e.obstruction = f.readByte() == 0 ? false : true;
				e.autoface = f.readByte() == 0 ? false : true;
				e.speed = f.readUnsignedShortLittleEndian();
				f.readByte(); // unused
				e.movecode = f.readByte();
				e.wx1 = f.readUnsignedShortLittleEndian();
				e.wy1 = f.readUnsignedShortLittleEndian();
				e.wx2 = f.readUnsignedShortLittleEndian();
				e.wy2 = f.readUnsignedShortLittleEndian();
				e.wdelay = f.readUnsignedShortLittleEndian();
				f.readInt(); // unused
				e.movescript = f.readFixedString(256);

				switch(e.movecode) {
					case 0: e.SetMotionless(); break;
					case 1: e.SetWanderZone(); break;
					case 2: e.SetWanderBox(e.wx1, e.wy1, e.wx2, e.wy2); break; //FIXME
					case 3: e.SetMoveScript(e.movescript); break;				
				}
				
				e.chrname = f.readFixedString(256);
				e.description = f.readFixedString(256);
				e.script = f.readFixedString(256);  // this is the actual script

				this.entities[i] = e;
			}

			// VC Code
			
			f.close();
			
		} catch (IOException e) {
			System.err.println("IOException : " + e);
		}
	}

	/**
	 * Saves a Map to a specified file path
	 * 
	 */
	public void save(String strFilePath) {

		// create FileOutputStream object
		try {
			FileOutputStream fos = new FileOutputStream(strFilePath);
			int vcoffset = this.save(this, fos);
			
			RandomAccessFile raf = new RandomAccessFile(strFilePath, "rw");
			raf.seek(10);
			raf.writeInt(Integer.reverseBytes(vcoffset));
			raf.close();
		}
		catch(FileNotFoundException fnfe) {
			System.err.println("Map::FileNotFoundException : " + strFilePath);
			fnfe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Saves a Map to a specified output stream
	 * 
	 * @param Map m
	 * @param OutputStream
	 */
	public int save(MapVerge m, OutputStream os) {

		ExtendedDataOutputStream f = null;
		try {
			f = new ExtendedDataOutputStream(os);

			// Begin to write
			f.writeString(MapVerge.MAP_SIGNATURE);
			f.writeSignedIntegerLittleEndian(MapVerge.MAP_VERSION);

			// Write a dummy offset for now, but this needs to be backpatched,
			// once the real map is completed.
			int vc = f.size();
			f.writeSignedIntegerLittleEndian(0);
			f.writeFixedString(m.mapname, 256);
			f.writeFixedString(m.vspname, 256);
			f.writeFixedString(m.musicname, 256);
			f.writeFixedString(m.renderstring, 256);
			f.writeFixedString(m.startupscript, 256);
			f.writeUnsignedShortLittleEndian(m.startX);
			f.writeUnsignedShortLittleEndian(m.startY);

			f.writeSignedIntegerLittleEndian(m.layers.length);
			for (Layer l : m.layers) {
				f.writeFixedString(l.name, 256);
				f.writeDoubleLittleEndian(l.parallax_x);
				f.writeDoubleLittleEndian(l.parallax_y);
				f.writeUnsignedShortLittleEndian(l.width);
				f.writeUnsignedShortLittleEndian(l.height);
				f.writeUnsignedByte(100 - (int) (l.lucent * 100.0 + 0.5));
				//l.tiledata[0] = 1;
				//l.tiledata[1] = 15;
				//l.tiledata[2] = 800;
				//l.tiledata[3] = 14000;
				f.writeCompressedUnsignedShorts(l.tiledata);
			}

			f.writeCompressedBytes(m.obsLayer);
			f.writeCompressedUnsignedShorts(m.zoneLayer);

			f.writeSignedIntegerLittleEndian(m.zones.length);
			for (Zone z : m.zones) {
				f.writeFixedString(z.name, 256);
				f.writeFixedString(z.script, 256);
				f.writeUnsignedByte(z.percent);
				f.writeUnsignedByte(z.delay);
				f.writeUnsignedByte(z.method);
			}

			f.writeSignedIntegerLittleEndian(m.entities.length);
			for (Entity e : m.entities) {
				f.writeUnsignedShortLittleEndian(e.getx()/256);
				f.writeUnsignedShortLittleEndian(e.gety()/256);
				f.writeByte(e.face); // 0 or 1
				f.writeByte(e.obstructable == false ? 0 : 1);
				f.writeByte(e.obstruction == false ? 0 : 1);
				f.writeByte(e.autoface == false ? 0 : 1);
				f.writeUnsignedShortLittleEndian(e.speed);
				f.writeByte(0);
				f.writeByte(e.movecode);
				f.writeUnsignedShortLittleEndian(e.wx1);
				f.writeUnsignedShortLittleEndian(e.wy1);
				f.writeUnsignedShortLittleEndian(e.wx2);
				f.writeUnsignedShortLittleEndian(e.wy2);
				f.writeUnsignedShortLittleEndian(e.wdelay);
				f.writeSignedIntegerLittleEndian(0);
				f.writeFixedString(e.movescript, 256);
				f.writeFixedString(e.chrname, 256);
				f.writeFixedString(e.description, 256);
				f.writeFixedString(e.script, 256);
			}

			// Write the vc offset. (Don't need, but see RandomAccessFile)
			 int end = f.size();
			// f.seek(vc);
			f.writeSignedIntegerLittleEndian(0);

			System.err.println("[Rafael, the Esper]SAVE: " + f.size());
			f.close();

			return end;
			
		} catch (IOException e) {
			System.err.println("IOException : " + e);
		}
		return 0;
	}
	

	// Rafael: Code disassociated with map loading
	private void startMap() {

		if(!musicname.trim().isEmpty())
			playmusic(Script.load(musicname));
		
		current_map = this;
		//se.LoadMapScript(f, mapfname);
		
		for(int i=0; i<current_map.getEntities().length; i++) {
			Entity e = current_map.getEntities()[i];
			e.chr = new CHR(e.chrname); //RequestCHR(e.chrname);
			
			e.index = Script.numentities++;
			entity.add(e);
		}

		//TODO Check if this is needed
		//if(this.tileset.numobs == 0)
			//this.tileset.numobs = 1;
		
		if(startupscript != null && !startupscript.trim().equals(""))
			callfunction(startupscript);
		
		log( "Map Loaded, now running "+startupscript );

	}
	
	
	// Use

	public int getzone(int x, int y) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight())
			return 0;
		return zoneLayer[(y * getWidth()) + x];
	}

	public boolean getobs(int x, int y) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight())
			return true;
		if (obsLayer[(y * getWidth()) + x] == 0)
			return false;
		return true;
	}

	public boolean getobspixel(int x, int y) { // modified by [Rafael, the Esper]
		if (!horizontalWrapable && (x < 0 || (x >> 4) >= getWidth()))
				return true;
		if (!verticalWrapable && (y < 0 || (y >> 4) >= getHeight()))
				return true;
		if(horizontalWrapable && x < 0)
			x+= (getWidth() << 4); 
		if(horizontalWrapable && (x >> 4) >= getWidth())
			x-= (getWidth() << 4);
		if(verticalWrapable && y < 0)
			y+= (getHeight() << 4); 
		if(verticalWrapable && (y >> 4) >= getHeight())
			y-= (getHeight() << 4);

		int t = obsLayer[((y >> 4) * getWidth()) + (x >> 4)];
		return tileset.GetObs(t, x&15, y&15);
	}
	
	public int gettile(int x, int y, int i) { 
		if(i>=this.layers.length) 
			return 0; 
		return this.layers[i].getTile(x,y); 
	}

	public void setzone(int x, int y, int z) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight())
			return;
		if (z >= zones.length)
			return;
		zoneLayer[(y * getWidth()) + x] = z;
	}

	public void setobs(int x, int y, int t) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight())
			return;
		if (t >= tileset.numobs && t!=0)
			return;
		obsLayer[(y * getWidth()) + x] = (byte) t;
	}
	
	public void settile(int x, int y, int i, int z) { 
		if(i>=this.layers.length) {
			return;
		}
		else {
			this.layers[i].setTile(x,y,z); 
		}
		resetCacheArray();
	}
	
	public int getWidth() {
		if (layers != null && layers[0] != null) {
			return layers[0].width;
		}
		return 0;
	}

	public int getHeight() {
		if (layers != null && layers[0] != null) {
			return layers[0].height;
		}
		return 0;
	}
	
	public String getFilename() {
		return this.filename;
	}

	public String getMapname() {
		return this.mapname;
	}
	
	public static void main(String args[]) throws MalformedURLException {
		/* Save map to clipboard*/ 
		MapVerge m = new MapVerge(new URL("file:///C:\\Temp\\teste.map"),
				new URL("file:///C:\\Temp\\ps1.vsp"));
		  current_map = m;
		  VImage img = new VImage(m.getWidth()*16, m.getHeight()*16);
		  m.render(0, 0, img);
		  img.copyImageToClipboard();
	}

	private Layer createLayerFromImage(URL url, Vsp vsp) {

		Layer l = this.new Layer();
		VImage source = new VImage(url);
		l.width = source.width / 16;
		l.height = source.height / 16;
		
		l.tiledata = new int[l.width * l.height];
		
		for(int posy=0;posy < l.height; posy++) {
			for(int posx=0;posx < l.width; posx++) {	
				System.out.println("Testing (" + posy + ", " + posx + ")");
				for(int i=0; i<vsp.getNumtiles(); i++) {
				//	if(i==0) {System.err.println(vsp.tiles[i].getRGB(0, 0));System.exit(0);}
					int repeated = 0;
					for(int py=0;py<16;py++) {
						for(int px=0;px<16;px++) {
							if(vsp.getTiles()[i].getRGB(px,  py) == source.image.getRGB(posx*16+px, posy*16+py)
							|| vsp.getTiles()[i].getRGB(px,  py) == source.image.getRGB(posx*16+px, posy*16+py) + 16711935	
									) {
								repeated++;
							}
							else
							{
								px=20;py=20;
							}
						}
					}
				
					if(repeated >= 200) {
						System.out.println("Found tile " + i);
						l.tiledata[(posy*l.width)+posx] = i;
						break;
					}
					else
						l.tiledata[(posy*l.width)+posx] = 0;
				}
			}
		}

		return l;
	}

	public Zone[] getZones() {
		return this.zones;
	}

	public Entity[] getEntities() {
		return this.entities;
	}

	public void setRenderstring(String string) {
		this.renderstring = string;
	}

	public String getRenderstring() {
		return this.renderstring;
	}

	public String getScriptZone(int zone) {
		if(zones!=null)
			return zones[zone].script;
		return null;
	}

	public int getPercentZone(int zone) {
		if(zones!=null)
			return zones[zone].percent;
		return 0;
	}

	public int getMethodZone(int zone) {
		if(zones!=null)
			return zones[zone].method;
		return 0;
	}

	public int getNumLayers() {
		return this.layers.length;
	}

	public int getLayerLucent(int layer) {
		return layers[layer].lucent;
	}
	
	
	
	/*
	 * 
	 */
	class Layer {
			
			public static final int DEFAULT_X = 30;
			public static final int DEFAULT_Y = 20;
			
			private String name = "";
			private double parallax_x = 1.0, parallax_y = 1.0;
			private int width = DEFAULT_X, height = DEFAULT_Y; // Unsigned short
			private int lucent = 0; // Unsigned Byte

			private int x_offset, y_offset; // used to account for changing parallax
			
			private int[] tiledata = new int[DEFAULT_X*DEFAULT_Y]; // width * height Unsigned shorts!


			public int getTile(int x, int y) {
				if (x<0 || y<0 || x>=width || y>=height) return 0;
				return tiledata[(y*width)+x];
			}

			public void setTile(int x, int y, int t) {
				if (x<0 || y<0 || x>=width || y>=height) return;
				tiledata[(y*width)+x] = t;
			}
			
			void setParallaxX(double p, int xwin) { // [Rafael, the Esper]: changed to receive xwin
			    // increase the x_offset to the current layer pos given the current parallax
			    x_offset += (int) ((float) xwin * parallax_x);

			    // then reduce it by what the parallax will be
			    x_offset -= (int) ((float) xwin * p);

			    // then we can set the parallax
			    parallax_x = p;
			}

			void setParallaxY(double p, int ywin) { // [Rafael, the Esper]: changed to receive ywin
			    // increase the x_offset to the current layer pos given the current parallax
			    y_offset += (int) ((float) ywin * parallax_y);

			    // then reduce it by what the parallax will be
			    y_offset -= (int) ((float) ywin * p);

			    // then we can set the parallax
			    parallax_y = p;
			}
			
			public String toString() {
				return "Layer " + name + ": (" + parallax_x + ", " + parallax_y + ") (" + width + ", " + height + ") " + lucent + " Data: " + tiledata;
			}
	}
	
	
	class Zone {

		private String name = "";
		private String script = "";
		private int percent=255, delay, method; // Unsigned byte
		
		public String toString() {
			return "Zone " + name + " Act:" + script + " Chance:" + percent + " Delay:" + delay + " Method:" + method;
		}

		
	}
	
}
