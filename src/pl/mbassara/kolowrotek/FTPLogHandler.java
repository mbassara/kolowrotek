package pl.mbassara.kolowrotek;
import java.io.IOException;
import java.util.logging.LogRecord;

public class FTPLogHandler extends MyHandler{

	public FTPLogHandler(String fileName, boolean append) {
		super(fileName, append);
	}

	@Override
	public void publish(LogRecord record) {
		try {
			if(record.getMessage().contains("SND: PASS"))
				record.setMessage("SND: PASS ******");
			
			time.setTime(record.getMillis());
			out.write("\r\nTIME:\t\t" + time.toString() +
						"\r\n#" + record.getSequenceNumber() + "\t" + record.getMessage() + "\r\n\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
