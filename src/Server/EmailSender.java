package Server;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

public class EmailSender {
    private MakePDF MakePDF;

    public EmailSender() { this.MakePDF = new MakePDF(); }

    public void aboutRegistering(String clientEmail){
        String text, subject;
        Message message;
        Multipart multipart = new MimeMultipart();

        text = "Dear Sir, or Madam,\nThank you for registering in our app!\nWe wish You many pleasant screenings.\nAdministration";
        subject = "Registering email";
        message = makeCommonMessage(multipart,clientEmail,text,subject);
        sendMessage(message, multipart);
    }

    public void aboutForgottenPassword(String clientEmail, String newPassword){
        String text, subject;
        Message message;
        Multipart multipart = new MimeMultipart();

        text = "Dear Sir, or Madam,\nIt looks like you've forgotten your password, here you will find new one: "+ newPassword +".\nRemember to change the password in the application";
        subject = "Password reset";
        message = makeCommonMessage(multipart,clientEmail,text,subject);
        sendMessage(message, multipart);
    }

    public void aboutOrder(String clientEmail, String number, String person, String owner){
        String text, subject;
        Message message;
        Multipart multipart = new MimeMultipart();

        text = "Dear Sir, or Madam,\nThank you for ordering a screening at our cinema!\nHere is your ticket, please print it.\nHave a nice viewing,\nAdministration";
        subject = "Order confirmation email";
        message = makeCommonMessage(multipart,clientEmail,text,subject);
        extendMessage(multipart, number, person, owner);
        sendMessage(message, multipart);
    }

    private Message makeCommonMessage(Multipart multipart, String clientEmail, String text, String subject) {
        String sender, host;
        Properties properties;
        Session session;
        MimeMessage message = null;

        sender = "YourPlace@cinema.com";
        host = "127.0.0.1";
        properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        session = Session.getDefaultInstance(properties);
        try {
            BodyPart mainBody = new MimeBodyPart();

            message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(clientEmail));
            message.setSubject(subject);

            mainBody.setText(text);
            multipart.addBodyPart(mainBody);
        }
        catch (MessagingException ex) {
            System.out.println("Message send error: " + ex.getMessage());
        }
        return message;
    }

    private void extendMessage( Multipart multipart, String number, String person, String owner){
        BodyPart messageBodyPart;
        ByteArrayOutputStream outputStream;
        DataSource dataSource;
        byte[] bytes;

        try {
            messageBodyPart = new MimeBodyPart();

            outputStream = new ByteArrayOutputStream();
            MakePDF.generate(outputStream,number,person,owner);
            bytes = outputStream.toByteArray();
            dataSource = new ByteArrayDataSource(bytes, "application/pdf");
            messageBodyPart.setDataHandler(new DataHandler(dataSource));
            messageBodyPart.setFileName("Ticket");

            multipart.addBodyPart(messageBodyPart);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Message message, Multipart multipart){
        try {
            message.setContent(multipart);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
