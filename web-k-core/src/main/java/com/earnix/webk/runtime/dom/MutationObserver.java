package com.earnix.webk.runtime.dom;

import com.earnix.webk.runtime.web_idl.Constructor;
import com.earnix.webk.runtime.web_idl.Exposed;
import com.earnix.webk.runtime.web_idl.Optional;
import com.earnix.webk.runtime.web_idl.Sequence;

/**
 * @author Taras Maslov
 * 6/21/2018
 */
@Exposed(Window.class)
@Constructor
public interface MutationObserver {
    void constructor(MutationCallback callback);

    void observe(Node target, @Optional MutationObserverInit options);

    void disconnect();

    Sequence<MutationRecord> takeRecords();
}
