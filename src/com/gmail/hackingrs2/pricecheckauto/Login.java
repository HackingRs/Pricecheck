package com.gmail.hackingrs2.pricecheckauto;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gmail.hackingrs2.pricecheck.Database;

public class Login extends JFrame {

	private static final long serialVersionUID = 1L;

	private static WebClient webClient;
	private static long sessionId = 0;
	private static Database database;

	private JPanel contentPane;

	private static JLabel lblResult;

	private JTextField txtName;
	private JLabel lblPassword;
	private JPasswordField txtPassword;

	public Login() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		webClient = new WebClient(BrowserVersion.CHROME);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setRedirectEnabled(true);
		webClient.getOptions().setCssEnabled(true);
		webClient.getOptions().setUseInsecureSSL(true);
		webClient.getCookieManager().setCookiesEnabled(true);

		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
		java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
		new Shutit(webClient);

		database = new Database();
		database.init();

		setResizable(false);
		setTitle("Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 215);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		txtName = new JTextField();
		txtName.setBounds(65, 27, 165, 28);
		contentPane.add(txtName);
		txtName.setColumns(10);

		txtPassword = new JPasswordField();
		txtPassword.setColumns(10);
		txtPassword.setBounds(65, 92, 165, 28);
		contentPane.add(txtPassword);

		JLabel username = new JLabel("Username:");
		username.setBounds(119, 11, 52, 16);
		contentPane.add(username);

		lblPassword = new JLabel("Password:");
		lblPassword.setBounds(119, 76, 52, 16);
		contentPane.add(lblPassword);

		lblResult = new JLabel("", SwingConstants.CENTER);
		lblResult.setBounds(65, 170, 165, 14);
		lblResult.setVisible(false);
		contentPane.add(lblResult);
		
		JButton btnLogin = new JButton("Login");
		btnLogin.setBounds(10, 130, 274, 29);
		contentPane.add(btnLogin);
		
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String username = txtName.getText();
				char[] c = txtPassword.getPassword();
				String password = new String(c);

				lblResult.setVisible(true);
				lblResult.setForeground(Color.BLACK);

				try {
					login(username, password);
				} catch (FailingHttpStatusCodeException | IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		txtPassword.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					String username = txtName.getText();
					char[] c = txtPassword.getPassword();
					String password = new String(c);
					
					try {
						login(username, password);
					} catch (FailingHttpStatusCodeException | IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
	}

	private void login(String username, String password) throws FailingHttpStatusCodeException, IOException {
		final HtmlPage page1 = webClient.getPage("https://network.ultimatescape2.com/");
		final HtmlForm form = page1.getForms().get(0);
		final HtmlSubmitInput button = form.getInputByValue("Login");
		final HtmlTextInput textField = form.getInputByName("username");
		final HtmlPasswordInput textField1 = form.getInputByName("password");

		textField.setValueAttribute(username); // username
		textField1.setValueAttribute(password); // password

		HtmlPage page2 = button.click();
		String url = page2.getUrl().toString();

		if (!url.endsWith("home")) {
			lblResult.setText("Invalid username or password.");
			lblResult.setForeground(Color.RED);
			return;
		}

		webClient.closeAllWindows();
		sessionId = Long.valueOf(url.split("cpsess")[1].split("/home")[0]);

		new Client(webClient, database, sessionId);
		dispose();
	}
	
	public static void main(String[] args) {
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
	}

	public static Database getDatabase() {
		return database;
	}
}