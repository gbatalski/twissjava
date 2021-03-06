package example;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import example.models.User;

/**
 * Authorize allows viewers to log in as a user or create
 *  a user account.
 */
public class Authorize extends Base {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7960746156253945782L;
	private final Boolean login;
    private final Boolean register;

    public Authorize(final PageParameters parameters) {
        super(parameters);
        login = parameters.get("login").toBoolean(true);
        register = parameters.get("register").toBoolean();
        TwissSession s = (TwissSession) WebSession.get();

        setup();
        if (s.getUname() != null) {
            //User is logged in. Log them out and redirect to home page.
            s.authorize(null);
            //setRedirect(true);
            setResponsePage(getPage().getClass());
        }
    }

    private void setup() {
        LoginForm l = new LoginForm("login");
        l.add(new Label("loginerror",loginErrorMsg()));
        add(l);
        //Make and define a RegisterForm. Same as above.
        RegisterForm r = new RegisterForm("signup");
        r.add(new Label("regerror",registerErrorMsg()));
        add(r);
    }

    private String loginErrorMsg() {
        //get login param
        if (!login) {
            return "Invalid username/password entered.";
        }
        return "";
    }

    private String registerErrorMsg() {
        //get regis param
        if (register != null) {
            if (register) {
                return "Username already taken.";
            }
            else {
                return "Passwords do not match.";
            }
        }
        return "";
    }

	private class LoginForm extends Form<LoginForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -6972788719244812360L;
		private String username;
        private String password;

        public LoginForm(String id) {
            super(id);
			add(new TextField<String>(	"username",
										PropertyModel.<String> of(	this,
															"username")));
			add(new PasswordTextField(	"password",
										PropertyModel.<String> of(	this,
																	"password")));
        }
        @Override
        public void onSubmit() {
            User test = getUserByUsername(username);
            if (test == null) {
				setResponsePage(getPage().getClass(),
								new PageParameters().add("login", false));
                return;
            }
            if (!test.comparePasswords(password)){
				setResponsePage(getPage().getClass(),
								new PageParameters().add("login", false));
                return;
            }
            TwissSession s = (TwissSession) WebSession.get();
            s.authorize(username);
            setResponsePage(Userline.class);
        }
    }

	class RegisterForm extends Form<RegisterForm> {
        /**
		 * 
		 */
		private static final long serialVersionUID = -600476161343749424L;
		private String new_username;
        private String password1;
        private String password2;

        public RegisterForm(String id) {
            super(id);
			add(new TextField<String>(	"new_username",
								PropertyModel.<String> of(this, "new_username")));
			add(new PasswordTextField(	"password1",
										PropertyModel.<String> of(	this,
																	"password1")));
			add(new PasswordTextField(	"password2",
										PropertyModel.<String> of(	this,
																	"password2")));
        }
        @Override
        public void onSubmit() {
            User test = getUserByUsername(new_username);
            if (test != null) {
				setResponsePage(getPage().getClass(),
								new PageParameters().add("register", true));
                return;
            }
            if (!password1.equals(password2)) {
				setResponsePage(getPage().getClass(),
								new PageParameters().add("register", false));
                return;
            }
            test = new User(new_username.getBytes(), password1);
            saveUser(test);
            TwissSession s = (TwissSession) WebSession.get();
            s.authorize(new_username);
            setResponsePage(Userline.class);
        }
    }
}