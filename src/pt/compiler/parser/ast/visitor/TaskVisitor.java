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
 * Created on 05/10/2006
 */
package pt.compiler.parser.ast.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import pt.compiler.helper.Func;
import pt.compiler.helper.Permutation;
import pt.compiler.helper.SourceHelper;
import pt.compiler.parser.ast.BlockComment;
import pt.compiler.parser.ast.CompilationUnit;
import pt.compiler.parser.ast.ImportDeclaration;
import pt.compiler.parser.ast.LineComment;
import pt.compiler.parser.ast.Node;
import pt.compiler.parser.ast.PackageDeclaration;
import pt.compiler.parser.ast.TypeParameter;
import pt.compiler.parser.ast.body.AnnotationDeclaration;
import pt.compiler.parser.ast.body.AnnotationMemberDeclaration;
import pt.compiler.parser.ast.body.BodyDeclaration;
import pt.compiler.parser.ast.body.ClassOrInterfaceDeclaration;
import pt.compiler.parser.ast.body.ConstructorDeclaration;
import pt.compiler.parser.ast.body.EmptyMemberDeclaration;
import pt.compiler.parser.ast.body.EmptyTypeDeclaration;
import pt.compiler.parser.ast.body.EnumConstantDeclaration;
import pt.compiler.parser.ast.body.EnumDeclaration;
import pt.compiler.parser.ast.body.FieldDeclaration;
import pt.compiler.parser.ast.body.InitializerDeclaration;
import pt.compiler.parser.ast.body.JavadocComment;
import pt.compiler.parser.ast.body.MethodDeclaration;
import pt.compiler.parser.ast.body.ModifierSet;
import pt.compiler.parser.ast.body.Parameter;
import pt.compiler.parser.ast.body.TaskDeclaration;
import pt.compiler.parser.ast.body.TypeDeclaration;
import pt.compiler.parser.ast.body.VariableDeclarator;
import pt.compiler.parser.ast.body.VariableDeclaratorId;
import pt.compiler.parser.ast.expr.AnnotationExpr;
import pt.compiler.parser.ast.expr.ArrayAccessExpr;
import pt.compiler.parser.ast.expr.ArrayCreationExpr;
import pt.compiler.parser.ast.expr.ArrayInitializerExpr;
import pt.compiler.parser.ast.expr.AssignExpr;
import pt.compiler.parser.ast.expr.BinaryExpr;
import pt.compiler.parser.ast.expr.BooleanLiteralExpr;
import pt.compiler.parser.ast.expr.CastExpr;
import pt.compiler.parser.ast.expr.CharLiteralExpr;
import pt.compiler.parser.ast.expr.ClassExpr;
import pt.compiler.parser.ast.expr.ConditionalExpr;
import pt.compiler.parser.ast.expr.DoubleLiteralExpr;
import pt.compiler.parser.ast.expr.EnclosedExpr;
import pt.compiler.parser.ast.expr.Expression;
import pt.compiler.parser.ast.expr.FieldAccessExpr;
import pt.compiler.parser.ast.expr.InstanceOfExpr;
import pt.compiler.parser.ast.expr.IntegerLiteralExpr;
import pt.compiler.parser.ast.expr.IntegerLiteralMinValueExpr;
import pt.compiler.parser.ast.expr.LongLiteralExpr;
import pt.compiler.parser.ast.expr.LongLiteralMinValueExpr;
import pt.compiler.parser.ast.expr.MarkerAnnotationExpr;
import pt.compiler.parser.ast.expr.MemberValuePair;
import pt.compiler.parser.ast.expr.MethodCallExpr;
import pt.compiler.parser.ast.expr.NameExpr;
import pt.compiler.parser.ast.expr.NormalAnnotationExpr;
import pt.compiler.parser.ast.expr.NullLiteralExpr;
import pt.compiler.parser.ast.expr.ObjectCreationExpr;
import pt.compiler.parser.ast.expr.QualifiedNameExpr;
import pt.compiler.parser.ast.expr.SingleMemberAnnotationExpr;
import pt.compiler.parser.ast.expr.StringLiteralExpr;
import pt.compiler.parser.ast.expr.SuperExpr;
import pt.compiler.parser.ast.expr.SuperMemberAccessExpr;
import pt.compiler.parser.ast.expr.TaskClauseExpr;
import pt.compiler.parser.ast.expr.ThisExpr;
import pt.compiler.parser.ast.expr.UnaryExpr;
import pt.compiler.parser.ast.expr.VariableDeclarationExpr;
import pt.compiler.parser.ast.stmt.AssertStmt;
import pt.compiler.parser.ast.stmt.BlockStmt;
import pt.compiler.parser.ast.stmt.BreakStmt;
import pt.compiler.parser.ast.stmt.CatchClause;
import pt.compiler.parser.ast.stmt.ContinueStmt;
import pt.compiler.parser.ast.stmt.DoStmt;
import pt.compiler.parser.ast.stmt.EmptyStmt;
import pt.compiler.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import pt.compiler.parser.ast.stmt.ExpressionStmt;
import pt.compiler.parser.ast.stmt.ForStmt;
import pt.compiler.parser.ast.stmt.ForeachStmt;
import pt.compiler.parser.ast.stmt.IfStmt;
import pt.compiler.parser.ast.stmt.LabeledStmt;
import pt.compiler.parser.ast.stmt.ReturnStmt;
import pt.compiler.parser.ast.stmt.Statement;
import pt.compiler.parser.ast.stmt.SwitchEntryStmt;
import pt.compiler.parser.ast.stmt.SwitchStmt;
import pt.compiler.parser.ast.stmt.SynchronizedStmt;
import pt.compiler.parser.ast.stmt.ThrowStmt;
import pt.compiler.parser.ast.stmt.TryStmt;
import pt.compiler.parser.ast.stmt.TypeDeclarationStmt;
import pt.compiler.parser.ast.stmt.WhileStmt;
import pt.compiler.parser.ast.type.ClassOrInterfaceType;
import pt.compiler.parser.ast.type.PrimitiveType;
import pt.compiler.parser.ast.type.ReferenceType;
import pt.compiler.parser.ast.type.Type;
import pt.compiler.parser.ast.type.VoidType;
import pt.compiler.parser.ast.type.WildcardType;
import pt.compiler.pt.HandlerArg;
import pt.compiler.pt.NotifyArg;
import pt.compiler.pt.PT_DependsOn;
import pt.compiler.pt.PT_Handler;
import pt.compiler.pt.PT_Notify;

/**
 * @author Nasser Giacaman 			(Author for ParaTask additions)
 * @author Julio Vilmar Gesser		(Author of initial Java parser)
 */

public final class TaskVisitor implements VoidVisitor<Object> {
	
	public static final String PT_PREFIX = "__pt__";
	
    private static SourcePrinter printer = new SourcePrinter();
    
    private static MethodDeclaration currentMethod = null;
    private static Stack<ClassOrInterfaceDeclaration> currentClassStack = new Stack<ClassOrInterfaceDeclaration>();
    private static String packageName = "";
    
    private static Stack<Boolean> alreadyPrinted_isEDT = new Stack<Boolean>();
    
    private static final int EXCEPTION = 1;
    private static final int NOTIFY = 2;
    private static final int NOTIFY_INTER = 3;
    
    private static boolean paraTaskFoundError = false;

    private static String dummyTaskID = "ParaTaskHelper.dummyTaskID";
    
    public boolean paraTaskFoundErrors() {
    	return paraTaskFoundError;
    }
    
    public void createNewSourcePrinter() {
    	printer = new SourcePrinter();
    }
    
    public void visit(TaskClauseExpr tc, Object arg) {
    	Expression exp = tc.getExpression();
    	exp.accept(this, arg);
    }
    
