package menus;

import static core.Script.setMenuFocus;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;

import core.Controls;
import domain.VImage;
import domain.VImageGradient;
import domain.VSound;

/**  A Vmenu Class that contains a series of manipulatable guages.
 *   Use when you need to take multiple bounded numbers for input.
 *   Good for a configuration settings screen.
 *        Notes:
 *   Can add both Integer and Decimal VmiGuage* objects.
 *   It can also be used only for show by disabling input.
 *   A status bar displays the graphical information in text form.
 *   Various keystrokes are used to adjust the guage in values by an internal 
 *      "quantum unit" which is typically 1, 10, 25% of the value range..
 *   It is bounded to a fixed width, as are the component guages.
 *      It will consume as much vertical space as needed.  Thus there is no 
 *      specified height.
 *   Component input boxes are visibly hidden objects.
 * (Apr.2016)
 * 
 * @author Krybo
 *
 */

public class VmenuSliders implements Vmenu
	{
	private int x;		// X anchor
	private int y;		// Y anchor
	private int w;		// Pixel Width allocation
	
	private int calcH;

	private Integer selectedIndex;
	private Integer items;
	private Integer guageHeight = 10;
	private Integer borderWidth = 2;
	private Integer paddingWidthPx = 2;
	private Integer iconPaddingPx = 16;

	private boolean isEnableInput = false;
	private boolean isEnableStatusBar = false;
	private boolean useIcons = false;
	private boolean isBkgImg = false;
	private boolean isVisible = true;
	private boolean isActive = true;

	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

	// For incremental guage movement.
	private int quantaIndex = 6;
	private final Double[] quanta = { 0.01d, 0.02d, 0.33333d, 0.04d, 
			0.05d,0.08d, 0.10d, 0.125d, 0.20d, 0.25d, 0.50d };

	// Displays string representation of the selected guage.
	private VmiTextSimple statusBar = null;

	private HashMap<Integer,VmiGuageDecimal> hmGDec = 
			new HashMap<Integer,VmiGuageDecimal>();
	private HashMap<Integer,VmiGuageInt> hmGInt = 
			new HashMap<Integer,VmiGuageInt>();
	private HashMap<Integer,VmiInputInteger> hmInInt = 
			new HashMap<Integer,VmiInputInteger>();
	private HashMap<Integer,VmiInputDecimal> hmInDec = 
			new HashMap<Integer,VmiInputDecimal>();
	// true = that item is a decimal guage.   false = integer guage.
	private HashMap<Integer,Boolean> hmType = 
			new HashMap<Integer,Boolean>();
	private HashMap<Integer,VImage> hmIcons =
			new HashMap<Integer,VImage>();
	private HashMap<enumMenuEVENT,VSound> hmSounds =
			new HashMap<enumMenuEVENT,VSound>();

	private VImage bkgImg;
	private Color clrBack;
	private Color clrFore;
	private Color highlighter = new Color(1.0f, 1.0f, 1.0f, 0.36f );

	public VmenuSliders(int anchorX, int anchorY, int widthPx, 
			int guageCount, boolean isDecimalGuage, boolean useInputs,
			boolean useStatusBar )
		{
		this.x = anchorX;
		this.y = anchorY;
		this.w = widthPx;
		
		this.isBkgImg = false;
		this.isVisible = true;
		this.isActive = true;
		
		this.selectedIndex = -1;
		this.setIconPaddingPx(16);
		this.items = new Integer(-1);
		this.isEnableInput = false;
		this.useIcons = false;
		this.guageHeight = new Integer(10);
		this.borderWidth = new Integer(2);
		this.focusID = Vmenu.getRandomID();
		this.clrFore = Color.WHITE;
		this.clrBack  = Color.BLACK;

		for( int n = 0; n < guageCount; n++ )
			{
			if( isDecimalGuage == true )
				{	this.addDecimalGuage( useInputs );	}
			else
				{	this.addIntegerGuage( useInputs );	}
			}

		this.isEnableInput = useInputs;
		this.isEnableStatusBar = useStatusBar;
		if( useStatusBar == true )
			{
			this.statusBar = new VmiTextSimple("INIT");
			this.statusBar.setExtendX( this.w, false );
			}

		this.refresh();
		return;
		}

	/** adds a default gauge, Integer values 0 to 100. */
	public Integer addIntegerGuage( boolean hasInput )
		{
		if( this.items < 0 )		{ this.items = 0; }
		// Width of guages cannot be changed, so get it right here.
		Double tWidth = new Double(this.w);
		tWidth = tWidth - (this.paddingWidthPx*3) - this.iconPaddingPx -
				(this.borderWidth*2);
		VmiGuageInt vmi = new VmiGuageInt( 0, 0, 
			tWidth.intValue(), this.guageHeight, 0, 100, 0 );
		this.hmGInt.put( this.items, vmi );

		if( hasInput == true )
			{
//			System.out.println("VMS: Added int input");
			VmiInputInteger vi = new VmiInputInteger("X", 
					"Change Integer Value", 0, 0 );
			this.hmInInt.put( this.items, vi );
			}

		this.hmType.put( this.items, false );
		this.items++;
		if( this.items == 1 )	{ this.selectedIndex = 0; }
		return( this.items );
		}

	/** adds a default gauge, Double values 0 to 100. */
	public Integer addDecimalGuage( boolean hasInput )
		{
		if( this.items < 0 )		{ this.items = 0; }
		// Width of guages cannot be changed, so get it right here.
		Double tWidth = new Double( this.w );
		tWidth = tWidth - (this.paddingWidthPx*3) - this.iconPaddingPx - 
				(this.borderWidth*2);;
		VmiGuageDecimal vmi = new VmiGuageDecimal( 0, 0, 
			tWidth.intValue(), this.guageHeight, 0.0d, 100.0d, 0.0d );
		this.hmGDec.put( this.items, vmi );

		if( hasInput == true )
			{
//			System.out.println("VMS: Added decimal input");
			VmiInputDecimal vd = new VmiInputDecimal("X", 
					"Change Integer Value", 0, 0 );
			this.hmInDec.put( this.items, vd );
			}

		this.hmType.put( this.items, true );
		this.items++;
		if( this.items == 1 )	{ this.selectedIndex = 0; }
		return( this.items );
		}


	public boolean paint( VImage target )
		{
		if( target == null )			{ return(false); }
		if( this.isVisible == false )	{ return(false); }
		this.refresh();

		// The background.
		if( this.isBkgImg == true   &&   this.bkgImg != null )
			{
			target.scaleblit(this.x, this.y, this.w, this.calcH, this.bkgImg);
			}
		else
			{
			target.rectfill(this.x, this.y, this.x+this.w, 
					this.y+this.calcH, clrBack);			
			}

		int c = -1;
		for( Integer n : this.hmType.keySet() )
			{
			c++;
			// ignore improper indexed or hidden guages
			if( n < 0 )  { continue; }		
			// tmp X and Y relative to anchor position.
			int tmpX = this.paddingWidthPx + this.borderWidth; 
			int tmpY = tmpX + c*(this.guageHeight+this.paddingWidthPx);
			// The main guage body.
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).paint(target);	}
			else
				{	this.hmGInt.get(n).paint( target );	}
			// The icon. -- if needed.
			if( this.useIcons == true && this.hmIcons.get(n) != null )
				{
				int tmpicon = this.iconPaddingPx;
				if( this.guageHeight < this.iconPaddingPx )
					{ tmpicon = this.guageHeight; } 
				target.scaleblit( this.x+tmpX, this.y+tmpY,
					tmpicon, tmpicon, this.hmIcons.get(n) );
//						this.iconPaddingPx, this.iconPaddingPx, 
				}
			// Selection highlighter
			if( c == this.selectedIndex )
				{
				target.rectfill( this.x, this.y+tmpY, this.x+this.w, 
						this.y+tmpY+this.guageHeight, 
						this.highlighter );
				}
			}

		// The border.
		for( int a = 0; a < this.borderWidth; a++ )
			{
			target.rect( this.x, this.y, this.x+this.w-a-1, 
					this.y+this.calcH-1-a, 	clrFore );
			}
		
		// The status bar
		if( this.isEnableStatusBar == true )
			{
			this.statusBar.paint(target);
			}
		return(true);
		}

	public boolean doControls(Integer kc )
		{
		boolean redraw = false;
//		sltd.setState(enumMenuItemSTATE.NORMAL.value() );
//		this.playMenuSound(enumMenuEVENT.CANCEL, 33);

		Integer basecode = Controls.extcodeGetBasecode(kc);
		boolean isShift = Controls.extcodeGetSHIFT(kc);
		boolean isCntl = Controls.extcodeGetCNTL(kc);
		
		switch( basecode )
			{
			case 101:
			case 8: // BACKSPACE <CANCEL>
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				redraw = true;
				this.returnToParent();
				return( true );
			case 10: // ENTER KEY <CONFIRM>
			case 32: // SPACE BAR
				if( isCntl == true )   { break; } 
				this.funcActivate();
				redraw = true;
				break;
			case 105:		// NPad9
			case 33: 		// Page UP
				this.selectedIndex = 0;
				redraw = true;
				break;
			case 99:		// NPad 3
			case 34:		// page Down
				this.selectedIndex = this.hmType.size() - 1;
				redraw = true;
				break;
			case 109:		// NPad minus
			case 35:
				if( this.hmType.get(this.selectedIndex) == true )
					{	this.hmGDec.get(this.selectedIndex).setToMin();  }
				else
					{	this.hmGInt.get(this.selectedIndex).setToMin();  }
				redraw = true;
				break;
			case 107:		// NPad plus
			case 36:
				if( this.hmType.get(this.selectedIndex) == true )
					{	this.hmGDec.get(this.selectedIndex).setToMax();  }
				else
					{	this.hmGInt.get(this.selectedIndex).setToMax();  }
				redraw = true;
				break;
			case 104:		// NPad 8
			case 38: 		// ARROW-UP
				if( isShift == true ) { break; }
				if( (isCntl == true)  &&  (this.y > 0) )   
					{ y--; break; }
				redraw = true;
				this.moveRow( -1 );
				this.playMenuSound(enumMenuEVENT.MOVE, 33);				
				break;
			case 98:		// NPad 2
			case 40: 		// ARROW-DOWN
				if( isShift == true ) { break; }
				if( isCntl == true )   
					{ this.y++; break; }
				redraw = true;				
				this.moveRow( +1 );
				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;
			
			case 100:		// NPad 4
			case 37: 		// ARROW-LEFT, move minus horizontal
				redraw = true;
				if( isShift == true )    // shift: decrease by only 1 unit
					{
					if( this.hmType.get(this.selectedIndex) == true )
						{
						this.hmGDec.get(this.selectedIndex).setValueRelative( 
							-1.0d , false );
						}
					else {
						this.hmGInt.get(this.selectedIndex).setValueRelative( 
							-1 , false );
						}
					break;
					}
				if( isCntl == true && this.x > 0 )   
					{ this.x--;  break; }				
				if( this.hmType.get(this.selectedIndex) == true )
					{
					this.hmGDec.get(this.selectedIndex).setValueRelativePercent( 
						(-1)*this.quanta[this.quantaIndex] , false );
					}
				else {
					this.hmGInt.get(this.selectedIndex).setValueRelativePercent( 
							(-1)*this.quanta[this.quantaIndex] , false );
					}
				this.playMenuSound(enumMenuEVENT.DECREMENT, 33);
				break;

			case 102:		// NPad 6
			case 39: 		// ARROW-RIGHT, move minus horizontal
				redraw = true;
				if( isShift == true )		// shift: increase by only 1 unit
					{
					if( this.hmType.get(this.selectedIndex) == true )
						{
						this.hmGDec.get(this.selectedIndex).setValueRelative( 
							+1.0d , false );
						}
					else {
						this.hmGInt.get(this.selectedIndex).setValueRelative( 
							+1 , false );
						}		
					break;
					}
				if( isCntl == true  )   
					{ this.x++;  break; }				
				if( this.hmType.get(this.selectedIndex) == true )
					{
					this.hmGDec.get(this.selectedIndex).setValueRelativePercent( 
						this.quanta[this.quantaIndex] , false );
					}
				else {
					this.hmGInt.get(this.selectedIndex).setValueRelativePercent( 
						this.quanta[this.quantaIndex] , false );
					}
				this.playMenuSound(enumMenuEVENT.INCREMENT, 33);
				break;
			
			case 47:		// foreward slash /
			case 103:		// NPad 7
				redraw = true;
				this.changeQuanta( +1 );
				break;
			case 46:		// period .
			case 97:		// NPad 1
				redraw = true;
				this.changeQuanta( -1 );
				break;
			case 106:		// NPad *
				redraw = true;
				this.setAllGuagesMax();
				break;
			case 111:		// NPad /
				redraw = true;
				this.setAllGuagesMin();
				break;
			case 110:		// NPad dot
				redraw = true;
				this.setAllGuagesToPercentage(0.5d);
				break;

			default:
				System.out.println(" unhandled menu keystroke ["
					+ kc.toString() + " ]  Base <"
					+ basecode.toString() + "> ");
				break;
			}		

		if( redraw == true )
			{  this.updateStatus(); }
		return( redraw );
		}
	

	private void moveRow( int move )
		{
		int current = this.selectedIndex;
		current += move;
//		if( t his.enableWrap == true )
//			{ 
		while( current >= this.hmType.size() )
			{	current -= this.hmType.size(); 	}
		if( current < 0 )
			{ current +=  this.hmType.size();  }
//			}
//		else		// un-wrapped. 
//			{
//			if( current >= this.row )
//				{  current = this.row - 1; }
//			if( current < 0 )
//				{  current = 0; }
//			}
		this.selectedIndex = current;
		return;
		}

	public void moveAbs(int x, int y)
		{
		this.x = x;
		this.y = y;
		this.resolvePositions();
		return;
		}

	public void moveRel(int x, int y)
		{
		this.x = this.x + x;
		this.y = this.y + y;
		if (this.x < 0)
			{	this.x = 0; 	}
		if (this.y < 0)
			{	this.y = 0; 	}
		this.resolvePositions();
		return;
		}



	public Integer countMenuItems()
		{	return(new Integer(this.items));	}

	/** This will only accept Vmenuitems of type
	 * - VmiGuageDecimal 
	 * - VmiGuageInt 
	 * All others will do nothing and return null  */
	public Integer addItem(Vmenuitem vmi)
		{
		if( vmi == null )	{ return(null); }
		if( this.items < 0 ) { this.items = 0; }
		if( vmi.getClass().isInstance( VmiGuageDecimal.class ) )
			{
			this.hmGDec.put( this.items, (VmiGuageDecimal) vmi );
			this.hmType.put( this.items, true );
			this.items++;
			return( new Integer(this.items) );
			}
		
		if( vmi.getClass().isInstance( VmiGuageInt.class ) )
			{
			this.hmGInt.put( this.items, (VmiGuageInt) vmi );
			this.hmType.put( this.items, false );
			
			return( new Integer( this.items ) );			
			}

		return(null);
		}

	/**  Removes the last menuitem in the last and returns it.   */
	public Vmenuitem popItem()
		{
		if( this.items == 0 )	{ return(null); }
		Vmenuitem vmi;
		if( this.hmType.get(this.items-1) == true ) 
			{
			vmi = this.hmGDec.get( this.items-1 );
			this.hmGDec.remove( this.items-1 );
			this.hmType.remove(this.items-1);
			this.items--;
			}
		else
			{
			vmi = this.hmGInt.get( this.items-1 );
			this.hmGInt.remove( this.items-1 );
			this.hmType.remove( this.items-1 );
			this.items--;			
			}
		return(vmi);
		}

	/**  Removes the target menuitem in the last and returns it.   
	 * index is BASE ZERO */
	public Vmenuitem removeItem( int index )
		{
		if( index < 0 ) 			{ return(null); }
		if( index >= this.items )	{ return(null); }
		if( this.hmType.get( index ) == null )
			{ return(null); }
		Vmenuitem vmi;
		if( this.hmType.get( index ) == true ) 
			{
			vmi = this.hmGDec.get( index );
			this.hmGDec.remove( index );
			this.hmType.remove( index );
			this.items--;
			}
		else
			{
			vmi = this.hmGInt.get( index );
			this.hmGInt.remove( index );
			this.hmType.remove( index );
			this.items--;			
			}
		return(vmi);
		}

	/**  Adds or replaces a guage menu-item. */
	public Integer insertItem(Vmenuitem vmi, int index)
		{
		// caution: Might be replaceing a key rather then adding it.
		if( vmi == null )	{ return(null); }
		if( this.items < 0 ) { this.items = 0; }
		boolean rpl = this.hmType.containsKey(index);

		if( vmi.getClass().isInstance( VmiGuageDecimal.class ) )
			{
			this.hmGDec.put( index, (VmiGuageDecimal) vmi );
			this.hmType.put( index, true );
			if( rpl == false )		{ this.items++; }
			return( new Integer(this.items) );
			}
		
		if( vmi.getClass().isInstance( VmiGuageInt.class ) )
			{
			this.hmGInt.put( index, (VmiGuageInt) vmi );
			this.hmType.put( index, false );
			if( rpl == false )		{ this.items++; }
			return( new Integer( this.items ) );			
			}

		return(null);
		}

	public void refresh()
		{
		this.resolveStates();
		this.resolvePositions();
		this.processInput();
		return;
		}


	/** Fetches a guage.   Might be of Integer or Decimal type */
	public Vmenuitem getMenuItem(int index)
		{
		if( this.hmType.get(index) == null )
			{ return(null); }
		if( this.hmType.get(index) == true )
			{	return( this.hmGDec.get(index) );	}		
		return( this.hmGInt.get(index) );
		}
	
	public Vmenuitem getMenuItemSelected()
		{
		if( this.hmType.get(this.selectedIndex) == null )
			{ return(null); }
		if( this.hmType.get(this.selectedIndex) == true )
			{	return( this.hmGDec.get(this.selectedIndex) );	}		
		return( this.hmGInt.get(this.selectedIndex) );
		}

	/** Looks for a menu component with a specified ID, then returns it.
	 *   if its not found, returns null */
	public Vmenuitem getMenuItemByID( Long id )
		{
		 // Make sure that anything with an ID is searched.
		for( Integer is : this.hmType.keySet() )
			{
			if( this.hmType.get(is) == true )
				{
				if( this.hmGDec.get(is).getId() == id )
					{	return( this.hmGDec.get(is) );   }
				if( this.hmInDec.get(is).getId() == id )
					{	return( this.hmInDec.get(is) );   }
				}
			else
				{
				if( this.hmGInt.get(is).getId() == id )
					{	return( this.hmGInt.get(is) );   }
				if( this.hmInInt.get(is).getId() == id )
					{	return( this.hmInInt.get(is) );   }
				}
			}
		return(null);
		}

	public Integer getSelectedIndex()
		{	return(this.selectedIndex);	}

	public int getSelectedIndexPosX()
		{
		if( this.hmType.get(this.selectedIndex) == null )
			{ return(-1); }
		if( this.hmType.get(this.selectedIndex) == true )
			{
			return( this.hmGDec.get(this.selectedIndex).getX().intValue() );
			}
		else
			{
			return( this.hmGInt.get(this.selectedIndex).getX().intValue() );
			}
		}

	public int getSelectedIndexPosY()
		{
		if( this.hmType.get(this.selectedIndex) == null )
			{ return(-1); }
		if( this.hmType.get(this.selectedIndex) == true )
			{
			return( this.hmGDec.get(this.selectedIndex).getY().intValue() );
			}
		else
			{
			return( this.hmGInt.get(this.selectedIndex).getY().intValue() );
			}
		}

	public void setSelectedIndex( Integer index )
		{
		if( index < 0 )	{ return; }
		if( this.hmType.containsKey(index) == false )
			{ return; }
		this.selectedIndex = index;
		return;
		}

	public void setFocusId(Long id)
		{
		this.focusID = id;
		return;
		}

	public Long getFocusId()
		{	return( this.focusID );	}

	public boolean isFocus(Long id)
		{
		if (id == this.focusID )
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
		this.isBkgImg = onOff;
		return;
		}


	public boolean isActive()
		{	return(this.isActive);	}

	public boolean isVisible()
		{	return(this.isVisible); }

	/** If input is on, clicking a guage will open input mode.
	 * If input is off -- will attempt to run the attached method. */
	public void activateSelected()
		{
		if( ! this.hmType.containsKey(this.selectedIndex) )
			{ return; }
		if( this.isEnableInput == true ) 
			{
			if( this.hmType.get(this.selectedIndex) == true )
				{	this.hmInDec.get(this.selectedIndex).doInput();   }
			else
				{	this.hmInInt.get(this.selectedIndex).doInput();   }
			}
		else
			{
			if( this.hmType.get(this.selectedIndex) == true )
				{	this.hmGDec.get(this.selectedIndex).doAction();  }
			else
				{	this.hmGInt.get(this.selectedIndex).doAction();  }
			}

		return;
		}

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

	/** Either display all - or zero - icons */
	public void setIconsAll( boolean onOff)
		{	this.useIcons = onOff;	}

	public void setBorderAll(boolean onOff, int thick)
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).enableFrame( onOff );  }
			else
				{	this.hmGInt.get(n).enableFrame( onOff );  }
			}
		return;
		}

	public void setParentID(Long id, boolean recursive)
		{
		this.parentID = id;
		
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setParentID( id );  }
			else
				{	this.hmGInt.get(n).setParentID( id );  }
			}
		return;
		}

	public void setChildID(Long id, boolean recursive)
		{
		this.childID = id;
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setChildID( id );  }
			else
				{	this.hmGInt.get(n).setChildID( id );  }
			}
		return;
		}


	/** -----------------=  Non-interface methods  =---------------------- */


	public boolean isInputEnabled()
		{ return(this.isEnableInput);  }
	public void setInputEnabled( boolean onOff )
		{ this.isEnableInput = onOff; }

	/** Calculatges and then sets relative positions of all constituant items
	 *    Do not move menu-items anywhere else but here .. to avoid 
	 *    confusion.   Only do positional changes here.   
	 *    If you alter the menu as a whole.. do call this.  */
	private void resolvePositions()
		{
		int num = this.hmType.size();
		this.calcH  = num * (this.guageHeight + this.paddingWidthPx ) +
			this.paddingWidthPx + (this.borderWidth*2);
//		int gWidth = this.w - (this.paddingWidthPx*2) - this.iconPaddingPx;
//		if( this.useDirectInputs == true ) 
//			{  gWidth -= (this.w * 0.8); }

		int c = -1;
		for( Integer n : this.hmType.keySet() )
			{
			c++;
			int baseline = c * (this.guageHeight + this.paddingWidthPx); 
			if( this.hmType.get(n) == true )
				{
				this.hmGDec.get(n).reposition( this.x, this.y, 
					(this.paddingWidthPx*2) + this.borderWidth +
						this.iconPaddingPx, 
					baseline + this.paddingWidthPx+this.borderWidth );
				if( this.isEnableInput == true ) 
					{
					this.hmInDec.get(n).reposition( this.x, this.y, 
						(this.paddingWidthPx*3) + this.borderWidth +
						this.iconPaddingPx + 
						this.hmGDec.get(n).getDX().intValue(), 
						baseline + this.paddingWidthPx+this.borderWidth );
					}
				}
			else
				{
				this.hmGInt.get(n).reposition( this.x, this.y, 
					(this.paddingWidthPx*2)+this.borderWidth + 
						this.iconPaddingPx, 
					baseline + this.paddingWidthPx+this.borderWidth );
				if( this.isEnableInput == true )
					{
					this.hmInInt.get(n).reposition( this.x, this.y, 
						(this.paddingWidthPx*3) + this.borderWidth +
						this.iconPaddingPx + 
						this.hmGInt.get(n).getDX().intValue(), 
						baseline + this.paddingWidthPx+this.borderWidth );
					}
				}
			}
		if( this.isEnableStatusBar == true )
			{	this.statusBar.reposition(this.x, this.y, 0, calcH );	}

		return;
		}

	private void resolveStates()
		{
		// TODO Auto-generated method stub
		return;
		}


	public void setIcon( Integer idx, VImage iconImg )
		{
		this.hmIcons.put(idx,iconImg);
		return;
		}
	public VImage getIcon( Integer idx )
		{  return( this.hmIcons.get(idx) ); }

	public Integer getIconPaddingPx()
		{	return iconPaddingPx;	}

	public void setIconPaddingPx(Integer iconPaddingPx)
		{	this.iconPaddingPx = iconPaddingPx;	}

	public Integer getPaddingWidthPx()
		{	return paddingWidthPx;	}
	public void setPaddingWidthPx(Integer paddingWidthPx)
		{	this.paddingWidthPx = paddingWidthPx;	}

	public Color getColorBackground()
		{	return clrBack;	}
	public void setColorBackground(Color clrBack)
		{	this.clrBack = clrBack;	}

	public Color getHighlighterColor()
		{	return highlighter;	}
	public void setHighlighterColor(Color highlighter)
		{	this.highlighter = highlighter;	}

	private void returnToParent()
		{
		if( this.parentID < 0 ) { return; }
		setMenuFocus( 0, this.parentID );
		return;
		}

	private void funcActivate()
		{
		if( this.selectedIndex < 0 )	{ return; }
		if( this.selectedIndex > this.hmType.size() )     {  return; }
		if( this.isEnableInput == true )
			{
			if( this.hmType.get(this.selectedIndex) == true )
				{	this.hmInDec.get(this.selectedIndex).doInput();  }
			else
				{	this.hmInInt.get(this.selectedIndex).doInput();	}
			}
		else
			{
			if( this.hmType.get(this.selectedIndex) == true )
				{	this.hmInDec.get(this.selectedIndex).doAction();  }
			else
				{	this.hmInInt.get(this.selectedIndex).doAction();	}
			}			
		return;
		}

	/**  Changes the unit of bar moved per button press.
	 * 
	 * @param increment  Quantum unit index.
	 */
	public void changeQuanta( int increment )
		{
		this.quantaIndex += increment;
		if( this.quantaIndex < 0 )	{ this.quantaIndex = 0; }
		if( this.quantaIndex > (this.quanta.length-1) )	
			{ this.quantaIndex = (this.quanta.length-1); }
		return;
		}
	public Double getQuanta( int index )
		{	return(this.quanta[index]);	}
	public Double getQuanta( )
		{	return( this.quanta[this.quantaIndex] );	}
	
	/**  Transfer input into guages  */
	private void processInput()
		{
		if( this.isEnableInput == false )					{ return; }
// * below check seems prudent.. but does not work.
//	if( ! core.VergeEngine.Vmm.transferInput() )	{ return; }
		core.VergeEngine.Vmm.transferInput();

		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{
				if( this.hmInDec.get(n).getText().isEmpty() ) 
					{ continue; }
				System.out.println("-- processInput :: transferred -- ");
				this.hmGDec.get(n).setValue(
					this.hmInDec.get(n).getInput() );
				this.hmInDec.get(n).setText("");
				}
			else
				{
				if( this.hmInInt.get(n).getText().isEmpty() ) 
					{ continue; }
				this.hmGInt.get(n).setValue(
					this.hmInInt.get(n).getInput() );
				this.hmInInt.get(n).setText("");
				}
			}
		return;
		}
	
	private void updateStatus()
		{
		String rslt = new String("");
		String mytip = new String("");
		if( this.hmType.get(this.selectedIndex) == true )
			{
			mytip = this.hmGDec.get(this.selectedIndex).getTip()[0];
			if( mytip != null && mytip.isEmpty() == false )
				{ rslt.concat(mytip+" = "); }

			rslt = rslt.concat( Double.toString( 
					this.hmGDec.get( this.selectedIndex ).getValue() ));
			rslt = rslt.concat(" ( ");
			rslt = rslt.concat( Double.toString( 
					this.hmGDec.get( this.selectedIndex ).getValueMin() ));
			rslt = rslt.concat(" - ");
			rslt = rslt.concat( Double.toString( 
					this.hmGDec.get( this.selectedIndex ).getValueMax() ));
			rslt = rslt.concat(" ) +");
			rslt = rslt.concat( Double.toString(
				this.quanta[this.quantaIndex] * 100.0d ) );
			rslt = rslt.concat("%");
			}
		else
			{
			mytip = this.hmGInt.get(this.selectedIndex).getTip()[0];
			if( mytip != null && mytip.isEmpty() == false )
				{ rslt = rslt.concat(mytip+" = "); }
			rslt = rslt.concat(Integer.toString( 
					this.hmGInt.get(this.selectedIndex).getValue() ));
			rslt = rslt.concat(" ( ");
			rslt = rslt.concat(Integer.toString( 
					this.hmGInt.get(this.selectedIndex).getValueMin() ));
			rslt = rslt.concat(" - ");
			rslt = rslt.concat( Integer.toString( 
					this.hmGInt.get(this.selectedIndex).getValueMax() ));
			rslt = rslt.concat(" ) +");
			rslt = rslt.concat( Double.toString(
				this.quanta[this.quantaIndex] * 100.0d ) );
			rslt = rslt.concat("%");
			}
		this.statusBar.setText( rslt );

		return;
		}
	
	/**  Directly sets a status bar that to a pre-built object.
	 * The text and positions are overridden, but style will remain.
	 * 
	 * @param vmiTS  a vmiTextSimple object with desired appearance.
	 */
	public void setStatusBarObject( VmiTextSimple vmiTS )
		{
		this.statusBar = vmiTS;
		this.statusBar.setExtendX(this.w, false );
		this.resolvePositions();
		this.updateStatus();
		return;
		}
	
	/**  Returns the status bar reference.   Use it to directly alter 
	 * the attributes of the status bar.  Do not use this to adjust the text
	 * or positioning, as those get overridden by the menu.
	 * 
	 * @return   A VmiTextSimple Object currently used as the status bar.
	 */
	public VmiTextSimple getStatusBarObject(  )
		{	return(this.statusBar);	}

	/** Directly changes the string displayed in the status bar, if its On.
	 * Note that many internal methods will also change this alot.  
	 * 
	 * @param msg	The String message to display.
	 */
	public void setStatus( String msg )
		{
		if( this.isEnableStatusBar == false )
			{ return; }
		this.statusBar.setText( msg );
		return;
		}
	public String getStatus()
		{ return( this.statusBar.getText() ); }
	
	/**  Sets all guages to their maximum value.	 */
	public void setAllGuagesMax()
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setToMax();	}
			else
				{	this.hmGInt.get(n).setToMax();	}
			}
		return;
		}
	
	/**  Sets all guages to their minimum value.	 */
	public void setAllGuagesMin()
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setToMin();	}
			else
				{	this.hmGInt.get(n).setToMin();	}
			}
		return;
		}
	
	/**  Sets all guages to a percentage of max value. */
	public void setAllGuagesToPercentage( Double pct )
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setToPercentage(pct);	}
			else
				{	this.hmGInt.get(n).setToPercentage(pct);	}
			}
		return;
		}

	/**  Sets all guages to the same color.. */
	public void setAllGuagesColor( Color solidColor )
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setBarSolidColor(solidColor);	}
			else
				{	this.hmGInt.get(n).setBarSolidColor(solidColor);	}
			}
		return;
		}
	
	/**  Sets all guages to two color vertical.gradient.. */
	public void setAllGuagesColor( Color coreColor, Color edgeColor )
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{
				this.hmGDec.get(n).setBarColors(coreColor, edgeColor);	 
				}
			else
				{
				this.hmGInt.get(n).setBarColors(coreColor, edgeColor);	
				}
			}
		return;
		}
	
	/**  Sets all guages frames on or off */
	public void setAllGuagesBorder( boolean onOff )
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).enableFrame(onOff);	}
			else
				{	this.hmGInt.get(n).enableFrame(onOff); 	}
			}
		return;
		}


	/**
	 * @return the Long childID
	 */
	public Long getChildID()
		{	return childID;	}

	/**
	 * @param childID  Long childID to set
	 */
	public void setChildID(Long childID)
		{	this.childID = childID;	}

	/**   Get the number type of the guage at position [index]
	 * 
	 * @param index  guage number, hash key.
	 * @return  true if this is a Decimal guage, false if an Integer.
	 */
	public boolean getGuageType( Integer index )
		{	return( this.hmType.get(index) );	}

	/** If the value of guage [index] is an Intege,  returns the Integer.
	 *   otherwise it will return null.   Always check for null.  */
	public Integer getGuageValueInteger( Integer index )
		{
		Integer rslt = null;
		if( this.hmGInt.get( index ) == null )
			{ return(rslt); }
		if( this.hmType.get(index) == true ) 
			{ return(rslt); }
		rslt = new Integer( this.hmGInt.get(index).getValue() );
		return(rslt);
		}
	/** If the value of guage [index] is a Double (decimal), return it.
	 *   otherwise it will return null.   Always check for null.  */
	public Double getGuageValueDecimal( Integer index )
		{
		Double rslt = null;
		if( this.hmGDec.get( index ) == null )
			{ return(rslt); }
		if( this.hmType.get(index) == false ) 
			{ return(rslt); }
		rslt = this.hmGDec.get(index).getValue();
		return(rslt);
		}

	/** Attaches the given Method to all guages. 
	 * This will be the method invoked when a guage is "activated" when 
	 * input mode is turned off.  Limited practical usefulness. */
	public void setAllGuagesAction( Method action )
		{
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{	this.hmGDec.get(n).setAction(action);	}
			else
				{	this.hmGInt.get(n).setAction(action); 	}
			}
		return;
		}

	/**  Attach a keycode (hotkey) to a guage.
	 * It will activate either the attached method or start nput for target.
	 * @param index   target guage number.  Base 0
	 * @param vergeKeyCode   extended keycode (typcially 0-255)
	 */
	public void setGuageKeyCode( Integer index, Integer vergeKeyCode )
		{
		if( this.hmType.get(index) == null )
			{ return; }
		if( this.hmType.get(index) == true )
			{	this.hmGDec.get(index).setKeycode(vergeKeyCode); }
		else
			{	this.hmGInt.get(index).setKeycode(vergeKeyCode);	}
		return;
		}

	/** Attaches a string description to a guage index.  */
	public void setGuageTip( Integer index, String theTip )
		{
		if( this.hmType.get(index) == null )
			{ return; }
		if( this.hmType.get(index) == true )
			{	this.hmGDec.get(index).setTip(theTip); }
		else
			{	this.hmGInt.get(index).setTip(theTip);	}
		return;
		}
	public String getGuageTip( Integer index )
		{
		if( this.hmType.get(index) == null )
			{ return(null); }
		if( this.hmType.get(index) == true )
			{	return( this.hmGDec.get(index).getTip()[0] ); }
		else
			{	return( this.hmGInt.get(index).getTip()[0] ); }
		}

	/**  Returns the bounding width of the entire menu. */
	public int getWidthPx()
		{ return(this.w); }
	/**  Returns the calculated height of the entire menu body in pixels. 
	 * Does not count the status bar, */
	public int getHeightPx()
		{ return(this.calcH); }

	public void setGuageGradient( Integer index, 
			VImageGradient gradientImage )
		{
		if( this.hmType.get(index) == null )
			{ return; }
		if( this.hmType.get(index) == true )
			{	this.hmGDec.get(index).setBarGradient(gradientImage); }
		else
			{	this.hmGInt.get(index).setBarGradient(gradientImage);	}
		return;
		}

	public void setGuageHeight( int newHeightPx )
		{
		if( newHeightPx < 3 )		{ return; }
		this.guageHeight = newHeightPx;
		for( Integer n : this.hmType.keySet() )
			{
			if( this.hmType.get(n) == true )
				{
				this.hmGDec.get(n).resize( this.hmGDec.get(n).getWidth(),
					newHeightPx);
				}
			else
				{
				this.hmGInt.get(n).resize( this.hmGInt.get(n).getWidth(),
					newHeightPx);				
				}
			}
		return;
		}
	
	}


/**   Box model   (not quite to scale) 
 * 
 * 		|-------- Border ------------------------------------------------------|
 * 		|            |____________|__padding __________________| |
 * 		| padding | icon pad |  Guage Body                 | input []   | |
 *		|            |________|______padding _________|________| | 
 * 		|----------------------------------------------------------------------|
 *		|																|	
 * 		|					<< repeat for each item >>				|
 * 		|																|
 * 		|-------- Border -----------------------------------------------------|
 * */
