package com.example.coronastop;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.protobuf.compiler.PluginProtos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    String imagePath;
    private FirebaseFirestore fdb;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference recvRef;


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
                setPin(map);
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
        corona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent intent=new Intent(MainActivity.this,uploadActivity.class);
                startActivity(intent);
                corona();
                }
        });

                //타이머
        Timer timer = new Timer();
        TimerTask TT = new TimerTask() {
            @Override
            public void run() {
                uploadDB();
            }
        };
        timer.schedule(TT,0,3600000);
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
    public void uploadDB(){
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Intent homeIntent = getIntent();
            final String id = homeIntent.getStringExtra("id");
            CollectionReference doc = db.collection("User").document(id).collection("Path");
            Map<String, Object> Location = new HashMap<>();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date time = new Date();
            String time1 = format1.format(time);
            Location.put("latitude", location.getLatitude());
            Location.put("longitude", (double) (location.getLongitude()));
            Location.put("timestamp", time1);
            doc.document(time1).set(Location).addOnSuccessListener(new OnSuccessListener<Void>() {
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
        }catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    public void setPin(final GoogleMap googleMap){
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time = new Date();
        long target = time.getTime() - 1209600000;
        Date TargetDate = new Date(target);
        String TargetTime = format1.format(TargetDate);

        db.collection("Path").whereGreaterThanOrEqualTo("timestamp",TargetTime)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for(QueryDocumentSnapshot document : task.getResult()) {
                        double latitude = (double) document.get("latitude");
                        double longitude = (double) document.get("longitude");
                        MarkerOptions makerOptions = new MarkerOptions();
                        String time = (String) document.get("timestamp");
                        makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                                .position(new LatLng(latitude,longitude))
                                .title(time); // 타이틀.
                        googleMap.addMarker(makerOptions);
                    }
                }else{
                    Toast.makeText(MainActivity.this, "위치를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }


    public void corona(){
        Intent homeIntent = getIntent();
        final String id = homeIntent.getStringExtra("id");

        SimpleDateFormat format1 = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");
        Date time = new Date();
        String time1 = format1.format(time);

        db.collection("User").document(id).collection("Path").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Map<String, Object> temp = new HashMap<String, Object>();
                        temp.put("latitude",document.get("latitude"));
                        temp.put("longitude",document.get("longitude"));
                        temp.put("timestamp",document.get("timestamp"));
                        temp.put("id",id);
                        db.collection("Path").document((String) document.get("timestamp")).set(temp);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "저장을 하지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    //location listener
    class GPSListener implements LocationListener {
        public void onLocationChanged(Location location) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();
            showCurrentLocation(latitude,longitude);
        }
        public Map<String,Double> LocationRead(Location location){
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();
            Map<String,Double> result = new HashMap<String,Double>();
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            return result;
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



