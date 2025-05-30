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
package org.diylc.components.transform;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.diylc.common.IComponentTransformer;
import org.diylc.common.Orientation;
import org.diylc.components.misc.Label;
import org.diylc.components.misc.PCBText;
import org.diylc.core.IDIYComponent;

public class TextTransformer extends SimpleComponentTransformer {

  @Override
  public boolean canRotate(IDIYComponent<?> component) {
    return component.getClass().equals(Label.class) || component.getClass().equals(PCBText.class);
  }

  @Override
  public void rotate(IDIYComponent<?> component, Point2D center, int direction) {
    AffineTransform rotate = AffineTransform.getRotateInstance(Math.PI / 2 * direction, center.getX(), center.getY());
    for (int index = 0; index < component.getControlPointCount(); index++) {
      Point2D p = new Point2D.Double();
      rotate.transform(component.getControlPoint(index), p);
      component.setControlPoint(p, index);
    }

    if (component instanceof Label) {
      Label snap = (Label) component;
      Orientation o = snap.getOrientation();
      int oValue = o.ordinal();
      oValue += direction;
      if (oValue < 0)
        oValue = Orientation.values().length - 1;
      if (oValue >= Orientation.values().length)
        oValue = 0;
      o = Orientation.values()[oValue];
      snap.setOrientation(o);
    } else if (component instanceof PCBText) {
      PCBText snap = (PCBText) component;
      Orientation o = snap.getOrientation();
      int oValue = o.ordinal();
      oValue += direction;
      if (oValue < 0)
        oValue = Orientation.values().length - 1;
      if (oValue >= Orientation.values().length)
        oValue = 0;
      o = Orientation.values()[oValue];
      snap.setOrientation(o);
    }
  }
}
