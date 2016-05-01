package menus;

import java.awt.Color;
import java.util.HashMap;

import menus.Vmenuitem.enumMenuItemSTATE;
import core.Controls;
import domain.VImage;

/**  Similar to VmenuVertical.. because it extends it... This Vmenu
 * mplementation automatically arranges Vmenuitem implemented
 * objects into a horizontal layout.   Good small width items such as
 * buttons.   And run controls appropriately through 
 * them.   Make sure you create a way to return to the originating menus,
 * if there is one -- Either in doControls or a doAction() of one of the items.
 * (Apr.2016)
 * 
 * @author Krybo
 *
 */

public class VmenuHorizontal 
		extends VmenuVertical 
		implements Vmenu
	{

	public VmenuHorizontal()
		{	super();	}

	public VmenuHorizontal(int x, int y)
		{
		super(x, y);
		}

	protected void resolvePositions()
		{
		int maxh = 0;
		int iX = 0;
		int head = 0;
		int hi;			// running count of horizontal pixels.
		int curX = super.getX();
		int curY = super.getY();
		
		if( super.isEnableCaption() == true )
			{
			head = super.getCaptionObject().getDY().intValue();
			super.getCaptionObject().reposition( curX,curY,0,0 );
			}
		// Analyse the maximum height in all items.
		for( Vmenuitem vmi : super.getContent() )
			{
			if( vmi.isActive() == false )
				{	continue;	}
			if( vmi.getDX().intValue() > maxh )
				{
				maxh = vmi.getDX().intValue();
				}
			}

		// Keep content Y the same, add each items height to create vertical menu.
		for( Vmenuitem vmi : super.getContent() )
			{
			if( vmi.isActive() == false )
				{	continue;	}
			hi = vmi.getDX().intValue();
			vmi.setExtendY( maxh, true );
			vmi.reposition( curX, curY , iX, head );
			iX += hi;
			}

		super.getCaptionObject().setExtendX( iX, false);
		super.setWidth( iX );
		super.setHeight( head+maxh );

		return;
		}

	public boolean paint( VImage target )
		{
		// System.out.println("VerticalMenu draw called.");
		if( super.isVisible() == false )
			{	return (false);	}
		if( target == null )
			{	return (false);	}

		int curX = super.getX();
		int curY = super.getY();
		int curW = super.getWidth();
		int curH = super.getHeight();
		super.refresh();

		if( super.isEnableImgBackground() == true )
			{
			target.scaleblit( curX, curY, curW, curH, super.getBkgImage());
			}

		if( super.isEnableCaption() == true )
			{
			super.getCaptionObject().paint(target);
			}

		int counter = -1;
		for( Vmenuitem myvmi : super.getContent() )
			{
			counter = counter + 1;
			if( myvmi.isVisible() == false )
				{	continue;  }
			myvmi.paint(target);
			}
		return true;
		}

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

		switch( basecode )
			{
			case 8: // BACKSPACE <CANCEL>
				super.getSelectedMenuItem().setState(
						enumMenuItemSTATE.NORMAL.value());
				this.playMenuSound(enumMenuEVENT.CANCEL, 33);
				redraw = true;
				break;
			case 10: 		// ENTER KEY <CONFIRM>
				super.funcActivate();
				redraw = true;
				break;
			case 35:		// home/end
			case 36:
			case 32: 		// SPACE BAR
				break;
			case 33: 		// Page UP
				redraw=true;
				super.moveSelection(super.countMenuItems()*-1, false );
				break;
			case 34:		// page Down
				redraw=true;
				super.moveSelection(super.countMenuItems(), false );
				break;
			case 38: // ARROW-UP
				if (isShift)		{	break;  	}
				redraw = true;
				if (isCntl)
					{
					super.moveRel( 0, -1 );
					if( super.getY() < 0 )
						{	super.moveAbs( super.getX(), 0 );	}
					}
				break;
			case 37: // ARROW-LEFT
				if (isShift)		{	break;  	}
				redraw = true;
				if (isCntl)
					{
					super.moveRel(-1, 0);
					if( super.getX() < 0 )
						{	super.moveAbs(0, super.getY() );	}
					break;
					}
				super.moveSelection( -1, true );
				// Ensure new selection is active.
				if (this.getActiveItemCount() == 0) 
					{	break;	}		// infinite loop protection
				while( super.isSelectionActive() == false )
					{	super.moveSelection( -1, true );	}

				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;
			case 40: // ARROW-DOWN
				if (isShift)		{	break;  	}
				redraw = true;
				if (isCntl)
					{	super.moveRel( 0, 1 );	}
				break;
			case 39: // ARROW-RIGHT
				if (isShift)		{	break;  	}
				redraw = true;
				if (isCntl)
					{	
					super.moveRel( 1, 0);
					break;
					}
				super.moveSelection( +1, true );
				// Ensure new selection is active.
				if (this.getActiveItemCount() == 0) 
					{	break;	}		// infinite loop protection
				while( super.isSelectionActive() == false )
					{
					super.moveSelection( +1, true );
					}

				this.playMenuSound(enumMenuEVENT.MOVE, 33);
				break;
			default:
				System.out.println(" unhandled menu keystroke ["
						+ kc.toString() + " ]  Base <"
						+ basecode.toString() + "> ");
				break;
			}

		if (redraw)
			{	this.resolvePositions();	}
		return (redraw);
		}

	}		// END CLASS
