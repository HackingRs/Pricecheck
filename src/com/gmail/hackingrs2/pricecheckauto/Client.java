package com.gmail.hackingrs2.pricecheckauto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gmail.hackingrs2.pricecheck.Database;

public class Client {

	private static Client instance;

	private JFrame frame;
	private JPanel contentPane;

	private JTextPane history;
	private StyledDocument doc;
	private Style error;
	private Style normal;

	private JTextField textField;
	private JMenuBar menuBar;
	private JMenu file;
	private JMenuItem logout;
	private JMenuItem exit;

	private Timer timer;
	private boolean running = false;
	private static WebClient webClient;
	private static long sessionId = 0;

	public static Set<String> latestAnswered = new HashSet<>();

	public Client(WebClient webClient, Database database, long sessionId2) {
		instance = this;

		Client.webClient = webClient;
		Client.sessionId = sessionId2;
		timer = new Timer();

		initialize();
	}

	private void initialize() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setTitle("Client");
		frame.setSize(600, 350);
		frame.setLocationRelativeTo(null);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		frame.setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);

		file = new JMenu("File");
		menuBar.add(file);

		logout = new JMenuItem("Logout");
		logout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				logout();
			}
		});

		file.add(logout);

		exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		file.add(exit);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		tabbedPane.addTab("Client", null, panel, null);
		panel.setLayout(null);

		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Console", null, panel_1, null);
		panel_1.setLayout(null);

		history = new JTextPane();
		history.setBounds(10, 11, 559, 220);
		panel_1.add(history);

		JScrollPane scrollPane = new JScrollPane(history);
		scrollPane.setBounds(history.getBounds());
		panel_1.add(scrollPane);

		doc = history.getStyledDocument();
		error = history.addStyle("error", null);
		StyleConstants.setForeground(error, Color.red);

		normal = history.addStyle("normal", null);
		StyleConstants.setForeground(error, Color.black);

		textField = new JTextField();
		textField.setBounds(10, 237, 483, 20);
		panel_1.add(textField);

		JButton btnSend = new JButton("Send");
		btnSend.setBounds(499, 236, 70, 23);
		panel_1.add(btnSend);

		JPanel panel_2 = new JPanel();
		tabbedPane.addTab("Options", null, panel_2, null);
		panel.setLayout(null);

		btnSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (textField.getText().startsWith("--")) {
					processInput(textField.getText());
					return;
				}

				send(textField.getText(), false);
			}
		});

		textField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (textField.getText().startsWith("--")) {
						processInput(textField.getText());
						return;
					}

					send(textField.getText(), false);
				}
			}
		});
	}

	private void processInput(String next) {
		String[] commandLine = next.split("--");
		String command = commandLine[1];
		String[] args2 = command.split(" ");

		if (command.equalsIgnoreCase("start")) {
			if (!running) {
				timer.scheduleAtFixedRate(new Listen(), 0, 1000);
				running = true;
				send("Task started.", false);
			} else {
				send("Task is already running!", true);
			}

		} else if (command.equalsIgnoreCase("end")) {
			if (running) {
				timer.cancel();
				send("Task stopped.", false);
			} else {
				send("Task is not running!", true);
			}

		} else if (command.equalsIgnoreCase("exit")) {
			if (running) {
				timer.cancel();
			}

			System.exit(0);
		}
	}

	public Set<String> latestMessage() throws FailingHttpStatusCodeException, IOException {
		final HtmlPage page = webClient.getPage("https://network.ultimatescape2.com/cpsess" + sessionId + "/chat-full");
		String data = page.asText();

		webClient.closeAllWindows();

		String[] lines = data.split(System.getProperty("line.separator"));
		Set<String> current = new HashSet<>(3);

		for (int i = 2; i < 5; i++) {
			if (lines.length < i)
				return current;

			current.add(lines[i]);
		}

		return current;
	}

	public void post(String message) throws IOException {
		final HtmlPage page = webClient.getPage("https://network.ultimatescape2.com/cpsess" + sessionId + "/chat-full");
		final HtmlForm form = page.getForms().get(0);
		final HtmlSubmitInput button = form.getInputByValue("Send");
		final HtmlTextInput textField = form.getInputByName("message");

		textField.setValueAttribute(message);
		button.click();

		webClient.closeAllWindows();
	}

	public void send(String message, boolean error) {
		try {
			doc.insertString(doc.getLength(), message + "\n", error ? this.error : normal);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		try {
			post(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void logout() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		frame.dispose();
	}

	public static Client getClient() {
		return instance;
	}
}