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

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.Column;
import com.foundationdb.ais.model.PrimaryKey;
import com.foundationdb.ais.model.UserTable;
import com.foundationdb.server.error.PrimaryKeyNullColumnException;

/**
 * Validates that the columns used in the primary key are all not null. 
 * This is a requirement for the Derby (but not enforced except here).
 * MySQL enforces this by silently making the columns not null.   
 * @author tjoneslo
 *
 */
class PrimaryKeyIsNotNull implements AISValidation {

    @Override
    public void validate(AkibanInformationSchema ais, AISValidationOutput output) {
        for (UserTable table : ais.getUserTables().values()) {
            PrimaryKey index = table.getPrimaryKeyIncludingInternal();
            if(index == null) {
                continue; // Checked by TableHasPrimaryKey
            }
            for (Column column : index.getColumns()) {
                if (column.getNullable()) {
                    output.reportFailure(new AISValidationFailure (
                            new PrimaryKeyNullColumnException (table.getName(), column.getName())));
                }
            }
        }
    }
}
