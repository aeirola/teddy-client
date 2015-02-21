package fi.iki.aeirola.teddyclientlib.utils;

/**
 * Created by Axel on 19.2.2015.
 */
public class IdGenerator {
    private long id = 0L;

    protected void increment() {
        id = id + 1 % Long.MAX_VALUE;
    }

    public long get() {
        increment();
        return id;
    }
}
