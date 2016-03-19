package menus;

import static core.Script.load;
import static core.Script.log;
import static core.Script.screen;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import menus.Vmenu.enumMenuEVENT;
import core.JVCL;
import core.Script;
import domain.VImage;
import domain.VSound;

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
	private Color backdropColor = new Color( 0.0f,0.0f,0.0f,0.5f );
	private float backdropAlpha = 0.5f;
	protected static HashMap<Long,Long> hmLinkList =
			new HashMap<Long,Long>();
	private JVCL targetLayerStack = null;
	
	
	public VMenuManager(  )
		{
			// Make a new JVCL stack for the menus system.
		this.targetLayerStack = 
				new JVCL(24,screen.width,screen.height); 

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
		vmChild.setParentID( vmParent.getFocusId() );
		vmParent.getMenuItem( parentMenuItemNumber ).setChildID(
				vmChild.getFocusId() );

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
//		System.out.println(" DEBUG : entered delegateControl " );
		
		if( onlyFocused == true )
			{
			if( this.focus == null  || this.focus.isEmpty() )
				{ return(0); }

			for( Long xid : this.focus )
				{
				for( Vmenu vm : this.menus )	
					{
					if( vm.getFocusId() == xid )
						{
//						System.out.println("Sent keycode " + 
//								Integer.toString(ext_keycode) + " to menu id "+
//								vm.getFocusId().toString() );

						if( vm.doControls(ext_keycode) )
							{ rslt++; }						
						}
					}
				}
			
			// Attempted to do this the other way, looping over menu
			// then checking focus.. but this did not work becasuse the 
			// focus was being changed within the loop.
			
			}
		else
			{
			for( Vmenu vm : this.menus )
				{
//				System.out.println("Sent * keycode " + 
//					Integer.toString(ext_keycode) + " to menu id "+
//					vm.getFocusId().toString() );
				
				if( vm.doControls(ext_keycode) )
					{ rslt++; }
				}
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
		return(-1);
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

	/**  Forces a redraw of menus to the containing JVCL stack.
	 * 
	 * @param reverseOrder	 false draws low layers first, 
	 * false draws high layers first
	 * @return	Integer number of menus actually drawn
	 */
	public Integer paintMenus( boolean reverseOrder )
		{
		Integer rslt = 0;
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
		{ return(this.paintMenus(false)); }

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
		
		vmix = new VmiTextSimple("END GAME");
			// This is how you link menus.
		vmix.setAction( core.Script.getFunction( 
				Vmenuitem.class, "goChild" ) );
		SYS_MENU.addItem( vmix );
		
		SYS_MENU.setVisible(true);
		SYS_MENU.setActive(true);
		SYS_MENU.setCaption("  JVERGE  ");
		SYS_MENU.setCaptionVisible(true);
		
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
		linkMenu(this.menus.get(0), 5, this.menus.get(1) );
			// Link the confirm prompt back to the system menu.
		linkMenu( this.menus.get(1), 0, this.menus.get(0) );
		}

	}
