package core;

import static core.Controls.*;
import static core.Script.*;
import static core.Sprite.RenderSpritesAboveEntity;
import static core.Sprite.RenderSpritesBelowEntity;
import static core.Sprite.sprites;
import static domain.Entity.EAST;
import static domain.Entity.NE;
import static domain.Entity.NORTH;
import static domain.Entity.NW;
import static domain.Entity.SE;
import static domain.Entity.SOUTH;
import static domain.Entity.SW;
import static domain.Entity.WEST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import domain.Config;
import domain.Entity;
import domain.Map;
import domain.VImage;

public class VergeEngine extends Thread {

	public static boolean done, inscroller = false;
	public static int px, py;
	
	public static int lastentitythink;
	public static int lastspritethink = 0;

	public static boolean die;
	//rbp	public static int vc_paranoid, vc_arraycheck;

	
	static GUI gui;
	
	public static GUI getGUI() {
		return gui;
	}

	// RBP
	private static int time_increment = 2;

	
	/****************************** data ******************************/

	// rbp
	static Config config = null;
	protected static String mapname;
	public static Class systemclass; // rbp


	/****************************** code ******************************/

	// main engine code

	public static int AllocateEntity(int x, int y, String chr) {
		Entity e = new Entity(x, y, chr);
		e.index = numentities;
		entity.add(e);
		return numentities++;
	}

	protected static class EntityComparator implements Comparator<Entity> {
		public int compare(Entity ent1, Entity ent2) {
			return ent1.gety() - ent2.gety();
		}
	}

	/*
	 * RBP static int CDECL cmpent(const void* a, const void* b) { return
	 * entity[*(byte*)a].gety() - entity[*(byte*)b].gety(); }
	 */

