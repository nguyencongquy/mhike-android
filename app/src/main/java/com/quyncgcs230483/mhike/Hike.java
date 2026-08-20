package com.quyncgcs230483.mhike;

public final class Hike {

  private final int id;
  private final String name;
  private final String location;
  private final String date;
  private final String parking;
  private final String length;
  private final String difficulty;
  private final String description;
  private final String gear;
  private final String trail;
  private final double latitude;
  private final double longitude;

  public Hike(
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
    this.id = id;
    this.name = name;
    this.location = location;
    this.date = date;
    this.parking = parking;
    this.length = length;
    this.difficulty = difficulty;
    this.description = description;
    this.gear = gear;
    this.trail = trail;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLocation() {
    return location;
  }

  public String getDate() {
    return date;
  }

  public String getParking() {
    return parking;
  }

  public String getLength() {
    return length;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public String getDescription() {
    return description;
  }

  public String getGear() {
    return gear;
  }

  public String getTrail() {
    return trail;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }
}
