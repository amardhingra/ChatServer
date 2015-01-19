import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	// class variables
	Socket socket;
	PrintWriter output;
	BufferedReader input;
	boolean loggedIn;
	static ListeningThread listener;
	static SendingThread sender;

	public Client(String ipAddress, int port) {

		try {
			// creating a socket connection to the server
			socket = new Socket(ipAddress, port);

			// creating IO for the connection
			input = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		try {

			// Continously try to sign in
			while (!loggedIn)
				loggedIn = login(input, output);

			// create a thread to listen for incoming messages
			listener = new ListeningThread(input);
			listener.start();
			// create a thread to send messages to the server
			sender = new SendingThread(output);
			sender.start();

			// thread to cleanup in case user terminates the program
			Runtime.getRuntime().addShutdownHook(
					new CleanupThread(socket, input, output));

		} catch (IOException e) {
		} catch (NullPointerException e) {
			// NullPointerException gets thrown if connection to the server gets
			// lost
			System.out.println("Connection to server lost. Exiting...");
			new CleanupThread(socket, input, output).start();
		}

	}

	public static boolean login(BufferedReader input, PrintWriter output)
			throws IOException, NullPointerException {

		// Scanner for user input
		Scanner userInput = new Scanner(System.in);

		// taking user input
		System.out.print("Would you like to Login or Signup? (login/signup) ");
		String option = userInput.nextLine();

		System.out.print("Username: ");
		String username = userInput.nextLine();

		System.out.print("Password: ");
		String password = userInput.nextLine();

		// user wishes to login
		if (option.equals(Codes.LOGIN)) {

			// write login code, username and password to server and read
			// response
			output.write(Codes.LOGIN + "\n" + username + "\n" + password + "\n");
			output.flush();
			String response = input.readLine();

			if (response.equals(Codes.OK)) {
				// user logged in successfully
				System.out.println("Welcome to the chat room " + username);
				System.out.print(Codes.COMMAND_PROMPT);
				return true;
			}

			// if the user doesn't successfully login let them know the reason
			else if (response.equals(Codes.INVALID_USERNAME)) {
				System.out.println("Invalid username");
			} else if (response.equals(Codes.INVALID_PASSWORD)) {
				System.out.println("Invalid password");
			} else if (response.equals(Codes.ALREADY_LOGGED_IN)) {
				System.out
						.println("Sorry. You are already logged in from another system");
			} else if (response.equals(Codes.BLOCKED)) {
				System.out
						.println("You have been blocked for multiple failed login requests");
			}

			// login failed
			return false;

		}
		// user wishes to signup for a new account
		else if (option.equals(Codes.SIGNUP)) {

			// write signup code, username and password to server and get
			// response
			output.write(Codes.SIGNUP + "\n" + username + "\n" + password
					+ "\n");
			output.flush();

			String response = input.readLine();

			// successful login
			if (response.equals(Codes.OK)) {
				System.out.println("Welcome to the chat room " + username);
				System.out.print(Codes.COMMAND_PROMPT);
				return true;
			}
			// username already taken
			else if (response.equals(Codes.UNAME_TAKEN)) {
				System.out
						.println("Sorry that username has already been taken");
				return false;
			}

		}

		return false;

	}

	private static class ListeningThread extends Thread {

		// Listening thread only has a bufferedreader
		BufferedReader input;

		public ListeningThread(BufferedReader in) {
			input = in;
		}

		public void run() {

			for (;;) {

				String inputString = null;

				try {

					// read any message the server sends
					inputString = input.readLine();

					// if the server is sending multiple lines just print them
					if (inputString.equals(Codes.WHO_ELSE)
							|| inputString.equals(Codes.WHO_LAST_HOUR)
							|| inputString.equals(Codes.RETRIEVE_MESSAGES)) {

						String nextLine = input.readLine();
						while (!nextLine.equals(Codes.EOT)) {
							System.out.println(nextLine);
							nextLine = input.readLine();
						}

					}
					// incoming message to display to the user
					else if (inputString.equals(Codes.MESSAGE)
							|| inputString.equals(Codes.BROADCAST)) {

						String message = input.readLine();
						System.out.println("\n" + message);

					}
					// user is logging out so exit loop
					else if (inputString.equals(Codes.LOGOUT)) {
						System.out.println("Thanks for using the chat room!");
						break;
					}

				} catch (IOException e) {
				} catch (NullPointerException e) {
					// catch nullpointerexception if connection is lost
					sender.interrupt();
					new CleanupThread(null, input, null).start();
					break;
				}

				// at the end of every loop ask for the next command
				System.out.print(Codes.COMMAND_PROMPT);

			}

		}

	}

	private static class SendingThread extends Thread {

		// sending thread just requires a printwriter
		PrintWriter output;

		public SendingThread(PrintWriter out) {
			output = out;
		}

		public void run() {

			Scanner userInput = new Scanner(System.in);
			String userInputString = null;

			for (;;) {

				try {
					userInputString = getNextLine(userInput);
				} catch (IOException e) {
					break;
				}

				if (userInputString == null)
					break;

				// send single line commands
				if (userInputString.equals(Codes.WHO_ELSE)
						|| userInputString.equals(Codes.WHO_LAST_HOUR)
						|| userInputString.equals(Codes.RETRIEVE_MESSAGES)) {

					output.write(userInputString + "\n");
					output.flush();
				}
				// split the message into 3 lines and send them
				else if (userInputString.startsWith(Codes.MESSAGE)) {
					String[] splitMessage = userInputString.split(" ");

					output.write(Codes.MESSAGE + Codes.NL + splitMessage[1]
							+ Codes.NL);

					for (int i = 2; i < splitMessage.length; i++) {
						output.write(splitMessage[i] + " ");
					}
					output.write("\n");
					output.flush();

				}
				// split the broadcast into 2 lines and sent them
				else if (userInputString.startsWith(Codes.BROADCAST)) {
					output.write(Codes.BROADCAST
							+ Codes.NL
							+ userInputString
									.replace(Codes.BROADCAST + " ", "")
							+ Codes.NL);
					output.flush();

				}
				// user wants to end thread, break out of loop
				else if (userInputString.equals(Codes.LOGOUT)) {
					output.write(userInputString + "\n");
					output.flush();
					break;
				}
				// group messageing
				else if (userInputString.startsWith(Codes.GROUP_MESSAGE)) {

					String[] splitMessage = userInputString.split(" ");

					output.write(Codes.GROUP_MESSAGE + Codes.NL
							+ splitMessage[1] + Codes.NL);

					for (int i = 2; i < splitMessage.length; i++) {
						output.write(splitMessage[i] + " ");
					}
					output.write("\n");
					output.flush();

				}
				// any other input that may be implemented later
				else {
					output.write(userInputString + Codes.NL);
					output.flush();
				}

			}

		}

		public String getNextLine(Scanner input) throws IOException {

			while (System.in.available() == 0) {
				if (this.isInterrupted()) {
					return null;
				}
			}
			return input.nextLine();
		}
	}

	public static void main(String[] args) {

		// check that IP address and port number have been provided
		if (args.length < 2) {
			System.out.println("Invalid arguments\nUsage [IPArress] [Port]");
			System.exit(1);
		}
		new Client(args[0], Integer.parseInt(args[1])).start();

	}

}
