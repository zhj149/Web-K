package com.earnix.webk.runtime.whatwg_dom.impl;

/**
 * Text utils to ease testing
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class TextUtil {
    public static String stripNewlines(String text) {
        text = text.replaceAll("\\r?\\n\\s*", "");
        return text;
    }
}