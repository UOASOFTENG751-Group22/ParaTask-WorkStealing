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
public interface GenericVisitor<R, A> {

    public R visit(Node n, A arg);

    //- Compilation Unit ----------------------------------

    public R visit(CompilationUnit n, A arg);

    public R visit(PackageDeclaration n, A arg);

    public R visit(ImportDeclaration n, A arg);

    public R visit(TypeParameter n, A arg);

    public R visit(LineComment n, A arg);

    public R visit(BlockComment n, A arg);

    //- Body ----------------------------------------------

    public R visit(ClassOrInterfaceDeclaration n, A arg);

    public R visit(EnumDeclaration n, A arg);

    public R visit(EmptyTypeDeclaration n, A arg);

    public R visit(EnumConstantDeclaration n, A arg);

    public R visit(AnnotationDeclaration n, A arg);

    public R visit(AnnotationMemberDeclaration n, A arg);

    public R visit(FieldDeclaration n, A arg);

    public R visit(VariableDeclarator n, A arg);

    public R visit(VariableDeclaratorId n, A arg);

    public R visit(ConstructorDeclaration n, A arg);

    public R visit(MethodDeclaration n, A arg);

    public R visit(Parameter n, A arg);

    public R visit(EmptyMemberDeclaration n, A arg);

    public R visit(InitializerDeclaration n, A arg);

    public R visit(JavadocComment n, A arg);

    //- Type ----------------------------------------------

    public R visit(ClassOrInterfaceType n, A arg);

    public R visit(PrimitiveType n, A arg);

    public R visit(ReferenceType n, A arg);

    public R visit(VoidType n, A arg);

    public R visit(WildcardType n, A arg);

    //- Expression ----------------------------------------

    public R visit(ArrayAccessExpr n, A arg);

    public R visit(ArrayCreationExpr n, A arg);

    public R visit(ArrayInitializerExpr n, A arg);

    public R visit(AssignExpr n, A arg);

    public R visit(BinaryExpr n, A arg);

    public R visit(CastExpr n, A arg);

    public R visit(ClassExpr n, A arg);

    public R visit(ConditionalExpr n, A arg);

    public R visit(EnclosedExpr n, A arg);

    public R visit(FieldAccessExpr n, A arg);

    public R visit(InstanceOfExpr n, A arg);

    public R visit(StringLiteralExpr n, A arg);

    public R visit(IntegerLiteralExpr n, A arg);

    public R visit(LongLiteralExpr n, A arg);

    public R visit(IntegerLiteralMinValueExpr n, A arg);

    public R visit(LongLiteralMinValueExpr n, A arg);

    public R visit(CharLiteralExpr n, A arg);

    public R visit(DoubleLiteralExpr n, A arg);

    public R visit(BooleanLiteralExpr n, A arg);

    public R visit(NullLiteralExpr n, A arg);

    public R visit(MethodCallExpr n, A arg);

    public R visit(NameExpr n, A arg);

    public R visit(ObjectCreationExpr n, A arg);

    public R visit(QualifiedNameExpr n, A arg);

    public R visit(SuperMemberAccessExpr n, A arg);

    public R visit(ThisExpr n, A arg);

    public R visit(SuperExpr n, A arg);

    public R visit(UnaryExpr n, A arg);

    public R visit(VariableDeclarationExpr n, A arg);

    public R visit(MarkerAnnotationExpr n, A arg);

    public R visit(SingleMemberAnnotationExpr n, A arg);

    public R visit(NormalAnnotationExpr n, A arg);

    public R visit(MemberValuePair n, A arg);

    //- Statements ----------------------------------------

    public R visit(ExplicitConstructorInvocationStmt n, A arg);

    public R visit(TypeDeclarationStmt n, A arg);

    public R visit(AssertStmt n, A arg);

    public R visit(BlockStmt n, A arg);

    public R visit(LabeledStmt n, A arg);

    public R visit(EmptyStmt n, A arg);

    public R visit(ExpressionStmt n, A arg);

    public R visit(SwitchStmt n, A arg);

    public R visit(SwitchEntryStmt n, A arg);

    public R visit(BreakStmt n, A arg);

    public R visit(ReturnStmt n, A arg);

    public R visit(IfStmt n, A arg);

    public R visit(WhileStmt n, A arg);

    public R visit(ContinueStmt n, A arg);

    public R visit(DoStmt n, A arg);

    public R visit(ForeachStmt n, A arg);

    public R visit(ForStmt n, A arg);

    public R visit(ThrowStmt n, A arg);

    public R visit(SynchronizedStmt n, A arg);

    public R visit(TryStmt n, A arg);

    public R visit(CatchClause n, A arg);

}
