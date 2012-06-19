/**
 * 
 */
package example;


import static com.google.common.collect.ImmutableList.copyOf;
import me.prettyprint.cassandra.service.OperationType;
import me.prettyprint.hector.api.HConsistencyLevel;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;


/**
 * @author gena
 *
 */
public class SchemaUpdate extends Base {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean drop;

	private int replicationFactor = cassandra.getReplicationFactor();

	private HConsistencyLevel readConsistencyLevel = cassandra.getConsistencyLevelPolicy()
																	.get(OperationType.READ);

	private HConsistencyLevel writeConsistencyLevel = cassandra.getConsistencyLevelPolicy()
																.get(OperationType.WRITE);

	public SchemaUpdate(PageParameters parameters) {
		super(parameters);
		SchemaUpdateForm suf = new SchemaUpdateForm("updateForm");
		add(suf);

		suf.add(new CheckBox(	"drop",
								PropertyModel.<Boolean> of(this, "drop")))
			.add(new TextField<Integer>("replicationFactor",
										PropertyModel.<Integer> of(	this,
																	"replicationFactor")))
			.add(new ListChoice<HConsistencyLevel>(	"readConsistencyLevel",
													PropertyModel.<HConsistencyLevel> of(	this,
																							"readConsistencyLevel"),
													copyOf(HConsistencyLevel.values())))
			.add(new ListChoice<HConsistencyLevel>(	"writeConsistencyLevel",
													PropertyModel.<HConsistencyLevel> of(	this,
																							"writeConsistencyLevel"),
													copyOf(HConsistencyLevel.values())));

	}

	private class SchemaUpdateForm extends Form<SchemaUpdateForm> {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public SchemaUpdateForm(String id) {
			super(id);
		}

		@Override
		public void onSubmit() {
			cassandra.updateSchema(	drop,
									replicationFactor,
									readConsistencyLevel,
									writeConsistencyLevel);
		}
	}

	public boolean isDrop() {
		return drop;
	}

	public void setDrop(boolean drop) {
		this.drop = drop;
	}

	public int getReplicationFactor() {
		return replicationFactor;
	}

	public void setReplicationFactor(int replication) {
		this.replicationFactor = replication;
	}

	public HConsistencyLevel getWriteConsistencyLevel() {
		return writeConsistencyLevel;
	}

	public void setWriteConsistencyLevel(HConsistencyLevel consistency) {
		this.writeConsistencyLevel = consistency;
	}

	public HConsistencyLevel getReadConsistencyLevel() {
		return readConsistencyLevel;
	}

	public void setReadConsistencyLevel(HConsistencyLevel readConsistencyLevel) {
		this.readConsistencyLevel = readConsistencyLevel;
	}
}
