package com.quyncgcs230483.mhike;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class HikeDetailActivity extends AppCompatActivity {

  private static final String API_KEY = BuildConfig.OPEN_WEATHER_API_KEY;
  private static final String TAG = "HikeDetailActivity";
  private static final int LOCATION_PERMISSION_REQUEST = 100;

  private TextInputEditText etName,
      etLocation,
      etCoordinates,
      etDate,
      etLength,
      etDesc,
      etGear,
      etTrail;
  private AutoCompleteTextView actDifficulty;
  private RadioGroup rgParking;

  private Button btnPickDate,
      btnUpdate,
      btnDelete,
      btnStartHike,
      btnStopTrack,
      btnViewObs,
      btnOpenMap,
      btnViewRoute;
  private ImageButton btnBack, btnAutoFind;
  private TextView tvTrackedDistance;

  // Weather UI
  private MaterialCardView cardWeather;
  private ImageView ivWeatherIcon;
  private TextView tvTemp, tvCondition, tvHumidity, tvWind;

  private DatabaseHelper dbHelper;
  private int hikeId = -1;
  private String parkingStatus = "Yes";
  private double currentLat = 0.0;
  private double currentLong = 0.0;

  private final SimpleDateFormat sdfApp = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
  private final SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hike_detail);

    dbHelper = new DatabaseHelper(this);

    btnBack = findViewById(R.id.btnBackDetail);
    etName = findViewById(R.id.etNameDetail);
    etLocation = findViewById(R.id.etLocationDetail);
    etCoordinates = findViewById(R.id.etCoordinatesDetail);
    etDate = findViewById(R.id.etDateDetail);
    etLength = findViewById(R.id.etLengthDetail);
    actDifficulty = findViewById(R.id.actDifficultyDetail);
    etDesc = findViewById(R.id.etDescDetail);
    etGear = findViewById(R.id.etGearDetail);
    etTrail = findViewById(R.id.etTrailDetail);

    rgParking = findViewById(R.id.rgParkingDetail);

    btnPickDate = findViewById(R.id.btnPickDateDetail);
    btnAutoFind = findViewById(R.id.btnAutoFindDetail);
    btnOpenMap = findViewById(R.id.btnOpenMapDetail);
    btnUpdate = findViewById(R.id.btnUpdateHike);
    btnDelete = findViewById(R.id.btnDeleteHike);

    // Tracking UI
    btnStartHike = findViewById(R.id.btnStartTrack);
    btnStopTrack = findViewById(R.id.btnStopTrack);
    btnViewRoute = findViewById(R.id.btnViewRoute);
    tvTrackedDistance = findViewById(R.id.tvTrackedDistance);

    btnViewObs = findViewById(R.id.btnViewObservation);

    cardWeather = findViewById(R.id.cardWeatherDetail);
    ivWeatherIcon = findViewById(R.id.ivWeatherIconDetail);
    tvTemp = findViewById(R.id.tvTempDetail);
    tvCondition = findViewById(R.id.tvConditionDetail);
    tvHumidity = findViewById(R.id.tvHumidityDetail);
    tvWind = findViewById(R.id.tvWindDetail);

    // 4. Setup Difficulty & Parking
    String[] difficulties = {"High", "Medium", "Low"};
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, difficulties);
    actDifficulty.setAdapter(adapter);

    rgParking.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbYesDetail) parkingStatus = "Yes";
          else parkingStatus = "No";
        });

    if (getIntent().hasExtra("HIKE_ID")) {
      hikeId = getIntent().getIntExtra("HIKE_ID", -1);
      if (!loadHikeDetails(hikeId)) {
        Toast.makeText(this, "Hike not found", Toast.LENGTH_SHORT).show();
        finish();
        return;
      }
      calculateTrackedDistance();
    } else {
      Toast.makeText(this, "Hike not found", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    btnBack.setOnClickListener(v -> finish());
    btnPickDate.setOnClickListener(v -> showDatePicker());

    btnAutoFind.setOnClickListener(v -> searchLocationFromText());

    btnOpenMap.setOnClickListener(
        v -> {
          Intent intent = new Intent(HikeDetailActivity.this, MapsActivity.class);

          if (currentLat != 0 && currentLong != 0) {
            intent.putExtra("current_lat", currentLat);
            intent.putExtra("current_long", currentLong);
          }
          startActivityForResult(intent, 1);
        });
    // -------------------------

    btnUpdate.setOnClickListener(v -> updateHike());
    btnDelete.setOnClickListener(v -> confirmDelete());

    btnStartHike.setOnClickListener(
        v -> {
          if (checkPermissions()) {
            startTrackingService();
            btnStartHike.setVisibility(View.GONE);
            btnStopTrack.setVisibility(View.VISIBLE);
          } else {
            requestPermissions();
          }
        });

    btnStopTrack.setOnClickListener(
        v -> {
          stopTrackingService();
          btnStartHike.setVisibility(View.VISIBLE);
          btnStopTrack.setVisibility(View.GONE);
        });

    btnViewRoute.setOnClickListener(
        v -> {
          Intent intent = new Intent(this, MapsActivity.class);
          intent.putExtra("HIKE_ID", hikeId);
          startActivity(intent);
        });

    btnViewObs.setOnClickListener(
        v -> {
          Intent intent = new Intent(HikeDetailActivity.this, ObservationActivity.class);
          intent.putExtra("HIKE_ID", hikeId);
          intent.putExtra("HIKE_NAME", etName.getText().toString());
          startActivity(intent);
        });
  }

  // =========================================================================

  // =========================================================================

  private void startTrackingService() {
    Intent serviceIntent = new Intent(this, TrackingService.class);
    serviceIntent.putExtra(TrackingService.EXTRA_HIKE_ID, hikeId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      startForegroundService(serviceIntent);
    } else {
      startService(serviceIntent);
    }
    Toast.makeText(this, "Tracking Started!", Toast.LENGTH_SHORT).show();
  }

  private void stopTrackingService() {
    Intent serviceIntent = new Intent(this, TrackingService.class);
    stopService(serviceIntent);
    Toast.makeText(this, "Tracking Stopped!", Toast.LENGTH_SHORT).show();
    calculateTrackedDistance();
  }

  private void calculateTrackedDistance() {
    Cursor cursor = dbHelper.getTrackPoints(hikeId);
    if (cursor == null || cursor.getCount() < 2) {
      tvTrackedDistance.setText("Tracked Distance: 0.00 km");
      if (cursor != null) cursor.close();
      return;
    }
    float totalDistanceMeters = 0;
    double startLat = 0, startLon = 0;
    boolean isFirstPoint = true;
    if (cursor.moveToFirst()) {
      do {
        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TP_LAT));
        double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TP_LON));
        if (isFirstPoint) {
          startLat = lat;
          startLon = lon;
          isFirstPoint = false;
        } else {
          float[] results = new float[1];
          Location.distanceBetween(startLat, startLon, lat, lon, results);
          totalDistanceMeters += results[0];
          startLat = lat;
          startLon = lon;
        }
      } while (cursor.moveToNext());
    }
    cursor.close();
    float totalDistanceKm = totalDistanceMeters / 1000;
    tvTrackedDistance.setText(
        String.format(Locale.getDefault(), "Tracked Distance: %.2f km", totalDistanceKm));
  }

  // =========================================================================

  private boolean checkPermissions() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS
          },
          LOCATION_PERMISSION_REQUEST);
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
    if (requestCode != LOCATION_PERMISSION_REQUEST) {
      return;
    }
    if (checkPermissions()) {
      startTrackingService();
      btnStartHike.setVisibility(View.GONE);
      btnStopTrack.setVisibility(View.VISIBLE);
    } else {
      Toast.makeText(this, "Location permission is required for route tracking", Toast.LENGTH_LONG)
          .show();
    }
  }

  private boolean loadHikeDetails(int id) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor =
        db.rawQuery(
            "SELECT * FROM "
                + DatabaseHelper.TABLE_HIKES
                + " WHERE "
                + DatabaseHelper.COL_ID
                + " = ?",
            new String[] {String.valueOf(id)})) {
      if (!cursor.moveToFirst()) {
        return false;
      }
      etName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)));
      etLocation.setText(
          cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION)));
      etDate.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE)));
      etLength.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LENGTH)));
      actDifficulty.setText(
          cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DIFFICULTY)), false);
      etDesc.setText(
          cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION)));
      etGear.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GEAR)));
      etTrail.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRAIL)));

      String parking = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PARKING));
      if (parking.equals("Yes")) rgParking.check(R.id.rbYesDetail);
      else rgParking.check(R.id.rbNoDetail);

      currentLat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LATITUDE));
      currentLong = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LONGITUDE));

      if (currentLat != 0 && currentLong != 0) {
        etCoordinates.setText(
            String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", currentLat, currentLong));
        fetchWeather(currentLat, currentLong);
      }
      return true;
    }
  }

  private void searchLocationFromText() {
    String locationName = etLocation.getText().toString().trim();
    String hikeName = etName.getText().toString().trim();

    String query = !hikeName.isEmpty() ? hikeName : locationName;

    if (query.isEmpty()) {
      Toast.makeText(this, "Please enter Name or Location to search!", Toast.LENGTH_SHORT).show();
      return;
    }

    Toast.makeText(this, "Searching for: " + query + "...", Toast.LENGTH_SHORT).show();

    new Thread(
            () -> {
              Geocoder geocoder = new Geocoder(this, Locale.getDefault());
              try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                if (addresses != null && !addresses.isEmpty()) {
                  Address address = addresses.get(0);
                  double lat = address.getLatitude();
                  double lon = address.getLongitude();
                  String fullAddress = address.getAddressLine(0);

                  runOnUiThread(
                      () -> {
                        etLocation.setText(fullAddress);
                        etCoordinates.setText(
                            String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
                        currentLat = lat;
                        currentLong = lon;
                        fetchWeather(currentLat, currentLong);
                        Toast.makeText(this, "Location Updated to: " + query, Toast.LENGTH_SHORT)
                            .show();
                      });
                } else {
                  runOnUiThread(
                      () ->
                          Toast.makeText(
                                  this, "Location not found for: " + query, Toast.LENGTH_SHORT)
                              .show());
                }
              } catch (IOException exception) {
                Log.e(TAG, "Location search failed", exception);
                runOnUiThread(
                    () -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show());
              }
            })
        .start();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
      double lat = data.getDoubleExtra("latitude", 0.0);
      double lon = data.getDoubleExtra("longitude", 0.0);

      if (lat != 0.0 && lon != 0.0) {
        currentLat = lat;
        currentLong = lon;
        etCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
        fetchWeather(currentLat, currentLong);
      }
    }
  }

  private void fetchWeather(double lat, double lon) {

    if (lat == 0.0 || lon == 0.0) {
      cardWeather.setVisibility(View.GONE);
      return;
    }
    if (API_KEY.trim().isEmpty()) {
      cardWeather.setVisibility(View.GONE);
      return;
    }

    String dateStr = etDate.getText().toString();
    String url;

    if (!dateStr.isEmpty() && isDateWithin5Days(dateStr)) {
      url =
          "https://api.openweathermap.org/data/2.5/forecast?lat="
              + lat
              + "&lon="
              + lon
              + "&appid="
              + API_KEY
              + "&units=metric";
      getJsonWeather(url, false);
    } else {

      url =
          "https://api.openweathermap.org/data/2.5/weather?lat="
              + lat
              + "&lon="
              + lon
              + "&appid="
              + API_KEY
              + "&units=metric";
      getJsonWeather(url, true);
    }
  }

  private void getJsonWeather(String url, boolean isCurrent) {
    cardWeather.setVisibility(View.VISIBLE);
    tvCondition.setText("Loading...");

    JsonObjectRequest request =
        new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
              try {
                JSONObject dataObj;
                if (isCurrent) {
                  dataObj = response;
                } else {
                  JSONArray list = response.getJSONArray("list");
                  dataObj = list.getJSONObject(0);
                  String targetDate = formatDateForAPI(etDate.getText().toString());
                  for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    if (item.getString("dt_txt").contains(targetDate)
                        && item.getString("dt_txt").contains("12:00:00")) {
                      dataObj = item;
                      break;
                    }
                  }
                }

                JSONObject main = dataObj.getJSONObject("main");
                double temp = main.getDouble("temp");
                tvTemp.setText(String.format(Locale.getDefault(), "%.0f", temp));

                int humidity = main.getInt("humidity");
                tvHumidity.setText(humidity + "");

                JSONObject windObj = dataObj.getJSONObject("wind");
                double windSpeed = windObj.getDouble("speed") * 3.6;
                tvWind.setText(String.format(Locale.getDefault(), "%.1f", windSpeed));

                JSONArray weatherArray = dataObj.getJSONArray("weather");
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                String description = weatherObj.getString("description");
                String iconCode = weatherObj.getString("icon");

                tvCondition.setText(
                    description.substring(0, 1).toUpperCase() + description.substring(1));

                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
                Glide.with(this).load(iconUrl).into(ivWeatherIcon);

              } catch (Exception exception) {
                Log.e(TAG, "Weather response could not be parsed", exception);
                tvCondition.setText("Weather N/A");
              }
            },
            error -> tvCondition.setText("Weather N/A"));

    Volley.newRequestQueue(this).add(request);
  }

  private void updateHike() {
    if (etName.getText().toString().isEmpty() || etLocation.getText().toString().isEmpty()) {
      Toast.makeText(this, "Name and Location are required!", Toast.LENGTH_SHORT).show();
      return;
    }
    dbHelper.updateHike(
        hikeId,
        etName.getText().toString(),
        etLocation.getText().toString(),
        etDate.getText().toString(),
        parkingStatus,
        etLength.getText().toString(),
        actDifficulty.getText().toString(),
        etDesc.getText().toString(),
        etGear.getText().toString(),
        etTrail.getText().toString(),
        currentLat,
        currentLong);
    Toast.makeText(this, "Hike Updated Successfully!", Toast.LENGTH_SHORT).show();
    finish();
  }

  private void confirmDelete() {
    new AlertDialog.Builder(this)
        .setTitle("Delete Hike")
        .setMessage("Are you sure you want to delete this hike?")
        .setPositiveButton(
            "Yes",
            (dialog, which) -> {
              dbHelper.deleteHike(hikeId);
              Toast.makeText(HikeDetailActivity.this, "Hike Deleted!", Toast.LENGTH_SHORT).show();
              finish();
            })
        .setNegativeButton("No", null)
        .show();
  }

  private void showDatePicker() {
    final Calendar calendar = Calendar.getInstance();
    DatePickerDialog datePickerDialog =
        new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
              String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
              etDate.setText(selectedDate);
              fetchWeather(currentLat, currentLong);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH));
    datePickerDialog.show();
  }

  private boolean isToday(String dateStr) {
    try {
      return sdfApp.format(sdfApp.parse(dateStr)).equals(sdfApp.format(new Date()));
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isDateWithin5Days(String dateStr) {
    try {
      Date input = sdfApp.parse(dateStr);
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_YEAR, 5);
      return input.after(new Date()) && input.before(cal.getTime());
    } catch (Exception e) {
      return false;
    }
  }

  private String formatDateForAPI(String dateStr) {
    try {
      return sdfApi.format(sdfApp.parse(dateStr));
    } catch (Exception e) {
      return "";
    }
  }

  @Override
  protected void onDestroy() {
    dbHelper.close();
    super.onDestroy();
  }
}
