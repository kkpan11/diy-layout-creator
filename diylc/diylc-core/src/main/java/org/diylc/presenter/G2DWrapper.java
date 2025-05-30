/*
 * 
 * DIY Layout Creator (DIYLC). Copyright (c) 2009-2025 held jointly by the individual authors.
 * 
 * This file is part of DIYLC.
 * 
 * DIYLC is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * DIYLC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with DIYLC. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.diylc.presenter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.diylc.common.ObjectCache;
import org.diylc.common.ZoomableStroke;
import org.diylc.core.IDrawingObserver;

/**
 * {@link Graphics2D} wrapper that keeps track of all drawing actions and creates an {@link Area}
 * that corresponds to drawn objects. Before each component is drawn,
 * {@link #startedDrawingComponent()} should be called. After the component is drawn, area may be
 * retrieved using {@link #finishedDrawingComponent()}. Graphics configuration (color, font, etc) is
 * reset between each two components.
 * 
 * @author Branislav Stojkovic
 */
class G2DWrapper extends Graphics2D implements IDrawingObserver {

  public static int LINE_SENSITIVITY_MARGIN = 2;
  public static int CURVE_SENSITIVITY = 6;

  private static final Logger LOG = Logger.getLogger(G2DWrapper.class);

  public static String DEBUG_AREA_PERFORMANCE = "org.diylc.debugAreaPerformance";

  private boolean drawingComponent = false;
  private final boolean debugAreaPerformance;
  private boolean trackingAllowed = true;
  private boolean trackingContinuityAllowed = false;
  private boolean trackingContinuityPositive = true;
  
  private HashMap<String, Integer> trackingStacks = new HashMap<String, Integer>();
  
  private int uniqueCounter = 0;

  private Graphics2D canvasGraphics;
  private Stroke originalStroke;
  private Color originalColor;
  private Composite originalComposite;
  private AffineTransform originalTx;
  private Font originalFont;
  private AffineTransform currentTx;
  private AffineTransform initialTx;
  private Area currentArea;
  private Map<String, Area> continuityPositiveAreas;
  private Map<String, Area> continuityNegativeAreas;
  private String continuityMarker;
  private Shape lastShape;

  private double zoom;
  private int zOrder;  

  /**
   * Creates a wrapper around specified {@link Graphics2D} object.
   * 
   * @param canvasGraphics
   */
  public G2DWrapper(Graphics2D canvasGraphics, double zoom) {
    super();
    this.canvasGraphics = canvasGraphics;
    String debugAreaPerformanceStr = System.getProperty(DEBUG_AREA_PERFORMANCE);
    debugAreaPerformance = "true".equalsIgnoreCase(debugAreaPerformanceStr);
    this.zoom = zoom;
    currentArea = new Area();
    continuityPositiveAreas = new HashMap<String, Area>();
    continuityNegativeAreas = new HashMap<String, Area>();
    continuityMarker = null;
    currentTx = new AffineTransform();
  }
  
  public Graphics2D getCanvasGraphics() {
    return canvasGraphics;
  }

  /**
   * Clears out the current area and caches canvas settings.
   * 
   * @param zOrder
   */
  public void startedDrawingComponent(int zOrder) {
    this.zOrder = zOrder;
    this.drawingComponent = true;
    this.currentArea = new Area();
    this.continuityPositiveAreas = new HashMap<String, Area>();
    this.continuityNegativeAreas = new HashMap<String, Area>();
//    trackingStacks.clear();
    this.continuityMarker = null;
    this.originalStroke = canvasGraphics.getStroke();
    this.originalColor = canvasGraphics.getColor();
    this.originalTx = canvasGraphics.getTransform();
    this.originalComposite = canvasGraphics.getComposite();
    this.originalFont = canvasGraphics.getFont();
    this.currentTx = new AffineTransform();
    this.initialTx = canvasGraphics.getTransform();
    this.lastShape = null;
    startTracking();
  }

