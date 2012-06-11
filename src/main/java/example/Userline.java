package example;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;

import example.models.Timeline;
import example.models.Tweet;

/**
 * This is the typical twitter page. A form for submitting a 140-character
 *  tweet, and all the tweets that user has made and tweets from everyone
 *  that he is following.
 */
public class Userline extends HomePage {
    /**
	 * 
	 */
	private static final long serialVersionUID = -4893440430661022168L;
	private final String username;
    private Long nextpage;

	private enum UserListType {
		FRIEND,
		FOLLOWER
	};

	private static final int MAX_USER_LIST = 3;

    public Userline(final PageParameters parameters) {
        super(parameters);
        nextpage = parameters.get("nextpage").toLong(0);
        username = ((TwissSession) WebSession.get()).getUname();

        if (username == null) {
			throw new RestartResponseException(Publicline.class);
        }
        else {
			setup();
        }
    }

	private String getUsersLabel(String username, UserListType type) {
		checkNotNull(username);
		int counter = 0;
		Set<String> users = null;

		if (type == UserListType.FRIEND) {
			counter = (int) getFriendsCount(username);
			users = ImmutableSortedSet.copyOf(getFriendUnames(username, MAX_USER_LIST));
		} else if (type == UserListType.FOLLOWER) {
			counter = (int) getFollowersCount(username);
			users = ImmutableSortedSet.copyOf(getFollowerUnames(username, MAX_USER_LIST));
		} else {
			throw new IllegalArgumentException(type + " is not a valid "
					+ UserListType.class.getSimpleName());
		}

		StringBuilder s = new StringBuilder();
		s.append(counter);

		if (counter > 0) {

			s.append(" (");
			s.append(Joiner.on(',')
							.join(users));

			int remaining = counter - users.size();

			if (remaining > 0) {
				s.append(" and ");
				s.append(remaining);
				s.append(" more");
			}

			s.append(")");
		}

		return s.toString();
	}

	private void setup() {
		String friendsLabel = getUsersLabel(username, UserListType.FRIEND);
		String followersLabel = getUsersLabel(username, UserListType.FOLLOWER);
		add(new TweetForm("poster"));
		add(new Label(	"friends",
						friendsLabel));
		add(new Label(	"followers",
						followersLabel));


        Timeline timeline = getUserline(username, nextpage);
        List<Tweet> tweets;//timeline.getView();

        if (null == timeline) {
            tweets = new ArrayList<Tweet>(0);
        } else {
            tweets = timeline.getView();
        }

        if (tweets.size() > 0) {
            add(new ListView<Tweet>("tweetlist", tweets) {
                /**
				 * 
				 */
				private static final long serialVersionUID = -7413317851324521943L;

				@Override
                public void populateItem(final ListItem<Tweet> listitem) {
					listitem.add(new Link<String>("link") {
                        /**
						 * 
						 */
						private static final long serialVersionUID = 6663254615255592969L;

						@Override
                        public void onClick() {
                            PageParameters p = new PageParameters();
                            p.add("username", listitem.getModel().getObject().getUname());
                            setResponsePage(Publicline.class, p);
                        }
                    }.add(new Label("tuname",listitem.getModel().getObject().getUname())));
                    listitem.add(new Label("tbody", ": " + listitem.getModel().getObject().getBody()));
					listitem.add(DateLabel.forDateStyle("datetime",
															new IModel<Date>() {

																private static final long serialVersionUID = 1L;

																@Override
																public void detach() {
																	//

																}

																@Override
																public void setObject(
																		Date object) {
																	//

																}

																@Override
																public Date getObject() {
																	return new Date(listitem.getModel()
																							.getObject()
																							.getTimestamp());

																}
														},
														"SS"));
                }
            }).setVersioned(false);
            Long linktopaginate = timeline.getNextview();
            if (linktopaginate != null) {
                nextpage = linktopaginate;
                WebMarkupContainer pagediv = new WebMarkupContainer("pagedown");
                PageForm pager = new PageForm("pageform");
                pagediv.add(pager);
                add(pagediv);
            }
            else {
                add(new WebMarkupContainer("pagedown") {
                    /**
					 * 
					 */
					private static final long serialVersionUID = 8869323692236862225L;

					@Override
                    public boolean isVisible() {
                        return false;
                    }
                });
            }
        }
        else {
            ArrayList<String> hack = new ArrayList<String>(1);
            hack.add("There are no tweets yet. Post one!");
            add(new ListView<String>("tweetlist", hack ) {
                /**
				 * 
				 */
				private static final long serialVersionUID = -5128393223572874187L;

				@Override
                public void populateItem(final ListItem<String> listitem) {
					listitem.add(new Link<String>("link") {
                        /**
						 * 
						 */
						private static final long serialVersionUID = -3127820289077794200L;

						@Override
                        public void onClick() {
                        }
                    }.add(new Label("tuname","")));
                    listitem.add(new Label("tbody", listitem.getModel().getObject()));
					listitem.add(new Label(	"datetime",
											listitem.getModel()
													.getObject()));
                }
            }).setVersioned(false);
            add(new WebMarkupContainer("pagedown") {
                /**
				 * 
				 */
				private static final long serialVersionUID = 296155464292010568L;

				@Override
                public boolean isVisible() {
                    return false;
                }
            });
        }
    }

	private class PageForm extends Form<PageForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -2660860800827486928L;
		public PageForm(String id) {
            super(id);
        }
        @Override
        public void onSubmit() {

			setResponsePage(getPage().getClass(),
							new PageParameters().add("nextpage", nextpage));
        }
    }

	private class TweetForm extends Form<TweetForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1359967079163951398L;
		private String tweetbody;

        public TweetForm(String id) {
            super(id);

			add(new TextArea<String>(	"tweetbody",
										PropertyModel.<String> of(	this,
																	"tweetbody")).setRequired(true)).add(new FeedbackPanel("feedback"));

        }
        @Override
        public void onSubmit() {
			saveTweet(new Tweet(UUID.randomUUID()
									.toString()
									.getBytes(),
								username,
								tweetbody,
								System.currentTimeMillis()));
            setResponsePage(getPage().getClass());
        }
    }
}
