package com.vuece.controller.model;

import java.util.ArrayList;
import java.util.List;

import com.vuece.vtalk.android.util.JabberUtils;

public class HubEntry {
	public static byte ACCESSIBLE_UNKNOWN=0;
	public static byte ACCESSIBLE_YES=1;
	public static byte ACCESSIBLE_NO=2;
	public String jid;
	public String name;
	public String id;
	public byte accessable;
	public String dbChecksum;
	public List<String> functions;
	public boolean isHome;
	
	public HubEntry(String jid, String name, String hubId, byte accessable){
		this.jid=jid;
		this.name=name;
		this.id=hubId;
		this.accessable=accessable;
		functions=new ArrayList<String>();
	}
	public String getJid() {
		return jid;
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return name==null?jid:name;
	}
	public void addFunction(String f){
		functions.add(f);
	}
	public void removeFunction(String f){
		functions.remove(f);
	}
	public boolean is(String jid2) {
		if (JabberUtils.normalizeJid(jid).equalsIgnoreCase(JabberUtils.normalizeJid(jid2))) return true;
		return false;
	}
	public List<String> getFunctions() {
		return functions;
	}
	public String toString() {
		return "jid:"+jid+";name:"+name+";id="+id+";accessable="+accessable;
	}
}
