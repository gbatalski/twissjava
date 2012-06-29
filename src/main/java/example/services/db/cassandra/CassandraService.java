package example.services.db.cassandra;

import static com.google.common.collect.ImmutableList.of;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import me.prettyprint.cassandra.model.BasicKeyspaceDefinition;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.beans.HSuperColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.CounterQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.RangeSubSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;
import me.prettyprint.hector.api.query.SubColumnQuery;
import me.prettyprint.hector.api.query.SubCountQuery;
import me.prettyprint.hector.api.query.SubSliceQuery;
import me.prettyprint.hector.api.query.SuperColumnQuery;

import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import example.guice.annotation.Host;
import example.guice.annotation.Port;
import example.guice.annotation.ReadConsistencyLevel;
import example.guice.annotation.WriteConsistencyLevel;

/**
 * Nebula Cloud Platform Copyright 2010 Innovation Works Limited, All Rights
 * Reserved. Author: dikang
 */
@Singleton
public class CassandraService {

	private static final Logger LOG = Logger.getLogger(CassandraService.class);

	private static final int CASSANDRA_THRIFT_SOCKET_TIMEOUT = 3000;

	private static final int MAXWAITTIMEWHENEXHAUSTED = 4000;

	private static final int MAX_ROW_COUNT = 1000;

	private static final int MAX_COLUMN_COUNT = 1000;

	public static final StringSerializer SE = StringSerializer.get();

	private final Cluster _cluster;

	private Keyspace _keyspace = null;

	private final String keyspaceName;

	private final ConfigurableConsistencyLevel consistencyLevelPolicy = new ConfigurableConsistencyLevel();

	private final SchemaUtils schemaUtils;


	@Inject
	public CassandraService(@Host final String host, @Port final Integer port,
			@example.guice.annotation.Cluster final String clusterName,
			@example.guice.annotation.Keyspace final String keyspace,
			SchemaUtils schemaUtils,
			@ReadConsistencyLevel final HConsistencyLevel readConsistencyLevel,
			@WriteConsistencyLevel final HConsistencyLevel writeConsistencyLevel) {

		CassandraHostConfigurator cassandraHostConfigurator = new CassandraHostConfigurator(host.trim()
				+ ":" + port);
		cassandraHostConfigurator.setMaxActive(100);
		cassandraHostConfigurator.setCassandraThriftSocketTimeout(CASSANDRA_THRIFT_SOCKET_TIMEOUT);
		cassandraHostConfigurator.setMaxWaitTimeWhenExhausted(MAXWAITTIMEWHENEXHAUSTED);
		cassandraHostConfigurator.setRetryDownedHosts(true);

		_cluster = HFactory.getOrCreateCluster(	clusterName,
												cassandraHostConfigurator);
		// createKeyspaceIfAbsent(keyspace);
		this.keyspaceName = keyspace;
		this.schemaUtils = schemaUtils;
		this.consistencyLevelPolicy.setDefaultWriteConsistencyLevel(writeConsistencyLevel);
		this.consistencyLevelPolicy.setDefaultReadConsistencyLevel(readConsistencyLevel);
	}

	/**
	 * 
	 * @param keyspace
	 */
	@SuppressWarnings("unused")
	private void createKeyspaceIfAbsent(final String keyspace) {
		try {
			KeyspaceDefinition kdef = HFactory.createKeyspaceDefinition(keyspace,
																		"org.apache.cassandra.locator.SimpleStrategy",
																		1,
																		null);

			_cluster.addKeyspace(kdef);
		} catch (Exception ex) {
			// ignore
			LOG.warn(ex.getMessage(), ex);
		}
	}

	/**
	 * create column family if absent, not super man.
	 * 
	 * @param columnFamily
	 */
	public void createColumnFamilyIfAbsent(final String columnFamily) {

		try {
			ColumnFamilyDefinition def = HFactory.createColumnFamilyDefinition(	getKeyspace().getKeyspaceName(),
																				columnFamily);
			_cluster.addColumnFamily(def);
		} catch (Exception ex) {
			LOG.warn(ex.getMessage(), ex);
		}
	}

