import broker.dispatching.EnumDispatcher
import broker.network.MessageClient
import broker.network.MessageServer
import org.apache.log4j.BasicConfigurator
import proto.ChatMessageProto
import proto.GenericMessageProto
import java.net.InetSocketAddress
import java.net.InterfaceAddress

/**
 * Created by user on 6/20/16.
 */


fun main(args : Array<String>) {
    println("Hello, world!")


    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1231)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    val server = MessageServer(a1)
    val client = MessageClient(a2)
//    //sample message
//    //Todo - Protobuf
    val sampleBuilder = ChatMessageProto.ChatMessage.newBuilder()
    sampleBuilder.cahtId = 666
    sampleBuilder.message = "Need more Souls"
    sampleBuilder.user = (ChatMessageProto.User.newBuilder().setAdress(a2.toString()).setName("Mark Geller").build())
    val sample = GenericMessageProto.GenericMessage.newBuilder().
            setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).setChatMessage(sampleBuilder).build()

    val x = EnumDispatcher<GenericMessageProto.GenericMessage, GenericMessageProto.GenericMessage.Type>(GenericMessageProto.GenericMessage.Type::class.java)
    x.dispatch(sample)
    client.send(a1, sample)
    Thread.sleep(1000)
    server.close()
    client.close()

}
