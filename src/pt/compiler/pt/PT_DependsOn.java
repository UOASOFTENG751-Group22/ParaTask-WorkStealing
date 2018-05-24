/* 
 *  Copyright (C) 2010 Nasser Giacaman, Oliver Sinnen
 *
 *  This file is part of Parallel Task. 
 * 
 *  Parallel Task is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at 
 *  your option) any later version.
 *
 *  Parallel Task is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 *  Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along 
 *  with Parallel Task. If not, see <http://www.gnu.org/licenses/>.
 */

package pt.compiler.pt;

import java.util.Iterator;
import java.util.List;

public class PT_DependsOn {
	
	private List deps = null;
	
	public void addDeps(List deps) {
		this.deps = deps;
	}
	
	public Iterator depsIterator() {
		return deps.iterator();
	}
	
	public String getDepList() {
		if (deps == null)
			return null;
		String ret = "";
		for (Iterator it = deps.iterator(); it.hasNext(); ) {
			ret += it.next();
			if (it.hasNext())
				ret += ", ";
		}
		return ret;
	}
}
