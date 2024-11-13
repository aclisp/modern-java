package fqueue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEntity {
    private final static Logger logger = LoggerFactory.getLogger(LogEntity.class);

    public static final byte WRITESUCCESS = 1;
    public static final byte WRITEFAILURE = 2;
    public static final byte WRITEFULL = 3;
    public static final String MAGIC = "FQueuefs";
    public static int messageStartPosition = 20;

    private final Thread syncer;
    private File file;
    private RandomAccessFile raFile;
    private FileChannel fc;
    private MappedByteBuffer mappedByteBuffer;
    private int fileLimitLength = 1024 * 1024 * 40;
    private LogIndex db = null;

    // The positions for this file
    private String magicString = null;
    private int version = 1;
    private int readerPosition = -1;
    private int writerPosition = -1;
    private int nextFile = -1;
    private int endPosition = -1;
    private int currentFileNumber = -1;

    public LogEntity(String path, LogIndex db, int fileNumber,
            int fileLimitLength) throws IOException, FileFormatException {
        this.currentFileNumber = fileNumber;
        this.fileLimitLength = fileLimitLength;
        this.db = db;
        file = new File(path);
        if (file.exists() == false) {
            createLogEntity();
        } else {
            raFile = new RandomAccessFile(file, "rwd");
            if (raFile.length() < LogEntity.messageStartPosition) {
                throw new FileFormatException(
                        "data file format error: file length(" + raFile.length() + ") too small"
                                + LogEntity.messageStartPosition);
            }
            fc = raFile.getChannel();
            mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.fileLimitLength);
            // magicString
            byte[] b = new byte[8];
            mappedByteBuffer.get(b);
            magicString = new String(b);
            if (magicString.equals(MAGIC) == false) {
                throw new FileFormatException(
                        "data file format error: MAGIC header(" + magicString + ") is NOT " + MAGIC);
            }
            // version
            version = mappedByteBuffer.getInt();
            // nextfile
            nextFile = mappedByteBuffer.getInt();
            endPosition = mappedByteBuffer.getInt();
            // The file is not full
            if (endPosition == -1) {
                this.writerPosition = db.getWriterPosition();
            } else if (endPosition == -2) { // Pre-allocated file
                this.writerPosition = LogEntity.messageStartPosition;
                db.putWriterPosition(this.writerPosition);
                mappedByteBuffer.position(16);
                mappedByteBuffer.putInt(-1);
                this.endPosition = -1;
            } else {
                this.writerPosition = endPosition;
            }
            if (db.getReaderIndex() == this.currentFileNumber) {
                this.readerPosition = db.getReaderPosition();
            } else {
                this.readerPosition = LogEntity.messageStartPosition;
            }
        }
        syncer = new Thread(new Sync(), "FQLogSyncer" + fileNumber);
        syncer.start();
    }

    private boolean createLogEntity() throws IOException {
        if (file.createNewFile() == false) {
            return false;
        }
        raFile = new RandomAccessFile(file, "rwd");
        fc = raFile.getChannel();
        mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.fileLimitLength);
        mappedByteBuffer.put(MAGIC.getBytes());
        mappedByteBuffer.putInt(version); // 8 version
        mappedByteBuffer.putInt(nextFile); // 12 next fileindex
        mappedByteBuffer.putInt(endPosition); // 16
        mappedByteBuffer.force();
        this.magicString = MAGIC;
        this.writerPosition = LogEntity.messageStartPosition;
        this.readerPosition = LogEntity.messageStartPosition;
        db.putWriterPosition(this.writerPosition);
        return true;
    }

    public class Sync implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mappedByteBuffer != null) {
                    try {
                        mappedByteBuffer.force();
                    } catch (Throwable e) {
                        logger.error("****** LogEntity Sync Error! ****** : " + e.toString(), e);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void close() {
        try {
            if (mappedByteBuffer == null) {
                return;
            }
            mappedByteBuffer.force();
            MappedByteBufferUtil.clean(mappedByteBuffer);
            mappedByteBuffer = null;
            fc.close();
            raFile.close();
        } catch (IOException e) {
            logger.error("Close data file failure: " + e.toString(), e);
        }
    }

    public boolean isFull(int increment) {
        // confirm if the file is full
        if (this.fileLimitLength < this.writerPosition + increment) {
            return true;
        }
        return false;
    }

    /**
     * write next File number id.
     */
    public void putNextFile(int number) {
        mappedByteBuffer.position(12);
        mappedByteBuffer.putInt(number);
        this.nextFile = number;
    }

    public int getCurrentFileNumber() {
        return this.currentFileNumber;
    }

    public int getNextFile() {
        return this.nextFile;
    }

    private void putWriterPosition(int pos) {
        db.putWriterPosition(pos);
    }

    private void putReaderPosition(int pos) {
        db.putReaderPosition(pos);
    }

    public byte write(byte[] log, int offset, int length) {
        int increment = length + 4;
        if (isFull(increment)) {
            mappedByteBuffer.position(16);
            mappedByteBuffer.putInt(this.writerPosition);
            this.endPosition = this.writerPosition;
            return WRITEFULL;
        }
        mappedByteBuffer.position(this.writerPosition);
        mappedByteBuffer.putInt(length);
        mappedByteBuffer.put(log, offset, length);
        this.writerPosition += increment;
        putWriterPosition(this.writerPosition);
        return WRITESUCCESS;
    }

    public byte[] readNextAndRemove() throws FileEOFException {
        if (this.endPosition != -1 && this.readerPosition >= this.endPosition) {
            throw new FileEOFException("file eof");
        }
        // readerPosition must be less than writerPosition
        if (this.readerPosition >= this.writerPosition) {
            return null;
        }
        mappedByteBuffer.position(this.readerPosition);
        int length = mappedByteBuffer.getInt();
        byte[] b = new byte[length];
        this.readerPosition += length + 4;
        mappedByteBuffer.get(b);
        putReaderPosition(this.readerPosition);
        return b;
    }

    public String headerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(" magicString:");
        sb.append(magicString);
        sb.append(" version:");
        sb.append(version);
        sb.append(" readerPosition:");
        sb.append(readerPosition);
        sb.append(" writerPosition:");
        sb.append(writerPosition);
        sb.append(" nextFile:");
        sb.append(nextFile);
        sb.append(" endPosition:");
        sb.append(endPosition);
        sb.append(" currentFileNumber:");
        sb.append(currentFileNumber);
        return sb.toString();
    }
}
