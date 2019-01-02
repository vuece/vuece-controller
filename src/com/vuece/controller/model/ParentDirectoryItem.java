package com.vuece.controller.model;

import com.vuece.controller.R;

public class ParentDirectoryItem extends DirectoryItem {

	public static String ROOT_NAME="ROOT";
	public ParentDirectoryItem(String name, String uri, int numSongs, int numDirs) {
		super(name, uri, numSongs, numDirs);
		// TODO Auto-generated constructor stub
	}
	@Override
	public int getIcon() {
		// TODO Auto-generated method stub
		return R.drawable.ic_action_goleft;
	}

}
