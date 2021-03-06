package network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.ssl.SslHandler
import network.dispatching.Dispatcher
import proto.GameMessageProto
import proto.GenericMessageProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/21/16.
 */


class MessageServer(val addr: InetSocketAddress, val dispatcher: Dispatcher<GenericMessageProto.GenericMessage>) {
    private val future: ChannelFuture
    val bootstrap = ServerBootstrap()

    init {
        val group = NioEventLoopGroup()
        bootstrap.group(group).channel(NioServerSocketChannel::class.java).
                childHandler(MessageServerChannelInitializer(dispatcher))
        bootstrap.option(ChannelOption.SO_REUSEADDR, true)
        future = bootstrap.bind(addr).sync()
    }

    /**
     * Terminate connection
     */
    fun close() {
        future.channel().close()
        bootstrap.group().shutdownGracefully()
    }
}


/**
 * Server response handler (process message from client)
 */
class MessageServerHandler(val dispatcher: Dispatcher<GenericMessageProto.GenericMessage>) : SimpleChannelInboundHandler<GenericMessageProto.GenericMessage>() {
    var response: GenericMessageProto.GenericMessage? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.pipeline().get(SslHandler::class.java).handshakeFuture().addListener { print("Ssl connection established\n") }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?,
                              msg: GenericMessageProto.GenericMessage?) {
        if (msg != null) {
            if (msg.hasGameMessage() && msg.gameMessage.type == GameMessageProto.GameMessage.Type.GAME_END_MESSAGE) {
                println("Recieved End Game Message")
            }
            response = dispatcher.dispatch(msg)
            if (response != null) {
                ctx!!.write(response)
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}


/**
 * Pipeline for protobuf network serialization/deserialization
 */
class MessageServerChannelInitializer(val dispatcher: Dispatcher<GenericMessageProto.GenericMessage>) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?) {
        val pipeline = ch!!.pipeline()
        val ctx = SslContextFactory.serverContext
        pipeline.addLast(ctx.newHandler(ch.alloc()))

        pipeline.addLast(ProtobufVarint32FrameDecoder())
        pipeline.addLast(ProtobufDecoder(
                GenericMessageProto.GenericMessage.getDefaultInstance()))
        pipeline.addLast(ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast(ProtobufEncoder())
        pipeline.addLast(MessageServerHandler(dispatcher))
    }

}