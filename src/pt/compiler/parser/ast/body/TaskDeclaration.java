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

package pt.compiler.parser.ast.body;

import pt.compiler.parser.ast.visitor.GenericVisitor;
import pt.compiler.parser.ast.visitor.VoidVisitor;

public class TaskDeclaration extends BodyDeclaration {

	private int modifiers;
	private MethodDeclaration method;
	private String multiTaskSize;
	private boolean isInteractive;
	private String smartCutoff;
	
	public TaskDeclaration(int line, int column, JavadocComment javaDoc, int modifiers, MethodDeclaration method, String multiTaskSize, boolean isInteractive, String smartCutoff) {
		super(line, column, javaDoc);
		this.modifiers = modifiers;
		this.method = method;
		this.multiTaskSize = multiTaskSize;
		this.isInteractive = isInteractive;
		this.smartCutoff = smartCutoff;
	}
	
	public String getMultiTaskSize() {
		return multiTaskSize;
	}
	
    public int getModifiers() {
        return modifiers;
    }
    
    public boolean isInteractive() {
    	return isInteractive;
    }
    
    public String getSmartCutoff() {
    	try {
    		int sc = Integer.parseInt(smartCutoff);
    		if (sc <= -1)
    			return "0";
    		else
    			return smartCutoff;
    	} catch (NumberFormatException e) {
    		return smartCutoff;		//-- a variable name (not an integer literal)
    	}
    }
    
    public boolean isSmart() {
    	return !smartCutoff.equals("-1");
    }
    
    public MethodDeclaration getMethodDeclaration() {
    	return method;
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
