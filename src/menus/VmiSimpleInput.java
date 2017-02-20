package menus;

import java.util.Stack;

import core.Controls;

/** Class VmiSimpleInput is a Vmenuitem that is used for collecting
 *   user input.  It is just an extention of VmiTextSimple with input capability.
 *   This base class accepts any input characters indescriminatnly.
 *   Utilizes a java Stack to hold multiple input data entries.
 *   Intended to be polymorphed into more complex, specialized objects.
 * @author Krybo
 *
 */

public class VmiSimpleInput extends VmiTextSimple 
		implements Vmenuitem
	{

	String caption = new String("Enter new value:");
	Stack<String> inputdata =  new Stack<String>();

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

	/**  Takes in an external string
	 *   by default, replaces the current text with it.
	 */
	public void processInput( String input )
		{
		super.processInput(input);
		Integer x = this.addData(input);
		System.out.println(" VmiSimpleInput stored data # "+ x.toString() );
		return;
		}
	
	public int addData( String data )
		{
		this.inputdata.push(data);
		return( this.inputdata.size() );
		}
	
	public String shiftData( )
		{
		if( this.inputdata.empty() == true )
			{ return(null); }
		return( this.inputdata.remove(0) );
		}

	public String popData( )
		{
		if( this.inputdata.empty() == true )
			{ return(null); }
		return( this.inputdata.pop() );
		}
	
	public boolean hasData()
		{ return( ! this.inputdata.empty() ); }
	
	}
