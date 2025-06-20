import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppWithGUI {
    private static final int PORT = 12345;
    private static SecureServer server = null;
    private static SecureClient client = null;
    private static JTextArea chatArea;
    private static JTextField inputField;
    private static JButton sendButton;
    private static JButton fileButton;
    private static AtomicBoolean exitFlag = new AtomicBoolean(false);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppWithGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("安全通信系统");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);

        // 顶部面板（模式选择 + IP 输入 + 启动）
        String[] modes = { "服务端", "客户端" };
        JComboBox<String> modeSelector = new JComboBox<>(modes);
        JTextField ipField = new JTextField("localhost", 12);
        JButton startButton = new JButton("启动");

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("选择模式: "));
        topPanel.add(modeSelector);
        topPanel.add(new JLabel("服务器IP: "));
        topPanel.add(ipField);
        topPanel.add(startButton);

        // 聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // 输入面板（消息 + 按钮）
        inputField = new JTextField();
        sendButton = new JButton("发送");
        fileButton = new JButton("发送文件");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(fileButton, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);

        // 布局添加
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
        inputField.setEnabled(false);

        // 启动按钮监听
        startButton.addActionListener(e -> {
            int mode = modeSelector.getSelectedIndex();
            String ip = ipField.getText().trim();
            new Thread(() -> startCommunication(mode, ip)).start();
            startButton.setEnabled(false);
            modeSelector.setEnabled(false);
            ipField.setEnabled(false);
            sendButton.setEnabled(true);
            inputField.setEnabled(true);
            fileButton.setEnabled(true);
        });

        // 消息发送监听
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());

        frame.setVisible(true);
    }

    private static void startCommunication(int mode, String ip) {
        try {
            if (mode == 0) {
                server = new SecureServer(PORT);
                appendMessage("服务端启动，等待连接...");
                server.acceptConnection();
                appendMessage("客户端已连接");
            } else {
                client = new SecureClient(ip, PORT);
                appendMessage("已连接到服务端: " + ip);
            }

            Thread receiverThread = new Thread(() -> {
                try {
                    while (!exitFlag.get()) {
                        String msg = (server != null) ? server.receiveMessage() : client.receiveMessage();
                        if ("exit".equalsIgnoreCase(msg)) {
                            exitFlag.set(true);
                            appendMessage("对方已断开连接");
                            break;
                        }
                        appendMessage("对方: " + msg);
                    }
                } catch (Exception ex) {
                    if (!"Socket closed".equalsIgnoreCase(ex.getMessage())) {
                        appendMessage("接收错误: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                } finally {
                    closeConnection();
                }
            });

            receiverThread.start();

        } catch (Exception e) {
            appendMessage("连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty())
            return;

        appendMessage("我: " + text);
        inputField.setText("");
        inputField.requestFocusInWindow();

        try {
            if ("exit".equalsIgnoreCase(text)) {
                exitFlag.set(true);
                if (server != null)
                    server.sendMessage("exit");
                else
                    client.sendMessage("exit");
                closeConnection();
            } else {
                if (server != null)
                    server.sendMessage(text);
                else
                    client.sendMessage(text);
            }
        } catch (Exception e) {
            appendMessage("发送错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                appendMessage("发送文件: " + file.getName());
                if (server != null)
                    server.sendFile(file);
                else
                    client.sendFile(file);
            } catch (Exception e) {
                appendMessage("文件发送失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private static void closeConnection() {
        try {
            if (server != null)
                server.close();
            if (client != null)
                client.close();
        } catch (IOException e) {
            appendMessage("关闭连接失败: " + e.getMessage());
        }
    }
}
