package com.gmail.hackingrs2.pricecheckauto;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

public class Listen extends TimerTask {

	@Override
	public void run() {
		Set<String> messages = new HashSet<>(3);

		try {
			messages = Client.getClient().latestMessage();
		} catch (FailingHttpStatusCodeException | IOException e) {
			Client.getClient().sendError(e);
		}

		for (String message : messages) {
			if (!Client.latestAnswered.contains(message)) {
				parseAndAnswer(message);
			}
		}
	}

	private void parseAndAnswer(String message) {
		Client.getClient().send(message, false, false);

		if (Client.latestAnswered.size() >= 10) {
			Client.latestAnswered.clear();
		}

		Client.latestAnswered.add(message);

		String[] c = message.split(":");
		String s = c[2].trim();
		String ss = "";

		if (s.startsWith("pc on")) {
			ss = "pc on";
		} else if (s.startsWith("pc")) {
			ss = "pc";
		} else if (s.startsWith("price check on")) {
			ss = "price check on";
		} else if (s.startsWith("pricecheck")) {
			ss = "pricecheck";
		} else if (s.startsWith("price check")) {
			ss = "price check";
		} else if (s.startsWith("how much is")) {
			ss = "how much is";
		} else {
			return;
		}

		String[] cc = s.split(ss);
		String end = cc[1].replaceAll("\\?", "").replaceAll("\\!", "").trim();

		String itemName = "";

		for (Object item : Login.getDatabase().getItems()) {
			if (end.equals(item.toString())) {
				itemName = item.toString();
			}
		}

		if (!itemName.isEmpty()) {
			System.out.println(Login.getDatabase().find(itemName).replaceAll("\\[", "").replaceAll("\\]", ""));
			/*
			 * try { Client.getClient().post(Login.getDatabase().find(itemName).replaceAll("\\[", "").replaceAll("\\]", "")); } catch (IOException e) { e.printStackTrace(); }
			 */
		}
	}
}
