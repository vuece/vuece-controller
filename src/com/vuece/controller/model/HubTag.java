package com.vuece.controller.model;

import com.vuece.vtalk.android.util.JabberUtils;

public class HubTag {
	public static final int TYPE_OFFLINE=0;
	public static final int TYPE_MSG=1;
	public static final int TYPE_VOICE=2;
	public static final int TYPE_VIDEO=3;
	private String jid;
	private String displayName;
	private int type;
	private int subscription;
	private boolean hasShare;
	public String getJid() {
		return jid;
	}
	public String getBareJid() {
		return JabberUtils.normalizeJid(jid);
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getSubscription() {
		return subscription;
	}
	public void setSubscription(int subscription) {
		this.subscription = subscription;
	}
	public void canShare(boolean b){
		this.hasShare=b;
	}
	public boolean hasShare(){
		return this.hasShare;
	}
	
}
