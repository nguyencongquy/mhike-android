package com.quyncgcs230483.mhike;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Owns local schema and persistence operations used by app. */
public final class DatabaseHelper extends SQLiteOpenHelper {

  // Keep legacy filename so existing coursework installations retain their data.
  public static final String DATABASE_NAME = "M_Hike.db";
  public static final int DATABASE_VERSION = 6;

  public static final String TABLE_HIKES = "hikes";
  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_LOCATION = "location";
  public static final String COL_DATE = "date";
  public static final String COL_PARKING = "parking";
  public static final String COL_LENGTH = "length";
  public static final String COL_DIFFICULTY = "difficulty";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_GEAR = "gear";
  public static final String COL_TRAIL = "trail";
  public static final String COL_LATITUDE = "latitude";
  public static final String COL_LONGITUDE = "longitude";

  public static final String TABLE_OBSERVATIONS = "observations";
  public static final String COL_OBS_ID = "id";
  public static final String COL_OBS_HIKE_ID = "hike_id";
  public static final String COL_OBSERVATION = "observation";
  public static final String COL_TIME = "time";
  public static final String COL_COMMENTS = "comments";
  public static final String COL_IMAGE = "image";
  public static final String COL_AUDIO = "audio";

  public static final String TABLE_TRACK_POINTS = "track_points";
  public static final String COL_TP_ID = "id";
  public static final String COL_TP_HIKE_ID = "hike_id";
  public static final String COL_TP_LAT = "latitude";
  public static final String COL_TP_LON = "longitude";
  public static final String COL_TP_TIME = "timestamp";

  private static final String CREATE_TABLE_HIKES =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_HIKES
          + " ("
          + COL_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + COL_NAME
          + " TEXT NOT NULL, "
          + COL_LOCATION
          + " TEXT NOT NULL, "
          + COL_DATE
          + " TEXT NOT NULL, "
          + COL_PARKING
          + " TEXT NOT NULL, "
          + COL_LENGTH
          + " TEXT NOT NULL, "
          + COL_DIFFICULTY
          + " TEXT NOT NULL, "
          + COL_DESCRIPTION
          + " TEXT NOT NULL DEFAULT '', "
          + COL_GEAR
          + " TEXT NOT NULL DEFAULT '', "
          + COL_TRAIL
          + " TEXT NOT NULL DEFAULT '', "
          + COL_LATITUDE
          + " REAL NOT NULL DEFAULT 0, "
          + COL_LONGITUDE
          + " REAL NOT NULL DEFAULT 0)";

  private static final String CREATE_TABLE_OBSERVATIONS =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_OBSERVATIONS
          + " ("
          + COL_OBS_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + COL_OBS_HIKE_ID
          + " INTEGER NOT NULL, "
          + COL_OBSERVATION
          + " TEXT NOT NULL, "
          + COL_TIME
          + " TEXT NOT NULL, "
          + COL_COMMENTS
          + " TEXT NOT NULL DEFAULT '', "
          + COL_IMAGE
          + " TEXT NOT NULL DEFAULT '', "
          + COL_AUDIO
          + " TEXT NOT NULL DEFAULT '', "
          + "FOREIGN KEY ("
          + COL_OBS_HIKE_ID
          + ") REFERENCES "
          + TABLE_HIKES
          + "("
          + COL_ID
          + ") ON DELETE CASCADE)";

  private static final String CREATE_TABLE_TRACK_POINTS =
      "CREATE TABLE IF NOT EXISTS "
          + TABLE_TRACK_POINTS
          + " ("
          + COL_TP_ID
          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
          + COL_TP_HIKE_ID
          + " INTEGER NOT NULL, "
          + COL_TP_LAT
          + " REAL NOT NULL, "
          + COL_TP_LON
          + " REAL NOT NULL, "
          + COL_TP_TIME
          + " INTEGER NOT NULL, "
          + "FOREIGN KEY ("
          + COL_TP_HIKE_ID
          + ") REFERENCES "
          + TABLE_HIKES
          + "("
          + COL_ID
          + ") ON DELETE CASCADE)";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    db.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(CREATE_TABLE_HIKES);
    db.execSQL(CREATE_TABLE_OBSERVATIONS);
    db.execSQL(CREATE_TABLE_TRACK_POINTS);
    createIndexes(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // Preserve coursework data from earlier schema versions.
    db.execSQL(CREATE_TABLE_HIKES);
    addColumnIfMissing(db, TABLE_HIKES, COL_LATITUDE, "REAL NOT NULL DEFAULT 0");
    addColumnIfMissing(db, TABLE_HIKES, COL_LONGITUDE, "REAL NOT NULL DEFAULT 0");

    db.execSQL(CREATE_TABLE_OBSERVATIONS);
    addColumnIfMissing(db, TABLE_OBSERVATIONS, COL_AUDIO, "TEXT NOT NULL DEFAULT ''");

    db.execSQL(CREATE_TABLE_TRACK_POINTS);
    createIndexes(db);
  }

