package com.merlin.minispring.mvcframework.servlet;

import com.merlin.minispring.mvcframework.annotation.MLController;
import com.merlin.minispring.mvcframework.annotation.MLAutowired;
import com.merlin.minispring.mvcframework.annotation.MLRequestMapping;
import com.merlin.minispring.mvcframework.annotation.MLService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * 当Servlet容器启动时，会调用GPDispatcherServlet的init()方法，
 * 从init方法的参数中，可以拿到主配置文件的路径，从能够读取到配置文件中的信息。
 * 现在完成(Spring的三个阶段中)初始化阶段的代码。
 */
public class MLDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = -1029760016507746890L;

    // same as param-name in web.xml
    private static final String LOCATION = "contextConfigLocation";

    // to save all config info
    private Properties p = new Properties();

    // to save all scanned class names.
    private List<String> classNames = new ArrayList<String>();

    // core IOC container, to save all initialized bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    // to save mapping between url and method
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public MLDispatcherServlet() {
        super();
    }

    /**
     * Initialize, load config file.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //super.init(config);
        // 1. load config file
        doLoadConfig(config.getInitParameter(LOCATION));

        // 2. scan according classes
        doScanner(p.getProperty("scanPackage"));

        // 3. initialize according instances, and save to ioc container.
        doInstance();

        // 4. Dependency Injection
        doAutowired();

        // 5. to build HandlerMapping
        initHandlerMapping();

        // wait for request, match url + method, reflect invoke to execute
        // execute doGet or doPost method

        System.out.println("ML mvcframework is init!");
    }

    /**
     * 将文件读取到Properties对象中
     * @param location
     */
    private void doLoadConfig(String location) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            // load config file
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 递归扫描出所有的Class文件
     * @param packageName
     */
    private void doScanner(String packageName) {
        // change all package dir to file dir
        System.out.println("PackageName:" + packageName);
        String srcName = "/" + packageName.replaceAll("\\.", "/");
        //String srcName = "/" + packageName.replace(".", "/");
        System.out.println("URL:" + srcName);
        URL url = this.getClass().getClassLoader().getResource(srcName);

        File dir = new File(url.getFile());
        for(File file : dir.listFiles()) {
            // recursive if directory
            if(file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    /**
     * 初始化所有相关的类，并放入到IOC容器之中。IOC容器的key默认是类名首字母小写，如果是自己设置类名，则优先使用自定义的。
     * 因此，要先写一个针对类名首字母处理的工具方法。
     */
    private void doInstance() {
        if(classNames.size() == 0) { return;}
        try {
            for(String className: classNames) {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(MLController.class)) {
                    // by default, 首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if(clazz.isAnnotationPresent(MLService.class)) {
                    MLService service = clazz.getAnnotation(MLService.class);
                    String beanName = service.value();
                    // if user has set name, then use it.
                    if(!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    // if use do not set, then new a instance according by interface type
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将初始化到IOC容器中的类，需要赋值的字段进行赋值
     */
    private void doAutowired() {
        if(ioc.isEmpty()) { return;}
        for(Entry<String, Object> entry: ioc.entrySet()) {
            // Get all attributes of instance object
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field: fields) {
                if(!field.isAnnotationPresent(MLAutowired.class)) { continue;}

                MLAutowired autowired = field.getAnnotation(MLAutowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true); // set privilege to access private attribute
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * 将GPRequestMapping中配置的信息和Method进行关联，并保存这些关系。
     */
    private void initHandlerMapping() {
        if(ioc.isEmpty()) { return;}

        for(Entry<String, Object> entry : ioc.entrySet() ) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MLController.class)) { return;}

            String baseUrl = "";
            // Get Controller's url config
            if(clazz.isAnnotationPresent(MLRequestMapping.class)) {
                MLRequestMapping requestMapping = clazz.getAnnotation(MLRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            // Get Method's url config
            Method[] methods = clazz.getMethods();
            for(Method method : methods) {
                //ignore if no RequestMapping Annotation
                if(!method.isAnnotationPresent(MLRequestMapping.class)) {continue;}

                // Mapping URL
                MLRequestMapping requestMapping = method.getAnnotation(MLRequestMapping.class);
                String url = "/" + baseUrl + "/" + requestMapping.value();
                url = url.replaceAll("/+", "/");
                //String url = ("/" + baseUrl + "/" + requestMapping.value()).replace("//", "/");
                handlerMapping.put(url, method);
                System.out.println("mapped " + url + "," + method);
            }
        }
    }



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * execute buz logic
     * 运行阶段，当用户发送请求被Servlet接受时，都会统一调用doPost方法，先在doPost方法中再调用doDispach()方法
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.doPost(req, resp);
        try {
            doDispatch(req, resp);
        } catch(Exception e) {
            //
            resp.getWriter().write("500 Exception, Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replace("\\[|\\", "").replaceAll("\\s", "\r\n"));

        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(this.handlerMapping.isEmpty()) {return;}

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        if(!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("400 Not Found!");
            return;
        }

        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        // 获取方法的参数列表
        Class<?>[] paramTypes = method.getParameterTypes();
        // Get request params
        Map<String, String[]> paramMap = req.getParameterMap();
        // Save param value
        Object[] paramValues = new Object[paramTypes.length];
        // method's param list
        for(int i = 0; i< paramTypes.length; i++) {
            // do something according by param's name
            Class paramType = paramTypes[i];
            if(paramType == HttpServletRequest.class) {
                //
                paramValues[i] = req;
                continue;
            } else if(paramType == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else if(paramType == String.class) {
                for(Entry<String, String[]> param: paramMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll("\\s", ",");
                    paramValues[i] = value;
                }
            } else if(paramType == Integer.class) {
                for (Entry<String, String[]> param : paramMap.entrySet()) {
                    String tmp = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll("\\s", ",");
                    Integer value = Integer.valueOf(tmp);
                    paramValues[i] = value;
                }
            }
        }

        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            method.invoke(this.ioc.get(beanName), paramValues);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // 首字母转为小写
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
