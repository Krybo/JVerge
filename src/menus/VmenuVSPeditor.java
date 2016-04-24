package menus;

import static core.Script.setMenuFocus;
import static core.Script.getFunction;

import java.awt.Color;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import menus.Vmenu.enumMenuEVENT;
import menus.VmiButton.enumMenuButtonCOLORS;
import core.Controls;
import core.DefaultPalette;
import core.Script;
import core.VergeEngine;
import domain.Map;
import domain.VImage;
import domain.VSound;
import domain.Vsp;

public class VmenuVSPeditor implements Vmenu
	{
	//    control focus:
	//   This is a multi-menu, this index is like a tab-order, and
	//   controls will use it to delegate to the active menu.
	private Integer cFocus = 0;

	// tile index: This is the current tile being edited.
	private Integer tIndex = 0;

	// The component guts.
	private Vsp vsp = null;
	private VmenuButtonPalette main = null;
	private VmenuVertical sidebar = null;
	private VmenuHorizontal colorkey = null;
	private VmenuSliders colorEditor = null;
	private final DefaultPalette vPal = new DefaultPalette();
	private HashMap<Integer,Color> clrs = null;
	private VImage bkgImg = null;
	private boolean isBkgImg = false;
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();

	// Essentials.
	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);
	
	private final Color clrShader = new Color(0.0f,0.0f,0.0f,0.25f);


	/** ----------------  Constructors --------------------  */

	public VmenuVSPeditor( Map sourceMap )
		{	this(sourceMap.getTileSet());	}
	
	public VmenuVSPeditor( Vsp sourceTileSet )
		{
		this.focusID = Vmenu.getRandomID();
		this.vsp = sourceTileSet;
		this.sidebar = new VmenuVertical( 30, 60 );
		this.colorEditor = new VmenuSliders(30, 300, 128, 3, 
				false, true, true );
		this.main = new VmenuButtonPalette(250, 60, 256, 256, 16, 16);
		this.colorkey = new VmenuHorizontal(1,1);
		// instantly sets the default verge color palette as the 
		//  hashmap of available colors at hand.
		this.clrs = this.vPal.getAllColors(255);
		
		// Fill out the menus here.
		
		main.setPadding( 0, true, Color.WHITE );
		main.setBorderAll( false, 0 );
		main.refresh();
		
		Method m4 = null;
		Method m5 = null;
		Method m6 = null;
		try {
			m4 = VmenuVSPeditor.class.getDeclaredMethod(
				"focusSubMenuIndex",  Integer.class );
			m5 = VmenuVSPeditor.class.getDeclaredMethod(
				"nextTile",  new Class[]{} );
			m6 = VmenuVSPeditor.class.getDeclaredMethod(
				"prevTile",  new Class[]{} );
			} catch( Exception e ) 	{ e.printStackTrace(); }

		VmiTextSimple vts01 = new VmiTextSimple("Return to Map");
		vts01.setAction( getFunction(Script.class,"focusSystemMenu") );
		
		VmiTextSimple vts04 = new VmiTextSimple("Edit Tile");
		vts04.setAction( m4 );
		vts04.setActionArgs( new Object[]{new Integer(3)} );

		VmiTextSimple vts05 = new VmiTextSimple("Next Tile");
		vts05.setAction( m5 );
		
		VmiTextSimple vts06 = new VmiTextSimple("Prev. Tile");
		vts06.setAction( m6 );

		this.sidebar.addItem( vts01 );
		this.sidebar.addItem( new VmiTextSimple("Save VSP") );
		this.sidebar.addItem( new VmiTextSimple("Edit Palette") );
		this.sidebar.addItem( vts04 );
		this.sidebar.addItem( vts05 );
		this.sidebar.addItem( vts06 );
		this.sidebar.addItem( 
			new VmiInputInteger( "<GoTo Tile #>", 
				"Enter VSP Tile number (INT)", 	0, 0) );

		this.bkgImg = null;
		this.isBkgImg = false;

		this.tIndex = 0;
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
						new Color(0.0f,0.0f,0.0f, 0.25f ) );
				break;
			default:
				target.rectfill( 240, 50, 516, 326, this.clrShader );
				break;
			}
		
		this.main.paint(target);
		this.sidebar.paint(target);
		this.colorkey.paint(target);
		this.colorEditor.paint(target);
		return(true);
		}

	public boolean doControls( Integer ext_keycode )
		{
		Integer basecode = Controls.extcodeGetBasecode(ext_keycode);
		boolean isShift = Controls.extcodeGetSHIFT(ext_keycode);
		boolean isCntl = Controls.extcodeGetCNTL(ext_keycode);
		
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
		this.childID = this.getControlItem().getFocusId();
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

//	private void focusMain()
//		{  this.cFocus = 3;  return;  }
//	private void focusSideBar()
//		{  this.cFocus = 0; return;  }
//	private void focusColorKeyBar()
//		{  this.cFocus = 1; return;  }
//	private void focusColorEditor()
//		{  this.cFocus = 2; return;  }
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
		return;
		}

	private void prevTile()
		{
		this.tIndex--;
		if( this.tIndex < 0 )
			{ this.tIndex =  vsp.getNumtiles() - 1; }
		this.loadTile(this.tIndex);
		return;
		}
	
	}
