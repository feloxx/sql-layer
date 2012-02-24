/**
 * Copyright (C) 2012 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.sql.optimizer.rule;

import com.akiban.sql.optimizer.rule.join_enum.*;

import com.akiban.sql.optimizer.plan.*;
import com.akiban.sql.optimizer.plan.Sort.OrderByExpression;
import com.akiban.sql.optimizer.plan.JoinNode.JoinType;

import com.akiban.server.error.UnsupportedSQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/** Pick joins and indexes. 
 * This the the core of actual query optimization.
 */
public class JoinAndIndexPicker extends BaseRule
{
    private static final Logger logger = LoggerFactory.getLogger(JoinAndIndexPicker.class);

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void apply(PlanContext planContext) {
        BaseQuery query = (BaseQuery)planContext.getPlan();
        List<Picker> pickers = 
          new JoinsFinder(((SchemaRulesContext)planContext.getRulesContext())
                          .getCostEstimator()).find(query);
        for (Picker picker : pickers) {
            picker.apply();
        }
    }

    static class Picker {
        Map<SubquerySource,Picker> subpickers;
        CostEstimator costEstimator;
        Joinable joinable;
        BaseQuery query;
        QueryIndexGoal queryGoal;

        public Picker(Joinable joinable, BaseQuery query,
                      CostEstimator costEstimator, 
                      Map<SubquerySource,Picker> subpickers) {
            this.subpickers = subpickers;
            this.costEstimator = costEstimator;
            this.joinable = joinable;
            this.query = query;
        }

        public void apply() {
            queryGoal = determineQueryIndexGoal(joinable);
            if (joinable instanceof TableGroupJoinTree) {
                // Single group.
                pickIndex((TableGroupJoinTree)joinable);
            }
            else if (joinable instanceof JoinNode) {
                // General joins.
                pickJoinsAndIndexes((JoinNode)joinable);
            }
            else if (joinable instanceof SubquerySource) {
                // Single subquery // view. Just do its insides.
                subpicker((SubquerySource)joinable).apply();
            }
            // TODO: Any other degenerate cases?
        }

        protected QueryIndexGoal determineQueryIndexGoal(PlanNode input) {
            ConditionList whereConditions = null;
            Sort ordering = null;
            AggregateSource grouping = null;
            Project projectDistinct = null;
            input = input.getOutput();
            if (input instanceof Select) {
                ConditionList conds = ((Select)input).getConditions();
                if (!conds.isEmpty()) {
                    whereConditions = conds;
                }
            }
            input = input.getOutput();
            if (input instanceof Sort) {
                ordering = (Sort)input;
            }
            else if (input instanceof AggregateSource) {
                grouping = (AggregateSource)input;
                if (!grouping.hasGroupBy())
                    grouping = null;
                input = input.getOutput();
                if (input instanceof Select)
                    input = input.getOutput();
                if (input instanceof Sort) {
                    // Needs to be possible to satisfy both.
                    ordering = (Sort)input;
                    if (grouping != null) {
                        List<ExpressionNode> groupBy = grouping.getGroupBy();
                        for (OrderByExpression orderBy : ordering.getOrderBy()) {
                            ExpressionNode orderByExpr = orderBy.getExpression();
                            if (!((orderByExpr.isColumn() &&
                                   (((ColumnExpression)orderByExpr).getTable() == grouping)) ||
                                  groupBy.contains(orderByExpr))) {
                                ordering = null;
                                break;
                            }
                        }
                    }
                }
            }
            else if (input instanceof Project) {
                Project project = (Project)input;
                input = project.getOutput();
                if (input instanceof Distinct)
                    projectDistinct = project;
                else if (input instanceof Sort)
                    ordering = (Sort)input;
            }
            return new QueryIndexGoal(query, costEstimator, whereConditions, 
                                      grouping, ordering, projectDistinct);
        }

        // Only a single group of tables. Don't need to run general
        // join algorithm and can shortcut some of the setup for this
        // group.
        protected void pickIndex(TableGroupJoinTree tables) {
            GroupIndexGoal groupGoal = new GroupIndexGoal(queryGoal, tables);
            groupGoal.updateRequiredColumns(); // No more joins / bound tables.
            IndexScan index = groupGoal.pickBestIndex();
        }

