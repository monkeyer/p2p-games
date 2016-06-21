package broker.network

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import proto.GenericMessageProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/21/16.
 */


class MessageServer(val addr: InetSocketAddress) {
    private val future : ChannelFuture
    val bootstrap = ServerBootstrap()
    init{
        val group = NioEventLoopGroup();
        bootstrap.group(group).channel(NioServerSocketChannel::class.java).
                childHandler(MessageServerChannelInitializer())
        bootstrap.option(ChannelOption.SO_REUSEADDR, true)
        future = bootstrap.bind(addr).sync()
    }

    /**
     * Terminate connection
     */
    fun close(){
        bootstrap.group().shutdownGracefully()
        future.channel().closeFuture().sync()
    }
}


/**
 * Server response handler (process message from client)
 */
class MessageServerHandler : SimpleChannelInboundHandler<GenericMessageProto.GenericMessage>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: GenericMessageProto.GenericMessage?) {
        print(msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}


// TODO merge with MessageClientChannelInitializer
/**
 * Pipeline for protobuf network serialization/deserialization
 */
class MessageServerChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?) {
        val pipeline = ch!!.pipeline()
        pipeline.addLast(ProtobufVarint32FrameDecoder())
        pipeline.addLast(ProtobufDecoder(GenericMessageProto.GenericMessage.getDefaultInstance()))
        pipeline.addLast(ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast(ProtobufEncoder())
        pipeline.addLast(MessageServerHandler())
    }

}