  /**
   * Reverts {@link Graphics2D} settings and returns area drawn by component in the meantime.
   * 
   * @return
   */
  public ComponentArea finishedDrawingComponent() {
    drawingComponent = false;
    canvasGraphics.setStroke(originalStroke);
    canvasGraphics.setColor(originalColor);
    canvasGraphics.setTransform(originalTx);
    canvasGraphics.setComposite(originalComposite);
    canvasGraphics.setFont(originalFont);
//    Map<String, Integer> sorted = sortByComparator(trackingStacks, false);    
    return new ComponentArea(zOrder,currentArea, new ArrayList<Area>(continuityPositiveAreas.values()), 
        new ArrayList<Area>(continuityNegativeAreas.values()));
  }
  
  // start - used for caching
  
  public Area getCurrentArea() {
    return currentArea;
  }
  
  public Map<String, Area> getContinuityPositiveAreas() {
    return continuityPositiveAreas;
  }
  
  public Map<String, Area> getContinuityNegativeAreas() {
    return continuityNegativeAreas;
  }
  
  // end - used for caching
  
  public void merge(Area currentArea, Map<String, Area> continuityPositiveAreas,  Map<String, Area> continuityNegativeAreas) {
    this.currentArea = currentArea;
    this.continuityPositiveAreas = continuityPositiveAreas;
    this.continuityNegativeAreas = continuityNegativeAreas;
  }

  @Override
  public void startTracking() {
    this.trackingAllowed = true;
  }

  @Override
  public void stopTracking() {
    this.trackingAllowed = false;
  }

  @Override
  public void startTrackingContinuityArea(boolean positive) {
    this.trackingContinuityAllowed = true;
    this.trackingContinuityPositive = positive;
  }

  @Override
  public void stopTrackingContinuityArea() {
    this.trackingContinuityAllowed = false;
  }

  @Override
  public boolean isTrackingContinuityArea() {   
    return trackingContinuityAllowed;
  }  
  
  @Override
  public void setContinuityMarker(String marker) {
    continuityMarker = marker;    
  }    

  /**
   * Appends shape interior to the current component area.
   * 
   * @param s
   */
  private void appendShape(Shape s) {
    if (!drawingComponent || (!trackingAllowed && !trackingContinuityAllowed)) {
      return;
    }
    Rectangle2D bounds = s.getBounds2D();
    // Only cache the shape if it's not 1D.
    if (bounds.getWidth() > 1 && bounds.getHeight() > 1) {
      Area area = new Area(s);
      area.transform(currentTx);

      Area areaBefore = null;
      if (debugAreaPerformance) {
        areaBefore = new Area(currentArea);
      }

      if (trackingAllowed) {
        currentArea.add(area);

        if (debugAreaPerformance && currentArea.equals(areaBefore)) {
          LOG.warn("Detected unnecessary shape tracking at", new Throwable());
        }
      }

      Map<String, Area> toAdd;
      if (trackingContinuityAllowed) {
        if (trackingContinuityPositive)
          toAdd = continuityPositiveAreas;
        else
          toAdd = continuityNegativeAreas;
        String marker = continuityMarker == null ? ("internalUnique" + uniqueCounter++) : continuityMarker;
        if (continuityMarker == null) {
          toAdd.put(marker, area);
//          markStack();
        } else {
          Area current = toAdd.getOrDefault(marker, null); 
          if (current == null) {            
            toAdd.put(marker, area);
//            markStack();          
          } else
            current.add(area);
        }        
      }
      lastShape = s;
    }
  }
  
