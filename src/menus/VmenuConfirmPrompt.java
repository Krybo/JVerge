package menus;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;

import menus.Vmenuitem.enumMenuItemSTATE;
import core.Controls;
import domain.VImage;
import domain.VSound;

public class VmenuConfirmPrompt implements Vmenu
	{
	HashMap<String,String> content = new HashMap<String,String>();
	Long focusID = new Long(-1);
	int x = 0,y = 0;
	int sx = 0, sy = 0, sline = 1, smargin = 10;
	int maxDialogWidthPx = 80;
	int framewidth = 3;
	Integer selectedIndex = -1;		// selected menuitem in the menu.
	boolean isActive = false;
	boolean isVisible = false;
	private Font fnt = core.Script.fntMASTER;
//	private boolean enableCaption = false;
//	private VmiTextSimple caption;
	private Color bkgColor;
	private VImage bkgImage;
	private ArrayList<String> multiLineDialog = new ArrayList<String>();
	private VmiTextSimple vmiYes = new VmiTextSimple("YES");
	private VmiTextSimple vmiNo = new VmiTextSimple("NO");
	private HashMap<enumMenuEVENT,VSound> hmSounds;
	private Long parentID = new Long(-1);
	private Long childID = new Long(-1);

	public VmenuConfirmPrompt( int x, int y, int width,
			String myDialog, String positiveCaption, String negativeCaption )
		{
		this.x = x;
		this.y = y;

		this.maxDialogWidthPx = width;
		this.content.put("dialog", myDialog );
		this.parseDialog();
		this.framewidth = 3;

		this.content.put( this.vmiYes.getText(), positiveCaption );
		this.content.put( this.vmiNo.getText() , negativeCaption );

		this.bkgColor = new Color( 0.0f,0.0f,0.0f,1.0f );

		this.selectedIndex = new Integer(0);
		this.vmiNo.setState(enumMenuItemSTATE.SELECTED.value() );
		this.vmiYes.setState(enumMenuItemSTATE.NORMAL.value() );
		this.refresh();
		
		this.hmSounds = new HashMap<enumMenuEVENT,VSound>();
		
		this.focusID = Vmenu.getRandomID();
		return;
		}

	public boolean paint(VImage target )
		{
		int targetWidth = target.width;
		int targetHeight = target.height;
		this.calcTextArea();

			// Calc bounds
		int bx1 = this.x - (this.maxDialogWidthPx/2) - this.smargin;
		int bx2 = this.x + (this.maxDialogWidthPx/2) + this.smargin;
		int by1 = this.y;
		int by2 = by1 + (this.smargin*4) + 
				this.sy+this.vmiNo.getDY().intValue();

		// Save some cpu cycles if its requested to draw offscreen.....
		if( bx1 > targetWidth )	{ return(false); }
		if( by1 > targetHeight )	{ return(false); }

			// Body
		if( this.bkgImage != null )
			{
			target.scaleblit(bx1, by1, bx2, by2, this.bkgImage );
			}
		else
			{
			target.rectfill(bx1,by1,bx2,by2, this.bkgColor );	
			}

			// Border frame
		for( int n = 0; n < this.framewidth; n++ )
			{
			target.rect(bx1+n, by1+n, bx2-n, by2-n, Color.WHITE );
			target.setPixel(bx1, by1, new Color(0.0f,0.0f,0.0f,0.0f) );
			target.setPixel(bx2, by1, new Color(0.0f,0.0f,0.0f,0.0f) );
			target.setPixel(bx1, by2, new Color(0.0f,0.0f,0.0f,0.0f) );
			target.setPixel(bx1, by2, new Color(0.0f,0.0f,0.0f,0.0f) );
			}

		int lh = this.sy / this.sline;		// rederive line height [px]
		int ln = 0;
		for( String s : this.multiLineDialog )
			{
			target.printString( bx1 + this.smargin, 
					by1 + this.smargin + (ln * lh), 
					this.fnt, s );
			ln++;
			}

		this.vmiNo.paint( target );
		this.vmiYes.paint( target );
		
		int tipY = by1 + (this.smargin*3) + (multiLineDialog.size() * lh) 
				+ this.vmiNo.getDY().intValue();
		String tipKey = this.vmiNo.getText();
		if( this.selectedIndex == 1 )
			{  tipKey = this.vmiYes.getText(); }
		if( ! this.content.get( tipKey ).isEmpty() )
			{
			target.printString( bx1 + this.smargin, 
				tipY, this.fnt,	this.content.get(tipKey) );
			}
		
//		this.content.get( this.selectedIndex )

		return(true);
		}

	/**  Parses the desired dialog into multiple parts
	 *    depending on the box-space available for it.
	 *    Should be run every time the dialog text changes.
	 */
	private void parseDialog()
		{
		String text = this.content.get("dialog");
		String[] words = text.split(" ");
		
		AffineTransform affinetransform = new AffineTransform();     
		FontRenderContext frc = 
				new FontRenderContext(affinetransform,true,true);
		this.sx = (int)( this.fnt.getStringBounds(text, frc).getWidth());
		int pxPerChar = this.sx / text.length();
		int charPerLine = this.maxDialogWidthPx / pxPerChar;
		
		ArrayList<String> arWords = new ArrayList<String>();
		for(int x = 0; x < words.length; x++)
			{
			arWords.add(words[x]);
			}
		String ln = new String("");
		while( ! arWords.isEmpty() )
			{
				// Going to go over-bound
				// start new line.
			if( ln.length() + arWords.get(0).length() > charPerLine  )
				{
				this.multiLineDialog.add( ln );
				ln = arWords.remove(0)+" ";
				}
			else		// build line..
				{
				ln = ln.concat( arWords.remove(0)+" " );
//				if( ! arWords.isEmpty() )
//					{ ln = ln.concat( " " ); }
				}
			}
		
		ln.trim();
		if( ln.length() != 0  )
			{	this.multiLineDialog.add( ln );		}
		
		}
	
	private void calcTextArea()
		{
	// Thank you : http://stackoverflow.com/questions/258486/calculate-the-display-width-of-a-string-in-java
		String text = this.content.get("dialog");
		this.sline = this.multiLineDialog.size();
		if( text == null || text.length() == 0 || text.isEmpty() 
				|| this.sline <= 0 )
			{
			this.sx = 0;   this.sy = 0;
			this.sline = 1;
			return;
			}
		AffineTransform affinetransform = new AffineTransform();     
		FontRenderContext frc = 
				new FontRenderContext(affinetransform,true,true);

		
		this.sx = (int)( this.fnt.getStringBounds(text, frc).getWidth());
		this.sy = Math.abs( 
				(int) this.fnt.getStringBounds(text, frc).getMinY() );
		this.smargin = this.sy;

		if( this.sline > 1 )
			{
			this.sx = maxDialogWidthPx;
			this.sy = this.sy * this.sline;
			}
		else
			{	this.sline = 1;	}
		return;
		}
	
	public boolean doControls( Integer kc )
		{
		boolean redraw = false;
		
		if( kc <= -1 )		// fake keystroke.   cause redraw
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

				// Close and/or return control to parent.
			case 10:			// ENTER KEY <CONFIRM>
				this.funcActivate();
				redraw=true;
				break;


				// : set negative state.
			case 8:		// BACKSPACE <CANCEL>
			case 37:		// ARROW-UP 
			case 38:  		// ARROW-LEFT

				if( isShift || isCntl )	{ break; }
				redraw = true;
				if( this.selectedIndex != 0 )
					{
					this.selectedIndex = 0;
					this.vmiNo.setState(enumMenuItemSTATE.SELECTED.value() );
					this.vmiYes.setState(enumMenuItemSTATE.NORMAL.value() );
					this.playMenuSound(enumMenuEVENT.MOVE, 33 );
					}	
				break;

				// (Change to positive) state
			case 32:	// SPACE BAR
			case 39:	// ARROW-RIGHT	
			case 40:	// ARROW-DOWN

				if( isShift || isCntl )	{ break; }
				redraw = true;
	
				if( this.selectedIndex != 1 )
					{
					this.selectedIndex = 1;
					this.vmiYes.setState(enumMenuItemSTATE.SELECTED.value() );
					this.vmiNo.setState(enumMenuItemSTATE.NORMAL.value() );
					this.playMenuSound(enumMenuEVENT.MOVE, 33 );
					}	
				break;

			default:
				System.out.println(" unhandled prompt keystroke ["
					+kc.toString()+" ]  Base <"+basecode.toString()+"> " );
				break;
			}

		if( redraw )
			{	this.resolvePositions();	}
		return(redraw);

		}

	private void resolvePositions()
		{
		int buttonX = this.maxDialogWidthPx/2;
		int buttonY = this.sy + this.smargin;
		
		this.vmiYes.reposition( this.x, this.y, 
			this.smargin, buttonY );
		this.vmiNo.reposition( this.x, this.y, 
			this.smargin - buttonX, buttonY );
		
		this.vmiYes.setExtendX( (this.maxDialogWidthPx/2)-20, false );
		this.vmiNo.setExtendX( (this.maxDialogWidthPx/2)-20, false );
		return;
		}

	private void funcActivate()
		{
		if( this.selectedIndex == 0 )
			{ this.vmiNo.doAction(); }
		if( this.selectedIndex == 1 )
			{ this.vmiYes.doAction(); }
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
		{	return(new Integer(2));	}

	public Integer addItem(Vmenuitem vmi)
		{	return(0);	}

	public Vmenuitem popItem()
		{	return null;	}

	public Vmenuitem removeItem(int index)
		{	return null;	}

	public Integer insertItem( Vmenuitem vmi, int index)
		{	return null;	}

	public void refresh()
		{
		this.calcTextArea();
		this.resolvePositions();
		return;
		}

	public Vmenuitem getMenuItem(int index)
		{
		if( index == 0 )	{ return(this.vmiNo); }
		if( index == 1 )	{ return(this.vmiYes); }
		return null;
		}
	
	/**  Given a Long menu id , checks all menuItems for that ID
	 * 	If so, Returns the menu item object.   Always check for null.
	 */
	public Vmenuitem getMenuItemByID( Long id )
		{
		if( this.vmiNo.getId() == id )		{ return( this.vmiNo); }
		if( this.vmiYes.getId() == id )	{ return( this.vmiYes); }
		return null;
		}

	public Integer getSelectedIndex()
		{	return this.selectedIndex;	}

	public int getSelectedIndexPosX()
		{
		if( this.selectedIndex == 0 )
			{ return(this.vmiNo).getX().intValue(); }
		if( this.selectedIndex == 1 )
			{ return(this.vmiYes).getX().intValue(); }
		return 0;
		}

	public int getSelectedIndexPosY()
		{
		if( this.selectedIndex == 0 )
			{ return(this.vmiNo).getY().intValue(); }
		if( this.selectedIndex == 1 )
			{ return(this.vmiYes).getY().intValue(); }
		return 0;
		}

	public void setSelectedIndex(Integer index)
		{	this.selectedIndex = index;	}

	public void setFocusId(Long id)
		{	this.focusID = id;	}

	public Long getFocusId()
		{	return this.focusID;	}

	public boolean isFocus(Long id)
		{
		if( this.focusID == id )		{ return(true); }
		return false;
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

	public boolean isActive()
		{	return this.isActive;	}

	public boolean isVisible()
		{	return this.isVisible;	}

//	public int addSubmenus(Vmenu slave)
//		{
//		this.submenus.add(slave);
//		return(this.submenus.size());
//		}

//	public Vmenu getSubmenus( Integer submenuIndex)
//		{
//		if( this.hasSubmenus() == false )   { return(null);  }
//		if( submenuIndex >= this.submenus.size() )   { return(null);  }
//		return this.submenus.get(submenuIndex);
//		}

//	public boolean setSubmenus(Vmenu slave, Integer index)
//		{
//		if( index >= this.submenus.size() || index < 0 )
//			{
//			this.submenus.add(slave);
//			return(false);
//			}
//		this.submenus.add(index, slave );
//		return true;
//		}

//	public Vmenu popSubmenus()
//		{
//		if( this.hasSubmenus() == false )   {  return(null); }
//		return this.submenus.remove( this.submenus.size()-1 );
//		}

//	public boolean hasSubmenus()
//		{
//		if( this.submenus.isEmpty() )  { return(false); }
//		return true;
//		}

//	public int countSubmenus()
//		{
//		if( this.hasSubmenus() == false ) { return(0); }
//		return(this.submenus.size());
//		}

	public void activateSelected()
		{
		if( this.selectedIndex == 0 )
			{  this.vmiNo.doAction(); }
		if( this.selectedIndex == 1 )
			{  this.vmiYes.doAction(); }
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

	public void setIconsAll( boolean onOff )
		{
		this.vmiNo.enableIcons(onOff);
		this.vmiYes.enableIcons(onOff);
		return;
		}
	public void setBorderAll( boolean onOff, int thick )
		{
		this.vmiNo.enableFrame(onOff);
		this.vmiNo.setFrameThicknessPx(thick);
		this.vmiYes.enableFrame(onOff);
		this.vmiYes.setFrameThicknessPx(thick);
		return;
		}
	
	public void setParentID( Long id )
		{
		this.parentID = id;	
		this.vmiYes.setParentID(id);
		this.vmiNo.setParentID(id);
		}
	public void setChildID( Long id )
		{
		this.childID = id;
		this.vmiYes.setChildID(id);
		this.vmiNo.setChildID(id);
		}
	public Long getParentID()
		{	return(this.parentID);	}
	public Long getChildID()
		{	return(this.childID);	}
	
	}
