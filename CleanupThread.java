import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class CleanupThread extends Thread {

	Socket socket;
	BufferedReader input;
	PrintWriter output;

	public CleanupThread(Socket sock, BufferedReader reader, PrintWriter writer) {
		socket = sock;
		input = reader;
		output = writer;
	}

	public void run() {

		// close all IO when the JVM is shutting down
		try {
			if (socket != null)
				socket.close();

			if (input != null) {
				input.close();
			}

			if (output != null) {
				output.write(Codes.LOGOUT + Codes.NL);
				output.flush();
				output.close();
			}

		} catch (IOException e) {
			System.err.println("Error closing resource during cleanup");
		}

	}

}
