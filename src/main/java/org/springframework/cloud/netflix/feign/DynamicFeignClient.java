package org.springframework.cloud.netflix.feign;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import ru.x100.spring.cloud.netflix.feign.utils.FeignClientInterfaceGenerator;

import java.text.MessageFormat;
import java.util.Map;

@SuppressWarnings("unchecked")
@Builder
public class DynamicFeignClient<T> {

    @NonNull private String microServiceName;
    @NonNull private String serviceUrl;
    @NonNull private Class<T> feignClientClass;
    @NonNull private ConfigurableApplicationContext context;

    @SneakyThrows
    public T create() {
        String beanName = MessageFormat.format("{0}-feign-client", microServiceName);

        Map<String, T> beanNames = context.getBeansOfType(feignClientClass);
        if (beanNames.containsKey(beanName)) {
            return (T) context.getBean(beanName);
        }
        Class newFeignClientInterface = FeignClientInterfaceGenerator.newFeignClientInterface(beanName, feignClientClass, microServiceName, serviceUrl);
        AbstractBeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class)
                .addPropertyValue("name", microServiceName)
                .addPropertyValue("url", "")
                .addPropertyValue("path", "")
                .addPropertyValue("type", newFeignClientInterface.getName())
                .addPropertyValue("decode404", false)
                .getBeanDefinition();

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();
        registry.registerBeanDefinition(beanName, definition);

        return (T) context.getBean(beanName);
    }
}
