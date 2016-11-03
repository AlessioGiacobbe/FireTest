package giacobbe.alessio.firetest;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.LocationManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.entire.sammalik.samlocationandgeocoding.SamLocationRequestService;
import com.github.buchandersenn.android_permission_manager.PermissionManager;
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionDeniedCallback;
import com.github.buchandersenn.android_permission_manager.callbacks.OnPermissionGrantedCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;
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
import com.piotrek.customspinner.CustomSpinner;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static giacobbe.alessio.firetest.R.id.map;


public class Home extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    SamLocationRequestService samLocationRequestService;
    private DatabaseReference mDatabase;
    public FirebaseUser user;
    public DatabaseReference locationref;
    public String nodetitle;
    private ArrayList<Marker> mMarkerArray = new ArrayList<Marker>();
    public EditText latedit, longedit;
    private final PermissionManager permissionManager = PermissionManager.create(this);

    LocationManager mLocationManager;
    public boolean isFirstLaunch = true;
    private String[] arraySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        user = FirebaseAuth.getInstance().getCurrentUser();

        latedit = (EditText) findViewById(R.id.input_lat);
        longedit = (EditText) findViewById(R.id.input_long);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        populatespinner();
        populate();

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
                final Location nuova = dataSnapshot.getValue(Location.class);
                boolean isenabled = true;
                for (Marker marcatore: mMarkerArray){
                    if(marcatore.getTitle().equals(nuova.Title)){
                        isenabled = false;
                    }
                }
                if (nuova.Latitude != null && isenabled) {
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
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nuova.Latitude, nuova.Longitude), 10));
                                    }
                                });

                        snackbar.show();
                    }
                    addmark(nuova);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                final Location nuova = dataSnapshot.getValue(Location.class);
                mMap.clear();
                isFirstLaunch = true;
                populate();
                Snackbar snackbar = Snackbar
                        .make(findViewById(android.R.id.content), nuova.Title + " rimosso", Snackbar.LENGTH_LONG)
                        ;
                snackbar.show();
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
                samLocationRequestService.stopLocationUpdates();
            }


        });
    }

    public void populatespinner(){
        final List<String> types = new ArrayList<String>();
        locationref = FirebaseDatabase.getInstance().getReference("types");
        locationref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data: dataSnapshot.getChildren()) {
                    types.add(data.getValue().toString());
                }
                CustomSpinner custom = (CustomSpinner) findViewById(R.id.typespinner);
                String[] tipi = new String[types.size()];
                tipi =  types.toArray(tipi);
                custom.initializeStringValues(tipi, "seleziona il tipo");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    public void addmark(Location mark){
        LatLng location = new LatLng(mark.Latitude, mark.Longitude);

        Marker marcatore =  mMap.addMarker(new MarkerOptions()
                                        .position(location)
                                        .snippet("premi per maggiori informazioni")
                                        .title(mark.Title));

        switch (mark.type){
            case "wifi":
                marcatore.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wifi_white_24dp));
                break;
            case "parcheggi":
                marcatore.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_local_parking_white_24dp));
                break;
            case "wc pubblici":
                marcatore.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wc_white_24dp));
                break;
            case "fontanelle":
                marcatore.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_opacity_white_24dp));
                break;
            default:
                break;

        }

        mMarkerArray.add(marcatore);
    }


    public int resolveicon(String name){
        int current = R.drawable.cast_ic_notification_pause;
        switch (name){

        }

        return current;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.handlePermissionResult(requestCode, grantResults);
    }

    @Override
    public void onResume(){
        super.onResume();

    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d("riprendo", "");
        EditText Title = (EditText) findViewById(R.id.input_Title);
        EditText latin = (EditText) findViewById(R.id.input_lat);
        EditText longin = (EditText) findViewById(R.id.input_long);
        try {
            Title.setText(savedInstanceState.getString("title"));
            latin.setText(String.valueOf(savedInstanceState.getDouble("lat")));
            longin.setText(String.valueOf(savedInstanceState.getDouble("long")));
        }catch(Exception e){

        }
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        EditText Title = (EditText) findViewById(R.id.input_Title);
        EditText latin = (EditText) findViewById(R.id.input_lat);
        EditText longin = (EditText) findViewById(R.id.input_long);
        try {

            savedInstanceState.putString("title", Title.getText().toString());
            savedInstanceState.putDouble("lat", Double.valueOf(latin.getText().toString()));
            savedInstanceState.putDouble("long", Double.valueOf(longin.getText().toString()));

        }catch (Exception e){

        }
        // etc.
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

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                final Dialog dialogo =  new Dialog(Home.this);
                dialogo.setContentView(R.layout.infodialog);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialogo.getWindow().getAttributes());
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.MATCH_PARENT;

                final ImageView delete = (ImageView) dialogo.findViewById(R.id.delete);
                locationref = FirebaseDatabase.getInstance().getReference("admin");
                locationref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot data: dataSnapshot.getChildren()){
                            if(data.getValue().toString().equals(user.getEmail())){
                               delete.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                final TextView Titolo = (TextView) dialogo.findViewById(R.id.infotitle);
                Titolo.setText(marker.getTitle());
                TextView coordinates = (TextView) dialogo.findViewById(R.id.subtitlecoord);
                coordinates.setText("lat: " + marker.getPosition().latitude + " - long: " + marker.getPosition().latitude);
                final TextView mail = (TextView) dialogo.findViewById(R.id.email);
                final TextView haiaggiunto = (TextView) dialogo.findViewById(R.id.haaggiunto);
                final ListView lista = (ListView) dialogo.findViewById(R.id.lista);
                locationref = FirebaseDatabase.getInstance().getReference("locations");
                ArrayList<Location> arrayOfUsers = new ArrayList<Location>();
                final locationadapter adapter = new locationadapter(Home.this, arrayOfUsers);
                lista.setAdapter(adapter);
                lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(adapter.getItem(position).Latitude , adapter.getItem(position).Longitude), 10));
                        dialogo.dismiss();
                    }
                });

                locationref.orderByChild("Title").equalTo(marker.getTitle()).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        nodetitle = dataSnapshot.getKey();
                        Location trovata = dataSnapshot.getValue(Location.class);
                        if (trovata.UserName.equals(user.getEmail())){
                            mail.setText("Te!");
                            haiaggiunto.setText("Hai aggiunto anche");
                        }else {
                            mail.setText(trovata.UserName);
                            haiaggiunto.setText("Ha aggiunto anche");

                        }
                        locationref.orderByChild("UserName").equalTo(trovata.UserName).addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                Location loc = dataSnapshot.getValue(Location.class);
                                if (!loc.Title.equals(Titolo.getText())) {
                                    adapter.add(loc);
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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


                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        locationref = FirebaseDatabase.getInstance().getReference("locations");
                        locationref.child(nodetitle).removeValue();
                        mMap.clear();
                        isFirstLaunch = true;
                        populate();
                        dialogo.dismiss();
                    }
                });

                dialogo.show();
                dialogo.getWindow().setAttributes(lp);
            }
        });
    }

    public void populate(){
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
    }

    public void additem(){

        EditText Title = (EditText) findViewById(R.id.input_Title);


        CustomSpinner spinner = (CustomSpinner) findViewById(R.id.typespinner);

        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date();
        if(!(Title.getText().toString().matches(""))&& !(latedit.getText().toString().matches("")) && !(longedit.getText().toString().matches(""))) {

            try {
                if(new Double(latedit.getText().toString())>-90 && new Double(latedit.getText().toString()) < 90 && new Double(longedit.getText().toString())>-180 && new Double(longedit.getText().toString())<180) {
                    String titolo = Title.getText().toString();
                    if(String.valueOf(titolo.charAt(0)).equals(" ")){
                        titolo = titolo.substring(1);
                    }
                    if (spinner.getSelectedItem().toString().equals("seleziona il tipo")){
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Hey!")

                                .setMessage("devi scegliere il tipo di posto che vuoi aggiungere.")

                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        // Create the AlertDialog object and return it
                        builder.create();
                        builder.show();
                    }else {
                        Location loc = new Location(titolo, new Double(latedit.getText().toString()), new Double(longedit.getText().toString()), user.getEmail(), spinner.getSelectedItem().toString());

                        mDatabase.child("locations").child(dateFormat.format(date)).setValue(loc);
                    }
                }else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Capra!")

                            .setMessage("latitudine e longitudine vanno rispettivamente da -90 a 90 gradi e da -180 a 180")

                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                }
                            });
                    // Create the AlertDialog object and return it
                    builder.create();
                    builder.show();
                }
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
