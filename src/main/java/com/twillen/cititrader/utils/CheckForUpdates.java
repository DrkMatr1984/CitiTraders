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

	public void run() {
		try {
			/*
			 * code from
			 * http://forums.bukkit.org/threads/update-checker-simple-method
			 * -for-copying-and-pasting.125833/
			 */
			String version = this.plugin.getDescription().getVersion().replaceAll("[^\\d.]", "");
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
					// line like CitiTrader_BETA-2.0.4  will become 204
					line = line.replaceAll("[^\\d.]", "");
					if (lineNum == 1) {
						try {
							int newVer = Integer.parseInt(line.replace(".",""));
							int oldVer = Integer.parseInt(version.replace(".",""));
							if(oldVer == newVer){
								strVersionCheck = "You are on the current version.";
							}
							else if (oldVer < newVer) {
								CitiTrader.outdated = true;
								// They are using an old version
								strVersionCheck = "Your CitiTrader is out of date. The newest version of CitiTrader is "
										+ line;
							} 
							else{
								// They are using a FUTURE version!
								strVersionCheck = "You are on a development version";
							}
						} catch (Exception e) {
							strVersionCheck = "Twillen mislabeled the new file again...";
						}
					}
					lineNum = lineNum + 1;
				}
			}
			in.close();
		} catch (IOException e) {
			strVersionCheck = "Unable to find current release.";
		}

	}

}
