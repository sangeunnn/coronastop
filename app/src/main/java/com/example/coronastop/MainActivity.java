package com.example.coronastop;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    SupportMapFragment mapFragment;
    GoogleMap map;
    final FirebaseFirestore db = FirebaseFirestore.getInstance(); //파이어스토어
    MarkerOptions myLocationMarker;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Log.d("Map", "지도 준비됨");
                map = googleMap;
            }
        });

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocationService();
            }
        });
        Button corona = findViewById(R.id.corona);
        corona.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                corona();
            }

        });

    }

    public void onResume() {
        super.onResume();

        if (map != null) {
            map.setMyLocationEnabled(true);
        }
    }

    public void onPause() {
        super.onPause();

        if (map != null) {
            map.setMyLocationEnabled(false);
        }
    }
    //get current location
    private void startLocationService() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try{
            Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
                Date time = new Date();
                String time1 = format1.format(time);
                uploadDB(latitude,longitude,time1);
            }

            GPSListener gpsListener = new GPSListener();
            long minTime = 10000;   //최소시간 10초
            float minDistance = 0;  //최소거리 0

            //10초마다 위치정보를 전달받습니다.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,minTime,minDistance,gpsListener);
            Toast.makeText(getApplicationContext(), "내 위치확인 요청함",Toast.LENGTH_SHORT).show();

        }catch(SecurityException e) {
            e.printStackTrace();
        }

    }
    public void uploadDB(double latitude, double longitude, String Time){
        Intent homeIntent = getIntent();
        final String id = homeIntent.getStringExtra("id");
        CollectionReference doc = db.collection("User").document(id).collection("Path");
        Map<String, Object> location = new HashMap<>();
        location.put("latitude", latitude);
        location.put("longitude", longitude);
        location.put("Time", Time);
        doc.document(Time).set(location).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "위치업로드 저장하였습니다.", Toast.LENGTH_SHORT).show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "위치를 저장하지 못했습니다..", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void corona(){
        Intent homeIntent = getIntent();
        final String id = homeIntent.getStringExtra("id");
        CollectionReference doc = db.collection("User").document(id).collection("Path");
        SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        Date time = new Date();
        String time1 = format1.format(time);
        db.collection("Path").document(id).set(time1);
    }

    //location listener
    class GPSListener implements LocationListener {
        public void onLocationChanged(Location location) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();
            String message = "내 위치-> Latitude : "+ latitude + "\nLongitude:"+ longitude;

            showCurrentLocation(latitude,longitude);
        }
        private void showCurrentLocation(Double latitude, Double longitude) {
            LatLng curPoint = new LatLng(latitude, longitude);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(curPoint, 15));
            showMyLocationMarker(curPoint);
        }
        //location marker
        private void showMyLocationMarker(LatLng curPoint) {
            if (myLocationMarker == null) {
                myLocationMarker = new MarkerOptions();
                myLocationMarker.position(curPoint);
                myLocationMarker.title("* 내위치\n");
                myLocationMarker.snippet("* GPS로 확인한 위치");
                myLocationMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocation));
                map.addMarker(myLocationMarker);
            } else {
                myLocationMarker.position(curPoint);
            }
        }

        public void onProviderDisabled(String provider) {};

        public void onProviderEnabled(String provider) {};

        public void onStatusChanged(String provider, int status, Bundle extras) {}

    }
}



