package menus;

import static core.Script.setMenuFocus;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;

import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiButton.enumMenuButtonCOLORS;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import core.Controls;
import domain.VImage;
import domain.VSound;

public class VmenuButtonPalette implements Vmenu
	{
	
	private int x = 0,y = 0;
	private int w = 0, h = 0;
	private int selectedIndex = -1;
	
	private boolean isActive = false;
	private boolean isVisible = false;
	private boolean enableCaption = false;
	private boolean enableHelpBar = false;
	private boolean isImgBackground = false;
	private boolean isCircularButtons = false;
	private boolean enableWrap = true;
	private boolean autoCenter = true;
	

	private Integer row = 3;
	private Integer col = 3;
	
	private Integer capacity;
	
	// store from recalculate()
	private Double xCellDiff = 0.0d;
	private Double yCellDiff = 0.0d;

	// Defines the padding space between buttons.
	private boolean isSolidPadding = true;
	private Integer pad = 3;
	private Color paddingClr = core.Script.Color_DEATH_MAGENTA;

	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

	private VmiTextSimple caption;
	private VmiTextSimple helpBar;

	private VImage bkgImg = null;

	private HashMap<Integer,Vmenuitem> hmButtons = 
			new HashMap<Integer,Vmenuitem>();
	private HashMap<Integer,Integer> hmHotkeyMap = 
			new HashMap<Integer,Integer>();	
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();



	/** ----------------------  Construction --------------------------   */

	/**   Verbose Constructor with default buttons allocated to fill.
	 * 
	 * @param x0	The X-pixel anchor position of the menu
	 * @param y0	The Y-pixel anchor position of the menu
	 * @param widthPx	width allocated to the menu, contents adapt to it.
	 * @param hgtPx		height allocated to the menu, contents adapt to it.
	 * @param numCols		Vertical Columns available.
	 * @param numRows		Horizontal Columns available.
	 * @param hm	HashMap<Integer,Vmenuitem> pre-defined content
	 *   only Vmenuitems of tyhpe VmiButton will be accepted. rest nulled 
	 */
	public VmenuButtonPalette(int x0, int y0, int widthPx, int hgtPx,
			int numCols, int numRows, HashMap<Integer,Vmenuitem> hm )
		{
		this.x = x0;
		this.y = y0;
		this.w = widthPx;
		this.h = hgtPx;
		this.pad = 3;
		this.col = numCols;
		this.row = numRows;
		
		this.hmButtons = hm;
		for( Integer n : this.hmButtons.keySet() )	// Check em.
			{
				// someone trying to insert something besides a VmiButton. 
				// Stop them.
			if( ! this.hmButtons.get(n).getClass().isInstance( 
					VmiButton.class ) )
				{
				System.err.print("Incompatible Vmenuitem sent to button palette!");
				this.hmButtons.put(n,new VmiButton() );
				this.hmButtons.get(n).setState(
					enumMenuItemSTATE.DISABLED.value());
				}
			}
		this.hmSounds = new HashMap<enumMenuEVENT,VSound>();
		
		this.selectedIndex = -1;
		this.focusID = Vmenu.getRandomID();
		
		this.caption = new VmiTextSimple(" Button Palette ");
		this.helpBar = new VmiTextSimple(" ");
		
		this.isSolidPadding = true;
		this.paddingClr = core.Script.Color_DEATH_MAGENTA;
		
		this.isActive = true;
		this.isVisible = true;
		this.enableCaption = false;
		this.setEnableHelpBar(false);
		this.isImgBackground = false;
		this.setCircularButtons(false);
		
		this.bkgImg = new VImage( this.w,this.h,
				core.Script.Color_DEATH_MAGENTA);

		this.recalculate();
		this.selectCenter();
		return;
		}
	
	/**   Verbose Constructor with default buttons allocated to fill.
	 * 
	 * @param x0	The X-pixel anchor position of the menu
	 * @param y0	The Y-pixel anchor position of the menu
	 * @param widthPx	width allocated to the menu, contents adapt to it.
	 * @param hgtPx		height allocated to the menu, contents adapt to it.
	 * @param numCols		Vertical Columns available.
	 * @param numRows		Horizontal Columns available.
	 */
	public VmenuButtonPalette(int x0, int y0, int widthPx, int hgtPx,
			int numCols, int numRows )
		{
		this.x = x0;
		this.y = y0;
		this.w = widthPx;
		this.h = hgtPx;
		this.pad = 3;
		this.col = numCols;
		this.row = numRows;
		
		// Ensure minimal real estate 5x5 button space.
		if( this.w < this.col * 5 )
			{ this.w = this.col * 5; }
		if( this.h < this.row * 5 )
			{ this.h = this.row * 5; }

		for( int bn = 0; bn < (this.col*this.row); bn++ )
			{
			this.hmButtons.put(bn, new VmiButton(  ) );
			}
		
		this.hmSounds = new HashMap<enumMenuEVENT,VSound>();
		
		this.selectedIndex = -1;
		this.focusID = Vmenu.getRandomID();
		
		this.caption = new VmiTextSimple(" Button Palette ");
		this.helpBar = new VmiTextSimple(" ");

		this.isSolidPadding = true;
		this.paddingClr = core.Script.Color_DEATH_MAGENTA;
		
		this.isActive = true;
		this.isVisible = true;
		this.enableCaption = false;
		this.setEnableHelpBar(false);
		this.isImgBackground = false;
		this.setCircularButtons(false);
		
		this.bkgImg = new VImage( this.w,this.h,
				core.Script.Color_DEATH_MAGENTA);

		this.recalculate();
		this.selectCenter();
		this.makeCenterGoParent();
		return;
		}

	
	/** --------------- Interface Methods  --------------------------   */	
	
	public boolean paint(VImage target)
		{
		// System.out.println("VerticalMenu draw called.");
		if (this.isVisible == false)
			{	return( false );  }
		if (target == null)
			{	return( false );	}
		
		this.resolveStates();
		
		if (this.isImgBackground == true && this.bkgImg != null)
			{
			target.scaleblit(this.x, this.y, this.w, this.h, this.bkgImg);
			this.helpBar.enableIcons( false );
			this.helpBar.enableFrame( false );
			this.helpBar.enableBackdrop( false );
			this.caption.enableIcons( false );
			this.caption.enableFrame( false );
			this.caption.enableBackdrop( false );
			this.caption.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
					core.Script.Color_DEATH_MAGENTA);
			this.helpBar.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
					core.Script.Color_DEATH_MAGENTA);
			}
		else
			{
			this.helpBar.enableIcons( false );
			this.helpBar.enableFrame( true );
			this.helpBar.enableBackdrop( true );
			this.caption.enableIcons( false );
			this.caption.enableFrame( true );
			this.caption.enableBackdrop( true );
			this.caption.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
					Color.BLACK );
			this.helpBar.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
					Color.BLACK );
			}
		
		if( this.enableCaption )
			{
			this.caption.paint(target);
			}
		
		if( this.isEnableHelpBar() == true )
			{
			this.helpBar.paint(target);
			}
		
		// now paints the menu items
		for( Integer n : this.hmButtons.keySet() )
			{
			Vmenuitem myvmi = this.hmButtons.get(n);
			if (myvmi.isVisible() == false)
				{	continue;		}
			myvmi.paint( target );
			}
		
		// and the frame, if set.
		int yI = this.y;
		int yH = this.y + this.h;
		if( this.enableCaption == true )
			{ 
			yI += this.caption.getDY().intValue();
			yH -=  this.caption.getDY().intValue();
			}
		if( this.enableHelpBar )
			{
			yH -=  this.helpBar.getDY().intValue();
			}

		Double tmp;
		if( this.isSolidPadding == true )
			{
			for( int t = 0; t < pad; t++ )
				{
				for( int c = 0; c <= this.col; c++ )
					{
					tmp = new Double(c) * this.xCellDiff + 0.5d +
							new Double(c * this.pad);

					target.lineTrans( this.x+tmp.intValue()+t, yI, 
						this.x+tmp.intValue()+t, yH-1, 
						this.paddingClr );
					}
				for( int r = 0; r <= this.row; r++ )
					{
					tmp = new Double(r) * this.yCellDiff + 0.5d + 
							new Double(r * this.pad);

					target.lineTrans( this.x, yI+tmp.intValue()+t,  
						this.x+this.w-1, yI+tmp.intValue()+t, 
						this.paddingClr );	
					}
				}
			}		// End border frame.
		
