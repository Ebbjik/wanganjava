import javax.crypto.*;
import java.security.*;
import java.util.Base64;

public class CryptoUtil {
    /**
     * 生成一个RSA密钥对。
     * 
     * @return 生成的RSA密钥对，包含公钥和私钥。
     * @throws Exception 如果生成密钥对过程中发生错误。
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    /**
     * 使用指定的私钥对数据进行签名。
     * 
     * @param data       要签名的数据。
     * @param privateKey 用于签名的私钥。
     * @return 签名后的数据，Base64编码。
     * @throws Exception 如果签名过程中发生错误。
     */
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data.getBytes());
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    /**
     * 使用指定的公钥验证签名。
     * 
     * @param data      原始数据。
     * @param signature 要验证的签名，Base64编码。
     * @param publicKey 用于验证的公钥。
     * @return 如果签名验证通过则返回true，否则返回false。
     * @throws Exception 如果验证过程中发生错误。
     */
    public static boolean verify(String data, String signature, PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data.getBytes());
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    /**
     * 使用指定的公钥对数据进行RSA加密。
     * 
     * @param data 要加密的数据。
     * @param key  用于加密的公钥。
     * @return 加密后的数据。
     * @throws Exception 如果加密过程中发生错误。
     */
    public static byte[] encryptRSA(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * 使用指定的私钥对数据进行RSA解密。
     * 
     * @param data 要解密的数据。
     * @param key  用于解密的私钥。
     * @return 解密后的数据。
     * @throws Exception 如果解密过程中发生错误。
     */
    public static byte[] decryptRSA(byte[] data, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * 生成一个AES密钥。
     * 
     * @return 生成的AES密钥。
     * @throws Exception 如果生成密钥过程中发生错误。
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(128);
        return gen.generateKey();
    }

    /**
     * 使用指定的AES密钥对数据进行加密。
     * 
     * @param data 要加密的数据。
     * @param key  用于加密的AES密钥。
     * @return 加密后的数据。
     * @throws Exception 如果加密过程中发生错误。
     */
    public static byte[] encryptAES(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data.getBytes());
    }

    /**
     * 使用指定的AES密钥对数据进行解密。
     * 
     * @param data 要解密的数据。
     * @param key  用于解密的AES密钥。
     * @return 解密后的数据。
     * @throws Exception 如果解密过程中发生错误。
     */
    public static String decryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(data));
    }
}
