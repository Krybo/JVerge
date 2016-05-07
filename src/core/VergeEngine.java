package core;

import static core.Controls.*;
import static core.Script.*;
import static core.Sprite.RenderSpritesAboveEntity;
import static core.Sprite.RenderSpritesBelowEntity;
import static core.Sprite.sprites;
import static domain.Entity.EAST;
import core.JVCL;
import static domain.Entity.NE;
import static domain.Entity.NORTH;
import static domain.Entity.NW;
import static domain.Entity.SE;
import static domain.Entity.SOUTH;
import static domain.Entity.SW;
import static domain.Entity.WEST;















import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import menus.VMenuManager;
import domain.Config;
import domain.Entity;
import domain.MapDynamic;
import domain.MapVerge;
import domain.VImage;

public class VergeEngine extends Thread 
	{
	public static boolean done, inscroller = false;
	public static int px, py;
	public static Double JAVA_VERSION =  
		Double.parseDouble( 
				System.getProperty("java.specification.version") );
	public static int lastentitythink;
	public static int lastspritethink = 0;
	
	public static int currentMapZoneWidth = -1;
	public static int currentMapZoneHeight = -1;
		// Krybo (Mar.2016) : keep this a static in case we lose menu focus.
	public static VMenuManager Vmm;
	private static Long SYSTEM_MENU_FOCUS_ID = new Long( -1 );
	
	public static final String JVERGE_VERSION = "1.1.1";
	// (Krybo) The current working directory when the engine starts.
	public static File JVERGE_CWD;

	public static boolean die;
	
	static GUI gui;
	
	public static GUI getGUI() {
		return gui;
	}

	/****************************** data ******************************/

	// Rafael: new code
	protected static Config config = null;
	public static Class<?> systemclass;

	protected static String mapname;

	/****************************** code ******************************/

	// Krybo : experimental wait(x) engine-level function(s)  U.w/Caution.!
	public static void enginePause( int milliseconds )
		{
		Long ms = new Long(milliseconds);
		try {
			Thread.sleep(ms); 
			}
		catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
			}
		return;
		}
	public static void enginePause( Long milliseconds )
		{
		try {
			Thread.sleep(milliseconds); 
			}
		catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
			}
		return;
		}

	// main engine code

	static int AllocateEntity(int x, int y, String chr) 
		{
		// Krybo (Feb.2016) : added capability to handle blank "ghost" entities.
		Entity e;
		if( chr == null || chr.isEmpty() )
			{	
			e = new Entity();
			e.setx(x);
			e.sety(y);
			}			// A "ghost"
		else
			{ e = new Entity(x, y, chr); }		// entity-CHR from file.
		e.index = numentities;
		entity.add(e);
		return numentities++;
		}
	static int AllocateEntityCursor( int x, int y, int cSize, 
			Color cClr1, Color cClr2, Color cClr3 ) 
		{
		// Krybo (Feb.2016) : added capability to handle blank "ghost" entities.
		Entity e = new Entity( cSize, cClr1, cClr2, cClr3 );
		e.setx(x);
		e.sety(y);

		e.index = numentities;
		entity.add(e);
		return numentities++;
		}

	protected static class EntityComparator implements Comparator<Entity> {
		public int compare(Entity ent1, Entity ent2) {
			return ent1.gety() - ent2.gety();
		}
	}

	public static void RenderEntities(VImage dest) {
		List<Entity> entidx = new ArrayList<Entity>();
		int entnum = 0;

		// Build a list of entities that are visible.
		// FIXME: Make it actually only be entities that are onscreen
		for (int i = 0; i < numentities; i++) {
			entidx.add(entity.get(i));
			entnum++;
		}

		// Ysort that list, then draw.
		Collections.sort(entidx, new EntityComparator());
		// qsort(entidx, entnum, 1, cmpent);
		for (int i = 0; i < entnum; i++) {
			RenderSpritesBelowEntity(i); // Rafael: entidx.get(i));
			setlucent(entidx.get(i).lucent);
			entidx.get(i).draw(dest);
			setlucent(0);
			RenderSpritesAboveEntity(i); // Rafael: entidx.get(i));
		}
	}

	static void ProcessEntities() {
		if (entitiespaused)
			return;
		for (int i = 0; i < numentities; i++) {
			entity.get(i).think();
		}
	}

	static int EntityAt(int x, int y) {
		for (int i = 0; i < numentities; i++) {
			if (entity.get(i).active && x >= entity.get(i).getx()
					&& x < entity.get(i).getx() + entity.get(i).chr.hw
					&& y >= entity.get(i).gety()
					&& y < entity.get(i).gety() + entity.get(i).chr.hh)
				return i;
		}
		return -1;
	}

	static int EntityObsAt(int x, int y) {
		for (int i = 0; i < numentities; i++) {
			if (entity.get(i).active && entity.get(i).obstruction
					&& x >= entity.get(i).getx()
					&& x < entity.get(i).getx() + entity.get(i).chr.hw
					&& y >= entity.get(i).gety()
					&& y < entity.get(i).gety() + entity.get(i).chr.hh)
				return i;
		}
		return -1;
	}

	static boolean isEntityCollisionCapturing() {
		return !_trigger_onEntityCollide.isEmpty();
	}

	int __obstructionHappened = 0;

	public static boolean ObstructAt(int x, int y) {
		if (current_map.getobspixel(x, y)) {

			if (isEntityCollisionCapturing()) {
				event_tx = x / 16;
				event_ty = y / 16;
				event_entity = __grue_actor_index;
				event_zone = current_map.getzone(x / 16, y / 16);
				event_entity_hit = -1;
				onEntityCollision();
			}

			return true;
		}

		int ent_idx = EntityObsAt(x, y);

		if (ent_idx > -1) {

			if (isEntityCollisionCapturing()) {
				event_tx = x / 16;
				event_ty = y / 16;
				event_entity = __grue_actor_index;
				event_zone = -1;
				event_entity_hit = ent_idx;
				onEntityCollision();
			}

			return true;
		}

		return false;
	}

	// returns distance possible to move up to
	// the first obstruction in the given direction
	static int MaxPlayerMove(int d, int max) {
		__grue_actor_index = myself.index;

		int x, y;
		int ex = myself.getx();
		int ey = myself.gety();

		// check to see if the player is obstructable at all
		if (!myself.obstructable)
			return max;

		for (int check = 1; check <= max + 1; check++) {
			switch (d) {
			case NORTH:
				for (x = ex; x < ex + myself.chr.hw; x++)
					if (ObstructAt(x, ey - check))
						return check - 1;
				break;
			case SOUTH:
				for (x = ex; x < ex + myself.chr.hw; x++)
					if (ObstructAt(x, ey + myself.chr.hh + check - 1))
						return check - 1;
				break;
			case WEST:
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex - check, y))
						return check - 1;
				break;
			case EAST:
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex + myself.chr.hw + check - 1, y))
						return check - 1;
				break;
			case NW:
				for (x = ex; x < ex + myself.chr.hw; x++)
					if (ObstructAt(x - check, ey - check))
						return check - 1;
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex - check, y - check))
						return check - 1;
				break;
			case SW:
				for (x = ex; x < ex + myself.chr.hw; x++)
					if (ObstructAt(x - check, ey + myself.chr.hh + check - 1))
						return check - 1;
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex - check, y + check))
						return check - 1;
				break;
			case NE:
				for (x = ex; x < ex + myself.chr.hw; x++)
					if (ObstructAt(x + check, ey - check))
						return check - 1;
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex + myself.chr.hw + check - 1, y - check))
						return check - 1;
				break;
			case SE:
				for (x = ex; x < ex + myself.chr.hh; x++)
					if (ObstructAt(x + check, ey + myself.chr.hh + check - 1))
						return check - 1;
				for (y = ey; y < ey + myself.chr.hh; y++)
					if (ObstructAt(ex + myself.chr.hw + check - 1, y + check))
						return check - 1;
				break;
			}
		}
		return max;
	}

	static void onStep() {
		if (!_trigger_onStep.isEmpty()) {
			Script.callfunction(_trigger_onStep);
		}
	}

	static void afterStep() {
		if (!_trigger_afterStep.isEmpty()) {
			Script.callfunction(_trigger_afterStep);
		}
	}

	static void afterPlayerMove() {
		if (!_trigger_afterPlayerMove.isEmpty()) {
			Script.callfunction(_trigger_afterPlayerMove);
		}
	}

	static void beforeEntityActivation() {
		if (!_trigger_beforeEntityScript.isEmpty()) {
			Script.callfunction(_trigger_beforeEntityScript);
		}
	}

	static void afterEntityActivation() {
		if (!_trigger_afterEntityScript.isEmpty()) {
			Script.callfunction(_trigger_afterEntityScript);
		}
	}

	static void onEntityCollision() {
		if (isEntityCollisionCapturing()) {
			Script.callfunction(_trigger_onEntityCollide);
		}
	}

	public static void ProcessControls() 
		{
		Controls.UpdateControls();

			// Krybo (Mar.2016) We are in menu mode.  Stop entity controls
			// Controls can then be re-purposed for menus manipulation.
		if( Controls.MENU_OPEN == true )
			{
			Controls.UpdateMenusControls( new Long(90000000) );
			return; 
			}

		// No player movement can be done if there's no ready player, or if
		// there's a script active.
		if (myself == null || !myself.ready() || invc != 0) 
			{	return;	}

		if (myself.movecode == 3) {
			// ScriptEngine::
			playerentitymovecleanup();
		}

		// kill contradictory input
		if (up && down)
			up = down = false;
		if (left && right)
			left = right = false;

		// if we're not supposed to be using diagonals,
		// prevent that, too.
		// We keep track of the last direction we moved in
		// and if we have diagonal input, we move along the same
		// axis of movement as before the conflict (horiz or vert.)
		// - Jesse 22-10-05
		if (!playerdiagonals) {
			if ((up || down) && (left || right) && !smoothdiagonals) {
				if (lastplayerdir == WEST || lastplayerdir == EAST)
					up = down = false;
				else
					left = right = false;
			} else { 
				if (left) {
					lastplayerdir = WEST;
				} else if (right) {
					lastplayerdir = EAST;
				} else if (up) {
					lastplayerdir = NORTH;
				} else if (down) {
					lastplayerdir = SOUTH;
				} else {
					lastplayerdir = 0;
				}
			}
		}

		// check diagonals first
		if (left && up) {
			myself.setface(WEST);
			int dist = MaxPlayerMove(NW, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(-1 * dist, -1 * dist, true);
				return;
			}
		}
		if (right && up) {
			myself.setface(EAST);
			int dist = MaxPlayerMove(NE, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(dist, -1 * dist, true);
				return;
			}
		}
		if (left && down) {
			myself.setface(WEST);
			int dist = MaxPlayerMove(SW, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(-1 * dist, dist, true);
				return;
			}
		}
		if (right && down) {
			myself.setface(EAST);
			int dist = MaxPlayerMove(SE, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(dist, dist, true);
				return;
			}
		}

		// check four cardinal directions last
		if (up) {
			myself.setface(NORTH);
			int dist = MaxPlayerMove(NORTH, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(0, -1 * dist, true);
				return;
			}

			if (playerdiagonals) {
				// check for sliding along walls if we permit diagonals
				dist = MaxPlayerMove(NW, playerstep);
				if (dist != 0) {
					myself.setface(WEST);
					myself.set_waypoint_relative(-1 * dist, -1 * dist, true);
					return;
				}

				dist = MaxPlayerMove(NE, playerstep);
				if (dist != 0) {
					myself.setface(EAST);
					myself.set_waypoint_relative(dist, -1 * dist, true);
					return;
				}
			}
		}
		if (down) {
			myself.setface(SOUTH);
			int dist = MaxPlayerMove(SOUTH, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(0, dist, true);
				return;
			}

			if (playerdiagonals) {
				// check for sliding along walls if we permit diagonals
				dist = MaxPlayerMove(SW, playerstep);
				if (dist != 0) {
					myself.setface(WEST);
					myself.set_waypoint_relative(-1 * dist, 1 * dist, true);
					return;
				}

				dist = MaxPlayerMove(SE, playerstep);
				if (dist != 0) {
					myself.setface(EAST);
					myself.set_waypoint_relative(dist, dist, true);
					return;
				}
			}
		}
		if (left) {
			myself.setface(WEST);
			int dist = MaxPlayerMove(WEST, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(-1 * dist, 0, true);
				return;
			}

			if (playerdiagonals) {
				// check for sliding along walls if we permit diagonals
				dist = MaxPlayerMove(NW, playerstep);
				if (dist != 0) {
					myself.setface(WEST);
					myself.set_waypoint_relative(-1 * dist, -1 * dist, true);
					return;
				}

				dist = MaxPlayerMove(SW, playerstep);
				if (dist != 0) {
					myself.setface(WEST);
					myself.set_waypoint_relative(-1 * dist, 1 * dist, true);
					return;
				}
			}
		}
		if (right) {
			myself.setface(EAST);
			int dist = MaxPlayerMove(EAST, playerstep);
			if (dist != 0) {
				myself.set_waypoint_relative(dist, 0, true);
				return;
			}

			if (playerdiagonals) {
				// check for sliding along walls if we permit diagonals
				dist = MaxPlayerMove(NE, playerstep);
				if (dist != 0) {
					myself.setface(EAST);
					myself.set_waypoint_relative(dist, -1 * dist, true);
					return;
				}

				dist = MaxPlayerMove(SE, playerstep);
				if (dist != 0) {
					myself.setface(EAST);
					myself.set_waypoint_relative(dist, dist, true);
					return;
				}
			}
		}

		// Check for entity/zone activation
		if (b1) {
			int ex = 0, ey = 0;
			UnB1();
			switch (myself.face) // face
			{
			case NORTH:
				ex = myself.getx() + (myself.chr.hw / 2);
				ey = myself.gety() - 1;
				break;
			case SOUTH:
				ex = myself.getx() + (myself.chr.hw / 2);
				ey = myself.gety() + myself.chr.hh + 1;
				break;
			case WEST:
				ex = myself.getx() - 1;
				ey = myself.gety() + (myself.chr.hh / 2);
				break;
			case EAST:
				ex = myself.getx() + myself.chr.hw + 1;
				ey = myself.gety() + (myself.chr.hh / 2);
				break;
			}

			int i = EntityAt(ex, ey);
			if (i != -1) { // FIXME && entity.get(i).movescript.length() > 0) {
				if (entity.get(i).autoface) { // FIXME && entity.get(i).ready()) {
					switch (myself.face) // face
					{
					case NORTH:
						entity.get(i).setface(SOUTH);
						break;
					case SOUTH:
						entity.get(i).setface(NORTH);
						break;
					case WEST:
						entity.get(i).setface(EAST);
						break;
					case EAST:
						entity.get(i).setface(WEST);
						break;
					default:
						System.err.println("ProcessControls() - uwahh? invalid myself.face parameter");
					}
				}

				event_tx = entity.get(i).getx() / 16;
				event_ty = entity.get(i).gety() / 16;
				event_entity = i;
				int cur_timer = timer;
				beforeEntityActivation();
				Script.callfunction(entity.get(i).script);
				entity.get(i).clear_waypoints(); // Rafael
				afterEntityActivation();
				timer = cur_timer;
				return;
			}

			int cz = current_map.getzone(ex / 16, ey / 16);
			if (cz > 0 && current_map.getScriptZone(cz).length() > 0
					&& current_map.getMethodZone(cz) > 0) {
				int cur_timer = timer;

				event_zone = cz;
				event_tx = ex / 16;
				event_ty = ey / 16;
				event_entity = i;

				Script.callfunction(current_map.getScriptZone(cz));
				timer = cur_timer;
			}
		}
	}

	static void MapScroller(VImage dest) {
		inscroller = true;
		int oldx = xwin;
		int oldy = ywin;
		int oldtimer = timer;
		int oldcamera = cameratracking;
		cameratracking = 0;
		clearLastKey(); // lastpressed = 0;

		while (getLastKeyChar() != 41) {
			if (getKey(KeyUp))
				ywin--;
			if (getKey(KeyDown))
				ywin++;
			if (getKey(KeyLeft))
				xwin--;
			if (getKey(KeyRight))
				xwin++;
			Controls.UpdateControls();
			RenderMap(dest);
			showpage();
		}

		clearLastKey(); // lastpressed = 0;
		clearKey(41); // keys[41] = 0;
		cameratracking = oldcamera;
		timer = oldtimer;
		ywin = oldy;
		xwin = oldx;
		inscroller = false;
	}

	private static void complyToLimits(VImage dest, int mapRight, int mapDown) {

		if (!current_map.getHorizontalWrapable()) { // Rafael: new code
			if (xwin + dest.width >= mapRight)
				xwin = mapRight - dest.width;
			if (xwin < 0)
				xwin = 0;
		}
		if (!current_map.getVerticalWrapable()) { // Rafael: new code
			if (ywin + dest.height >= mapDown)
				ywin = mapDown - dest.height;
			if (ywin < 0)
				ywin = 0;
		}
	}
	
	
	public static final int CAMERA_STATIC = 0;
	public static final int CAMERA_PLAYER = 1;
	public static final int CAMERA_ENTITY = 2;
	public static final int CAMERA_TRANSITION = 3;
	
	public static void RenderMap(VImage dest) {
		if (current_map == null) {
			return;
		}
		if (!inscroller && getLastKeyChar() == 41)
			MapScroller(dest);

		int rmap = currentMapZoneWidth;
		int dmap = currentMapZoneHeight;

		switch (cameratracking) {
		case CAMERA_STATIC:
			complyToLimits(dest, rmap, dmap);
			break;
		case CAMERA_PLAYER:
			if (myself != null) {
				xwin = (myself.getx() + myself.chr.hw / 2) - (dest.width / 2) -8;
				ywin = (myself.gety() + myself.chr.hh / 2) - (dest.height / 2) -24;
			} else {
				xwin = 0;
				ywin = 0;
			}
			complyToLimits(dest, rmap, dmap);
			break;
		case CAMERA_ENTITY:
			if (cameratracker >= numentities || cameratracker < 0) {
				xwin = 0;
				ywin = 0;
			} else {
				xwin = (entity.get(cameratracker).getx() + 8)
						- (dest.width / 2);
				ywin = (entity.get(cameratracker).gety() + 8)
						- (dest.height / 2);
			}
			complyToLimits(dest, rmap, dmap);
			break;
			
		case CAMERA_TRANSITION: // Rafael: New camera tracking mode = scrolling transition (Zelda-like)

			if (myself != null) {

				if(myself.getx() - xwin <= -8) { // scroll left
					setentitiespaused(true);
					myself.setx((myself.getx()/16)*16);
					xwin = (xwin/dest.width)*dest.width -4;
					while(xwin % dest.width != 0) {
						xwin-=4;
						current_map.render(xwin, ywin, dest);
						showpage();
					}
					setentitiespaused(false);
				}
				if(myself.getx() - xwin >= dest.width) { // scroll right
					setentitiespaused(true);
					xwin = (xwin/dest.width)*dest.width +4;
					while(xwin % dest.width != 0) {
						xwin+=4;
						current_map.render(xwin, ywin, dest);
						showpage();
					}
					setentitiespaused(false);
				}
				if(myself.gety() - ywin <= -8) { // scroll up
					setentitiespaused(true);
					myself.sety((myself.gety()/16)*16);
					ywin = (ywin/dest.height)*dest.height -4;
					while(ywin % dest.height != 0) {
						ywin-=4;
						current_map.render(xwin, ywin, dest);
						showpage();
					}
					setentitiespaused(false);
				}
				if(myself.gety() - ywin >= dest.height) { // scroll down
					setentitiespaused(true);
					ywin = (ywin/dest.height)*dest.height +4;
					while(ywin % dest.height != 0) {
						ywin+=4;
						current_map.render(xwin, ywin, dest);
						showpage();
					}
					setentitiespaused(false);
				}				
			} else {
				xwin = 0;
				ywin = 0;
			}
			
			complyToLimits(dest, rmap, dmap);
			break;
			
		}
		
		// Doesn't work if systemtime is not updated! // RBP Map rendering skip to accelerate drawing
		//if(framecount>=2) {
		current_map.render(xwin, ywin, dest);
			//framecount=0;
		//}
		//framecount++;
		
	}

	//static int framecount = 0;

	static void CheckZone() {
		int cur_timer = timer;
		int cz = current_map.getzone(px, py);
		// the following line is probably now correct, since .percent is in
		// [0,255]
		// and so the max rnd() will produce is 254, which will still always
		// trigger
		// if .percent is 255, and the lowest is 0, which will never trigger,
		// even if
		// .percent is 0
		int rnd = (int) (255 * Math.random());
		if (rnd < current_map.getPercentZone(cz)) {
			event_zone = cz;
			Script.callfunction(current_map.getScriptZone(cz));
		}
		timer = cur_timer;
	}

	public static void TimedProcessEntities() {
		if (entitiespaused || Controls.MENU_OPEN )
			{ return; }

		while (lastentitythink < systemtime) {
			if (done)
				break;
			if (myself != null) {
				px = (myself.getx() + (myself.chr.hw / 2)) / 16;
				py = (myself.gety() + (myself.chr.hh / 2)) / 16;
			}
			ProcessEntities();
			if (invc == 0)
				ProcessControls();
			if (myself != null && invc == 0) {
				if ((px != (myself.getx() + (myself.chr.hw / 2)) / 16)
						|| (py != (myself.gety() + (myself.chr.hh / 2)) / 16)) {
					px = (myself.getx() + (myself.chr.hw / 2)) / 16;
					py = (myself.gety() + (myself.chr.hh / 2)) / 16;

					event_tx = px;
					event_ty = py;

					onStep();
					CheckZone();
					afterStep();
				}
			}
			lastentitythink++;
		}
	}

	public static void TimedProcessSprites() {
		while (lastspritethink < systemtime) {
			for (int i = 0; i < sprites.size(); i++) {
				if (sprites.get(i).image == null)
					continue;
				if (sprites.get(i).wait > 0) {
					sprites.get(i).wait--;
					continue;
				}
				sprites.get(i).timer++;
				sprites.get(i).thinkctr++;
				if (sprites.get(i).thinkctr > sprites.get(i).thinkrate) {
					sprites.get(i).thinkctr = 0;
					event_sprite = i;
					Script.callfunction(sprites.get(i).thinkproc);
				}
			}
			lastspritethink++;
		}
	}

	public void run() 
		{

			// Krybo (Mar.2016) : System menus are built once, here.
			// needed to move this here so menus can be modified
			// by users in the autoexec() function. before the map loads.

		VergeEngine.Vmm = new VMenuManager(  );
		VergeEngine.SYSTEM_MENU_FOCUS_ID = 
				Vmm.getSystemMenuFocusID();
		log("System menu init, ID = " + 
			SYSTEM_MENU_FOCUS_ID.toString());

		// Start with 4 user friendly graphics drawing layers.
			core.Script.jvcl = 	new JVCL(4,screen.width,screen.height);
	
		callfunction("autoexec");
		
		while(mapname!=null && !mapname.isEmpty()) 
			{
			log("JVerge Engine Startup  v( "+
				JVERGE_VERSION+" )");
			engine_start();
			log(" --------------------------------------------------------------" );
			log("Entering Area: " + mapname);
			
			// Game Map Loop
			while(!done) {
				updateControls();
				//TimedProcessEntities();
				while (!die) 
					{
					updateControls();

					if(virtualScreen==null) {
						screen.render();
					}
					else {
						virtualScreen.render();
					}
					
//					if(!die) // redundant?
//						showpage();

			/* Krybo : June-2015 : showpage() hard substitution 
			 * agree there were some redundancies affecting
			 * performance.   especially the duplicate UpdateControls
			 * Which was removed.
			 */
					
					lastchangetime = 0;
					
					// Practically the same as screen.render() above, so removed
//					if(virtualScreen!=null) 
//						{ screen.blit(0, 0, virtualScreen); }
// Seemingly unnecessary
//				Controls.UpdateControls();
				
				DefaultTimer();	//[Rafael, the Esper]
				GUI.paintFrame();

				/* Copy clipboard code Stub */

				}

			// Krybo (Jan.2016):  loop flow comment.
			// above engine loop dies when the map changes or player exits verge.
			// If only the map changes, It will proceed to top & restart the engine
			// using the filename within the static var "mapname"

			}
		}
		
		
	}


	public static void engine_start() 
		{
		numentities = 0;
		entity.clear();
		player = -1;
		myself = null;
		xwin = ywin = 0;
		done = false;
		die = false;

//			// Krybo (Mar.2016) : System menus are built once, here.
//		VergeEngine.Vmm = new VMenuManager(  );
//		VergeEngine.SYSTEM_MENU_FOCUS_ID = 
//				Vmm.getSystemMenuFocusID();
//		log("System menu init, ID = " + 
//				SYSTEM_MENU_FOCUS_ID.toString());
		
//		init_system_menus();
		
		// Krybo (Jan 2016) : felt some exception handled is needed here.
		try {
		if( mapname.equals("_map_change_") == true )
			{
			// The contents of current_map have already been changed to the new map.
			currentMapZoneWidth = current_map.getWidth() * 16;
			currentMapZoneHeight = current_map.getHeight() * 16;
			domain.MapVerge.refreshCache();
			}
		else if(mapname.toLowerCase().endsWith(".map")) 
			{
			current_map = new MapVerge(mapname);
			currentMapZoneWidth = current_map.getWidth() * 16;
			currentMapZoneHeight = current_map.getHeight() * 16;
			}
		else {
			current_map = new MapDynamic(mapname);
			}
		
		// Krybo:  Initialize the graphics and menu layers if need.
		if( core.Script.jvcl != null )
			{  
			core.Script.jvcl.destroy();
			core.Script.jvcl = 	new JVCL(4,screen.width,screen.height);
			}
		
		}	catch( Exception e )
			{ 
			System.out.println("!!! Something went wrong with map object creation.");
			e.printStackTrace();
			}

		// CleanupCHRs();
		timer = 0;

		lastentitythink = systemtime;
		lastspritethink = systemtime;		
		
	}

	static int timeIncrement = 1;
	protected static void DefaultTimer() {

		systemtime +=timeIncrement;
		// if (engine_paused) // Rafael: Used only in debug
			// return;
		timer +=timeIncrement;
		hooktimer +=timeIncrement;
	}
	public static void setTimeIncrement(int i) { // Used to speed up some game
		timeIncrement = i;
	}
	
	// RBP Avoid FPS getting higher than needed, after spending lot of time loading 
	public static void syncAfterLoading() {
		GUI.cycleTime = System.currentTimeMillis();
	}
	
	public static void initVergeEngine(String[] args) 
		{
		// Krybo (May.2016)  Logs some information about the system.
		//    Helpful if system specific bugs are found.
		log("Hello JVerge : "+ JVERGE_VERSION.toString() );
		VergeEngine.logSystemProperties();

		if( VergeEngine.JAVA_VERSION < 1.6 )
			{
			String stp = 
					new String(" !! System Requirement violation:"+
					"old JVM : JVerge requires java 1.6 or later.");
			GUI.showMessage( stp );
			exit( stp );
			return;
			}

		 /* Krybo (May.2016)  Save up Initialization Current Working DIR */
		try	{
			if( VergeEngine.isExecJar() )
				{  
				// if we're in a jar file.. must use system variable & hope
				//    It will act as a sandbox so the program can write files.
				VergeEngine.JVERGE_CWD = new File( 
					System.getProperty( "user.home" )+File.separator+
					"JVergeProgramData" );
				if( ! VergeEngine.JVERGE_CWD.exists() )
					{
					if( ! VergeEngine.JVERGE_CWD.mkdir() ) 
						{ 
						System.err.println(	"Running in Jar file but "+
							" cannot set up sandbox writable directory. "+
							VergeEngine.JVERGE_CWD.toString()+
							" JVerge will continue but may be doomed "+
							" for I/O Errors.");
						}
					else
						{
						log(" Jar mode sandbox created : "+
							VergeEngine.JVERGE_CWD.toString() );
						}
					}
				}
			else
				{
				VergeEngine.JVERGE_CWD = new File( 
					new java.io.File( "." ).getCanonicalPath() );
				}
			}
		catch (IOException e)
			{	VergeEngine.JVERGE_CWD = null; }
	     System.out.println(" >$ CWD: ["+JVERGE_CWD.toString()+"]");

		if (args !=null && args.length != 0) {
			mapname = args[0];
		}

		// Verge (startup)
		config = new Config(load("verge.cfg"));

		// If the program is called without a particular map to execute, run
		// the default mapname specified in the Config file
		if (mapname == null || mapname.isEmpty()) {
			mapname = config.getMapname();
			log("Mapname from config file: " + mapname);
		}
		
		// Krybo (Mar.2016)  configurable master menu key code.
		if( config.getSysMenuKeycode() > -1 )
			{
			MENU_MASTERKEY = config.getSysMenuKeycode();
			}
		log(" Menu master key is : "+Integer.toString(MENU_MASTERKEY) );

		// [Rafael, the Esper]: See http://www.cap-lore.com/code/java/JavaPixels.html
		// config.v3_xres = config.v3_xres * 2;
		// config.v3_yres = config.v3_yres * 2;

		screen = new VImage(config.getV3_xres(), config.getV3_yres() );
		screenZOOM = new VImage(config.getV3_xres(), config.getV3_yres() );
		screenHalfWidth = (Integer) (screen.width / 2);	// for optimizations
		screenHalfHeight = (Integer) (screen.height / 2);

		// Unused: useful for frameskipping
		//finalScreen = new VImage(config.getV3_xres(), config.getV3_yres());

		if (config.isWindowmode()) {
			gui = new GUI(config.getV3_xres(), config.getV3_yres());
		} else {
			gui = new GUI(0, 0);
		}

		getGUI().updateCanvasSize();

	}

	public static boolean isExecJar()
		{
		return( VergeEngine.class.getResource(
				"VergeEngine.class").getProtocol().equals("jar") );
		}
	
	/** Logs a good deal of basic system properties. */
	public static void logSystemProperties()
		{		
		log(  " System properties:   JVM "+	JAVA_VERSION.toString() +   
			" Operating System: " + System.getProperty("os.name") +
			" " + System.getProperty("os.version") +
			" (" + System.getProperty("os.arch") + 
			")   User Home: " + System.getProperty("user.home" )
				);
		if( VergeEngine.isExecJar() )
			{ log(" Running from an executable jar."); }
		else { log("Not Running from a jar."); }
		log("-----------------------------------------------------------");
		return;
		}
	
}		// END VergeEngine CLASS

