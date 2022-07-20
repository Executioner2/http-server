package com.ranni.startup;

import com.ranni.annotation.core.WebBootstrap;
import com.ranni.common.SystemProperty;
import com.ranni.container.Container;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ConfigureMap;
import com.ranni.deploy.ServerConfigure;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.loader.AbstractClassLoader;
import com.ranni.naming.FileDirContext;
import com.ranni.naming.Resource;
import com.ranni.naming.ResourceAttributes;
import com.ranni.util.WARDecUtil;

import javax.annotation.processing.FilerException;
import javax.naming.NamingException;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 11:39
 */
public class StandardServerStartup implements ServerStartup {

    // ==================================== 属性字段 ====================================
    
    /**
     * 单例的StandardServerStartup
     */
    private volatile static StandardServerStartup instance;

    /**
     * 扫描线程完成数
     */
    private static AtomicInteger finishCount = new AtomicInteger(0);

    /**
     * 扫描文件数
     */
    private static Deque<ScanFileEntity> scanFiles = new ConcurrentLinkedDeque<>();

    /**
     * 服务器配置文件
     */
    private String serverConfigurePath;
    

    /**
     * 服务器基本路径
     */
    private final String serverBase;

    /**
     * 是否已经初始化
     */
    protected boolean initialized;

    /**
     * 引擎
     */
    protected Engine engine;

    /**
     * 服务器
     */
    protected Server server;

    /**
     * 服务器启动方式
     */
    protected StartingMode startingMode = StartingMode.SERVER;


    /**
     * 服务器是否已启动
     */
    protected boolean started;

    /**
     * 服务器实例与服务器实例映射
     */
    protected ConfigureMap<Server, ServerConfigure> configureMap;

    /**
     * 配置文件解析实例
     */
    protected ConfigureParse<Server, ServerConfigure> configureParse;

    /**
     * 启动包装类集合
     */
    protected List<BootstrapWrapper> bootstrapWrappers = new ArrayList<>();
    
    /**
     * 等待超时时长 （秒）
     */
    private long awaitTime = 60;


    // ==================================== 私有构造方法 ====================================
    
    private StandardServerStartup() {
        this.serverBase = System.getProperty(SystemProperty.SERVER_BASE);
    }


    // ==================================== 内部类 ====================================

    /**
     * 启动类启动状态
     */
    enum BootstrapStatus {
        /**
         * 从来没有启动过
         */
        NOT_YET,

        /**
         * 启动失败
         */
        FAIL,

        /**
         * 执行过main方法
         */
        INVOKE,

        /**
         * 启动成功并关闭
         */
        SUCCEED;        
    }
    
    
    /**
     * 启动类的包装类
     */
    class BootstrapWrapper {
        /**
         * 启动类
         */
        private Class clazz;

        /**
         * main方法
         */
        private Method main;

        /**
         * main方法的参数
         */
        private String[] args = new String[0];

        /**
         * 上次启动状态
         */
        private BootstrapStatus prevStatus = BootstrapStatus.NOT_YET;

        
        public BootstrapWrapper(Class clazz) {
            this.clazz = clazz;
        }
        
        
        public void setArgs(String[] args) {
            this.args = args;
        }
        
        public void startup() throws Exception {
            this.startup(args);
        }
        
        public void startup(String[] args) throws Exception {
            this.startup((Object) args);
        }
        
        private void startup(Object args) throws Exception  {
            try {
                if (main == null) {
                    main = clazz.getDeclaredMethod("main", String[].class);
                }

                prevStatus = BootstrapStatus.INVOKE;
                main.invoke(null, args);
                prevStatus = BootstrapStatus.SUCCEED;

            } catch (Exception e) {
                prevStatus = BootstrapStatus.FAIL;
                throw e;
            }
        }
            
    }
    
    
    /**
     * 启动类加载器
     */
    class BootstrapClassLoader extends AbstractClassLoader {
        
        public BootstrapClassLoader(String base) {
            FileDirContext context = new FileDirContext();
            context.setDocBase(base);
            setResources(context);
        }


