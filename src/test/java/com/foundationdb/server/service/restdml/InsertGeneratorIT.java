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
package com.foundationdb.server.service.restdml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.foundationdb.ais.model.TableName;
import com.foundationdb.qp.operator.Operator;
import com.foundationdb.server.explain.ExplainContext;
import com.foundationdb.server.explain.format.DefaultFormatter;
import com.foundationdb.server.service.session.Session;
import com.foundationdb.server.t3expressions.T3RegistryService;
import com.foundationdb.server.test.it.ITBase;

public class InsertGeneratorIT extends ITBase {

    public static final String SCHEMA = "test";
    private OperatorGenerator insertGenerator;
    
    @After
    public void commit() {
        this.txnService().commitTransaction(this.session());
    }

    @Before
    public void start() {
        Session session = this.session();
        this.txnService().beginTransaction(session);
    }
    
    @Test
    public void testCInsert() {
        
        createTable(SCHEMA, "c",
                "cid INT PRIMARY KEY NOT NULL",
                "name VARCHAR(32)");

        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(Field(0), Field(1))\n" +
                "        ValuesScan_Default([$1, $2])");
    }

    @Test
    public void testNoPKInsert() {
        
        createTable (SCHEMA, "c", 
                "cid INT NOT NULL",
                "name VARCHAR(32)");
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Insert_Returning(INTO c)\n" +
                "    Project_Default(Field(0), Field(1), NULL)\n" +
                "      ValuesScan_Default([$1, $2])");
    }

    @Test
    public void testIdentityDefault() {
        createTable (SCHEMA, "c",
                "cid int NOT NULL PRIMARY KEY generated by default as identity",
                "name varchar(32) NOT NULL");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        
        Pattern explain = Pattern.compile("\n  Project_Default\\(Field\\(0\\)\\)\n" +
                "    Insert_Returning\\(INTO c\\)\n" +
                "      Project_Default\\(ifnull\\(Field\\(0\\), NEXTVAL\\('test', '_sequence-3556597(\\$1)?'\\)\\), Field\\(1\\)\\)\n" +
                "        ValuesScan_Default\\(\\[\\$1, \\$2\\]\\)");
        assertTrue("Generated explain does not match test explain", explain.matcher(getExplain(insert, table.getSchemaName())).matches());
    }
    
    @Test
    public void testIdentityAlways() {
        createTable (SCHEMA, "c",
                "cid int NOT NULL PRIMARY KEY generated always as identity",
                "name varchar(32) NOT NULL");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        Pattern explain = Pattern.compile("\n  Project_Default\\(Field\\(0\\)\\)\n" +
                "    Insert_Returning\\(INTO c\\)\n" +
                "      Project_Default\\(NEXTVAL\\('test', '_sequence-3556597(\\$1)?'\\), Field\\(1\\)\\)\n" +
                "        ValuesScan_Default\\(\\[\\$1, \\$2\\]\\)");
        assertTrue("Generated explain does not match test explain", explain.matcher(getExplain(insert, table.getSchemaName())).matches());
    }
    
    @Test
    public void testDefaults() {
        createTable (SCHEMA, "c", 
                "cid int not null primary key default 0",
                "name varchar(32) not null default ''",
                "taxes double not null default '0.0'");
        
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(ifnull(Field(0), 0), ifnull(Field(1), ''), ifnull(Field(2), 0.000000e+00))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test 
    public void testPKNotFirst() {
        createTable (SCHEMA, "c",
                "name varchar(32) not null",
                "address varchar(64) not null",
                "cid int not null primary key");
        TableName table = new TableName (SCHEMA, "c");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(2))\n" +
                "    Insert_Returning(INTO c)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test
    public void testPKMultiColumn() {
        createTable(SCHEMA, "o",
                "cid int not null",
                "oid int not null",
                "items int not null",
                "primary key (cid, oid)");
        TableName table = new TableName (SCHEMA, "o");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0), Field(1))\n" +
                "    Insert_Returning(INTO o)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }
    
    @Test
    public void testJoinedTable() {
        createTable(SCHEMA, "c",
                "cid int not null",
                "fist_name varchar(32)",
                "PRIMARY KEY(cid)");
        createTable (SCHEMA, "a",
                "aid int not null",
                "cid int not null",
                "state char(2)",
                "PRIMARY KEY (aid)",
                "GROUPING FOREIGN KEY (cid) REFERENCES c(cid)");
        TableName table = new TableName (SCHEMA, "a");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO a)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");
    }

    @Test
    public void testOrdersTable() {
        createTable(SCHEMA, "customers",
                "cid int not null",
                "first_name varchar(32)",
                "primary key (cid)");
        createTable (SCHEMA, "orders",
                "oid int not null",
                "cid int not null",
                "odate datetime",
                "primary key (oid)",
                "grouping foreign key (cid) references customers(cid)");
        TableName table = new TableName (SCHEMA, "orders");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Project_Default(Field(0))\n" +
                "    Insert_Returning(INTO orders)\n" +
                "      Project_Default(Field(0), Field(1), Field(2))\n" +
                "        ValuesScan_Default([$1, $2, $3])");

    }
    
    @Test
    public void testAllTypes() {
        createTable (SCHEMA, "all_types",
                "year_field year",
                "bigint_field bigint",
                "bigint_unsigned_field bigint unsigned",
                "blob_field blob",
                "boolean_field boolean",
                "char_field char",
                "char_multi_field char(32)",
                "clob_field clob",
                "date_field date",
                "decimal_field decimal(10,0)",
                "double_field double",
                "float_field float",
                "integer_field integer",
                "numeric_field numeric(10,0)",
                "real_field real",
                "smallint_field smallint",
                "time_field time",
                "timestamp_field timestamp",
                "varchar_field varchar(32)",
                "datetime_field datetime");
        TableName table = new TableName (SCHEMA, "all_types");
        this.insertGenerator = new InsertGenerator (this.ais());
        insertGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator insert = insertGenerator.create(table);
        assertEquals(
                getExplain(insert, table.getSchemaName()),
                "\n  Insert_Returning(INTO all_types)\n" + 
                "    Project_Default(Field(0), Field(1), Field(2), Field(3), Field(4), Field(5), Field(6), Field(7), Field(8), Field(9), Field(10), Field(11), Field(12), Field(13), Field(14), Field(15), Field(16), Field(17), Field(18), Field(19), NULL)\n" +
                "      ValuesScan_Default([$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20])");                
    }
    
    
    protected String getExplain (Operator plannable, String defaultSchemaName) {
        StringBuilder str = new StringBuilder();
        ExplainContext context = new ExplainContext(); // Empty
        DefaultFormatter f = new DefaultFormatter(defaultSchemaName);
        for (String operator : f.format(plannable.getExplainer(context))) {
            str.append("\n  ");
            str.append(operator);
        }
        return str.toString();
    }
}
