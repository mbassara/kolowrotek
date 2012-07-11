package pl.mbassara.kolowrotek;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.sauronsoftware.ftp4j.FTPCommunicationListener;


public class CommunicationListener implements FTPCommunicationListener {
	
	private Logger logger;
	
	public CommunicationListener(Handler handler) {
		super();
		logger = Logger.getLogger(CommunicationListener.class.getName());
		logger.addHandler(handler);
	}

	@Override
	public void received(String msg) {
		logger.log(Level.INFO, "RCV: " + msg);
	}

	@Override
	public void sent(String msg) {
		logger.log(Level.INFO, "SND: " + msg);
	}

}
