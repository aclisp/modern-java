package com.mycompany.web.filters;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.Headers;

public class BufferRecordedInputStream extends FilterInputStream {

    private ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private int maxCount = 250;
    private long srcCount;
    private Headers requestHeaders;

    public BufferRecordedInputStream(Headers requestHeaders, InputStream in) {
        super(in);
        this.requestHeaders = requestHeaders;
    }

    private boolean canRecord() {
        String contentType = requestHeaders.getFirst("Content-Type");
        String contentEncoding = requestHeaders.getFirst("Content-Encoding");

        return contentEncoding == null
                && "application/json".equals(contentType)
                && buf.size() < maxCount;
    }

    private int bytesCanRecord(int length) {
        int leftSpace = maxCount - buf.size();
        return Math.min(leftSpace, length);
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            srcCount += 1;
            if (canRecord())
                buf.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int n = in.read(b);
        if (n != -1) {
            srcCount += n;
            if (canRecord())
                buf.write(b, 0, bytesCanRecord(n));
        }
        return n;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n != -1) {
            srcCount += n;
            if (canRecord())
                buf.write(b, off, bytesCanRecord(n));
        }
        return n;
    }

    @Override
    public String toString() {
        String result = buf.toString();
        long overflow = srcCount - buf.size();
        if (overflow > 0) {
            result += "...(omitted " + overflow + " bytes)";
        }
        return result;
    }
}
