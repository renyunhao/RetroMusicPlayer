/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.model.lyrics;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public abstract class AbsSynchronizedLyrics extends Lyrics {

  private static final int TIME_OFFSET_MS =
      500; // time adjustment to display line before it actually starts

  protected final SparseArray<String> lines = new SparseArray<>();

  protected int offset = 0;

  public String getLine(int time) {
    time += offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;

    int lastLineTime = lines.keyAt(0);

    for (int i = 0; i < lines.size(); i++) {
      int lineTime = lines.keyAt(i);

      if (time >= lineTime) {
        lastLineTime = lineTime;
      } else {
        break;
      }
    }

    return lines.get(lastLineTime);
  }

  public String getPreviousLine(int time) {
    time += offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;

    int prevLineTime = -1;
    int currentLineTime = -1;

    for (int i = 0; i < lines.size(); i++) {
      int lineTime = lines.keyAt(i);

      if (time >= lineTime) {
        prevLineTime = currentLineTime;
        currentLineTime = lineTime;
      } else {
        break;
      }
    }

    return prevLineTime >= 0 ? lines.get(prevLineTime) : "";
  }

  public String getNextLine(int time) {
    time += offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;

    for (int i = 0; i < lines.size(); i++) {
      int lineTime = lines.keyAt(i);

      if (time < lineTime) {
        return lines.get(lineTime);
      }
    }

    return "";
  }

  public List<String> getNextLines(int time, int count) {
    time += offset + AbsSynchronizedLyrics.TIME_OFFSET_MS;
    List<String> result = new ArrayList<>();

    int currentLineIndex = -1;
    for (int i = 0; i < lines.size(); i++) {
      int lineTime = lines.keyAt(i);
      if (time >= lineTime) {
        currentLineIndex = i;
      } else {
        break;
      }
    }

    for (int i = 1; i <= count; i++) {
      int nextIndex = currentLineIndex + i;
      if (nextIndex < lines.size()) {
        result.add(lines.valueAt(nextIndex));
      } else {
        result.add("");
      }
    }

    return result;
  }

  @Override
  public String getText() {
    parse(false);

    if (valid) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < lines.size(); i++) {
        String line = lines.valueAt(i);
        sb.append(line).append("\r\n");
      }

      return sb.toString().trim().replaceAll("(\r?\n){3,}", "\r\n\r\n");
    }

    return super.getText();
  }

  public boolean isSynchronized() {
    return true;
  }

  public boolean isValid() {
    parse(true);
    return valid;
  }
}
