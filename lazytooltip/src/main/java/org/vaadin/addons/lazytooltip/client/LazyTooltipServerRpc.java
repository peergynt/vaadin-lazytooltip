package org.vaadin.addons.lazytooltip.client;

import com.vaadin.shared.communication.ServerRpc;

@FunctionalInterface
public interface LazyTooltipServerRpc extends ServerRpc {
    public void updateTooltip(long tooltipId, String widgetClass, String elementId);
}
