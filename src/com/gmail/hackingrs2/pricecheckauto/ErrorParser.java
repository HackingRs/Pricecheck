package com.gmail.hackingrs2.pricecheckauto;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ErrorParser {

	public static String findResponsible(String errorLine) {
		String[] lines = errorLine.split(System.getProperty("line.separator"));
		String class_ = "";
		int lineNum = 0;

		String lineStart = "at com.gmail.hackingrs2.pricecheckauto.";
		for (String line : lines) {
			if (line.trim().startsWith(lineStart)) {
				String[] _class = line.split(lineStart);
				class_ = _class[1].split("\\.")[0];
				break;
			}

			lineNum++;
		}

		if (!class_.isEmpty()) {
			int line = Integer.valueOf(lines[lineNum].split(class_ + "\\.java\\:")[1].split("\\)")[0]);

			Document doc = null;

			try {
				doc = Jsoup.connect("https://github.com/HackingRs/Pricecheck/blob/master/src/com/gmail/hackingrs2/pricecheckauto/" + class_ + ".java").get();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Element el = doc.getElementById("LC" + line);
			String text = el.text();

			if (!text.trim().isEmpty()) {
				return text.trim();
			}
		}

		return "null";
	}
}
