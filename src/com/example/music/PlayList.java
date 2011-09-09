package com.example.music;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Service;

/** Singleton */
public enum PlayList {
	instance;
	
	public List<AudioFile> files = new CopyOnWriteArrayList<AudioFile>();
	private AudioFile current;
	
	// HAAAAAAAAAAACK
	public Service service = null;
	public MusicPlayerTabWidget frontEnd = null;
	
	public synchronized AudioFile getCurrent() {
		return current;
	}
	
	public synchronized AudioFile nextFile() {
		current = null;
		
		if (!files.isEmpty()) {
			current = files.remove(0);
		}
		
		if (frontEnd != null) {
			frontEnd.changeFile(current);
		}
		
		return current;
	}
}
