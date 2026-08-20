package com.quyncgcs230483.mhike;

public final class Observation {
  private final int id;
  private final String observation;
  private final String time;
  private final String comments;
  private final int hikeId;
  private final String imagePath;
  private final String audioPath;

  public Observation(
      int id,
      String observation,
      String time,
      String comments,
      int hikeId,
      String imagePath,
      String audioPath) {
    this.id = id;
    this.observation = observation;
    this.time = time;
    this.comments = comments;
    this.hikeId = hikeId;
    this.imagePath = imagePath;
    this.audioPath = audioPath;
  }

  public int getId() {
    return id;
  }

  public String getObservation() {
    return observation;
  }

  public String getTime() {
    return time;
  }

  public String getComments() {
    return comments;
  }

  public int getHikeId() {
    return hikeId;
  }

  public String getImagePath() {
    return imagePath;
  }

  public String getAudioPath() {
    return audioPath;
  }
}
