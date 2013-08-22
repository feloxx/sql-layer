/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.expression.subquery;

import com.foundationdb.qp.operator.Operator;
import com.foundationdb.qp.row.Row;
import com.foundationdb.qp.rowtype.RowType;
import com.foundationdb.server.explain.*;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionEvaluation;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.server.types.extract.BooleanExtractor;
import com.foundationdb.server.types.extract.Extractors;
import com.foundationdb.server.types.util.BoolValueSource;

public final class AnySubqueryExpression extends SubqueryExpression {

    @Override
    public ExpressionEvaluation evaluation() {
        return new InnerEvaluation(subquery(), expression.evaluation(),
                                   outerRowType(), innerRowType(),
                                   bindingPosition());
    }

    @Override
    public AkType valueType() {
        return AkType.BOOL;
    }

    @Override
    public String toString() {
        return "ANY(" + subquery() + ")";
    }

    @Override
    public CompoundExplainer getExplainer(ExplainContext context) {
        CompoundExplainer explainer = super.getExplainer(context);
        explainer.addAttribute(Label.EXPRESSIONS, expression.getExplainer(context));
        return explainer;
    }

    public AnySubqueryExpression(Operator subquery, Expression expression,
                                 RowType outerRowType, RowType innerRowType, 
                                 int bindingPosition) {
        super(subquery, outerRowType, innerRowType, bindingPosition);
        this.expression = expression;
    }
                                 
    private final Expression expression;

    @Override
    public String name()
    {
        return "ANY";
    }

    private static final class InnerEvaluation extends SubqueryExpressionEvaluation {
        @Override
        public ValueSource doEval() {
            expressionEvaluation.of(queryContext());
            expressionEvaluation.of(queryBindings());
            Boolean result = Boolean.FALSE;
            BooleanExtractor extractor = Extractors.getBooleanExtractor();
            while (true) {
                Row row = next();
                if (row == null) break;
                expressionEvaluation.of(row);
                Boolean value = extractor.getBoolean(expressionEvaluation.eval(), null);
                if (value == Boolean.TRUE) {
                    result = value;
                    break;
                }
                else if (value == null) {
                    result = value;
                }
            }
            return BoolValueSource.of(result);
        }

        private InnerEvaluation(Operator subquery,
                                ExpressionEvaluation expressionEvaluation, 
                                RowType outerRowType, RowType innerRowType, 
                                int bindingPosition) {
            super(subquery, outerRowType, innerRowType, bindingPosition);
            this.expressionEvaluation = expressionEvaluation;
        }

        private final ExpressionEvaluation expressionEvaluation;
    }

}
