/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
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
package org.wso2.ballerinalang.compiler.desugar;

import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.ballerinalang.model.tree.statements.VariableDefinitionNode;
import org.ballerinalang.model.tree.types.TypeNode;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.parser.BLangAnonymousModelHelper;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangErrorVariable;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangRecordVariable;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTupleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangDoClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangLetClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangGroupExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryAction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeTestExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.types.BLangErrorType;
import org.wso2.ballerinalang.compiler.tree.types.BLangLetVariable;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUnionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.BLangDiagnosticLogHelper;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for desugar query pipeline into actual Ballerina code.
 *
 * @since 1.2.0
 */
public class QueryDesugar extends BLangNodeVisitor {

    private static final CompilerContext.Key<QueryDesugar> QUERY_DESUGAR_KEY =
            new CompilerContext.Key<>();
    private final SymbolEnter symbolEnter;
    private final Desugar desugar;
    private final SymbolTable symTable;
    private final BLangAnonymousModelHelper anonymousModelHelper;
    private BLangDiagnosticLogHelper dlog;
    private final SymbolResolver symResolver;
    private final Names names;
    private final Types types;
    private BLangBlockStmt parentBlock = null;
    private int streamElementCount = 0;
    private SymbolEnv env;

    private QueryDesugar(CompilerContext context) {
        context.put(QUERY_DESUGAR_KEY, this);
        this.symTable = SymbolTable.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.symbolEnter = SymbolEnter.getInstance(context);
        this.names = Names.getInstance(context);
        this.types = Types.getInstance(context);
        this.dlog = BLangDiagnosticLogHelper.getInstance(context);
        this.desugar = Desugar.getInstance(context);
        this.anonymousModelHelper = BLangAnonymousModelHelper.getInstance(context);
    }

    public static QueryDesugar getInstance(CompilerContext context) {
        QueryDesugar desugar = context.get(QUERY_DESUGAR_KEY);
        if (desugar == null) {
            desugar = new QueryDesugar(context);
        }

        return desugar;
    }

    // Create While statement
    //
    // Below query expression :
    //    Person[]|error outputDataArray = from var person in personList
    //                                        select person;
    //
    // changes as,
    //    Person[]|error outputDataArray = ();
    //    Person[] $tempDataArray$ = [];
    //
    //    Person[] $data$ = personList;
    //    abstract object {public function next() returns record {|Person value;|}? $iterator$ = $data$.iterator();
    //    record {|Person value;|}|error? $result$ = $iterator$.next();
    //
    //    while (true) {
    //        if ($result$ is ()) {
    //            break;
    //        } else if ($result$ is error) {
    //            outputDataArray = $result$;
    //            break;
    //        } else {
    //            var $value$ = $result$.value;
    //            $tempDataArray$.push($value$);
    //        }
    //        $result$ = $iterator$.next();
    //    }
    //
    //    if (outputDataArray is ()) {
    //        outputDataArray = tempDataArray;
    //    }
    BLangStatementExpression desugarQueryExpr(BLangQueryExpr queryExpr, SymbolEnv env) {
        this.env = env;
        List<BLangFromClause> fromClauseList = queryExpr.fromClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangSelectClause selectClause = queryExpr.selectClause;
        List<BLangWhereClause> whereClauseList = queryExpr.whereClauseList;
        List<BLangLetClause> letClauseList = queryExpr.letClausesList;
        DiagnosticPos pos = fromClause.pos;
        parentBlock = ASTBuilderUtil.createBlockStmt(fromClause.pos);

        // --- start new stream desugar ---
        // TODO
        List<BLangNode> queryClauses = queryExpr.getQueryClauses();
        BLangBlockStmt queryBlock = parentBlock;
        BLangNode initFromClause = queryClauses.get(0);

        final BLangVariableReference initPipeline = addPipeline(queryBlock, (BLangFromClause) initFromClause);
        BLangVariableReference initFrom = addFromFunction(queryBlock, (BLangFromClause) initFromClause);
        addStreamFunction(queryBlock, initPipeline, initFrom);
        for (BLangNode clause : queryClauses.subList(1, queryClauses.size())) {
            switch (clause.getKind()) {
                case FROM:
                    BLangVariableReference pipeline = addPipeline(queryBlock, (BLangFromClause) clause);
                    BLangVariableReference fromFunc = addFromFunction(queryBlock, (BLangFromClause) clause);
                    addStreamFunction(queryBlock, pipeline, fromFunc);
                    BLangVariableReference joinFunc = addJoinFunction(queryBlock, pipeline);
                    addStreamFunction(queryBlock, initPipeline, joinFunc);
                    break;
                case LET_CLAUSE:
                    BLangVariableReference letFunc = addLetFunction(queryBlock, (BLangLetClause) clause);
                    addStreamFunction(queryBlock, initPipeline, letFunc);
                    break;
                case WHERE:
                    BLangVariableReference filterFunc = addFilterFunction(queryBlock, (BLangWhereClause) clause);
                    addStreamFunction(queryBlock, initPipeline, filterFunc);
                    break;
                case SELECT:
                    BLangVariableReference selectFunc = addSelectFunction(queryBlock, (BLangSelectClause) clause);
                    addStreamFunction(queryBlock, initPipeline, selectFunc);
                    break;
                case DO:
                    BLangVariableReference doFunc = addDoFunction(queryBlock, (BLangDoClause) clause);
                    addStreamFunction(queryBlock, initPipeline, doFunc);
                    break;
            }
        }
        BLangVariableReference streamRef = addGetStreamFromPipeline(queryBlock, initPipeline);
        BLangStatementExpression streamStmtExpr = ASTBuilderUtil.createStatementExpression(queryBlock, streamRef);
        streamStmtExpr.type = streamRef.type;
        return streamStmtExpr;

//        // TODO
//        if (queryExpr.isStream) {
//            // addGetStreamFromPipeline(queryBlock, initPipeline);
//        } else {
//            // to array?
//        }
//        // --- end new stream desugar ---

//
//        // Create output data array variable
//        // Person[]|error $outputDataArray$ = ();
//        BType queryExpOutputType = queryExpr.type;
//        BType outputType = types.getSafeType(queryExpOutputType, true, true);
//        if (outputType.tag == TypeTags.ARRAY) {
//            BVarSymbol outputVarSymbol = new BVarSymbol(0, new Name("$outputDataArray$"),
//                    env.scope.owner.pkgID, queryExpOutputType, env.scope.owner);
//            BLangExpression outputInitExpression = ASTBuilderUtil.createLiteral(fromClause.pos, symTable.nilType,
//                    null);
//            BLangSimpleVariable outputVariable =
//                    ASTBuilderUtil.createVariable(pos, "$outputDataArray$", queryExpOutputType,
//                            outputInitExpression, outputVarSymbol);
//            BLangSimpleVariableDef outputVariableDef =
//                    ASTBuilderUtil.createVariableDef(pos, outputVariable);
//            BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputVariable.symbol);
//
//            // Create temp array variable
//            // Person[] $tempDataArray$ = [];
//            BVarSymbol tempArrayVarSymbol = new BVarSymbol(0, new Name("$tempDataArray$"),
//                    env.scope.owner.pkgID, outputType, env.scope.owner);
//            BLangListConstructorExpr emptyArrayExpr = ASTBuilderUtil.createEmptyArrayLiteral(pos,
//                    (BArrayType) outputType);
//            BLangSimpleVariable tempArrayVariable = ASTBuilderUtil.createVariable(pos, "$tempDataArray$",
//                    outputType, emptyArrayExpr, tempArrayVarSymbol);
//            BLangSimpleVariableDef tempArrayVariableDef =
//                    ASTBuilderUtil.createVariableDef(pos, tempArrayVariable);
//            BLangSimpleVarRef tempArrayVarRef = ASTBuilderUtil.createVariableRef(pos, tempArrayVariable.symbol);
//
//            parentBlock.addStatement(outputVariableDef);
//            parentBlock.addStatement(tempArrayVariableDef);
//
//            BLangBlockStmt leafElseBlock = buildFromClauseBlock(fromClauseList, outputVarRef);
//
//            // Create indexed based access expression statement
//            //      $tempDataArray$.push({
//            //         firstName: person.firstName,
//            //         lastName: person.lastName
//            //      });
//            BLangBlockStmt bodyBlock = ASTBuilderUtil.createBlockStmt(pos);
//            BLangInvocation arrPushInvocation = createLangLibInvocation("push", tempArrayVarRef,
//                    new ArrayList<>(), Lists.of(ASTBuilderUtil.generateConversionExpr(selectClause.expression,
//                            symTable.anyOrErrorType, symResolver)), symTable.nilType, pos);
//            BLangExpressionStmt pushInvocationStmt = ASTBuilderUtil.createExpressionStmt(pos, bodyBlock);
//            pushInvocationStmt.expr = arrPushInvocation;
//
//            buildWhereClauseBlock(whereClauseList, letClauseList, leafElseBlock, bodyBlock, selectClause.pos);
//
//            // if (outputDataArray is ()) {
//            //     outputDataArray = tempDataArray;
//            // }
//            BLangBlockStmt nullCheckIfBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
//            BLangAssignment outputAssignment = ASTBuilderUtil
//                    .createAssignmentStmt(fromClause.pos, outputVarRef, tempArrayVarRef);
//            nullCheckIfBody.addStatement(outputAssignment);
//            BLangIf nullCheckIf = createTypeCheckIfNode(fromClause.pos, outputVarRef,
//                    desugar.getNillTypeNode(), nullCheckIfBody);
//
//            parentBlock.addStatement(nullCheckIf);
//
//            // Create statement expression with temp variable definition statements & while statement
//            BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(parentBlock, outputVarRef);
//
//            stmtExpr.type = queryExpOutputType;
//            return stmtExpr;
//        }
//        throw new IllegalStateException();
    }

