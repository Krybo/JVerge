package menus;

import java.awt.Color;
import java.util.HashMap;

import menus.Vmenu.enumMenuEVENT;
import domain.VImage;
import domain.VSound;

public class VmenuSliders implements Vmenu
	{
	private int x;		// X anchor
	private int y;		// Y anchor
	private int w;		// Pixel Width allocation

	private Integer selectedIndex;
	private Integer items;
	private Integer guageHeight = 10;
	private Integer borderWidth = 2;
	private Integer iconPaddingPx = 16;
	private Double magnifier = 1.0d;
	
	private boolean useDirectInputs = false;
	private boolean useIcons = false;
	private boolean isEnableInput = false;
	private boolean isBkgImg = false;
	private boolean isVisible = true;
	private boolean isActive = true;

	private Long focusID = new Long(-1);
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

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

	public VmenuSliders(int anchorX, int anchorY, int widthPx, 
			int guageCount, boolean isDecimalGuage, boolean useInputs )
		{
		this.x = anchorX;
		this.y = anchorY;
		this.w = widthPx;
		
		this.isBkgImg = false;
		this.isVisible = true;
		this.isActive = true;
		
		this.selectedIndex = -1;
		this.setIconPaddingPx(16);
		this.items = new Integer(0);
		this.useDirectInputs = false;
		this.useIcons = false;
		this.guageHeight = new Integer(10);
		this.borderWidth = new Integer(2);

		for( int n = 0; n < guageCount; n++ )
			{
			if( isDecimalGuage == true )
				{	this.addDecimalGuage( useInputs );	}
			else
				{	this.addIntegerGuage( useInputs );	}
			}
		
		this.isEnableInput = useInputs;
		this.refresh();
		return;
		}

	/** adds a default gauge, Integer values 0 to 100. */
	public Integer addIntegerGuage( boolean hasInput )
		{
		// Width of guages cannot be changed, so get it right here.
		Double tWidth = new Double(this.w);
		if( this.useDirectInputs == true )
			{ tWidth = new Double(this.w) * 0.8d; }
		VmiGuageInt vmi = new VmiGuageInt( 0, 0, 
			tWidth.intValue(), this.guageHeight, 0, 100, 0 );
		this.hmGInt.put( this.items, vmi );

		if( hasInput == true )
			{
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
		// Width of guages cannot be changed, so get it right here.
		Double tWidth = new Double( this.w );
		if( this.useDirectInputs == true )
			{ tWidth = new Double(this.w) * 0.8d; }
		VmiGuageDecimal vmi = new VmiGuageDecimal( 0, 0, 
			tWidth.intValue(), this.guageHeight, 0.0d, 100.0d, 0.0d );
		this.hmGDec.put( this.items, vmi );

		if( hasInput == true )
			{
			VmiInputDecimal vd = new VmiInputDecimal("X", 
					"Change Integer Value", 0, 0 );
			this.hmInDec.put( this.items, vd );
			}

		this.hmType.put( this.items, true );
		this.items++;
		if( this.items == 1 )	{ this.selectedIndex = 0; }
		return( this.items );
		}


	@Override
	public boolean paint(VImage target)
		{
		// TODO Auto-generated method stub
		return false;
		}

	@Override
	public boolean doControls(Integer ext_keycode)
		{
		// TODO Auto-generated method stub
		return false;
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
			this.items++;
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

	/** Looks for a menu-item with a specified ID, then returns it.
	 *   if its not found, returns null */
	public Vmenuitem getMenuItemByID(Long id)
		{
		for( Integer is : this.hmType.keySet() )
			{
			if( this.hmType.get(is) == true )
				{
				if( this.hmGDec.get(is).getId() == id )
					{	return( this.hmGDec.get(is) );   }
				}
			else
				{
				if( this.hmGInt.get(is).getId() == id )
					{	return( this.hmGInt.get(is) );   }				
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
	 *    confusion.   If you alter the menu as a whole.. do call this.  */
	private void resolvePositions()
		{
		// TODO Auto-generated method stub
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
