package menus;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
*   user input.  It is just an extention of VmiTextSimple with input capability
*   added in.  This type accepts anything but high and control characters.
*   That includes all letters, numbers and special chars 
*   
* @author Krybo
*
*/

public class VmiInputGeneral extends VmiSimpleInput implements Vmenuitem
	{

	public VmiInputGeneral(String initialText, String caption, int relX,
			int relY)
		{
		super(initialText, caption, relX, relY);
		return;
		}

	public Long doInput()
		{
		Controls.begin_input( this.caption,  super.getId(),
			super.getX().intValue(), super.getY().intValue(),
			true, false, true, true, false );
		return(super.getId());
		}

	}