	public static void RenderEntities() {
		List<Entity> entidx = new ArrayList<Entity>();
		int entnum = 0;

		// Build a list of entities that are onscreen/visible.
		// FIXME: Make it actually only be entities that are onscreen
		for (int i = 0; i < numentities; i++) {
			entidx.add(entity.get(i));
			entnum++;
		}

		// Ysort that list, then draw.
		Collections.sort(entidx, new EntityComparator());
		// qsort(entidx, entnum, 1, cmpent);
		for (int i = 0; i < entnum; i++) {
			RenderSpritesBelowEntity(i); // rbp entidx.get(i));
			setlucent(entidx.get(i).lucent);
			entidx.get(i).draw();
			setlucent(0);
			RenderSpritesAboveEntity(i); // rbp entidx.get(i));
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
		if (current_map.obstructpixel(x, y)) {

			if (isEntityCollisionCapturing()) {
				event_tx = x / 16;
				event_ty = y / 16;
				event_entity = __grue_actor_index;
				event_zone = current_map.zone(x / 16, y / 16);
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
			Script.executefunctionstring(_trigger_onStep);
		}
	}

	static void afterStep() {
		if (!_trigger_afterStep.isEmpty()) {
			Script.executefunctionstring(_trigger_afterStep);
		}
	}

	void afterPlayerMove() {
		if (!_trigger_afterPlayerMove.isEmpty()) {
			Script.executefunctionstring(_trigger_afterPlayerMove);
		}
	}

	static void beforeEntityActivation() {
		if (!_trigger_beforeEntityScript.isEmpty()) {
			Script.executefunctionstring(_trigger_beforeEntityScript);
		}
	}

	static void afterEntityActivation() {
		if (!_trigger_afterEntityScript.isEmpty()) {
			Script.executefunctionstring(_trigger_afterEntityScript);
		}
	}

	static void onEntityCollision() {
		if (isEntityCollisionCapturing()) {
			Script.executefunctionstring(_trigger_onEntityCollide);
		}
	}

	public static void ProcessControls() {
		Controls.UpdateControls();
		// No player movement can be done if there's no ready player, or if
		// there's a script active.
		if (myself == null || !myself.ready() || invc != 0) {
			return;
		}

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
			//System.out.println(i + " " + entity.get(i).movecode);
			//System.exit(0);
			System.out.println("Chegou aqui 1");
			if (i != -1) { // FIXME && entity.get(i).movescript.length() > 0) {
				System.out.println("Chegou aqui 2 " + entity.get(i).ready());
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
						System.err
								.println("ProcessControls() - uwahh? invalid myself.face parameter");
					}
				}
				System.out.println("Chegou aqui 3");
				event_tx = entity.get(i).getx() / 16;
				event_ty = entity.get(i).gety() / 16;
				event_entity = i;
				int cur_timer = timer;
				beforeEntityActivation();
				Script.executefunctionstring(entity.get(i).script);
				entity.get(i).clear_waypoints(); // rbp
				afterEntityActivation();
				timer = cur_timer;
				return;
			}

			int cz = current_map.zone(ex / 16, ey / 16);
			if (cz > 0 && current_map.zones[cz].script.length() > 0
					&& current_map.zones[cz].method > 0) {
				int cur_timer = timer;

				event_zone = cz;
				event_tx = ex / 16;
				event_ty = ey / 16;
				event_entity = i;

				Script.executefunctionstring(current_map.zones[cz].script);
				timer = cur_timer;
			}
		}
	}

	static void MapScroller() {
		inscroller = true;
		int oldx = xwin;
		int oldy = ywin;
		int oldtimer = timer;
		int oldvctimer = vctimer;
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
			RenderMap();
			showpage();
		}

		clearLastKey(); // lastpressed = 0;
		clearKey(41); // keys[41] = 0;
		cameratracking = oldcamera;
		vctimer = oldvctimer;
		timer = oldtimer;
		ywin = oldy;
		xwin = oldx;
		inscroller = false;
	}

	static void RenderMap() {
		if (current_map == null) {
			//hookretrace(); // rbp
			return;
		}
		
		if (!inscroller && getLastKeyChar() == 41)
			MapScroller();

		int rmap = (current_map.getWidth() * 16);
		int dmap = (current_map.getHeight() * 16);

		switch (cameratracking) {
		case 0:
			if (xwin + screen.width >= rmap)
				xwin = rmap - screen.width;
			if (ywin + screen.height >= dmap)
				ywin = dmap - screen.height;
			if (xwin < 0)
				xwin = 0;
			if (ywin < 0)
				ywin = 0;
			break;
		case 1:
			if (myself != null) {
				xwin = (myself.getx() + myself.chr.hw / 2) - (screen.width / 2);
				ywin = (myself.gety() + myself.chr.hh / 2)
						- (screen.height / 2);
			} else {
				xwin = 0;
				ywin = 0;
			}

			if (!current_map.horizontalWrapable) { // rbp new code
				if (xwin + screen.width >= rmap)
					xwin = rmap - screen.width;
				if (xwin < 0)
					xwin = 0;
			}
			if (!current_map.verticalWrapable) { // rbp new code
				if (ywin + screen.height >= dmap)
					ywin = dmap - screen.height;

				if (ywin < 0)
					ywin = 0;
			}
			break;
		case 2:
			if (cameratracker >= numentities || cameratracker < 0) {
				xwin = 0;
				ywin = 0;
			} else {
				xwin = (entity.get(cameratracker).getx() + 8)
						- (screen.width / 2);
				ywin = (entity.get(cameratracker).gety() + 8)
						- (screen.height / 2);
			}
			if (xwin + screen.width >= rmap)
				xwin = rmap - screen.width;
			if (ywin + screen.height >= dmap)
				ywin = dmap - screen.height;
			if (xwin < 0)
				xwin = 0;
			if (ywin < 0)
				ywin = 0;
			break;
		}
		current_map.render(xwin, ywin, screen);
	}

	static void CheckZone() {
		int cur_timer = timer;
		int cz = current_map.zone(px, py);
		// the following line is probably now correct, since .percent is in
		// [0,255]
		// and so the max rnd() will produce is 254, which will still always
		// trigger
		// if .percent is 255, and the lowest is 0, which will never trigger,
		// even if
		// .percent is 0
		int rnd = (int) (255 * Math.random());
		if (rnd < current_map.zones[cz].percent) {
			event_zone = cz;
			Script.executefunctionstring(current_map.zones[cz].script);
		}
		timer = cur_timer;
	}

	public static void TimedProcessEntities() {
		if (entitiespaused)
			return;

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
					Script.executefunctionstring(sprites.get(i).thinkproc);
				}
			}
			lastspritethink++;
		}
	}

	public static void initVergeEngine(String[] args) {

		if (args !=null && args.length != 0) {
			mapname = args[0];
		}

		// Verge (startup)
		config = new Config(systemclass.getResource("./verge.cfg")); // RBP Check if this is safe in other SOs

		// If the program is called without a particular map to execute, run
		// the default mapname specified in the Config file
		if (mapname == null || mapname.isEmpty()) {
			mapname = config.getMapname();
		}

		// RBP: See http://www.cap-lore.com/code/java/JavaPixels.html
		// config.v3_xres = config.v3_xres * 2;
		// config.v3_yres = config.v3_yres * 2;

		screen = new VImage(config.getV3_xres(), config.getV3_yres());

		if (config.isWindowmode()) {
			gui = new GUI(config.getV3_xres(), config.getV3_yres());
		} else {
			gui = new GUI(0, 0);
		}
	
		getGUI().updateCanvasSize();
		
		//gameLoop(); // start the game loop
	}
	
	public void run() {

		executefunctionstring("autoexec");
		
		while(mapname!=null && !mapname.isEmpty()) {
			System.out.println("Entering: " + mapname);
			engine_start();
			
			// Game Loop
			while(!done) {
				updatecontrols();
				//TimedProcessEntities();
				while (!die) {
					updatecontrols();
					//TimedProcessEntities();
					render();
					if(!die) // rbp
						showpage();
				}
			}
		}
		
		
	}


	public static void engine_start() {
		numentities = 0;
		entity.clear();
		player = -1;
		myself = null;
		xwin = ywin = 0;
		done = false;
		die = false;
		current_map = new Map(mapname);
		// RBP CleanupCHRs();
		timer = 0;

		lastentitythink = systemtime;
		lastspritethink = systemtime;		
		
	}

	protected static void DefaultTimer() {

		//time_increment = 1;
		systemtime += time_increment;
		// if (engine_paused) // RBP Used only in debug
		// return;
		timer += time_increment;
		vctimer += time_increment;
		hooktimer += time_increment;
	}

	
	
