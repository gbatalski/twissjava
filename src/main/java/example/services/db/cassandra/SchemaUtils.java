/**
 * 
 */
package example.services.db.cassandra;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.write;
import static java.lang.String.format;

import java.io.File;
import java.security.Permission;

import org.apache.cassandra.cli.CliMain;
import org.apache.log4j.Logger;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import example.guice.annotation.Host;
import example.guice.annotation.Keyspace;
import example.guice.annotation.Port;
import example.guice.annotation.ReplicationFactor;
import example.guice.annotation.SchemaFilename;
/**
 * @author gena
 * 
 */
@Singleton
public class SchemaUtils {
	private final String host;

	private final Integer port;

	private final File schemaFile;

	private final String keyspace;

	private final int replicationFactor;

	private static final Logger LOG = Logger.getLogger(SchemaUtils.class);

	@Inject
	public SchemaUtils(@Host String host, @Port Integer port,
			@SchemaFilename String schemaFile, @Keyspace String keyspace,
			@ReplicationFactor int replicationFactor) {
		this.host = host;
		this.port = port;
		this.keyspace = keyspace;
		this.schemaFile = new File(this.getClass()
										.getResource("/" + schemaFile)
										.getFile());

		this.replicationFactor = replicationFactor;
	}

	public void deploySchema() {
		this.deploySchema(replicationFactor, false);
	}

	public void deploySchema(final int replicationFactor, final boolean drop) {
		new Runnable() {
			@Override
			public void run() {
				final SecurityManager securityManager = new SecurityManager() {
					@Override
					public void checkPermission(Permission permission) {
						// If you want to forbid any kind of halt, just use
						// "exitVM"
						if (permission.getName()
										.contains("exitVM")) {
							throw new SecurityException("System.exit calls not allowed!");
						}
					}
				};
				SecurityManager savedManager = System.getSecurityManager();
				System.setSecurityManager(securityManager);
				try {
					
					File f = null;
					if (drop) {
						f = File.createTempFile("drop", null);
						write(format("drop keyspace %s;", keyspace), f, UTF_8);

						CliMain.main(new String[] { "-h", host, "-p",
								port.toString(), "-f", f.getAbsolutePath() });
						f.delete();
					}
					f = File.createTempFile("schema", null);

					String content = Files.toString(schemaFile, UTF_8);
					content.replaceAll("(replication_factor\\s*:)\\s*\\d", "$1"
							+ replicationFactor);
					write(content, f, UTF_8);
					LOG.info("Replication factor " + replicationFactor);
					LOG.info("Create or update schema with " + content);
					CliMain.main(new String[] { "-h", host, "-p",
							port.toString(), "-f", f.getAbsolutePath() });
					f.delete();
				} catch (SecurityException e) {
					// Nothing
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					System.setSecurityManager(savedManager);
				}

			}
		}.run();
	}
}
