package sully;

import static core.Script.*;
import static sully.Flags.*;
import static sully.Sully.*;
import sully.vc.v1_rpg.V1_Simpletype;

import static sully.vc.v1_rpg.V1_RPG.*;
import static sully.vc.v1_rpg.V1_Music.*;

import static sully.vc.simpletype_rpg.Party.*;
import static sully.vc.simpletype_rpg.Inventory.*;
import static sully.vc.v1_rpg.V1_Maineffects.*;
import static sully.vc.v1_rpg.V1_Textbox.*;
import static sully.vc.v1_menu.Menu_System.*;
import static sully.vc.v1_menu.Menu_Shop.*;
import static sully.vc.Sfx.*;
import static sully.vc.util.General.*;
import static sully.vc.v1_rpg.V1_Simpletype.*;

public class Village {

	public static void start()
	{
		Sully.SaveDisable(); //cannot save in towns.
			
		InitMap();
	
		V1_StartMusic("res/music/DOS3L.S3M");
	
		//don't show the banner when we return from the flashback
		if( flags[F_RODNE_FLASHBACK]==0 )
		{
			Banner("Rodne Town",300);
		}
	
		//sets all the tiles as they should be.
		rodne_upkeepery();
		
	}
	
	
	public static void rodne_upkeepery()
	{
		int darin, dexter;
		
		RodneMasterChestCleanup( CHEST_RODNE_A,51,81 );	
		RodneMasterChestCleanup( CHEST_RODNE_B,51,85 );
		RodneMasterChestCleanup( CHEST_RODNE_C,53,83 );	
		RodneMasterChestCleanup( CHEST_RODNE_D,55,81 );
		RodneMasterChestCleanup( CHEST_RODNE_E,55,85 );
		
		if(flags[F_RODNE_CLEAN_DUST]!=0)
		{
			AlterBTile( 53,59,157,2 );
			AlterBTile( 54,59,158,2 );
			AlterBTile( 55,59,159,2 );
			AlterBTile( 53,60,155,2 );
			AlterBTile( 54,60,195,0 );
			AlterBTile( 55,60,156,2 );
		}
		
		if( flags[F_RODNE_FLASHBACK]!=0 )
		{
			Warp(9,84,TNONE);
			VCScreenFilterOff();
			
			
			darin	= GetPartyEntity( "Darin" );
			dexter	= GetPartyEntity( "Dexter" );
			
			entity.get(darin).face = FACE_UP;
			entity.get(dexter).face = FACE_UP;
			
			flags[F_RODNE_FLASHBACK] = 0;
			
			FadeIn(30);
			
			elder_b();
		}
		
		if( flags[F_CARROT_QUEST]!=0 )
		{
			AlterBTile(13,6,89,2);
		}
	}
	
	public static void n_exit() /* 1 */
	{
		V1_MapSwitch("overworld.map",25,74,0);
	}
	
	public static void e_exit() /* 2 */
	{
		V1_MapSwitch("overworld.map",32,72,0);
	}
	
	public static void villager_a() /* 3 */
	{
		EntStart();
		
		TextBox(0,	"Welcome to the town of Rodne.",
					"Yes, I'm the dork who says the town name",
					"and I'm proud of it!");
				
		EntFinish();
	}
	
	public static void villager_b() /* 4 */
	{
		EntStart();
		
		if( flags[F_CARROT_QUEST]!=0 )
		{
			TextBox(0,	"I... I don't believe it!",
						"Please use the power of the",
						"carrot to destroy Lord Stan.");
						
			EntFinish();
			return;
		}
		
	
		TextBoxM(0,	"Lord Stan has cursed our sky so we never ",
					"have any rain.",
					"Only this carrot can grow.");
				
		TextBox(0,	"We all pray that one day it will grow to",
					"be the Sacred Carrot of Ultimate Power.", "");
	
		if( CharInParty("Dexter") )
		{
			TextBox(T_DEXTER,	"The chance that this root will grow into an",
								"omnipotent holy weapon is exceedingly",
								"remote.");
					
			TextBox(T_DARIN,	"Don't be such an egghead, Dex.","","");
		}
		
		EntFinish();
	}
	
	public static void villager_c() /* 5 */
	{
		EntStart();
		
		TextBox(0,	"If it's a technician you're looking for,",
					"the lab is in the southwest area of the",
					"village.");
		TextBox(0,	"Sara doesn't understand that we live in",
					"simple happiness without evil technology.","");
		
	
		if( CharInParty("Sara") )
		{
			TextBox(T_SARA,"Hey!","","");
		}
		
		EntFinish();
	}
	
