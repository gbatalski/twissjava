/**
 * 
 */
package example.guice.module;

import com.google.inject.Binder;
import com.google.inject.Module;

import example.guice.annotation.Cluster;
import example.guice.annotation.Host;
import example.guice.annotation.Keyspace;
import example.guice.annotation.Port;
import example.guice.annotation.SchemaFilename;
import example.services.db.cassandra.CassandraService;
import example.services.db.cassandra.SchemaUtils;

/**
 * @author gena
 *
 */
public class CassandraModule extends Object implements Module {

	/* (non-Javadoc)
	 * @see com.google.inject.Module#configure(com.google.inject.Binder)
	 */
	@Override
	public void configure(Binder binder) {

		binder.bind(CassandraService.class);
		binder.bind(SchemaUtils.class);
		binder.bind(String.class)
				.annotatedWith(Host.class)
				.toInstance("localhost");
		binder.bind(Integer.class)
				.annotatedWith(Port.class)
				.toInstance(9160);
		binder.bind(String.class)
				.annotatedWith(SchemaFilename.class)
				.toInstance("schema-twissjava.txt");

		binder.bind(String.class)
				.annotatedWith(Cluster.class)
				.toInstance("Nebula Cassandra Cluster");

		binder.bind(String.class)
				.annotatedWith(Keyspace.class)
				.toInstance("Twissjava");

	}

}
