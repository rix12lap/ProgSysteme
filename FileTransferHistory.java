import java.io.*;
import java.net.Socket;
import java.util.*;
import java.text.SimpleDateFormat;

public class FileTransferHistory {
    private List<FileTransfer> transfers;
    private Client client;
    private static Properties config;
    private static final String SERVER_HOST;
    private static final int SERVER_PORT;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la configuration: " + e.getMessage());
        }
        SERVER_HOST = config.getProperty("main.server.host", "localhost");
        SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
    }

    public FileTransferHistory(Client client) {
        this.transfers = new ArrayList<>();
        this.client = client;
    }

    public void addFileTransfer(String fileName, String filePath) {
        transfers.add(new FileTransfer(fileName, filePath, new Date()));
        System.out.println("Transfert ajouté à l'historique: " + fileName);
    }

    public HashSet<String> getAvailableFiles() {
        HashSet<String> availableFiles = new HashSet<>();
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST_FILES");
            int fileCount = dis.readInt();
            
            for (int i = 0; i < fileCount; i++) {
                availableFiles.add(dis.readUTF());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la récupération de la liste des fichiers: " + e.getMessage());
        }
        return availableFiles;
    }

    public boolean deleteFileFromServer(String fileName) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DELETE_FILE");
            dos.writeUTF(fileName);

            return dis.readBoolean();
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
            return false;
        }
    }

    public void showTransferHistory() {
        if (transfers.isEmpty()) {
            System.out.println("Aucun transfert dans l'historique.");
            return;
        }

        System.out.println("\nHistorique des transferts:");
        System.out.println("----------------------------------------");
        for (FileTransfer transfer : transfers) {
            System.out.printf("Fichier: %s%n", transfer.getFileName());
            System.out.printf("Chemin: %s%n", transfer.getFilePath());
            System.out.printf("Date: %s%n", dateFormat.format(transfer.getTransferDate()));
            System.out.println("----------------------------------------");
        }
    }

    private static class FileTransfer {
        private String fileName;
        private String filePath;
        private Date transferDate;

        public FileTransfer(String fileName, String filePath, Date transferDate) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.transferDate = transferDate;
        }

        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public Date getTransferDate() { return transferDate; }
    }
}