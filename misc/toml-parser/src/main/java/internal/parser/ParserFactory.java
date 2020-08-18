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
package internal.parser;

import syntax.tree.SyntaxTree;
import text.TextDocument;
import text.TextDocumentChange;
import text.TextDocuments;

/**
 * A factory for creating {@code BallerinaParser} instances.
 * <p>
 * Creates a regular parser or an incremental parser based on the parameters.
 *
 * @since 1.3.0
 */
public class ParserFactory {

    private ParserFactory() {
    }

    /**
     * Creates a regular {@code BallerinaParser} instance from the given {@code String}.
     *
     * @param text source code
     * @return a {@code BallerinaParser} instance
     */
    public static TomlParser getParser(String text) {
        TextDocument textDocument = TextDocuments.from(text);
        AbstractTokenReader tokenReader = new TokenReader(getLexer(textDocument));
        return new TomlParser(tokenReader);
    }

    /**
     * Creates a regular {@code BallerinaParser} instance from the given {@code TextDocument}.
     *
     * @param textDocument source code
     * @return a {@code BallerinaParser} instance
     */
    public static TomlParser getParser(TextDocument textDocument) {
        AbstractTokenReader tokenReader = new TokenReader(getLexer(textDocument));
        return new TomlParser(tokenReader);
    }

//    /**
//     * Creates an incremental {@code BallerinaParser} instance from
//     * the old {@code SyntaxTree} and text modifications.
//     *
//     * @param oldTree            previous syntax tree
//     * @param newTextDocument    new source code
//     * @param textDocumentChange a collection of text edits applied to the previous source code
//     * @return an incremental {@code BallerinaParser} instance
//     */
//    public static BallerinaParser getParser(SyntaxTree oldTree,
//                                            TextDocument newTextDocument,
//                                            TextDocumentChange textDocumentChange) {
//        HybridNodeStorage hybridNodeStorage = new HybridNodeStorage(oldTree,
//                getLexer(newTextDocument), textDocumentChange);
//        AbstractTokenReader tokeReader = new HybridTokenReader(hybridNodeStorage);
//        UnmodifiedSubtreeSupplier subtreeReader = new UnmodifiedSubtreeSupplier(hybridNodeStorage);
//        return new IncrementalParser(tokeReader, subtreeReader);
//    }

    private static TomlLexer getLexer(TextDocument textDocument) {
        return new TomlLexer(textDocument.getCharacterReader());
    }
}
