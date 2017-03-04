package menus;

import static core.Script.load;

import java.lang.reflect.Method;

import domain.VImage;
import domain.VSound;

/* Krybo (Feb.2016)
 * 
 * :: Vmenu ::   The menu interface  
 * 	Main purpose is to wrangle up Vmenuitems into a collection and  
 *  manage their display properties as a full menu.   
 *  The implemented Classes will 
 *  manage controls and paint the layout of the child menuitems 
 *  
 *  Submenu recursion was scrapped as too complicated.
 *  
 *  
 *  One master menu can be created from chaining together any various
 *     polymorphic Classes that implement Vmenu interface.   Multiple menu
 *     levels can be created by using the childID and parentID members
 *     held within Vmenuitem objects.
 *     
 *  The primary method is the paint() method which will draw the results
 *     by calling paint() of all its menuitem(), outputing to a referenced
 *     VImage object, that may be an existing object, or temporary one.
 *  
 *  Conventions:
 *  	   Implemented Vmenu Classes should be named "Vmenu"+Name
 *     Implemented Vmenuitems Classes should be named "Vmi"+Name
 *  
 */

public interface Vmenu
	{

	// Generalizes user events that can happen within a menu.
	public static enum enumMenuEVENT
		{
		MOVE (0),
		CONFIRM (1),
		CANCEL (2),
		INCREMENT (3),
		DECREMENT (4),
		SPECIAL (5);
		
		private final Integer index;
		enumMenuEVENT( Integer n )
			{	this.index = n;	}
		public Integer value()
			{ return(this.index); }
		public String getName()
			{ return(this.name()); }
		public int val()
			{ return(this.index.intValue()); }
		}

	/**
	 * The main drawing routine.  handles drawing of child menuitems.
	 * @param target  a VImage reference to draw on.
	 * @return boolean success or fail.
	 */
	public boolean paint( VImage target );

	/** Special animated drawing content.   Is called on a pre-specified 
	 * Frequency by the VmenuManager.  Do not make animation too
	 * intricate as blocks that take significantly long will 
	 * quickly impact performance, depending on frequency and intricacy.
	 *  ** Vmenu implementations need to delegate the .animate
	 *     calls down to their sub-menus and Vmenuitem's
	 * Krybo: Mar.2017
	 * @param target  a VImage reference to draw on.
	 * @return boolean success or fail.
	 */
	public boolean animate( VImage target );
	
	/**
	 * Control handler,  handles "signals" passed to the menu by external
	 *     control managing routines.   It changes the state of the menu
	 *     on a single keystroke basis.
	 * @param ext_keycode External extended keycode that was pressed
	 * @return	boolean- does the menu need redrawn?  in most cases, true.
	 */
	public boolean doControls( Integer ext_keycode );
	
	public void moveAbs(int x, int y);
	public void moveRel(int x, int y);

	public Integer countMenuItems();
	/**  Adds (appends) any implementation of Vmenuitem into the menu.
	 * Returns the menu item number of the new addition.   This can be 
	 * later used to link menus together.
	 * 
	 * @param vmi	Any object that implements Vmenuitem interface.
	 * @return		An base-one ordinal menu item number.
	 */
	public Integer addItem(Vmenuitem vmi );
	public Vmenuitem popItem();
	public Vmenuitem removeItem(int index);
	public Integer insertItem(Vmenuitem vmi , int index );
	public Vmenuitem getMenuItemSelected();
	public Vmenuitem getMenuItem(int index);
	public Vmenuitem getMenuItemByID( Long id );
	
	public void refresh();
	
	public Integer getSelectedIndex();
	public int getSelectedIndexPosX();
	public int getSelectedIndexPosY();
	
	public void setSelectedIndex(Integer index);
	public void setFocusId( Long id );
	public Long getFocusId( );
	public boolean isFocus( Long id );
	public void setActive( boolean active );
	public void setVisible( boolean vis );
	
	public void setBackgroundImage( VImage bkg, boolean onOff );
	
	public boolean isActive();
	public boolean isVisible();
	
		// -- SCRAPPED --
		// This class can recurse itself.   Keep a list of menu objects.
		//  These menus can then be referenced by menuitems within.
//	public int addSubmenus( Vmenu slave );
//	public Vmenu getSubmenus( Integer submenuIndex );
//	public boolean setSubmenus( Vmenu slave, Integer index );
//	public Vmenu popSubmenus( );
//	public boolean hasSubmenus();
//	public int countSubmenus();

	public void activateSelected();

	/**
	 * Implement this to attach sounds to generalized menu events
	 * @param slot	enum menu event
	 * @param sfx	The VSound object to play on this event.
	 */
	public void attachSound( enumMenuEVENT slot, VSound sfx );
	public boolean playMenuSound( enumMenuEVENT slot, int volume0to100 );

	// Delegators to Vmenuitem interface 
	public void setIconsAll( boolean onOff );
	public void setBorderAll( boolean onOff, int thick );

	/**
	 * Parent and child menus are tied to the Vmenus content
	 * items (Vmenuitem) . but these methods pass the same
	 * value to all content members.
	 * @param 	id	the menus unique ID
	 */
	public void setParentID( Long id, boolean recursive );
	public void setChildID( Long id, boolean recursive );

//	public static Long focusSubmenus( Vmenu me, Integer FocusSlot )
//		{
//		int pos = me.getSelectedIndex();
//		if( pos < 0 )	{ return(new Long(-1)); }
//		Vmenu m = me.getSubmenus( pos );
//		if( m == null )	{ return(new Long(-1)); }
//		
//		me.setChildID( m.getFocusId() );
//		m.setParentID( me.getFocusId() );
//		
//		MENU_FOCUS[FocusSlot] = m.getFocusId();
//		
//		return( MENU_FOCUS[FocusSlot] );
//		}
	

	public static Long getRandomID()
		{
		return( new Double(
				Math.random() * Long.MAX_VALUE ).longValue());
		}
	
	/**	  Loads most basic sound effects.
	 * 	  does nothing if these sounds are not availabe.
	 * They are expected to be in the subdirectory called <src>/sounds/
	 */
	public static void loadStandardSounds( Vmenu target )
		{
		target.attachSound(enumMenuEVENT.CANCEL,
			new VSound( load("\\sounds\\cancel.wav" ) )	);
		target.attachSound(enumMenuEVENT.CONFIRM,
			new VSound( load("\\sounds\\select.wav" ) )	);
		target.attachSound(enumMenuEVENT.MOVE,
			new VSound( load("\\sounds\\pointer.wav" ) )	);
		return;
		}

	/** This will search a Vmenu's components hotkeys for a target.
	 * If Found, it will activate that menuitem.   As a static interface method,
	 *  any properly implemented Vmenu with Vmenuitem
	 * contents enabled with hotkey functions will activate properly.
	 * !! note that hotkey codes are extended keycodes. */
	public static boolean passHotKey( Vmenu vm, int keycode )
		{
		int members = vm.countMenuItems();
		for( int n = 0; n < members; n++ )
			{
			if( vm.getMenuItem(n).getKeycode() == keycode )
				{
				vm.getMenuItem(n).doAction();
				System.out.println("Struck hotkey for menuitem # "+
					Integer.toString(n) + 
					" in menu id "+Long.toString(vm.getFocusId()) );
				return(true);
				}
			}
		return(false);
		}

	/**  Static method to retreive the function attached to the hotkey
	 * activation.   Use this when the attached method cannot be executed
	 * from the menuitems scope.  MUST check result for null. */
	public static Method getHotKeyMethod( Vmenu vm, int keycode )
		{
		int members = vm.countMenuItems();
		for( int n = 0; n < members; n++ )
			{
			if( vm.getMenuItem(n).getKeycode() == keycode )
				{
				System.out.println("Struck hotkey for menuitem # "+
					Integer.toString(n) + 
					" in menu id "+Long.toString(vm.getFocusId()) );
				return( vm.getMenuItem(n).getAction() );
				}
			}
		return( null );
		}
	public static Object[] getHotKeyMethodArgs( Vmenu vm, 
			int keycode )
		{
		int members = vm.countMenuItems();
		for( int n = 0; n < members; n++ )
			{
			if( vm.getMenuItem(n).getKeycode() == keycode )
				{	return( vm.getMenuItem(n).getActionArgs() );	}
			}
		return( new Object[]{} );
		}

	
	/** Returns if any Vmenu contains an element set to a given hetkey. */
	public static boolean hasHotKey( Vmenu vm, int keycode )
		{
		int members = vm.countMenuItems();
		for( int n = 0; n < members; n++ )
			{
			if( vm.getMenuItem(n).getKeycode() == keycode )
				{	return(true);	}
			}
		return( false );
		}

	
	}