    //-- return true if there was an exception clause used
    private boolean printTaskClauseExpr(TaskClauseExpr tce, String id, boolean createDeclaration) {
    	
    	if (tce == null)
    		return false;
    	else
    		printer.printLn("TaskInfo "+ id + " = new TaskInfo();", tce.getBeginLine());
    	
		//-- Dependence clause
		PT_DependsOn deps = tce.getDependences();
		if (deps != null) {
			printer.printLn(tce.getBeginLine());
			printer.printLn("/*  -- ParaTask dependsOn clause for '"+id.substring(PT_PREFIX.length())+"' -- */",-1);
			
			Iterator it = deps.depsIterator();
			while (it.hasNext()) {
				Node n = (Node)it.next();
				printer.printLn(id+ ".addDependsOn(" + n + ");",n.getBeginLine());
			}
		}

		//-- Notify clause
		PT_Notify ptn = tce.getNotifyList();

		//-- Notify clause
		PT_Notify ptin = tce.getNotifyInterList();
		
		//-- Exception handler clause
		PT_Handler pth = tce.getExceptionHandlerList();
		
		if (!alreadyPrinted_isEDT.peek() && (ptn != null || pth != null || ptin != null)) {
			// only print this if using a notify or trycatch clause
	    	printer.printLn(-1);
			printer.printLn("boolean isEDT = GuiThread.isEventDispatchThread();",-1);
			alreadyPrinted_isEDT.pop();
			alreadyPrinted_isEDT.push(true);
		} else {
		}
		
    	printer.printLn(-1);
		
		//-- Notify clause
		if (ptn != null) {
			printer.printLn(-1);
			printer.printLn("/*  -- ParaTask notify clause for '"+id.substring(PT_PREFIX.length())+"' -- */",-1);
			printSlotCreation(ptn.getNotifyList(), id, NOTIFY,tce);
		}
		
		//-- NotifyIntermediate clause
		if (ptin != null) {
			printer.printLn(-1);
			printer.printLn("/*  -- ParaTask notify-intermediate clause for '"+id.substring(PT_PREFIX.length())+"' -- */",-1);
			printSlotCreation(ptin.getNotifyList(), id, NOTIFY_INTER,tce);
		}
		
		//-- Exception handler clause
		if (pth != null) {
			printer.printLn(-1);
			printer.printLn("/*  -- ParaTask trycatch clause for '"+id.substring(PT_PREFIX.length())+"' -- */",-1);
			printSlotCreation(pth.getNotifyList(), id, EXCEPTION, tce);
		}
		return pth != null;
    }
    
    private void printSlotCreation(List argList, String id, int type, TaskClauseExpr n) {

		ClassOrInterfaceDeclaration topMostClass = currentClassStack.elementAt(0);

    	boolean insideStaticMethod = false;
    	if (currentMethod != null) {
    		//-- the currentMethod is null if inside a constructor
    		insideStaticMethod = ModifierSet.isStatic(currentMethod.getModifiers());
    	}
		
		//-- get first part of the variable name for the slots (will append a counter at the end of each one later)
    	String methodVarName = "<UNKNOWN>";
    	if (type == NOTIFY)
    		methodVarName = id+"_slot_";
    	else if (type == EXCEPTION)
    		methodVarName = id+"_handler_";
    	else if (type == NOTIFY_INTER)
    		methodVarName = id+"_inter_slot_";
    	
		//-- declare the slot/exceptionHandler as a Method
//    	for (int i = 0; i < argList.size(); i++) {
//			printer.printLn("Method "+methodVarName+i+" = null;");
//    	}
    	
    	//-- assign the Method variables
		printer.printLn("try {",-1);
		printer.indent();
		for (int i = 0; i < argList.size(); i++) {
			printer.printLn("Method "+methodVarName+i+" = null;",-1);
			
			Expression inst = null;
			String instance = "";
			String completeSlot = "";
			NotifyArg slotNotifyArg = null;
			boolean isStaticSlot = false;  //-- not to be confused with "insideStaticMethod"!!!
			
			if (type == NOTIFY) {
				slotNotifyArg = (NotifyArg) argList.get(i);
			} else if (type == EXCEPTION) {
				HandlerArg handArg = (HandlerArg) argList.get(i);
				slotNotifyArg = handArg.getNotifyHandler();
			} else if (type == NOTIFY_INTER) {
				slotNotifyArg = (NotifyArg) argList.get(i);
			} else {
				System.err.println("<UNKNOWN type>");
			}
			
			inst = slotNotifyArg.getInstance();
			if (inst == null)
				instance = "";
			else
				instance = slotNotifyArg.getInstance().toString();
			
			completeSlot = slotNotifyArg.getSlot().toString();
			isStaticSlot = slotNotifyArg.isStaticSlot();
			
			if (insideStaticMethod && inst == null) {
				//-- this implies the slot being called is static (cannot call a non-static method from a static method)
				
				if (!packageName.equals(""))
					instance = packageName+"."+topMostClass.getName();
				else
					instance = topMostClass.getName();
				
				isStaticSlot = true; 

			}
			
			//-- static methods only exist in top-level class TODO this is not completely true, static methods allowed in static nested classes
			if (isStaticSlot && inst == null) {
				if (!packageName.equals(""))
					instance = packageName+"."+topMostClass.getName();
				else
					instance = topMostClass.getName();
			}
			
			String slotName = completeSlot.substring(0,completeSlot.indexOf('('));
			String slotArgList;
			
			String line = completeSlot.substring(completeSlot.lastIndexOf('(')+1, completeSlot.lastIndexOf(')'));
			List<String > args = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(line,",");
			while (st.hasMoreTokens()) {
				String t = st.nextToken();
				args.add(t.trim());
			}
			
			boolean isTaskIDArg = args.size() == 1 && args.get(0).equals("TaskID");
			boolean isTaskIDGroupArg = args.size() == 1 && args.get(0).equals("TaskIDGroup");

			if (isTaskIDArg)
				slotArgList = "new Class[] { TaskID.class }";
			else if (isTaskIDGroupArg)
				slotArgList = "new Class[] { TaskIDGroup.class }";
			else {
				slotArgList = "new Class[] {";
				
				for (int s = 0; s < args.size(); s++) {
					slotArgList += args.get(s)+".class";
					if (s < (args.size()-1))
						slotArgList+= ", ";
				}
				slotArgList+="}";
			}
			
			String className = instance;

			if (!instance.equals(""))
				className+=".";
			
			if (isStaticSlot)
				className+="class";
			else 
				className+="getClass()";

			printer.printLn(methodVarName+i+" = ParaTaskHelper.getDeclaredMethod("+className+", \""+slotName+"\", " + slotArgList + ");",slotNotifyArg.getSlot().getBeginLine());
				
			for (int d = 0; d < args.size(); d++) {
				String paramType = args.get(d);
				printer.printLn(paramType+" "+methodVarName+i+"_dummy_"+d+" = null;",-1);
			}
			
			if (instance.equals(""))
				printer.print("if (false) "+ slotName+"(");
			else
				printer.print("if (false) "+ instance+"."+slotName+"(");
			
			for (int d = 0; d < args.size(); d++) {
				printer.print(methodVarName+i+"_dummy_"+d);
				if (d < (args.size()-1))
					printer.print(", ");
			}
			
			printer.printLn("); //-- ParaTask uses this dummy statement to ensure the slot exists (otherwise Java compiler will complain)",-1);
			
			inst = null;
			instance = "this";
			String exception = "";
			slotNotifyArg = null;
			
			if (type == NOTIFY) {
				slotNotifyArg = (NotifyArg) argList.get(i);
			} else if (type == EXCEPTION) {
				HandlerArg handArg = (HandlerArg) argList.get(i);
				slotNotifyArg = handArg.getNotifyHandler();
				exception = handArg.getException();
			} else if (type == NOTIFY_INTER) {
				slotNotifyArg = (NotifyArg) argList.get(i);
			} else {
				System.err.println("<UNKNOWN type>");
			}
			inst = slotNotifyArg.getInstance();
			
			if (inst != null && !slotNotifyArg.isStaticSlot())
				instance = inst.toString();
			
			if (insideStaticMethod && inst==null)
				instance = "null";
			
			if (type == NOTIFY) {
				printer.printLn(id+ ".addSlotToNotify(new Slot("+methodVarName+i+", "+ instance +", false));",-1);
			} else if (type == EXCEPTION) {
				printer.printLn(id+ ".addExceptionHandler("+exception+".class, new Slot("+methodVarName+i+", "+ instance +", false));",-1);
			} else if (type == NOTIFY_INTER) {
				printer.printLn(id+ ".addInterSlotToNotify(new Slot("+methodVarName+i+", "+ instance +", true));",-1);
			} else {
				System.err.println("<UNKNOWN type>");
			}
			printer.printLn(-1);
		}
		
		printer.unindent();
		printer.printLn("} catch(Exception "+PT_PREFIX+"e) { ",-1);
		printer.indent();
		printer.printLn("System.err.println(\"Problem registering method in clause:\");",-1);
		printer.printLn(PT_PREFIX+"e.printStackTrace();",-1);
		printer.unindent();
		printer.printLn("}",-1);
    }
    
