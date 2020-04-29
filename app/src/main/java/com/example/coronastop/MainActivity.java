package com.example.coronastop;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.protobuf.compiler.PluginProtos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    boolean Emergency = false;
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

            }
        });

        startLocationService();
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
                EmergencyLocationService();
                setPin(map);
            }
        });

        Button corona = findViewById(R.id.corona);
        corona.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, uploadActivity.class);
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
        timer.schedule(TT, 60000, 3600000);
    }

    public void showNoti() {
        NotificationManager manager;

        String CHANNEL_ID = "channel1";
        String CHANNEL_NAME = "channel1";

        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(new NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
                ));
                builder = new NotificationCompat.Builder(this, CHANNEL_ID);
                ;
            }
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        }
        builder.setContentTitle("위험합니다. 코로나가 근처에 있습니다.");
        builder.setContentText("코로나 확진자가 500m 근처에 출몰하였습니.");
        builder.setSmallIcon(android.R.drawable.ic_menu_view);
        Notification noti = builder.build();


        manager.notify(1, noti);
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

        try {
            Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
            }

            GPSListener gpsListener = new GPSListener();
            long minTime = 10000;   //최소시간 10초
            float minDistance = 0;  //최소거리 0

            //10초마다 위치정보를 전달받습니다.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
            Toast.makeText(getApplicationContext(), "내 위치확인 요청함", Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    private void EmergencyLocationService() {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time = new Date();
        long target = time.getTime() - 1209600000;
        Date TargetDate = new Date(target);
        String TargetTime = format1.format(TargetDate);
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        System.out.println("시작했어유");

        try {
            final Location location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            final Location destLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
            }
            db.collection("Path").whereGreaterThanOrEqualTo("timestamp", TargetTime).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            double deslatitude = (double) doc.get("latitude");
                            double deslongitude = (double) doc.get("longitude");
                            destLocation.setLatitude(deslatitude);
                            destLocation.setLongitude(deslatitude);
                            int distance = (int)(location.distanceTo(destLocation));
                            if (distance < 500) {
                                showNoti();
                            }
                        }
                    }
                }
            });
            GPSListener gpsListener = new GPSListener();
            long minTime = 10000;   //최소시간 10초
            float minDistance = 0;  //최소거리 0

            //10초마다 위치정보를 전달받습니다.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
            Toast.makeText(getApplicationContext(), "내 위치확인 요청함", Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void uploadDB() {
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
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    public void setPin(final GoogleMap googleMap){
        final SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final Date time = new Date();
        final long target1 = time.getTime() - 1209600000; // 2 week
        final long target2 = time.getTime() - 259200000; // 3 days
        final long target3 = time.getTime() - 604800016; // 1 week


        db.collection("Path").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for(QueryDocumentSnapshot document : task.getResult()) {
                        double latitude = (double) document.get("latitude");
                        double longitude = (double) document.get("longitude");
                        MarkerOptions makerOptions = new MarkerOptions();
                        try {
                            Date target =  format1.parse((String)document.get("timestamp"));
                            if(target.getTime() > target2){
                                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                                        .position(new LatLng(latitude,longitude))
                                        .title((String)document.get("timestamp")); // 타이틀.
                                makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                googleMap.addMarker(makerOptions);
                            }else if(target.getTime() > target3 && target.getTime() < target2){
                                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                                        .position(new LatLng(latitude,longitude))
                                        .title((String)document.get("timestamp")); // 타이틀.
                                makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                googleMap.addMarker(makerOptions);
                            }else{
                                makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                                        .position(new LatLng(latitude,longitude))
                                        .title((String)document.get("timestamp")); // 타이틀.
                                makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                googleMap.addMarker(makerOptions);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    Toast.makeText(MainActivity.this, "위치를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }

            }
        });
//        db.collection("Path").whereGreaterThan("timestamp",TargetTime3)
//                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if(task.isSuccessful()) {
//                    for(QueryDocumentSnapshot document : task.getResult()) {
//                        double latitude = (double) document.get("latitude");
//                        double longitude = (double) document.get("longitude");
//                        MarkerOptions makerOptions = new MarkerOptions();
//                        String time = (String) document.get("timestamp");
//                        makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
//                                .position(new LatLng(latitude,longitude))
//                                .title(time) // 타이틀.
//                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
//                        googleMap.addMarker(makerOptions);
//                    }
//                }else{
//                    Toast.makeText(MainActivity.this, "위치를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
//                }
//
//            }
//        });
//        db.collection("Path").whereGreaterThan("timestamp",TargetTime1)
//                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if(task.isSuccessful()) {
//                    for(QueryDocumentSnapshot document : task.getResult()) {
//                        double latitude = (double) document.get("latitude");
//                        double longitude = (double) document.get("longitude");
//                        MarkerOptions makerOptions = new MarkerOptions();
//                        String time = (String) document.get("timestamp");
//                        makerOptions // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
//                                .position(new LatLng(latitude,longitude))
//                                .title(time); // 타이틀.
//                        makerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//                        googleMap.addMarker(makerOptions);
//                    }
//                }else{
//                    Toast.makeText(MainActivity.this, "위치를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
//                }
//
//            }
//        });

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
                        String timestamp = (String) document.get("timestamp") + id;
                        temp.put("latitude",document.get("latitude"));
                        temp.put("longitude",document.get("longitude"));
                        temp.put("timestamp",document.get("timestamp"));
                        temp.put("id",id);
                        db.collection("Path").document(timestamp).set(temp);
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



