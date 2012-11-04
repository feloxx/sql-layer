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

package com.akiban.ais.model;

import java.util.List;
import java.util.Set;

public class SynchronizedNameGenerator implements NameGenerator {
    private final Object LOCK = new Object();
    private final NameGenerator realNamer;


    public static SynchronizedNameGenerator wrap(NameGenerator wrapped) {
        return new SynchronizedNameGenerator(wrapped);
    }

    private SynchronizedNameGenerator(NameGenerator realNamer) {
        this.realNamer = realNamer;
    }


    @Override
    public TableName generateIdentitySequenceName(TableName table) {
        synchronized(LOCK) {
            return realNamer.generateIdentitySequenceName(table);
        }
    }

    @Override
    public String generateJoinName(TableName parentTable, TableName childTable, List<JoinColumn> joinIndex) {
        synchronized(LOCK) {
            return realNamer.generateJoinName(parentTable, childTable, joinIndex);
        }
    }

    @Override
    public String generateJoinName(TableName parentTable, TableName childTable, List<String> pkColNames, List<String> fkColNames) {
        synchronized(LOCK) {
            return realNamer.generateJoinName(parentTable, childTable, pkColNames, fkColNames);
        }
    }

    @Override
    public int generateTableID(TableName name) {
        synchronized(LOCK) {
            return realNamer.generateTableID(name);
        }
    }

    @Override
    public int generateIndexID(int rootTableID) {
        synchronized(LOCK) {
            return realNamer.generateIndexID(rootTableID);
        }
    }

    @Override
    public String generateGroupTreeName(String schemaName, String groupName) {
        synchronized(LOCK) {
            return realNamer.generateGroupTreeName(schemaName, groupName);
        }
    }

    @Override
    public String generateIndexTreeName(Index index) {
        synchronized(LOCK) {
            return realNamer.generateIndexTreeName(index);
        }
    }

    @Override
    public String generateSequenceTreeName(Sequence sequence) {
        synchronized(LOCK) {
            return realNamer.generateSequenceTreeName(sequence);
        }
    }

    @Override
    public void removeTableID(int tableID) {
        synchronized(LOCK) {
            realNamer.removeTableID(tableID);
        }
    }

    @Override
    public void removeTreeName(String treeName) {
        synchronized(LOCK) {
            realNamer.removeTreeName(treeName);
        }
    }

    @Override
    public Set<String> getTreeNames() {
        synchronized(LOCK) {
            return realNamer.getTreeNames();
        }
    }
}
