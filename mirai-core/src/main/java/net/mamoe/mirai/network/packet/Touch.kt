package net.mamoe.mirai.network.packet

import net.mamoe.mirai.network.Protocol
import net.mamoe.mirai.utils.*
import java.io.DataInputStream
import java.io.IOException

/**
 * A packet received when logging in, used to redirect server address
 *
 * @see net.mamoe.mirai.network.packet.client.login.ClientServerRedirectionPacket
 * @see net.mamoe.mirai.network.packet.client.login.ClientPasswordSubmissionPacket
 *
 * @author Him188moe
 */
class ServerTouchResponsePacket(inputStream: DataInputStream) : ServerPacket(inputStream) {
    var serverIP: String? = null;

    var loginTime: Int = 0
    lateinit var loginIP: String
    lateinit var token0825: ByteArray
    lateinit var tgtgtKey: ByteArray

    enum class Type {
        TYPE_08_25_31_01,
        TYPE_08_25_31_02,
    }

    @ExperimentalUnsignedTypes
    override fun decode() {
        when (val id = input.readByte().toUByte().toInt()) {
            0xFE -> {
                input.skip(94)
                serverIP = input.readIP()
            }
            0x00 -> {
                input.skip(4)
                token0825 = input.readNBytes(56)
                input.skip(6)

                loginTime = input.readInt()
                loginIP = input.readIP()
                tgtgtKey = getRandomKey(16)
            }

            else -> {
                throw IllegalStateException(arrayOf(id.toUByte()).toUByteArray().toUHexString())
            }
        }
    }
}

class ServerTouchResponsePacketEncrypted(private val type: ServerTouchResponsePacket.Type, inputStream: DataInputStream) : ServerPacket(inputStream) {
    override fun decode() {

    }

    @ExperimentalUnsignedTypes
    fun decrypt(): ServerTouchResponsePacket {
        input.skip(7)
        var bytes = input.readAllBytes();
        bytes = bytes.copyOfRange(0, bytes.size - 1);
        println(bytes.toUByteArray().toUHexString())

        return ServerTouchResponsePacket(DataInputStream(TEACryptor.decrypt(bytes, when (type) {
            ServerTouchResponsePacket.Type.TYPE_08_25_31_02 -> Protocol.redirectionKey.hexToBytes()
            ServerTouchResponsePacket.Type.TYPE_08_25_31_01 -> Protocol._0825key.hexToBytes()
        }).inputStream()));
    }
}

/**
 * The packet to touch server, that is, to start the connection to the server.
 *
 * @author Him188moe
 */
@ExperimentalUnsignedTypes
@PacketId("08 25 31 01")
class ClientTouchPacket(val qq: Int, val serverIp: String) : ClientPacket() {
    @ExperimentalUnsignedTypes
    @Throws(IOException::class)
    override fun encode() {
        this.writeQQ(qq)
        this.writeHex(Protocol.fixVer)
        this.writeHex(Protocol._0825key)

        this.write(TEACryptor.CRYPTOR_0825KEY.encrypt(object : ByteArrayDataOutputStream() {
            @Throws(IOException::class)
            override fun toByteArray(): ByteArray {
                this.writeHex(Protocol._0825data0)
                this.writeHex(Protocol._0825data2)
                this.writeQQ(qq)
                this.writeHex("00 00 00 00 03 09 00 08 00 01")
                this.writeIP(serverIp);
                this.writeHex("00 02 00 36 00 12 00 02 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 14 00 1D 01 02 00 19")
                this.writeHex(Protocol.publicKey)
                println(super.toUByteArray().toUHexString())
                return super.toByteArray()
            }
        }.toByteArray()))
    }
}

/**
 * Server redirection (0825 response)
 *
 * @author Him188moe
 */
@ExperimentalUnsignedTypes
@PacketId("08 25 31 02")
class ClientServerRedirectionPacket(private val serverIP: String, private val qq: Int) : ClientPacket() {
    @ExperimentalUnsignedTypes
    override fun encode() {
        this.writeQQ(qq)
        this.writeHex(Protocol.fixVer)
        this.writeHex(Protocol.redirectionKey)


        this.write(TEACryptor.encrypt(object : ByteArrayDataOutputStream() {
            @Throws(IOException::class)
            override fun toByteArray(): ByteArray {
                this.writeHex(Protocol._0825data0)
                this.writeHex(Protocol._0825data2)
                this.writeQQ(qq)
                this.writeHex("00 01 00 00 03 09 00 0C 00 01")
                this.writeIP(serverIP)
                this.writeHex("01 6F A1 58 22 01 00 36 00 12 00 02 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 14 00 1D 01 03 00 19")
                this.writeHex(Protocol.publicKey)
                return super.toByteArray()
            }
        }.toByteArray(), Protocol.redirectionKey.hexToBytes()))
    }
}