	/**
	 * create super column family if absent
	 * 
	 * @param columnFamily
	 */
	public void createSuperColumnFamilyIfAbsent(final String columnFamily) {
		try {
			ThriftCfDef def = (ThriftCfDef) HFactory.createColumnFamilyDefinition(	getKeyspace().getKeyspaceName(),
																					columnFamily);
			def.setColumnType(ColumnType.SUPER);

			_cluster.addColumnFamily(def);
		} catch (Exception ex) {
			LOG.warn(ex.getMessage(), ex);
		}
	}

	/**
	 * read the value from the cassandra
	 * 
	 * @param key
	 * @param columnName
	 * @param columnFamily
	 * @return
	 */
	public String readColumn(final String key, final String columnName,
			final String columnFamily) {
		ColumnQuery<String, String, String> columnQuery = HFactory.createStringColumnQuery(getKeyspace());
		columnQuery.setColumnFamily(columnFamily)
					.setKey(key)
					.setName(columnName);
		QueryResult<HColumn<String, String>> result = columnQuery.execute();
		HColumn<String, String> column = result.get();

		if (null == column)
			return "";
		return column.getValue();
	}

	/**
	 * read a column from the super column
	 * 
	 * @param key
	 * @param columnName
	 * @param superColumn
	 * @param columnFamily
	 * @return
	 */
	public String readSubColumn(final String key, final String columnName,
			final String superColumn, final String columnFamily) {

		SubColumnQuery<String, String, String, String> subColumnQuery = HFactory.createSubColumnQuery(	getKeyspace(),
																										SE,
																										SE,
																										SE,
																										SE);
		subColumnQuery.setKey(key)
						.setColumn(columnName)
						.setSuperColumn(superColumn)
						.setColumnFamily(columnFamily);

		QueryResult<HColumn<String, String>> result = subColumnQuery.execute();
		HColumn<String, String> column = result.get();

		if (null == column)
			return "";
		return column.getValue();
	}

	/**
	 * read multi columns
	 * 
	 * @param key
	 * @param columns
	 * @param columnFamily
	 * @return
	 */
	public Map<String, String> readColumns(final String key,
			final String[] columns, final String columnFamily) {
		Map<String, String> results = new TreeMap<String, String>();

		SliceQuery<String, String, String> sliceQuery = HFactory.createSliceQuery(	getKeyspace(),
																					SE,
																					SE,
																					SE);
		sliceQuery.setColumnFamily(columnFamily)
					.setKey(key)
					.setColumnNames(columns);

		ColumnSlice<String, String> columnSlice = sliceQuery.execute()
															.get();

		for (String column : columns) {
			HColumn<String, String> hColumn = columnSlice.getColumnByName(column);
			if (null == hColumn) {
				results.put(column, "");
			} else {
				results.put(column, hColumn.getValue());
			}
		}

		return results;
	}

	/**
	 * list columns in a column family
	 * 
	 * return MAX_COLUMN_COUNT columns at most
	 * 
	 * @param key
	 * @param columnFamily
	 * @return
	 */
	public Map<String, String> listColumns(final String key,
			final String columnFamily, final boolean reversed) {

		return listColumns(key, columnFamily, null, MAX_COLUMN_COUNT, reversed);
	}

	public Map<String, String> listColumns(final String key,
			final String columnFamily, final String startColumn,
			final boolean reversed) {

		return listColumns(	key,
							columnFamily,
							startColumn,
							MAX_COLUMN_COUNT,
							reversed);
	}

	public Map<String, String> listColumns(final String key,
			final String columnFamily, final String startColumn,
			final int count, final boolean reversed) {
		Map<String, String> results = new TreeMap<String, String>();

		RangeSlicesQuery<String, String, String> sliceQuery = HFactory.createRangeSlicesQuery(	getKeyspace(),
																								SE,
																								SE,
																								SE);
		sliceQuery.setColumnFamily(columnFamily)
					.setRange(startColumn, null, reversed, count)
					.setKeys(key, key)
					.setRowCount(1);

		OrderedRows<String, String, String> rows = sliceQuery.execute()
																.get();

		if (null == rows || 0 == rows.getCount()) {
			return results;
		}

		ColumnSlice<String, String> columnSlice = rows.getList()
														.get(0)
														.getColumnSlice();

		if (null == columnSlice) {
			return results;
		}

		for (HColumn<String, String> hColumn : columnSlice.getColumns()) {
			results.put(hColumn.getName(), hColumn.getValue());
		}

		return results;
	}

