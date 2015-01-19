public class Codes {

	public static final int BLOCK_TIME = 60;
	public static final int TIME_OUT = 30;
	public static final int LAST_HOUR = 60;

	// actual times for the variables to compare with System.currentTimeMillis()
	public static final long REAL_BLOCK_TIME = BLOCK_TIME * 60 * 1000;
	public static final long REAL_TIME_OUT = TIME_OUT * 60 * 1000;
	public static final long REAL_LAST_HOUR = LAST_HOUR * 60 * 1000;

	// list of all the commands a user can send the server
	public static final String WHO_ELSE = "whoelse";
	public static final String WHO_LAST_HOUR = "wholasthr";
	public static final String BROADCAST = "broadcast";
	public static final String MESSAGE = "message";
	public static final String RETRIEVE_MESSAGES = "mymessages";
	public static final String GROUP_MESSAGE = "group";
	public static final String LOGOUT = "logout";

	public static final String LOGIN = "login";
	public static final String SIGNUP = "signup";
	
	// login and signup return types
	public static final String OK = "OK";
	public static final String INVALID_USERNAME = "err_uname";
	public static final String INVALID_PASSWORD = "err_pword";
	public static final String BLOCKED = "blocked";
	public static final String ALREADY_LOGGED_IN = "logged_in";
	public static final String UNAME_TAKEN = "uname_tkn";

	public static final String EOT = "end";
	public static final String INVALID_MESSAGE_TYPE = "err_type";
	public static final String COMMAND_PROMPT = "Command: ";
	public final static String NL = "\n";
	public final static String FILENAME = "user_pass.txt";

	public static final boolean DEBUG = true;

}
