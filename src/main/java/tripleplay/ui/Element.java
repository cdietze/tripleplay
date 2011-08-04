//
// Triple Play - utilities for use in ForPlay-based games
// Copyright (c) 2011, Three Rings Design, Inc. - All rights reserved.
// http://github.com/threerings/tripleplay/blob/master/LICENSE

package tripleplay.ui;

import pythagoras.f.AffineTransform;
import pythagoras.f.Dimension;
import pythagoras.f.IDimension;
import pythagoras.f.IPoint;
import pythagoras.f.IRectangle;
import pythagoras.f.Point;
import pythagoras.f.Rectangle;

import react.Slot;

import forplay.core.Asserts;
import forplay.core.ForPlay;
import forplay.core.GroupLayer;
import forplay.core.Transform;

/**
 * The root of the interface element hierarchy. See {@link Widget} for the root of all interactive
 * elements, and {@link Group} for the root of all grouping elements.
 */
public abstract class Element
{
    /** Defines the states that may be assumed by an element. */
    public static enum State {
        DEFAULT, DISABLED, DOWN;
    }

    /** The layer associated with this element. */
    public final GroupLayer layer = ForPlay.graphics().createGroupLayer();

    /**
     * Returns this element's x offset relative to its parent.
     */
    public float x () {
        return layer.transform().tx();
    }

    /**
     * Returns this element's y offset relative to its parent.
     */
    public float y () {
        return layer.transform().ty();
    }

    /**
     * Returns the width and height of this element's bounds.
     */
    public IDimension size () {
        return _size;
    }

    /**
     * Writes the location of this element (relative to its parent) into the supplied point.
     * @return {@code loc} for convenience.
     */
    public IPoint location (Point loc) {
        Transform transform = layer.transform();
        return loc.set(transform.tx(), transform.ty());
    }

    /**
     * Writes the current bounds of this element into the supplied bounds.
     * @return {@code bounds} for convenience.
     */
    public IRectangle bounds (Rectangle bounds) {
        Transform transform = layer.transform();
        bounds.setBounds(transform.tx(), transform.ty(), _size.width, _size.height);
        return bounds;
    }

    /**
     * Returns the parent of this element, or null.
     */
    public Group parent () {
        return _parent;
    }

    /**
     * Returns the styles configured on this element.
     */
    public Styles styles () {
        return _styles;
    }

    /**
     * Configures the styles for this element. Any previously configured styles are overwritten.
     */
    public void setStyles (Styles styles) {
        _styles = styles;
    }

    /**
     * Adds the supplied styles to this element. Where the new styles overlap with existing styles,
     * the new styles are preferred, but non-overlapping old styles are preserved.
     */
    public void addStyles (Styles styles) {
        _styles = _styles.merge(styles);
    }

    /**
     * Adds the supplied styles to this element. Where the new styles overlap with existing styles,
     * the new styles are preferred, but non-overlapping old styles are preserved.
     */
    public void addStyles (Style.Binding<?>... bindings) {
        addStyles(Styles.make(bindings));
    }

    /**
     * Returns whether this element is enabled.
     */
    public boolean isEnabled () {
        return isSet(Flag.ENABLED);
    }

    /**
     * Enables or disables this element. Disabled elements are not interactive and are usually
     * rendered so as to communicate this state to the user.
     */
    public void setEnabled (boolean enabled) {
        if (enabled != isEnabled()) {
            set(Flag.ENABLED, enabled);
            invalidate();
        }
    }

    /**
     * Returns a slot which can be used to wire the enabled status of this element to a {@link
     * react.Signal} or {@link react.Value}.
     */
    public Slot<Boolean> enabledSlot () {
        return new Slot<Boolean>() {
            public void onEmit (Boolean value) {
                setEnabled(value);
            }
        };
    }

    /**
     * Returns whether this element is visible.
     */
    public boolean isVisible () {
        return isSet(Flag.VISIBLE);
    }

    /**
     * Configures whether this element is visible. An invisible element is not rendered and
     * consumes no space in a group.
     */
    public void setVisible (boolean visible) {
        if (visible != isVisible()) {
            set(Flag.VISIBLE, visible);
            invalidate();
        }
    }

    /**
     * Returns a slot which can be used to wire the visible status of this element to a {@link
     * react.Signal} or {@link react.Value}.
     */
    public Slot<Boolean> visibleSlot () {
        return new Slot<Boolean>() {
            public void onEmit (Boolean value) {
                setVisible(value);
            }
        };
    }

    /**
     * Returns true if this element is part of an interface heirarchy.
     */
    public boolean isAdded () {
        return getRoot() != null;
    }

    /**
     * Called when this element (or its parent element) was added to the interface hierarchy.
     */
    protected void wasAdded (Group parent) {
        _parent = parent;
    }

    /**
     * Called when this element (or its parent element) was removed from the interface hierarchy.
     */
    protected void wasRemoved () {
        _parent = null;
    }

    /**
     * Returns true if the supplied, element-relative, coordinates are inside our bounds.
     */
    protected boolean contains (float x, float y) {
        return !(x < 0 || x > _size.width || y < 0 || y > _size.height);
    }

