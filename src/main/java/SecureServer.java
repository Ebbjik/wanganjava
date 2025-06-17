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

        // 接收客户端公钥和签名
        String clientPubKeyStr = in.readUTF();
        String clientSignature = in.readUTF();
        PublicKey clientPubKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(clientPubKeyStr)));

        if (!CryptoUtil.verify(clientPubKeyStr, clientSignature, clientPubKey)) {
            throw new SecurityException("客户端身份验证失败");
        }
        System.out.println("客户端身份已验证");

        // 发送服务器公钥和签名
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
        return CryptoUtil.decryptAES(encrypted, aesKey);
    }

    public void close() throws IOException {
        socket.close();
        serverSocket.close();
    }

    public void sendMessage(String msg) throws Exception {
        byte[] encrypted = CryptoUtil.encryptAES(msg, aesKey);
        out.writeInt(encrypted.length);
        out.write(encrypted);
    }
}
