/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.codeaction.providers.changetype;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.codeaction.CodeActionUtil;
import org.ballerinalang.langserver.codeaction.providers.AbstractCodeActionProvider;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.CodeActionContext;
import org.ballerinalang.langserver.commons.codeaction.spi.DiagBasedPositionDetails;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Code Action for incompatible types.
 *
 * @since 2.0.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.codeaction.spi.LSCodeActionProvider")
public class TypeCastCodeAction extends AbstractCodeActionProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CodeAction> getDiagBasedCodeActions(Diagnostic diagnostic,
                                                    DiagBasedPositionDetails positionDetails,
                                                    CodeActionContext context) {
        if (!(diagnostic.message().contains(CommandConstants.INCOMPATIBLE_TYPES))) {
            return Collections.emptyList();
        }
        Node matchedNode = getMatchedNode(positionDetails.matchedNode());
        if (matchedNode == null) {
            return Collections.emptyList();
        }

        Optional<TypeSymbol> rhsTypeSymbol = positionDetails.diagnosticProperty(
                DiagBasedPositionDetails.DIAG_PROP_INCOMPATIBLE_TYPES_EXPECTED_SYMBOL_INDEX);
        if (rhsTypeSymbol.isEmpty()) {
            return Collections.emptyList();
        }
        if (rhsTypeSymbol.get().typeKind() == TypeDescKind.UNION) {
            // If RHS is a union and has error member type; skip code-action
            UnionTypeSymbol unionTypeDesc = (UnionTypeSymbol) rhsTypeSymbol.get();
            boolean hasErrorMemberType = unionTypeDesc.memberTypeDescriptors().stream()
                    .anyMatch(member -> member.typeKind() == TypeDescKind.ERROR);
            if (hasErrorMemberType) {
                return Collections.emptyList();
            }
        }
        Optional<ExpressionNode> expressionNode = getExpression(matchedNode);
        Optional<TypeSymbol> variableTypeSymbol = getVariableTypeSymbol(matchedNode, rhsTypeSymbol.get(), context);
        if (expressionNode.isEmpty() || variableTypeSymbol.isEmpty() ||
                expressionNode.get().kind() == SyntaxKind.TYPE_CAST_EXPRESSION) {
            return Collections.emptyList();
        }
        Position editPos = CommonUtil.toPosition(expressionNode.get().lineRange().startLine());
        List<TextEdit> edits = new ArrayList<>();
        Optional<String> typeName = CodeActionUtil.getPossibleType(variableTypeSymbol.get(), edits, context);
        if (typeName.isEmpty()) {
            return Collections.emptyList();
        }
        String editText = "<" + typeName.get() + "> ";
        edits.add(new TextEdit(new Range(editPos, editPos), editText));
        String commandTitle = CommandConstants.ADD_TYPE_CAST_TITLE;
        return Collections.singletonList(createQuickFixCodeAction(commandTitle, edits, context.fileUri()));
    }

    private NonTerminalNode getMatchedNode(NonTerminalNode node) {
        List<SyntaxKind> syntaxKinds = Arrays.asList(SyntaxKind.LOCAL_VAR_DECL,
                SyntaxKind.MODULE_VAR_DECL, SyntaxKind.ASSIGNMENT_STATEMENT);
        while (node != null && !syntaxKinds.contains(node.kind())) {
            node = node.parent();
        }

        return node;
    }

    private Optional<ExpressionNode> getExpression(Node node) {
        if (node.kind() == SyntaxKind.LOCAL_VAR_DECL) {
            return ((VariableDeclarationNode) node).initializer();
        } else if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
            return ((ModuleVariableDeclarationNode) node).initializer();
        } else if (node.kind() == SyntaxKind.ASSIGNMENT_STATEMENT) {
            return Optional.of(((AssignmentStatementNode) node).expression());
        } else {
            return Optional.empty();
        }
    }

    protected Optional<TypeSymbol> getVariableTypeSymbol(Node matchedNode,
                                                         TypeSymbol typeSymbol,
                                                         CodeActionContext context) {
        switch (matchedNode.kind()) {
            case LOCAL_VAR_DECL:
            case MODULE_VAR_DECL:
                return Optional.of(typeSymbol);
            case ASSIGNMENT_STATEMENT:
                Optional<VariableSymbol> optVariableSymbol = getVariableSymbol(context, matchedNode);
                if (optVariableSymbol.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(optVariableSymbol.get().typeDescriptor());
            default:
                return Optional.empty();
        }
    }

    protected Optional<VariableSymbol> getVariableSymbol(CodeActionContext context, Node matchedNode) {
        AssignmentStatementNode assignmentStmtNode = (AssignmentStatementNode) matchedNode;
        SemanticModel semanticModel = context.currentSemanticModel().orElseThrow();
        Document srcFile = context.currentDocument().orElseThrow();
        Optional<Symbol> symbol = semanticModel.symbol(srcFile,
                assignmentStmtNode.varRef().lineRange().startLine());
        if (symbol.isEmpty() || symbol.get().kind() != SymbolKind.VARIABLE) {
            return Optional.empty();
        }
        return Optional.of((VariableSymbol) symbol.get());
    }
}
