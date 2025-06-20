import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecureServer {
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private SecretKey aesKey;

    public SecureServer(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println("服务器监听端口 " + port + "...");
    }

    public void acceptConnection() throws Exception {
        KeyPair serverKeyPair = CryptoUtil.generateRSAKeyPair();

        socket = serverSocket.accept();
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        // 身份验证（略）
        String clientPubKeyStr = in.readUTF();
        String clientSignature = in.readUTF();
        PublicKey clientPubKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(clientPubKeyStr)));

        if (!CryptoUtil.verify(clientPubKeyStr, clientSignature, clientPubKey)) {
            throw new SecurityException("客户端身份验证失败");
        }
        System.out.println("客户端身份已验证");

        // 发送公钥和签名
        String serverPubKeyStr = Base64.getEncoder().encodeToString(serverKeyPair.getPublic().getEncoded());
        String serverSignature = CryptoUtil.sign(serverPubKeyStr, serverKeyPair.getPrivate());
        out.writeUTF(serverPubKeyStr);
        out.writeUTF(serverSignature);

        // 接收 AES 密钥
        int len = in.readInt();
        byte[] encryptedAesKey = new byte[len];
        in.readFully(encryptedAesKey);
        byte[] aesKeyBytes = CryptoUtil.decryptRSA(encryptedAesKey, serverKeyPair.getPrivate());
        aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        System.out.println("安全连接已建立");
    }

    public String receiveMessage() throws Exception {
        int length = in.readInt();
        byte[] encrypted = new byte[length];
        in.readFully(encrypted);
        String message = CryptoUtil.decryptAES(encrypted, aesKey);

        // 文件标识
        if (message.equals("[FILE]")) {
            String fileName = CryptoUtil.decryptAES(readEncrypted(), aesKey);
            long fileSize = Long.parseLong(CryptoUtil.decryptAES(readEncrypted(), aesKey));

            File outputFile = new File("received_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                long remaining = fileSize;
                while (remaining > 0) {
                    int chunkLen = in.readInt();
                    byte[] chunkEncrypted = new byte[chunkLen];
                    in.readFully(chunkEncrypted);
                    byte[] chunk = CryptoUtil.decryptAESBytes(chunkEncrypted, aesKey);
                    fos.write(chunk);
                    remaining -= chunk.length;
                }
            }

            return "收到文件: " + fileName;
        }

        return message;
    }

    public void sendMessage(String msg) throws Exception {
        byte[] encrypted = CryptoUtil.encryptAES(msg, aesKey);
        out.writeInt(encrypted.length);
        out.write(encrypted);
    }

    public void sendFile(File file) throws Exception {
        // 1. 发送文件标识头
        sendMessage("[FILE]");

        // 2. 发送文件名 + 文件大小（加密发送）
        sendMessage(file.getName());
        sendMessage(String.valueOf(file.length()));

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] actualBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, actualBytes, 0, bytesRead);
                byte[] encryptedChunk = CryptoUtil.encryptAESBytes(actualBytes, aesKey);
                out.writeInt(encryptedChunk.length);
                out.write(encryptedChunk);
            }
        }
    }

    private byte[] readEncrypted() throws IOException {
        int len = in.readInt();
        byte[] data = new byte[len];
        in.readFully(data);
        return data;
    }

    public void close() throws IOException {
        socket.close();
        serverSocket.close();
    }
}
