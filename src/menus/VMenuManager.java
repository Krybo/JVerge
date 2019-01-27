package menus;

import static core.Script.log;
import static core.Script.screen;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import core.Controls;
import core.JVCL;
import core.Script;
import domain.VImage;

/**  This Class is designed to manage inter-menus communications.
 *  As well as delegate controls and focus menus.
 *  It requires a JVCL to draw upon, which is typically the one built 
 *  into the verge engine... but does not have to be.
 * @author Krybo
 *
 */

public class VMenuManager
	{

	private Long SYSTEM_MENU_FOCUS_ID = new Long(-1);
	private boolean enableBackdrop = false;
	private ArrayList<Vmenu> menus = null;
	private ArrayList<Long> focus = null;
	private VImage backdrop = null;
	private VImage inputScreen = null;
	private static Font inFont = core.Script.fntCONSOLE;
	private Color backdropColor = new Color( 0.0f,0.0f,0.0f,0.5f );
	private float backdropAlpha = 0.5f;
	private boolean reverseOrder = false; 
	private JVCL targetLayerStack = null;
	
	protected static HashMap<Long,Long> hmLinkList =
			new HashMap<Long,Long>();
	private static VImage MENU_BKG = null;
	// Animation control  Key,value <Menu Id, Frequency (ns) >
	private HashMap<Long,Long> hmAnimationpFreq = 
			new HashMap<Long,Long>();
	private HashMap<Long,Long> hmAnimationpClock = 
			new HashMap<Long,Long>();
	public static final Long ANIM_ONE_SECOND = 
			new Long(1000000000L);
	public static final Long ANIM_THREE_SECOND = 
			new Long(3000000000L);
	public static final Long ANIM_FIVE_SECOND = 
			new Long(5000000000L);
	public static final Long ANIM_QUARTER_SECOND = 
			new Long(250000000L);
	public static final Long ANIM_HALF_SECOND = 
			new Long(500000000L);
	public static final Long ANIM_TENTH_SECOND = 
			new Long(100000000L);
	


	public VMenuManager(  )
		{
			// Make a new JVCL stack for the menus system.
		this.targetLayerStack = 
				new JVCL(24,screen.width,screen.height);
		this.inputScreen = new VImage( screen.width,screen.height );
		this.inputScreen.rectfill(0, 0, screen.width, screen.height,
				core.Script.CF_INV	 );

		this.backdropAlpha = 0.5f;
		this.menus = new ArrayList<Vmenu>();
		
			// No need to disable layers here.
		this.targetLayerStack.setLayerActiveAll(true);
		this.targetLayerStack.setLayerVisibilityAll(true);
		this.targetLayerStack.JVCclearAllLayers();
		this.setEnableBackdrop(false);

		this.loadDefaultMenus();

		log(" Initialized Sytem menus");
		
//		Integer sysmenutest = targetLayerStack.JVCmenuPaintAll( false );
//		log("System Menu Init returned : "+sysmenutest.toString());
	
		return;
		}
	
	/** Adds a Vmenu object to the managed stack.
	 * 
	 * @param vm	A fully implemented Vmenu object. 
	 */
	public void addVmenu( Vmenu vm )
		{
		this.menus.add(vm);
		this.assignToLayers();
		}

	/** Adds a Vmenu object to the managed stack.
	 *		Also sets it to be control focus 
	 * @param vm	A fully implemented Vmenu object. 
	 */
	public void addVmenuWithFocus( Vmenu vm )
		{
		this.menus.add(vm);
		this.focus.set( 0, vm.getFocusId() );
		this.assignToLayers();
		}

	/** Adds a series of Vmenu object to the managed stack.
	 * 
	 * @param vm	ArrayList<> containing fully implemented Vmenu 
	 */
	public void addVmenu( ArrayList<Vmenu> vms )
		{
		for( Vmenu vm : vms )
			{ this.menus.add(vm); }
		this.assignToLayers();
		}
	
	private void assignToLayers()
		{
		int n = 0;
		for( Vmenu vm : this.menus )
			{
			// this is base 1, and layer 0 and backdrop layer are reserved.
			if( n > (this.targetLayerStack.getLayerCount() ) )
				{ this.targetLayerStack.addLayer(); }	// need new layer.
			
			this.targetLayerStack.setWriteLayer(n+2);
			this.targetLayerStack.JVCmenuDetach();
			this.targetLayerStack.JVCmenuAttach( vm );
			log("rettached menu id# "+vm.getFocusId()+" to layer # "+
					Integer.toString(n+2) );
			n++;
			}
		this.targetLayerStack.refresh();
		return;
		}

	public Long getSystemMenuFocusID()
		{	return(this.SYSTEM_MENU_FOCUS_ID);	}
	public Vmenu getSystemMenu()
		{
		return(this.getMenuByID( this.getSystemMenuFocusID() ));
		}
	/** Searches the system menu for an item with given ID, returns it. */
	public Vmenuitem getSystemMenuItemByID( Long itemID )
		{
		Vmenu tmp = this.getMenuByID( this.getSystemMenuFocusID() );
		if( tmp.getMenuItemByID( itemID ) instanceof Vmenuitem )
			{ return( tmp.getMenuItemByID( itemID ) );  }
		return( null );
		}
	public Vmenu getMenuByID( Long id )
		{
		for( Vmenu vm : this.menus )
			{
			if( vm.getFocusId() == id )
				{ return(vm);  }
			}
		System.out.println("WARN getMenuByID : Item was not found");
		return(null);
		}

	public boolean isEnableBackdrop()
		{
		return enableBackdrop;
		}

	public void setEnableBackdrop(boolean enableBackdrop)
		{
		this.enableBackdrop = enableBackdrop;
		return;
		}

	public void setBackdropVImage( VImage bkg )
		{
		this.backdrop = bkg;
		return;
		}

	public void setBackdropColor( Color clr )
		{
		this.backdropColor = clr;
		return;
		}

	public void setBackdropAlpha( float newAlpha )
		{ this.backdropAlpha = newAlpha;  return; }
	
	/**	This method creates a link between a menuitem and a menu
	 * 	Allowing the activation of that menuitem to transfer the menu focus
	 *   to the child Vmenu object.
	 * @param vmParent		The parent menu
	 * @param vMenuItemNumber	the item number within the parent
	 * @param vmChild	The child menu that opens on activation.
	 * @return The Vmeuitem ID of the source linked item
	 */
	public static Long linkMenu(Vmenu vmParent, int parentMenuItemNumber,
			Vmenu vmChild )
		{
// Set the child menus parent Id to the originating menu so one may return
		vmChild.setParentID( vmParent.getFocusId(), true );

		try {
			if( vmParent.getMenuItem( parentMenuItemNumber ) == null )
				{ return(new Long(-1)); }
			// Set the destination id into the target item.
			vmParent.getMenuItem( parentMenuItemNumber ).setChildID(
				vmChild.getFocusId() );
			// Set the action of target so it goes to the child.
			vmParent.getMenuItem( parentMenuItemNumber ).setAction(
				 core.Script.getFunction( 	Vmenuitem.class, "goChild" ) );

			}
		catch( IndexOutOfBoundsException e )
			{  return(new Long(-1)); }

			// Store this away for easy/quick access later.
		VMenuManager.hmLinkList.put(
				vmParent.getMenuItem( parentMenuItemNumber ).getId(),
				vmChild.getFocusId() );

		return( vmParent.getMenuItem( parentMenuItemNumber ).getId() );
		}
	
	/**  Returns the one child menu-id that a given parent menu-id is linking too.
	 * 	Vmenuitem's have their "id"  while Vmenu use "focusID"
	 * @param parentID		the parent menu id
	 * @return	the child menu id, or -1 if no such.
	 */
	public static Long getLinkChild( Long parentID  )
		{
		if( VMenuManager.hmLinkList.get( parentID ) == null )
			{ return(new Long(-1)); }
		return( VMenuManager.hmLinkList.get( parentID ) );
		}
	
	/**  Returns all parent menu ID's that may be linking to a specific
	 * 		child menuitem id.  As an arraylist<Long>.
	 * @param childID	the child id to look for.
	 * @return	ArrayList<long> of any parents referencing this child.
	 */
	public static ArrayList<Long> getLinkParents( Long childID )
		{
		ArrayList<Long> rslt = new ArrayList<Long>();
		for( Long pid : VMenuManager.hmLinkList.keySet() )
			{
			if( VMenuManager.hmLinkList.get(pid) == childID )
				{ rslt.add(pid); }
			}
		return(rslt);
		}
	
	
	/**  Check the focus id's versus the data in the stored menus
	 * 
	 * @return	returns true if at least one focus is a real menu.
	 */
	public synchronized boolean checkFocus()
		{
		boolean rtn = false;
		for( Long xid : this.focus )
			{
			for( Vmenu vm : this.menus )	
				{
				if( vm.getFocusId() == xid )
					{ rtn = true; }
				}
			}
		return(rtn);
		}
	
	/**  With a given focus tracker number, check if a menu with that 
	 * cooresponding focus ID exists within the manager.
	 * 
	 * @param trackerID  The focus tracker ID
	 * @return	returns true if target focus has a menu.in this manager.
	 */
	public synchronized boolean checkFocus( int trackerID )
		{
		if( this.focus.get(trackerID) == null )
			{ return(false); }
		Long xid = this.focus.get(trackerID);
		for( Vmenu vm : this.menus )	
			{
			if( vm.getFocusId() == xid )
				{ return( true ); }
			}
		return( false );
		}
	
	/**  Delegates a keystroke to  Vmenu managed by this object
	 * 
	 * @param ext_keycode	Extended keycode (see core.Controls)
	 * @param onlyFocused	false: indescriminately send keycode to all --- 
	 * 		true:  only send keycode to focused menus
	 * @return   the number (count) of keystrokes processed that returned
	 * 		a request to update graphics..
	 */
	public synchronized long delegateControl( int ext_keycode, boolean onlyFocused )
		{
		long rslt = 0;
		// First scan for applicaable targets to send the key press to.
		// *Then actually do it.   This avoid ConcurrentModificationException
		// Since these are only refs.. it should be easy on resources.
		ArrayList<Vmenu> targets = new ArrayList<Vmenu>();
//		System.out.println(" DEBUG : entered delegateControl " );

		
		// Warning: Attempted to do this the other way, looping over menu
		// then checking focus.. but this did not work becasuse the 
		// focus was being changed within the loop.
		
		if( onlyFocused == true )
			{
			if( this.focus == null  || this.focus.isEmpty() )
				{ return(0); }

			for( Long xid : this.focus )
				{
				for( Vmenu vm : this.menus )	
					{
					if( vm.getFocusId() == xid )
						{	targets.add(vm);	}
					}
				}
			}
		else
			{
			for( Vmenu vm : this.menus )
				{	targets.add(vm);	}
			}
		
		// now, actually do the operations.
		Iterator<Vmenu> it = targets.iterator();
		while( it.hasNext() )
			{
			Vmenu vm = it.next();
			if( vm.doControls(ext_keycode) )
				{ rslt++; }			
			}
		
		return(rslt);
		}

	public Long getFocus()
		{
		if( this.focus == null )
			{ return(new Long(-1)); }
		return( this.focus.get(0) );	 
		}
	
	/**  returns true if the provided menu-id has focus (in any slot).
	 * 
	 * @param id		The menu id
	 * @return	true or false
	 */
	public synchronized boolean hasFocus( Long id )
		{
		for( Long fc : this.focus )
			{
			if( fc == id )	{ return(true); }
			}
		return(false);
		}
	
	/**  Search focus slots for a particular menu id
	 * 
	 * @param id		the menu id to look for
	 * @return	a negative number if not found, else a positive int (index)
	 */
	public int hasFocusGetSlot( Long id )
		{
		int rslt = -1;
		for( int x = 0; x < this.focus.size(); x++ )
			{
			if( this.focus.get(x) == id )
				{ return(x); }
			}
		return(rslt);
		}

	/**  This can be used if there are multiple focused menus
	 * 
	 * @param index	The focus "slot"
	 * @return	The cooresponding Long menu focus ID, else -1 
	 */
	public Long getFocus( int index )
		{
		if( this.focus == null )
			{ return(new Long(-1)); }
		if( this.focus.size() < (index+1) )
			{ return(new Long(-1)); }
		return( this.focus.get(index) );	 
		}
	
	public synchronized boolean setFocus( Long id, int slot )
		{
		System.out.println(" Changed menu in slot "+
				Integer.toString(slot) + " to ID "+id.toString() );

		if( this.focus.size() <= slot )	{ return(false); }
		this.focus.set( slot, id );
		this.refreshGraphics();
		return(true);
		}

	/**
	 *    Can call this if you detect that menu focus has been totally lost.
	 */
	public synchronized void restoreSystemMenuFocus()
		{
		this.focus.set(0, this.SYSTEM_MENU_FOCUS_ID );
		return;
		}

	/**  Forces a redraw of menus onto the containing JVCL stack.
	 * 
	 * @param reverseOrder	 false draws low layers first, 
	 * false draws high layers first
	 * @return	Integer number of menus actually drawn
	 */
	public Integer paintMenus( boolean reverseOrder )
		{
		Integer rslt = 0;
		this.reverseOrder = reverseOrder;	// remember this. 
		this.targetLayerStack.JVCclearAllLayers();
		int st = 2;
		int end = this.targetLayerStack.getLayerCount();

		for( Long lookfor : this.focus )
			{
			boolean stop = false;
	
			if( reverseOrder == false )
				{
				for( int x = st; x < end; x++ )
					{
					if( stop == true )  { continue; }
					if( lookfor == 
						this.targetLayerStack.JVCmenuGetMenuFocusID(x)  )
						{
//						System.out.println(" painting focused menu "+
//								lookfor.toString() );
						
						this.targetLayerStack.JVCmenuPaintAttached(x);
						stop = true;
						rslt++;
						}
					}
				}
			else
				{
				for( int x = (end-1); x >= st; x-- )
					{
					if( stop == true )  { continue; }
					if( lookfor == 
						this.targetLayerStack.JVCmenuGetMenuFocusID(x)  )
						{
						System.out.println(" painting focused menu "+
								lookfor.toString() );
						
						this.targetLayerStack.JVCmenuPaintAttached(x);
						stop = true;
						rslt++;
						}
					}				
				}
			}
//		return( this.targetLayerStack.JVCmenuPaintAll(reverseOrder) );
		return(rslt);
		}
	public Integer paintMenus()
		{ return(this.paintMenus( this.reverseOrder )); }
	
	/** Control the order in which the drawing layers are 
	 * painted.   Normal order = the lowest layer first.  
	 * Reverse order draws highest layer first.  */
	public void setMenuPaintOrder( boolean reverseOrder )
		{  this.reverseOrder = reverseOrder;  return; }

	public synchronized JVCL getJVCL()
		{ return(this.targetLayerStack); }
	
	public BufferedImage getBufferedImage()
		{
		return( this.targetLayerStack.getBufferedImage() );
		}
	
	public void refreshGraphics()
		{
		this.targetLayerStack.JVCclearAllLayers();
		this.paintMenus();
		this.targetLayerStack.refresh();
		}

	private void loadDefaultMenus()
		{			
			// Generate the default system menu.
		VmenuVertical SYS_MENU = new VmenuVertical( 2,2 );
		
		VmenuConfirmPrompt exitConf = new VmenuConfirmPrompt(
				220, 220, 160, "This will terminate the program.", 
				"Yes, I am done.", "Return to menu" );
			// No -- return to parent.
		exitConf.getMenuItem(0).setAction(
			core.Script.getFunction( Vmenuitem.class, "goParent") );
			// Yes -- we're done.
		exitConf.getMenuItem(1).setAction(
			core.Script.getFunction( Script.class, "terminate") );

		VmiTextSimple vmix;
		vmix = new VmiTextSimple("RETURN");
		vmix.setAction( core.Script.getFunction( 
			Script.class, "menuClose") );
		SYS_MENU.addItem( vmix );
		
		vmix = new VmiTextSimple("NEW");
		vmix.setState(3);  //  Disabled
		SYS_MENU.addItem( vmix );
		
		vmix = new VmiTextSimple("SAVE");
		vmix.setState(3);  //  Disabled
		SYS_MENU.addItem( vmix );
		
		vmix = new VmiTextSimple("LOAD");
		vmix.setState(3);  //  Disabled
		SYS_MENU.addItem( vmix );
		
		vmix = new VmiTextSimple("CNFIG");
		vmix.setState(3);  //  Disabled
		SYS_MENU.addItem( vmix );
		
		// Example of a Input field.

//		VmiSimpleInput vmiy = new VmiSimpleInput("DEBUG 1", 
//				"Talk to me! ",
//				200, 200 );
//		SYS_MENU.addItem( vmiy );
		
		vmix = new VmiTextSimple("END GAME");
			// This is how you link menus.
		vmix.setAction( core.Script.getFunction( 
				Vmenuitem.class, "goChild" ) );
		SYS_MENU.addItem( vmix );
		
		SYS_MENU.setVisible(true);
		SYS_MENU.setActive(true);
		SYS_MENU.setCaption("  JVERGE  ");
		SYS_MENU.setEnableCaption(true);
		
			// Start by using a single focus slot with the system menu in it.
		this.focus = new ArrayList<Long>();
		this.focus.add( SYS_MENU.getFocusId() );
		
		Vmenu.loadStandardSounds( SYS_MENU );
		Vmenu.loadStandardSounds( exitConf );

		this.SYSTEM_MENU_FOCUS_ID = SYS_MENU.getFocusId();

		this.menus.add(SYS_MENU);
		this.menus.add(exitConf);
		this.assignToLayers();

			// Link the Quit button to a confirm prompt.
		linkMenu(this.menus.get(0), 
			this.menus.get(0).countMenuItems()-1, 
			this.menus.get(1) );
			// Link the confirm prompt back to the system menu.
		linkMenu( this.menus.get(1), 0, this.menus.get(0) );
		}

	/** Simply returns yes or no if the Controls are in INPUT mode.
	 * 
	 * @return  boolean true or false
	 */
	public static boolean isInInputMode()
		{	return( Controls.isInInputMode() );	}
	public static boolean isInMenuMode()
		{	return( Controls.isInMenuMode() );	}	
	
	/**  Update the input screen and return its BufferedImage
	 *    that is ready to be shown on screen (GUI).
	 * 
	 * @return	a BufferedImage object showing current input.
	 */
	public BufferedImage getInputImage()
		{
			// Renders the input screen to the VImage
			// Then tosses back its BufferedImage for the GUI
		return( Controls.getInputBImage( this.inputScreen ) );
		}

	public static Font getInputFont()
		{	return inFont;		}

	/** You can use this to change the Font object used to render
	 *     the input displays   A cross-platform font is used by default.
	 * @param inFont	any valid Font object.
	 */
	public static void setInFont(Font inFont)
		{	VMenuManager.inFont = inFont;	return;	}
	
		/**  Reads Controls.INPUT and passes any new items to menus.
		 * 
		 * @return	true if there was input transfered.
		 */
	public boolean transferInput()
		{
		if( Controls.hasInput() == false ) 
			{ return(false); }
		for( Long myid : Controls.obtain_input_keys() )
			{
			for( Vmenu vm : this.menus )
				{
				Vmenuitem target = vm.getMenuItemByID( myid );
				if( target != null )
					{
					target.processInput( Controls.obtain_input(myid) );
					this.paintMenus();		// must redraw
					return(true);
					}
				}
			}

		return(false);
		}
	
	/**  Sets a background image as the backdrop for all menus 
	 * currently under management by this Vmm
	 * 
	 * @param img	The VImage object 
	 * @param force	force the menus to use the backdrop immediately
	 */
	public void setUniversalMenuBackdrop( VImage img, 
			boolean force )
		{
		if( img == null )	{ return; }
		VMenuManager.MENU_BKG = img;
		for( Vmenu m : this.menus )
			{
			m.setBackgroundImage( VMenuManager.MENU_BKG, force );
			}
		return;
		}

	/** Set the menu backdrop to a given VImage
	 * 	The image is automatically scaled to fit the whole layer.
	 * 	The backdrop layer appears below all menus... like a shader. 
	 * @param theBackdrop  A VImage object.
	 */
	public void setBackdrop( VImage theBackdrop )
		{
		this.backdrop = theBackdrop;
		if( this.enableBackdrop )
			{
			this.targetLayerStack.setLayerVisible(1);
			this.targetLayerStack.setFullLayerImage(this.backdrop, 1 );
			}
		return;
		}

	/** Set the menu backdrop to a solid color with an alpha
	 * 	The backdrop layer appears below all menus... like a shader.
	 *   -untested- 
	 * @param clr	The solid color, alpha is ignored.
	 * @param theAlpha	The alpha to use.
	 */
	public void setBackdrop( Color clr, float theAlpha )
		{
		this.backdropAlpha = theAlpha;
		this.backdropColor = clr;
		
		if( this.enableBackdrop )
			{
			int xx = this.targetLayerStack.getVImage().width;
			int yy = this.targetLayerStack.getVImage().height;
			VImage tmp = new VImage(xx,yy);

			Color clrNewAlpha = new Color(
					new Float( this.backdropColor.getRed()/256 ),
					new Float( this.backdropColor.getGreen()/256 ),
					new Float( this.backdropColor.getBlue()/256 ),
					this.backdropAlpha );
			
			tmp.rectfill(0, 0, xx, yy, clrNewAlpha );
			this.targetLayerStack.setLayerVisible(1);
			this.targetLayerStack.setFullLayerImage( tmp, 1 );
			}
		return;
		}


	/** The engine calls this method to invoke privileged 
	 * 	menus to refresh constantly at the engine master clock.  
	 *          (as fast as possible).
	 *   Normally, menus paint only upon getting Input. 
	 *   
	 *    Use ONLY on menus that need animations and other high 
	 *    frequency refresh - as doing too much within this
	 *    feature WILL slow down your game easily.
	 *    
	 *    * Be careful to revoke these privilegeds for items that are 
	 * not actively being viewed !  
	 * Krybo: Mar.2017 */
	public void highFreqProcessor( Long tickNs )
		{
		if( this.hmAnimationpFreq.isEmpty() ) 
			{ return; }
		for( Long id : hmAnimationpFreq.keySet() )
			{
			this.hmAnimationpClock.put( id, 
				this.hmAnimationpClock.get( id ) - tickNs );
			boolean needsUpdate = false;
			while( this.hmAnimationpClock.get( id ) < 0 )
				{	// Wind down the clock.
				this.hmAnimationpClock.put( id, 
					this.hmAnimationpClock.get( id ) +
					this.hmAnimationpFreq.get(id) );
				needsUpdate = true;
				}
			// Do ANIMATION ACTION (once)
			if( needsUpdate )
				{
//				System.out.println( "highFreqProcessor   "+tickNs.toString() +
//						" id = " + id.toString() );
				Integer rslt = 0;
				int st = 2;
				int end = this.targetLayerStack.getLayerCount();

				for( Long lookfor : this.focus )
					{
					boolean stop = false;
			
					if( this.reverseOrder == false )
						{
						for( int x = st; x < end; x++ )
							{
							if( stop == true )  { continue; }
							if( lookfor == 
						this.targetLayerStack.JVCmenuGetMenuFocusID(x) )
								{
							this.targetLayerStack.JVCmenuAnimateAttached(x);;
								stop = true;
								rslt++;
								}
							}
						}
					else
						{
						for( int x = (end-1); x >= st; x-- )
							{
							if( stop == true )  { continue; }
							if( lookfor == 
						this.targetLayerStack.JVCmenuGetMenuFocusID(x)  )
								{
							this.targetLayerStack.JVCmenuAnimateAttached(x);;
								stop = true;
								rslt++;
								}
							}				
						}
					}
				}	// End animation action.
			}
		return;
		}

	/** Privileges a menu or menuitem with given ID to allow
	 * it to animate at high frequency, given in 2nd argument:
	 * nanoseconds per frame 
	 * Returns true if a pre-existing value was replaced, else false if new.*/
	public boolean setAnimated( Long menuId, Long FrequencyNs )
		{
		boolean rslt = false;
		if( this.hmAnimationpFreq.containsKey(menuId) )
			{ rslt = true; }
		this.hmAnimationpFreq.put( menuId, FrequencyNs );
		this.hmAnimationpClock.put(menuId, new Long(0) );
		return(rslt);
		}
	/** Stops the animation of given menuId 
	 * returns true if it was actually found and removed.  */
	public boolean revokeAnimated( Long menuId )
		{
		if( ! this.hmAnimationpFreq.containsKey(menuId) )
			{ return(false); }
		this.hmAnimationpClock.remove( menuId );
		this.hmAnimationpFreq.remove( menuId );
		return(true);
		}

	public void resetAnimationTimers()
		{
		for( Long id : hmAnimationpClock.keySet() )
			{
			this.hmAnimationpClock.put(id, new Long(0) );
			}
		}

	public void resetAnimationTimers( Long id )
		{
		this.hmAnimationpClock.put( id, new Long(0) );
		return;
		}

	}


