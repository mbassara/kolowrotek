package pl.mbassara.kolowrotek;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MyHandler extends Handler {

	protected BufferedWriter out;
	protected Time time;
	protected File file;

	public static MyHandler getHandler(String fileName) {
		return new MyHandler(fileName, true);
	}

	public static MyHandler getHandler(File file) {
		MyHandler result = null;
		try {
			result = new MyHandler(file.getCanonicalPath(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public MyHandler(String fileName, boolean append){
		super();
		time = new Time(System.currentTimeMillis());
		file = new File(fileName);
		try {
			out = new BufferedWriter(
					new FileWriter(fileName, append));
			
			out.write("\r\n\t##############################################" +
						"\r\n\t# LOG SESSION: " + (new Date(System.currentTimeMillis())).toString() + " #" +
						"\r\n\t##############################################\r\n\r\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public File getFile() {
		return file;
	}

	@Override
	public void close() throws SecurityException {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void flush() {
		try {
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void publish(LogRecord record) {
		try {
			out.write("#" + record.getSequenceNumber() + "\t" + record.getMessage() + "\r\n\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}