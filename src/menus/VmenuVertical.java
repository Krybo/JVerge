package menus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import core.Controls;
import domain.VImage;
import domain.VSound;



// Arranges menuitems in a traditional vertical fashion.

public class VmenuVertical implements Vmenu
	{
	
	private int x = 0,y = 0;
	private int w = 0, h = 0;
	private Integer selectedIndex = -1;		// selected menuitem in the menu.
	private boolean isActive = false;
	private boolean isVisible = false;
	private boolean enableCaption = false;
	private boolean enableImgBackground = false;
	private VmiTextSimple caption;
	private ArrayList<Vmenuitem> content = 
			new ArrayList<Vmenuitem>();
	private VImage bkgImg = null;
//	private ArrayList<Vmenu> submenus = new ArrayList<Vmenu>();
	private HashMap<Integer,Vmenu> hmSubmenus = 
			new HashMap<Integer,Vmenu>();
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();

	// Focus : is the player currently controlling this menu?
	// give each menu an id, control focus externally
	// The external variable you use to control focus is not important.
	//   but there needs to be one and it must be consistant accross 
	//    Vmenu implementations.
	Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

	public VmenuVertical()
		{ 	this(0,0);	}

	public VmenuVertical(int x, int y)
		{
		this.x = x;
		this.y = y;
		this.w = 0;
		this.h = 0;
		this.content.clear();
		this.hmSubmenus.clear();
		this.selectedIndex = -1;
		this.focusID = Vmenu.getRandomID();
		this.hmSounds = new HashMap<enumMenuEVENT,VSound>();
		this.setCaption(" ");
		this.enableCaption = false;
		this.enableImgBackground = false;
		return;
		}

	public boolean paint( VImage target)
		{
//		System.out.println("VerticalMenu draw called.");
		if( this.isVisible == false )	{ return(false);	}
		if( target == null )			{ return(false);	}

		this.refresh();
		
		if( this.enableImgBackground == true && this.bkgImg != null )
			{
			target.scaleblit( this.x, this.y, this.w, this.h, this.bkgImg );
			}

		if( this.enableCaption )
			{ this.caption.paint(target); }

		int counter = -1;
		for( Vmenuitem myvmi : this.content )
			{
			counter = counter + 1;
			if( myvmi.isVisible() == false ) 	{ continue; }
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
		
		if( kc <= -1 )	// fake keystroke.   cause redraw
			{
			this.resolvePositions();
			return(true);
			}
		
		Integer basecode = Controls.extcodeGetBasecode( kc );
//		Integer extCode = Controls.extcodeGetExtention( kc );
		boolean isShift = Controls.extcodeGetSHIFT( kc );
		boolean isCntl = Controls.extcodeGetCNTL( kc );
//		boolean isAlt = Controls.extcodeGetALT( kc );		
		
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
			if( vmi.isActive() == false )	{ continue; }
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
		this.w = maxw;
		this.h = iY;
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

	/**  Given a Long menu id , checks all menuItems for that ID
	 * 	If so, Returns the menu item object.   ALWAYS check for null.
	 */
	public Vmenuitem getMenuItemByID( Long id )
		{
		for( Vmenuitem vmi : this.content )
			{
			if( vmi.getId() == id  ) 		{ return(vmi); }
			}
		return null;
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

	public void setActive( boolean active)
		{
		this.isActive = active;
		return;
		}
	public void setVisible( boolean vis)
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

//	public int addSubmenus( Vmenu slave)
//		{
//		this.hmSubmenus.put( this.hmSubmenus.size()-1 ,slave);
//		return(this.hmSubmenus.size());
//		}

//	public Vmenu getSubmenus( Integer submenuIndex )
//		{
//		if( this.hasSubmenus() == false )   { return(null);  }
//		if( submenuIndex >= this.hmSubmenus.size() )   { return(null);  }
//		return this.hmSubmenus.get(submenuIndex);
//		}

//	public boolean setSubmenus( Vmenu slave , Integer slot )
//		{
//		if( slot >= this.hmSubmenus.size() || slot < 0 )
//			{
//			this.hmSubmenus.put( slot, null );
//			return(false);
//			}
//		this.hmSubmenus.put( slot, slave );
//		return(true);
//		}

//	public Vmenu popSubmenus()
//		{
//		if( this.hasSubmenus() == false )   {  return(null); }
//		return this.hmSubmenus.remove( this.hmSubmenus.size()-1 );
//		}

//	public boolean hasSubmenus()
//		{
//		if( this.hmSubmenus.isEmpty() )  { return(false); }
//		return true;
//		}

//	public int countSubmenus()
//		{
//		if( this.hasSubmenus() == false ) { return(0); }
//		return(this.hmSubmenus.size());
//		}

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
		if( this.hmSounds == null )				{ return(false); }
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

	public void setIconsAll( boolean onOff )
		{
		for( Vmenuitem myvmi : this.content )
			{	myvmi.enableIcons(onOff);	}
		return;
		}
	public void setBorderAll( boolean onOff, int thick )
		{
		for( Vmenuitem myvmi : this.content )
			{
			myvmi.enableFrame(onOff);
			myvmi.setFrameThicknessPx(thick);
			}		
		return;
		}

	public void setParentID( Long id )
		{
		this.parentID = id;
		for( Vmenuitem vmi : this.content )
			{	vmi.setParentID(id);	}
		}
	public void setChildID( Long id )
		{
		this.childID = id;
		for( Vmenuitem vmi : this.content )
			{	vmi.setChildID(id);	}
		}
	public Long getParentID()
		{	return(this.parentID);	}
	public Long getChildID()
		{	return(this.childID);	}
	
	/**  Sets a background image by passing a Vimage
	 *   The Vimage will be Scaled to fit the width and height after
	 *   at least one menuitem is added.
	 *  
	 * @param theBkg	A Vimage object containing the background
	 * @param enable	Set all values needed to ensure the bkg is visible
	 */
	public void setBackgroundImage( VImage theBkg, boolean enable )
		{
		this.bkgImg = theBkg;
		if( enable == true )
			{
			this.enableImgBackground = enable;
				// To ensure visibility - turn OFF component bkgs
			for( Vmenuitem vmi : this.content )
				{	vmi.enableBackdrop(true);	}
			}
		else
			{
			this.enableImgBackground = enable;
			for( Vmenuitem vmi : this.content )
				{	vmi.enableBackdrop(false);	}
			}
		return;
		}
	
//	
//	private ArrayList<Long> getSubmenuFocusID()
//		{
//		boolean recurse = true;
//		ArrayList<Long> rslt = new ArrayList<Long>();
//
//			// Recurses up to 5 levels and gets IDs
//		for( Integer x : this.hmSubmenus.keySet() ))
//			{
//			rslt.add( this.hmSubmenus.get(x).getFocusId() );
//			if( this.hmSubmenus.get(x).hasSubmenus() == true )
//				{
//				Vmenu tmp = this.getSubmenus(x);
//				rslt.add(tmp.getFocusId());
//				if( tmp.hasSubmenus() == true )
//					{
//					Vmenu tmp2 = tmp.getSubmenus(x);
//					rslt.add(tmp2.getFocusId());
//					if( tmp2.hasSubmenus() == true )
//						{
//						Vmenu tmp3 = tmp2.getSubmenus(x);
//						rslt.add(tmp3.getFocusId());
//						if( tmp3.hasSubmenus() == true )
//							{
//							Vmenu tmp4 = tmp3.getSubmenus(x);
//							rslt.add(tmp4.getFocusId());
//							if( tmp4.hasSubmenus() == true )
//								{
//								Vmenu tmp5 = tmp4.getSubmenus(x);
//								rslt.add( tmp5.getFocusId() );
//								}
//							}
//						}					
//					}
//				}
//			}
//		return(rslt);
//		}
	
//	public Vmenu getSubmenuByID( Long targetID )
//		{
//
//		for( Integer x : this.hmSubmenus.keySet() ))
//			{
//			Vmenu tmp = this.hmSubmenus.get(x);
//			if( tmp.getFocusId() == targetID )	{ return(tmp); }
//			if( tmp.hasSubmenus()  == true )
//				{
//					
//				}
//			}
//		}
	
	}
