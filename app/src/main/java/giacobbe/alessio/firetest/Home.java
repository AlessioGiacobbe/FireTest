package giacobbe.alessio.firetest;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.entire.sammalik.samlocationandgeocoding.SamLocationRequestService;
import com.github.buchandersenn.android_permission_manager.PermissionManager;
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionDeniedCallback;
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionGrantedCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.messaging.RemoteMessage;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Home extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    SamLocationRequestService samLocationRequestService;
    private DatabaseReference mDatabase;
    public FirebaseUser user;
    private ArrayList<Marker> mMarkerArray = new ArrayList<Marker>();
    public EditText latedit, longedit;
    private final PermissionManager permissionManager = PermissionManager.create(this);

    LocationManager mLocationManager;
    public boolean isFirstLaunch = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        user = FirebaseAuth.getInstance().getCurrentUser();

        latedit = (EditText) findViewById(R.id.input_lat);
        longedit = (EditText) findViewById(R.id.input_long);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        final DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("locations");

        locationref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (isFirstLaunch) {
                    Log.e("count ", String.valueOf(dataSnapshot.getChildrenCount()));
                    int i = 0;
                    for (DataSnapshot locations : dataSnapshot.getChildren()) {
                        Location posizioni = locations.getValue(Location.class);
                        Log.d("posizione " + i + " ", posizioni.Title);

                        i++;
                        addmark(posizioni);
                    }
                    isFirstLaunch = false;
                    addTrigger();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        PackageManager pm = this.getPackageManager();
        int hasPerm = pm.checkPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this.getPackageName());
        if (hasPerm != PackageManager.PERMISSION_GRANTED) {
            permissionManager.with(Manifest.permission.ACCESS_FINE_LOCATION)
                    .onPermissionGranted(new OnPermissionGrantedCallback() {
                        @Override
                        public void onPermissionGranted() {
                            Log.d("YEEEEEE", ":)");

                            locationrequest();
                        }

                    })
                    .request();

        }else {

            locationrequest();

        }




        Button Submitbtn = (Button) findViewById(R.id.submit);

        Submitbtn.setOnClickListener(this);
    }


    public void addTrigger(){

        final DatabaseReference locationref = FirebaseDatabase.getInstance().getReference("locations");

        locationref.orderByChild("Latitude").addChildEventListener(new ChildEventListener() {


            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d("aggiunto", "dasd");
                final Location nuova = dataSnapshot.getValue(Location.class);
                boolean isenabled = true;
                for (Marker marcatore: mMarkerArray){
                    Log.d("titolo ", marcatore.getTitle());
                    if(marcatore.getTitle().equals(nuova.Title)){
                        isenabled = false;
                    }
                }
                Log.d("abilitato", String.valueOf(isenabled));
                if (nuova.Latitude != null && isenabled) {
                    Log.d("AAAAh", nuova.Latitude.toString());
                    if (nuova.UserName == user.getEmail()) {
                        Snackbar snackbar = Snackbar
                                .make(findViewById(android.R.id.content), nuova.Title + " Aggiunto", Snackbar.LENGTH_LONG);

                        snackbar.show();
                    } else {
                        Snackbar snackbar = Snackbar
                                .make(findViewById(android.R.id.content), "Qualcuno ha aggiunto " + nuova.Title, Snackbar.LENGTH_LONG)
                                .setAction("MOSTRA", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nuova.Latitude, nuova.Longitude), 14));
                                    }
                                });

                        snackbar.show();
                    }
                    addmark(nuova);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d("cambiato", "dsad");

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    public void locationrequest(){
        samLocationRequestService = new SamLocationRequestService(Home.this);
        samLocationRequestService.executeService(new SamLocationRequestService.SamLocationListener() {
            @Override
            public void onLocationUpdate(android.location.Location location, Address address) {
                latedit.setText(String.valueOf(location.getLatitude()));
                longedit.setText(String.valueOf(location.getLongitude()));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
                Log.d("muovo la cmaera", "");
                samLocationRequestService.stopLocationUpdates();
            }


        });
    }

    public void addmark(Location mark){
        LatLng location = new LatLng(mark.Latitude, mark.Longitude);
        Marker marcatore =  mMap.addMarker(new MarkerOptions()
                                        .position(location)
                                        .snippet("caricato da " + mark.UserName)
                                        .title(mark.Title));

        mMarkerArray.add(marcatore);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.handlePermissionResult(requestCode, grantResults);
    }

    @Override
    public void onResume(){
        super.onResume();
        // put your code here...

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void additem(){

        EditText Title = (EditText) findViewById(R.id.input_Title);


        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        if(!(Title.getText().toString().matches(""))&& !(latedit.getText().toString().matches("")) && !(longedit.getText().toString().matches(""))) {

            try {
                Location loc = new Location(Title.getText().toString(), new Double(latedit.getText().toString()), new Double(longedit.getText().toString()), user.getEmail());
                mDatabase.child("locations").child(dateFormat.format(date)).setValue(loc);

                Title.setText(" ");
            }catch(Exception e){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Qualcosa non va!")

                        .setMessage("Probabilmente hai sbagliato ad inserire il testo, anche se non è poi così difficle...")

                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create();
                builder.show();
            }
        }else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Hey!")
                    .setMessage("Hai inserito correttamente il testo?")

                    .setPositiveButton("Ehm... no", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create();
            builder.show();
        }
    }

    @Override
    public void onBackPressed()
    {
        // code here to show dialog
    }





    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.submit:
                additem();
                break;
        }
    }
}