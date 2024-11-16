package io.fqueue;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FQueue extends AbstractQueue<byte[]> {
    private final static Logger logger = LoggerFactory.getLogger(FQueue.class);

    private FSQueue fsQueue = null;
    private Lock lock = new ReentrantLock();

    public FQueue(String path) throws IOException, FileFormatException {
        fsQueue = new FSQueue(path, 1024 * 1024 * 300, 1024 * 1024);
    }

    public FQueue(String path, int logsize, int itemsize) throws IOException, FileFormatException {
        fsQueue = new FSQueue(path, logsize, itemsize);
    }

    @Override
    public Iterator<byte[]> iterator() {
        throw new UnsupportedOperationException("Iterator Unsupported now");
    }

    @Override
    public int size() {
        return fsQueue.getQueueSize();
    }

    @Override
    public boolean offer(byte[] e) {
        try {
            lock.lock();
            fsQueue.add(e, 0, e.length);
            return true;
        } catch (IOException | FileFormatException e1) {
            logger.error(e1.toString(), e1);
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public byte[] peek() {
        throw new UnsupportedOperationException("Peek Unsupported now");
    }

    @Override
    public byte[] poll() {
        try {
            lock.lock();
            return fsQueue.readNextAndRemove();
        } catch (IOException | FileFormatException e) {
            logger.error(e.toString(), e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        if (fsQueue != null) {
            fsQueue.close();
            fsQueue = null;
        }
    }
}
