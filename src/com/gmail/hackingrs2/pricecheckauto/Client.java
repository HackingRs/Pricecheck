package com.gmail.hackingrs2.pricecheckauto;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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

	private JMenu task;
	private JMenuItem start;
	private JMenuItem stop;

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
			sendError(e1);
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

		task = new JMenu("Task");
		menuBar.add(task);

		start = new JMenuItem("Start");
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!running) {
					timer.scheduleAtFixedRate(new Listen(), 0, 1000);
					running = true;
					send("Task started.", false, false);
				} else {
					send("Task is already running!", false, true);
				}
			}
		});

		task.add(start);

		stop = new JMenuItem("Stop");
		stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (running) {
					timer.cancel();
					send("Task stopped.", false, false);
				} else {
					send("Task is not running!", false, true);
				}
			}
		});

		task.add(stop);

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
		scrollPane.setAutoscrolls(true);
		panel_1.add(scrollPane);

		doc = history.getStyledDocument();
		error = history.addStyle("error", null);
		StyleConstants.setForeground(error, Color.red);

		normal = history.addStyle("normal", null);
		StyleConstants.setForeground(normal, Color.black);

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
				send(textField.getText(), true, false);
				textField.setText(null);
			}
		});

		textField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					send(textField.getText(), true, false);
					textField.setText(null);
				}
			}
		});
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

	public void send(String message, boolean chat, boolean error) {	
		try {
			doc.insertString(doc.getLength(), message + "\n", error ? this.error : this.normal);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		if (chat) {
			try {
				post(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendError(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);

		send("A fatal error has occured.\n", false, true);
		
		send("----- ERROR -----", false, true);
		send(sw.toString(), false, true);
		
		send("----- LINE -----", false, true);
		send(ErrorParser.findResponsible(sw.toString()), false, true);
	}

	private void logout() {
		if(running) {
			send("Please stop the task first.", false, true);
			return;
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login();
					frame.setVisible(true);
				} catch (Exception e) {
					sendError(e);
				}
			}
		});

		frame.dispose();
	}

	public static Client getClient() {
		return instance;
	}
}