	/**
	 * read the columns in a super column.
	 * 
	 * @param key
	 * @param columns
	 * @param superColumn
	 * @param columnFamily
	 * @return
	 */
	public Map<String, String> readSubColumns(final String key,
			final String[] columns, final String superColumn,
			final String columnFamily) {
		Map<String, String> results = new TreeMap<String, String>();

		SubSliceQuery<String, String, String, String> subSliceQuery = HFactory.createSubSliceQuery(	getKeyspace(),
																									SE,
																									SE,
																									SE,
																									SE);
		subSliceQuery.setColumnFamily(columnFamily)
						.setSuperColumn(superColumn)
						.setKey(key)
						.setColumnNames(columns);

		ColumnSlice<String, String> columnSlice = subSliceQuery.execute()
																.get();

		for (String column : columns) {
			HColumn<String, String> hColumn = columnSlice.getColumnByName(column);
			if (null == hColumn) {
				results.put(column, "");
			} else {
				results.put(column, hColumn.getValue());
			}
		}

		return results;
	}

	/**
	 * read a super column
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @return
	 */
	public Map<String, String> readSuperColumns(final String key,
			final String superColumn, final String columnFamily) {
		Map<String, String> results = new TreeMap<String, String>();

		SuperColumnQuery<String, String, String, String> superColumnQuery = HFactory.createSuperColumnQuery(getKeyspace(),
																											SE,
																											SE,
																											SE,
																											SE);
		superColumnQuery.setKey(key)
						.setSuperName(superColumn)
						.setColumnFamily(columnFamily);

		HSuperColumn<String, String, String> sColumn = superColumnQuery.execute()
																		.get();

		if (null == sColumn) {
			return results;
		}

		for (HColumn<String, String> column : sColumn.getColumns()) {
			results.put(column.getName(), column.getValue());
		}
		return results;
	}

	/**
	 * update the value in the column
	 * 
	 * @param key
	 * @param value
	 * @param columnName
	 * @param columnFamily
	 */
	public void updateColumn(final String key, final String value,
			final String columnName, final String columnFamily) {
		Mutator<String> mutator = HFactory.createMutator(getKeyspace(), SE);

		// insert (row, columnfamily, column(key, value));
		mutator.insert(	key,
						columnFamily,
						HFactory.createStringColumn(columnName, value));
	}

	/**
	 * update the column in a super column
	 * 
	 * @param key
	 * @param value
	 * @param columnName
	 * @param superColumn
	 * @param columnFamily
	 */
	public void updateSubColumn(final String key, final String value,
			final String columnName, final String superColumn,
			final String columnFamily) {
		Mutator<String> mutator = HFactory.createMutator(getKeyspace(), SE);

		// insert (row, column family, column(key, value));
		mutator.insert(	key,
						columnFamily,
						HFactory.createSuperColumn(	superColumn,
													of(HFactory.createStringColumn(	columnName,
																					value)),
													SE,
													SE,
													SE));
	}

	/**
	 * 
	 * update the columns in a super column
	 * 
	 * @param key
	 * @param columns
	 * @param superColumn
	 * @param columnFamily
	 */
	public void updateSubColumns(final String key,
			final Map<String, String> columns, final String superColumn,
			final String columnFamily) {
		Mutator<String> mutator = HFactory.createMutator(getKeyspace(), SE);

		List<HColumn<String, String>> columnList = new ArrayList<HColumn<String, String>>();
		for (String columnName : columns.keySet()) {
			columnList.add(HFactory.createStringColumn(	columnName,
														columns.get(columnName)));
		}

		mutator.insert(	key,
						columnFamily,
						HFactory.createSuperColumn(	superColumn,
													columnList,
													SE,
													SE,
													SE));
	}

	/**
	 * delete the column
	 * 
	 * @param key
	 * @param columnName
	 * @param columnFamily
	 */
	public void deleteColumn(final String key, final String columnName,
			final String columnFamily) {
		Mutator<String> mutator = HFactory.createMutator(getKeyspace(), SE);

		mutator.delete(key, columnFamily, columnName, SE);
	}

