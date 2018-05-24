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

package pt.compiler;

import pt.compiler.parser.JavaParser;
import pt.compiler.parser.ParseException;
import pt.compiler.parser.ast.CompilationUnit;
import pt.compiler.parser.ast.visitor.TaskVisitor;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * The source-to-source ParaTask compiler. Generates Java code (*.java) from ParaTask code (*.ptjava). 
 * The resulting code may then be compiled using a standard Java compiler.
 * 
 * @author Nasser Giacaman
 * @author Oliver Sinnen
 *
 */
public class ParaTaskParser {

	static int count = 0;
	
	ParaTaskParser() {
	}
	
	/**
	 * Parse the ParaTask input into standard Java code. 
	 *    
	 * Given a starting file (either a *.ptjava file or a folder), recursively generate standard Java code 
	 * (i.e. the *.java) for every *.ptjava file found.  
	 * 
	 * @param file		A *.ptjava file, or folder containing (more folders and) *.ptjava files.  
	 * @throws IOException
	 */
	public static void parse(File file) throws IOException {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++)
				parse(files[i]);
		}
		
		if (!file.getName().endsWith(".ptjava"))
			return;
		
		count++;
		
		
		FileInputStream is = new FileInputStream(file);
		try {
			CompilationUnit cu = JavaParser.parse(is);
			
			TaskVisitor visitor = new TaskVisitor();
			visitor.createNewSourcePrinter();
	        cu.accept(visitor, null);
	        
	        String fileContent = visitor.getSource();
//			System.out.println(fileContent);
			
			String outputFileName = file.getName().substring(0,file.getName().lastIndexOf("."))+".java";
//			System.out.println(outputFileName);
//			System.out.println(file.getAbsolutePath());
//			System.out.println(file.getAbsoluteFile().getParentFile());
			
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(file.getAbsoluteFile().getParentFile(),outputFileName)));
	        out.write(fileContent);
	        out.close();
			
			if (visitor.paraTaskFoundErrors())
				System.err.println("ParaTask found errors with "+file.getName()+", please check the output.");
			else
				System.out.println("Successfully parsed file:  "+file.getName());
			
		} catch (ParseException pe) {
			System.err.println("********* Failed to parse " + file.getName());
			pe.printStackTrace();
		}
	}
	
	/**
	 * A command-line option to use the ParaTask compiler. Must supply one file name to parse. 
	 * 
	 * @param args	The file name
	 */
	public static void main(String[] args) {
		if( args.length != 1 )
		{
			System.err.println("Must specify ONE file to parse in command line arguments");
			return;
		}
		File start = new File(args[0]);
		try {
			parse(start);
		} catch(IOException e) {
			System.err.println("Problem opening file: "+start.getName());
		}
	}
}
