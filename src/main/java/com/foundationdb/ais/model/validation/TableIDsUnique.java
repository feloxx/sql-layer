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

package com.foundationdb.ais.model.validation;

import java.util.Map;
import java.util.TreeMap;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.Table;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.ais.model.UserTable;
import com.foundationdb.server.error.DuplicateTableIdException;

class TableIDsUnique implements AISValidation {
    @Override
    public void validate(AkibanInformationSchema ais, AISValidationOutput output) {
        final Map<Integer, Table> tableIDList= new TreeMap<>();
        for (UserTable table : ais.getUserTables().values()) {
            checkTableID (output, tableIDList, table);
        }
    }
    
    private void checkTableID (AISValidationOutput failures, Map<Integer, Table> tableIDList, Table table) {
        if (tableIDList.containsKey(table.getTableId())) {
            TableName name = tableIDList.get(table.getTableId()).getName();
            
            failures.reportFailure(new AISValidationFailure (
                    new DuplicateTableIdException(table.getName(), name)));
        } else {
            tableIDList.put(table.getTableId(), table);
        }
    }
}