	public static void villager_d() /* 6 */
	{
		EntStart();
		
		TextBox(0,	"This is the elder's place.",
					"He will teach you about the heritage of",
					"our village.");
		
	
		if( CharInParty("Dexter") )
		{
			TextBox(3,"Oh, boy! Learning!","","");
		}
		
		EntFinish();
	}
	
	public static void villager_e() /* 7 */
	{
		EntStart();
		
		TextBoxM(0,	"We live in fear of both Lord Stan and",
					"Big Daddy Bubba.",
					"Lord Stan resides in Castle Heck.");
		TextBoxM(0,	"Castle Heck is the treacherous looking",
					"place on the shores of the eastern",
					"peninsula.");
		TextBox(0,	"Big Daddy Bubba is a reclusive hermit",
					"who captures people for his love shack",
					"in the Forest.");
		
	
		if( CharInParty("Sara") )
		{
			TextBox(2,	"Yes, Bubba is a big stinker.",
						"We shall also beat Lord Stan",
						"and his evil lackey, Galfrey!");
					
			if( CharInParty("Galfrey") )
			{
				TextBox(5,"Hey, that hurts...","","");
			}
		}
		
		EntFinish();
	}
	
	public static void villager_f() /* 8 */
	{
		EntStart();
		
		TextBox(0,	"There's a trail leading deep into the Forest on",
					"the east edge of the village.", "");
					
		TextBox(0,	"If you go, be sure to steer clear of",
					"Big Daddy Bubba's love shack.", "");
					
		
	
		if( CharInParty("Sara") && CharInParty("Crystal") )
		{
			TextBox(2,	"Love shack... I bet you and Darin plan on",
						"spending some time there, eh, Crystal?", "");
			TextBox(4,	"Don't make me scratch your eyes out.", "","");
		}
		
		EntFinish();
	}
	
	public static void rabbit() /* 9 */
	{
		EntStart();
		
		if( flags[F_CARROT_QUEST]!=0 )
		{
			TextBox(T_BUNNY,	"You... monster! You absolute",
								"monster! How you could you rob",
								"me of my sole passion in life?");		
						
			EntFinish();
			return;
		}
		
		TextBox(T_BUNNY,	"My sole purpose in life is to eat that",
							"sacred carrot, but that goober keeps",
							"guarding it.");
		TextBox(T_BUNNY,	"Sooner or later, he will grow sleepy.",
							"I shall bide my time, then I shall strike!","");
					
				
		if( CharInParty("Crystal") )
		{
			TextBox(4,	"What an adorable little",
					"rabbit. I shall pet it.","");
	
			if( CharInParty("Sara") && CharInParty("Galfrey") )
			{
				TextBox(2,"Do you think rabbits are",
					"cute, Galfrey?","");
				
				TextBox(5,"Not really.","","");
			}
		}
		
		EntFinish();
	}
	
	public static void bird() /* 10 */
	{
		EntStart();
		
		TextBox(T_BIRD,	"This trail leads deep into the Forest.",
						"Enjoy the serene beauty of the wind",
						"and trees.");
		TextBox(T_BIRD,	"Umm... er... I mean...",
						"*CHIRP*!","");
				
		if( CharInParty("Dexter") )
		{
			TextBox(3,"What a remarkable bird.","","");
		}
		
		EntFinish();
	}
	
	public static void elder_a() /* 11 */
	{
		EntStart();
		
		TextBox(0,	"The village elder is upstairs.",
				"He remembers the early days of this",
				"crappy little village.");
		if( CharInParty("Sara") )
		{
			TextBox(2,	"I'd really rather you not know",
						"about this, Darin.","");
	
			if( CharInParty("Crystal") )
			{
				TextBox(4,	"What's the deal, Darin? Juicy",
							"secrets about Sara's sordid",
							"past? I must know!");
			}
		}
		
		EntFinish();
	}
	
