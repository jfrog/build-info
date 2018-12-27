package org.jfrog.build.extractor.executor;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CommandResults {

    private String res;
    private String err;
    private int exitValue;

    public boolean isOk() {
        return exitValue == 0;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public int getExitValue() {
        return exitValue;
    }

    public void setExitValue(int exitValue) {
        this.exitValue = exitValue;
    }
}
