# vaadin-lazytooltip

Lazy Tooltip Add-on is an add-on for [Vaadin](https://vaadin.com). It requires Vaadin 7.3+.

## Installation

The Vaadin add-ons installation is described on the Vaadin website: https://vaadin.com/directory/help/using-vaadin-add-ons/

Note that the Lazy Tooltip Add-on will replace the VTooltip widget with VLazyTooltip (see `LazyTooltipWidgetset.gwt.xml`).

## How to Build

This add-on is built with maven. To build and run the demo:

`mvn clean install`

`cd lazytooltip-demo`

`mvn jetty:run`

To see the demo, navigate to http://localhost:8080/

## Usage

A Lazy Tooltip can be added to any Vaadin component:

    LazyTooltip lazyTooltip = LazyTooltip.addToComponent(component);

Once added to a component, you should define a handler for the tooltip:

    lazyTooltip.setTooltipHandler(handler);

A Lazy Tootlip handler must implement `generateTooltip()`:

    public interface LazyTooltipHandler {
        public void generateTooltip(LazyTooltipUpdater updater);
    }

`generateTooltip()` is invoked (lazily) when a tooltip is requested by the client-side widget.
`LazyTooltipUpdater` is the interface that allows the tooltip to be updated.
`updateTooltip(String tooltip)` must be used to update the content of the tooltip.
The content of the tooltip will be displayed as HTML (see `AbstractComponent.setDescription(String description)`).

With Java 8, you could use a lambda expression to define the tooltip handler:

    lazyTooltip.setTooltipHandler((LazyTooltipUpdater updater) ->
        updater.updateTooltip("This is a static tooltip"));

Note that `updateTooltip()` must be invoked from the Vaadin UI thread to properly update the client component.

`updateTooltip()` can also be called multiple times and the tooltip will be updated dynamically on the client side as long as the tooltip is open.

## License

vaadin-lazytooltip is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
