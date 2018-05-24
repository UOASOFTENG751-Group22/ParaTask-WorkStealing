/*
 *  Copyright (C) 2010 Jonathan Chow, Nasser Giacaman, Oliver Sinnen
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

package pt.compiler.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.compiler.parser.ast.type.PrimitiveType;
import pt.compiler.parser.ast.type.Type;
import pt.compiler.parser.ast.type.VoidType;
import pt.compiler.parser.ast.visitor.SourcePrinter;


public class SourceHelper {

	/**
	 * Combines an iterable set of elements into a string with each element
	 * separated by the specified separator.
	 */
	public static <T> String join(Iterable<T> iterable, String separator, Func<String, T> map) {
		if (iterable == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		String printedSeparator = "";
		for (T element : iterable) {
			sb.append(printedSeparator);
			sb.append(map.map(element));
			printedSeparator = separator;
		}
		return sb.toString();
	}
	
	/**
	 * Combines an iterable set of elements into a string with each element
	 * separated by the string ", ".
	 */
	public static <T> String join(Iterable<T> iterable) {
		return join(iterable, ", ", new Func<String, T>() { public String map(T e) { return e.toString(); } });
	}
	
	/**
	 * Returns 'type' genericised with type 'param' as a string.
	 */
	public static String makeGeneric(String type, String param) {
		return String.format("%s<%s>", type, param);
	}
	
	/**
	 * Returns the given type as a boxed type if possible.
	 */
	public static String makeBoxedIfPrimitive(Type type) {
    	if (type instanceof VoidType) {
    		return "Void";
    	} else if (type instanceof PrimitiveType) {
    		return ((PrimitiveType)type).getType().name();
		} else {
			return type.toString();
		}
	}
	
	/**
	 * Completely unreliable way to strip generic parameters from the name of a type.
	 */
	public static String stripGenerics(String type) {
		return type.contains("<") ? type.substring(0, type.indexOf("<")) : type; 
	}
	
	/**
	 * Returns a list of all indexes of 'array' that equal 'match'.
	 */
	public static <T> List<Integer> findMatchingIndexes(T[] array, T match) {
		List<Integer> output = new ArrayList<Integer>();
		if (array == null)
			return output;
		
		for (int i = 0; i < array.length; i++) {
			if (array[i] != null && array[i].equals(match))
				output.add(i);
		}
		return output;
	}
	
	/**
	 * Prints code such that lines after a line ending with '{' are indented
	 * by one level and lines beginning with '}' are unindented by one level.
	 */
	public static void printFormattedCode(SourcePrinter printer, String[] lines) {
    	for (String line : lines) {
    		if (line.startsWith("}")) printer.unindent();
    		printer.printLn(line, -1);
    		if (line.endsWith("{")) printer.indent();
    	}
	}
}
