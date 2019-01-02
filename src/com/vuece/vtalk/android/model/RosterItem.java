package com.vuece.vtalk.android.model;

public class RosterItem {
	private String jid;
	private String substription;
	private String name;
	public RosterItem(String jid, String substription, String name){
		this.jid=jid;
		this.substription=substription;
		this.name=name;
	}
	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getSubstription() {
		return substription;
	}
	public void setSubstription(String substription) {
		this.substription = substription;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String toString(){
		return "jid:"+jid+"; substription:"+substription+"; name:"+name;
	}
}
