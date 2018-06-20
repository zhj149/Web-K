package org.xhtmlrenderer.js.whatwg_dom;

import org.xhtmlrenderer.js.web_idl.Dictionary;

/**
 * @author Taras Maslov
 * 6/18/2018
 */
@Dictionary
public interface EventInit {
    boolean bubbles = false;
    boolean cancelable = false;
    boolean composed = false;
}
