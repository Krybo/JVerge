package menus;

import static core.Script.load;
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
 *     VImage object, which can be an existing object, or temporary one.
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
	public Integer addItem(Vmenuitem vmi );
	public Vmenuitem popItem();
	public Vmenuitem removeItem(int index);
	public Integer insertItem(Vmenuitem vmi , int index );
	
	public void refresh();
	
	public Vmenuitem getMenuItem(int index);
	public Integer getSelectedIndex();
	public int getSelectedIndexPosX();
	public int getSelectedIndexPosY();
	
	public void setSelectedIndex(Integer index);
	public void setFocusId( Long id );
	public Long getFocusId( );
	public boolean isFocus( Long id );
	public void setActive( boolean active );
	public void setVisible( boolean vis );
	
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
	public void setParentID( Long id );
	public void setChildID( Long id );

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

	}
