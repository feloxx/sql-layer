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

package com.akiban.sql.optimizer.plan;

import static com.akiban.server.service.text.FullTextQueryBuilder.BooleanType;

import java.util.ArrayList;
import java.util.List;

public class FullTextBoolean extends FullTextQuery
{
    private List<FullTextQuery> operands;
    private List<BooleanType> types;

    public FullTextBoolean(List<FullTextQuery> operands, List<BooleanType> types) {
        this.operands = operands;
        this.types = types;
    }

    public List<FullTextQuery> getOperands() {
        return operands;
    }
    public List<BooleanType> getTypes() {
        return types;
    }

    public boolean accept(ExpressionVisitor v) {
        for (FullTextQuery operand : operands) {
            if (!operand.accept(v)) {
                return false;
            }
        }
        return true;
    }

    public void accept(ExpressionRewriteVisitor v) {
        for (FullTextQuery operand : operands) {
            operand.accept(v);
        }
    }

    public FullTextBoolean duplicate(DuplicateMap map) {
        List<FullTextQuery> newOperands = new ArrayList<>(operands.size());
        for (FullTextQuery operand : operands) {
            newOperands.add((FullTextQuery)operand.duplicate(map));
        }
        return new FullTextBoolean(newOperands, new ArrayList<>(types));
    }
    
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) {
                str.append(", ");
            }
            str.append(types.get(i));
            str.append("(");
            str.append(operands.get(i));
            str.append(")");
        }
        str.append("]");
        return str.toString();
    }

}