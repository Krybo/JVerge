package menus;

import static core.Script.getFunction;
import static core.Script.setMenuFocus;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.swing.filechooser.FileNameExtensionFilter;

import persist.ExtendedDataInputStream;
import persist.ExtendedDataOutputStream;
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
 * [0-9 and -] 	Change color, cooresponding to palette bar.
 * [cntl][backspace]	Exits menu regardless of focus
 * [Cntl Arrow-Right]	Previous Tile
 * [Cntl Arrow-Left]	Next Tile
 * a	Areal Random Spray
 * b	Circle		[Cntl] Wrapping Circle		[Shift]  Fill Circle.
 * c	Copy working tile to the clipboard
 * 			[Shift]  Copy working tile into tile group. 
 * 			[Cntl] Copy entire Vsp tileset to clipboard without padding.
 * 			[Cntl+Shift]   Copy entire Vsp to clipboard with 1px padding
 * d	DEBUG: for now.
 * e	Focus tile edit.   [Cntl] Focus Palette bar.
 * f		Flood fill Tool
 * g	Goto Tile #
 * h	Toggle Keyboard Help table.
 * i		invert current cell, invert palette entry    [Cntl] invert tile
 * j
 * k
 * l		Line		[Cntl] Wrapping line
 * m
 * n	New		{main} 		New Tile: Clears tile to tool color 1
 * 					{colorKey} 	Set the basic 8-unit color key.
 * o	Rotate Clockwise		[Cntl] Rotate CCW
 * p
 * q
 * r		Rectangle	[Cntl] Wrapping rectangle
 * s	HOT:			Saves working tile
 * 		HOT [Cntl] 	Save entire VSP to a file
 * 		[Shift] 			Save Palette to a file
 * t 	Toggle view of entire Tileset
 * u
 * v	Clipboard Paste : Single tile 
 * 			[Shift] Insert tiles here
 * 			[Cntl] Single tile & save
 * 			[Cntl+Alt]  Re-paste entire VSP
 * w
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
	private final Color clrTrans = new Color(255,0,255,0);
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
	private final Color clrShader = new Color(0.0f,0.0f,0.0f,0.20f);
	private static final int STANDARD_TSIZE = 16;

	private boolean showHelp;
	private static final int DEFAULT_TILES_PER_ROW = 16;
	
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
		this.colorEditor = new VmenuSliders( 16, 334, 128, 3, 
				false, true, true );
		this.main = new VmenuButtonPalette(250, 60, 256, 256, 
				VmenuVSPeditor.STANDARD_TSIZE, 
				VmenuVSPeditor.STANDARD_TSIZE  );
		this.colorkey = new VmenuHorizontal( 68,1 );
		// instantly sets the default verge color palette as the 
		//  hashmap of available colors at hand.
		this.clrs = this.vPal.getAllColors(255);
		
		// Fill out the menus here.
		
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
		vts08.setKeycode( 672 );
		vts08.setAction( m8 );
		VmiTextSimple vts09 = new VmiTextSimple("Keyboard map");
		vts09.setKeycode( 576 );
