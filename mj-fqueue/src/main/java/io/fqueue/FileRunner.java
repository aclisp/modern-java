package io.fqueue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRunner implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(FileRunner.class);

    private final Queue<String> deleteQueue = new ConcurrentLinkedQueue<String>();
    private final Queue<String> createQueue = new ConcurrentLinkedQueue<String>();

    private int fileLimitLength = 0;
    private LogIndex db = null;
    private volatile boolean keepRunning = true;

    public void addDeleteFile(String path) {
        deleteQueue.add(path);
    }

    public void addCreateFile(String path) {
        createQueue.add(path);
    }

    public FileRunner(LogIndex db, int fileLimitLength) {
        this.db = db;
        this.fileLimitLength = fileLimitLength;
    }

    public void exit() {
        keepRunning = false;
    }

    @Override
    public void run() {
        String deletePath, createPath;
        while (keepRunning) {
            try {
                deletePath = deleteQueue.poll();
                createPath = createQueue.poll();
                if (deletePath == null && createPath == null) {
                    db.force();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                if (deletePath != null) {
                    File delFile = new File(deletePath);
                    delFile.delete();
                }
                if (createPath != null) {
                    try {
                        create(createPath);
                    } catch (IOException e) {
                        logger.error("Pre-create data file failure: " + e.toString(), e);
                    }
                }
            } catch (Throwable e) {
                logger.error("****** FileRunner Error! ****** : " + e.toString(), e);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private boolean create(String path) throws IOException {
        File file = new File(path);
        if (file.exists() == false) {
            if (file.createNewFile() == false) {
                return false;
            }
            RandomAccessFile raFile = new RandomAccessFile(file, "rwd");
            FileChannel fc = raFile.getChannel();
            MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.fileLimitLength);
            mappedByteBuffer.put(LogEntity.MAGIC.getBytes());
            mappedByteBuffer.putInt(1);// 8 version
            mappedByteBuffer.putInt(-1);// 12next fileindex
            mappedByteBuffer.putInt(-2);// 16
            mappedByteBuffer.force();
            MappedByteBufferUtil.clean(mappedByteBuffer);
            fc.close();
            raFile.close();
            return true;
        } else {
            return false;
        }
    }
}
