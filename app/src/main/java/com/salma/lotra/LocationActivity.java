package com.salma.lotra;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, android.location.LocationListener {

    private static final int REQUEST_LOCATION = 121;
    private FirebaseAuth mFirebaseAuth;
    private ImageView mLocationImgView;
    private GoogleApiClient mGoogleApiClient;
    private double mLatitude;
    private double mLongitude;
    private DatabaseReference mDatabase;
    private AlertDialog dialog;
    private String mBusNumber;
    private LocationManager locationManager;
    private String provider;
    private DriverModel mDriverModel;
    private TextView mCounterTxtView;
    private Button mIncrementalBtn;
    private Button mDecrementalBtn;
    private int mNumberOfPassengers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mDriverModel = new DriverModel();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();

        mNumberOfPassengers = retrieveNumberOfPassenger();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
        mLocationImgView = (ImageView) findViewById(R.id.location_img_view);
        mCounterTxtView = (TextView) findViewById(R.id.number_of_passenger_txt_view);
        mIncrementalBtn = (Button) findViewById(R.id.increment_btn);
        mDecrementalBtn = (Button) findViewById(R.id.decrement_btn);
        mCounterTxtView.setText(""+mNumberOfPassengers);
        mIncrementalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumberOfPassengers++;
                if (mNumberOfPassengers > 30) {
                    mNumberOfPassengers = 30;
                }

                mCounterTxtView.setText("" + mNumberOfPassengers);
                updateInfo();
                saveNumberOfPassenger(mNumberOfPassengers);
            }
        });
        mDecrementalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNumberOfPassengers--;
                if (mNumberOfPassengers < 1) {
                    mNumberOfPassengers = 1;
                }

                mCounterTxtView.setText("" + mNumberOfPassengers);
                updateInfo();
                saveNumberOfPassenger(mNumberOfPassengers);

            }
        });


        mLocationImgView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                updateInfo();
                Toast.makeText(LocationActivity.this, "Your Location has been updated !", Toast.LENGTH_SHORT).show();

            }
        });


        mFirebaseAuth = FirebaseAuth.getInstance();


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
                        Toast.makeText(LocationActivity.this, "Please, enter a valid Bus Number !", Toast.LENGTH_SHORT).show();
                    } else {
                        setBusNumber(mBusNumber);
                        Toast.makeText(LocationActivity.this, "Bus Number saved !", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });
        }


    }

    private void updateInfo() {

        mDriverModel.Latitude = mLatitude;
        mDriverModel.Longitude = mLongitude;
        mDriverModel.BusNumber = getBusNumber();
        mDriverModel.NumberOfPassenger = mNumberOfPassengers;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("DriverInfo").child(mDriverModel.DriverName).setValue(mDriverModel);
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

    public void saveNumberOfPassenger(int busNumber) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("NumberOfPassenger", busNumber);
        editor.apply();
    }

    public int retrieveNumberOfPassenger() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int number = preferences.getInt("NumberOfPassenger", 0);
        return number;
    }


    public void deleteSharedPref() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();

    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        mDriverModel.DriverName = mFirebaseAuth.getCurrentUser().getDisplayName();
        mDriverModel.Email = mFirebaseAuth.getCurrentUser().getEmail();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(provider, 401, 1, this);
            }
        }
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

                cleanUp();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void cleanUp() {
        mGoogleApiClient.disconnect();
        ProgressDialog pd = new ProgressDialog(LocationActivity.this);
        pd.setMessage("loading");
        pd.show();
        AuthUI.getInstance()
                .signOut(this)

                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                            finish();
                        }
                    }
                });
        deleteSharedPref();
    }

    @Override
    public void onConnected(Bundle bundle) {

        checkLocationPermission();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission is required !")
                        .setMessage("The app will shut down if the permission denied! ")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(LocationActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        locationManager.getLastKnownLocation(provider);
                    }


                } else {

                }
                return;
            }

        }
    }


    @Override
    public void onLocationChanged(Location location) {
        //    Toast.makeText(this, "onLocationChanged" + location.getLatitude(), Toast.LENGTH_SHORT).show();

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        updateInfo();
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