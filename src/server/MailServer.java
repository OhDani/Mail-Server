package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        String[] creds = credentials.split(",");
        String username = creds[0];
        String password = creds[1];

        // Đường dẫn đến tệp thông tin tài khoản của người dùng
        Path userFilePath = Paths.get(BASE_DIR + username + "/" + username + "_new_email.txt");

        // Kiểm tra xem tệp người dùng có tồn tại không
        if (!Files.exists(userFilePath)) {
            return "USER_NOT_FOUND"; // Tài khoản không tồn tại
        }

        try (BufferedReader reader = Files.newBufferedReader(userFilePath)) {
            String line;
            String storedPassword = null;

            // Đọc tệp để tìm mật khẩu đã lưu
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Password: ")) {
                    storedPassword = line.substring("Password: ".length()).trim();
                    break;
                }
            }

            // Kiểm tra mật khẩu
            if (storedPassword == null) {
                return "INVALID_ACCOUNT_FORMAT"; // Tệp không đúng định dạng
            }

            if (storedPassword.equals(password)) {
                return "LOGIN_SUCCESS"; // Đăng nhập thành công
            } else {
                return "INVALID_CREDENTIALS"; // Sai mật khẩu
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR_READING_FILE"; // Lỗi khi đọc tệp
        }
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

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        try {
            String emailFileName = subject;
            String emailData = "From: " + sender + "\n" +
                    "To: " + recipient + "\n" +
                    "Subject: " + subject + "\n" +
                    "Sent: " + timestamp + "\n\n" + // Thêm thời gian gửi
                    content;
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
            String content = "Username: " + username + "\nPassword: " + password + "\nThank you for using this service. we hope that you will feel comfortable!";
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
