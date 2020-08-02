package NIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class NIOServer implements Runnable {
    private final ServerSocketChannel server;
    private final Selector selector;

    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("server started");
            while (server.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        System.out.println("client accepted");
                        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
                        channel.configureBlocking(false);
                        SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        newKey.attach(new ChannelHandler(channel));
                    }
                    if (key.isReadable() || key.isWritable()) {
                        ChannelHandler handler = (ChannelHandler) key.attachment();
                        try {
                            handler.dispatch();
                        } catch (IOException e) {
                            handler.dispose();
                            System.out.println("Client disconnected");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }
}
