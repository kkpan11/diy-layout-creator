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
 * 
 */
package org.diylc.netlist;

import java.util.List;
import java.util.Map;

import org.diylc.core.IDIYComponent;

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
 * 
 */
public interface INetlistParser {

  String getName();

  String getFileExt();

  String getItemRegex();

  List<Class<?>> getComponentTypeCandidates(String type, Map<String, Object> additionalOutputs);

  List<String> getPropertyNames();

  Object parseValue(String type, String propertyName, String valueStr);

  List<ParsedNetlistEntry> parseFile(String fileName, List<String> outputWarnings)
      throws NetlistParseException;

  List<ParsedNetlistEntry> parse(String content, List<String> outputWarnings)
      throws NetlistParseException;

  List<IDIYComponent<?>> generateComponents(List<ParsedNetlistComponent> components,
      List<String> outputWarnings);
}
