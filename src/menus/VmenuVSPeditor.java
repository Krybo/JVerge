package menus;

import static core.Script.getFunction;
import static core.Script.setMenuFocus;
import static core.Script.getInput;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Iterator;

import javax.swing.filechooser.FileNameExtensionFilter;

import persist.ExtendedDataInputStream;
import persist.ExtendedDataOutputStream;
import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiButton.enumMenuButtonCOLORS;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import core.Controls;
import core.DefaultPalette;
import core.Script;
import core.VergeEngine;
import domain.Map;
import domain.VImage;
import domain.VSound;
import domain.Vsp;

/**                    An Interactive Tile editor.   
 * Consists of several Vmenu objects with an overriding
 * Vmenu to manage interactions between them.
 * 
 * ----- Controls Overview --------
 * 
 * { 0-9 and - } 	"Color Keys" Do Operations, corresponding to color bar.
 * 		Unmodified		Change target cell in working tile to a color key
 * 		[Cntl]			Change color key to current working cell. (dropper)
 * 		[Alt + Shift]	Replace color at cursor with a color key
 * 		[Alt + Cntl]		Change full working tile to a color key  
 * [cntl][backspace]	Exits menu regardless of focus
 * {Arrow Keys}		Navigate menu - move cursor within submenus.
 * 				[Cntl]	Navigate tileset
 * 				[Shift]	Shift entire tile contents.
 * a	Airbrush, areal Random Spray Tool 3px
 *				[Cntl]	High Density (10 px)
 *				[Shift]	Sprays random colors. 
 * b
 * c	Copy working tile to the clipboard
 * 				[Shift]  Copy working tile into tile group. 
 * 				[Cntl] Copy entire Vsp tileset to clipboard without padding.
 * 				[Cntl+Shift]   Copy entire Vsp to clipboard with 1px padding
 * d	DEBUG: for now.
 * e	focus tile Editor   
 * 				[Cntl]	Focus Palette bar.
 * f		Flood fill Tool
 * 				[Shift]	Use tool color 2 instead of Color 1
 * 				[Cntl]   Fill with 26% tolerance
 * 				[Alt]	Fill with 50% tolerance
 * g	Goto tile #
 * h	keyboard Help table.
 * i		Invert current cell, invert palette entry    [Cntl] invert tile
 * j
 * k
 * l		Line				Toggle Line drawing mode.
 * 					[Shift]   Toggle contiguous line mode.
 * 				{ In Color key } :  Load palette file
 * 			HOT	[Cntl]	Load entire VSP file from disk.
 * m
 * n	New		{main} 		New Tile: Clears tile to tool color 1
 * 					{colorKey} 	Set the basic 8-unit color key.
 * o	rOtate 				Rotates clockwise
 * 					[Cntl]	Rotate Counter-Clockwise
 * 					[Alt]	Rotate 180 degrees
 * 					[Shift]	Mirror / flip
 * p
 * q
 * r		Rectangle		Toggle Rectangle drawing mode.
 * 						[Shift]   Contiguous rectangle mode
 * s	HOT:			Saves working tile
 * 		HOT [Cntl] 	Save entire VSP to a file
 * 		[Shift] 			Save Palette to a file
 * t 	{ In color key }	Set item to transparent color.
 * 		[Cntl]			Toggle view of entire Tileset
 * u
 * v	Clipboard Paste : Single tile 
 * 			[Shift] Insert tiles here
 * 			[Cntl] Single tile & save
 * 			[Cntl+Alt]  Re-paste entire VSP
 * w	Circle
 * x	Clear, set tile to solid color
 * y
 * z	Undo		[Cntl] Redo
 * ~	[Cntl]		Immediately Opens New Blank VSP
 * NP+		Increase areal size of certain tool functions
 * NP+		Decrease areal size of certain tool functions
 * Page-Up + Cntl			Next Color Key line
 * Page-Down + Cntl		Previous Color Key line
 * 
 * @author Krybo
 *
 */

public class VmenuVSPeditor implements Vmenu
	{
	
	
	// Krybo: Jan.2017
	// simple sub-class to hold the state information of the 
	//   current tile pixel color array.
	private class StateSaveMain
		{
		private Color[] theData;
		private Integer tileSize = 16;
		public StateSaveMain( int newTielSize )
			{
			this.tileSize = newTielSize;
			this.theData = new Color[tileSize*tileSize];
			for( int x = 0; x < tileSize*tileSize; x++ )
				{
				this.theData[x] = new Color(clrTrans.getRGB());
				}
			}

		public StateSaveMain( VmiButton[] input )
			{
			this(input.length);
			int counter = 0;
			for( VmiButton vmbtn : input )
				{
				theData[counter] = vmbtn.getColorComponent(
					enumMenuButtonCOLORS.BODY_ACTIVE.value() );
				counter++;
				}
			}
		
		public StateSaveMain( )
			{ this(16); }
		
		public int getArraySize()
			{ return(this.tileSize); }
		
		public Color get( int index )
			{	return(this.theData[index]);	}

		}		// End state save main sub class

	//    control focus:
	//   This is a multi-menu, this index is like a tab-order, and
	//   controls will use it to delegate to the active menu.
	private Integer cFocus = 0;

	// tile index: This is the current tile being edited.
	private Integer tIndex = 0;
	// Active line the color palette is on.
	private Integer cBarIndexSet = 0;

	// The component guts.
	private Vsp vsp = null;
	private VmenuButtonPalette main = null;
	private VmenuVertical sidebar = null;
	private VmenuHorizontal colorkey = null;
	private VmenuSliders colorEditor = null;
	private final DefaultPalette vPal = new DefaultPalette();
	private HashMap<Integer,Color> clrs = null;
	protected final Color clrTrans = new Color(255,0,255,0);
	private Color clr1st = Color.WHITE;
	private Color clr2nd = Color.BLACK;
	private VImage bkgImg = null;
	private boolean isBkgImg = false;
		// This is used when jumping from main directly to color editor.
	private boolean editColorInPlace = false;
	private boolean showOverview = false;	// vsp overview toggle.
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();

	// Essentials.
	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);
	
	private VmiGuageInt gR;
	private VmiGuageInt gG;
	private VmiGuageInt gB;

	private VImage preview = null;
	private VImage vspOverview = null;
	private final Color clrShader = new Color(0.0f,0.0f,0.0f,0.24f);
	private static final int STANDARD_TSIZE = 16;

	// related to editing tools
	private int brushSize = 1;
	private static final int MAX_BRS_SZ = 8;
	private int arealTool = -1;
	private int arealToolDensity = 3;
	private boolean arealToolCRandom = false;
	private int lineTool = -1;
	private boolean lineContiguous = false;
	private int rectTool = -1;
	private boolean rectContiguous = false;
	private int circleTool = -1;

	private boolean showHelp;
	private static final int DEFAULT_TILES_PER_ROW = 16;
	// primative undo functionality.
	private Stack<Object> undoStack = new Stack<Object>();
	private Stack<Object> redoStack = new Stack<Object>();
	private Object unredoLastOp = null;
	// undoLastOp : to prevent undoing repeated similar actions
	private Integer undoLastOp = 0;   
	private static final int MAX_UNDO = 10;
	
	//  Add menu ID's to this to cause the input to be scanned
	//   See  method  processInputs()
	private Stack<Long> inputIDstack = new Stack<Long>();

