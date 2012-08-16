/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hcatalog.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.MetaStoreUtils;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.InvalidPartitionException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PartitionEventType;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.UnknownDBException;
import org.apache.hadoop.hive.metastore.api.UnknownPartitionException;
import org.apache.hadoop.hive.metastore.api.UnknownTableException;
import org.apache.hcatalog.common.HCatException;
import org.apache.hcatalog.common.HCatUtil;
import org.apache.thrift.TException;

/**
 * The HCatClientHMSImpl is the Hive Metastore client based implementation of
 * HCatClient.
 */
public class HCatClientHMSImpl extends HCatClient {

    private HiveMetaStoreClient hmsClient;
    private Configuration  config;
    private HiveConf hiveConfig;

    @Override
    public List<String> listDatabaseNamesByPattern(String pattern)
            throws HCatException, ConnectionFailureException {
        List<String> dbNames = null;
        try {
            dbNames = hmsClient.getDatabases(pattern);
        } catch (MetaException exp) {
            throw new HCatException("MetaException while listing db names", exp);
        }
        return dbNames;
    }

    @Override
    public HCatDatabase getDatabase(String dbName) throws HCatException,
            ConnectionFailureException {
        HCatDatabase db = null;
        try {
            Database hiveDB = hmsClient.getDatabase(checkDB(dbName));
            if (hiveDB != null) {
                db = new HCatDatabase(hiveDB);
            }
        } catch (NoSuchObjectException exp) {
            throw new HCatException(
                    "NoSuchObjectException while fetching database", exp);
        } catch (MetaException exp) {
            throw new HCatException("MetaException while fetching database",
                    exp);
        } catch (TException exp) {
            throw new ConnectionFailureException(
                    "TException while fetching database", exp);
        }
        return db;
    }

    @Override
    public void createDatabase(HCatCreateDBDesc dbInfo) throws HCatException,
            ConnectionFailureException {
        try {
            hmsClient.createDatabase(dbInfo.toHiveDb());
        } catch (AlreadyExistsException exp) {
            if (!dbInfo.getIfNotExists()) {
                throw new HCatException(
                        "AlreadyExistsException while creating database", exp);
            }
        } catch (InvalidObjectException exp) {
            throw new HCatException(
                    "InvalidObjectException while creating database", exp);
        } catch (MetaException exp) {
            throw new HCatException("MetaException while creating database",
                    exp);
        } catch (TException exp) {
            throw new ConnectionFailureException(
                    "TException while creating database", exp);
        }
    }

    @Override
    public void dropDatabase(String dbName, boolean ifExists, DROP_DB_MODE mode)
            throws HCatException, ConnectionFailureException {
        boolean isCascade;
        if (mode.toString().equalsIgnoreCase("cascade")) {
            isCascade = true;
        } else {
            isCascade = false;
        }
        try {
            hmsClient.dropDatabase(checkDB(dbName), true, ifExists, isCascade);
        } catch (NoSuchObjectException e) {
            if (!ifExists) {
                throw new HCatException(
                        "NoSuchObjectException while dropping db.", e);
            }
        } catch (InvalidOperationException e) {
            throw new HCatException(
                    "InvalidOperationException while dropping db.", e);
        } catch (MetaException e) {
            throw new HCatException("MetaException while dropping db.", e);
        } catch (TException e) {
            throw new ConnectionFailureException("TException while dropping db.",
                    e);
        }
    }

    @Override
    public List<String> listTableNamesByPattern(String dbName,
            String tablePattern) throws HCatException, ConnectionFailureException {
        List<String> tableNames = null;
        try {
            tableNames = hmsClient.getTables(checkDB(dbName), tablePattern);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while fetching table names.", e);
        }
        return tableNames;
    }

    @Override
    public HCatTable getTable(String dbName, String tableName)
            throws HCatException, ConnectionFailureException {
        HCatTable table = null;
        try {
            Table hiveTable = hmsClient.getTable(checkDB(dbName), tableName);
            if (hiveTable != null) {
                table = new HCatTable(hiveTable);
            }
        } catch (MetaException e) {
            throw new HCatException("MetaException while fetching table.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while fetching table.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while fetching table.", e);
        }
        return table;
    }

    @Override
    public void createTable(HCatCreateTableDesc createTableDesc)
            throws HCatException, ConnectionFailureException {
        try {
            hmsClient.createTable(createTableDesc.toHiveTable(hiveConfig));
        } catch (AlreadyExistsException e) {
            if (createTableDesc.getIfNotExists() == false) {
                throw new HCatException(
                        "AlreadyExistsException while creating table.", e);
            }
        } catch (InvalidObjectException e) {
            throw new HCatException(
                    "InvalidObjectException while creating table.", e);
        } catch (MetaException e) {
            throw new HCatException("MetaException while creating table.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while creating table.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while creating table.", e);
        } catch (IOException e) {
            throw new HCatException("IOException while creating hive conf.", e);
        }

    }

