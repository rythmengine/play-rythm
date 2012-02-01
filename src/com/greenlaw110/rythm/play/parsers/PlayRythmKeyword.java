package com.greenlaw110.rythm.play.parsers;

import com.greenlaw110.rythm.spi.IKeyword;

/**
 * Created by IntelliJ IDEA.
 * User: luog
 * Date: 29/01/12
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
public enum PlayRythmKeyword implements IKeyword {
    /**
     * Absolute reverse url lookup
     */
    AURL,
    /**
     * used for url reverse lookup
     */
    URL;

    private final String s;
    private PlayRythmKeyword() {
        this.s = name().toLowerCase();
    }
    private PlayRythmKeyword(String s) {
        this.s = (null == s) ? name().toLowerCase() : s;
    }

    @Override
    public String toString() {
        return s;
    }

    @Override
    public boolean isRegexp() {
        return !s.equals(name().toLowerCase());
    }
}
