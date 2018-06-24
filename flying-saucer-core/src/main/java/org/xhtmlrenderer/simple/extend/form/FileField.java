/*
 * {{{ header & license
 * Copyright (c) 2007 Sean Bright
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.simple.extend.form;

import javax.swing.JComponent;

import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.XhtmlForm;

class FileField extends InputField {

    public FileField(Element e, XhtmlForm form, LayoutContext context, BlockBox box)
    {
    public FileField(org.jsoup.nodes.Element e, XhtmlForm form, LayoutContext context, BlockBox box) {
        super(e, form, context, box);
    }

    @Override
    public JComponent create()
    {
        return (JComponent) SwingComponentFactory.getInstance().createFileInputComponent(this);
    }

    @Override
    protected void applyOriginalState()
    {
        // This is always the default, since you can't set a default
        // value for this in the HTML
        FileInputComponent com = (FileInputComponent) getComponent();
        com.setFilePath("");
    }

    @Override
    protected String[] getFieldValues()
    {
        FileInputComponent com = (FileInputComponent) getComponent();
        return new String[] { com.getFilePath() };
    }

}
