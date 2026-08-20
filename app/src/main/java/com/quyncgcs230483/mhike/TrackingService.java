package com.quyncgcs230483.mhike;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/** Foreground service that records route points for one active hike. */
public final class TrackingService extends Service {

  public static final String EXTRA_HIKE_ID = "HIKE_ID";

  private static final String TAG = "TrackingService";
  private static final String CHANNEL_ID = "route_tracking";
  private static final int NOTIFICATION_ID = 1;
  private static final long UPDATE_INTERVAL_MS = 5_000L;
  private static final float MIN_UPDATE_DISTANCE_METERS = 5f;

  private FusedLocationProviderClient fusedLocationClient;
  private LocationCallback locationCallback;
  private DatabaseHelper databaseHelper;
  private int hikeId = -1;
  private boolean updatesRequested;

  @Override
  public void onCreate() {
    super.onCreate();
    databaseHelper = new DatabaseHelper(this);
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    locationCallback =
        new LocationCallback() {
          @Override
          public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult == null ? null : locationResult.getLastLocation();
            if (location != null && hikeId >= 0) {
              databaseHelper.insertTrackPoint(
                  hikeId, location.getLatitude(), location.getLongitude());
            }
          }
        };
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    createNotificationChannel();
    startForeground(NOTIFICATION_ID, buildNotification());

    if (intent == null || !intent.hasExtra(EXTRA_HIKE_ID)) {
      Log.w(TAG, "Stopping tracking because hike ID is missing");
      stopSelf(startId);
      return START_NOT_STICKY;
    }

    hikeId = intent.getIntExtra(EXTRA_HIKE_ID, -1);
    if (hikeId < 0 || !hasLocationPermission()) {
      Log.w(TAG, "Stopping tracking because ID or location permission is invalid");
      stopSelf(startId);
      return START_NOT_STICKY;
    }

    requestLocationUpdates();
    return START_NOT_STICKY;
  }

  private Notification buildNotification() {
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.tracking_notification_title))
        .setContentText(getString(R.string.tracking_notification_text))
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build();
  }

  private boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestLocationUpdates() {
    if (updatesRequested) {
      return;
    }
    LocationRequest request =
        new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .build();

    try {
      fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
      updatesRequested = true;
    } catch (SecurityException exception) {
      Log.e(TAG, "Location permission disappeared before tracking started", exception);
      stopSelf();
    }
  }

  @Override
  public void onDestroy() {
    if (updatesRequested) {
      fusedLocationClient.removeLocationUpdates(locationCallback);
      updatesRequested = false;
    }
    databaseHelper.close();
    super.onDestroy();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel =
          new NotificationChannel(
              CHANNEL_ID,
              getString(R.string.tracking_channel_name),
              NotificationManager.IMPORTANCE_LOW);
      NotificationManager manager = getSystemService(NotificationManager.class);
      if (manager != null) {
        manager.createNotificationChannel(channel);
      }
    }
  }
}
