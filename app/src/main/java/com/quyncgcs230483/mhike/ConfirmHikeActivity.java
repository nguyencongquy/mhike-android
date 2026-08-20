package com.quyncgcs230483.mhike;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConfirmHikeActivity extends AppCompatActivity {

  private TextView tvName,
      tvLocation,
      tvDate,
      tvParking,
      tvLength,
      tvDifficulty,
      tvDesc,
      tvGear,
      tvTrail;
  private Button btnEdit, btnConfirm;
  private String name, location, date, parking, length, difficulty, description, gear, trail;
  private double latitude, longitude;

  private DatabaseHelper dbHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_confirm_hike);

    dbHelper = new DatabaseHelper(this);

    tvName = findViewById(R.id.tvConfirmName);
    tvLocation = findViewById(R.id.tvConfirmLocation);
    tvDate = findViewById(R.id.tvConfirmDate);
    tvParking = findViewById(R.id.tvConfirmParking);
    tvLength = findViewById(R.id.tvConfirmLength);
    tvDifficulty = findViewById(R.id.tvConfirmDifficulty);
    tvDesc = findViewById(R.id.tvConfirmDescription);
    tvGear = findViewById(R.id.tvConfirmGear);
    tvTrail = findViewById(R.id.tvConfirmTrail);

    btnEdit = findViewById(R.id.btnEdit);
    btnConfirm = findViewById(R.id.btnConfirm);

    Intent intent = getIntent();
    if (intent != null && intent.hasExtra("name")) {

      name = intent.getStringExtra("name");
      location = intent.getStringExtra("location");
      date = intent.getStringExtra("date");
      parking = intent.getStringExtra("parking");
      length = intent.getStringExtra("length");
      difficulty = intent.getStringExtra("difficulty");
      description = intent.getStringExtra("description");
      gear = intent.getStringExtra("gear");
      trail = intent.getStringExtra("trail");

      latitude = intent.getDoubleExtra("latitude", 0.0);
      longitude = intent.getDoubleExtra("longitude", 0.0);

      tvName.setText("Name: " + name);
      tvLocation.setText("Location: " + location);
      tvDate.setText("Date: " + date);
      tvParking.setText("Parking: " + parking);
      tvLength.setText("Length: " + length + " km");
      tvDifficulty.setText("Difficulty: " + difficulty);
      tvDesc.setText("Description: " + description);

      tvGear.setText("Gear: " + (gear != null && !gear.isEmpty() ? gear : "None"));
      tvTrail.setText("Trail: " + (trail != null && !trail.isEmpty() ? trail : "None"));
    } else {
      Toast.makeText(this, "Hike details are missing", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    btnEdit.setOnClickListener(v -> finish());
    btnConfirm.setOnClickListener(v -> saveHikeToDatabase());
  }

  private void saveHikeToDatabase() {
    try {
      dbHelper.insertHike(
          name,
          location,
          date,
          parking,
          length,
          difficulty,
          description,
          gear,
          trail,
          latitude,
          longitude);
    } catch (SQLException exception) {
      Toast.makeText(this, "Error saving hike. Please try again.", Toast.LENGTH_LONG).show();
      return;
    }

    Toast.makeText(this, "Hike saved successfully!", Toast.LENGTH_LONG).show();
    Intent intent = new Intent(this, HomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }

  @Override
  protected void onDestroy() {
    dbHelper.close();
    super.onDestroy();
  }
}
