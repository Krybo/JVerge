package domain;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

public class VImageGradient extends VImage
	{
	
	private ArrayList<Color> c = new ArrayList<Color>();

	/** Constructs a default resolution (100px) VImage with a 
	 * smooth 2-Color Vertical gradient set.
	 * 
	 * @param c1		Start Color at top.
	 * @param c2		End Color at bottom.
	 */
	public VImageGradient( Color c1, Color c2 )
		{	
		this( 100, c1, c2 );
		boundDouble(0.5d, 1.0d, 0.0d );	// stop unused method warning.
		return;
		}

/** Constructs a square VImage with a smooth 2-Color Vertical gradient set.
 * 
 * @param resolution   Pixel width and height -
 *   use higher resolution for smoother gradient but more memory expense.
 *   use lower (<20) for grainy but fast & cheap gradients. 
 * @param c1		Start Color at top.
 * @param c2		End Color at bottom.
 */
	public VImageGradient( int resolution, Color c1, Color c2 )
		{
		super( resolution,resolution );
		int w = resolution;
		int h = resolution;

		this.c.add(c1);
		this.c.add(c2);

		Float r1 = new Float(c1.getRed()) / 255.0f;
		Float g1 = new Float(c1.getGreen()) / 255.0f;
		Float b1 = new Float(c1.getBlue()) / 255.0f;

		Float r2 = new Float(c2.getRed()) / 255.0f;
		Float g2 = new Float(c2.getGreen()) / 255.0f;
		Float b2 = new Float(c2.getBlue()) / 255.0f;

		Float divider = new Float(w);
		Float rGrad = (r2 - r1) / divider;
		Float gGrad = (g2 - g1) / divider;
		Float bGrad = (b2 - b1) / divider;

		Color cLine;
		for( int n = 0; n < h; n++ )
			{
			cLine = new Color( 
					r1 + (n*rGrad)  , 
					g1 + (n*gGrad), 
					b1 + (n*bGrad), 
					1.0f );	
			super.line(0, n, w-1, n, cLine );
			}

		return;
		}
	
	/**  Advanced, infinite-sequence constructor using a hashmap to define.
	 * TODO: This is vulnerable to failure if given two or more hashmap 
	 *             keys that are identical. This should not nomrally happen, 
	 *             but should be accommodated  
	 * 
	 * @param resolution   sample square pixel size
	 * @param horizontalClr     Horizontal Gradient hashmap
	 * @param verticalClr        Vertical gradient hashmap
	 * 		values 0.0f to 1.0f representing area from top to bottom-most.
	 * @param horizontal   use horizontal gradient
	 *		values 0.0f to 1.0f representing area from left to right-most.
	 * @param vertical       use horizontal gradient
	 * @param layoutCutoff number at which horizontal nodes 
	 *      switch to vertical ones.  Only used when both H and V are true
	 */
	public VImageGradient( int resolution, 
			HashMap<Double,Color> horizontalClr,
			HashMap<Double,Color> verticalClr )
		{
		super( resolution,resolution, Color.BLACK );
		Long performance_st = System.nanoTime();
		
		if( (horizontalClr == null || horizontalClr.size() < 2) && 
			( verticalClr == null || verticalClr.size() < 2) )
			{ return; }
		
		// Need a temporary VImage to work each direction on.
		//     then later blit them together.
		VImage tmpImg = new VImage(resolution,resolution,
				Color.BLACK );

		Double[] sortedH = null;
		Double[] sortedV = null;
		int count;

		if( horizontalClr != null && horizontalClr.size() >= 2 )
			{
			sortedH = new Double[horizontalClr.size()];
			count = 0;
			for( Double d : horizontalClr.keySet() )
				{
				sortedH[count] = d;
				count++;
				}
			java.util.Arrays.sort(sortedH);
			}
		
		if( verticalClr != null && verticalClr.size() >= 2 )
			{
			sortedV = new Double[verticalClr.size()];
			count = 0;
			for( Double d : verticalClr.keySet() )
				{
				sortedV[count] = d;
				count++;
				}
			java.util.Arrays.sort(sortedV);
			}

		Float blender = 0.5f;
			// If only drawing 1 gradient, no need to blend.
		if( sortedH == null || sortedH == null )
			{ blender = 1.0f; }

		Color c1,c2,cLine;
		Float r1,g1,b1,r2,g2,b2,n = 0.0f;
		Float divider,rGrad,gGrad,bGrad;
		r1 = g1 = b1 = new Float(0.0f);
		r2 = g2 = b2 = new Float(1.0f);
		divider = rGrad = gGrad = bGrad = 0.0f;

		//   float value per-pixel length.
		Float frack = 1.0f / new Float(resolution);

		int cIdx = -1;
		int u = -1;		// loop var.
		Double dIdx = -1.0d;
		Double eIdx = -1.0d;

		Float segmentEnd = -1.0f;
		Float nLeader = 0.0f;
		Float nStep = 0.0f;
 
		if( sortedH != null )
			{
			for( u = 0; u < resolution; u++ )
				{
				n = new Float(u);
	
					// Switch bands if needed
	//			if( (sortedH[cIdx+1] > dIdx) 
	//					&& (cIdx+2 < sortedH.length) 	 )
				if( n > segmentEnd )
					{
					cIdx++;
					nLeader = n;
//					System.out.println( "DEBUG: At HORZ n = "+
//							n.toString()+ " " +
//							Integer.toString(cIdx)+
//							" / "+Integer.toString(cIdx+1) );
					dIdx = sortedH[cIdx];
					eIdx = sortedH[cIdx+1];
					segmentEnd = eIdx.floatValue() * new Float(resolution);
	
					c1 = horizontalClr.get(dIdx);
					c2 = horizontalClr.get(eIdx);
	
					r1 = new Float(c1.getRed()) / 255.0f;
					g1 = new Float(c1.getGreen()) / 255.0f;
					b1 = new Float(c1.getBlue()) / 255.0f;
	
					r2 = new Float(c2.getRed()) / 255.0f;
					g2 = new Float(c2.getGreen()) / 255.0f;
					b2 = new Float(c2.getBlue()) / 255.0f;
					
					// Value range for this block.
					//   Then calc this local gradient.
					divider = eIdx.floatValue() - dIdx.floatValue();
					if( divider == 0.0f )	{ divider = 0.000000001f; }
					rGrad = (r2 - r1) / divider * frack;
					gGrad = (g2 - g1) / divider * frack;
					bGrad = (b2 - b1) / divider * frack;
					}
				
				nStep = n - nLeader;
				cLine = new Color( 
					boundFloat( r1 + (nStep*rGrad), 1.0f, 0.0f ),
					boundFloat( g1 + (nStep*gGrad), 1.0f, 0.0f ), 
					boundFloat( b1 + (nStep*bGrad), 1.0f, 0.0f ),
					1.0f );			
				tmpImg.line( 0, n.intValue(), resolution-1, n.intValue(), cLine );
				}			
			this.blit(0, 0, tmpImg);
//			this.rotScaleBlendBlit(tmpImg, 0, 0, 	0.0f, 1.0f, 1.0f, blender );
			}
		
		
			// Vertical Gradient component.
		if( sortedV != null )
			{
			segmentEnd = -1.0f;
			nLeader = 0.0f;
			nStep = 0.0f;
			cIdx = -1;

			for( u = 0; u < resolution; u++ )
				{
				n = new Float(u);
	
					// Switch bands if needed
	//			if( (sortedH[cIdx+1] > dIdx) 
	//					&& (cIdx+2 < sortedH.length) 	 )
				if( n > segmentEnd )
					{
					cIdx++;
					nLeader = n;
//					System.out.println( "DEBUG: At  VERT n = "+
//							n.toString()+ " " +
//							Integer.toString(cIdx)+
//							" / "+Integer.toString(cIdx+1) );
					dIdx = sortedV[cIdx];
					eIdx = sortedV[cIdx+1];
					segmentEnd = eIdx.floatValue() * new Float(resolution);
	
					c1 = verticalClr.get( dIdx );
					c2 = verticalClr.get( eIdx );
	
					r1 = new Float(c1.getRed()) / 255.0f;
					g1 = new Float(c1.getGreen()) / 255.0f;
					b1 = new Float(c1.getBlue()) / 255.0f;
	
					r2 = new Float(c2.getRed()) / 255.0f;
					g2 = new Float(c2.getGreen()) / 255.0f;
					b2 = new Float(c2.getBlue()) / 255.0f;
					
					// Value range for this block.
					//   Then calc this local gradient.
					divider = eIdx.floatValue() - dIdx.floatValue();
					if( divider == 0.0f )	{ divider = 0.000000001f; }
					rGrad = (r2 - r1) / divider * frack;
					gGrad = (g2 - g1) / divider * frack;
					bGrad = (b2 - b1) / divider * frack;
					}
				
				nStep = n - nLeader;
				cLine = new Color( 
					boundFloat( r1 + (nStep*rGrad), 1.0f, 0.0f ),
					boundFloat( g1 + (nStep*gGrad), 1.0f, 0.0f ), 
					boundFloat( b1 + (nStep*bGrad), 1.0f, 0.0f ),
					1.0f );			
				tmpImg.line( 0, n.intValue(), resolution-1, n.intValue(), cLine );
				}

			// Blend only if this is going over top a horizontal grad.
			if( sortedH != null ) 
				{
				this.rotScaleBlendBlit(tmpImg, 0, 0, 
					new Double(Math.PI / -2.0d).floatValue(), 1.0f, 1.0f, 
					blender );
				}
			else
				{
				this.rotScaleBlendBlit(tmpImg, 0, 0, 
						new Double(Math.PI / -2.0d).floatValue(), 1.0f, 1.0f, 
						1.0f );
				}
			}


		Long performance_et = System.nanoTime() - performance_st;
		System.out.println(" Color Gradient calculation took "+
			performance_et.toString() + " nsec."	);

		return;
		}

	/**  delegator-consructor to a solid color VImage.
	 * 
	 * @param x	X pixel size
	 * @param y	Y pixel size
	 * @param c  Color object 
	 */
	public VImageGradient(int x, int y, Color c)
		{
		super(x, y, c);
		return;
		}


	/**  Instead of a black empty cell, starts with a pre-existing image 
	 *    resource and blends a gradient over top it.
	 * 
	 * @param url
	 */
	public VImageGradient( VImage img )
		{
		super(img);
		return;
		}

	private static Double boundDouble(Double val, Double max, Double min)
		{
		if( val <= max && val >= min )	{ return(val); }
		if( val > max ) 					{ return(max); }
		return(min);
		}
	private static Float boundFloat( Float val, Float max, Float min)
		{
		if( val <= max && val >= min )	{ return(val); }
		if( val > max ) 					{ return(max); }
		return(min);
		}
	
	}

