package org.xhtmlrenderer.js.impl;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CSSDeclaration;
import com.helger.css.decl.shorthand.CSSShortHandDescriptor;
import com.helger.css.decl.shorthand.CSSShortHandRegistry;
import com.helger.css.property.ECSSProperty;
import com.helger.css.reader.CSSReaderDeclarationList;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.xhtmlrenderer.js.geom.DOMMatrix;
import org.xhtmlrenderer.js.geom.DOMMatrix2DInit;
import org.xhtmlrenderer.js.html5.canvas.*;
import org.xhtmlrenderer.js.web_idl.Attribute;
import org.xhtmlrenderer.js.web_idl.DOMString;
import org.xhtmlrenderer.js.web_idl.Sequence;
import org.xhtmlrenderer.js.whatwg_dom.Element;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Taras Maslov
 * 6/21/2018
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class CanvasRenderingContext2DImpl implements CanvasRenderingContext2D {

    int width;
    int height;

    Object fillStyle;
    Object strokeStyle;

    Graphics2D g2d;
    BufferedImage image;

    LinkedList<G2DState> stateStack = new LinkedList<>();

    java.awt.geom.Path2D path2D = new java.awt.geom.Path2D.Double();

    // G2D state is not equal to state in stack
    boolean stateDirty;

    HTMLCanvasElementImpl canvas;
    boolean wasFill;
    DOMString fontStyle;
    
    // region external WebIDL attributes implementations
    
    Attribute<Double> globalAlpha = new Attribute<Double>() {
        @Override
        public Double get() {
            log.trace("globalAlpha get");
            return state().getGlobalAlpha();
        }

        @Override
        public void set(Double aDouble) {
            log.trace("globalAlpha set");
            state().setGlobalAlpha(aDouble);
            stateDirty = true;
        }
    };
    
    // endregion

    public CanvasRenderingContext2DImpl(HTMLCanvasElementImpl canvas, int width, int height) {
        this.canvas = canvas;
        resize(width, height);
    }

    @Override
    public HTMLCanvasElement canvas() {
        return canvas;
    }

    @Override
    public Attribute<Double> globalAlpha() {
        return globalAlpha;
    }

    @Override
    public Attribute<DOMString> globalCompositeOperation() {
        return new Attribute<DOMString>() {
            @Override
            public DOMString get() {
                log.trace("globalCompositeOperation get");
                return null;
            }

            @Override
            public void set(DOMString string) {
                log.trace("globalCompositeOperation set");
            }
        };
    }

    @Override
    public void drawImage(CanvasImageSource image, double dx, double dy) {
        log.trace("drawImage");
    }

    @Override
    public void drawImage(CanvasImageSource image, double dx, double dy, double dw, double dh) {
        log.trace("drawImage");
    }

    @Override
    public void drawImage(CanvasImageSource image, double sx, double sy, double sw, double sh, double dx, double dy, double dw, double dh) {
        log.trace("drawImage");
    }

    @Override
    public void beginPath()
    {
        log.trace("beginPath");
        path2D = new java.awt.geom.Path2D.Double();
    }

    @Override
    public void fill(CanvasFillRule fillRule) {
        log.trace("fill");
        ensureState(true);
        g2d.fill(path2D);
    }

//    @Override
//    public void fill(Path2D path, CanvasFillRule fillRule) {
//
//    }

    @Override
    public void stroke() {
        log.trace("stroke");
        ensureState(false);
        g2d.draw(path2D);
    }

    @Override
    public void clip(CanvasFillRule fillRule) {
        log.trace("clip");
    }

    @Override
    public void clip(Path2D path, CanvasFillRule fillRule) {
        log.trace("clip");
    }

    @Override
    public void resetClip() {
        log.trace("resetClip");
    }

    @Override
    public boolean isPointInPath(double x, double y, CanvasFillRule fillRule) {
        log.trace("isPointInPath");
        return false;
    }

    @Override
    public boolean isPointInPath(Path2D path, double x, double y, CanvasFillRule fillRule) {
        log.trace("isPointInPath");
        return false;
    }

    @Override
    public boolean isPointInStroke(double x, double y) {
        log.trace("isPointInStroke");
        return false;
    }

    @Override
    public boolean isPointInStroke(Path2D path, double x, double y) {
        log.trace("isPointInStroke");
        return false;
    }

    @Override
    public Attribute<Object> strokeStyle() {
        return Attribute.receive((value) -> {
            log.trace("strokeStyle set");
            CanvasRenderingContext2DImpl.this.strokeStyle = value;
            if (value instanceof DOMString) {

//                try {
//                    val color = Color.decode(value.toString());
//                    state().setStrokeColor(color);
//                    stateDirty = true;
//                } catch (NumberFormatException e) {
                    state().setStrokeColor(new Color((int) (Math.random() * Integer.MAX_VALUE)));
                    stateDirty = true;
//                }
                

            } else {
                // todo grad and pattern
                throw new RuntimeException();
            }
        }).give(() -> {
            log.trace("strokeStyle get");
            return strokeStyle;
        });
    }

    @Override
    public Attribute<Object> fillStyle() {
        return Attribute.receive((value) -> {
            log.trace("fillStyle set");
            CanvasRenderingContext2DImpl.this.fillStyle = value;
            if (value instanceof DOMString) {

//                try {
//                    val color = Color.decode(value.toString());
//                    state().setFillColor(color);
//                    stateDirty = true;
//                } catch (NumberFormatException e) {
                    state().setFillColor(new Color((int) (Math.random() * Integer.MAX_VALUE)));
                    stateDirty = true;
//                }

            } else {
                // todo grad and pattern
                throw new RuntimeException();
            }
        }).give(() -> {
            log.trace("fillStyle get");
            return fillStyle;
        });
    }

    @Override
    public CanvasGradient createLinearGradient(double x0, double y0, double x1, double y1) {
        log.trace("createLinearGradient");
        return null;
    }

    @Override
    public CanvasGradient createRadialGradient(double x0, double y0, double r0, double x1, double y1, double r1) {
        log.trace("createRadialGradient");
        return null;
    }

    @Override
    public CanvasPattern createPattern(CanvasImageSource image, DOMString repetition) {
        log.trace("createPattern");
        return null;
    }

    @Override
    public Attribute<DOMString> filter() {
        log.trace("filter");
        return null;
    }

    @Override
    public ImageData createImageData(long sw, long sh) {
        log.trace("createImageData");
        return null;
    }

    @Override
    public ImageData createImageData(ImageData imagedata) {
        log.trace("createImageData");
        return null;
    }

    @Override
    public ImageData getImageData(long sx, long sy, long sw, long sh) {
        log.trace("getImageData");
        return null;
    }

    @Override
    public void putImageData(ImageData imagedata, long dx, long dy) {
        log.trace("putImageData");
    }

    @Override
    public void putImageData(ImageData imagedata, long dx, long dy, long dirtyX, long dirtyY, long dirtyWidth, long dirtyHeight) {
        log.trace("putImageData");
    }

    @Override
    public Attribute<Boolean> imageSmoothingEnabled() {
        return new Attribute<Boolean>() {
            @Override
            public Boolean get() {
                log.trace("imageSmoothingEnabled get");
                return true;
            }

            @Override
            public void set(Boolean aBoolean) {
                log.trace("imageSmoothingEnabled set");
            }
        };
    }

    @Override
    public Attribute<ImageSmoothingQuality> imageSmoothingQuality() {
        return new Attribute<ImageSmoothingQuality>() {
            @Override
            public ImageSmoothingQuality get() {
                log.trace("imageSmoothingQuality get");
                return ImageSmoothingQuality.height;
            }

            @Override
            public void set(ImageSmoothingQuality imageSmoothingQuality) {
                log.trace("imageSmoothingQuality set");
            }
        };
    }

    @Override
    public void closePath() {
        log.trace("closePath");
        path2D.closePath();
    }

    @Override
    public void moveTo(double x, double y) {
        log.trace("moveTo");
        path2D.moveTo(x, y);
    }

    @Override
    public void lineTo(double x, double y) {
        log.trace("lineTo");
        path2D.lineTo(x, y);
    }

    @Override
    public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
        log.trace("quadraticCurveTo");
        path2D.quadTo(cpx, cpy, x, y);
    }

    @Override
    public void bezierCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        log.trace("bezierCurveTo");
        path2D.curveTo(cp1x, cp1y, cp2x, cp2y, x, y);
    }

    @Override
    public void arcTo(double x1, double y1, double x2, double y2, double radius) {
        log.trace("arcTo");
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        log.trace("rect");
        path2D.append(new Rectangle2D.Double(x, y, w, h), false);
    }

    //  boolean ccw = arcAngle > 0;
    //    ctx.beginPath();
    //    ctx.arc(cx, cy, r, -startAngle, -(startAngle + arcAngle), ccw);
    //    ctx.stroke();

    // x = -startAngle - arcAngle

    // arcA = - startAngle - x

    //  int top = (int) (cx - r);
    //    int left = (int) (cy - r);
    //    int diam = (int) (2*r);
    //    currentState().prepareStroke(g2d);
    //    g2d.drawArc(top, left, diam, diam,
    //                FloatMath.round(FloatMath.toDegrees(startAngle)),
    //                FloatMath.round(FloatMath.toDegrees(arcAngle)));

    @Override
    public void arc(double x, double y, double r, double startAngle, double endAngle, boolean anticlockwise) {
        log.trace("arc");
        double top = x - r;
        double left = y - r;
        double diam = 2 * r;
        ensureState(false);

        val ext = Math.abs(startAngle - endAngle);

        path2D.append(
                new Arc2D.Double(top, left, diam, diam,
                        Math.toDegrees(anticlockwise ? -startAngle : endAngle),
                        Math.toDegrees(anticlockwise ? -endAngle : endAngle), Arc2D.OPEN), false);
    }

    @Override
    public void ellipse(double x, double y, double radiusX, double radiusY, double rotation, double startAngle, double endAngle, boolean anticlockwise) {
        log.trace("ellipse");
    }

    @Override
    public Attribute<Double> lineWidth() {
        return Attribute.<Double>receive(value -> {
            log.trace("lineWidth set");
            state().setLineWidth(value);
            stateDirty = true;
        }).give(() -> {
                    log.trace("lineWidth get");
                    return state().getLineWidth();
                }
        );
    }

    @Override
    public Attribute<CanvasLineCap> lineCap() {
        
        return new Attribute<CanvasLineCap>() {
            
            @Override
            public CanvasLineCap get() {
                log.trace("lineCap get");
                return CanvasLineCap.butt;
            }

            @Override
            public void set(CanvasLineCap canvasLineCap) {
                log.trace("lineCap set");
            }
            
        };
        
    }

    @Override
    public Attribute<CanvasLineJoin> lineJoin() {
        return new Attribute<CanvasLineJoin>() {
            @Override
            public CanvasLineJoin get() {
                log.trace("lineJoin get");
                return CanvasLineJoin.miter;
            }

            @Override
            public void set(CanvasLineJoin canvasLineJoin) {
                log.trace("lineJoin set");

            }
        };
    }

    @Override
    public Attribute<Double> miterLimit() {
        return new Attribute<Double>() {
            @Override
            public Double get() {
                log.trace("miterLimit get");
                return null;
            }

            @Override
            public void set(Double aDouble) {
                log.trace("miterLimit set");
            }
        };
    }

    @Override
    public void setLineDash(Sequence<Double> segments) {
        log.trace("setLineDash");
    }

    @Override
    public Sequence<Double> getLineDash() {
        log.trace("getLineDash");
        return null;
    }

    @Override
    public Attribute<Double> lineDashOffset() {
        return new Attribute<Double>() {
            @Override
            public Double get() {
                log.trace("lineDashOffset get");
                return null;
            }

            @Override
            public void set(Double aDouble) {
                log.trace("lineDashOffset set");
            }
        };
    }

    @Override
    public void clearRect(double x, double y, double w, double h) {
        log.trace("clearRect {} {} {} {}", x, y, w, h);
        g2d.clearRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        log.trace("fillRect");
        ensureState(true);
        g2d.fillRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        log.trace("strokeRect {} {} {} {}", x, y, w, h);
        ensureState(false);
        g2d.drawRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public Attribute<Double> shadowOffsetX() {
        return new Attribute<Double>() {
            @Override
            public Double get() {
                log.trace("shadowOffsetX get");
                return null;
            }

            @Override
            public void set(Double aDouble) {
                log.trace("shadowOffsetX set");
            }
        };
    }

    @Override
    public Attribute<Double> shadowOffsetY() {
        return new Attribute<Double>() {
            @Override
            public Double get() {
                log.trace("shadowOffsetY get");
                return null;
            }

            @Override
            public void set(Double aDouble) {
                log.trace("shadowOffsetY set");
            }
        };
    }

    @Override
    public Attribute<Double> shadowBlur() {
        return new Attribute<Double>() {
            @Override
            public Double get() {
                log.trace("shadowBlur get");
                return null;
            }

            @Override
            public void set(Double aDouble) {
                log.trace("shadowBlur set");
            }
        };
    }

    @Override
    public Attribute<DOMString> shadowColor() {
        return new Attribute<DOMString>() {
            @Override
            public DOMString get() {
                log.trace("shadowColor get");
                return null;
            }

            @Override
            public void set(DOMString string) {
                log.trace("shadowColor set");
            }
        };
    }

    @Override
    public void save() {
        log.trace("save");
        stateStack.add(state().clone());
    }

    @Override
    public void restore() {
        log.trace("restore");
        stateStack.removeLast();
        stateDirty = true;
    }

    @Override
    public void fillText(DOMString text, double x, double y, Double maxWidth) {
        log.trace("fillText");
        ensureState(true);
        g2d.drawString(text.toString(), (int) x, (int) y);
        // todo handle max width
    }

    @Override
    public void strokeText(DOMString text, double x, double y, Double maxWidth) {
        log.trace("strokeText");
    }

    @Override
    public TextMetrics measureText(DOMString text) {
        log.trace("measureText");
        ensureState();
        return new TextMetricsImpl(g2d.getFontMetrics().stringWidth(text.toString()));
    }

    @Override
    public Attribute<DOMString> font() {
        return Attribute.<DOMString>receive(style -> {
            log.trace("font set");
            fontStyle = style;
            val styleString = style.toString();
            val declarations = CSSReaderDeclarationList.readFromString("font: " + styleString, ECSSVersion.CSS30);
            val declaration = declarations.getDeclarationAtIndex(0);

            // Get the Shorthand descriptor for "border"    
            final CSSShortHandDescriptor aSHD = CSSShortHandRegistry.getShortHandDescriptor(ECSSProperty.FONT);

            // And now split it into pieces
            final List<CSSDeclaration> aSplittedDecls = aSHD.getSplitIntoPieces(declaration);
            for (CSSDeclaration aSplittedDecl : aSplittedDecls) {

                val fontSize = aSplittedDecl.getExpression().getAsCSSString();
                if (fontSize.endsWith("px")) {
                    state().setFontSize((int) Math.round(Double.parseDouble(fontSize.substring(0, 2))));
                    stateDirty = true;
                } else {
                    state().setFontSize(14);
                }

            }

        }).give(() -> {
            log.trace("font get");
            return fontStyle;
        });
    }

    @Override
    public Attribute<CanvasTextAlign> textAlign() {
        return new Attribute<CanvasTextAlign>() {
            @Override
            public CanvasTextAlign get() {
                log.trace("textAlign get");
                return null;
            }

            @Override
            public void set(CanvasTextAlign canvasTextAlign) {
                log.trace("textAlign set");
            }
        };
    }

    @Override
    public Attribute<CanvasTextBaseline> textBaseline() {
        
        return new Attribute<CanvasTextBaseline>() {
            @Override
            public CanvasTextBaseline get() {
                log.trace("textBaseline get");
                return null;
            }

            @Override
            public void set(CanvasTextBaseline canvasTextBaseline) {
                log.trace("textBaseline set");
            }
        };
    }

    @Override
    public Attribute<CanvasDirection> direction() {
        return new Attribute<CanvasDirection>() {
            @Override
            public CanvasDirection get() {
                log.trace("direction get");
                return CanvasDirection.inherit;
            }

            @Override
            public void set(CanvasDirection canvasDirection) {
                log.trace("direction set");
            }
        };
    }

    @Override
    public void scale(double x, double y) {
        log.trace("scale");
        state().scale(x, y);
        stateDirty = true;
    }

    @Override
    public void rotate(double angle) {
        log.trace("rotate");
        state().rotate(angle);
        stateDirty = true;
    }

    @Override
    public void translate(double x, double y) {
        log.trace("translate");
        state().translate(x, y);
        stateDirty = true;
    }

    @Override
    public void transform(double a, double b, double c, double d, double e, double f) {
        log.trace("transform");
        state().transform(a, b, c, d, e, f);
        stateDirty = true;
    }

    @Override
    public DOMMatrix getTransform() {
        log.trace("getTransform");
        return null;
    }

    @Override
    public void setTransform(double a, double b, double c, double d, double e, double f) {
        log.trace("setTransform");
        state().setTransform(a, b, c, d, e, f);
        stateDirty = true;
    }

    @Override
    public void setTransform(DOMMatrix2DInit transform) {
        log.trace("setTransform dm");
    }

    @Override
    public void resetTransform() {
        log.trace("resetTransform");
        state().setTransform(new AffineTransform());
        stateDirty = true;
    }

    @Override
    public void drawFocusIfNeeded(Element element) {
        log.trace("drawFocusIfNeeded");
    }

    @Override
    public void drawFocusIfNeeded(Path2D path, Element element) {
        log.trace("drawFocusIfNeeded");
    }

    @Override
    public void scrollPathIntoView() {
        log.trace("scrollPathIntoView");
    }

    @Override
    public void scrollPathIntoView(Path2D path) {
        log.trace("scrollPathIntoView");
    }

    // region custom

    void init() {

        // initial state
        stateStack = new LinkedList<>();
        stateStack.push(new G2DState());
        stateDirty = false;

        if (width == 0 || height == 0) {
            return;
        }

        image = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(width, height);
        g2d = (Graphics2D) image.getGraphics();
        g2d.setBackground(Color.red);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, height);
    }


    public void resize(int w, int h) {
        canvas.getTarget().attr("width", String.valueOf(w));
        canvas.getTarget().attr("height", String.valueOf(w));
        
        if (w == this.width && h == this.height) {
            return;
        }
        this.width = w;
        this.height = h;
        init();
    }

    private G2DState state() {
        return stateStack.getLast();
    }

    public BufferedImage getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void ensureState(boolean fill) {
        if (stateDirty || wasFill != fill) {
            state().apply(g2d, fill);
            wasFill = fill;
            stateDirty = false;
        }
    }

    private void ensureState() {
        if (stateDirty) {
            state().apply(g2d);
            stateDirty = false;
        }
    }


    // endregion
}