    public void visit(VariableDeclarationExpr n, Object arg) {
    	//-- TODO  Should fix this, shouldn't be using 'startsWith' since users might have other classes that start with this too 
    	//-- should really tokenize the name, remove the "<>" (if any) and see if equals "TaskID" || "TaskIDGroup"
    	//-- similarly, won't work if the users specify the package name of TaskID
    	boolean isTaskID = n.getType().toString().startsWith("TaskID");
    	boolean usesTryCatchClause = false;
    	
    	//-- a TaskInfo is created for the TaskID instance since it might use one of the ParaTask clauses 
    	if (isTaskID) {
        	for (int i = 0; i < n.getVars().size(); i++) {
				VariableDeclarator v = (VariableDeclarator) n.getVars().get(i);
				VariableDeclaratorId id = v.getId();
				
				Expression exp = v.getInit();
				if (exp instanceof TaskClauseExpr) {
					usesTryCatchClause |= printTaskClauseExpr((TaskClauseExpr) exp, PT_PREFIX+id, true);
				} else {
					//-- Does not use any ParaTask clauses, but still create a TaskInfo in case the same TaskID is re-used later with a ParaTask clause
					printTaskClauseExpr(null, PT_PREFIX+id, true);
				}
        	}
        }
    	
    	if (isTaskID && usesTryCatchClause) {
    		//-- A trycatch clause is being used, therefore need to surround task invocation with a try/catch clause to quiet the Java compiler
    		
    		//-- declare the TaskID variables separately..
    		for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {

        		printAnnotations(n.getAnnotations(), arg);
                printModifiers(n.getModifiers());

				n.getType().accept(this, arg);
				printer.print(" ");
				
    			VariableDeclarator v = i.next();
				VariableDeclaratorId id = v.getId();
				
				Expression exp = v.getInit();
				if (exp instanceof TaskClauseExpr) {
					TaskClauseExpr tce = (TaskClauseExpr) exp;
					if (tce.getExceptionHandlerList() != null) {
				    	id.accept(this, arg);
				    	int l = -1;
				    	try {
				    		l = ((HandlerArg)tce.getExceptionHandlerList().getNotifyList().get(0)).getNotifyHandler().getSlot().getBeginLine();
				    	} catch (Exception e) {
				    	}
						printer.printLn(" = null;",l);
						printer.printLn("try {",l);
						printer.indent();
						v.accept(this, arg);
						printer.printLn(";",l);
						printer.unindent();
						printDummyTryCatch(tce.getExceptionHandlerList().getNotifyList());
					} else {
						v.accept(this, arg);
					}
				} else {
					v.accept(this, arg);
				}
				printer.printLn(";",-1);
    		}
    	} else {
    		//-- No need to surround with try/catch block
    		
    		printAnnotations(n.getAnnotations(), arg);
            printModifiers(n.getModifiers());
            
            n.getType().accept(this, arg);
            printer.print(" ");
            
            for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
                VariableDeclarator v = i.next();
                v.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
    	}
    }
    
    private void printDummyTryCatch(List notifyList) {
    	for (Iterator<HandlerArg> it = notifyList.iterator(); it.hasNext(); ) {
			HandlerArg hand = it.next();
			printer.printLn("} catch("+hand.getException()+" "+PT_PREFIX+"e) { ",-1);
			printer.indent();
			printer.printLn("/*   This is a dummy try/catch to quiet the Java compiler. If '"+hand.getException()+"' ",-1);
			printer.printLn(" *   occurs, this is properly handled by the ParaTask runtime. */",-1);
			printer.unindent();
		}
		printer.printLn("}",-1);		
	}

	public void visit(VariableDeclarator n, Object arg) {
        n.getId().accept(this, arg);
        if (n.getInit() != null) {
            printer.print(" = ");
            Expression exp = n.getInit();
            
            if (exp instanceof TaskClauseExpr) {
                n.getInit().accept(this, PT_PREFIX+n.getId());
            } else {
            	n.getInit().accept(this, arg);
            }
        }
    }
	
    private void printSignature(TaskDeclaration task, boolean isTaskMethod, boolean withTaskInfoArg, String[] paramTypes, Object arg) {
    	//-- convenience variables
    	MethodDeclaration method = task.getMethodDeclaration();
    	boolean isMultiTask = !task.getMultiTaskSize().equals("-");
    	
    	//-- javadoc
    	if (method.getJavaDoc() != null) {
            method.getJavaDoc().accept(this, arg);
        }
    	
    	//-- annotations
        printMemberAnnotations(method.getAnnotations(), arg);
        
        //-- modifiers
        //-- convert user code to public (the programmer will still see the TASK signature as private/protected) 
        if (!isTaskMethod) {
        	int newModifiers = ModifierSet.removeModifier(method.getModifiers(), ModifierSet.PRIVATE);
        	newModifiers = ModifierSet.removeModifier(newModifiers, ModifierSet.PROTECTED);
        	newModifiers = ModifierSet.addModifier(newModifiers, ModifierSet.PUBLIC);
        	printModifiers(newModifiers);
        } else {
        	printModifiers(method.getModifiers());
        }
        
        //-- generic types
        printTypeParameters(method.getTypeParameters(), arg);
        
        //-- return type and method name
        Type type = method.getType();
        if (isTaskMethod) {
        	printer.print(SourceHelper.makeGeneric(isMultiTask ? "TaskIDGroup" : "TaskID",
        			SourceHelper.makeBoxedIfPrimitive(type)));
            printer.print(" " + method.getName());
        } else { 
            type.accept(this, arg);
            printer.print(" " + PT_PREFIX + method.getName());
        }
        
        //-- parameters with overrides based on paramTypes
        printer.print("(");
        List<Parameter> params = method.getParameters();
        if (params != null) {
        	// print original types if paramTypes is null
            if (paramTypes == null)
            	paramTypes = new String[params.size()];
            
            // print each parameter with the specified override type
	        for (int i = 0; i < paramTypes.length; i++) {
	        	if (i > 0)
	        		printer.print(", ");
	        	
	        	Parameter p = params.get(i);
	        	
	        	String override = paramTypes[i];
	        	if (override != null && (override.equals("TaskID") || override.equals("BlockingQueue")))
	        		override = SourceHelper.makeGeneric(override, SourceHelper.makeBoxedIfPrimitive(p.getType()));
	        	printParameter(p, override, arg);
	        }
        }
        if (withTaskInfoArg) {
        	if (params != null)
        		printer.print(", ");
        	printer.print("TaskInfo taskinfo");
        }
        printer.print(")");
        
        //-- trailing array syntax
        for (int i = 0; i < method.getArrayCount(); i++) {
            printer.print("[]");
        }

        //-- exceptions
        if (method.getThrows() != null) {
            printer.print(" throws ");
            printer.print(SourceHelper.join(method.getThrows()));
        }
    }
    
