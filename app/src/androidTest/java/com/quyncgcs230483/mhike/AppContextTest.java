package com.quyncgcs230483.mhike;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppContextTest {

  @Test
  public void applicationUsesPortfolioPackageName() {
    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    assertEquals("com.quyncgcs230483.mhike", appContext.getPackageName());
  }
}
