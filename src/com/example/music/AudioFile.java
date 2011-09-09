package com.example.music;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

public class AudioFile {
	private final long id;
	private final String artist;
	private final String album;
	private final long album_id;
	private final int disc;
	private final int track;
	private final String title;
	private final long duration;
	
	public AudioFile(long id, String artist, String album, long album_id, int track, String title, long duration) {
		this.id = id;
		this.artist = artist;
		this.album = album;
		this.album_id = album_id;
		this.disc = track / 1000;
		this.track = track % 1000;
		this.title = title;
		this.duration = duration;
	}

	public Uri getUri() {
		return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
	}

	public String getArtist() {
		return artist;
	}

	public String getAlbum() {
		return album;
	}

	public Uri getImageUri() {
		Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
		return ContentUris.withAppendedId(sArtworkUri, album_id);
	}

	public int getDisc() {
		return disc;
	}

	public int getTrack() {
		return track;
	}

	public String getTitle() {
		return title;
	}

	public long getDuration() {
		return duration;
	}
	
	@Override
	public String toString() {
		return artist + "\n" +
		 //album + "\n" +
		 //(track != 0 ? (track + ": ") : "") +
		 title +
		 " (" + formatDuration(duration) + ")";
	}
	
	public static String formatDuration(long millis) {
		long seconds = (millis / 1000) + 1;
		return String.format("%d:%02d", seconds / 60, seconds % 60);
	}
	
}