//		System.out.println("painted button palette at Select index = "+
//				Integer.toString(this.selectedIndex) );
		
		return(true);
		}

	public boolean reposition(int posX, int posY, 
			int relPosX, int relPosY)
		{
		this.x = posX+relPosX;
		this.y = posY+relPosY;
		this.recalculate();
		return false;
		}


	private void recalculate()
		{
		if( this.x < 0 ) 	{ this.x = 0; }
		if( this.y < 0 ) 	{ this.y = 0; }
		if( this.w < 1 )	{ this.w = 1; }
		if( this.h < 1 )	{ this.h = 1; }
		if( this.row < 2 )	{ this.row = 2; }
		if( this.col < 2 )	{ this.col = 2; }

		this.capacity  = this.row * this.col;

		int ySpace = this.h;		// pixel space actually available
		int x0 = this.x;
		int y0 = this.y;
		if( this.enableCaption == true )
			{ 
			ySpace -= this.caption.getDY();
			y0 += this.caption.getDY();
			this.caption.reposition(this.x, this.y, 0, 0);
			this.caption.setExtendX(this.w, true);
			}
		if( this.isEnableHelpBar() == true )
			{ 
			ySpace -= this.helpBar.getDY();
			this.helpBar.reposition(this.x, 
					this.y + this.h - this.helpBar.getDY().intValue(), 0, 0);
			this.helpBar.setExtendX( this.w, true );			
			}

		// QC..  and inherit hotkey codes from children.
		for( Integer n : this.hmButtons.keySet() )
			{
			if( n > this.capacity )  { continue; }
			// Bad button got in palette... fix it.
			if( this.hmButtons.get(n) == null )
				{
				this.hmButtons.put(n, new VmiButton() );
				this.hmButtons.get(n).setState(
						enumMenuItemSTATE.DISABLED.value() );
				}
			Integer tmp01 = this.hmButtons.get(n).getKeycode();
			if( tmp01 != null )
				{ this.hmHotkeyMap.put(n, tmp01 ); }
			}

		Double dx = new Double( this.w-(this.pad*(this.col+1))) / 
			this.col.doubleValue();
		Double dy = new Double( (ySpace-(this.pad*(this.row+1)))) /
				this.row.doubleValue();

		this.xCellDiff = dx;
		this.yCellDiff = dy;
		
		for( int by = 0; by < this.row; by++ )
			{
			for( int bx = 0; bx < this.col; bx++ )	
				{
				Integer bn = (by*this.col) + bx;
				Integer xDiff = 
					new Double( dx * new Double( bx )+0.5d ).intValue();
				Integer yDiff = 
					new Double( dy * new Double( by )+0.5d ).intValue();

				VmiButton vmi = (VmiButton) this.hmButtons.get(bn);
				if( vmi.isActive() == true )
					{
					vmi.resize( dx.intValue(), dy.intValue() );
					vmi.reposition( x0, y0, xDiff.intValue()+((bx+1)*this.pad), 
						yDiff.intValue() + ((by+1)*this.pad) );
					}
				}
			}

		if( this.isEnableHelpBar() == true )
			{
			this.helpBar.setText( 
					this.hmButtons.get(this.selectedIndex).getTip()[0] );
			}
		
		return;
		}


	public boolean doControls(Integer kc )
		{
		boolean redraw = false;
		
		if (kc <= -1) // fake keystroke. cause redraw
			{
			this.refresh();
			return( true );
			}
		
		Integer basecode = Controls.extcodeGetBasecode(kc);
		boolean isShift = Controls.extcodeGetSHIFT(kc);
		boolean isCntl = Controls.extcodeGetCNTL(kc);
			// The selected button.  This simplifies code alot.
		Vmenuitem sltd = this.hmButtons.get(this.selectedIndex);
		
		switch( basecode )
			{
			case 8: // BACKSPACE <CANCEL>
				sltd.setState(enumMenuItemSTATE.NORMAL.value() );
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				redraw = true;
				this.returnToParent();
				return( true );
			case 10: // ENTER KEY <CONFIRM>
			case 32: // SPACE BAR
				if( isCntl == true )   { break; } 
				this.funcActivate();
				if( this.autoCenter == true )
					{	this.selectCenter();	}
				redraw = true;
				break;
			case 33: 		// Page UP
				this.selectedIndex = this.col-1;
				redraw = true;
				break;
			case 34:		//home/end
				this.selectedIndex = this.capacity - 1;
				redraw = true;
				break;
			case 35:		// page Down
				this.selectedIndex = (this.row-1)*this.col;
				redraw = true;
				break;
			case 36:
				this.selectedIndex = 0;
				redraw = true;
				break;
			case 127:	// Delete
				this.selectCenter();
				redraw = true;
				break;
			case 37: 		// ARROW-LEFT, move minus horizontal
				if( isCntl == true || isShift == true  )   
					{ break; }				
				this.moveCol( -1 );
				redraw = true;
				break;
			case 38: // ARROW-UP
				if( isCntl == true || isShift == true  )   
					{ break; }
				redraw = true;
				this.moveRow( -1 );
				this.playMenuSound(enumMenuEVENT.MOVE, 33);				
				break;
			case 39: // ARROW-RIGHT
				if( isCntl == true || isShift == true  )   
					{ break; }
				this.moveCol( 1 );
				redraw = true;
				break;
			case 40: // ARROW-DOWN
				if( isCntl == true || isShift == true  )   
					{ break; }
				redraw = true;				
				this.moveRow( 1 );
				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;
			default:
				System.out.println(" VmenuButtonPalette: " +
					"unhandled menu keystroke ["
					+ kc.toString() + " ]  Base <"
					+ basecode.toString() + "> ");
				break;
			}

//		System.out.println(Integer.toString( this.selectedIndex ));
		
		// Inactive item ward:  prevent Inf-loop
		if( this.getActiveItemCount() != 0 && this.selectedIndex >= 0 ) 
			{
			while( this.hmButtons.get(
					this.selectedIndex).isActive() == false )
				{
				this.selectedIndex--;
				if (this.selectedIndex < 0)
					{
					this.selectedIndex = this.capacity - 1;
					}
				}
			}

		if (redraw)
			{
			this.resolveStates();
			this.recalculate();
			}

		return(redraw);
		}
	
	/**
	 * This is run when the ENTER Key is used. Invokes selected menuitem
	 */
	private void funcActivate()
		{
		// So Cannot activate greyed out items.
		if (this.hmButtons.get(this.selectedIndex).getState() == 
				enumMenuItemSTATE.DISABLED.value())
			{	return;  } 
		this.hmButtons.get( this.selectedIndex ).setState(
				enumMenuItemSTATE.ACTIVATED.value() );
		// Time for action! Does whatever Method the menuitem is set
		if (this.hmButtons.get( this.selectedIndex).doAction() )
			{	this.playMenuSound(enumMenuEVENT.CONFIRM, 33);	}
		return;
		}

	public void moveAbs(int x, int y)
		{
		this.x = x;
		this.y = y;
		this.recalculate();
		return;
		}

	public void moveRel(int x, int y)
		{
		this.x = this.x + x;
		this.y = this.y + y;
		this.recalculate();
		return;
		}

	public Integer countMenuItems()
		{	return( this.hmButtons.size() );	}

	public Integer addItem( Vmenuitem vmi)
		{
		Integer startat = this.countMenuItems();  
		if( startat >= this.capacity )
			{ return( startat ); }

		// Only buttons can be added to this particular menu.
		if( ! vmi.getClass().isInstance( VmiButton.class ) )
			{ return( startat ); }
		
		// Added first item.   start the index there.
		if (this.hmButtons.isEmpty())
			{
			this.selectedIndex = 0;
			}
		
		this.hmButtons.put( new Integer(startat+1), vmi );
		return( this.hmButtons.size() );
		}

	public Vmenuitem popItem()
		{
		return( this.hmButtons.remove( this.hmButtons.size() - 1 ) );
		}

	public Vmenuitem removeItem(int index)
		{
		return( this.hmButtons.remove(index) );
		}

	public Integer insertItem(Vmenuitem vmi, int index)
		{
		if( ! vmi.getClass().isInstance( VmiButton.class ) )
			{ return( new Integer(this.hmButtons.size()) ); } 
		this.hmButtons.put( new Integer(index), vmi );
		return( new Integer(this.hmButtons.size()) );
		}

	public void refresh()
		{
		this.recalculate();
		this.resolveStates();
		return;
		}


	private void resolveStates()
		{
		if( this.selectedIndex == -1 )
			{ return; }
		
		for( Integer n : this.hmButtons.keySet() )
			{
			Vmenuitem myvmi = this.hmButtons.get(n);

			if( myvmi.isActive() == false )
				{	continue;	}
			// Select the active item .. long as its not activated or disabled.
			if( n == this.selectedIndex && 
				myvmi.getState() != 2 && 
				myvmi.getState() != 3 )
				{
				myvmi.setState( enumMenuItemSTATE.SELECTED.value());
				}
			else
				{
				myvmi.setState( enumMenuItemSTATE.NORMAL.value());
				}
			}

		return;
		}

	public Vmenuitem getMenuItem(int index)
		{
		return(this.hmButtons.get(index));
		}
	
	public Vmenuitem getMenuItemSelected()
		{
		if( this.selectedIndex < 0 )	{ return(null); }
		if( this.selectedIndex >= this.hmButtons.size() )  { return(null); }
		return( this.hmButtons.get(this.selectedIndex) );
		}

	public Vmenuitem getMenuItemByID(Long id)
		{
		for( Integer n : this.hmButtons.keySet() )
			{
			if(  this.hmButtons.get(n).getId() == id)
				{
				return( this.hmButtons.get(n) );
				}
			}
		return null;
		}

	public Integer getSelectedIndex()
		{
		return(this.selectedIndex);
		}

	public int getSelectedIndexPosX()
		{
		return( this.hmButtons.get(this.selectedIndex).getX().intValue() );
		}

	public int getSelectedIndexPosY()
		{
		return( this.hmButtons.get( this.selectedIndex ).getY().intValue() );
		}

	public void setSelectedIndex(Integer index)
		{
		this.selectedIndex = index.intValue();
		return;
		}
	public void setSelectedIndex( int index)
		{
		this.selectedIndex = index;
		return;
		}

	public void setFocusId(Long id)
		{
		this.focusID = id;
		return;
		}

	public Long getFocusId()
		{
		return( this.focusID );
		}

	public boolean isFocus(Long id)
		{
		if (id == this.focusID)
			{	return (true);	  }
		return (false);
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

	public void setBackgroundImage( VImage bkg, boolean onOff )
		{
		this.bkgImg = bkg;
		this.isImgBackground = onOff;
		return;
		}

	public boolean isActive()
		{
		return(this.isActive);
		}

	public boolean isVisible()
		{
		return(this.isVisible);
		}

	public void activateSelected()
		{
		this.hmButtons.get( this.selectedIndex ).doAction();
		this.playMenuSound(enumMenuEVENT.CONFIRM, 33 );
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

	// buttons ARE icons... cannot turn them off.
	public void setIconsAll(boolean onOff)
		{
		return;
		}

	public void setBorderAll( boolean onOff, int thick )
		{
		for( Integer myvmi : this.hmButtons.keySet() )
			{
			this.hmButtons.get(myvmi).enableFrame(onOff);
			this.hmButtons.get(myvmi).setFrameThicknessPx(thick);
			}
		return;
		}

	public void setParentID( Long id, boolean recurse )
		{
		this.parentID = id;
		if( recurse == true )
			{
			for( Integer n : this.hmButtons.keySet() )
				{
				this.hmButtons.get(n).setParentID(id);
				}
			}
		return;
		}

	public void setChildID( Long id, boolean recurse )
		{
		this.childID = id;
		if( recurse == true )
			{
			for( Integer n : this.hmButtons.keySet() )
				{
				this.hmButtons.get(n).setChildID(id);
				}
			}
		return;
		}


	/** ----------------- Non-interface methods -------------------------  */
	
	public int getX()
		{	return(  this.x );	}
	public int getY()
		{	return( this.y );	}
	public int getWidth()
		{ return(this.w);   }
	public int getHeight()
		{ return(this.h);   }


	public boolean isCircularButtons()
		{	return(this.isCircularButtons);	}
	public void setCircularButtons(boolean isCircularButtons)
		{	this.isCircularButtons = isCircularButtons;	}
	
	/**  Define the padding specifications.   Size of padding between button.
	 * drawing can be turned off completely , or recolored
	 * 
	 * @param widthPx		Pixel size of padding between button space.
	 * @param solid			Is the borders visible
	 * @param solidColor		If visible, set the Color.
	 */
	public void setPadding(int widthPx, boolean solid, Color solidColor ) 
		{
		this.pad = widthPx;
		this.isSolidPadding = solid;
		this.paddingClr = solidColor;
		return;
		}

	public int getPaddingWidthPx()
		{ return(this.pad); }
	public boolean isSolidPadding()
		{ return(this.isSolidPadding); }
	public Color getPaddingColor()
		{ return(this.paddingClr); }


	/** The caption is an auxilary Vmenuitem used for the Menus Title.
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
		newCaption.setColor(enumMenuStxtCOLORS.BKG_ACTIVE, 
				Color.BLACK);
		this.caption = newCaption;
		return;
		}
	
	/**  Directly replaces the caption box. 
	 * Dimensions will be overridden.
	 * */
	public void setCaption( VmiTextSimple theCaption )
		{
		this.caption = theCaption;
		this.recalculate();
		return;
		}

	public void setCaptionVisible(boolean show)
		{
		this.enableCaption = show;
		return;
		}

	public boolean isCaptionEnabled()
		{
		return (this.enableCaption);
		}

	private int getActiveItemCount()
		{
		int rslt = 0;
		for( Integer vmi : this.hmButtons.keySet() )
			{
			if( this.hmButtons.get(vmi).isActive() == true )
				{	rslt++;	}
			}
		return (rslt);
		}


	/** is the help-text bar shown? */
	public boolean isEnableHelpBar()
		{	return( this.enableHelpBar );	}
	/** Set if the help text for the buttons is visible. */
	public void setEnableHelpBar( boolean enableHelpBar )
		{	this.enableHelpBar = enableHelpBar;	}

	private void returnToParent()
		{
//		System.out.println(" -- returning to parent -- ");
		if( this.parentID < 0 ) { return; }
		setMenuFocus( 0, this.parentID );
		return;
		}
	
	/** Centers the selected Index onto the center of the palette  */
	private void selectCenter()
		{
		int rowC = this.row / 2;
		int colC = this.col / 2;
		this.selectedIndex = (rowC*this.col) + colC;
		}

	/** Sets the action of the "center" button to return to the 
	 * parent menu object.  Commonly desired behavior. */
	public void makeCenterGoParent()
		{
		if( this.parentID == -1 )
			{ return; }
		int rowC = this.row / 2;
		int colC = this.col / 2;
		int center = (rowC*this.col) + colC;
		Method m = core.Script.getFunction( Vmenuitem.class, 
				"goParent");
		this.hmButtons.get( center ).setParentID(
				this.parentID );
		this.hmButtons.get( center ).setAction(m);
		this.hmButtons.get( center ).setTip("Return");
//		System.out.println( " Center Parent ID : " +  
//			this.hmButtons.get( (rowC*this.col) + colC ).getParentID().toString() );
		return;
		}

	public boolean isAllowWrap()
		{	return enableWrap;	}
	/** When true, cursor can wrap around the edges of the palette */
	public void setAllowWrap(boolean allowWrap)
		{	this.enableWrap = allowWrap;	}

	public boolean isAutoCenter()
		{	return autoCenter;	}
	/** When set to true, cursor will return to center button  
	 * upon activation of any item in the palette */
	public void setAutoCenter(boolean autoCenter)
		{	this.autoCenter = autoCenter;	}
	
	/** Set a color component of all buttons in the palette  */
	public void setColorComponentAll(enumMenuButtonCOLORS e, Color clr)
		{
		for( Integer n : this.hmButtons.keySet() )
			{
			VmiButton vmi = (VmiButton) this.hmButtons.get(n);
			vmi.setColorComponent(e, clr);
			}
		return;
		}
	
	/** Set complete color definition of all buttons in the palette  */
	public void setColorAll( HashMap<Integer,Color> hm )
		{
		for( Integer n : this.hmButtons.keySet() )
			{	this.hmButtons.get(n).setColorContent(hm);	}
		return;
		}

	public void setAction(int cellX, int cellY, Method function )
		{
		this.hmButtons.get( cellY*this.col + cellX ).setAction(function);
		return;
		}
	public void setAction(int cellX, int cellY, Method function, 
			String helpCaption )
		{
		this.hmButtons.get( cellY*this.col + cellX ).setAction(function);
		this.hmButtons.get( cellY*this.col + cellX ).setTip(helpCaption);
		return;
		}
	public void setAction(int cellX, int cellY, Method function, 
			String helpCaption, String longDescription )
		{
		String[] tmp = new String[2];
		tmp[0] = helpCaption;
		tmp[1] = longDescription;
		this.hmButtons.get( cellY*this.col + cellX ).setAction(function);
		this.hmButtons.get( cellY*this.col + cellX ).setTip( tmp );
		return;
		}
	public void setAction(int index, Method function )
		{
		this.hmButtons.get( index ).setAction(function);
		return;
		}
	public void setAction(int index, Method function,
		String helpCaption, String longDescription )
		{
		String[] tmp = new String[2];
		tmp[0] = helpCaption;
		tmp[1] = longDescription;
		this.hmButtons.get( index ).setAction(function);
		this.hmButtons.get( index ).setTip( tmp );
		return;		
		}

	public Long getParentID()
		{	return( this.parentID );   }
	public void setParentID(Long pID)
		{	this.parentID = pID;	  }
	public Long getChildID()
		{	return( this.childID );   }
	public void setChildID(Long childID)
		{	this.childID = childID;	  }
	
	/**  Directly replaces the caption box. 
	 * Dimensions will be overridden.
	 * */
	public void setTip( VmiTextSimple theTip )
		{
		this.helpBar = theTip;
		this.recalculate();
		return;
		}
	public void setTip( int index, String buttonTip )
		{
		String[] s0 = new String[1];
		s0[0] = buttonTip;
		this.hmButtons.get( index ).setTip( s0 );
		this.recalculate();
		return;
		}
	public void setTip( int index, String[] buttonTip )
		{
		this.hmButtons.get( index ).setTip( buttonTip );
		this.recalculate();
		return;
		}
	public void setTip( int cellX, int cellY, String buttonTip )
		{
		String[] s0 = new String[1];
		s0[0] = buttonTip;
		if( cellX >= this.col )		{ cellX = this.col - 1; }
		if( cellY >= this.row )		{ cellY = this.row - 1; }
		this.hmButtons.get( cellY*this.col + cellX ).setTip( s0 );
		this.recalculate();
		return;
		}
	public void setTip( int cellX, int cellY, String[] buttonTip )
		{
		if( cellX >= this.col )		{ cellX = this.col - 1; }
		if( cellY >= this.row )		{ cellY = this.row - 1; }
		this.hmButtons.get( cellY*this.col + cellX ).setTip( buttonTip );
		this.recalculate();
		return;
		}
	
	
	//  These two are used to move the cursor around the palette.
	private void moveCol( int move )
		{
		int current = this.selectedIndex % this.col;
		int base = this.selectedIndex - current;
		current += move;
		if( this.enableWrap == true )
			{  
			while( current >= this.col )
				{	current -= this.col; 	}
			if( current < 0 )
				{ current +=  this.col;  }
			}
		else		// un-wrapped. 
			{
			if( current >= this.col )
				{  current = this.col - 1; }
			if( current < 0 )
				{  current = 0; }
			}
		this.selectedIndex = base + current;
		return;
		}
	
	private void moveRow( int move )
		{
		int current = this.selectedIndex / this.col;
		int mycol = this.selectedIndex % this.col;
		current += move;
		if( this.enableWrap == true )
			{  
			while( current >= this.row )
				{	current -= this.row; 	}
			if( current < 0 )
				{ current +=  this.row;  }
			}
		else		// un-wrapped. 
			{
			if( current >= this.row )
				{  current = this.row - 1; }
			if( current < 0 )
				{  current = 0; }
			}
		this.selectedIndex =  (current * this.col) + mycol;
		return;
		}

	public void setColorContentAll( 
			HashMap<Integer,Color> newColors )
		{
		for( Integer n : this.hmButtons.keySet() )
			{	this.hmButtons.get(n).setColorContent(newColors);	}
		return;
		}


	
	}
