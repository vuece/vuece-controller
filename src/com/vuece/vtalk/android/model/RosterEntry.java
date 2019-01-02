package com.vuece.vtalk.android.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.vuece.vtalk.android.util.JabberUtils;

public class RosterEntry implements Comparable{
	private String jid;
	private int priority;
	private String status;
	private int show;
	private boolean hasVoice;
	private boolean hasVideo;
	private boolean hasCamera;
	private boolean hasMuc;
	private boolean hasShare;
	private boolean hasControl;
	private String iconHash;
	private int subscription;

	private String fullname;
	private byte[] icon;
	
	private List<RosterEntry> duplicates;
	
	public RosterEntry(String jid, int priority, String status,
			int show, boolean hasVoice, boolean hasVideo, boolean hasCamera,
			boolean hasMuc, boolean hasShare, boolean hasControl, String iconHash){
		this.jid=jid;
		this.priority=priority;
		this.status=status;
		this.show=show;
		this.hasVoice=hasVoice;
		this.hasVideo=hasVideo;
		this.hasCamera=hasCamera;
		this.hasMuc=hasMuc;
		this.hasShare=hasShare;
		this.hasControl=hasControl;
		this.iconHash=iconHash;
	}
	public RosterEntry(String jid, int subscription){
		this.jid=jid;
		this.subscription=subscription;
		this.priority=-120;
		this.show=0;
		this.hasVoice=false;
		this.hasVideo=false;
		this.hasCamera=false;
		this.hasMuc=false;
	}
	
	public String getDisplayName(){
		return getFullname()==null||fullname.length()==0?JabberUtils.normalizeJid(getJid()):getFullname();
	}
	public String getDisplayStatus(){
		return status==null||status.length()==0?displayShow(show):status;
	}
	private String displayShow(int show) {
		switch (show){
			case 0:return "None";
			case 1:return "Offline";
			case 2:return "XA";
			case 3:return "Away";
			case 4:return "DND";
			case 5:return "Online";
			case 6:return "Chat";
		}
		return "Unknown";
	}
	public String getJid() {
		return jid;
	}
	public String getBareJid(){
		return JabberUtils.normalizeJid(jid);
	}
	public void setJid(String jid) {
		this.jid = jid;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getShow() {
		return show;
	}
	public void setShow(int show) {
		this.show = show;
	}
	public boolean isHasVoice() {
		return hasVoice;
	}
	public void setHasVoice(boolean hasVoice) {
		this.hasVoice = hasVoice;
	}
	public boolean isHasVideo() {
		return hasVideo;
	}
	public void setHasVideo(boolean hasVideo) {
		this.hasVideo = hasVideo;
	}
	public boolean isHasCamera() {
		return hasCamera;
	}
	public void setHasCamera(boolean hasCamera) {
		this.hasCamera = hasCamera;
	}
	public boolean isHasMuc() {
		return hasMuc;
	}
	public void setHasMuc(boolean hasMuc) {
		this.hasMuc = hasMuc;
	}
	public String getIconHash() {
		return iconHash;
	}
	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public byte[] getIcon() {
		return icon;
	}
	public void setIcon(byte[] icon) {
		this.icon = icon;
	}
	public boolean isHasShare() {
		return hasShare;
	}
	public void setHasShare(boolean hasShare) {
		this.hasShare = hasShare;
	}
	public boolean isHasControl() {
		return hasControl;
	}
	public void setHasControl(boolean hasControl) {
		this.hasControl = hasControl;
	}
	public String toString(){
		StringBuffer sb=new StringBuffer();
		sb.append("jid:").append(jid).append(";");
		sb.append("status:").append(status).append(";");
		if (duplicates!=null) {
			sb.append("other-logins:");
			for (RosterEntry dup:duplicates){
				sb.append(dup.getJid()).append(",");
			}
			sb.append("|");
		}
		return sb.toString();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof RosterEntry){
			if (((RosterEntry)o).getBareJid().equalsIgnoreCase(getBareJid())) return true;
		}
		return false;
	}
	public int getSubscription() {
		return subscription;
	}
	public void setSubscription(int subscription) {
		this.subscription = subscription;
	}
	
