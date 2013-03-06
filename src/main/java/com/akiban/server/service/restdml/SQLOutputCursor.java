/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.service.restdml;

import com.akiban.qp.operator.Cursor;
import com.akiban.qp.row.DelegateRow;
import com.akiban.qp.row.Row;
import com.akiban.server.api.dml.ColumnSelector;
import com.akiban.server.service.externaldata.GenericRowTracker;
import com.akiban.server.service.externaldata.JsonRowWriter;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.sql.embedded.JDBCResultSet;
import com.akiban.sql.embedded.JDBCResultSetMetaData;
import com.akiban.util.AkibanAppender;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

public class SQLOutputCursor extends GenericRowTracker implements Cursor, JsonRowWriter.WriteRow {
    private final Deque<ResultSetHolder> holderStack = new ArrayDeque<>();
    private ResultSetHolder currentHolder;

    public SQLOutputCursor(JDBCResultSet rs) throws SQLException {
        currentHolder = new ResultSetHolder(rs, null, 0);
    }

    //
    // GenericRowTracker
    //

    @Override
    public String getRowName() {
        return currentHolder.name;
    }

    //
    // Cursor
    //

    @Override
    public void open() {
    }

    @Override
    public Row next() {
        if(currentHolder == null) {
            assert holderStack.isEmpty();
            return null;
        }
        try {
            Row row = null;
            if(!holderStack.isEmpty() && holderStack.peek().depth > currentHolder.depth) {
                currentHolder = holderStack.pop();
            }
            if(currentHolder.resultSet.next()) {
                row = currentHolder.resultSet.unwrap(Row.class);
                setDepth(currentHolder.depth);
            } else if(!holderStack.isEmpty()) {
                currentHolder = holderStack.pop();
                row = next();
            } else {
                currentHolder = null;
            }
            return (row != null) ? new RowHolder(row, currentHolder) : null;
        } catch(SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void jump(Row row, ColumnSelector columnSelector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        holderStack.clear();
        currentHolder = null;
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIdle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDestroyed() {
        throw new UnsupportedOperationException();
    }

    //
    // JsonRowWriter.WriteRow
    //

    @Override
    public void write(Row row, AkibanAppender appender) {
        if(!(row instanceof RowHolder)) {
            throw new IllegalArgumentException("Unexpected row: " + row.getClass());
        }
        try {
            RowHolder rowHolder = (RowHolder)row;
            JDBCResultSet resultSet = rowHolder.rsHolder.resultSet;
            JDBCResultSetMetaData metaData = resultSet.getMetaData();
            boolean begun = false;
            boolean savedCurrent = false;
            for(int col = 1; col <= metaData.getColumnCount(); ++col) {
                String colName = metaData.getColumnName(col);
                if(metaData.getNestedResultSet(col) != null) {
                    if(!savedCurrent) {
                        holderStack.push(currentHolder);
                        savedCurrent = true;
                    }
                    JDBCResultSet nested = (JDBCResultSet)resultSet.getObject(col);
                    holderStack.push(new ResultSetHolder(nested, colName, rowHolder.rsHolder.depth + 1));
                } else {
                    PValueSource pValueSource = row.pvalue(col - 1);
                    JsonRowWriter.writeValue(colName, pValueSource, appender, !begun);
                    begun = true;
                }
            }
        } catch(SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class ResultSetHolder {
        public final JDBCResultSet resultSet;
        public final String name;
        public final int depth;

        private ResultSetHolder(JDBCResultSet resultSet, String name, int depth) {
            this.resultSet = resultSet;
            this.name = name;
            this.depth = depth;
        }

        @Override
        public String toString() {
            return name + "(" + depth + ")";
        }
    }

    private static class RowHolder extends DelegateRow {
        public final ResultSetHolder rsHolder;

        public RowHolder(Row delegate, ResultSetHolder rsHolder) {
            super(delegate);
            this.rsHolder = rsHolder;
        }
    }
}
