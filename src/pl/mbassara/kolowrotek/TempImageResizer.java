package pl.mbassara.kolowrotek;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class TempImageResizer {
	
	private static boolean wasHandlerAdded = false;
	private static Logger log = Logger.getLogger(TempImageResizer.class.getName());
 
    public static File resizeImage(File inFile, File outFile, int height, Handler logToFileHandler){
		try {
			if(logToFileHandler != null && !wasHandlerAdded){
				log.addHandler(logToFileHandler);
				wasHandlerAdded = true;
			}
			
			BufferedImage originalImage = ImageIO.read(inFile);
			log.log(Level.INFO, "resizing file: " + inFile.getName());
			int width = originalImage.getWidth() * height / originalImage.getHeight();
			int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
			
			BufferedImage resizedImage = new BufferedImage(width, height, type);
			Graphics2D g = resizedImage.createGraphics();
			boolean drawResult = g.drawImage(originalImage, 0, 0, width, height, null);
			log.log(Level.INFO, "Graphics2D.drawImage() result: " + drawResult);
			
			FileOutputStream fileOutputStream = new FileOutputStream(outFile);
			resizedImage.flush();
		 
//			boolean writeResult = ImageIO.write(resizedImage, "jpg", outFile); 
			boolean writeResult = ImageIO.write(resizedImage, "jpg", fileOutputStream);
			fileOutputStream.flush();
			log.log(Level.INFO, "ImageIO.write() result: " + drawResult);
			g.dispose();
			log.log(Level.INFO, "file: " + inFile.getName() + " has" + (drawResult && writeResult ? " " : "n't ") + "been successfully resized to " + outFile.length() + " bytes");
			log.log(Level.INFO, "md5sum: " + md5sum(outFile));
			return outFile;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static String md5sum(File file) {
    	try {
	        InputStream fis =  new FileInputStream(file);
	
	        byte[] buffer = new byte[1024];
	        MessageDigest complete = MessageDigest.getInstance("MD5");
	        int numRead;
	
	        do {
	            numRead = fis.read(buffer);
	            if (numRead > 0) {
	                complete.update(buffer, 0, numRead);
	            }
	        } while (numRead != -1);
	
	        fis.close();
	        
	        return (new HexBinaryAdapter()).marshal(complete.digest()).toLowerCase();
    	} catch (Exception e) {
			return null;
		}
    }
 
	public static void main(String [] args){
		if(args.length != 2){
			System.err.println("Too few arguments. Try:\njava TempImageResizer originalImage height");
			System.exit(1);
		}
		resizeImage(new File(args[0]), new File("tmp.jpg"), Integer.valueOf(args[1]), null); 
    }

}
