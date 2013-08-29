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

package com.foundationdb.ais.util;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.CharsetAndCollation;
import com.foundationdb.ais.model.Index;
import com.foundationdb.ais.model.IndexName;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.ais.model.UserTable;
import com.foundationdb.ais.model.aisb2.AISBBasedBuilder;
import com.foundationdb.ais.model.aisb2.NewAISBuilder;
import com.foundationdb.ais.model.aisb2.NewUserTableBuilder;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.foundationdb.ais.util.ChangedTableDescription.ParentChange;
import static com.foundationdb.ais.util.TableChangeValidator.ChangeLevel;
import static com.foundationdb.ais.util.TableChangeValidatorException.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TableChangeValidatorTest {
    private static final String SCHEMA = "test";
    private static final String TABLE = "t";
    private static final TableName TABLE_NAME = new TableName(SCHEMA, TABLE);
    private static final String NO_INDEX_CHANGE = "{}";
    private static final String NO_IDENTITY_CHANGE = "";

    private List<TableChange> NO_CHANGES = new ArrayList<>();

    @After
    public void clearChanges() {
        NO_CHANGES.clear();
    }

    private static NewUserTableBuilder builder(TableName name) {
        return AISBBasedBuilder.create(SCHEMA).userTable(name);
    }

    private UserTable table(NewAISBuilder builder) {
        AkibanInformationSchema ais = builder.ais();
        assertEquals("User table count", 1, ais.getUserTables().size());
        return ais.getUserTables().values().iterator().next();
    }

    private UserTable table(NewAISBuilder builder, TableName tableName) {
        UserTable table = builder.ais().getUserTable(tableName);
        assertNotNull("Found table: " + tableName, table);
        return table;
    }

    private static TableChangeValidator validate(UserTable t1, UserTable t2,
                                                 List<TableChange> columnChanges, List<TableChange> indexChanges,
                                                 ChangeLevel expectedChangeLevel) {
        return validate(t1, t2, columnChanges, indexChanges, expectedChangeLevel,
                        asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, "PRIMARY", "PRIMARY")),
                        false, false, NO_INDEX_CHANGE, false, NO_IDENTITY_CHANGE);
    }

    private static TableChangeValidator validate(UserTable t1, UserTable t2,
                                                 List<TableChange> columnChanges, List<TableChange> indexChanges,
                                                 ChangeLevel expectedChangeLevel,
                                                 List<String> expectedChangedTables) {
        return validate(t1, t2, columnChanges, indexChanges, expectedChangeLevel,
                        expectedChangedTables,
                        false, false, NO_INDEX_CHANGE, false, NO_IDENTITY_CHANGE);
    }

    private static TableChangeValidator validate(UserTable t1, UserTable t2,
                                                 List<TableChange> columnChanges, List<TableChange> indexChanges,
                                                 ChangeLevel expectedChangeLevel,
                                                 List<String> expectedChangedTables,
                                                 boolean expectedParentChange,
                                                 boolean expectedPrimaryKeyChange,
                                                 String expectedAutoGroupIndexChange,
                                                 boolean autoIndexChanges,
                                                 String expectedIdentityChange) {
        TableChangeValidator validator = new TableChangeValidator(t1, t2, columnChanges, indexChanges, autoIndexChanges);
        validator.compareAndThrowIfNecessary();
        assertEquals("Final change level", expectedChangeLevel, validator.getFinalChangeLevel());
        assertEquals("Parent changed", expectedParentChange, validator.isParentChanged());
        assertEquals("Primary key changed", expectedPrimaryKeyChange, validator.isPrimaryKeyChanged());
        assertEquals("Changed tables", expectedChangedTables.toString(), validator.getAllChangedTables().toString());
        assertEquals("Affected group index", expectedAutoGroupIndexChange, validator.getAffectedGroupIndexes().toString());
        assertEquals("Unmodified changes", "[]", validator.getUnmodifiedChanges().toString());
        assertEquals("Changed identity", expectedIdentityChange, identityChangeDesc(validator.getAllChangedTables()));
        return validator;
    }

    private static Map<String,String> map(String... pairs) {
        assertTrue("Even number of pairs", (pairs.length % 2) == 0);
        Map<String,String> map = new TreeMap<>();
        for(int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i+1]);
        }
        return map;
    }

    private static String changeDesc(TableName oldName, TableName newName, boolean newGroup, ParentChange parentChange, String... indexPairs) {
        return ChangedTableDescription.toString(oldName, newName, newGroup, parentChange, map(indexPairs));
    }

    private static String identityChangeDesc(Collection<ChangedTableDescription> tableChanges) {
        StringBuilder str = new StringBuilder();
        for (ChangedTableDescription change : tableChanges) {
            if (!change.getDroppedSequences().isEmpty()) {
                str.append("-").append(change.getDroppedSequences());
            }
            if (!change.getIdentityAdded().isEmpty()) {
                str.append("+").append(change.getIdentityAdded());
            }
        }
        return str.toString();
    }
    
    //
    // Table
    //

    @Test
    public void sameTable() {
        UserTable t = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t, t, NO_CHANGES, NO_CHANGES, ChangeLevel.NONE);
    }

    @Test
    public void unchangedTable() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, ChangeLevel.NONE);
    }

    @Test
    public void changeOnlyTableName() {
        TableName name2 = new TableName("x", "y");
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(name2).colBigInt("id").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, ChangeLevel.METADATA,
                 asList(changeDesc(TABLE_NAME, name2, false, ParentChange.NONE, "PRIMARY", "PRIMARY")));
    }

    @Test
    public void changeDefaultCharset() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        t1.setCharsetAndCollation(CharsetAndCollation.intern("utf8", "binary"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        t1.setCharsetAndCollation(CharsetAndCollation.intern("utf16", "binary"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, ChangeLevel.METADATA);
    }

    @Test
    public void changeDefaultCollation() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        t1.setCharsetAndCollation(CharsetAndCollation.intern("utf8", "binary"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        t1.setCharsetAndCollation(CharsetAndCollation.intern("utf8", "utf8_general_ci"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, ChangeLevel.METADATA);
    }

    //
    // Column
    //

    @Test
    public void addColumn() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        validate(t1, t2, asList(TableChange.createAdd("x")), NO_CHANGES, ChangeLevel.TABLE);
    }

    @Test
    public void addIdentityColumn() {
        final TableName SEQ_NAME = new TableName(SCHEMA, "seq-1");
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("x"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("x").colBigInt("id").pk("id").sequence(SEQ_NAME.getTableName()));
        t2.getColumn("id").setIdentityGenerator(t2.getAIS().getSequence(SEQ_NAME));
        t2.getColumn("id").setDefaultIdentity(true);
        validate(t1, t2, asList(TableChange.createAdd("id")), asList(TableChange.createAdd("PRIMARY")), ChangeLevel.GROUP,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE)),
                 false, true, NO_INDEX_CHANGE, false, "+[id]");
    }

    @Test
    public void dropColumn() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, asList(TableChange.createDrop("x")), NO_CHANGES, ChangeLevel.TABLE);
    }

    @Test
    public void modifyColumnDataType() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("y").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colString("y", 32).pk("id"));
        validate(t1, t2, asList(TableChange.createModify("y", "y")), NO_CHANGES, ChangeLevel.TABLE);
    }

    @Test
    public void modifyColumnName() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("y").pk("id"));
        validate(t1, t2, asList(TableChange.createModify("x", "y")), NO_CHANGES, ChangeLevel.METADATA);
    }

    @Test
    public void modifyColumnNotNullToNull() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", true).pk("id"));
        validate(t1, t2, asList(TableChange.createModify("x", "x")), NO_CHANGES, ChangeLevel.METADATA);
    }

    @Test
    public void modifyColumnNullToNotNull() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", true).pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).pk("id"));
        validate(t1, t2, asList(TableChange.createModify("x", "x")), NO_CHANGES, ChangeLevel.METADATA_NOT_NULL);
    }

    @Test
    public void modifyAddGeneratedBy() {
        final TableName SEQ_NAME = new TableName(SCHEMA, "seq-1");
        UserTable t1 = table(builder(TABLE_NAME).colLong("id", false).pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colLong("id", false).pk("id").sequence(SEQ_NAME.getTableName()));
        t2.getColumn("id").setIdentityGenerator(t2.getAIS().getSequence(SEQ_NAME));
        t2.getColumn("id").setDefaultIdentity(true);
        validate(t1, t2, asList(TableChange.createModify("id", "id")), NO_CHANGES, ChangeLevel.METADATA,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, "PRIMARY", "PRIMARY")),
                 false, false, NO_INDEX_CHANGE, false,
                 "+[id]");
    }

    @Test
    public void modifyDropGeneratedBy() {
        final TableName SEQ_NAME = new TableName(SCHEMA, "seq-1");
        UserTable t1 = table(builder(TABLE_NAME).colLong("id", false).pk("id").sequence(SEQ_NAME.getTableName()));
        t1.getColumn("id").setIdentityGenerator(t1.getAIS().getSequence(SEQ_NAME));
        t1.getColumn("id").setDefaultIdentity(true);
        UserTable t2 = table(builder(TABLE_NAME).colLong("id", false).pk("id"));
        validate(t1, t2, asList(TableChange.createModify("id", "id")), NO_CHANGES, ChangeLevel.METADATA,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, "PRIMARY", "PRIMARY")),
                 false, false, NO_INDEX_CHANGE, false,
                 "-[test.seq-1]");
    }

    @Test
    public void modifyColumnAddDefault() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).colLong("c1", true).pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).colLong("c1", true).pk("id"));
        t2.getColumn("c1").setDefaultValue("42");
        validate(t1, t2, asList(TableChange.createModify("c1", "c1")), NO_CHANGES, ChangeLevel.METADATA);
    }

    @Test
    public void modifyColumnDropDefault() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).colLong("c1", true).pk("id"));
        t1.getColumn("c1").setDefaultValue("42");
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x", false).colLong("c1", true).pk("id"));
        validate(t1, t2, asList(TableChange.createModify("c1", "c1")), NO_CHANGES, ChangeLevel.METADATA);
    }

    //
    // Column (negative)
    //

    @Test(expected=UndeclaredColumnChangeException.class)
    public void addColumnUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    @Test(expected=UnchangedColumnNotPresentException.class)
    public void dropColumnUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    @Test(expected=DropColumnNotPresentException.class)
    public void dropColumnUnknown() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, asList(TableChange.createDrop("x")), NO_CHANGES, null);
    }

    @Test
    public void modifyColumnNotChanged() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        TableChangeValidator tcv = new TableChangeValidator(t1, t2, asList(TableChange.createModify("x", "x")), NO_CHANGES, false);
        tcv.compareAndThrowIfNecessary();
        assertEquals("Final change level", ChangeLevel.NONE, tcv.getFinalChangeLevel());
        assertEquals("Unmodified change count", 1, tcv.getUnmodifiedChanges().size());
    }

    @Test(expected=ModifyColumnNotPresentException.class)
    public void modifyColumnUnknown() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, asList(TableChange.createModify("y", "y")), NO_CHANGES, null);
    }

    @Test(expected=UndeclaredColumnChangeException.class)
    public void modifyColumnUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colString("x", 32).pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    //
    // Index
    //

    @Test
    public void addIndex() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createAdd("x")), ChangeLevel.INDEX);
    }

    @Test
    public void dropIndex() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createDrop("x")), ChangeLevel.INDEX);
    }

    @Test
    public void modifyIndexedColumn() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").colBigInt("y").key("k", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").colBigInt("y").key("k", "y").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createModify("k", "k")), ChangeLevel.INDEX);
    }

    @Test
    public void modifyIndexedType() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colString("x", 32).key("x", "x").pk("id"));
        validate(t1, t2, asList(TableChange.createModify("x", "x")), asList(TableChange.createModify("x", "x")),
                 ChangeLevel.TABLE);
    }

    @Test
    public void modifyIndexName() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("a", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("b", "x").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createModify("a", "b")), ChangeLevel.METADATA);
    }

    //
    // Index (negative)
    //

    @Test(expected=UndeclaredIndexChangeException.class)
    public void addIndexUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    @Test(expected=UnchangedIndexNotPresentException.class)
    public void dropIndexUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    @Test(expected=DropIndexNotPresentException.class)
    public void dropIndexUnknown() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createDrop("x")), null);
    }

    @Test
    public void modifyIndexNotChanged() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        TableChangeValidator tcv = new TableChangeValidator(t1, t2, NO_CHANGES, asList(
                TableChange.createModify("x", "x")), false);
        tcv.compareAndThrowIfNecessary();
        assertEquals("Final change level", ChangeLevel.NONE, tcv.getFinalChangeLevel());
        assertEquals("Unmodified change count", 1, tcv.getUnmodifiedChanges().size());
    }

    @Test(expected=ModifyIndexNotPresentException.class)
    public void modifyIndexUnknown() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        validate(t1, t2, NO_CHANGES, asList(TableChange.createModify("y", "y")), null);
    }

    @Test(expected=UndeclaredIndexChangeException.class)
    public void modifyIndexUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").colBigInt("y").key("k", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").colBigInt("y").key("k", "y").pk("id"));
        validate(t1, t2, NO_CHANGES, NO_CHANGES, null);
    }

    @Test(expected=UndeclaredIndexChangeException.class)
    public void modifyIndexedColumnIndexUnspecified() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").key("x", "x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colString("x", 32).key("x", "x").pk("id"));
        validate(t1, t2, asList(TableChange.createModify("x", "x")), NO_CHANGES, null);
    }

    //
    // Group
    //

    @Test
    public void modifyPKColumnTypeSingleTableGroup() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colString("id", 32).pk("id"));
        validate(t1, t2,
                 asList(TableChange.createModify("id", "id")),
                 asList(TableChange.createModify(Index.PRIMARY_KEY_CONSTRAINT, Index.PRIMARY_KEY_CONSTRAINT)),
                 ChangeLevel.GROUP,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE)),
                 false, true, NO_INDEX_CHANGE, false, NO_IDENTITY_CHANGE);
    }

    @Test
    public void dropPrimaryKeySingleTableGroup() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id"));
        validate(t1, t2,
                 NO_CHANGES,
                 asList(TableChange.createDrop(Index.PRIMARY_KEY_CONSTRAINT)),
                 ChangeLevel.GROUP,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE)),
                 false, true, NO_INDEX_CHANGE, false, NO_IDENTITY_CHANGE);
    }

    @Test
    public void dropParentJoinTwoTableGroup() {
        TableName parentName = new TableName(SCHEMA, "parent");
        UserTable t1 = table(
                builder(parentName).colLong("id").pk("id").
                        userTable(TABLE_NAME).colBigInt("id").colLong("pid").pk("id").joinTo(SCHEMA, "parent", "fk").on("pid", "id"),
                TABLE_NAME
        );
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colLong("pid").pk("id"));
        validate(t1, t2,
                 NO_CHANGES,
                 asList(TableChange.createDrop("__akiban_fk")),
                 ChangeLevel.GROUP,
                 asList(changeDesc(TABLE_NAME, TABLE_NAME, true, ParentChange.DROP)),
                 true, false, NO_INDEX_CHANGE, false, NO_IDENTITY_CHANGE);
    }

    @Test
    public void dropPrimaryKeyMiddleOfGroup() {
        TableName cName = new TableName(SCHEMA, "c");
        TableName oName = new TableName(SCHEMA, "o");
        TableName iName = new TableName(SCHEMA, "i");
        NewAISBuilder builder1 = AISBBasedBuilder.create();
        builder1.userTable(cName).colBigInt("id", false).pk("id")
                .userTable(oName).colBigInt("id", false).colBigInt("cid", true).pk("id").joinTo(SCHEMA, "c", "fk1").on("cid", "id")
                .userTable(iName).colBigInt("id", false).colBigInt("oid", true).pk("id").joinTo(SCHEMA, "o", "fk2").on("oid", "id");
        NewAISBuilder builder2 = AISBBasedBuilder.create();
        builder2.userTable(cName).colBigInt("id", false).pk("id")
                .userTable(oName).colBigInt("id", false).colBigInt("cid", true).joinTo(SCHEMA, "c", "fk1").on("cid", "id")
                .userTable(iName).colBigInt("id", false).colBigInt("oid", true).pk("id").joinTo(SCHEMA, "o", "fk2").on("oid", "id");
        UserTable t1 = builder1.unvalidatedAIS().getUserTable(oName);
        UserTable t2 = builder2.unvalidatedAIS().getUserTable(oName);
        validate(
                t1, t2,
                NO_CHANGES,
                asList(TableChange.createDrop(Index.PRIMARY_KEY_CONSTRAINT)),
                ChangeLevel.GROUP,
                asList(
                        changeDesc(oName, oName, false, ParentChange.NONE),
                        changeDesc(iName, iName, true, ParentChange.DROP)
                ),
                false,
                true,
                NO_INDEX_CHANGE,
                false,
                NO_IDENTITY_CHANGE
        );
    }

    //
    // Multi-part
    //

    @Test
    public void addAndDropColumn() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("x").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colBigInt("y").pk("id"));
        validate(t1, t2, asList(TableChange.createDrop("x"), TableChange.createAdd("y")), NO_CHANGES, ChangeLevel.TABLE);
    }

    @Test
    public void addAndDropMultipleColumnAndIndex() {
        UserTable t1 = table(builder(TABLE_NAME).colBigInt("id").colDouble("d").colLong("l").colString("s", 32).
                key("d", "d").key("l", "l").uniqueKey("k", "l", "d").pk("id"));
        UserTable t2 = table(builder(TABLE_NAME).colBigInt("id").colDouble("d").colVarBinary("v", 32).colString("s", 64).
                key("d", "d").key("v", "v").uniqueKey("k", "v", "d").pk("id"));
        validate(
                t1, t2,
                asList(TableChange.createDrop("l"), TableChange.createModify("s", "s"), TableChange.createAdd("v")),
                asList(TableChange.createDrop("l"), TableChange.createAdd("v"), TableChange.createModify("k", "k")),
                ChangeLevel.TABLE,
                asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, "PRIMARY", "PRIMARY", "d", "d"))
        );
    }

    //
    // Auto index changes
    //

    @Test
    public void addDropAndModifyIndexAutoChanges() {
        UserTable t1 = table(builder(TABLE_NAME).colLong("c1").colLong("c2").colLong("c3").key("c1", "c1").key("c3", "c3"));
        UserTable t2 = table(builder(TABLE_NAME).colLong("c1").colLong("c2").colString("c3", 32).key("c2", "c2").key("c3", "c3"));
        validate(
                t1, t2,
                asList(TableChange.createModify("c3", "c3")),
                NO_CHANGES,
                ChangeLevel.TABLE,
                asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, "PRIMARY", "PRIMARY")),
                false,
                false,
                NO_INDEX_CHANGE,
                true, 
                NO_IDENTITY_CHANGE
        );
    }

    //
    // Group Index changes
    //

    @Test
    public void dropColumnInGroupIndex() {
        NewAISBuilder builder = AISBBasedBuilder.create(SCHEMA);
        builder.userTable("p").colLong("id").colLong("x").pk("id")
               .userTable(TABLE).colLong("id").colLong("pid").colLong("y").pk("id").joinTo(SCHEMA, "p", "fk").on("pid", "id")
               .groupIndex("x_y", Index.JoinType.LEFT).on(TABLE, "y").and("p", "x");
        UserTable t1 = builder.unvalidatedAIS().getUserTable(TABLE_NAME);
        builder = AISBBasedBuilder.create(SCHEMA);
        builder.userTable("p").colLong("id").colLong("x").pk("id")
               .userTable(TABLE).colLong("id").colLong("pid").pk("id").joinTo(SCHEMA, "p", "fk").on("pid", "id");
        UserTable t2 = builder.unvalidatedAIS().getUserTable(TABLE_NAME);
        final String KEY1 = Index.PRIMARY_KEY_CONSTRAINT;
        final String KEY2 = "__akiban_fk";
        validate(
                t1, t2,
                asList(TableChange.createDrop("y")),
                NO_CHANGES,
                ChangeLevel.TABLE,
                asList(changeDesc(TABLE_NAME, TABLE_NAME, false, ParentChange.NONE, KEY1, KEY1, KEY2, KEY2)),
                false,
                false,
                "{test.p.x_y=[]}",
                false, 
                NO_IDENTITY_CHANGE
        );
    }

    @Test
    public void dropGFKFrommMiddleWithGroupIndexes() {
        TableName iName = new TableName(SCHEMA, "i");
        NewAISBuilder builder = AISBBasedBuilder.create(SCHEMA);
        builder.userTable("p").colLong("id").colLong("x").pk("id")
               .userTable(TABLE).colLong("id").colLong("pid").colLong("y").pk("id").joinTo(SCHEMA, "p", "fk1").on("pid", "id")
               .userTable(iName).colLong("id").colLong("tid").colLong("z").pk("id").joinTo(SCHEMA, TABLE, "fk2").on("tid", "id")
               .groupIndex("x_y", Index.JoinType.LEFT).on(TABLE, "y").and("p", "x")                  // spans 2
               .groupIndex("x_y_z", Index.JoinType.LEFT).on("i", "z").and(TABLE, "y").and("p", "x"); // spans 3
        UserTable t1 = builder.unvalidatedAIS().getUserTable(TABLE_NAME);
        builder = AISBBasedBuilder.create(SCHEMA);
        builder.userTable("p").colLong("id").colLong("x").pk("id")
                .userTable(TABLE).colLong("id").colLong("pid").colLong("y").pk("id").key("__akiban_fk1", "pid")
                .userTable(iName).colLong("id").colLong("tid").colLong("z").pk("id").joinTo(SCHEMA, TABLE, "fk2").on("tid", "id");
        UserTable t2 = builder.unvalidatedAIS().getUserTable(TABLE_NAME);
        validate(
                t1, t2,
                NO_CHANGES,
                NO_CHANGES,
                ChangeLevel.GROUP,
                asList(changeDesc(TABLE_NAME, TABLE_NAME, true, ParentChange.DROP), changeDesc(iName, iName, true, ParentChange.UPDATE)),
                true,
                false,
                "{test.p.x_y=[], test.p.x_y_z=[]}",
                false, 
                NO_IDENTITY_CHANGE
        );
    }
}