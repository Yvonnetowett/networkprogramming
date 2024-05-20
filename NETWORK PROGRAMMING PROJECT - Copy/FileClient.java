import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileClient extends JFrame {

    private DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private JList<String> fileList = new JList<>(fileListModel);
    private ExecutorService downloadExecutor = Executors.newCachedThreadPool();
    private JTextField ipAddressField; // Field for IP address
    private Socket socket;

    public FileClient() {
        setTitle("File Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create components
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(fileList);
        add(scrollPane, BorderLayout.CENTER);

        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> downloadSelectedFile());
        add(downloadButton, BorderLayout.SOUTH);

        // Panel for IP address input
        JPanel ipPanel = new JPanel();
        ipPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Align left
        JLabel ipLabel = new JLabel("Server IP: ");
        ipAddressField = new JTextField(15); // Field for IP address
        ipPanel.add(ipLabel);
        ipPanel.add(ipAddressField);
        add(ipPanel, BorderLayout.NORTH); // Add IP panel to the top

        // Connect button
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> establishConnection());
        add(connectButton, BorderLayout.WEST); // Add the button to the left side of the frame

        // Initialize socket
        socket = null;

        // Fetch file list only after connection is established
    }

    private void establishConnection() {
        try {
            String ipAddress = ipAddressField.getText(); // Get IP address from the field
            if (ipAddress.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter server IP address", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            socket = new Socket(ipAddress, 1026);
            fetchFileList();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to server", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fetchFileList() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("LIST");
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                fileListModel.addElement(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to fetch file list", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadSelectedFile() {
        if (socket == null || socket.isClosed()) {
            establishConnection();
            return;
        }

        String selectedFile = fileList.getSelectedValue();
        if (selectedFile == null || selectedFile.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No file selected", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        downloadExecutor.submit(() -> {
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("GET " + selectedFile);

                File download = new File(selectedFile);
                try (BufferedWriter fileOut = new BufferedWriter(new FileWriter(download))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        fileOut.write(line);
                        fileOut.newLine();
                    }
                }

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Downloaded: " + selectedFile, "Download Complete", JOptionPane.INFORMATION_MESSAGE));

            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Failed to download: " + selectedFile, "Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileClient client = new FileClient();
            client.setVisible(true);
        });
    }
}
