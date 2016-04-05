package domain;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
//import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
//import java.awt.geom.AffineTransform;
//import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
//import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import core.VergeEngine;
import persist.PCXReader;
import static core.Script.*;


public class VImage implements Transferable 
	{
	public BufferedImage image;
	public Graphics2D g;
	
	public int width, height;
	
	public VImage(int x, int y) {
		this.width = x;
		this.height = y;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gs.getDefaultConfiguration();
		image = gc.createCompatibleImage(x, y, Transparency.TRANSLUCENT);
		//image = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D)image.getGraphics();
		}
	// Krybo (Feb.2016)  : builds a new VImage & fills it with solid Color c
	public VImage(int x, int y, Color c ) 
		{
		this(x,y);
		this.rectfill(0, 0, x, y, c);
		}
			// Krybo (2014-10-03) Essentially a copy method.
	public VImage( VImage existingVImage ) 
		{
		this.width = existingVImage.getWidth();
		this.height = existingVImage.getHeight();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gs = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gs.getDefaultConfiguration();
		image = gc.createCompatibleImage( existingVImage.getWidth(), 
				existingVImage.getHeight(), Transparency.TRANSLUCENT);
		//image = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
		g = (Graphics2D)image.getGraphics();
		g.drawImage(existingVImage.getImage(), 0, 0, null );
		}	


	 public VImage(URL url, boolean transparent) {
		  try {
			  if(url==null) {
				  System.err.println("Unable to find image from URL " + url);
				  return;
			  }
			  if(url.getFile().toUpperCase().endsWith("PCX")) {
				  image = PCXReader.loadImage(url.openStream());
			  } else
			  {			  
				  image = ImageIO.read(url);
			  }
		  } catch (IOException e) {
			  System.err.println("Unable to read image from URL " + url);
		  }
		  this.width = image.getWidth();
		  this.height = image.getHeight();

		  // Make death magenta = transparent
		  if(transparent) {
			  Image img = makeColorTransparent(image, new Color(255, 0, 255));
			  this.image = imageToBufferedImage(img);
		  }
		  
		  g = (Graphics2D)image.getGraphics();
	 }
	 	// A constructor with configurable transparent color
	 public VImage(URL url, int transR, int transG, int transB ) 
		 {
		  try {
			  if(url==null) {
				  System.err.println("Unable to find image from URL " + url);
				  return;
			  }
			  if(url.getFile().toUpperCase().endsWith("PCX")) {
				  image = PCXReader.loadImage(url.openStream());
			  } else
			  {			  
				  image = ImageIO.read(url);
			  }
		  } catch (IOException e) {
			  System.err.println("Unable to read image from URL " + url);
		  }
		  this.width = image.getWidth();
		  this.height = image.getHeight();
	
		  Image img = makeColorTransparent( image, 
				  new Color(transR, transG, transB ) );
		  this.image = imageToBufferedImage(img);
		  g = (Graphics2D)image.getGraphics();
		 }
	 
	 public VImage(URL url) { // Rafael: per default, all images are loaded as transparent
		 this(url, true);
	 }
	
	public BufferedImage getImage() {
		return this.image;
	}
	
		// Krybo (2014-09-20) this very well could NOT work as expected
		// Use at ones own peril
	public boolean setImage( BufferedImage i )
		{
		boolean noError = true;
		try {
			image = i;
			width = i.getWidth();
			height = i.getHeight();
			g = (Graphics2D)i.getGraphics();
			}
		catch(Exception e) 
			{
			noError = false;
			System.err.println(" setImage : "+e.getMessage() );
			e.printStackTrace();
			}
			
		return(noError);
		}
    
	public int getWidth() {
		return this.width;
	}
	public int getHeight() {
		return this.height;
	}

	// See http://wiki.java.net/bin/view/Games/LoadingSpritesWithImageIO
   private static BufferedImage imageToBufferedImage(Image image) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice ().getDefaultConfiguration();
        BufferedImage dst = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), Transparency.TRANSLUCENT);
        Graphics2D g2d = dst.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        // Copy image
        g2d.drawImage(image,0,0,null);
        g2d.dispose();
        return dst;
    }

	   //http://stackoverflow.com/questions/665406/how-to-make-a-color-transparent-in-a-bufferedimage-and-save-as-png 
	   public static Image makeColorTransparent(BufferedImage im, final Color color) {
	        ImageFilter filter = new RGBImageFilter() {

	                // the color we are looking for... Alpha bits are set to opaque
	                public int markerRGB = color.getRGB() | 0xFF000000;

	                public final int filterRGB(int x, int y, int rgb) {
	                        if ((rgb | 0xFF000000) == markerRGB) {
	                                // Mark the alpha bits as zero - transparent
	                                return 0x00FFFFFF & rgb;
	                        } else {
	                                // nothing to do
	                                return rgb;
	                        }
	                }
	        };

	        ImageProducer ip = new FilteredImageSource(
	     		   im.getSource(), filter);
	        return Toolkit.getDefaultToolkit().createImage(ip);
	    }

		// Fast copy of a BufferedImage 
	   // http://stackoverflow.com/questions/2825837/java-how-to-do-fast-copy-of-a-bufferedimages-pixels-unit-test-included
	   public static void copySrcIntoDstAt(final BufferedImage src, final BufferedImage dst, final int dx, final int dy) 
	   {
		    int[] srcbuf = ((java.awt.image.DataBufferInt) src.getRaster().getDataBuffer()).getData();
		    int[] dstbuf = ((java.awt.image.DataBufferInt) dst.getRaster().getDataBuffer()).getData();
		    int width = src.getWidth();
		    int height = src.getHeight();
		    int dstoffs = dx + dy * dst.getWidth();
		    int srcoffs = 0;
		    for (int y = 0 ; y < height ; y++ , dstoffs+= dst.getWidth(), srcoffs += width ) {
		        System.arraycopy(srcbuf, srcoffs , dstbuf, dstoffs, width);
		    }
		}

	   // Transfer to Clipboard
	   // http://elliotth.blogspot.com/2005/09/copying-images-to-clipboard-with-java.html
	   public void copyImageToClipboard() {
	        //VImage imageSelection = new VImage(image.getWidth(null), image.getHeight(null));
	        //imageSelection.image = (BufferedImage) image;
	        Toolkit toolkit = Toolkit.getDefaultToolkit();
	        toolkit.getSystemClipboard().setContents(this, null);
	    }
	    
	   public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
	        if (flavor.equals(DataFlavor.imageFlavor) == false) {
	            throw new UnsupportedFlavorException(flavor);
	        }
	        return image;
	    }
	   
	    public boolean isDataFlavorSupported(DataFlavor flavor) {
	        return flavor.equals(DataFlavor.imageFlavor);
	    }
	   
	    public DataFlavor[] getTransferDataFlavors() {
	        return new DataFlavor[] {
	            DataFlavor.imageFlavor
	        };
	    }	   

		// TODO Create function drawRoundRect, similar to Graphics

		// Render the map and the entities to this VImage
	    public void render() 
		    {
	    		// Krybo: June-2015 : Clearing every cycle is REQUIRED.   otherwise
	    		//   entities were leaving trails in transparent areas of the map
	    		//   and in some instances map did not "appear" to scroll.
	    		//  * Seems to have a negligable affect on performance given this 
	    		//     is done every loop cycle.   This single line ended both problems
	    		this.paintBlack(1.0f);
	    		
			VergeEngine.TimedProcessEntities(); 
			VergeEngine.RenderMap(this);
		    }
	    
		//VI.f. Graphics Functions
		/*static void AdditiveBlit(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			AdditiveBlit(x, y, s, d);
		}*/
		public void alphablit(int x, int y, VImage src, VImage alpha) {
			// [Rafael, the Esper] TODO Implement
			//AlphaBlit(x, y, s, a, d);
			//error("Non implemented function");
			this.tblit(x, y, src);
		}
		
		public void blitentityframe(int x, int y, int e, int f) {
			if (current_map==null || e<0 || e >= numentities) return;
			entity.get(e).chr.render(x, y, f, this);
		}
		
		public void blitentityframe(int x, int y, CHR chr, int f) {
			if (current_map==null) return;
			chr.render(x, y, f, this);
		}

		// Overkill (2007-08-25): src and dest were backwards. Whoops!
		public void blitlucent(int x, int y, int lucent, VImage src) {
			int oldalpha = currentLucent;
			setlucent(lucent);
			this.blit(x, y, src);
			setlucent(oldalpha);
		}
		
		public void blitTile(int x, int y, int t) {
			if (current_map != null) {
				current_map.getTileSet().UpdateAnimations();
				current_map.getTileSet().Blit(x, y, t, this);
			}
		}

		public void blit(int x, int y, VImage src) {
			this.blit(x, y, src.image);
		}
		public void blit(int x, int y, Image src) { // [Rafael, the Esper] Always opaque
			if(currentLucent < 255) {
				Graphics2D g2d = (Graphics2D) getImage().getGraphics();
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(currentLucent)/255));
				g2d.drawImage(src, x, y, Color.BLACK, null);
			}
			else {
				this.g.drawImage(src, x, y, Color.BLACK, null);
			}
		}

		
		
		public void tblit(int x, int y, VImage src) {
			this.tblit(x, y, src.image);
		}
		public void tblit(int x, int y, Image src) {
			if(currentLucent < 255) {
				Graphics2D g2d = (Graphics2D) getImage().getGraphics();
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(currentLucent)/255));
				g2d.drawImage(src, x, y, null);
			}
			else {
				this.g.drawImage(src,x,y,null);
			}
		}

			// Krybo (2014-10-04)  Some independance from 
			// the currentLucent Script variable was needed, so overloaded
		public void tblit(int x, int y, Image src, float alphaBlend ) 
			{
			if( alphaBlend < 0.0f ) { alphaBlend = 0.0f; }
			if( alphaBlend > 1.0f ) { alphaBlend = 1.0f; }
			if( alphaBlend < 1.0f ) 
				{
				Graphics2D g2d = (Graphics2D) getImage().getGraphics();
				g2d.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, alphaBlend ) );
				g2d.drawImage(src, x, y, null);
				g2d.dispose();
				}
			else 
				{
				this.g.drawImage(src,x,y,null);
				}
			return;
			}
		
		/*static void BlitWrap(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			BlitWrap(x, y, s, d);
		}*/
		

		public VImage duplicateImage() { // TODO Test
			VImage img = new VImage(this.image.getWidth(), this.image.getHeight());
			img.g.drawImage(this.getImage(), 0, 0, null);
			return img;
		}
		
		public enum FlipType{FLIP_HORIZONTALLY, FLIP_VERTICALLY, FLIP_BOTH};
		
		public void flipBlit(int x, int y, FlipType type, VImage src) {
			/*
			AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
	        tx.translate(-src.width, 0);
	        AffineTransformOp op = new AffineTransformOp(tx, 
	                                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	        VImage flippedImage = this.duplicateimage();
	        //BufferedImage flippedImage =  new BufferedImage(src.width,src.height, BufferedImage.TYPE_INT_RGB);
	        //flippedImage = op.filter(src.image, null);		
	        flippedImage.image = op.filter(flippedImage.image, null);
	        //blit(x, y, flippedImage, dest.image);
	       	this.blit(x,y,flippedImage);*/

			if(type == FlipType.FLIP_HORIZONTALLY) {
				this.g.drawImage(src.image, src.getWidth()+x, y, -src.getWidth(), src.getHeight(), null);
				//this.blit(x, y, flipimage(0,0,src.image));
			}
			else {
				System.err.println("Not supported yet!");
			}
		}
		
		public static BufferedImage flipImage(int x, int y, BufferedImage src) {
			BufferedImage flippedImage = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for(int j=0;j<src.getHeight(); j++)
				for(int i=0;i<src.getWidth(); i++)
					flippedImage.setRGB(i, j, src.getRGB(src.getWidth()-i-1, j)); // Flip horizontally
					//flippedImage.setRGB(i, j, src.getRGB(src.getWidth()-i-1, src.getHeight()-j-1)); // Flip Both
			return flippedImage;
		}
		

		
		/*static int GetImageFromClipboard() {
			image *t = clipboard_getImage();
			if (!t) return 0;
			else return HandleForImage(t);
		}*/
		
		/*public static int GetPixel(int x, int y, VImage src) {
			WritableRaster wr = src.image.getRaster();
			wr.getPixel(x, y, arg2);
			
			return ReadPixel(x, y, s);
		}*/
		

		
		/*static void GrabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);

			int dcx1, dcy1, dcx2, dcy2;
			d.GetClip(dcx1, dcy1, dcx2, dcy2);

			if (sx1>sx2) SWAP(sx1, sx2);
			if (sy1>sy2) SWAP(sy1, sy2);
			int grabwidth = sx2 - sx1;
			int grabheight = sy2 - sy1;
			if (dx+grabwidth<0 || dy+grabheight<0) return;
			d.SetClip(dx, dy, dx+grabwidth, dy+grabheight);
			Blit(dx-sx1, dy-sy1, s, d);

			d.SetClip(dcx1, dcy1, dcx2, dcy2);
		}*/
		
		/** This extremely powerful function will allow you to take an image, define a rectangle 
		 * within it, and get an image handle referencing that rectangle within the original image. 
		 * xofs, yofs, width, height indicate the position and dimensions of the rectangle within 
		 * the source image. Clipping rectangles for the two images are completely independent. 
		 * Rendering into one will render into the other.* If that is what you want to do, then this 
		 * is the function you want to use. 
		 * @deprecated Use {@link #imageShell(int,int,int,int)} instead
		 */
		public VImage setc(int x, int y, int w, int h) {
			return imageShell(x, y, w, h);
			}


		/** This extremely powerful function will allow you to take an image, define a rectangle 
		 * within it, and get an image handle referencing that rectangle within the original image. 
		 * xofs, yofs, width, height indicate the position and dimensions of the rectangle within 
		 * the source image. Clipping rectangles for the two images are completely independent. 
		 * Rendering into one will render into the other.* If that is what you want to do, then this 
		 * is the function you want to use. 
		 */
		public VImage imageShell(int x, int y, int w, int h) {
			if (w+x > this.width || y+h > this.height)
				System.err.printf(
					"ImageShell() - Bad arguments. x/y+w/h greater than original image dimensions\n\nx:%d,w:%d (%d),y:%d,h:%d (%d), orig_x:%d, orig_y:%d",
					x,w,x+w,y,h,y+h,this.width,this.height
				);

			VImage dst = new VImage(w, h);
			//dst.delete_data();
			//dst.shell = true;

			//dst.data = ((quad *)src.data + (y*src.pitch)+x);
			//dst.pitch = src.pitch;
			
			// TODO Implement this mechanism!
			error("Non implemented function: imageshell");
			return dst;
		}
		
		public void line(int x1, int y1, int x2, int y2, Color c) { // [Rafael, the Esper]
			this.g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent));
			this.g.drawLine(x1, y1, x2, y2);
		}
		
		
		/**  Same as line()  - but uses the alpha of the given Color.
		 * 
		 * @param x1	screen pixel location   (top left X)
		 * @param y1	screen pixel location   (top left Y)
		 * @param x2	screen pixel location   (bottom right X)
		 * @param y2	screen pixel location   (bottom right Y)
		 * @param c		Color object
		 */
		public void lineTrans( int x1, int y1, int x2, int y2, Color c) 
			{
			Composite save = this.g.getComposite();
			this.g.setComposite( AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER )  );
			this.g.setColor( c );
			this.g.drawLine(x1, y1, x2, y2);
			this.g.setComposite(save);
			}
	
		/*static void Mosaic(int xgran, int ygran, int dst) {
			image *dest = ImageForHandle(dst);
			Mosaic(xgran, ygran, dest);
		}*/
		
		public void rotscale(int x, int y, int angle, int scale, VImage src) {
			//TODO [Rafael, the Esper] Implement
			this.blit(x, y, src);
			//RotScale(x, y,  angle*(float)3.14159/(float)180.0, scale/(float)1000.0, s, d);
		}
		public void scaleblit(int x, int y, int dw, int dh, VImage src) {
			//ScaleBlit(x, y, dw, dh, s, d);
			//this.blit(x, y, src); // TODO [Rafael, the Esper] Implement scaling
			this.g.drawImage(src.getImage(), x, y, x+dw, y+dh, 0, 0, src.getWidth(), src.getHeight(), null);
		}
		
		/* Draws a scaled image. A bit more complex than the other blitters to use. 
		 * The x,y values give the upper-left corner of where the blit will start. 
		 * iw,ih are the width and height of the *source* image. 
		 * dw, dh are the width and height that the image should appear on screen. 
		 * (ie, the end result bounding box of the image would be, x, y, x+dw, y+dh) 
		 * Image is, as with the other blit routines, a pointer to the image graphic.
		 */
		public void scalesprite(int x, int y, int iw, int ih, int dw, int dh) {
			screen.g.drawImage(this.getImage(), x, y, x+dw, y+dh, 0, 0, iw, ih, null);
		}
		
		
		/**
		 * @deprecated Use {@link #setClip(int,int,int,int)} instead
		 */
		public void setclip(int x1, int y1, int x2, int y2) {
			setClip(x1, y1, x2, y2);
			}


		public void setClip(int x1, int y1, int x2, int y2) {
			//img.SetClip(x1, y1, x2, y2);
			// TODO [Rafael, the Esper] Implement this mechanism in VImage
			//error("Non implemented function: setclip");
		}

		public void silhouette(int x, int y, Color c, VImage src) {
			int x1,x2,y1,y2;
			
			//WritableRaster wr = dst.getImage().getRaster();
			x1 = y1 = 0;
			x2 = src.width;
			y2 = src.height;

			for (int j=y1; j<y2; j++)
			{
				for(int i=x1;i<x2;i++) {
					if(src.getImage().getRGB(i, j)==transcolor || src.getImage().getRGB(i, j)==0) // black 
						this.setPixel(x+i, y+j, new Color(0,0,0,0));
					else
						this.setPixel(x+i, y+j, c);
				}
			}		
		}
		/*
		static void SubtractiveBlit(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			SubtractiveBlit(x, y, s, d);
		}
		static void TAdditiveBlit(int x, int y, int src ,int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			TAdditiveBlit(x, y, s, d);
		}*/
		
		/**
		 * @deprecated Use {@link #grabRegion(int,int,int,int,int,int,VImage)} instead
		 */
		public void grabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, VImage src) {
			grabRegion(sx1, sy1, sx2, sy2, dx, dy, src);
			}

		public void grabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, VImage src) {
			this.grabRegion(sx1, sy1, sx2, sy2, dx, dy, src.image);
		}
		
		/**
		 * @deprecated Use {@link #grabRegion(int,int,int,int,int,int,BufferedImage)} instead
		 */
		public void grabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, BufferedImage src) {
			grabRegion(sx1, sy1, sx2, sy2, dx, dy, src);
			}

		public void grabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, BufferedImage src) {
			
				// Getclip
				//int dcx1 = dst.cx1;
				//int dcy1 = dst.cy1;
				//int dcx2 = dst.cx2;
				//int dcy2 = dst.cy2;
			
				if (sx1>sx2) { // swap sx1, sx2
					int temp = sx1;
					sx1 = sx2;
					sx2 = temp;
				}
				if (sy1>sy2) { // swap sy1, sy2
					int temp = sy1;
					sy1 = sy2;
					sy2 = temp;				
				}

				if( src == null ) 
					{
					System.err.println("src image to VImage.grabRegion is null.");
					return;
					}
				
				Color color = null;
				for(int j=0; j<sy2-sy1; j++)
				for(int i=0; i<sx2-sx1; i++) {
					if(sx1+i >= src.getWidth() || sy1+j >= src.getHeight()
					   || dx+i >= this.getWidth() || dy+j >= this.getHeight())		
						break;
					color = new Color(src.getRGB(sx1+i, sy1+j));
					if(color.getRed() + color.getGreen() + color.getBlue() == 0) // TODO [Rafael, the Esper] Probably move it to tgrabregion?
						color = new Color(0,0,0,0); //color.getRed(), color.getGreen(), color.getBlue(), 0);
					this.setPixel(i+dx, j+dy, color);

				}
				/*int grabwidth = sx2 - sx1;
				int grabheight = sy2 - sy1;
				if (dx+grabwidth<0 || dy+grabheight<0) return;
				dst.SetClip(dx, dy, dx+grabwidth, dy+grabheight);
				Blit(dx-sx1, dy-sy1, src, dst);
			
				dst.SetClip(dcx1, dcy1, dcx2, dcy2);*/
		}

		public void tgrabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, Color transC, VImage src) {
			this.tgrabRegion(sx1, sy1, sx2, sy2, dx, dy, transC, src.image);
		}	
		
		/**
		 * @deprecated Use {@link #tgrabRegion(int,int,int,int,int,int,Color,BufferedImage)} instead
		 */
		public void tgrabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, Color transC, BufferedImage src) {
			tgrabRegion(sx1, sy1, sx2, sy2, dx, dy, transC, src);
			}


		public void tgrabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, Color transC, BufferedImage src) {
			
			if (sx1>sx2) { // swap sx1, sx2
				int temp = sx1;
				sx1 = sx2;
				sx2 = temp;
			}
			if (sy1>sy2) { // swap sy1, sy2
				int temp = sy1;
				sy1 = sy2;
				sy2 = temp;				
			}
			
			Color color = null;
			for(int j=0; j<sy2-sy1; j++)
			for(int i=0; i<sx2-sx1; i++) {
				if(sx1+i >= src.getWidth() || sy1+j >= src.getHeight()
				   || dx+i >= this.getWidth() || dy+j >= this.getHeight())		
					break;
				color = new Color(src.getRGB(sx1+i, sy1+j));
				if(color.equals(transC)) 
					color = new Color(255,0,255,0); //color.getRed(), color.getGreen(), color.getBlue(), 0);
				this.setPixel(i+dx, j+dy, color);

			}	
		}
	
		public void setPixel(int x, int y, Color color) {
			this.image.setRGB(x, y, color.getRGB());
		}	
		
		/**
		 * @deprecated Use {@link #readPixel(int,int)} instead
		 */
		public  int readpixel(int x, int y)
			 { return readPixel(x, y); }



		public int readPixel(int x, int y) {
			if(this.image != null) {
				return this.image.getRGB(x, y);
			}
			return 0;
		}
		
		public void changeColor(Color src, Color dest) {
			for(int y=0;y<height;y++) {
				for(int x=0;x<width;x++) {
					if(readPixel(x, y) == src.getRGB()) {
						setPixel(x, y, dest);
					}
				}
			}
		}
		
		
		// Overkill (2007-08-25): src and dest were backwards. Whoops!
		/**
		 * @deprecated Use {@link #tblitLucent(int,int,int,VImage)} instead
		 */
		public void tblitlucent(int x, int y, int lucent, VImage src) {
			tblitLucent(x, y, lucent, src);
			}


		// Overkill (2007-08-25): src and dest were backwards. Whoops!
		public void tblitLucent(int x, int y, int lucent, VImage src) {
			int oldalpha = currentLucent;
			setlucent(lucent);
			this.tblit(x, y, src);
			setlucent(oldalpha);
		}

		/**
		 * @deprecated Use {@link #tblitTile(int,int,int)} instead
		 */
		public void tblittile(int x, int y, int t) {
			tblitTile(x, y, t);
			}
		/*
		static void TGrabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
		
			int dcx1, dcy1, dcx2, dcy2;
			d.GetClip(dcx1, dcy1, dcx2, dcy2);
		
			if (sx1>sx2) SWAP(sx1, sx2);
			if (sy1>sy2) SWAP(sy1, sy2);
			int grabwidth = sx2 - sx1;
			int grabheight = sy2 - sy1;
			if (dx+grabwidth<0 || dy+grabheight<0) return;
			d.SetClip(dx, dy, dx+grabwidth, dy+grabheight);
			TBlit(dx-sx1, dy-sy1, s, d);
		
			d.SetClip(dcx1, dcy1, dcx2, dcy2);
		}*/


		public void tblitTile(int x, int y, int t) {
			if (current_map!=null) 
				current_map.getTileSet().TBlit(x, y, t, this);
		}
		/*
		static void TGrabRegion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);

			int dcx1, dcy1, dcx2, dcy2;
			d.GetClip(dcx1, dcy1, dcx2, dcy2);

			if (sx1>sx2) SWAP(sx1, sx2);
			if (sy1>sy2) SWAP(sy1, sy2);
			int grabwidth = sx2 - sx1;
			int grabheight = sy2 - sy1;
			if (dx+grabwidth<0 || dy+grabheight<0) return;
			d.SetClip(dx, dy, dx+grabwidth, dy+grabheight);
			TBlit(dx-sx1, dy-sy1, s, d);

			d.SetClip(dcx1, dcy1, dcx2, dcy2);
		}*/

		public void rect(int x1, int y1, int x2, int y2, int c) {
			this.rect(x1, y1, x2, y2, palette.getColor(c, currentLucent));
		}
		public void rect(int x1, int y1, int x2, int y2, Color c) 
			{ // [Rafael, the Esper]
			if( c == null ) 	{ c = Color_DEATH_MAGENTA; }
			this.g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent));
			if(x1>x2) {	int temp = x1;	x1 = x2;	x2 = temp;	} // swap x1,x2
			if(y1>y2) {	int temp = y1;	y1 = y2;	y2 = temp;	} // swap y1,y2
			this.g.drawRect(x1, y1, x2-x1, y2-y1);
		}
		
		/** same as rect() - but uses the translucency of the given color 
		 * Krybo(Apr.2016) */
		public void rectTrans(int x1, int y1, int x2, int y2, Color c) 
			{ 
			if( c == null ) 	{ c = Color_DEATH_MAGENTA; }
			Composite save = this.g.getComposite();
			this.g.setComposite( AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER )  );
			this.g.setColor( c );
			if(x1>x2) {	int temp = x1;	x1 = x2;	x2 = temp;	} // swap x1,x2
			if(y1>y2) {	int temp = y1;	y1 = y2;	y2 = temp;	} // swap y1,y2
			this.g.drawRect(x1, y1, x2-x1, y2-y1);
			this.g.setComposite(save);
			}
		


		public void rectfill(int x1, int y1, int x2, int y2, int c) {
			if(c==transcolor) {
				Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
				g2d.setColor(new Color(0, 0, 0, 0));
				g2d.fillRect(x1, y1, x2, y2);			
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			}
			else
				this.rectfill(x1, y1, x2, y2, palette.getColor(c, currentLucent));
		}
		
		public void rectfill(int x1, int y1, int x2, int y2, Color c) 
			{ 	// [Rafael, the Esper]
			if( c == null )   
				{ c = Color_DEATH_MAGENTA; }
			if(c.getAlpha()==255)
				c = new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent);

			this.g.setColor(c);
			if(x1>x2) {	int temp = x1;	x1 = x2;	x2 = temp;	} // swap x1,x2
			if(y1>y2) {	int temp = y1;	y1 = y2;	y2 = temp;	} // swap y1,y2
			this.g.fillRect(x1, y1, x2-x1, y2-y1);
		}
		
		// Note: different from Java fillOval, the circle is centered in (x1, y1)
		public void circle(int x1, int y1, int xr, int yr, Color c, VImage dst) { // [Rafael, the Esper]
			dst.g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent));
			dst.g.drawOval(x1-xr, y1-yr, xr*2, yr*2);
		}
		
		/**  Draws a circle using the translucency of the argument color.
		 *     ignoring the lucent of the VImage.
		 *   (Krybo Apr.2016)
		 * @param x1	The X pixel center point
		 * @param y1	The Y pixel center point
		 * @param xr		X radius of the circle.
		 * @param yr		Y Radius of the circle.
		 * @param c		Color object, include alpha here.
		 * @param dst	A target VImage (?)
		 */
		public void circleTrans(int x1, int y1, int xr, int yr, 
				Color c, VImage dst) 
			{
			Composite save = this.g.getComposite();
			this.g.setComposite( AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER )  );
			dst.g.setColor( c );
			dst.g.drawOval(x1-xr, y1-yr, xr*2, yr*2);
			dst.g.setComposite(save);
			}

		

		public void circlefill(int x1, int y1, int xr, int yr, int c) {
			if(c==transcolor) {
				Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
				g2d.setColor(new Color(0, 0, 0, 0));
				g2d.fillOval(x1-xr, y1-yr, xr*2, yr*2);			
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			}
			else {
				circlefill(x1, y1, xr, yr, palette.getColor(c, currentLucent));
			}
		}

		public void circlefill(int x1, int y1, int xr, int yr, Color c) { // [Rafael, the Esper]
			if(c.getAlpha()==255)
				c = new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent);
			
			this.g.setColor(c);
			this.g.fillOval(x1-xr, y1-yr, xr*2, yr*2);
		}
		
		// Note: it's a filled triangle. A non-filled triangle can be draw with lines.
		public void triangle(int x1, int y1, int x2, int y2, int x3, int y3, Color c) { // [Rafael, the Esper]
			Polygon p = new Polygon();
			p.addPoint(x1, y1);
			p.addPoint(x2, y2);
			p.addPoint(x3, y3);
			this.g.setColor(c);
			this.g.fillPolygon(p);
		}
		
		public void tscaleblit(int x, int y, int dw, int dh, VImage src) {
			//TScaleBlit(x, y, dw, dh, s, d);
			this.tblit(x, y, src); // TODO [Rafael, the Esper] Implement scaling
			
		}
		/*
		static void TSubtractiveBlit(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			TSubtractiveBlit(x, y, s, d);
		}*/
		/**
		 * @deprecated Use {@link #twrapBlit(int,int,VImage)} instead
		 */
		public void twrapblit(int x, int y, VImage src) {
			twrapBlit(x, y, src);
			}


		public void twrapBlit(int x, int y, VImage src) {
			// TODO [Rafael, the Esper] Implement
			//TWrapBlit(x, y, s, d);
			error("Non implemented function: twrapblit");
		}
		/**
		 * @deprecated Use {@link #wrapBlit(int,int,VImage)} instead
		 */
		public void wrapblit(int x, int y, VImage src) {
			wrapBlit(x, y, src);
			}


		public void wrapBlit(int x, int y, VImage src) {
			// TODO [Rafael, the Esper] Implement
			//WrapBlit(x, y, s, d);
			error("Non implemented function: wrapblit");
		}

		/**
		 * @deprecated Use {@link #printString(int,int,Font,String)} instead
		 */
		public void printstring(int x, int y, Font font, String text) {
			printString(x, y, font, text);
			}


		public void printString(int x, int y, Font font, String text) {
			this.g.setFont(font);
			this.g.setColor(Color.WHITE);
			this.g.drawString(text, x, y);
			}
		// Krybo (Feb.2016) enable color control
		public void printString(int x, int y, Font font, Color cf, String text) 
			{
			this.g.setFont(font);
			this.g.setColor( cf );
			this.g.drawString(text, x, y);
			}
		

		// Fade functions
		public void fadeOut(int delay, boolean rendermap) {
			timer = 0;	
			while (timer<delay)
			{
				if(rendermap)
					this.render();
				setlucent(100 - (timer*100/delay));
				this.paintBlack();
				setlucent(0);	
				showpage();
			}
		}
		
		public void fadeIn(int delay, boolean rendermap) {
			timer = 0;
			while (timer<delay)
			{
				if(rendermap)
					this.render();
				setlucent(timer*100/delay);
				this.paintBlack();
				setlucent(0);
				showpage();
			}
		}
		
		// Handy code by [Rafael, the Esper]
		public void fade(int delay, boolean black) { // fade in and out
			if(black) {
				fadeOut(delay, true);
				this.paintBlack();
				fadeIn(delay, false);
			}
			else {
				fadeOut(delay, false);
				fadeIn(delay, true);
			}
			
		}

		public void paintBlack() {
			this.rectfill(0, 0, this.width, this.height, Color.BLACK);
		}

			// same thing but with a translucency
		public void paintBlack( float alphaValue ) 
			{
			this.rectfill(0, 0, this.width, this.height, new Color(0.0f,0.0f,0.0f,alphaValue ) );
			}
		
		
			// Krybo (2014-09-30)  zoom-like function.  now used for map zoom.
		public void scaleBlendWithImageSubsection( VImage src, 
				int ssX1, int ssY1,
				int ssW, int ssH, float blendValue )
			{
			double xFactor = (double) this.getWidth() / (double) ssW;
			double yFactor = (double) this.getHeight() / (double) ssH;
			
			AffineTransform at = new AffineTransform();
			at.scale( xFactor,  yFactor );

			this.paintBlack();
			Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
			g2d.setComposite( AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, blendValue )  );
			// Grab a subsection and use it immediately in an affine transform
			g2d.drawImage( src.getImage().getSubimage(
					ssX1, ssY1, ssW, ssH ), 
					at, null );
			g2d.dispose();
			return;
			}

		/**  This does true rotation of a non-square object.
		 *     Since the dimensions of the rotated non-square image will
		 *     be different.   This will return a VImage of not-so obvious
		 *     x and y sizes.   Keep this in mind when using.
		 *     Consequently, the new image will likely contain empty space.
		 *     Not useful for all rotation applications, 
		 *     try instead:  rotateScaleBlendWithImageSubsection(...)
		 *     Krybo (Apr.2016)
		 * @param src  The source image to rotate.  It will not be altered.
		 * @param rotationRads  Rotation angle in cartesean radians.
		 * @return  A new VImage with rotation.  
		 */
	public static VImage rotateRadiansIntoNewImage(VImage src, 
			Double rotationRads )
		{
//		src.line(0, 5, 25, 5, Color.RED );
		int ox = src.getWidth();
		int oy = src.getHeight();
		Double OX = new Double( ox );		// original x );
		Double OY = new Double( oy );	// original y );

		// This was a brain-teasing nasty geometric derivation...
		// the 0.5d round to the nearest pixel.
		Double cos = Math.abs( Math.cos(rotationRads) );
		Double sin = Math.abs( Math.sin(rotationRads) );
		int newX = new Double( (OX * cos) + (OY * sin) + 0.5d ).intValue();
		int newY = new Double( (OX * sin) + (OY * cos ) + 0.5d ).intValue();
				
//		Double conv = rotationRads * 57.2958d;
//		System.out.println(" DEBUG Rotation :: "+ conv.toString() +
//			" deg :: Original : "+
//		    Integer.toString(ox) + " / " + 
//		    Integer.toString(oy) + "   ==>  New : " +
//		    Integer.toString(newX) + " / " + Integer.toString(newY) );

		VImage rtn = new VImage( newX,newY,
				core.Script.Color_DEATH_MAGENTA);

		AffineTransform at = new AffineTransform();
		at.translate(  newX/2,  newY/2 );
		at.rotate( -1.0d*rotationRads );
		at.translate( ox/-2, oy/-2 );

		Graphics2D g2d = (Graphics2D) rtn.getImage().getGraphics();
		g2d.drawImage( src.getImage(), at, null );
		g2d.dispose();

		return(rtn);
		}

	/**  This does true rotation of a non-square object.
	 *     Since the dimensions of the rotated non-square image will
	 *     be different.   This will return a VImage of not-so obvious
	 *     x and y sizes.   Keep this in mind when using.
	 *     Consequently, the new image will likely contain empty space.
	 *     Not useful for all rotation applications, 
	 *     try instead:  rotateScaleBlendWithImageSubsection(...)
	 *     Krybo (Apr.2016)
	 * @param src  The source image to rotate.  It will not be altered.
	 * @param rotationRads  Rotation angle in cartesean degrees.  
	 * @return  A new VImage with rotation.  
	 */
	public static VImage rotateDegreesIntoNewImage(VImage src, 
			Double rotationDegs )
			{
			return( VImage.rotateRadiansIntoNewImage(src, 
				Math.toRadians(rotationDegs) ) );
			}


		// Krybo (2014-09-30)  zoom-like function.  now used for map zoom.
	public void rotateScaleBlendWithImageSubsection( VImage src, 
			int ssX1, int ssY1,
			int ssW, int ssH, float blendValue, float rotationRadians  )
		{
		double xFactor = (double) this.getWidth() / (double) ssW;
		double yFactor = (double) this.getHeight() / (double) ssH;
		
		AffineTransform at = new AffineTransform();
		at.scale( xFactor,  yFactor );
		if( rotationRadians != 0.0f )		// Save a few calcs when no rotation.
			{
			at.rotate(rotationRadians, Math.floorDiv(ssW , 2) , 
				Math.floorDiv(ssH , 2) );
			}

		this.paintBlack();
		Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
		g2d.setComposite( AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, blendValue )  );
		// Grab a subsection and use it immediately in an affine transform
		g2d.drawImage( src.getImage().getSubimage(
				ssX1, ssY1, ssW, ssH ), 
				at, null );
		g2d.dispose();
		return;
		}


			// The above, but simpler
	public void rotateBlend( float rotationRadians,  float blendValue )
		{
		AffineTransform at = new AffineTransform();
		
		if( rotationRadians != 0.0f )		// Save a few calcs when no rotation.
			{
			at.rotate(rotationRadians, Math.floorDiv( this.getWidth() , 2) , 
				Math.floorDiv(this.getHeight() , 2) );
			}

		Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
		if( blendValue >= 0 && blendValue < 1.0f )
			{
			g2d.setComposite( AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, blendValue )  );
			}
		else
			{	g2d.setComposite( AlphaComposite.Src);  	}
		
		
		AffineTransformOp op = new AffineTransformOp(
				at, AffineTransformOp.TYPE_BILINEAR );

