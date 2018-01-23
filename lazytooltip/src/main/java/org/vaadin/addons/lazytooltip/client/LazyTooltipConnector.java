package org.vaadin.addons.lazytooltip.client;

import java.util.Set;
import java.util.logging.Logger;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.TooltipInfo;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import org.vaadin.addons.lazytooltip.LazyTooltip;

@Connect(LazyTooltip.class)
public class LazyTooltipConnector extends AbstractExtensionConnector {

    private static final long serialVersionUID = 1L;

    private AbstractComponentConnector connector;

    private LazyTooltipServerRpc rpc = RpcProxy.create(LazyTooltipServerRpc.class, this);

    @Override
    protected void extend(ServerConnector target) {
        connector = (AbstractComponentConnector) target;
        registerRpc(LazyTooltipClientRpc.class, (tooltipId, elementId, tooltipText) -> {
            if (getWidget().isActiveTooltip(tooltipId)) {
                getLogger().finer("(LZT) updating tooltip " + tooltipId + " in client");
                getWidget().updateTooltip(elementId, tooltipText);
            };
        });
    }

    @Override
    public LazyTooltipState getState() {
        return (LazyTooltipState) super.getState();
    }

    public TooltipInfo getLazyTooltipInfo(Widget widget) {
        if (widget == null) {
            return null;
        }
        Set<String> handled = getState().handledWidgets;
        if (handled.isEmpty()) {
            return null;
        }
        String className = widget.getClass().getName();
        if (!handled.contains(null) && !handled.contains(className)) {
            return null;
        }
        String widgetID = widget.getElement().getId();
        long tooltipId = getWidget().getNewTooltipId();
        getLogger().finer("(LZT) request tooltip update for tooltip " +
                         tooltipId + " (class=" + className + ", widget=" + widgetID + ")");
        rpc.updateTooltip(tooltipId, className, widgetID);
        return new LazyTooltipInfo("Loading...", connector.getState().errorMessage);
    }

    public VLazyTooltip getWidget() {
        return (VLazyTooltip) connector.getConnection().getVTooltip();
    }

    private static Logger getLogger() {
        return Logger.getLogger(LazyTooltipConnector.class.getName());
    }

}
