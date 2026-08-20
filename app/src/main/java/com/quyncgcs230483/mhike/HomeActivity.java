package com.quyncgcs230483.mhike;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

  private Button btnAdd, btnView, btnAbout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    btnAdd = findViewById(R.id.btnHomeAdd);
    btnView = findViewById(R.id.btnHomeView);
    btnAbout = findViewById(R.id.btnHomeAbout);

    btnAdd.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
    btnView.setOnClickListener(v -> startActivity(new Intent(this, ViewAllHikesActivity.class)));
    btnAbout.setOnClickListener(v -> showAboutDialog());
  }

  private void showAboutDialog() {
    new AlertDialog.Builder(this)
        .setTitle("About M-Hike")
        .setMessage(
            "M-Hike App v1.0\n\n"
                + "Developed by: Nguyen Cong Quy\n"
                + "Student ID: GCS230483\n"
                + "Coursework Project 2025")
        .setPositiveButton("Close", null)
        .setIcon(android.R.drawable.ic_dialog_info)
        .show();
  }
}
