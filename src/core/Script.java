package core;

import static core.VergeEngine.*;
import static core.Controls.*;
import static core.VergeEngine.Vmm;	// The Verge menu manager.
import core.JVCL;









//import java.awt.AlphaComposite;
//import java.awt.Font;
//import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
//import java.net.ServerSocket;
//import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import menus.Vmenu;
import menus.Vmenuitem;
import audio.VMusic;
import domain.MapVerge;
import domain.VSound;
import domain.VImage;
import domain.Entity;
import domain.Map;
import static core.SinCos.*;


public class Script {

	// For Testing purposes (simulations)
	public static boolean TEST_SIMULATION = false;
	public static int TEST_POS = 0;
	public static int[] TEST_OPTIONS;



		// Krybo (Feb.2016) : Some Global Fonts
		// This one is guarenteed to work anywhere and used as backup
		//  if fancy fonts are not avbl on a particular machine.
	public static Font fntMASTER =
			new Font("Monospaced", Font.PLAIN, 14 );
		// Plain, Large, easy on the eyes, textbox() kinda font.
	public static Font fntVERGE = 
			new Font("Tahoma",Font.PLAIN, 18 );
		// good console font
	public static Font fntCONSOLE = 
			new Font("Consolas",Font.PLAIN, 12 );
	
	//public static final int VCFILES		=		51;
	public static final int  VC_READ	=			1;
	public static final int  VC_WRITE		=	2;
	public static final int  VC_WRITE_APPEND	=	3; // Overkill (2006-07-05): Append mode added.	
	
	

	public static final int CF_GRAY = 1;
	public static final int CF_INV_GRAY = 2;
	public static final int CF_INV = 3;
	public static final int CF_RED = 4;
	public static final int CF_GREEN = 5;
	public static final int CF_BLUE = 6;
	public static final int CF_CUSTOM = 7;
	public static final Color Color_DEATH_MAGENTA = 
			new Color( 1.0f , 0.0f , 1.0f , 0.0f );
	public static final Color Color_CLEAR = 
			new Color( 0.0f , 0.0f , 0.0f , 0.0f );

		// The java vergeC graphics layers are to be controlled using 
		//		core.Script functions and a few rendering engine ties.
		// 	User graphics layers are drawn over the map layers.
		//   Menus are drawn over map and graphics.
		//  		[Map] < [graphics & text] < [menu]
	public static JVCL jvcl;
	public static JVCL jvclText;

	// VERGE ENGINE VARIABLES: Moved to Script for easy of use
	/**
	 * This is a hardcoded image handle for the screen. It is a pointer to a
	 * bitmap of the screen's current dimensions (set in verge.cfg or by
	 * SetResolution() function at runtime). Anything you want to appear in the
	 * verge window should be blitted here with one of the graphics functions.
	 * When ShowPage() is called the screen bitmap is transfered to the display.
	 */
	public static VImage screen, screenZOOM;
		// Krybo (2014-09-19) added these for map zoom.
	public static VImage ZoomScreenSubset;		// needs reinit upon zoom change
	public static int screenHalfWidth = -1;
	public static int screenHalfHeight = -1;
	
	// For partial screen rendering
	static VImage virtualScreen = null;

	/** read-only timer variable constantly increasing*/
	public static int systemtime;

	/** read/write timer variable*/
	public static int timer;

	// internal use only
	static int vctimer = 0; // [Rafael, the Esper]
	static int hooktimer = 0;

	public static List<Entity> entity = new ArrayList<Entity>();
	public static Entity myself = null;

	/**
	 * The number of entities currently on the map. Use this an the upper bound
	 * any time you need to loop through and check entites for something.
	 */
	public static int numentities;

	public static int player;
	public static int playerstep = 1;
	public static boolean playerdiagonals = true;
	public static boolean smoothdiagonals = true; // [Rafael, the Esper]

	public static int xwin, ywin;
	public static Map current_map = null;

	public static int cameratracking = 1;
	public static int cameratracker = 0;
	public static int lastplayerdir = 0;

	public static boolean entitiespaused = false;
		// Krybo (2014-09-18) for V1 VC layer emulation
	public static boolean vcLayerEmulation = false;	
	
	// END OF VERGE ENGINE VARIABLES

	public static String renderfunc, timerfunc; // was VergeCallback (struct)

	public static DefaultPalette palette = new DefaultPalette();
	public static int transcolor = -65281; // Color(255, 0, 255);
	public static int currentLucent = 255;
	
	public static int event_tx;
	public static int event_ty;
	public static int event_zone;
	public static int event_entity;
	public static int event_param;
	public static int event_sprite;
	public static int event_entity_hit;

	public static int __grue_actor_index;
	
	private static VMusic musicplayer; // [Rafael, the Esper] 
	
	public static int invc;

	public static String _trigger_onStep = "", _trigger_afterStep = "";
	public static String _trigger_beforeEntityScript = "", _trigger_afterEntityScript = "";
	public static String _trigger_onEntityCollide = "";
	public static String _trigger_afterPlayerMove = "";

