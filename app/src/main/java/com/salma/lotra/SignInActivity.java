package com.salma.lotra;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ResultCodes;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class SignInActivity extends AppCompatActivity {
    private static final int FLAG_FOR_SIGN_IN = 1;
    private FirebaseAuth mFireBaseAuth;
    private FirebaseAuth.AuthStateListener zAuthStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Check the internet connection and try again !", Toast.LENGTH_SHORT).show();
            // finish();
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, LocationActivity.class));
            finish();
            return;
        }

        mFireBaseAuth = FirebaseAuth.getInstance();
        zAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder().setIsSmartLockEnabled(false)
                                .setAvailableProviders(
                                        Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())).setLogo(R.drawable.ic_launcher)
                                .build(),
                        FLAG_FOR_SIGN_IN);
            }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLAG_FOR_SIGN_IN) {
            if (resultCode == ResultCodes.OK) {
                startActivity(new Intent(this, LocationActivity.class));
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFireBaseAuth.addAuthStateListener(zAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFireBaseAuth.removeAuthStateListener(zAuthStateListener);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
