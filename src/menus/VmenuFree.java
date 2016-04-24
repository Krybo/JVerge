package menus;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import core.Controls;
import domain.VImage;
import domain.VSound;

/** This Vmenu Free implementation is a simple collection of Vmenuitems 
 * (any implemented Class) in a free display, meaning the designer has
 * control over where components are placed using relative coordintes.
 * Unlike Horizontal and Vertical menus, this type has a fixed width / Height,
 * and if component items are drawn outside bounds, they are simply
 * not painted..
 * 
 * Items are painted last-in > first-out.   And this will paint items overtop
 * each other.   No collision detection is done.  Placing items are completely
 * in the users hands.   Set appropriate relative positions of menu items 
 * with .reposition() either before or after attaching them ot the menu.
 *    
 * Good for Extending custom menus by taking over the controls and 
 *  placement of items.
 * 
 *  Make sure you create a way to return to the originating menus,
 * if there is one -- Either in doControls or a doAction() of one of the items.
 * (Apr.2016)
 * 
 * @author Krybo
 *
 */

public class VmenuFree implements Vmenu
	{

	private int x = 0, y = 0;
	private int w = 0, h = 0;
	private Integer selectedIndex = -1; // selected menuitem in the menu.
	private boolean isActive = false;
	private boolean isVisible = false;
	private boolean enableCaption = false;
	private boolean enableImgBackground = false;
	private VmiTextSimple caption;
	private ArrayList<Vmenuitem> content = new ArrayList<Vmenuitem>();
	private VImage bkgImg = null;
	// Holds sound the menus effects.
	private HashMap<enumMenuEVENT, VSound> hmSounds = new HashMap<enumMenuEVENT, VSound>();

	// Focus : is the player currently controlling this menu?
	// give each menu an id, control focus externally
	// The external variable you use to control focus is not important.
	// but there needs to be one and it must be consistant accross
	// Vmenu implementations.
	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

	/** Most basic construct.   The menus will be able to use the entire 
	 * area of the first target paint()ed   */
	public VmenuFree()
		{	this(0, 0, -1, -1);	}

	/** Constructor with default width and height, which will adapt to
	 * fill the area between the anchor point (x,y) and bottom right 
	 * corner of the first paint() target. */
	public VmenuFree(int x, int y )
		{	this( x, y, -1, -1);	}
	
	/** Constructor - setting with anchor point */
	public VmenuFree(int x, int y, int widthPx, int heightPx )
		{
		this.x = x;
		this.y = y;
		this.w = widthPx;
		this.h = heightPx;
		if( this.w < -1 )	{ this.w = -1;  }
		if( this.h < -1 )	{ this.h = -1;  }
		
		this.focusID = Vmenu.getRandomID();
		this.content.clear();
		this.selectedIndex = -1;
		this.hmSounds = new HashMap<enumMenuEVENT, VSound>();
		this.setCaption(" ");

		this.setEnableCaption(false);
		this.setEnableImgBackground(false);

		this.isActive = true;
		this.isVisible = true;
		this.setEnableCaption(false);
		this.setEnableImgBackground(false);

		return;
		}

	public boolean paint(VImage target)
		{
		if (this.isVisible == false)
			{	return (false);	}
		if( this.w <= -1 )
			{  this.w = target.getWidth() - this.x;	}
		if( this.h <= -1 )
			{  this.h = target.getHeight() - this.y;	}
		if (target == null)
			{	return (false);	}

		this.refresh();

		int boundX = this.x + this.w;
		int boundY = this.y + this.h;
		
		if (this.isEnableImgBackground() == true && this.bkgImg != null)
			{
			target.scaleblit(this.x, this.y, this.w, this.h, this.bkgImg);
			}

		if (this.isEnableCaption())
			{
			this.caption.paint(target);
			}

		int counter = -1;
		for (Vmenuitem myvmi : this.content)
			{
			counter = counter + 1;
			if (myvmi.isVisible() == false)
				{	continue;	}
			if( myvmi.getX() >= boundX || myvmi.getX() < 0 )
				{ continue; }
			if( myvmi.getY() >= boundY || myvmi.getY() < 0 )
				{ continue; }
			myvmi.paint(target);
			}
		// System.out.println("-- uVERTICAL MENU PAINTED --");
		return true;
		}

	/**
	 * Control handler, handles "signals" passed to the menu by external
	 * control managing routines. It changes the state of the menu on a single
	 * keystroke basis.
	 * 
	 * @param ext_keycode
	 *             External extended keycode that was pressed
	 * @return boolean- does the menu need redrawn? in most cases, true.
	 */
	public boolean doControls(Integer kc)
		{
		boolean redraw = false;

		if (kc <= -1) // fake keystroke. cause redraw
			{
			this.resolvePositions();
			return (true);
			}

		Integer basecode = Controls.extcodeGetBasecode(kc);
		// Integer extCode = Controls.extcodeGetExtention( kc );
		boolean isShift = Controls.extcodeGetSHIFT(kc);
		boolean isCntl = Controls.extcodeGetCNTL(kc);
		// boolean isAlt = Controls.extcodeGetALT( kc );

		switch (basecode)
			{
			case 8: // BACKSPACE <CANCEL>
				this.content.get(this.selectedIndex).setState(
						enumMenuItemSTATE.NORMAL.value());
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				redraw = true;
				break;
			case 10: // ENTER KEY <CONFIRM>
				this.funcActivate();
				redraw = true;
				break;
			case 35:		// home/end
			case 36:
			case 32: 		// SPACE BAR
				break;
			case 33: 		// Page UP
				redraw=true;
				this.moveSelection( this.countMenuItems()*-1, false );
				break;
			case 34:		// page Down
				redraw=true;
				this.moveSelection( this.countMenuItems(), false );
				break;
			// Both UP and LEFT do the same.  so don't break; here.
			case 38: // ARROW-UP
				if (isShift)		{	break;  }
				redraw = true;
				if (isCntl)
					{
					this.moveRel( 0, -1);
					if( this.y < 0 )
						{	this.moveAbs( this.x, 0 );	}
					break;
					}

			case 37: // ARROW-LEFT
				if (isShift)		{	break;  }
				redraw = true;
				if (isCntl)
					{
					this.moveRel(-1, 0);
					if( this.x < 0 )
						{	this.moveAbs( 0, this.y );	}
					break;
					}
				this.moveSelection( -1, true );
				// Ensure new selection is active.
				if (this.getActiveItemCount() == 0 )
					{	break;	}		// infinite loop protection
				while( this.isSelectionActive() == false )
					{	this.moveSelection( -1, true );	}
	
				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;

				// Both DOWN and RIGHT do the same.  so don't break; here.
			case 40: // ARROW-DOWN
				if (isShift)		{	break;  }
				redraw = true;
				if (isCntl)
					{
					this.moveRel( 0, 1);
					break;
					}
			case 39: // ARROW-RIGHT
				if (isShift)		{	break;  	}
				redraw = true;
				if (isCntl)
					{	
					this.moveRel( 1, 0 );
					break;
					}
				this.moveSelection( +1, true );
				// Ensure new selection is active.
				if (this.getActiveItemCount() == 0) 
					{	break;	}		// infinite loop protection
				while( this.isSelectionActive() == false )
					{
					this.moveSelection( +1, true );
					}
	
				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;

			default:
				System.out.println(" unhandled menu keystroke ["
						+ kc.toString() + " ]  Base <"
						+ basecode.toString() + "> ");
				break;
			}

		if( redraw )
			{	this.resolvePositions();	}
		return( redraw );
		}

	protected void resolvePositions()
		{
		// Do not monkey with positions in a Free menus
		return;
		}

	// Resolve states of items.
	private void resolveStates()
		{
		int counter = -1;
		for (Vmenuitem myvmi : this.content)
			{
			counter++;
			if (myvmi.isActive() == false)
				{
				continue;
				}

			// Manage state based on selectedIndex
			// Do different things to selected/non-selected items
			// sometimes based on their individual state
			if (counter == this.selectedIndex)
				{ // This is the currently selected content
				switch (this.content.get(counter).getState())
					{
					case 2: // Activated
						break;
					case 3: // Disabled
						break;
					default:
						this.content.get(counter).setState(
								enumMenuItemSTATE.SELECTED.value());
						break;
					}
				}
			else
				// This is NOT selected content
				{
				switch (this.content.get(counter).getState())
					{
					// case 2: let activated items return to normal.
					case 3: // Disabled
						break;
					default:
						this.content.get(counter).setState(
								enumMenuItemSTATE.NORMAL.value());
						break;
					}
				}

			}
		return;
		}

	public void moveAbs(int x, int y)
		{
		int dx = x - this.x;
		int dy = y - this.y;
		this.x = x;
		this.y = y;
		for( Vmenuitem vmi : this.content )
			{	vmi.repositionDelta( dx, dy, 0, 0 );	}
		this.caption.repositionDelta( dx, dy, 0, 0 );
		return;
		}

	/** Nudge the entire menu in pixels in x or y dims. */
	public void moveRel(int x, int y)
		{
		this.x = this.x + x;
		this.y = this.y + y;
		
		for( Vmenuitem vmi : this.content )
			{	vmi.repositionDelta( x, y, 0, 0 );	}
		this.caption.repositionDelta( x, y, 0, 0 );

		if (this.x < 0)
			{
			this.x = 0;
			}
		if (this.y < 0)
			{
			this.y = 0;
			}
		return;
		}

	public Integer countMenuItems()
		{
		return (new Integer(this.content.size()));
		}

	public Integer addItem(Vmenuitem vmi)
		{
		// adding first item, certain things need to be init now.
		if (this.content.isEmpty())
			{
			this.selectedIndex = 0;
			}
		this.content.add(vmi);
		return (new Integer(this.content.size()));
		}

	public Vmenuitem popItem()
		{
		return this.content.remove(this.content.size() - 1);
		}

	public Vmenuitem removeItem(int index)
		{
		return this.content.remove(index);
		}

	public Integer insertItem(Vmenuitem vmi, int index)
		{
		this.content.add(index, vmi);
		return (new Integer(this.content.size()));
		}

	public void refresh()
		{
		this.resolveStates();
		this.resolvePositions();
		return;
		}

	public Vmenuitem getMenuItem(int index) throws IndexOutOfBoundsException
		{
		if (index < 0 || index >= this.content.size())
			{
			throw new IndexOutOfBoundsException("Invalid menu index "
					+ Integer.toString(index) + " max "
					+ Integer.toString(this.content.size() - 1));
			}
		return this.content.get(index);
		}

	public Vmenuitem getMenuItemSelected()
		{
		if( this.selectedIndex < 0 || this.selectedIndex >= this.content.size())
			{ return(null); }
		return( this.content.get(this.selectedIndex) );
		}
	
	/**
	 * Given a Long menu id , checks all menuItems for that ID If so, Returns
	 * the menu item object. ALWAYS check for null.
	 */
	public Vmenuitem getMenuItemByID(Long id)
		{
		for (Vmenuitem vmi : this.content)
			{
			if (vmi.getId() == id)
				{
				return (vmi);
				}
			}
		return null;
		}

	public Integer getSelectedIndex()
		{
		return this.selectedIndex;
		}

	public int getSelectedIndexAsInt()
		{
		return this.getSelectedIndex().intValue();
		}

	public int getSelectedIndexPosX()
		{
		return this.content.get(selectedIndex).getX().intValue();
		}

	public int getSelectedIndexPosY()
		{
		return this.content.get(selectedIndex).getY().intValue();
		}

	public void setSelectedIndex(Integer index)
		{
		this.selectedIndex = index;
		}

	public Integer incrementSelectedIndex()
		{
		this.selectedIndex++;
		if (this.selectedIndex >= this.content.size())
			{
			this.selectedIndex = 0;
			}
		return (this.selectedIndex);
		}

	public void setFocusId(Long theId)
		{
		this.focusID = theId;
		return;
		}

	public Long getFocusId()
		{
		return (this.focusID);
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

	public boolean isFocus(Long id)
		{
		if (id == this.focusID)
			{
			return (true);
			}
		return (false);
		}

	public boolean isActive()
		{
		return this.isActive;
		}

	public boolean isVisible()
		{
		return this.isVisible;
		}
	
	
	/**   Repositions a component menuitem.
	 * consider using setMenuItemRelativePosition() should be used instead
	 * 
	 * @param itemIndex int - Which index menu item to move?
	 * @param posX		  int absolute X pixels of anchor point
	 * @param posY		  int absolute Y pixels of anchor point
	 * @param relPosX	  int X pixels to move relative to X anchor point
	 * @param relPosY	  int Y pixels to move relative to Y anchor point
	 * @see setMenuItemRelativePosition
	 */
	public void setMenuItemPosition( int itemIndex, 
			int posX, int posY, int relPosX, int relPosY )
		{
		if( itemIndex < 0 || itemIndex >= this.content.size() )
			{ return; }
		if( this.content.get(itemIndex) == null )
			{ return; }
		this.content.get(itemIndex).reposition( 
			posX, posY, relPosX, relPosY );
		return;
		}
	
	/**  Moves a menuitem relative the menus master x/y anchors.
	 *   Use this instead of setMenuItemPosition() whenever practical..
	 * 
	 * @param itemIndex  itemIndex int - Which index menu item to move?
	 * @param relPosX	   relative position to the menus X anchor point
	 * @param relPosY	   relative position to the menus Y anchor point
	 */
	public void setMenuItemRelativePosition( int itemIndex, 
			int relPosX, int relPosY )
		{
		this.setMenuItemPosition( itemIndex, 
				this.x, this.y, relPosX, relPosY );
		}

	public void activateSelected()
		{
		this.content.get(this.selectedIndex).doAction();
		return;
		}

	public void attachSound(enumMenuEVENT slot, VSound sfx)
		{
		if (sfx == null)
			{
			return;
			}
		this.hmSounds.put(slot, sfx);
		return;
		}

	public boolean playMenuSound(enumMenuEVENT slot, int volume0to100)
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

	/**
	 * This is run when the ENTER Key is used. Invokes selected menuitem
	 */
	protected void funcActivate()
		{
		if (this.content.get(this.selectedIndex).getState() == 
				enumMenuItemSTATE.DISABLED.value())
			{
			return;
			} // So Cannot activate greyed out items.
		this.content.get(this.selectedIndex).setState(
				enumMenuItemSTATE.ACTIVATED.value());
		// Time for action! Does whatever Method the menuitem is set
		if (this.content.get(this.selectedIndex).doAction())
			{
			this.playMenuSound(enumMenuEVENT.CONFIRM, 33);
			}
		}

	/**
	 * The caption is an auxilary Vmenuitem used for the Menus Title.
	 * 
	 * @param theCapt
	 *             String - The displayed title.
	 */
	public void setCaption(String theCapt)
		{
		VmiTextSimple newCaption = new VmiTextSimple(theCapt);
		newCaption.reposition(this.x, this.y, 0, 0);
		newCaption.enableBackdrop(true);
		newCaption.enableFrame(false);
		newCaption.enableIcons(false);
		newCaption.enableText(true);
		newCaption.setState(enumMenuItemSTATE.NORMAL.value());
		newCaption.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, Color.BLACK);
		this.caption = newCaption;
		return;
		}

	/** Use this to position the Caption where you want it.  
	 *  Relative to the anchor points of the menus */
	public void setCaptionPosition( int relX, int relY )
		{
		this.caption.reposition(this.x, this.y, relX, relY );
		}


	public void setIconsAll(boolean onOff)
		{
		for (Vmenuitem myvmi : this.content)
			{
			myvmi.enableIcons(onOff);
			}
		return;
		}

	public void setBorderAll(boolean onOff, int thick)
		{
		for (Vmenuitem myvmi : this.content)
			{
			myvmi.enableFrame(onOff);
			myvmi.setFrameThicknessPx(thick);
			}
		return;
		}

	public void setParentID( Long id, boolean recurse )
		{
		this.parentID = id;
		if( recurse == true )
			{
			for (Vmenuitem vmi : this.content)
				{
				vmi.setParentID(id);
				}
			}
		return;
		}

	public void setChildID( Long id, boolean recurse )
		{
		this.childID = id;
		if( recurse == true )
			{
			for (Vmenuitem vmi : this.content)
				{
				vmi.setChildID(id);
				}
			}
		return;
		}

	public Long getParentID()
		{
		return (this.parentID);
		}

	public Long getChildID()
		{
		return (this.childID);
		}

	/**
	 * Sets a background image by passing a Vimage The Vimage will be Scaled to
	 * fit the width and height after at least one menuitem is added.
	 * 
	 * @param theBkg
	 *             A Vimage object containing the background
	 * @param enable
	 *             Set all values needed to ensure the bkg is visible
	 */
	public void setBackgroundImage(VImage theBkg, boolean enable)
		{
		this.bkgImg = theBkg;
		if (enable == true)
			{
			this.setEnableImgBackground(enable);
			// To ensure visibility - turn OFF component bkgs
			for (Vmenuitem vmi : this.content)
				{
				vmi.enableBackdrop(true);
				}
			}
		else
			{
			this.setEnableImgBackground(enable);
			for (Vmenuitem vmi : this.content)
				{
				vmi.enableBackdrop(false);
				}
			}
		return;
		}

	protected int getActiveItemCount()
		{
		int rslt = 0;
		for (Vmenuitem vmi : this.content)
			{
			if (vmi.isActive() == true)
				{
				rslt++;
				}
			}
		return (rslt);
		}


	/** ---- These are were needed to extend the class ---- */
	
	public boolean isEnableCaption()
		{	return( this.enableCaption );	}
	public void setEnableCaption( boolean enableCaption )
		{	this.enableCaption = enableCaption;	}
	public Vmenuitem getCaptionObject()
		{	return( this.caption );	}
	public int getX()
		{ return(this.x); }
	public int getY()
		{ return(this.y); }
	public ArrayList<Vmenuitem> getContent()
		{ return(this.content); }
	public void setWidth( int newWidth )
		{ this.w = newWidth; }
	public void setHeight( int newHgt )
		{ this.h = newHgt; }
	public int getWidth()
		{ return(this.w); }
	public int getHeight()
		{ return(this.h); }

	public boolean isEnableImgBackground()
		{
		if( this.bkgImg == null )	
			{ return(false); }
		return( this.enableImgBackground );	
		}
	public void setEnableImgBackground( boolean enableImgBackground )
		{	this.enableImgBackground = enableImgBackground;	}

	public VImage getBkgImage()
		{	return( this.bkgImg );	}


	/** Facilitates the Increment/Decrement the selectedIndex,
	 *  with an optional wrap.  Use in doControls.
	 * 
	 * @param delta    integer amount to move the selectedIndex.
	 * @param wrap    true to wrap the new value, false to bound it. 
	 * @return	The resulting selectedIndex value
	 */
	protected int moveSelection(int delta, boolean wrap )
		{
		int max = this.content.size();
		this.selectedIndex += delta;
		if( wrap == true )
			{
			while( this.selectedIndex < 0 )
				{ this.selectedIndex += max;  }
			while( this.selectedIndex > max-1 )
				{ this.selectedIndex -= max;  }			
			}
		else
			{
			if( this.selectedIndex < 0 )
				{ this.selectedIndex = 0;  }
			if( this.selectedIndex > max-1 )
				{ this.selectedIndex = (max - 1);  }			
			}
		return(this.selectedIndex);
		}

	protected boolean isSelectionActive()
		{
		return( this.content.get(this.selectedIndex).isActive() );
		}
	
	public Vmenuitem getSelectedMenuItem()
		{
		return( this.content.get(this.selectedIndex) );
		}


	}
