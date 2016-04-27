package menus;

import static core.Script.setMenuFocus;
import static core.Script.getFunction;

import java.awt.Color;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import menus.VmiButton.enumMenuButtonCOLORS;
import core.Controls;
import core.DefaultPalette;
import core.Script;
import core.VergeEngine;
import domain.Map;
import domain.VImage;
import domain.VSound;
import domain.Vsp;

/**  A Tile editor.   Consists of several Vmenu objects with an overriding
 * Vmenu to manage interactions between them.
 * 
 * ----- Controls Overview --------
 * 
 * [0-9] 	Change color, cooresponding to palette bar.
 * [cntl][backspace]	Exits menu regardless of focus
 * [Cntl Arrow-Right]	Previous Tile
 * [Cntl Arrow-Left]	Next Tile
 * a	Areal Random Spray
 * b
 * c	Circle		[Cntl] Wrapping Circle		[Shift]  Fill Circle.
 * d
 * e	Focus tile edit.   [Cntl] Focus Palette bar.
 * f		Flood fill Tool
 * g	Goto Tile #
 * h	Help screen
 * i
 * j
 * k
 * l		Line		[Cntl] Wrapping line
 * m
 * n
 * o	Rotate Clockwise		[Cntl] Rotate CCW
 * p
 * q
 * r		Rectangle	[Cntl] Wrapping rectangle
 * s	Save single tile		[Cntl] Save entire VSP
 * t
 * u
 * v
 * w
 * x	Clear, set tile to solid color
 * y
 * z	Undo		[Cntl] Redo
 * NP+		Increase areal size of certain tool functions
 * NP+		Decrease areal size of certain tool functions
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
	// The line the color palette is on.
	private Integer cBarIndexSet = 0;

	// The component guts.
	private Vsp vsp = null;
	private VmenuButtonPalette main = null;
	private VmenuVertical sidebar = null;
	private VmenuHorizontal colorkey = null;
	private VmenuSliders colorEditor = null;
	private final DefaultPalette vPal = new DefaultPalette();
	private HashMap<Integer,Color> clrs = null;
	private final Color clrTrans = core.Script.Color_DEATH_MAGENTA;
	private Color clr1st = Color.WHITE;
	private Color clr2nd = Color.BLACK;
	private VImage bkgImg = null;
	private boolean isBkgImg = false;
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();

	// Essentials.
	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);
	
	VmiGuageInt gR;
	VmiGuageInt gG;
	VmiGuageInt gB;
	
	VImage preview = null;
	private final Color clrShader = new Color(0.0f,0.0f,0.0f,0.20f);


	/** ----------------  Constructors --------------------  */

	public VmenuVSPeditor( Map sourceMap )
		{	this(sourceMap.getTileSet());	}
	
	public VmenuVSPeditor( Vsp sourceTileSet )
		{
		this.focusID = Vmenu.getRandomID();
		// copy the VSP from the source.  So we do not edit it directly.
		this.vsp = new Vsp( sourceTileSet );
		this.sidebar = new VmenuVertical( 30, 60 );
		this.colorEditor = new VmenuSliders( 20, 300, 128, 3, 
				false, true, true );
		this.main = new VmenuButtonPalette(250, 60, 256, 256, 16, 16);
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
		
		Method m3 = null;
		Method m4 = null;
		Method m5 = null;
		Method m6 = null;
		Method m7 = null;
		try {
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

			} catch( Exception e ) 	{ e.printStackTrace(); }

		VmiTextSimple vts01 = new VmiTextSimple("Return to Map");
		vts01.setKeycode( 66 );		// Cntl+Backspace exits anywhere.
		vts01.setAction( getFunction(Script.class,"focusSystemMenu") );

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

		VmiInputInteger vmiIn01 = new VmiInputInteger( 
				"<GoTo Tile #>", "Enter VSP Tile number (INT)", 	0, 0 );
		vmiIn01.setKeycode( 568 );

		this.sidebar.addItem( vts01 );
		this.sidebar.addItem( new VmiTextSimple("Save VSP") );
		this.sidebar.addItem( vts03 );
		this.sidebar.addItem( vts07 );
		this.sidebar.addItem( vts04 );
		this.sidebar.addItem( vts05 );
		this.sidebar.addItem( vts06 );
		this.sidebar.addItem( vmiIn01 );
		
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
		this.gB.setBarColors(Color.BLUE, Color.BLACK );
		this.gR.setTip("Red"); this.gG.setTip("Grn"); this.gB.setTip("Blu");

		// -- finish up

		this.bkgImg = null;
		this.isBkgImg = false;

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

		return(true);
		}

	public boolean doControls( Integer ext_keycode )
		{
		Integer basecode = Controls.extcodeGetBasecode(ext_keycode);
		boolean isShift = Controls.extcodeGetSHIFT(ext_keycode);
//		boolean isCntl = Controls.extcodeGetCNTL(ext_keycode);

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
			case 101:
			case 8: // BACKSPACE <CANCEL>
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				if( this.cFocus == 0 )
					{ VmenuVSPeditor.returnToSystem(); }
				else
					{ this.cFocus = 0; }
				return( true );
			case 10: // ENTER KEY <CONFIRM> 
				this.funcActivate();
				break;
			case 37: 		// ARROW-LEFT
				this.getControlItem().doControls(ext_keycode);
				if( this.cFocus == 1 )		// Control is in color keybar.
					{ this.setColorEditorToCurrentColorKey(); }
				break;
			case 38: 		// ARROW-UP
				if( isShift == true )   
				{
				// Intercept Shift+Up to transverse palette.
				this.nextCbarLine();
				this.setColorEditorToCurrentColorKey();
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
				if( isShift == true )   
					{
					// Intercept Shift+Up to transverse palette.
					this.prevCbarLine();
					this.setColorEditorToCurrentColorKey();
					break; 
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			// Number keys do da buzinesss.
			case 48:	case 49:  case 50:  case 51:  case 52:
			case 53:	case 54:  case 55:  case 56:  case 57:
				if( this.cFocus == 3 )   // focus only on main.
					{
					int transform = basecode - 49;
					if( transform == -1 )  { transform = 9; }
					this.setCurrentCell( transform );
					break;
					}
				this.getControlItem().doControls(ext_keycode);
				break;
			default:
				this.getControlItem().doControls(ext_keycode);
				break;
			}
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

//			case 1:		// Color key bar
//			case 2:		// Color Editor

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
	     		   System.err.println("Invoking instanced method not"+
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

	private void loadTile( int vspIdx)
		{
		int z = this.vsp.getTileSquarePixelSize();
		VImage t = new VImage(z,z);
		t.setImage( this.vsp.getTiles()[vspIdx] );
		for( int y = 0; y < z; y++ )
			{ 
			for( int x = 0; x < z; x++ )			
				{
				int idx = (y*z)+x;
				if( idx >= this.main.countMenuItems() )
					{ continue; }
				VmiButton element = 
						(VmiButton) this.main.getMenuItem(idx);
				element.setColorComponent(
						enumMenuButtonCOLORS.BODY_ACTIVE, 
						t.getPixelColor(x, y) );
				}
			}
		return;
		}

	private void nextTile()
		{
		this.tIndex++;
		if( this.tIndex >= vsp.getNumtiles() )
			{ this.tIndex = 0; }
		this.loadTile(this.tIndex);
		this.updatePreview();
		return;
		}

	private void prevTile()
		{
		this.tIndex--;
		if( this.tIndex < 0 )
			{ this.tIndex =  vsp.getNumtiles() - 1; }
		this.loadTile(this.tIndex);
		this.updatePreview();
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
		return( new Color( 255 - in.getRed(), 255 - in.getGreen(), 
				255 - in.getBlue(), in.getAlpha() ) );
		}

	/** Moves the data from the menu object to the actual VSP. */
	private void saveWorkingTileAs( int vspIdx )
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
		vsp.modifyTile( vspIdx, output.getImage() );
		this.updatePreview();
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

	}		// END CLASS
