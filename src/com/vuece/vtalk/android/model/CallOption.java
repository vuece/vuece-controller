package com.vuece.vtalk.android.model;

public class CallOption {
	private String jid;
	private boolean isVideo;
	private boolean isMuc;
	private int videoBandwidth;
	public CallOption(String jid, boolean isVideo, boolean isMuc, int videoBandwidth){
		this.jid=jid;
		this.isVideo=isVideo;
		this.isMuc=isMuc;
		this.videoBandwidth=videoBandwidth;
	}
	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public boolean isVideo() {
		return isVideo;
	}
	public void setVideo(boolean isVideo) {
		this.isVideo = isVideo;
	}
	public boolean isMuc() {
		return isMuc;
	}
	public void setMuc(boolean isMuc) {
		this.isMuc = isMuc;
	}
	public int getVideoBandwidth() {
		return videoBandwidth;
	}
	public void setVideoBandwidth(int videoBandwidth) {
		this.videoBandwidth = videoBandwidth;
	}
	public String toString(){
		return jid+" is calling, isvideo: "+isVideo+", ismuc:"+isMuc+", videoBandwidth:"+videoBandwidth;
	}
}
