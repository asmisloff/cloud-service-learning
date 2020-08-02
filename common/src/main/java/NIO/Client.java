package NIO;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {

    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private final int userID;
    private final String clientFilesPath;

    public Client(int id) {
        userID = id;
        clientFilesPath = "./common/src/main/resources/user" + userID;
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Path path = Paths.get(clientFilesPath);
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch(
        IOException e)

        {
            e.printStackTrace();
        }
    }

    private boolean upload(String path) throws IOException {
        byte[] req = (Request.UPLOAD.value() + path).getBytes(StandardCharsets.UTF_8);
        os.writeLong(req.length);
        os.write(req);
        System.out.println("Request (upload) is sent, waiting for response");
        long response = is.readLong();
        System.out.printf("Response received: %d\n", response);
        if (response == 0) {
            File file = new File(clientFilesPath + "/" + path);
            byte[] buffer = new byte[4096];

            os.writeLong(file.length());
            try (FileInputStream fis = new FileInputStream(file)) {
                while (fis.available() > 0) {
                    int cnt = fis.read(buffer, 0, buffer.length);
                    os.write(buffer, 0, cnt);
                }
                System.out.println("File uploaded");
                response = is.readLong();
                System.out.printf("Response received: %d\n", response);
                return true;
            }
        }
        return false;
    }

    private void download(String path) throws IOException {
        byte[] req = (Request.DOWNLOAD.value() + path).getBytes(StandardCharsets.UTF_8);
        os.writeLong(req.length);
        os.write(req);
        System.out.println("Request (download) is sent, waiting for response");
        long response = is.readLong();
        System.out.printf("Response received: %d\n", response);

        if (response > 0) {
            File file = new File(clientFilesPath + "/" + path);
            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] buffer = new byte[4096];
            try (FileOutputStream fos = new FileOutputStream(file)) {
                while (response > 0) {
                    int cnt = is.read(buffer);
                    response -= cnt;
                    fos.write(buffer, 0, cnt);
                }
                System.out.println("File downloaded. responce = " + response);
            }
        }
    }

    public static void main(String[] args) {
        new Thread(() -> {
            Client c = new Client(1);
            try {
                c.upload("webinar_96561461851_0.MP4");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            Client c = new Client(2);
            try {
                c.download( "2.txt");
                c.upload("детская_отпрака.dwg");
                c.download( "1.txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }
}