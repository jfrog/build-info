package org.jfrog.build.client;

import org.jfrog.build.api.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Alexei Vainshtein
 */
public class ItemLastModified {
    String uri;
    String lastModified;

    public ItemLastModified(String uri, String lastModified) {
        this.uri = uri;
        this.lastModified = lastModified;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getLastModified() throws ParseException{
        return getLastModified(lastModified);
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "uri: " + uri + "\n" + "lastModified:" + lastModified;
    }

    private long getLastModified(String date) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        Date parse = simpleDateFormat.parse(date);
        return parse.getTime();
    }
}