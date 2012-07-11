package pl.mbassara.kolowrotek;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

public class MyFTPClient extends FTPClient {
	
	Logger log;
	
	public MyFTPClient(){
		super();
		log = Logger.getLogger(MyFTPClient.class.getName());
	}
	
	public void deleteDirRecursively(String dirPath) throws IOException, FTPException, ParseException {
		
		FTPFile[] list = this.dirDetails(dirPath);
		for(FTPFile file : list){
			if(file.getName().equals(".") || file.getName().equals(".."))
				continue;
			
			if(file.isDir()){
				this.deleteDirRecursively(dirPath + "/" + file.getName());
			}
			else{
				this.delete(dirPath + "/" + file.getName());
			}
		}	
		this.rmdir(dirPath);
	}
}
