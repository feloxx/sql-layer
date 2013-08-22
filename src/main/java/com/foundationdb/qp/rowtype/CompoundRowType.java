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

package com.foundationdb.qp.rowtype;

import java.util.ArrayList;
import java.util.List;

import com.foundationdb.ais.model.UserTable;
import com.foundationdb.server.explain.CompoundExplainer;
import com.foundationdb.server.explain.ExplainContext;
import com.foundationdb.server.explain.Label;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types3.TInstance;

public class CompoundRowType extends DerivedRowType {

    @Override
    public int nFields() {
        return nFields;
    }

    @Override
    public AkType typeAt(int index) {
        if (index < first.nFields())
            return first.typeAt(index);
        return second.typeAt(index - first.nFields());
    }

    @Override
    public TInstance typeInstanceAt(int index) {
        if (index < first.nFields())
            return first.typeInstanceAt(index);
        return second.typeInstanceAt(index - first.nFields());
    }
    
    public RowType first() {
        return first;
    }
    
    public RowType second() {
        return second;
    }
    
    protected CompoundRowType(DerivedTypesSchema schema, int typeId, RowType first, RowType second) {
        super(schema, typeId);

        assert first.schema() == schema : first;
        assert second.schema() == schema : second;
        
        this.first = first;
        this.second = second; 
        this.nFields = first.nFields() + second.nFields();

        List<UserTable> tables = new ArrayList<>(first.typeComposition().tables());
        tables.addAll(second.typeComposition().tables());
        typeComposition(new TypeComposition(this, tables));
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CompoundRowType that = (CompoundRowType) o;

        if (second != null ? !second.equals(that.second) : that.second != null) return false;
        if (first != null ? !first.equals(that.first) : that.first != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (first != null ? first.hashCode() : 0);
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
    
    private final RowType first;
    private final RowType second;
    protected int nFields;

}
