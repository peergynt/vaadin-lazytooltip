package org.vaadin.addons.lazytooltip.client;

import com.vaadin.client.TooltipInfo;

public class LazyTooltipInfo extends TooltipInfo {

    private boolean needUpdate = false;
    private boolean visible = true;

    public LazyTooltipInfo() {
    }

    public LazyTooltipInfo(String tooltip) {
        super(tooltip);
    }

    public LazyTooltipInfo(String tooltip, String errorMessage) {
        super(tooltip, errorMessage);
    }

    public LazyTooltipInfo(String tooltip, String errorMessage, Object identifier) {
        super(tooltip, errorMessage, identifier);
    }

    public boolean isNeedUpdate() {
        return needUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