//	g2d.drawImage( this.getImage(), at, null );
//		It was leaving edge related artifacts -- this solved it:with an Op
//		http://stackoverflow.com/questions/8639567/java-rotating-images
		g2d.drawImage(op.filter(this.getImage(), null), 0, 0, null );
		
		g2d.dispose();
		return;
		}
			// Delegator to above that returns a new VImage
	public VImage getVImageRotateBlend( float rotationRadians,  float blendValue )
		{
		VImage newImage = new VImage(this.width,this.height);
		newImage.paintBlack(0.0f);

		AffineTransform at = new AffineTransform();
		
		if( rotationRadians != 0.0f )		// Save a few calcs when no rotation.
			{
			at.rotate(rotationRadians, Math.floorDiv( this.getWidth() , 2) , 
				Math.floorDiv(this.getHeight() , 2) );
			}

		Graphics2D g2d = (Graphics2D) newImage.getImage().getGraphics();
		if( blendValue >= 0 && blendValue < 1.0f )
			{
			g2d.setComposite( AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, blendValue )  );
			}
		else
			{	g2d.setComposite( AlphaComposite.Src);  	}
		
		AffineTransformOp op = new AffineTransformOp(
				at, AffineTransformOp.TYPE_BILINEAR );
		g2d.drawImage(op.filter(this.getImage(), null), 0, 0, null );		
		g2d.dispose();
		
		return(newImage);
		}

		// Krybo: (2014-10-03)
		// Composites another VImage with rotation & scaling
		//  Thank you:  Eric Petroelje et.al @  
		// http://stackoverflow.com/questions/682770/rotation-and-scaling-how-to-do-both-and-get-the-right-result
	public void rotScaleBlendBlit( VImage src, int x, int y, 
			float rotationRadians, float scaleX, float scaleY, float blendValue )
		{
		if( x >= this.width || x < 0  )  { return; }
		if( y >= this.height || y < 0  )  { return; }
		if( scaleX < 0 )  { scaleX = 0.0001f; }
		if( scaleY < 0 )  { scaleY = 0.0001f; }
		if( scaleX == 1.0f && scaleY == 1.0f && rotationRadians == 0.0f )
			{ 
			// Thsi reduces to a simple tblit
			this.tblit(x, y, src.getImage(), blendValue );
			return;
			}

		// Scaling is the less intense process, so lets optimize for that
		AffineTransform at = new AffineTransform();
		AffineTransform at2 = new AffineTransform();
		at2.scale( scaleX, scaleY );
		if( rotationRadians != 0.0f )		// Save a few calcs when no rotation.
			{
			at.rotate(rotationRadians, Math.floorDiv( src.getWidth() , 2) , 
				Math.floorDiv( src.getHeight() , 2) );
			at2.concatenate(at);
			}

		AffineTransformOp op = new AffineTransformOp(
			at2, AffineTransformOp.TYPE_BILINEAR );

		Graphics2D g2d = (Graphics2D) this.getImage().getGraphics();
		g2d.setComposite( AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, blendValue )  );
		g2d.drawImage(op.filter(src.getImage(), null), x, y, null );		
		g2d.dispose();
		
		return;
		}
	
