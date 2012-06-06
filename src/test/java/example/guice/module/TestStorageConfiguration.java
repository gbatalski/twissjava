/**
 * 
 */
package example.guice.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import example.guice.module.CassandraModule.CassandraStorage;
/**
 * @author gena
 *
 */
public class TestStorageConfiguration {
	@Test
	public void testToYaml() {
		CassandraStorage cs = new CassandraStorage();
		cs.toYaml();
		assertEquals(	cs,
						CassandraStorage.fromYaml("!!example.guice.module.CassandraModule$CassandraStorage\n"
				+ "cluster: Nebula Cassandra Cluster\n"
				+ "hosts: [localhost]\n" + "keyspace: Twissjava\n"
							+ "port: 9160\n"));
		}
}