    @Override
    public void createTableLike(String dbName, String existingTblName,
            String newTableName, boolean ifNotExists, boolean isExternal,
            String location) throws HCatException, ConnectionFailureException {

        Table hiveTable = getHiveTableLike(checkDB(dbName), existingTblName,
                newTableName, ifNotExists, location);
        if (hiveTable != null) {
            try {
                hmsClient.createTable(hiveTable);
            } catch (AlreadyExistsException e) {
                if (!ifNotExists) {
                    throw new HCatException(
                            "A table already exists with the name "
                                    + newTableName, e);
                }
            } catch (InvalidObjectException e) {
                throw new HCatException(
                        "InvalidObjectException in create table like command.",
                        e);
            } catch (MetaException e) {
                throw new HCatException(
                        "MetaException in create table like command.", e);
            } catch (NoSuchObjectException e) {
                throw new HCatException(
                        "NoSuchObjectException in create table like command.",
                        e);
            } catch (TException e) {
                throw new ConnectionFailureException(
                        "TException in create table like command.", e);
            }
        }
    }

    @Override
    public void dropTable(String dbName, String tableName, boolean ifExists)
            throws HCatException, ConnectionFailureException {
        try {
            hmsClient.dropTable(checkDB(dbName), tableName);
        } catch (NoSuchObjectException e) {
            if (!ifExists) {
                throw new HCatException(
                        "NoSuchObjectException while dropping table.", e);
            }
        } catch (MetaException e) {
            throw new HCatException("MetaException while dropping table.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while dropping table.", e);
        }
    }

    @Override
    public void renameTable(String dbName, String oldName, String newName)
            throws HCatException, ConnectionFailureException {
        Table tbl;
        try {
            Table oldtbl = hmsClient.getTable(checkDB(dbName), oldName);
            if (oldtbl != null) {
                // TODO : Should be moved out.
                if (oldtbl
                        .getParameters()
                        .get(org.apache.hadoop.hive.metastore.api.Constants.META_TABLE_STORAGE) != null) {
                    throw new HCatException(
                            "Cannot use rename command on a non-native table");
                }
                tbl = new Table(oldtbl);
                tbl.setTableName(newName);
                hmsClient.alter_table(checkDB(dbName), oldName, tbl);
            }
        } catch (MetaException e) {
            throw new HCatException("MetaException while renaming table", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while renaming table", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while renaming table", e);
        } catch (InvalidOperationException e) {
            throw new HCatException(
                    "InvalidOperationException while renaming table", e);
        }
    }

