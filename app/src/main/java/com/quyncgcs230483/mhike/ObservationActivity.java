package com.quyncgcs230483.mhike;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ObservationActivity extends AppCompatActivity {
  private static final String TAG = "ObservationActivity";

  private TextView tvTitle;
  private Button btnAdd;
  private RecyclerView recyclerView;
  private ImageButton btnBack;

  private DatabaseHelper dbHelper;
  private ObservationAdapter adapter;
  private List<Observation> observationList;
  private int hikeId = -1;
  private String hikeName = "";

  // Camera Variables
  private static final int REQUEST_IMAGE_CAPTURE = 1;
  private static final int REQUEST_CAMERA_PERMISSION = 200;
  private ImageView ivPreview;
  private String encodedImage = "";

  // Audio Variables
  private MediaRecorder mediaRecorder;
  private MediaPlayer mediaPlayer;
  private String currentAudioPath = "";
  private boolean isRecording = false;
  private static final int REQUEST_AUDIO_PERMISSION = 100;

  private AlertDialog currentDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation);

    tvTitle = findViewById(R.id.tvHikeTitle);
    btnAdd = findViewById(R.id.btnAddObservation);
    recyclerView = findViewById(R.id.rvObservations);
    btnBack = findViewById(R.id.btnBackObs);

    dbHelper = new DatabaseHelper(this);
    observationList = new ArrayList<>();

    if (getIntent().hasExtra("HIKE_ID")) {
      hikeId = getIntent().getIntExtra("HIKE_ID", -1);
      hikeName = getIntent().getStringExtra("HIKE_NAME");
      if (tvTitle != null) {
        tvTitle.setText("Observations for: " + hikeName);
      }
    }

    adapter =
        new ObservationAdapter(
            observationList,
            new ObservationAdapter.OnItemLongClickListener() {
              @Override
              public void onItemLongClick(Observation observation) {
                showActionDialog(observation);
              }
            });

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    btnAdd.setOnClickListener(v -> showAddDialog(null));

    loadObservations();
  }

  private void showActionDialog(Observation observation) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_observation_action, null);
    builder.setView(view);

    Button btnEdit = view.findViewById(R.id.btnActionEdit);
    Button btnDelete = view.findViewById(R.id.btnActionDelete);

    AlertDialog dialog = builder.create();

    if (dialog.getWindow() != null) {
      dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    btnEdit.setOnClickListener(
        v -> {
          dialog.dismiss();
          showAddDialog(observation);
        });

    btnDelete.setOnClickListener(
        v -> {
          dialog.dismiss();
          confirmDelete(observation);
        });

    dialog.show();
  }

  private void confirmDelete(Observation observation) {
    new AlertDialog.Builder(this)
        .setTitle("Confirm Delete")
        .setMessage("Are you sure you want to delete this observation?")
        .setPositiveButton(
            "Yes",
            (dialog, which) -> {
              dbHelper.deleteObservation(observation.getId());
              deleteAudioFile(observation.getAudioPath());
              loadObservations();
              Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
            })
        .setNegativeButton("No", null)
        .show();
  }

  private void showAddDialog(@Nullable Observation obsToEdit) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_observation, null);
    builder.setView(view);

    EditText etName = view.findViewById(R.id.etObservationName);
    EditText etComment = view.findViewById(R.id.etObservationComment);
    EditText etTime = view.findViewById(R.id.etObservationTime);

    etTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    Button btnCamera = view.findViewById(R.id.btnCapturePhoto);
    ivPreview = view.findViewById(R.id.ivPreviewDialog);

    Button btnRecord = view.findViewById(R.id.btnRecordAudio);
    Button btnPlay = view.findViewById(R.id.btnPlayAudio);
    TextView tvAudioStatus = view.findViewById(R.id.tvAudioStatus);

    Button btnSave = view.findViewById(R.id.btnSaveObservation);
    Button btnCancel = view.findViewById(R.id.btnCancelObservation);
    String originalAudioPath = obsToEdit == null ? "" : obsToEdit.getAudioPath();
    boolean[] observationSaved = {false};

    currentDialog = builder.create();
    if (currentDialog.getWindow() != null) {
      currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    // --- EDIT MODE: LOAD DATA ---
    if (obsToEdit != null) {
      etName.setText(obsToEdit.getObservation());
      etComment.setText(obsToEdit.getComments());
      etTime.setText(obsToEdit.getTime());
      encodedImage = obsToEdit.getImagePath();
      if (encodedImage != null && !encodedImage.isEmpty()) {
        try {
          byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
          Bitmap decodedByte =
              BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
          ivPreview.setImageBitmap(decodedByte);
          ivPreview.setVisibility(View.VISIBLE);
        } catch (Exception exception) {
          Log.e(TAG, "Stored observation image could not be decoded", exception);
        }
      }

      currentAudioPath = obsToEdit.getAudioPath();
      if (currentAudioPath != null && !currentAudioPath.isEmpty()) {
        tvAudioStatus.setText("Has old audio");
        btnPlay.setVisibility(View.VISIBLE);
      }

      btnSave.setText("UPDATE");
    } else {
      encodedImage = "";
      currentAudioPath = "";
    }

    btnCamera.setOnClickListener(
        v -> {
          if (checkCameraPermission()) {
            openCamera();
          } else {
            requestCameraPermission();
          }
        });

    btnRecord.setOnClickListener(
        v -> {
          if (checkAudioPermission()) {
            if (!isRecording) {
              File audioDirectory = new File(getFilesDir(), "audio");
              if (!audioDirectory.exists() && !audioDirectory.mkdirs()) {
                Toast.makeText(this, "Could not create audio directory", Toast.LENGTH_SHORT).show();
                return;
              }
              String newAudioPath =
                  new File(audioDirectory, "voice_" + System.currentTimeMillis() + ".3gp")
                      .getAbsolutePath();
              if (startRecording(newAudioPath)) {
                if (!currentAudioPath.equals(originalAudioPath)) {
                  deleteAudioFile(currentAudioPath);
                }
                currentAudioPath = newAudioPath;
                btnRecord.setText("⏹️ STOP");
                btnRecord.setBackgroundColor(Color.RED);
                tvAudioStatus.setText("Recording...");
                btnPlay.setVisibility(View.GONE);
                isRecording = true;
              }
            } else {
              stopRecording();
              btnRecord.setText("🎤 RE-RECORD");
              btnRecord.setBackgroundColor(Color.parseColor("#E91E63"));
              tvAudioStatus.setText("Audio Saved!");
              btnPlay.setVisibility(View.VISIBLE);
              isRecording = false;
            }
          } else {
            requestAudioPermission();
          }
        });

    btnPlay.setOnClickListener(v -> playAudio(currentAudioPath));

    // --- SAVE / UPDATE ---
    btnSave.setOnClickListener(
        v -> {
          String name = etName.getText().toString().trim();
          String comment = etComment.getText().toString().trim();
          String time = etTime.getText().toString().trim();
          if (time.isEmpty()) {

            time = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date());
          }

          if (name.isEmpty()) {
            Toast.makeText(this, "Name is required!", Toast.LENGTH_SHORT).show();
            return;
          }

          if (obsToEdit == null) {
            dbHelper.addObservation(hikeId, name, time, comment, encodedImage, currentAudioPath);
            Toast.makeText(this, "Observation Added!", Toast.LENGTH_SHORT).show();
          } else {
            dbHelper.updateObservation(
                obsToEdit.getId(), name, time, comment, encodedImage, currentAudioPath);
            if (!originalAudioPath.equals(currentAudioPath)) {
              deleteAudioFile(originalAudioPath);
            }
            Toast.makeText(this, "Observation Updated!", Toast.LENGTH_SHORT).show();
          }

          loadObservations();
          observationSaved[0] = true;
          currentDialog.dismiss();
        });

    if (btnCancel != null) {
      btnCancel.setOnClickListener(v -> currentDialog.dismiss());
    }

    currentDialog.setOnDismissListener(
        d -> {
          if (isRecording) {
            stopRecording();
            isRecording = false;
          }
          if (!observationSaved[0] && !originalAudioPath.equals(currentAudioPath)) {
            deleteAudioFile(currentAudioPath);
          }
          currentDialog = null;
          ivPreview = null;
        });

    currentDialog.show();
  }

  // ================================================================

  // ================================================================
  private boolean checkCameraPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestCameraPermission() {
    ActivityCompat.requestPermissions(
        this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
  }

  private void openCamera() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    } else {
      Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
    }
  }

  // ================================================================

  // ================================================================
  private boolean checkAudioPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void requestAudioPermission() {
    ActivityCompat.requestPermissions(
        this, new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
  }

  // ================================================================

  // ================================================================
  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        openCamera();
      } else {
        Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
      }
    } else if (requestCode == REQUEST_AUDIO_PERMISSION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show();
      }
    }
  }

  // ================================================================

  // ================================================================
  private boolean startRecording(String path) {
    try {
      mediaRecorder = new MediaRecorder();
      mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mediaRecorder.setOutputFile(path);
      mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mediaRecorder.prepare();
      mediaRecorder.start();
      return true;
    } catch (Exception e) {
      if (mediaRecorder != null) {
        mediaRecorder.release();
        mediaRecorder = null;
      }
      deleteAudioFile(path);
      Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      return false;
    }
  }

  private void stopRecording() {
    if (mediaRecorder != null) {
      try {
        mediaRecorder.stop();
      } catch (Exception exception) {
        Log.w(TAG, "Recorder stopped before audio data was available", exception);
      }
      mediaRecorder.release();
      mediaRecorder = null;
    }
  }

  private void playAudio(String path) {
    if (path == null || path.isEmpty()) {
      Toast.makeText(this, "No Audio File", Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      if (mediaPlayer != null) mediaPlayer.release();
      mediaPlayer = new MediaPlayer();

      android.media.AudioManager am =
          (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
      am.setMode(android.media.AudioManager.MODE_NORMAL);
      am.setSpeakerphoneOn(true);

      mediaPlayer.setDataSource(path);
      mediaPlayer.prepare();
      mediaPlayer.start();

      mediaPlayer.setOnCompletionListener(
          mp -> {
            Toast.makeText(this, "Playback finished", Toast.LENGTH_SHORT).show();
          });
    } catch (IOException exception) {
      Log.e(TAG, "Audio playback failed", exception);
      Toast.makeText(this, "Playback failed", Toast.LENGTH_SHORT).show();
    }
  }

  // ================================================================

  // ================================================================
  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      if (data != null && data.getExtras() != null) {
        Bundle extras = data.getExtras();
        Bitmap imageBitmap = (Bitmap) extras.get("data");

        if (imageBitmap != null) {

          if (ivPreview != null) {
            ivPreview.setImageBitmap(imageBitmap);
            ivPreview.setVisibility(View.VISIBLE);
          }

          // Encode to Base64
          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
          byte[] byteArray = byteArrayOutputStream.toByteArray();
          encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

          Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  // ================================================================

  // ================================================================
  private void loadObservations() {
    observationList.clear();
    Cursor cursor = dbHelper.getObservations(hikeId);

    if (cursor.moveToFirst()) {
      do {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_OBS_ID));
        String name =
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_OBSERVATION));
        String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TIME));
        String comment =
            cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COMMENTS));
        String image = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_IMAGE));

        String audio = "";
        int audioIndex = cursor.getColumnIndex(DatabaseHelper.COL_AUDIO);
        if (audioIndex != -1) audio = cursor.getString(audioIndex);

        int hId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_OBS_HIKE_ID));

        Observation obs = new Observation(id, name, time, comment, hId, image, audio);
        observationList.add(obs);
      } while (cursor.moveToNext());
    }
    cursor.close();
    adapter.notifyDataSetChanged();
  }

  private void deleteAudioFile(String path) {
    if (path == null || path.trim().isEmpty()) {
      return;
    }
    File audioFile = new File(path);
    File audioDirectory = new File(getFilesDir(), "audio");
    if (audioFile.getParentFile() != null
        && audioFile.getParentFile().equals(audioDirectory)
        && audioFile.exists()) {
      // Best-effort cleanup. Database operation remains source of truth.
      audioFile.delete();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
    if (mediaRecorder != null) {
      stopRecording();
    }
    dbHelper.close();
  }
}
