package org.vaadin.addons.lazytooltip.client;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.shared.communication.SharedState;

public class LazyTooltipState extends SharedState {

    private static final long serialVersionUID = 1L;

    public Set<String> handledWidgets = new HashSet<String>();

}
