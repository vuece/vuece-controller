package com.vuece.controller.model;

import com.vuece.controller.R;

public class DirectoryItem extends DisplayItem {

	public static String DIRECTORY_SUB_TITLE_LEFT="Directory";
	public static String DIRECTORY_SUB_TITLE_RIGHT=" ";
	protected String name;
	protected String uri;
	protected int numSongs;
	protected int numDirs;
	public DirectoryItem(String name, String uri, int numDirs, int numSongs){
		this.name=name;
		this.uri=uri;
		this.numSongs=numSongs;
		this.numDirs=numDirs;
	}
	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String getSubTitleLeft() {
		// TODO Auto-generated method stub
		return DIRECTORY_SUB_TITLE_LEFT;
	}

	@Override
	public String getUri() {
		// TODO Auto-generated method stub
		return uri;
	}
	
	public int getNumSongs () {
		return numSongs;
	}
	public int getNumDirs() {
		return numDirs;
	}
	@Override
	public int getIcon() {
		// TODO Auto-generated method stub
		return R.drawable.ic_action_folder_open;
	}
	@Override
	public String getSubTitleRight() {
		// TODO Auto-generated method stub
		StringBuffer sb=new StringBuffer();
		if (numDirs>0) {
			sb.append(numDirs).append(" folder");
			if (numDirs>1) sb.append("s");
		}
		if (numSongs>0) {
			if (sb.length()>0) sb.append(" ");
			sb.append(numSongs).append(" song");
			if (numSongs>1) sb.append("s");
		}
		return sb.toString();
//		return numSongs>0||numDirs>0?String.valueOf(numDirs)+"/"+String.valueOf(numSongs):"";
	}

}
