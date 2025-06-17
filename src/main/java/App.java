
/*
 * 灵活的双模式安全通信系统
 */
import java.util.Scanner;

public class App {
    private static final int PORT = 12345;
    private static final String HOST = "localhost";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("请选择模式: ");
        System.out.println("1. 作为服务端等待连接");
        System.out.println("2. 作为客户端主动连接");
        System.out.print("请输入选择(1/2): ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符

        try {
            if (choice == 1) {
                // 服务端模式
                SecureServer server = new SecureServer(PORT);
                System.out.println("服务端启动，等待连接...");

                server.acceptConnection();
                System.out.println("客户端已连接");

                // 通信完成后释放服务端
                communicate(server, null);
                server.close();
            } else if (choice == 2) {
                // 客户端模式
                SecureClient client = new SecureClient(HOST, PORT);
                System.out.println("已连接到服务端");

                // 通信完成后释放客户端
                communicate(null, client);
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void communicate(SecureServer server, SecureClient client)
            throws Exception {
        Scanner scanner = new Scanner(System.in);

        try {
            while (true) {
                System.out.print("请输入消息(或输入'exit'退出): ");
                String message = scanner.nextLine();

                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }

                if (server != null) {
                    // 服务端发送消息
                    server.sendMessage(message);
                    // 接收回复
                    String reply = server.receiveMessage();
                    System.out.println("收到回复: " + reply);
                } else {
                    // 客户端发送消息
                    client.sendMessage(message);
                    // 接收回复
                    String reply = client.receiveMessage();
                    System.out.println("收到回复: " + reply);
                }
            }
        } finally {
            scanner.close();
        }
    }
}
