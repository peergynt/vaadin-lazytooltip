package org.vaadin.addons.lazytooltip.client;

import java.util.logging.Logger;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.TooltipInfo;
import com.vaadin.client.Util;
import com.vaadin.client.VErrorMessage;
import com.vaadin.client.VTooltip;

public class VLazyTooltip extends VTooltip {
    private static final int MARGIN = 4;

    VErrorMessage em = new VErrorMessage();
    Element description = DOM.createDiv();

    private TooltipInfo currentTooltipInfo = new TooltipInfo(" ");

    private boolean closing = false;
    private boolean opening = false;

    // Open next tooltip faster. Disabled after 2 sec of showTooltip-silence.
    private boolean justClosed = false;

    private String uniqueId = DOM.createUniqueId();
    private int maxWidth;

    // Delays for the tooltip, configurable on the server side
    private int openDelay;
    private int quickOpenDelay;
    private int quickOpenTimeout;
    private int closeTimeout;

    private long tooltipId;

    /**
     * Current element hovered
     */
    private com.google.gwt.dom.client.Element currentElement = null;

    private LazyTooltipConnector lazyTooltipConnector;

    /**
     * Used to show tooltips; usually used via the singleton in
     * {@link ApplicationConnection}. NOTE that #setOwner(Widget)} should be
     * called after instantiating.
     * 
     * @see ApplicationConnection#getVTooltip()
     */
    public VLazyTooltip() {
        super();

        FlowPanel layout = new FlowPanel();
        setWidget(layout);
        layout.add(em);
        description.setPropertyString("className", getStyleName() + "-text");
        DOM.appendChild(layout.getElement(), description);
    }

    /**
     * Show the tooltip with the provided info for assistive devices.
     * 
     * @param info
     *            with the content of the tooltip
     */
    public void showAssistive(TooltipInfo info) {
        updatePosition(null, true);
        setTooltipText(info);
        showTooltip();
    }

    /**
     * Initialize the tooltip overlay for assistive devices.
     * 
     * @since 7.2.4
     */
    public void initializeAssistiveTooltips() {
        updatePosition(null, true);
        setTooltipText(new TooltipInfo(" "));
        showTooltip();
        hideTooltip();
        description.getParentElement().getStyle().clearWidth();
    }

    private void setTooltipText(TooltipInfo info) {
        if (info.getErrorMessage() != null && !info.getErrorMessage().isEmpty()) {
            em.setVisible(true);
            em.updateMessage(info.getErrorMessage());
        } else {
            em.setVisible(false);
        }
        if (info.getTitle() != null && !info.getTitle().isEmpty()) {
            description.setInnerHTML(info.getTitle());
            /*
             * Issue #11871: to correctly update the offsetWidth of description
             * element we need to clear style width of its parent DIV from old
             * value (in some strange cases this width=[tooltip MAX_WIDTH] after
             * tooltip text has been already updated to new shortly value:
             * 
             * <div class="popupContent"> <div style="width:500px;"> <div
             * class="v-errormessage" aria-hidden="true" style="display: none;">
             * <div class="gwt-HTML"> </div> </div> <div
             * class="v-tooltip-text">This is a short tooltip</div> </div>
             * 
             * and it leads to error during calculation offsetWidth (it is
             * native GWT method getSubPixelOffsetWidth()) of description
             * element")
             */
            description.getParentElement().getStyle().clearWidth();
            description.getStyle().clearDisplay();
        } else {
            description.setInnerHTML("");
            description.getStyle().setDisplay(Display.NONE);
        }
        currentTooltipInfo = info;
    }

