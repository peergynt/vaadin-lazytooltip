package org.vaadin.addons.demo;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.vaadin.addons.lazytooltip.LazyTooltip;
import org.vaadin.addons.lazytooltip.LazyTooltipHandler;
import org.vaadin.addons.lazytooltip.LazyTooltipUpdater;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("demo")
@Title("LazyTooltip Add-on Demo")
@Push
@SuppressWarnings("serial")
public class DemoUI extends UI
{

    private Random random = new Random();

    private static final String[] fruits = {
        "Apple",     "Apricot",    "Avocado", "Banana",     "Blackberry", "Blueberry", 
        "Cherry",    "Clementine", "Coconut", "Grapefruit", "Kiwi",       "Lemon", 
        "Mango",     "Nectarine",  "Olive",   "Orange",     "Peach",      "Pear",
        "Pineapple", "Pumpkin",    "Raisin",  "Raspberry",  "Strawberry", "Tomato"
    };

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "org.vaadin.addons.demo.DemoWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {

        GridLayout grid = new GridLayout(4, 6);
        grid.setSpacing(true);

        createColumn(regularTooltip, grid, 0);
        createColumn(staticLazyTooltip, grid, 1);
        createColumn(randomLazyTooltip, grid, 2);
        // column 2: lazy tooltip with long back-end process
        // column 3: lazy tooltip with dynamic updates
        createColumn(dynamicLazyTooltip, grid, 3);

        // Show it in the middle of the screen
        final VerticalLayout layout = new VerticalLayout();
        layout.setStyleName("demoContentLayout");
        layout.setSizeFull();
        layout.addComponent(grid);
        layout.setComponentAlignment(grid, Alignment.MIDDLE_CENTER);
        setContent(layout);
    }

    private void createColumn(TooltipConfigurator configurator, GridLayout grid, int columnNumber) {
        int rowNumber = 0;

        Label header = new Label("<b>" + configurator.getTooltipDesccription() + "</b>", ContentMode.HTML);
        header.setWidth(300f, Unit.PIXELS);
        grid.addComponent(header, columnNumber, rowNumber++);

        Button defaultButton = new Button("Default Button");
        defaultButton.setWidth(100f, Unit.PERCENTAGE);
        configurator.configureTooltip(defaultButton);
        grid.addComponent(defaultButton, columnNumber, rowNumber++);

        Button errorButton = new Button("Button with Errors");
        errorButton.setWidth(100f, Unit.PERCENTAGE);
        errorButton.setComponentError(new UserError("This component has an error!"));
        configurator.configureTooltip(errorButton);
        grid.addComponent(errorButton, columnNumber, rowNumber++);

        Button disabledButton = new Button("Disabled Button");
        disabledButton.setWidth(100f, Unit.PERCENTAGE);
        disabledButton.setEnabled(false);
        configurator.configureTooltip(disabledButton);
        grid.addComponent(disabledButton, columnNumber, rowNumber++);

        CheckBox readOnlyCheckBox = new CheckBox("Read-only CheckBox");
        readOnlyCheckBox.setWidth(100f, Unit.PERCENTAGE);
        readOnlyCheckBox.setReadOnly(true);
        configurator.configureTooltip(readOnlyCheckBox);
        grid.addComponent(readOnlyCheckBox, columnNumber, rowNumber++);

        Label label = new Label("Default Label");
        label.setWidth(100f, Unit.PERCENTAGE);
        configurator.configureTooltip(label);
        grid.addComponent(label, columnNumber, rowNumber++);
    }

    interface TooltipConfigurator {
        public void configureTooltip(AbstractComponent component);
        public String getTooltipDesccription();
    }

    private void asyncTooltipUpdate(final LazyTooltipUpdater updater, final String tooltip) {
        UI ui = UI.getCurrent();
        if ((ui == null) || (!ui.isAttached())) {
            return;
        }
        ui.access(new Runnable() {
            public void run() {
                updater.updateTooltip(tooltip);
            }
        });
    }

    private TooltipConfigurator regularTooltip = new TooltipConfigurator() {
        public void configureTooltip(AbstractComponent component) {
            // Standard vaadin tooltip: set the description
            component.setDescription("This is a regular vaadin tooltip");
        }

        public String getTooltipDesccription() {
            return "Normal tooltip";
        }
    };

    private TooltipConfigurator staticLazyTooltip = new TooltipConfigurator() {
        public void configureTooltip(AbstractComponent component) {
            LazyTooltip lazyTooltip = LazyTooltip.addToComponent(component);
            lazyTooltip.setTooltipHandler(new LazyTooltipHandler() {
                @Override
                public void generateTooltip(LazyTooltipUpdater updater) {
                    updater.updateTooltip("This is a static lazy tooltip");
                }
            });
        }

        public String getTooltipDesccription() {
            return "Lazy tooltip with static text";
        }
    };

    private TooltipConfigurator randomLazyTooltip = new TooltipConfigurator() {
        public void configureTooltip(AbstractComponent component) {
            LazyTooltip lazyTooltip = LazyTooltip.addToComponent(component);
            lazyTooltip.setTooltipHandler(new LazyTooltipHandler() {
                @Override
                public void generateTooltip(LazyTooltipUpdater updater) {
                    int n = random.nextInt(fruits.length);
                    updater.updateTooltip("I am a lazy " + fruits[n]);
                }
            });
        }

        public String getTooltipDesccription() {
            return "Lazy tooltip with random text";
        }
    };

    private TooltipConfigurator dynamicLazyTooltip = new TooltipConfigurator() {
        public void configureTooltip(AbstractComponent component) {
            LazyTooltip lazyTooltip = LazyTooltip.addToComponent(component);
            lazyTooltip.setTooltipHandler(new LazyTooltipHandler() {
                @Override
                public void generateTooltip(final LazyTooltipUpdater updater) {
                    final AtomicInteger counter = new AtomicInteger(0);
                    final Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        public void run() {
                            String tooltip = null;
                            int count = counter.getAndIncrement();
                            if (count < 12) {
                                tooltip = "Loading...".substring(0, (count % 3) + 8);
                            } else {
                                tooltip = "All done!!!";
                                timer.cancel();
                            }
                            asyncTooltipUpdate(updater, tooltip);
                        }
                    }, 0, 500);
                }
            });
        }

        public String getTooltipDesccription() {
            return "Lazy tooltip with dynamic updates";
        }
    };

}
