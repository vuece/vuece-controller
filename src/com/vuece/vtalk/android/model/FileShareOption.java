package com.vuece.vtalk.android.model;

public class FileShareOption {
	public static final int T_FILE = 0;
	public static final int T_IMAGE = 1;
	public static final int T_FOLDER = 2;
	public static final int T_MUSIC = 3;
	public static final int T_NONE = 4;
	public String id;
	public String jid;
	public int type;
	public String filename;
	public int size;
	public boolean previewAvailable;
	public FileShareOption(String id, String jid, int type, String filename, int size, boolean previewAvailable){
		this.id = id;
		this.jid=jid;
		this.type=type;
		this.filename=filename;
		this.size=size;
		this.previewAvailable=previewAvailable;
	}
	
	public boolean isPreviewAvailable() {
		return previewAvailable;
	}

	public void setPreviewAvailable(boolean previewAvailable) {
		this.previewAvailable = previewAvailable;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public String toString(){
		return jid+" wants to share "+filename+"(type:"+type+";size:"+size+"bytes;hasPreview:"+previewAvailable+")";
	}
}
