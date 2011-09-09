package com.example.music;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.commonsware.cwac.tlv.TouchListView;
import com.example.music.MusicService.LocalBinder;

public class MusicPlayerTabWidget extends TabActivity {
    MusicListAdapter playListAdapter;
    PlayList playList = PlayList.instance;
    
    // TODO what to do if mService is null when we try to use it?
    MusicService mService = null;

	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    playList.frontEnd = this;
	    
	    // ---- Set up playing controls ----
	    
	    View.OnClickListener playPauseListener = new View.OnClickListener() {
	    	public void onClick(View v) {
	    		if (mService != null) {
	    			mService.processPlayPauseRequest();
	    		}
	    	}
	    };
	    
	    findViewById(R.id.playingIcon).setOnClickListener(playPauseListener);
	    findViewById(R.id.playingInfo).setOnClickListener(playPauseListener);
	    findViewById(R.id.playPauseIcon).setOnClickListener(playPauseListener);
	    
	    SeekBar.OnSeekBarChangeListener seekListener = new SeekBar.OnSeekBarChangeListener() {
	    	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	    		if (fromUser && mService != null) {
	    			mService.setPosition(progress);
	    		}
	    	}
	    	
	    	public void onStartTrackingTouch (SeekBar seekBar) {}
	    	public void onStopTrackingTouch (SeekBar seekBar) {}
	    };
	    
	    ((SeekBar)findViewById(R.id.playingSeekBar)).setOnSeekBarChangeListener(seekListener);
	    
	    // ---- Set up playlist ----
	    
	    playListAdapter = new MusicListAdapter(this, R.layout.playlist_item, R.id.playlist_item_text, playList.files, R.id.playlist_icon, R.id.playlist_item_text);
	    
	    TouchListView playListView = (TouchListView) findViewById(R.id.playListList);
	    playListView.setAdapter(playListAdapter);
	    
	    playListView.setDropListener(new TouchListView.DropListener() {
	    	public void drop(int from, int to) {
	    		AudioFile file = playList.files.remove(from);
	    		playList.files.add(to, file);
	    		playListAdapter.notifyDataSetChanged();
	    	}
	    });
	    
	    playListView.setRemoveListener(new TouchListView.RemoveListener() {
	    	public void remove(int which) {
	    		playList.files.remove(which);
	    		playListAdapter.notifyDataSetChanged();
	    	}
	    });
	    
	    playListView.setOnItemLongClickListener(new OnItemLongClickListener() {
	    	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	    		AudioFile file = playList.files.remove(position);
	    		playList.files.add(0, file);
				playListAdapter.notifyDataSetChanged();
				
				Toast.makeText(getApplicationContext(),
						"Playing " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				if (mService != null) {
	    			mService.processPlayNowRequest();
	    		}
				return true;
			}
    	});
	    
	    // ---- Set up song list ----
	    
	    final List<AudioFile> fileList = scanFiles();
	    final MusicListAdapter fileListAdapter = new MusicListAdapter(this, R.layout.list_item, R.id.list_item_text, fileList, R.id.icon, R.id.list_item_text);
        
	    ListView fileListView = (ListView) findViewById(R.id.fileListView);
	    fileListView.setTextFilterEnabled(true);
	    fileListView.setAdapter(fileListAdapter);
	    
	    fileListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AudioFile file = fileListAdapter.getItem(position);
				
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						"Queued " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				playList.files.add(file);
				playListAdapter.notifyDataSetChanged();
			}
		});
	    
	    fileListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				AudioFile file = fileListAdapter.getItem(position);
				
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						"Playing " + file.getTitle(),
						Toast.LENGTH_SHORT).show();
				
				playList.files.add(0, file);
				playListAdapter.notifyDataSetChanged();
				if (mService != null) {
	    			mService.processPlayNowRequest();
	    		}
				return true;
			}
		});
	    
	    // ---- Set up tabs ----

	    Resources res = getResources(); 	// Resource object to get Drawables
	    TabHost tabHost = getTabHost();  	// The activity TabHost
	    
	    tabHost.addTab(tabHost.newTabSpec("playlist")
	    				      .setIndicator("Playlist", res.getDrawable(R.drawable.ic_tab_artists))
	    				      .setContent(R.id.playListView));
	    tabHost.addTab(tabHost.newTabSpec("songs")
	    					  .setIndicator("Songs", res.getDrawable(R.drawable.ic_tab_artists))	// ic_tab_songs
	    					  .setContent(R.id.fileListView));
	    tabHost.setCurrentTab(0);
	}
	
	
	@Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
        // Start the updater
        updateHandler.postDelayed(new Updater(), 100);
    }
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	mService = null;
        }
    };
	
	
	private List<AudioFile> scanFiles() {
		List<AudioFile> audioFiles = new ArrayList<AudioFile>();
		
		ContentResolver resolver = getBaseContext().getContentResolver();
		
		String[] columns = new String [] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.TRACK,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DURATION,
		};
		
		String where = "IS_MUSIC";
		String[] whereArgs = null;
		
		String orderBy = "Artist, Album, Track, Title";
 		
		Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				columns,
				where,
				whereArgs,
				orderBy);
		
		// TODO handle null => no external storage
		if (cursor != null) {
			while (cursor.moveToNext()) {
				audioFiles.add(
						new AudioFile(
								cursor.getLong(0),
								cursor.getString(1),
								cursor.getString(2),
								cursor.getLong(3),
								cursor.getInt(4),
								cursor.getString(5),
								cursor.getLong(6)
						)
				);
	        }
		}
		
		return audioFiles;
	}

	
	// ----------------------------------------------------------------
	
	private Handler updateHandler = new Handler();
	
    private class Updater implements Runnable {
    	public void run() {
    	    TextView playTime = (TextView) findViewById(R.id.playingTime);
    	    SeekBar bar = (SeekBar) findViewById(R.id.playingSeekBar);
    		ImageView play = (ImageView) findViewById(R.id.playPauseIcon);
    	    
    		if (playList.getCurrent() != null) {
    			int position = mService.getPosition();
    			int duration = (int) playList.getCurrent().getDuration();
    		
	    	    playTime.setText(AudioFile.formatDuration(position) + " / " + AudioFile.formatDuration(duration));
	    	    bar.setMax(duration);
	    	    bar.setProgress(position);
	    	    
	    	    switch (mService.mState) {
	    	    	case Stopped:
	    	    	case Paused:
	    	    		play.setImageResource(R.drawable.play);
	    	    		break;
	    	    	case Preparing:
	    	    	case Playing:
	    	    		play.setImageResource(R.drawable.pause);
	    	    		break;
	    	    }
    		}
    		else {
    			playTime.setText("");
    			bar.setMax(0);
    			bar.setProgress(0);
    			play.setImageResource(R.drawable.play);
    		}    		
    		
    		updateHandler.postDelayed(this, 100);
    	}
    }
    
    public void changeFile(AudioFile file) {
		ImageView art = (ImageView) findViewById(R.id.playingIcon);
		TextView info = (TextView) findViewById(R.id.playingInfo);
		ImageView play = (ImageView) findViewById(R.id.playPauseIcon);
		
		if (file != null) {
			try {
	    		InputStream in = getContentResolver().openInputStream(file.getImageUri());
	    		Bitmap bitmap = BitmapFactory.decodeStream(in);
	    		art.setImageBitmap(bitmap);
	    	}
	    	catch (FileNotFoundException e) {
	    		art.setImageResource(R.drawable.ic_tab_artists_white);
	    	}
			
			info.setText(file.toString());
			play.setImageResource(R.drawable.play);
		}
		else {
			art.setImageBitmap(null);
			info.setText("");
			play.setImageBitmap(null);
		}
    	
		playListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	changeFile(PlayList.instance.getCurrent());
    }
}
