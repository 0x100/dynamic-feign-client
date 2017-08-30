package ru.x100.spring.cloud.netflix.feign.utils;

import com.google.common.base.CaseFormat;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import lombok.SneakyThrows;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class FeignClientInterfaceGenerator {

    private static final String GET = "get";
    private static final String DOWNLOAD = "download";
    private static final String UPDATE = "update";
    private static final String DELETE = "delete";

    @SneakyThrows
    public static <T> Class newFeignClientInterface(String name, Class<T> oldFeignClientInterface, String microServiceName, String serviceUrl) {
        String newInterfaceName = beanNameToCamelCase(name);
        CtClass newInterface = copyInterface(oldFeignClientInterface, newInterfaceName);
        ClassPool classPool = ClassPool.getDefault();
        CtClass oldInterface = classPool.get(oldFeignClientInterface.getName());
        newInterface.setSuperclass(oldInterface);
        addFeignClientAnnotation(microServiceName, newInterface);

        for (CtMethod method : newInterface.getDeclaredMethods()) {
            addRequestMappingAnnotation(newInterface, method, serviceUrl);
        }
        return newInterface.toClass(oldFeignClientInterface.getClassLoader(), null);
    }

    private static <T> CtClass copyInterface(Class<T> oldFeignClientInterface, String newInterfaceName) throws NotFoundException {
        ClassPool classPool = ClassPool.getDefault();
        checkClassPath(oldFeignClientInterface, classPool);
        CtClass oldInterface = classPool.get(oldFeignClientInterface.getName());
        oldInterface.defrost();
        oldInterface.setName(newInterfaceName);
        return oldInterface;
    }

    private static void addFeignClientAnnotation(String microServiceName, CtClass newInterface) {
        ClassFile ccFile = newInterface.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(FeignClient.class.getName(), constpool);
        annotation.addMemberValue("value", new StringMemberValue(microServiceName, constpool));
        attr.addAnnotation(annotation);
        ccFile.addAttribute(attr);
    }

    private static void addRequestMappingAnnotation(CtClass newInterface, CtMethod method, String serviceUrl) {
        ClassFile ccFile = newInterface.getClassFile();
        ConstPool constpool = ccFile.getConstPool();

        ArrayMemberValue values = new ArrayMemberValue(constpool);
        values.setValue(new MemberValue[]{new StringMemberValue(serviceUrl, constpool)});

        EnumMemberValue httpMethod = new EnumMemberValue(constpool);
        httpMethod.setType(RequestMethod.class.getName());
        httpMethod.setValue(getHttpMethod(method.getName()).name());

        ArrayMemberValue httpMethods = new ArrayMemberValue(constpool);
        httpMethods.setValue(new MemberValue[]{httpMethod});

        AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
        Annotation annotation = new Annotation(RequestMapping.class.getName(), constpool);
        annotation.addMemberValue("value", values);
        annotation.addMemberValue("method", httpMethods);
        attr.addAnnotation(annotation);

        method.getMethodInfo().addAttribute(attr);
    }

    private static String beanNameToCamelCase(String name) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name);
    }

    private static RequestMethod getHttpMethod(String methodName) {
        RequestMethod httpMethod;
        if (methodName.startsWith(GET) || methodName.startsWith(DOWNLOAD)) {
            httpMethod = RequestMethod.GET;
        } else if (methodName.startsWith(UPDATE)) {
            httpMethod = RequestMethod.PUT;
        } else if (methodName.startsWith(DELETE)) {
            httpMethod = RequestMethod.DELETE;
        } else {
            httpMethod = RequestMethod.POST;
        }
        return httpMethod;
    }

    private static <T> void checkClassPath(Class<T> clazz, ClassPool classPool) {
        if (classPool.find(clazz.getName()) == null) {
            classPool.appendClassPath(new ClassClassPath(clazz));
        }
    }
}
