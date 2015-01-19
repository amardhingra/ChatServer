import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

	// Threadsafe hashmap mapping usernames to user objects
	ConcurrentHashMap<String, User> users;

	public Server() {

		users = new ConcurrentHashMap<String, User>();

		// read the file into the hashmap
		readUsers(Codes.FILENAME);

	}

	public void start(int port) {

		// start a cleaningthread which automatically logs out inactive users
		CleaningThread cleaner = new CleaningThread(users);
		cleaner.start();

		try {

			// create the server socket on which to listen for incoming
			// connections
			ServerSocket socket = new ServerSocket(port);

			for (;;) {

				// accept new connections and create new threads to handle the
				// clients
				Socket clientSocket = socket.accept();
				new ServerThread(users, clientSocket, cleaner).start();

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void readUsers(String filename) {

		try {
			// create a bufferedreader to read the file
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			// loop while the file has more text
			String line = reader.readLine();
			while (line != null) {

				// add new user for every lie of the file
				users.put(line.split(" ")[0],
						new User(line.split(" ")[0], line.split(" ")[1]));
				line = reader.readLine();
			}

			reader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public class ServerThread extends Thread {

		// variables required for each server thread
		ConcurrentHashMap<String, User> users;
		Socket socket;
		PrintWriter output;
		BufferedReader input;
		CleaningThread cleaner;

		public ServerThread(ConcurrentHashMap<String, User> usrs, Socket sock,
				CleaningThread cleaner) {

			this.users = usrs;
			this.socket = sock;
			this.cleaner = cleaner;

			try {
				// create an input and output for the socket
				input = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				output = new PrintWriter(socket.getOutputStream());

			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.exit(0);
			}

			// add a shutdown hook to close IO
			Runtime.getRuntime().addShutdownHook(
					new CleanupThread(socket, input, output));
		}

		public void run() {
			try {

				// get the User object for the client logging in
				User activeUser = login();

				// wake up the cleaner thread
				cleaner.interrupt();

				// check that a valid user was received
				if (activeUser != null) {

					String inputString = null;

					for (;;) {

						// read the input and updated the last issued command
						// time
						inputString = input.readLine();

						activeUser.setLastIssuedCommand(System
								.currentTimeMillis());

						// if the user was going to be logged out for inactivity
						// notify the cleaner that they have issued a command
						if (activeUser.isGoingToBeLoggedOut)
							cleaner.interrupt();

						if (inputString.equals(Codes.WHO_ELSE)) {

							// send start of message
							activeUser.sendMessage(Codes.WHO_ELSE);

							// Send the username of every other logged in user
							User user = null;
							for (Entry<String, User> entry : users.entrySet()) {
								user = entry.getValue();
								if (user.isLoggedIn
										&& !user.username
												.equals(activeUser.username))
									activeUser.sendMessage(user.username);
							}
							// send end of message
							activeUser.sendMessage(Codes.EOT);

						} else if (inputString.equals(Codes.WHO_LAST_HOUR)) {

							// send start of message
							activeUser.sendMessage(Codes.WHO_LAST_HOUR);

							// send every user other user who logged on in the
							// last hour
							User user = null;
							for (Entry<String, User> entry : users.entrySet()) {
								user = entry.getValue();

								if (System.currentTimeMillis()
										- user.lastLoggedIn < Codes.REAL_LAST_HOUR
										&& !user.username
												.equals(activeUser.username))
									activeUser.sendMessage(user.username);
							}

							// send end of message
							activeUser.sendMessage(Codes.EOT);

						} else if (inputString.equals(Codes.BROADCAST)) {

							// read the message they want to broadcast
							String broadcastMessage = input.readLine();

							// send the message to every other logged on user
							User user = null;
							for (Entry<String, User> entry : users.entrySet()) {
								user = entry.getValue();
								if (user.isLoggedIn
										&& !user.username
												.equals(activeUser.username)) {
									user.sendMessage(Codes.BROADCAST);
									user.sendMessage("(BROADCAST) "
											+ activeUser.username + ": "
											+ broadcastMessage);
									// user.sendMessage(broadcastMessage);
								}
							}

							activeUser.sendMessage(Codes.OK);

						} else if (inputString.equals(Codes.MESSAGE)) {

							// read the name of the recipient and the message
							String messageRecipient = input.readLine();
							String message = input.readLine();

							User user = users.get(messageRecipient);

							// if the user is online send them the message
							if (user != null && user.isLoggedIn) {
								user.sendMessage(Codes.MESSAGE);
								user.sendMessage(activeUser.username + ": "
										+ message);
								// user.sendMessage(message);
							}
							// if they are offline add it to their list of
							// messages
							else if (user != null && !user.isLoggedIn) {
								user.addOfflineMessage(activeUser.username
										+ ": " + message);
							}

							activeUser.sendMessage(Codes.OK);

						} else if (inputString.equals(Codes.RETRIEVE_MESSAGES)) {

							// send the user all the messages they recieved
							// while they were offline
							activeUser.sendOfflineMessages();

						} else if (inputString.equals(Codes.LOGOUT)) {

							// logout the user and end the thread
							activeUser.logout();
							break;

						} else if (inputString.equals(Codes.GROUP_MESSAGE)) {

							String[] recipients = input.readLine().split(",");
							String message = input.readLine();
							
							// send the message to all the members of the group
							for (String recipient : recipients) {
								User user = users.get(recipient);
								if (user != null)
									if (user.isLoggedIn)
										user.sendMessage(Codes.MESSAGE
												+ Codes.NL
												+ activeUser.username + ": "
												+ message);
									else
										user.addOfflineMessage(activeUser.username
												+ ": " + message);
							}

							// tell the user the message was sent
							activeUser.sendMessage(Codes.OK);

						} else {
							// invalid code
							activeUser.sendMessage(Codes.INVALID_MESSAGE_TYPE);
						}

					}

				}

			} catch (IOException e) {
			} catch (NullPointerException e) {
			}

			// start a cleanupthread to close IO
			// new CleanupThread(socket, input, output).start();

		}

		private User login() throws IOException, NullPointerException {

			// variables to check login attempts and block users
			int loginAttempts = 0;
			String username = null;
			String lastUsername = null;
			String password = null;
			String type = null;

			User currentUser = null;

			while (true) {

				// read the type of message, username and password
				type = input.readLine();
				username = input.readLine();
				password = input.readLine();

				if (type.equals(Codes.LOGIN)) {

					// check if a user with the username exists
					currentUser = users.get(username);

					if (currentUser == null) {

						// Invalid user
						output.write(Codes.INVALID_USERNAME + Codes.NL);
						output.flush();
					}

					else if (!currentUser.password.equals(password)) {

						// Invalid password

						if (username.equals(lastUsername)) {

							// same user has attampted multiple failed logins
							loginAttempts++;

							if (loginAttempts == 3) {

								// block the user
								currentUser.isBlocked = true;
								currentUser.lastBlockedTime = System
										.currentTimeMillis();
								currentUser.blockedIP = socket.getInetAddress()
										.toString();
								output.write(Codes.BLOCKED + Codes.NL);
								output.flush();

							} else {
								// tell the user they sent the wrong password
								output.write(Codes.INVALID_PASSWORD + Codes.NL);
								output.flush();
							}
						} else {

							// tell the user they sent the wrong password
							output.write(Codes.INVALID_PASSWORD + Codes.NL);
							output.flush();

							// save the username of the user who tried to login
							lastUsername = username;
							loginAttempts = 1;

						}
					} else {

						// If the user is blocked and they are trying to login
						// from the same IP
						// tell them they are blocked
						if (currentUser.isBlocked
								&& currentUser.lastBlockedTime
										- System.currentTimeMillis() < Codes.REAL_BLOCK_TIME
								&& socket.getInetAddress().toString()
										.equals(currentUser.blockedIP)) {
							output.write(Codes.BLOCKED + Codes.NL);
							output.flush();
						}

						// if the user is already logged in on another instance
						// inform the user
						else if (currentUser.isLoggedIn) {
							output.write(Codes.ALREADY_LOGGED_IN + Codes.NL);
							output.flush();
						}

						// log the user in
						currentUser.login(input, output);
						return currentUser;
					}

				} else if (type.equals(Codes.SIGNUP)) {

					// search for user with username recieved
					User user = users.get(username);

					if (user == null) {

						// create a new user and add it to the map
						user = new User(username, password);
						users.put(username, user);

						User u = null;

						// save the updated list of username password
						// combinations
						PrintWriter pw = new PrintWriter(new File(
								Codes.FILENAME));
						for (Entry<String, User> entry : users.entrySet()) {
							u = entry.getValue();
							pw.write(u.username + " " + u.password + Codes.NL);
							pw.flush();
						}
						pw.close();

						user.login(input, output);
						return user;

					} else {
						// user with this username already exists
						output.write(Codes.UNAME_TAKEN + Codes.NL);
						output.flush();
					}

				}

			}

		}

	}

	public static void main(String[] args) {

		// check that the user has given a port number
		if (args.length < 1) {
			System.out.println("Invalid arguments\nUsage [Port]");
			System.exit(1);
		}

		// start running the server
		new Server().start(Integer.parseInt(args[0]));
	}

}
