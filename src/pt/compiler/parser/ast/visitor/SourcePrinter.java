/* 
 *  Copyright (C) 2010 Nasser Giacaman, Oliver Sinnen
 *
 *  This file is part of Parallel Task. 
 * 
 *  Parallel Task has been developed based on the Java 1.5 parser and
 *  Abstract Syntax Tree as a foundation. This file is part of the original
 *  Java 1.5 parser and Abstract Syntax Tree code, but has been extended
 *  to support features necessary for Parallel Task. Below is the original
 *  Java 1.5 parser and Abstract Syntax Tree license. 
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


/*
 * Copyright (C) 2007 Jlio Vilmar Gesser.
 * 
 * This file is part of Java 1.5 parser and Abstract Syntax Tree.
 *
 * Java 1.5 parser and Abstract Syntax Tree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java 1.5 parser and Abstract Syntax Tree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Java 1.5 parser and Abstract Syntax Tree.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 08/10/2006
 */
package pt.compiler.parser.ast.visitor;

/**
 * @author Nasser Giacaman 			(Author for ParaTask additions)
 * @author Julio Vilmar Gesser		(Author of initial Java parser)
 */

public final class SourcePrinter {

    private int level = 0;
    private int lastUserLine = -1;
    
    private boolean indented = false;

    private final StringBuilder buf = new StringBuilder();

    public void indent() {
        level++;
    }

    public void unindent() {
        level--;
    }

    private void makeIndent() {
        for (int i = 0; i < level; i++) {
            buf.append("    ");
        }
    }

    public void print(String arg) {
        if (!indented) {
            makeIndent();
            indented = true;
        }
        buf.append(arg);
    }
    
    //NOTE might eventually make this private, to force a line number to be specified 
    @Deprecated
    public void printLn() {
        buf.append("\n");
        indented = false;
    }
    
    //NOTE might eventually remove this method, to force a line number to be specified
    @Deprecated
    public void printLn(String arg) {
        print(arg);
        printLn();
    }
    
    public void setLastUserLine(int lastUserLine) {
    	this.lastUserLine = lastUserLine;
    }
    
    public void printLn(int l) {
    	//-- uses the last line number printed ...
    	int line = lastUserLine;
    	
    	//-- ... unless the line number specified is a valid line number 
    	if (l > 0)
    		line = l;
    	
    	//-- ... but opts for the larger line number
    	if (lastUserLine > l)
    		line = lastUserLine;
    	
		lastUserLine = line;
		buf.append("//####["+lastUserLine+"]####");
		printLn();
    }

    public void printLn(String arg, int line) {
    	print(arg);
    	printLn(line);
    }

    public String getSource() {
        return buf.toString();
    }

    @Override
    public String toString() {
        return getSource();
    }
}