    private void printSynchronousCheckCode(MethodDeclaration method, String cutoff) {
        printer.printLn("if ((Thread.currentThread() instanceof WorkerThread) && TaskpoolFactory.getTaskpool().executeSynchronously("+cutoff+")) {", -1);
        
        printer.indent();
        printer.printLn("//-- the task will be executed synchronously by the current worker thread", -1);
        Type type = method.getType();
        printer.printLn("TaskID id = new TaskID(true);", -1);
        if (!(type instanceof VoidType)){
        	printer.print(type.toString()+" result = ");
        }
        
        //-- print direct method invocation
        printer.print(PT_PREFIX+method.getName()+"(");
        if (method.getParameters() != null) {
            for (Iterator<Parameter> i = method.getParameters().iterator(); i.hasNext();) {
                Parameter p = i.next();
                printer.print(""+p.getId());
                if (i.hasNext())
                	printer.print(", ");
            }
            // TODO: should be able to replace this with
            // printer.print(SourceHelper.join(method.getParameters(), ", ", new Func<String, Parameter>() { public String m(Parameter p) { return p.getId().toString(); }}));
        }
        printer.printLn(");", -1);
        
        if (!(type instanceof VoidType)){
        	printer.printLn("id.setReturnResult(result);", -1);
        }
        
        printer.printLn("return id;", -1);
        printer.unindent();
        printer.printLn("}", -1);
        printer.printLn(-1);
    }
    
    private String getParamsWithoutTypes(MethodDeclaration method) {
    	return SourceHelper.join(method.getParameters(), ", ", new Func<String, Parameter>() { 
    		public String map(Parameter p) { 
    			return p.getId().toString(); 
    		}
    	});
    }
    
    private String getMethodUniqueName(MethodDeclaration method) {
    	return PT_PREFIX + method.getName() + "_" + SourceHelper.join(method.getParameters(), "_", new Func<String, Parameter>() {
    		public String map(Parameter p) {
    			return p.getType().toString()
    					.replaceAll("<|>", "")
    					.replace('[', 'A')
    					.replace(']', 'r');
    		}
    	});
    }
    
	private void printOriginalMethod(TaskDeclaration task, Object arg) {
		MethodDeclaration method = task.getMethodDeclaration();
		
		printSignature(task, false, false, null, arg);
        if (method.getBody() == null) {
            printer.printLn(";", method.getEndLine());
        } else {
            printer.print(" ");
            method.getBody().accept(this, arg);
        }
        printer.printLn(method.getBody().getEndLine());
	}
	
	private void printTaskMethodWithoutTaskInfo(TaskDeclaration task, String[] paramTypes, Object arg) {
		MethodDeclaration method = task.getMethodDeclaration();
    	boolean isMultiTask = !task.getMultiTaskSize().equals("-");
    	List<Parameter> params = method.getParameters();
    	
    	//-- we don't need to print this if any of the overrides in paramTypes overlap with the original parameters
    	if (params != null) {
    		for (int i = 0; i < params.size(); i++) {
    			if (params.get(i).getType().toString().startsWith("TaskID") 
    					&& paramTypes[i] != null && paramTypes[i].equals("TaskID"))
    				return;
    		}
    	}
		
    	printSignature(task, true, false, paramTypes, arg);
    	if (method.getBody() == null) {
            printer.printLn(";", method.getEndLine());
        } else {
        	printer.printLn(" {", method.getBody().getBeginLine());
            printer.indent();
            
            if (task.isSmart() && !isMultiTask)
            	printSynchronousCheckCode(method, task.getSmartCutoff());

            // this method is an overload for the version with a TaskInfo parameter
            printer.printLn("//-- execute asynchronously by enqueuing onto the taskpool", -1);
            printer.printLn("return " + method.getName() + "(" + getParamsWithoutTypes(method) 
            		+ (params == null ? "" : ", ") + "new TaskInfo());", -1);
            printer.unindent();
            printer.printLn("}", -1);
        }
	}
	
	private void printTaskMethodWithTaskInfo(TaskDeclaration task, String[] paramTypes, Object arg) {
		MethodDeclaration method = task.getMethodDeclaration();
    	boolean isMultiTask = !task.getMultiTaskSize().equals("-");
    	List<Parameter> params = method.getParameters();
    	
    	//-- we don't need to print this if any of the overrides in paramTypes overlap with the original parameters
    	if (params != null) {
    		for (int i = 0; i < params.size(); i++) {
    			if (params.get(i).getType().toString().startsWith("TaskID") 
    					&& paramTypes[i] != null && paramTypes[i].equals("TaskID"))
    				return;
    		}
    	}
    	
    	printSignature(task, true, true, paramTypes, arg);
    	if (method.getBody() == null) {
            printer.printLn(";", method.getEndLine());
        } else {
        	//-- set some common variables
        	String methodUniqueName = getMethodUniqueName(method);
        	String methodVar = methodUniqueName + "_method";
        	
        	//-- method body opening brace
        	printer.printLn(" {", method.getBody().getBeginLine());
            printer.indent();
            
            //-- code to ensure the Method object is set
            SourceHelper.printFormattedCode(printer,  new String[] {
            	"// ensure Method variable is set",
            	"if (" + methodVar + " == null) {",
            		methodUniqueName + "_ensureMethodVarSet();",
            	"}"
            });
        	
        	//-- test if this is the general version which converts all params to Object type
        	if (paramTypes.length != 0 && paramTypes.length == SourceHelper.findMatchingIndexes(paramTypes, "Object").size()) {
        		//-- must dynamically check at runtime for taskid arguments
        		String taskIdIndexListVar = PT_PREFIX + "taskIdIndexList";
        		String taskIdIndexArrayVar = PT_PREFIX + "taskIdIndexArray";
        		String queueIndexListVar = PT_PREFIX + "queueIndexList";
        		String queueIndexArrayVar = PT_PREFIX + "queueIndexArray";
        		String loopVar = PT_PREFIX + "i";
        		
        		printer.printLn("List<Integer> " + taskIdIndexListVar + " = new ArrayList<Integer>();", -1);
        		printer.printLn("List<Integer> " + queueIndexListVar + " = new ArrayList<Integer>();", -1);
        		
        		for (int i = 0; i < params.size(); i++) {
        			Parameter p = params.get(i);
        			
        			// skip originally BlockingQueue types
        			if (!p.getType().toString().startsWith("BlockingQueue")) {
        				
        				// do a runtime check of the type and add as pipeline input if necessary
        				SourceHelper.printFormattedCode(printer, new String[] {
        					"if (" + p.getId() + " instanceof BlockingQueue) {",
        						queueIndexListVar + ".add(" + i + ");",
        					"}"
        				});
        			}
        			
        			// skip originally TaskID types
        			if (!p.getType().toString().startsWith("TaskID")) {
        			
	        			// do a runtime check of the type and add as dependency if necessary
	        			SourceHelper.printFormattedCode(printer, new String[] {
	    					"if (" + p.getId() + " instanceof TaskID) {",
	    						"taskinfo.addDependsOn((TaskID)" + p.getId() + ");",
	    						taskIdIndexListVar + ".add(" + i + ");",
	    					"}"
	        			});
        			}
        		}
        		
        		// set queue and taskid information
        		SourceHelper.printFormattedCode(printer, new String[] {
        			"int[] " + queueIndexArrayVar + " = new int[" + queueIndexListVar + ".size()];",
        			"for (int " + loopVar + " = 0; " + loopVar + " < " + queueIndexArrayVar + ".length; " + loopVar + "++) {",
    					queueIndexArrayVar + "[" + loopVar + "] = " + queueIndexListVar + ".get(" + loopVar + ");",
    				"}",
    				"taskinfo.setQueueArgIndexes(" + queueIndexArrayVar + ");",
    				"if (" + queueIndexArrayVar + ".length > 0) {",
    					"taskinfo.setIsPipeline(true);",
    				"}",
        			"int[] " + taskIdIndexArrayVar + " = new int[" + taskIdIndexListVar + ".size()];",
        			"for (int " + loopVar + " = 0; " + loopVar + " < " + taskIdIndexArrayVar + ".length; " + loopVar + "++) {",
        				taskIdIndexArrayVar + "[" + loopVar + "] = " + taskIdIndexListVar + ".get(" + loopVar + ");",
        			"}",
        			"taskinfo.setTaskIdArgIndexes(" + taskIdIndexArrayVar + ");"
        		});
        	} else {
        		//-- set static queue arguments
        		List<Integer> queueIndexes = SourceHelper.findMatchingIndexes(paramTypes, "BlockingQueue");
        		if (queueIndexes.size() > 0) {
        			printer.printLn("taskinfo.setQueueArgIndexes(" + SourceHelper.join(queueIndexes) + ");", -1);
        			printer.printLn("taskinfo.setIsPipeline(true);", -1);
        		}
        		
	        	//-- set static taskid arguments
        		// isPipeline is set automatically when addDependsOn() is called
	        	List<Integer> taskIdIndexes = SourceHelper.findMatchingIndexes(paramTypes, "TaskID");
	        	if (taskIdIndexes.size() > 0) {
	        		printer.printLn("taskinfo.setTaskIdArgIndexes(" + SourceHelper.join(taskIdIndexes) + ");", -1);
		        	for (int index : taskIdIndexes) {
		        		printer.printLn("taskinfo.addDependsOn(" + params.get(index).getId() + ");", -1);
		        	}
	        	}
        	}

            //-- set arguments
            printer.printLn("taskinfo.setParameters(" + getParamsWithoutTypes(method) + ");", -1);
            
            //-- set method
            printer.printLn("taskinfo.setMethod(" + methodVar + ");", -1);
            
            //-- set instance to self if the method is not static
            if (!ModifierSet.isStatic(method.getModifiers()))
            	printer.printLn("taskinfo.setInstance(this);", -1);
            
            //-- set flag for IO_TASK
        	if (task.isInteractive())
        		printer.printLn("taskinfo.setInteractive(true);", -1);
            
        	//-- enqueue and return TaskID
        	if (isMultiTask) {
        		printer.print("return TaskpoolFactory.getTaskpool().enqueueMulti(taskinfo, ");
        		printer.print(task.getMultiTaskSize().equals("*") ? "-1" : task.getMultiTaskSize());
        		printer.printLn(");", -1);
        	} else {
        		printer.printLn("return TaskpoolFactory.getTaskpool().enqueue(taskinfo);", -1);
        	}
      
        	//-- method body closing brace
            printer.unindent();
            printer.printLn("}", -1);
        }
	}
    
