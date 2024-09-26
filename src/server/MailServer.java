package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MailServer {
    private static final int PORT = 9876;
    private static final String BASE_DIR = "mailboxes/";

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(PORT);
        System.out.println("Mail Server is running...");

        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received request: " + request);
            String response = handleRequest(request);

            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            byte[] sendData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        }
    }

    private static String handleRequest(String request) {
        String[] parts = request.split(":");
        String command = parts[0];

        switch (command) {
            case "LOGIN":
                return handleLogin(parts[1]);
            case "REGISTER":
                return handleRegister(parts[1]);
            case "SEND_EMAIL":
                return handleSendEmail(parts[1]);
            case "GET_INBOX":
                return handleGetInbox(parts[1]);
            case "GET_EMAIL":
                return handleGetEmail(parts[1]);
            default:
                return "INVALID_COMMAND";
        }
    }

    private static String handleLogin(String credentials) {
        // Xử lý đăng nhập (nên lưu tài khoản trong database hoặc file)
        return "LOGIN_SUCCESS"; // Thay đổi theo logic của bạn
    }

    private static String handleRegister(String credentials) {
        // Xử lý đăng ký tài khoản (tạo thư mục người dùng)
        String[] creds = credentials.split(",");
        String username = creds[0];
        String password = creds[1]; // Giả sử bạn đã thêm password vào thông tin đăng ký

        try {
            Path userDir = Paths.get(BASE_DIR + username);
            Files.createDirectories(userDir); // Tạo thư mục người dùng nếu chưa tồn tại
            // Tạo một đối tượng MailServer để gọi phương thức không tĩnh
            MailServer server = new MailServer();
            server.createNewAccountFile(username, password); // Gọi hàm để tạo tệp thông tin tài khoản
            return "REGISTER_SUCCESS";
        } catch (IOException e) {
            return "REGISTER_FAILED";
        }
    }

    private static String handleSendEmail(String data) {
        String[] emailParts = data.split(",");
        String sender = emailParts[0];
        String recipient = emailParts[1];
        String subject = emailParts[2];
        String content = emailParts[3];

        try {
            String emailFileName = subject.replaceAll("[^a-zA-Z0-9]", "_");
            String emailData = "From: " + sender + "\nTo: " + recipient + "\nSubject: " + subject + "\n\n" + content;
            Files.write(Paths.get(BASE_DIR + recipient + "/" + emailFileName), emailData.getBytes());
            return "SEND_SUCCESS";
        } catch (IOException e) {
            return "SEND_FAILED";
        }
    }

    private static String handleGetInbox(String username) {
        try {
            Path inboxDir = Paths.get(BASE_DIR + username);
            if (!Files.exists(inboxDir)) {
                return "INBOX_EMPTY";
            }

            StringBuilder inbox = new StringBuilder();
            DirectoryStream<Path> stream = Files.newDirectoryStream(inboxDir);
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                inbox.append(fileName).append("\n");
            }
            return inbox.toString().trim();
        } catch (IOException e) {
            return "ERROR_GETTING_INBOX";
        }
    }
    private void createNewAccountFile(String username, String password) {
        try {
            String content = "Username: " + username + "\nPassword: " + password + "\nWelcome " + username + "!";
            // Sử dụng BASE_DIR để tạo đường dẫn cho tệp
            Path filePath = Paths.get(BASE_DIR + username + "/" + username + "_new_email.txt");
            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String handleGetEmail(String data) {
        String[] parts = data.split(",");
        String username = parts[0];
        String emailFileName = parts[1];

        try {
            Path emailPath = Paths.get(BASE_DIR + username + "/" + emailFileName);
            String content = new String(Files.readAllBytes(emailPath));
            return content;
        } catch (IOException e) {
            return "ERROR_GETTING_EMAIL";
        }
    }
}
