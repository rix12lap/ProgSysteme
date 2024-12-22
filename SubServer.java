// SubServer.java
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class SubServer {
    private static Properties config;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int port;
    private String storagePath;

    public SubServer(int port) {
        this.port = port;
        this.storagePath = config.getProperty("server.storage.path",
                System.getProperty("user.home") + File.separator + "ServerFile" +
                        File.separator + "SubServer" + port + File.separator);
    }

    public void start() {
        printServerBanner();
        try {
            initializeStorage();
            startServer();
        } catch (IOException e) {
            logError("Erreur fatale du sous-serveur", e);
        }
    }

    private void printServerBanner() {
        System.out.println("\n========================================");
        System.out.println("=           Sous-Serveur              =");
        System.out.println("========================================");
        System.out.println("Port: " + port);
        System.out.println("Stockage: " + storagePath);
        System.out.println("----------------------------------------");
    }

    private void initializeStorage() throws IOException {
        Files.createDirectories(Paths.get(storagePath));
        logInfo("Répertoire de stockage initialisé: " + storagePath);
    }

    private void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logInfo("Sous-serveur démarré sur le port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                logInfo("Nouvelle connexion depuis: " + socket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        }
    }

    private static void logInfo(String message) {
        System.out.println("[" + dateFormat.format(new Date()) + "] INFO: " + message);
    }

    private static void logError(String message, Exception e) {
        System.err.println("[" + dateFormat.format(new Date()) + "] ERROR: " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientAddress;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientAddress = socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                
                String command = dis.readUTF();
                logInfo("Commande reçue de " + clientAddress + ": " + command);
                
                // Traitement des commandes spécifiques au sous-serveur
                
            } catch (IOException e) {
                logError("Erreur avec le client " + clientAddress, e);
            } finally {
                try {
                    socket.close();
                    logInfo("Connexion fermée avec " + clientAddress);
                } catch (IOException e) {
                    logError("Erreur lors de la fermeture de la connexion", e);
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SubServer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        SubServer server = new SubServer(port);
        server.start();
    }
}