	public static int vc_GetYear()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.YEAR);
	}

	public static int vc_GetMonth()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.MONTH);

	}

	public static int vc_GetDay()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.DAY_OF_MONTH);
	}

	public static int vc_GetDayOfWeek()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.DAY_OF_WEEK);
	}

	public static int vc_GetHour()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.HOUR);
	}

	public static int vc_GetMinute()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.MINUTE);
	}

	public static int vc_GetSecond()
	{
		  Calendar cal=Calendar.getInstance();
		  return cal.get(Calendar.SECOND);
	}


	public static void error(String str) { 
	  	System.err.println(str);
	}

	static void hooktimer()
	{
		// To prevent hooktimer from happening before the script engine is loaded.
		//if(se == null) return;

		while (hooktimer != 0)
		{
			callfunction(timerfunc);
			hooktimer--;
		}
	}

	public static void hooktimer(String cb) {
		hooktimer = 0;
		timerfunc = cb;
	}
	
	public static void hookretrace()
	{
		if(renderfunc != null) {
			callfunction(renderfunc);
		}
	}

	public static void hookretrace(String cb) {
		renderfunc = cb;
	}

	
	// Rafael: Changed to ExecuteFunctionString 
	/*public void ExecuteCallback(String function, boolean callingFromLibrary) */

	// Silent engine termination.
	public static void terminate() 
		{ 	System.exit(0); 	}
	public static void exit(String message) 
		{ 
		// TODO:  any emergency cleanup before exiting.
		System.err.println(message);
		terminate(); 
		}

	/* Rafael: TODO Implement this.
	 public static void SetButtonJB(int b, int jb) {
		switch (b)
		{
			case 1: j_b1 = jb; break;
			case 2: j_b2 = jb; break;
			case 3: j_b3 = jb; break;
			case 4: j_b4 = jb; break;
		}
	}*/

	// Overkill (2007-08-25): HookButton is supposed to start at 1, not 0.
	// It's meant to be consistent with Unpress().
	public static void hookbutton(int b, String s) {
		if (b<1 || b>4) return;
		bindbutton[b-1] = s;
	}

	public static void hookkey(int k, String s) {
		if (k<0 || k>127) return;
		bindarray[k] = s;
	}
	
	// Krybo ( Feb.2016)
	// Instances a very simple square symetric cursor - size controlable.
	//   Has a circle (color 3), X (color 2) and a + (Color 1) overlayed.
	public static VImage createCursorImage( int squareSizePixels,
			Color c1, Color c2, Color c3 )
		{
		VImage theCursor = new VImage(squareSizePixels, squareSizePixels);
		theCursor.rectfill(0, 0, squareSizePixels-1, squareSizePixels-1, 
				Color_DEATH_MAGENTA );
		
		Double ptCenter = new Double( squareSizePixels / 2 );
		Double ptCRAD = new Double( ptCenter / 2.0d );
		Double priIndent = new Double( squareSizePixels * 0.20d );
		
		// Circle.
		theCursor.circle(ptCenter.intValue(), ptCenter.intValue(), 
				ptCRAD.intValue(), ptCRAD.intValue(), c3, theCursor );
		// X
		theCursor.line( priIndent.intValue()+1, 
			priIndent.intValue()+1, 
			squareSizePixels - priIndent.intValue() - 1, 
			squareSizePixels - priIndent.intValue() - 1, c2 );
		theCursor.line( squareSizePixels - priIndent.intValue() - 1, 
			priIndent.intValue()+1, 
			priIndent.intValue()+1, 
			squareSizePixels - priIndent.intValue() - 1, c2 );
		// plus
		theCursor.line( priIndent.intValue(), ptCenter.intValue(), 
			squareSizePixels - priIndent.intValue(), ptCenter.intValue(), c1);
		theCursor.line( ptCenter.intValue() , priIndent.intValue() ,  
			ptCenter.intValue(), squareSizePixels - priIndent.intValue(), c1 );
		
		return(theCursor);
		}

	/* Krybo:   June-2015
	 * I made this because hookkey() was executing target method way
	 * too fast (thousands of times a second) and I needed a way to
	 * throttle it. A waitKeyUp() within the target method seemed to 
	 * cause problems.  
	 * -- Each key may have its own throttle level in nanoseconds. -- 10 digits
	 * 1000000000 = 1 Second limit
	 * 500000000 = Half a second
	 * 10000000 = 100th of a second (commonly traditionally)
	 *    settings much below 100000 probably won't be useful
	 *    as "noise" in the machines background makes it inconsistant.
	 *    
	 *    It WILL NOT cause target method to execute *exactly* "delay"
	 *    nanoseconds frequency.   Think of it more like a minimal throttle
	 *    
	 *    I am not sure what this will do on systems that do not support
	 *    high precision timers  - specificially - System.nanoTime()
	 * */

	public static void nicedHookkey(int k, String s, long delay ) 
		{
		if (k<0 || k>127) return;
		if( delay < 0 ) { delay = 0; }
		bindarray[k] = s;
		bindarrayDelay[k] = delay;
		bindarrayCounter[k] = System.nanoTime();
		}

	/**
	 * opens & closes menus
	 * Krybo (Mar.2016)
	 */
	public static void menuOpen()
		{ 
		Controls.changeMenuMode(true);
		Vmm.paintMenus();
//		jvclMenu.JVCmenuPaintAll(false);
		}
	public static void menuClose()
		{ Controls.changeMenuMode(false); }
	public static void menuToggle()
		{ Controls.changeMenuMode(); }
	
	
	public static void log(String s) { 
		System.out.println(s); 
	}

	/*
	static void MessageBox(String msg) { showMessageBox(msg); }*/

	public static int random(int min, int max) { 
		if(min > max) {
			return random(max, min);
		}
		Random r = new Random(); // TODO Use unique random instance
		return r.nextInt(max+1-min) + min; 
	}
	
	
	public static void setappname(String s) { 
		getGUI().setTitle(s);
	}

	
	public static void setAppName(String s) { 
		getGUI().setTitle(s);
	}

	public static void unpress(int n) {
		switch (n)
		{
			case 0: 
				if (b1) UnB1(); 
				if (b2) UnB2(); 
				if (b3) UnB3(); 
				if (b4) UnB4(); 	
				break;
			case 1: if (b1) UnB1(); break;
			case 2: if (b2) UnB2(); break;
			case 3: if (b3) UnB3(); break;
			case 4: if (b4) UnB4(); break;
			case 5: if (up) UnUp(); break;
			case 6: if (down) UnDown(); break;
			case 7: if (left) UnLeft(); break;
			case 8: if (right) UnRight(); break;
			case 9: 
				if (b1) UnB1(); 
				if (b2) UnB2(); 
				if (b3) UnB3(); 
				if (b4) UnB4();
				if (up) UnUp();
				if (down) UnDown();
				if (left) UnLeft();
				if (right) UnRight();
				break;
		}
	}

	public static void updateControls() { 
		Controls.UpdateControls(); 
	}

	public static int asc(String s) { 
		if(s.length() == 0) 
			return 0; 
		else 
			return (int)s.charAt(0); 
	}
	
	public static String chr(int c) { 
		return Character.toString((char) c);
	}
	
	public static String gettoken(String s, String d, int i) { // Reimplemented by [Rafael, the Esper]
		String[] retorno = s.split(d);
		if(retorno.length <= i)
			return "";
		
		return retorno[i];
	}
	public static String left(String str, int len) { 
		return str.substring(0, str.length()>len?len:str.length());
	}
	
	public static int len(String s) { 
		return s.length(); 
	}
	
	public static String mid(String str, int pos, int len) { 
		return str.substring(pos, pos+len);
	}
	
	public static String right(String str, int len) { 
		return len > str.length() ? str : str.substring(str.length() - len);
	}
	
	public static String str(int d) { 
		return Integer.toString(d);
	}
	
	public static boolean strcmp(String s1, String s2) { 
		return s1.equals(s2); // ? 1 : 0;
	}
	
	public static String capitalize(String s) { // [Rafael, the Esper]
		if (s.length() == 0) return s;
		return s.substring(0, 1).toUpperCase() + s.substring(1);		
	}
	
		/* Krybo (Feb.2016)  : Causes the verge engine Thread to pause
		*  for the given number of seconds, take caution to consider
		*   that other threads may still be operating while the VergeEngine
		*     is sleeping.		     */
	public static void waitEngine(int milliseconds)
		{	enginePause(milliseconds);	}
	public static void waitEngine(Long milliseconds)
		{	enginePause(milliseconds);	}
	
		// Increment camera tracking mode by 1, loop around
	public static int nextCameraTrackingMode()
		{ 
		cameratracking++;
		if( cameratracking > 3 )
			{ cameratracking=0; }
		log("changed cameratracking to : "+Integer.toString(cameratracking));
		return(cameratracking);
		}
	public static void setCameraTrackingMode( int mode )
		{ cameratracking = mode;  return;  }

	public static String strdup(String s, int times) {
		String ret = "";
		for (int i=0; i<times; i++)
			ret = ret.concat(s);
		return ret;
	}
	
	public static int tokencount(String s, String d) {
		String[] retorno = s.split(d);
		return retorno.length;
	}
	
	public static String trim(String s) { 
		return s.trim(); 
	}
	
	public static String tolower(String str) { 
		return str.toLowerCase();
	}

	public static String toupper(String str) {
		return str.toUpperCase();
	}

	public static int val(String s) { 
		if(s==null || s.isEmpty() || s.trim().equals("-"))
			return 0;
		
		return Integer.valueOf(s.replace('+', ' ').trim());
	}


	
	//VI.d. Map Functions
	public static void map(String map) {
		mapname = map;
		die = true;
		done = true;

		/* Hookretrace carries over between maps!
		/ According to http://verge-rpg.com/docs/the-verge-3-manual/general-utility-functions/hookretrace/
		  hookretrace(""); */ 
	}

		// Krybo (Jan.2016) : overloaded map changer 
		// Change to a pre-built map object.
		// Forces the engine to restart using this new map object.
	public static void map( MapVerge newMap )
		{
		mapname = "_map_change_";
		current_map = newMap;
		die = true;		// these two kill the current map only.
		done = true;		
		}
	
	/*  Krybo (Jan.2016) : experimental vsp tile pixel changer.   */
	
	public static boolean changeVspPixel( int tileNum, int x1, int y1, int red256, int green256, int blue256 )
		{
		return(  current_map.getTileSet().modifyTile(
				tileNum, x1, y1, red256, green256, blue256	)	);
		}

	//VI.e. Entity Functions
	public static void changeCHR(int e, String c) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).set_chr(c);
	}
	public static void entitymove(int e, String s) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).setMoveScript(s);
	}
	public static void entitysetwanderdelay(int e, int d) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).setWanderDelay(d);
	}
	public static void entitysetwanderrect(int e, int x1, int y1, int x2, int y2) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).setWanderBox(x1, y1, x2, y2);
	}
	public static void entitysetwanderzone(int e) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).setWanderZone();
	}
	public static int entityspawn(int x, int y, String s) 
		{ 	return AllocateEntity(x*16,y*16,s); 	}
	
	// Krybo (Feb.2016) : added more flexibility on initial active/visible
	public static int entityspawn(int x, int y, String s, 
			boolean initActive, boolean initVisibility ) 
		{
		int newNum = AllocateEntity(x*16,y*16,s);
		entity.get(newNum).setActive(initActive);
		entity.get(newNum).setVisible(initVisibility);
		return(newNum);
		}
	public static int entitySpawnAsPlayer(int x, int y, String s) 
		{ 	
		int newone = AllocateEntity(x*16,y*16,s);
		setplayer(newone);
		return(newone);
		}
	public static int entitySpawnAsPlayer(int x, int y, String s, 
			boolean initActive, boolean initVisibility ) 
		{
		int newNum = AllocateEntity(x*16,y*16,s);
		entity.get(newNum).setActive(initActive);
		entity.get(newNum).setVisible(initVisibility);
		setplayer( newNum );
		return(newNum);
		}
	// Sets the player to a new Entity, its image uses a scripted cursor.
	public static int entitySpawnAsCursorPlayer(int x, int y, 
			int cursorSize, Color cursorColor1,Color cursorColor2, 
			Color cursorColor3,  boolean initActive, boolean initVisibility ) 
		{
		int newNum = AllocateEntityCursor( x*16,y*16, cursorSize, 
				cursorColor1, cursorColor2, cursorColor3 );

		entity.get(newNum).setActive(initActive);
		entity.get(newNum).setVisible(initVisibility);
		entity.get(newNum).speed = 300;
		setplayer( newNum );
		return(newNum);
		}

	public static int countParty(int first) {
		if (first<0 || first >= numentities) return 0;
		int num = 1;
		Entity e = entity.get(first);
		while(e.getFollower() != null) {
			e = e.getFollower();
			num++;
		}
			
		return num;
	}
	
	public static void entitystalk(int stalker, int stalkee) {
		if (stalker<0 || stalker>=numentities)
			return;
		if (stalkee<0 || stalkee>=numentities)
		{
			entity.get(stalker).clear_stalk();
			return;
		}
		entity.get(stalker).setx(entity.get(stalkee).getx()); // [Rafael, the Esper]
		entity.get(stalker).sety(entity.get(stalkee).gety()); // [Rafael, the Esper]
		entity.get(stalker).stalk(entity.get(stalkee));
	}
	public static void entitystop(int e) {
		if (e<0 || e >= numentities) return;
		else entity.get(e).setMotionless();
	}
	public static void hookentityrender(int i, String s) {
		if (i<0 || i>=numentities) 
			System.err.printf("vc_HookEntityRender() - no such entity %d", i);
		entity.get(i).hookrender = s;
	}
	
	public static void playermove(String s) {
		if (myself==null) 
			return;
		myself.setMoveScript(s);

		int current_invc = invc;
		invc=1;//Rafael
		while(myself.movecode != 0 )
		{
			screen.render();
			showpage();
		}
		invc=current_invc;//Rafael

		playerentitymovecleanup();
	}

	public static void playerentitymovecleanup() {
		if (myself==null) return;

		myself.movecode = 0;
		//[Rafael, the Esper] implementar afterPlayerMove();
	}

	public static void pauseplayerinput() { // [Rafael, the Esper]
		invc = 1;
	}
	public static void unpauseplayerinput() { // [Rafael, the Esper]
		invc = 0;
	}
	
	public static void setentitiespaused(boolean b) {
		entitiespaused = b;
		if (!entitiespaused)
			lastentitythink = systemtime;
	}
	
	public static Entity setplayer(int e) {
		if (e<0 || e>=numentities)
		{
			player = -1;
			myself = null;
			System.err.println("invalid Player.");
			return null;
		}
		
		// Krybo: since Setting player to an inactive entity would be nonsense
		entity.get(e).setActive(true);

		myself = entity.get(e);
		player = e;
		myself.setMotionless();
		myself.obstructable = true;
		return myself;
	}
	
		// Krybo (2014-09-21)  I needed these V1-ish player accessors 
		// Intentially disassociate the "player" entity. 
	public static void unsetplayer() 
		{
		player = -1;
		myself = null;
		}

	public static boolean playerIsSet()
		{
		if( (player < 0) || (player>=numentities) )	
			{ return(false); }
		if( myself == null )  { return(false); }
		return(true);
		}
	
	public static int playerGetFaceDirectionAsInt()
		{
		if( playerIsSet() == false) { return(0); }
		return( myself.face );
		}
	
	public static String playerGetFaceDirectionAsString()
		{
		if( playerIsSet() == false) { return("no"); }
		int f =  myself.face;
		if( f == 1 )  { return("north"); }
		if( f == 2 )  { return("south"); }
		if( f == 3 )  { return("west"); }
		if( f == 4 )  { return("east"); }
		return("error");
		}
	
	public static boolean playerIsFacing( int fdir )
		{
		if( myself.face == fdir )  { return(true); }
		return(false);
		}

	public static boolean playerIsFacing( String fdir )
		{
		fdir.toLowerCase();
		if( fdir.compareTo( playerGetFaceDirectionAsString() ) == 0 ) 
					{ return(true); }
		return(false);
		}

		// END Krybo edits (2014-09-21)
	
	// Krybo (2014-09-15)
	//  warp: Instantly moves the active entity to an x/y map position
	//	Speed:  cause the default is too slow
	public static void playerWarp(int xNew, int yNew)
		{
		myself.setx(xNew*16);
		myself.sety(yNew*16);
		myself.obstructable = true;
		playerentitymovecleanup();
		}
	public static void playerSetSpeed(int s)
		{
		myself.speed = s;
		}
	public static int playerGetSpeed()
		{
		return(myself.speed);
		}
	
	//  Return -1 if there is no "player" entity active.
	public static int playerGetMapPixelX()
		{
		if( myself == null )  { return(-1); }
		return( myself.getx() ); 
		}
	public static int playerGetMapPixelY()
		{
		if( myself == null )  { return(-1); }
		return( Math.abs( myself.gety()) ); 
		}
	public static int playerGetMapTileX()
		{
		if( myself == null )  { return(-1); }
		return( (Integer) ((myself.getx() / 16)+1) ); 
		}
	public static int playerGetMapTileY()
		{
		if( myself == null )  { return(-1); }
		return( (Integer) ((Math.abs(myself.gety()) / 16)+1) ); 
		}
	
	public static int getplayer()
		{	return player;  }
	
