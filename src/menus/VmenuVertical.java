package menus;

import java.util.ArrayList;

import domain.VImage;
import static core.Script.MENU_FOCUS;

// Arranges menuitems in a traditional vertical fashion.

public class VmenuVertical implements Vmenu
	{
	
	int x = 0,y = 0;
	Integer selectedIndex = -1;		// selected menuitem in the menu.
	boolean isActive = false;
	boolean isVisible = false;
	ArrayList<Vmenuitem> content = new ArrayList<Vmenuitem>();
	ArrayList<Vmenu> submenus = new ArrayList<Vmenu>();
	
	// Focus : is the player currently controlling this menu?
	// give each menu an id, control focus externally
	// The external variable you use to control focus is not important.
	//   but there needs to be one and it must be consistant accross 
	//    Vmenu implementations.
	Integer focusID = -1;

	public VmenuVertical()
		{ 	this(0,0);	}
	
	public VmenuVertical(int x, int y)
		{
		this.x = x;
		this.y = y;
		this.content.clear();
		this.submenus.clear();
		this.selectedIndex = -1;
		this.focusID = 
			new Double(Math.random()*1000000000.0d).intValue();
		
		return;
		}

	public boolean paint(VImage target)
		{
		if( this.isVisible == false )	{ return(false);	}
		if( target == null )			{ return(false);	}

		this.refresh();
		
		int counter = -1;
		for( Vmenuitem myvmi : this.content )
			{
			counter++;
			if( myvmi.isActive() == false ) 	{ continue; }
			if( myvmi.isVisible() == false ) 	{ continue; }
//			if( myvmi.getState() == 3 )		{ continue; }
			
			myvmi.paint( target );
			}
		return true;
		}
	
	private void resolvePositions()
		{
		int maxw = 0;
		int iY = 0;
		int hi;
		for( Vmenuitem vmi : this.content )
			{ 	
			if( vmi.getDX().intValue() > maxw )    
				{  maxw = vmi.getDX().intValue(); }	
			}

		// Keep x the same, add each items height to create vertical menu.
		for( Vmenuitem vmi : this.content )
			{
			hi = vmi.getDY().intValue();
			vmi.setExtendX(maxw);
			vmi.reposition(this.x, this.y, 0, iY);
			iY += hi;			
			}
		return;
		}
	
		// Resolve states of items.
	private void resolveStates()
		{
		int counter = 0;
		for( Vmenuitem myvmi : this.content )
			{
			counter++;
			if( myvmi.isActive() == false )  { continue; }

				// Manage state based on selectedIndex
			if( counter == this.selectedIndex )
				{	this.content.get(counter).setState(1);	}
			else if ( myvmi.getState() == 3 )
				{	continue;		}
			else	{ myvmi.setState(0); }
			}
		return;
		}

	public void moveAbs(int x, int y)
		{
		this.x = x;
		this.y = y;
		return;
		}

	public void moveRel(int x, int y)
		{
		this.x = this.x + x;
		this.y = this.y + y;
		if( this.x < 0 ) 	{  this.x = 0; }
		if( this.y < 0 ) 	{  this.y = 0; }
		return;
		}

	public Integer countMenuItems()
		{
		return( new Integer(this.content.size()) );
		}

	public Integer addItem(Vmenuitem vmi)
		{
		this.content.add(vmi);
		return( new Integer(this.content.size()) );
		}

	public Vmenuitem popItem()
		{
		return this.content.remove( this.content.size()-1 );
		}

	public Vmenuitem removeItem(int index)
		{
		return this.content.remove(index);
		}

	public Integer insertItem(Vmenuitem vmi, int index)
		{
		this.content.add(index, vmi);
		return( new Integer(this.content.size()) );
		}

	public void refresh()
		{
		this.resolveStates();
		this.resolvePositions();
		return;
		}

	public Vmenuitem getMenuItem(int index) 
			throws IndexOutOfBoundsException
		{
		if( index < 0 || index >= this.content.size() )
			{ 	
			throw new IndexOutOfBoundsException(
					"Invalid menu index "+Integer.toString(index)+" max "+
					Integer.toString(this.content.size()-1) ); 	
			}
		return this.content.get(index);
		}

	public Integer getSelectedIndex()
		{	return this.selectedIndex;	}
	public int getSelectedIndexAsInt()
		{ return this.getSelectedIndex().intValue(); }

	public int getSelectedIndexPosX()
		{	return this.content.get(selectedIndex).getX().intValue();	}
	public int getSelectedIndexPosY()
		{	return this.content.get(selectedIndex).getY().intValue();	}


	public void setSelectedIndex(Integer index)
		{
		this.selectedIndex = index;
		}
	public Integer incrementSelectedIndex()
		{ 
		this.selectedIndex++;
		if( this.selectedIndex >= this.content.size() )
			{ this.selectedIndex = 0; }
		return(this.selectedIndex);
		}

	public void setFocus( int focusControlIndex )
		{
		MENU_FOCUS[0] = this.focusID;
		return;
		}

	public void setActive(boolean active)
		{
		this.isActive = active;
		return;
		}
	public void setVisible(boolean vis)
		{
		this.isVisible = vis;
		return;
		}

	public boolean isFocus( int focusControlIndex )
		{
		if( MENU_FOCUS[focusControlIndex] == this.focusID )
			{ return(true); }
		return(false);
		}

	public boolean isActive()
		{	return this.isActive;	}
	public boolean isVisible()
		{	return this.isVisible;	}

	public int addSubmenus(Vmenu slave)
		{
		this.submenus.add(slave);
		return(this.submenus.size());
		}

	public Vmenu getSubmenus(int submenuIndex)
		{
		if( this.hasSubmenus() == false )   { return(null);  }
		if( submenuIndex >= this.submenus.size() )   { return(null);  }
		return this.submenus.get(submenuIndex);
		}

	public boolean setSubmenus(Vmenu slave, int index)
		{
		if( index >= this.submenus.size() || index < 0 )
			{
			this.submenus.add(slave);
			return(false);
			}
		this.submenus.add(index, slave );
		return true;
		}

	public Vmenu popSubmenus()
		{
		if( this.hasSubmenus() == false )   {  return(null); }
		return this.submenus.remove( this.submenus.size()-1 );
		}

	public boolean hasSubmenus()
		{
		if( this.submenus.isEmpty() )  { return(false); }
		return true;
		}

	public int countSubmenus()
		{
		if( this.hasSubmenus() == false ) { return(0); }
		return(this.submenus.size());
		}

	public void activateSelected()
		{
		this.content.get(this.selectedIndex).doAction();
		return;
		}

	}