	public void visit(TaskDeclaration task, Object arg) {
    	MethodDeclaration method = task.getMethodDeclaration();
    	List<Parameter> params = method.getParameters();
    	
    	//-- set flags and global vars
    	currentMethod = method;
    	alreadyPrinted_isEDT.push(false);
    	
    	//-- convenience variable
    	boolean isMultiTask = !task.getMultiTaskSize().equals("-");
    	
    	//-- print variables needed for this task
		String methodUniqueName = getMethodUniqueName(method);
    	String methodVar = methodUniqueName + "_method";
    	//-- TODO  Need to make sure this is only printed once for each method (e.g. when a method is overriden, or an interface implemented)
    	if (!currentClassStack.peek().isInterface()) {
    		// TODO: check if these can be static
    		printer.printLn("private static volatile Method " + methodVar + " = null;", method.getBeginLine());
    	}
    	
    	//-- print method to ensure Method variable set
    	String currentClassVar = "new ParaTaskHelper.ClassGetter().getCurrentClass()";
    	SourceHelper.printFormattedCode(printer, new String[] {
    		"private synchronized static void " + methodUniqueName + "_ensureMethodVarSet() {",
    			"if (" + methodVar + " == null) {",
    				"try {",
						methodVar + " = ParaTaskHelper.getDeclaredMethod(" + currentClassVar + ", \"" + PT_PREFIX + method.getName() + "\", new Class[] {",
    						SourceHelper.join(params, ", ", new Func<String, Parameter>() {
    							public String map(Parameter p) {
    								return SourceHelper.stripGenerics(p.getType().toString()) + ".class";
    							}
    						}),
						"});",
    				"} catch (Exception e) {",
    					"e.printStackTrace();",
    				"}",
    			"}",
    		"}"
        });
    	
    	//-- we need to generate method bodies for different original/taskid/queue types
    	//-- BUT only if the are 3 or less parameters to avoid code bloat
    	final int PERMUTE_THRESHOLD = 3;
    	if (params == null || params.size() <= PERMUTE_THRESHOLD) {
    		Permutation paramTypePermutes = new Permutation(new String[] { null, "TaskID", "BlockingQueue" }, params == null ? 0 : params.size());
        	for (String[] paramTypes : paramTypePermutes) {
    	    	printTaskMethodWithoutTaskInfo(task, paramTypes, arg);
    	    	printTaskMethodWithTaskInfo(task, paramTypes, arg);
        	}	
    	} else {
    		String[] paramTypes = new String[params.size()];
    		for (int i = 0; i < paramTypes.length; i++)
    			paramTypes[i] = "Object";
    		
    		printTaskMethodWithoutTaskInfo(task, paramTypes, arg);
    		printTaskMethodWithTaskInfo(task, paramTypes, arg);
    	}
        
        //-- original user code
        printOriginalMethod(task, arg);
        
        //-- unset flags
        alreadyPrinted_isEDT.pop();
    }
	
    public void visit(MethodCallExpr n, Object arg) {
    	
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printTypeArgs(n.getTypeArgs(), arg);
        printer.print(n.getName());
        printer.print("(");
        if (n.getArgs() != null) {
            for (Iterator<Expression> i = n.getArgs().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, null);		// made arg null so that it is not added to inner methods eg. TaskID id = task(innerMethod(123));
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        
        //-- add in the TaskInfo argument
        if (arg != null) {
        	if (n.getArgs()!=null)
        		printer.print(", ");
        	printer.print(arg.toString());
        }
        printer.print(")");
    }

    public void visit(AssignExpr n, Object arg) {
    	Expression exp = n.getValue();
    	boolean usesTryCatchClause = false;
    	
    	if (exp instanceof TaskClauseExpr) {
    		usesTryCatchClause = printTaskClauseExpr((TaskClauseExpr)exp, PT_PREFIX+n.getTarget(), false);
    	}
    	
    	if (usesTryCatchClause) {
    		printer.printLn(-1);
			printer.printLn("try {",-1);
			printer.indent();
    	}
    	
        n.getTarget().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case assign:
                printer.print("=");
                break;
            case and:
                printer.print("&=");
                break;
            case or:
                printer.print("|=");
                break;
            case xor:
                printer.print("^=");
                break;
            case plus:
                printer.print("+=");
                break;
            case minus:
                printer.print("-=");
                break;
            case rem:
                printer.print("%=");
                break;
            case slash:
                printer.print("/=");
                break;
            case star:
                printer.print("*=");
                break;
            case lShift:
                printer.print("<<=");
                break;
            case rSignedShift:
                printer.print(">>=");
                break;
            case rUnsignedShift:
                printer.print(">>>=");
                break;
        }
        printer.print(" ");
        
        if (exp instanceof TaskClauseExpr) {
        	n.getValue().accept(this, PT_PREFIX+n.getTarget());
    	} else {
            n.getValue().accept(this, arg);
    	}
        
        if (usesTryCatchClause) {
        	printer.printLn(";",-1);
			printer.unindent();
			printDummyTryCatch(((TaskClauseExpr)exp).getExceptionHandlerList().getNotifyList());
        }
    }
    
