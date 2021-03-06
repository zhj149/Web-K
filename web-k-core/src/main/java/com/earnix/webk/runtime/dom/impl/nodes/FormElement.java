package com.earnix.webk.runtime.dom.impl.nodes;

import com.earnix.webk.runtime.dom.impl.Connection;
import com.earnix.webk.runtime.dom.impl.Jsoup;
import com.earnix.webk.runtime.dom.impl.helper.HttpConnection;
import com.earnix.webk.runtime.dom.impl.helper.Validate;
import com.earnix.webk.runtime.dom.impl.parser.Tag;
import com.earnix.webk.runtime.dom.impl.select.Elements;
import com.earnix.webk.runtime.dom.impl.ElementImpl;
import com.earnix.webk.runtime.dom.impl.NodeImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * A HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 */
public class FormElement extends ElementImpl {
    private final Elements elements = new Elements();

    /**
     * Create a new, standalone form element.
     *
     * @param tag        tag of this element
     * @param baseUri    the base URI
     * @param attributes initial attributes
     */
    public FormElement(Tag tag, String baseUri, AttributesModel attributes) {
        super(tag, baseUri, attributes);
    }

    /**
     * Get the list of form control elements associated with this form.
     *
     * @return form controls associated with this element.
     */
    public Elements elements() {
        return elements;
    }

    /**
     * Add a form control element to this form.
     *
     * @param element form control to add
     * @return this form element, for chaining
     */
    public FormElement addElement(ElementImpl element) {
        elements.add(element);
        return this;
    }

    @Override
    protected void removeChild(NodeImpl out) {
        super.removeChild(out);
        elements.remove(out);
    }

    /**
     * Prepare to submit this form. A Connection object is created with the request set up from the form values. You
     * can then set up other options (like user-agent, timeout, cookies), then execute it.
     *
     * @return a connection prepared from the values of this form.
     * @throws IllegalArgumentException if the form's absolute action URL cannot be determined. Make sure you pass the
     *                                  document's base URI when parsing.
     */
    public Connection submit() {
        String action = hasAttr("action") ? absUrl("action") : baseUri();
        Validate.notEmpty(action, "Could not determine a form action URL for submit. Ensure you set a base URI when parsing.");
        Connection.Method method = attr("method").toUpperCase().equals("POST") ?
                Connection.Method.POST : Connection.Method.GET;

        return Jsoup.connect(action)
                .data(formData())
                .method(method);
    }

    /**
     * Get the data that this form submits. The returned list is a copy of the data, and changes to the contents of the
     * list will not be reflected in the DOM.
     *
     * @return a list of key vals
     */
    public List<Connection.KeyVal> formData() {
        ArrayList<Connection.KeyVal> data = new ArrayList<>();

        // iterate the form control elements and accumulate their values
        for (ElementImpl el : elements) {
            if (!el.tag().isFormSubmittable()) continue; // contents are form listable, superset of submitable
            if (el.hasAttr("disabled")) continue; // skip disabled form inputs
            String name = el.attr("name");
            if (name.length() == 0) continue;
            String type = el.attr("type");

            if ("select".equals(el.tagName())) {
                Elements options = el.select("option[selected]");
                boolean set = false;
                for (ElementImpl option : options) {
                    data.add(HttpConnection.KeyVal.create(name, option.val()));
                    set = true;
                }
                if (!set) {
                    ElementImpl option = el.select("option").first();
                    if (option != null)
                        data.add(HttpConnection.KeyVal.create(name, option.val()));
                }
            } else if ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)) {
                // only add checkbox or radio if they have the checked attribute
                if (el.hasAttr("checked")) {
                    final String val = el.val().length() > 0 ? el.val() : "on";
                    data.add(HttpConnection.KeyVal.create(name, val));
                }
            } else {
                data.add(HttpConnection.KeyVal.create(name, el.val()));
            }
        }
        return data;
    }
}