// DEPRECATED CODE	
	public static double fps = 45;
	public static double maxframeskip = 4.0; /* max # of frames to skip  */
	public static long target_time = System.currentTimeMillis()+(long)(1000.0/fps);

	// Deprecated
	public static void old_PaintToScreen() { // rbp
		
		if(screen==null) {
			System.err.println("Showpage failed. Screen is null");
			return;
		}

		//System.out.println("colorfilter");
		//Script.colorfilter(1, screen);
		
		//getGUI().getCanvas().setAlpha(((float)currentLucent / 255));
		
		int frames_skipped = 0;
		long cur_time = System.currentTimeMillis();
		if (cur_time < target_time+(long)(500.0/VergeEngine.fps)) {
			// we lag behind less than 1/2 frame -> do full frame.
			// This empirically produces the smoothest animation
			
			//getGUI().getCanvas().setCanvas_screen(screen.getImage());
			//getGUI().getCanvas().repaint();
			DefaultTimer(); // rbp
			frames_skipped=0;
			if (cur_time+3 < target_time) {
				//we even have some time left -> sleep it away
				try {
					Thread.sleep(target_time-cur_time);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				// we don't, just yield to give input handler and
				// painter some time
				Thread.yield();
			}
			target_time += (1000.0/VergeEngine.fps);

		} else {
			// we lag behind a little -> frame skip
			// if we skip too many frames in succession, draw a frame
			if ((++frames_skipped) > VergeEngine.maxframeskip) {
				
				//getGUI().getCanvas().setCanvas_screen(screen.getImage());
				//getGUI().getCanvas().repaint();
				DefaultTimer(); // rbp
				frames_skipped=0;
				target_time=cur_time + (long)(1000.0/VergeEngine.fps);
			} else {
				target_time += (long)(1000.0/VergeEngine.fps);
			}
			// yield to give input handler some time
			Thread.yield();
		}
	}
}