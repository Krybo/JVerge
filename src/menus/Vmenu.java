package menus;

import domain.VImage;
import domain.VSound;

/* Krybo (Feb.2016)
 * 
 * :: Vmenu ::   The menu interface  
 * 	Main purpose is to wrangle up Vmenuitems into a collection and  
 *  manage their display properties as a full menu.   
 *  The implemented Classes will 
 *  manage controls and paint the layout of the child menuitems and
 *  also control its subMenus.
 *  
 *  One master menu can be created from chaining together any various
 *     polymorphic Classes that implement this interface.   The instanced 
 *     objects should be built from the bottom (submenus), then main.
 *  The paint() methods in the Vmenuitems do most of the brute work.
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
	
		// This class can recurse itself.   Keep a list of menu objects.
		//  These menus can then be referenced by menuitems within.
	public int addSubmenus( Vmenu slave );
	public Vmenu getSubmenus( int submenuIndex );
	public boolean setSubmenus( Vmenu slave, int index );
	public Vmenu popSubmenus( );
	public boolean hasSubmenus();
	public int countSubmenus();

	public void activateSelected();

	/**
	 * Implement this to attach sounds to generalized menu events
	 * @param slot	enum menu event
	 * @param sfx	The VSound object to play on this event.
	 */
	public void attachSound( enumMenuEVENT slot, VSound sfx );
	public boolean playMenuSound( enumMenuEVENT slot, int volume0to100 );
	
	}
