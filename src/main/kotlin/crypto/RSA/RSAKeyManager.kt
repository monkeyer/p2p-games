package crypto.RSA

import crypto.random.secureRandom
import entity.User
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.crypto.AsymmetricBlockCipher
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PublicKeyFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Security
import java.util.*
import javax.xml.bind.DatatypeConverter

/**
 * Created by user on 7/28/16.
 */
val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")

class RSAKeyManager {
    private val userEncodeEngines = mutableMapOf<User, AsymmetricBlockCipher>()
    private val userDecodeEngines = mutableMapOf<User, AsymmetricBlockCipher>()
    private val KEY_LENGTH = 1024
    val engine = PKCS1Encoding(RSAEngine())
    private lateinit var keyPair: KeyPair
    init {
        Security.addProvider(BouncyCastleProvider())
        reset()
    }

    /**
     * Get generated public key(string representation_
     */
    fun getPublicKey(): String {
        val b64encoder = BASE64Encoder()
        return b64encoder.encode(keyPair.public.encoded)
    }

    /**
     * Get private key
     *
     * DISCLAIMER: revealing private key
     * might me a huge vulnerability
     */
    fun getPrivateKey(): String{
        val b64encoder = BASE64Encoder()
        return b64encoder.encode(keyPair.private.encoded)
    }

    /**
     * register public key generated by [getPublicKey]
     * for given user
     * @param user - User to be added
     * @param publicKey - public key of given user
     */
    fun registerUserPublicKey(user: User, publicKey: String){
        val b64decoder = BASE64Decoder()
        val keyParam = PublicKeyFactory.createKey(b64decoder.decodeBuffer(publicKey))
        val cypher: AsymmetricBlockCipher = PKCS1Encoding(RSAEngine())
        cypher.init(true, keyParam)
        userEncodeEngines[user] = cypher
    }

    /**
     * register private key generated by [getPrivateKey]
     * for given user
     * @param user - User to be added
     * @param privateKey - private key of given user
     */
    fun registerUserPrivateKey(user: User, privateKey: String){
        val b64decoder = BASE64Decoder()
        val keyParam = PrivateKeyFactory.createKey(b64decoder.decodeBuffer(privateKey))
        val cypher: AsymmetricBlockCipher = PKCS1Encoding(RSAEngine())
        cypher.init(false, keyParam)
        userDecodeEngines[user] = cypher
    }



    /**
     * encode message with users public key
     */
    fun encodeForUser(user: User, msg: String): String{

        val e = userEncodeEngines[user] ?: throw NoSuchUserException("No engine for user ${user.name}")
        var len = e.inputBlockSize
        val res = StringBuilder()
        val bytes = msg.toByteArray()
        for(i in 0..bytes.size-1 step len){
            len = Math.min(len, bytes.size-i)
            res.append(toHexString(e.processBlock(bytes, i, len)))
        }
        return res.toString()
    }


    /**
     * try to decode message with our own key
     * @param msg - message to decode
     * @throws InvalidCipherTextException  - if message has incorrect format
     */
    fun decodeString(msg: String): String{
        return decodeStringWithEngine(msg, engine)
    }

    /**
     * try to decode message for user whose private key we know
     * @param msg - message to decode
     * @throws InvalidCipherTextException  - if message has incorrect format
     */
    fun decodeForUser(user: User, msg: String): String{
        val engine = userDecodeEngines[user] ?: throw IllegalArgumentException("No private key known for ser ${user.name}")
        return decodeStringWithEngine(msg, engine)
    }


    /**
     * Given string message try to decode it with give block cypher
     *
     * @param msg - string representing encoded message
     * @param engine - decoding cypher
     *
     * @return - decoded string
     */
    private fun decodeStringWithEngine(msg: String, engine: AsymmetricBlockCipher): String{
        var len = engine.inputBlockSize
        val res = StringBuilder()
        val bytes = toByteArray(msg)
        for(i in 0..bytes.size-1 step len){
            len = Math.min(len, bytes.size-i)
            res.append(String(engine.processBlock(bytes, i, len)))
        }
        return res.toString()
    }

    fun toHexString(array: ByteArray): String {
        return DatatypeConverter.printHexBinary(array)
    }

    fun toByteArray(s: String): ByteArray {
        return DatatypeConverter.parseHexBinary(s)
    }

    /**
     * reset private/public keys as well as list of known enginies
     */
    fun reset(){
        val keyGen = KeyPairGenerator.getInstance("RSA", "BC")
        keyGen.initialize(KEY_LENGTH, secureRandom)
        keyPair = keyGen.genKeyPair()
        //TODO - there SHOULD be better convertion
        engine.init(false, PrivateKeyFactory.createKey(keyPair.private.encoded))
        userEncodeEngines.clear()
    }

}