    /**
     * Show a popup containing the currentTooltipInfo
     * 
     */
    private void showTooltip() {
        if (currentTooltipInfo.hasMessage()) {
            // Issue #8454: With IE7 the tooltips size is calculated based on
            // the last tooltip's position, causing problems if the last one was
            // in the right or bottom edge. For this reason the tooltip is moved
            // first to 0,0 position so that the calculation goes correctly.
            setPopupPosition(0, 0);

            setPopupPositionAndShow(new PositionCallback() {
                @Override
                public void setPosition(int offsetWidth, int offsetHeight) {

                    if (offsetWidth > getMaxWidth()) {
                        setWidth(getMaxWidth() + "px");

                        // Check new height and width with reflowed content
                        offsetWidth = getOffsetWidth();
                        offsetHeight = getOffsetHeight();
                    }

                    int x = 0;
                    int y = 0;
                    if (BrowserInfo.get().isTouchDevice()) {
                        setMaxWidth(Window.getClientWidth());
                        offsetWidth = getOffsetWidth();
                        offsetHeight = getOffsetHeight();

                        x = getFinalTouchX(offsetWidth);
                        y = getFinalTouchY(offsetHeight);
                    } else {
                        x = getFinalX(offsetWidth);
                        y = getFinalY(offsetHeight);
                    }

                    setPopupPosition(x, y);
                    sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
                }

                /**
                 * Return the final X-coordinate of the tooltip based on cursor
                 * position, size of the tooltip, size of the page and necessary
                 * margins.
                 * 
                 * @param offsetWidth
                 * @return The final X-coordinate
                 */
                private int getFinalX(int offsetWidth) {
                    int x = 0;
                    int widthNeeded = 10 + MARGIN + offsetWidth;
                    int roomLeft = tooltipEventMouseX;
                    int roomRight = Window.getClientWidth() - roomLeft;
                    if (roomRight > widthNeeded) {
                        x = tooltipEventMouseX + 10 + Window.getScrollLeft();
                    } else {
                        x = tooltipEventMouseX + Window.getScrollLeft() - 10
                                - offsetWidth;
                    }
                    if (x + offsetWidth + MARGIN - Window.getScrollLeft() > Window
                            .getClientWidth()) {
                        x = Window.getClientWidth() - offsetWidth - MARGIN
                                + Window.getScrollLeft();
                    }

                    if (tooltipEventMouseX != EVENT_XY_POSITION_OUTSIDE) {
                        // Do not allow x to be zero, for otherwise the tooltip
                        // does not close when the mouse is moved (see
                        // isTooltipOpen()). #15129
                        int minX = Window.getScrollLeft() + MARGIN;
                        x = Math.max(x, minX);
                    }
                    return x;
                }

                /**
                 * Return the final X-coordinate of the tooltip based on cursor
                 * position, size of the tooltip, size of the page and necessary
                 * margins.
                 *
                 * @param offsetWidth
                 * @return The final X-coordinate
                 */
                private int getFinalTouchX(int offsetWidth) {
                    int x = 0;
                    int widthNeeded = 10 + offsetWidth;
                    int roomLeft = currentElement != null ? currentElement
                            .getAbsoluteLeft() : EVENT_XY_POSITION_OUTSIDE;
                    int viewPortWidth = Window.getClientWidth();
                    int roomRight = viewPortWidth - roomLeft;
                    if (roomRight > widthNeeded) {
                        x = roomLeft;
                    } else {
                        x = roomLeft - offsetWidth;
                    }
                    if (x + offsetWidth - Window.getScrollLeft() > viewPortWidth) {
                        x = viewPortWidth - offsetWidth
                                + Window.getScrollLeft();
                    }

                    if (roomLeft != EVENT_XY_POSITION_OUTSIDE) {
                        // Do not allow x to be zero, for otherwise the tooltip
                        // does not close when the mouse is moved (see
                        // isTooltipOpen()). #15129
                        int minX = Window.getScrollLeft();
                        x = Math.max(x, minX);
                    }
                    return x;
                }

                /**
                 * Return the final Y-coordinate of the tooltip based on cursor
                 * position, size of the tooltip, size of the page and necessary
                 * margins.
                 * 
                 * @param offsetHeight
                 * @return The final y-coordinate
                 * 
                 */
                private int getFinalY(int offsetHeight) {
                    int y = 0;
                    int heightNeeded = 10 + offsetHeight;
                    int roomAbove = tooltipEventMouseY;
                    int roomBelow = Window.getClientHeight() - roomAbove;

                    if (roomBelow > heightNeeded) {
                        y = tooltipEventMouseY + 10 + Window.getScrollTop();
                    } else {
                        y = tooltipEventMouseY + Window.getScrollTop() - 10
                                - offsetHeight;
                    }

                    if (y + offsetHeight + MARGIN - Window.getScrollTop() > Window
                            .getClientHeight()) {
                        y = tooltipEventMouseY - 5 - offsetHeight
                                + Window.getScrollTop();
                        if (y - Window.getScrollTop() < 0) {
                            // tooltip does not fit on top of the mouse either,
                            // put it at the top of the screen
                            y = Window.getScrollTop();
                        }
                    }

                    if (tooltipEventMouseY != EVENT_XY_POSITION_OUTSIDE) {
                        // Do not allow y to be zero, for otherwise the tooltip
                        // does not close when the mouse is moved (see
                        // isTooltipOpen()). #15129
                        int minY = Window.getScrollTop() + MARGIN;
                        y = Math.max(y, minY);
                    }
                    return y;
                }

                /**
                 * Return the final Y-coordinate of the tooltip based on cursor
                 * position, size of the tooltip, size of the page and necessary
                 * margins.
                 *
                 * @param offsetHeight
                 * @return The final y-coordinate
                 *
                 */
                private int getFinalTouchY(int offsetHeight) {
                    int y = 0;
                    int heightNeeded = 10 + offsetHeight;
                    int roomAbove = currentElement != null ? currentElement
                            .getAbsoluteTop()
                            + currentElement.getOffsetHeight()
                            : EVENT_XY_POSITION_OUTSIDE;
                    int roomBelow = Window.getClientHeight() - roomAbove;

                    if (roomBelow > heightNeeded) {
                        y = roomAbove;
                    } else {
                        y = roomAbove
                                - offsetHeight
                                - (currentElement != null ? currentElement
                                        .getOffsetHeight() : 0);
                    }

                    if (y + offsetHeight - Window.getScrollTop() > Window
                            .getClientHeight()) {
                        y = roomAbove - 5 - offsetHeight
                                + Window.getScrollTop();
                        if (y - Window.getScrollTop() < 0) {
                            // tooltip does not fit on top of the mouse either,
                            // put it at the top of the screen
                            y = Window.getScrollTop();
                        }
                    }

                    if (roomAbove != EVENT_XY_POSITION_OUTSIDE) {
                        // Do not allow y to be zero, for otherwise the tooltip
                        // does not close when the mouse is moved (see
                        // isTooltipOpen()). #15129
                        int minY = Window.getScrollTop();
                        y = Math.max(y, minY);
                    }
                    return y;
                }
            });
        } else {
            hide();
        }
    }

