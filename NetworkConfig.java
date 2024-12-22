import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class NetworkConfig {
    private static final Logger LOGGER = Logger.getLogger(NetworkConfig.class.getName());
    private static final String DEFAULT_CONFIG_PATH = "network.properties";
    private static Properties properties;

    // Propriétés par défaut si le fichier est manquant
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_MAIN_PORT = 5000;
    private static final String[] DEFAULT_SUBSERVER_HOSTS = {"localhost", "localhost", "localhost"};
    private static final int[] DEFAULT_SUBSERVER_PORTS = {5001, 5002, 5003};

    static {
        properties = new Properties();
        loadConfig(DEFAULT_CONFIG_PATH);
    }

    // Méthode de chargement configurable
    public static void loadConfig(String configPath) {
        try {
            if (Files.exists(Paths.get(configPath))) {
                try (FileInputStream fis = new FileInputStream(configPath)) {
                    properties.load(fis);
                    LOGGER.info("Configuration chargée depuis : " + configPath);
                }
            } else {
                LOGGER.warning("Fichier de configuration non trouvé. Utilisation des paramètres par défaut.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur de chargement de la configuration", e);
        }
    }

    // Méthodes de récupération sécurisées
    public static String getMainServerHost() {
        return properties.getProperty("main.server.host", DEFAULT_HOST);
    }

    public static int getMainServerPort() {
        return Integer.parseInt(properties.getProperty("main.server.port",
                String.valueOf(DEFAULT_MAIN_PORT)));
    }

    public static String[] getSubserverHosts() {
        String hostsProperty = properties.getProperty("subservers.hosts",
                String.join(",", DEFAULT_SUBSERVER_HOSTS));
        return hostsProperty.split(",");
    }

    public static int[] getSubserverPorts() {
        String portsProperty = properties.getProperty("subservers.ports",
                "5001,5002,5003");
        String[] portStrings = portsProperty.split(",");

        int[] ports = new int[portStrings.length];
        for (int i = 0; i < portStrings.length; i++) {
            ports[i] = Integer.parseInt(portStrings[i]);
        }
        return ports;
    }

    public static String getStoragePath() {
        String defaultPath = System.getProperty("user.home") + File.separator + "ServerFiles" + File.separator;
        return properties.getProperty("storage.path", defaultPath);
    }

    public static String getDownloadPath() {
        return properties.getProperty("download.path",
                System.getProperty("user.home") + "/Downloads/");
    }
}