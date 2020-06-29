/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerinalang.compiler.internal.parser.tree;

import io.ballerinalang.compiler.syntax.tree.Node;
import io.ballerinalang.compiler.syntax.tree.NonTerminalNode;
import io.ballerinalang.compiler.syntax.tree.ParameterDocumentationLineNode;
import io.ballerinalang.compiler.syntax.tree.SyntaxKind;

import java.util.Collection;
import java.util.Collections;

/**
 * This is a generated internal syntax tree node.
 *
 * @since 2.0.0
 */
public class STParameterDocumentationLineNode extends STNode {
    public final STNode hashToken;
    public final STNode plusToken;
    public final STNode parameterName;
    public final STNode minusToken;
    public final STNode description;

    STParameterDocumentationLineNode(
            SyntaxKind kind,
            STNode hashToken,
            STNode plusToken,
            STNode parameterName,
            STNode minusToken,
            STNode description) {
        this(
                kind,
                hashToken,
                plusToken,
                parameterName,
                minusToken,
                description,
                Collections.emptyList());
    }

    STParameterDocumentationLineNode(
            SyntaxKind kind,
            STNode hashToken,
            STNode plusToken,
            STNode parameterName,
            STNode minusToken,
            STNode description,
            Collection<STNodeDiagnostic> diagnostics) {
        super(kind, diagnostics);
        this.hashToken = hashToken;
        this.plusToken = plusToken;
        this.parameterName = parameterName;
        this.minusToken = minusToken;
        this.description = description;

        addChildren(
                hashToken,
                plusToken,
                parameterName,
                minusToken,
                description);
    }

    public STNode modifyWith(Collection<STNodeDiagnostic> diagnostics) {
        return new STParameterDocumentationLineNode(
                this.kind,
                this.hashToken,
                this.plusToken,
                this.parameterName,
                this.minusToken,
                this.description,
                diagnostics);
    }

    public STParameterDocumentationLineNode modify(
            SyntaxKind kind,
            STNode hashToken,
            STNode plusToken,
            STNode parameterName,
            STNode minusToken,
            STNode description) {
        if (checkForReferenceEquality(
                hashToken,
                plusToken,
                parameterName,
                minusToken,
                description)) {
            return this;
        }

        return new STParameterDocumentationLineNode(
                kind,
                hashToken,
                plusToken,
                parameterName,
                minusToken,
                description,
                diagnostics);
    }

    public Node createFacade(int position, NonTerminalNode parent) {
        return new ParameterDocumentationLineNode(this, position, parent);
    }

    @Override
    public void accept(STNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T apply(STNodeTransformer<T> transformer) {
        return transformer.transform(this);
    }
}
