package com.quyncgcs230483.mhike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure filtering logic shared by UI and local unit tests. */
public final class HikeFilter {

  private HikeFilter() {}

  public static List<Hike> filter(
      List<Hike> hikes, String name, String location, String date, String length) {
    if (hikes == null || hikes.isEmpty()) {
      return Collections.emptyList();
    }

    String expectedName = normalize(name);
    String expectedLocation = normalize(location);
    String expectedDate = safe(date).trim();
    String expectedLength = safe(length).trim();
    List<Hike> matches = new ArrayList<>();

    for (Hike hike : hikes) {
      if (hike == null) {
        continue;
      }
      if (!expectedName.isEmpty() && !normalize(hike.getName()).contains(expectedName)) {
        continue;
      }
      if (!expectedLocation.isEmpty()
          && !normalize(hike.getLocation()).contains(expectedLocation)) {
        continue;
      }
      if (!expectedDate.isEmpty() && !safe(hike.getDate()).contains(expectedDate)) {
        continue;
      }
      if (!expectedLength.isEmpty() && !safe(hike.getLength()).contains(expectedLength)) {
        continue;
      }
      matches.add(hike);
    }
    return matches;
  }

  private static String normalize(String value) {
    return safe(value).trim().toLowerCase(Locale.ROOT);
  }

  private static String safe(String value) {
    return value == null ? "" : value;
  }
}
