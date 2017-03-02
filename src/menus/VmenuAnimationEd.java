package menus;

import static core.Script.getInput;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.HashMap;

import core.Controls;
import menus.Vmenu.enumMenuEVENT;
import menus.Vmenuitem.enumMenuItemSTATE;
import menus.VmiButton.enumMenuButtonCOLORS;
import menus.VmiTextSimple.enumMenuStxtCOLORS;
import domain.VImage;
import domain.VSound;
import domain.Vsp;

public class VmenuAnimationEd implements Vmenu
	{
	private int x = 0,y = 0;
	private int w = 0, h = 0;
	// Current modifyable Integer values for the text inputs.
	private Integer curStart = 0;
	private Integer curEnd = 0;
	private Integer curDelay = 0;
	private Integer curMode = 0;
	private String curDesc = new String("No Description");
	private boolean isActive = false;
	private boolean isVisible = false;

	private int vmFocus;		// Keyboard control focus
	private int animNum;	// Current working animation index.
	private Vsp theVsp;		// The attached VSP to draw from
	private boolean isModified = false;		// do changes need saved?
	// Timers used for display of animations.
	private final Long animTimerInit = System.nanoTime();
	private Long animTimer = new Long( animTimerInit );
	private Long timerAnimDuration = new Long(0);
	private Long timerFrameDuration = new Long(0);

	private VmiButton btnDone;
	private VmiButton btnNext, btnPrev;
	private VmiButton btnAdd, btnRemove;
	private VmiTextSimple vmitAnimNumber;
	private VmiTextSimple vmitBegin, vmitEnd;
	private VmiTextSimple vmitDelay, vmitMode;
	private VmiInputSentence vmitDesc;
	
	private Long focusID;
	private Long parentID;
	private Long childID;

	private VImage bkgImg = null;
	private boolean isImgBackground;

	private HashMap<enumMenuEVENT,VSound> hmSounds =
		new HashMap<enumMenuEVENT,VSound>();
	
	private final Color clrTrans = new Color(255,0,255,0);
	private final Color clrHlight = new Color(128,0,64, 64 );
	
	
	/**  Constructor - only need a VSP object to draw animations from */
	public VmenuAnimationEd( Vsp vspData, int leftX, int topY )
		{
		this.theVsp = vspData;
		this.x = leftX;
		this.y = topY;
		this.w = 300;
		this.h = 200;

		this.animNum = 0;

		// Initialize controls.
		this.vmitAnimNumber = new VmiTextSimple( 
			"Animation # 0", this.x+80, this.y+10 );
		this.btnPrev = new VmiButton(
			this.x, this.y, 10, 10, 24, 24, 1, false, true, false, true, true	);
		this.btnNext = new VmiButton(
			this.x, this.y, 40, 10, 24, 24, 1, false, true, false, true, true	);
		this.btnAdd = new VmiButton(
			this.x, this.y, 10, 50, 50, 24, 2, false, false, false, true, true	);
		this.btnRemove = new VmiButton(
			this.x, this.y, 10, 80, 50, 24, 2, false, false, false, true, true	);
		this.btnDone = new VmiButton(
			this.x, this.y, 10, 110, 50, 24, 2, false, false, false, true, true	);

		this.vmitDesc = new VmiInputSentence("No Description",
			"Type description of this animation:", this.x+10, this.y+164 );
		
		this.vmitBegin = new VmiTextSimple( "-", this.x+70, this.y+48 );
		this.vmitEnd = new VmiTextSimple( "-", this.x+70, this.y+75 );
		this.vmitDelay = new VmiTextSimple( "-", this.x+70, this.y+102 );
		this.vmitMode = new VmiTextSimple( "-", this.x+70, this.y+129 );

		this.focusID =  Vmenu.getRandomID();
		this.isImgBackground = false;

		this.configureMenuitems();
		return;
		}

	public boolean paint( VImage target )
		{
		if (this.isVisible == false)
			{	return( false );  }
		if (target == null)
			{	return( false );	}
		
		this.resolveStates();
		this.refresh();
		
		// Main body block.
		if (this.isImgBackground == true && this.bkgImg != null )
			{
			target.scaleblit(this.x, this.y, this.w, this.h, this.bkgImg);
			}
		else
			{
			target.rectfill( this.x, this.y, 
					this.x+w, this.y+h, Color.BLACK );
			target.rect( this.x, this.y, 
					this.x+w, this.y+h, Color.BLACK );
			}
		
		// Paint all sub menu items
		for( Integer x = 0; x < this.countMenuItems(); x++ )
			{	this.getMenuItem(x).paint( target );	}

		// Specific annotations
		target.printString( this.x+12, this.y+152, 
			core.Script.fntMASTER, Color.WHITE , 
			"x " + Integer.toString( this.theVsp.getNumAnimations() )  );
		target.printString( this.x+126, this.y+64, 
			core.Script.fntMASTER, Color.WHITE , "T# Start" );
		target.printString( this.x+126, this.y+91, 
			core.Script.fntMASTER, Color.WHITE , "T# End" );
		target.printString( this.x+126, this.y+118, 
			core.Script.fntMASTER, Color.WHITE , "Delay ms" );		
		target.printString( this.x+126, this.y+145, 
			core.Script.fntMASTER, Color.WHITE , "Mode: "+
			this.getCurrentMode() );
		
		target.printString( this.x+20, this.y+66, 
			core.Script.fntMASTER, Color.WHITE , "ADD" );

		target.printString( this.x+16, this.y+25, 
			core.Script.fntMASTER, Color.WHITE , "<" );
		target.printString( this.x+48, this.y+25, 
			core.Script.fntMASTER, Color.WHITE , ">" );

		if( this.btnRemove.getState() != 
				enumMenuItemSTATE.DISABLED.value() )
			{
			target.printString( this.x+20, this.y+96, 
				core.Script.fntMASTER, Color.WHITE , "RMOV" );
			}
		
		if( this.btnDone.getState() != 
				enumMenuItemSTATE.DISABLED.value() )
			{
			target.printString( this.x+20, this.y+126, 
				core.Script.fntMASTER, Color.WHITE , "SAVE" );
			}

		if( (this.timerFrameDuration > 0) && (this.timerAnimDuration > 0) )
			{
			Long curFrameNns = 
				(System.nanoTime() - this.animTimerInit) % timerAnimDuration;
			Long frameNum = 
				new Long( curFrameNns / this.timerFrameDuration);
			
			}
		
		// TODO Draw the preview.  HERE
		
		return false;
		}


	/** Sets the state of menu items. */
	private void resolveStates()
		{
		if( this.vmFocus < 0 || this.vmFocus >= this.countMenuItems() )
			{ return; }
		
		// Disable remove button if there are none left to remove....
		if( this.theVsp.getNumAnimations() == 0 )
			{
			this.btnRemove.setState( 
				enumMenuItemSTATE.DISABLED.value());
			}
		else
			{
			this.btnRemove.setState( 
				enumMenuItemSTATE.NORMAL.value());			
			}

		if( this.isModified )
			{
			this.btnDone.setState( 
				enumMenuItemSTATE.NORMAL.value());			
			}
		else
			{
			this.btnDone.setState( 
				enumMenuItemSTATE.DISABLED.value() );			
			}

		for( Integer x = 0; x < this.countMenuItems(); x++ )
			{
			Vmenuitem myvmi = this.getMenuItem(x);
			if( ! myvmi.isActive() )
					{  continue; }
			if( myvmi.getState() == 
				enumMenuItemSTATE.ACTIVATED.value()  )
					{ continue; }
			if( myvmi.getState() == 
				enumMenuItemSTATE.DISABLED.value()  )
					{ continue; }

			if( x == this.vmFocus )
//				myvmi.getState() != 
//					enumMenuItemSTATE.ACTIVATED.value()  &&
//				myvmi.getState() != 
//					enumMenuItemSTATE.DISABLED.value() )
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

	public boolean doControls(Integer ext_keycode )
		{

		if( this.processInputs() > 0 ) 
			{ return(true); }
		
		this.resolveStates();
		
		Integer basecode = Controls.extcodeGetBasecode(ext_keycode);
		boolean isShift = Controls.extcodeGetSHIFT(ext_keycode);
		boolean isCntl = Controls.extcodeGetCNTL(ext_keycode);
		boolean isAlt = Controls.extcodeGetALT(ext_keycode);
		
		// These extended keystrokes are not necessary in this menu
		if( isCntl || isShift || isAlt )
			{ return(false); }
		
		// Hotkeys are handled by parent menu
		
		// normal key overrides.
		switch( basecode )
			{
			case 0:		// "Update pulse"  do nothing.
				break;
			case 10: 		// ENTER KEY <CONFIRM>
				this.funcActivate();
				break;
			
			case 32:		// [SPACE] bar - zero out inputs.
				switch( this.vmFocus ) 
					{
					case 0:
						this.animNum = 0;
						this.loadAnimation( 0 );
						break;
					case 6:
						this.curDesc = new String("No Description");
						break;
					case 7:
						this.curStart = 0;
						break;
					case 8:
						this.curEnd = 0;
						break;
					case 9:
						this.curDelay = 0;
						break;
					case 10:
						this.curMode = 0;
						break;
					default:
						break;
					}
				break;
				
			// Left and right controls are more descrete due to menu layout
			//  3 sets of buttons can be jumped to.
			case 37: 		// ARROW-[LEFT]
				if( this.vmFocus < 3 && this.vmFocus >= 0 )
					{  this.vmFocus = 7; break; }
				if( this.vmFocus < 6 && this.vmFocus > 2 )
					{ this.vmFocus = 0; break; }
				if( this.vmFocus > 6 )
					{ this.vmFocus = 3; }
				break;
			case 38: 		// ARROW-[UP]
				this.changeSelectedIndex(-1);
				break;
			case 39: 		// ARROW-[RIGHT]
				if( this.vmFocus < 3 && this.vmFocus >= 0 )
					{  this.vmFocus = 3; break; }
				if( this.vmFocus < 6 && this.vmFocus > 2  )
					{ this.vmFocus = 7; break; }
				if( this.vmFocus > 6 )
					{ this.vmFocus = 0; }
				break;
			case 40: 		// ARROW-[DOWN]
				this.changeSelectedIndex(+1);
				break;
			
			// Numbers keystrokes in this menu will be inserted
			//     directly into the input boxes.  Funky conversions needed.
			case 48:	case 49:  case 50:  case 51:  case 52:
			case 53:	case 54:  case 55:  case 56:  case 57: case 45:
				if( this.vmFocus == 0 )
					{
					Integer rtn = this.normalizeIntegerInput(
						this.getMenuItem(this.vmFocus), "Animation # ", 
						Character.toString ( (char) basecode.intValue() ),
						0, this.theVsp.getNumAnimations()-1 );
					if( rtn == -1 )
						{ 
						System.out.println( "NUMERIC INPUT REJECTED.");
						}
					if( this.vmFocus == 7 )
						{ this.curStart = rtn; }
					if( this.vmFocus == 8 )
						{ this.curEnd = rtn; }
					if( this.vmFocus == 9 )
						{ this.curDelay = rtn; }
					if( this.vmFocus == 10 )
						{ this.curMode = rtn; }
					break;
					}
				if( this.vmFocus > 6 && this.vmFocus < 11) 
					{
					Integer rtn;
						rtn = this.normalizeIntegerInput(
							this.getMenuItem(this.vmFocus),"",
							Character.toString ( (char) basecode.intValue()),
							0, null);
					if( rtn == -1 )
						{ 
						System.out.println( "NUMERIC INPUT REJECTED.");
						}
					if( this.vmFocus == 7 )
						{ this.curStart = rtn; }
					if( this.vmFocus == 8 )
						{ this.curEnd = rtn; }
					if( this.vmFocus == 9 )
						{ this.curDelay = rtn; }
					if( this.vmFocus == 10 )
						{ this.curMode = rtn; }
					}
				break;
			
			default:
				System.out.println( " VM ANIM unhandled KC = " +
					Integer.toString(ext_keycode) + 
					" :: " + Integer.toString( basecode));
				break;
			}
		
		return false;
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
		return;
		}

	public Integer countMenuItems()
		{	return( new Integer(11) );	}

	/** interface method holder:   this does nothing. */
	public Integer addItem(Vmenuitem vmi)
		{	return( new Integer(11) );	}

	/** interface method holder:   this does nothing. */
	public Vmenuitem popItem()
		{	return null;	}

	/** interface method holder:   this does nothing. */
	public Vmenuitem removeItem(int index)
		{	return null;	}

	/** interface method holder:   this does nothing. */
	public Integer insertItem(Vmenuitem vmi, int index)
		{	return null;	}

	/** Returns the menuitem that has control focus */
	public Vmenuitem getMenuItemSelected()
		{	return( this.getMenuItem(this.vmFocus));	}

	/** Returns a descrete menu item */
	public Vmenuitem getMenuItem( int index )
		{
		switch( index )
			{
			case 0:
				return( this.vmitAnimNumber );
			case 1:
				return( this.btnPrev );
			case 2:
				return( this.btnNext );
			case 3:
				return( this.btnAdd );
			case 4:
				return( this.btnRemove );
			case 5:
				return( this.btnDone );
			case 6:
				return( this.vmitDesc );
			case 7:
				return( this.vmitBegin );
			case 8:
				return( this.vmitEnd );
			case 9:
				return( this.vmitDelay );
			case 10:
				return( this.vmitMode );
			default:
				return( this.vmitAnimNumber );
			}
		}

	/** Provides indexed access to the menu id's of the constituants 
	 * member Vmenuitems.  null if given invalid index. */
	public Vmenuitem getMenuItemByID( Long id )
		{
		if( this.vmitAnimNumber.getId() == id )
			{ return(this.vmitAnimNumber); }
		if( this.btnPrev.getId() == id )
			{ return( this.btnPrev ); }
		if( this.btnNext.getId() == id )
			{ return( this.btnNext ); }
		if( this.btnAdd.getId() == id )
			{ return( this.btnAdd ); }
		if( this.btnRemove.getId() == id )
			{ return( this.btnRemove ); }
		if( this.btnDone.getId() == id )
			{ return( this.btnDone ); }
		if( this.vmitDesc.getId() == id )
			{ return( this.vmitDesc ); }
		if( this.vmitBegin.getId() == id )
			{ return( this.vmitBegin ); }
		if( this.vmitEnd.getId() == id )
			{ return( this.vmitEnd ); }
		if( this.vmitDelay.getId() == id )
			{ return( this.vmitDelay ); }
		if( this.vmitMode.getId() == id )
			{ return( this.vmitMode ); }
		return null;
		}

	/** This should be the only place the current values are
	 * set into the text menu items.  */
	public void refresh()
		{
		this.vmitAnimNumber.setText(
			"Animation # " + Integer.toString( this.animNum)  );
		this.vmitBegin.setText( this.curStart.toString() );
		this.vmitEnd.setText( this.curEnd.toString() );
		this.vmitDelay.setText( this.curDelay.toString() );
		this.vmitMode.setText( this.curMode.toString() );
		this.vmitDesc.setText( this.curDesc );
		this.vmitBegin.setIconContent( 
			this.theVsp.getTileAsVImage(this.curStart) );
		this.vmitEnd.setIconContent( 
			this.theVsp.getTileAsVImage(this.curEnd) );
		return;
		}

	public Integer getSelectedIndex()
		{	return(this.vmFocus);	}

	public int getSelectedIndexPosX()
		{	return( this.getMenuItemSelected().getX().intValue() );	}

	public int getSelectedIndexPosY()
		{	return( this.getMenuItemSelected().getY().intValue() );	}

	/** Directly change the menuitem focus */
	public void setSelectedIndex( Integer index )
		{
		int diff = index - this.vmFocus; 
		this.vmFocus = index;
		int loopguard = 0;
		int loopMax = this.countMenuItems()*2;
		
		if( this.vmFocus > (this.countMenuItems()-1) )
			{ this.vmFocus = 0; return; }
		if( this.vmFocus < 0 )
			{ this.vmFocus = this.countMenuItems()-1; }
		
		// Prevent disabled buttons from becoming selected.
		while( (this.getMenuItem(this.vmFocus).getState() == 
				  enumMenuItemSTATE.DISABLED.value() ) && 
				(loopguard < loopMax ) )
			{
//			System.out.println("attempted to select disabled item!" );
			this.vmFocus += diff;
			loopguard++;
			}
		
		return;
		}

	/** set the unique menu id to something else.
	 * There is generally no need to change this from the ID 
	 * generated in the constructor. */
	public void setFocusId( Long id )
		{
		this.focusID = id;
		return;
		}

	public Long getFocusId()
		{	return( this.focusID );	}

	/** Given a unique menu id, tells if it matches this instances id. */
	public boolean isFocus(Long id)
		{
		if( id == this.focusID )
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
		{	return(this.isActive);	}

	public boolean isVisible()
		{	return(this.isVisible);	}

	public void activateSelected()
		{
		this.getMenuItemSelected().doAction();
		this.playMenuSound(enumMenuEVENT.CONFIRM, 33 );
		return;
		}

	public void attachSound(enumMenuEVENT slot, VSound sfx)
		{
		if( sfx == null )
			{	return;	}
		this.hmSounds.put( slot, sfx );
		return;
		}

	public boolean playMenuSound(enumMenuEVENT slot, int volume0to100)
		{
		if( this.hmSounds == null )
			{	return (false);	}
		if( this.hmSounds.get(slot) == null )
			{	return (false);	}
		this.hmSounds.get(slot).start(volume0to100);
		return (true);
		}

	//  No icons in this menu.
	public void setIconsAll(boolean onOff)
		{	return;	}

	// Denied menuitem frame control for this menu.
	public void setBorderAll( boolean onOff, int thick)
		{	return;	}

	/** Setting this ID essentially makes another menu own this one as 
	 * a sub-menus.   If set to recursive, will apply the given parentID
	 * directly to this menus Vmenuitem components.  */
	public void setParentID( Long id, boolean recursive )
		{
		this.parentID = id;
		if( recursive == true )
			{
			for( Integer x = 0; x <= this.countMenuItems(); x++ )
				{	this.getMenuItem(x).setParentID( id );	}
			}
		return;
		}

	/** If this has a sub Vmenu - document its ID here. 
	 * Recurse also applies sub Vmenuitems */
	public void setChildID(Long id, boolean recursive )
		{
		this.childID = id;
		if( recursive == true )
			{
			for( Integer x = 0; x <= this.countMenuItems(); x++ )
				{	this.getMenuItem(x).setChildID( id );	}
			}
		return;
		}
	

	/* ============  unique methods ================== */ 
	
	/** Changes the animation number currently being edited.
	 *    returns ralse if it does not exist.  ese true. */
	public boolean loadAnimation( int animationIndex )
		{
		System.out.println( "DEBUG: loadAnimation got arg: " + 
				Integer.toString(animationIndex) );
		// Sanity checks.
		if( animationIndex < 0 )
			{ 	animationIndex = this.theVsp.getNumAnimations() - 1; 	}
		if( animationIndex >= this.theVsp.getNumAnimations() )
			{ 	animationIndex = 0; 	}
		if( ! this.theVsp.checkAnimExistsByIndex(animationIndex) )
			{  return(false); }
		
		int backup = this.animNum; 
		try {
			this.animNum = animationIndex;
			this.curStart = this.theVsp.getAnimStart( animationIndex );
			this.curDelay = this.theVsp.getAnimDelay( animationIndex );
			this.curEnd = this.theVsp.getAnimEnd( animationIndex );
			this.curMode = this.theVsp.getAnimMode( animationIndex );
			this.curDesc = this.theVsp.getAnimName( animationIndex );

			this.vmitBegin.setIconContent( new VImage(
				this.theVsp.getTileAsVImage(
				this.theVsp.getAnimStart( animationIndex ) ) ) );
			this.vmitEnd.setIconContent( 
				this.theVsp.getTileAsVImage(
				this.theVsp.getAnimStart( animationIndex ) ) );
			
			this.refresh();
			}
		catch( Exception e )
			{ 
			e.printStackTrace();
			this.animNum = backup;
			return(false); 
			}
		
		System.out.println("DEBUG : changed to animation # "+
				Integer.toString( animationIndex ));

		this.isModified = false;
		return(true);
		}

	public boolean loadNextAnimation()
		{
		return( this.loadAnimation( this.animNum + 1 ) );
		}
	public boolean loadPrevAnimation()
		{
		return( this.loadAnimation( this.animNum - 1 ) );
		}
	
	// Sets colors and such for the component control items.
	//  *makes a shorter constructor method.
	//   Safe to assume all objects have been initialized by before method.
	private void configureMenuitems()
		{
		HashMap<Integer,Color> confBtn = 
			new HashMap<Integer,Color>();
		HashMap<Integer,Color> confInp = 
			new HashMap<Integer,Color>();
		
		confBtn.put( enumMenuButtonCOLORS.BODY_ACTIVE.value(), 
			this.clrTrans );
		confBtn.put( enumMenuButtonCOLORS.BODY_INACTIVE.value(), 
			this.clrTrans );
		confBtn.put( enumMenuButtonCOLORS.BODY_SELECTED.value(),
			this.clrTrans );

		confInp.put( enumMenuStxtCOLORS.BKG_ACTIVE.value(),
			this.clrTrans );
		confInp.put( enumMenuStxtCOLORS.BKG_INACTIVE.value(),
			this.clrTrans );
		confInp.put( enumMenuStxtCOLORS.TEXT_INACTIVE.value(),
			Color.GRAY );
		confInp.put( enumMenuStxtCOLORS.TEXT_ACTIVE.value(),
			Color.WHITE );

		this.vmitDesc.setExtendX( this.w - 20 , false );

		this.btnNext.setColorContent(confBtn);
		this.btnPrev.setColorContent(confBtn);
		this.btnAdd.setColorContent(confBtn);
		this.btnRemove.setColorContent(confBtn);
		this.btnDone.setColorContent(confBtn);

		this.vmitBegin.enableIcons( true );
		this.vmitBegin.enableFrame(false);
		this.vmitBegin.enableActivation();
		this.vmitBegin.setColorContent( confInp );
		this.vmitBegin.setExtendX( 130, false );
		this.vmitEnd.enableIcons( true );
		this.vmitEnd.enableFrame(false);
		this.vmitEnd.enableActivation();
		this.vmitEnd.setColorContent( confInp );
		this.vmitEnd.setExtendX( 130, false );
		this.vmitDelay.enableFrame(false);
		this.vmitDelay.enableActivation();
		this.vmitDelay.setColorContent( confInp );
		this.vmitDelay.setExtendX( 130, false );
		this.vmitMode.enableFrame(false);
		this.vmitMode.enableActivation();
		this.vmitMode.setColorContent( confInp );
		this.vmitMode.setExtendX( 190, false );

		this.btnDone.setAction(
			core.Script.getFunction( Vmenuitem.class, "goParent") );

		return;
		}
	
	private int processInputs()
		{
		int rslt = 0;
		// This form is simple enough to handle the inputs descretely
		String theInput = getInput( this.vmitDesc.getId() );	
		if( ! theInput.equals("") ) 
			{
			this.curDesc = theInput;
			this.isModified = true;
			rslt++; 
			}
		return(rslt);
		}
	
	/** Changes the selected menu item by a relative amount */
	public void changeSelectedIndex( Integer relChange )
		{
		this.setSelectedIndex( this.vmFocus + relChange );
		return;
		}

	/** Change to another Vsp.  Nothing special because its
	 * only changing a pointer.  */
	public void changeVsp( Vsp newVsp )
		{
		this.theVsp = newVsp;
		this.loadAnimation( 0 );
		return;
		}

	// Handle Executive functions for each button / input.
	private void funcActivate()
		{
		switch( this.vmFocus )
			{
			case 1:
				this.loadPrevAnimation();
				break;
			case 2:
				this.loadNextAnimation();
				break;
			case 3:  break;
			case 4:  break;
			case 5:  break;
			case 6:
				this.vmitDesc.doInput();
				break;
			case 7:  break;
			case 8:  break;
			case 9:  break;
			case 10:
				this.changeMode(+1);
				break;
			default:  break;
			}
		return;
		}
	
	/** Utility function that deals with altering input box values. */
	private Integer normalizeIntegerInput( VmiTextSimple vmi, 
			String label, String input, 
			Integer minBound, Integer maxBound )
		{
		// TODO:  Redo this to only add to the this.curX members.
		
		String filteredInput = input.replaceAll("[^0-9]","");
		Integer result = 0;
		String rebuild;
		if( filteredInput.length() <= 0 )
			{ return(-1); }
		try {
			String existing = vmi.getText();
			existing = existing.replaceAll( label ,"");
			existing = existing.replaceAll( "[^0-9]" ,"");
			if( existing.isEmpty() )
				{ existing = "0"; }
			
			rebuild = existing + filteredInput;
			rebuild = rebuild.replaceAll( "^0*" ,"" );
			// unique oppertunity here to obtain a number
			// Convert to int to ensure bounds.
			if( rebuild.isEmpty() || rebuild.equals("") || rebuild == null )
				{ rebuild = new String("0");  }
			Integer val = Integer.parseInt( rebuild );
			if( minBound != null && val < minBound )
				{ val = minBound; }
			if( maxBound != null && val > maxBound )
				{ val = maxBound; }
			result = val;
			rebuild = label + " " + Integer.toString(val);
			}
		catch( Exception e )
			{
			e.printStackTrace();
			return(-1);
			}

		vmi.setText(rebuild);
		this.isModified = true;
		return(result);	
		}

	/** attempts to cast a Vmenuitem and pass it a number. 
	 * The casting may fail.  Safer to use the alternative method.  */
	private Integer normalizeIntegerInput( Vmenuitem vmi, 
		String label, String input,
		Integer minBound, Integer maxBound )
		{
		VmiTextSimple tmp = null;
		try {
			tmp = (VmiTextSimple) vmi;
			}  
		catch( Exception e )	// If there is anything wrong.. bail.
			{ return(-1); }
		return( this.normalizeIntegerInput( tmp, label, input, 
			minBound, maxBound ) );
		}

	/**  Used by parent menus to change tile inputs.
	 * second argument == true means the start tile
	 * second argument == false means the end tile  */
	protected void importTileTarget( int tileIndex, boolean startOrEnd)
		{
		if( tileIndex >= this.theVsp.getNumtiles() || tileIndex < 0 )
			{ return; }
		
		if( startOrEnd )
			{
			this.curStart = tileIndex; 
			}
		else
			{
			this.curEnd = tileIndex;
			this.vmitEnd.setIconContent( 
				this.theVsp.getTileAsVImage( tileIndex) );
			}
		return;
		}
	
	protected Integer getCurrentStartTile()
		{	return( this.curStart );	}

	protected Integer getCurrentEndTile()
		{	return(this.curEnd);	}

	protected String getCurrentMode()
		{
		return( Vsp.animationModeToString(this.curMode) );
		}

	private void changeMode( Integer offset )
		{
		this.curMode += offset;
		while( this.curMode < 0 )
			{ this.curMode += 4; }
		while( this.curMode > 3 )
			{ this.curMode -= 4; }
		return;
		}
	
	}

