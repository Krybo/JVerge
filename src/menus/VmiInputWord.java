package menus;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
*   user input.  It is just an extention of VmiTextSimple with input capability
*   added in.  This type accepts only letters - no characters or numbers.
* @author Krybo
*
*/

public class VmiInputWord extends VmiSimpleInput implements Vmenuitem
	{

	public VmiInputWord(String initialText, String caption, int relX, int relY)
		{
		super(initialText, caption, relX, relY);
		return;
		}

	public Long doInput()
		{
		Controls.begin_input( this.caption,  super.getId(),
			super.getX().intValue(), super.getY().intValue(),
			false, false, true, false, false );
		return(super.getId());
		}
	
	}
