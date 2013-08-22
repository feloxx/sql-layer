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

package com.foundationdb.server.types3.mcompat.mcasts;

import com.foundationdb.server.types3.TCast;
import static com.foundationdb.server.types3.mcompat.mcasts.MNumericCastBase.*;
import com.foundationdb.server.types3.mcompat.mtypes.MApproximateNumber;
import com.foundationdb.server.types3.mcompat.mtypes.MNumeric;
import com.foundationdb.server.types3.texpressions.Constantness;

public class Cast_From_USmallint
{
     /**
     * TODO:
     * 
     * DATE
     * DATETIME
     * TIME
     * YEAR
     * TIMESTAMP
     * 
     * BIT
     * CHAR
     * BINARY
     * VARBINARY
     * TINYBLOG
     * TINYTEXT
     * TEXT
     * MEDIUMBLOB
     * MEDIUMTEXT
     * LONGBLOG
     * LONTTEXT
     * 
     */
    
    public static final TCast TO_TINYINT = new FromInt32ToInt8(MNumeric.SMALLINT_UNSIGNED, MNumeric.TINYINT, false, Constantness.UNKNOWN);
    
    public static final TCast TO_UNSIGNED_TINYINT = new FromInt32ToUnsignedInt8(MNumeric.SMALLINT_UNSIGNED, MNumeric.TINYINT_UNSIGNED, false, Constantness.UNKNOWN);

    public static final TCast TO_SMALLINT = new FromInt32ToInt16(MNumeric.SMALLINT_UNSIGNED, MNumeric.SMALLINT, false, Constantness.UNKNOWN);
    
    public static final TCast TO_MEDIUMINT = new FromInt32ToInt32(MNumeric.SMALLINT_UNSIGNED, MNumeric.MEDIUMINT, false, Constantness.UNKNOWN);
    
    public static final TCast TO_UNSIGNED_MEDIUMINT = new FromInt32ToUnsignedInt32(MNumeric.SMALLINT_UNSIGNED, MNumeric.MEDIUMINT_UNSIGNED, true, Constantness.UNKNOWN);
    
    public static final TCast TO_INT = new FromInt32ToInt32(MNumeric.SMALLINT_UNSIGNED, MNumeric.INT, false, Constantness.UNKNOWN);
    
    public static final TCast TO_UNSIGNED_INT = new FromInt32ToUnsignedInt32(MNumeric.SMALLINT_UNSIGNED, MNumeric.INT_UNSIGNED, true, Constantness.UNKNOWN);
    
    public static final TCast TO_BIGINT = new FromInt32ToInt64(MNumeric.SMALLINT_UNSIGNED, MNumeric.BIGINT, false, Constantness.UNKNOWN);

    public static final TCast TO_UNSIGNED_BIGINT = new FromInt32ToInt64(MNumeric.SMALLINT_UNSIGNED, MNumeric.BIGINT_UNSIGNED, true, Constantness.UNKNOWN);

    public static final TCast TO_DOUBLE = new FromInt32ToDouble(MNumeric.SMALLINT_UNSIGNED, MApproximateNumber.DOUBLE, false, Constantness.UNKNOWN);

    public static final TCast TO_DECIMAL = new FromInt32ToDecimal(MNumeric.SMALLINT_UNSIGNED, MNumeric.DECIMAL, false, Constantness.UNKNOWN);
}