    /**
     * For assistive tooltips to work correctly we must have the tooltip visible
     * and attached to the DOM well in advance. For this reason both isShowing
     * and isVisible return false positives. We can't override either of them as
     * external code may depend on this behavior.
     * 
     * @return boolean
     */
    public boolean isTooltipOpen() {
        return super.isShowing() && super.isVisible() && getPopupLeft() > 0
                && getPopupTop() > 0;
    }

    private void closeNow() {
        hide();
        setWidth("");
        closing = false;
        justClosedTimer.schedule(getQuickOpenTimeout());
        justClosed = true;
    }

    private Timer showTimer = new Timer() {
        @Override
        public void run() {
            opening = false;
            showTooltip();
        }
    };

    private Timer closeTimer = new Timer() {
        @Override
        public void run() {
            closeNow();
        }
    };

    private Timer justClosedTimer = new Timer() {
        @Override
        public void run() {
            justClosed = false;
        }
    };

    public void hideTooltip() {
        if (currentTooltipInfo instanceof LazyTooltipInfo) {
            Logger.getLogger(VLazyTooltip.class.getName()).info("(LZT) making lazy tooltip invisible");
            ((LazyTooltipInfo) currentTooltipInfo).setVisible(false);
        }
        if (opening) {
            showTimer.cancel();
            opening = false;
        }
        if (!isAttached()) {
            return;
        }
        if (closing) {
            // already about to close
            return;
        }
        if (isTooltipOpen()) {
            closeTimer.schedule(getCloseTimeout());
            closing = true;
        }
    }

