package fqueue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileSystemUtils;

import com.mycompany.util.JUL;
import com.mycompany.util.RandomStringUtils;

public class FQueueTest {
    @BeforeAll
    static void setup() {
        JUL.initLogging();
    }

    @Test
    void testCreateAndReopen() throws IOException, FileFormatException {
        String dir = "./fqueue-test";
        FQueue fQueue;
        for (int i = 0; i < 3; i++) {
            fQueue = new FQueue(dir, 100, 76);
            fQueue.close();

            byte[] index = Files.readAllBytes(Path.of(dir, "icqueue.db"));
            assertEquals(LogEntity.MAGIC, new String(index, 0, 8));
            assertEquals(1, ByteBuffer.wrap(index, 8, 4).getInt());
            assertEquals(20, ByteBuffer.wrap(index, 12, 4).getInt());
            assertEquals(20, ByteBuffer.wrap(index, 16, 4).getInt());
            assertEquals(1, ByteBuffer.wrap(index, 20, 4).getInt());
            assertEquals(1, ByteBuffer.wrap(index, 24, 4).getInt());
            assertEquals(0, ByteBuffer.wrap(index, 28, 4).getInt());
        }
        FileSystemUtils.deleteRecursively(Path.of(dir));
    }

    @Test
    void testAddAndPoll() throws IOException, FileFormatException {
        String dir = "./fqueue-test";
        String str = "1234567890223456789032345678904234567890523456789062345678907234567890823456789092345678900234567890";
        int itemSize = 76;
        FQueue fQueue = new FQueue(dir, 100, itemSize);
        for (int i = 0; i < 10; i++) {
            fQueue.add(str.getBytes());
        }
        assertEquals(10, fQueue.size());
        byte[] message;
        int count = 0;
        while (true) {
            message = fQueue.poll();
            if (message == null) {
                break;
            }
            count++;
            assertEquals(str.substring(0, itemSize), new String(message));
        }
        assertEquals(10, count);
        fQueue.close();
        FileSystemUtils.deleteRecursively(Path.of(dir));
    }

    @Test
    void testAddAndPoll2() throws IOException, FileFormatException, InterruptedException {
        String dir = "./fqueue-test";
        List<String> messages = new ArrayList<>();
        FQueue fQueue = new FQueue(dir, 100, 76);
        for (int i = 0; i < 10; i++) {
            String message = RandomStringUtils.random(
                    ThreadLocalRandom.current().nextInt(4, 50));
            messages.add(message);
            fQueue.add(message.getBytes());
        }
        assertEquals(10, fQueue.size());
        byte[] message;
        int count = 0;
        while (true) {
            message = fQueue.poll();
            if (message == null) {
                break;
            }
            assertEquals(messages.get(count), new String(message));
            count++;
        }
        assertEquals(10, count);
        fQueue.close();
        FileSystemUtils.deleteRecursively(Path.of(dir));
    }
}