        /**
         * 查询资源URL
         * 
         * @param name
         * @return
         */
        @Override
        public URL findResource(String name) {
            URL res = null;
            
            try {
                Object lookup = getResources().lookup(name);
                if (lookup instanceof FileDirContext) {
                    res = ((FileDirContext) lookup).getBase().toURI().toURL();
                } else if (lookup instanceof FileDirContext.FileResource) {
                    res = ((FileDirContext.FileResource) lookup).getURI().toURL();
                }
            } catch (NamingException e) {
                res = null;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            if (res != null) {
                return res;
            }
            
            try {
                Object lookup = getResources().lookup(com.ranni.common.Constants.WEBAPP_CLASSES + File.separator + name);
                if (lookup instanceof FileDirContext) {
                    res = ((FileDirContext) lookup).getBase().toURI().toURL();
                } else {
                    res = ((FileDirContext.FileResource) lookup).getURI().toURL();
                }
            } catch (NamingException e) {
                res = null;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            if (res == null) {
                res = super.findResource(name);
            }
            
            return res;
        }
        

        /**
         * 根据类名找到这个类文件并加载到JVM虚拟机中
         *
         * @param name 全限定类名
         * @return 返回找到并加载的类
         * @throws ClassNotFoundException 如果没有找到这个类将抛出此异常
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!name.endsWith(".class")) {
                name += ".class";
            }

            Object obj = null;
            
            try {
                obj = resources.lookup(name);
            } catch (NamingException e) {
                e.printStackTrace();
            }
            
            if (obj == null || !(obj instanceof Resource)) {
                throw new ClassNotFoundException("未找到该资源。" + name);
            }
            
            try {

                Resource res = (Resource) obj;
                int index = name.indexOf("classes");
                URL url = null;
                String realName = name;
                
                if (index > -1) {
                    realName = name.substring(index + 8, name.length() - 6);
                }

                realName = realName.replaceAll("\\\\|/", ".");
                
                if (resources instanceof FileDirContext) {
                    url = new File(((FileDirContext) resources).getBase(), name).toURI().toURL();
                } else {
                    url = new File(serverBase, name).toURI().toURL();
                }

                CodeSource codeSource = new CodeSource(url, new CodeSigner[0]);

                InputStream is = res.streamContent();

                ResourceAttributes ra = (ResourceAttributes) resources.getAttributes(name);
                int length = (int) ra.getContentLength();

                byte[] content = new byte[length];
                int n = -1;
                int pos = 0;
                
                do {
                    n = is.read(content, pos, length - pos);
                    pos += n;
                } while (n > 0);
                
                is.close();
                
                return defineClass(realName, content, 0,
                        length, codeSource);
                
            } catch (NamingException ne) {
                ne.printStackTrace();
            } catch (MalformedURLException mue) {
                mue.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return null;
            
        }
        
    }
        

    /**
     * 扫描文件实体
     */
    private class ScanFileEntity {
        private File file; // 被扫描的文件
        private Host host; // 所属主机

        public ScanFileEntity(File file, Host host) {
            this.file = file;
            this.host = host;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public Host getHost() {
            return host;
        }

        public void setHost(Host host) {
            this.host = host;
        }
    }


    /**
     * webapp启动类的扫描线程
     */
    private class WebappBootstrapScan implements Runnable {
        private File file;

        public WebappBootstrapScan(File file) {
            this.file = file;
        }
        
        @Override
        public void run() {
            try {
                scanBootstrap(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        /**
         * 扫描启动类并创建容器
         * 只要一层目录下有一个.class文件就不再往下继续扫描了。
         * 如果找到了启动类，会实例化启动类并调用startup()方法，
         * 往后就交给启动实例的代理类配置Context的参数并加入对应的Host容器。
         * 
         * @param file
         */
        private void scanBootstrap(File file) throws IOException {
            Queue<File> queue = new LinkedList<>();
            List<File> classFiles = new LinkedList<>();
            queue.offer(file);
            
            while (!queue.isEmpty()) {
                File f = queue.poll();
                for (File temp : f.listFiles()) {
                    if (temp.isDirectory()) {
                        queue.offer(temp);
                    } else if (temp.getName().endsWith(".class")) {
                        classFiles.add(temp);
                    }
                }
                
                if (!classFiles.isEmpty()) {
                    break;
                }
            }

            String canonicalPath = file.getCanonicalPath();
            BootstrapClassLoader loader = new BootstrapClassLoader(canonicalPath);
            for (File f : classFiles) {
                try {
                    String path = f.getCanonicalPath();
                    Class<?> aClass = loader.loadClass(path.substring(canonicalPath.length()));
                    
                    if (aClass != null && aClass.getDeclaredAnnotation(WebBootstrap.class) != null) {                        
                        bootstrapWrappers.add(new BootstrapWrapper(aClass));                      
                        return;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } 
            }
        }
    }


    /**
     * 容器扫描
     */
    class ContextScan implements Runnable {

        private Host host;
        private Service service;

        public ContextScan(Host host, Service service) {
            this.host = host;
            this.service = service;
        }


        /**
         * 容器扫描
         */
        @Override
        public void run() {
            try {
                scanContext();                
            } catch (IOException e) {
                e.printStackTrace();
            }
            finishCount.incrementAndGet();
        }


        /**
         * 扫描容器
         */
        private void scanContext() throws IOException {
            ServerConfigure serverConfigure = configureMap.getConfigure();

            Map<String, File> webapps = new HashMap<>();
            Deque<File> wars = new LinkedList<>();

            // 遍历Host容器
            String path = System.getProperty(SystemProperty.SERVER_BASE) + File.separator + host.getAppBase();
            File repository = new File(path);

            if (!repository.exists())
                return;

            if (!repository.isDirectory())
                throw new FilerException("此路径应该是个目录！");

            // 把目录和war文件分离出来
            for (File file : repository.listFiles()) {
                if (file.isDirectory()) {
                    webapps.put(file.getName(), file);
                } else if (serverConfigure.isUnpackWARS()
                        && file.getName().endsWith(".war")) {

                    wars.push(file);
                }
            }

            // 删除不需要解压的war（未修改的war包就不需要解压）
            Iterator<File> iterator = wars.iterator();
            while (iterator.hasNext()) {
                File file1 = iterator.next();
                int end = file1.getName().length() - 4;
                String name = file1.getName().substring(0, end);                
                File file2 = webapps.get(name);                
                
                if (file2 != null && file2.lastModified() >= file1.lastModified()) {
                    // war包未修改
                    iterator.remove();
                } else {
                    String absolutePath = file1.getAbsolutePath().substring(0, end);
                    WARDecUtil.unzip(file1);
                    webapps.put(name, new File(absolutePath));
                }
            }

            // 扫描启动类
            for (File file : webapps.values()) {
                new WebappBootstrapScan(file).run();
            }
        }
    }

    
    // ==================================== 核心方法 ====================================

    /**
     * 返回单例StandardServerStartup
     *
     * @return
     */
    public static StandardServerStartup getInstance() {
        if (instance == null) {
            synchronized (StandardServerStartup.class) {
                if (instance == null) {
                    instance = new StandardServerStartup();
                }
            }
        }

        return instance;
    }
    
    
    /**
     * 启动服务器
     */
    @Override
    public synchronized void startup() throws Exception {
        if (started) 
            throw new IllegalStateException("服务器已启动！");
        
        started = true;
        if (server instanceof Lifecycle) {
            try {
                server.initialize();
                ((Lifecycle) server).start(); // 启动服务器
                server.await(); // 等待关闭
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
        }
        
        try {
            if (server instanceof Lifecycle)
                ((Lifecycle) server).stop(); // 关闭服务器 
            
        } catch (LifecycleException e) {
            e.printStackTrace();
        } finally {
            // 回收资源
            this.recycle();                
        }        
    }


    /**
     * 解析服务器配置
     * 
     * @return
     */
    protected ConfigureMap<Server, ServerConfigure> parseServerConfigure() {
        if (initialized || started)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  服务器已初始化或已启动，不能再解析配置文件！");
        
        if (this.configureParse == null)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  配置文件解析实例不能为null！");

        try {
            File file = null;
            if (serverConfigurePath != null) {
                file = new File(serverConfigurePath);
                if (!file.exists() || !file.canRead())
                    throw new IOException("文件不存在或文件不可读！");
                
            } else {
                String filePath = System.getProperty(SystemProperty.SERVER_BASE) + com.ranni.common.Constants.CONF + File.separator;
                file = new File(filePath, Constants.DEFAULT_SERVER_YAML);
                if (!file.exists())
                    file = new File(filePath, Constants.DEFAULT_SERVER_YML);
            }

            InputStream input = null;
            
            if (file.exists() && file.canRead()) {
                
                input = new FileInputStream(file);
                
            } else {
                // 不存在，通过当前的类加载器去加载这个资源
                String serverConfigureName = Constants.DEFAULT_SERVER_YAML;
                URL resource = StandardServerStartup.class.getClassLoader().getResource(Constants.DEFAULT_SERVER_YAML);
                
                if (resource == null) {
                    resource = StandardServerStartup.class.getClassLoader().getResource(Constants.DEFAULT_SERVER_YML);
                    serverConfigureName = Constants.DEFAULT_SERVER_YML;
                }
                
                if (resource == null)
                    throw new FileNotFoundException("找不到server.yaml和server.yml配置文件！");
                
                if ("jar".equals(resource.getProtocol())) {
                    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                    int length = (ccl.getResource("").getProtocol() + ":/").length();
                    String path = resource.getPath().substring(length, resource.getPath().lastIndexOf("!/"));
                    file = new File(path);
                    
                    if (!file.exists() || !file.canRead() || !file.getName().endsWith(".jar"))
                        throw new IllegalStateException("jar文件状态异常！");

                    JarFile jarFile = new JarFile(file);
                    JarEntry jarEntry = jarFile.getJarEntry(serverConfigureName);
                    input = jarFile.getInputStream(jarEntry);
                } else if ("file".equals(resource.getProtocol())) {
                    input = new FileInputStream(new File(resource.toURI()));
                }
            }

            ConfigureMap<Server, ServerConfigure> parse = null;
            
            // 根据启动模式微调server.yaml配置文件
            if (startingMode == StartingMode.SERVER) {
                parse = configureParse.parse(input, true);
            } else {
                parse = configureParse.parse(input, false);
                ServerConfigure configure = parse.getConfigure();
                // 必然存在第0个默认的host配置
                configure.getEngine().getHosts().get(0).setAppBase("");
                parse.setInstance(configureParse.fit(configure));
            }             
            
            return parse;
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }


    /**
     * 设置服务器
     * 
     * @param server
     */
    @Override
    public void setServer(Server server) {
        this.server = server;
    }


    /**
     * 返回服务器
     * 
     * @return
     */
    @Override
    public Server getServer() {
        return this.server;
    }


    /**
     * 设置服务器
     *
     * @param configureMap
     */
    @Override
    public void setConfigureMap(ConfigureMap<Server, ServerConfigure> configureMap) {
        if (started)
            throw new IllegalStateException("服务器已经启动，不能修改服务器！");

        this.configureMap = configureMap;
    }


    /**
     * 取得服务器配置信息
     *
     * @return
     */
    @Override
    public ConfigureMap<Server, ServerConfigure> getConfigureMap() {
        if (this.configureMap == null) {
            this.configureMap = parseServerConfigure();
        }
        return this.configureMap;
    }


    /**
     * 回收资源
     */
    @Override
    public void recycle() {
        finishCount.set(0);
        scanFiles.clear();
        this.started = false;
        this.configureMap = null;
        this.startingMode = StartingMode.SERVER;
        this.awaitTime = 60;
        this.bootstrapWrappers.clear();
        this.configureParse = null;
        this.engine = null;
        this.server = null;
        this.initialized = false;
    }


    /**
     * 设置配置文件解析器
     * 单例模式
     * 
     * @param parse
     */
    @Override
    public void setConfigureParse(ConfigureParse<Server, ServerConfigure> parse) {
        if (configureParse == null) {
            synchronized (this) {
                if (configureParse == null) {
                    this.configureParse = parse;
                }
            }
        }
    }


    /**
     * 返回配置文件解析器
     * 
     * @return
     */
    @Override
    public ConfigureParse<Server, ServerConfigure> getConfigureParse() {
        return this.configureParse;
    }
    

    /**
     * 返回engine
     * 
     * @return
     */
    @Override
    public Engine getEngine() {
        return this.engine;
    }

    
    /**
     * 初始化，解析服务器配置
     * 要在调用startup()之前调用此方法
     */
    @Override
    public void initialize() throws Exception {
        if (initialized)
            throw new IllegalStateException("StandardServerStartup.initialize  已经初始化过了！");
        
        synchronized (this) {
            if (initialized)
                return;

            // 解析服务器配置信息
            // Host是通过server.yaml指定的，所以不需要扫描，仅通过配置文件中指定的路径创建Host容器即可
            ConfigureMap<Server, ServerConfigure> configureMap = getConfigureMap();

            if (configureMap == null)
                throw new IllegalArgumentException("无可用服务器！");

            // 获取server和engine
            Server server = configureMap.getInstance();
            Service[] services = server.findServices();
            Engine engine = services[0].getContainer(); // 所有services的engine都是同一个，因为engine只会存在一个
            setEngine(engine);
            setServer(server);
        }
        
        if (startingMode == StartingMode.SERVER) {
            // 多线程扫描context容器
            for (Container host : engine.findChildren()) {
                engine.getService().execute(new ContextScan((Host) host, engine.getService()));
            }

            long end = getAwaitTime();
            if (finishCount.get() != engine.findChildren().length) { // 这个比较不存在并发问题
                try {
                    if (end > 0) {
                        int i = 0;
                        for (; i < end; i++) {
                            if (finishCount.get() == engine.findChildren().length) {
                                break;
                            }

                            Thread.sleep(1000);
                        }
                        if (i >= end) {
                            throw new TimeoutException("StandardServerStartup.initialize 容器扫描超时！");
                        }

                    } else {
                        while (finishCount.get() != engine.findChildren().length) {
                            Thread.sleep(1000);
                        }
                    }
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
        
        // 到这里服务端初始化完成
        initialized = true;
        
        if (startingMode == StartingMode.SERVER) {
            // 通过服务器启动
            for (BootstrapWrapper wrapper : bootstrapWrappers) {
                if (wrapper.prevStatus == BootstrapStatus.FAIL) {
                   continue; 
                }
                
                try {
                    wrapper.startup();
                } catch (Exception e) {
                    wrapper.prevStatus = BootstrapStatus.FAIL;
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 重新通过BootstrapClassLoader载入启动类
     * 
     * @param clazz 要被重载的Bootstrap类
     * @return 返回重载后的类
     */
    @Deprecated
    public static Class reloadBootstrapClass(Class clazz) {
        System.out.println(clazz);
        System.out.println(clazz.getPackage());
        System.out.println(clazz.getClassLoader().getResource(""));
        System.out.println(System.getProperty("user.dir"));
        return null;
    }

    /**
     * 设置服务器启动方式
     * 
     * @param startingMode 服务器启动方式
     */
    @Override
    public void setStartingMode(StartingMode startingMode) {
        if (initialized) {
            throw new IllegalStateException("standardServerStartup.setStartingMode 设置启动方式出错，服务器已初始化！");
        }
        
        this.startingMode = startingMode;
    }

    /**
     * @return 返回服务器启动方式
     */
    @Override
    public StartingMode getStartingMode() {
        return this.startingMode;
    }

    /**
     * 设置等待超时时长
     *
     * @param awaitTime 超时时长
     */
    @Override
    public void setAwaitTime(long awaitTime) {
        this.awaitTime = awaitTime;
    }


    /**
     * @return 返回等待超时时长，负数为无限等待
     */
    @Override
    public long getAwaitTime() {
        return awaitTime;
    }
    

    /**
     * @return 返回服务器基本路径
     */
    @Override
    public String getServerBase() {
        return serverBase;
    }
    

    /**
     * 是否已经初始化
     * 
     * @return
     */
    @Override
    public boolean getInitialized() {
        return this.initialized;
    }


    /**
     * 设置服务器配置文件路径
     * 
     * @param path 服务器配置文件路径
     */
    @Override
    public void setServerConfigurePath(String path) {
        if (initialized)
            throw new IllegalStateException("StandardServerStartup.setServerConfigurePath  服务器已初始化，不能再修改配置文件路径！");
        
        this.serverConfigurePath = path;
    }


    /**
     * 返回启动标志位
     * 
     * @return
     */
    @Override
    public boolean getStarted() {
        return this.started;
    }


    /**
     * 设置engine
     * 
     * @param engine
     */
    private void setEngine(Engine engine) {
        this.engine = engine;
    }
}