  @SuppressWarnings("unused")
  private void markStack() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new Throwable().printStackTrace(pw);
    String stack = sw.toString();
    Integer currentCount = trackingStacks.getOrDefault(stack, 0);
    currentCount++;
    trackingStacks.put(stack, currentCount);
  }

  /**
   * Appends shape outline to the current component area.
   * 
   * @param s
   */
  private void appendShapeOutline(Shape s) {
    // Do not add shape outline if the same shape has been filled recently.
    if (!drawingComponent || (!trackingAllowed && !trackingContinuityAllowed) || s.equals(lastShape)) {
      return;
    }
    Stroke stroke = getStroke();
    if (stroke instanceof BasicStroke) {
      BasicStroke bstroke = (BasicStroke) stroke;
      if (bstroke.getLineWidth() < 3) {
        appendShape(ObjectCache.getInstance().fetchBasicStroke(3).createStrokedShape(s));
      } else {
        appendShape(getStroke().createStrokedShape(s));
      }
    } else {
      appendShape(getStroke().createStrokedShape(s));
    }
  }

  @Override
  public void addRenderingHints(Map<?, ?> hints) {
    canvasGraphics.addRenderingHints(hints);
  }

  @Override
  public void clip(Shape s) {
    canvasGraphics.clip(s);
  }

  @Override
  public void draw(Shape s) {
    canvasGraphics.draw(s);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(s);
    }
  }

  @Override
  public void drawGlyphVector(GlyphVector g, float x, float y) {
    canvasGraphics.drawGlyphVector(g, x, y);
  }

  @Override
  public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
    // FIXME: process map
    return canvasGraphics.drawImage(img, xform, obs);
  }

  @Override
  public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
    canvasGraphics.drawImage(img, op, x, y);
    // FIXME: process map
  }

  @Override
  public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
    canvasGraphics.drawRenderableImage(img, xform);
    // FIXME: process map
  }

  @Override
  public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
    canvasGraphics.drawRenderedImage(img, xform);
    // FIXME: process map
  }

  @Override
  public void drawString(String str, int x, int y) {
    canvasGraphics.drawString(str, x, y);
    if (drawingComponent && trackingAllowed) {
      FontMetrics fontMetrics = canvasGraphics.getFontMetrics();
      Rectangle2D rect = fontMetrics.getStringBounds(str, canvasGraphics);
      Point2D point = new Point2D.Double(x, y);
      // currentTx.transform(point, point);
      Rectangle2D finalRec =
          new Rectangle2D.Double(rect.getX() + point.getX(), rect.getY() + point.getY(), rect.getWidth(),
              rect.getHeight());
      appendShape(finalRec);
    }
  }

  @Override
  public void drawString(String str, float x, float y) {
    canvasGraphics.drawString(str, x, y);
    if (drawingComponent && trackingAllowed) {
      FontMetrics fontMetrics = canvasGraphics.getFontMetrics();
      Rectangle2D rect = fontMetrics.getStringBounds(str, canvasGraphics);
      appendShape(new Rectangle2D.Double(rect.getX() + x, rect.getY() + y, rect.getWidth(), rect.getHeight()));
    }
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, int x, int y) {
    canvasGraphics.drawString(iterator, x, y);
    // FIXME: process map
  }

  @Override
  public void drawString(AttributedCharacterIterator iterator, float x, float y) {
    canvasGraphics.drawString(iterator, x, y);
    // FIXME: process map
  }

  @Override
  public void fill(Shape s) {
    canvasGraphics.fill(s);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(s);
    }
  }

  @Override
  public Color getBackground() {
    return canvasGraphics.getBackground();
  }

  @Override
  public Composite getComposite() {
    return canvasGraphics.getComposite();
  }

  @Override
  public GraphicsConfiguration getDeviceConfiguration() {
    return canvasGraphics.getDeviceConfiguration();
  }

  @Override
  public FontRenderContext getFontRenderContext() {
    return canvasGraphics.getFontRenderContext();
  }

  @Override
  public Paint getPaint() {
    return canvasGraphics.getPaint();
  }

  @Override
  public Object getRenderingHint(Key hintKey) {
    return canvasGraphics.getRenderingHint(hintKey);
  }

  @Override
  public RenderingHints getRenderingHints() {
    return canvasGraphics.getRenderingHints();
  }

  @Override
  public Stroke getStroke() {
    return canvasGraphics.getStroke();
  }

  @Override
  public AffineTransform getTransform() {
    return canvasGraphics.getTransform();
  }

  @Override
  public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
    return canvasGraphics.hit(rect, s, onStroke);
  }

  @Override
  public void rotate(double theta) {
    canvasGraphics.rotate(theta);
    currentTx.rotate(theta);
  }

  @Override
  public void rotate(double theta, double x, double y) {
    canvasGraphics.rotate(theta, x, y);
    currentTx.rotate(theta, x, y);
  }

  @Override
  public void scale(double sx, double sy) {
    canvasGraphics.scale(sx, sy);
    currentTx.scale(sx, sy);
  }

  @Override
  public void setBackground(Color color) {
    canvasGraphics.setBackground(color);
    // FIXME: fix map
  }

  @Override
  public void setComposite(Composite comp) {
    canvasGraphics.setComposite(comp);
    // FIXME: check this.
  }

  @Override
  public void setPaint(Paint paint) {
    canvasGraphics.setPaint(paint);
    // FIXME: check this
  }

  @Override
  public void setRenderingHint(Key hintKey, Object hintValue) {
    canvasGraphics.setRenderingHint(hintKey, hintValue);
    // FIXME: check this
  }

  @Override
  public void setRenderingHints(Map<?, ?> hints) {
    canvasGraphics.setRenderingHints(hints);
    // FIXME: check this
  }

  @Override
  public void setStroke(Stroke s) {
    if (this.zoom > 1 && s instanceof BasicStroke) {
      BasicStroke bs = (BasicStroke) s;
      // make thin lines even thinner to compensate for zoom factor
      if (bs.getLineWidth() <= 2 && !(s instanceof ZoomableStroke))
        s = ObjectCache.getInstance().fetchStroke((float) (bs.getLineWidth() / zoom), bs.getDashArray(), bs.getDashPhase(), bs.getEndCap());
    }
    canvasGraphics.setStroke(s);
  }

  @Override
  public void setTransform(AffineTransform Tx) {
    canvasGraphics.setTransform(Tx);
    currentTx = new AffineTransform(Tx);
    try {
      // Invert the tx that was set before we started drawing the component.
      // We're left only component tx.
      AffineTransform inverseInitialTx = initialTx.createInverse();
      Point2D p = new Point2D.Double(currentTx.getTranslateX(), currentTx.getTranslateY());
      inverseInitialTx.transform(p, p);
      currentTx.concatenate(inverseInitialTx);
      double[] matrix = new double[6];
      currentTx.getMatrix(matrix);
      currentTx.setTransform(matrix[0], matrix[1], matrix[2], matrix[3], p.getX(), p.getY());
    } catch (NoninvertibleTransformException e) {
    }
  }

  @Override
  public void shear(double shx, double shy) {
    canvasGraphics.shear(shx, shy);
    currentTx.shear(shx, shy);
  }

  @Override
  public void transform(AffineTransform Tx) {
    canvasGraphics.transform(Tx);
    currentTx.concatenate(Tx);
  }

  @Override
  public void translate(int x, int y) {
    canvasGraphics.translate(x, y);
    currentTx.translate(x, y);
  }

  @Override
  public void translate(double tx, double ty) {
    canvasGraphics.translate(tx, ty);
    currentTx.translate(tx, ty);
  }

  @Override
  public void clearRect(int x, int y, int width, int height) {
    canvasGraphics.clearRect(x, y, width, height);
  }

  @Override
  public void clipRect(int x, int y, int width, int height) {
    canvasGraphics.clipRect(x, y, width, height);
  }

  @Override
  public void copyArea(int x, int y, int width, int height, int dx, int dy) {
    canvasGraphics.copyArea(x, y, width, height, dx, dy);
  }

  @Override
  public Graphics create() {
    return canvasGraphics.create();
  }

  @Override
  public void dispose() {
    canvasGraphics.dispose();
  }

  @Override
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    canvasGraphics.drawArc(x, y, width, height, startAngle, arcAngle);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));
    }    
  }

  @Override
  public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
    boolean result = canvasGraphics.drawImage(img, x, y, observer);
    appendShape(new Rectangle2D.Double(x, y, img.getWidth(observer), img.getHeight(observer)));
    return result;
  }

  @Override
  public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
    // FIXME: map
    return canvasGraphics.drawImage(img, x, y, bgcolor, observer);
  }

  @Override
  public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
    // FIXME: map
    return canvasGraphics.drawImage(img, x, y, width, height, observer);
  }

  @Override
  public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
    // FIXME: map
    return canvasGraphics.drawImage(img, x, y, width, height, bgcolor, observer);
  }

  @Override
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
      ImageObserver observer) {
    // FIXME: map
    return canvasGraphics.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
  }

  @Override
  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
      Color bgcolor, ImageObserver observer) {
    // FIXME: map
    return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
  }

  @Override
  public void drawLine(int x1, int y1, int x2, int y2) {
    canvasGraphics.drawLine(x1, y1, x2, y2);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new Line2D.Double(x1, y1, x2, y2));
    }
  }

  @Override
  public void drawOval(int x, int y, int width, int height) {
    canvasGraphics.drawOval(x, y, width, height);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new Ellipse2D.Double(x, y, width, height));
    }
  }

  @Override
  public void drawPolygon(int[] points, int[] points2, int points3) {
    canvasGraphics.drawPolygon(points, points2, points3);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new Polygon(points, points2, points3));
    }
  }

  @Override
  public void drawPolyline(int[] pointsX, int[] pointsY, int pointCount) {
    canvasGraphics.drawPolyline(pointsX, pointsY, pointCount);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed) && pointCount > 1) {
      Path2D path = new Path2D.Double();
      path.moveTo(pointsX[0], pointsY[0]);
      for (int i = 1; i < pointCount; i++)
        path.lineTo(pointsX[i], pointsY[i]);
      appendShapeOutline(path);
    }
  }

  @Override
  public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    canvasGraphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
    }
  }

  @Override
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    canvasGraphics.fillArc(x, y, width, height, startAngle, arcAngle);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.PIE));
    }
  }

  @Override
  public void fillOval(int x, int y, int width, int height) {
    canvasGraphics.fillOval(x, y, width, height);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(new Ellipse2D.Double(x, y, width, height));
    }
  }

  @Override
  public void fillPolygon(int[] points, int[] points2, int points3) {
    canvasGraphics.fillPolygon(points, points2, points3);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(new Polygon(points, points2, points3));
    }
  }

  @Override
  public void drawRect(int x, int y, int width, int height) {
    canvasGraphics.drawRect(x, y, width, height);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShapeOutline(new Rectangle(x, y, width, height));
    }
  }

  @Override
  public void fillRect(int x, int y, int width, int height) {
    canvasGraphics.fillRect(x, y, width, height);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(new Rectangle(x, y, width, height));
    }
  }

  @Override
  public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
    canvasGraphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    if (drawingComponent && (trackingAllowed || trackingContinuityAllowed)) {
      appendShape(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
    }
  }

  @Override
  public Shape getClip() {
    return canvasGraphics.getClip();
  }

  @Override
  public Rectangle getClipBounds() {
    return canvasGraphics.getClipBounds();
  }

  @Override
  public Color getColor() {
    return canvasGraphics.getColor();
  }

  @Override
  public Font getFont() {
    return canvasGraphics.getFont();
  }

  @Override
  public FontMetrics getFontMetrics(Font f) {
    return canvasGraphics.getFontMetrics(f);
  }

  @Override
  public void setClip(Shape clip) {
    canvasGraphics.setClip(clip);
  }

  @Override
  public void setClip(int x, int y, int width, int height) {
    canvasGraphics.setClip(x, y, width, height);
  }

  @Override
  public void setColor(Color c) {
    canvasGraphics.setColor(c);
  }

  @Override
  public void setFont(Font font) {
    canvasGraphics.setFont(font);
  }

  @Override
  public void setPaintMode() {
    canvasGraphics.setPaintMode();
  }

  @Override
  public void setXORMode(Color c1) {
    canvasGraphics.setXORMode(c1);
  }
  
  @SuppressWarnings("unused")
  private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap,
      final boolean order) {
    List<Entry<String, Integer>> list =
        new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

    // Sorting the list based on values
    Collections.sort(list, new Comparator<Entry<String, Integer>>() {
      public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
        if (order) {
          return o1.getValue().compareTo(o2.getValue());
        } else {
          return o2.getValue().compareTo(o1.getValue());

        }
      }
    });

    // Maintaining insertion order with the help of LinkedList
    Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
    for (Entry<String, Integer> entry : list) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }

    return sortedMap;
  }
}
