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
			content += "\n  <image imageURL=\"images/" + i;
			content += ".jpg\"\n\tthumbURL=\"thumbs/" + i;
			content += ".jpg\"\n\tlinkURL=\"images/" + i;
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
	
	private void generatePHP(FTPClient ftp, String dirPath, String partyName, String year) throws FTPException {
		
		String content = PHPbeginning;
		content += partyName;
		content += PHPmiddle;
		content += year + "</a> &#8250; <a href=\"\">" + partyName;
		content += PHPending;
		
		try {
			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp"),
										Charset.forName("UTF-8")));
			out.write(content);
			out.flush();
			out.close();
			
			ftp.put("tmp", dirPath + "/index.php");
			
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
		ftp.get(file.getCanonicalPath(), yearDirName + "/index.php");
		
		String tmp, siteContent = "";
		
		try {
			BufferedReader in = new BufferedReader(
									new InputStreamReader(
										new FileInputStream("tmp"),
										Charset.forName("UTF-8")));
			
			boolean listProcessed = false;
			int listLength = 0;
			while((tmp = in.readLine()) != null) {
				if(!listProcessed && tmp.contains("<li>"))
					listLength++;
				if(!listProcessed && tmp.contains("</ul>"))
					listProcessed = true;
				siteContent += tmp + "\n";
			}
			in.close();
			
			// INPUT DIALOG
			int newPartyPosition = showPartyPositionDialog(listLength);
			
			// INSERTING PARTY
			String finalSiteContent = "";
			BufferedReader siteContentReader = new BufferedReader(
													new StringReader(siteContent));
			
			boolean listSectionReached = false;
			while((tmp = siteContentReader.readLine()) != null) {
				
				if(listSectionReached && newPartyPosition == 0)
					finalSiteContent += "\t\t\t\t<li><a href=\"" + partyDirName
										+ "/\">" + partyName + "</a></li>\n";
				if(listSectionReached)
					newPartyPosition--;
				
				if(tmp.contains("<ul>")) 
					listSectionReached = true;
				
				finalSiteContent += tmp + "\n";
			}
			siteContentReader.close();
				
			// WRITTING UPDATED SITE CONTENT TO PHP FILE
			BufferedWriter out = new BufferedWriter(
									new OutputStreamWriter(
										new FileOutputStream("tmp"),
										Charset.forName("UTF-8")));
			out.write(finalSiteContent);
			out.flush();
			out.close();
			
			ftp.put(file.getCanonicalPath(), yearDirName + "/index.php");
			file.delete();
			
		} catch (FileNotFoundException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		} catch (IOException e) {
			manLogger.log(Level.WARNING, ExceptionsUtilities.printStackTraceToString(e));
		}
	}
	
	private String removePartyFromYearIndexFile(FTPClient ftp, String yearDirName, String partyName) throws FTPException, IOException {
		
		File file = new File("tmp");
		ftp.get(file.getCanonicalPath(), yearDirName + "/index.php");
		
		
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
				if(!tmp.contains("/\">" + partyName + "</a></li>")){
					out.write(tmp + "\n");
				}
				else{
					removedFlag = true;
					resultString = yearDirName + "/" + tmp.substring(tmp.indexOf("\"") + 1, tmp.indexOf("/"));
				}
			}
			
			in.close();
			out.flush();
			out.close();
			
			if(!removedFlag)
				firePropertyChange("error", null, "partyNotFound");
			
			file = new File("tmp2");
			ftp.put(file.getCanonicalPath(), yearDirName + "/index.php");
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
			
			generatePHP(ftp, dirName, partyName, year);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t" + dirName + "/index.php wygenerowany.");
			
			addPartyToYearIndexFile(ftp, dirName, partyName);
			setProgress((int) (currentProgress += 100.0 / taskLength));
			firePropertyChange("progress info", null, "Plik\t" + "/" + dirName.substring(1, 10) + "/index.php uaktualniony.");
			
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
			firePropertyChange("progress info", null, "Plik\t\t" + "/" + dirName.substring(1, 10) + "/index.php uaktualniony.");
			
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
	private static final String PHPbeginning = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\">\n\n<head>\n<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"/>\n<meta name=\"description\" content=\"description\"/>\n<meta name=\"keywords\" content=\"keywords\"/> \n<meta name=\"author\" content=\"author\"/> \n<link rel=\"stylesheet\" type=\"text/css\" href=\"../../default.css\" media=\"screen\"/>\n<title>";
	private static final String PHPmiddle = "</title>\n\t<script type=\"text/javascript\">\n\n\t\tvar _gaq = _gaq || [];\n\t\t_gaq.push(['_setAccount', 'UA-34584156-2']);\n\t\t_gaq.push(['_trackPageview']);\n\n\t\t(function() {\n\t\t\tvar ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;\n\t\t\tga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';\n\t\t\tvar s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);\n\t\t})();\n\n\t</script>\n</head>\n\n<body>\n\n<div 'class=\"outer-container\">\n\n<div class=\"inner-container\">\n\n\t<div class=\"header\">\n\t\t<a href=\"../../\"></a>\n\t</div>\n\n\t<div class=\"path\">\n\t\t\t\n\t\t\t<a href=\"../../\">Strona Główna</a> &#8250; <a href=\"../\">";
	private static final String PHPending = "</a>\n\n\t</div>\n\n\t<div class=\"main-gallery\">\t\t\n\t\t\n\t\t<div class=\"content\">\n\n\t\t\t<!--START SIMPLEVIEWER EMBED -->\n\t\t\t<script type=\"text/javascript\" src=\"../../svcore/js/simpleviewer.js\"></script>\n\t\t\t<script type=\"text/javascript\">\n\t\t\tsimpleviewer.ready(function () {\n\t\t\t\tsimpleviewer.load('sv-container', '100%', '100%', 'transparent', true);\n\t\t\t});\n\t\t\t</script>\n\t\t\t<div id=\"sv-container\"></div>\n\t\t\t<!--END SIMPLEVIEWER EMBED -->\n\n\n\t\t</div>\n\n\t\t<div class=\"navigation\">\n\n\t\t\t<h2>MENU</h2>\n\t\t\t<ul>\n<?php\n\t$file = fopen(\"../../years\", \"r\");\n\twhile (false !== ($line = fgets($file))) {\n\t\techo \"\\t\\t\\t\\t<li><a href=\\\"../../\".$line.\"\\\">\";\n\t\t$tmp = explode(\"-\", $line);\n\t\techo implode(\"/\", $tmp).\"</a></li>\";\n\t}\n?>\n\t\t\t</ul>\n\t\t\t<h2>Licznik odwiedzin</h2>\n\t\t\t<ul>\n\t\t\t\t<li><a href=\"http://www.free-counter.com/\"><img src=\"http://www.free-counter.com/counter.php?b=32562\" border=\"0\" alt=\"Free Counter\"></a></li>\n\t\t\t</ul>\n\n\t\t</div>\n\n\t\t<div class=\"clearer\">&nbsp;</div>\n\n\t</div>\n\n</div>\n\n<div class=\"author\">\n\n\t<?php $file = fopen(\"../../author\", \"r\");\n\t\tif (false !== ($line = fgets($file))) {\n\t\t\techo $line;\n\t\t}\n\t?>\n\t\n</div>\n\n</div>\n\n</body>\n\n</html>\n";
}
