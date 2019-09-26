package com.leyou.servlet;

import com.alibaba.fastjson.JSON;
import com.leyou.annotation.Controller;
import com.leyou.annotation.RequestMapping;
import com.leyou.annotation.Responsbody;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 中央处理器
 *
 * @author lh
 * @version 1.0
 * @date 2019-09-26 14:28
 */
public class MyDispatcherServlet extends HttpServlet {

    // 当前类所在的路径
    static String CLASS_PATH = MyDispatcherServlet.class.getResource("/").getPath();
    // 配置的扫描路径
    static String SCAN_PATH;
    // 基础路径
    static String BASE_PATH;
    // url和类的映射关系map
    static Map<String, Method> map = new HashMap();


    /**
     * get请求
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    /**
     * post请求
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 获取请求的uri
            String requestURI = req.getRequestURI();
            // 从map取出对应的方法信息
            Method method = map.get(requestURI);
            // 这里其实在spring mvc是在spring中获取对象的
            Class<?> declaringClass = method.getDeclaringClass();
            Object obj = declaringClass.newInstance();
            // 获取形式参数
            Parameter[] parameters = method.getParameters();
            // 定义实际参数
            Object[] objects = new Object[parameters.length];
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    String parameterName = parameters[i].getName();
                    if (parameters[i].getType() == HttpServletRequest.class) {
                        objects[i] = req;
                    } else if (parameters[i].getType() == HttpServletResponse.class) {
                        objects[i] = resp;
                    } else if (parameters[i].getType() == String.class) {
                        String parameter = req.getParameter(parameterName);
                        objects[i] = parameter;
                    } else {
                        // 包装接受的实体类
                        Class<?> type = parameters[i].getType();
                        Object o = type.newInstance();
                        Field[] declaredFields = type.getDeclaredFields();
                        for (Field declaredField : declaredFields) {
                            declaredField.setAccessible(true);
                            String name = declaredField.getName();
                            String parameter = req.getParameter(name);
                            declaredField.set(o, parameter);
                        }
                        objects[i] = o;
                    }
                }
            }
            // 调用方法
            Object invoke = method.invoke(obj, objects);
            if (method != null) {
                Responsbody annotation = method.getAnnotation(Responsbody.class);
                if (annotation != null) {
                    resp.getWriter().write(JSON.toJSONString(invoke));
                }
            } else {
                resp.setStatus(404);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化信息
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 取出在web.xml中配置的contextConfigLocation
            String contextConfigLocation = config.getInitParameter("contextConfigLocation");
            CLASS_PATH = CLASS_PATH.replaceAll("%20", "");
            // 构建配置文件的路径
            String newFile = CLASS_PATH + contextConfigLocation;
            // 加载文件
            File file = new File(newFile);
            SCAN_PATH = parseXml(file);
            // 取出当前路径
            File scanfile = new File(CLASS_PATH + SCAN_PATH);
            BASE_PATH = scanfile.getPath();
            // 解析文件
            circleParseObject(scanfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 遍历文件
     *
     * @param scanfile
     */
    public void circleParseObject(File scanfile) {
        try {
            // 如果这个文件是文件夹，那么循环遍历
            if (scanfile.isDirectory()) {
                for (File file : scanfile.listFiles()) {
                    circleParseObject(file);
                }
            } else {
                // 如果是文件
                // 判断是不是class文件
                String name = scanfile.getName();
                // 如果是class文件
                if (name.endsWith(".class")) {
                    // 取出完整类名
                    String path = scanfile.getPath();
                    path = SCAN_PATH + path.replace(BASE_PATH, "");
                    // 获取类的全限定名称
                    String newClassName = path.replaceAll("\\\\", ".");
                    String className = newClassName.substring(0, newClassName.lastIndexOf("."));
                    Class<?> aClass = Class.forName(className);
                    // Object obj = aClass.newInstance();
                    String pre_url = "";
                    // 判断是否加了controller注解
                    if (aClass.isAnnotationPresent(Controller.class)) {
                        RequestMapping annotation = aClass.getAnnotation(RequestMapping.class);
                        if (annotation != null) {
                            pre_url = annotation.value();
                        }
                        Method[] declaredMethods = aClass.getDeclaredMethods();
                        for (Method declaredMethod : declaredMethods) {
                            RequestMapping annotation1 = declaredMethod.getAnnotation(RequestMapping.class);
                            if (annotation1 != null) {
                                String value = annotation1.value();
                                map.put(pre_url + value, declaredMethod);
                                System.out.println("将路径为" + pre_url + value + "映射到了方法" + declaredMethod.getName());
                            }
                        }
                    } else {
                        // 不处理
                    }
                }
            }
            System.out.println("map=" + map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析文件
     *
     * @param file
     * @return
     */
    public String parseXml(File file) {
        try {
            // dom4j进行解析xml文件
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(file);
            // 获取根节点
            Element rootElement = document.getRootElement();
            Element packagescan = rootElement.element("packagescan");
            Attribute attribute = packagescan.attribute("pacakage");
            String packageScan = attribute.getValue();
            return packageScan;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
