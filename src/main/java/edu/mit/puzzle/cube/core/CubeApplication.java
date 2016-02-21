package edu.mit.puzzle.cube.core;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CubeApplication extends Application{

    @Override
    public synchronized Restlet createInboundRoot() {
        getTunnelService().setEnabled(true);
        getTunnelService().setExtensionsTunnel(true);
        return this;
    }

    public static void main(String[] args) throws Exception {
        String contextLaunchPoint = "development-config.xml";

        ApplicationContext springContext = new ClassPathXmlApplicationContext(contextLaunchPoint);

        Component component = ((Component) springContext.getBean("top"));
        component.start();
    }

}
