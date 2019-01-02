package com.vuece.controller.ui;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vuece.controller.R;
import com.vuece.controller.model.HubEntry;
import com.vuece.vtalk.android.util.Log;

public class HubEntryAdapter extends ArrayAdapter<HubEntry> {
	private static String TAG = "vuece/HubEntryAdapter";

	public HubEntryAdapter(Context context, ArrayList<HubEntry> hubs) {
        super(context, R.layout.hub_item, hubs);
     }

     @Override
     public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
     	HubEntry hubEntry = getItem(position);   
     	Log.d(TAG, "displaying hub: "+hubEntry);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
           convertView = LayoutInflater.from(getContext()).inflate(R.layout.hub_item, parent, false);
        }
        // Lookup view for data population
        TextView text = (TextView) convertView.findViewById(R.id.hubName);
        // Populate the data into the template view using the data object
        text.setText(hubEntry.getName());
        // Return the completed view to render on screen
        int statusIcon=R.drawable.ic_action_search;
        if (hubEntry.accessable==HubEntry.ACCESSIBLE_NO) 
        	statusIcon=R.drawable.ic_action_halt;
        else if (hubEntry.accessable==HubEntry.ACCESSIBLE_YES) 
        	statusIcon=R.drawable.ic_action_tick;
        ImageView hubState = (ImageView) convertView.findViewById(R.id.hubState);
        hubState.setImageResource(statusIcon);
        ImageView hubIcon = (ImageView) convertView.findViewById(R.id.hubIcon);
        if (hubEntry.isHome)
        	hubIcon.setImageResource(R.drawable.ic_action_home);
        else
        	hubIcon.setImageResource(R.drawable.ic_action_user);
        return convertView;
    }
 }
