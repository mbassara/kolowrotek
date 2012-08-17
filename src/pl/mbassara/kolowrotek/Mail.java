package pl.mbassara.kolowrotek;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

public class Mail {  
	  
    private String SMTP_HOST = "smtp.gmail.com";  
    private String FROM_ADDRESS = "kolowrotekgaleria@gmail.com";  
    private String PASSWORD = "mkonjibhubhu";  
    private String FROM_NAME = "Galeria Kolowrotka";  
  
    public boolean sendMail(String[] recipients, String[] bccRecipients, String subject, String message) {  
        try {  
            Properties props = new Properties();  
            props.put("mail.smtp.host", SMTP_HOST);  
            props.put("mail.smtp.auth", "true");  
            props.put("mail.debug", "false");  
            props.put("mail.smtp.ssl.enable", "true");  
  
            Session session = Session.getInstance(props, new SocialAuth());  
            Message msg = new MimeMessage(session);  
  
            InternetAddress from = new InternetAddress(FROM_ADDRESS, FROM_NAME);  
            msg.setFrom(from);  
  
            InternetAddress[] toAddresses = new InternetAddress[recipients.length];  
            for (int i = 0; i < recipients.length; i++) {  
                toAddresses[i] = new InternetAddress(recipients[i]);  
            }  
            msg.setRecipients(Message.RecipientType.TO, toAddresses);  
  
  
            InternetAddress[] bccAddresses = new InternetAddress[bccRecipients.length];  
            for (int j = 0; j < bccRecipients.length; j++) {  
                bccAddresses[j] = new InternetAddress(bccRecipients[j]);  
            }  
            msg.setRecipients(Message.RecipientType.BCC, bccAddresses);  
  
            msg.setSubject(subject);  
            msg.setContent(message, "text/plain");  
            Transport.send(msg);  
            return true;  
        } catch (UnsupportedEncodingException ex) {  
            Logger.getLogger(Mail.class.getName()).log(Level.SEVERE, null, ex);  
            return false;  
  
        } catch (MessagingException ex) {  
            Logger.getLogger(Mail.class.getName()).log(Level.SEVERE, null, ex);  
            return false;  
        }  
    }
  
    public boolean sendMail(String[] recipients, String subject, String messageText, File[] attachments) {  
        try {  
            Properties props = new Properties();  
            props.put("mail.smtp.host", SMTP_HOST);  
            props.put("mail.smtp.auth", "true");  
            props.put("mail.debug", "false");  
            props.put("mail.smtp.ssl.enable", "true");  
  
            Session session = Session.getInstance(props, new SocialAuth());  
            Message message = new MimeMessage(session);  
  
            InternetAddress from = new InternetAddress(FROM_ADDRESS, FROM_NAME);  
            message.setFrom(from);  
  
            InternetAddress[] toAddresses = new InternetAddress[recipients.length];  
            for (int i = 0; i < recipients.length; i++) {  
                toAddresses[i] = new InternetAddress(recipients[i]);  
            }  
            message.setRecipients(Message.RecipientType.TO, toAddresses);  
  
            message.setSubject(subject);  
            
            


            // Create the message part 
            BodyPart messageBodyPart = new MimeBodyPart();

            // Fill the message
            messageBodyPart.setText(messageText);
            
            // Create a multipart message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            for(File file : attachments) {
		        messageBodyPart = new MimeBodyPart();
		        DataSource source = new FileDataSource(file);
		        messageBodyPart.setDataHandler(new DataHandler(source));
		        messageBodyPart.setFileName(file.getName());
		        multipart.addBodyPart(messageBodyPart);
            }

            // Send the complete message parts
            message.setContent(multipart );
            
            Transport.send(message);  
            return true;  
        } catch (UnsupportedEncodingException ex) {  
            Logger.getLogger(Mail.class.getName()).log(Level.SEVERE, null, ex);  
            return false;  
  
        } catch (MessagingException ex) {  
            Logger.getLogger(Mail.class.getName()).log(Level.SEVERE, null, ex);  
            return false;  
        }  
    }
    
    public static boolean sendFilesToMe(String subject, File[] files) {
        String[] recipients = {"mbassara@gmail.com"};  
  
        return new Mail().sendMail(recipients, subject, subject, files);  
    }
    
    public static void main(String[] args) {  
        String[] recipients = new String[]{"mbassara@gmail.com"};  
        String subject = "test";  
        String messageBody = "Test Mail from codesstore.blogspot.com";  
        File[] attachments = {new File("./GUI.log") };
  
        new Mail().sendMail(recipients, subject, messageBody, attachments);  
  
    }  
  
    class SocialAuth extends Authenticator {  
  
        @Override  
        protected PasswordAuthentication getPasswordAuthentication() {  
  
            return new PasswordAuthentication(FROM_ADDRESS, PASSWORD);  
  
        }  
    }
    
}  