//	private static final long serialVersionUID = 6666410183698706332L;


	/** ----------------  Constructors --------------------  */

	public VmenuVSPeditor( Map sourceMap )
		{	this(sourceMap.getTileSet());	}

	public VmenuVSPeditor( Vsp sourceTileSet )
		{
		this.focusID = Vmenu.getRandomID();
		// copy the VSP from the source.  So we do not edit it directly.
		this.vsp = new Vsp( sourceTileSet );
		this.sidebar = new VmenuVertical( 30, 60 );
		this.colorEditor = new VmenuSliders( 20, 380, 128, 3, 
				false, true, true );
		this.main = new VmenuButtonPalette(250, 60, 256, 256, 
				VmenuVSPeditor.STANDARD_TSIZE, 
				VmenuVSPeditor.STANDARD_TSIZE  );
		this.colorkey = new VmenuHorizontal( 68,1 );
		// instantly sets the default verge color palette as the 
		//  hashmap of available colors at hand.
		this.clrs = this.vPal.getAllColors(255);

		this.undoStack =	new Stack<Object>();

		this.disableAllTools();

		// Construct the sub-menu objects.
		
		this.clr1st = Color.WHITE;
		this.clr2nd = Color.BLACK;

		main.setPadding( 0, true, Color.WHITE );
		main.setBorderAll( false, 0 );
		main.refresh();
		
		HashMap<Integer,Color> hmSideBarClrs = 
			new HashMap<Integer,Color>();
		hmSideBarClrs.put(enumMenuStxtCOLORS.BKG_ACTIVE.value(), 
			new Color(0.0f, 0.2f, 0.8f, 0.8f ) );
		hmSideBarClrs.put(enumMenuStxtCOLORS.BKG_INACTIVE.value(), 
			Color.BLACK );
		hmSideBarClrs.put(enumMenuStxtCOLORS.FRAME_INNER.value(), 
			Color.BLACK );
		hmSideBarClrs.put(enumMenuStxtCOLORS.FRAME_OUTER.value(), 
			Color.GRAY );
		
		Method m2 = null;   Method m2a = null;
		Method m2b = null;
		Method m3 = null;
		Method m4 = null;
		Method m5 = null;
		Method m6 = null;
		Method m7 = null;
		Method m8 = null;
		Method mIn1 = null;
		try {
			m2 = VmenuVSPeditor.class.getDeclaredMethod(
				"saveCurrentVSP",  new Class[]{} );
			m2b = VmenuVSPeditor.class.getDeclaredMethod(
				"loadVsp",  new Class[]{} );
			m2a = VmenuVSPeditor.class.getDeclaredMethod(
				"newVsp",  new Class[]{} );
			m3 = VmenuVSPeditor.class.getDeclaredMethod(
				"focusSubMenuIndex",  Integer.class );
			m4 = VmenuVSPeditor.class.getDeclaredMethod(
				"focusSubMenuIndex",  Integer.class );
			m5 = VmenuVSPeditor.class.getDeclaredMethod(
				"nextTile",  new Class[]{} );
			m6 = VmenuVSPeditor.class.getDeclaredMethod(
				"prevTile",  new Class[]{} );
			m7 = VmenuVSPeditor.class.getDeclaredMethod(
				"saveWorkingTile",  new Class[]{} );
			m8 = VmenuVSPeditor.class.getDeclaredMethod( 
				"toggleVspOverview",  new Class[]{} );
			mIn1 = VmenuVSPeditor.class.getDeclaredMethod( 
				"delegateInput",  VmiInputInteger.class );

			} catch( Exception e ) 	{ e.printStackTrace(); }

		VmiTextSimple vts01 = new VmiTextSimple("Return to Map");
		vts01.setKeycode( 66 );		// Cntl+Backspace exits anywhere.
		vts01.setAction( getFunction(Script.class,"focusSystemMenu") );

		VmiTextSimple vts02a = new VmiTextSimple("New VSP");
		vts02a.setKeycode( 1538 );		// Cntl Tilda
		vts02a.setAction( m2a );

		VmiTextSimple vts02 = new VmiTextSimple("Save VSP");
		vts02.setKeycode( 666 );		// Cntl S
		vts02.setAction( m2 );
		
		VmiTextSimple vts02b = new VmiTextSimple("Load VSP");
		vts02b.setKeycode( 610 );		// Cntl L
		vts02b.setAction( m2b );
		
		VmiTextSimple vts03 = new VmiTextSimple("Edit Palette");
		vts03.setKeycode( 554 );
		vts03.setAction( m3 );
		vts03.setActionArgs( new Object[]{new Integer(1)} );
		
		VmiTextSimple vts04 = new VmiTextSimple("Edit Tile");
		vts04.setKeycode( 552 );
		vts04.setAction( m4 );
		vts04.setActionArgs( new Object[]{new Integer(3)} );

		VmiTextSimple vts05 = new VmiTextSimple("Next Tile");
		vts05.setKeycode( 314 );
		vts05.setAction( m5 );
		
		VmiTextSimple vts06 = new VmiTextSimple("Prev. Tile");
		vts06.setKeycode( 298 );
		vts06.setAction( m6 );
		
		VmiTextSimple vts07 = new VmiTextSimple("Save Tile");
		vts07.setKeycode( 664 );
		vts07.setAction( m7 );
		
		VmiTextSimple vts08 = new VmiTextSimple("Show All Tiles");
		vts08.setKeycode( 674 );
		vts08.setAction( m8 );
		VmiTextSimple vts09 = new VmiTextSimple("Controls Help");
		vts09.setKeycode( 576 );
//		vts07.setAction( m7 );

		VmiInputInteger vmiIn01 = new VmiInputInteger( 
				"<GoTo Tile #> ", "Enter VSP Tile number (INT)", 	0, 0 );
		vmiIn01.setKeycode( 568 );
		vmiIn01.setAction( mIn1 );
//		vmiIn01.setActionArgs( new Object[]{new Integer(1)} );
		vmiIn01.setActionArgs( new Object[]{vmiIn01} );
		this.inputIDstack.push( vmiIn01.getId() );

		this.sidebar.addItem( vts01 );	this.sidebar.addItem( vts02a );
		this.sidebar.addItem( vts02 );	this.sidebar.addItem(vts02b);
		this.sidebar.addItem( vts03 );	this.sidebar.addItem( vts07 );
		this.sidebar.addItem( vts04 );	this.sidebar.addItem( vts05 );
		this.sidebar.addItem( vts06 );	this.sidebar.addItem( vmiIn01 );
		this.sidebar.addItem( vts08 );	this.sidebar.addItem( vts09 );

		this.sidebar.setColorContentAll( hmSideBarClrs );
		this.sidebar.setFontAll(core.Script.fntLogicalScans14 );

		this.resetPalette();
		
		// Pull out these guages from the sub-menu, but keep
		//     the functionality of the submenu.
		this.gR = (VmiGuageInt) this.colorEditor.getMenuItem(0);
		this.gG = (VmiGuageInt) this.colorEditor.getMenuItem(1);
		this.gB = (VmiGuageInt) this.colorEditor.getMenuItem(2);
		this.gR.setValueRange( 0, 255 );
		this.gG.setValueRange( 0, 255 );
		this.gB.setValueRange( 0, 255 );
		this.gR.setBarColors(Color.RED, Color.BLACK );
		this.gG.setBarColors(Color.GREEN, Color.BLACK );
		this.gB.setBarColors( new Color(0.2f,0.2f,1.0f,1.0f) ,
				Color.BLACK );
		this.gR.setTip("Red"); this.gG.setTip("Grn"); this.gB.setTip("Blu");
		
		VmiTextSimple clrEdHelp = new VmiTextSimple("");
		clrEdHelp.setFrameThicknessPx(0);
		clrEdHelp.enableBackdrop(false);
		clrEdHelp.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
				core.Script.Color_DEATH_MAGENTA);
		clrEdHelp.setColor(enumMenuStxtCOLORS.BKG_INACTIVE, 
				core.Script.Color_DEATH_MAGENTA);
		clrEdHelp.enableIcons(false);
		clrEdHelp.enableFrame(false);
		clrEdHelp.setFont(core.Script.fntLogicalScans10 );

		this.colorEditor.setAllGuagesBorder(false);
		this.colorEditor.setPaddingWidthPx(2);
		this.colorEditor.setStatusBarObject(clrEdHelp);
		this.colorEditor.setGuageHeight( 12 );
		this.colorEditor.setHighlight(false);

		// -- finish up

		this.bkgImg = null;
		this.isBkgImg = false;
		this.editColorInPlace = false;
		this.showOverview = false;
		this.showHelp = false;

		int tileRows = this.vsp.getNumtiles() / 
			VmenuVSPeditor.DEFAULT_TILES_PER_ROW;
		this.vspOverview = new VImage(
			VmenuVSPeditor.DEFAULT_TILES_PER_ROW *
				(this.vsp.getTileSquarePixelSize()+2),
			tileRows *	(this.vsp.getTileSquarePixelSize()+2) , 
			Color.BLACK );

		this.tIndex = 0;
		this.updatePreview();
		
		this.loadTile( this.tIndex );

		return;
		}


	/** ---------------- Interface methods --------------------  */
	
	public boolean paint( VImage target )
		{
		if(  this.isBkgImg == true && this.bkgImg != null  )
			{
			target.scaleblit(0, 0, 
				target.getWidth(), target.height, this.bkgImg);
			}
		
		// Update color editor status.
		this.colorEditor.setHighlight( false );
		if( this.cFocus != 2 )
			{
			this.colorEditor.setStatus("RGB : "+
				Integer.toString(gR.getValue()) + " " + 
				Integer.toString(gG.getValue()) + " " +
				Integer.toString(gB.getValue())  ); 
			}

			// Draw a shading box behind the active sub-menus
		switch( this.cFocus )
			{
			case 0:
				target.rectfill( this.sidebar.getX() - 10, this.sidebar.getY()-10,
						this.sidebar.getX() + this.sidebar.getWidth() + 10, 
						this.sidebar.getX() + this.sidebar.getHeight() + 10,  
						this.clrShader  );
				break;

			case 1:		// the color key bar.
//				target.rectfill( 58, 1, 636, 59, this.clrShader );
				target.rectfill( 50, 1, target.getWidth()-102 , 59,
						this.clrShader );
				break;

			case 2:		// Color Editor
				target.rectfill( 4, 322, this.colorEditor.getWidthPx()+28, 
					this.colorEditor.getHeightPx()+344, 
					this.clrShader );
				this.colorEditor.setHighlight(true);
				break;

			default:
				target.rectfill( 240, 50, 516, 326, this.clrShader );
				break;
			}
		
		// Small info panel in upper right.
		target.rectfill( target.getWidth()-100, 0, target.getWidth()-2 , 40,
				Color.BLACK );
		target.rect( target.getWidth()-100, 0, target.getWidth()-2 , 40,
				Color.WHITE );
		target.printString( target.getWidth()-95, 12, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				"Pal: " + Integer.toString( this.cBarIndexSet ) );
		target.printString( target.getWidth()-50, 12, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				 Integer.toString( this.clrs.size() >> 3 ) );
		target.printString( target.getWidth()-95, 24, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				"UN: "+Integer.toString(this.undoStack.size()));
		target.printString( target.getWidth()-50, 24, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				"RE: " +Integer.toString(this.redoStack.size()) );
		target.printString( target.getWidth()-95, 36, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				"T#  "+Integer.toString(this.tIndex) );
		target.printString( target.getWidth()-37, 36, 
				core.Script.fntLogicalScans10, Color.WHITE, 
				Integer.toString(this.vsp.getNumtiles()-1) );


		// Large Brush mode.   Expand selected area.
		if( this.brushSize > 1 )
			{
			this.main.setMultiSelect( true );
			this.main.setAllButtonStates(enumMenuItemSTATE.NORMAL);
			for( int by = 0; by < this.brushSize; by++ )
				{ for( int bx = 0; bx < this.brushSize; bx++ )
					{
					int eIdx = this.main.getSelectedIndex() + 
						(by*this.vsp.getTileSquarePixelSize() + bx);
					if( eIdx > (this.vsp.getTileSquarePixelSize()
								* this.vsp.getTileSquarePixelSize()-1) )
						{
						eIdx -= this.vsp.getTileSquarePixelSize()
								* this.vsp.getTileSquarePixelSize();
						}
					this.main.getMenuItemAsButton(eIdx).setState(
							enumMenuItemSTATE.SELECTED.value() );
				}	}
			}
		else {  this.main.setMultiSelect( false ); }

		// Paint the primary sub-menus objects.
		this.main.paint(target);
		this.sidebar.paint(target);
		this.colorkey.paint(target);
		this.colorEditor.paint(target);
		
		// Tile real-size 3x3 Preview
		int puX = 176;
		int puY = 260;
		target.rect( puX, puY, puX+51, puY+51, Color.BLACK );
		target.rect( puX+1, puY+1, puX+50, puY+50, Color.WHITE );
		for( int py = 0; py < 3; py++ )
			{ for( int px = 0; px < 3; px++ )
				{
				target.blit( puX+2+(px*this.preview.getWidth()), 
					puY+2+(py*this.preview.getHeight() ), 
					this.preview);  
				} }

		// Main frame.
		target.rect( this.main.getX()-2, this.main.getY()-2, 
				this.main.getX()+this.main.getWidth()+1, 
				this.main.getY()+this.main.getHeight()+1, 
				Color.WHITE );
		target.rect( this.main.getX()-3, this.main.getY()-3, 
				this.main.getX()+this.main.getWidth()+2, 
				this.main.getY()+this.main.getHeight()+2, 
				Color.BLACK );
		
		// Active Tool annotations
		if( this.lineTool != -1 )
			{
			int lx = this.main.getMenuItemPxX( this.lineTool );
			int ly = this.main.getMenuItemPxY( this.lineTool );
			int z = this.vsp.getTileSquarePixelSize() / 2;
			target.rect(lx, ly, lx+16, ly+16, Color.WHITE );
			target.rect(lx-1, ly-1, lx+17, ly+17, Color.BLACK );
			target.rect(lx-2, ly-2, lx+18, ly+18, Color.WHITE );
			target.rect(lx-3, ly-3, lx+19, ly+19, Color.BLACK );
			
			int lx2 = this.main.getMenuItemPxX(
					this.main.getSelectedIndex() );
			int ly2 = this.main.getMenuItemPxY( 
					this.main.getSelectedIndex());

			target.line( lx+z, ly+z, lx2+z, ly2+z, Color.WHITE );
			target.line( lx+z+1, ly+z, lx2+z+1, ly2+z, Color.WHITE );
			target.line( lx+z, ly+z+1, lx2+z, ly2+z+1, Color.BLACK );
			target.line( lx+z+1, ly+z+1, lx2+z+1, ly2+z+1, Color.BLACK );
			}
		
		if( this.rectTool != -1 )
			{
			int lx = this.main.getMenuItemPxX( this.rectTool );
			int ly = this.main.getMenuItemPxY( this.rectTool );
			int z0 = this.vsp.getTileSquarePixelSize();
			int z = z0 / 2;
			int curIdx = this.main.getSelectedIndex();
			target.rect(lx, ly, lx+16, ly+16, Color.WHITE );
			target.rect(lx-1, ly-1, lx+17, ly+17, Color.BLACK );
			target.rect(lx-2, ly-2, lx+18, ly+18, Color.WHITE );
			target.rect(lx-3, ly-3, lx+19, ly+19, Color.BLACK );

			int lx2 = this.main.getMenuItemPxX(curIdx );
			int ly2 = this.main.getMenuItemPxY(curIdx); 
			
			target.rect( lx+z, ly+z, lx2+z, ly2+z, Color.WHITE );
			target.rect( lx+z-1, ly+z-1, lx2+z+1, ly2+z+1, Color.BLACK );
			target.rect( lx+z+1, ly+z+1, lx2+z-1, ly2+z-1, Color.BLACK );
			}

		// Areal and circle use the same annotation.
		if( (this.circleTool != -1) ^ (this.arealTool != -1) )
			{
			int i = this.circleTool;
			if( i == -1 )   { i = this.arealTool; }
			int lx = this.main.getMenuItemPxX( i );
			int ly = this.main.getMenuItemPxY( i );
			int z0 = this.vsp.getTileSquarePixelSize();
			int z = z0 / 2;
			int curIdx = this.main.getSelectedIndex();
			target.rect(lx, ly, lx+16, ly+16, Color.WHITE );
			target.rect(lx-1, ly-1, lx+17, ly+17, Color.BLACK );
			target.rect(lx-2, ly-2, lx+18, ly+18, Color.WHITE );
			target.rect(lx-3, ly-3, lx+19, ly+19, Color.BLACK );

			int lx2 = this.main.getMenuItemPxX(curIdx );
			int ly2 = this.main.getMenuItemPxY(curIdx);
			int radX = Math.abs( lx2 - lx );
			int radY = Math.abs( ly2 - ly );
				// Normalize to a true circle.
			if( (this.arealTool != -1) && (radX != radY) )
				{
				if( radX > radY )   { radY = radX; }
				if( radY > radX )   { radX = radY; }
				}

			target.circle( lx+z, ly+z, radX-1, radY-1, Color.BLACK, target );
			target.circle( lx+z, ly+z, radX+1, radY+1, Color.BLACK,target);
			target.circle( lx+z, ly+z, radX, radY, Color.WHITE, target );
			}

		// Tile Selection preview.
		for( int row = -1; row <= 1; row++ )
			{	for( int col = -4; col <= 4; col++ )
				{
				int thistile = this.tIndex + col + 
					(row * DEFAULT_TILES_PER_ROW);
				while( thistile >= this.vsp.getNumtiles() )
					{ thistile -= this.vsp.getNumtiles(); }
				while( thistile < 0 )
					{ thistile += this.vsp.getNumtiles(); }
				int xAdj = col * 33;
				int yAdj = row * 33;
				target.tscaleblit( 350+xAdj, 380+yAdj, 32, 32, 
						this.vsp.getTileAsVImage( thistile ) );
			}	}
		target.rect(349, 379, 383, 413, Color.WHITE );
		target.rect(348, 378, 384, 414, Color.BLACK );
		target.rect(347, 377, 385, 415, Color.WHITE );
		target.rect(346, 376, 386, 416, Color.BLACK );

		if( this.showOverview )
			{
			target.tblit( 0, 0, 
				new VImage(target.getWidth(), target.getHeight(),
					new Color(0.0f,0.0f,0.0f,0.5f)) );
			target.blit( 200, 10, this.vspOverview );	
			}

		return( true );
		}		// End   paint()

	
	public boolean doControls( Integer ext_keycode )
		{
		// Checks all appropriate fields for incoming input
		if( this.processInputs() > 0 ) 
			{ return(true); }

		Integer basecode = Controls.extcodeGetBasecode(ext_keycode);
		boolean isShift = Controls.extcodeGetSHIFT(ext_keycode);
		boolean isCntl = Controls.extcodeGetCNTL(ext_keycode);
		boolean isAlt = Controls.extcodeGetALT(ext_keycode);

			// Eat hotkeys in specific order.
			// These get priority over regular controls.
		if( Vmenu.hasHotKey( this.sidebar, ext_keycode) == true )
			{
			this.exec( Vmenu.getHotKeyMethod( this.sidebar, ext_keycode ),
				Vmenu.getHotKeyMethodArgs( this.sidebar, ext_keycode ) );
			return( true ); 
			}
		if( Vmenu.hasHotKey( this.main, ext_keycode) == true )
			{
			this.exec( Vmenu.getHotKeyMethod( this.main, ext_keycode ),
				Vmenu.getHotKeyMethodArgs( this.main, ext_keycode ) );
			return( true ); 
			}
		if( Vmenu.hasHotKey( this.colorkey, ext_keycode) == true )
			{
			this.exec( Vmenu.getHotKeyMethod( this.colorkey, ext_keycode ),
				Vmenu.getHotKeyMethodArgs( this.colorkey, ext_keycode ) );
			return( true ); 
			}
		if( Vmenu.hasHotKey( this.colorEditor, ext_keycode) == true )
			{
			this.exec( Vmenu.getHotKeyMethod( this.colorEditor, ext_keycode ),
				Vmenu.getHotKeyMethodArgs( this.colorEditor, ext_keycode ) );
			return( true ); 
			}
		
		// normal key overrides.
		switch( basecode )
			{
			case -9999:		// TODO: warning killer -- eventually get rid of it
				this.toggleHelp();
				this.getSelectedColorkey();
				this.modifyColorEditor(100, 100, 100);
				this.nextTile();
				this.prevTile();
				this.newVsp();
				this.saveCurrentVSP();
				this.loadVsp();
				this.setColorPaletteEntry(0, 
						core.Script.Color_DEATH_MAGENTA);
				break;
			case 0:		// silent passthrough - meant to do nothing.
				return(true);
			case 101:
			case 8: 		// BACKSPACE <CANCEL> - change focus.
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				switch ( this.cFocus )
					{
					case 0:
						VmenuVSPeditor.returnToSystem();
						break;
					case 2:		// Return to color key, discarding changes.
						this.cFocus = 1;
						if( this.editColorInPlace == true )
							{
							this.cFocus = 3;
							this.editColorInPlace = false;
							}
						this.setColorEditorToCurrentColorKey();
						break;
					default:
						this.cFocus = 0;
						break;
					}

				return( true );
			case 10: 		// ENTER KEY <CONFIRM>
				// xfer control to the color editor to directly modify this cell.
				if( this.cFocus == 3 && isCntl == true && isShift == true)
					{
					this.setColorEditorToCursor();
					this.cFocus = 2;
					this.editColorInPlace = true;
					return(true);
					}
				this.funcActivate();
				break;
				
			case 33:		// [Page-UP]
				if( isCntl == true )
					{
					this.nextCbarLine();
					this.setColorEditorToCurrentColorKey();
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			
			case 34:		// Page Down
				if( isCntl == true )
					{
					this.prevCbarLine();
					this.setColorEditorToCurrentColorKey();
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			
			case 37: 		// ARROW-[LEFT]
				if( isShift == true )		// Shift working tile 1px left
					{
					this.wrapWorkingImage( -1, 0 );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				if( this.cFocus == 1 )		// Control is in color keybar.
					{ this.setColorEditorToCurrentColorKey(); }
				break;
			case 38: 		// ARROW-UP
				if( this.cFocus == 1 )
					{
					this.nextCbarLine();
					break;
					}
				if( isCntl == true ) 
					{
					this.changeTile( -1 *
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW);
					break;
					}
				if( isShift == true )   // Shift working tile 1px up
					{
					this.wrapWorkingImage( 0, +1 );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;

			case 39: 		// ARROW-RIGHT
				if( isShift == true )	// Shift working tile 1px right
					{
					this.wrapWorkingImage( +1, 0 );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				if( this.cFocus == 1 )		// Control is in color keybar.
					{ this.setColorEditorToCurrentColorKey(); }
				break;

			case 40: 		// ARROW-DOWN
				if( this.cFocus == 1 )
					{
					this.prevCbarLine();
					break;
					}
				if( isCntl == true ) 
					{
					this.changeTile( 
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW);
					break;
					}
				if( isShift == true )	// Shift working tile 1px down
					{
					this.wrapWorkingImage( 0, -1 );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;

			case 44:		// [,]  decrease brush size.
				if( this.lineTool != -1 || this.rectTool != -1 
						|| this.circleTool != -1 )
					{ break; }		// only functions in "brush" mode.
				if( this.brushSize > 1 )
					{ this.brushSize--; }
				break;

			case 46:		// [.]  Increase brush size.
				if( this.lineTool != -1 || this.rectTool != -1 
						|| this.circleTool != -1 )
					{ break; }		// only functions in "brush" mode.
				if( this.brushSize < VmenuVSPeditor.MAX_BRS_SZ )
					{ this.brushSize++; }
				break;

			// the Number keys that do their bizz.
			case 48:	case 49:  case 50:  case 51:  case 52:
			case 53:	case 54:  case 55:  case 56:  case 57: case 45:
				int transform = basecode - 49;
				if( transform == -1 )  { transform = 9; }
				if( transform == -4 )  { transform = 10; }
				if( this.cFocus == 1 ) 	// we are on color key
					{
					// Swaps colors, selected with #
					this.setUndoPoint( this.getColorPaletteCopy() , 1 );
					this.swapColorKeyColor( 
						this.colorkey.getSelectedIndex(), transform );
					break;
					}
				if( this.cFocus == 3 )   // focus on main.
					{
					// Sets a single pixel.
					this.saveMainTileState( 2 );
					Color cbarC = this.getColorkeyColor( transform );
					if( this.arealTool != -1 )
						{
						this.randomSpray( this.arealTool, 
							this.main.getSelectedIndex(),
							this.arealToolDensity, 
							cbarC, this.arealToolCRandom  );
						break;
						}
					// Time to draw a line for selected color.
					if( this.lineTool != -1 )
						{
						this.saveMainTileState( 3 );
						this.line( this.lineTool,
							this.main.getSelectedIndex(),
							this.getColorkeyColor(	transform) );
						if( this.lineContiguous == true )
							{ this.lineTool = this.main.getSelectedIndex(); }
						break;
						}
					// Time to draw a rectangle for this color.
					if( this.rectTool != -1 )
						{
						this.saveMainTileState( 3 );
						this.rect( this.rectTool,
							this.main.getSelectedIndex(),
							this.getColorkeyColor(	transform) );
						if( this.rectContiguous == true )
							{ this.rectTool = this.main.getSelectedIndex(); }
						break;
						}
					// draw circles
					if( this.circleTool != -1 )
						{
						this.saveMainTileState( 3 );
						this.circle( this.circleTool,
								this.main.getSelectedIndex(),
								this.getColorkeyColor(	transform) );
						break;
						}
					// Clear tile to a solid color
					if( isCntl == true && isAlt == true && isShift == false)
						{
						this.saveMainTileState( 4 );
						this.clearWorkingTile(
							this.getColorkeyColor(	transform) );
						break;
						}
					//  Full tile Color Replacer
					if( isCntl == false && isAlt == true && isShift == true)
						{
						this.saveMainTileState( 5 );
						this.replacer(this.getColorCursorCell(),
							this.getColorkeyColor(transform)	);
						break;
						}
					// "Dropper"  sets a color key from the current selected.
					if( isCntl == true && isAlt == false && transform > 0 ) 
						{
						this.setUndoPoint( this.getColorPaletteCopy() , 6 );
						this.setColorkeyColor( transform, 
								this.getColorCursorCell());
						break;
						}
					if( this.brushSize > 1 )
						{
						this.largeBrush( this.getColorkeyColor(transform) );
						break;
						}
					this.setCurrentCell( transform );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
				
			case 65: 		// [a]  Areal random spray tool toggle
				if( this.cFocus != 3 )	{ break; }
				if( this.arealTool == -1 )
					{
					this.disableAllTools();
					if( isCntl == true )
						{ this.arealToolDensity = 10; }
					if( isShift == true ) 
						{ this.arealToolCRandom = true; }
					this.arealTool = this.main.getSelectedIndex();
					}
				else			// actually create line. - reset mark.
					{
					this.main.setSelectedIndex(this.arealTool);
					this.arealTool = -1;
					this.arealToolCRandom = false;
					this.arealToolDensity = 3;
					}
				break;

			case 67:		// [c] Copy
				// tileset with padding.
				if( isCntl == true && isShift == true )	
					{
					this.vsp.exportToClipboard(
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW,
						true );
					break;
					}
			 	// 	Entire Tileset to clipboard
				if( isCntl == true && isShift == false ) 
					{
					this.vsp.exportToClipboard(
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW,
						false );
					break;
					}
				if( isShift == true  && isCntl == false )
					{
					// TODO : copy/add to tile group.
					break;
					}
				// plain c copies only the working tile.
				System.out.println("Working tile copied to clipboard.");
				this.copyWorkingTile().copyImageToClipboard();
				break;
			case 68:		// [d] Debug (for now)
//
				this.wrapWorkingImage( 0, -1 );
				break;

			case 70:		// [f] : flood fill tool
				if( this.cFocus != 3 )  { break; }
				Color thisColor = this.clr1st;
				int tolerance = 0;
				if( isShift == true )  { thisColor = this.clr2nd; }
				if( isCntl == true )	{ tolerance += 200; }
				if( isAlt == true )		{ tolerance += 384; }
				int selidx = this.main.getSelectedIndex();
				this.saveMainTileState( 8 );
				this.floodFiller( 
					selidx % this.vsp.getTileSquarePixelSize(),
					selidx / this.vsp.getTileSquarePixelSize(), 
					thisColor, tolerance );
				break;

			case 73:		//  [i] - invert function.
				if( isCntl == true )		
					{ this.invertAllCells(); break; }
				switch( this.cFocus )
					{
					case 1:		// Invert the color key.
					this.setUndoPoint( this.getColorPaletteCopy() , 9 );
						this.setColorkeySelectedColor( 
							VmenuVSPeditor.invertColor(
								this.getColorkeySelectedColor() ));
						this.setColorEditorToCurrentColorKey();
						break;
					case 3:		// invert a single cell
					this.saveMainTileState( 9 );
						this.invertSelectedCell();
						this.setColorEditorToCursor();
						break;
					default:
						break;
					}
				break;

			case 76:			// [l]  Load functions.
				if( isCntl == true )  { break; }		// eaten by hotkey.
				if( this.cFocus == 1 )
					{
					this.loadPalette();
					break;
					}
				if( this.cFocus == 3 )		// Toggle Line mode
					{
					if( this.lineTool == -1 )	// turn on. mark starting pt.
						{
						this.disableAllTools();
						this.lineTool = this.main.getSelectedIndex();
						this.lineContiguous = isShift;
						}
					else			// actually create line. - reset mark.
						{
						this.main.setSelectedIndex( this.lineTool );
						this.lineTool = -1;
						this.lineContiguous = false;
						}
					}
				break;

			case 78:			// [n] new
				System.out.println("n pressed");
				switch( this.cFocus )
					{
					case 1:		// New Basic Color Key
						if( isCntl == true )
							{
							this.setBasicPalette();
							break;
							}
						// New palette color.
						Integer targetidx = 
							this.colorkey.getSelectedIndex();
						if( targetidx >= 3 )
							{
							this.insertPaletteColor( (targetidx-3) 
									+ (this.cBarIndexSet*8) );
							}
						else
							{
							this.insertPaletteColor( (this.cBarIndexSet*8) );
							}
						break;

					case 3:		// new Tile (replicate tile)
						if( isCntl == true )
							{
							this.vsp.insertTile(this.tIndex, new VImage(
								this.vsp.getTileSquarePixelSize(),
								this.vsp.getTileSquarePixelSize(),
								this.getColorkeySelectedColor() 
								).getImage());
							}
						else 	{
							this.vsp.insertReplicaTile(this.tIndex);
							}
						this.refresh();
						this.updateMiniTileset();
						this.loadWorkingTile();
						break;
					default:		// unused.
						this.getControlItem().doControls(ext_keycode);
						break;
					}
				break;

			case 79:		// [o]  r[o]tation and mirroring.
				if( this.cFocus != 3 )		{ break; }
				if( isCntl == true )
					{ this.rotate( +1, false ); }
				else if( isAlt == true )
					{ this.rotate( +2, false ); }
				else if( isShift == true )
					{ this.rotate( 0, true ); }
				else
					{ this.rotate( -1, false ); } 
				break;

			case 82: 		// [r]  Rectangles
				if( this.cFocus != 3 )	{ break; }
				if( this.rectTool == -1 )	// toggle rectangle tool marker
					{
					this.disableAllTools();
					this.rectTool = this.main.getSelectedIndex();
					this.rectContiguous = isShift;
					}
				else			// actually create line. - reset mark.
					{
					this.main.setSelectedIndex(this.rectTool);
					this.rectTool = -1;
					this.rectContiguous = false;
					}
				break;

			case 83:			// [s] Save functions.
				if( isShift == true )
					{
					this.savePalette();
					break;
					}
				// Note plain [s] triggers "save working tile" KC 664 {menu}
				break;
				
			case 84:		// [t]  transparency & tile viewer
				if( isCntl == true )	{ break; }		// Eaten by hotkey
				if( this.cFocus == 1 )
					{	// Set selected color to transparent.
					this.setColorkeySelectedColor(this.clrTrans);
					}
				break;

			case 86:		//  [v] - paste functions
			
				if( isCntl == true && isAlt == true )
					{		// Full VSP inport from clipboard.
					this.handleVspPaste(core.Script.getClipboardVImage());
					break;					
					}
				if( isCntl == true )
					{		// tile paste + working save.
					this.handleTilePaste( core.Script.getClipboardVImage() );
					this.saveWorkingTile();
					break;
					}
				if( isShift == true )
					{		// Insert tiles from clipboard.
					this.handleTileInsert( 
							core.Script.getClipboardVImage() );
					this.saveWorkingTile();
					break;
					}
				this.handleTilePaste( core.Script.getClipboardVImage() );
				break;
				
			case 87: 		// [w]  Toggle Circle tool
				if( this.cFocus != 3 )	{ break; }
				if( this.circleTool == -1 )
					{
					this.disableAllTools();
					this.circleTool = this.main.getSelectedIndex();	
					}
				else			// actually create line. - reset mark.
					{
					this.main.setSelectedIndex(this.circleTool);
					this.circleTool = -1;
					}
				break;

			case 90:		// [z] Undo and Redo.
				if( isCntl == true )
					{ 
					this.undo();  
					break;
					}
				this.redo();
				break;

			case 127:		// [Delete] 
				switch( this.cFocus )
					{
					case 1:		// Delete current palette entry
						Integer targetidx = 
							this.colorkey.getSelectedIndex();
						System.out.println("Delete Pal @ "+targetidx.toString());
						if( targetidx >= 3 )
							{
							this.removePaletteColor( (targetidx-3) 
									+ (this.cBarIndexSet*8) );
							}
						break;

					default:		// Delete a current tile from vsp.
						if( this.vsp.getNumtiles() <= 1 )	
							{ break; }		// refuse to delete final tile.
						this.vsp.spliceTile(this.tIndex);
						if( this.tIndex == this.vsp.getNumtiles() )
							{ this.tIndex--; }
						this.refresh();
						this.updateMiniTileset();
						this.updatePreview();
						this.loadWorkingTile();
						break;
						}
				break;
		
			case 109:		// [Numpad - ]  remove tiles or colors.
				break;
		
			default:
				System.out.println(" VmenuVSPeditor: "
						+ " delegated unhandled keystroke > "
						+ Integer.toString(ext_keycode) );
				this.getControlItem().doControls(ext_keycode);
				break;
			}

		if( this.cFocus == 3 )
			{ this.setColorEditorToCursor(); }

		return(true);
		}


	// This is a full screen menu -- thus moving it makes no sense.
	public void moveAbs(int x, int y)
		{	return;	}
	public void moveRel(int x, int y)
		{	return;	}

	public Integer countMenuItems()
		{	return(0);	}

	// Disabled - cannot add or remove stuff to this. 
	public Integer addItem(Vmenuitem vmi)
		{ return(null); }
	public Vmenuitem popItem()
		{ return(null); }
	public Vmenuitem removeItem(int index)
		{ return(null); }
	public Integer insertItem(Vmenuitem vmi, int index)
		{ return(null); }

	public void refresh()
		{
		this.main.refresh();
		this.sidebar.refresh();
		this.colorkey.refresh();
		this.colorEditor.refresh();
		this.setChildID(this.getControlItem().getFocusId());
		return;
		}

	public Vmenuitem getMenuItem(int index)
		{	return( null );	}
	public Vmenuitem getMenuItemSelected()
		{	return( this.getControlItem().getMenuItemSelected() );	}

	public Vmenuitem getMenuItemByID(Long id)
		{
		return null;
		}

	// In this menu case,  the selected index is the active Vmenu 
	public Integer getSelectedIndex()
		{	return(this.cFocus);	}
	public int getSelectedIndexPosX()
		{	return(0);  }
	public int getSelectedIndexPosY()
		{	return(0);  }
	public void setSelectedIndex(Integer index)
		{	this.cFocus = index;	}

	public void setFocusId(Long id)
		{	this.focusID = id;	}

	public Long getFocusId()
		{	return(this.focusID);	}

	public boolean isFocus(Long id)
		{
		if (id == this.focusID )
			{	return (true);	  }
		return (false);
		}


	// Visibility and Active-ness are not applicable.  They are both - always.
	public void setActive(boolean active)
		{ return;	}
	public void setVisible(boolean vis)
		{ return;	}

	public void setBackgroundImage(VImage bkg, boolean onOff)
		{
		this.bkgImg = bkg;
		this.isBkgImg = onOff;
		}

	public boolean isActive()
		{	return(true);	}
	public boolean isVisible()
		{	return(true);	}

	public void activateSelected()
		{	this.getControlItem().activateSelected();	}
	
	public void attachSound(enumMenuEVENT slot, VSound sfx)
		{
		if (sfx == null)
			{	return;	}
		this.hmSounds.put(slot, sfx);
		return;
		}

	public boolean playMenuSound(enumMenuEVENT slot, 
			int volume0to100)
		{
		if (this.hmSounds == null)
			{
			return (false);
			}
		if (this.hmSounds.get(slot) == null)
			{
			return (false);
			}
		this.hmSounds.get(slot).start(volume0to100);
		return (true);
		}

	public void setIconsAll(boolean onOff)
		{
		this.main.setIconsAll(onOff);
		this.sidebar.setIconsAll(onOff);
		this.colorkey.setIconsAll(onOff);
		this.colorEditor.setIconsAll(onOff);
		return;
		}

	public void setBorderAll(boolean onOff, int thick)
		{
		this.main.setBorderAll(onOff, thick);
		this.sidebar.setBorderAll(onOff, thick);
		this.colorkey.setBorderAll(onOff, thick);
		this.colorEditor.setBorderAll(onOff, thick);
		return;
		}

	public void setParentID(Long id, boolean recursive)
		{
		this.parentID = id;
		if( recursive == true )
			{
			this.main.setParentID(id, true);
			this.sidebar.setParentID(id, true);
			this.colorkey.setParentID(id, true);
			this.colorEditor.setParentID(id, true);
			}
		return;
		}

	public void setChildID(Long id, boolean recursive)
		{
		this.childID = id;
		if( recursive == true )
			{
			this.main.setChildID(id, true);
			this.sidebar.setChildID(id, true);
			this.colorkey.setChildID(id, true);
			this.colorEditor.setChildID(id, true);
			}
		}


	/** ----------------  Non-Interface Methods  --------------------  */
	
	private Vmenu getControlItem()
		{
		switch( this.cFocus )
			{
			case 0:
				return( this.sidebar );
			case 1:
				return( this.colorkey );
			case 2:
				return( this.colorEditor );
			default:
				return( this.main );

			}
		}

	/**  Processes Enter key actions.
	 *  Intercepts (Enter key) to be handled differently by focus */
	private void funcActivate()
		{
		switch( this.cFocus )
			{
			// Yank out the action method and call it at menu level..
			//     This is so it can access the special Methods here.
			case 0:		// Nav side bar
				this.exec(
					this.sidebar.getMenuItemSelected().getAction(),
					this.sidebar.getMenuItemSelected().getActionArgs() );
				break;

			case 1:		// Color key bar - edit this color pod
				this.cFocus = 2;
				break;

			case 2:		// Color Editor - Save color, back to palette.
				Color nc = new Color( this.gR.getValue(), 
					this.gG.getValue(), this.gB.getValue(), 255 );
				if( this.editColorInPlace == true )
					{	// Directly edit the cursor cell.
					this.cFocus = 3;
					this.editColorInPlace = false;
					Color oc = this.getCursorCell().getColorComponent( 
						enumMenuButtonCOLORS.BODY_ACTIVE.value() );
					if( oc.getRGB() == nc.getRGB() )
						{	// Value actually was not changed....  cancel.
						break;
						}

					this.saveMainTileState( 7 );
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_ACTIVE , nc );
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_INACTIVE , nc);
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_SELECTED , nc);
					
					break;
					}
				// Else - we return it to the palette.
				this.cFocus = 1;
				
				int cidx = this.getSelectedColorkeyCIDX();
				// Actually abort..... color wasn't changed.
				if( nc.getRGB() == this.clrs.get(cidx).getRGB() ) 
					{ break; }

				this.setUndoPoint( this.getColorPaletteCopy() , 28 );

				HashMap<Integer,Color> hmTmp = 
					new HashMap<Integer,Color>();
				hmTmp.put(
					enumMenuButtonCOLORS.BODY_ACTIVE.value(), nc );
				hmTmp.put(
					enumMenuButtonCOLORS.BODY_INACTIVE.value(),nc); 
				hmTmp.put(
					enumMenuButtonCOLORS.BODY_SELECTED.value(),nc); 						 
				this.colorkey.getMenuItemSelected().setColorContent(
					hmTmp );
				if( cidx != -1 )
					{ this.clrs.put( cidx, nc ); }
				break;

			default:		// If unhandled, the sub menu object itself will.
				this.getControlItem().doControls(10);
				break;
			}
		return;
		}

	/** Similar, but not identical to menuitem's doAction() method -  */
	private boolean exec( Method action, Object[] args )
		{
		if( action == null ) 	{ return(false); }
			// Arg checks.
 		Type[] pType = action.getGenericParameterTypes();
 			// arguments are definately not right.  Stop.
 		if( pType.length != args.length )
 			{
 			System.err.println( "Prevented activation of method : "+
				action.getName() + " due to argument mismatch!" );
 			return(false); 
 			}
		
		System.out.println("DEBUG : Attempting to activate method : "+
				action.getName() + " with " + 
				Integer.toString( args.length ) + " args (" +
				action.getModifiers() + ")."  );
		if( args.length > 0 )
			{
			Integer n = -1;
			for( Object c : args )
				{
				n++;
				System.out.println(" ARG "+n.toString()+
					": Taken Type " + c.getClass().getTypeName()  +
					" :: Given Type "+c.getClass().getName() );
				if( pType[n].equals( c.getClass() ) == false ) 
					{
					System.err.println("doAction: Prevented type mismatch.");
					}
				}
			}

		try { 			// 	call the method. and prey
//			this.myAction.invoke(null);

	        if( ( action.getModifiers() & Modifier.STATIC) 
	     		   == Modifier.STATIC  )
	     	   { action.invoke( null, args ); }
	        else
	     	   {
	     	   boolean localTest = false;
	     	   for( Method mTest : this.getClass().getDeclaredMethods() )
	     		   {
	     		   if( mTest.getName().equals( action.getName() ))
	     			   { localTest = true; }
	     		   }
	     	   if( localTest == false )		
	     		   {
	     		   System.err.println("Invoking instanced method not "+
     				   "belonging to the local Class is unsupported!  "+
     				   "Either make it a static method or add it to this Class");
	     		   return(false); 
	     		   }
	     	   else
	     		   {   action.invoke( this, args );   }
	     	   }
			}
		catch(Exception e)
			{
			e.printStackTrace();
			System.err.println( e.getMessage() );
			return(false); 
			}

		return(true);		
		}

	private static void returnToSystem()
		{
		setMenuFocus( 0, VergeEngine.Vmm.getSystemMenuFocusID() );
		return;
		}

	public static void focusColorEditor( VmenuVSPeditor me )
		{  me.setSelectedIndex(2);  return; }
	public static void focusMain( VmenuVSPeditor me )
		{  me.setSelectedIndex(3);  return; }
	public static void focusSideBar( VmenuVSPeditor me )
		{  me.setSelectedIndex(0);  return; }
	public static void focusColorKeyBar( VmenuVSPeditor me )
		{  me.setSelectedIndex(1);  return; }
	public static void focusIncrement( VmenuVSPeditor me )
		{  
		int tmp = me.getSelectedIndex();
		tmp++;
		if( tmp > 3 )	{ tmp = 0; }
		me.setSelectedIndex(tmp); 
		return; 
		}
	public static void focusDecrement( VmenuVSPeditor me )
		{  
		int tmp = me.getSelectedIndex();
		tmp--;
		if( tmp < 0 )	{ tmp = 3; }
		me.setSelectedIndex(tmp); 
		return; 
		}

	public void focusSubMenuIndex( Integer newIndex )
		{
		this.setSelectedIndex(newIndex);
		return;
		}

/**  Sets the working tile to an arbituary VImage.
 * If source is larger, this will only use the top x,y pixels.
 * There is no padding involved. 
 * @param vi  Any Source VImage
 */
	private void loadWorkingImage( VImage vi )
		{
		int z = this.vsp.getTileSquarePixelSize();
		if( vi.getWidth() < z )		{ return; }
		if( vi.getHeight() < z )		{ return; }
		
		Color cTmp;
		
		for( int y = 0; y < z; y++ )
			{ 
			for( int x = 0; x < z; x++ )			
				{
				int idx = (y*z)+x;
				if( idx >= this.main.countMenuItems() )
					{ continue; }
				cTmp = vi.getPixelColor(x, y);
				if( VImage.colorRGBcomp(cTmp, this.clrTrans ) == true )
					{ cTmp = this.clrTrans; }
				VmiButton element = 
						(VmiButton) this.main.getMenuItem(idx);
				element.setColorComponent(
					enumMenuButtonCOLORS.BODY_INACTIVE, 
					cTmp );
				element.setColorComponent(
					enumMenuButtonCOLORS.BODY_ACTIVE, 
					cTmp );
				element.setColorComponent(
					enumMenuButtonCOLORS.BODY_SELECTED, 
					VmenuVSPeditor.invertColor( cTmp ) );
			}	}
		return;
		}


/**  Loads up a vsp tile into the working data.
 * If there are problems... tile # 0 is loaded.
 * @param vspIdx   The vsp tile index to load.
 */
	private void loadTile( int vspIdx )
		{
		System.out.println( " load tile # "+Integer.toString(vspIdx) );
		if( this.vsp.checkTile(vspIdx) == false )
			{  vspIdx = 0; }
		this.tIndex = vspIdx;
		VImage t = new VImage( this.vsp.getTileSquarePixelSize(),
			this.vsp.getTileSquarePixelSize() );
		t.setImage( this.vsp.getTiles()[vspIdx] );
		this.loadWorkingImage(t);
		this.setColorEditorToCursor();
		this.updatePreview();
		this.updatePreview();
		return;
		}

	/** (re) loads the data from current VSP into the working display.*/
	private void loadWorkingTile()
		{
		this.loadTile( this.tIndex );
		this.updatePreview();
		}

	/* Changes the displayed working tile by a offset */
	private void changeTile( int indexDiff )
		{
		int toNum = this.tIndex + indexDiff;
		while( toNum >= this.vsp.getNumtiles() )
			{ toNum -= this.vsp.getNumtiles(); }
		while( toNum < 0 )
			{ toNum += this.vsp.getNumtiles(); }

		this.changeTileAbs(toNum);
		return;
		}
	
	/* Changes the displayed working tile to exact vsp index */
	private void changeTileAbs( int index )
		{
		this.tIndex = index;
		if( this.tIndex < 0 )   { this.tIndex = 0; }
		if( this.tIndex >= this.vsp.getNumtiles() )   
			{ this.tIndex = this.vsp.getNumtiles()-1; }
		this.loadTile( this.tIndex );
		return;
		}
	
	private void nextTile()
		{
		this.changeTile( +1 );
		return;
		}

	private void prevTile()
		{
		this.changeTile( -1 );
		return;
		}
	
	private void setCbarLine( int lineIndex )
		{
		this.cBarIndexSet = lineIndex;
		if( this.clrs.size() <= 8 )		// dude, theres only 1 line of colors!
			{ this.cBarIndexSet = 0;  }
		
		int cbarsets = (this.clrs.size() >> 3);
		if( this.cBarIndexSet > cbarsets )
			{ this.cBarIndexSet = 0; }
		if( this.cBarIndexSet < 0  )
			{ this.cBarIndexSet = cbarsets; }

		Color theColor = null;
		for( Integer b = 0; b < 8; b++ )
			{
			int bn = b+3;
			int bc = (this.cBarIndexSet*8)+b;
			theColor = this.clrs.get(bc);
			HashMap<Integer,Color> tmp = 
					new HashMap<Integer,Color>();
			tmp.put(enumMenuButtonCOLORS.BODY_ACTIVE.value(), 
					theColor );
			tmp.put(enumMenuButtonCOLORS.BODY_INACTIVE.value(),
					theColor );
			tmp.put(enumMenuButtonCOLORS.BODY_SELECTED.value(), 
					theColor );
			this.colorkey.getMenuItem(bn).setColorContent( tmp );
			}
		return;
		}

	private void nextCbarLine()
		{
		this.setCbarLine( ++this.cBarIndexSet );
		return;
		}
	private void prevCbarLine()
		{
		this.setCbarLine( --this.cBarIndexSet );
		return;
		}
	
	/**  Applies color to one working pixel.  Given a color key number */
	private void setCell( int cellIdx, int keyNum )
		{
		if( this.colorkey.getMenuItem(keyNum) == null )
			{ return; }
		this.setCell( cellIdx, 
			this.colorkey.getMenuItem(keyNum).getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value() )	);
		return;
		}
	/** Set a cell index in the main working tile to Color c. */
	private void setCell( int cellIdx, Color c )
		{
		int z = this.vsp.getTileSquarePixelSize();
		while( cellIdx < 0 )  { cellIdx += (z*z); }
		while( cellIdx >= (z*z) )  { cellIdx -= (z*z); }

		HashMap<Integer,Color> hmTmp = 
				new HashMap<Integer,Color>();
		hmTmp.put(enumMenuButtonCOLORS.BODY_ACTIVE.value(),  c );
		hmTmp.put(enumMenuButtonCOLORS.BODY_INACTIVE.value(), c);
		hmTmp.put(enumMenuButtonCOLORS.BODY_SELECTED.value(), 
				VmenuVSPeditor.invertColor(c) );
		this.main.getMenuItem(cellIdx).setColorContent( hmTmp );
		return;
		}

	
	
	private void setCurrentCell( int keyNum )
		{	this.setCell( this.main.getSelectedIndex(), keyNum );	}
	
	/** Inverts a color.  By components. Leaves alpha alone. */
	private static Color invertColor( Color in )
		{
		if( in == null )	{ return(in); }
		return( new Color( 255 - in.getRed(), 255 - in.getGreen(), 
				255 - in.getBlue(), in.getAlpha() ) );
		}

	/** Moves the data from the menu object to the actual VSP. 
	 * Triggered by pressing [s] menus hotkey */
	private void saveWorkingTileAs( int vspIdx )
		{
		if( vspIdx < 0 ) { return; }
		if( vspIdx > this.vsp.getNumtiles() ) { return; }
		this.setUndoPoint( new Vsp(this.vsp), 27 );
		VImage output = this.copyWorkingTile();
		vsp.modifyTile( vspIdx, output.getImage() );
		this.updatePreview();
		}

/** Snatches the colors in the working tile into a VImage.
 * Opposite loadWorkingImage().  
 * Probably inefficient, so donot abuse it. */
	private VImage copyWorkingTile()
		{
		int z = vsp.getTileSquarePixelSize();
		VImage output = new VImage( z,z );
		for( Integer y = 0; y < z; y++ )
			{	for( Integer x = 0; x < z; x++ )
				{
				output.setPixel( x, y, this.main.getMenuItem(
					(y*z)+x).getColorComponent(
					enumMenuButtonCOLORS.BODY_ACTIVE.value() ));
			}	}
		return(output);
		}

	/** Moves the data from the menu object to the actual VSP.
	 * Saves the current work to the currently active tile.  */
	private void saveWorkingTile( )
		{
		this.saveWorkingTileAs( this.tIndex );
		return;
		}
	
	private void updatePreview()
		{
		this.preview = this.vsp.getTileAsVImage( this.tIndex );
		return;
		}

	public Vsp exportVsp()
		{	return( new Vsp(this.vsp) );	}

	public Long getParentID()
		{	return parentID;	}
	public void setParentID(Long parentID)
		{	this.parentID = parentID;	}
	public Long getChildID()
		{	return childID;	}
	public void setChildID(Long childID)
		{	this.childID = childID;	}
	
	/** immediately changes to color editor values to a r,g,b */
	private void setColorEditor( int r, int g, int b )
		{
		this.gR.setValue(r);
		this.gG.setValue(g);
		this.gB.setValue(b);
		return;
		}
	private void setColorEditor( Color c )
		{
		if( c == null )	{ this.setColorEditor( 0,0,0 ); return; }
		this.setColorEditor(c.getRed(), c.getGreen(), c.getBlue() );
		return;
		}
	
	/** Alters r,g,b values by a given amount. */
	private void modifyColorEditor( int relR, int relG, int relB )
		{
		this.gR.setValueRelative(relR, false);
		this.gG.setValueRelative(relG, false);
		this.gB.setValueRelative(relB, false);
		return;
		}
	private void setColorEditorToCurrentColorKey()
		{
		Color tmp = 
		this.colorkey.getMenuItemSelected().getColorComponent(
			enumMenuButtonCOLORS.BODY_ACTIVE.value());
		this.setColorEditor( tmp );
		return;
		}

	/** Sets the color editor to the cell color under the cursor in main */
	private void setColorEditorToCursor()
		{
		Color tmp = 
		this.main.getMenuItemSelected().getColorComponent(
			enumMenuButtonCOLORS.BODY_ACTIVE.value());
		this.setColorEditor( tmp );
		return;
		}

	private void invertCell( int mainIndex )
		{
		Color c = VmenuVSPeditor.invertColor(
				this.main.getMenuItem(mainIndex).getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value()) );
		HashMap<Integer,Color> hmTmp = 
				new HashMap<Integer,Color>(); 
		hmTmp.put(enumMenuButtonCOLORS.BODY_ACTIVE.value(), c );
		hmTmp.put(enumMenuButtonCOLORS.BODY_INACTIVE.value(),c);
		hmTmp.put(enumMenuButtonCOLORS.BODY_SELECTED.value(),c);
		this.main.getMenuItem(mainIndex).setColorContent(	 hmTmp );
		return;
		}
	private void invertSelectedCell( )
		{
		this.invertCell( this.main.getSelectedIndex() );
		return;
		}
	
	private VmiButton getCursorCell()
		{	return( (VmiButton) this.main.getMenuItemSelected() );	}
	private Color getColorCursorCell()
		{
		return( getCursorCell().getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value()) ); 
		}
	/** Obtain the Color of any cell # in main area. */
	private Color getColorCellIndex( int cellIndex )
		{
		if( cellIndex < 0 )   { cellIndex = 0; }
		if( cellIndex > (this.vsp.getTileSquarePixelSize() * 
				this.vsp.getTileSquarePixelSize()-1) ) 
			{ 
			cellIndex = (this.vsp.getTileSquarePixelSize() * 
					this.vsp.getTileSquarePixelSize()-1); 
			}
		
		return( 
			this.main.getMenuItemAsButton(cellIndex).getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value() )); 
		}
	/* States the working tile as an array of colors */
	private Color[] getColorTileState( )
		{		
		int cellsize = this.vsp.getTileSquarePixelSize() * 
				this.vsp.getTileSquarePixelSize(); 
		Color[] maindata = new Color[cellsize];
		for( int x = 0; x < cellsize; x++ )
			{
			maindata[x] = this.main.getMenuItemAsButton(
				x).getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value() );			
			}
		return( maindata );
		}

	
	// Utilities for manipulating color key bar.
	
	private void setColorPaletteEntry( int paletteIndex, Color c )
		{ 						 
		this.clrs.put( paletteIndex, c );
		this.setCbarLine( this.cBarIndexSet );
		return;
		}
	
	/* Returns a newly instantiated copy of the color palette  */
	private HashMap<Integer,Color> getColorPaletteCopy()
		{
		HashMap<Integer,Color> cp = new HashMap<Integer,Color>();
		for( int x : this.clrs.keySet() )
			{
			Color nc = new Color( this.clrs.get(x).getRGB() );
			cp.put( new Integer(x), nc );
			}
		return(cp);
		}

	private VmiButton getSelectedColorkey()
		{
		return( (VmiButton) this.colorkey.getMenuItemSelected() );
		}
	
	/**  Return the clrs index associated with the color key selection 
	 *   returns -1 if one of the 3 tool colors are selected. */
	private int getSelectedColorkeyCIDX()
		{
		Integer tmp = this.colorkey.getSelectedIndex();
		if( tmp >= 0 && tmp <= 2 )	{ return(-1); }
		return( this.cBarIndexSet*8 + (tmp - 3) );
		}

	private Color getColorkeyColor( int colorKeySlotNumber )
		{
		if( colorKeySlotNumber == 0 )	{ return(this.clrTrans); }
		if( colorKeySlotNumber == 1 )	{ return(this.clr1st); }
		if( colorKeySlotNumber == 2 )	{ return(this.clr2nd); }
		return( this.clrs.get( 
			this.cBarIndexSet*8 + (colorKeySlotNumber - 3) ) );
		}
	private Color getColorkeySelectedColor( )
		{
		return( this.getColorkeyColor( this.colorkey.getSelectedIndex() ) );
		}

	/** Directly Change a color keybar setting. */
	private void setColorkeyColor( int colorKeySlotNumber, Color c )
		{
		this.setUndoPoint( this.getColorPaletteCopy() , 11 );
		if( colorKeySlotNumber == 1 )
			{ this.clr1st = c;   }
		if( colorKeySlotNumber == 2 )
			{ this.clr2nd = c; }
		
		HashMap<Integer,Color> hmTmp = 
			new HashMap<Integer,Color>(); 
		hmTmp.put(enumMenuButtonCOLORS.BODY_ACTIVE.value(), c );
		hmTmp.put(enumMenuButtonCOLORS.BODY_INACTIVE.value(),c);
		hmTmp.put(enumMenuButtonCOLORS.BODY_SELECTED.value(),c);
		
		this.colorkey.getMenuItem(colorKeySlotNumber).setColorContent(
				hmTmp);

		if( colorKeySlotNumber >= 3 )
			{
			this.clrs.put( this.cBarIndexSet*8 + (colorKeySlotNumber - 3),
					c);
			}

		return;
		}
	private void setColorkeySelectedColor( Color c )
		{ 
		setColorkeyColor( this.colorkey.getSelectedIndex(), c );
		return; 
		}

	/** In one function, inverts all cells in the main area. */
	private void invertAllCells()
		{
		this.saveMainTileState( 16 );
		for( int vmi = 0; vmi < this.main.countMenuItems(); vmi++ )
			{
			HashMap<Integer,Color> hmTmp = 
					new HashMap<Integer,Color>();
			hmTmp.put( enumMenuButtonCOLORS.BODY_ACTIVE.value(), 
				VmenuVSPeditor.invertColor(
					this.main.getMenuItem(vmi).getColorComponent(
					enumMenuButtonCOLORS.BODY_ACTIVE.value())
					)	);
			hmTmp.put( enumMenuButtonCOLORS.BODY_INACTIVE.value(), 
				VmenuVSPeditor.invertColor(
					this.main.getMenuItem(vmi).getColorComponent(
					enumMenuButtonCOLORS.BODY_INACTIVE.value())
					)	);
			hmTmp.put( enumMenuButtonCOLORS.BODY_SELECTED.value(), 
				VmenuVSPeditor.invertColor(
					this.main.getMenuItem(vmi).getColorComponent(
					enumMenuButtonCOLORS.BODY_SELECTED.value())
					)	);
			this.main.getMenuItem(vmi).setColorContent(hmTmp);
			}
		return;
		}
	
	private void swapColorKeyColor( int slot1, int slot2 )
		{
		this.setUndoPoint( this.getColorPaletteCopy() , 12 );
		if( slot2 == 0 )  { return; }	// cannot swap with static trans c
		if( slot1 == slot2 )	{ return; }		// no point.
		Color tmp = this.getColorkeyColor( slot1 );
		this.setColorkeyColor(slot1, this.getColorkeyColor( slot2 ) );
		this.setColorkeyColor(slot2, tmp );
		return;
		}


	/** takes the top x/y pixels of the returned VImage and uses it
	 * to buld a new working tile.
	 * WILL NOT SAVE THE WORKING TILE */
	private boolean handleTilePaste( VImage clipboardVImage )
		{
		if( clipboardVImage == null )	
			{ System.err.print("Failed tile paste."); return(false); }
		if(clipboardVImage.getWidth() < this.vsp.getTileSquarePixelSize() ||
		   clipboardVImage.getHeight() < this.vsp.getTileSquarePixelSize() )
			{ System.err.print("Failed tile paste.II "); return(false); }
		this.loadWorkingImage( clipboardVImage );
		return(true);
		}

	/** Paste a VSP from an image in the clipboard. 
	 * Compatibility with external editors is unknown */
	private boolean handleVspPaste( VImage clipboardVImage )
		{
		if( clipboardVImage == null )
			{ 
			System.err.println("VSP Paste : non-compatible data"); 
			return(false);
			}
		int inX = clipboardVImage.getWidth();
		int inY = clipboardVImage.getHeight();
		Integer tcx, tcy;	// tile count in x and y.
		int z = this.vsp.getTileSquarePixelSize();
		Integer inportCount = 0;
		boolean r;
		// The incoming size must be a multiple of the square tile size
		//  +1 padding is also acceptable.
		if( (inX % z == 0) && (inY % z == 0) )		// No padding.
			{
			tcx = inX / z;
			tcy = inY / z;
			if( tcx <= 1 && tcy <= 1 )		// refuse to copy 1 tile vsp.
				{
				System.err.println("Refused to inport 1 tile VSP");
				return(false); 
				}
			this.setUndoPoint( new Vsp(this.vsp), 23 );
			System.out.println("Clipboard paste in no-padding form x "+
				tcx.toString() + " y " + tcy.toString() );
			for( int iy = 0; iy < tcy; iy++ )
				{ for( int ix = 0; ix < tcx; ix++ )
					{
					r = this.vsp.modifyTile( iy * tcx + ix, 
						clipboardVImage.getRegion( ix*z, iy*z, z, z) );
					if( r == true ) { inportCount++; }
				}	}
			System.out.println("Inported "+inportCount.toString()+" tiles.");
			this.loadWorkingTile();
			return(true);
			}
		if( ((inX-1) % (z+1) == 0) &&  ((inY-1) % (z+1) == 0) ) //1px pad
			{
			tcx = (inX-1) / (z+1);
			tcy = (inY-1) / (z+1);
			if( tcx <= 1 && tcy <= 1 )		// refuse to copy 1 tile vsp.
				{
				System.err.println("Refused to inport 1 tile VSP");
				return(false); 
				}
			this.setUndoPoint( new Vsp(this.vsp), 24 );
			System.out.println("Clipboard paste in padded form   x "+
				tcx.toString() + " y " + tcy.toString() );
			for( int iy = 0; iy < tcy; iy++ )
				{ for( int ix = 0; ix < tcx; ix++ )
					{
					r = this.vsp.modifyTile( iy * tcx + ix, 
						clipboardVImage.getRegion( ix*(z+1), iy*(z+1), 
							z, z) );
					if( r == true ) { inportCount++; }
				}	}
			System.out.println("Inported "+inportCount.toString()+" tiles.");
			this.loadWorkingTile();
			return(true);
			}
		System.err.println("VSP Paste : non-compatible clipboard");
		return(false);
		}