        // General joins: run enumerator.
        protected void pickJoinsAndIndexes(JoinNode joins) {
            new JoinEnumerator(this).run(joins, queryGoal.getWhereConditions());
        }

        // Get the handler for the given subquery so that it can be done in context.
        public Picker subpicker(SubquerySource subquery) {
            return subpickers.get(subquery);
        }

    }

    static class JoinEnumerator extends DPhyp<Object> {
        private Picker picker;

        public JoinEnumerator(Picker picker) {
            this.picker = picker;
        }

        @Override
        public Object evaluateTable(Joinable table) {
            return null;
        }

        @Override
        public Object evaluateJoin(Object p1, Object p2, Object existing,
                                   JoinType joinType, Collection<JoinOperator> joins) {
            return null;
        }
    }
    
    // Purpose is twofold: 
    // Find top-level joins and note what query they come from; 
    // Annotate subqueries with their outer table references.
    // Top-level queries and those used in expressions are returned directly.
    // Derived tables are deferred, since they need to be planned in
    // the context of various join orders to allow for join predicated
    // to be pushed "inside." So they are stored in a Map accessible
    // to other Pickers.
    static class JoinsFinder implements PlanVisitor, ExpressionVisitor {
        List<Picker> result;
        Map<SubquerySource,Picker> subpickers;
        BaseQuery rootQuery;
        Deque<SubqueryState> subqueries = new ArrayDeque<SubqueryState>();
        CostEstimator costEstimator;

        public JoinsFinder(CostEstimator costEstimator) {
            this.costEstimator = costEstimator;
        }

        public List<Picker> find(BaseQuery query) {
            result = new ArrayList<Picker>();
            subpickers = new HashMap<SubquerySource,Picker>();
            rootQuery = query;
            query.accept(this);
            result.removeAll(subpickers.values());
            return result;
        }

        @Override
        public boolean visitEnter(PlanNode n) {
            if (n instanceof Subquery) {
                subqueries.push(new SubqueryState((Subquery)n));
                return true;
            }
            return visit(n);
        }

        @Override
        public boolean visitLeave(PlanNode n) {
            if (n instanceof Subquery) {
                SubqueryState s = subqueries.pop();
                s.subquery.setOuterTables(s.getTablesReferencedButNotDefined());
            }
            return true;
        }

        @Override
        public boolean visit(PlanNode n) {
            if (!subqueries.isEmpty() &&
                (n instanceof ColumnSource)) {
                boolean added = subqueries.peek().tablesDefined.add((ColumnSource)n);
                assert added : "Table defined more than once";
            }
            if (n instanceof Joinable) {
                Joinable j = (Joinable)n;
                while (j.getOutput() instanceof Joinable)
                    j = (Joinable)j.getOutput();
                BaseQuery query = rootQuery;
                SubquerySource subquerySource = null;
                if (!subqueries.isEmpty()) {
                    query = subqueries.peek().subquery;
                    if (query.getOutput() instanceof SubquerySource)
                        subquerySource = (SubquerySource)query.getOutput();
                }
                for (Picker picker : result) {
                    if (picker.joinable == j)
                        // Already have another set of joins to same root join.
                        return true;
                }
                Picker picker = new Picker(j, query, costEstimator, subpickers);
                result.add(picker);
                if (subquerySource != null)
                    subpickers.put(subquerySource, picker);
            }
            return true;
        }

        @Override
        public boolean visitEnter(ExpressionNode n) {
            return visit(n);
        }

        @Override
        public boolean visitLeave(ExpressionNode n) {
            return true;
        }

        @Override
        public boolean visit(ExpressionNode n) {
            if (!subqueries.isEmpty() &&
                (n instanceof ColumnExpression)) {
                subqueries.peek().tablesReferenced.add(((ColumnExpression)n).getTable());
            }
            return true;
        }
    }

    static class SubqueryState {
        Subquery subquery;
        Set<ColumnSource> tablesReferenced = new HashSet<ColumnSource>();
        Set<ColumnSource> tablesDefined = new HashSet<ColumnSource>();

        public SubqueryState(Subquery subquery) {
            this.subquery = subquery;
        }

        public Set<ColumnSource> getTablesReferencedButNotDefined() {
            tablesReferenced.removeAll(tablesDefined);
            return tablesReferenced;
        }
    }

}