/*
	//VI.g. Sprite Functions
	static int GetSprite() { return GetSprite(); }
	static void ResetSprites() { return ResetSprites(); }

	//VI.h. Sound/Music Functions
	static void FreeSong(int handle) { FreeSong(handle); }
	static void FreeSound(int slot) { FreeSample((void*)slot); }
	static int GetSongPos(int handle) { return GetSongPos(handle); }
	static int GetSongVolume(int handle) { return GetSongVol(handle); }
	static int LoadSong(String fn) { return LoadSong(fn); }
	static int LoadSound(String fn) { return (int)LoadSample(fn); }*/
	public static void playsound(VSound sound) {
		playsound(sound, 100);
	}
	public static void playsound(VSound sound, int volume) {
		if(sound==null || VergeEngine.config.isNosound())
			return;

		if (volume < 0)
			volume = 0;
		else if (volume > 100)
			volume = 100;
		sound.start(volume);
	}

// Krybo : this was causing cyclical scoping permission problems, so i got rid of it.
//
//	public URL getmusic() 
//		{
//		if(VergeEngine.config==null || VergeEngine.config.isNosound() || musicplayer==null)
//			return null;
//		return VMusic.getPlay();
//		}
	
	public static void playmusic(URL fn) {
		playmusic(fn, 100);
	}
	
	public static void playmusic(URL fn, int volume) { 
		if(fn==null || VergeEngine.config==null || VergeEngine.config.isNosound())
			return;
		
		if(musicplayer!=null) {
			// If same music is playing, does nothing
			if(musicplayer.getPlay().equals(fn)) {
				return;
			}
			musicplayer.stop();
		}
		try {
			musicplayer = new VMusic(volume);
			log("Playing..." + fn);
			musicplayer.start(fn);
		}
		catch(Exception e) {
			System.err.println("Error when playing " + fn);
		}
	}
	
	/*
	static void PlaySong(int handle) { PlaySong(handle); }*/
	/*public static int playsound(String name, int volume) { 
		return 0;
		//return PlaySample((void*) slot, volume * 255 / 100); 
	}*/
	
	public static void setMusicVolume(int v) { 
		if(VergeEngine.config==null || VergeEngine.config.isNosound())
			return;
		
		if(musicplayer!=null) {
			musicplayer.setVolume(v);
		} else {
			musicplayer = new VMusic(v);
		}
		
	}

	/*static void StopSong(int handle) { StopSong(handle); }
	static void StopSound(int chan) { StopSound(chan); }
*/
	
	/*static void SetSongPaused(int h, int p) { SetPaused(h,p); }
	static void SetSongPos(int h, int p) { SetSongPos(h,p); }
	static void SetSongVolume(int h, int v) { SetSongVol(h,v); } */
	public static void stopmusic() { 
		if(musicplayer!=null) {
			musicplayer.stop();
		}
	}

	// Graphics
	
	public static void setcustomcolorfilter(Color c1, Color c2) {
		/*GetColor(c1, cf_r1, cf_g1, cf_b1);
		GetColor(c2, cf_r2, cf_g2, cf_b2);
		cf_rr = cf_r2 - cf_r1;
		cf_gr = cf_g2 - cf_g1;
		cf_br = cf_b2 - cf_b1;*/
		// TODO [Rafael, the Esper] Implement this
		graycolorfilter(screen.getImage());
		error("Non implemented function: setcustomcolorfilter");
	}
	
	public static void setlucent(int p) { 
		if(p < 0 || p > 100)
			return;
		currentLucent = (100-p) * 255 / 100;
		if(getGUI()!=null)
			getGUI().setAlpha ((float)(100-p) / 100);
	}

	static int lastchangetime = 0;

	public static void showpage() {
		//if(lastchangetime++ > 1) {
			lastchangetime = 0;
			if(virtualScreen!=null) {
				//finalScreen.blit(0, 0, screen);
				screen.blit(0, 0, virtualScreen);
			}
			//else {
				//finalScreen.blit(0, 0, screen);
			//}
		//}
		Controls.UpdateControls();
		
		DefaultTimer();//[Rafael, the Esper]
		GUI.paintFrame();

		/*if(toClipboard) {
			toClipboard = false;
			screen.copyImageToClipboard();
		}*/
		
		
	}
	

	public static void lightfilter(int scalefactor, VImage vimage) {
		RescaleOp op = new RescaleOp((float)scalefactor/100, 0, null);
		op.filter(vimage.image, vimage.image);
	}
	
	private static BufferedImageOp op = null;
	
	public static void graycolorfilter(BufferedImage img) {
		if(op==null)
			op = new ColorConvertOp (ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		img = op.filter(img, img);
		return;
	}
	
	
	public static void colorfilter(int filter, VImage img) { 
		if(filter>6) return;
		if(filter==1) {
			if(op==null)
				op = new ColorConvertOp (ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
			op.filter(img.getImage(), img.getImage());
			return;
		}
		
		int rr, gg, bb, z; Color c = null;

		int x1,x2,y1,y2;
		// [Rafael, the Esper] img.GetClip(x1,y1,x2,y2);
		x1 = y1 = 0;
		x2 = img.width;
		y2 = img.height;

		//PT ptr = (PT)img.data;
		//PT data = (PT)&ptr[(y1 * img.pitch) + x1];

		for (int y=y1; y<y2; y++)
		{
			//int* data_end = data+x2+1;
			for(int x=x1;x<x2;x++) {
				int rgb = img.getImage().getRGB(x, y);
				//Color col = new Color(img.getImage().getRGB(x, y));
				if (rgb == transcolor) continue; // Overkill (2006-07-27): Ignore trans pixels
				rr = (rgb >> 16) & 0x000000FF;				
				gg = (rgb >>8 ) & 0x000000FF;
				bb = (rgb) & 0x000000FF;
				//GetColor(col, rr, gg, bb);
				//if(filter==2) System.out.printf("%d %d %d %d\n", rr, gg, bb, 255-((rr+gg+bb)/3));
				switch (filter)
				{
					case 0: 
					case 1: z = (rr+gg+bb)/3; c = new Color(z,z,z); break; // GRAY
					case 2: z = 255-((rr+gg+bb)/3); c = new Color(z,z,z); break;
					case 3: c = new Color(255-rr, 255-gg, 255-bb); break;
					case 4: z = (rr+gg+bb)/3; c = new Color(z, 0, 0); break; // RED
					case 5: z = (rr+gg+bb)/3; c = new Color(0, z, 0); break; // GREEN
					case 6: z = (rr+gg+bb)/3; c = new Color(0, 0, z); break; // BLUE
					// [Rafael, the Esper] Custom color filter case 7: z = (rr+gg+bb)/3; c = new Color(cf_r1+((cf_rr*z)>>8), cf_g1+((cf_gr*z)>>8), cf_b1+((cf_br*z)>>8)).getRGB(); break;
				}
				img.setPixel(x, y, c);
			}
		}
	}	
	
	public static Color RGB(int r, int g, int b) {
		return new Color(r, g, b);
	}

	public static Color mixcolor(Color c1, Color c2, int p) {
		if (p>255) p=255;
		if (p<0) p=0;

		int r1 = c1.getRed();
		int g1 = c1.getGreen();
		int b1 = c1.getBlue();
		int r2 = c2.getRed();
		int g2 = c2.getGreen();
		int b2 = c2.getBlue();

		return new Color((r1*(255-p)/255)+(r2*p/255), (g1*(255-p)/255)+(g2*p/255), (b1*(255-p)/255)+(b2*p/255));
	}	


	public static int getB(int c) {
		return palette.getColor(c, currentLucent).getBlue();
	}
	public static int getG(int c) {
		return palette.getColor(c, currentLucent).getGreen();
	}
	public static int getR(int c) {
		return palette.getColor(c, currentLucent).getRed(); 
	}	
	
	/*

	static int HSV(int h, int s, int v) { return HSVtoColor(h,s,v); }
	static int GetH(int col) {
		int h, s, v;
		GetHSV(col, h, s, v);
		return h;
	}
	static int GetS(int col) {
		int h, s, v;
		GetHSV(col, h, s, v);
		return s;
	}
	static int GetV(int col) {
		int h, s, v;
		GetHSV(col, h, s, v);
		return v;
	}
	static void HueReplace(int hue_find, int hue_tolerance, int hue_replace, int image) {
		HueReplace(hue_find, hue_tolerance, hue_replace, ImageForHandle(image));
	}
	static void ColorReplace(int find, int replace, int image)
	{
		ColorReplace(find, replace, ImageForHandle(image));
	}
*/
	

	//VI.j. Math Functions
	//helper:
	public static int abs(int i) {
		return Math.abs(i);
	}
	public static int sgn(int i) {
		return (int) Math.signum(i);
	}
	public static int mydtoi(double d) { return (int)Math.floor(d + 0.5); }
	static int acos(int val) {
		double dv = (double) val / 65535;
		double ac = Math.acos(dv);
		ac = ac * 180 / 3.14159265358979; // convert radians to degrees
		return mydtoi(ac);
	}
	public static int facos(int val) {
		double dv = (double) val / 65535;
		double ac = Math.acos(dv);
		ac *= 65536; // Convert to 16.16 fixed point
		return mydtoi(ac);
	}
	public static int asin(int val) {
		double dv = (double) val / 65535;
		double as = Math.asin(dv);
		as = as * 180 / 3.14159265358979; // convert radians to degrees
		return mydtoi(as);
	}
	public static int fasin(int val) {
		double dv = (double) val / 65535;
		double as = Math.asin(dv);
		as *= 65536; // Convert to 16.16 fixed point
		return mydtoi(as);
	}
	public static int atan(int val) {
		double dv = (double) val / 65535;
		double at = Math.atan(dv);
		at = at * 180 / 3.14159265358979; // convert radians to degrees
		return mydtoi(at);
	}
	public static int fatan(int val) {
		double dv = (double) val / 65535;
		double at = Math.atan(dv);
		at *= 65536; // Convert to 16.16 fixed point
		return mydtoi(at);
	}
	public static int atan2(int y, int x) {
		float f = (float) Math.atan2((float)y,(float)x);
		return (int)(f/2.0/3.14159265358979*360.0);
	}
	public static int fatan2(int y, int x) {
		double theta = Math.atan2((double) y, (double) x);
		return mydtoi(theta * 65536);
	}
	public static int sin(int n) {
	    while (n < 0) n += 360;
	    while (n >= 360) n -= 360;
		return sintbl[n];
	}
	public static int cos(int n) {
	    while (n < 0) n += 360;
	    while (n >= 360) n -= 360;
		return costbl[n];
	}
	public static int tan(int n) {
	    while (n < 0) n += 360;
	    while (n >= 360) n -= 360;
		return tantbl[n];
	}
	public static int fsin(int val) {
		double magnitude = Math.sin((double) val / 65536);
		return mydtoi(magnitude * 65536);
	}
	public static int fcos(int val) {
		double magnitude = Math.cos((double) val / 65536);
		return mydtoi(magnitude * 65536);
	}
	public static int ftan(int val) {
		double magnitude = Math.tan((double) val / 65536);
		return mydtoi(magnitude * 65536);
	}
	public static int pow(int a, int b) {
		return (int) Math.pow((double)a, (double)b);
	}
	public static int sqrt(int val) {
		return (int) (float) Math.sqrt((float) val);
	}

	// Util Functions 

	private static boolean isLetterDigitOrSignal(char c) {
		if(Character.isLetterOrDigit(c) || c=='+' || c=='-')
			return true;
		return false;
	}
	
	// Split String in trimmed words
	public static List<String> splitTextIntoWords(String text) { 
		int initial = 0;
		List<String> words = new ArrayList<String>();
		if(text==null) 
			return words;
		
		for(int i=0; i<text.length(); i++) {
			while(i<text.length() && (isLetterDigitOrSignal(text.charAt(i)) || text.charAt(i) == '\'')) {
				i++;
			}
			while(i<text.length() && !isLetterDigitOrSignal(text.charAt(i))) {
				i++;
			}
			words.add(text.substring(initial, i).trim());
			initial = i;
		}
		return words;
	}	

	// Split list of words into rows 
	public static List<String> splitTextIntoRows(String text, int maxperrow) {
		
		List<String> words = splitTextIntoWords(text);
		List<String> rows = new ArrayList<String>();
		int i = 0;
		String str;
		while (i < words.size()) {
			str = words.get(i);
		    while (i < words.size()-1 && str.length()+ 1 + words.get(i+1).length() <= maxperrow) {
		       str = str.concat(" " + words.get(i+1));
		       i += 1;
			}
		    rows.add(str);
		    str = "";i+=1;
		}
		return rows;
	}	

	public static boolean up, down, left, right;
	public static boolean b1, b2, b3, b4;
	
	public static final int SCAN_A = java.awt.event.KeyEvent.VK_A;
	public static final int SCAN_B = java.awt.event.KeyEvent.VK_B;
	public static final int SCAN_C = java.awt.event.KeyEvent.VK_C;
	public static final int SCAN_D = java.awt.event.KeyEvent.VK_D;
	public static final int SCAN_E = java.awt.event.KeyEvent.VK_E;
	public static final int SCAN_F = java.awt.event.KeyEvent.VK_F;
	public static final int SCAN_G = java.awt.event.KeyEvent.VK_G;
	public static final int SCAN_H = java.awt.event.KeyEvent.VK_H;
	public static final int SCAN_I = java.awt.event.KeyEvent.VK_I;
	public static final int SCAN_J = java.awt.event.KeyEvent.VK_J;
	public static final int SCAN_K = java.awt.event.KeyEvent.VK_K;
	public static final int SCAN_L = java.awt.event.KeyEvent.VK_L;
	public static final int SCAN_M = java.awt.event.KeyEvent.VK_M;
	public static final int SCAN_N = java.awt.event.KeyEvent.VK_N;
	public static final int SCAN_O = java.awt.event.KeyEvent.VK_O;
	public static final int SCAN_P = java.awt.event.KeyEvent.VK_P;
	public static final int SCAN_Q = java.awt.event.KeyEvent.VK_Q;
	public static final int SCAN_R = java.awt.event.KeyEvent.VK_R;
	public static final int SCAN_S = java.awt.event.KeyEvent.VK_S;
	public static final int SCAN_T = java.awt.event.KeyEvent.VK_T;
	public static final int SCAN_U = java.awt.event.KeyEvent.VK_U;
	public static final int SCAN_V = java.awt.event.KeyEvent.VK_V;
	public static final int SCAN_W = java.awt.event.KeyEvent.VK_W;
	public static final int SCAN_X = java.awt.event.KeyEvent.VK_X;
	public static final int SCAN_Y = java.awt.event.KeyEvent.VK_Y;
	public static final int SCAN_Z = java.awt.event.KeyEvent.VK_Z;
	public static final int SCAN_0 = java.awt.event.KeyEvent.VK_0;
	public static final int SCAN_1 = java.awt.event.KeyEvent.VK_1;
	public static final int SCAN_2 = java.awt.event.KeyEvent.VK_2;
	public static final int SCAN_3 = java.awt.event.KeyEvent.VK_3;
	public static final int SCAN_4 = java.awt.event.KeyEvent.VK_4;
	public static final int SCAN_5 = java.awt.event.KeyEvent.VK_5;
	public static final int SCAN_6 = java.awt.event.KeyEvent.VK_6;
	public static final int SCAN_7 = java.awt.event.KeyEvent.VK_7;
	public static final int SCAN_8 = java.awt.event.KeyEvent.VK_8;
	public static final int SCAN_9 = java.awt.event.KeyEvent.VK_9;	
	
	public static boolean getkey(int key) {
		return Controls.getKey(key);
	}

	/*
	// Overkill (2006-06-30): Gets the contents of the key buffer.
	// TODO: Implement for other platforms.
	static String GetKeyBuffer()
	{
		//#ifdef __WIN32__
			return keybuffer;
		//#else 
			//err("The function GetKeyBuffer() is not defined for this platform.");
			//return String();
		//#endif
	}

	// Overkill (2006-06-30): Clears the contents of the key buffer.
	// TODO: Implement for other platforms.
	static void FlushKeyBuffer()
	{
		//#ifdef __WIN32__
			FlushKeyBuffer();
		//#else 
			//err("The function FlushKeyBuffer() is not defined for this platform.");
		//#endif
	}

	// Overkill (2006-06-30): Sets the delay in centiseconds before key repeat.
	// TODO: Implement for other platforms.
	static void SetKeyDelay(int d)
	{
		if (d < 0)
		{
			d = 0;
		}
		//#ifdef __WIN32__
			key_input_delay = d;
		//#else 
		//	err("The function SetKeyDelay() is not defined for this platform.");
		//#endif
	}	
	*/

	public static void setVirtualScreen(VImage dest) {
		virtualScreen = dest;
	}
	public static VImage getVirtualScreen() {
		return virtualScreen;
	}
	
	// Function (method) calling
	
	public static boolean functionexists(String function) {
		return executefunction(function, true);
	}
	
	/** Check methods in the following order:
	 * 
	 * 1. Direct Class-method (ex: sully.vc.v1_menu.Menu_System.DrawMenu)
	 * 2. System Lib (executed class, ex: Sully.class + method)
	 * 3. Loaded Map Class (ex: Bumsville.class + method)
	 *
	 * The called function must be public and without parameters.
	 * The capitalized version is also checked (ex: "entStart" checks also for "EntStart") 
	 * If the function is not found, nothing happens
	 * 	 
	 */
	public static void callfunction(String function) {
		executefunction(function, false);
	}
	
	/**
	 * Just gets a java.reflect.Method while handling exceptions.
	 * If its not found, a nullAction method that does nothing is returned.
	 * Krybo (Mar.2016)
	 * @param theClass			Generic parent Class
	 * @param methodName		String name of the desired Method.
	 * @return		a Method Object, upon total failure, returns null
	 */
	public static Method getFunction(Class<?> theClass, String methodName )
		{
		Method rslt = null;
		if( methodName.isEmpty() )	{ return(null); }
		
		for( Method m : theClass.getMethods() )
			{
//			System.out.println( " DEBUG >> " + m.getName() );
			if( m.getName().equals(methodName) == true )
				{ rslt = m; }
			}
		if( rslt != null )
			{ return(rslt); }

		log( "Cannot find method "+methodName+
			" in "+theClass.getName()+" -- null action was returned" );

		try	{
			rslt = Vmenuitem.class.getMethod("nullAction", 
					(Class<?>[]) null );
			}
		catch (NoSuchMethodException | SecurityException e1)
			{ 	return(null);	}		// BLARRRG

		return(rslt);
		}
	
	private static boolean executefunction(String function, boolean justCheck) {
		
		if(function==null || function.isEmpty()) 
			return false;

		Class<?> path = null;
		// This means that it is a direct class-method
		if (function.lastIndexOf(".") != -1) {
			String s = function.substring(function.lastIndexOf(".") + 1);
			String t = function.substring(0, function.lastIndexOf("."));
			try { 
				path = Class.forName(t);
			}
			catch(ClassNotFoundException cnfe) {
				error("Class " + path + " not found for direct execution (" + function + ")");
				return false;
			}
			invokeMethod(path, s, justCheck);
			return true;
		}
		else { // Try to find the class in the current_map
			 boolean notFoundInMap = false;
			 StringBuilder cName = new StringBuilder();
			 if(current_map != null && current_map.getFilename() != null) {
				 	cName.append(systemclass.getPackage().getName() + ".");
				 	
				 	int pos = current_map.getFilename().lastIndexOf('\\');
				 	if(pos==-1)
				 		pos = 0;
			 		StringBuilder b = new StringBuilder(current_map.getFilename().toLowerCase());
			 		b.replace(pos, pos+1, String.valueOf(Character.toUpperCase(b.charAt(pos))));
			 		String s = b.toString().substring(0, b.indexOf(".map")).replace('\\', '.');
			 		cName.append(s);
			 		
			 		try {
			 			path = Class.forName(cName.toString());
			 		}
					catch(ClassNotFoundException cnfe) {
						// FIXME Solve this mess, also use toUppercase and Capitalize first letter to avoid error on .MAP
		 				b = new StringBuilder(systemclass.getPackage().getName() + "." + mapname);
				 		s = b.toString().substring(0, b.lastIndexOf(".map")).replace('\\', '.').replace('/', '.');
				 		try {
							path = Class.forName(s);
						} catch (ClassNotFoundException e) {
							error("Class " + path + " not found for map execution.");
							notFoundInMap = true; //return;
						}	
					}
					if(path!=null) {
				 		if (!invokeMethod(path, function, justCheck))
							notFoundInMap = true;
						else
							return true; // Success
					}
			 }
			
			 // Try to find the method directly in the System class
			 if(current_map == null || current_map.getFilename() == null || notFoundInMap) {
			 
				 path = systemclass;
				 if (invokeMethod(path, function, justCheck)) {
					 return true; // Success
				 }
				 else {
					 error("Error invoking " + function + " in path " + path);
				 }
			 }
		}
		return false;
	}
	
	private static boolean invokeMethod(Class<?> c, String function, boolean justCheck) { 

		Method[] allMethods = c.getDeclaredMethods();
		for (Method m : allMethods) {
			String mname = m.getName();
			if(mname.equals(function) || mname.equals(capitalize(function))) 
				{
				if(justCheck)		{ return true; }
				
				try {
						/* uncomment for debug	
					log("Found method " + mname + " in path " + c + 
							" while attempting to invoke [" + function + "]" );
						*/
					m.invoke(null);
					return true;
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.getCause();
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	/**
	 * Method for loading resources from the classpath, like images, fonts, sounds, etc 
	 */
	public static URL load(String url) {
		if(TEST_SIMULATION) // [Rafael, the Esper]
			return null;
		
		log("(" + systemclass + ")" + ", reading: " + url);
		URL resource = systemclass.getResource(url);
		
		if( (resource == null)  && (url.startsWith("/") == false) )
			{
			String slashedUrl = new String("/"+url);
			resource = systemclass.getResource(slashedUrl);
			}
		
		// Optional code, a little robustness to avoid case-sensitive issues
		if(resource == null) { // try to capitalize
			String newUrl;
			if(url.lastIndexOf('/') != -1) 
				newUrl = url.substring(0, url.lastIndexOf('/')+1) +
					capitalize(url.substring( url.lastIndexOf('/')+1));
			else
				newUrl = capitalize(url);
			log("WARNING! Resource not found. Trying to read: " + newUrl);
			resource = systemclass.getResource(newUrl);
			
			if(resource==null) { // try uppercase 
				if(url.lastIndexOf('/') != -1) 
					newUrl = url.substring(0,  url.lastIndexOf('/')+1) +
						url.substring( url.lastIndexOf('/')+1).toUpperCase();
				else
					newUrl = url.toUpperCase();
				log("WARNING! Resource not found. Trying to read: " + newUrl);
				resource = systemclass.getResource(newUrl);				
			}
			
			if(resource==null) { // try lowercase 
				if(url.lastIndexOf('/') != -1) 
					newUrl = url.substring(0,  url.lastIndexOf('/')+1) +
						url.substring( url.lastIndexOf('/')+1).toLowerCase();
				else
					newUrl = url.toLowerCase();
				log("WARNING! Resource not found. Trying to read: " + newUrl);
				resource = systemclass.getResource(newUrl);				
			}			
			
			if(resource==null) {
				error("ERROR! Resource not found: " + url);
			}
			
		}
		syncAfterLoading(); // Rafael, the Esper
		return resource;
	}

	/**
	 * Krybo (2014-10-22) Added this so that loading URL's can be tested for existance 
	 * Similar to load() but returns true or false instead of the actual URL
	 * And will not leave log messages 
	 */
	public static boolean loadTEST(String url) 
		{
		if(TEST_SIMULATION) // [Rafael, the Esper]
			return false;
		
		URL resource = systemclass.getResource(url);
		
		if( (resource == null)  && (url.startsWith("/") == false) )
			{
			String slashedUrl = new String("/"+url);
			resource = systemclass.getResource(slashedUrl);
			}
		
		// Optional code, a little robustness to avoid case-sensitive issues
		if(resource == null) { // try to capitalize
			String newUrl;
			if(url.lastIndexOf('/') != -1) 
				newUrl = url.substring(0, url.lastIndexOf('/')+1) +
					capitalize(url.substring( url.lastIndexOf('/')+1));
			else
				newUrl = capitalize(url);

			resource = systemclass.getResource(newUrl);
			
			if(resource==null) { // try uppercase 
				if(url.lastIndexOf('/') != -1) 
					newUrl = url.substring(0,  url.lastIndexOf('/')+1) +
						url.substring( url.lastIndexOf('/')+1).toUpperCase();
				else
					newUrl = url.toUpperCase();

				resource = systemclass.getResource(newUrl);				
			}
			
			if(resource==null) { // try lowercase 
				if(url.lastIndexOf('/') != -1) 
					newUrl = url.substring(0,  url.lastIndexOf('/')+1) +
						url.substring( url.lastIndexOf('/')+1).toLowerCase();
				else
					newUrl = url.toLowerCase();

				resource = systemclass.getResource(newUrl);				
			}			
			
			if(resource==null) 
				{	return(false);  	}	
			}
		return true;
		}

	

	public static void setSystemPath(Class<?> c) {
		systemclass = c;
	}
	
	// Krybo (Mar.2016)  : menu related functions
	// most of these are delegators to the VMM
	
	public static void addMenu( Vmenu thisVm )
		{	Vmm.addVmenu( thisVm );	}
	public static void addMenuWithFocus( Vmenu thisVm )
		{	Vmm.addVmenuWithFocus( thisVm );	}
	public static void addMenu( ArrayList<Vmenu> theVms )
		{	Vmm.addVmenu( theVms );	}
	public static boolean setMenuFocus(int slot, Long id )
		{	return( Vmm.setFocus(id, slot ) );	}
	public static void refreshMenu()
		{ Vmm.refreshGraphics();   return; }
	
	// Krybo (2014-09-18)   map zooming functions
	
	public static BufferedImage scaleImage(
			BufferedImage originalImage, int type, float sfactorX, float sfactorY )
		{
		int scaledX = (int) Math.floor(originalImage.getWidth() * sfactorX );
		int scaledY = (int) Math.floor(originalImage.getHeight() * sfactorY );

		BufferedImage resizedImage = new BufferedImage(
				scaledX, scaledY, type);

		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, scaledX, scaledY, null);
		g.dispose();
		return resizedImage;
	    }

		// Freezes the engine until a key is released.
	public static void waitKeyUp( int keycode )
		{
		while( getKey(keycode) )
			{  UpdateControls(); }
		}


		// Krybo (2014-10-04)  imported this as it may be helpful
	  /**
	   * List directory contents for a resource folder. Not recursive.
	   * This is basically a brute-force implementation.
	   * Works for regular files and also JARs.
	   * http://stackoverflow.com/questions/6247144/how-to-load-a-folder-from-a-jar
	   * 
	   * @author Greg Briggs
	   * @param clazz Any java class that lives in the same place as the resources you want.
	   * @param path Should end with "/", but not start with one.
	   * @return Just the name of each member item, not the full paths.
	   * @throws URISyntaxException 
	   * @throws IOException 
	   */
	  public static String[] getResourceListing(Class<?> clazz, String path) throws URISyntaxException, IOException 
		{
		log(" getResourceListing : Scanning for files: "+path );
		URL dirURL = new URL("file:///null.null");
		try {
			dirURL = clazz.getClassLoader().getResource(path);
		} catch( Exception e ) { e.printStackTrace(); }
		finally  
			{
			if( dirURL != null )
				{ log(" getResourceListing : looking in "+dirURL.toString() ); }
			else  {  log(" Trying .jar mode.");  }
			}

		// Original un-error-handled code.
		//	      URL dirURL = clazz.getClassLoader().getResource(path);
		
	      if (dirURL != null && dirURL.getProtocol().equals("file")) 
	     	 {
	     	 /* A file path: easy enough */
	     	 return new File(dirURL.toURI()).list();
	     	 } 

	        /* 
	         * In case of a jar file, we can't actually find a directory.
	         * Have to assume the same jar as clazz.
	         */
	      if (dirURL == null) 
	        {
	        String me = clazz.getName().replace(".", "/")+".class";
	        dirURL = clazz.getClassLoader().getResource(me);
	        if( dirURL == null )
	     	   {	
	     	   log("JAR mode FAILED  Resources not read! ");   	
	     	   return(null); 
	     	   }
	        else
	     	   {   log(" JAR URL: "+dirURL.toString() );	   }
	        }

	      if (dirURL.getProtocol().equals("jar")) 
	     	 {
	        /* A JAR path */
	        String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
	        JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
	        
	        Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
	        Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
//	        jar.close();
	        while(entries.hasMoreElements()) 
	     	   {
	          String name = entries.nextElement().getName();
	          if (name.startsWith(path)) 
	          	{ //filter according to the path
	            String entry = name.substring(path.length() , name.length() );
	            if( entry.equals("/") || entry.equals("") )
	          	  { continue; }
	            if( entry.endsWith("/") || entry.endsWith("\\") )
	          	  { continue; }		// We don't do subdirectories here
	            	// Lop leading slashes if they remain
	            if( entry.startsWith("/") || entry.startsWith("\\") )
	          	  { entry = entry.substring(1, entry.length() ); }
	            int checkSubdir = entry.indexOf("/");
	            if (checkSubdir >= 0)  { continue; } 
//	          	  {
	              // if it is a subdirectory, we just return the directory name
//	          	  entry = entry.substring(0, checkSubdir);
//	          	  log("JAR  subDIR : "+entry );
//	          	  }
	            result.add(entry);
	          }	        
	        }
	        
	        jar.close();
	        return result.toArray(new String[result.size()]);
	      } 

	      throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
	  }

	  
	  // Krybo (2014-10-09)  Verge1 style VC graphics emulation 

	  public int VCaddLayer()
		  {  return jvcl.addLayer();  }
	  public int VCremoveLayer()
		  {  return jvcl.dropLayer();   }
	  
	  public void VClayerWrite(int layer)
		{	jvcl.setWriteLayer(layer);  	}
	  
	  public void VCbox(int xCoord1, int yCoord1, int xCoord2, int yCoord2 )
		  {
		  if( xCoord2 > xCoord1 )
			  {
			  int tmp = xCoord1;
			  xCoord1 = xCoord2;
			  xCoord2 = tmp;
			  }
		  if( yCoord2 > yCoord1 )
			  {
			  int tmp = yCoord1;
			  yCoord1 = yCoord2;
			  yCoord2 = tmp;
			  }

		  jvcl.JVCrect(xCoord1, yCoord1, xCoord2-xCoord1, yCoord2 - yCoord1, Color.white );
		  return;
		  }
	  public void VCboxfill(int xCoord1, int yCoord1, int xCoord2, int yCoord2 )
		  {
		  if( xCoord2 > xCoord1 )
			  {
			  int tmp = xCoord1;
			  xCoord1 = xCoord2;
			  xCoord2 = tmp;
			  }
		  if( yCoord2 > yCoord1 )
			  {
			  int tmp = yCoord1;
			  yCoord1 = yCoord2;
			  yCoord2 = tmp;
			  }

		  jvcl.JVCrectfill(xCoord1, yCoord1, xCoord2-xCoord1, yCoord2 - yCoord1, Color.white );
		  return;
		  }
	  
	  public void VCline(int xCoordinate1, int yCoordinate1, int xCoordinate2, int yCoordinate2, Color c )
		  {
		  jvcl.JVCline(xCoordinate1, yCoordinate1, xCoordinate2, yCoordinate2, c);
		  return;
		  }
	  
	  public void VCputPCX( VImage img, int xCoordinate, int yCoordinate )
	  	{
	  	jvcl.JVCblitImage(xCoordinate, yCoordinate, img );
	  	return;
	  	}
	  
	  public void VCtext(int xCoordinate, int yCoordinate, String message )
	  	{
	  	jvcl.JVCstring(xCoordinate, yCoordinate, message );
	  	return;
	  	}
	  
	  public void VCTextNum(int xCoordinate, int yCoordinate, int number )
		  {
		  jvcl.JVCstring(xCoordinate, yCoordinate, Integer.toString(number) );
		  return;
		  }	  
	  public void VCTextNum(int xCoordinate, int yCoordinate, long number )
		  {
		  jvcl.JVCstring(xCoordinate, yCoordinate, Long.toString(number) );
		  return;
		  }
	  public void VCTextNum(int xCoordinate, int yCoordinate, float number )		  
		  {
		  jvcl.JVCstring(xCoordinate, yCoordinate, Float.toString(number) );
		  return;
		  }
	  public void VCTextNum(int xCoordinate, int yCoordinate, double number )		  
		  {
		  jvcl.JVCstring(xCoordinate, yCoordinate, Double.toString(number) );
		  return;
		  }
	  
	  public void VCclearAll()
		  {   jvcl.JVCclearAllLayers();   }
	  public void VCclear()
		  {   jvcl.JVCclear();   }
	  
	  public void VCclearRegion(int xCoordinate1, int yCoordinate1, int xCoordinate2, int yCoordinate2 )
		  {
		  if( xCoordinate2 > xCoordinate1 )
			  {
			  int tmp = xCoordinate1;
			  xCoordinate1 = xCoordinate2;
			  xCoordinate2 = tmp;
			  }
		  if( yCoordinate2 > yCoordinate1 )
			  {
			  int tmp = yCoordinate1;
			  yCoordinate1 = yCoordinate2;
			  yCoordinate2 = tmp;
			  }

		  jvcl.JVCrectfill(xCoordinate1, yCoordinate1, 
				  xCoordinate2 - xCoordinate1, yCoordinate2 - yCoordinate1, 
				  new Color(0.0f,0.0f,0.0f,0.0f ) );
		  return;
		  }

  		// Krybo: (2014-11-02)  Delegator to add dynamic dialog boxes
	  	// Using JVCL's special dialog layer
	  public static void VCdynamicDialogBox( int mapEntityNum, String message, int durationMsec, Font fnt, 
			 Color textColor, Color outlineColor, int frameWidth, 
			 VImage imgBackground, float alphaBackground )
		{
		jvcl.JVCdialogAdd(mapEntityNum, message, durationMsec, fnt,
				textColor, outlineColor, frameWidth, 
				imgBackground, alphaBackground);
		return;
	  	}
	  
	  // -END- Krybo (2014-10-09)  Verge1 VC graphics emulation 

	  
	/* V1 VC stub list  :: Implement all these
	 *	- Movement -
	 * Warp(x coordinate, y coordinate, no fade);
	 * MapSwitch("map file name",x coordinate, y coordinate, no fade);
	 *StatusScreen(roster order index);
	 * AddCharacter(party.dat index);
	 * RemoveCharacter(party.dat index);
	 * GiveXP(character, amount);
	 * ChangeCHR(character, "chr file name");
	 * HealAll();
	 * GiveGP(amount);
	 * TakeGP(amount);
	 * GiveItem(items.dat index);
	 * DestroyItem(items.dat index, character);
	 * GetItem(items.dat index, character);
	 * GetMagic(character, magic.dat index);
	 * ForceEquip(items.dat index, character);
	 * Shop(item1, item2, item3, ... item12);
	 * MagicShop(spell1, spell2, spell3, ... spell12);
	 * Exit();
	 * Quit(message);
	 * 
	 *    -Sound-
	 *  SoundEffect(main.sfx index);
	 *  PlayMusic("file name");
	 *  StopMusic();
	 *  
	 *    - communication -
	 *  TextMenu(x coordinate, y coordinate, flag, default, "choice1","choice2",..);
	 *  ItemMenu(roster order index);
	 *  EquipMenu(roster order index);
	 *  MagicMenu(roster order index);
	 *  Text(speech portrait, "line1", "line2", "line3");
	 *  SText(speech portrait, "line1", "line2", "line3");
	 *  Prompt(speech portrait, "line1", "line2", "line3", flag, "choice1", "choice2");
	 *  Banner("message", duration);
	 *  
	 *     - ENV -
	 *  AlterBTile(x coordinate, y coordinate, new tile, obstruction value);
	 *  AlterFTile(x coordinate, y coordinate, new tile, obstruction value);
	 *  ChangeZone(x coordinate, y coordinate, new zone);
	 *  AlterParallax(parallax control mode, multiplier, divisor);
	 *  EnforceAnimation();
	 *  DisableMenu();
	 *  EnableMenu();
	 *  DisableSave();
	 *  EnableSave();
	 *  SaveMenu();
	 *  
	 *  		-- VC Event --
	 *  Return;
	 *  WaitKeyUp();
	 *  ReadControls();
	 *  ChainEvent(event number, optional variable 1, optional variable 2, etc.);
	 *  CallEvent(event number, optional variable 1, optional variable 2, etc.);
	 *  CallEffect(effect number, optional variable 1, optional variable 
	 *  CallScript(script number, optional variable 1, optional variable 
	 *  HookTimer(event number);
	 *  HookRetrace(event number);
	 *  
	 *  	-- Visual --
	 *  PlayVAS ("VAS filename", delay, width, height, x coordinate, y coordinate);
	 *  FadeIn(duration);
	 *  FadeOut(duration);
	 *	BoxFadeIn(duration);
	 *	BoxFadeOut(duration);
	 *Earthquake(x intensity, y intensity, duration);
	 *Redraw();
	 *Wait(duration);
	 *SetFace(character, direction);
	 *PaletteMorph(red, green, blue, intensity, lighting);
	 *MapPaletteGradient(start color, end color, invert, mode);
	 *
	 *		-- VC Graphics --
	 *	o VCBox(x coordinate1, y coordinate1, x coordinate2, y coordinate2);
	 *	VCCharName(x coordinate, y coordinate, party.dat index, align);
	 * VCItemName(x coordinate, y coordinate, items.dat index, align);    
	 * VCItemDesc(x coordinate, y coordinate, items.dat index, align);    
	 * VCSpellName(x coordinate, y coordinate, magic.dat index, align);
	 * 	VCSpellDesc(x coordinate, y coordinate, magic.dat index, align);   
	 * VCItemImage(x coordinate, y coordinate, items.dat index, greyflag);
	 * 	VCSpellImage(x coordinate, y coordinate, items.dat index, greyflag);
	 * 	VCTextBox(x coordinate, y coordinate, pointer, "choice1","choice2",..);
	 * 	VCCr2(x coordinate, y coordinate, roster order index, greyflag);
	 * 	VCSpc(x coordinate, y coordinate, speech portrait, greyflag);
	 * 	VCPutPCX("pcx file name", x coordinate, y coordinate);
	 * 	VCLoadPCX("pcx file name", memory offset);
	 * 	VCBlitImage(x coordinate, y coordinate, width, height, memory offset);
	 * VCTBlitImage(x coordinate, y coordinate, width, height, memory offset);
	 * 	VCText(x coordinate, y coordinate, "message");
	 * 	VCCenterText(y coordinate, "message");
	 * 	VCTextNum(x coordinate, y coordinate, number);
	 * 	VCATextNum(x coordinate, y coordinate, number, align);
	 * 	VCClear();
	 * 	VCLine(x coordinate1, y coordinate1, x coordinate2, y coordinate2, color); 
	 * 	  VCClearRegion(x coordinate1, y coordinate1, x coordinate2, y coordinate2);
	 * VCLoadRaw(filename, vc data buf offset, file start offset, length);
	 * 	Screen[x,y]
	 * 	
	 * 	-- Entity --
	 * PartyMove(movement script);
	 * EntityMove(entity number, movement script);
	 * EntityMoveScript(entity number, map movement script index number);
	 * AutoOn()/AutoOff();
	 * SpecialFrame(entity)
	 * Face(entity)
	 * Speed(entity)
	 * MoveCode(entity)
	 * ActiveMode(entity)
	 * ObsMode(entity)
	 * Entity.Moving(entity)
	 * Entity.CHRindex(entity)
	 * Entity.Step(entity)
	 * Entity.Delay(entity)
	 * Entity.LocX(entity)
	 * Entity.LocY(entity)
	 * Entity.Face(entity)
	 * Entity.Chasing(entity)
	 * Entity.ChaseDist(entity)
	 * Entity.ChaseSpeed(entity)
	 * 
	 * 	-- Startup --
	 * Sys_ClearScreen();
	 * Sys_DisplayPCX("filename.pcx");
	 * OldStartupMenu();
	 * VGAdump();
	 * NewGame("mapfile.map");
	 * LoadMenu();
	 * BindKey(key code, script number);
	 * 
	 *	
	 *
	 *
	 *
	 */
	 
	
	
}