    /**
     * Used to determine whether a point falls in this element's bounds.
     * @param xform a scratch transform for use by the element.
     * @param point the point to be tested in this element's parent's coordinate system.
     * @return the leaf-most element that contains the supplied point or null if neither this
     * element, nor its children contain the point. Also {@code point} is updated to contain the
     * hit-element-relative coordinates in the event of a hit.
     */
    protected Element hitTest (AffineTransform xform, Point point) {
        // transform the point into our coordinate system
        Transform lt = layer.transform();
        xform.setTransform(lt.m00(), lt.m10(), lt.m01(), lt.m11(), lt.tx(), lt.ty());
        point = xform.inverseTransform(point, point);
        float x = point.x + layer.originX(), y = point.y + layer.originY();
        // check whether it falls within our bounds
        if (!contains(x, y)) return null;
        // if we're the hit component, update the supplied point
        point.set(x, y);
        return this;
    }

    /**
     * Called when the a touch/drag is started within the bounds of this component.
     */
    protected void onPointerStart (float x, float y) {
    }

    /**
     * Called when a touch that started within the bounds of this component is dragged. The drag
     * may progress outside the bounds of this component, but the events will still be dispatched
     * to this component until the touch is released.
     */
    protected void onPointerDrag (float x, float y) {
    }

    /**
     * Called when a touch that started within the bounds of this component is released. The
     * coordinates may be outside the bounds of this component, but the touch in question started
     * inside this component's bounds.
     */
    protected void onPointerEnd (float x, float y) {
    }

    /**
     * Computes the style state of this element based on its flags.
     */
    protected State state () {
        return isSet(Flag.ENABLED) ? State.DEFAULT : State.DISABLED;
    }

    /**
     * An element should call this method when it knows that it has changed in such a way that
     * requires it to recreate its visualization.
     */
    protected void invalidate () {
        if (isSet(Flag.VALID)) {
            set(Flag.VALID, false);
            // note that our preferred size and background are no longer valid
            _preferredSize = null;
            // invalidate our parent if we've got one
            if (_parent != null) {
                _parent.invalidate();
            }
        }
    }

    /**
     * Does whatever this element needs to validate itself. This may involve recomputing
     * visualizations, or laying out children, or anything else.
     */
    protected void validate () {
        if (!isSet(Flag.VALID)) {
            layout();
            set(Flag.VALID, true);
        }
    }

    /**
     * Returns the root of this element's hierarchy, or null if the element is not currently added
     * to a hierarchy.
     */
    protected Root getRoot () {
        return (_parent == null) ? null : _parent.getRoot();
    }

    /**
     * Returns whether the specified state flag is set.
     */
    protected boolean isSet (Flag flag) {
        return (flag.mask & _flags) != 0;
    }

    /**
     * Sets or clears the specified state flag.
     */
    protected void set (Flag flag, boolean on) {
        if (on) {
            _flags |= flag.mask;
        } else {
            _flags &= ~flag.mask;
        }
    }

    /**
     * Returns this element's preferred size, potentially recomputing it if needed.
     *
     * @param hintX if non-zero, an indication that the element will be constrained in the x
     * direction to the specified width.
     * @param hintY if non-zero, an indication that the element will be constrained in the y
     * direction to the specified height.
     */
    protected IDimension getPreferredSize (float hintX, float hintY) {
        if (_preferredSize == null) _preferredSize = computeSize(hintX, hintY);
        return _preferredSize;
    }

    /**
     * Configures the location of this element, relative to its parent.
     */
    protected void setLocation (float x, float y) {
        layer.transform().setTranslation(x, y);
    }

    /**
     * Configures the size of this widget.
     */
    protected void resize (float width, float height) {
        if (_size.width == width && _size.height == height) return; // NOOP
        _size.setSize(width, height);
        invalidate();
    }

    /**
     * Resolves the value for the supplied style. See {@link Styles#resolveStyle} for the gritty
     * details.
     */
    protected <V> V resolveStyle (State state, Style<V> style) {
        return Styles.resolveStyle(this, state, style);
    }

    /**
     * Recomputes this element's preferred size.
     *
     * @param hintX if non-zero, an indication that the element will be constrained in the x
     * direction to the specified width.
     * @param hintY if non-zero, an indication that the element will be constrained in the y
     * direction to the specified height.
     */
    protected abstract Dimension computeSize (float hintX, float hintY);

    /**
     * Rebuilds this element's visualization. Called when this element's size has changed. In the
     * case of groups, this will relayout its children, in the case of widgets, this will rerender
     * the widget.
     */
    protected abstract void layout ();

    protected int _flags = Flag.VISIBLE.mask | Flag.ENABLED.mask;
    protected Group _parent;
    protected Dimension _preferredSize;
    protected Dimension _size = new Dimension();
    protected Styles _styles = Styles.none();

    protected static enum Flag {
        VALID(1 << 0), ENABLED(1 << 1), VISIBLE(1 << 2), DOWN(1 << 3);

        public final int mask;

        Flag (int mask) {
            this.mask = mask;
        }
    };
}