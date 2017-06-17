package com.salma.lotra;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, LocationListener {
    private static final int FLAG_FOR_SIGN_IN = 1;
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 121;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener zAuthStateListener;
    private ImageView mLocationView;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private double mLatitude;
    private double mLongitude;
    private DatabaseReference mDatabase;
    private AlertDialog dialog;
    private String mBusNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        mLocationView = (ImageView) findViewById(R.id.location_img_view);


        mLocationView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                updateInfo();

            }
        });


        mFirebaseAuth = FirebaseAuth.getInstance();

        zAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    // Log.i("ZOKA","user out");

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder().setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build())).setLogo(R.drawable.ic_launcher)
                                    .build(),
                            FLAG_FOR_SIGN_IN);

                }
            }
        };

        if (getBusNumber().equalsIgnoreCase("")) {
            dialog = new AlertDialog.Builder(this)
                    .create();
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            dialog.setView(layoutInflater.inflate(R.layout.message_dialog, null));
            dialog.show();

            Button button = (Button) dialog.findViewById(R.id.ok);
            final EditText editText = (EditText) dialog.findViewById(R.id.bus_number_edit_txt);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBusNumber = editText.getText().toString();

                    if (mBusNumber.length() < 5) {
                        Toast.makeText(MainActivity.this, "Please, enter a valid Bus Number !", Toast.LENGTH_SHORT).show();
                    } else {
                        setBusNumber(mBusNumber);
                        Toast.makeText(MainActivity.this, "Bus Number saved !", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        }


    }

    private void updateInfo() {
        DriverModel model = new DriverModel();
        model.DriverName = mFirebaseAuth.getCurrentUser().getDisplayName();
        model.Email = mFirebaseAuth.getCurrentUser().getEmail();
        model.Latitude = mLatitude;
        model.Longitude = mLongitude;
        model.BusNumber = mBusNumber;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("DriverInfo").child(model.DriverName).setValue(model);
        Toast.makeText(MainActivity.this, "You Location has been updated !", Toast.LENGTH_SHORT).show();
    }

    public void setBusNumber(String busNumber) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("BusNumber", busNumber);
        editor.apply();
    }

    public String getBusNumber() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("BusNumber", "");
        return name;
    }

    public void deleteSharedPref() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();

    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLAG_FOR_SIGN_IN) {
            // Successfully signed in
            if (resultCode == ResultCodes.OK) {
                Toast.makeText(this, "Sucessfull Login !", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    private void onSignedOutCleanup() {
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(zAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(zAuthStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {


            case R.id.sign_out_menu:
                deleteSharedPref();
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // user is now signed out
                                //     startActivity(new Intent(MainActivity.this, SignInHubActivity.class));
                                finish();
                            }
                        });
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initLocation();
        }

        if (mLocation != null) {
            mLatitude = mLocation.getLatitude();
            mLongitude = mLocation.getLongitude();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {

            if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                Toast.makeText(this, "Location permission is required !", Toast.LENGTH_SHORT).show();

            }

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_FINE_LOCATION);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        initLocation();
                    } else {
                        //permission denied
                        Toast.makeText(getApplicationContext(), "This app requires location permission to be granted", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onLocationChanged(Location location) {
        if (mLocation != null) {
            mLatitude = mLocation.getLatitude();
            mLongitude = mLocation.getLongitude();
            Toast.makeText(this, "" + mLongitude, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}