//		vts07.setAction( m7 );

		VmiInputInteger vmiIn01 = new VmiInputInteger( 
				"<GoTo Tile #> ", "Enter VSP Tile number (INT)", 	0, 0 );
		vmiIn01.setKeycode( 568 );

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
				target.rectfill( 58, 0, 636, 59, this.clrShader );
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

		return(true);
		}		// End   paint()

	public boolean doControls( Integer ext_keycode )
		{
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
				if( this.cFocus == 3 && isCntl == true && isShift == true )
					{
					this.setColorEditorToCursor();
					this.cFocus = 2;
					this.editColorInPlace = true;
					return(true);
					}
				this.funcActivate();
				break;
				
			case 33:		// Page-UP
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
			
			case 37: 		// ARROW-LEFT
				this.getControlItem().doControls(ext_keycode);
				if( this.cFocus == 1 )		// Control is in color keybar.
					{ this.setColorEditorToCurrentColorKey(); }
				break;
			case 38: 		// ARROW-UP
				if( isCntl == true ) 
					{
					this.changeTile( -1 *
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW);
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			case 39: 		// ARROW-RIGHT
				this.getControlItem().doControls(ext_keycode);
				if( this.cFocus == 1 )		// Control is in color keybar.
					{ this.setColorEditorToCurrentColorKey(); }
				break;
			case 40: 		// ARROW-DOWN
				if( isCntl == true ) 
					{
					this.changeTile( 
						VmenuVSPeditor.DEFAULT_TILES_PER_ROW);
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			// Number keys do da buzinesss.
			case 48:	case 49:  case 50:  case 51:  case 52:
			case 53:	case 54:  case 55:  case 56:  case 57: case 45:
				int transform = basecode - 49;
				if( transform == -1 )  { transform = 9; }
				if( transform == -4 )  { transform = 10; }
				if( this.cFocus == 1 ) 	// we are on color key
					{
					// Swaps colors, selected with #
					this.swapColorKeyColor( 
						this.colorkey.getSelectedIndex(), transform );
					break;
					}
				if( this.cFocus == 3 )   // focus on main.
					{
					if( isCntl == true && basecode == 50 ) 
						{
						this.setColorkeyColor(1, this.getColorCursorCell() );
						break;
						}
					if( isCntl == true && basecode == 51 ) 
						{
						this.setColorkeyColor(2, this.getColorCursorCell() );
						break;
						}
					this.setCurrentCell( transform );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
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
				this.workingTileToVImage().copyImageToClipboard();
				break;
			case 68:		// [d] Debug (for now)
//
				this.toggleVspOverview();
				break;
			case 73:		//  [i] - invert function.
				if( isCntl == true )		
					{ this.invertAllCells(); break; }
				switch( this.cFocus )
					{
					case 1:		// Invert the color key.
						this.setColorkeySelectedColor( 
							VmenuVSPeditor.invertColor(
								this.getColorkeySelectedColor() ));
						this.setColorEditorToCurrentColorKey();
						break;
					case 3:
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
				break;

			case 78:			// [n] new
				System.out.println("n pressed");
				switch( this.cFocus )
					{
					case 1:		// New Basic Color Key
						this.setBasicPalette();
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

			case 83:			// [s] Save functions.
				if( isShift == true )
					{
					this.savePalette();
					break;
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
		
			case 127:		// [Delete] 
				switch( this.cFocus )
					{
					case 1:
						break;
					default:
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

	/** Intercepts (Enter key) to be handled differently by sub-menu */
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
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_ACTIVE , nc );
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_INACTIVE , nc);
					this.getCursorCell().setColorComponent(
						enumMenuButtonCOLORS.BODY_SELECTED , nc);
					this.editColorInPlace = false;
					this.cFocus = 3;
					break;
					}
				// Else - we return it to the palette.
				this.cFocus = 1;
				int cidx = this.getSelectedColorkeyCIDX();

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
					{ this.clrs.put(cidx, nc); }
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
		if( this.vsp.checkTile(vspIdx) == false )
			{  vspIdx = 0; }
		VImage t = new VImage( this.vsp.getTileSquarePixelSize(),
			this.vsp.getTileSquarePixelSize() );
		t.setImage( this.vsp.getTiles()[vspIdx] );
		this.loadWorkingImage(t);
		return;
		}

	/** (re) loads the data from current VSP into the working display.*/
	private void loadWorkingTile()
		{
		this.loadTile( this.tIndex );
		this.updatePreview();
		}

	private void changeTile( int indexDiff )
		{
		this.tIndex += indexDiff;
		while( this.tIndex >= this.vsp.getNumtiles() )
			{ this.tIndex -= this.vsp.getNumtiles(); }
		while( this.tIndex < 0 )
			{ this.tIndex += this.vsp.getNumtiles(); }
		this.loadTile( this.tIndex );
		this.setColorEditorToCursor();
		this.updatePreview();
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
		HashMap<Integer,Color> hmTmp = 
				new HashMap<Integer,Color>();
		if( this.colorkey.getMenuItem(keyNum) == null )
			{ return; }
		Color c = this.colorkey.getMenuItem(keyNum).getColorComponent(
				enumMenuButtonCOLORS.BODY_ACTIVE.value());
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

	/** Moves the data from the menu object to the actual VSP. */
	private void saveWorkingTileAs( int vspIdx )
		{
		VImage output = this.workingTileToVImage();
		vsp.modifyTile( vspIdx, output.getImage() );
		this.updatePreview();
		}

	// exports the current working tile to a VImage
	private VImage workingTileToVImage()
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
	
	// Utilities for manipulating color key bar.
	
	private void setColorPaletteEntry( int paletteIndex, Color c )
		{ 						 
		this.clrs.put( paletteIndex, c );
		this.setCbarLine( this.cBarIndexSet );
		return;
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
		this.clrs.put( 8, new Color(0,0,0) );

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
	
	
	
	}		// END CLASS
