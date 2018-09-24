/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
package org.xhtmlrenderer.simple.extend;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.xhtmlrenderer.dom.nodes.Element;
import org.xhtmlrenderer.dom.nodes.TextNode;

import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.form.FormField;
import org.xhtmlrenderer.simple.extend.form.DefaultFormFieldFactory;
import org.xhtmlrenderer.simple.extend.form.FormFieldFactory;
import org.xhtmlrenderer.util.XRLog;

/**
 * Represents a form object
 *
 * @author Torbjoern Gannholm
 * @author Sean Bright
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class XhtmlForm {
    public static FormFieldFactory fieldFactory = new DefaultFormFieldFactory();
    private static final String FS_DEFAULT_GROUP = "__fs_default_group_";
    private static int defaultGroupCount = 1;

    
    UserAgentCallback userAgentCallback;
    LinkedHashMap<Element, FormField> componentCache;
    HashMap<String, ButtonGroupWrapper> buttonGroups;
    Element parentFormElement;
    FormSubmissionListener formSubmissionListener;


    public static void setFieldFactory(FormFieldFactory fieldFactory) {
        XhtmlForm.fieldFactory = fieldFactory;
    }

    public XhtmlForm(UserAgentCallback uac, Element e, FormSubmissionListener fsListener) {
        userAgentCallback = uac;
        buttonGroups = new HashMap<>();
        componentCache = new LinkedHashMap<>();
        parentFormElement = e;
        formSubmissionListener = fsListener;
    }

    public XhtmlForm(UserAgentCallback uac, Element e) {
        this(uac, e, new DefaultFormSubmissionListener());
    }

    public UserAgentCallback getUserAgentCallback() {
        return userAgentCallback;
    }

    public void addButtonToGroup(String groupName, AbstractButton button) {
        if (groupName == null) {
            groupName = createNewDefaultGroupName();
        }

        ButtonGroupWrapper group = buttonGroups.get(groupName);

        if (group == null) {
            group = new ButtonGroupWrapper();

            buttonGroups.put(groupName, group);
        }

        group.add(button);
    }

    private static String createNewDefaultGroupName() {
        return FS_DEFAULT_GROUP + ++defaultGroupCount;
    }

    private static boolean isFormField(Element e) {
        String nodeName = e.nodeName();

        if (nodeName.equals("input") || nodeName.equals("select") || nodeName.equals("textarea")) {
            return true;
        }

        return false;
    }

    public FormField addComponent(Element e, LayoutContext context, BlockBox box) {
        FormField field;

        if (componentCache.containsKey(e)) {
            field = componentCache.get(e);
        } else {
            if (!isFormField(e)) {
                return null;
            }

            field = fieldFactory.create(this, context, box);

            if (field == null) {
                XRLog.layout("Unknown field type: " + e.nodeName());

                return null;
            }

            componentCache.put(e, field);
        }

        return field;
    }

    public void reset() {
        Iterator buttonGroups = this.buttonGroups.values().iterator();
        while (buttonGroups.hasNext()) {
            ((ButtonGroupWrapper) buttonGroups.next()).clearSelection();
        }

        Iterator fields = componentCache.values().iterator();
        while (fields.hasNext()) {
            ((FormField) fields.next()).reset();
        }
    }

    public void submit(JComponent source) {
        // If we don't have a <form> to tell us what to do, don't
        // do anything.
        if (parentFormElement == null) {
            return;
        }

        StringBuilder data = new StringBuilder();
        String action = parentFormElement.attr("action");
        data.append(action).append("?");
        Iterator fields = componentCache.entrySet().iterator();
        boolean first = true;
        boolean valid = true;
        while (fields.hasNext()) {
            Map.Entry entry = (Map.Entry) fields.next();

            FormField field = (FormField) entry.getValue();

            if (!field.isValid()) {
                valid = false;
            } else {

                if (field.includeInSubmission(source)) {
                    String[] dataStrings = field.getFormDataStrings();

                    for (int i = 0; i < dataStrings.length; i++) {
                        if (!first) {
                            data.append('&');
                        }

                        data.append(dataStrings[i]);
                        first = false;
                    }
                }
            }
        }

        if (valid && formSubmissionListener != null) formSubmissionListener.submit(data.toString());
    }

    public static String collectText(Element e) {
        StringBuilder result = new StringBuilder();
        if (e.childNodeSize() > 0) {
            org.xhtmlrenderer.dom.nodes.Node node = e.childNodeSize() > 0 ? e.childNode(0) : null;

            do {
//                short nodeType = node.getNodeType();
                if (node instanceof TextNode) {
                    TextNode text = (TextNode) node;
                    result.append(text.getWholeText());
                }
            } while ((node = node.nextSibling()) != null);
        }
        return result.toString().trim();
    }

    private static class ButtonGroupWrapper {
        private ButtonGroup group;
        private AbstractButton dummy;

        public ButtonGroupWrapper() {
            group = new ButtonGroup();
            dummy = new JRadioButton();

            // We need a dummy button to have the appearance of all of
            // the radio buttons being in an unselected state.
            //
            // From:
            //   http://java.sun.com/j2se/1.5/docs/api/javax/swing/ButtonGroup.html
            //
            // "There is no way to turn a button programmatically to 'off', in
            // order to clear the button group. To give the appearance of 'none
            // selected', add an invisible radio button to the group and then
            // programmatically select that button to turn off all the displayed
            // radio buttons. For example, a normal button with the label 'none'
            // could be wired to select the invisible radio button.
            group.add(dummy);
        }

        public void add(AbstractButton b) {
            group.add(b);
        }

        public void clearSelection() {
            group.setSelected(dummy.getModel(), true);
        }
    }
}
