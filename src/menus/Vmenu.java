package menus;

import domain.VImage;

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

	public boolean paint( VImage target );
	
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
	public void setFocus( int focusControlIndex );
	public void setActive( boolean active );
	public void setVisible( boolean vis );
	
	public boolean isFocus( int focusControlIndex );
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
	
	}