	public static void elder_b() /* 12 */
	{
		EntStart();
		
		if( CharInParty("Sara") )
		{
			TextBox(0,	"Good luck, young folks. Enjoy",
						"your stay here in Rodne.","");
			
			EntFinish();
			return;
		}
		
		if( flags[F_RODNE_FLASHBACK]!=0 )
		{
			TextBox(0,	"Good luck, young folks. Enjoy",
						"your stay here in Rodne.","");
			
			EntFinish();
			return;
		}
		
		if( flags[F_RODNE_FLASH_OVER]==0 && !CharInParty("Sara") )
		{
			TextBoxM(0,	"I am the village elder.",
						"Have a seat and I will tell you about the",
						"ancestry of Rodne.");
			
			TextBoxM(0,	"It all began when a young engineer named ",
						"Sara started a laboratory in the Forest.", "");
			TextBox(0,	"Her machine to clone rats exploded and",
						"created all the clone people you see here.", "");
		
			
			
			VCCustomFilter( RGB(0,0,0), RGB(255,128,64) );
			VCScreenFilterLucent( 20 );
			
			
			MenuOff();
	
			RemovePlayer( "Dexter" );	//
			RemovePlayer( "Darin" );	//remove the boys for the flashback!
			
			
			JoinParty("Sara", 2);	//It's Peanut Butter Sara Time!
	
			flags[F_RODNE_FLASHBACK] = 1;
		
			V1_MapSwitch("OLDVILLE.MAP",40,10,0);
		}
		
		TextBoxM(0,	"So you see, it's not because of a lazy",
					"sprite artist, but rather it's scientific.", "");
					
		TextBox(0,	"Sara still lives here,",
					"designing her machines and making the",
					"world spiffier.");
		
		
		EntFinish();
	}
	
	public static void Blacksmith() /* 13 */
	{
		EntStart();
		
		TextBox(0,	"I am Thor, the blacksmith.",
					"Well met, travelers! What",
					"can I do for you today?");
		
		SetSellEquipmentShop(true);
		SetSellSupplyShop(false);
		MenuShop("Sting_Whip&Lead_Wrench&Iron_Sword&Steel_Lance&Bracer&Buckler&Tower_Shield&Cloak,Robe&Titanium_Suit&Tiara&Head_Brace");
		
	
		TextBox(0,	"I thank you, sir! Good day.","","");
		
		EntFinish();
	}
	
	public static void Pharmacy() /* 14 */
	{
		EntStart();
		
		TextBox(0,	"You look like a nice bunch of kids. ",
					"I'll show you the real medicine, not just", "Tic-Tacs.");
		
		SetSellEquipmentShop(false);
		SetSellSupplyShop(true);
	
		MenuShop("Herb&Medicine&Miracle_Brew&Starlight&Fury_Ring&Protect_Locket");
		
		TextBox(0,	"Aww... come on!",
					"Buy one more medicine, please?","");
					
		EntFinish();
	}
	
	public static void Rat() /* 15 */
	{
		EntStart();
		
		if( flags[F_LOVE_SARA_JOIN]!=0 || flags[F_RODNE_TALKRAT]!=0 )
		{
			TextBox(T_SLASHER,	"Seeya, guys!",
								"Have fun defeating Lord Stan!","");
	
			if( CharInParty("Sara") )
			{
				TextBox(T_SARA,	"I'll be back soon, Slasher,",
								"and I'll be sure to pick up",
								"some sunflower seeds.");
			}
			
			EntFinish();
			return;
		}
		
		//Do this if Sara hasn't joined yet.
		if( flags[F_LOVE_SARA_JOIN]==0 )
		{
			TextBox(T_SLASHER,	"Squeak! Squeak!","","");
			
			TextBox(T_DARIN,	"How terrible! They're enslaving and",
								"experimenting on poor helpless animals!", "");
						
			TextBoxM(T_SLASHER,	"No, actually Sara treats me well, except I",
								"don't have any friends.", "");
						
			TextBox(T_SLASHER,	"After Sara's machine blew up, she was",
								"unable to clone any more cute little rats.", "");
			
			TextBox(T_DEXTER,	"Sara?",
								"Yes, we need to speak to her.",
								"Where is she?");
						
			TextBox(T_SLASHER,	"She went into the Forest to find some",
								"wood for her new machine.", "");
						
			TextBox(T_DARIN,	"Well, we can't wait for her to get back.",
								"Dexter, let's go into the Forest to find her.", "");
						
			TextBox(T_DEXTER,	"Agreed.",
								"Thank you, cute little rat.","");
		}
		
		flags[F_RODNE_TALKRAT] = 1;
		
		EntFinish();
	}
	
	public static void Elder_Enter() /* 16 */
	{
		Warp( 8,67, TCROSS );
		Banner( "Elder's Home",100 );
	}
	
	public static void Weap_Enter() /* 17 */
	{
		Warp( 30,69, TCROSS );
		Banner( "Blacksmith",100 );
	}
	
	public static void Item_Enter() /* 18 */
	{
		Warp( 34,90, TCROSS );
		Banner( "Pharmacy",100 );
	}
	
	public static void Sara_Enter() /* 19 */
	{
		Warp( 53,69, TCROSS );
		Banner( "Laboratory",100 );
	}
	
	public static void Elder_Exit() /* 20 */
	{
		Warp(11,21, TCROSS);
	}
	
	public static void Elder_Upstair() /* 21 */
	{
		Warp( 11,79, TCROSS );
		Banner( "2F",100 );
	}
	
