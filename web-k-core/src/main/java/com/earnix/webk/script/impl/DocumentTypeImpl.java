package com.earnix.webk.script.impl;

import com.earnix.webk.dom.nodes.DocumentTypeModel;
import com.earnix.webk.swing.BasicPanel;

/**
 * @author Taras Maslov
 * 7/13/2018
 */
public class DocumentTypeImpl extends NodeImpl implements com.earnix.webk.script.whatwg_dom.DocumentType {

    private DocumentTypeModel target;

    public DocumentTypeImpl(DocumentTypeModel target, BasicPanel panel) {
        super(target, panel);
        this.target = target;
    }

    @Override
    public String name() {
        return target.attr("name");
    }

    @Override
    public String publicId() {
        return target.attr("publicId");
    }

    @Override
    public String systemId() {
        return target.attr("systemId");
    }
}