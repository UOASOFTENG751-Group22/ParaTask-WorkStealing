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

import pt.compiler.parser.ast.expr.Expression;

public class NotifyArg {

	private Expression instance;
	private Expression slot;
	private boolean staticSlot;
	private boolean isIntermediateNotify = false;
	
	public NotifyArg(Expression instance, Expression slot, boolean staticSlot) {
		this.instance = instance;
		this.slot = slot;
		this.staticSlot = staticSlot;
	}

	public Expression getInstance() {
		return instance;
	}

	public Expression getSlot() {
		return slot;
	}

	public boolean isStaticSlot() {
		return staticSlot;
	}
	
	public boolean getIsIntermediateNotify() {
		return isIntermediateNotify;
	}

	public void setInterNotify(boolean intermediate) {
		this.isIntermediateNotify = intermediate;
	}
}
