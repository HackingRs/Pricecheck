package com.gmail.hackingrs2.pricecheckauto;

import java.io.IOException;
import java.util.Scanner;
import java.util.Timer;
import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gmail.hackingrs2.pricecheck.Database;

public class Main {

	/** Variables */
	private static boolean loggedIn = false;

	private static Timer timer;
	private static boolean running = false;

	private static WebClient webClient;
	private static long sessionId = 0;

	private static String latestMessage = "";

	/** Database class from external jar. Contains item information. */
	private static Database database;

	/**
	 * Main method. First thing that is called when the application is launched.
	 */
	public static void main(String[] args) {
		/** Init WebClient and edit its options. */
		webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setCssEnabled(true);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.getCookieManager().setCookiesEnabled(true);

		/** Enable cookies, so we can hold the website sessions. */
		CookieManager cookieMan = new CookieManager();
		cookieMan = webClient.getCookieManager();
		cookieMan.setCookiesEnabled(true);

		/** Disable HTMLUnit parser spam. */
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
		new Shutit(webClient);

		/** Init Database. */
		database = new Database();
		database.init();

		// System.out.println("Please wait while the client is logging in...");

		/** Log in to the website. */
		/*
		 * try { login(); } catch (FailingHttpStatusCodeException | IOException
		 * e) { e.printStackTrace(); }
		 */

		/** Init Timer. */
		timer = new Timer();

		/** Init Scanner, used for user input. */
		@SuppressWarnings("resource")
		final Scanner scanner = new Scanner(System.in);

		/** Read user input */
		while (true) {
			/** Get the line written in console. */
			String next = scanner.nextLine();

			if (next.startsWith("--")) {
				/** Create a command system. */
				String[] commandLine = next.split("--");
				String command = commandLine[1];
				String[] args2 = command.split(" ");

				if (command.equalsIgnoreCase("start")) {
					if (!running) {
						if (!loggedIn) {
							/** Start the repeating task that reads the website. */
							timer.scheduleAtFixedRate(new Listen(), 0, 1000);
							System.out.println("Task started.");
						} else {
							System.err.println("You must first log in to do this! Type --login <name> <password>");
						}
					} else {
						System.err.println("Task is already running!");
					}

				} else if (command.equalsIgnoreCase("end")) {
					if (running) {
						/** Stop the repeating task. */
						timer.cancel();
						System.out.println("Task stopped.");
					} else {
						System.err.println("Task is not running!");
					}

				} else if (command.startsWith("login")) {
					if (args2.length == 3) {
						String username = args2[1];
						String password = args2[2];

						try {
							System.out.println("Logging in...");
							login(username, password);
						} catch (FailingHttpStatusCodeException | IOException e) {
							e.printStackTrace();
						}
					}

				} else if (command.startsWith("post")) {
					/**
					 * Used for testing purposes. Manually send a string to the
					 * website.
					 */

					/** Build the message. */
					StringBuilder builder = new StringBuilder();

					for (int i = 1; i < args2.length; i++) {
						builder.append(args2[i]).append(" ");
					}

					/** Post the built message. */
					try {
						post(builder.toString());
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else if (command.equalsIgnoreCase("exit")) {
					/** Exit the program */
					if (running) {
						timer.cancel();
					}

					System.exit(0);
				}
			}
		}
	}

	/** Log in to the website. */
	private static void login(String username, String password) throws FailingHttpStatusCodeException, IOException {
		/** Connect to the website. */
		final HtmlPage page1 = webClient.getPage("https://network.ultimatescape2.com/");

		/** Get the login form. */
		final HtmlForm form = page1.getForms().get(0);

		/** The login button. */
		final HtmlSubmitInput button = form.getInputByValue("Login");

		/** Username and password input fields. */
		final HtmlTextInput textField = form.getInputByName("username");
		final HtmlPasswordInput textField1 = form.getInputByName("password");

		/** Set the field values. */
		textField.setValueAttribute(username); // username
		textField1.setValueAttribute(password); // password

		/** Click the button and get the opened page. */
		HtmlPage page2 = button.click();

		/** Close the window. */
		webClient.closeAllWindows();

		/** Get the session id from the website url. */
		String url = page2.getUrl().toString();
		sessionId = Long.valueOf(url.split("cpsess")[1].split("/home")[0]);

		System.out.println("Succesfully logged in as " + textField.asText() + "!");
	}

	/** Get the latest message in the chat box. */
	public static String latestMessage() throws FailingHttpStatusCodeException, IOException {
		/** Connect to the website using our session id. */
		final HtmlPage page = webClient.getPage("https://network.ultimatescape2.com/cpsess" + sessionId + "/chat-full");

		/** Get the source code of the page. */
		String data = page.asText();

		/** Close the window. */
		webClient.closeAllWindows();

		/** Get the first message in the chat box. */
		String[] lines = data.split(System.getProperty("line.separator"));
		String message = lines[2]; // first message

		if (!latestMessage.equals(message)) {
			latestMessage = message;
			return latestMessage;
		}

		return "null";
	}

	/** Post a message in the chat box. */
	public static void post(String message) throws IOException {
		/** Connect to the website using our session id. */
		final HtmlPage page = webClient.getPage("https://network.ultimatescape2.com/cpsess" + sessionId + "/chat-full");

		/** Get the input form. */
		final HtmlForm form = page.getForms().get(0);

		/** The send button. */
		final HtmlSubmitInput button = form.getInputByValue("Send");

		/** The message field. */
		final HtmlTextInput textField = form.getInputByName("message");

		/** Set the field value. */
		textField.setValueAttribute(message);

		/** Click the button. */
		button.click();

		/** Close the window. */
		webClient.closeAllWindows();
	}

	/** Static method to get the database. */
	public static Database getDatabase() {
		return database;
	}
}