    @Override
    public void hide() {
        em.updateMessage("");
        description.setInnerHTML("");

        updatePosition(null, true);
        setPopupPosition(tooltipEventMouseX, tooltipEventMouseY);
    }

    private int EVENT_XY_POSITION_OUTSIDE = -5000;
    private int tooltipEventMouseX;
    private int tooltipEventMouseY;

    public void updatePosition(Event event, boolean isFocused) {
        tooltipEventMouseX = getEventX(event, isFocused);
        tooltipEventMouseY = getEventY(event, isFocused);
    }

    private int getEventX(Event event, boolean isFocused) {
        return isFocused ? EVENT_XY_POSITION_OUTSIDE : event.getClientX();
    }

    private int getEventY(Event event, boolean isFocused) {
        return isFocused ? EVENT_XY_POSITION_OUTSIDE : event.getClientY();
    }

    @Override
    public void onBrowserEvent(Event event) {
        final int type = DOM.eventGetType(event);
        // cancel closing event if tooltip is mouseovered; the user might want
        // to scroll of cut&paste

        if (type == Event.ONMOUSEOVER) {
            // Cancel closing so tooltip stays open and user can copy paste the
            // tooltip
            closeTimer.cancel();
            closing = false;
        }
    }

    /**
     * Replace current open tooltip with new content
     */
    public void replaceCurrentTooltip() {
        if (closing) {
            closeTimer.cancel();
            closeNow();
            justClosedTimer.cancel();
            justClosed = false;
        }

        showTooltip();
        opening = false;
    }

