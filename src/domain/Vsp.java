package domain;

import static core.Script.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
//import java.awt.Color;
//import java.awt.Graphics2D;
//import java.awt.Rectangle;
//import java.awt.TexturePaint;
//import java.io.FileInputStream;
//import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

//import javax.security.auth.Destroyable;
import persist.ExtendedDataInputStream;
import persist.ExtendedDataOutputStream;
import core.Script;

public class Vsp 
	{
	public static final int  VID_BYTESPERPIXEL	=	3;
	public static final int  VSP_SIGNATURE	=	5264214;
	public static final int  VSP_VERSION	=		6;
	
	public static final int ANIM_MODE_FORWARD   = 0;
	public static final int ANIM_MODE_BACKWARD  = 1;
	public static final int ANIM_MODE_RANDOM    = 2;
	public static final int ANIM_MODE_PINGPONG  = 3;

	public static int mytimer;
	
	// .vsp format: 	https://github.com/Bananattack/v3tiled/wiki/.vsp-file
	
	private int signature = VSP_SIGNATURE;
	private int version = VSP_VERSION;
	private int tileSize = 16;
	private int tileArea = this.tileSize * this.tileSize;	// Krybo added as utility - not needed in vsp file 
	private int format = 1;
	private int compression = 1;
	private int numtiles = 0;
	
	private Animation[] anims = new Animation[0];
	private byte[] obsPixels = new byte[ this.tileArea ]; // width * height * 1 bytes!
	int numobs;
	
	int vadelay[], tileidx[], flipped[];
	
	// [Rafael, the Esper]
	private BufferedImage [] tiles;

	public static void main(String args[]) throws Exception{

			
			/*// EXAMPLE OF ADDING ANIMATIONS PROGRAMATICALLY
			Vsp v = new Vsp(new URL("file:///C:\\JavaRef3\\EclipseWorkspace\\wmap.vsp"));
			
			Animation[] anims = new Animation[42];
			
			int j=0;
			for(int i=45; i<=250;i+=5) {
				Animation a = v.new Animation();
				a.delay = 30;
				a.start = i;
				a.finish = i+4;
				a.mode = 3;
				a.name = "Anim" + ((i-40)/5);
				anims[j++] = a;
				System.out.println(a);
			}
			
			v.anims = anims;
			v.save("C:\\JAVAREF3\\ECLIPSEWORKSPACE\\wmap2.vsp");
			*/

		Vsp v = new Vsp(new URL("file:///C:\\javaref\\workspace\\Phantasy\\src\\ps\\ps1.vsp"));
		
		int NEW_PIXELS = 4;
		byte[] newPixels = new byte[256*(v.numobs+NEW_PIXELS)];
		int pos=0;
		for(int i=0;i<256*v.numobs; i++) {
			newPixels[i] = v.obsPixels[i];
			pos++;
		}
		for(int i=0; i<256; i++) { // Add vertical | left obs
			if( i % 16 == 0)
				newPixels[pos++] = 1;
			else
				newPixels[pos++] = 0;
		}
		for(int i=0; i<256; i++) { // Add vertical | right obs
			if(i%16==15)
				newPixels[pos++] = 1;
			else
				newPixels[pos++] = 0;
		}
		for(int i=0; i<256; i++) { // Add horizontal _ bottom obs
			if(i<16)
				newPixels[pos++] = 1;
			else
				newPixels[pos++] = 0;
		}
		for(int i=0; i<256; i++) { // Add horizontal -- top obs
			if(i>=240)
				newPixels[pos++] = 1;
			else
				newPixels[pos++] = 0;
		}		
		
		v.numobs = v.numobs + NEW_PIXELS;
		v.obsPixels = newPixels;
		
		v.save("C:\\ps1.vsp");
		System.out.println(v.obsPixels.length);
			
			
			
		
	}

	
	
	public Vsp() 
		{		}

		// Krybo (Jan 2016) : sets up a blank vsp, with X number of black tiles  
	public Vsp( int blankTiles )
		{ this.createDummy(blankTiles); }
	
	public Vsp(URL urlpath) {
		try {
			this.load(urlpath.openStream());

		} catch (FileNotFoundException fnfe) {
			error("VSP::FileNotFoundException : " + urlpath);
		} catch (IOException e) {
			error("VSP::IOException : " + e.getMessage());
		}		
	}
	
	
	private void load (InputStream fis) {

		try    
		{
			ExtendedDataInputStream f = new ExtendedDataInputStream(fis);
			
			this.signature = f.readSignedIntegerLittleEndian();
			this.version = f.readSignedIntegerLittleEndian();
			this.tileSize = f.readSignedIntegerLittleEndian();
			this.tileArea = this.tileSize * this.tileSize;
			this.format = f.readSignedIntegerLittleEndian();
			this.numtiles = f.readSignedIntegerLittleEndian();
			this.compression = f.readSignedIntegerLittleEndian();
			
			System.out.println(this.signature + ";"+this.version+";"+this.getNumtiles()+";"+this.compression);
			// Krybo:: e.x.   5264214;6;100;1
			
			byte[] vspdata = f.readCompressedUnsignedShortsIntoBytes(); // tileCount * width * height * 3 bytes!
				
	        int numAnim = f.readSignedIntegerLittleEndian();	// anim.length
	        this.anims = new Vsp.Animation[numAnim];	
	        System.out.println("numAnim = " + numAnim);
	        
	        for(int i=0; i<numAnim; i++) {
	        	Vsp.Animation a = this.new Animation();
	        	a.name = f.readFixedString(256);
	        	a.start = f.readSignedIntegerLittleEndian();
	        	a.finish = f.readSignedIntegerLittleEndian();
	        	a.delay = f.readSignedIntegerLittleEndian();
	        	a.mode = f.readSignedIntegerLittleEndian();
	        	
	        	this.anims[i] = a;
	        }			

	        this.numobs = f.readSignedIntegerLittleEndian();	// obs.length
	        System.out.println("numObs = " + numobs);
	        
			this.obsPixels = f.readCompressedUnsignedShortsIntoBytes();

			/* Obs array DEBUG
			 * for(int i=0;i<obsPixels.length;i++) {
	        	if(i%16==0)
	        		System.out.println();
	        	if(i%256==0)
	        		System.out.println();
	        	System.out.print(obsPixels[i]);
	        }*/
	        
			f.close();
			
			// initialize tile anim stuff
			tileidx = new int[getNumtiles()];
			flipped = new int[getNumtiles()];
			vadelay = new int[numAnim];
			int i;
			for (i=0; i<numAnim; i++)
				vadelay[i]=0;
			for (i=0; i<getNumtiles(); i++)
			{
				flipped[i] = 0;
				tileidx[i] = i;
			}
			mytimer = systemtime;
			
			
			// Get image tiles from pixel array 
			System.out.println("Numtiles: " + getNumtiles() + "(" + vspdata.length + " bytes)");
			this.tiles = f.getBufferedImageArrayFromPixels(vspdata, getNumtiles(), this.tileSize, this.tileSize);
			//for(int x=0; x<tiles.length; x++)
				//Script.graycolorfilter(tiles[x]);
			
			
		 }catch (IOException e) {
			 System.out.println("IOException : " + e);
		 }		

	}

	private void save(String filename) {

		System.out.println("VSP::save at " + filename);
		ExtendedDataOutputStream f = null;
		try {
			OutputStream os = new FileOutputStream(filename);
			f = new ExtendedDataOutputStream(os);
			
			
			f.writeSignedIntegerLittleEndian(this.signature);
			f.writeSignedIntegerLittleEndian(this.version);
			f.writeSignedIntegerLittleEndian(this.tileSize);
			f.writeSignedIntegerLittleEndian(this.format);
			f.writeSignedIntegerLittleEndian(this.getNumtiles());
			f.writeSignedIntegerLittleEndian(this.compression);
			
			System.out.println(this.signature + ";"+this.version+";"+this.getNumtiles()+";"+this.compression);
			byte[] pixels = f.getPixelArrayFromFrames(tiles, tiles.length, this.tileSize, this.tileSize);
			f.writeCompressedBytes(pixels);

			f.writeSignedIntegerLittleEndian(this.anims.length);
	        
	        for(int i=0; i<this.anims.length; i++) {
	        	Animation a = anims[i];
	        	f.writeFixedString(a.name, 256);
	        	f.writeSignedIntegerLittleEndian(a.start);
	        	f.writeSignedIntegerLittleEndian(a.finish);
	        	f.writeSignedIntegerLittleEndian(a.delay);
	        	f.writeSignedIntegerLittleEndian(a.mode);
	        }			

	        f.writeSignedIntegerLittleEndian(this.numobs);
	        
			f.writeCompressedBytes(this.obsPixels);
	
		}
		catch(IOException e) {
			System.err.println("VSP::save " + e.getMessage());
		}
		finally {
			try {
				f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	

	private void createDummy(int blankTiles) 
		{
		this.signature = 5264214;
		this.version = 6;
		this.tileSize = 16;
		this.tileArea = this.tileSize * this.tileSize;
		this.format = 3;
		this.numtiles = blankTiles;
		this.compression = 1;

//        int numAnim = 0;
        this.anims = null;

       // Krybo:  nope, no animation in auto-generated VSP
//        this.anims = new Vsp.Animation[numAnim];	
//        System.out.println("numAnim = " + numAnim);
//        for(int i=0; i<numAnim; i++) {
//        	Vsp.Animation a = this.new Animation();
//        	a.name = f.readFixedString(256);
//        	a.start = f.readSignedIntegerLittleEndian();
//        	a.finish = f.readSignedIntegerLittleEndian();
//        	a.delay = f.readSignedIntegerLittleEndian();
//        	a.mode = f.readSignedIntegerLittleEndian();
//        	
//        	this.anims[i] = a;
//        }			

        	this.numobs = 12;
		this.obsPixels = new byte[ numobs*this.tileSize*this.tileSize ];
		for( int op = 0; op < (numobs*this.tileSize*this.tileSize)-1; op++ )
			{
			this.obsPixels[op] = (byte) 0;
				// Make 2nd obs tile is default full-tile obsturction.
			if( (op >= this.tileArea) && (op < this.tileArea*2) )
				{ this.obsPixels[op] = (byte) 1; }
			// Perhaps add more later.
			}
        
		// initialize tile anim stuff
		tileidx = new int[getNumtiles()];
		flipped = new int[getNumtiles()];
		vadelay = null;

		for( int i=0; i<getNumtiles(); i++ )
			{
			flipped[i] = 0;
			tileidx[i] = i;
			}
		mytimer = systemtime;

			// Setup blank tiles.
		this.tiles = new BufferedImage[this.numtiles];

			// Replicates a blank tile this.numtiles times.
		for( int tn = 0; tn < this.numtiles; tn++ )
			{
				// Krybo: pass-by-reference strangeness would occur if these 
				// VImages are not created separately each pass.
			VImage emptyTile = new VImage(this.tileSize,this.tileSize);
			emptyTile.rectfill(0, 0, this.tileSize, this.tileSize, Color.BLACK );
			this.tiles[tn] = emptyTile.getImage();	
			}
		log("Created Blank VSP with "+Integer.toString(this.numtiles)+" tiles.");

		return;
		}

	public void exportToClipboard(int tiles_per_row) {
		
		int row_size = tiles_per_row*16;
		VImage clipboard = new VImage(row_size, (this.getNumtiles()/tiles_per_row+1) * 16);
//		Font font = new Font("Serif", Font.PLAIN, 7);
		
		for(int i=0; i<this.getNumtiles(); i++) {
			clipboard.blit((i*16)%row_size, i/tiles_per_row*16, getTiles()[i]);
			//if(i%tiles_per_row == 0)
				//clipboard.printstring(0, i/tiles_per_row*16+7, font, Integer.toString(i/tiles_per_row)); 
		}
		clipboard.copyImageToClipboard();

	}

	static void createVspFromImages(VImage[] images) {
		
		// First pixel is default transparent color
		//Color transC = new Color(images[0].image.getRGB(0, 0));
		
		ArrayList<VImage> allTiles = new ArrayList<VImage>();
				
		for(int img=0; img<images.length; img++) {
		
			int posx=0, posy=0;	
			int sizex = images[img].width;
			int sizey = images[img].height;
			System.out.println("Analysing image " + img);
			for(int j=0; j<sizey/16;j++) {
				for(int i=0; i<sizex/16;i++) {
					VImage newTile = new VImage(16, 16);
					newTile.grabRegion(posx, posy, posx+16, posy+16, 0, 0, images[img].image);					
					posx+=16;

					// Checks for repeated tile 
					/*int repeated = 0;
					for(BufferedImage bi: allTiles) {
						for(int py=0;py<16;py++)
							for(int px=0;px<16;px++)
								if(bi.getRGB(px,  py) == newTile.getRGB(px, py)) {
									repeated++;
								}
								else
									{px=20;py=20;}
					}
					if(repeated < 256)*/
						allTiles.add(newTile);
					/*else
						allTiles.add(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
					repeated = 0;*/
				}
				posy+=16;
				posx=0;
			}				
			
			
		}
		
		VImage clipboard = new VImage(512, (allTiles.size()/32+1) * 16);
		Font font = new Font("Serif", Font.PLAIN, 7);
		
		Vsp v = new Vsp();
		v.tiles = new BufferedImage[allTiles.size()];
		System.out.println("Got " + allTiles.size() + " tiles");
		for(int i=0; i<allTiles.size(); i++) {
			v.getTiles()[i] = allTiles.get(i).image;
			clipboard.blit((i*16)%512, i/32*16, v.getTiles()[i]);
			clipboard.printString((i*16)%512, i/32*16+16, font, Integer.toString(i)); 
		}
		clipboard.copyImageToClipboard();
		
		v.numtiles = v.tiles.length;
		v.obsPixels = new byte[512];
		for(int i=0; i<512; i++) {
			v.obsPixels[i] = (byte) ((i >= 256) ? 1: 0);
		}
		v.save("C:\\TEMP.VSP");
	}
	
	public int getNumtiles() {
		return numtiles;
	}
	
		// Krybo (Jan.2016) : 
		// opens the internal size of tiles in pixels to public
		//  Needed this for some display functionality
	public int getTileSquarePixelSize()
		{	return( this.tileSize );  }

	public BufferedImage [] getTiles() {
		return tiles;
	}

	/* Krybo (Jan.2016) : sets one whole individual tile */
	
	public boolean modifyTile( int tileIdx, BufferedImage newTileImage )
		{
		if( newTileImage.getWidth() != this.tileSize ) 	{ return(false); }
		if( newTileImage.getHeight() != this.tileSize ) 	{ return(false); }
		if( tileIdx < 0 || tileIdx > this.numtiles )			{ return(false); }
		tiles[tileIdx] = newTileImage;
		return(true);
		}
	public boolean modifyTile( int tileIdx, VImage newTileImage )
		{	return(this.modifyTile(tileIdx, newTileImage.getImage() ) );	}

	/* Krybo (Jan.2016) : modify pixels within an existing tile */
	
	public boolean modifyTile( int tileNum, int x1, int y1,  
			int red256, int green256, int blue256 )
			{
			if( tileNum >= this.numtiles )  { return(false); }
			if( x1 < 0 || x1 > this.tileSize )  { return(false); }
			if( y1 < 0 || y1 > this.tileSize )  { return(false); }

			float R = (float) red256 / 256.0f; 
			float G = (float) green256 / 256.0f;
			float B = (float) blue256 / 256.0f;
			Color C = new Color(R, G, B, 1.0f );

			VImage dat = new VImage(this.tileSize, this.tileSize);
			dat.setImage(this.tiles[tileNum]);
			dat.setPixel(x1, y1, C );
			tiles[tileNum] = dat.getImage();
			
			return(true);
			}
	
	/* Krybo (Jan.2016) */
	
	public int getNumObsTiles()
		{	return(this.numobs);	}
	
	boolean GetObs(int t, int x, int y)
		{
		if (t==0) return false;
		if (t>=numobs || t<0) return true;
		if (x<0 || y<0 || x>15 || y>15) return true;
		return obsPixels[(t*this.tileArea)+(y*this.tileSize)+x] == 0 ? false: true;
		}
	
	public boolean UpdateAnimations()
	{
		boolean animated = false;
		while (mytimer < systemtime)
		{
			animated = AnimateTiles();
			mytimer++;
		}
		return animated;
	}
	public void Blit(int x, int y, int index, VImage dest)
	{
		// tileidx[index] = the actual pointer to a tile, can change due to VSP animation
		if (index >= getNumtiles() || tileidx[index] >= getNumtiles()) {
			System.err.printf("VSP::BlitTile(), tile %d exceeds %d", index, getNumtiles());
			return;
		}
		//if(systemtime%3!=0) 
		dest.blit(x, y, current_map.getTileSet().getTiles()[tileidx[index]]);
		//dest.g.drawImage(current_map.tileset.tiles[index], x, y, Color.BLACK, null);
		// Faster, but doesn't support animations
		/*Graphics2D g2 = (Graphics2D) dest.g;
		g2.setPaint(new TexturePaint(current_map.tileset.tiles[index], new Rectangle(x,y,16,16)));
		g2.fillRect(x,y,16,16);*/
		
		
	}

	public void TBlit(int x, int y, int index, VImage dest)
	{
		/*while (mytimer < systemtime)
		{
			AnimateTiles();
			mytimer++;
		}*/
		//if (index >= numtiles) err("VSP::BlitTile(), tile %d exceeds %d", index, numtiles);
		if (index >= getNumtiles() || tileidx[index] >= getNumtiles()) {
			System.err.printf("VSP::TBlitTile(), tile %d exceeds %d", index, getNumtiles());
			return;
		}
		
		dest.tblit(x, y, current_map.getTileSet().getTiles()[tileidx[index]]);
		//dest.g.drawImage(current_map.tileset.tiles[index], x, y, null);
	}

	public void BlitObs(int x, int y, int index, VImage dest)
		{
		if (index >= numobs) return;
		//[Rafael, the Esper] char c[] = (char) obs + (index * 256);
		//[Rafael, the Esper] int white = MakeColor(255,255,255);
		int destOffsetX = 0;
		int destOffsetY = 0;
		byte targetPixel = 0;
		for (int yy=0; yy< this.tileSize; yy++ )
			for (int xx=0; xx<this.tileSize; xx++)
				{
				destOffsetX = x+xx;
				destOffsetY = y+yy;
				targetPixel = this.obsPixels[this.tileArea*index + this.tileSize*yy + xx]; 
				if( targetPixel != 0 )
					{
					dest.setPixel(destOffsetX, destOffsetY, Color.WHITE );
					}
				}		// Krybo (Jan.2016): This now operates as intended.
			// ; [Rafael, the Esper] if (c[(yy*16)+xx]>0) PutPixel(x+xx, y+yy, white, dest);
		}

	void AnimateTile(int i, int l)
	{
		switch (anims[i].mode)
		{
		    case ANIM_MODE_FORWARD:
				if (tileidx[l]<anims[i].finish) tileidx[l]++;
	            else tileidx[l]=anims[i].start;
	            break;
			case ANIM_MODE_BACKWARD:
				if (tileidx[l]>anims[i].start) tileidx[l]--;
	            else tileidx[l]=anims[i].finish;
	            break;
			case ANIM_MODE_RANDOM:
				tileidx[l]=Script.random(anims[i].start, anims[i].finish);
	            break;
			case ANIM_MODE_PINGPONG:
				if (flipped[l]>0)
	            {
					if (tileidx[l]!=anims[i].start) tileidx[l]--;
					else { tileidx[l]++; flipped[l]=0; }
	            }
	            else
	            {
					if (tileidx[l]!=anims[i].finish) tileidx[l]++;
					else { tileidx[l]--; flipped[l]=1; }
	            }
				break;
		}
	}

	boolean AnimateTiles()
		{
		boolean animated = false;
			// Krybo (Jan.2016):  auto-gen maps will not have any of these.  
		if( anims == null || anims.length == 0 )
			{ return(false); }

		for (int i=0; i<anims.length; i++)
		{
			if(anims[i] == null || vadelay==null)		// [Rafael, the Esper]
				return animated;
			
			if ((anims[i].delay>0) && (anims[i].delay<vadelay[i]))
			{
				vadelay[i]=0;
				animated = true;
				for (int l=anims[i].start; l<=anims[i].finish; l++)
					AnimateTile(i,l);
			}
			vadelay[i]++;
		}
		return animated;
	}

	void ValidateAnimations()
	{
		for (int i=0; i<anims.length; i++)
			if (anims[i].start<0 || anims[i].start>=getNumtiles() || anims[i].finish<0 || anims[i].finish>=getNumtiles())
				System.err.printf("VSP::ValidateAnimations() - animation %d references out of index tiles", i);
	}
	
	



	public class Animation {
		
		public String name = "";
		public int start = 0, finish = 0;
		
		public int delay, mode; 
		
		public String toString() {
			return "Animation: " + name + "; startTile:" + start + "; endTile:" + finish + "; delay:" + delay + "; mode: " + mode;
		}

	}
	
	
}

