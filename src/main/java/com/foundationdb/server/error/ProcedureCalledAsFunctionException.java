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
package com.foundationdb.server.error;

import com.foundationdb.ais.model.TableName;

public class ProcedureCalledAsFunctionException extends BaseSQLException {
    public ProcedureCalledAsFunctionException(String schemaName, String routineName) {
        super(ErrorCode.PROCEDURE_CALLED_AS_FUNCTION, schemaName, routineName, null);
    }
    
    public ProcedureCalledAsFunctionException(TableName name) {
        super(ErrorCode.PROCEDURE_CALLED_AS_FUNCTION, name.getSchemaName(), name.getTableName(), null);
    }

}
