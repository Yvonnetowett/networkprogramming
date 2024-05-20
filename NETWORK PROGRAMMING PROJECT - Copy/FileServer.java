import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private static final int SERVER_PORT = 1026;
    private static String currentDirectory = System.getProperty("user.dir");

    public static void main(String[] args) {
        // Use try-with-resources to ensure that the ServerSocket is automatically closed
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server is listening on " + serverSocket.getInetAddress().getHostAddress() + ":" + SERVER_PORT);
            System.out.println("Please connect to this address.");

            while (true) {
                // Accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from " + clientSocket.getInetAddress().getHostAddress() + " established.");

                // Handle each client connection in a separate thread
                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error occurred while starting the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (clientSocket; // Auto-close the client socket when done or an exception is thrown
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            while (true) {
                String command = in.readLine();
                if (command == null) break;

                switch (command.split(" ")[0]) {
                    case "LIST":
                        out.println(getFileStructure(currentDirectory));
                        break;
                    case "GET":
                        sendFile(command.split(" ")[1], out, clientSocket);
                        break;
                    case "CD":
                        changeDirectory(command.split(" ")[1], out);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred with client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        }
    }

    private static void sendFile(String filename, PrintWriter out, Socket clientSocket) throws IOException {
        String filePath = currentDirectory + File.separator + filename;
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(filePath))) {
                String fileData;
                while ((fileData = fileReader.readLine()) != null) {
                    out.println(fileData);
                }
            }
            System.out.println("File '" + filename + "' sent to " + clientSocket.getInetAddress().getHostAddress());
        } else {
            out.println("File not found");
        }
    }

    private static void changeDirectory(String folderName, PrintWriter out) {
        String newDirectory = currentDirectory + File.separator + folderName;
        File directory = new File(newDirectory);
        if (directory.exists() && directory.isDirectory()) {
            currentDirectory = newDirectory;tg3
            out.println("Entered folder successfully.");
        } else {
            out.println("Folder not found.");
        }
    }

    private static String getFileStructure(String directory) {
        StringBuilder fileStructure = new StringBuilder();
        File rootDirectory = new File(directory);
        File[] files = rootDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileStructure.append(file.getName()).append("/\n");
                } else {
                    fileStructure.append(file.getName()).append("\n");
                }
            }
        }

        return fileStructure.toString();
    }
}