	/**
	 * delete the column in a super column
	 * 
	 * @param key
	 * @param columnName
	 * @param superColumn
	 * @param columnFamily
	 */
	public void deleteSubColumn(final String key, final String columnName,
			final String superColumn, final String columnFamily) {
		Mutator<String> mutator = HFactory.createMutator(getKeyspace(), SE);

		mutator.subDelete(key, columnFamily, superColumn, columnName, SE, SE);
	}

	/**
	 * return all the keys
	 * 
	 * @param columnFamily
	 * @return
	 */
	public List<String> listKeys(final String columnFamily) {
		RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory.createRangeSlicesQuery(getKeyspace(),
																									SE,
																									SE,
																									SE);

		rangeSlicesQuery.setColumnFamily(columnFamily)
						.setReturnKeysOnly()
						.setRowCount(MAX_ROW_COUNT);
		OrderedRows<String, String, String> rows = rangeSlicesQuery.execute()
																	.get();

		List<String> result = new ArrayList<String>();

		// return empty list
		if (null == rows) {
			return result;
		}

		for (Row<String, String, String> row : rows.getList()) {
			result.add(row.getKey());
		}

		if (result.size() < MAX_ROW_COUNT) {
			return result;
		}

		String startKey = rows.peekLast()
								.getKey();
		List<String> next = listKeys(columnFamily, startKey);

		while (next.size() != 0) {

			result.addAll(next);
			startKey = next.get(next.size() - 1);
			next = listKeys(columnFamily, startKey);
		}
		return result;
	}

	/**
	 * return the keys after the startKey
	 * 
	 * @param columnFamily
	 * @param startKey
	 * @return
	 */
	public List<String> listKeys(final String columnFamily,
			final String startKey) {
		RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory.createRangeSlicesQuery(getKeyspace(),
																									SE,
																									SE,
																									SE);

		rangeSlicesQuery.setColumnFamily(columnFamily)
						.setReturnKeysOnly()
						.setRowCount(MAX_ROW_COUNT + 1)
						.setKeys(startKey, null);
		OrderedRows<String, String, String> rows = rangeSlicesQuery.execute()
																	.get();

		List<String> result = new ArrayList<String>();

		// return empty list
		if (null == rows) {
			return result;
		}

		for (Row<String, String, String> row : rows.getList()) {
			// ignore the startKey
			if (row.getKey()
					.equals(startKey)) {
				continue;
			}
			result.add(row.getKey());
		}
		return result;
	}

	/**
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @return
	 */
	public Map<String, String> listSubColumns(final String key,
			final String superColumn, final String columnFamily) {
		return listSubColumns(key, superColumn, columnFamily, true);
	}

	/**
	 * list the columns in a super column.
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @param reversed
	 * @return
	 */
	public Map<String, String> listSubColumns(final String key,
			final String superColumn, final String columnFamily,
			final boolean reversed) {
		Map<String, String> result = new TreeMap<String, String>();

		RangeSubSlicesQuery<String, String, String, String> rangeSubSlicesQuery = HFactory.createRangeSubSlicesQuery(	getKeyspace(),
																														SE,
																														SE,
																														SE,
																														SE);

		rangeSubSlicesQuery.setColumnFamily(columnFamily)
							.setKeys(key, key)
							.setRange(null, null, reversed, MAX_COLUMN_COUNT)
							.setSuperColumn(superColumn);

		OrderedRows<String, String, String> rows = rangeSubSlicesQuery.execute()
																		.get();

		if (null == rows || 0 == rows.getCount()) {
			return result;
		}

		// get the row
		Row<String, String, String> row = rows.getList()
												.get(0);
		ColumnSlice<String, String> columns = row.getColumnSlice();

		if (null == columns) {
			return result;
		}

		for (HColumn<String, String> column : columns.getColumns()) {
			result.put(column.getName(), column.getValue());
		}

		return result;
	}

	/**
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @param startColumn
	 * @return
	 */
	public Map<String, String> listSubColumns(final String key,
			final String superColumn, final String columnFamily,
			final String startColumn) {
		return listSubColumns(key, superColumn, columnFamily, startColumn, true);

	}

