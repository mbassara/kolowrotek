package pl.mbassara.kolowrotek;
import java.io.IOException;
import java.util.logging.Logger;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;

public class MyFTPClient extends FTPClient {
	
	Logger log;
	
	public MyFTPClient(){
		super();
		log = Logger.getLogger(MyFTPClient.class.getName());
	}
	
	public void deleteDirRecursively(String dirPath)
	throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException, FTPDataTransferException, FTPAbortedException, FTPListParseException{
		this.changeDirectory(dirPath);
		
		FTPFile[] list = this.list();
		for(FTPFile file : list){
			if(file.getType() == FTPFile.TYPE_DIRECTORY){
				this.deleteDirRecursively(dirPath + "/" + file.getName());
			}
			else{
				this.deleteFile(dirPath + "/" + file.getName());
			}
		}	
		this.deleteDirectory(dirPath);
	}
}
