package client;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MailClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9876;
    private DatagramSocket clientSocket;
    private JTextArea emailContentTextArea;
    private DefaultListModel<String> inboxListModel;
    private String currentUsername = null;

    public MailClient() {
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        initLoginUI();
    }

    // Giao diện đăng nhập
    private void initLoginUI() {
        JFrame frame = new JFrame("Mail Client - Login");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 50, 100, 25);
        frame.add(userLabel);

        JTextField userText = new JTextField(20);
        userText.setBounds(150, 50, 200, 25);
        frame.add(userText);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(50, 100, 100, 25);
        frame.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(150, 100, 200, 25);
        frame.add(passwordText);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(50, 150, 100, 25);
        frame.add(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.setBounds(250, 150, 100, 25);
        frame.add(registerButton);

        // Xử lý đăng nhập
        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passwordText.getPassword());
            if (!username.isEmpty() && !password.isEmpty()) {
                currentUsername = username;
                try {
                    if (sendLoginRequest(username, password)) {
                        frame.dispose();
                        initMailUI();
                    } else {
                        JOptionPane.showMessageDialog(frame, "Login failed, please try again.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter both username and password.");
            }
        });

        // Xử lý đăng ký
        registerButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passwordText.getPassword());
            if (!username.isEmpty() && !password.isEmpty()) {
                try {
                    sendRegisterRequest(username, password);
                    JOptionPane.showMessageDialog(frame, "Account created successfully.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter both username and password.");
            }
        });

        frame.setVisible(true);
    }

    // Giao diện chính sau khi đăng nhập
    private void initMailUI() {
        JFrame frame = new JFrame("Mail Client - Inbox");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Thêm JLabel để hiển thị tên người dùng
        JLabel userLabel = new JLabel();
        userLabel = new JLabel("User: " + currentUsername);
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        frame.add(userLabel, BorderLayout.NORTH); // Đặt JLabel ở trên cùng bên phải

        JPanel inboxPanel = new JPanel();
        inboxPanel.setLayout(new BorderLayout());

        JLabel inboxLabel = new JLabel("Inbox:");
        inboxPanel.add(inboxLabel, BorderLayout.NORTH);

        inboxListModel = new DefaultListModel<>();
        JList<String> inboxList = new JList<>(inboxListModel);
        inboxPanel.add(new JScrollPane(inboxList), BorderLayout.CENTER);

        // Nút Reload để tải lại inbox
        JButton reloadButton = new JButton("Reload Inbox");
        inboxPanel.add(reloadButton, BorderLayout.SOUTH);

        frame.add(inboxPanel, BorderLayout.WEST);

        // Hiển thị nội dung email
        emailContentTextArea = new JTextArea();
        emailContentTextArea.setEditable(false);
        frame.add(new JScrollPane(emailContentTextArea), BorderLayout.CENTER);

        // Tải lại inbox khi bấm Reload
        reloadButton.addActionListener(e -> {
            try {
                reloadInbox();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Nút để gửi email
        JButton sendEmailButton = new JButton("Send Email");
        inboxPanel.add(sendEmailButton, BorderLayout.NORTH);

        sendEmailButton.addActionListener(e -> {
            // Giao diện gửi email
            JFrame sendEmailFrame = new JFrame("Send Email");
            sendEmailFrame.setSize(400, 300);
            sendEmailFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            sendEmailFrame.setLayout(null);

            JLabel recipientLabel = new JLabel("Recipient:");
            recipientLabel.setBounds(50, 30, 100, 25);
            sendEmailFrame.add(recipientLabel);

            JTextField recipientText = new JTextField(20);
            recipientText.setBounds(150, 30, 200, 25);
            sendEmailFrame.add(recipientText);

            JLabel subjectLabel = new JLabel("Subject:");
            subjectLabel.setBounds(50, 80, 100, 25);
            sendEmailFrame.add(subjectLabel);

            JTextField subjectText = new JTextField(20);
            subjectText.setBounds(150, 80, 200, 25);
            sendEmailFrame.add(subjectText);

            JLabel contentLabel = new JLabel("Content:");
            contentLabel.setBounds(50, 130, 100, 25);
            sendEmailFrame.add(contentLabel);

            JTextArea contentText = new JTextArea();
            contentText.setBounds(150, 130, 200, 80);
            sendEmailFrame.add(contentText);

            JButton sendButton = new JButton("Send");
            sendButton.setBounds(150, 220, 100, 25);
            sendEmailFrame.add(sendButton);

            // Xử lý gửi email
            sendButton.addActionListener(sendE -> {
                String recipient = recipientText.getText();
                String subject = subjectText.getText();
                String content = contentText.getText();

                if (!recipient.isEmpty() && !subject.isEmpty() && !content.isEmpty()) {
                    try {
                        sendEmail(currentUsername, recipient, subject, content);
                        JOptionPane.showMessageDialog(sendEmailFrame, "Email sent successfully.");
                        sendEmailFrame.dispose(); // Đóng cửa sổ gửi email
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(sendEmailFrame, "Please fill in all fields.");
                }
            });

            sendEmailFrame.setVisible(true);
        });

        // Hiển thị nội dung email khi click vào email trong danh sách
        inboxList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && inboxList.getSelectedValue() != null) {
                try {
                    displayEmailContent(inboxList.getSelectedValue());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        frame.setVisible(true);

        // Tải inbox ngay sau khi đăng nhập
        try {
            reloadInbox();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Phương thức gửi yêu cầu đăng nhập
    private boolean sendLoginRequest(String username, String password) throws Exception {
        String request = "LOGIN:" + username + "," + password;
        return sendRequestToServer(request).equals("LOGIN_SUCCESS");
    }

    // Phương thức gửi yêu cầu đăng ký
    private void sendRegisterRequest(String username, String password) throws Exception {
        String request = "REGISTER:" + username + "," + password;
        sendRequestToServer(request);
    }

    // Phương thức tải lại inbox
    private void reloadInbox() throws Exception {
        String request = "GET_INBOX:" + currentUsername;
        String inboxFiles = sendRequestToServer(request);
        inboxListModel.clear();
        for (String email : inboxFiles.split("\n")) {
            inboxListModel.addElement(email);
        }
    }

    // Phương thức hiển thị nội dung email
    private void displayEmailContent(String emailFileName) throws Exception {
        String request = "GET_EMAIL:" + currentUsername + "," + emailFileName;
        String emailContent = sendRequestToServer(request);
        emailContentTextArea.setText(emailContent);
    }

    // Phương thức gửi yêu cầu đến server
    private String sendRequestToServer(String message) throws Exception {
        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
        clientSocket.send(sendPacket);

        // Nhận phản hồi từ server
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength());
    }
    private void sendEmail(String sender, String recipient, String subject, String content) throws Exception {
        String request = "SEND_EMAIL:" + sender + "," + recipient + "," + subject + "," + content;
        String response = sendRequestToServer(request);
        if (!response.equals("SEND_SUCCESS")) {
            throw new Exception("Failed to send email.");
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MailClient::new);
    }
}
