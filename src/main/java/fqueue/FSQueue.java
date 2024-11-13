package fqueue;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSQueue {
    private final static Logger logger = LoggerFactory.getLogger(FSQueue.class);

    public static final String filePrefix = "fqueue";
    private final int fileLimitLength;
    private final int messageLimitLength;
    private final int messageLimitLengthPerFile;
    private static final String dbName = "icqueue.db";
    private static final String fileSeparator = System.getProperty("file.separator");
    private final String path;
    private final Thread fileRunnerThread;
    private final FileRunner fileRunner;

    // File instances
    private LogIndex db = null;
    private LogEntity writerHandle = null;
    private LogEntity readerHandle = null;

    // File positions
    private int readerIndex = -1;
    private int writerIndex = -1;

    public FSQueue(String path) throws IOException, FileFormatException {
        this(path, 1024 * 1024 * 150, 1024 * 1024);
    }

    /**
     * Initialize a FSQueue in a directory.
     *
     * @param dir                the directory path to store the queue data
     * @param fileLimitLength    the maximum size of one data file, should be no
     *                           more than 2G
     * @param messageLimitLength the maximum size of one message, should be no more
     *                           than fileLimitLength
     * @throws IOException
     */
    public FSQueue(String dir, int fileLimitLength, int messageLimitLength) throws IOException, FileFormatException {
        this.fileLimitLength = fileLimitLength;
        this.messageLimitLength = messageLimitLength;
        this.messageLimitLengthPerFile = fileLimitLength - LogEntity.messageStartPosition - 4;
        File fileDir = new File(dir);
        if (fileDir.exists() == false && fileDir.isDirectory() == false) {
            if (fileDir.mkdirs() == false) {
                throw new IOException("create dir error");
            }
        }
        path = fileDir.getAbsolutePath();
        db = new LogIndex(path + fileSeparator + dbName);
        writerIndex = db.getWriterIndex();
        readerIndex = db.getReaderIndex();
        writerHandle = createLogEntity(db, writerIndex);
        if (readerIndex == writerIndex) {
            readerHandle = writerHandle;
        } else {
            readerHandle = createLogEntity(db, readerIndex);
        }
        fileRunner = new FileRunner(db, fileLimitLength);
        fileRunnerThread = new Thread(fileRunner, "FSQueue" + truncate(dir, 0, 8));
        fileRunnerThread.start();
    }

    private static String truncate(final String str, final int offset, final int maxWidth) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        if (maxWidth < 0) {
            throw new IllegalArgumentException("maxWith cannot be negative");
        }
        if (str == null) {
            return null;
        }
        if (offset > str.length()) {
            return "";
        }
        if (str.length() > maxWidth) {
            final int ix = Math.min(offset + maxWidth, str.length());
            return str.substring(offset, ix);
        }
        return str.substring(offset);
    }

    /**
     * Create or get a data file instance
     */
    private LogEntity createLogEntity(LogIndex db, int fileNumber) throws IOException,
            FileFormatException {
        return new LogEntity(logEntityPath(fileNumber), db, fileNumber, this.fileLimitLength);
    }

    /**
     * Rotate to the next data file instance when the data written size reach
     * fileLimitLength
     */
    private void rotateNextLogWriter() throws IOException, FileFormatException {
        writerIndex = writerIndex + 1;
        writerHandle.putNextFile(writerIndex);
        if (readerHandle != writerHandle) {
            writerHandle.close();
        }
        db.putWriterIndex(writerIndex);
        writerHandle = createLogEntity(db, writerIndex);
        fileRunner.addCreateFile(logEntityPath(writerIndex + 1));
    }

    /**
     * Add the byte array data to this file queue
     */
    public void add(byte[] message, int offset, int length) throws IOException, FileFormatException {
        if (length > messageLimitLength) {
            length = messageLimitLength;
        }
        if (length > messageLimitLengthPerFile) {
            logger.error("****** Message size ({}) too big !!! It must be no more than {} bytes. ******",
                    length, messageLimitLengthPerFile);
            length = messageLimitLengthPerFile;
        }

        short status = writerHandle.write(message, offset, length);
        if (status == LogEntity.WRITEFULL) {
            rotateNextLogWriter();
            status = writerHandle.write(message, offset, length);
        }
        if (status == LogEntity.WRITEFULL) {
            logger.error("****** Unexpected WRITE_FULL !!! ******");
        }
        if (status == LogEntity.WRITESUCCESS) {
            db.incrementSize();
        }
    }

    /**
     * Get and remove the data that was put into the file queue with a FIFO manner
     */
    public byte[] readNextAndRemove() throws IOException, FileFormatException {
        byte[] b = null;
        try {
            b = readerHandle.readNextAndRemove();
        } catch (FileEOFException e) {
            int deleteNum = readerHandle.getCurrentFileNumber();
            int nextfile = readerHandle.getNextFile();
            readerHandle.close();
            fileRunner.addDeleteFile(logEntityPath(deleteNum));
            // Update the position and index for next read
            db.putReaderPosition(LogEntity.messageStartPosition);
            db.putReaderIndex(nextfile);
            if (writerHandle.getCurrentFileNumber() == nextfile) {
                readerHandle = writerHandle;
            } else {
                readerHandle = createLogEntity(db, nextfile);
            }
            try {
                b = readerHandle.readNextAndRemove();
            } catch (FileEOFException e1) {
                logger.error("****** Unexpected FileEOFException !!! ******", e1);
            }
        }
        if (b != null) {
            db.decrementSize();
        }
        return b;
    }

    public void close() {
        readerHandle.close();
        writerHandle.close();
        fileRunner.exit();
        try {
            fileRunnerThread.join();
        } catch (InterruptedException ignored) {
        }
        db.close();
    }

    public int getQueueSize() {
        return db.getSize();
    }

    private String logEntityPath(int fileNumber) {
        return path + fileSeparator + filePrefix + "data_" + fileNumber + ".idb";
    }
}
