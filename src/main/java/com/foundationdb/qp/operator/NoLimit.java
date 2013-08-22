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

package com.foundationdb.qp.operator;

import com.foundationdb.qp.row.RowBase;

public final class NoLimit implements Limit {

    private static final Limit INSTANCE = new NoLimit();

    public static Limit instance() {
        return INSTANCE;
    }

    private NoLimit() {
        // private ctor
    }

    @Override
    public boolean limitReached(RowBase row) {
        return false;
    }

    @Override
    public String toString() {
        return "NO_LIMIT";
    }
}
