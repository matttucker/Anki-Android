/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;import com.ichi2.anki2.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.async.Connection;
import com.ichi2.async.Connection.Payload;
import com.ichi2.libanki.sync.HttpSyncer;
import com.ichi2.themes.StyledDialog;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.tomgibara.android.veecheck.util.PrefSettings;

public class MyAccount extends AnkiActivity {

    private View mLoginToMyAccountView;
    private View mLoggedIntoMyAccountView;

    private EditText mUsername;
    private EditText mPassword;

    private TextView mUsernameLoggedIn;

    private StyledProgressDialog mProgressDialog;
    private StyledDialog mNoConnectionAlert;
    private StyledDialog mConnectionErrorAlert;
    private StyledDialog mInvalidUserPassAlert;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Themes.applyTheme(this);
        super.onCreate(savedInstanceState);

        initAllContentViews();
        initAllAlertDialogs();

        SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
        if (preferences.getString("hkey", "").length() > 0) {
            String username = preferences.getString("username", "");
            mUsernameLoggedIn.setText(username);
            setContentView(mLoggedIntoMyAccountView);
        } else {
            setContentView(mLoginToMyAccountView);
        }

    }


    // Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
//    private boolean isUsernameAndPasswordValid(String username, String password) {
//        return isLoginFieldValid(username) && isLoginFieldValid(password);
//    }
//
//
//    private boolean isLoginFieldValid(String loginField) {
//        boolean loginFieldValid = false;
//
//        if (loginField.length() >= 2 && loginField.matches("[A-Za-z0-9]+")) {
//            loginFieldValid = true;
//        }
//
//        return loginFieldValid;
//    }


    private void saveUserInformation(String username, String hkey) {
        SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        editor.putString("username", username);
        editor.putString("hkey", hkey);
        editor.commit();
    }


    private void login() {
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mUsername.getWindowToken(), 0);

        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        /*
         * Commented awaiting the resolution of the next issue: http://code.google.com/p/anki/issues/detail?id=1932
         * if(isUsernameAndPasswordValid(username, password)) { Connection.login(loginListener, new
         * Connection.Payload(new Object[] {username, password})); } else { mInvalidUserPassAlert.show(); }
         */

        if (!"".equalsIgnoreCase(username) && !"".equalsIgnoreCase(password)) {
            Connection.login(loginListener, new Connection.Payload(new Object[] { username, password }));
        } else {
            mInvalidUserPassAlert.show();
        }
    }


    private void logout() {
        SharedPreferences preferences = PrefSettings.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        editor.putString("username", "");
        editor.putString("hkey", "");
        editor.commit();

        setContentView(mLoginToMyAccountView);
    }


    private void initAllContentViews() {
        mLoginToMyAccountView = getLayoutInflater().inflate(R.layout.my_account, null);
        Themes.setWallpaper(mLoginToMyAccountView);
        Themes.setTextViewStyle(mLoginToMyAccountView.findViewById(R.id.MyAccountLayout));
        Themes.setTextViewStyle(mLoginToMyAccountView.findViewById(R.id.no_account_text));
        mUsername = (EditText) mLoginToMyAccountView.findViewById(R.id.username);
        mPassword = (EditText) mLoginToMyAccountView.findViewById(R.id.password);

        Button loginButton = (Button) mLoginToMyAccountView.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }

        });

        Button signUpButton = (Button) mLoginToMyAccountView.findViewById(R.id.sign_up_button);
        signUpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
            	Intent intent = new Intent(MyAccount.this, Info.class);
            	intent.putExtra(Info.TYPE_EXTRA, Info.TYPE_CREATE_ACCOUNT);
                startActivity(intent);
                if (UIUtils.getApiLevel() > 4) {
                    ActivityTransitionAnimation.slide(MyAccount.this, ActivityTransitionAnimation.RIGHT);
                }
            }

        });

        mLoggedIntoMyAccountView = getLayoutInflater().inflate(R.layout.my_account_logged_in, null);
        Themes.setWallpaper(mLoggedIntoMyAccountView);
        Themes.setTitleStyle(mLoggedIntoMyAccountView.findViewById(R.id.logged_text));
        mUsernameLoggedIn = (TextView) mLoggedIntoMyAccountView.findViewById(R.id.username_logged_in);
        Button logoutButton = (Button) mLoggedIntoMyAccountView.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                logout();
            }

        });
    }


    /**
     * Create AlertDialogs used on all the activity
     */
    private void initAllAlertDialogs() {
        Resources res = getResources();

        StyledDialog.Builder builder = new StyledDialog.Builder(this);

        builder.setTitle(res.getString(R.string.connection_error_title));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(res.getString(R.string.connection_needed));
        builder.setPositiveButton(res.getString(R.string.ok), null);
        mNoConnectionAlert = builder.create();

        builder = new StyledDialog.Builder(this);
        builder.setTitle(res.getString(R.string.log_in));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(res.getString(R.string.invalid_username_password));
        builder.setPositiveButton(res.getString(R.string.ok), null);
        mInvalidUserPassAlert = builder.create();

        builder = new StyledDialog.Builder(this);
        builder.setTitle(res.getString(R.string.connection_error_title));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(res.getString(R.string.connection_error_message));
        builder.setPositiveButton(res.getString(R.string.retry), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                login();
            }
        });
        builder.setNegativeButton(res.getString(R.string.cancel), null);
        mConnectionErrorAlert = builder.create();
    }

    /**
     * Listeners
     */
    Connection.TaskListener loginListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Log.i(AnkiDroidApp.TAG, "MyAccount - onPreExcecute");
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = StyledProgressDialog.show(MyAccount.this, "",
                        getResources().getString(R.string.alert_logging_message), true);
            }
        }


        @Override
        public void onPostExecute(Payload data) {
            Log.i(AnkiDroidApp.TAG, "MyAccount - onPostExecute, succes = " + data.success);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (data.success) {
                Log.i(AnkiDroidApp.TAG, "User successfully logged in!");
                saveUserInformation((String) data.data[0], (String) data.data[1]);

                Intent i = MyAccount.this.getIntent();
                if (i.hasExtra("notLoggedIn") && i.getExtras().getBoolean("notLoggedIn", false)) {
                	MyAccount.this.setResult(RESULT_OK, i);
                	finishWithAnimation(ActivityTransitionAnimation.FADE);
                } else {
                    // Show logged view
                    mUsernameLoggedIn.setText((String) data.data[0]);
                    setContentView(mLoggedIntoMyAccountView);
                }
            } else {
                if (data.returnType == 403) {
                    if (mInvalidUserPassAlert != null) {
                        mInvalidUserPassAlert.show();
                    }
                } else {
                    if (mConnectionErrorAlert != null) {
                        mConnectionErrorAlert.show();
                    }
                }
            }
        }


        @Override
        public void onDisconnected() {
            if (mNoConnectionAlert != null) {
                mNoConnectionAlert.show();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	Log.i(AnkiDroidApp.TAG, "MyAccount - onBackPressed()");
        	finish();
            if (UIUtils.getApiLevel() > 4) {
                ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.FADE);
            }
        	return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}
