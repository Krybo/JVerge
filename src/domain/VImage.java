package domain;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
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
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import core.VergeEngine;
import persist.PCXReader;
import static core.Script.*;

public class VImage implements Transferable {

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
	
	 public VImage(URL url) {
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
			  //if(image==null)
				//  image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB); // [Rafael, the Esper] Temp
		  } catch (IOException e) {
			  System.err.println("Unable to read image from URL " + url);
		  }
		  this.width = image.getWidth();
		  this.height = image.getHeight();

		  // Make death magenta = transparent
		  Image img = makeColorTransparent(image, new Color(255, 0, 255));
		  this.image = imageToBufferedImage(img);		  
		  
		  g = (Graphics2D)image.getGraphics();
	 }
	 
	 /*public VImage(URL url, boolean transparent) {
		 this(url);

		 // Post-processing for transparent pixels
		 if(transparent) {
			 int tcolor = new Color(255, 0, 255).getRGB();
			 Image img = makeColorTransparent(image, new Color(tcolor));
			 this.image = imageToBufferedImage(img);
		 }
		 // End-code
	 }*/
	
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

	        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
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
	    public void render() {
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

		// Overkill (2007-08-25): src and dest were backwards. Whoops!
		public void blitlucent(int x, int y, int lucent, VImage src) {
			int oldalpha = currentLucent;
			setlucent(lucent);
			this.blit(x, y, src);
			setlucent(oldalpha);
		}
		
		public void blittile(int x, int y, int t) {
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
		
		/*static void BlitWrap(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			BlitWrap(x, y, s, d);
		}*/
		

		public VImage duplicateimage() { // TODO Test
			VImage img = new VImage(this.image.getWidth(), this.image.getHeight());
			img.g.drawImage(this.getImage(), 0, 0, null);
			return img;
		}
		
		// FIXME
		public enum FlipType{FLIP_HORIZONTALLY, FLIP_VERTICALLY, FLIP_BOTH};
		//public static void flipblit(int x, int y, boolean fx, boolean fy, VImage src, VImage dest) {
		public void flipblit(int x, int y, FlipType type, VImage src) {
			AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
	        tx.translate(-src.width, 0);
	        AffineTransformOp op = new AffineTransformOp(tx, 
	                                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	        VImage flippedImage = this.duplicateimage();
	        //BufferedImage flippedImage =  new BufferedImage(src.width, 
	          //      src.height, BufferedImage.TYPE_INT_RGB);
	        //flippedImage = op.filter(src.image, null);		
	        flippedImage.image = op.filter(flippedImage.image, null);
	        //blit(x, y, flippedImage, dest.image);
	       	this.blit(x,y,flippedImage);
		}
		
		public static BufferedImage flipimage(int x, int y, BufferedImage src) {
		/*	BufferedImage flippedImage = duplicateimage(src);
			AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
	        tx.translate(-src.getWidth(), 0);
	        AffineTransformOp op = new AffineTransformOp(tx, 
	                                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			flippedImage = op.filter(flippedImage, null);
			return flippedImage;*/
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
		 */
		public VImage imageshell(int x, int y, int w, int h) {
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
		
		// TODO: Eliminate them (redundant methods)
		public int imagewidth() { 
			return this.width; 
		}
		public int imageheight() { 
			return this.height; 
		}
		
		public void line(int x1, int y1, int x2, int y2, Color c) { // [Rafael, the Esper]
			this.g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent));
			this.g.drawLine(x1, y1, x2, y2);
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
		
		//ScaleBlit(x, y, dw, dh, s, d);
		public void scaleblit(int x, int y, int dw, int dh, VImage src) 
			{
			// Worthless scale -- ignore.
			if( dw <= 1 && dh <= 1 )
				{ this.blit(x, y, src); }
		
			int wOrig = src.width;
			int hOrig = src.height;
			int xBound = this.width;
			int yBound = this.height;
			int xDestStart = 0;
			int yDestStart = 0;
			int srcColor = 0;
			
			for( int ySrc=0; ySrc < hOrig; ySrc++ )
				{
				for( int xSrc=0; xSrc < wOrig; xSrc++ )
					{
					//  we have the source pixel, now replicate it
					srcColor = src.image.getRGB(xSrc, ySrc);

					xDestStart =  (xSrc*dw)+x;
					yDestStart =  (ySrc*dh)+y;
						// This is like an enlarged square "stamp" onto the destination
					for( int yScaled = 0; yScaled < dh; yScaled++ )
						{
						for( int xScaled = 0; xScaled < dw; xScaled++ )
							{
							if( (xDestStart+xScaled) >= xBound ) { continue; }
							if( (yDestStart+yScaled) >= yBound ) { continue; }
							this.image.setRGB(xDestStart+xScaled, 
								yDestStart+yScaled, srcColor);
							}
						}
					}
				}

			// That is it for simple scaling 
			return;		
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
		
		
		public void setclip(int x1, int y1, int x2, int y2) {
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
						this.setpixel(x+i, y+j, new Color(0,0,0,0));
					else
						this.setpixel(x+i, y+j, c);
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
		
		public void grabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, VImage src) {
			this.grabregion(sx1, sy1, sx2, sy2, dx, dy, src.image);
		}
		
		public void grabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, BufferedImage src) {
			
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
				
				Color color = null;
				for(int j=0; j<sy2-sy1; j++)
				for(int i=0; i<sx2-sx1; i++) {
					if(sx1+i >= src.getWidth() || sy1+j >= src.getHeight()
					   || dx+i >= this.getWidth() || dy+j >= this.getHeight())		
						break;
					color = new Color(src.getRGB(sx1+i, sy1+j));
					if(color.getRed() + color.getGreen() + color.getBlue() == 0) // TODO [Rafael, the Esper] Probably move it to tgrabregion?
						color = new Color(0,0,0,0); //color.getRed(), color.getGreen(), color.getBlue(), 0);
					this.setpixel(i+dx, j+dy, color);

				}
				/*int grabwidth = sx2 - sx1;
				int grabheight = sy2 - sy1;
				if (dx+grabwidth<0 || dy+grabheight<0) return;
				dst.SetClip(dx, dy, dx+grabwidth, dy+grabheight);
				Blit(dx-sx1, dy-sy1, src, dst);
			
				dst.SetClip(dcx1, dcy1, dcx2, dcy2);*/
		}

		public void tgrabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, Color transC, VImage src) {
			this.tgrabregion(sx1, sy1, sx2, sy2, dx, dy, transC, src.image);
		}	
		
		public void tgrabregion(int sx1, int sy1, int sx2, int sy2, int dx, int dy, Color transC, BufferedImage src) {
			
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
				this.setpixel(i+dx, j+dy, color);

			}	
		}
		
	
		public void setpixel(int x, int y, Color color) {
			this.image.setRGB(x, y, color.getRGB());
		}	
		
		public  int readpixel(int x, int y) {
			//if(workingimage.image != null)
			return this.image.getRGB(x, y);
			//return 0;
		}
		
		
		// Overkill (2007-08-25): src and dest were backwards. Whoops!
		public void tblitlucent(int x, int y, int lucent, VImage src) {
			int oldalpha = currentLucent;
			setlucent(lucent);
			this.tblit(x, y, src);
			setlucent(oldalpha);
		}

		public void tblittile(int x, int y, int t) {
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
		public void rect(int x1, int y1, int x2, int y2, Color c) { // [Rafael, the Esper]
			this.g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), currentLucent));
			if(x1>x2) {	int temp = x1;	x1 = x2;	x2 = temp;	} // swap x1,x2
			if(y1>y2) {	int temp = y1;	y1 = y2;	y2 = temp;	} // swap y1,y2
			this.g.drawRect(x1, y1, x2-x1, y2-y1);
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
		
		public void rectfill(int x1, int y1, int x2, int y2, Color c) { // [Rafael, the Esper]
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
			
		}/*
		static void TSubtractiveBlit(int x, int y, int src, int dst) {
			image *s = ImageForHandle(src);
			image *d = ImageForHandle(dst);
			TSubtractiveBlit(x, y, s, d);
		}*/
		public void twrapblit(int x, int y, VImage src) {
			// TODO [Rafael, the Esper] Implement
			//TWrapBlit(x, y, s, d);
			error("Non implemented function: twrapblit");
		}
		public void wrapblit(int x, int y, VImage src) {
			// TODO [Rafael, the Esper] Implement
			//WrapBlit(x, y, s, d);
			error("Non implemented function: wrapblit");
		}

		public void printstring(int x, int y, Font font, String text) {
			this.g.setFont(font);
			this.g.setColor(Color.WHITE);
			this.g.drawString(text, x, y);
		}
		
		// Krybo (2014-09-19)  needed this for mapzooming to kill transparency
		public void blackOut()
			{			
			for( int iy=0; iy < this.height; iy++ )
				{
				for( int ix=0; ix < this.width; ix++ )
					{
					this.image.setRGB(ix, iy, 1 );
					}
				}
			this.image.flush();
			return;
			}
		
}
