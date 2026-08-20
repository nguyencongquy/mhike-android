package com.quyncgcs230483.mhike;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapsActivity extends AppCompatActivity {
  private static final int LOCATION_PERMISSION_REQUEST = 1;

  private MapView map;
  private MyLocationNewOverlay mLocationOverlay;
  private IMapController mapController;
  private Button btnConfirmLocation;
  private ImageView ivCenterPin;

  private DatabaseHelper dbHelper;
  private int viewHikeId = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
    setContentView(R.layout.activity_maps);

    dbHelper = new DatabaseHelper(this);

    map = findViewById(R.id.map);
    btnConfirmLocation = findViewById(R.id.btnConfirmLocation);

    ivCenterPin = findViewById(R.id.ivCenterPin);

    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setMultiTouchControls(true);
    mapController = map.getController();
    mapController.setZoom(18.0);

    // ============================================================

    // ============================================================

    Intent intent = getIntent();

    if (intent.hasExtra("HIKE_ID")) {
      viewHikeId = intent.getIntExtra("HIKE_ID", -1);

      if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.GONE);
      if (ivCenterPin != null) ivCenterPin.setVisibility(View.GONE);

      loadRoute(viewHikeId);

    } else {

      if (btnConfirmLocation != null) btnConfirmLocation.setVisibility(View.VISIBLE);
      if (ivCenterPin != null) ivCenterPin.setVisibility(View.VISIBLE);

      double passedLat = intent.getDoubleExtra("current_lat", 0.0);
      double passedLon = intent.getDoubleExtra("current_long", 0.0);

      if (passedLat != 0.0 && passedLon != 0.0) {

        GeoPoint startPoint = new GeoPoint(passedLat, passedLon);
        mapController.setCenter(startPoint);

        // addMarker(startPoint);
      } else {

        setupGPS();
      }
    }

    if (btnConfirmLocation != null) {
      btnConfirmLocation.setOnClickListener(
          v -> {
            IGeoPoint center = map.getMapCenter();
            double lat = center.getLatitude();
            double lon = center.getLongitude();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", lat);
            resultIntent.putExtra("longitude", lon);
            setResult(RESULT_OK, resultIntent);
            finish();
          });
    }
  }

  private void loadRoute(int hikeId) {

    Cursor cursor = dbHelper.getTrackPoints(hikeId);
    List<GeoPoint> routePoints = new ArrayList<>();

    if (cursor.moveToFirst()) {
      do {
        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TP_LAT));
        double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TP_LON));
        routePoints.add(new GeoPoint(lat, lon));
      } while (cursor.moveToNext());
    }
    cursor.close();

    if (!routePoints.isEmpty()) {
      Polyline line = new Polyline();
      line.setPoints(routePoints);
      line.setColor(Color.RED);
      line.setWidth(10.0f);

      map.getOverlays().add(line);

      mapController.setCenter(routePoints.get(0));
      mapController.setZoom(16.0);

      addMarker(routePoints.get(0), "Start");
      addMarker(routePoints.get(routePoints.size() - 1), "End");

      map.invalidate();
    } else {
      Toast.makeText(this, "No tracking data found for this hike.", Toast.LENGTH_SHORT).show();
    }
  }

  private void addMarker(GeoPoint p, String title) {
    Marker m = new Marker(map);
    m.setPosition(p);
    m.setTitle(title);
    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
    map.getOverlays().add(m);
  }

  private void setupGPS() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
      mLocationOverlay.enableMyLocation();
      map.getOverlays().add(mLocationOverlay);
      mLocationOverlay.runOnFirstFix(
          () ->
              runOnUiThread(
                  () -> {
                    GeoPoint location = mLocationOverlay.getMyLocation();
                    if (location != null) {
                      mapController.animateTo(location);
                    }
                  }));
    } else {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          LOCATION_PERMISSION_REQUEST);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == LOCATION_PERMISSION_REQUEST
        && grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      setupGPS();
    } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
      Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (map != null) map.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (map != null) map.onPause();
  }

  @Override
  protected void onDestroy() {
    if (mLocationOverlay != null) {
      mLocationOverlay.disableMyLocation();
    }
    dbHelper.close();
    super.onDestroy();
  }
}
