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

    public void close() throws IOException {
        socket.close();
    }
}
