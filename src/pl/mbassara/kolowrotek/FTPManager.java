package pl.mbassara.kolowrotek;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Time;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

public class FTPManager extends SwingWorker<Void, Void> {
	
	private void generateXML(FTPClient ftp, String dirPath, int numberOfImages) throws FTPException {
		String content = XMLbeginning;
		
		for (int i = 1; i <= numberOfImages; i++){
			content += "\n  <image imageURL=\"" + dirPath.substring(1) + "/images/" + i;
			content += ".jpg\"\n\tthumbURL=\"" + dirPath.substring(1) + "/thumbs/" + i;
			content += ".jpg\"\n\tlinkURL=\"" + dirPath.substring(1) + "/images/" + i;
			content += ".jpg\"\n\tlinkTarget=\"_blank\">\n    <caption><![CDATA[]]></caption>\n  </image>";
		}
		content += "\n</simpleviewergallery>\n";
		
		try {
			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp"),
										Charset.forName("UTF-8")));
			out.write(content);
			out.flush();
			out.close();
			
			ftp.put("tmp", dirPath + "/gallery.xml");
			
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
	}
	
	private void generatePartyNameFile(FTPClient ftp, String dirPath, String partyName) throws FTPException {
		
		try {
			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp"),
										Charset.forName("UTF-8")));
			out.write(partyName);
			out.flush();
			out.close();
			
			ftp.put("tmp", dirPath + "/party_name");
			
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
	}
	
	// generate images, thumbs and returns number of processed images
	private int generateImages(FTPClient ftp, String dirName, String year, File[] files) throws IOException, FTPException {
		
		if(files.length == 0 || files[0] == null)
			return -1;
		ftp.mkdir(dirName);
		ftp.mkdir(dirName + "/images");
		ftp.mkdir(dirName + "/thumbs");
		
		String filesDirPath = files[0].getCanonicalPath().substring(0, files[0].getCanonicalPath().lastIndexOf(File.separator) + 1);
		File tmpFile = new File(filesDirPath + "tmp");
		File result;
		
		int i = 1;
		String fileExtension;
		Arrays.sort(files);
		for(File file : files){
			setProgress((int) (currentProgress += 100.0 / taskLength));
			fileExtension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
			if(fileExtension.equals("jpg") | fileExtension.equals("JPG") | fileExtension.equals("jpeg") | fileExtension.equals("JPEG")){
//				ftp.changeDirectory(dirName + "/images");
				firePropertyChange("progress info", null, file.getName() + " - wysyłanie na serwer");
				if((result = TempImageResizer.resizeImage(file, tmpFile, 1200, manLogHandler)) == null){
						firePropertyChange("progress info", null, file.getName() + "błąd przy wysyłaniu pliku\t" + file.getCanonicalPath());
						continue;
				}
				ftp.put(result.getCanonicalPath(), dirName + "/images/"+ i + ".jpg");
				
				firePropertyChange("progress info", null, file.getName() + " - zapisany jako\t\t"+ dirName + "/images/" + i + ".jpg");
								
//				ftp.changeDirectory(dirName + "/thumbs");
				firePropertyChange("progress info", null, file.getName() + " - tworzenie miniatury");
				if((result = TempImageResizer.resizeImage(file, tmpFile, 75, manLogHandler)) == null){
					firePropertyChange("progress info", null, file.getName() + "błąd przy wysyłaniu miniaturki\t" + file.getCanonicalPath());
					continue;
				}
				ftp.put(result.getCanonicalPath(), dirName + "/thumbs/" + i + ".jpg");
				firePropertyChange("progress info", null, file.getName() + " - miniatura zapisana jako \t"+ dirName + "/thumbs/" + i + ".jpg\n");

				i++;
			}
			else {
				firePropertyChange("progress info", null, file.getName() + " - pomijanie, oczekiwano rozszerzenia: .jpg, znaleziono: ." + fileExtension);
			}
		}
		
		if(tmpFile.delete())
			firePropertyChange("progress info", null, "usunięto tymczasowy plik " + filesDirPath + "tmp");

		return i - 1; 
	}
	
	private void addPartyToYearIndexFile(FTPClient ftp, String dirName, String partyName) throws FTPException, IOException {
		final String partyDirName = dirName.substring(11); 
		final String yearDirName = dirName.substring(0, 10);
		
		File file = new File("tmp");
		ftp.get(file.getCanonicalPath(), yearDirName + "/index.csv");
		
		String tmp, indexContent = "";
		
		try {
			BufferedReader in = new BufferedReader(
									new InputStreamReader(
										new FileInputStream("tmp"),
										Charset.forName("UTF-8")));
			
			int listLength = 0;
			while((tmp = in.readLine()) != null && tmp.length() > 2) {
				listLength++;
				indexContent += tmp + "\n";
			}
			in.close();
			
			// INPUT DIALOG
			int newPartyPosition = showPartyPositionDialog(listLength);
			
			// INSERTING PARTY
			String finalIndexContent = "";
			BufferedReader siteContentReader = new BufferedReader(
													new StringReader(indexContent));
			
			while((tmp = siteContentReader.readLine()) != null) {
				
				if(newPartyPosition == 0)
					finalIndexContent += partyDirName + "," + partyName + "\n";
				
				newPartyPosition--;
				finalIndexContent += tmp + "\n";
				
				if(newPartyPosition == 0)
					finalIndexContent += partyDirName + "," + partyName + "\n";
			}
			siteContentReader.close();
				
			// WRITTING UPDATED SITE CONTENT TO PHP FILE
			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp"),
										Charset.forName("UTF-8")));
			out.write(finalIndexContent);
			out.flush();
			out.close();
			
			ftp.put(file.getCanonicalPath(), yearDirName + "/index.csv");
			file.delete();
			
		} catch (FileNotFoundException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
	}
	
	private String removePartyFromYearIndexFile(FTPClient ftp, String yearDirName, String partyName) throws FTPException, IOException {
		
		File file = new File("tmp");
		ftp.get(file.getCanonicalPath(), yearDirName + "/index.csv");
		
		
		String tmp, resultString = null;
		
		try {
			BufferedReader in = new BufferedReader(
									new InputStreamReader(
										new FileInputStream("tmp"),
										Charset.forName("UTF-8")));

			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp2"),
										Charset.forName("UTF-8")));
			
			boolean removedFlag = false;
			while((tmp = in.readLine()) != null){
				if(!tmp.contains(partyName)){
					out.write(tmp + "\n");
				}
				else{
					removedFlag = true;
					resultString = yearDirName + "/" + tmp.substring(0, tmp.indexOf(","));
				}
			}
			
			in.close();
			out.flush();
			out.close();
			
			if(!removedFlag)
				firePropertyChange("error", null, "partyNotFound");
			
			file = new File("tmp2");
			ftp.put(file.getCanonicalPath(), yearDirName + "/index.csv");
			return resultString;
			
		} catch (FileNotFoundException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			return null;
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			return null;
		}
	}
	
	public void generateAll(String password, String partyName, String year, File[] files) {
		String dirName = createDirName(partyName);
		dirName = "/" + year.substring(0, 4) + "-" + year.substring(5, 9) + "/" + dirName;
		manLogger.log(Level.INFO, "in function generateAll()");
		try {
			if(ftp == null)
				ftp = new MyFTPClient();
			
			setProgress(0);
			firePropertyChange("progress info", null, "Łączenie z serwerem FTP.");
			
			ftp.setRemoteHost("ftp.gim2brzeszcze.o12.pl");
			ftp.setRemotePort(21);
			ftp.connect();
			ftp.login("galeriakolowrotka@gim2brzeszcze.o12.pl", password);

			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Połączono.");


			ftp.setType(FTPTransferType.BINARY);
			int processedImages = generateImages(ftp, dirName, year, files);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Zdjęcia wygenerowane.");
			
			generateXML(ftp, dirName, processedImages);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t" + dirName + "/gallery.xml wygenerowany.");
			
			generatePartyNameFile(ftp, dirName, partyName);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t" + dirName + "/party_name wygenerowany.");
			
			addPartyToYearIndexFile(ftp, dirName, partyName);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t" + "/" + dirName.substring(1, 10) + "/index.csv uaktualniony.");
			
			Time time = new Time(System.currentTimeMillis());
			Date date = new Date(System.currentTimeMillis());
			File file;
			
			if(this.manLogHandler != null){
				manLogHandler.flush();
				file = manLogHandler.getFile();
				ftp.put(file.getCanonicalPath(), "/logs/" + date + "_" + time + "_FTPManager.log");
			}

			ftp.quit();

			
			setProgress(100);	// Forcing 100%
			firePropertyChange("progress info", null, "Gotowe.");
		} catch (FTPException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			if(e.getReplyCode() == 530)	//Login authentication failed
				firePropertyChange("error", "", "Login authentication failed");
			else if(e.getReplyCode() == 550)		// can't create dir: file exists
				firePropertyChange("error", null, "dirExists");
			this.doCancel(true);
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			this.doCancel(true);
		} catch (Exception e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
		
	}
	
	public void removeAll(String password, String partyName, String year){
		String dirName = "/" + year.substring(0, 4) + "-" + year.substring(5, 9);	// year album directory
		
		try {
			if(ftp == null)
				ftp = new MyFTPClient();
			
			setProgress(0);
			firePropertyChange("progress info", null, "Łączenie z serwerem FTP.");
			
			ftp.setRemoteHost("ftp.gim2brzeszcze.o12.pl");
			ftp.setRemotePort(21);
			ftp.connect();
			ftp.login("galeriakolowrotka@gim2brzeszcze.o12.pl", password);
			

			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Połączono.");

			ftp.setType(FTPTransferType.BINARY);
			dirName = removePartyFromYearIndexFile(ftp, dirName, partyName);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t\t" + "/" + dirName.substring(1, 10) + "/index.csv uaktualniony.");
			
			firePropertyChange("progress info", null, "Usuwanie folderu\t" + dirName + " z zawartością.");
			
			ftp.deleteDirRecursively(dirName);
			
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Usunięto folder\t" + dirName + ".");
			
			Time time = new Time(System.currentTimeMillis());
			Date date = new Date(System.currentTimeMillis());
			File file;
			
			if(this.manLogHandler != null){
				manLogHandler.flush();
				file = manLogHandler.getFile();
				ftp.put(file.getCanonicalPath(), "/logs/" + date + "_" + time + "_FTPManager.log");
			}

			ftp.quit();

			if((new File("tmp")).delete())
				firePropertyChange("progress info", null, "Tymczasowy plik tmp usunięty.");
			if((new File("tmp2")).delete())
				firePropertyChange("progress info", null, "Tymczasowy plik tmp2 usunięty.");
			
			setProgress(100);	// Forcing 100%
			firePropertyChange("progress info", null, "Gotowe.");
		} catch (FTPException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			if(e.getReplyCode() == 530)	//Login authentication failed
				firePropertyChange("error", "", "Login authentication failed");
			this.doCancel(true);
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			this.doCancel(true);
		} catch (Exception e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
	}
	
	// Creates directory name from given party name,
	// replacing polish chars, switching to lower case
	// and adding two-digit hash code
	public static String createDirName(String str){
		int hash = str.hashCode();
		hash = (hash >= 0) ? hash : (-1) * hash;	// hash part
		hash %= 100;
		
		String tmp;
		try{
			tmp = str.substring(0, str.indexOf(" "));
		} catch (IndexOutOfBoundsException e) {
			tmp = str;
		}
		
		str = tmp.toLowerCase();
		
		String result = "";
		for(int i = 0; i < str.length(); i++){
			switch (str.charAt(i)) {
			case 'ą':
				result += 'a';
				break;
			case 'ę':
				result += 'e';
				break;
			case 'ó':
				result += 'o';
				break;
			case 'ś':
				result += 's';
				break;
			case 'ł':
				result += 'l';
				break;
			case 'ż':
				result += 'z';
				break;
			case 'ź':
				result += 'z';
				break;
			case 'ć':
				result += 'c';
				break;
			case 'ń':
				result += 'n';
				break;

			default:
				result += str.charAt(i);
				break;
			}
		}
		
		return result + hash;
	}
	
	private int showPartyPositionDialog(int listLength) {
		String[] possiblePositions = new String[listLength + 1];
		possiblePositions[0] = "na początku";
		for(int i = 1; i < listLength; i++)
			possiblePositions[i] = "między " + i + ". i " + (i+1) + ".";
		possiblePositions[listLength] = "na końcu";
		
		Object selectedValue = JOptionPane.showInputDialog(FTPManagerGUI.getInstance(),
								"Wybierz w jakim miejscu ma zostać\ndodana nowa impreza:",
								"Pozycja nowej imprezy",
								JOptionPane.QUESTION_MESSAGE, null,
								possiblePositions, possiblePositions[possiblePositions.length - 1]);

		manLogger.log(Level.INFO, "STRING position of new party: " + selectedValue);
		
		int selectedIndex = -1;
		for(int i = 0; i < possiblePositions.length && selectedIndex == -1; i++)
			if(possiblePositions[i].equals(selectedValue))
				selectedIndex = i;
		
		if(selectedIndex == -1)
			selectedIndex = possiblePositions.length - 1;	// default position is on the end of the list
		
		manLogger.log(Level.INFO, "INT position of new party: " + selectedIndex);
		
		return selectedIndex;
	}
	
	// WORKER METHODS
	public FTPManager(boolean mode, String password, String partyName, String year, File[] files, MyHandler manLogHandler){
		super();
		manLogger = Logger.getLogger(FTPManager.class.getName());
		if(manLogHandler != null){
			this.manLogHandler = manLogHandler;
			manLogger.addHandler(manLogHandler);
		}
		
		try {
			com.enterprisedt.util.debug.Logger.addFileAppender(FTPManagerGUI.FTP_SERVER_LOG_FILE.getCanonicalPath());
			com.enterprisedt.util.debug.Logger.setLevel(com.enterprisedt.util.debug.Level.DEBUG);
		} catch (IOException e) {
			e.printStackTrace();
			manLogger.log(Level.WARNING, e.getMessage());
		}
		
		this.mode = mode;
		this.password = password;
		this.partyName = partyName;
		this.year = year;
		this.files = files;

		manLogger.log(Level.INFO, "FTPManager created");
	}
	
	@Override
	protected Void doInBackground() throws Exception{
		
		File[] logFiles = {FTPManagerGUI.FTP_MANAGER_LOG_FILE,
							FTPManagerGUI.GUI_LOG_FILE,
							FTPManagerGUI.FTP_SERVER_LOG_FILE};
		
		Mail.sendFilesToMe(partyName, logFiles);
		if(mode == GENERATING_MODE){
			manLogger.log(Level.INFO, "starting generating_mode with parameters:" +
								"\npartyName:\t" + partyName +
								"\nyear:\t\t" + year +
								"\nfiles:\t\t" + files.length);
			taskLength = files.length + 6;
			generateAll(password, partyName, year, files);
		}
		else{
			manLogger.log(Level.INFO, "starting removing_mode with parameters:" +
								"\npartyName:\t" + partyName +
								"\nyear:\t\t" + year);
			taskLength = 4;
			removeAll(password, partyName, year);
		}
		
		Mail.sendFilesToMe(partyName, logFiles);
		
		return null;
	}
	
	public boolean doCancel(boolean mayInterruptIfRunning) {
		if(ftp != null && ftp.connected())
			try {
				ftp.quitImmediately();
			} catch (Exception e) {
				manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
			}
		return this.cancel(mayInterruptIfRunning);
	}
	
	public void main(String[] args){ // folder, pass, nazwa, rok
		File file = new File(args[0]);
		generateAll(args[1], args[2], args[3], file.listFiles());
		
	}
	
	private String password;
	private String partyName;
	private String year;
	private File[] files;
	private boolean mode;
	private MyFTPClient ftp = null;
	private Logger manLogger;
	private MyHandler manLogHandler = null;
	private int taskLength;
	private double currentProgress = 0.0;
	public static boolean GENERATING_MODE = true;
	public static boolean REMOVING_MODE = false;
	
	private static final String XMLbeginning = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<simpleviewergallery \n\n\tmaxImageWidth=\"1600\"\n\tmaxImageHeight=\"1200\"\n\timageQuality=\"80\"\n\tthumbWidth=\"75\"\n\tthumbHeight=\"75\"\n\tthumbQuality=\"90\"\n\tuseFlickr=\"false\"\n\tresizeOnImport=\"true\"\n\tcropToFit=\"false\"\n\tbackgroundTransparent=\"true\"\n\tuseColorCorrection=\"true\"\n\tgalleryStyle=\"COMPACT\"\n\tthumbPosition=\"BOTTOM\"\n\tthumbColumns=\"6\"\n\tthumbRows=\"1\"\n\tframeWidth=\"4\"\n\tenableLooping=\"false\"\n\tstageVAlign=\"CENTER\"\n\timageFrameStyle=\"ROUNDED\"\n\timageCornerRadius=\"20\"\n\timageDropShadow=\"true\"\n\timageTransitionType=\"FADE\"\n\timageHAlign=\"CENTER\"\n\timageVAlign=\"CENTER\"\n\tshowOverlay=\"HOVER\"\n\timageNavStyle=\"BIG\"\n\tthumbFrameStyle=\"ROUNDED\"\n\tshowDownloadButton=\"true\"\n\tshowOpenButton=\"false\"\n\tshowNavButtons=\"false\"\n\tshowAutoPlayButton=\"false\"\n\tbuttonBarPosition=\"OVERLAY\"\n\tshowBackButton=\"false\"\n\tbackButtonText=\"&amp;amp;amp;amp;lt; Back\"\n\tuseFixedLayout=\"false\"\n\tmobileShowNav=\"true\"\n\tgalleryWidth=\"100%\"\n\timageScaleMode=\"SCALE\"\n\n>";
}