    //-- Prints method openers, just in case the programmer uses private/protected methods in the notify/exception clauses
    //-- Also prints a helper function to determine the class from with a static method 
    private void printHelperMethods() {
    	
    	//-- Print opener methodmethod
    	printer.printLn(-1);
    	printer.printLn("/*  ParaTask helper method to access private/protected slots */",-1);
    	if (currentClassStack.size()>1 && ModifierSet.isStatic(currentClassStack.peek().getModifiers()))
    		printer.print("static ");
    	printer.print("public void " + PT_PREFIX+ "accessPrivateSlot(Method m, Object instance, TaskID arg, Object interResult ) ");
    	printer.printLn("throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {",-1);
		printer.indent();
		printer.printLn("if (m.getParameterTypes().length == 0)",-1);
		printer.indent();
		printer.printLn("m.invoke(instance);",-1);
		printer.unindent();
		printer.printLn("else if ((m.getParameterTypes().length == 1))",-1);
		printer.indent();
		printer.printLn("m.invoke(instance, arg);",-1);
		printer.unindent();
		printer.printLn("else ",-1);
		printer.indent();
		printer.printLn("m.invoke(instance, arg, interResult);",-1);
		printer.unindent();
		printer.unindent();
		printer.printLn("}",-1);
    }
    
    public String getSource() {
        return printer.getSource();
    }
    
    private void printModifiers(int modifiers) {
        if (ModifierSet.isPrivate(modifiers)) {
            printer.print("private ");
        }
        if (ModifierSet.isProtected(modifiers)) {
            printer.print("protected ");
        }
        if (ModifierSet.isPublic(modifiers)) {
            printer.print("public ");
        }
        if (ModifierSet.isAbstract(modifiers)) {
            printer.print("abstract ");
        }
        if (ModifierSet.isStatic(modifiers)) {
            printer.print("static ");
        }
        if (ModifierSet.isFinal(modifiers)) {
            printer.print("final ");
        }
        if (ModifierSet.isNative(modifiers)) {
            printer.print("native ");
        }
        if (ModifierSet.isStrictfp(modifiers)) {
            printer.print("strictfp ");
        }
        if (ModifierSet.isSynchronized(modifiers)) {
            printer.print("synchronized ");
        }
        if (ModifierSet.isTransient(modifiers)) {
            printer.print("transient ");
        }
        if (ModifierSet.isVolatile(modifiers)) {
            printer.print("volatile ");
        }
    }
    
    private void printMembers(List<BodyDeclaration> members, Object arg) {
        for (BodyDeclaration member : members) {
            printer.printLn(member.getBeginLine());
            member.accept(this, arg);
            printer.printLn(member.getEndLine());
        }
    }

    private void printMemberAnnotations(List<AnnotationExpr> annotations, Object arg) {
        if (annotations != null) {
            for (AnnotationExpr a : annotations) {
                a.accept(this, arg);
                printer.printLn(a.getBeginLine());
            }
        }
    }

    private void printAnnotations(List<AnnotationExpr> annotations, Object arg) {
        if (annotations != null) {
            for (AnnotationExpr a : annotations) {
                a.accept(this, arg);
                printer.print(" ");
            }
        }
    }

    private void printTypeArgs(List<Type> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<Type> i = args.iterator(); i.hasNext();) {
                Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printTypeParameters(List<TypeParameter> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
                TypeParameter t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }
    
    public void visit(Node n, Object arg) {
        throw new IllegalStateException(n.getClass().getName());
    }
    
    public void visit(CompilationUnit n, Object arg) {
    	
//    	int ee = 1;
//    	for (Comment c: n.getComments()) {
//    		printer.printLn();
//    		printer.printLn("// comment "+(ee++));
//    		c.accept(this, arg);
//    	}
//		printer.printLn();
//		printer.printLn();
    	
        if (n.getPakage() != null) {
            n.getPakage().accept(this, arg);
        }
        if (n.getImports() != null) {
            for (ImportDeclaration i : n.getImports()) {
                i.accept(this, arg);
            }
            printer.printLn(-1);
        }
        
    	//-- added this for ParaTask
        printer.printLn("//-- ParaTask related imports",-1);
    	printer.printLn("import pt.runtime.*;",-1);
    	printer.printLn("import java.util.concurrent.ExecutionException;",-1);
    	printer.printLn("import java.util.concurrent.locks.*;",-1);
    	printer.printLn("import java.lang.reflect.*;",-1);
    	printer.printLn("import pt.runtime.GuiThread;",-1);
    	printer.printLn("import java.util.concurrent.BlockingQueue;", -1);
    	printer.printLn("import java.util.ArrayList;", -1);
    	printer.printLn("import java.util.List;", -1);
    	printer.printLn(-1);
    	
        if (n.getTypes() != null) {
            for (Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
            	TypeDeclaration t = i.next(); 
                t.accept(this, arg);
                printer.printLn(-1);
                if (i.hasNext()) {
                    printer.printLn(-1);
                }
            }
        }
        packageName = "";
    }
    
    public void visit(PackageDeclaration n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printer.print("package ");
        n.getName().accept(this, arg);
        
        //-- Added this in for ParaTask
        packageName = n.getName().toString();
        
        printer.printLn(";", n.getBeginLine());
        printer.printLn(-1);
    }

    public void visit(NameExpr n, Object arg) {
        printer.print(n.getName());
    }

    public void visit(QualifiedNameExpr n, Object arg) {
        n.getQualifier().accept(this, arg);
        printer.print(".");
        printer.print(n.getName());
    }

    public void visit(ImportDeclaration n, Object arg) {
        printer.print("import ");
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getName().accept(this, arg);
        if (n.isAsterisk()) {
            printer.print(".*");
        }
        printer.printLn(";", n.getBeginLine());
    }
    
    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
    	
    	//-- ParaTask  added this to determine the name of the current class
    	currentClassStack.push(n);
    	
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        if (n.isInterface()) {
            printer.print("interface ");
        } else {
            printer.print("class ");
        }

        printer.print(n.getName());

        printTypeParameters(n.getTypeParameters(), arg);

        if (n.getExtends() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        
        printer.printLn(" {",n.getBeginLine());
        printer.indent();
        
        if (this.currentClassStack.size() == 1)
        	printer.print("static{ParaTask.init();}");
        
        //-- Note added this for ParaTask... even if no TASKS are declared in here, the normal methods might be used as slots
        //--  e.g.   TaskID id = instance.someTaskInAnotherClass() notify( slotInHere() );
        if (!n.isInterface())
        	printHelperMethods();
        
        //-- Add the Taskpool variable
//        printer.printLn("private static Taskpool taskpool = TaskpoolFactory.getTaskpool();");
		
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        
        printer.unindent();
        printer.print("}");
        
        currentClassStack.pop();
    }
    
    public void visit(EmptyTypeDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(JavadocComment n, Object arg) {
        printer.print("/**");
        printer.print(n.getContent());
        printer.printLn("*/", n.getEndLine());
    }

    public void visit(ClassOrInterfaceType n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printer.print(n.getName());
        printTypeArgs(n.getTypeArgs(), arg);
    }

    public void visit(TypeParameter n, Object arg) {
        printer.print(n.getName());
        if (n.getTypeBound() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(" & ");
                }
            }
        }
    }

    public void visit(PrimitiveType n, Object arg) {
        switch (n.getType()) {
            case Boolean:
                printer.print("boolean");
                break;
            case Byte:
                printer.print("byte");
                break;
            case Char:
                printer.print("char");
                break;
            case Double:
                printer.print("double");
                break;
            case Float:
                printer.print("float");
                break;
            case Integer:
                printer.print("int");
                break;
            case Long:
                printer.print("long");
                break;
            case Short:
                printer.print("short");
                break;
        }
    }

