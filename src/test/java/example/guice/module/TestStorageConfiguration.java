/**
 * 
 */
package example.guice.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import me.prettyprint.hector.api.HConsistencyLevel;

import org.junit.Test;

import com.google.common.base.Joiner;

import example.guice.module.CassandraModule.CassandraStorage;
/**
 * @author gena
 *
 */
public class TestStorageConfiguration {
	@Test
	public void testToYaml() {
		CassandraStorage cs = new CassandraStorage();
		cs.setHosts(new String[] { "184.72.179.92", "184.72.179.91" });
		cs.setReplicationFactor(2);
		cs.setReadConsistencyLevel(HConsistencyLevel.ONE);
		cs.setWriteConsistencyLevel(HConsistencyLevel.QUORUM);
		cs.toYaml();
		assertEquals(	cs,
						CassandraStorage.fromYaml(
						                          	Joiner.on("\n")
														.join(	"!!example.guice.module.CassandraModule$CassandraStorage",
																"cluster: Nebula Cassandra Cluster",
																"hosts: [184.72.179.92, 184.72.179.91]",
																"keyspace: Twissjava",
																"port: 9160",
																"readConsistencyLevel: ONE",
																"replicationFactor: 2",
																"writeConsistencyLevel: QUORUM",
																"")));

		cs = CassandraStorage.fromYaml(Joiner.on("\n")
										.join("!!example.guice.module.CassandraModule$CassandraStorage",
														"cluster: Nebula Cassandra Cluster",
														"hosts: [localhost, huj]",
														"keyspace: Twissjava",
														"port: 9160",
														""));
		assertNotNull(cs);
	}
}
