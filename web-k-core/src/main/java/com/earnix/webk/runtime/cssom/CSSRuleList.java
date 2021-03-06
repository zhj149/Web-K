package com.earnix.webk.runtime.cssom;

import com.earnix.webk.runtime.web_idl.Exposed;
import com.earnix.webk.runtime.web_idl.Getter;
import com.earnix.webk.runtime.web_idl.Nullable;
import com.earnix.webk.runtime.web_idl.ReadonlyAttribute;
import com.earnix.webk.runtime.web_idl.Unsigned;

/**
 * @author Taras Maslov
 * 11/12/2018
 */
@Exposed(Window.class)
public interface CSSRuleList {
    @Getter
    @Nullable
    CSSRule item(@Unsigned int index);

    @ReadonlyAttribute
    @Unsigned
    int length();
}
