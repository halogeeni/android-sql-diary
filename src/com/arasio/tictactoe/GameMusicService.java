package com.arasio.tictactoe;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class GameMusicService extends Service {

	MediaPlayer player;
	
	@Override
	public IBinder onBind(Intent i) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		player = MediaPlayer.create(this, R.raw.bg_music_loop);
		player.setLooping(true);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		player.stop();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    super.onStartCommand(intent, flags, startId);
	    player.start();
	    return START_STICKY;
	}

}
