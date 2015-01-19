import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class CleaningThread extends Thread {

	ConcurrentHashMap<String, User> users;

	public CleaningThread(ConcurrentHashMap<String, User> usrs) {
		this.users = usrs;
	}

	public void run() {

		for (;;) {

			User user = null;
			User first = null;

			for (Entry<String, User> entry : users.entrySet()) {

				// get each user
				user = entry.getValue();

				// reset the logout indicaters
				user.isGoingToBeLoggedOut = false;

				// check if we have a logged in user who has issued a command
				// less recently
				// than our current maximum
				if ((first == null && user.isLoggedIn && user.lastIssuedCommand != 0)
						|| (user.isLoggedIn && user.lastIssuedCommand < first.lastIssuedCommand)) {
					first = user;
				}

			}

			// if we have a user sleep until they have to be logged out
			if (first != null) {
				first.isGoingToBeLoggedOut = true;
				try {
					Thread.sleep(Codes.REAL_TIME_OUT
							- (System.currentTimeMillis() - first.lastIssuedCommand));
					// logout the user if they haven't issued a command
					first.logout();
				} catch (InterruptedException e) {
					// if the user issued a command the sleep is interrupted
				}
			} else {
				// if no one has logged in sleep for 15 seconds
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					// sleep gets interrupted when a user logs on
				}
			}

		}

	}
}
