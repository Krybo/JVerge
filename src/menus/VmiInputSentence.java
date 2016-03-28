package menus;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
*   user input.  It is just an extention of VmiTextSimple with input capability
*   added in.  This type accepts letters and characters.   Enough to form a 
*   sentence.
*   
* @author Krybo
*
*/

public class VmiInputSentence extends VmiSimpleInput implements Vmenuitem
	{

	public VmiInputSentence(String initialText, String caption, int relX,
			int relY)
		{
		super(initialText, caption, relX, relY);
		return;
		}

	public Long doInput()
		{
		Controls.begin_input( this.caption,  super.getId(),
			super.getX().intValue(), super.getY().intValue(),
			false, false, true, true, false );
		return(super.getId());
		}
	
	}
