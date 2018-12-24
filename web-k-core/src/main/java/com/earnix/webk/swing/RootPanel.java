/*
 * {{{ header & license
 * Copyright (c) 2004-2008 Joshua Marinacci, Torbjoern Gannholm, Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.earnix.webk.swing;

import com.earnix.webk.css.constants.CSSName;
import com.earnix.webk.css.constants.IdentValue;
import com.earnix.webk.css.parser.FSRGBColor;
import com.earnix.webk.css.parser.PropertyValue;
import com.earnix.webk.css.style.CalculatedStyle;
import com.earnix.webk.css.style.derived.ColorValue;
import com.earnix.webk.css.style.derived.LengthValue;
import com.earnix.webk.css.style.derived.StringValue;
import com.earnix.webk.dom.nodes.DocumentModel;
import com.earnix.webk.dom.nodes.ElementModel;
import com.earnix.webk.event.DocumentListener;
import com.earnix.webk.extend.FSCanvas;
import com.earnix.webk.extend.NamespaceHandler;
import com.earnix.webk.extend.UserInterface;
import com.earnix.webk.layout.BoxBuilder;
import com.earnix.webk.layout.Layer;
import com.earnix.webk.layout.LayoutContext;
import com.earnix.webk.layout.SharedContext;
import com.earnix.webk.render.BlockBox;
import com.earnix.webk.render.Box;
import com.earnix.webk.render.PageBox;
import com.earnix.webk.render.RenderingContext;
import com.earnix.webk.render.ViewportBox;
import com.earnix.webk.script.ScriptContext;
import com.earnix.webk.util.Configuration;
import com.earnix.webk.util.XRLog;
import org.w3c.dom.css.CSSPrimitiveValue;

import javax.swing.CellRendererPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;


public class RootPanel extends JPanel implements Scrollable, UserInterface, FSCanvas, RepaintListener {
    static final long serialVersionUID = 1L;

    private Box rootBox = null;
    private boolean needRelayout = false;
    private CellRendererPane cellRendererPane;
    private final Set<DocumentListener> documentListeners = new HashSet<DocumentListener>();
    private boolean defaultFontFromComponent;
    protected SharedContext sharedContext;
    private volatile LayoutContext layoutContext;
    private JScrollPane enclosingScrollPane;
    private boolean viewportMatchWidth = true;
    protected ScriptContext scriptContext;

    // initialize to JViewport default mode
    private int default_scroll_mode = JViewport.BLIT_SCROLL_MODE;

    protected DocumentModel doc = null;

    /*
     * ========= UserInterface implementation ===============
     */
    public ElementModel hovered_element = null;
    public ElementModel active_element = null;
    public ElementModel focus_element = null;

    // On-demand repaint requests for async image loading
    private long lastRepaintRunAt = System.currentTimeMillis();
    private final long maxRepaintRequestWaitMs = 50;
    private boolean repaintRequestPending = false;
    private long pendingRepaintCount = 0;

    public RootPanel() {
    }

    public SharedContext getSharedContext() {
        return sharedContext;
    }

    public LayoutContext getLayoutContext() {
        return layoutContext;
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public void setDocument(DocumentModel doc, String url, NamespaceHandler nsh) {
        documentListeners.forEach(DocumentListener::documentStarted);

        resetScrollPosition();
        setRootBox(null);
        this.doc = doc;

        //have to do this first
        if (Configuration.isTrue("xr.cache.stylesheets", true)) {
            getSharedContext().getCss().flushStyleSheets();
        } else {
            getSharedContext().getCss().flushAllStyleSheets();
        }

        getSharedContext().reset();
        getSharedContext().setBaseURL(url);
        getSharedContext().setNamespaceHandler(nsh);
        getSharedContext().getCss().setDocumentContext(getSharedContext(), getSharedContext().getNamespaceHandler(), doc, this);

        repaint();
        documentListeners.forEach(DocumentListener::documentLoaded);
    }

    public void reset() {
        setRootBox(null);

        //have to do this first
        if (Configuration.isTrue("xr.cache.stylesheets", true)) {
            getSharedContext().getCss().flushStyleSheets();
        } else {
            getSharedContext().getCss().flushAllStyleSheets();
        }

        getSharedContext().reset();
        getSharedContext().getCss().setDocumentContext(getSharedContext(), getSharedContext().getNamespaceHandler(), doc, this);
        relayout();
    }

    // iterates over all boxes and, if they have a BG url assigned, makes a call to the UAC
    // to request it. when running with async image loading, this means BG images will start
    // loading before the box ever shows on screen
    private void requestBGImages(final Box box) {
        if (box.getChildCount() == 0) return;
        Iterator ci = box.getChildIterator();
        while (ci.hasNext()) {
            final Box cb = (Box) ci.next();
            CalculatedStyle style = cb.getStyle();
            if (!style.isIdent(CSSName.BACKGROUND_IMAGE, IdentValue.NONE)) {
                String uri = style.getStringProperty(CSSName.BACKGROUND_IMAGE);
                XRLog.load(Level.FINE, "Greedily loading background property " + uri);
                try {
                    getSharedContext().getUac().getImageResource(uri);
                } catch (Exception ex) {
                    // swallow
                }
            }
            requestBGImages(cb);
        }
    }

    public void resetScrollPosition() {
        if (enclosingScrollPane != null) {
            JScrollBar scrollBar = enclosingScrollPane.getVerticalScrollBar();
            if (scrollBar != null) {
                scrollBar.setValue(0);
            }
        }
    }

    /**
     * The method is invoked by {@link #addNotify} and {@link #removeNotify} to
     * ensure that any enclosing {@link JScrollPane} works correctly with this
     * panel. This method can be safely invoked with a <tt>null</tt> scrollPane.
     *
     * @param scrollPane the enclosing {@link JScrollPane} or <tt>null</tt> if
     *                   the panel is no longer enclosed in a {@link JScrollPane}.
     */
    protected void setEnclosingScrollPane(JScrollPane scrollPane) {

        enclosingScrollPane = scrollPane;

        if (enclosingScrollPane != null) {
//            Uu.p("added root panel as a component listener to the scroll pane");
            JViewport viewPort = enclosingScrollPane.getViewport();
            if (viewPort != null) {
                default_scroll_mode = viewPort.getScrollMode();
            }
        }
    }

    protected JScrollPane getEnclosingScrollPane() {
        return enclosingScrollPane;
    }

    /**
     * Gets the fixedRectangle attribute of the BasicPanel object
     *
     * @return The fixedRectangle value
     */
    public Rectangle getFixedRectangle() {
        if (enclosingScrollPane != null) {
            return enclosingScrollPane.getViewportBorderBounds();
        }
        Dimension dim = getSize();
        return new Rectangle(0, 0, dim.width, dim.height);
    }

    /**
     * Overrides the default implementation to test for and configure any {@link
     * JScrollPane} parent.
     */
    public void addNotify() {
        super.addNotify();
        XRLog.general(Level.FINE, "add notify called");
        Container p = getParent();
        if (p instanceof JViewport) {
            Container vp = p.getParent();
            if (vp instanceof JScrollPane) {
                setEnclosingScrollPane((JScrollPane) vp);
            }
        }
    }

    /**
     * Overrides the default implementation unconfigure any {@link JScrollPane}
     * parent.
     */
    public void removeNotify() {
        super.removeNotify();
        setEnclosingScrollPane(null);
    }

    protected void init() {
        setBackground(Color.white);
        super.setLayout(null);
    }

    public RenderingContext newRenderingContext(Graphics2D g) {
        XRLog.layout(Level.FINEST, "new context begin");

        getSharedContext().setCanvas(this);

        XRLog.layout(Level.FINEST, "new context end");

        RenderingContext result = getSharedContext().newRenderingContextInstance();
        result.setFontContext(new Java2DFontContext(g));
        result.setOutputDevice(new Java2DOutputDevice(g));

        getSharedContext().getTextRenderer().setup(result.getFontContext());

        final Box rb = getRootBox();
        if (rb != null) {
            result.setRootLayer(rb.getLayer());
        }

        return result;
    }

    protected LayoutContext newLayoutContext(Graphics2D g) {
        XRLog.layout(Level.FINEST, "new context begin");

        getSharedContext().setCanvas(this);

        XRLog.layout(Level.FINEST, "new context end");

        LayoutContext result = getSharedContext().newLayoutContextInstance();

        Graphics2D layoutGraphics =
                g.getDeviceConfiguration().createCompatibleImage(1, 1).createGraphics();
        result.setFontContext(new Java2DFontContext(layoutGraphics));

        getSharedContext().getTextRenderer().setup(result.getFontContext());

        return result;
    }

    private Rectangle getInitialExtents(LayoutContext c) {
        if (!c.isPrint()) {
            Rectangle extents = getScreenExtents();

            // HACK avoid bogus warning
            if (extents.width == 0 && extents.height == 0) {
                extents = new Rectangle(0, 0, 1, 1);
            }

            return extents;
        } else {
            PageBox first = Layer.createPageBox(c, "first");
            return new Rectangle(0, 0,
                    first.getContentWidth(c), first.getContentHeight(c));
        }
    }

    public Rectangle getScreenExtents() {
        Rectangle extents;
        if (enclosingScrollPane != null) {
            Rectangle bnds = enclosingScrollPane.getViewportBorderBounds();
            extents = new Rectangle(0, 0, bnds.width, bnds.height);
            //Uu.p("bnds = " + bnds);
        } else {
            extents = new Rectangle(getWidth(), getHeight());//200, 200 ) );
            Insets insets = getInsets();
            extents.width -= insets.left + insets.right;
            extents.height -= insets.top + insets.bottom;
        }
        return extents;
    }

    public void doDocumentLayout(Graphics g) {
        this.removeAll();
        if (g == null) {
            return;
        }
        if (doc == null) {
            return;
        }

        LayoutContext c = newLayoutContext((Graphics2D) g);
        synchronized (this) {
            this.layoutContext = c;
        }

        long start = System.currentTimeMillis();

        BlockBox root = (BlockBox) getRootBox();
        if (root != null && isNeedRelayout()) {
            root.reset(c);
        } else {
            root = BoxBuilder.createRootBox(c, doc);
            setRootBox(root);
        }

        initFontFromComponent(root);

        Rectangle initialExtents = getInitialExtents(c);
        root.setContainingBlock(new ViewportBox(initialExtents));

        root.layout(c);

        long end = System.currentTimeMillis();

        XRLog.layout(Level.INFO, "Layout took " + (end - start) + "ms");

            /*
            System.out.println(root.dump(c, "", BlockBox.DUMP_LAYOUT));
            */

        // if there is a fixed child then we need to set opaque to false
        // so that the entire viewport will be repainted. this is slower
        // but that's the hit you get from using fixed layout
        if (root.getLayer().containsFixedContent()) {
            super.setOpaque(false);
        } else {
            super.setOpaque(true);
        }

        XRLog.layout(Level.FINEST, "after layout: " + root);

        Dimension intrinsic_size = root.getLayer().getPaintingDimension(c);

        if (c.isPrint()) {
            root.getLayer().trimEmptyPages(c, intrinsic_size.height);
            root.getLayer().layoutPages(c);
        }

        // If the initial size we fed into the layout matches the width
        // of the layout generated then we can set the scrollable property
        // that matches width of the view pane to the width of this panel.
        // Otherwise, if the intrinsic width is different then we can't
        // couple the width of the view pane to the width of this panel
        // (we hit the minimum size threshold).
        viewportMatchWidth = (initialExtents.width == intrinsic_size.width);

        setPreferredSize(intrinsic_size);
        revalidate();

        if (enclosingScrollPane != null) {
            JViewport viewPort = enclosingScrollPane.getViewport();
            if (viewPort != null) {
                // turn on simple scrolling mode if there's any fixed elements
                if (root.getLayer().containsFixedContent()) {
                    // Uu.p("is fixed");
                    viewPort.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
                } else {
                    // Uu.p("is not fixed");
                    viewPort.setScrollMode(default_scroll_mode);
                }
            }
        }

        documentListeners.forEach(DocumentListener::documentRendered);
            /* FIXME
            if (Configuration.isTrue("xr.image.background.greedy", false)) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        XRLog.load("loading images in document and css greedily");
                        requestBGImages(getRootBox());
                    }
                });
            }*/
    }

    private void initFontFromComponent(BlockBox root) {
        if (isDefaultFontFromComponent()) {
            CalculatedStyle style = root.getStyle();
            PropertyValue fontFamilyProp = new PropertyValue(CSSPrimitiveValue.CSS_STRING, getFont().getFamily(),
                    getFont().getFamily());
            fontFamilyProp.setStringArrayValue(new String[]{fontFamilyProp.getStringValue()});
            style.setDefaultValue(CSSName.FONT_FAMILY, new StringValue(CSSName.FONT_FAMILY, fontFamilyProp));
            style.setDefaultValue(CSSName.FONT_SIZE, new LengthValue(style, CSSName.FONT_SIZE,
                    new PropertyValue(CSSPrimitiveValue.CSS_PX, getFont().getSize(), Integer
                            .toString(getFont().getSize()))));
            Color c = getForeground();
            style.setDefaultValue(CSSName.COLOR, new ColorValue(CSSName.COLOR,
                    new PropertyValue(new FSRGBColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()))));

            if (getFont().isBold()) {
                style.setDefaultValue(CSSName.FONT_WEIGHT, IdentValue.BOLD);
            }

            if (getFont().isItalic()) {
                style.setDefaultValue(CSSName.FONT_STYLE, IdentValue.ITALIC);
            }
        }
    }

    /**
     * Adds the specified Document listener to receive Document events from this
     * component. If listener l is null, no exception is thrown and no action is
     * performed.
     *
     * @param listener Contains the DocumentListener for DocumentEvent data.
     */
    public void addDocumentListener(DocumentListener listener) {
        if (listener == null) {
            return;
        }
        documentListeners.add(listener);
    }

    /**
     * Removes the specified Document listener from receive Document events from this
     * component. If listener l is null, no exception is thrown and no action is
     * performed.
     *
     * @param listener Contains the DocumentListener to remove.
     */
    public void removeDocumentListener(DocumentListener listener) {
        if (listener == null) {
            return;
        }
        documentListeners.remove(listener);
    }

    protected boolean hasDocumentListeners() {
        return !documentListeners.isEmpty();
    }

    protected void fireDocumentStarted() {
        for (DocumentListener list : documentListeners) {
            try {
                list.documentStarted();
            } catch (Exception e) {
                XRLog.load(Level.WARNING, "Document listener threw an exception; continuing processing", e);
            }
        }
    }


    protected void fireOnLayoutException(Throwable t) {
        for (DocumentListener list : documentListeners) {
            try {
                list.onLayoutException(t);
            } catch (Exception e) {
                XRLog.load(Level.WARNING, "Document listener threw an exception; continuing processing", e);
            }
        }
    }

    protected void fireOnRenderException(Throwable t) {
        for (DocumentListener list : documentListeners) {
            try {
                list.onRenderException(t);
            } catch (Exception e) {
                XRLog.load(Level.WARNING, "Document listener threw an exception; continuing processing", e);
            }
        }
    }

    /**
     * @return a CellRendererPane suitable for drawing components in (with CellRendererPane.paintComponent)
     */
    public CellRendererPane getCellRendererPane() {
        if (cellRendererPane == null || cellRendererPane.getParent() != this) {
            cellRendererPane = new CellRendererPane();
            this.add(cellRendererPane);
        }

        return cellRendererPane;
    }


    public boolean isHover(ElementModel e) {
        return e == hovered_element;
    }

    public boolean isActive(ElementModel e) {
        return e == active_element;
    }

    public boolean isFocus(ElementModel e) {
        return e == focus_element;
    }

    public void relayout() {
        if (doc != null) {
            setNeedRelayout(true);
            repaint();
        }
    }

    public double getLayoutWidth() {
        if (enclosingScrollPane != null) {
            return enclosingScrollPane.getViewportBorderBounds().width;
        } else {
            return getSize().width;
        }
    }

    public boolean isPrintView() {
        return false;
    }

    public synchronized Box getRootBox() {
        return rootBox;
    }

    public synchronized void setRootBox(Box rootBox) {
        this.rootBox = rootBox;
    }

    public synchronized Layer getRootLayer() {
        return getRootBox() == null ? null : getRootBox().getLayer();
    }

    public Box find(MouseEvent e) {
        return find(e.getX(), e.getY());
    }

    public Box find(int x, int y) {
        Layer l = getRootLayer();
        if (l != null) {
            return l.find(layoutContext, x, y, false);
        }
        return null;
    }

    public void doLayout() {
        if (isExtentsHaveChanged()) {
            setNeedRelayout(true);
        }
        super.doLayout();
    }

    public void validate() {
        super.validate();

        if (isExtentsHaveChanged()) {
            setNeedRelayout(true);
        }
    }

    protected boolean isExtentsHaveChanged() {
        if (rootBox == null) {
            return true;
        } else {
            Rectangle oldExtents = ((ViewportBox) rootBox.getContainingBlock()).getExtents();
            if (!oldExtents.equals(getScreenExtents())) {
                return true;
            } else {
                return false;
            }
        }
    }

    protected synchronized boolean isNeedRelayout() {
        return needRelayout;
    }

    protected synchronized void setNeedRelayout(boolean needRelayout) {
        this.needRelayout = needRelayout;
    }

    public void repaintRequested(final boolean doLayout) {
        final long now = System.currentTimeMillis();
        final long el = now - lastRepaintRunAt;
        if (!doLayout || el > maxRepaintRequestWaitMs || pendingRepaintCount > 5) {
            XRLog.general(Level.FINE, "*** Repainting panel, by request, el: " + el + " pending " + pendingRepaintCount);
            if (doLayout) {
                relayout();
            } else {
                repaint();
            }
            lastRepaintRunAt = System.currentTimeMillis();
            repaintRequestPending = false;
            pendingRepaintCount = 0;
        } else {
            if (!repaintRequestPending) {
                XRLog.general(Level.FINE, "... Queueing new repaint request, el: " + el + " < " + maxRepaintRequestWaitMs);
                repaintRequestPending = true;
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.currentThread().sleep(Math.min(maxRepaintRequestWaitMs, Math.abs(maxRepaintRequestWaitMs - el)));
                            EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    XRLog.general(Level.FINE, "--> running queued repaint request");
                                    repaintRequested(doLayout);
                                    repaintRequestPending = false;
                                }
                            });
                        } catch (InterruptedException e) {
                            // swallow
                        }
                    }
                }).start();
            } else {
                pendingRepaintCount++;
                XRLog.general("hmm... repaint request, but already have one");
            }
        }
    }

    public boolean isDefaultFontFromComponent() {
        return defaultFontFromComponent;
    }

    public void setDefaultFontFromComponent(boolean defaultFontFromComponent) {
        this.defaultFontFromComponent = defaultFontFromComponent;
    }

    // ----- Scrollable interface -----

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int dif = 1;
        if (orientation == SwingConstants.VERTICAL) {
            dif = visibleRect.height;
        } else if (orientation == SwingConstants.HORIZONTAL) {
            dif = visibleRect.width;
        }
        return Math.min(35, dif);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        int dif = 1;
        if (orientation == SwingConstants.VERTICAL) {
            dif = Math.max(visibleRect.height - 10, dif);
        } else if (orientation == SwingConstants.HORIZONTAL) {
            dif = Math.max(visibleRect.width, dif);
        }
        return dif;
    }

    public boolean getScrollableTracksViewportWidth() {
        // If the last layout successfully filled the desired width then
        // viewport should match the component size.
        return viewportMatchWidth;
    }

    public boolean getScrollableTracksViewportHeight() {
        // If the last layout height of this component is <= the viewport
        // height then we make the viewport height match the component size.
        if (enclosingScrollPane != null) {
            JViewport viewPort = enclosingScrollPane.getViewport();
            if (viewPort != null) {
                return getPreferredSize().height <= viewPort.getHeight();
            }
        }
        return false;
    }

}
