package eu.fbk.soa.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {

	private Properties properties;
	
	private static ConfigUtils instance;

	public ConfigUtils() throws IOException {
		properties = new Properties();
		InputStream inStream = new FileInputStream(new File("config.properties"));
		properties.load(inStream);
	}
	
	private String getValue(String key) {
		return properties.getProperty(key);
	}

	public static String getProperty(String key) throws IOException {
		if (instance == null) { 
			instance = new ConfigUtils();
		}
		return instance.getValue(key);
	}
}