    private class TooltipEventHandler implements MouseMoveHandler,
            KeyDownHandler, FocusHandler, BlurHandler, MouseDownHandler {

        /**
         * Marker for handling of tooltip through focus
         */
        private boolean handledByFocus;

        /**
         * Locate the tooltip for given element
         * 
         * @param element
         *            Element used in search
         * @return TooltipInfo if connector and tooltip found, null if not
         */
        private TooltipInfo getTooltipFor(Element element) {
            ApplicationConnection ac = getApplicationConnection();
            ComponentConnector connector = Util.getConnectorForElement(ac,
                    RootPanel.get(), element);
            // Try to find first connector with proper tooltip info
            TooltipInfo info = null;
            while (connector != null) {

                lazyTooltipConnector = null;
                for (ServerConnector childConnector : connector.getChildren()) {
                    if (childConnector instanceof LazyTooltipConnector) {
                        lazyTooltipConnector = (LazyTooltipConnector) childConnector;
                        break;
                    }
                }
                if (lazyTooltipConnector != null) {
                    info = lazyTooltipConnector.getLazyTooltipInfo(getParentWidget(element));
                } else {
                    info = connector.getTooltipInfo(element);
                }

                if (info != null && info.hasMessage()) {
                    break;
                }

                if (!(connector.getParent() instanceof ComponentConnector)) {
                    connector = null;
                    info = null;
                    break;
                }
                connector = (ComponentConnector) connector.getParent();
            }

            if (connector != null && info != null) {
                assert connector.hasTooltip() : "getTooltipInfo for "
                        + Util.getConnectorString(connector)
                        + " returned a tooltip even though hasTooltip claims there are no tooltips for the connector.";
                return info;

            }

            return null;
        }

        private Widget getParentWidget(Element e) {
            Element element = e;
            while (element != null) {
                EventListener eventListener = Event.getEventListener(element);
                if (eventListener instanceof Widget) {
                    return (Widget) eventListener;
                }
                element = element.getParentElement();
            }
            return null;
        }

        /**
         * Handle hide event
         * 
         */
        private void handleHideEvent() {
            hideTooltip();
        }

        @Override
        public void onMouseMove(MouseMoveEvent mme) {
            handleShowHide(mme, false);
        }

        public void updateTooltip() {
            handleShowHide(null, false);
        }

        @Override
        public void onMouseDown(MouseDownEvent event) {
            handleHideEvent();
        }

        @Override
        public void onKeyDown(KeyDownEvent event) {
            handleHideEvent();
        }

        /**
         * Displays Tooltip when page is navigated with the keyboard.
         * 
         * Tooltip is not visible. This makes it possible for assistive devices
         * to recognize the tooltip.
         */
        @Override
        public void onFocus(FocusEvent fe) {
            handleShowHide(fe, true);
        }

        /**
         * Hides Tooltip when the page is navigated with the keyboard.
         * 
         * Removes the Tooltip from page to make sure assistive devices don't
         * recognize it by accident.
         */
        @Override
        public void onBlur(BlurEvent be) {
            handledByFocus = false;
            handleHideEvent();
        }

        private void handleShowHide(DomEvent<?> domEvent, boolean isFocused) {
            boolean nativeEvent = (domEvent != null);
            Event event = nativeEvent ? Event.as(domEvent.getNativeEvent()) : null;
            Element element = null;
            if (nativeEvent) {
                Widget widget = getParentWidget(Element.as(event.getEventTarget()));
                if (widget != null) {
                    element = widget.getElement();
                }
            } else {
                element = currentElement;
            }
            if (element == null) {
                return;
            }

            // We can ignore move event if it's handled by move or over already
            if (currentElement == element && handledByFocus == true) {
                return;
            }

            TooltipInfo info = null;
            if (currentElement == element && currentTooltipInfo instanceof LazyTooltipInfo) {
                LazyTooltipInfo lazyTooltipInfo = (LazyTooltipInfo) currentTooltipInfo;
                if (!lazyTooltipInfo.isNeedUpdate() || !lazyTooltipInfo.isVisible()) {
                    return;
                }
                lazyTooltipInfo.setNeedUpdate(false);
                info = lazyTooltipInfo;
            } else {

                if (currentElement != element) {
                    currentTooltipInfo = new TooltipInfo(" ");
                }
                // If the parent (sub)component already has a tooltip open and it
                // hasn't changed, we ignore the event.
                // TooltipInfo contains a reference to the parent component that is
                // checked in it's equals-method.

                if (currentElement != null && isTooltipOpen() && currentTooltipInfo != null && nativeEvent) {
                    TooltipInfo newTooltip = getTooltipFor(element);
                    if (currentTooltipInfo.equals(newTooltip)) {
                        return;
                    }
                }

                if (nativeEvent) {
                    info = getTooltipFor(element);
                }
            }
            if (info == null) {
                handleHideEvent();
            } else {
                if (closing) {
                    closeTimer.cancel();
                    closing = false;
                }

                if (isTooltipOpen() && nativeEvent) {
                    closeNow();
                }

                setTooltipText(info);
                if (nativeEvent) {
                    updatePosition(event, isFocused);
                }
                // Schedule timer for showing the tooltip according to if it
                // was recently closed or not.

                if (BrowserInfo.get().isIOS()) {
                    element.focus();
                }

                int timeout = justClosed ? getQuickOpenDelay() : getOpenDelay();
                if (timeout == 0) {
                    showTooltip();
                } else {
                    showTimer.schedule(timeout);
                    opening = true;
                }
            }

            handledByFocus = isFocused;
            currentElement = element;
        }
    }

    private final TooltipEventHandler tooltipEventHandler = new TooltipEventHandler();

    /**
     * Connects DOM handlers to widget that are needed for tooltip presentation.
     * 
     * @param widget
     *            Widget which DOM handlers are connected
     */
    public void connectHandlersToWidget(Widget widget) {
        widget.addDomHandler(tooltipEventHandler, MouseMoveEvent.getType());
        widget.addDomHandler(tooltipEventHandler, MouseDownEvent.getType());
        widget.addDomHandler(tooltipEventHandler, KeyDownEvent.getType());
        widget.addDomHandler(tooltipEventHandler, FocusEvent.getType());
        widget.addDomHandler(tooltipEventHandler, BlurEvent.getType());
    }

    /**
     * Returns the unique id of the tooltip element.
     * 
     * @return String containing the unique id of the tooltip, which always has
     *         a value
     */
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setPopupPositionAndShow(PositionCallback callback) {
        if (isAttached()) {
            callback.setPosition(getOffsetWidth(), getOffsetHeight());
        } else {
            super.setPopupPositionAndShow(callback);
        }
    }

