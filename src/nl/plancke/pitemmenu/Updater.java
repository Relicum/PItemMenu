package nl.plancke.pitemmenu;

import java.io.*;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import static nl.plancke.pitemmenu.PItemMenu.*;
import static nl.plancke.pitemmenu.Functions.*;

public class Updater {
	public static int projectId = 63386;
	public static String jsonFeed;

	public static String getContent(String page) {
		String content = "";
		String curLine;
		try {
			URL url = new URL(page);
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			while ( (curLine = br.readLine()) != null)
				content  += curLine;

			br.close();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content;  
	}

	public static void checkUpdate() {
		try {
			jsonFeed = getContent("https://api.curseforge.com/servermods/files?projectIds=" + projectId);
			JSONArray array = (JSONArray) JSONValue.parse(jsonFeed);
			JSONObject latestFile = (JSONObject) JSONValue.parse(array.get(array.size() -1).toString());
			if(!latestFile.get("name").toString().replaceAll("[a-zA-Z ]", "").equals(version)) {
				consoleTagMessage("New file found: " + latestFile.get("name").toString());
				consoleTagMessage("Check BukkitDev for the update!");
			}
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
	}
}