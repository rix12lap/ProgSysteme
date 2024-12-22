Côté Serveur (PC Serveur) :
javac MainServer.java SubServer.java NetWorkConfig.java

# Terminal 1 : Serveur principal
java MainServer

# Terminal 2 : Sous-serveur 1
java SubServer 5001

# Terminal 3 : Sous-serveur 2
java SubServer 5002

# Terminal 4 : Sous-serveur 3
java SubServer 5003

Côté Client (Client PC) :
javac Client.java FileTransferHistory.java FileTransferCLI.java

java FileTransferCLI



