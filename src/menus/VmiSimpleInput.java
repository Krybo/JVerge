package menus;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
 *   user input.  It is just an extention of VmiTextSimple with input capability
 *   added in.  This base class accepts any input characters indescriminatnly.
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

	public Long doInput()
		{
		Controls.begin_input( this.caption,  super.getId(),
			super.getX().intValue(), super.getY().intValue(),
			true, true, true, true, true );
		return( super.getId()  );
		}

	}