    /**
     * Desugar fromClause/joinClause to below and return a reference to created join _StreamPipeline.
     * _StreamPipeline pipeline = createPipeline(collection);
     *
     * @param blockStmt  parent block to write to.
     * @param fromClause to init pipeline.
     * @return variableReference to created _StreamPipeline.
     */
    BLangVariableReference addPipeline(BLangBlockStmt blockStmt, BLangFromClause fromClause) {
        BLangExpression collection = fromClause.collection;
        DiagnosticPos pos = fromClause.pos;
        String name = getNewVarName();
        BVarSymbol dataSymbol = new BVarSymbol(0, names.fromString(name), env.scope.owner.pkgID,
                collection.type, this.env.scope.owner);
        BLangSimpleVariable dataVariable = ASTBuilderUtil.createVariable(fromClause.pos, name,
                collection.type, collection, dataSymbol);
        BLangSimpleVariableDef dataVarDef = ASTBuilderUtil.createVariableDef(fromClause.pos, dataVariable);
        BLangVariableReference valueVarRef = ASTBuilderUtil.createVariableRef(pos, dataSymbol);
        blockStmt.addStatement(dataVarDef);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_PIPELINE_FUNCTION,
                Lists.of(valueVarRef), fromClause.pos);
    }

    /**
     * Desugar fromClause to below and return a reference to created from _StreamFunction.
     * _StreamFunction xsFrom = createFromFunction(function(_Frame frame) returns _Frame|error? {
     * int x = <int> frame["value"];
     * frame["x"] = x;
     * return frame;
     * });
     *
     * @param blockStmt  parent block to write to.
     * @param fromClause to be desugared.
     * @return variableReference to created from _StreamFunction.
     */
    BLangVariableReference addFromFunction(BLangBlockStmt blockStmt, BLangFromClause fromClause) {
        DiagnosticPos pos = fromClause.pos;
        // function(_Frame frame) returns _Frame|error? { return frame; }
        BLangLambdaFunction lambda = createPassthroughLambda(pos);
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) lambda.function.body;
        BVarSymbol frameSymbol = lambda.function.requiredParams.get(0).symbol;

        // frame["x"] = x;, note: stmts will get added in reverse order.
        List<BVarSymbol> symbols = getIntroducedSymbols((BLangVariable)
                fromClause.variableDefinitionNode.getVariable());
        Collections.reverse(symbols);
        for (BVarSymbol symbol : symbols) {
            body.stmts.add(0, addToFrameFunctionStmt(pos, frameSymbol, symbol));
        }

        // int x = <int> frame["value"];, note: stmts will get added in reverse order.
        BLangFieldBasedAccess valueAccessExpr = desugar.getValueAccessExpression(fromClause.pos,
                symTable.anyOrErrorType, frameSymbol);
        valueAccessExpr.expr = desugar.addConversionExprIfRequired(valueAccessExpr.expr,
                types.getSafeType(valueAccessExpr.expr.type, true, false));
        VariableDefinitionNode variableDefinitionNode = fromClause.variableDefinitionNode;
        BLangVariable variable = (BLangVariable) variableDefinitionNode.getVariable();
        variable.setInitialExpression(desugar.addConversionExprIfRequired(valueAccessExpr, fromClause.varType));
        // add at 0, otherwise, this goes under existing stmts.
        body.stmts.add(0, (BLangStatement) variableDefinitionNode);

        // at this point;
        // function(_Frame frame) returns _Frame|error? {
        //      int x = <int> frame["value"];
        //      frame["x"] = x;
        //      return frame;
        // }
        lambda.accept(this);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_FROM_FUNCTION, Lists.of(lambda), pos);
    }

    /**
     * Desugar joinClauses / nested fromClauses to below and return a reference to created join _StreamFunction.
     * _StreamFunction joinFunc = createJoinFunction(joinPipeline);
     *
     * @param blockStmt    parent block to write to.
     * @param joinPipeline previously created _StreamPipeline reference to be joined.
     * @return variableReference to created join _StreamFunction.
     */
    BLangVariableReference addJoinFunction(BLangBlockStmt blockStmt, BLangVariableReference joinPipeline) {
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_JOIN_FUNCTION,
                Lists.of(joinPipeline), joinPipeline.pos);
    }

    /**
     * Desugar letClause to below and return a reference to created let _StreamFunction.
     * _StreamFunction ysLet = createLetFunction(function(_Frame frame) returns _Frame|error? {
     * frame["y2"] = <int> frame["y"] * <int> frame["y"];
     * return frame;
     * });
     *
     * @param blockStmt parent block to write to.
     * @param letClause to be desugared.
     * @return variableReference to created let _StreamFunction.
     */
    BLangVariableReference addLetFunction(BLangBlockStmt blockStmt, BLangLetClause letClause) {
        DiagnosticPos pos = letClause.pos;
        // function(_Frame frame) returns _Frame|error? { return frame; }
        BLangLambdaFunction lambda = createPassthroughLambda(pos);
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) lambda.function.body;
        BVarSymbol frameSymbol = lambda.function.requiredParams.get(0).symbol;

        // frame["x"] = x;, note: stmts will get added in reverse order.
        List<BVarSymbol> symbols = getIntroducedSymbols(letClause);
        Collections.reverse(symbols);
        for (BVarSymbol symbol : symbols) {
            body.stmts.add(0, addToFrameFunctionStmt(pos, frameSymbol, symbol));
        }

        // TODO: have to mark all closure variables in expression
        // TODO: have to re-write all non closure variables with Frame access
        for (BLangLetVariable letVariable : letClause.letVarDeclarations) {
            // add at 0, otherwise, this goes under existing stmts.
            body.stmts.add(0, (BLangStatement) letVariable.definitionNode);
        }
        lambda.accept(this);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_LET_FUNCTION, Lists.of(lambda), pos);
    }

    /**
     * Desugar whereClause to below and return a reference to created filter _StreamFunction.
     * _StreamFunction xsFilter = createFilterFunction(function(_Frame frame) returns boolean {
     * return <int>frame["x"] > 0;
     * });
     *
     * @param blockStmt   parent block to write to.
     * @param whereClause to be desugared.
     * @return variableReference to created filter _StreamFunction.
     */
    BLangVariableReference addFilterFunction(BLangBlockStmt blockStmt, BLangWhereClause whereClause) {
        DiagnosticPos pos = whereClause.pos;
        BLangLambdaFunction lambda = createFilterLambda(pos);
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) lambda.function.body;
        BLangReturn returnNode = (BLangReturn) TreeBuilder.createReturnNode();
        returnNode.pos = pos;
        returnNode.setExpression(whereClause.expression);
        body.addStatement(returnNode);
        lambda.accept(this);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_FILTER_FUNCTION, Lists.of(lambda), pos);
    }

    /**
     * Desugar selectClause to below and return a reference to created select _StreamFunction.
     * _StreamFunction selectFunc = createSelectFunction(function(_Frame frame) returns _Frame|error? {
     * int x2 = <int> frame["x2"];
     * int y2 = <int> frame["y2"];
     * _Frame frame = {"value": x2 + y2};
     * return frame;
     * });
     *
     * @param blockStmt    parent block to write to.
     * @param selectClause to be desugared.
     * @return variableReference to created select _StreamFunction.
     */
    BLangVariableReference addSelectFunction(BLangBlockStmt blockStmt, BLangSelectClause selectClause) {
        DiagnosticPos pos = selectClause.pos;
        BLangLambdaFunction lambda = createPassthroughLambda(pos);
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) lambda.function.body;
        BVarSymbol frameSymbol = lambda.function.requiredParams.get(0).symbol;
        BLangSimpleVarRef frameRef = ASTBuilderUtil.createVariableRef(pos, frameSymbol);
        // frame = {"value": x2 + y2};;
        // return frame; <- this comes from createPassthroughLambda()
        // TODO: temp solution
