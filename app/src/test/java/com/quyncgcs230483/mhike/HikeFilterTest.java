package com.quyncgcs230483.mhike;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class HikeFilterTest {

  private final Hike snowdon =
      new Hike(
          1,
          "Snowdon Sunrise",
          "Wales",
          "12/07/2026",
          "Yes",
          "11.4",
          "High",
          "",
          "",
          "Rocky",
          53.0685,
          -4.0763);
  private final Hike langBiang =
      new Hike(
          2,
          "Lang Biang",
          "Da Lat",
          "20/08/2026",
          "No",
          "8.0",
          "Medium",
          "",
          "",
          "Forest",
          12.0500,
          108.4400);

  @Test
  public void filterCombinesCriteriaWithAndSemantics() {
    List<Hike> result =
        HikeFilter.filter(Arrays.asList(snowdon, langBiang), "snow", "wal", "12/07", "11");

    assertEquals(Collections.singletonList(snowdon), result);
  }

  @Test
  public void filterIsCaseInsensitive() {
    List<Hike> result =
        HikeFilter.filter(Arrays.asList(snowdon, langBiang), "LANG BIANG", "DA LAT", "", "");

    assertEquals(Collections.singletonList(langBiang), result);
  }

  @Test
  public void filterHandlesNullAndEmptyInputs() {
    assertTrue(HikeFilter.filter(null, null, null, null, null).isEmpty());
    assertEquals(
        Arrays.asList(snowdon, langBiang),
        HikeFilter.filter(Arrays.asList(snowdon, langBiang), "", "", "", ""));
  }
}