/**  Takes clipboard contents and splits them into new tiles.
 * then inserts them starting at the current tile Index.
 * It is not picky what size the clipboard is.. long as its an image flavor.
 * Any partial tiles along the edges will be discarded.
 *
 * @param clipboardVImage		Image to chop & insert.
 */
	private void handleTileInsert( VImage clipboardVImage )
		{
		if( clipboardVImage == null )	{ return; }
		this.setUndoPoint( new Vsp(this.vsp), 25 );
		VImage parts[] = VImage.splitIntoTiles( clipboardVImage,
				0, 0, this.vsp.getTileSquarePixelSize() );
		for( int n = (parts.length-1); n >= 0; n-- )
			{	this.vsp.insertTile( this.tIndex, parts[n].getImage() );	}
		return;
		}

	private void toggleVspOverview()
		{
		this.showOverview = ! this.showOverview;
		if( this.showOverview == true )
			{
			this.updateMiniTileset();
			}
		return;
		}
	
	private void updateMiniTileset()
		{
		this.vspOverview.paintBlack();
		int padding = this.vsp.getTileSquarePixelSize() / 2;
		for( int n = 0; n < this.vsp.getNumtiles(); n++ )
			{
			this.vspOverview.blit( 
				((n%VmenuVSPeditor.DEFAULT_TILES_PER_ROW)
					*17)+padding ,
				((n/VmenuVSPeditor.DEFAULT_TILES_PER_ROW)
					*17)+padding , 
				this.vsp.getTileAsVImage(n) );
			}
		return;
		}
	
	private void toggleHelp()
		{
		this.showHelp = ! this.showHelp;
		// TODO : implement
		return;
		}


	/**  Does an file system object save of the current VSP data. */
	private static boolean saveVSP( Vsp theTileSet )
		{
		if( VmenuVSPeditor.directoryCheck() == false )
			{ return(false); }
		File dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" +
				File.separator + "vsp/" );
		
		javax.swing.JFileChooser fileChooser = 
				new javax.swing.JFileChooser();
		fileChooser.setToolTipText("Browse and save this Vsp");
		fileChooser.setDialogTitle("Browse and save this Vsp");
		fileChooser.setApproveButtonText( "Save VSP" );
		fileChooser.setFileFilter( new FileNameExtensionFilter(
				"Verge Sprite Palette", "vsp") );
		fileChooser.setAutoscrolls( true );
		fileChooser.setCurrentDirectory( dirOut );
		int returnVal = fileChooser.showSaveDialog(
				VergeEngine.getGUI() );
		if( returnVal == 1 )	//  save was Cancelled.
			{  return(false); }

		return( Vsp.saveVsp( 
				theTileSet, fileChooser.getSelectedFile().toString() ) );
		}
	
	private boolean loadVsp()
		{
		if( VmenuVSPeditor.directoryCheck() == false )
			{ return(false); }
		File dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" +
				File.separator + "vsp/" );

		javax.swing.JFileChooser fileChooser = 
				new javax.swing.JFileChooser();
		fileChooser.setToolTipText("Browse for a .vsp file to edit.");
		fileChooser.setDialogTitle("Browse for a .vsp file to open");
		fileChooser.setApproveButtonText( "Load VSP" );
		fileChooser.setApproveButtonToolTipText( "Load VSP" );
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter( new FileNameExtensionFilter(
				"Verge Sprite Palette", "vsp") );
		fileChooser.setAutoscrolls( true );
		fileChooser.setCurrentDirectory( dirOut );
		int returnVal = fileChooser.showOpenDialog(
				VergeEngine.getGUI() );
		if( returnVal == 1 )	//  load cancelled in GUI
			{  return(false); }
		
		java.net.URL testUrl;
		try	{
			testUrl = fileChooser.getSelectedFile().toURI().toURL();
			}
		catch (MalformedURLException e)
			{
			e.printStackTrace();
			return(false);
			}

		Vsp newguy = new Vsp( testUrl );

		// This could very easily fail...
		// Examine this new VSP before we engage it.
		if( newguy.getSignature() == 0 )		// indicates load problem. 
			{
			System.err.println("VSP Load failed, bad vsp signature.");
			return(false); 
			}

		if( newguy.getTileSquarePixelSize() != 
				VmenuVSPeditor.STANDARD_TSIZE  )
			{
			System.err.println("VSP uses non-standard tile size ("+
				Integer.toString( newguy.getTileSquarePixelSize()) +
				") - VSP editor cannot work with it." );
			return(false);
			}

		System.out.println(   " Loading VSP with : " +
			Integer.toString( newguy.getNumtiles() ) + " tiles.   " + 
			Integer.toString( newguy.getNumObsTiles() ) + " obs  " +
			Integer.toString( newguy.getNumAnimations() ) + " anims "
			);
		
		this.vsp = newguy;

		// Post-load - must restore some internals to a fresh state.

		this.clearUndoRedo();
		this.tIndex = 0;
		this.editColorInPlace = false;
		this.showOverview = false;
		this.showHelp = false;

		int tileRows = newguy.getNumtiles() / 
				VmenuVSPeditor.DEFAULT_TILES_PER_ROW;
		this.vspOverview = new VImage(
			VmenuVSPeditor.DEFAULT_TILES_PER_ROW *
				(this.vsp.getTileSquarePixelSize()+2),
			tileRows *	(this.vsp.getTileSquarePixelSize()+2) , 
			Color.BLACK );

		main.refresh();
		this.loadTile( this.tIndex );
		this.updatePreview();

		return(true);
		}

	private boolean saveCurrentVSP()
		{	return( VmenuVSPeditor.saveVSP( this.vsp ) );	}

	private void newVsp()
		{
		this.setUndoPoint( new Vsp(this.vsp), 26 );
		// Make a blank "dummy" VSP.
		this.vsp = new Vsp(
				VmenuVSPeditor.DEFAULT_TILES_PER_ROW);

		// Post-load stuff.
		this.tIndex = 0;
		this.editColorInPlace = false;
		this.showOverview = false;
		this.showHelp = false;

		int tileRows = this.vsp.getNumtiles() / 
				VmenuVSPeditor.DEFAULT_TILES_PER_ROW;
		this.vspOverview = new VImage(
			VmenuVSPeditor.DEFAULT_TILES_PER_ROW *
				(this.vsp.getTileSquarePixelSize()+2),
			tileRows *	(this.vsp.getTileSquarePixelSize()+2) , 
			Color.BLACK );

		this.clearUndoRedo();
		main.refresh();
		this.loadTile( this.tIndex );
		this.updatePreview();

		return;
		}

	/** Select a file and save the color bar palette within it. */
	private boolean savePalette( )
		{
		if( VmenuVSPeditor.directoryCheck() == false )
			{ return(false); }
		File dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
			File.separator + "Editor/" +
			File.separator + "palette/" );

		javax.swing.JFileChooser fileChooser = 
				new javax.swing.JFileChooser();
		fileChooser.setToolTipText("Browse for a .vpal file to edit.");
		fileChooser.setDialogTitle("Browse for a .vpal file to open");
		fileChooser.setApproveButtonText( "Save Palette" );
		fileChooser.setApproveButtonToolTipText( "Save Palette" );
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter( new FileNameExtensionFilter(
				"Verge Color Palette", "vpal") );
		fileChooser.setAutoscrolls( true );
		fileChooser.setCurrentDirectory( dirOut );
		int returnVal = fileChooser.showSaveDialog(
				VergeEngine.getGUI() );
		if( returnVal == 1 )	//  load cancelled in GUI
			{  return(false); }
		
		ExtendedDataOutputStream exout = null;
		try {
			OutputStream os = new FileOutputStream(
					fileChooser.getSelectedFile()  );
			exout = new ExtendedDataOutputStream(os);
			exout.writeInt( this.clrs.size() );
			for( Integer c : this.clrs.keySet() )
				{
				exout.writeByte((byte) this.clrs.get(c).getRed() % 256);
				exout.writeByte((byte) this.clrs.get(c).getGreen() % 256);
				exout.writeByte((byte) this.clrs.get(c).getBlue() % 256);
				}
			}
		catch( Exception e )
			{ return(false); }
		finally {   
			if( exout != null )
				{	try { exout.close(); } catch(Exception e ) {}	}
			}
		return(true);
		}
	
	/** Restore color bar palette from a file */
	private boolean loadPalette( )
		{
		if( VmenuVSPeditor.directoryCheck() == false )
			{ return(false); }
		File dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" +
				File.separator + "palette/" );
		
		javax.swing.JFileChooser fileChooser = 
				new javax.swing.JFileChooser();
		fileChooser.setToolTipText("Browse for a .vpal file to open");
		fileChooser.setDialogTitle("Browse for a .vpal file to open");
		fileChooser.setApproveButtonText( "Open Palette" );
		fileChooser.setApproveButtonToolTipText( "Open Palette" );
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setFileFilter( new FileNameExtensionFilter(
				"Verge Color Palette", "vpal") );
		fileChooser.setAutoscrolls( true );
		fileChooser.setCurrentDirectory( dirOut );
		int returnVal = fileChooser.showOpenDialog(
				VergeEngine.getGUI() );
		if( returnVal == 1 )	//  load cancelled in GUI
			{  return(false); }
		
		// Open and get first integer.   If failure >. bail out.
		ExtendedDataInputStream exin = null;
		int numcolors = 0;
		try {
			FileInputStream os = new FileInputStream(
				fileChooser.getSelectedFile()  );
			exin = new ExtendedDataInputStream(os);
			numcolors = exin.readInt();
			}
		catch( Exception e) 
			{
			if( exin != null )
				{	try { exin.close(); } catch(Exception ee ) { }	}
			return(false); 
			}

		this.setBasicPalette();
		int cr=0, cg=0, cb=0;
		for( Integer n = 0; n < numcolors; n++ )
			{		// read until expected # or we hit EOF
			try {
				cr = (int) exin.readUnsignedByte();
				cg = (int) exin.readUnsignedByte();
				cb = (int) exin.readUnsignedByte();
				}
			catch(Exception e ) { continue; }
			Color c = new Color( cr, cg, cb);
			this.clrs.put(n, c);
			}

		if( exin != null )
			{	try { exin.close(); } catch(Exception e ) {}	}

		this.resetPalette();
		return(true);
		}

	private void setBasicPalette()
		{
		this.setUndoPoint( this.getColorPaletteCopy() , 13 );
		if( this.clrs == null )
			{ this.clrs = new HashMap<Integer,Color>(); }
		if( ! this.clrs.isEmpty() ) 
			{ this.clrs.clear(); }
		
		this.clrs.put( 0, new Color(255,0,0) );
		this.clrs.put( 1, new Color(0,255,0) );
		this.clrs.put( 2, new Color(0,0,255) );
		
		this.clrs.put( 3, new Color(255,255,0) );
		this.clrs.put( 4, new Color(255,1,255) );
		this.clrs.put( 5, new Color(0,255,255) );
		
		this.clrs.put( 6, new Color(255,255,255) );
		this.clrs.put( 7, new Color(127,127,127) );

		this.resetPalette();
		return;
		}
	
	/** Ensures a directory heiharchy exists for the editor's 
	 * resources. */
	private static boolean directoryCheck()
		{
		File dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" );
		if( ! dirOut.exists() )	 { dirOut.mkdir(); }
		if( ! dirOut.exists() )	 { return(false); }
		
		dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" +		
				File.separator + "palette/" );
		if( ! dirOut.exists() )	 { dirOut.mkdir(); }

		dirOut = new File( VergeEngine.JVERGE_CWD.toString() +
				File.separator + "Editor/" +		
				File.separator + "vsp/" );
		if( ! dirOut.exists() )	 { dirOut.mkdir(); }

		return(true);
		}

	/** Sets all the buttons in the currently shown color key. */
	private void resetPalette()
		{
		while( this.colorkey.countMenuItems() > 0 )
			{ this.colorkey.removeItem(0); }
		this.cBarIndexSet = 0;
		for( Integer b = 0; b < 11; b++ )
			{
			VmiButton cBtn = new VmiButton(42,42);
			cBtn.setFrameThicknessPx(3);
			cBtn.setCircular(true);
			cBtn.setShadowThicknessPx(3, true);
			
			Color thiscolor = this.clrTrans;
			if( b == 1 )	{ thiscolor = this.clr1st; }
			if( b == 2 )	{ thiscolor = this.clr2nd; }
			if( b >= 3 )	{ thiscolor = this.clrs.get(b-3); }
			
			cBtn.setColorComponent(
					enumMenuButtonCOLORS.BODY_ACTIVE, 
					thiscolor );
			cBtn.setColorComponent(
					enumMenuButtonCOLORS.BODY_INACTIVE, 
					thiscolor );
			cBtn.setColorComponent(
					enumMenuButtonCOLORS.BODY_SELECTED, 
					thiscolor );
			cBtn.setColorComponent(
					enumMenuButtonCOLORS.FRAME_OUTER, 
					Color.GRAY );
	
			this.colorkey.addItem(cBtn);
			}
		return;
		}
	
	/** Inserts an entry into the customizable palette.   Note that 
	 * this is different then the color key bar object, which reads values
	 * from the palette source and displays a 8-range-set of palette 
	 * entries with a VmenuHorizontal filled with Circular Button objects. 
	 * Erroneous palette index arguments are naturally bounded. */
	private void insertPaletteColor( int palIndex )
		{
		this.setUndoPoint( this.getColorPaletteCopy() , 14 );
		Integer cnt = this.clrs.size();
		if( palIndex < 0 ) 
			{ palIndex = 0; }
		if( palIndex > cnt-1 )
			{ palIndex = cnt-1; }
		this.clrs.put( new Integer(this.clrs.size()), new Color(0,0,0) );
		for( Integer x = cnt; x >= palIndex; x-- )
			{	this.clrs.put( x, this.clrs.get(x-1) );	}
		this.clrs.put( palIndex, this.clr1st);
		this.setCbarLine( this.cBarIndexSet );
		return;
		}

	/** Removes a palette color at given index, shifting the remaining. */
	private void removePaletteColor( int palIndex )
		{
		Integer cnt = this.clrs.size();
		if( cnt <= 1 )  { return; }	// will not remove final color.
		this.setUndoPoint( this.getColorPaletteCopy() , 15 );
		if( palIndex < 0 ) 
			{ palIndex = 0; }
		if( palIndex > cnt-1 )
			{ palIndex = cnt-1; }
		// Shift down.
		for( Integer x = palIndex; x <= cnt-1; x++ )
			{	this.clrs.put( x, this.clrs.get(x+1) );	}
		this.clrs.remove(new Integer(cnt-1));	// pop off the end.
		
		while( (this.cBarIndexSet*8) > (this.clrs.size()-1) )
			{  this.cBarIndexSet--; }
		this.setCbarLine( this.cBarIndexSet );
		return;
		}

	/** Set all pixels to a single color. */
	private void clearWorkingTile( Color c )
		{
		this.saveMainTileState( 17 );
		if( c == null )
			{ c = this.clrTrans; }
		HashMap<Integer,Color> hmC = 
				new HashMap<Integer,Color>();
		hmC.put(enumMenuButtonCOLORS.BODY_ACTIVE.value(), c);
		hmC.put(enumMenuButtonCOLORS.BODY_INACTIVE.value(), c);
		hmC.put(enumMenuButtonCOLORS.BODY_SELECTED.value(), 
				VmenuVSPeditor.invertColor(c) );
		this.main.setColorAll( hmC );
		return;
		}

	private void floodFiller( int x, int y, Color newColor, 
			int diffThreshold )
		{
		if( newColor == null )
			{ newColor = this.clrTrans; }
		Integer z  = this.vsp.getTileSquarePixelSize();
		Integer s = z*z;
		Integer i0 = (y * z) + x;
		VImage work = copyWorkingTile();
		Color targetColor = work.getPixelColor(x, y);
	
		//  Target color is same as replacement.   so do nothing.
		if( targetColor.equals(newColor) )		{ return; } 

		ArrayList<Integer> hm = new ArrayList<Integer>();
			// (ex)clude - Keeps track of cells that have been examined.
		ArrayList<Integer> ex = new ArrayList<Integer>();
		hm.add(i0);
		while( ! hm.isEmpty() )
			{
			Integer idx = hm.remove(0);

			if( ! ex.isEmpty() && ex.contains(idx) == true )
				{ continue; }		// Already did this one.
			ex.add( idx );			// ok, but make sure we don't again.

			// Analyze pixels at 4 adjacent sides of idx.
			Integer pN = idx - z;
			if( pN < 0 )		{ pN += s; }
			Integer pE = idx + 1;
			if( pE >= s )		{ pE -= s; }
			Integer pS = idx + z;
			if( pS >= s )		{ pS -= s; }
			Integer pW = idx - 1;
			if( pW < 0 )		{ pW += s; }

					// If colors match.. add it for next pass.
			if( VmenuVSPeditor.color3ComponentDifference( targetColor, 
					work.getPixelColorAtIndex(pN, 255) ) <= diffThreshold 
				&& ! ex.contains(pN) )
					{	hm.add( pN );	}
			if( VmenuVSPeditor.color3ComponentDifference( targetColor, 
					work.getPixelColorAtIndex(pE, 255) ) <= diffThreshold 
				&& ! ex.contains(pE) )
					{	hm.add( pE );	}
			if( VmenuVSPeditor.color3ComponentDifference( targetColor, 
					work.getPixelColorAtIndex(pS, 255) ) <= diffThreshold 
				&& ! ex.contains(pS) )
					{	hm.add( pS );	}
			if( VmenuVSPeditor.color3ComponentDifference( targetColor, 
					work.getPixelColorAtIndex(pW, 255) ) <= diffThreshold
				&& ! ex.contains(pW) ) 
					{	hm.add( pW );	}

			System.out.println( "DIFF : "+VmenuVSPeditor.color3ComponentDifference( targetColor, 
					work.getPixelColorAtIndex(pW, 255) ) );
			}

		// finally, alter the pixels, then reset the workign image.
		for( Integer i : ex )
			{ work.setPixel( i % z, i / z, newColor ); }

		this.loadWorkingImage(work);
		System.out.println(" Flood Filled " + Integer.toString( ex.size())+
				" pixels " );
		return;
		}
	
