package domain;

import java.net.URL;

import audio.Mp3Player;
import audio.WavPlayer;

public class VSound {

	URL url;
	float volume;
	
	// Krybo (Mar.2016) : check on construct
		public VSound(URL url) 
			{
			this.url = url;
			if(url == null || url.getFile() == null) 
				{
				System.err.println("WARN: Bad VSound.- ");
//				+url.toString() );
				}
			return;
			}
	
	static Mp3Player mp3player;
	static WavPlayer wavplayer;
	
	public void start(float volume)
	{
		if(url == null || url.getFile() == null) {
			System.err.println("No file to play.");
			return;
		}
		
		if(url.getFile().toLowerCase().endsWith("mp3")){
			mp3player = new Mp3Player(url, volume);
			mp3player.play();
		}
		else if(url.getFile().toLowerCase().endsWith("wav")){
			wavplayer = new WavPlayer(url, volume);
			wavplayer.play();
		}
		
	}
	
	
}
