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
package pt.compiler.parser.ast;

import pt.compiler.parser.ast.body.TypeDeclaration;
import pt.compiler.parser.ast.visitor.GenericVisitor;
import pt.compiler.parser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author Julio Vilmar Gesser
 */
public final class CompilationUnit extends Node {

    private final PackageDeclaration pakage;

    private final List<ImportDeclaration> imports;

    private final List<TypeDeclaration> types;

    private final List<Comment> comments;

    public CompilationUnit(int line, int column, PackageDeclaration pakage, List<ImportDeclaration> imports, List<TypeDeclaration> types, List<Comment> comments) {
        super(line, column);
        this.pakage = pakage;
        this.imports = imports;
        this.types = types;
        this.comments = comments;
    }

    public PackageDeclaration getPakage() {
        return pakage;
    }

    public List<ImportDeclaration> getImports() {
        return imports;
    }

    public List<TypeDeclaration> getTypes() {
        return types;
    }

    public List<Comment> getComments() {
        return comments;
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
