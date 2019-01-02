package com.vuece.controller.model;


import com.vuece.controller.R;
import com.vuece.vtalk.android.util.JabberUtils;

public class SongItem extends DisplayItem {

	private String name; //file name
	private String uri;
	private int length; //in seconds
	private int size;
	private String artist;
	private String album;
	private String title;
	private String previewPath;
	private int sampleRate;

	public SongItem(String name, String uri, int length, int size, String title, String artist, String album, int sampleRate) {
		this.name=name;
		this.uri=uri;
		this.length=length;
		this.size=size;
		this.artist=artist;
		this.album=album;
		this.title=title;
		this.sampleRate=sampleRate;
	}
	public int getSampleRate() {
		return sampleRate;
	}
	public String getPreviewPath() {
		return previewPath;
	}
	public void setPreviewPath(String previewPath) {
		this.previewPath = previewPath;
	}
	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return title==null||title.length()==0?name:title;
	}

	@Override
	public String getSubTitleLeft() {
		// TODO Auto-generated method stub
		return artist==null||artist.length()==0?"Unkown Artist":artist;
	}

	@Override
	public String getUri() {
		// TODO Auto-generated method stub
		return uri;
	}
	public String getName() {
		return name;
	}
	public int getLength() {
		return length;
	}
	public int getSize() {
		return size;
	}
	public String getArtist() {
		return artist;
	}
	public String getAlbum() {
		return album==null||album.length()==0?"Unknown Album":album;
	}
	@Override
	public int getIcon() {
		// TODO Auto-generated method stub
		return R.drawable.ic_action_music_1;
	}
	@Override
	public String getSubTitleRight() {
		// TODO Auto-generated method stub
		return JabberUtils.getTimeFormat(getLength());
	}
	public String toString(){
		StringBuffer sb=new StringBuffer();
		sb.append("[Name:").append(name).append("]");
		sb.append("[Artist:").append(artist).append("]");
		sb.append("[Album:").append(album).append("]");
		sb.append("[Length:").append(length).append("]");
		return sb.toString();
	}

}
