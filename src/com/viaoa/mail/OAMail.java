/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.mail;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;

import com.viaoa.lang.OAString;

/**
 * Utility for sending email through an SMTP or SMTPS server using the JavaMail
 * API. Supports HTML or plain-text message bodies, file attachments, and
 * in-memory attachments supplied as byte arrays. The class maintains basic SMTP
 * connection information (host, port, user ID, password) and provides optional
 * debug and SSL flags. <p>
 *
 * Message construction uses {@link MimeMessage} with a {@link MimeMultipart}
 * body to support attachments. Recipients are supplied as arrays of addresses,
 * and empty or null entries are ignored. The {@code sendSmtp} methods ultimately
 * authenticate and transmit the message using {@link Transport}. <p>
 *
 * The class does not configure advanced JavaMail properties such as timeouts,
 * TLS, or authentication handlers, and callers that need fine-grained control
 * should supply the appropriate JavaMail properties. The class is
 * per-instance-stateful but thread-safe as long as each instance is confined to
 * a single thread.
 */
public class OAMail implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * SMTP server host name.
     */
    private String host;
    
    /**
     * SMTP server port.
     */
    private int port;
    
    /**
     * User ID for authenticating with the mail server.
     */
    private String userId;
    
    /**
     * Password for authenticating with the mail server.
     */
    private String password;
    
    /**
     * Flag indicating whether JavaMail debug output is enabled.
     */
    private boolean bDebug;
    
    /**
     * Flag indicating whether SSL should be used for SMTP connections.
     */
    private boolean bUseSSL;

    
    /**
     * Container for an in-memory email attachment.
     */
    public static class OAMailAttachment {
    	/**
    	 * Raw attachment data.
    	 */
        public byte[] bsData;

        /**
         * MIME type of the attachment.
         */
        public String mimeType;
        
        /**
         * File name of the attachment.
         */
        public String fileName;
    }
    

    /**
     * Creates a mail sender using the default SMTP port.
     *
     * @param host the SMTP server host
     * @param user the user ID for authentication
     * @param pw the password for authentication
     */
    public OAMail(String host, String user, String pw) {
        this.host = host;
        this.port = 25;
        this.userId = user;
        this.password = pw;
    }

    /**
     * Creates a mail sender using the specified SMTP port.
     *
     * @param host the SMTP server host
     * @param port the SMTP server port
     * @param user the user ID for authentication
     * @param pw the password for authentication
     */
    public OAMail(String host, int port, String user, String pw) {
        this.host = host;
        this.port = port;
        this.userId = user;
        this.password = pw;
    }

    /**
     * Enables or disables SSL for SMTP connections.
     *
     * @param b true to enable SSL, false otherwise
     */
    public void setUseSSL(boolean b) {
        bUseSSL = b;
    }

    /**
     * Returns whether SSL is enabled for SMTP connections.
     *
     * @return true if SSL is enabled
     */
    public boolean getUsesSSL() {
        return bUseSSL;
    }
    
    /**
     * Enables or disables JavaMail debug output.
     *
     * @param b true to enable debug output
     */
    public void setDebug(boolean b) {
        this.bDebug = b;

    }

    /**
     * Sends an email with optional file attachments using single recipient strings.
     *
     * @param to the TO recipient
     * @param cc the CC recipient
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param fileNames file paths to attach
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String to, String cc, String from, 
            String subject, String text,
            String contentType,
            String[] fileNames) throws Exception 
    {
        sendSmtp(new String[] {to}, new String[] {cc}, from, subject, text, contentType, fileNames);
    }    

    /**
     * Sends an email with a single in-memory attachment using single recipient strings.
     *
     * @param to the TO recipient
     * @param cc the CC recipient
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param bsAttachment attachment data
     * @param mimeTypeBs attachment MIME type
     * @param bsFileName attachment file name
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String to, String cc, String from, 
            String subject, String text,
            String contentType,
            byte[] bsAttachment, String mimeTypeBs, String bsFileName) throws Exception 
    {
        sendSmtp(new String[] {to}, new String[] {cc}, from, subject, text, contentType, null, bsAttachment, mimeTypeBs, bsFileName);
    }    
    
    /*
     * @param contentType if null or blank, then will default to "text/html; charset=UTF-8"
     *              , others: "text/html", "text/plain", "text/richtext", "text/css", "image/gif"
     */
    /**
     * Sends an email with optional file attachments using recipient arrays.
     *
     * @param to the TO recipients
     * @param cc the CC recipients
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param fileNames file paths to attach
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String[] to, String[] cc, String from, 
            String subject, String text,
            String contentType,
            String[] fileNames) throws Exception 
    {
        sendSmtp(to, cc, from, subject, text, contentType, fileNames, null, null, null);
    }

    /**
     * Sends an email without attachments using recipient arrays.
     *
     * @param to the TO recipients
     * @param cc the CC recipients
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String[] to, String[] cc, String from, 
            String subject, String text,
            String contentType
            ) throws Exception 
    {
        sendSmtp(to, cc, from, subject, text, contentType, null, null, null, null);
    }

    /**
     * Sends an email with a single in-memory attachment using recipient arrays.
     *
     * @param to the TO recipients
     * @param cc the CC recipients
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param bsAttachment attachment data
     * @param mimeTypeBs attachment MIME type
     * @param bsFileName attachment file name
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String[] to, String[] cc, String from, 
            String subject, String text,
            String contentType,
            byte[] bsAttachment, String mimeTypeBs, String bsFileName) throws Exception 
    {
        sendSmtp(to, cc, from, subject, text, contentType, null, bsAttachment, mimeTypeBs, bsFileName);
    }
    
    /**
     * Sends an email with optional file and in-memory attachments using recipient arrays.
     *
     * @param to the TO recipients
     * @param cc the CC recipients
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param fileNames file paths to attach
     * @param bsAttachment attachment data
     * @param mimeTypeBs attachment MIME type
     * @param bsFileName attachment file name
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String[] to, String[] cc, String from, 
            String subject, String text,
            String contentType,
            String[] fileNames, byte[] bsAttachment, String mimeTypeBs, String bsFileName) throws Exception 
    {
        OAMailAttachment[] mas = null;
        if (bsAttachment != null) {
            mas = new OAMailAttachment[1];
            OAMailAttachment ma = new OAMailAttachment();
            ma.bsData = bsAttachment;
            ma.fileName= bsFileName;
            ma.mimeType = mimeTypeBs;
            mas[0] = ma;
        }
        sendSmtp(to, cc, from, subject, text, contentType, fileNames, mas);
    }
    
    
    /**
     * Sends an email with optional file attachments and attachment objects.
     *
     * @param to the TO recipients
     * @param cc the CC recipients
     * @param from the FROM address
     * @param subject the message subject
     * @param text the message body
     * @param contentType the MIME content type
     * @param fileNames file paths to attach
     * @param attachments in-memory attachments
     * @throws Exception if sending fails
     */
    public void sendSmtp( 
            String[] to, String[] cc, String from, 
            String subject, String text,
            String contentType,
            String[] fileNames, OAMailAttachment[] attachments) throws Exception 
    {
        String msg = "";
        if (from == null) from = "";
        Transport transport = null;
        if (port < 1) port = 25;

        if (contentType == null || contentType.length() == 0) {
            // contentType = "text/html; charset=iso-8859-1";
            contentType = "text/html; charset=UTF-8";
        }
        
        
        // create some properties and get the default Session
        Properties props = System.getProperties();
        
        props.put("mail.smtp.auth", "true"); // required
        //props.put("mail.smtp.host", host);
        // if (port > 0) props.put("mail.smtp.port", port+""); // default 25
        // props.put("mail.smtp.user", "test.com@test-smtp");
        // props.put("mail.smtp.password", "testPw"); 
        // props.setProperty ("mail.transport.protocol", "smtp");             
        // props.setProperty("mail.smtp.starttls.enable", "true"); 
        // see: http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html

        /*
        Authenticator auth = new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("test.com@test-smtp", "testPw");
            }
        };
        */       
        
        Session session = Session.getInstance(System.getProperties());
        // Session session = Session.getDefaultInstance(System.getProperties(), null);
        // Session session = Session.getInstance(System.getProperties(), auth);
        session.setDebug(bDebug);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        for (int i=0; to != null && i<to.length; i++) {
            if (!OAString.isEmpty(to[i])) {
                message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to[i]));
            }
        }
        for (int i=0; cc != null && i<cc.length; i++) {
            if (!OAString.isEmpty(cc[i])) {
                message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(cc[i]));
            }
        }
        message.setSubject(subject==null?"":subject);
        //message.setContent(text+host,"text/html; charset=iso-8859-1"); // HTML ??
        
        // create and fill the first message part

        MimeBodyPart mbp1 = new MimeBodyPart();
        //mbp1.setText(text);
        
        mbp1.setContent( text, contentType);  // 20150925
        // was: mbp1.setContent( text, "text/html; charset=iso-8859-1" );
        
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(mbp1);


        if (fileNames != null) {
            for (int i=0; i<fileNames.length; i++) {
                String filename = fileNames[i];
                // attach the file to the message
                FileDataSource fds = new FileDataSource(filename);
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setDataHandler(new DataHandler(fds));
                mbp2.setFileName(fds.getName());
                mp.addBodyPart(mbp2);    // add the Multipart to the message
            }
        }

        
        if (attachments != null) {
            for (OAMailAttachment attachment : attachments) {
                ByteArrayDataSource bads = new ByteArrayDataSource(attachment.bsData, attachment.mimeType);
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setDataHandler(new DataHandler(bads));
                mbp2.setFileName(attachment.fileName);
                mp.addBodyPart(mbp2);    // add the Multipart to the message
            }
        }
        
        message.setContent(mp);
        message.setSentDate(new java.util.Date());
        message.saveChanges();
      
        String s = "stmp";
        if (getUsesSSL()) s += "s";
        
        transport = session.getTransport(s); 
        transport.connect(host, port, userId, password);
        transport.sendMessage(message, message.getAllRecipients());

        transport.close();
        //was: Transport.send(message);
    }

    
    /**
     * Validates whether the given string is a syntactically valid email address.
     *
     * @param email the email address to validate
     * @return true if the email address is valid
     */
    public boolean isValidEmailAddress(String email) {
        if (email == null || email.length() == 0) return false;
        
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }
}





