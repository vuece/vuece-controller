package com.vuece.controller.model;

public class MediaItem {
	
	public final static int TYPE_DIR = 1;
	public final static int TYPE_MUSIC = 2;
	public final static int TYPE_VIDEO = 3;
	
	
	private String name;
	private String uri;
	private int type;
	private int length; //in seconds
	private int size;
	private String artist;
	private String album;
	
	public MediaItem(String name, String uri){
		this.name=name;
		this.uri=uri;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}
	
	
}
