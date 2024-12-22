import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Properties;

public class Client {
    private static Properties config;
    private static final String SERVER_HOST;
    private static final int SERVER_PORT;
    private static final String DOWNLOAD_PATH;

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration: " + e.getMessage());
        }

        SERVER_HOST = config.getProperty("main.server.host", "localhost");
        SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
        DOWNLOAD_PATH = config.getProperty("download.path", System.getProperty("user.home") + File.separator + "Downloads" + File.separator);
    }

    public boolean sendFile(String filePath) throws IOException {
        File file = new File(filePath); 
        if (!file.exists()) {
            System.out.println("Le fichier n'existe pas: " + filePath);
            return false;
        }

        System.out.println("Connexion au serveur " + SERVER_HOST + ":" + SERVER_PORT + "...");
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(filePath)) {

            String fileName = file.getName();
            long fileSize = file.length();
            System.out.println("Envoi du fichier: " + fileName + " (" + formatFileSize(fileSize) + ")");

            dos.writeUTF("SEND_FILE");
            dos.writeUTF(fileName);
            dos.writeLong(fileSize);

            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalSent = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                totalSent += bytesRead;
                int progress = (int) ((totalSent * 100) / fileSize);
                System.out.print("\rProgression: " + progress + "% (" + formatFileSize(totalSent) + "/" + formatFileSize(fileSize) + ")");
            }
            System.out.println("\nFichier envoyé avec succès");
            return true;
        } catch (IOException e) {
            System.err.println("\nErreur lors de l'envoi du fichier: " + e.getMessage());
            throw e;
        }
    }

    public void retrieveFile(String fileName) {
        System.out.println("Demande de téléchargement du fichier: " + fileName);
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("RETRIEVE_FILE");
            dos.writeUTF(fileName);

            boolean fileExists = dis.readBoolean();
            if (!fileExists) {
                System.out.println("Le fichier " + fileName + " n'existe pas sur le serveur");
                return;
            }

            long fileSize = dis.readLong();
            File downloadFile = new File(DOWNLOAD_PATH + fileName);
            System.out.println("Téléchargement de " + fileName + " (" + formatFileSize(fileSize) + ")");

            try (FileOutputStream fos = new FileOutputStream(downloadFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalReceived = 0;

                while (totalReceived < fileSize) {
                    bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalReceived));
                    if (bytesRead == -1) break;
                    
                    fos.write(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                    
                    int progress = (int) ((totalReceived * 100) / fileSize);
                    System.out.print("\rProgression: " + progress + "% (" + formatFileSize(totalReceived) + "/" + formatFileSize(fileSize) + ")");
                }
                System.out.println("\nFichier téléchargé avec succès dans: " + downloadFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("\nErreur lors du téléchargement du fichier: " + e.getMessage());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}