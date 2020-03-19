package com.example.googlemapsreto;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean marker;

    //Posición del usuario
    private Marker positionUser;

    //Todos los marcadores
    private ArrayList<MarkerOptions> allMarkers = new ArrayList<MarkerOptions>();

    private double lat = 0.0;
    private double lng = 0.0;

    private final int REQUEST_ACCESS_FINE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_ACCESS_FINE);
        }
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
    }

    public void addMarker(View view) {
        if (view.getId() == R.id.addMarker) {
            marker = true;

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if (marker == true) {
                        //Crear marcador y agregarlo
                        EditText text = findViewById(R.id.markerName);

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.title(text.getText() + "");

                        float results[] = new float[10];
                        Location.distanceBetween(positionUser.getPosition().latitude, positionUser.getPosition().longitude, latLng.latitude, latLng.longitude,results);
                        markerOptions.snippet("Distancia aprox.: "+results[0]+" metros");
                        mMap.addMarker(markerOptions);
                        allMarkers.add(markerOptions);
                        mostrarInformacion();
                        marker = false;
                    }
                }
            });
        }
    }

    private void agregarPosicion(double lat, double lng) {
        LatLng coordenadas = new LatLng(lat, lng);
        CameraUpdate miUbicacion = CameraUpdateFactory.newLatLngZoom(coordenadas, 16);
        if (positionUser != null) positionUser.remove();
        positionUser = mMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title("Mi posición")
        .snippet(nombreCalle(coordenadas)));
        mMap.animateCamera(miUbicacion);

        LatLngBounds la = new LatLngBounds(coordenadas, coordenadas);
        mMap.setLatLngBoundsForCameraTarget(la);
    }

    private void actualizarPosicion(Location location) {
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            agregarPosicion(lat, lng);
        }
    }


    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            actualizarPosicion(location);
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
    };

    private void miUbicacion() {

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        actualizarPosicion(location);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,10000,0,locationListener);
    }

    public void mostrarInformacion(){
        float fin[] = new float[10];
        float results2[] = new float[10];
        int numero = 0;

        Location.distanceBetween(positionUser.getPosition().latitude, positionUser.getPosition().longitude, allMarkers.get(0).getPosition().latitude, allMarkers.get(0).getPosition().longitude,fin);
        for (int i = 1; i<=allMarkers.size()-1;i++){
            Location.distanceBetween(positionUser.getPosition().latitude, positionUser.getPosition().longitude, allMarkers.get(i).getPosition().latitude, allMarkers.get(i).getPosition().longitude,results2);

            if (fin[0] > results2[0]){
                fin[0] = results2[0];
                numero = i;
            }
        }

        TextView textView = findViewById(R.id.description);

        if (fin[0] <= 10){
            textView.setText("Usted se encuentra en: "+allMarkers.get(numero).getTitle());
        }else{
            textView.setText("Usted está cerca de: "+allMarkers.get(numero).getTitle());
        }
    }

    public String nombreCalle(LatLng coordenadas){
        Geocoder geocoder = new Geocoder(this);
        String name = "";

        if (geocoder.isPresent()){
            try{
                List<Address> addresses = geocoder.getFromLocation(coordenadas.latitude,coordenadas.longitude,1);
                Address address = addresses.get(0);
                name = address.getAddressLine(0);
            }catch (IOException e){
            }
        }
        return name;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        if (requestCode == REQUEST_ACCESS_FINE){


            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                miUbicacion();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
