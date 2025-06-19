
/*
 * 灵活的双模式安全通信系统
 */
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

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
            } else if (choice == 2) {
                // 客户端模式
                SecureClient client = new SecureClient(HOST, PORT);
                System.out.println("已连接到服务端");

                // 通信完成后释放客户端
                communicate(null, client);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    private static void communicate(SecureServer server, SecureClient client) {
        Scanner scanner = new Scanner(System.in);
        AtomicBoolean exitFlag = new AtomicBoolean(false); // 标志变量，用于指示是否退出

        // 创建一个线程用于发送消息
        Thread senderThread = new Thread(() -> {
            try {
                while (!exitFlag.get()) { // 根据退出标志判断是否继续发送消息
                    System.out.print("请输入消息(或输入'exit'退出): ");
                    String message = scanner.nextLine();

                    if ("exit".equalsIgnoreCase(message)) {
                        exitFlag.set(true); // 设置退出标志
                        if (server != null) {
                            // 服务端发送消息
                            server.sendMessage("exit");
                        } else {
                            // 客户端发送消息
                            client.sendMessage("exit");
                        }
                        try {
                            Thread.sleep(1000); // 等待一段时间以确保消息发送完毕
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break; // 退出发送循环
                    } else {
                        if (server != null) {
                            // 服务端发送消息
                            server.sendMessage(message);
                        } else {
                            // 客户端发送消息
                            client.sendMessage(message);
                        }
                    }
                }
            } catch (Exception e) {
                if (!"Socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                    System.out.println("发送消息时发生错误: " + e.getMessage());
                }
            } finally {
                scanner.close();
                try {
                    if (server != null) {
                        System.out.println("发送=服务端连接已关闭");
                        server.close(); // 关闭服务端连接
                    } else if (client != null) {
                        System.out.println("发送=客户端连接已关闭");
                        client.close(); // 关闭客户端连接
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("发送=关闭连接时发生错误: " + e.getMessage());
                }
            }
        });

        // 创建一个线程用于接收消息
        Thread receiverThread = new Thread(() -> {
            try {
                while (!exitFlag.get()) { // 根据退出标志判断是否继续接收消息
                    String reply;
                    if (server != null) {
                        reply = server.receiveMessage();
                    } else {
                        reply = client.receiveMessage();
                    }

                    if ("exit".equalsIgnoreCase(reply)) {
                        exitFlag.set(true); // 设置退出标志
                        System.out.println("收到退出信号，即将关闭连接");
                        break; // 退出接收循环
                    }

                    System.out.println("收到回复: " + reply);
                }
            } catch (java.io.EOFException e) {
                // 处理EOFException异常，表示连接已关闭
                System.out.println("接收=连接已关闭");
                exitFlag.set(true); // 设置退出标志
            } catch (Exception e) {
                if (!"Socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                    System.out.println("接收消息时发生错误: " + e.getMessage());
                }
            } finally {
                try {
                    if (server != null) {
                        System.out.println("接收=服务端连接已关闭");
                        server.close(); // 关闭服务端连接
                    } else if (client != null) {
                        System.out.println("接收=客户端连接已关闭");
                        client.close(); // 关闭客户端连接
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("接收=关闭连接时发生错误: " + e.getMessage());
                }
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

        // 等待接收线程结束
        try {
            receiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 确保在所有线程结束后关闭 Scanner
        scanner.close();
    }

}