//        selectClause.expression.type = frameSymbol.type;
//        BLangAssignment frameAssignment = ASTBuilderUtil.createAssignmentStmt(pos, frameRef, selectClause.expression);
//        body.stmts.add(body.stmts.size() - 1, frameAssignment);
        lambda.accept(this);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_SELECT_FUNCTION, Lists.of(lambda), pos);
    }

    /**
     * Desugar doClause to below and return a reference to created do _StreamFunction.
     * _StreamFunction doFunc = createDoFunction(function(_Frame frame) {
     * int x2 = <int> frame["x2"];
     * int y2 = <int> frame["y2"];
     * });
     *
     * @param blockStmt parent block to write to.
     * @param doClause  to be desugared.
     * @return variableReference to created do _StreamFunction.
     */
    BLangVariableReference addDoFunction(BLangBlockStmt blockStmt, BLangDoClause doClause) {
        DiagnosticPos pos = doClause.pos;
        BLangLambdaFunction lambda = createActionLambda(pos);
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) lambda.function.body;
        for (BLangStatement stmt : doClause.body.stmts) {
            body.addStatement(stmt);
        }
        lambda.accept(this);
        return getStreamFunctionVariableRef(blockStmt, Names.QUERY_CREATE_DO_FUNCTION, Lists.of(lambda), pos);
    }

    /**
     * Desugar to following invocation.
     * stream:addStreamFunction(pipeline, streamFunction);
     *
     * @param blockStmt   parent block to write to.
     * @param pipelineRef variableReference to pipeline.
     * @param functionRef variableReference to stream function.
     */
    void addStreamFunction(BLangBlockStmt blockStmt, BLangVariableReference pipelineRef,
                           BLangVariableReference functionRef) {
        BLangInvocation addStreamFunctionInvocation = createQueryLibInvocation(Names.QUERY_ADD_STREAM_FUNCTION,
                Lists.of(pipelineRef, functionRef), pipelineRef.pos);
        BLangExpressionStmt stmt = ASTBuilderUtil.createExpressionStmt(pipelineRef.pos, blockStmt);
        stmt.expr = addStreamFunctionInvocation;
    }

    /**
     * Desugar to following invocation.
     * stream<any|error, error?> result = xsPipeline.getStream();
     *
     * @param blockStmt   parent block to write to.
     * @param pipelineRef variableReference to pipeline.
     * @return variableReference to stream.
     */
    BLangVariableReference addGetStreamFromPipeline(BLangBlockStmt blockStmt, BLangVariableReference pipelineRef) {
        DiagnosticPos pos = pipelineRef.pos;
        // TODO: instead of null, send the expected type;??
        // TODO: for now type will be stream<any|error, error?> ; we can pass the expected type and add a cast
        BLangVariableReference streamVarRef = getStreamFunctionVariableRef(blockStmt,
                Names.QUERY_GET_STREAM_FROM_PIPELINE_FUNCTION, null, Lists.of(pipelineRef), pos);
        return streamVarRef;
    }

    private BVarSymbol currectFrameSymbol;
    private BLangBlockFunctionBody currectLambdaBody;
    private Map<BLangIdentifier, BVarSymbol> identifiers;

    @Override
    public void visit(BLangLambdaFunction lambda) {
        BLangFunction function = lambda.function;
        currectFrameSymbol = function.requiredParams.get(0).symbol;
        identifiers = new HashMap<>();
        currectLambdaBody = (BLangBlockFunctionBody) function.getBody();
        List<BLangStatement> stmts = new ArrayList<>(currectLambdaBody.getStatements());
        stmts.forEach(stmt -> stmt.accept(this));
        currectFrameSymbol = null;
        identifiers = null;
        currectLambdaBody = null;
    }

    @Override
    public void visit(BLangSimpleVariableDef bLangSimpleVariableDef) {
        bLangSimpleVariableDef.getVariable().accept(this);
    }

    @Override
    public void visit(BLangSimpleVariable bLangSimpleVariable) {
        bLangSimpleVariable.expr.accept(this);
    }

    @Override
    public void visit(BLangTypeConversionExpr conversionExpr) {
        conversionExpr.expr.accept(this);
    }

    @Override
    public void visit(BLangFieldBasedAccess fieldAccessExpr) {
        fieldAccessExpr.expr.accept(this);
    }

    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        exprStmtNode.expr.accept(this);
    }

    @Override
    public void visit(BLangInvocation invocationExpr) {
        invocationExpr.requiredArgs.forEach(arg -> arg.accept(this));
        invocationExpr.restArgs.forEach(arg -> arg.accept(this));
    }

    @Override
    public void visit(BLangLiteral literalExpr) {
        // do nothing;
    }

    @Override
    public void visit(BLangReturn bLangReturn) {
        bLangReturn.expr.accept(this);
    }

    @Override
    public void visit(BLangBinaryExpr bLangBinaryExpr) {
        bLangBinaryExpr.lhsExpr.accept(this);
        bLangBinaryExpr.rhsExpr.accept(this);
    }

    @Override
    public void visit(BLangAssignment bLangAssignment) {
        bLangAssignment.varRef.accept(this);
        bLangAssignment.expr.accept(this);
    }

    @Override
    public void visit(BLangRecordLiteral bLangRecordLiteral) {
        for (RecordLiteralNode.RecordField field : bLangRecordLiteral.fields) {
            ((BLangNode) field).accept(this);
        }
    }

    @Override
    public void visit(BLangRecordLiteral.BLangRecordKeyValueField recordKeyValue) {
        recordKeyValue.key.accept(this);
        recordKeyValue.valueExpr.accept(this);
    }

    @Override
    public void visit(BLangRecordLiteral.BLangRecordSpreadOperatorField spreadOperatorField) {
        spreadOperatorField.expr.accept(this);
    }

    @Override
    public void visit(BLangSimpleVarRef bLangSimpleVarRef) {
        BSymbol symbol = bLangSimpleVarRef.symbol;
        if (symbol == null) {
            if (!identifiers.containsKey(bLangSimpleVarRef.variableName)) {
                // TODO: have to re-write all non closure variables with Frame access
                //      Either define a new variable with the symbol, i.e Person person = <Person> frame["person"];
                DiagnosticPos pos = currectLambdaBody.pos;
                BVarSymbol newsSymbol = new BVarSymbol(0, names.fromIdNode(bLangSimpleVarRef.variableName),
                        this.env.scope.owner.pkgID, symTable.anyType, this.env.scope.owner);
                BLangFieldBasedAccess frameAccessExpr = desugar.getFieldAccessExpression(pos,
                        newsSymbol.name.getValue(), symTable.anyOrErrorType, currectFrameSymbol);
                frameAccessExpr.expr = desugar.addConversionExprIfRequired(frameAccessExpr.expr,
                        types.getSafeType(frameAccessExpr.expr.type, true, false));
                BLangSimpleVariable variable = ASTBuilderUtil.createVariable(pos, null, newsSymbol.type,
                        desugar.addConversionExprIfRequired(frameAccessExpr, newsSymbol.type), newsSymbol);
                BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDef(pos, variable);
                identifiers.put(bLangSimpleVarRef.variableName, newsSymbol);
                currectLambdaBody.stmts.add(0, variableDef);
            }
            bLangSimpleVarRef.type = symTable.anyOrErrorType;
            bLangSimpleVarRef.symbol = identifiers.get(bLangSimpleVarRef.variableName);
        } else {
            BSymbol resolvedSymbol = symResolver.lookupClosureVarSymbol(env, symbol.name, SymTag.VARIABLE);
            resolvedSymbol.closure = (resolvedSymbol != symTable.notFoundSymbol);
        }

    }

    /**
     * Create and return a lambda `function(_Frame frame) returns _Frame|error? {...; return frame;}`
     *
     * @param pos of the lambda.
     * @return created lambda function.
     */
    private BLangLambdaFunction createPassthroughLambda(DiagnosticPos pos) {
        // returns (_Frame|error)?
        BLangUnionTypeNode returnType = getUnionTypeNode();
        // return frame;
        BLangReturn returnNode = (BLangReturn) TreeBuilder.createReturnNode();
        returnNode.pos = pos;
        return createLambdaFunction(pos, returnType, returnNode, true);
    }

    /**
     * Create and return a lambda `function(_Frame frame) returns boolean {...}`
     *
     * @param pos of the lambda.
     * @return created lambda function.
     */
    private BLangLambdaFunction createFilterLambda(DiagnosticPos pos) {
        // returns boolean
        BLangValueType returnType = getBooleanTypeNode();
        return createLambdaFunction(pos, returnType, null, false);
    }

    /**
     * Create and return a lambda `function(_Frame frame) {...}`
     *
     * @param pos of the lambda.
     * @return created lambda function.
     */
    private BLangLambdaFunction createActionLambda(DiagnosticPos pos) {
        // returns ()
        BLangValueType returnType = getNilTypeNode();
        return createLambdaFunction(pos, returnType, null, false);
    }

    /**
     * Creates and return a lambda function without body.
     *
     * @param pos of the lambda.
     * @return created lambda function.
     */
    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos,
                                                     TypeNode returnType,
                                                     BLangReturn returnNode,
                                                     boolean isPassthrough) {
        // load symbol for function query:lambdaTemplate
        BInvokableSymbol templateSymbol = getLambdaTemplateSymbol();
        BVarSymbol templateFrameSymbol = templateSymbol.getParameters().get(0);

        // function(_Frame frame) ... and ref to frame
        BVarSymbol frameSymbol = new BVarSymbol(0, templateFrameSymbol.name,
                this.env.scope.owner.pkgID, templateFrameSymbol.type, this.env.scope.owner);
        BLangSimpleVariable frameVariable = ASTBuilderUtil.createVariable(pos, null,
                frameSymbol.type, null, frameSymbol);
        BLangVariableReference frameVarRef = ASTBuilderUtil.createVariableRef(pos, frameSymbol);

        // lambda body
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) TreeBuilder.createBlockFunctionBodyNode();

        // add `return x;`
        if (returnNode != null) {
            // passthrough will return same frame parameter
            if (isPassthrough) returnNode.setExpression(frameVarRef);
            body.addStatement(returnNode);
        }
        return createLambdaFunction(pos, Lists.of(frameVariable), returnType, body);
    }

    /**
     * Creates and returns a lambda function.
     *
     * @param pos            diagnostic pos.
     * @param requiredParams required parameters.
     * @param returnType     return type of the lambda function.
     * @param lambdaBody     body of the lambda function.
     * @return created lambda function.
     */
    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos,
                                                     List<BLangSimpleVariable> requiredParams,
                                                     TypeNode returnType,
                                                     BLangFunctionBody lambdaBody) {
        return desugar.createLambdaFunction(pos, "$streamLambda$",
                requiredParams, returnType, lambdaBody);
    }

    /**
     * Creates a variable to hold what function invocation returns,
     * and then return a varRef to that variable.
     *
     * @param blockStmt    parent block to write the varDef into.
     * @param functionName function name.
     * @param requiredArgs required args.
     * @param pos          pos diagnostic pos.
     * @return varRef to the created variable.
     */
    private BLangVariableReference getStreamFunctionVariableRef(BLangBlockStmt blockStmt,
                                                                Name functionName,
                                                                List<BLangExpression> requiredArgs,
                                                                DiagnosticPos pos) {
        return getStreamFunctionVariableRef(blockStmt, functionName, null, requiredArgs, pos);
    }

    /**
     * Creates a variable to hold what function invocation returns,
     * and then return a varRef to that variable.
     *
     * @param blockStmt    parent block to write the varDef into.
     * @param functionName function name.
     * @param type         expected type of the variable.
     * @param requiredArgs required args.
     * @param pos          pos diagnostic pos.
     * @return varRef to the created variable.
     */
    private BLangVariableReference getStreamFunctionVariableRef(BLangBlockStmt blockStmt,
                                                                Name functionName,
                                                                BType type,
                                                                List<BLangExpression> requiredArgs,
                                                                DiagnosticPos pos) {
        String name = getNewVarName();
        BLangInvocation queryLibInvocation = createQueryLibInvocation(functionName, requiredArgs, pos);
        type = (type == null) ? queryLibInvocation.type : type;
        BVarSymbol varSymbol = new BVarSymbol(0, new Name(name), env.scope.owner.pkgID, type, env.scope.owner);
        BLangSimpleVariable variable = ASTBuilderUtil.createVariable(pos, name, type,
                desugar.addConversionExprIfRequired(queryLibInvocation, type), varSymbol);
        BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDef(pos, variable);
        blockStmt.addStatement(variableDef);
        return ASTBuilderUtil.createVariableRef(pos, variable.symbol);
    }

    /**
     * Get unique variable name.
     *
     * @return new variable name.
     */
    private String getNewVarName() {
        return "$streamElement$" + streamElementCount++;
    }

    /**
     * Load a function invokable symbol and return a invocation for that function.
     *
     * @param functionName function name.
     * @param requiredArgs list of required args.
     * @param pos          diagnostic pos.
     * @return created invocation.
     */
    private BLangInvocation createQueryLibInvocation(Name functionName,
                                                     List<BLangExpression> requiredArgs,
                                                     DiagnosticPos pos) {
        BInvokableSymbol symbol = getQueryLibInvokableSymbol(functionName);
        BLangInvocation bLangInvocation = ASTBuilderUtil
                .createInvocationExprForMethod(pos, symbol, requiredArgs, symResolver);
        bLangInvocation.type = symbol.retType;
        return bLangInvocation;
    }

    /**
     * Load and return symbol for given functionName in query lib.
     *
     * @param functionName of the function.
     * @return symbol for the function.
     */
    private BInvokableSymbol getQueryLibInvokableSymbol(Name functionName) {
        return (BInvokableSymbol) symTable.langStreamModuleSymbol.scope
                .lookup(functionName).symbol;
    }

    /**
     * Load and return symbol for function query:lambdaTemplate()
     *
     * @return symbol for above function.
     */
    private BInvokableSymbol getLambdaTemplateSymbol() {
        return getQueryLibInvokableSymbol(names.fromString("lambdaTemplate"));
    }

    private BLangExpressionStmt addToFrameFunctionStmt(DiagnosticPos pos,
                                                       BVarSymbol frameSymbol,
                                                       BVarSymbol valueSymbol) {
        BLangVariableReference frameVarRef = ASTBuilderUtil.createVariableRef(pos, frameSymbol);
        BLangVariableReference valueVarRef = ASTBuilderUtil.createVariableRef(pos, valueSymbol);
        BLangLiteral keyLiteral = ASTBuilderUtil.createLiteral(pos, symTable.stringType, valueSymbol.name.value);
        BLangInvocation addToFrameInvocation = createQueryLibInvocation(Names.QUERY_ADD_TO_FRAME_FUNCTION,
                Lists.of(frameVarRef, keyLiteral, valueVarRef), pos);
        final BLangExpressionStmt exprStmt = (BLangExpressionStmt) TreeBuilder.createExpressionStatementNode();
        exprStmt.expr = addToFrameInvocation;
        exprStmt.pos = pos;
        return exprStmt;
    }

    private List<BVarSymbol> getIntroducedSymbols(BLangLetClause letClause) {
        List<BVarSymbol> symbols = new ArrayList<>();
        for (BLangLetVariable letVariable : letClause.letVarDeclarations) {
            symbols.addAll(getIntroducedSymbols(letVariable));
        }
        return symbols;
    }

    private List<BVarSymbol> getIntroducedSymbols(BLangLetVariable variable) {
        return getIntroducedSymbols((BLangVariable) variable.definitionNode.getVariable());
    }

    private List<BVarSymbol> getIntroducedSymbols(BLangVariable variable) {
        if (variable != null) {
            List<BVarSymbol> symbols = new ArrayList<>();
            if (variable.getKind() == NodeKind.RECORD_VARIABLE) {
                // Record binding
                BLangRecordVariable record = (BLangRecordVariable) variable;
                for (BLangRecordVariable.BLangRecordVariableKeyValue keyValue : record.variableList) {
                    symbols.addAll(getIntroducedSymbols(keyValue.valueBindingPattern));
                }
                if (record.hasRestParam()) {
                    symbols.addAll(getIntroducedSymbols((BLangVariable) record.restParam));
                }
            } else if (variable.getKind() == NodeKind.TUPLE_VARIABLE) {
                // Tuple binding
                BLangTupleVariable tuple = (BLangTupleVariable) variable;
                for (BLangVariable memberVariable : tuple.memberVariables) {
                    symbols.addAll(getIntroducedSymbols(memberVariable));
                }
                if (tuple.restVariable != null) {
                    symbols.addAll(getIntroducedSymbols(tuple.restVariable));
                }
            } else if (variable.getKind() == NodeKind.ERROR_VARIABLE) {
                // Error binding
                BLangErrorVariable error = (BLangErrorVariable) variable;
                if (error.reason != null) {
                    symbols.addAll(getIntroducedSymbols(error.reason));
                }
                if (error.restDetail != null) {
                    symbols.addAll(getIntroducedSymbols(error.restDetail));
                }
                for (BLangErrorVariable.BLangErrorDetailEntry entry : error.detail) {
                    symbols.addAll(getIntroducedSymbols(entry.valueBindingPattern));
                }
            } else {
                // Simple binding
                symbols.add(((BLangSimpleVariable) variable).symbol);
            }
            return symbols;
        }
        return Collections.emptyList();
    }

    /**
     * Return BLangValueType of a nil `()` type.
     *
     * @return a nil type node.
     */
    BLangValueType getNilTypeNode() {
        BLangValueType nilTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
        nilTypeNode.typeKind = TypeKind.NIL;
        nilTypeNode.type = symTable.nilType;
        return nilTypeNode;
    }

    /**
     * Return BLangValueType of a any type.
     *
     * @return a any type node.
     */
    BLangValueType getAnyTypeNode() {
        BLangValueType anyTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
        anyTypeNode.typeKind = TypeKind.ANY;
        anyTypeNode.type = symTable.anyType;
        return anyTypeNode;
    }

    /**
     * Return BLangErrorType node.
     *
     * @return a error type node.
     */
    BLangErrorType getErrorTypeNode() {
        BLangErrorType errorTypeNode = (BLangErrorType) TreeBuilder.createErrorTypeNode();
        errorTypeNode.type = symTable.errorType;
        return errorTypeNode;
    }

    /**
     * Return BLangValueType of a boolean type.
     *
     * @return a boolean type node.
     */
    private BLangValueType getBooleanTypeNode() {
        BLangValueType booleanTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
        booleanTypeNode.typeKind = TypeKind.BOOLEAN;
        booleanTypeNode.type = symTable.booleanType;
        return booleanTypeNode;
    }

    /**
     * Return union type node consists of _Frame & error & ().
     *
     * @return a union type node.
     */
    private BLangUnionTypeNode getUnionTypeNode() {
        BInvokableSymbol templateSymbol = getLambdaTemplateSymbol();
        BLangUnionTypeNode unionTypeNode = (BLangUnionTypeNode) TreeBuilder.createUnionTypeNode();
        unionTypeNode.type = templateSymbol.retType;
        unionTypeNode.desugared = true;
        unionTypeNode.memberTypeNodes.add(getFrameTypeNode());
        unionTypeNode.memberTypeNodes.add(getErrorTypeNode());
        unionTypeNode.memberTypeNodes.add(getNilTypeNode());
        return unionTypeNode;
    }

    /**
     * Return _Frame type node.
     *
     * @return a _Frame type node.
     */
    private BLangRecordTypeNode getFrameTypeNode() {
        BRecordTypeSymbol frameTypeSymbol = (BRecordTypeSymbol) symTable.langStreamModuleSymbol.scope
                .lookup(names.fromString("_Frame")).symbol;
        BRecordType frameType = (BRecordType) frameTypeSymbol.type;

        BLangUnionTypeNode restFieldType = (BLangUnionTypeNode) TreeBuilder.createUnionTypeNode();
        restFieldType.type = frameType.restFieldType;
        restFieldType.memberTypeNodes.add(getErrorTypeNode());
        restFieldType.memberTypeNodes.add(getAnyTypeNode());

        BLangRecordTypeNode frameTypeNode = (BLangRecordTypeNode) TreeBuilder.createRecordTypeNode();
        frameTypeNode.type = frameType;
        frameTypeNode.restFieldType = restFieldType;
        frameTypeNode.symbol = frameType.tsymbol;
        frameTypeNode.desugared = true;
        return frameTypeNode;
    }

    BLangStatementExpression desugarQueryAction(BLangQueryAction queryAction, SymbolEnv env) {
        this.env = env;
        List<BLangFromClause> fromClauseList = queryAction.fromClauseList;
        List<BLangLetClause> letClauseList = queryAction.letClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangDoClause doClause = queryAction.doClause;
        List<BLangWhereClause> whereClauseList = queryAction.whereClauseList;
        DiagnosticPos pos = fromClause.pos;
        parentBlock = ASTBuilderUtil.createBlockStmt(fromClause.pos);

        BLangExpression nilExpression = ASTBuilderUtil.createLiteral(pos, symTable.nilType, Names.NIL_VALUE);
        BVarSymbol outputVarSymbol = new BVarSymbol(0, new Name("$outputVar$"),
                env.scope.owner.pkgID, symTable.errorOrNilType, env.scope.owner);
        BLangSimpleVariable outputVariable =
                ASTBuilderUtil.createVariable(pos, "$outputVar$", symTable.errorOrNilType,
                        nilExpression, outputVarSymbol);
        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputVariable.symbol);
        parentBlock.addStatement(outputVariableDef);

        BLangBlockStmt leafElseBlock = buildFromClauseBlock(fromClauseList, outputVarRef);
        buildWhereClauseBlock(whereClauseList, letClauseList, leafElseBlock, doClause.body, doClause.pos);

        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(parentBlock, outputVarRef);
        stmtExpr.type = symTable.errorOrNilType;
        return stmtExpr;
    }

    private BLangBlockStmt buildFromClauseBlock(List<BLangFromClause> fromClauseList, BLangSimpleVarRef outputVarRef) {
        BLangBlockStmt leafElseBody = null;
        for (BLangFromClause fromClause : fromClauseList) {
            // int[] $data$ = personList;
            BVarSymbol dataSymbol = new BVarSymbol(0, names.fromString("$data$"), env.scope.owner.pkgID,
                    fromClause.collection.type, this.env.scope.owner);
            BLangSimpleVariable dataVariable = ASTBuilderUtil.createVariable(fromClause.pos, "$data$",
                    fromClause.collection.type, fromClause.collection, dataSymbol);
            BLangSimpleVariableDef dataVarDef = ASTBuilderUtil.createVariableDef(fromClause.pos, dataVariable);

            // abstract object {public function next() returns record {|Person value;|}? $iterator$ = $data$.iterator();
            BVarSymbol collectionSymbol = dataVariable.symbol;
            BInvokableSymbol iteratorInvSymbol;
            BLangSimpleVariableDef iteratorVarDef;
            if (collectionSymbol.type.tag == TypeTags.OBJECT) {
                iteratorInvSymbol = desugar.getIterableObjectIteratorInvokableSymbol(collectionSymbol);
                iteratorVarDef = desugar.getIteratorVariableDefinition(fromClause.pos,
                        collectionSymbol, iteratorInvSymbol, false);
            } else {
                iteratorInvSymbol = desugar.getLangLibIteratorInvokableSymbol(collectionSymbol);
                iteratorVarDef = desugar.getIteratorVariableDefinition(fromClause.pos,
                        collectionSymbol, iteratorInvSymbol, true);
            }
            BVarSymbol iteratorSymbol = iteratorVarDef.var.symbol;

            // Create a new symbol for the $result$.
            BVarSymbol resultSymbol = new BVarSymbol(0, names.fromString("$result$"),
                    this.env.scope.owner.pkgID, fromClause.nillableResultType, this.env.scope.owner);

            // Note - map<T>? $result$ = $iterator$.next();
            BLangSimpleVariableDef resultVariableDefinition = desugar.getIteratorNextVariableDefinition(fromClause.pos,
                    fromClause.nillableResultType, iteratorSymbol, resultSymbol);
            BLangSimpleVarRef resultReferenceInWhile = ASTBuilderUtil.createVariableRef(fromClause.pos, resultSymbol);

            // create while loop: while (true)
            BLangLiteral conditionLiteral = ASTBuilderUtil.createLiteral(fromClause.pos, symTable.booleanType,
                    true);
            BLangGroupExpr whileCondition = new BLangGroupExpr();
            whileCondition.type = symTable.booleanType;
            whileCondition.expression = conditionLiteral;

            BLangWhile whileNode = (BLangWhile) TreeBuilder.createWhileNode();
            whileNode.expr = whileCondition;

            // if ($result$ is ()){
            //     break;
            // }
            BLangBlockStmt nullCheckIfBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
            nullCheckIfBody.addStatement(TreeBuilder.createBreakNode());
            BLangIf nullCheckIf = createTypeCheckIfNode(fromClause.pos, resultReferenceInWhile,
                    getNilTypeNode(), nullCheckIfBody);

            // if ($result$ is error){
            //    outputDataArray = $result$;
            //    break;
            // }
            BLangBlockStmt errorCheckIfBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
            BLangAssignment errorValueAssignment = ASTBuilderUtil.createAssignmentStmt(fromClause.pos,
                    outputVarRef, resultReferenceInWhile);
            errorCheckIfBody.addStatement(errorValueAssignment);
            errorCheckIfBody.addStatement(TreeBuilder.createBreakNode());
            BLangIf errorCheckIf = createTypeCheckIfNode(fromClause.pos, resultReferenceInWhile,
                    getErrorTypeNode(), errorCheckIfBody);

            nullCheckIf.elseStmt = errorCheckIf;

            // else{
            //     var value = $result$.value;
            //     $tempDataArray$[$tempDataArray$.length()] = value;
            // }
            BLangBlockStmt elseBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
            // Note - $result$ = $iterator$.next(); < this should go after initial assignment of `value`
            BLangAssignment resultAssignment = desugar.getIteratorNextAssignment(fromClause.pos,
                    iteratorSymbol, resultSymbol);
            VariableDefinitionNode variableDefinitionNode = fromClause.variableDefinitionNode;

            // var $value$ = $result$.value;
            BLangFieldBasedAccess valueAccessExpr = desugar.getValueAccessExpression(fromClause.pos,
                    fromClause.varType, resultSymbol);
            valueAccessExpr.expr = desugar.addConversionExprIfRequired(valueAccessExpr.expr,
                    types.getSafeType(valueAccessExpr.expr.type, true, false));
            variableDefinitionNode.getVariable()
                    .setInitialExpression(desugar.addConversionExprIfRequired(valueAccessExpr, fromClause.varType));

            elseBody.stmts.add(0, (BLangStatement) variableDefinitionNode);
            errorCheckIf.elseStmt = elseBody;

            // if($outputDataArray$ is error) {
            //     break;
            // }
            BLangBlockStmt outputErrorCheckIfBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
            outputErrorCheckIfBody.addStatement(TreeBuilder.createBreakNode());
            BLangIf outputErrorCheckIf = createTypeCheckIfNode(fromClause.pos, outputVarRef, getErrorTypeNode(),
                    outputErrorCheckIfBody);

            BLangBlockStmt whileBody = ASTBuilderUtil.createBlockStmt(fromClause.pos);
            whileBody.addStatement(nullCheckIf);
            whileBody.addStatement(outputErrorCheckIf);
            whileBody.addStatement(resultAssignment);
            whileNode.body = whileBody;

            if (leafElseBody != null) {
                BLangBlockStmt childBlock = ASTBuilderUtil.createBlockStmt(fromClause.pos);
                childBlock.addStatement(dataVarDef);
                childBlock.addStatement(iteratorVarDef);
                childBlock.addStatement(resultVariableDefinition);
                childBlock.addStatement(whileNode);
                leafElseBody.addStatement(childBlock);
            } else {
                parentBlock.addStatement(dataVarDef);
                parentBlock.addStatement(iteratorVarDef);
                parentBlock.addStatement(resultVariableDefinition);
                parentBlock.addStatement(whileNode);
            }
            leafElseBody = elseBody;
        }
        return leafElseBody;
    }

    private BLangIf createTypeCheckIfNode(DiagnosticPos pos, BLangExpression expr, BLangType type,
                                          BLangBlockStmt body) {
        BLangTypeTestExpr testExpr = ASTBuilderUtil.createTypeTestExpr(pos, expr, type);
        testExpr.type = symTable.booleanType;
        BLangIf typeCheckIf = (BLangIf) TreeBuilder.createIfElseStatementNode();
        typeCheckIf.pos = pos;
        typeCheckIf.expr = testExpr;
        typeCheckIf.body = body;
        return typeCheckIf;
    }

    private void buildLetClauseBlock(List<BLangLetClause> letClauseList, BLangBlockStmt bLangBlockStmt) {
        // Create variable definitions for the let variable declarations
        if (letClauseList != null) {
            for (BLangLetClause letClause : letClauseList) {
                for (BLangLetVariable letVariable  : letClause.letVarDeclarations) {
                    bLangBlockStmt.addStatement(letVariable.definitionNode);
                }
            }
        }
    }

    private void buildWhereClauseBlock(List<BLangWhereClause> whereClauseList, List<BLangLetClause> letClauseList,
                                       BLangBlockStmt elseBlock, BLangBlockStmt bodyBlock, DiagnosticPos pos) {
        BLangBlockStmt stmtBlock = ASTBuilderUtil.createBlockStmt(pos);
        if (whereClauseList.size() > 0) {
            // Create If Statement with Where expression and foreach body
            BLangIf outerIf = null;
            BLangIf innerIf = null;
            for (BLangWhereClause whereClause : whereClauseList) {
                BLangIf bLangIf = (BLangIf) TreeBuilder.createIfElseStatementNode();
                bLangIf.pos = whereClause.pos;
                bLangIf.expr = whereClause.expression;
                if (innerIf != null) {
                    BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(whereClause.pos);
                    bLangBlockStmt.addStatement(bLangIf);
                    innerIf.setBody(bLangBlockStmt);
                } else {
                    outerIf = bLangIf;
                }
                innerIf = bLangIf;
            }
            innerIf.setBody(bodyBlock);

            buildLetClauseBlock(letClauseList, stmtBlock);
            stmtBlock.addStatement(outerIf);
        } else {
            buildLetClauseBlock(letClauseList, stmtBlock);
            stmtBlock.stmts.addAll(bodyBlock.getStatements());
        }
        elseBlock.getStatements().addAll(stmtBlock.getStatements());
    }

    private BLangInvocation createLangLibInvocation(String functionName, BLangExpression onExpr,
                                                    List<BLangExpression> requiredArgs,
                                                    List<BLangExpression> restArgs,
                                                    BType retType,
                                                    DiagnosticPos pos) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.pos = pos;
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(functionName);
        name.pos = pos;
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();

        invocationNode.symbol = symResolver.lookupLangLibMethod(onExpr.type, names.fromString(functionName));

        invocationNode.argExprs = new ArrayList<BLangExpression>() {{
            add(onExpr);
            addAll(requiredArgs);
            addAll(restArgs);
        }};
        invocationNode.requiredArgs = new ArrayList<BLangExpression>() {{
            add(onExpr);
            addAll(requiredArgs);
        }};
        invocationNode.restArgs = restArgs;
        invocationNode.type = retType != null ? retType : ((BInvokableSymbol) invocationNode.symbol).retType;
        return invocationNode;
    }
}
