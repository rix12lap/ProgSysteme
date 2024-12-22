import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashSet;

public class FileTransferCLI {
    private Client client;
    private FileTransferHistory fileHistory;
    private Scanner scanner;

    public FileTransferCLI() {
        this.client = new Client();
        this.fileHistory = new FileTransferHistory(client);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("\n=== File Transfer System ===");
        
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    sendFile();
                    break;
                case "2":
                    retrieveFile();
                    break;
                case "3":
                    listFiles();
                    break;
                case "4":
                    deleteFile();
                    break;
                case "5":
                    showHistory();
                    break;
                case "0":
                    System.out.println("Au revoir!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Option invalide. Veuillez réessayer.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\nMenu Principal:");
        System.out.println("1. Envoyer un fichier");
        System.out.println("2. Récupérer un fichier");
        System.out.println("3. Lister les fichiers disponibles");
        System.out.println("4. Supprimer un fichier");
        System.out.println("5. Voir l'historique des transferts");
        System.out.println("0. Quitter");
        System.out.print("\nChoisissez une option: ");
    }

    private void sendFile() {
        System.out.print("\nEntrez le chemin complet du fichier à envoyer: ");
        String filePath = scanner.nextLine().trim();
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Erreur: Le fichier n'existe pas.");
                return;
            }

            boolean success = client.sendFile(filePath);
            if (success) {
                fileHistory.addFileTransfer(file.getName(), filePath);
                System.out.println("Fichier envoyé avec succès!");
            } else {
                System.out.println("Échec de l'envoi du fichier.");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
        }
    }

    private void retrieveFile() {
        System.out.print("\nEntrez le nom du fichier à récupérer: ");
        String fileName = scanner.nextLine().trim();
        client.retrieveFile(fileName);
    }

    private void listFiles() {
        System.out.println("\nListe des fichiers disponibles:");
        HashSet<String> files = fileHistory.getAvailableFiles();
        
        if (files.isEmpty()) {
            System.out.println("Aucun fichier disponible.");
            return;
        }

        for (String file : files) {
            System.out.println("- " + file);
        }
    }

    private void deleteFile() {
        System.out.print("\nEntrez le nom du fichier à supprimer: ");
        String fileName = scanner.nextLine().trim();
        
        boolean success = fileHistory.deleteFileFromServer(fileName);
        if (success) {
            System.out.println("Fichier supprimé avec succès.");
        } else {
            System.out.println("Échec de la suppression du fichier.");
        }
    }

    private void showHistory() {
        System.out.println("\nHistorique des transferts:");
        // Cette fonctionnalité peut être améliorée en modifiant FileTransferHistory
        // pour exposer la liste des transferts
        fileHistory.showTransferHistory();
    }

    public static void main(String[] args) {
        FileTransferCLI cli = new FileTransferCLI();
        cli.start();
    }
}