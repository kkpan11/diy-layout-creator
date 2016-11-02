package org.diylc.parsing;

import java.awt.Color;
import java.awt.Point;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.diylc.common.Display;
import org.diylc.common.HorizontalAlignment;
import org.diylc.common.Orientation;
import org.diylc.common.OrientationHV;
import org.diylc.common.VerticalAlignment;
import org.diylc.components.AbstractLeadedComponent;
import org.diylc.components.AbstractTransparentComponent;
import org.diylc.components.boards.AbstractBoard;
import org.diylc.components.boards.BlankBoard;
import org.diylc.components.boards.PerfBoard;
import org.diylc.components.boards.VeroBoard;
import org.diylc.components.connectivity.CopperTrace;
import org.diylc.components.connectivity.HookupWire;
import org.diylc.components.connectivity.Jumper;
import org.diylc.components.connectivity.SolderPad;
import org.diylc.components.connectivity.TraceCut;
import org.diylc.components.electromechanical.MiniToggleSwitch;
import org.diylc.components.electromechanical.ToggleSwitchType;
import org.diylc.components.misc.Label;
import org.diylc.components.passive.PotentiometerPanel;
import org.diylc.components.passive.RadialElectrolytic;
import org.diylc.components.passive.RadialFilmCapacitor;
import org.diylc.components.passive.Resistor;
import org.diylc.components.passive.Taper;
import org.diylc.components.semiconductors.DIL_IC;
import org.diylc.components.semiconductors.DiodePlastic;
import org.diylc.components.semiconductors.LED;
import org.diylc.components.semiconductors.SIL_IC;
import org.diylc.components.semiconductors.TransistorTO92;
import org.diylc.core.IDIYComponent;
import org.diylc.core.Project;
import org.diylc.core.measures.Capacitance;
import org.diylc.core.measures.Resistance;
import org.diylc.core.measures.Size;
import org.diylc.core.measures.SizeUnit;
import org.diylc.presenter.CalcUtils;
import org.diylc.presenter.ComparatorFactory;
import org.diylc.utils.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class V1FileParser implements IOldFileParser {

  private static final Logger LOG = Logger.getLogger(V1FileParser.class);

  private static final Size V1_GRID_SPACING = new Size(0.1d, SizeUnit.in);
  private static final Map<String, Color> V1_COLOR_MAP = new HashMap<String, Color>();
  static {
    V1_COLOR_MAP.put("red", Color.red);
    V1_COLOR_MAP.put("blue", Color.blue);
    V1_COLOR_MAP.put("white", Color.white);
    V1_COLOR_MAP.put("green", Color.green.darker());
    V1_COLOR_MAP.put("black", Color.black);
    V1_COLOR_MAP.put("yellow", Color.yellow);
  }

  @Override
  public boolean canParse(String version) {
    return version == null || version.trim().isEmpty();
  }

  public Project parseFile(Element root, List<String> warnings) {
    Project project = new Project();
    project.setTitle(root.getAttribute("Project"));
    project.setAuthor(root.getAttribute("Credits"));
    project.setGridSpacing(V1_GRID_SPACING);
    String type = root.getAttribute("Type");

    // Create the board.
    int width = Integer.parseInt(root.getAttribute("Width")) + 1;
    int height = Integer.parseInt(root.getAttribute("Height")) + 1;
    int boardWidth = (int) (width * Constants.PIXELS_PER_INCH * V1_GRID_SPACING.getValue());
    int boardHeight = (int) (height * Constants.PIXELS_PER_INCH * V1_GRID_SPACING.getValue());
    int projectWidth = (int) project.getWidth().convertToPixels();
    int projectHeight = (int) project.getHeight().convertToPixels();
    int x = (projectWidth - boardWidth) / 2;
    int y = (projectHeight - boardHeight) / 2;
    AbstractBoard board;
    if (type.equalsIgnoreCase("pcb")) {
      board = new BlankBoard();
      board.setBoardColor(Color.white);
      board.setBorderColor(Color.black);
    } else if (type.equalsIgnoreCase("perfboard")) {
      board = new PerfBoard();
    } else if (type.equalsIgnoreCase("stripboard")) {
      board = new VeroBoard();
    } else {
      throw new IllegalArgumentException("Unrecognized board type: " + type);
    }
    board.setName("Main board");
    Point referencePoint =
        new Point(CalcUtils.roundToGrid(x, V1_GRID_SPACING), CalcUtils.roundToGrid(y, V1_GRID_SPACING));
    board.setControlPoint(referencePoint, 0);
    board.setControlPoint(
        new Point(CalcUtils.roundToGrid(x + boardWidth, V1_GRID_SPACING), CalcUtils.roundToGrid(y + boardHeight,
            V1_GRID_SPACING)), 1);
    project.getComponents().add(board);

    NodeList childNodes = root.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node node = childNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        String nodeName = node.getNodeName();
        String nameAttr = node.getAttributes().getNamedItem("Name").getNodeValue();
        Node valueNode = node.getAttributes().getNamedItem("Value");
        String valueAttr = valueNode == null ? null : valueNode.getNodeValue();
        int x1Attr = Integer.parseInt(node.getAttributes().getNamedItem("X1").getNodeValue());
        int y1Attr = Integer.parseInt(node.getAttributes().getNamedItem("Y1").getNodeValue());
        Point point1 = convertV1CoordinatesToV3Point(referencePoint, x1Attr, y1Attr);
        Point point2 = null;
        Integer x2Attr = null;
        Integer y2Attr = null;
        Color color = null;
        if (node.getAttributes().getNamedItem("Color") != null) {
          String colorAttr = node.getAttributes().getNamedItem("Color").getNodeValue();
          color = V1_COLOR_MAP.get(colorAttr.toLowerCase());
        }
        if (node.getAttributes().getNamedItem("X2") != null && node.getAttributes().getNamedItem("Y2") != null) {
          x2Attr = Integer.parseInt(node.getAttributes().getNamedItem("X2").getNodeValue());
          y2Attr = Integer.parseInt(node.getAttributes().getNamedItem("Y2").getNodeValue());
          point2 = convertV1CoordinatesToV3Point(referencePoint, x2Attr, y2Attr);
        }
        IDIYComponent<?> component = null;
        if (nodeName.equalsIgnoreCase("text")) {
          LOG.debug("Recognized " + nodeName);
          Label label = new Label();
          label.setName(nameAttr);
          if (color != null) {
            label.setColor(color);
          }
          label.setText(valueAttr);
          label.setHorizontalAlignment(HorizontalAlignment.LEFT);
          label.setVerticalAlignment(VerticalAlignment.CENTER);
          label.setControlPoint(convertV1CoordinatesToV3Point(referencePoint, x1Attr, y1Attr), 0);
          component = label;
        } else if (nodeName.equalsIgnoreCase("pad")) {
          LOG.debug("Recognized " + nodeName);
          SolderPad pad = new SolderPad();
          pad.setName(nameAttr);
          if (color != null) {
            pad.setColor(color);
          }
          pad.setControlPoint(convertV1CoordinatesToV3Point(referencePoint, x1Attr, y1Attr), 0);
          component = pad;
        } else if (nodeName.equalsIgnoreCase("cut")) {
          LOG.debug("Recognized " + nodeName);
          TraceCut cut = new TraceCut();
          cut.setCutBetweenHoles(false);
          cut.setName(nameAttr);
          cut.setControlPoint(convertV1CoordinatesToV3Point(referencePoint, x1Attr, y1Attr), 0);
          component = cut;
        } else if (nodeName.equalsIgnoreCase("trace")) {
          LOG.debug("Recognized " + nodeName);
          CopperTrace trace = new CopperTrace();
          trace.setName(nameAttr);
          if (color != null) {
            trace.setLeadColor(color);
          }
          trace.setControlPoint(point1, 0);
          trace.setControlPoint(point2, 1);
          component = trace;
        } else if (nodeName.equalsIgnoreCase("jumper")) {
          LOG.debug("Recognized " + nodeName);
          Jumper jumper = new Jumper();
          jumper.setName(nameAttr);
          jumper.setControlPoint(point1, 0);
          jumper.setControlPoint(point2, 1);
          component = jumper;
        } else if (nodeName.equalsIgnoreCase("wire")) {
          LOG.debug("Recognized " + nodeName);
          HookupWire wire = new HookupWire();
          Point midPoint = new Point((point1.x + point2.x) / 2, (point1.y + point2.y) / 2);
          wire.setName(nameAttr);
          String colorAttr = node.getAttributes().getNamedItem("Color").getNodeValue();
          wire.setColor(parseV1Color(colorAttr));
          wire.setControlPoint(point1, 0);
          wire.setControlPoint(midPoint, 1);
          wire.setControlPoint(midPoint, 2);
          wire.setControlPoint(point2, 3);
          component = wire;
        } else if (nodeName.equalsIgnoreCase("resistor")) {
          LOG.debug("Recognized " + nodeName);
          Resistor resistor = new Resistor();
          resistor.setName(nameAttr);
          try {
            resistor.setValue(Resistance.parseResistance(valueAttr));
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          resistor.setLength(new Size(6.35d, SizeUnit.mm));
          resistor.setWidth(new Size(2.2d, SizeUnit.mm));
          resistor.setControlPoint(point1, 0);
          resistor.setControlPoint(point2, 1);
          component = resistor;
        } else if (nodeName.equalsIgnoreCase("capacitor")) {
          LOG.debug("Recognized " + nodeName);
          RadialFilmCapacitor capacitor = new RadialFilmCapacitor();
          capacitor.setName(nameAttr);
          try {
            capacitor.setValue(Capacitance.parseCapacitance(valueAttr));
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          capacitor.setLength(new Size(6d, SizeUnit.mm));
          capacitor.setWidth(new Size(2d, SizeUnit.mm));
          capacitor.setControlPoint(point1, 0);
          capacitor.setControlPoint(point2, 1);
          component = capacitor;
        } else if (nodeName.equalsIgnoreCase("electrolyte")) {
          LOG.debug("Recognized " + nodeName);
          RadialElectrolytic capacitor = new RadialElectrolytic();
          capacitor.setName(nameAttr);
          try {
            capacitor.setValue(Capacitance.parseCapacitance(valueAttr));
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          try {
            String sizeAttr = node.getAttributes().getNamedItem("Size").getNodeValue();
            if (sizeAttr.equalsIgnoreCase("small")) {
              capacitor.setLength(new Size(3.5d, SizeUnit.mm));
            } else if (sizeAttr.equalsIgnoreCase("medium")) {
              capacitor.setLength(new Size(5d, SizeUnit.mm));
            } else if (sizeAttr.equalsIgnoreCase("large")) {
              capacitor.setLength(new Size(7d, SizeUnit.mm));
            } else {
              capacitor.setLength(new Size(4d, SizeUnit.mm));
            }
          } catch (Exception e) {
            capacitor.setLength(new Size(5d, SizeUnit.mm));
            LOG.debug("Could not set size of " + nameAttr);
          }
          capacitor.setControlPoint(point1, 0);
          capacitor.setControlPoint(point2, 1);
          component = capacitor;
        } else if (nodeName.equalsIgnoreCase("diode")) {
          LOG.debug("Recognized " + nodeName);
          DiodePlastic capacitor = new DiodePlastic();
          capacitor.setName(nameAttr);
          try {
            capacitor.setValue(valueAttr);
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          capacitor.setLength(new Size(6d, SizeUnit.mm));
          capacitor.setWidth(new Size(2d, SizeUnit.mm));
          capacitor.setControlPoint(point1, 0);
          capacitor.setControlPoint(point2, 1);
          component = capacitor;
        } else if (nodeName.equalsIgnoreCase("led")) {
          LOG.debug("Recognized " + nodeName);
          LED led = new LED();
          led.setName(nameAttr);
          led.setValue(valueAttr);
          led.setBodyColor(Color.red);
          led.setBorderColor(Color.red.darker());
          led.setLength(new Size(3d, SizeUnit.mm));
          led.setControlPoint(point1, 0);
          led.setControlPoint(point2, 1);
          component = led;
        } else if (nodeName.equalsIgnoreCase("transistor")) {
          LOG.debug("Recognized " + nodeName);
          TransistorTO92 transistor = new TransistorTO92();
          transistor.setName(nameAttr);
          try {
            transistor.setValue(valueAttr);
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          transistor.setControlPoint(point1, 0);
          if (point1.y > point2.y) {
            transistor.setOrientation(Orientation._180);
          } else if (point1.y < point2.y) {
            transistor.setOrientation(Orientation.DEFAULT);
          } else if (point1.x < point2.x) {
            transistor.setOrientation(Orientation._270);
          } else if (point1.x > point2.x) {
            transistor.setOrientation(Orientation._90);
          }
          // capacitor.setControlPoint(point2, 1);
          component = transistor;
        } else if (nodeName.equalsIgnoreCase("ic")) {
          LOG.debug("Recognized " + nodeName);
          DIL_IC ic = new DIL_IC();
          int pinCount = 8;
          int rowSpace = 3;
          if (x1Attr < x2Attr && y1Attr < y2Attr) {
            pinCount = (y2Attr - y1Attr + 1) * 2;
            rowSpace = x2Attr - x1Attr;
            ic.setOrientation(Orientation.DEFAULT);
          } else if (x1Attr > x2Attr && y1Attr < y2Attr) {
            pinCount = (x1Attr - x2Attr + 1) * 2;
            rowSpace = y2Attr - y1Attr;
            ic.setOrientation(Orientation._90);
          } else if (x1Attr > x2Attr && y1Attr > y2Attr) {
            rowSpace = x1Attr - x2Attr;
            pinCount = (y1Attr - y2Attr + 1) * 2;
            ic.setOrientation(Orientation._180);
          } else if (x1Attr < x2Attr && y1Attr > y2Attr) {
            rowSpace = y1Attr - y2Attr;
            pinCount = (x2Attr - x1Attr + 1) * 2;
            ic.setOrientation(Orientation._270);
          }
          ic.setRowSpacing(new Size(0.1 * rowSpace, SizeUnit.in));
          ic.setPinCount(DIL_IC.PinCount.valueOf("_" + pinCount));
          ic.setName(nameAttr);
          // Translate control points.
          for (int j = 0; j < ic.getControlPointCount(); j++) {
            Point p = new Point(ic.getControlPoint(j));
            p.translate(point1.x, point1.y);
            ic.setControlPoint(p, j);
          }
          ic.setValue(valueAttr);
          component = ic;
        } else if (nodeName.equalsIgnoreCase("switch")) {
          LOG.debug("Recognized " + nodeName);
          MiniToggleSwitch sw = new MiniToggleSwitch();
          int sizeX = Math.abs(x1Attr - x2Attr);
          int sizeY = Math.abs(y1Attr - y2Attr);
          ToggleSwitchType switchType = null;
          OrientationHV orientation = null;
          if (Math.min(sizeX, sizeY) == 0 && Math.max(sizeX, sizeY) == 1) {
            switchType = ToggleSwitchType.SPST;
            orientation = sizeX < sizeY ? OrientationHV.VERTICAL : OrientationHV.HORIZONTAL;
          }
          if (Math.min(sizeX, sizeY) == 0 && Math.max(sizeX, sizeY) == 2) {
            switchType = ToggleSwitchType.SPDT;
            orientation = sizeX < sizeY ? OrientationHV.VERTICAL : OrientationHV.HORIZONTAL;
          }
          if (Math.min(sizeX, sizeY) == 1 && Math.max(sizeX, sizeY) == 2) {
            switchType = ToggleSwitchType.DPDT;
            orientation = sizeX < sizeY ? OrientationHV.VERTICAL : OrientationHV.HORIZONTAL;
          }
          if (Math.min(sizeX, sizeY) == 2 && Math.max(sizeX, sizeY) == 2) {
            switchType = ToggleSwitchType._3PDT;
            orientation = OrientationHV.VERTICAL;
          }
          if (Math.min(sizeX, sizeY) == 2 && Math.max(sizeX, sizeY) == 3) {
            switchType = ToggleSwitchType._4PDT;
            orientation = sizeX < sizeY ? OrientationHV.HORIZONTAL : OrientationHV.VERTICAL;
          }
          if (Math.min(sizeX, sizeY) == 2 && Math.max(sizeX, sizeY) == 4) {
            switchType = ToggleSwitchType._5PDT;
            orientation = sizeX < sizeY ? OrientationHV.HORIZONTAL : OrientationHV.HORIZONTAL;
          }

          if (switchType == null || orientation == null) {
            String message = "Unsupported toggle switch dimensions";
            LOG.debug(message);
            if (!warnings.contains(message)) {
              warnings.add(message);
            }
          } else {
            sw.setName(nameAttr);
            sw.setOrientation(orientation);
            sw.setValue(switchType);
            sw.setSpacing(new Size(0.1, SizeUnit.in));
            // compensate for potential negative coordinates after the type and orientation have
            // been set. Make sure that the top left corner is at (0, 0)
            int dx = 0;
            int dy = 0;
            for (int j = 0; j < sw.getControlPointCount(); j++) {
              Point p = new Point(sw.getControlPoint(j));
              if (p.x < 0 && p.x < dx)
                dx = p.x;
              if (p.y < 0 && p.y < dy)
                dy = p.y;
            }
            // Translate control points.
            for (int j = 0; j < sw.getControlPointCount(); j++) {
              Point p = new Point(sw.getControlPoint(j));
              p.translate(Math.min(point1.x, point2.x) - dx, Math.min(point1.y, point2.y) - dy);
              sw.setControlPoint(p, j);
            }
            component = sw;
          }
        } else if (nodeName.equalsIgnoreCase("lineic")) {
          LOG.debug("Recognized " + nodeName);
          SIL_IC ic = new SIL_IC();
          int pinCount = 8;
          if (x1Attr == x2Attr && y1Attr < y2Attr) {
            pinCount = (y2Attr - y1Attr + 1);
            ic.setOrientation(Orientation.DEFAULT);
          } else if (x1Attr > x2Attr && y1Attr == y2Attr) {
            pinCount = (x1Attr - x2Attr + 1);
            ic.setOrientation(Orientation._90);
          } else if (x1Attr == x2Attr && y1Attr > y2Attr) {
            pinCount = (y1Attr - y2Attr + 1);
            ic.setOrientation(Orientation._180);
          } else if (x1Attr < x2Attr && y1Attr == y2Attr) {
            pinCount = (x2Attr - x1Attr + 1);
            ic.setOrientation(Orientation._270);
          }
          ic.setPinCount(SIL_IC.PinCount.valueOf("_" + pinCount));
          ic.setName(nameAttr);
          // Translate control points.
          for (int j = 0; j < ic.getControlPointCount(); j++) {
            Point p = new Point(ic.getControlPoint(j));
            p.translate(point1.x, point1.y);
            ic.setControlPoint(p, j);
          }
          ic.setValue(valueAttr);
          component = ic;
        } else if (nodeName.equalsIgnoreCase("pot")) {
          LOG.debug("Recognized " + nodeName);
          PotentiometerPanel pot = new PotentiometerPanel();
          pot.setBodyDiameter(new Size(14d, SizeUnit.mm));
          pot.setSpacing(new Size(0.2, SizeUnit.in));
          pot.setName(nameAttr);
          try {
            pot.setValue(Resistance.parseResistance(valueAttr));
          } catch (Exception e) {
            LOG.debug("Could not set value of " + nameAttr);
          }
          String taperAttr = node.getAttributes().getNamedItem("Taper").getNodeValue();
          if ("Linear".equals(taperAttr)) {
            pot.setTaper(Taper.LIN);
          } else if ("Audio".equals(taperAttr)) {
            pot.setTaper(Taper.LOG);
          } else if ("Reverse Audio".equals(taperAttr)) {
            pot.setTaper(Taper.REV_LOG);
          }
          // Pin spacing, we'll need to move pot around a bit.
          int delta = Constants.PIXELS_PER_INCH / 5;
          if (x1Attr < x2Attr) {
            pot.setOrientation(Orientation.DEFAULT);
            for (int j = 0; j < pot.getControlPointCount(); j++) {
              Point p = new Point(pot.getControlPoint(j));
              p.translate(point1.x - delta, point1.y);
              pot.setControlPoint(p, j);
            }
          } else if (x1Attr > x2Attr) {
            pot.setOrientation(Orientation._180);
            for (int j = 0; j < pot.getControlPointCount(); j++) {
              Point p = new Point(pot.getControlPoint(j));
              p.translate(point1.x + delta, point1.y);
              pot.setControlPoint(p, j);
            }
          } else if (y1Attr < y2Attr) {
            pot.setOrientation(Orientation._90);
            for (int j = 0; j < pot.getControlPointCount(); j++) {
              Point p = new Point(pot.getControlPoint(j));
              p.translate(point1.x, point1.y - delta);
              pot.setControlPoint(p, j);
            }
          } else if (y1Attr > y2Attr) {
            pot.setOrientation(Orientation._270);
            for (int j = 0; j < pot.getControlPointCount(); j++) {
              Point p = new Point(pot.getControlPoint(j));
              p.translate(point1.x, point1.y + delta);
              pot.setControlPoint(p, j);
            };
          }
          component = pot;
        } else {
          String message = "Could not recognize component type " + nodeName;
          LOG.debug(message);
          if (!warnings.contains(message)) {
            warnings.add(message);
          }
        }
        if (component != null) {
          if (component instanceof AbstractLeadedComponent<?>) {
            ((AbstractLeadedComponent<?>) component).setDisplay(Display.NAME);
          }
          if (component instanceof AbstractTransparentComponent<?>) {
            ((AbstractTransparentComponent<?>) component).setAlpha((byte) 100);
          }
          project.getComponents().add(component);
        }
      }
    }
    Collections.sort(project.getComponents(), ComparatorFactory.getInstance().getComponentZOrderComparator());
    return project;
  }

  private Point convertV1CoordinatesToV3Point(Point reference, int x, int y) {
    Point point = new Point(reference);
    point.translate((int) (x * Constants.PIXELS_PER_INCH * V1_GRID_SPACING.getValue()), (int) (y
        * Constants.PIXELS_PER_INCH * V1_GRID_SPACING.getValue()));
    return point;
  }

  private Color parseV1Color(String color) {
    if ("brown".equals(color.toLowerCase()))
      return new Color(139, 69, 19);
    try {
      Field field = Color.class.getDeclaredField(color.toLowerCase());
      return (Color) field.get(null);
    } catch (Exception e) {
      LOG.error("Could not parse color \"" + color + "\"", e);
      return Color.black;
    }
  }

}