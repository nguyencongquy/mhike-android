package com.quyncgcs230483.mhike;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

  private static final String API_KEY = BuildConfig.OPEN_WEATHER_API_KEY;
  private static final String TAG = "MainActivity";

  private TextInputEditText etHikeName,
      etLocationName,
      etCoordinates,
      etLength,
      etDescription,
      etGear,
      etTrailCondition;
  private TextView tvSelectedDate;
  private RadioGroup rgParking;
  private AutoCompleteTextView actDifficulty;
  private Button btnOpenMap, btnAddHike, btnViewAllHikes, btnPickDate;
  private ImageButton btnAutoFind;

  private MaterialCardView cardWeather;
  private ImageView ivWeatherIcon;
  private TextView tvTempNum, tvCondition, tvHumidityNum, tvWindNum;

  private String selectedDate = "";
  private String parkingStatus = "Yes";
  private double currentLatitude = 0.0;
  private double currentLongitude = 0.0;

  private final SimpleDateFormat sdfApp = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
  private final SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    etHikeName = findViewById(R.id.etHikeName);
    etLocationName = findViewById(R.id.etLocationName);
    etCoordinates = findViewById(R.id.etCoordinates);
    etLength = findViewById(R.id.etLength);
    etDescription = findViewById(R.id.etDescription);
    etGear = findViewById(R.id.etGear);
    etTrailCondition = findViewById(R.id.etTrailCondition);

    tvSelectedDate = findViewById(R.id.tvSelectedDate);
    rgParking = findViewById(R.id.rgParking);
    actDifficulty = findViewById(R.id.actDifficulty);

    btnOpenMap = findViewById(R.id.btnOpenMap);
    btnAddHike = findViewById(R.id.btnAddHike);
    btnViewAllHikes = findViewById(R.id.btnViewAllHikes);
    btnPickDate = findViewById(R.id.btnPickDate);
    btnAutoFind = findViewById(R.id.btnAutoFind);

    cardWeather = findViewById(R.id.cardWeather);
    ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
    tvTempNum = findViewById(R.id.tvTempNum);
    tvCondition = findViewById(R.id.tvCondition);
    tvHumidityNum = findViewById(R.id.tvHumidityNum);
    tvWindNum = findViewById(R.id.tvWindNum);

    // 3. Setup Difficulty Dropdown
    String[] difficulties = {"High", "Medium", "Low"};
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, difficulties);
    actDifficulty.setAdapter(adapter);
    actDifficulty.setText("Medium", false);

    // 4. Setup Parking Radio Group
    rgParking.setOnCheckedChangeListener(
        (group, checkedId) -> {
          if (checkedId == R.id.rbYes) parkingStatus = "Yes";
          else parkingStatus = "No";
        });

    // 5. Setup Date Picker
    btnPickDate.setOnClickListener(v -> showDatePicker());

    // 6. Open Map Logic
    btnOpenMap.setOnClickListener(
        v -> {
          Intent intent = new Intent(MainActivity.this, MapsActivity.class);
          if (currentLatitude != 0 && currentLongitude != 0) {
            intent.putExtra("current_lat", currentLatitude);
            intent.putExtra("current_long", currentLongitude);
          }
          startActivityForResult(intent, 1);
        });

    // 7. Auto Find Location Logic
    btnAutoFind.setOnClickListener(v -> searchLocationFromText());

    // 8. Save Logic
    btnAddHike.setOnClickListener(
        v -> {
          if (validateInput()) {
            Intent intent = new Intent(MainActivity.this, ConfirmHikeActivity.class);
            intent.putExtra("name", etHikeName.getText().toString());
            intent.putExtra("location", etLocationName.getText().toString());
            intent.putExtra("date", selectedDate);
            intent.putExtra("parking", parkingStatus);
            intent.putExtra("length", etLength.getText().toString());
            intent.putExtra("difficulty", actDifficulty.getText().toString());
            intent.putExtra("description", etDescription.getText().toString());
            intent.putExtra("gear", etGear.getText().toString());
            intent.putExtra("trail", etTrailCondition.getText().toString());
            intent.putExtra("latitude", currentLatitude);
            intent.putExtra("longitude", currentLongitude);
            startActivity(intent);
          }
        });

    // 9. View All Logic
    btnViewAllHikes.setOnClickListener(
        v -> {
          Intent intent = new Intent(MainActivity.this, ViewAllHikesActivity.class);
          startActivity(intent);
        });
  }

  private void searchLocationFromText() {
    String locationName = etLocationName.getText().toString().trim();
    String hikeName = etHikeName.getText().toString().trim();
    String query = locationName.isEmpty() ? hikeName : locationName;

    if (query.isEmpty()) {
      Toast.makeText(this, "Please enter Location to search!", Toast.LENGTH_SHORT).show();
      return;
    }

    Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();

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
                        etLocationName.setText(fullAddress);
                        etCoordinates.setText(
                            String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
                        currentLatitude = lat;
                        currentLongitude = lon;
                        fetchWeather(currentLatitude, currentLongitude);
                        Toast.makeText(this, "Location Found!", Toast.LENGTH_SHORT).show();
                      });
                } else {
                  runOnUiThread(
                      () -> Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show());
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
        currentLatitude = lat;
        currentLongitude = lon;
        etCoordinates.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f", lat, lon));
        fetchWeather(currentLatitude, currentLongitude);
      }
    }
  }

  private void fetchWeather(double lat, double lon) {
    if (selectedDate.isEmpty()) return;
    if (API_KEY.trim().isEmpty()) {
      cardWeather.setVisibility(View.GONE);
      return;
    }

    String url;
    if (isToday(selectedDate)) {
      url =
          "https://api.openweathermap.org/data/2.5/weather?lat="
              + lat
              + "&lon="
              + lon
              + "&appid="
              + API_KEY
              + "&units=metric";
      getJsonWeather(url, true);
    } else if (isDateWithin5Days(selectedDate)) {
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
      cardWeather.setVisibility(View.GONE);
      Toast.makeText(this, "Weather forecast only available for next 5 days", Toast.LENGTH_SHORT)
          .show();
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
                  String targetDate = formatDateForAPI(selectedDate);
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
                tvTempNum.setText(String.format(Locale.getDefault(), "%.0f", temp));

                int humidity = main.getInt("humidity");
                tvHumidityNum.setText(String.valueOf(humidity));

                JSONObject windObj = dataObj.getJSONObject("wind");
                double windMs = windObj.getDouble("speed");
                double windKmh = windMs * 3.6;
                tvWindNum.setText(String.format(Locale.getDefault(), "%.1f", windKmh));

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
            error -> {
              tvCondition.setText("Weather N/A");
              Toast.makeText(this, "Error fetching weather", Toast.LENGTH_SHORT).show();
            });

    Volley.newRequestQueue(this).add(request);
  }

  private void showDatePicker() {
    final Calendar calendar = Calendar.getInstance();
    DatePickerDialog datePickerDialog =
        new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
              selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
              tvSelectedDate.setText(selectedDate);
              if (currentLatitude != 0) fetchWeather(currentLatitude, currentLongitude);
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

  private boolean validateInput() {
    if (etHikeName.getText().toString().isEmpty()) {
      etHikeName.setError("Required");
      return false;
    }
    if (etLocationName.getText().toString().isEmpty()) {
      etLocationName.setError("Required");
      return false;
    }
    if (selectedDate.isEmpty()) {
      Toast.makeText(this, "Select date", Toast.LENGTH_SHORT).show();
      return false;
    }
    if (etLength.getText().toString().isEmpty()) {
      etLength.setError("Required");
      return false;
    }
    try {
      if (Double.parseDouble(etLength.getText().toString()) <= 0) {
        etLength.setError("Must be greater than zero");
        return false;
      }
    } catch (NumberFormatException exception) {
      etLength.setError("Enter a valid number");
      return false;
    }
    return true;
  }
}
