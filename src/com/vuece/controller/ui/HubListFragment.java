package com.vuece.controller.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.vuece.controller.R;
import com.vuece.controller.model.HubEntry;
import com.vuece.vtalk.android.jni.JabberClient;
import com.vuece.vtalk.android.util.Log;

public class HubListFragment extends Fragment {
	private static String TAG = "vuece/HubListFragment";
    public interface OnHubSelectedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onHubSelected(String name);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {

        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
        if (savedInstanceState != null) {
//            mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
        }
        

        // Inflate the layout for this fragment
        View thisView=inflater.inflate(R.layout.hub_list, container, false);
        ListView listview=(ListView)thisView.findViewById(R.id.hubList);
        View empty=thisView.findViewById(R.id.empty);
        listview.setEmptyView(empty);
        HubEntryAdapter itemsAdapter = 
        	    new HubEntryAdapter(this.getActivity(), JabberClient.getInstance().getHubManager().hubs);
        JabberClient.getInstance().getHubManager().adaptor=itemsAdapter;
        listview.setAdapter(itemsAdapter);
        listview.setSelector(R.drawable.vuecetheme_list_selector_holo_light);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

          @Override
          public void onItemClick(AdapterView<?> parent, final View view,
              int position, long id) {
            final HubEntry item = (HubEntry) parent.getItemAtPosition(position);
            OnHubSelectedListener listener = (OnHubSelectedListener) getActivity();
            Log.d(TAG, "selected hub: "+item.getJid());
            listener.onHubSelected(item.getJid());
          }

        });
        return thisView;//inflater.inflate(R.layout.waiting, container, false);
    }

}
