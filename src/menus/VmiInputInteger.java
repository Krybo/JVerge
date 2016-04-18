package menus;

import static core.Script.log;
import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
 *   user input.  It is just an extention of VmiTextSimple with input capability
 *   added in.  This type accepts only an integer.
 * @author Krybo
 *
 */

public class VmiInputInteger extends VmiSimpleInput 
		implements Vmenuitem
	{

	public VmiInputInteger(String initialText, String caption, int relX,
			int relY)
		{
		super(initialText, caption, relX, relY);
		return;
		}
	
	public Long doInput()
		{
		Controls.begin_input( this.caption,  super.getId(),
			super.getX().intValue(), super.getY().intValue(),
			true, false, false, false, false );
		return(super.getId());
		}

	/**  Process the input value into the appropriate type.
	 * 
	 * @return  An integer from the last input.
	 */
	public Integer getInput()
		{
		Integer result = 0;
		try { result = Integer.parseInt( super.getText() );	}
		catch( NumberFormatException e ) 
		   { log("WARN: Integer input box has invalid Value. returned zero."); }
		return(result);
		}

	}
