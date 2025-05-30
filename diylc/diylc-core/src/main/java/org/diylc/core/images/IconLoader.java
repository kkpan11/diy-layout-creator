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
package org.diylc.core.images;

import org.apache.log4j.Logger;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Loads image resources as Icons.
 * 
 * @author Branislav Stojkovic
 */
public enum IconLoader {

  Delete("delete.png"), Add("add.png"), FolderOut("folder_out.png"), Garbage("garbage.png"), DiskBlue("disk_blue.png"), SaveAs(
      "save_as.png"), Exit("exit.png"), DocumentPlainYellow("document_plain_yellow.png"), PhotoScenery(
      "photo_scenery.png"), LightBulbOn("lightbulb_on.png"), LightBulbOff("lightbulb_off.png"), NotebookAdd(
      "notebook_add.png"), FormGreen("form_green.png"), Gears("gears.png"), About("about.png"), WindowColors(
      "window_colors.png"), WindowGear("window_gear.png"), NavigateCheck("navigate_check.png"), Undo("undo.png"), Error(
      "error.png"), Warning("warning.png"), ZoomSmall("zoom_small.png"), MoveSmall("move_small.png"), Print("print.png"), PDF(
      "pdf.png"), Excel("excel.png"), CSV("csv.png"), HTML("html.png"), Image("image.png"), Cut("cut.png"), Copy(
      "copy.png"), Paste("paste.png"), Selection("selection.png"), BOM("bom.png"), BlackBoard("blackboard.png"), IdCard(
      "id_card.png"), IdCardAdd("id_card_add.png"), Chest("chest.png"), Upload("upload.png"), Wrench("wrench.png"), Group(
      "group.png"), Ungroup("ungroup.png"), TraceMask("trace_mask.png"), Faq("faq.png"), Component("component.png"), Plugin(
      "plugin.png"), Manual("manual.png"), Donate("donate.png"), Bug("bug.png"), IconLarge("icon_large.png"), IconMedium(
      "icon_medium.png"), IconSmall("icon_small.png"), DocumentEdit("document_edit.png"), EditComponent(
      "edit_component.png"), Size("size.png"), Front("front.png"), Back("back.png"), Pens("pens.png"), Sort("sort.png"), ElementsSelection(
      "elements_selection.png"), Schaller("schaller.png");

  protected String name;

  private IconLoader(String name) {
    this.name = name;
  }

  public Icon getIcon() {
    java.net.URL imgURL = getClass().getResource("/diylc-core-images/" + name);
    if (imgURL != null) {
      return new ImageIcon(imgURL, name);
    } else {
      Logger.getLogger(IconLoader.class).error("Couldn't find file: " + name);
      return null;
    }
  }

  public Image getImage() {
    BufferedImage img = null;
    try {
      img = ImageIO.read(getClass().getResourceAsStream("/diylc-core-images/" + name));
    } catch (IOException e) {
      Logger.getLogger(IconLoader.class).error("Couldn't find file: " + name);
    }
    return img;
  }
}