	/**
	 * 
	 * list columns start from a startcolumn.
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @param startColumn
	 * @param reversed
	 * @return
	 */
	public Map<String, String> listSubColumns(final String key,
			final String superColumn, final String columnFamily,
			final String startColumn, final boolean reversed) {

		/*
		 * if (StringHelper.IsNullOrEmpty(startColumn)) { return
		 * listSubColumns(key, superColumn, columnFamily); }
		 */

		Map<String, String> result = new TreeMap<String, String>();

		RangeSubSlicesQuery<String, String, String, String> rangeSubSlicesQuery = HFactory.createRangeSubSlicesQuery(	getKeyspace(),
																														SE,
																														SE,
																														SE,
																														SE);

		rangeSubSlicesQuery.setColumnFamily(columnFamily)
							.setKeys(key, key)
							.setRange(	startColumn,
										null,
										reversed,
										MAX_COLUMN_COUNT + 1)
							.setSuperColumn(superColumn);

		OrderedRows<String, String, String> rows = rangeSubSlicesQuery.execute()
																		.get();

		if (null == rows || 0 == rows.getCount()) {
			return result;
		}

		// get the row
		Row<String, String, String> row = rows.getList()
												.get(0);
		ColumnSlice<String, String> columns = row.getColumnSlice();

		if (null == columns) {
			return result;
		}

		for (HColumn<String, String> column : columns.getColumns()) {
			if (startColumn.equals(column.getName())) {
				continue;
			}

			result.put(column.getName(), column.getValue());
		}

		return result;
	}

	/**
	 * count the columns for a key in a super column.
	 * 
	 * @param key
	 * @param superColumn
	 * @param columnFamily
	 * @return
	 */
	public int countSubColumns(final String key, final String superColumn,
			final String columnFamily) {

		SubCountQuery<String, String, String> subCountQuery = HFactory.createSubCountQuery(	getKeyspace(),
																							SE,
																							SE,
																							SE);
		subCountQuery.setColumnFamily(columnFamily)
						.setKey(key)
						.setSuperColumn(superColumn)
						.setRange(null, null, Integer.MAX_VALUE);

		int count = subCountQuery.execute()
									.get();
		return count;
	}

	public long countColumns(final String key, final String columnName,
			final String columnFamily) {

		CounterQuery<String, String> countQuery = HFactory.createCounterColumnQuery(getKeyspace(),
																					SE,
																					SE);
		countQuery.setColumnFamily(columnFamily)
					.setKey(key)
					.setName(columnName);
		HCounterColumn<String> cc = countQuery.execute()
												.get();
		Long value = cc != null ? cc.getValue() : null;
		return value != null ? value.longValue() : 0L;
	}

	public Mutator<String> getMutator() {
		return HFactory.createMutator(getKeyspace(), SE);
	}

	public Keyspace getKeyspace() {
		if (_keyspace == null)
			_keyspace = HFactory.createKeyspace(keyspaceName,
												_cluster,
												consistencyLevelPolicy);
		return _keyspace;
	}

	public ConsistencyLevelPolicy getConsistencyLevelPolicy() {
		return consistencyLevelPolicy;
	}

	public void updateSchema(boolean drop, int replicationFactor,
			HConsistencyLevel readConsistencyLevel,
			HConsistencyLevel writeConsistencyLevel) {
		if (drop) {
			_cluster.dropKeyspace(keyspaceName, true);
			schemaUtils.deploySchema(replicationFactor, false);
		}
		else {
			KeyspaceDefinition kesd = _cluster.describeKeyspace(keyspaceName);
			BasicKeyspaceDefinition updatedKesd = new BasicKeyspaceDefinition();
			updatedKesd.setReplicationFactor(replicationFactor);
			updatedKesd.setDurableWrites(kesd.isDurableWrites());
			updatedKesd.setName(kesd.getName());
			updatedKesd.setStrategyClass(kesd.getStrategyClass());
			updatedKesd.getStrategyOptions()
						.putAll(kesd.getStrategyOptions());

			_cluster.updateKeyspace(updatedKesd, true);
		}
		this.consistencyLevelPolicy.setDefaultReadConsistencyLevel(readConsistencyLevel);
		this.consistencyLevelPolicy.setDefaultWriteConsistencyLevel(writeConsistencyLevel);

	}

	public int getReplicationFactor() {
		return _cluster.describeKeyspace(keyspaceName)
						.getReplicationFactor();
	}

}