    public void visit(ReferenceType n, Object arg) {
        n.getType().accept(this, arg);
        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }
    }

    public void visit(WildcardType n, Object arg) {
        printer.print("?");
        if (n.getExtends() != null) {
            printer.print(" extends ");
            n.getExtends().accept(this, arg);
        }
        if (n.getSuper() != null) {
            printer.print(" super ");
            n.getSuper().accept(this, arg);
        }
    }

    public void visit(FieldDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());
        n.getType().accept(this, arg);

        printer.print(" ");
        for (Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
            VariableDeclarator var = i.next();
            var.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }

        printer.print(";");
    }
    
    public void visit(VariableDeclaratorId n, Object arg) {
        printer.print(n.getName());
        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }
    }

    public void visit(ArrayInitializerExpr n, Object arg) {
        printer.print("{");
        if (n.getValues() != null) {
            printer.print(" ");
            for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
                Expression expr = i.next();
                expr.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(" ");
        }
        printer.print("}");
    }

    public void visit(VoidType n, Object arg) {
        printer.print("void");
    }

    public void visit(ArrayAccessExpr n, Object arg) {
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
    }

    public void visit(ArrayCreationExpr n, Object arg) {
        printer.print("new ");
        n.getType().accept(this, arg);
        printTypeArgs(n.getTypeArgs(), arg);

        if (n.getDimensions() != null) {
            for (Expression dim : n.getDimensions()) {
                printer.print("[");
                dim.accept(this, arg);
                printer.print("]");
            }
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
        } else {
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
            printer.print(" ");
            n.getInitializer().accept(this, arg);
        }
    }
    
    public void visit(BinaryExpr n, Object arg) {
        n.getLeft().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case or:
                printer.print("||");
                break;
            case and:
                printer.print("&&");
                break;
            case binOr:
                printer.print("|");
                break;
            case binAnd:
                printer.print("&");
                break;
            case xor:
                printer.print("^");
                break;
            case equals:
                printer.print("==");
                break;
            case notEquals:
                printer.print("!=");
                break;
            case less:
                printer.print("<");
                break;
            case greater:
                printer.print(">");
                break;
            case lessEquals:
                printer.print("<=");
                break;
            case greaterEquals:
                printer.print(">=");
                break;
            case lShift:
                printer.print("<<");
                break;
            case rSignedShift:
                printer.print(">>");
                break;
            case rUnsignedShift:
                printer.print(">>>");
                break;
            case plus:
                printer.print("+");
                break;
            case minus:
                printer.print("-");
                break;
            case times:
                printer.print("*");
                break;
            case divide:
                printer.print("/");
                break;
            case remainder:
                printer.print("%");
                break;
        }
        printer.print(" ");
        n.getRight().accept(this, arg);
    }

    public void visit(CastExpr n, Object arg) {
        printer.print("(");
        n.getType().accept(this, arg);
        printer.print(") ");
        n.getExpr().accept(this, arg);
    }

    public void visit(ClassExpr n, Object arg) {
        n.getType().accept(this, arg);
        printer.print(".class");
    }

    public void visit(ConditionalExpr n, Object arg) {
        n.getCondition().accept(this, arg);
        printer.print(" ? ");
        n.getThenExpr().accept(this, arg);
        printer.print(" : ");
        n.getElseExpr().accept(this, arg);
    }

    public void visit(EnclosedExpr n, Object arg) {
        printer.print("(");
        n.getInner().accept(this, arg);
        printer.print(")");
    }

    public void visit(FieldAccessExpr n, Object arg) {
        n.getScope().accept(this, arg);
        printer.print(".");
        printer.print(n.getField());
    }

    public void visit(InstanceOfExpr n, Object arg) {
        n.getExpr().accept(this, arg);
        printer.print(" instanceof ");
        n.getType().accept(this, arg);
    }

    public void visit(CharLiteralExpr n, Object arg) {
        printer.print("'");
        printer.print(n.getValue());
        printer.print("'");
    }

    public void visit(DoubleLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(StringLiteralExpr n, Object arg) {
        printer.print("\"");
        printer.print(n.getValue());
        printer.print("\"");
    }

    public void visit(BooleanLiteralExpr n, Object arg) {
        printer.print(n.getValue().toString());
    }

    public void visit(NullLiteralExpr n, Object arg) {
        printer.print("null");
    }

    public void visit(ThisExpr n, Object arg) {
        if (n.getClassExpr() != null) {
            n.getClassExpr().accept(this, arg);
            printer.print(".");
        }
        printer.print("this");
    }

    public void visit(SuperExpr n, Object arg) {
        if (n.getClassExpr() != null) {
            n.getClassExpr().accept(this, arg);
            printer.print(".");
        }
        printer.print("super");
    }

    public void visit(ObjectCreationExpr n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }

        printer.print("new ");

        printTypeArgs(n.getTypeArgs(), arg);
        n.getType().accept(this, arg);

        printer.print("(");
        if (n.getArgs() != null) {
            for (Iterator<Expression> i = n.getArgs().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        if (n.getAnonymousClassBody() != null) {
            printer.printLn(" {", -1);
            printer.indent();
            printMembers(n.getAnonymousClassBody(), arg);
            printer.unindent();
            printer.print("}");
        }
    }

    public void visit(SuperMemberAccessExpr n, Object arg) {
        printer.print("super.");
        printer.print(n.getName());
    }

    public void visit(UnaryExpr n, Object arg) {
        switch (n.getOperator()) {
            case positive:
                printer.print("+");
                break;
            case negative:
                printer.print("-");
                break;
            case inverse:
                printer.print("~");
                break;
            case not:
                printer.print("!");
                break;
            case preIncrement:
                printer.print("++");
                break;
            case preDecrement:
                printer.print("--");
                break;
        }

        n.getExpr().accept(this, arg);

        switch (n.getOperator()) {
            case posIncrement:
                printer.print("++");
                break;
            case posDecrement:
                printer.print("--");
                break;
        }
    }

    public void visit(ConstructorDeclaration n, Object arg) {
    	//-- added in for ParaTask
    	alreadyPrinted_isEDT.push(false);
    	
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printTypeParameters(n.getTypeParameters(), arg);
        if (n.getTypeParameters() != null) {
            printer.print(" ");
        }
        printer.print(n.getName());

        printer.print("(");
        if (n.getParameters() != null) {
            for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        if (n.getThrows() != null) {
            printer.print(" throws ");
            for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
                NameExpr name = i.next();
                name.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(" ");
        n.getBlock().accept(this, arg);
        alreadyPrinted_isEDT.pop();
    }

    public void visit(MethodDeclaration n, Object arg) {
    	
    	//-- ParaTask, added this in
    	currentMethod = n;
    	alreadyPrinted_isEDT.push(false);
    	
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printTypeParameters(n.getTypeParameters(), arg);
        if (n.getTypeParameters() != null) {
            printer.print(" ");
        }

        n.getType().accept(this, arg);
        printer.print(" ");
        printer.print(n.getName());

        printer.print("(");
        if (n.getParameters() != null) {
            for (Iterator<Parameter> i = n.getParameters().iterator(); i.hasNext();) {
                Parameter p = i.next();
                p.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");

        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }

        if (n.getThrows() != null) {
            printer.print(" throws ");
            for (Iterator<NameExpr> i = n.getThrows().iterator(); i.hasNext();) {
                NameExpr name = i.next();
                name.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (n.getBody() == null) {
            printer.print(";");
        } else {
            printer.print(" ");
            n.getBody().accept(this, arg);
        }
        
        currentMethod = null;
        alreadyPrinted_isEDT.pop();
    }
    
    private void printParameter(Parameter n, String typeOverride, Object arg) {
    	printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        if (typeOverride == null) {
	        n.getType().accept(this, arg);
	        if (n.isVarArgs()) {
	            printer.print("...");
	        }
        } else {
        	if (n.isVarArgs()) {
        		printer.print("Object...");
        	} else {
        		printer.print(typeOverride);
        	}
        }
        
        printer.print(" ");
        n.getId().accept(this, arg);
    }

    public void visit(Parameter n, Object arg) {
        printParameter(n, null, arg);
    }

    public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
        if (n.isThis()) {
            printTypeArgs(n.getTypeArgs(), arg);
            printer.print("this");
        } else {
            if (n.getExpr() != null) {
                n.getExpr().accept(this, arg);
                printer.print(".");
            }
            printTypeArgs(n.getTypeArgs(), arg);
            printer.print("super");
        }
        printer.print("(");
        if (n.getArgs() != null) {
            for (Iterator<Expression> i = n.getArgs().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(");");
    }
    
    public void visit(TypeDeclarationStmt n, Object arg) {
        n.getTypeDeclaration().accept(this, arg);
    }

    public void visit(AssertStmt n, Object arg) {
        printer.print("assert ");
        n.getCheck().accept(this, arg);
        if (n.getMessage() != null) {
            printer.print(" : ");
            n.getMessage().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(BlockStmt n, Object arg) {
        printer.printLn("{", n.getBeginLine());
        alreadyPrinted_isEDT.push(false);
        if (n.getStmts() != null) {
            printer.indent();
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                printer.printLn(s.getBeginLine());
            }
            printer.unindent();
        }
        alreadyPrinted_isEDT.pop();
        printer.print("}");
        printer.setLastUserLine(n.getEndLine());
    }

    public void visit(LabeledStmt n, Object arg) {
        printer.print(n.getLabel());
        printer.print(": ");
        n.getStmt().accept(this, arg);
    }

    public void visit(EmptyStmt n, Object arg) {
        printer.print(";");
    }

    public void visit(ExpressionStmt n, Object arg) {
        n.getExpression().accept(this, arg);
        printer.print(";");
    }

    public void visit(SwitchStmt n, Object arg) {
        printer.print("switch(");
        n.getSelector().accept(this, arg);
        printer.printLn(") {", n.getBeginLine());
        if (n.getEntries() != null) {
            printer.indent();
            for (SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
            }
            printer.unindent();
        }
        printer.print("}");

    }

    public void visit(SwitchEntryStmt n, Object arg) {
        if (n.getLabel() != null) {
            printer.print("case ");
            n.getLabel().accept(this, arg);
            printer.print(":");
        } else {
            printer.print("default:");
        }
        printer.printLn(-1);
        printer.indent();
        if (n.getStmts() != null) {
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                printer.printLn(s.getBeginLine());
            }
        }
        printer.unindent();
    }

    public void visit(BreakStmt n, Object arg) {
        printer.print("break");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    public void visit(ReturnStmt n, Object arg) {
        printer.print("return");
        if (n.getExpr() != null) {
            printer.print(" ");
            n.getExpr().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(EnumDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("enum ");
        printer.print(n.getName());

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.printLn(" {",-1);
        printer.indent();
        if (n.getEntries() != null) {
            printer.printLn(-1);
            for (Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
                EnumConstantDeclaration e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (n.getMembers() != null) {
            printer.printLn(";",-1);
            printMembers(n.getMembers(), arg);
        } else {
            if (n.getEntries() != null) {
                printer.printLn(-1);
            }
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(EnumConstantDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printer.print(n.getName());

        if (n.getArgs() != null) {
            printer.print("(");
            for (Iterator<Expression> i = n.getArgs().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(")");
        }

        if (n.getClassBody() != null) {
            printer.printLn(" {",-1);
            printer.indent();
            printMembers(n.getClassBody(), arg);
            printer.unindent();
            printer.printLn("}",-1);
        }
    }

    public void visit(EmptyMemberDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(InitializerDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getBlock().accept(this, arg);
    }

    public void visit(IfStmt n, Object arg) {
        printer.print("if (");
        n.getCondition().accept(this, arg);
        printer.printLn(") ", n.getBeginLine());
        printer.setLastUserLine(n.getThenStmt().getBeginLine());
        n.getThenStmt().accept(this, arg);
        if (n.getElseStmt() != null) {
            printer.print(" else ");
            n.getElseStmt().accept(this, arg);
        }
    }

    public void visit(WhileStmt n, Object arg) {
        printer.print("while (");
        n.getCondition().accept(this, arg);
        printer.printLn(") ", n.getBeginLine());
        printer.setLastUserLine(n.getBody().getBeginLine());
        n.getBody().accept(this, arg);
    }

    public void visit(ContinueStmt n, Object arg) {
        printer.print("continue");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    public void visit(DoStmt n, Object arg) {
        printer.print("do ");
        printer.setLastUserLine(n.getBody().getBeginLine());
        n.getBody().accept(this, arg);
        printer.print(" while (");
        n.getCondition().accept(this, arg);
        printer.print(");");
    }

    public void visit(ForeachStmt n, Object arg) {
        printer.print("for (");
        n.getVariable().accept(this, arg);
        printer.print(" : ");
        n.getIterable().accept(this, arg);
        printer.printLn(") ",n.getBeginLine());
        printer.setLastUserLine(n.getBody().getBeginLine());
        n.getBody().accept(this, arg);
    }

    public void visit(ForStmt n, Object arg) {
        printer.print("for (");
        if (n.getInit() != null) {
            for (Iterator<Expression> i = n.getInit().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print("; ");
        if (n.getCompare() != null) {
            n.getCompare().accept(this, arg);
        }
        printer.print("; ");
        if (n.getUpdate() != null) {
            for (Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.printLn(") ",n.getBeginLine());
        printer.setLastUserLine(n.getBody().getBeginLine());
        n.getBody().accept(this, arg);
    }

    public void visit(ThrowStmt n, Object arg) {
        printer.print("throw ");
        n.getExpr().accept(this, arg);
        printer.print(";");
    }

    public void visit(SynchronizedStmt n, Object arg) {
        printer.print("synchronized (");
        n.getExpr().accept(this, arg);
        printer.print(") ");
        printer.setLastUserLine(n.getBlock().getBeginLine());
        n.getBlock().accept(this, arg);
    }

    public void visit(TryStmt n, Object arg) {
        printer.print("try ");
        printer.setLastUserLine(n.getTryBlock().getBeginLine());
        n.getTryBlock().accept(this, arg);
        if (n.getCatchs() != null) {
            for (CatchClause c : n.getCatchs()) {
                c.accept(this, arg);
            }
        }
        if (n.getFinallyBlock() != null) {
            printer.print(" finally ");
            printer.setLastUserLine(n.getFinallyBlock().getBeginLine());
            n.getFinallyBlock().accept(this, arg);
        }
    }

    public void visit(CatchClause n, Object arg) {
        printer.print(" catch (");
        n.getExcept().accept(this, arg);
        printer.print(") ");
        printer.setLastUserLine(n.getCatchBlock().getBeginLine());
        n.getCatchBlock().accept(this, arg);
    }

    public void visit(AnnotationDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("@interface ");
        printer.print(n.getName());
        printer.printLn(" {",-1);
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(AnnotationMemberDeclaration n, Object arg) {
        if (n.getJavaDoc() != null) {
            n.getJavaDoc().accept(this, arg);
        }
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");
        printer.print(n.getName());
        printer.print("()");
        if (n.getDefaultValue() != null) {
            printer.print(" default ");
            n.getDefaultValue().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(MarkerAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
    }

    public void visit(SingleMemberAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        n.getMemberValue().accept(this, arg);
        printer.print(")");
    }

    public void visit(NormalAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        for (Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext();) {
            MemberValuePair m = i.next();
            m.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
        printer.print(")");
    }

    public void visit(MemberValuePair n, Object arg) {
        printer.print(n.getName());
        printer.print(" = ");
        n.getValue().accept(this, arg);
    }

    public void visit(LineComment n, Object arg) {
        printer.print("//");
        printer.printLn(n.getContent(), n.getBeginLine());
    }

    public void visit(BlockComment n, Object arg) {
        printer.print("/*");
        printer.print(n.getContent());
        printer.printLn("*/", n.getEndLine());
    }
}
