package fqueue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogIndex {
    private final static Logger logger = LoggerFactory.getLogger(LogIndex.class);

    private final int dbFileLimitLength = 32;
    private RandomAccessFile dbRandFile = null;
    private FileChannel fc;
    private MappedByteBuffer mappedByteBuffer;

    // The positions for this file
    private String magicString = null;
    private int version = -1;
    private int readerPosition = -1;
    private int writerPosition = -1;
    private int readerIndex = -1;
    private int writerIndex = -1;
    private AtomicInteger size = new AtomicInteger();

    public LogIndex(String path) throws IOException, FileFormatException {
        File dbFile = new File(path);

        if (dbFile.exists() == false) {
            dbFile.createNewFile();
            dbRandFile = new RandomAccessFile(dbFile, "rwd");
            dbRandFile.write(LogEntity.MAGIC.getBytes()); // magic
            dbRandFile.writeInt(1); // 8 version
            dbRandFile.writeInt(LogEntity.messageStartPosition); // 12 reader pos
            dbRandFile.writeInt(LogEntity.messageStartPosition); // 16 writer pos
            dbRandFile.writeInt(1); // 20 readerindex
            dbRandFile.writeInt(1); // 24 writerindex
            dbRandFile.writeInt(0); // 28 size
            magicString = LogEntity.MAGIC;
            version = 1;
            readerPosition = LogEntity.messageStartPosition;
            writerPosition = LogEntity.messageStartPosition;
            readerIndex = 1;
            writerIndex = 1;
        } else {
            dbRandFile = new RandomAccessFile(dbFile, "rwd");
            if (dbRandFile.length() < 32) {
                throw new FileFormatException("Index file format error: file length too small");
            }
            byte[] b = new byte[this.dbFileLimitLength];
            dbRandFile.read(b);
            ByteBuffer buffer = ByteBuffer.wrap(b);
            b = new byte[LogEntity.MAGIC.getBytes().length];
            buffer.get(b);
            magicString = new String(b);
            version = buffer.getInt();
            readerPosition = buffer.getInt();
            writerPosition = buffer.getInt();
            readerIndex = buffer.getInt();
            writerIndex = buffer.getInt();
            size.set(buffer.getInt());
        }
        fc = dbRandFile.getChannel();
        mappedByteBuffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, this.dbFileLimitLength);
    }

    public void putWriterPosition(int pos) {
        mappedByteBuffer.position(16);
        mappedByteBuffer.putInt(pos);
        this.writerPosition = pos;
    }

    public void putReaderPosition(int pos) {
        mappedByteBuffer.position(12);
        mappedByteBuffer.putInt(pos);
        this.readerPosition = pos;
    }

    public void putWriterIndex(int index) {
        mappedByteBuffer.position(24);
        mappedByteBuffer.putInt(index);
        this.writerIndex = index;
    }

    public void putReaderIndex(int index) {
        mappedByteBuffer.position(20);
        mappedByteBuffer.putInt(index);
        this.readerIndex = index;
    }

    public void incrementSize() {
        int num = size.incrementAndGet();
        mappedByteBuffer.position(28);
        mappedByteBuffer.putInt(num);
    }

    public void decrementSize() {
        int num = size.decrementAndGet();
        mappedByteBuffer.position(28);
        mappedByteBuffer.putInt(num);
    }

    public String getMagicString() {
        return magicString;
    }

    public int getVersion() {
        return version;
    }

    public int getReaderPosition() {
        return readerPosition;
    }

    public int getWriterPosition() {
        return writerPosition;
    }

    public int getReaderIndex() {
        return readerIndex;
    }

    public int getWriterIndex() {
        return writerIndex;
    }

    public int getSize() {
        return size.get();
    }

    public void close() {
        try {
            mappedByteBuffer.force();
            MappedByteBufferUtil.clean(mappedByteBuffer);
            fc.close();
            dbRandFile.close();
            mappedByteBuffer = null;
            fc = null;
            dbRandFile = null;
        } catch (IOException e) {
            logger.error("Close index file failure: " + e.toString(), e);
        }
    }

    public void force() {
        mappedByteBuffer.force();
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
        sb.append(" size:");
        sb.append(size);
        sb.append(" readerIndex:");
        sb.append(readerIndex);
        sb.append(" writerIndex:");
        sb.append(writerIndex);
        return sb.toString();
    }
}
