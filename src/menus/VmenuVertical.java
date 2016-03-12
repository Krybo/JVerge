package menus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import menus.Vmenu.enumMenuEVENT;
import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import core.Controls;
import domain.VImage;
import domain.VSound;
import static core.Script.up;
import static core.Script.down;
import static core.Script.left;
import static core.Script.right;
import static core.Script.b1;
import static core.Script.waitKeyUp;
import static core.Script.unpress;


// Arranges menuitems in a traditional vertical fashion.

public class VmenuVertical implements Vmenu
	{
	
	int x = 0,y = 0;
	Integer selectedIndex = -1;		// selected menuitem in the menu.
	boolean isActive = false;
	boolean isVisible = false;
	private boolean enableCaption = false;
	private VmiTextSimple caption;
	private ArrayList<Vmenuitem> content = new ArrayList<Vmenuitem>();
	private ArrayList<Vmenu> submenus = new ArrayList<Vmenu>();
	private HashMap<enumMenuEVENT,VSound> hmSounds;
	
	// Focus : is the player currently controlling this menu?
	// give each menu an id, control focus externally
	// The external variable you use to control focus is not important.
	//   but there needs to be one and it must be consistant accross 
	//    Vmenu implementations.
	Long focusID = new Long(-1);

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
			new Double(Math.random()*1000000000.0d).longValue();
		this.hmSounds = new HashMap<enumMenuEVENT,VSound>();
		this.setCaption(" ");
		this.enableCaption = false;
		return;
		}

	public boolean paint(VImage target)
		{
		System.out.println("VerticalMenu draw called.");
		if( this.isVisible == false )	{ return(false);	}
		if( target == null )			{ return(false);	}

		this.refresh();
		
		if( this.enableCaption )
			{ this.caption.paint(target); }

		int counter = -1;
		for( Vmenuitem myvmi : this.content )
			{
			counter++;
			if( myvmi.isActive() == false ) 	{ continue; }
			if( myvmi.isVisible() == false ) 	{ continue; }
//			if( myvmi.getState() == 3 )		{ continue; }
			
//			myvmi.debug_function(null);
			myvmi.paint( target );
			}
//		System.out.println("-- uVERTICAL MENU PAINTED --");
		return true;
		}
	
	/**
	 * Control handler,  handles "signals" passed to the menu by external
	 *     control managing routines.   It changes the state of the menu
	 *     on a single keystroke basis.
	 * @param ext_keycode External extended keycode that was pressed
	 * @return	boolean- does the menu need redrawn?  in most cases, true.
	 */
	public boolean doControls( Integer kc )
		{
		boolean redraw = false;
		Integer basecode = Controls.extcodeGetBasecode( kc );
		Integer extCode = Controls.extcodeGetExtention( kc );
		boolean isShift = Controls.extcodeGetSHIFT( kc );
		boolean isCntl = Controls.extcodeGetCNTL( kc );
		boolean isAlt = Controls.extcodeGetALT( kc );
		
//		System.out.println("DEBUG: doControls got "+kc.toString()+
//				" ("+basecode.toString()+" )" );
		
		/*  -- uncomment to Debug --
		String debugtext = new String(" <Vmenu>.doControls got "+
				kc.toString()+" -- base code : "+basecode.toString() +
				" EXT: " + extCode.toString());
		if( isShift == true ) 
			{ debugtext = debugtext.concat(" w<SHIFT> "); }
		if( isCntl == true ) 
			{ debugtext = debugtext.concat(" w<CNTL> "); }
		if( isAlt == true ) 
			{ debugtext = debugtext.concat(" w<ALT> "); }
		
		System.out.println( debugtext );
		*/
		
		
		// TODO : add method to interface that will allow alterations to 
		//  all submenus items - like icons on/off, borders.. etc.
		
		switch ( basecode )
			{
			case 8:			// BACKSPACE <CANCEL>
				this.content.get(this.selectedIndex).setState(
					enumMenuItemSTATE.NORMAL.value() );
				this.playMenuSound(enumMenuEVENT.CANCEL, 33 );
				redraw=true;
				break;
			case 10:			// ENTER KEY <CONFIRM>
				this.funcActivate();
				redraw=true;
				break;
			case 32:			// SPACE BAR
				break;
			case 37:		// ARROW-LEFT
				if( isCntl )
					{
					this.x--;
					if( this.x < 0 ) 	{ this.x = 0; }	
					}
				redraw = true;
				break;
			case 38:			// ARROW-UP
				if( isShift )	{ break; }
				redraw = true;
				if( isCntl )
					{
					this.y--;
					if( this.y < 0 )   { this.y = 0; }
					break;
					}

//				this.content.get(this.selectedIndex).setState(
//					enumMenuItemSTATE.NORMAL.value() );
				this.selectedIndex--;				
				if( this.selectedIndex < 0 )
					{ this.selectedIndex = this.content.size() - 1; }
				
				this.playMenuSound(enumMenuEVENT.MOVE, 33 );
				
//				this.content.get(this.selectedIndex).setState(
//					enumMenuItemSTATE.SELECTED.value() );

//				System.out.print(this.selectedIndex.toString() );
				break;
			case 39:  		// ARROW-RIGHT
				if( isCntl )
					{
					this.x++;
					// TODO: bound this screenwidth... somehow	
					}
				redraw = true;
				break;
			case 40:	// ARROW-DOWN
				redraw = true;
				if( isShift )	{ break; }
				if( isCntl )
					{
					this.y++;
//			TODO		if( this.y > ??? )   { this.y = ???; }
					break;
					}

				// Change the state of the new selected menuitem.
//				this.content.get(this.selectedIndex).setState(
//					enumMenuItemSTATE.NORMAL.value() );
				this.selectedIndex++;
				if( this.selectedIndex > this.content.size()-1 )
					{ this.selectedIndex = 0; }
				
				this.playMenuSound(enumMenuEVENT.MOVE, 33 );

//				this.content.get(this.selectedIndex).setState(
//					enumMenuItemSTATE.SELECTED.value() );

//				System.out.print(this.selectedIndex.toString() );
				break;
			default:
				System.out.println(" unhandled menu keystroke ["
					+kc.toString()+" ]  Base <"+basecode.toString()+"> " );
				break;
			}

		if( redraw )
			{	this.resolvePositions();	}
		return(redraw);
		}

	private void resolvePositions()
		{
		int maxw = 0;
		int iY = 0;
		int hi;

		if( this.enableCaption == true )
			{
			iY = this.caption.getDY().intValue();
			maxw = this.caption.getDX().intValue();
			this.caption.reposition(this.x, this.y, 0, 0);
			}
		for( Vmenuitem vmi : this.content )
			{ 	
			if( vmi.getDX().intValue() > maxw )    
				{  maxw = vmi.getDX().intValue(); }	
			}

		// Keep x the same, add each items height to create vertical menu.
		for( Vmenuitem vmi : this.content )
			{
			hi = vmi.getDY().intValue();
			vmi.setExtendX(maxw, true );
			vmi.reposition(this.x, this.y, 0, iY);
			iY += hi;			
			}
		this.caption.setExtendX(maxw, false );
		return;
		}
	
		// Resolve states of items.
	private void resolveStates()
		{
		int counter = -1;
		for( Vmenuitem myvmi : this.content )
			{
			counter++;
			if( myvmi.isActive() == false )  { continue; }

				// Manage state based on selectedIndex
				// Do different things to selected/non-selected items
				//    sometimes based on their individual state
			if( counter == this.selectedIndex )
				{		// This is the currently selected content
				switch( this.content.get(counter).getState() )
					{
					case 2:		//  Activated 
						break;
					case 3:		// Disabled 
						break;
					default: 
						this.content.get(counter).setState(
							enumMenuItemSTATE.SELECTED.value() );
						break;
					}
				}
			else		// This is NOT  selected content
				{
				switch( this.content.get(counter).getState() )
					{
					// case 2:  let activated items return to normal.
					case 3:		// Disabled 
						break;
					default: 
						this.content.get(counter).setState(
							enumMenuItemSTATE.NORMAL.value() );
						break;
					}
				}
			
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
		// adding first item, certain things need to be init now.
		if( this.content.isEmpty() )
			{ this.selectedIndex = 0; }
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

	public void setFocusId( Long theId )
		{
		this.focusID = theId;
		return;
		}
	public Long getFocusId()	
		{ return(this.focusID); }

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

	public boolean isFocus( Long id )
		{
		if( id == this.focusID )
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

	public void attachSound( enumMenuEVENT slot, VSound sfx )
		{
		if( sfx == null ) { return; }
		this.hmSounds.put(slot, sfx);
		return;
		}

	public boolean playMenuSound( enumMenuEVENT slot, int volume0to100 )
		{
		if( this.hmSounds.get(slot) == null )	{ return(false); }
		this.hmSounds.get(slot).start( volume0to100 );
		return(true);
		}
	
	/**
	 * This is run when the ENTER Key is used.  Invokes selected menuitem
	 */
	private void funcActivate()
		{
		if( this.content.get(this.selectedIndex).getState() == 
			enumMenuItemSTATE.DISABLED.value() )
			{ return; }	// So Cannot activate greyed out items.
		this.content.get(this.selectedIndex).setState(
				enumMenuItemSTATE.ACTIVATED.value() );
		// Time for action!   Does whatever Method the menuitem is set
		if( this.content.get(this.selectedIndex).doAction() )
			{
			this.playMenuSound(enumMenuEVENT.CONFIRM, 33 );
			}
		}

	
	/**
	 * The caption is an auxilary Vmenuitem used for the Menus Title.
	 * @param theCapt  String - The displayed title.
	 */
	public void setCaption( String theCapt )
		{
		VmiTextSimple newCaption = new VmiTextSimple( theCapt );
		newCaption.reposition(this.x, this.y, 0, 0);
		newCaption.enableBackdrop(true);
		newCaption.enableFrame(false);
		newCaption.enableIcons(false);
		newCaption.enableText(true);
		newCaption.setState(enumMenuItemSTATE.NORMAL.value() );
		newCaption.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
				Color.BLACK);
		this.caption = newCaption;
		return;
		}
	public void setCaptionVisible(boolean show )
		{	this.enableCaption = show;	return;	}
	public boolean isCaptionEnabled()
		{ return(this.enableCaption); }
	
	}