/** returns an integer that represents the absolute sum of the 
 *     three component colors (RGB).  alpha is ignored. */
	private static int color3ComponentDifference(Color c1, Color c2 )
		{
		int diff = 0;
		diff += Math.abs( c2.getRed() - c1.getRed() );
		diff += Math.abs( c2.getGreen() - c1.getGreen() );
		diff += Math.abs( c2.getBlue() - c1.getBlue() );
		return(diff);
		}

	private void line( int index1, int index2, Color c )
		{
		VImage work = copyWorkingTile();
		int z = this.vsp.getTileSquarePixelSize();
		work.line( index1 % z, index1 / z, index2 % z, index2 / z, c);
		this.loadWorkingImage(work);
		return;
		}

	private void rect( int index1, int index2, Color c )
		{
		VImage work = copyWorkingTile();
		int z = this.vsp.getTileSquarePixelSize();
		work.rect( index1 % z, index1 / z, index2 % z, index2 / z, c);
		this.loadWorkingImage(work);
		return;
		}
	
	private void circle( int index1, int index2, Color c )
		{
		VImage work = copyWorkingTile();
		int z = this.vsp.getTileSquarePixelSize();
		work.circle( index1 % z, index1 / z, 
				Math.abs( (index2 % z) - (index1 % z)), 
				Math.abs( (index2 / z)-(index1 / z) ),
				c, work);
		this.loadWorkingImage(work);
		return;
		}

	private void replacer( Color find, Color replace )
		{
		if( find == null || replace == null )	{ return; }
		int z = this.vsp.getTileSquarePixelSize() * 
					this.vsp.getTileSquarePixelSize();
		for( int x = 0; x < z; x++ )
			{
			if( this.getColorCellIndex(x).equals(find) == true  )
				{ this.setCell( x, replace ); }
			}
		return;
		}

	public static Double distance(int px1x, int px1y, int px2x, int px2y )
		{
		return( Math.sqrt( (px2y-px1y)*(px2y-px1y)+
				(px2x-px1x)*(px2x-px1x) ) );
		}

	/** Rotates the working tile in 90 degree "quadangles"  */
	private void rotate( int QuadAngle, boolean mirror )
		{
		this.saveMainTileState( 18 );
		Double rads = (Math.PI / 2.0d) * new Double(QuadAngle);
		VImage work = copyWorkingTile();
		VImage tmp = VImage.rotateRadiansIntoNewImage(work,rads);
		if( mirror == true ) { tmp.mirror(); }
		this.loadWorkingImage( tmp );
		return;		
		}
	
	/** When [main] view is in multi-selection mode, colors c to all them */
	private void largeBrush( Color c )
		{
		if( this.main.isMultiSelect() == false )
			{ return; }
		this.saveMainTileState( 20 );
		for( int x = 0; x < this.main.countMenuItems(); x++ )
			{
			if( this.main.getMenuItemAsButton(x).getState() == 
					enumMenuItemSTATE.SELECTED.value() )
				{
				this.setCell( x, c );	
			}	}
		return;
		}
	
	/** Picks random pixels within a radius between two working points.
	 * Number of pixels to spray, color, and random color are options. */
	private void randomSpray( int indexCenter, int index2,
			int sprayCount, Color c, boolean randomColororizer )
		{
		int z = this.vsp.getTileSquarePixelSize();
		
		if( c == null && randomColororizer == false )
			{ return; }		// Avoid NPE

		this.saveMainTileState( 21 );
		
		while( indexCenter >= (z*z) )	{ indexCenter -= (z*z); }
		while( indexCenter < 0 )			{ indexCenter += (z*z); }
		while( index2 >= (z*z) )			{ index2 -= (z*z); }
		while( index2 < 0 )				{ index2 += (z*z); }
		
		int lx = (indexCenter % z);
		int ly = (indexCenter / z);
		int lx2 = (index2 % z);
		int ly2 = (index2 / z);

		int radX = Math.abs( lx2 - lx );
		int radY = Math.abs( ly2 - ly );
		Double radius = new Double( radX );
		if( radY > radX )	{ radius = new Double(radY); }

		for( int pt = 0; pt < sprayCount; pt++ )
			{
			// Get a random angle and a random bounded radius
			Double r = Math.random() * radius;
			Double a = Math.random() * Math.PI * 2.0d;
			int xDiff = new Double( Math.cos(a) * r ).intValue();
			int yDiff = new Double( Math.sin(a) * r).intValue();
			int targetIdx = indexCenter + ((yDiff*z)+xDiff);
			if( randomColororizer == true )
				{ 
				this.setCell( targetIdx, 
					VmenuVSPeditor.getColorRandom(1.0f) ); 
				}
			else	{ this.setCell( targetIdx, c); }
			}
		return;
		}
	
	/** As implied... makes up a random color, 
	 * with ensured alpha value (0-1.0f)   */
	public static Color getColorRandom( float alpha )
		{
		if( alpha < 0.0f )   { alpha = 0.0f; }
		if( alpha > 1.0f )   { alpha = 1.0f; }
		return( new Color(
				(float) Math.random(),
				(float) Math.random(),
				(float) Math.random(),
				alpha) );
		}
	
	/** Use to break out of any tool mode.  Add any new tools to it. 
	 * This will return the menus features to its entry state.   */
	private void disableAllTools()
		{
		this.arealTool = -1;
		this.arealToolDensity = 3;
		this.arealToolCRandom = false;
		this.brushSize = 1;
		this.lineTool = -1;
		this.lineContiguous = false;
		this.rectTool = -1;
		this.rectContiguous = false;
		this.circleTool = -1;
		this.showHelp = false;
		this.showOverview = false;
		return;
		}

	/** Swaps colors at two index in the working tile. */
	private void colorSwap( int idx1, int idx2 )
		{
		Color tmp = this.getColorCellIndex(idx2);
		this.setCell( idx2, this.getColorCellIndex(idx1) );
		this.setCell( idx1, tmp );
		return;
		}
	
	/*  Undo functionality methods */
	
	private void wrapWorkingImage(int x, int y)
		{
		if( x == 0 && y == 0 )	{ return; }
		this.saveMainTileState( 22 );
		int z = this.vsp.getTileSquarePixelSize();
		int z2 = z*z;
		if( x > 0 )
			{
			for( int xs = z2-1; xs >= 0; xs-- )
				{
				if( (xs % z) == 0 )  { }
				else { this.colorSwap( xs, xs-1); }
				}
			}
		if( x < 0 )
			{
			for( int xs2 = 0; xs2 < z2; xs2++ )
				{
				if( (xs2 % z) == 15 )  {  }
				else { this.colorSwap( xs2, xs2+1); }
				}			
			}
		if( y > 0 )
			{
			for( int ys = z2-1; ys >= 0; ys-- )
				{
				if( ys < z  )  {  }
				else { this.colorSwap( ys, ys-z ); }
				}			
			}
		if( y < 0 )
			{
			for( int ys = 0; ys < z2; ys++ )
				{
				if( ys >= (z2-z)  )  {  }
				else { this.colorSwap( ys, ys+z); }
				}
			}
		return;
		}
	
	/**
	 *   Items that should call this:
	 *    --- Any changes to the working tile.
	 * 	  --- Any changes to the color palette clrs[], not the colorKey!
	 * 	  --- Any Hard changes to the VSP tileset.
	 * @param obj  One of the above three. - as clones.
	 */
	private void setUndoPoint( Object obj, Integer optype )
		{
			// Discard previous similar operation.
		if( (optype > 0) && (this.undoLastOp == optype) && 
				this.undoStack.size() > 0 )
			 { return; }
		// Stack is too big...discard oldest.
		if( this.undoStack.size() > VmenuVSPeditor.MAX_UNDO )
			{ this.undoStack.remove(0); }
		this.undoStack.push(obj);
		this.undoLastOp = optype;
		System.out.println(" Set undo operation # "+optype.toString() +
			"  Stack @ "+ Integer.toString( this.undoStack.size() ));
		return;
		}

	/*  Pops the undo stack and sends last item to the undo func */
	private void undo()
		{
		if( this.undoStack.isEmpty() )		
			{
			System.out.println( 
				"Undo attempted but undo stack is empty." );
			return; 
			}
		int t1 = this.undoStack.size();
		Object tmp = this.undoStack.pop();
		int t2 = this.undoStack.size();

		System.out.println("UNDO now ["+Integer.toString(t2)+
				"]   reduced from "+
				Integer.toString(t1));
		
		// Analyze tmp and set the redo accordingly.
		this.doUndo(tmp , true );
		return;
		}

	/** This can either be an undo or redo.. the given object is 
	 * type name interrogated, then restored, overwritting the current 
	 * working contents.    The second arg. tells weither to add a
	 * redo operation to that separate stack.
	 * 
	 *  The following 3 object restore ops are supported:
	 *  1)  Working tile change  (menus.VmenuVSPeditor StateSaveMain) 
	 *  2)  Color Palette change  (HashMap<Integer,Color>)
	 *  3)  hard VSP tile save  {domain.vsp)
	 *  
	 *    Any other object found on the undo stack will be thrown out
	 *    
	 *  */
	private void doUndo( Object obj , boolean setRedo )
		{
		if( obj == null )	{ return; }
		String theClassName = 
			new String( obj.getClass().getName() );
		String theClassCName = 
			new String( obj.getClass().getCanonicalName() );
		
		System.out.println( "Start Undo proc operation: " + theClassName +
			" ( " + theClassCName + " ) " );
		if( obj instanceof HashMap )
			{
			HashMap hmObj = (HashMap) obj;
			if( hmObj.containsKey( new Integer(0) ) )
				{
				Object tmpVal = hmObj.get(new Integer(0));
				String itsName = 
					new String( tmpVal.getClass().getName() );
				
				// At this point, we can assume this is a color palette save
				if( itsName.compareTo("java.awt.Color") == 0  )
					{
					try {
						// Surpress.. because we have already
						//    done sufficient checks above for types
						// 	Given limited variation of undo functional types
						@SuppressWarnings("unchecked")
						HashMap<Integer,Color> reconst =
							(HashMap<Integer,Color>) hmObj;
						Integer testSize = reconst.size();
						if( testSize < 1 )  { return; }
						if( setRedo )
							{
							this.redoStack.push( 
								this.getColorPaletteCopy() );
							this.unredoLastOp = obj;
							System.out.println(" ** Set redo point");
							}

						// Restore color bar. - copy key value pairs.					
						this.clrs.clear();
						for( Integer x : reconst.keySet() )
							{ 
							Color cx = new Color( reconst.get(x).getRGB() );
							this.setColorPaletteEntry(x, cx);
//							this.clrs.put( x, cx ); 
							}
			 
						System.out.println(" Undo ColorBar::  "+
							"HashMap with value type : "+itsName.toString()
							+"  with "+testSize.toString()+" Entries");
						return;
						}
					catch( Exception e )
						{
						System.out.println(
							" ERROR, ignored unexpected Undo object");
						return;
						}

					}
				}
			}	// End HashMap type resolution.
		
		//	Tile set change undo
		if( theClassName.compareTo(
				"domain.Vsp") == 0 && 
			theClassCName.compareTo(
				"domain.Vsp" ) == 0 )
			{
			if( setRedo )
				{
				this.redoStack.push( new Vsp(this.vsp) );
				this.unredoLastOp = obj;
				}

			this.vsp = new Vsp( (Vsp) obj );
			this.updatePreview();
			this.loadTile( this.tIndex );
			}

		// restore a snapshot of the tile data - reload it.
		if( theClassName.compareTo(
				"menus.VmenuVSPeditor$StateSaveMain") == 0 && 
			theClassCName.compareTo(
				"menus.VmenuVSPeditor.StateSaveMain" ) == 0 )
			{
			
			if( setRedo )
				{
				this.redoStack.push( new StateSaveMain(
					this.main.getMenuItemAsButtonArray() )  );
				// This is used later if Redo is used.
				this.unredoLastOp = obj;
				}

			StateSaveMain ssm = (StateSaveMain) obj;
			// Check for size compatibility
			if( this.main.countMenuItems() != ssm.getArraySize() )
				{ 
				System.out.println(" Discarded undo - Incompatible size");
				return;
				}

			for( int idx = 0; idx < ssm.getArraySize(); idx++ )
				{
				VmiButton tmpBtn = 
					(VmiButton) this.main.getMenuItem(idx);
				tmpBtn.setColorComponent( enumMenuButtonCOLORS.BODY_ACTIVE, 
						ssm.get(idx) );
				}
 
			System.out.println(" restored state of tile data.");
			}

		return;
		}

	private void redo()
		{
		if( this.redoStack.isEmpty() )   
			{ return; }
		System.out.println( "REDO: " + Integer.toString( this.redoStack.size()) + 
			" on stack " );
		// Take an object from the redo.. put it back on undo.
		Object o = this.redoStack.pop();
		
		this.doUndo( o , false );		// fires the redo operation.
		
		// put the last undo operation back onto its stack.
		if( this.unredoLastOp != null )
			{
			Object oo = this.unredoLastOp; 
			this.setUndoPoint( oo, 0 );
			}

//		this.undoStack.push(o);
		return;
		}

	private void clearUndoRedo()
		{
		this.undoStack.clear();
		this.redoStack.clear();
		this.unredoLastOp = null;
		return;
		}
	
	/* pushes the colors from the main tile editor into the undo stack.
	  A subclass needed to be created in order to differentiate the tile
	  cell colors from the color palette array. */
	private void saveMainTileState( int actionClassifier )
		{
		StateSaveMain ssm = new StateSaveMain(
			this.main.getMenuItemAsButtonArray() );
		setUndoPoint( ssm, actionClassifier );
		return;
		}
	
	public Color getTransparentColor()
		{ return(this.clrTrans); }

	/*   INPUT ROUTINES */
	
	/* Note, input is not asyncronous, This method ends and puts JVerge
		into input mode.   Another routine needs to
 		be sniffing for input entry and applying the payloads.  */
	public static void delegateInput( VmiInputInteger in )
		{
		in.doInput();
		return;
		}


	/**  This routine will check all menu items with ID's 
	 * added to the inputIDstack member for incoming input.
	 * It will then proceed to use these input functionally on a case
	 * by case basis..   Best to put at top of doControls() method. */
	private Integer processInputs()
		{		
		Integer rslt = 0;
		int n = this.inputIDstack.size();
		for( int x = 0; x < n; x++ )	// Scan each input field.
			{
			VmiSimpleInput vsi = (VmiSimpleInput) 
				this.sidebar.getMenuItemByID( this.inputIDstack.get(x) );
			String theInput = getInput( vsi.getId() );	
			if( theInput.equals("") )   { continue; }
			else { rslt++; }

			// here, specific inputs are put to use 
			switch( x )
				{
				case 0:  	//  This is "goto tile #"
					int tilenum = Integer.parseInt(theInput);
					this.changeTileAbs( tilenum );
//					System.out.println( " Got INPUT " + theInput );
					break;
				default:
					System.out.println("Input caught but not utilized");
					break;
				}				
			}
		return(rslt);
		}
	
	}		// END CLASS
