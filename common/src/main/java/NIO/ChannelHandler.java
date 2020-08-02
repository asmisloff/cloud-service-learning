package NIO;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ChannelHandler {

    private Request req;
    private long header;
    private RandomAccessFile file;
    private final SocketChannel channel;
    private final ByteBuffer bb;

    public ChannelHandler(SocketChannel channel) {
        req = Request.NONE;
        this.channel = channel;
        header = 0;
        bb = ByteBuffer.allocate(4096);
    }

    public void reset() throws IOException {
        file.close();
        bb.clear();
        req = Request.NONE;
        header = 0;
    }

    public void dispose() {
        try {
            file.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispatch() throws IOException {
        switch (req) {
            case NONE:
                channelRead();
                tryParseRequest();
                break;
            case DOWNLOAD:
                executeDownload();
                break;
            case UPLOAD:
                executeUpload();
                break;
        }
    }

    private void tryParseRequest() throws IOException {
        if (header == 0) {
            if (bb.position() < Long.SIZE / 8) {
                return;
            }
            bb.flip();
            header = bb.getLong();
            bb.compact();
        }

        if (bb.position() < header) {
            return;
        }

        String s = new String(bb.array(), 0, (int)header, StandardCharsets.UTF_8);
        bb.clear();

        if (s.startsWith(Request.DOWNLOAD.value())) {
            req = Request.DOWNLOAD;
            String path = s.substring(Request.DOWNLOAD.value().length());
            setupForDownload(path);
        } else if (s.startsWith(Request.UPLOAD.value())) {
            req = Request.UPLOAD;
            String path = s.substring(Request.UPLOAD.value().length());
            setupForUpload(path);
        } else {
            bb.putLong(-1);
            bb.flip();
            channel.write(bb);
            reset();
            System.out.printf("Unknown request -- %s\n", s);
        }
    }

    private void channelRead() throws IOException {
        int qty = channel.read(bb);
        if (qty == -1) {
            throw new IOException("Ошибка соединения");
        }
    }

    private void setupForDownload(String path) {
        System.out.printf("Setup for download file: \"%s\"\n", path);
        bb.clear();
        try {
            if (file != null) {
                file.close();
            }
            file = new RandomAccessFile(path, "r");
            bb.putLong(file.length());
            bb.flip();
            channel.write(bb);
            executeDownload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupForUpload(String path) {
        System.out.printf("Setup for upload file: \"%s\"\n", path);
        bb.clear();
        try {
            if (file != null) {
                file.close();
            }
            file = new RandomAccessFile(path, "rw");
            bb.putLong(0);
            bb.flip();
            channel.write(bb);
            bb.clear();
            header = Long.MIN_VALUE;
            executeUpload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeDownload() {
        try {
            bb.compact();
            int cnt = file.getChannel().read(bb);
            if (bb.position() == 0 && cnt == -1) {
                System.out.println("Download -- executed");
                reset();
            } else {
                bb.flip();
                channel.write(bb);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeUpload() throws IOException {
        channelRead();
        if (header == Long.MIN_VALUE) {
            if (bb.position() < Long.SIZE / 8) {
                return;
            }
            bb.flip();
            header = bb.getLong();
            bb.compact();
        }

        bb.flip();
        header -= file.getChannel().write(bb);
        bb.compact();
        if (header == 0) {
            bb.putLong(0);
            bb.flip();
            channel.write(bb);
            reset();
            System.out.println("File uploaded");
        }
    }
}
