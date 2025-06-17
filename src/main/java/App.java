
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

    private static void communicate(SecureServer server, SecureClient client) {
        Scanner scanner = new Scanner(System.in);

        // 创建一个线程用于发送消息
        Thread senderThread = new Thread(() -> {
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
                    } else {
                        // 客户端发送消息
                        client.sendMessage(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 或者根据需要添加其他的异常处理逻辑
                System.out.println("发送消息时发生错误: " + e.getMessage());
            } finally {
                scanner.close();
            }
        });

        // 创建一个线程用于接收消息
        Thread receiverThread = new Thread(() -> {
            try {
                while (true) {
                    String reply;
                    if (server != null) {
                        reply = server.receiveMessage();
                    } else {
                        reply = client.receiveMessage();
                    }

                    if (reply == null) {
                        break; // 假设如果接收到null则退出接收线程
                    }

                    System.out.println("收到回复: " + reply);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 启动发送线程和接收线程
        senderThread.start();
        receiverThread.start();

        // 等待发送线程结束
        try {
            senderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 退出接收线程，这里可以通过向接收线程发送退出信号来优雅地关闭接收线程
        // 这里假设当发送线程结束时，接收线程也会自然结束，或者你有其他方式来结束接收线程
        receiverThread.interrupt();
    }

}
