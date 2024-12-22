import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.HashSet;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainServer {
    private static Properties config;
    private static final int MAIN_SERVER_PORT;
    private static final String[] SUBSERVER_HOSTS;
    private static final int[] SUBSERVER_PORTS;
    private static final String STORAGE_PATH;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MAIN_SERVER_PORT = Integer.parseInt(config.getProperty("main.server.port", "5000"));
        SUBSERVER_HOSTS = config.getProperty("subservers.hosts", "localhost:5001,localhost:5002,localhost:5003").split(",");
        SUBSERVER_PORTS = new int[SUBSERVER_HOSTS.length];
        for (int i = 0; i < SUBSERVER_HOSTS.length; i++) {
            SUBSERVER_PORTS[i] = Integer.parseInt(SUBSERVER_HOSTS[i].split(":")[1]);
        }
        STORAGE_PATH = config.getProperty("server.storage.path", 
            System.getProperty("user.home") + File.separator + "ServerFile" + File.separator);
    }

    public static void main(String[] args) {
        printServerBanner();
        try {
            initializeStorage();
            startServer();
        } catch (IOException e) {
            logError("Erreur fatale du serveur", e);
        }
    }

    private static void printServerBanner() {
        System.out.println("\n========================================");
        System.out.println("=        Serveur de Fichiers           =");
        System.out.println("========================================");
        System.out.println("Port principal: " + MAIN_SERVER_PORT);
        System.out.println("Stockage: " + STORAGE_PATH);
        System.out.println("Sous-serveurs configurés: " + SUBSERVER_HOSTS.length);
        System.out.println("----------------------------------------");
    }

    private static void initializeStorage() throws IOException {
        // Créer les répertoires pour chaque sous-serveur
        for (int port : SUBSERVER_PORTS) {
            Path subServerPath = Paths.get(STORAGE_PATH + "SubServer" + port);
            Files.createDirectories(subServerPath);
            logInfo("Répertoire sous-serveur initialisé: " + subServerPath);
        }

        // Créer le répertoire principal
        Path mainStoragePath = Paths.get(STORAGE_PATH);
        Files.createDirectories(mainStoragePath);
        logInfo("Répertoire principal initialisé: " + mainStoragePath);
    }

    private static void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(MAIN_SERVER_PORT)) {
            logInfo("Serveur démarré et en attente de connexions sur le port " + MAIN_SERVER_PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logInfo("Nouvelle connexion: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String clientAddress;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientAddress = socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                String command = dis.readUTF();
                logInfo("Client " + clientAddress + " - Commande reçue: " + command);

                switch (command) {
                    case "SEND_FILE":
                        receiveFile(dis);
                        break;
                    case "RETRIEVE_FILE":
                        String fileName = dis.readUTF();
                        sendFileToClient(fileName, dos);
                        break;
                    case "LIST_FILES":
                        listFiles(dos);
                        break;
                    case "DELETE_FILE":
                        deleteFile(dis, dos);
                        break;
                    default:
                        logError("Commande inconnue reçue: " + command, null);
                }
            } catch (IOException e) {
                logError("Erreur avec le client " + clientAddress, e);
            } finally {
                try {
                    clientSocket.close();
                    logInfo("Connexion fermée: " + clientAddress);
                } catch (IOException e) {
                    logError("Erreur lors de la fermeture de la connexion", e);
                }
            }
        }

        private void listFiles(DataOutputStream dos) throws IOException {
            logInfo("Listage des fichiers demandé par " + clientAddress);
            HashSet<String> uniqueFiles = new HashSet<>();
            
            for (int port : SUBSERVER_PORTS) {
                File subServerDir = new File(STORAGE_PATH + "SubServer" + port);
                if (subServerDir.exists() && subServerDir.isDirectory()) {
                    File[] files = subServerDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().contains("_part1")) {
                                String originalName = file.getName().substring(0, file.getName().indexOf("_part1"));
                                uniqueFiles.add(originalName);
                            }
                        }
                    }
                }
            }

            dos.writeInt(uniqueFiles.size());
            logInfo("Envoi de la liste des fichiers (" + uniqueFiles.size() + " fichiers) à " + clientAddress);
            
            for (String fileName : uniqueFiles) {
                dos.writeUTF(fileName);
                logInfo("- " + fileName);
            }
        }

        private void deleteFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();
            logInfo("Demande de suppression: " + fileName + " par " + clientAddress);
            boolean success = true;

            for (int port : SUBSERVER_PORTS) {
                String subServerPath = STORAGE_PATH + "SubServer" + port + File.separator;
                for (int i = 1; i <= 3; i++) {
                    File partFile = new File(subServerPath + fileName + "_part" + i);
                    if (partFile.exists()) {
                        if (partFile.delete()) {
                            logInfo("Partie " + i + " supprimée sur le sous-serveur " + port);
                        } else {
                            success = false;
                            logError("Échec de la suppression de la partie " + i + " sur " + port, null);
                        }
                    }
                }
            }

            dos.writeBoolean(success);
            logInfo("Suppression " + (success ? "réussie" : "échouée") + " pour " + fileName);
        }

        private void receiveFile(DataInputStream dis) throws IOException {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            logInfo("Réception de " + fileName + " (" + formatFileSize(fileSize) + ") depuis " + clientAddress);

            byte[] buffer = new byte[1024];
            int partSize = (int) Math.ceil(fileSize / 3.0);

            for (int i = 0; i < 3; i++) {
                String partFileName = fileName + "_part" + (i + 1);
                String subServerStoragePath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;
                Files.createDirectories(Paths.get(subServerStoragePath));

                try (FileOutputStream fos = new FileOutputStream(subServerStoragePath + partFileName)) {
                    int bytesRead;
                    int totalBytesRead = 0;
                    int remainingBytes = (i == 2) ? (int) (fileSize - (partSize * 2)) : partSize;

                    while (totalBytesRead < remainingBytes &&
                            (bytesRead = dis.read(buffer, 0, Math.min(buffer.length, remainingBytes - totalBytesRead))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }

                    logInfo("Partie " + (i + 1) + " (" + formatFileSize(totalBytesRead) + ") sauvegardée dans SubServer" + SUBSERVER_PORTS[i]);
                }
            }

            logInfo("Fichier " + fileName + " complètement reçu et distribué");
        }

        private void sendFileToClient(String fileName, DataOutputStream dos) throws IOException {
            logInfo("Demande de récupération: " + fileName + " par " + clientAddress);
            File tempFile = new File(STORAGE_PATH + fileName);

            boolean allPartsExist = verifyAllParts(fileName);
            dos.writeBoolean(allPartsExist);

            if (!allPartsExist) {
                logError("Parties manquantes pour " + fileName, null);
                return;
            }

            reassembleAndSendFile(fileName, tempFile, dos);
        }

        private boolean verifyAllParts(String fileName) {
            for (int i = 0; i < SUBSERVER_PORTS.length; i++) {
                String partFileName = fileName + "_part" + (i + 1);
                String subServerPath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;
                File partFile = new File(subServerPath + partFileName);
                if (!partFile.exists()) {
                    logError("Partie manquante: " + partFileName, null);
                    return false;
                }
            }
            return true;
        }

        private void reassembleAndSendFile(String fileName, File tempFile, DataOutputStream dos) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                for (int i = 0; i < SUBSERVER_PORTS.length; i++) {
                    String partFileName = fileName + "_part" + (i + 1);
                    String subServerPath = STORAGE_PATH + "SubServer" + SUBSERVER_PORTS[i] + File.separator;
                    File partFile = new File(subServerPath + partFileName);

                    copyPartToFinalFile(partFile, fos);
                    logInfo("Partie " + (i + 1) + " assemblée depuis SubServer" + SUBSERVER_PORTS[i]);
                }
            }

            sendReassembledFile(tempFile, dos);
            tempFile.delete();
            logInfo("Fichier " + fileName + " envoyé avec succès à " + clientAddress);
        }

        private void copyPartToFinalFile(File partFile, FileOutputStream fos) throws IOException {
            try (FileInputStream fis = new FileInputStream(partFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        }

        private void sendReassembledFile(File file, DataOutputStream dos) throws IOException {
            dos.writeLong(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    // Méthodes utilitaires pour le logging
    private static void logInfo(String message) {
        System.out.println("[" + dateFormat.format(new Date()) + "] INFO: " + message);
    }

    private static void logError(String message, Exception e) {
        System.err.println("[" + dateFormat.format(new Date()) + "] ERROR: " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    private static String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}