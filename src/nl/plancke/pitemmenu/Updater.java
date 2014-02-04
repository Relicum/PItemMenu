package nl.plancke.pitemmenu;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import static nl.plancke.pitemmenu.PItemMenu.*;

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
	
	public static boolean hasUpdate() {
		try {
			if(config.getBoolean("check-update", true)) {
				jsonFeed = getContent("https://api.curseforge.com/servermods/files?projectIds=" + projectId);
				JSONArray array = (JSONArray) JSONValue.parse(jsonFeed);
				JSONObject latestFile = (JSONObject) JSONValue.parse(array.get(array.size() -1).toString());
				if(!latestFile.get("name").toString().replaceAll("[a-zA-Z ]", "").equals(version)) { return true; }
			}
			return false;
		} catch (Exception e) { 
			e.printStackTrace(); 
			return false;
		}
	}
	
	public static ArrayList<String> getInfo() {
		try {
			ArrayList<String> info = new ArrayList<String>();
			jsonFeed = getContent("https://api.curseforge.com/servermods/files?projectIds=" + projectId);
			JSONArray array = (JSONArray) JSONValue.parse(jsonFeed);
			JSONObject latestFile = (JSONObject) JSONValue.parse(array.get(array.size() -1).toString());
			
			info.add("New File found: " + latestFile.get("name").toString());
			info.add(latestFile.get("downloadUrl").toString());
			info.add("You can also check the BukkitDev Page:");
			info.add("http://dev.bukkit.org/bukkit-plugins/pitemmenu/");
			return info;
		} catch (Exception e) { 
			e.printStackTrace(); 
		}
		return new ArrayList<String>();
	}
}