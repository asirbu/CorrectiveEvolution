package eu.fbk.soa.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;

public class StreamGobbler extends Thread {

	private InputStream is;
	private String type;
	private OutputStream os;
	private String output = "";

	private int debugLevel = 0;
	
	public StreamGobbler(InputStream is, String type) {
		this.is = is;
		this.type = type;
	}
	
	public StreamGobbler(InputStream is, String type, OutputStream redirect) {
		this.is = is;
		this.type = type;
		this.os = redirect;
	}
	
	public StreamGobbler(InputStream is, String type, PipedOutputStream redirect) {
		this.is = is;
		this.type = type;
		this.os = redirect;
	}
	

	public void run() {
		try {
			PrintWriter pw = null;
			if (os != null)
				pw = new PrintWriter(os);

			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (pw != null) {
					pw.println(line);
					pw.flush();
				} else {
					
					if (debugLevel == 1) {
						System.out.println("> " + line);
					}
					
				}
			}
			if (pw != null)
				pw.flush();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
