package example;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import example.models.User;

/**
 * AddFriends has a query section for finding a user that may exist
 *  on the system, as well as an action for friending/defriending that
 *   user if they exist. A user cannot friend himself.
 */
public class AddFriends extends Base {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1698292222005748513L;
	private String username;
    private String query;
    private Boolean found;
    private Boolean act;

    public AddFriends(final PageParameters parameters) {
        super(parameters);
        username = ((TwissSession) WebSession.get()).getUname();
        
		query = parameters.get("query")
							.toOptional(String.class);
        if (query == null) {
            query = "";
        }
		found = parameters.get("found")
							.toOptional(Boolean.class);

		act = parameters.get("act")
						.toOptional(Boolean.class);


        add(new FriendForm("friendfinder"));

        WebMarkupContainer action = new WebMarkupContainer("action") {
            /**
			 * 
			 */
			private static final long serialVersionUID = 7830131077362480652L;

			@Override
            public boolean isVisible() {
                return (found != null) && (found) && (username != null) && (!query.equals(username));
            }
        };

        add(new Label("flash", getFlashMsg()));

		Form<ActionFriendForm> aff = new ActionFriendForm("actionfriend");

        String actiontext = "";
        List<String> friendUnames = getFriendUnames(username);
        if (action.isVisible()) {
            if (friendUnames.contains(query)) {
                actiontext = "Remove Friend ";
            }
            else {
                actiontext = "Add Friend ";
            }
        }
        
        
            add(new ListView<String>("friendlist", friendUnames) {
                /**
				 * 
				 */
				private static final long serialVersionUID = 2976401971784724238L;

				@Override
                public void populateItem(final ListItem<String> listitem) {
					listitem.add(new Link<String>("link") {
                        /**
						 * 
						 */
						private static final long serialVersionUID = 726753731699398445L;

						@Override
                        public void onClick() {

							setResponsePage(Publicline.class,
											new PageParameters().add(	"username",
																		listitem.getModel()
																				.getObject()));
                        }
					}.add(new Label("tuname",
									listitem.getModel()
											.getObject())));
					listitem.add(new Label(	"tbody",
											": " + listitem.getModel()
															.getObject()));
                }
            }).setVersioned(false);

        
        aff.add(new Label("actionname", actiontext));
        //TODO : I really don't like not having it on the button itself. ;_;
        action.add(aff);
        add(action);
    }

    private String getFlashMsg() {
        if (act != null) {
            if (act) {
                return "Friendship was established.";
            }
            if (!act) {
                return "Friendship was broken.";
            }
        }
        if (found != null) {
            if (!found) {
                return query + " is not here.";
            }
            if (found) {
                if ((query != null) && (query.equals(username))) {
                    return "You attempt to befriend yourself and fail.";
                }
                else {
                    return query + " is here!";
                }
            }
        }
        //default nothing
        return "";
    }

	private class FriendForm extends Form<FriendForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -8179213315208310890L;
		private String q;

        public FriendForm(String id) {
            super(id);
			add(new RequiredTextField<String>(	"q",
										PropertyModel.<String> of(this,
																			"q"))).add(new FeedbackPanel("feedback"));
        }
        @Override
        public void onSubmit() {
            User test = getUserByUsername(q);
            if (test == null) {
                setResponsePage(getPage().getClass(),new PageParameters().add("query",q)
													.add("found", false));
            }
            else {

				setResponsePage(getPage().getClass(),
								new PageParameters().add("query", q)
													.add("found", true));
            }
        }
    }

    //<p>Hooray, this page exists!</p>
    //<p>There was nobody with username {{ q }}</p>
    //<p>Enter a username above to see if they are on the site!</p>

	private class ActionFriendForm extends Form<ActionFriendForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = 3958649030743533718L;
		public ActionFriendForm(String id) {
            super(id);
        }
        @Override
        public void onSubmit() {
            List<String> friendUnames = getFriendUnames(username);
            if (friendUnames.contains(query)) {
                List<String> friendname = new ArrayList<String>();
                friendname.add(query);
                removeFriends(username, friendname);
				setResponsePage(getPage().getClass(),
								new PageParameters().add("act", false));
            }
            else {
                List<String> friendname = new ArrayList<String>();
                friendname.add(query);
                addFriends(username, friendname);
				setResponsePage(getPage().getClass(),
								new PageParameters().add("act", true));
            }
        }
    }
}