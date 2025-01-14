/*
 * The MIT License
 * Copyright © 2022 Hiram K
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.idelstak.matrixrain.intro.title;

import java.awt.Point;

import com.github.idelstak.matrixrain.auxiliary.graphics.IndexBitmapObject;
import com.github.idelstak.matrixrain.auxiliary.math.intersect.Circle1PixelArbitraryIntersectorFactory;
import com.github.idelstak.matrixrain.phosphore.PhosphoreCloudFactory;
import com.github.idelstak.matrixrain.phosphore.Phosphorizer;
import com.github.idelstak.matrixrain.position.TitleConnectorPosition;
import com.github.idelstak.matrixrain.position.TitleGlyphPosition;

public class TitleZoomManager {
  private class TitleFrameElement {
    public IndexBitmapObject titleBitmap;

    public int iterationsToLive;

    public int currIteration;

    public boolean toAccelerateDrops;

    public TitleFrameElement next;

    public TitleFrameElement prev;

    public TitleFrameElement(
        IndexBitmapObject titleBitmap, int iterationsToLive, boolean toAccelerateDrops) {

      this.titleBitmap = titleBitmap;
      this.iterationsToLive = iterationsToLive;
      this.currIteration = iterationsToLive;
      this.toAccelerateDrops = toAccelerateDrops;
      this.next = null;
      this.prev = null;
    }
  }

  private class TitleFrameList {
    public TitleFrameElement head;

    public TitleFrameElement tail;

    public TitleFrameElement curr;

    public boolean toAccelerateDrops;

    public int length;

    public TitleFrameList() {
      head = null;
      tail = null;
      curr = null;
      length = 0;
      this.toAccelerateDrops = false;
    }

    public synchronized void addTitleFrameAtTail(
        IndexBitmapObject titleBitmap, int iterationsToLive, boolean toAccelerateDrops) {

      TitleFrameElement newElement =
          new TitleFrameElement(titleBitmap, iterationsToLive, toAccelerateDrops);
      if (this.tail == null) {
        // empty list
        this.head = newElement;
        this.tail = newElement;
      } else {
        this.tail.next = newElement;
        newElement.prev = this.tail;
        this.tail = newElement;
      }
      length++;
    }

    public synchronized void removeTitleFrame(TitleFrameElement element) {
      // update tail pointer
      if (element == this.tail) {
        this.tail = element.prev;
      }

      if (element == this.head) {
        this.head = element.next;
        if (this.head != null) this.head.prev = null;
      } else {
        if (element.next != null) element.next.prev = element.prev;
        if (element.prev != null) element.prev.next = element.next;
      }
      length--;
    }

    public synchronized void setCursorAtFirstFrame() {
      this.curr = this.head;
    }

    public synchronized void setCursorAtNextFrame() {
      if (this.curr != null) {
        this.toAccelerateDrops = false;
        this.curr.currIteration--;
        if (this.curr.currIteration <= 0) {
          this.curr = this.curr.next;
          if (this.curr != null) this.toAccelerateDrops = this.curr.toAccelerateDrops;
        }
      }
    }

    public synchronized boolean isCursorAfterLastFrame() {
      return (this.curr == null);
    }
  }

  private int windowWidth;

  private int windowHeight;

  private int glyphCount;

  private Point[] originalGlyphPositions;

  private int connectorCount;

  private Point[] originalConnectorPositions;

  private IndexBitmapObject startBitmapObject;

  private PhosphoreCloudFactory pcFct;

  private Circle1PixelArbitraryIntersectorFactory iFct;

  private TitleFrameList titleFrameList;

  public TitleZoomManager(
      PhosphoreCloudFactory pcFct, Circle1PixelArbitraryIntersectorFactory iFct) {
    this.titleFrameList = new TitleFrameList();
    this.pcFct = pcFct;
    this.iFct = iFct;
  }

  public void setWindowWidth(int value) {
    this.windowWidth = value;
  }

  public void setWindowHeight(int value) {
    this.windowHeight = value;
  }

  public void setGlyphCount(int value) {
    this.glyphCount = value;
  }

  public int getGlyphCount() {
    return this.glyphCount;
  }

  public void setOriginalGlyphPositions(TitleGlyphPosition[] values) {
    if (values == null) {
      this.originalGlyphPositions = null;
      return;
    }

    int length = values.length;
    if (length == 0) {
      this.originalGlyphPositions = null;
      return;
    }

    this.originalGlyphPositions = new Point[length];
    for (int i = 0; i < length; i++)
      this.originalGlyphPositions[i] = new Point(values[i].getX(), values[i].getEndY());
  }

  public void setConnectorCount(int value) {
    this.connectorCount = value;
  }

  public int getConnectorCount() {
    return this.connectorCount;
  }

  public void setOriginalConnectorPositions(TitleConnectorPosition[] values) {
    if (values == null) {
      this.originalConnectorPositions = null;
      return;
    }

    int length = values.length;
    if (length == 0) {
      this.originalConnectorPositions = null;
      return;
    }

    this.originalConnectorPositions = new Point[length];
    for (int i = 0; i < length; i++)
      this.originalConnectorPositions[i] = new Point(values[i].getPosition());
  }

  public void setFirstBitmapObject(IndexBitmapObject startBitmapObject) {
    this.startBitmapObject = startBitmapObject;
  }

  private IndexBitmapObject createNextBitmap(double factor) {
    // System.out.print("Creating bitmap with factor " + factor + "... ");

    this.pcFct.setCurrFactor(factor);

    IndexBitmapObject toPhosphorize = new IndexBitmapObject();
    int tpWidth = (int) (this.startBitmapObject.getWidth() / factor) + 4;
    if (tpWidth > this.windowWidth) tpWidth = this.windowWidth;
    int tpHeight = (int) (this.startBitmapObject.getHeight() / factor) + 4;
    if (tpHeight > this.windowHeight) tpHeight = this.windowHeight;

    int startCol = (this.startBitmapObject.getWidth() - tpWidth) / 2;
    int startRow = (this.startBitmapObject.getHeight() - tpHeight) / 2;

    int[][] startPixels = this.startBitmapObject.getBitmap();
    int[][] tpPixels = new int[tpWidth][tpHeight];
    for (int i = 0; i < tpWidth; i++)
      for (int j = 0; j < tpHeight; j++) tpPixels[i][j] = startPixels[i + startCol][j + startRow];
    toPhosphorize.setWidth(tpWidth);
    toPhosphorize.setHeight(tpHeight);
    toPhosphorize.setBitmap(tpPixels);

    IndexBitmapObject newBitmap =
        Phosphorizer.createScaledUpPhosphoreVersion(
            toPhosphorize, this.pcFct, this.iFct, factor, true);

    // System.out.println("time: " + (time1-time0) + ", create: " + delta);
    return newBitmap;
  }

  public void createAllFrames() {
    IndexBitmapObject firstFrame = this.createNextBitmap(1.0);
    this.titleFrameList.addTitleFrameAtTail(firstFrame, 2, true);

    for (double factor = 1.05; factor < 1.5; factor += 0.05) {
      IndexBitmapObject nextBitmap = this.createNextBitmap(factor);
      this.titleFrameList.addTitleFrameAtTail(nextBitmap, 2, false);
    }

    for (double factor = 1.5; factor <= 10.0; factor += 1.5) {
      IndexBitmapObject nextBitmap = this.createNextBitmap(factor);
      this.titleFrameList.addTitleFrameAtTail(nextBitmap, 1, true);
    }
  }

  public IndexBitmapObject getCurrentBitmap() {
    if (this.titleFrameList.curr == null) return null;

    return this.titleFrameList.curr.titleBitmap;
  }

  public boolean getToAccelerateDrops() {
    if (this.titleFrameList.curr == null) return false;

    return this.titleFrameList.toAccelerateDrops;
  }

  public synchronized void setCurrentAtFirstFrame() {
    this.titleFrameList.setCursorAtFirstFrame();
  }

  public synchronized void setCurrentAtNextFrame() {
    this.titleFrameList.setCursorAtNextFrame();
  }

  public synchronized boolean isCurrentAfterLastFrame() {
    return this.titleFrameList.isCursorAfterLastFrame();
  }
}
