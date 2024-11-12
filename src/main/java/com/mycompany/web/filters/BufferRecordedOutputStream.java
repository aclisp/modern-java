package com.mycompany.web.filters;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;

public class BufferRecordedOutputStream extends FilterOutputStream {

    private ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private int maxCount = 250;
    private long srcCount;
    private Headers responseHeaders;

    public BufferRecordedOutputStream(Headers responseHeaders, OutputStream out) {
        super(out);
        this.responseHeaders = responseHeaders;
    }

    private boolean canRecord() {
        String contentType = responseHeaders.getFirst("Content-Type");
        String contentEncoding = responseHeaders.getFirst("Content-Encoding");

        return contentEncoding == null
                && "application/json".equals(contentType)
                && buf.size() < maxCount;
    }

    private int bytesCanRecord(int length) {
        int leftSpace = maxCount - buf.size();
        return Math.min(leftSpace, length);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (canRecord())
            buf.write(b, 0, bytesCanRecord(b.length));

        out.write(b);
        srcCount += b.length;
    }

    @Override
    public void write(int b) throws IOException {
        if (canRecord())
            buf.write(b);

        out.write(b);
        srcCount += 1;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (canRecord())
            buf.write(b, off, bytesCanRecord(len));

        out.write(b, off, len);
        srcCount += len;
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
