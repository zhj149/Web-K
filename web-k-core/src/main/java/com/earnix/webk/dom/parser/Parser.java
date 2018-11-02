package com.earnix.webk.dom.parser;

import com.earnix.webk.dom.Jsoup;
import com.earnix.webk.dom.nodes.DocumentModel;
import com.earnix.webk.dom.nodes.ElementModel;
import com.earnix.webk.dom.nodes.NodeModel;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * Parses HTML into a {@link DocumentModel}. Generally best to use one of the  more convenient parse methods
 * in {@link Jsoup}.
 */
public class Parser {
    private TreeBuilder treeBuilder;
    private ParseErrorList errors;
    private ParseSettings settings;

    /**
     * Create a new Parser, using the specified TreeBuilder
     *
     * @param treeBuilder TreeBuilder to use to parse input into Documents.
     */
    public Parser(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        settings = treeBuilder.defaultSettings();
        errors = ParseErrorList.noTracking();
    }

    public DocumentModel parseInput(String html, String baseUri) {
        return treeBuilder.parse(new StringReader(html), baseUri, this);
    }

    public DocumentModel parseInput(Reader inputHtml, String baseUri) {
        return treeBuilder.parse(inputHtml, baseUri, this);
    }

    public List<NodeModel> parseFragmentInput(String fragment, ElementModel context, String baseUri) {
        return treeBuilder.parseFragment(fragment, context, baseUri, this);
    }
    // gets & sets

    /**
     * Get the TreeBuilder currently in use.
     *
     * @return current TreeBuilder.
     */
    public TreeBuilder getTreeBuilder() {
        return treeBuilder;
    }

    /**
     * Update the TreeBuilder used when parsing content.
     *
     * @param treeBuilder current TreeBuilder
     * @return this, for chaining
     */
    public Parser setTreeBuilder(TreeBuilder treeBuilder) {
        this.treeBuilder = treeBuilder;
        treeBuilder.parser = this;
        return this;
    }

    /**
     * Check if parse error tracking is enabled.
     *
     * @return current track error state.
     */
    public boolean isTrackErrors() {
        return errors.getMaxSize() > 0;
    }

    /**
     * Enable or disable parse error tracking for the next parse.
     *
     * @param maxErrors the maximum number of errors to track. Set to 0 to disable.
     * @return this, for chaining
     */
    public Parser setTrackErrors(int maxErrors) {
        errors = maxErrors > 0 ? ParseErrorList.tracking(maxErrors) : ParseErrorList.noTracking();
        return this;
    }

    /**
     * Retrieve the parse errors, if any, from the last parse.
     *
     * @return list of parse errors, up to the size of the maximum errors tracked.
     */
    public ParseErrorList getErrors() {
        return errors;
    }

    public Parser settings(ParseSettings settings) {
        this.settings = settings;
        return this;
    }

    public ParseSettings settings() {
        return settings;
    }

    // static parse functions below

    /**
     * Parse HTML into a Document.
     *
     * @param html    HTML to parse
     * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return parsed Document
     */
    public static DocumentModel parse(String html, String baseUri) {
        TreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parse(new StringReader(html), baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into a list of nodes. The context element, if supplied, supplies parsing context.
     *
     * @param fragmentHtml the fragment of HTML to parse
     * @param context      (optional) the element that this HTML fragment is being parsed for (i.e. for inner HTML). This
     *                     provides stack context (for implicit element creation).
     * @param baseUri      base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return list of nodes parsed from the input HTML. Note that the context element, if supplied, is not modified.
     */
    public static List<NodeModel> parseFragment(String fragmentHtml, ElementModel context, String baseUri) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        return treeBuilder.parseFragment(fragmentHtml, context, baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into a list of nodes. The context element, if supplied, supplies parsing context.
     *
     * @param fragmentHtml the fragment of HTML to parse
     * @param context      (optional) the element that this HTML fragment is being parsed for (i.e. for inner HTML). This
     *                     provides stack context (for implicit element creation).
     * @param baseUri      base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @param errorList    list to add errors to
     * @return list of nodes parsed from the input HTML. Note that the context element, if supplied, is not modified.
     */
    public static List<NodeModel> parseFragment(String fragmentHtml, ElementModel context, String baseUri, ParseErrorList errorList) {
        HtmlTreeBuilder treeBuilder = new HtmlTreeBuilder();
        Parser parser = new Parser(treeBuilder);
        parser.errors = errorList;
        return treeBuilder.parseFragment(fragmentHtml, context, baseUri, parser);
    }

    /**
     * Parse a fragment of XML into a list of nodes.
     *
     * @param fragmentXml the fragment of XML to parse
     * @param baseUri     base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return list of nodes parsed from the input XML.
     */
    public static List<NodeModel> parseXmlFragment(String fragmentXml, String baseUri) {
        XmlTreeBuilder treeBuilder = new XmlTreeBuilder();
        return treeBuilder.parseFragment(fragmentXml, baseUri, new Parser(treeBuilder));
    }

    /**
     * Parse a fragment of HTML into the {@code body} of a Document.
     *
     * @param bodyHtml fragment of HTML
     * @param baseUri  base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return Document, with empty head, and HTML parsed into body
     */
    public static DocumentModel parseBodyFragment(String bodyHtml, String baseUri) {
        DocumentModel doc = DocumentModel.createShell(baseUri);
        ElementModel body = doc.body();
        List<NodeModel> nodeList = parseFragment(bodyHtml, body, baseUri);
        NodeModel[] nodes = nodeList.toArray(new NodeModel[nodeList.size()]); // the node list gets modified when re-parented
        for (int i = nodes.length - 1; i > 0; i--) {
            nodes[i].remove();
        }
        for (NodeModel node : nodes) {
            body.appendChild(node);
        }
        return doc;
    }

    /**
     * Utility method to unescape HTML entities from a string
     *
     * @param string      HTML escaped string
     * @param inAttribute if the string is to be escaped in strict mode (as attributes are)
     * @return an unescaped string
     */
    public static String unescapeEntities(String string, boolean inAttribute) {
        Tokeniser tokeniser = new Tokeniser(new CharacterReader(string), ParseErrorList.noTracking());
        return tokeniser.unescapeEntities(inAttribute);
    }

    /**
     * @param bodyHtml HTML to parse
     * @param baseUri  baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     * @return parsed Document
     * @deprecated Use {@link #parseBodyFragment} or {@link #parseFragment} instead.
     */
    public static DocumentModel parseBodyFragmentRelaxed(String bodyHtml, String baseUri) {
        return parse(bodyHtml, baseUri);
    }

    // builders

    /**
     * Create a new HTML parser. This parser treats input as HTML5, and enforces the creation of a normalised document,
     * based on a knowledge of the semantics of the incoming tags.
     *
     * @return a new HTML parser.
     */
    public static Parser htmlParser() {
        return new Parser(new HtmlTreeBuilder());
    }

    /**
     * Create a new XML parser. This parser assumes no knowledge of the incoming tags and does not treat it as HTML,
     * rather creates a simple tree directly from the input.
     *
     * @return a new simple XML parser.
     */
    public static Parser xmlParser() {
        return new Parser(new XmlTreeBuilder());
    }
}