	public void addDuplicate(RosterEntry entry){
		if (this.duplicates==null) {
			this.duplicates=new ArrayList<RosterEntry>();
		}
		duplicates.add(entry);
	}
	@SuppressWarnings("unchecked")
	private void sortConnections(){
		if (duplicates==null||duplicates.size()==0) return;
		Collections.sort(duplicates);
		if (priority<duplicates.get(0).getPriority()
				||(priority==duplicates.get(0).getPriority()&&duplicates.get(0).isHasVideo())){
			RosterEntry newEntry=new RosterEntry(jid, priority, status,	show, hasVoice, hasVideo, hasCamera, hasMuc, hasShare, hasControl, iconHash);
			duplicates.add(newEntry);
			this.jid=duplicates.get(0).getJid();
			this.priority=duplicates.get(0).getPriority();
			this.status=duplicates.get(0).getStatus();
			this.show=duplicates.get(0).getShow();
			this.hasVoice=duplicates.get(0).isHasVoice();
			this.hasVideo=duplicates.get(0).isHasVideo();
			this.hasCamera=duplicates.get(0).isHasCamera();
			this.hasMuc=duplicates.get(0).isHasMuc();
			this.hasShare=duplicates.get(0).isHasShare();
			this.hasControl=duplicates.get(0).isHasControl();
			duplicates.remove(0);
			Collections.sort(duplicates);
		}
	} 
	synchronized public void update(RosterEntry entry){
		RosterEntry updateEntry=entry;
		boolean update=false;
		if (entry.getJid().equals(jid)||this.subscription>0) {
			if (entry.getShow()==0) { // shown connection has logged out
				if (duplicates!=null&&duplicates.size()>0) { // if there are other connections
					updateEntry=duplicates.remove(0);
					update=true;
				} else { //if there is only this connection
					update=true;
				}
			} else { //shown connection changed status
				update=true;
				sortConnections();
			}
		}else{  // this is a duplicate connection in a different location
			boolean exists=false;
			if (duplicates!=null&&duplicates.size()>0) {
				Iterator<RosterEntry> it=duplicates.iterator();
				while (it.hasNext()) {
					RosterEntry ent=it.next();
					if (entry.getJid().equals(ent.getJid())) {
						exists=true;
						if (entry.getShow()==0) {// a duplicate connection has logged out
							it.remove();
							break;
						} else { //a duplicate connection changed status
							ent.setPriority(entry.getPriority());
							ent.setStatus(entry.getStatus());
							ent.setShow(entry.getShow());
							ent.setHasVoice(entry.isHasVoice());
							ent.setHasVideo(entry.isHasVideo());
							ent.setHasCamera(entry.isHasCamera());
							ent.setHasMuc(entry.isHasMuc());
							ent.setHasShare(entry.isHasShare());
							ent.setHasControl(entry.isHasControl());
							sortConnections();
						}
					}
				}
			}
			if (!exists) { // new duplicate connection
				addDuplicate(entry);
				sortConnections();
			}
		}
		if (update) {
			this.jid=updateEntry.getJid();
			this.priority=updateEntry.getPriority();
			this.status=updateEntry.getStatus();
			this.show=updateEntry.getShow();
			this.hasVoice=updateEntry.isHasVoice();
			this.hasVideo=updateEntry.isHasVideo();
			this.hasCamera=updateEntry.isHasCamera();
			this.hasMuc=updateEntry.isHasMuc();
			this.hasShare=updateEntry.isHasShare();
			this.hasControl=updateEntry.isHasControl();
			this.subscription=0;
		}
	}
	@Override
	public int compareTo(Object entry) {
		if (priority>((RosterEntry)entry).getPriority()) 
			return -1;
		else if (priority<((RosterEntry)entry).getPriority()) 
			return 1;
		return 0;
	}
	
	public List<String> getHubJids(){
		List<String> jids=new ArrayList<String>();
		if (hasControl) jids.add(jid);
		for (RosterEntry re:duplicates){
			if (re.isHasControl()) jids.add(re.getJid());
		}
		return jids;
	}
}
