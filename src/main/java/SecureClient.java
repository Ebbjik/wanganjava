import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.SecretKey;

public class SecureClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private SecretKey aesKey;

    public SecureClient(String host, int port) throws Exception {
        connect(host, port);
    }

    private void connect(String host, int port) throws Exception {
        KeyPair keyPair = CryptoUtil.generateRSAKeyPair();
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        // 发送客户端公钥和签名
        String pubKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String signature = CryptoUtil.sign(pubKeyStr, keyPair.getPrivate());
        out.writeUTF(pubKeyStr);
        out.writeUTF(signature);

        // 接收服务器公钥和签名
        String serverPubKeyStr = in.readUTF();
        String serverSignature = in.readUTF();

        PublicKey serverPubKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(serverPubKeyStr)));

        if (!CryptoUtil.verify(serverPubKeyStr, serverSignature, serverPubKey)) {
            throw new SecurityException("服务器身份验证失败");
        }

        // 使用服务器公钥加密 AES 密钥并发送
        aesKey = CryptoUtil.generateAESKey();
        byte[] encryptedAesKey = CryptoUtil.encryptRSA(aesKey.getEncoded(), serverPubKey);
        out.writeInt(encryptedAesKey.length);
        out.write(encryptedAesKey);

        System.out.println("连接成功，通信已加密");
    }

    public void sendMessage(String msg) throws Exception {
        byte[] encrypted = CryptoUtil.encryptAES(msg, aesKey);
        out.writeInt(encrypted.length);
        out.write(encrypted);
    }

    public String receiveMessage() throws Exception {
        int length = in.readInt();
        byte[] encrypted = new byte[length];
        in.readFully(encrypted);
        String message = CryptoUtil.decryptAES(encrypted, aesKey);

        // 文件接收逻辑
        if (message.equals("[FILE]")) {
            String fileName = CryptoUtil.decryptAES(readEncrypted(), aesKey);
            long fileSize = Long.parseLong(CryptoUtil.decryptAES(readEncrypted(), aesKey));

            File receivedFile = new File("received_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
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

    public void sendFile(File file) throws Exception {
        // 1. 发送文件标识头
        sendMessage("[FILE]");

        // 2. 发送文件名和文件大小（加密）
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
    }
}
