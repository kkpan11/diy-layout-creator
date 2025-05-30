/*

    DIY Layout Creator (DIYLC).
    Copyright (c) 2009-2025 held jointly by the individual authors.

    This file is part of DIYLC.

    DIYLC is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DIYLC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DIYLC.  If not, see <http://www.gnu.org/licenses/>.

*/
package org.diylc.components.misc;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import org.diylc.common.ObjectCache;
import org.diylc.common.Orientation;
import org.diylc.components.AbstractComponent;
import org.diylc.core.ComponentState;
import org.diylc.core.ICommonNode;
import org.diylc.core.IDIYComponent;
import org.diylc.core.IDrawingObserver;
import org.diylc.core.Project;
import org.diylc.core.VisibilityPolicy;
import org.diylc.core.annotations.BomPolicy;
import org.diylc.core.annotations.ComponentDescriptor;
import org.diylc.core.annotations.EditableProperty;
import org.diylc.core.measures.Size;
import org.diylc.core.measures.SizeUnit;
import org.diylc.utils.Constants;

@ComponentDescriptor(name = "Ground", author = "Branislav Stojkovic", category = "Schematic Symbols",
    instanceNamePrefix = "GND", description = "Ground schematic symbol",
    zOrder = IDIYComponent.COMPONENT, bomPolicy = BomPolicy.NEVER_SHOW, autoEdit = false)
public class GroundSymbol extends AbstractComponent<Void> implements ICommonNode {

  private static final long serialVersionUID = 1L;

  public static Color COLOR = Color.black;
  public static Size SIZE = new Size(0.15d, SizeUnit.in);

  private Point2D.Double point = new Point2D.Double(0, 0);
  private Color color = COLOR;
  private Size size = SIZE;
  private GroundSymbolType type = GroundSymbolType.DEFAULT;
  private Orientation orientation = Orientation.DEFAULT;
  private Integer stroke = 1;

  @SuppressWarnings("incomplete-switch")
  @Override
  public void draw(Graphics2D g2d, ComponentState componentState, boolean outlineMode, Project project,
      IDrawingObserver drawingObserver) {
    double sizePx = size.convertToPixels();
    g2d.setStroke(ObjectCache.getInstance().fetchBasicStroke((float)getStroke()));
    
    double x = point.getX();
    double y = point.getY();

    switch (getOrientation()) {
      case _90:
        g2d.rotate(Math.PI / 2, point.getX(), point.getY());
        break;
      case _180:
        g2d.rotate(Math.PI, point.getX(), point.getY());
        break;
      case _270:
        g2d.rotate(Math.PI * 3 / 2, point.getX(), point.getY());
        break;
    }
    
    Polygon poly =
        new Polygon(new int[] {(int)(x - sizePx / 2), (int)(x + sizePx / 2), (int)x}, new int[] {(int)(y + sizePx / 6), (int)(y + sizePx / 6),
            (int)(y + sizePx)}, 3);
    
    g2d.setColor(Constants.TRANSPARENT_COLOR);
    g2d.fill(poly);
    
    drawingObserver.stopTracking();

    g2d.setColor(color);
    g2d.drawLine((int)x, (int)y, (int)x, (int)(y + sizePx / 6));
    if (type == GroundSymbolType.DEFAULT) {
      double delta = sizePx / 7.5;
      for (int i = 0; i < 5; i++) {
        g2d.drawLine((int)(x - sizePx / 2 + delta * i), (int)(y + sizePx / 6 * (i + 1)), (int)(x + sizePx / 2 - delta * i), (int)(y + sizePx / 6
            * (i + 1)));
      }
    } else {      
      g2d.draw(poly);     
    }
  }

  @Override
  public void drawIcon(Graphics2D g2d, int width, int height) {
    int margin = 3 * width / 32;
    g2d.setStroke(ObjectCache.getInstance().fetchBasicStroke(1));
    g2d.setColor(COLOR);
    g2d.drawLine(width / 2, margin * 2, width / 2, margin * 3 + height / 5);
    for (int i = 0; i < 5; i++) {
      g2d.drawLine(margin * (i + 1), margin * (3 + i) + height / 5, width - margin * (i + 1), margin * (3 + i) + height
          / 5);
    }
  }

  @EditableProperty(name = "Style")
  public GroundSymbolType getType() {
    return type;
  }

  public void setType(GroundSymbolType type) {
    this.type = type;
  }
  
  @EditableProperty
  public int getStroke() {
    if (stroke == null) {
      stroke = 1;
    }
    return stroke;
  }
  
  public void setStroke(int stroke) {
    this.stroke = stroke;
  }
  
  @EditableProperty
  public Orientation getOrientation() {
    if (orientation == null)
      orientation = Orientation.DEFAULT;
    return orientation;
  }
  
  public void setOrientation(Orientation orientation) {
    this.orientation = orientation;
  }

  @Override
  public Point2D getControlPoint(int index) {
    return point;
  }

  @EditableProperty
  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  @EditableProperty
  public Size getSize() {
    return size;
  }

  public void setSize(Size size) {
    this.size = size;
  }

  @Override
  public int getControlPointCount() {
    return 1;
  }

  @Override
  public boolean isControlPointSticky(int index) {
    return true;
  }

  @Override
  public void setControlPoint(Point2D point, int index) {
    this.point.setLocation(point);
  }
  
  @Override
  public String getCommonNodeLabel() {    
    return "GND";
  }

  @Override
  public VisibilityPolicy getControlPointVisibilityPolicy(int index) {
    return VisibilityPolicy.WHEN_SELECTED;
  }

  @Deprecated
  @Override
  public Void getValue() {
    return null;
  }

  @Deprecated
  @Override
  public void setValue(Void value) {}

  public static enum GroundSymbolType {
    DEFAULT("Default"), TRIANGLE("Triangle");

    private String title;

    private GroundSymbolType(String title) {
      this.title = title;
    }

    public String getTitle() {
      return title;
    }

    @Override
    public String toString() {
      return title;
    }
  }
}
