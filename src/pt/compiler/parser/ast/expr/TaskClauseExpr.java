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

package pt.compiler.parser.ast.expr;

import pt.compiler.parser.ast.visitor.GenericVisitor;
import pt.compiler.parser.ast.visitor.VoidVisitor;
import pt.compiler.pt.PT_DependsOn;
import pt.compiler.pt.PT_Handler;
import pt.compiler.pt.PT_Notify;

public class TaskClauseExpr extends Expression {
	
	private Expression expression;
	private PT_DependsOn dependences;
	private PT_Notify notifyList;
	private PT_Notify notifyInterList;
	private PT_Handler exceptionHandlerList;

	public TaskClauseExpr(int line, int column, Expression expression, PT_DependsOn dependences, PT_Notify notifyList, PT_Notify notifyInterList, PT_Handler exceptionHandlerList) {
		super(line, column);
		this.expression = expression;
		this.dependences = dependences;
		this.notifyList = notifyList;
		this.notifyInterList = notifyInterList;
		this.exceptionHandlerList = exceptionHandlerList;
	}
	
	public Expression getExpression() {
		return expression;
	}
	
	public PT_DependsOn getDependences() {
		return dependences;
	}
	
	public PT_Notify getNotifyList() {
		return notifyList;
	}
	
	public PT_Notify getNotifyInterList() {
		return notifyInterList;
	}

	public PT_Handler getExceptionHandlerList() {
		return exceptionHandlerList;
	} 
	
    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }
}
