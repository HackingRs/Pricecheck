package com.gmail.hackingrs2.pricecheckauto;

import java.io.IOException;
import java.util.TimerTask;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

public class Listen extends TimerTask {

	@Override
	public void run() {
		String message = "";

		/** Get the latest message. */
		try {
			message = Main.latestMessage();
		} catch (FailingHttpStatusCodeException | IOException e) {
			e.printStackTrace();
		}

		/** Continue if there is a new message. */
		if (!message.equals("null")) {
			System.out.println(message);

			/** Parse the message.. */
			String[] c = message.split(":");
			String s = c[2].trim();
			String ss = "";

			/** Check if the message starts with either "pc" or "pricecheck". */
			if (s.startsWith("pc")) {
				ss = "pc";
			} else if (s.startsWith("pricecheck")) {
				ss = "pricecheck";
			} else {
				return;
			}

			/** Get the item said after "pc" or "pricecheck". */
			String[] cc = s.split(ss);
			String end = cc[1]; // the item name

			String itemName = "";

			/** Check if database contains the item. */
			for (Object item : Main.getDatabase().getItems()) {
				if (end.contains(item.toString())) {
					itemName = item.toString();
				}
			}

			/** If item exists, say the price in the chat box. */
			if (!itemName.isEmpty()) {
				try {
					Main.post(Main.getDatabase().find(itemName).replaceAll("\\[", "").replaceAll("\\]", ""));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}