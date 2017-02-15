/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.plugins.idea.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.ResolveResult;
import org.antlr.jetbrains.adaptor.SymtabUtils;
import org.antlr.jetbrains.adaptor.psi.IdentifierDefSubtree;
import org.antlr.jetbrains.adaptor.psi.ScopeNode;
import org.ballerinalang.plugins.idea.BallerinaLanguage;
import org.ballerinalang.plugins.idea.BallerinaParserDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FunctionNode extends IdentifierDefSubtree implements ScopeNode {

    public FunctionNode(@NotNull ASTNode node) {
        super(node, BallerinaParserDefinition.ID);
    }

    @Nullable
    @Override
    public PsiElement resolve(PsiNamedElement element) {
        if (element.getParent() instanceof CallableUnitNameNode) {
            return SymtabUtils.resolve(this, BallerinaLanguage.INSTANCE, element,
                    "//functionDefinition/function/Identifier");
        } else if (element.getParent() instanceof VariableReferenceNode) {
            PsiElement resolved = SymtabUtils.resolve(this, BallerinaLanguage.INSTANCE, element,
                    "//parameter/Identifier");
            if (resolved == null) {
                resolved = SymtabUtils.resolve(this, BallerinaLanguage.INSTANCE, element,
                        "//namedParameter/Identifier");
            }
            return resolved;
        } else if (element.getParent() instanceof SimpleTypeNode) {
            return SymtabUtils.resolve(this, BallerinaLanguage.INSTANCE, element,
                    "//connectorDefinition/Identifier");
        }
        return null;
    }

    @Override
    public ResolveResult[] multiResolve(IdentifierPSINode myElement) {
        return new ResolveResult[0];
    }
}
