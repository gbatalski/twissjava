/**
 * 
 */
package example.guice.module;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import me.prettyprint.hector.api.HConsistencyLevel;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.inject.Binder;
import com.google.inject.Module;

import example.guice.annotation.Cluster;
import example.guice.annotation.Host;
import example.guice.annotation.Keyspace;
import example.guice.annotation.Port;
import example.guice.annotation.ReadConsistencyLevel;
import example.guice.annotation.ReplicationFactor;
import example.guice.annotation.SchemaFilename;
import example.guice.annotation.WriteConsistencyLevel;
import example.services.db.cassandra.CassandraService;
import example.services.db.cassandra.SchemaUtils;

;
/**
 * @author gena
 * 
 */
public class CassandraModule extends Object implements Module {

	private static final Logger LOG = Logger.getLogger(CassandraService.class);
	/* (non-Javadoc)
	 * @see com.google.inject.Module#configure(com.google.inject.Binder)
	 */
	@Override
	public void configure(Binder binder) {
		CassandraStorage cluster = new CassandraStorage();
		try {
			cluster = CassandraStorage.fromYaml(Files.toString(	new File(getClass().getClassLoader()
																					.getResource("./storageConfig.yaml")
																					.getFile()),
																Charsets.UTF_8));

		} catch (IOException e) {
			// ignore
			LOG.error(e);
		}
		LOG.info("Using cassandra cluster: " + cluster.toString());

		binder.bind(CassandraService.class);
		binder.bind(SchemaUtils.class);
		binder.bind(String.class)
				.annotatedWith(Host.class)
				.toInstance(cluster.getHosts()[0]);
		binder.bind(Integer.class)
				.annotatedWith(Port.class)
				.toInstance(cluster.getPort());
		binder.bind(String.class)
				.annotatedWith(SchemaFilename.class)
				.toInstance("schema-twissjava.txt");

		binder.bind(String.class)
				.annotatedWith(Cluster.class)
				.toInstance(cluster.getCluster());

		binder.bind(String.class)
				.annotatedWith(Keyspace.class)
				.toInstance(cluster.getKeyspace());

		binder.bind(Integer.class)
				.annotatedWith(ReplicationFactor.class)
				.toInstance(cluster.getReplicationFactor());
		binder.bind(HConsistencyLevel.class)
				.annotatedWith(WriteConsistencyLevel.class)
				.toInstance(cluster.getWriteConsistencyLevel());

		binder.bind(HConsistencyLevel.class)
				.annotatedWith(ReadConsistencyLevel.class)
				.toInstance(cluster.getReadConsistencyLevel());

	}

	public static class CassandraStorage {
		private String cluster = "Nebula Cassandra Cluster";

		private String keyspace = "Twissjava";

		private String[] hosts = new String[] { "localhost" };

		private int port = 9160;

		private int replicationFactor = 1;

		private HConsistencyLevel readConsistencyLevel = HConsistencyLevel.QUORUM;

		private HConsistencyLevel writeConsistencyLevel = HConsistencyLevel.QUORUM;

		public String getCluster() {
			return cluster;
		}

		public void setCluster(String cluster) {
			this.cluster = cluster;
		}

		public String getKeyspace() {
			return keyspace;
		}

		public void setKeyspace(String keyspace) {
			this.keyspace = keyspace;
		}

		public String[] getHosts() {
			return hosts;
		}

		public void setHosts(String[] hosts) {
			this.hosts = hosts;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String toYaml() {
			return new Yaml().dump(this);
		}

		public static CassandraStorage fromYaml(String config) {
			return (CassandraStorage) new Yaml().load(config);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((cluster == null) ? 0 : cluster.hashCode());
			result = prime * result + Arrays.hashCode(hosts);
			result = prime * result
					+ ((keyspace == null) ? 0 : keyspace.hashCode());
			result = prime * result + port;
			result = prime
					* result
					+ ((readConsistencyLevel == null) ? 0
							: readConsistencyLevel.hashCode());
			result = prime * result + replicationFactor;
			result = prime
					* result
					+ ((writeConsistencyLevel == null) ? 0
							: writeConsistencyLevel.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CassandraStorage other = (CassandraStorage) obj;
			if (cluster == null) {
				if (other.cluster != null)
					return false;
			} else if (!cluster.equals(other.cluster))
				return false;
			if (!Arrays.equals(hosts, other.hosts))
				return false;
			if (keyspace == null) {
				if (other.keyspace != null)
					return false;
			} else if (!keyspace.equals(other.keyspace))
				return false;
			if (port != other.port)
				return false;
			if (readConsistencyLevel != other.readConsistencyLevel)
				return false;
			if (replicationFactor != other.replicationFactor)
				return false;
			if (writeConsistencyLevel != other.writeConsistencyLevel)
				return false;
			return true;
		}

		public int getReplicationFactor() {
			return replicationFactor;
		}

		public void setReplicationFactor(int replicationFactor) {
			this.replicationFactor = replicationFactor;
		}

		public HConsistencyLevel getReadConsistencyLevel() {
			return readConsistencyLevel;
		}

		public void setReadConsistencyLevel(HConsistencyLevel consistencyLevel) {
			this.readConsistencyLevel = consistencyLevel;
		}

		public HConsistencyLevel getWriteConsistencyLevel() {
			return writeConsistencyLevel;
		}

		public void setWriteConsistencyLevel(
				HConsistencyLevel writeConsistencyLevel) {
			this.writeConsistencyLevel = writeConsistencyLevel;
		}

		@Override
		public String toString() {
			return "CassandraStorage [cluster=" + cluster + ", keyspace="
					+ keyspace + ", hosts=" + Arrays.toString(hosts)
					+ ", port=" + port + ", replicationFactor="
					+ replicationFactor + ", readConsistencyLevel="
					+ readConsistencyLevel + ", writeConsistencyLevel="
					+ writeConsistencyLevel + "]";
		}

	}
}