    /**
     * Returns the time (in ms) the tooltip should be displayed after an event
     * that will cause it to be closed (e.g. mouse click outside the component,
     * key down).
     * 
     * @return The close timeout (in ms)
     */
    public int getCloseTimeout() {
        return closeTimeout;
    }

    /**
     * Sets the time (in ms) the tooltip should be displayed after an event that
     * will cause it to be closed (e.g. mouse click outside the component, key
     * down).
     * 
     * @param closeTimeout
     *            The close timeout (in ms)
     */
    public void setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
    }

    /**
     * Returns the time (in ms) during which {@link #getQuickOpenDelay()} should
     * be used instead of {@link #getOpenDelay()}. The quick open delay is used
     * when the tooltip has very recently been shown, is currently hidden but
     * about to be shown again.
     * 
     * @return The quick open timeout (in ms)
     */
    public int getQuickOpenTimeout() {
        return quickOpenTimeout;
    }

    /**
     * Sets the time (in ms) that determines when {@link #getQuickOpenDelay()}
     * should be used instead of {@link #getOpenDelay()}. The quick open delay
     * is used when the tooltip has very recently been shown, is currently
     * hidden but about to be shown again.
     * 
     * @param quickOpenTimeout
     *            The quick open timeout (in ms)
     */
    public void setQuickOpenTimeout(int quickOpenTimeout) {
        this.quickOpenTimeout = quickOpenTimeout;
    }

    /**
     * Returns the time (in ms) that should elapse before a tooltip will be
     * shown, in the situation when a tooltip has very recently been shown
     * (within {@link #getQuickOpenDelay()} ms).
     * 
     * @return The quick open delay (in ms)
     */
    public int getQuickOpenDelay() {
        return quickOpenDelay;
    }

    /**
     * Sets the time (in ms) that should elapse before a tooltip will be shown,
     * in the situation when a tooltip has very recently been shown (within
     * {@link #getQuickOpenDelay()} ms).
     * 
     * @param quickOpenDelay
     *            The quick open delay (in ms)
     */
    public void setQuickOpenDelay(int quickOpenDelay) {
        this.quickOpenDelay = quickOpenDelay;
    }

    /**
     * Returns the time (in ms) that should elapse after an event triggering
     * tooltip showing has occurred (e.g. mouse over) before the tooltip is
     * shown. If a tooltip has recently been shown, then
     * {@link #getQuickOpenDelay()} is used instead of this.
     * 
     * @return The open delay (in ms)
     */
    public int getOpenDelay() {
        return openDelay;
    }

    /**
     * Sets the time (in ms) that should elapse after an event triggering
     * tooltip showing has occurred (e.g. mouse over) before the tooltip is
     * shown. If a tooltip has recently been shown, then
     * {@link #getQuickOpenDelay()} is used instead of this.
     * 
     * @param openDelay
     *            The open delay (in ms)
     */
    public void setOpenDelay(int openDelay) {
        this.openDelay = openDelay;
    }

    /**
     * Sets the maximum width of the tooltip popup.
     * 
     * @param maxWidth
     *            The maximum width the tooltip popup (in pixels)
     */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * Returns the maximum width of the tooltip popup.
     * 
     * @return The maximum width the tooltip popup (in pixels)
     */
    public int getMaxWidth() {
        return maxWidth;
    }

    public void updateTooltip(String elementId, String tooltipText) {
        if ((currentTooltipInfo != null) && (currentTooltipInfo instanceof LazyTooltipInfo)) {
            LazyTooltipInfo lazyTooltipInfo = (LazyTooltipInfo) currentTooltipInfo;
            String current = lazyTooltipInfo.getTitle();
            boolean changed = (tooltipText == null) ? (current != null) : !tooltipText.equals(current);
            if (changed) {
                lazyTooltipInfo.setTitle(tooltipText);
                lazyTooltipInfo.setNeedUpdate(true);
            }
        }
        tooltipEventHandler.updateTooltip();
    }

    public boolean isActiveTooltip(long tooltipId) {
        return (this.tooltipId == tooltipId);
    }

    public long getNewTooltipId() {
        return ++tooltipId;
    }

}