import org.springframework.web.*;
import org.springframework.web.context.support.*;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.MultipartConfigElement;
import java.io.*;
import org.springframework.context.annotation.*;
import org.springframework.web.context.*;
import org.springframework.web.servlet.DispatcherServlet;
import rootContextBeans.StaticServlet;

public class initializer implements WebApplicationInitializer{
    
    @Override
    public void onStartup(ServletContext container) throws ServletException{
        
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.scan("rootContextBeans");
        container.addListener(new ContextLoaderListener(ctx)); // root context
        
        AnnotationConfigWebApplicationContext servlet_ctx = new AnnotationConfigWebApplicationContext();
        servlet_ctx.scan("viewControllerBeans");
        
        ServletRegistration.Dynamic dispatcher = container.addServlet("dispatcherServlet", new DispatcherServlet(servlet_ctx));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/boards/*"); // DispatcherServlet
        
        AnnotationConfigWebApplicationContext ajax_servlet_ctx = new AnnotationConfigWebApplicationContext();
        ajax_servlet_ctx.register(MvcConfig.class);
        ajax_servlet_ctx.scan("ajaxControllerBeans");
        
        ServletRegistration.Dynamic ajaxDispatcher = container.addServlet("ajaxDispatcherServlet", new DispatcherServlet(ajax_servlet_ctx));
        ajaxDispatcher.setLoadOnStartup(1);
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("catalina.base")
        + "/webapps/root/WEB-INF/upload_temp", 
          1024*1024*25, 1024*1024*12, 1024*1024*100); // last digit is mbs
        ajaxDispatcher.setMultipartConfig(multipartConfigElement);
        ajaxDispatcher.addMapping("/takaba/*"); // ajax
        
        ServletRegistration.Dynamic staticResourcesDispatcher = container.addServlet("staticResourcesServlet", new StaticServlet());
        staticResourcesDispatcher.setLoadOnStartup(1);
        staticResourcesDispatcher.addMapping("/res/*");
    }
}