  private static void createIndexes(SQLiteDatabase db) {
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_observations_hike_id ON "
            + TABLE_OBSERVATIONS
            + "("
            + COL_OBS_HIKE_ID
            + ")");
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_track_points_hike_time ON "
            + TABLE_TRACK_POINTS
            + "("
            + COL_TP_HIKE_ID
            + ", "
            + COL_TP_TIME
            + ")");
  }

  private static void addColumnIfMissing(
      SQLiteDatabase db, String table, String column, String definition) {
    try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      while (cursor.moveToNext()) {
        if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
          return;
        }
      }
    }
    db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
  }

  public long insertHike(
      String name,
      String location,
      String date,
      String parking,
      String length,
      String difficulty,
      String description,
      String gear,
      String trail,
      double latitude,
      double longitude) {
    return getWritableDatabase()
        .insertOrThrow(
            TABLE_HIKES,
            null,
            hikeValues(
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
                longitude));
  }

  public Cursor getHikes() {
    return getReadableDatabase().query(TABLE_HIKES, null, null, null, null, null, COL_ID + " DESC");
  }

  public int updateHike(
      int id,
      String name,
      String location,
      String date,
      String parking,
      String length,
      String difficulty,
      String description,
      String gear,
      String trail,
      double latitude,
      double longitude) {
    return getWritableDatabase()
        .update(
            TABLE_HIKES,
            hikeValues(
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
                longitude),
            COL_ID + " = ?",
            new String[] {String.valueOf(id)});
  }

  private static ContentValues hikeValues(
      String name,
      String location,
      String date,
      String parking,
      String length,
      String difficulty,
      String description,
      String gear,
      String trail,
      double latitude,
      double longitude) {
    ContentValues values = new ContentValues();
    values.put(COL_NAME, name);
    values.put(COL_LOCATION, location);
    values.put(COL_DATE, date);
    values.put(COL_PARKING, parking);
    values.put(COL_LENGTH, length);
    values.put(COL_DIFFICULTY, difficulty);
    values.put(COL_DESCRIPTION, description);
    values.put(COL_GEAR, gear);
    values.put(COL_TRAIL, trail);
    values.put(COL_LATITUDE, latitude);
    values.put(COL_LONGITUDE, longitude);
    return values;
  }

  public void deleteHike(int id) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      String[] args = {String.valueOf(id)};
      // Explicit child deletion supports databases created before cascades were enabled.
      db.delete(TABLE_OBSERVATIONS, COL_OBS_HIKE_ID + " = ?", args);
      db.delete(TABLE_TRACK_POINTS, COL_TP_HIKE_ID + " = ?", args);
      db.delete(TABLE_HIKES, COL_ID + " = ?", args);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public void deleteAllHikes() {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete(TABLE_OBSERVATIONS, null, null);
      db.delete(TABLE_TRACK_POINTS, null, null);
      db.delete(TABLE_HIKES, null, null);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  public long addObservation(
      int hikeId, String name, String time, String comments, String image, String audioPath) {
    ContentValues values = observationValues(name, time, comments, image, audioPath);
    values.put(COL_OBS_HIKE_ID, hikeId);
    return getWritableDatabase().insertOrThrow(TABLE_OBSERVATIONS, null, values);
  }

  public Cursor getObservations(int hikeId) {
    return getReadableDatabase()
        .query(
            TABLE_OBSERVATIONS,
            null,
            COL_OBS_HIKE_ID + " = ?",
            new String[] {String.valueOf(hikeId)},
            null,
            null,
            COL_OBS_ID + " DESC");
  }

  public int deleteObservation(int id) {
    return getWritableDatabase()
        .delete(TABLE_OBSERVATIONS, COL_OBS_ID + " = ?", new String[] {String.valueOf(id)});
  }

  public int updateObservation(
      int id, String name, String time, String comments, String image, String audioPath) {
    return getWritableDatabase()
        .update(
            TABLE_OBSERVATIONS,
            observationValues(name, time, comments, image, audioPath),
            COL_OBS_ID + " = ?",
            new String[] {String.valueOf(id)});
  }

  private static ContentValues observationValues(
      String name, String time, String comments, String image, String audioPath) {
    ContentValues values = new ContentValues();
    values.put(COL_OBSERVATION, name);
    values.put(COL_TIME, time);
    values.put(COL_COMMENTS, comments);
    values.put(COL_IMAGE, image == null ? "" : image);
    values.put(COL_AUDIO, audioPath == null ? "" : audioPath);
    return values;
  }

  public long insertTrackPoint(int hikeId, double latitude, double longitude) {
    ContentValues values = new ContentValues();
    values.put(COL_TP_HIKE_ID, hikeId);
    values.put(COL_TP_LAT, latitude);
    values.put(COL_TP_LON, longitude);
    values.put(COL_TP_TIME, System.currentTimeMillis());
    return getWritableDatabase().insertOrThrow(TABLE_TRACK_POINTS, null, values);
  }

  public Cursor getTrackPoints(int hikeId) {
    return getReadableDatabase()
        .query(
            TABLE_TRACK_POINTS,
            null,
            COL_TP_HIKE_ID + " = ?",
            new String[] {String.valueOf(hikeId)},
            null,
            null,
            COL_TP_TIME + " ASC");
  }

  public int deleteTrackPoints(int hikeId) {
    return getWritableDatabase()
        .delete(TABLE_TRACK_POINTS, COL_TP_HIKE_ID + " = ?", new String[] {String.valueOf(hikeId)});
  }
}
