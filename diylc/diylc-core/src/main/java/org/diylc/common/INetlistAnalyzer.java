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
package org.diylc.common;

import java.util.List;
import java.util.Set;

import org.diylc.netlist.Netlist;
import org.diylc.netlist.Node;
import org.diylc.netlist.Summary;
import org.diylc.netlist.TreeException;

public interface INetlistAnalyzer {

  /**   
   * @return summarizer name
   */
  String getName();
  
  /**   
   * @return summarizer short name
   */
  String getShortName();
  
  /**   
   * @return name of the icon to use
   */
  String getIconName();  
  
  /**
   * Summarizes all {@link Netlist}s provided. 
   * 
   * @param netlists
   * @return
   * @throws TreeException 
   */
  List<Summary> summarize(List<Netlist> netlists) throws TreeException;
  
  /**   
   * @return name of the font to be used for display.
   */
  String getFontName();

  /**
   * @return ability to process netlists with or without switches included.
   */
  Set<NetlistSwitchPreference> getSwitchPreference();
}
