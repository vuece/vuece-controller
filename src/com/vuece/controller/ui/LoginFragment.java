package com.vuece.controller.ui;

import android.annotation.TargetApi;
import static android.R.layout.simple_dropdown_item_1line;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.otto.Subscribe;
import com.vuece.controller.R;
import com.vuece.controller.core.BusProvider;
import com.vuece.controller.event.ClientStateChangedEvent;
import com.vuece.controller.model.AuthType;
import com.vuece.controller.model.ClientEvent;
import com.vuece.controller.service.ControllerService;
import com.vuece.controller.service.ControllerService.LocalBinder;
import com.vuece.vtalk.android.jni.VTalkListener;
import com.vuece.vtalk.android.util.JabberUtils;
import com.vuece.vtalk.android.util.Log;

public class LoginFragment extends Fragment {

	static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1001;
	LoginListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface LoginListener {
        /** Called by LoginListener when a list item is selected */
        public void onLoginButtonClicked(String username, String password, AuthType type);
        public void onCancelButtonClicked();
    }
    
	private static String TAG = "LoginFragment";
	private static String ADD_NEW_ACCOUNT_STRING= "Add new account ...";
    private AccountManager accountManager;
    private View connectingLayout;
    private TextView waitingText;
    private Button loginButton;
    private Button cancelButton;
    private CheckBox autoLoginChkBox;
    private ProgressBar loginProgress;
    private Spinner accountList;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        accountManager = AccountManager.get(getActivity());
        

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.login, container, false);
    }
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Bind to LocalService
        Intent intent = new Intent(getActivity(), ControllerService.class);
        loginButton=(Button) getActivity().findViewById(R.id.buttonLogin);
        cancelButton=(Button) getActivity().findViewById(R.id.buttonCancel);
        autoLoginChkBox=(CheckBox) getActivity().findViewById(R.id.checkboxAutoLogin);
        waitingText=(TextView) getActivity().findViewById(R.id.waitingText);
        connectingLayout=(View) getActivity().findViewById(R.id.LinearLayoutConnecting);
        loginProgress=(ProgressBar) getActivity().findViewById(R.id.pb_loading);
        accountList=(Spinner) getActivity().findViewById(R.id.accountlist);
		//get config
		final Resources res = getResources();
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
	    boolean autoLogin = settings.getBoolean(res.getString(R.string.pref_auto_login_key), false);
	    Log.d(TAG, "autoLogin:"+autoLogin);
    	String username = settings.getString(res.getString(R.string.pref_username_key), null);
    	Log.d(TAG, "username:"+username);
    	List<String> emailAccounts=userEmailAccounts();
    	Log.d(TAG, "userEmailAccounts():"+emailAccounts);
    	ArrayAdapter<String> accountListAdaptor = new ArrayAdapter<String>(getActivity(), simple_dropdown_item_1line, emailAccounts);
    	accountList.setAdapter(accountListAdaptor);
    	int spinnerPosition = accountListAdaptor.getPosition(username);
    	//set the default according to value
    	if (spinnerPosition>-1)
    		accountList.setSelection(spinnerPosition);
    	accountList.setOnItemSelectedListener(new OnItemSelectedListener() {
    	    @Override
    	    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
    	    	String selected=accountList.getSelectedItem().toString();
    	    	if (ADD_NEW_ACCOUNT_STRING.equalsIgnoreCase(selected)) {
    	    		startAddGoogleAccountIntent(LoginFragment.this.getActivity());
    	    	}
    	    }

    	    @Override
    	    public void onNothingSelected(AdapterView<?> parentView) {
    	        // nothing to do
    	    }

    	});    	
    	if (autoLogin){
			autoLoginChkBox.setChecked(true);
		}
    	cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mCallback.onCancelButtonClicked();
            }
        });
    	loginButton.setEnabled(true);
    	loginButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
        		// check network status first
        		ConnectivityManager connManager=(ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo lNetworkInfo = connManager.getActiveNetworkInfo();
                Log.i(TAG, "Network info before connecting ["+lNetworkInfo+"]");
                if (lNetworkInfo==null||!lNetworkInfo.isConnected()) {
                	Toast.makeText(getActivity(), R.string.network_not_connected,
                            Toast.LENGTH_SHORT).show();
                	return;
                }
        		String username=accountList.getSelectedItem().toString();
        		
            	loginButton.setEnabled(false);
				waitingText.setText(R.string.text_connecting);
				connectingLayout.setVisibility(View.VISIBLE);
				
                //change login config and login
    			SharedPreferences.Editor editor = settings.edit();
    		    editor.putBoolean(res.getString(R.string.pref_auto_login_key), autoLoginChkBox.isChecked());
    		    editor.putString(res.getString(R.string.pref_username_key), username);
    		    // Commit the edits!
    		    editor.commit();
        		
                new GetTokenTask(getActivity(), LoginFragment.this, accountList.getSelectedItem().toString()).execute();
            }
        });
    }
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
        Log.d(TAG, "onResume");
        BusProvider.getInstance().register(this);
	}
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
        Log.d(TAG, "onPause");
        BusProvider.getInstance().unregister(this);
	}
    @Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (LoginListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LoginListener");
        }
    }
	
	public void loginWithToken(String token) {
        mCallback.onLoginButtonClicked(accountList.getSelectedItem().toString(), token, AuthType.OAUTH);
	}
	public void handleException(final Exception e) {
	    // Because this call comes from the AsyncTask, we must ensure that the following
	    // code instead executes on the UI thread.
	    getActivity().runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
	            if (e instanceof GooglePlayServicesAvailabilityException) {
	                // The Google Play services APK is old, disabled, or not present.
	                // Show a dialog created by Google Play services that allows
	                // the user to update the APK
	                int statusCode = ((GooglePlayServicesAvailabilityException)e)
	                        .getConnectionStatusCode();
	                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
	                		getActivity(),
	                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
	                dialog.show();
	            } else if (e instanceof UserRecoverableAuthException) {
	                // Unable to authenticate, such as when the user has not yet granted
	                // the app access to the account, but the user can fix this.
	                // Forward the user to an activity in Google Play services.
	                Intent intent = ((UserRecoverableAuthException)e).getIntent();
	                startActivityForResult(intent,
	                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
	            }
	        }
	    });
	}
	public void oauthFailed(final String message) {
	    getActivity().runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
			    Toast.makeText(LoginFragment.this.getActivity(), message, Toast.LENGTH_SHORT).show();
            	loginButton.setEnabled(true);
				connectingLayout.setVisibility(View.GONE);
	        }
	    });
	}
	
    // message subscription
	@Subscribe public void onClientStateChanged(ClientStateChangedEvent event) {
		if (event.event==ClientEvent.CLIENT_INITIATED.getValue()) {
				//auto login for testing
//		        mProgressDialog = ProgressDialog.show(RosterView.this, "Please wait...", "Client ready ...", true);
//		    	String username=VTalkMain.localUser;
//		    	String password=VTalkMain.localPassword; //passwordField.getText().toString();
//		    	VTalkService.getInstance().connect(username, password);
			final Resources res = getResources();
			final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		    boolean autoLogin = settings.getBoolean(res.getString(R.string.pref_auto_login_key), false);
		    Log.d(TAG, "autoLogin:"+autoLogin);
		}else if (event.event==ClientEvent.LOGGING_IN.getValue()) {
			waitingText.setText(R.string.text_connecting);
			connectingLayout.setVisibility(View.VISIBLE);
		}else if (event.event==ClientEvent.LOGIN_OK.getValue()) {
			connectingLayout.setVisibility(View.GONE);
		}else if (event.event==ClientEvent.LOGOUT_OK.getValue()) {
		    Toast.makeText(getActivity(), R.string.logged_out,
                    Toast.LENGTH_SHORT).show();
		    getActivity().finish();
		}else if (event.event==ClientEvent.AUTH_ERR.getValue()) {
			// show login dialog again
		    Toast.makeText(getActivity(), R.string.login_failed,
                    Toast.LENGTH_SHORT).show();
		    connectingLayout.setVisibility(View.GONE);
		    loginButton.setEnabled(true);
		}
		
	}
    private List<String> userEmailAccounts() {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        List<String> emailAddresses = new ArrayList<String>(accounts.length);
        for (Account account : accounts)
            emailAddresses.add(account.name);
        emailAddresses.add(ADD_NEW_ACCOUNT_STRING);
        return emailAddresses;
    }
    private static void startAddGoogleAccountIntent(Context context)
    {
        Intent addAccountIntent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (JabberUtils.isJellyBeanMR2OrLater())
        	addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
        context.startActivity(addAccountIntent); 
    }
}
