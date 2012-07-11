package pl.mbassara.kolowrotek;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;


public class TransferListener implements FTPDataTransferListener {
	
	public static String UNCHANGED = "unchanged";
	public static String ABORTED = "aborted";
	public static String COMPLETED = "completed";
	public static String FAILED = "failed";
	public static String STARTED = "started";
	
	private String state;
	private int transferred;
	
	public TransferListener() {
		state = UNCHANGED;
		transferred = 0;
	}
	
	public void reset() {
		state = UNCHANGED;
		transferred = 0;
	}
	
	public String getState() {
		return state;
	}
	
	public int getTransferred() {
		return transferred;
	}

	@Override
	public void aborted() {
		state = ABORTED;

	}

	@Override
	public void completed() {
		state = COMPLETED;

	}

	@Override
	public void failed() {
		state = FAILED;

	}

	@Override
	public void started() {
		state = STARTED;

	}

	@Override
	public void transferred(int size) {
		transferred += size;
	}

}
