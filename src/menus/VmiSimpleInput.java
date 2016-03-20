package menus;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
 *   user input.  It is just an extention of VmiTextSimple with input capability
 *   added in.
 * @author Krybo
 *
 */

public class VmiSimpleInput extends VmiTextSimple 
		implements Vmenuitem
	{

	String caption = new String("Enter new value:");
	
	public VmiSimpleInput( String initialText, 
			String caption, int relX, int relY)
		{
		super( initialText, relX, relY);
		this.caption = caption;
		super.setAction( core.Script.getFunction( 
				VmiSimpleInput.class, "doInput" ) );
		return;
		}

	public String doInput()
		{
		Controls.begin_input( this.caption, 
			super.getX().intValue(), super.getY().intValue(),
			true, true, true, false, false );
		return(new String("TEST"));
		}

	}
