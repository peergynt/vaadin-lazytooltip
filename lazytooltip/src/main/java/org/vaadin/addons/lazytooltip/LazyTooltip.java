package org.vaadin.addons.lazytooltip;

import java.util.HashMap;
import java.util.Map;

import org.vaadin.addons.lazytooltip.client.LazyTooltipClientRpc;
import org.vaadin.addons.lazytooltip.client.LazyTooltipServerRpc;
import org.vaadin.addons.lazytooltip.client.LazyTooltipState;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.AbstractComponent;

public class LazyTooltip extends AbstractExtension implements LazyTooltipServerRpc {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final AbstractComponent component;
    private final Map<String, LazyTooltipHandler> handlers = new HashMap<String, LazyTooltipHandler>();

    private LazyTooltip(AbstractComponent component) {
        this.component = component;
        registerRpc(this);
    }

    public static LazyTooltip addToComponent(AbstractComponent component) {
        LazyTooltip lazyTooltip = new LazyTooltip(component);
        lazyTooltip.extend(component);
        return lazyTooltip;
    }

    public void setTooltipHandler(LazyTooltipHandler lazyTooltipHandler) {
        setTooltipHandler(null, lazyTooltipHandler);
    }

    public void setTooltipHandler(String className, LazyTooltipHandler lazyTooltipHandler) {
        handlers.put(className, lazyTooltipHandler);
        getState().handledWidgets.add(className);
    }

    @Override
    public void updateTooltip(long tooltipId, String widgetClass, String elementId) {
        LazyTooltipHandler lazyTooltipHandler = null;
        lazyTooltipHandler = handlers.get(widgetClass);
        if (lazyTooltipHandler == null) {
            // Fallback to default handler
            lazyTooltipHandler = handlers.get(null);
        }
        if (lazyTooltipHandler != null) {
            lazyTooltipHandler.generateTooltip(new LazyTooltipUpdaterImpl(tooltipId, elementId));
        }
    }

    @Override
    public boolean isConnectorEnabled() {
        // Always returns 'enabled' so that lazy tooltips work 
        // even if the extended component is disabled.
        return true;
    };

    @Override
    public LazyTooltipState getState() {
        return (LazyTooltipState) super.getState();
    }

    private LazyTooltipClientRpc getRpc() {
        return getRpcProxy(LazyTooltipClientRpc.class);
    }

    private class LazyTooltipUpdaterImpl implements LazyTooltipUpdater {

        private final long tooltipId;
        private final String elementId;

        public LazyTooltipUpdaterImpl(long tooltipId, String elementId) {
            this.tooltipId = tooltipId;
            this.elementId = elementId;
        }

        @Override
        public void updateTooltip(final String tooltip) {
            getRpc().updateTooltip(tooltipId, elementId, tooltip);
        }

        @Override
        public String getElementId() {
            return elementId;
        }

    }

}
