import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class User {

	public String username;
	public String password;

	public BufferedReader input;
	public PrintWriter output;

	public boolean isLoggedIn;
	public long lastLoggedIn;
	public long lastIssuedCommand;

	public boolean isBlocked;
	public long lastBlockedTime;
	public String blockedIP;

	public ArrayList<String> offlineMessages;

	public boolean isGoingToBeLoggedOut;

	public User(String uname, String pword) {
		this.username = uname;
		this.password = pword;
	}

	public void sendMessage(String message) {
		if (isLoggedIn && output != null) {
			output.write(message + Codes.NL);
			output.flush();
		}
	}

	public void sendOfflineMessages() {

		sendMessage(Codes.RETRIEVE_MESSAGES);
		for (String offlineMessage : offlineMessages)
			sendMessage(offlineMessage);
		sendMessage(Codes.EOT);
		offlineMessages = null;

	}

	public void addOfflineMessage(String offlineMessage) {
		if (offlineMessages == null) {
			offlineMessages = new ArrayList<String>();
		}
		offlineMessages.add(offlineMessage);
	}

	public void login(BufferedReader input, PrintWriter output) {
		lastLoggedIn = System.currentTimeMillis();
		lastIssuedCommand = lastLoggedIn;
		isLoggedIn = true;
		this.output = output;
		this.input = input;
		sendMessage(Codes.OK);
	}

	public void logout() {
		sendMessage(Codes.LOGOUT);
		if (output != null)
			output.close();
		input = null;
		output = null;
		lastIssuedCommand = 0;
		isLoggedIn = false;
	}

	public void setLastIssuedCommand(long lastIssuedCommand) {
		this.lastIssuedCommand = lastIssuedCommand;
	}
	
	public String toString(){return username;}

}
