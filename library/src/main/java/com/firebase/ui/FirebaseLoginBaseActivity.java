package com.firebase.ui;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.ui.com.firebasei.ui.authimpl.GoogleAuthHelper;
import com.firebase.ui.com.firebasei.ui.authimpl.SocialProvider;
import com.firebase.ui.com.firebasei.ui.authimpl.TokenAuthHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public abstract class FirebaseLoginBaseActivity extends AppCompatActivity {

    private final String LOG_TAG = "FirebaseLoginBaseAct";

    public Firebase ref;

    private GoogleAuthHelper mGoogleAuthHelper;

    public SocialProvider chosenProvider;

    /* Abstract methods for Login Events */
    public abstract void onFirebaseLogin(AuthData authData);

    public abstract void onFirebaseLogout();

    public abstract void onFirebaseLoginError(FirebaseError firebaseError);

    public abstract void onFirebaseLoginCancel();

    public abstract Firebase setupFirebase();

    /* Login/Logout */

    public void loginWithProvider(SocialProvider provider) {
        switch (provider) {
            case Google:
                mGoogleAuthHelper.login();
                break;
            case Facebook:
                break;
            case Twitter:
                break;
        }

        chosenProvider = provider;
    }

    public void logout() {
        switch (chosenProvider) {
            case Google:
                mGoogleAuthHelper.logout();
                break;
            case Facebook:
                break;
            case Twitter:
                break;
        }
        ref.unauth();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);

        ref = setupFirebase();

        mGoogleAuthHelper = new GoogleAuthHelper(this, new TokenAuthHandler() {
            @Override
            public void onTokenReceived(String token) {
                authenticateRefWithProvider(mGoogleAuthHelper.PROVIDER_NAME, token);
            }

            @Override
            public void onCancelled() {
                onFirebaseLoginCancel();
            }

            @Override
            public void onError(Exception ex) {
                // TODO: Raise GMS Dialog Box?
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ref.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    onFirebaseLogin(authData);
                } else {
                    onFirebaseLogout();
                }
            }
        });
    }

    private void authenticateRefWithProvider(String provider, String token) {
        ref.authWithOAuthToken(provider, token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                 // Do nothing. Auth updates are handled in the AuthStateListener
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                onFirebaseLoginError(firebaseError);
            }
        });

    }

    private String getFirebaseUrlFromConfig() {
        String firebaseUrl;
        try {

            InputStream inputStream = getAssets().open("firebase-config.json");

            int size  = inputStream.available();

            byte[] buffer = new byte[size];

            inputStream.read(buffer);

            inputStream.close();

            String json = new String(buffer, "UTF-8");

            JSONObject obj = new JSONObject(json);

            firebaseUrl = obj.getString("firebaseUrl");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }

        return firebaseUrl;
    }

}