    @Override
    public List<HCatPartition> getPartitions(String dbName, String tblName)
            throws HCatException, ConnectionFailureException {
        List<HCatPartition> hcatPtns = new ArrayList<HCatPartition>();
        try {
            List<Partition> hivePtns = hmsClient.listPartitions(
                    checkDB(dbName), tblName, (short) -1);
            for (Partition ptn : hivePtns) {
                hcatPtns.add(new HCatPartition(ptn));
            }
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while retrieving partition.", e);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while retrieving partition.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while retrieving partition.", e);
        }
        return hcatPtns;
    }

    @Override
    public HCatPartition getPartition(String dbName, String tableName,
            Map<String, String> partitionSpec) throws HCatException,
            ConnectionFailureException {
        HCatPartition partition = null;
        try {
            ArrayList<String> ptnValues = new ArrayList<String>();
            ptnValues.addAll(partitionSpec.values());
            Partition hivePartition = hmsClient.getPartition(checkDB(dbName),
                    tableName, ptnValues);
            if (hivePartition != null) {
                partition = new HCatPartition(hivePartition);
            }
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while retrieving partition.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while retrieving partition.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while retrieving partition.", e);
        }
        return partition;
    }

    @Override
    public void addPartition(HCatAddPartitionDesc partInfo)
            throws HCatException, ConnectionFailureException {
        Table tbl = null;
        try {
            tbl = hmsClient.getTable(partInfo.getDatabaseName(),
                    partInfo.getTableName());
            // TODO: Should be moved out.
            if (tbl.getPartitionKeysSize() == 0) {
                throw new HCatException("The table " + partInfo.getTableName()
                        + " is not partitioned.");
            }

            hmsClient.add_partition(partInfo.toHivePartition(tbl));
        } catch (InvalidObjectException e) {
            throw new HCatException(
                    "InvalidObjectException while adding partition.", e);
        } catch (AlreadyExistsException e) {
            throw new HCatException(
                    "AlreadyExistsException while adding partition.", e);
        } catch (MetaException e) {
            throw new HCatException("MetaException while adding partition.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while adding partition.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException("The table " + partInfo.getTableName()
                    + " is could not be found.", e);
        }
    }

    @Override
    public void dropPartition(String dbName, String tableName,
            Map<String, String> partitionSpec, boolean ifExists)
            throws HCatException, ConnectionFailureException {
        try {
            List<String> ptnValues = new ArrayList<String>();
            ptnValues.addAll(partitionSpec.values());
            hmsClient.dropPartition(checkDB(dbName), tableName, ptnValues,
                    ifExists);
        } catch (NoSuchObjectException e) {
            if (!ifExists) {
                throw new HCatException(
                        "NoSuchObjectException while dropping partition.", e);
            }
        } catch (MetaException e) {
            throw new HCatException("MetaException while dropping partition.",
                    e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while dropping partition.", e);
        }
    }

    @Override
    public List<HCatPartition> listPartitionsByFilter(String dbName,
            String tblName, String filter) throws HCatException,
            ConnectionFailureException {
        List<HCatPartition> hcatPtns = new ArrayList<HCatPartition>();
        try {
            List<Partition> hivePtns = hmsClient.listPartitionsByFilter(
                    checkDB(dbName), tblName, filter, (short) -1);
            for (Partition ptn : hivePtns) {
                hcatPtns.add(new HCatPartition(ptn));
            }
        } catch (MetaException e) {
            throw new HCatException("MetaException while fetching partitions.",
                    e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while fetching partitions.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while fetching partitions.", e);
        }
        return hcatPtns;
    }

    @Override
    public void markPartitionForEvent(String dbName, String tblName,
            Map<String, String> partKVs, PartitionEventType eventType)
            throws HCatException, ConnectionFailureException {
        try {
            hmsClient.markPartitionForEvent(checkDB(dbName), tblName, partKVs,
                    eventType);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while marking partition for event.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while marking partition for event.",
                    e);
        } catch (UnknownTableException e) {
            throw new HCatException(
                    "UnknownTableException while marking partition for event.",
                    e);
        } catch (UnknownDBException e) {
            throw new HCatException(
                    "UnknownDBException while marking partition for event.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while marking partition for event.", e);
        } catch (InvalidPartitionException e) {
            throw new HCatException(
                    "InvalidPartitionException while marking partition for event.",
                    e);
        } catch (UnknownPartitionException e) {
            throw new HCatException(
                    "UnknownPartitionException while marking partition for event.",
                    e);
        }
    }

    @Override
    public boolean isPartitionMarkedForEvent(String dbName, String tblName,
            Map<String, String> partKVs, PartitionEventType eventType)
            throws HCatException, ConnectionFailureException {
        boolean isMarked = false;
        try {
            isMarked = hmsClient.isPartitionMarkedForEvent(checkDB(dbName),
                    tblName, partKVs, eventType);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while checking partition for event.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException(
                    "NoSuchObjectException while checking partition for event.",
                    e);
        } catch (UnknownTableException e) {
            throw new HCatException(
                    "UnknownTableException while checking partition for event.",
                    e);
        } catch (UnknownDBException e) {
            throw new HCatException(
                    "UnknownDBException while checking partition for event.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while checking partition for event.", e);
        } catch (InvalidPartitionException e) {
            throw new HCatException(
                    "InvalidPartitionException while checking partition for event.",
                    e);
        } catch (UnknownPartitionException e) {
            throw new HCatException(
                    "UnknownPartitionException while checking partition for event.",
                    e);
        }
        return isMarked;
    }

    @Override
    public String getDelegationToken(String owner,
            String renewerKerberosPrincipalName) throws HCatException,
            ConnectionFailureException {
        String token = null;
        try {
            token = hmsClient.getDelegationToken(owner,
                    renewerKerberosPrincipalName);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while getting delegation token.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while getting delegation token.", e);
        }

        return token;
    }

    @Override
    public long renewDelegationToken(String tokenStrForm) throws HCatException,
            ConnectionFailureException {
        long time = 0;
        try {
            time = hmsClient.renewDelegationToken(tokenStrForm);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while renewing delegation token.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while renewing delegation token.", e);
        }

        return time;
    }

    @Override
    public void cancelDelegationToken(String tokenStrForm)
            throws HCatException, ConnectionFailureException {
        try {
            hmsClient.cancelDelegationToken(tokenStrForm);
        } catch (MetaException e) {
            throw new HCatException(
                    "MetaException while canceling delegation token.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while canceling delegation token.", e);
        }
    }

    /*
     * @param conf /* @throws HCatException,ConnectionFailureException
     *
     * @see
     * org.apache.hcatalog.api.HCatClient#initialize(org.apache.hadoop.conf.
     * Configuration)
     */
    @Override
    void initialize(Configuration conf) throws HCatException,
            ConnectionFailureException {
        this.config = conf;
        try {
            hiveConfig = HCatUtil.getHiveConf(config);
            hmsClient = HCatUtil.getHiveClient(hiveConfig);
        } catch (MetaException exp) {
            throw new HCatException("MetaException while creating HMS client",
                    exp);
        } catch (IOException exp) {
            throw new HCatException("IOException while creating HMS client",
                    exp);
        }

    }

    private Table getHiveTableLike(String dbName, String existingTblName,
            String newTableName, boolean isExternal, String location)
            throws HCatException, ConnectionFailureException {
        Table oldtbl = null;
        Table newTable = null;
        try {
            oldtbl = hmsClient.getTable(checkDB(dbName), existingTblName);
        } catch (MetaException e1) {
            throw new HCatException(
                    "MetaException while retrieving existing table.", e1);
        } catch (TException e1) {
            throw new ConnectionFailureException(
                    "TException while retrieving existing table.", e1);
        } catch (NoSuchObjectException e1) {
            throw new HCatException(
                    "NoSuchObjectException while retrieving existing table.",
                    e1);
        }
        if (oldtbl != null) {
            newTable = new Table();
            newTable.setTableName(newTableName);
            newTable.setDbName(dbName);
            StorageDescriptor sd = new StorageDescriptor(oldtbl.getSd());
            newTable.setSd(sd);
            newTable.setParameters(oldtbl.getParameters());
            if (location == null) {
                newTable.getSd().setLocation(oldtbl.getSd().getLocation());
            } else {
                newTable.getSd().setLocation(location);
            }
            if (isExternal) {
                newTable.putToParameters("EXTERNAL", "TRUE");
                newTable.setTableType(TableType.EXTERNAL_TABLE.toString());
            } else {
                newTable.getParameters().remove("EXTERNAL");
            }
            // set create time
            newTable.setCreateTime((int) (System.currentTimeMillis() / 1000));
            newTable.setLastAccessTimeIsSet(false);
        }
        return newTable;
    }

    /*
     * @throws HCatException
     *
     * @see org.apache.hcatalog.api.HCatClient#closeClient()
     */
    @Override
    public void close() throws HCatException {
        hmsClient.close();
    }

    private String checkDB(String name) {
        if (StringUtils.isEmpty(name)) {
            return MetaStoreUtils.DEFAULT_DATABASE_NAME;
        } else {
            return name;
        }
    }

    /*
     * @param partInfoList
     *  @return The size of the list of partitions.
     * @throws HCatException,ConnectionFailureException
     * @see org.apache.hcatalog.api.HCatClient#addPartitions(java.util.List)
     */
    @Override
    public int addPartitions(List<HCatAddPartitionDesc> partInfoList)
            throws HCatException, ConnectionFailureException {
        int numPartitions = -1;
        if ((partInfoList == null) || (partInfoList.size() == 0)) {
            throw new HCatException("The partition list is null or empty.");
        }

        Table tbl = null;
        try {
            tbl = hmsClient.getTable(partInfoList.get(0).getDatabaseName(),
                    partInfoList.get(0).getTableName());
            ArrayList<Partition> ptnList = new ArrayList<Partition>();
            for (HCatAddPartitionDesc desc : partInfoList) {
                ptnList.add(desc.toHivePartition(tbl));
            }
            numPartitions = hmsClient.add_partitions(ptnList);
        } catch (InvalidObjectException e) {
            throw new HCatException(
                    "InvalidObjectException while adding partition.", e);
        } catch (AlreadyExistsException e) {
            throw new HCatException(
                    "AlreadyExistsException while adding partition.", e);
        } catch (MetaException e) {
            throw new HCatException("MetaException while adding partition.", e);
        } catch (TException e) {
            throw new ConnectionFailureException(
                    "TException while adding partition.", e);
        } catch (NoSuchObjectException e) {
            throw new HCatException("The table "
                    + partInfoList.get(0).getTableName()
                    + " is could not be found.", e);
        }
        return numPartitions;
    }

}