	public static void Elder_Down() /* 22 */
	{
		Warp( 13,61, TCROSS );
	}
	
	public static void Sara_Upstair() /* 23 */
	{
		Warp( 73,64, TCROSS );
	}
	
	public static void Sara_Down() /* 24 */
	{
		Warp( 53,80, TCROSS );
		Banner( "Basement",100 );
	}
	
	public static void Basement_Up() /* 25 */
	{
		if( flags[F_RODNE_CLEAN_DUST]==0 && CharInParty("Sara") )
		{
			EntStart();
			TextBoxM(T_SARA,	"This leads to my basement.",
								"Let me remove the dust that",
								"conceals the passage.");
			
			AlterBTile(53,59,157,2);
			AlterBTile(54,59,158,2);
			AlterBTile(55,59,159,2);
			AlterBTile(53,60,155,2);
			AlterBTile(54,60,195,0);
			AlterBTile(55,60,156,2);
			
			TextBox(T_SARA,	"Please feel free to enter.",
							"The Activator is in my",
							"storage room below.");
						
			flags[F_RODNE_CLEAN_DUST] = 1;
			EntFinish();
			return;
		}
		
		if( flags[F_RODNE_CLEAN_DUST]!=0 )
		{
			Warp( 73,68, TCROSS );
		}
	}
	
	public static void Basement_Down() /* 26 */
	{
		Warp( 54,61, TCROSS );
	}
	
	public static void Weap_Exit() /* 27 */
	{
		Warp( 33,21, TCROSS );
	}
	
	public static void Item_Exit() /* 28 */
	{
		Warp( 29,33, TCROSS );
	}
	
	public static void Sara_Exit() /* 29 */
	{
		Warp( 7,35, TCROSS );
	}
	
	public static void Chest_A() /* 30 */
	{
		if( OpenTreasure(CHEST_RODNE_A, 51,81, 177 ) )
		{
			FindItem( "Medicine", 1 );
		}
	}
	
	public static void Chest_B() /* 31 */
	{
		if( OpenTreasure(CHEST_RODNE_B, 51,85, 177 ) )
		{
			FindItem( "Gold_Helmet", 1 );
		}
	}
	
	public static void Chest_C() /* 32 */
	{
		if( OpenTreasure(CHEST_RODNE_C, 53,83, 177 ) )
		{
			FindItem( "Thermal_Activator", 1 );
		}
	}
	
	public static void Chest_D() /* 33 */
	{
		if( OpenTreasure(CHEST_RODNE_D, 55,81, 177 ) )
		{
			FindItem( "Blur_Ring", 1 );
		}
	}
	
	public static void Chest_E() /* 34 */
	{
		if( OpenTreasure(CHEST_RODNE_E, 55,85, 177 ) )
		{
			FindMoney( 450 );
		}
	}
	
	public static void RodneMasterChestCleanup( int flag_idx, int tile_x, int tile_y ) 
	{
		if( flags[flag_idx]!=0 )
		{
			AlterBTile( tile_x,tile_y, 177,2 );
		}
	}
	
	public static void Carrot() /* 39 */
	{
		int save_vol = V1_GetCurVolume(); //save the current volume to restore later
		
		EntStart();
		
		if( HasItem("Pearl_of_Truth") && flags[F_CARROT_QUEST]==0)
		{
			TextBox(1,	"This is strange... the [Pearl of Truth]",
						"is glowing brighter when I hold it down",
						"here.");
	
			WhiteOut(100);
			
			AlterBTile(13,6,89,2);
			
			WhiteIn(100);
			
			TextBox(1,	"Wow! The Pearl has lifted the",
						"[Sacred Carrot of Ultimate Power] from",
						"the earth!");
			
			V1_FadeOutMusic( 100 );
	
			SoundFanfare();
			
			FindItem("Sacred_Carrot",1);
			Wait( 500 );
			
			flags[F_CARROT_QUEST] = 1; //first step in the CARROT QUEST!
			
			V1_StartMusic("res/music/DOS3L.S3M");
			V1_FadeInMusic( 100, save_vol );//restore the volume
			
		}
		
		V1_SetCurVolume(save_vol);
		
		EntFinish();
	}
	
	
	public static void machine()
	{
		EntStart();
		
		TextBox(T_DARIN,	"What is this monstrosity?",
							"It looks like a mangled wreck",
							"of pipes.");
	         
		if( CharInParty("Sara") )
		{
			TextBox(T_SARA,	"I'm *trying* to re-build it,",
							"thank you very much. That's",
							"why I needed the wood.");
		}
		
		EntFinish();
	}
	
}