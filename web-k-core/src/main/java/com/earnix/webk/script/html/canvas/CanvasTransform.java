package com.earnix.webk.script.html.canvas;

import com.earnix.webk.script.geom.DOMMatrix;
import com.earnix.webk.script.geom.DOMMatrix2DInit;
import com.earnix.webk.script.web_idl.Mixin;
import com.earnix.webk.script.web_idl.NewObject;
import com.earnix.webk.script.web_idl.Optional;
import com.earnix.webk.script.web_idl.Unrestricted;

/**
 * @author Taras Maslov
 * 6/20/2018
 */
@Mixin
public interface CanvasTransform {
    // transformations (default transform is the identity matrix)
    void scale(@Unrestricted double x, @Unrestricted double y);

    void rotate(@Unrestricted double angle);

    void translate(@Unrestricted double x, @Unrestricted double y);

    void transform(@Unrestricted double a, @Unrestricted double b, @Unrestricted double c, @Unrestricted double d, @Unrestricted double e, @Unrestricted double f);

    @NewObject
    DOMMatrix getTransform();

    void setTransform(@Unrestricted double a, @Unrestricted double b, @Unrestricted double c, @Unrestricted double d, @Unrestricted double e, @Unrestricted double f);

    void setTransform(@Optional DOMMatrix2DInit transform);

    void resetTransform();
}