/**  Takes a larger VImage and splits it into multiple (array) VImages.
 *   { Krybo adaptation of RafaelEsper's original }
 * @param startx  Pixel-X within source to start cutting.
 * @param starty  Pixel-Y within source to start cutting.
 * @param cellW  The width of the cut for smaller cells.
 * @param cellH   The height of the cut for smaller cells.
 * @param skipx   ???
 * @param skipy   ???
 * @param columns   Number of columns to wrap cutting.
 * @param totalframes	Max number of cells to cut , total.
 * @param padding  Is there a one pixel padding around each cell?
 * @param sourceImg  The larger source image.
 * @return
 */

	public static VImage[] split(int startx, int starty, 
			int cellW, int cellH, int skipx, int skipy, int columns, 
			int totalframes, boolean padding, VImage sourceImg ) 
		{
		VImage[] images = new VImage[totalframes];
		
		int frames = 0, posx = 0, posy = 0, column = 0;

		if( padding )		{ posy++; }

		// First pixel is default transparent color
		Color transC = core.Script.Color_DEATH_MAGENTA;
		Color unused = transC;   transC = unused;   // kill warning
		
		while(frames < totalframes) 
			{
			if( padding )		{ posx++; }
				 
			images[frames] = new VImage(cellW, cellH );
			images[frames].grabRegion(startx+posx, starty+posy, 
				startx+posx+cellW, starty+posy+cellH, 0, 0, 
				sourceImg.getImage() );

			column++;
			posx += cellW + skipx;
			
			if( column >= columns ) 
				{
				column = 0;
				posx = 0;
				posy+= cellH + skipy;
				if(padding)
					{ posy++; }
				}
			frames++;
			}	
		return( images );
		}

	/**	Returns a new Vimage that is a fractional truncation
	 *  of the source on the X-axis.  Truncates TO fraction, not BY it.
	 * @param src	The source image
	 * @param fraction  fraction of which to truncate to.
	 * @return	a new VImage of different width 
	 *          retaining non-scaled content.
	 */
	public static VImage truncateX( VImage src, Double fraction )
		{
		if( fraction <= 0.0f || fraction > 1.0f )
			{ return(src);  }
		Double newX = new Double(src.getWidth()) * fraction;
		VImage rtn = new VImage(newX.intValue(),src.getHeight());
		rtn.blit( 0, 0, src );
		return(rtn);
		}

	/**	Returns a new Vimage that is a fractional truncation
	 *  of the source on the Y-axis.  Truncates TO fraction, not BY it.
	 * @param src	The source image
	 * @param fraction  fraction of which to truncate to.
	 * @return	a new VImage of different height 
	 *          retaining non-scaled content.
	 */
	public static VImage truncateY( VImage src, Double fraction )
		{
		if( fraction <= 0.0f || fraction > 1.0f )
			{ return(src);  }
		Double newY = new Double(src.getHeight()) * fraction;
		VImage rtn = new VImage( src.getWidth(), newY.intValue() );
		rtn.blit( 0, 0, src );
		return(rtn);
		}
	
	}

