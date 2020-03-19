import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.view.*;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
@EnableWebMvc

public class MvcConfig{
   @Bean
   public ViewResolver viewResolver() {
      InternalResourceViewResolver bean = new InternalResourceViewResolver();
      bean.setViewClass(JstlView.class);
      bean.setPrefix("/");
      bean.setSuffix(".jsp");
      bean.setOrder(0);
      return bean;
   }
   
   @Bean("multipartResolver")
   public StandardServletMultipartResolver multipartResolver() {
      StandardServletMultipartResolver bean = new StandardServletMultipartResolver();
      return bean;
   }
}