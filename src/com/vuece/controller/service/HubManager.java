package com.vuece.controller.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Message;

import com.vuece.controller.model.HubEntry;
import com.vuece.controller.ui.HubEntryAdapter;
import com.vuece.vtalk.android.util.JabberUtils;
import com.vuece.vtalk.android.util.Log;

public class HubManager extends Handler {
	
	private static String TAG = "vuece/HubManager";
	public ArrayList<HubEntry> hubs;
	public HubEntryAdapter adaptor;
	public String username;
	public HubManager() {
		hubs=new ArrayList<HubEntry>();
	}
	@Override
	public void handleMessage(Message msg) {
		// TODO Auto-generated method stub
		String type=msg.getData().getString("type");
		HubEntry newHub=new HubEntry(msg.getData().getString("jid"),msg.getData().getString("name"),msg.getData().getString("id"),msg.getData().getByte("accessible"));
		if (username.equalsIgnoreCase(JabberUtils.normalizeJid(msg.getData().getString("jid")))) {
			newHub.isHome=true;
		}
		if ("add".equals(type)) {
			for (HubEntry hub : hubs) {
				if (hub.jid.equalsIgnoreCase(newHub.jid)) {
					Log.d(TAG, "hub ("+hub.jid+") exists...");
					if (msg.getData().containsKey("name")) {
						Log.d(TAG, "...name changed to "+msg.getData().getString("name"));
						hub.name=msg.getData().getString("name");
					}
					if (msg.getData().containsKey("id")) {
						Log.d(TAG, "...id changed to "+msg.getData().getString("id"));
						hub.id=msg.getData().getString("id");
					}
					if (msg.getData().containsKey("accessible")) {
						Log.d(TAG, "...accessible changed to "+msg.getData().getByte("accessible"));
						hub.accessable=msg.getData().getByte("accessible");
					}
					if (msg.getData().containsKey("db-checksum")) {
						Log.d(TAG, "...db-checksum is "+msg.getData().getString("db-checksum"));
						hub.dbChecksum=msg.getData().getString("db-checksum");
					}
					if (adaptor!=null) adaptor.notifyDataSetChanged();
					return;
				}
			}
			Log.d(TAG, "adding hub: "+newHub);
			hubs.add(newHub);
			if (adaptor!=null) adaptor.notifyDataSetChanged();
		} else if ("remove".equals(type)) {
			String jid=msg.getData().getString("jid");
			Iterator<HubEntry> i=hubs.iterator();
			HubEntry h;
			while (i.hasNext()) {
				h=i.next();
				if (h.jid.equalsIgnoreCase(jid)) {
					i.remove();
					if (adaptor!=null) adaptor.notifyDataSetChanged();
					break;
				}
			}
		}
	}
	public HubEntry getHubByJid(String jid) {
		for (HubEntry hub : hubs) 
			if (hub.jid.equalsIgnoreCase(jid)) return hub;
		return null;
	}
	public void clear() {
		hubs.clear();
	}
}
