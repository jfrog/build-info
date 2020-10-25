package org.jfrog.build.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author Noam Y. Tenne
 */
public class EolDetectingInputStream extends InputStream {

    private static final byte[] lfBytes = new byte[]{10};
    private static final byte[] crBytes = new byte[]{13};

    private boolean lf;
    private boolean cr;

    private InputStream inputStream;

    public EolDetectingInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        int readByte = inputStream.read();
        if ((!lf || !cr) && readByte != -1) {
            byte[] bytes = {
                    (byte) (readByte >>> 24),
                    (byte) (readByte >>> 16),
                    (byte) (readByte >>> 8),
                    (byte) readByte};
            isByteEol(bytes);
        }
        return readByte;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int readBytes = inputStream.read(b);
        if ((!lf || !cr) && readBytes != -1) {
            isByteEol(b);
        }
        return readBytes;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int readBytes = inputStream.read(b, off, len);
        if ((!lf || !cr) && readBytes != -1) {
            isByteEol(Arrays.copyOfRange(b, off, off + (readBytes - 1)));
        }
        return readBytes;
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    public boolean isLf() {
        return lf;
    }

    public boolean isCr() {
        return cr;
    }

    public String getEol() {
        String eol = "";
        if (cr) {
            eol += "\r";
        }
        if (lf) {
            eol += "\n";
        }
        return eol;
    }

    private void isByteEol(byte[] bytes) {
        if (!lf) {
            lf = isByteEol(bytes, lfBytes);
        }
        if (!cr) {
            cr = isByteEol(bytes, crBytes);
        }
    }

    private boolean isByteEol(byte[] bytesToCheck, byte[] eolType) {
        String strCheck = new String(bytesToCheck, StandardCharsets.UTF_8);
        String strEOL = new String(eolType, StandardCharsets.UTF_8);
        return strCheck.contains(strEOL);
    }
}
