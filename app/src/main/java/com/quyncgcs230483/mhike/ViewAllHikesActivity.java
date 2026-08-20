package com.quyncgcs230483.mhike;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ViewAllHikesActivity extends AppCompatActivity {

  private RecyclerView recyclerView;
  private HikeAdapter adapter;
  private List<Hike> hikeList;
  private DatabaseHelper dbHelper;

  private ImageButton btnSearchIcon, btnMenuIcon;
  private FloatingActionButton fabAddHike;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view_all_hikes);

    recyclerView = findViewById(R.id.rvHikes);
    btnSearchIcon = findViewById(R.id.btnSearchIcon);
    btnMenuIcon = findViewById(R.id.btnMenuIcon);
    fabAddHike = findViewById(R.id.fabAddHike);

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    hikeList = new ArrayList<>();
    dbHelper = new DatabaseHelper(this);

    loadHikesFromDatabase();

    adapter = new HikeAdapter(hikeList, this);
    recyclerView.setAdapter(adapter);

    btnSearchIcon.setOnClickListener(v -> showAdvancedSearchDialog());

    btnMenuIcon.setOnClickListener(v -> showPopupMenu(v));

    fabAddHike.setOnClickListener(
        v -> {
          startActivity(new Intent(ViewAllHikesActivity.this, MainActivity.class));
        });
  }

  private void showAdvancedSearchDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_advanced_search, null);
    builder.setView(view);
    AlertDialog dialog = builder.create();

    EditText etName = view.findViewById(R.id.etSearchName);
    EditText etLocation = view.findViewById(R.id.etSearchLocation);
    EditText etDate = view.findViewById(R.id.etSearchDate);
    EditText etLength = view.findViewById(R.id.etSearchLength);
    Button btnSearch = view.findViewById(R.id.btnPerformSearch);
    Button btnReset = view.findViewById(R.id.btnResetSearch);

    etDate.setOnClickListener(
        v -> {
          Calendar calendar = Calendar.getInstance();
          new DatePickerDialog(
                  this,
                  (view1, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    etDate.setText(date);
                  },
                  calendar.get(Calendar.YEAR),
                  calendar.get(Calendar.MONTH),
                  calendar.get(Calendar.DAY_OF_MONTH))
              .show();
        });

    btnSearch.setOnClickListener(
        v -> {
          String sName = etName.getText().toString().trim();
          String sLocation = etLocation.getText().toString().trim();
          String sDate = etDate.getText().toString().trim();
          String sLength = etLength.getText().toString().trim();

          performAdvancedSearch(sName, sLocation, sDate, sLength);
          dialog.dismiss();
        });

    btnReset.setOnClickListener(
        v -> {
          adapter.filterList(hikeList);
          Toast.makeText(this, "Filter cleared", Toast.LENGTH_SHORT).show();
          dialog.dismiss();
        });

    dialog.show();
  }

  private void performAdvancedSearch(String name, String location, String date, String length) {
    List<Hike> filteredList = HikeFilter.filter(hikeList, name, location, date, length);

    if (filteredList.isEmpty()) {
      Toast.makeText(this, "No hikes found matching criteria", Toast.LENGTH_SHORT).show();
      adapter.filterList(new ArrayList<>());
    } else {
      adapter.filterList(filteredList);
      Toast.makeText(this, "Found " + filteredList.size() + " hike(s)", Toast.LENGTH_SHORT).show();
    }
  }

  private void showPopupMenu(View view) {
    PopupMenu popup = new PopupMenu(this, view);
    popup.getMenu().add("Delete All Hikes");
    popup.setOnMenuItemClickListener(
        item -> {
          if (item.getTitle().equals("Delete All Hikes")) {
            confirmDeleteAll();
            return true;
          }
          return false;
        });
    popup.show();
  }

  private void loadHikesFromDatabase() {
    hikeList.clear();
    try (Cursor cursor = dbHelper.getHikes()) {
      if (cursor.moveToFirst()) {
        do {
          int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID));
          String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME));
          String location =
              cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LOCATION));
          String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
          String parking =
              cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PARKING));
          String length = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LENGTH));
          String difficulty =
              cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DIFFICULTY));
          String description =
              cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION));
          String gear = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GEAR));
          String trail = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRAIL));
          double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LATITUDE));
          double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_LONGITUDE));

          hikeList.add(
              new Hike(
                  id,
                  name,
                  location,
                  date,
                  parking,
                  length,
                  difficulty,
                  description,
                  gear,
                  trail,
                  lat,
                  lon));
        } while (cursor.moveToNext());
      }
    }
  }

  private void confirmDeleteAll() {
    new AlertDialog.Builder(this)
        .setTitle("Delete All Hikes")
        .setMessage("Are you sure you want to delete EVERYTHING?")
        .setPositiveButton(
            "Yes",
            (dialog, which) -> {
              dbHelper.deleteAllHikes();
              hikeList.clear();
              adapter.filterList(hikeList);
              Toast.makeText(this, "All hikes deleted!", Toast.LENGTH_SHORT).show();
            })
        .setNegativeButton("No", null)
        .show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadHikesFromDatabase();
    if (adapter != null) {
      adapter.filterList(hikeList);
    }
  }

  @Override
  protected void onDestroy() {
    dbHelper.close();
    super.onDestroy();
  }
}
