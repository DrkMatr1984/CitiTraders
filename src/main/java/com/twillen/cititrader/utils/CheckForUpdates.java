package com.twillen.cititrader.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;

import me.tehbeard.cititrader.CitiTrader;
import org.bukkit.plugin.Plugin;

public class CheckForUpdates implements Runnable {

	private Plugin plugin;
	private String yourSlug = "cititraders2";
	private String urlString = "http://dev.bukkit.org/server-mods/" + yourSlug
			+ "/files.rss";
	public String strVersionCheck = "Hasn't checked for an update yet.";

	public CheckForUpdates(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		try {
			/*
			 * code from
			 * http://forums.bukkit.org/threads/update-checker-simple-method
			 * -for-copying-and-pasting.125833/
			 */
			String version = this.plugin.getDescription().getVersion();
			// Remove the [url="http:// part, as that is BBCode's fault
			URL url = new URL(urlString);
			InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(url.openStream());
			} catch (UnknownHostException e) {
				throw e; // Cannot connect
			}
			BufferedReader in = new BufferedReader(isr);
			String line;
			int lineNum = 0;
			while ((line = in.readLine()) != null) {
				if (line.length() != line.replace("<title>", "").length()) {
					// Substring 1 for me, takes off the beginning v on my file
					// name "v1.3.2"
					line = line.replaceAll("<title>", "")
							.replaceAll("</title>", "").replaceAll("	", "")
							.replaceAll("CitiTrader", "")
							.replaceAll("_BETA-", "");
					if (lineNum == 1) {
						Integer newVer = Integer
								.parseInt(line.replace(".", ""));
						Integer oldVer = Integer.parseInt(version.replace(".",
								"").replaceAll("BETA-", ""));
						if (oldVer < newVer) {
							CitiTrader.outdated = true;
							// They are using an old version
							strVersionCheck = "Your CitiTrader is out of date. The newest version of CitiTrader is "
									+ line;
						} else if (oldVer > newVer) {
							// They are using a FUTURE version!
							strVersionCheck = "You are on a development version";
						} else {
							// They are up to date!
							strVersionCheck = "You are on the current version.";
						}
					}
					lineNum = lineNum + 1;
				}
			}
			in.close();
		} catch (IOException e) {
			strVersionCheck = "Was unable to find current release.";
		}

	}

}
