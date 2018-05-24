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
 * Copyright (C) 2007 J�lio Vilmar Gesser.
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

import pt.compiler.parser.ast.BlockComment;
import pt.compiler.parser.ast.CompilationUnit;
import pt.compiler.parser.ast.ImportDeclaration;
import pt.compiler.parser.ast.LineComment;
import pt.compiler.parser.ast.Node;
import pt.compiler.parser.ast.PackageDeclaration;
import pt.compiler.parser.ast.TypeParameter;
import pt.compiler.parser.ast.body.AnnotationDeclaration;
import pt.compiler.parser.ast.body.AnnotationMemberDeclaration;
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
import pt.compiler.parser.ast.body.Parameter;
import pt.compiler.parser.ast.body.TaskDeclaration;
import pt.compiler.parser.ast.body.VariableDeclarator;
import pt.compiler.parser.ast.body.VariableDeclaratorId;
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
import pt.compiler.parser.ast.type.VoidType;
import pt.compiler.parser.ast.type.WildcardType;

/**
 * @author Julio Vilmar Gesser
 */
public interface VoidVisitor<A> {

    public void visit(Node n, A arg);
    
    //- ParaTask --------------------
    public void visit(TaskClauseExpr tc, A arg);
    
    public void visit(TaskDeclaration n, A arg);

    //- Compilation Unit ----------------------------------

    public void visit(CompilationUnit n, A arg);

    public void visit(PackageDeclaration n, A arg);

    public void visit(ImportDeclaration n, A arg);

    public void visit(TypeParameter n, A arg);

    public void visit(LineComment n, A arg);

    public void visit(BlockComment n, A arg);

    //- Body ----------------------------------------------

    public void visit(ClassOrInterfaceDeclaration n, A arg);

    public void visit(EnumDeclaration n, A arg);

    public void visit(EmptyTypeDeclaration n, A arg);

    public void visit(EnumConstantDeclaration n, A arg);

    public void visit(AnnotationDeclaration n, A arg);

    public void visit(AnnotationMemberDeclaration n, A arg);

    public void visit(FieldDeclaration n, A arg);

    public void visit(VariableDeclarator n, A arg);

    public void visit(VariableDeclaratorId n, A arg);

    public void visit(ConstructorDeclaration n, A arg);

    public void visit(MethodDeclaration n, A arg);

    public void visit(Parameter n, A arg);

    public void visit(EmptyMemberDeclaration n, A arg);

    public void visit(InitializerDeclaration n, A arg);

    public void visit(JavadocComment n, A arg);

    //- Type ----------------------------------------------

    public void visit(ClassOrInterfaceType n, A arg);

    public void visit(PrimitiveType n, A arg);

    public void visit(ReferenceType n, A arg);

    public void visit(VoidType n, A arg);

    public void visit(WildcardType n, A arg);

    //- Expression ----------------------------------------

    public void visit(ArrayAccessExpr n, A arg);

    public void visit(ArrayCreationExpr n, A arg);

    public void visit(ArrayInitializerExpr n, A arg);

    public void visit(AssignExpr n, A arg);

    public void visit(BinaryExpr n, A arg);

    public void visit(CastExpr n, A arg);

    public void visit(ClassExpr n, A arg);

    public void visit(ConditionalExpr n, A arg);

    public void visit(EnclosedExpr n, A arg);

    public void visit(FieldAccessExpr n, A arg);

    public void visit(InstanceOfExpr n, A arg);

    public void visit(StringLiteralExpr n, A arg);

    public void visit(IntegerLiteralExpr n, A arg);

    public void visit(LongLiteralExpr n, A arg);

    public void visit(IntegerLiteralMinValueExpr n, A arg);

    public void visit(LongLiteralMinValueExpr n, A arg);

    public void visit(CharLiteralExpr n, A arg);

    public void visit(DoubleLiteralExpr n, A arg);

    public void visit(BooleanLiteralExpr n, A arg);

    public void visit(NullLiteralExpr n, A arg);

    public void visit(MethodCallExpr n, A arg);

    public void visit(NameExpr n, A arg);

    public void visit(ObjectCreationExpr n, A arg);

    public void visit(QualifiedNameExpr n, A arg);

    public void visit(SuperMemberAccessExpr n, A arg);

    public void visit(ThisExpr n, A arg);

    public void visit(SuperExpr n, A arg);

    public void visit(UnaryExpr n, A arg);

    public void visit(VariableDeclarationExpr n, A arg);

    public void visit(MarkerAnnotationExpr n, A arg);

    public void visit(SingleMemberAnnotationExpr n, A arg);

    public void visit(NormalAnnotationExpr n, A arg);

    public void visit(MemberValuePair n, A arg);

    //- Statements ----------------------------------------

    public void visit(ExplicitConstructorInvocationStmt n, A arg);

    public void visit(TypeDeclarationStmt n, A arg);

    public void visit(AssertStmt n, A arg);

    public void visit(BlockStmt n, A arg);

    public void visit(LabeledStmt n, A arg);

    public void visit(EmptyStmt n, A arg);

    public void visit(ExpressionStmt n, A arg);

    public void visit(SwitchStmt n, A arg);

    public void visit(SwitchEntryStmt n, A arg);

    public void visit(BreakStmt n, A arg);

    public void visit(ReturnStmt n, A arg);

    public void visit(IfStmt n, A arg);

    public void visit(WhileStmt n, A arg);

    public void visit(ContinueStmt n, A arg);

    public void visit(DoStmt n, A arg);

    public void visit(ForeachStmt n, A arg);

    public void visit(ForStmt n, A arg);

    public void visit(ThrowStmt n, A arg);

    public void visit(SynchronizedStmt n, A arg);

    public void visit(TryStmt n, A arg);

    public void visit(CatchClause n, A arg);

}
