package com.ranni.startup;

import com.ranni.annotation.core.WebBootstrap;
import com.ranni.common.SystemProperty;
import com.ranni.connector.HttpConnector;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Engine;
import com.ranni.container.Host;
import com.ranni.container.context.StandardContext;
import com.ranni.container.host.StandardHost;
import com.ranni.core.Server;
import com.ranni.core.Service;
import com.ranni.deploy.ApplicationConfigure;
import com.ranni.deploy.ConfigureMap;
import com.ranni.deploy.ServerConfigure;
import com.ranni.lifecycle.Lifecycle;
import com.ranni.lifecycle.LifecycleException;
import com.ranni.loader.WebappLoader;
import com.ranni.util.WARDecUtil;

import javax.annotation.processing.FilerException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;


/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/13 11:39
 */
public class StandardServerStartup implements ServerStartup {
    private static final int MAX_THREAD = 8; // 最大线程数
    private volatile static StandardServerStartup instance; // 单例的StandardServerStartup
    private static AtomicInteger finishCount = new AtomicInteger(0); // 扫描线程完成数
    private static Deque<ScanFileEntity> scanFiles = new ConcurrentLinkedDeque<>(); // 扫描文件数

    protected boolean initialized; // 是否已经初始化
    protected Engine engine; // 引擎
    protected Server server; // 服务器
    protected float divisor = 0.4f; // 线程数量因子，范围：[0.1, 1]
    protected boolean serverStartup; // 是否从服务器启动的标志位
    protected boolean started; // 服务器是否已启动
    protected ConfigureMap<Server, ServerConfigure> configureMap; // 服务器实例与服务器实例映射
    protected ConfigureParse<Server, ServerConfigure> configureParse; // 配置文件解析实例


    private StandardServerStartup() { }


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

        @Override
        public void run() {
            while (!scanFiles.isEmpty()) {
                try {
                    ScanFileEntity scanFileEntity = scanFiles.pop();
                    File file = scanFileEntity.getFile();
                    // FIXME - 需要重写
                    
                    scanBootstrap(file);
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                    break;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    break;
                }
            }
            
            finishCount.incrementAndGet();
        }


        /**
         * 扫描启动类并创建容器
         * 只要一层目录下有一个.class文件就不再往下继续扫描了。
         * 如果找到了启动类，会实例化启动类并调用startup()方法，
         * 往后就交给启动实例的代理类配置Context的参数并加入对应的Host容器。
         * 
         * @param file
         */
        private void scanBootstrap(File file) throws MalformedURLException {
            Queue<File> queue = new LinkedList<>();
            List<File> classFiles = new LinkedList<>();
            URL[] urls = new URL[1];
            URLClassLoader urlClassLoader = null;
            queue.offer(file);
            
            while (!queue.isEmpty()) {
                File f = queue.poll();
                for (File temp : f.listFiles()) {
                    if (temp.isDirectory()) {
                        queue.offer(temp);
                    } else if (temp.getName().endsWith(".class")) {
                        classFiles.add(file);
                    }
                }
                
                if (!classFiles.isEmpty()) {
                    URLStreamHandler streamHandler = null;
                    String base = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf("\\WEB-INF\\classes\\") + 17);
                    String repository = (new URL("file", null, base)).toString();
                    urls[0] = new URL(null, repository, streamHandler);
                    urlClassLoader = new URLClassLoader(urls);
                    break;
                }
            }
            
            for (File f : classFiles) {
                try {
                    String path = f.getAbsolutePath();
                    String className = path.substring(path.lastIndexOf("\\WEB-INF\\classes\\") + 17, path.lastIndexOf(".class"));
                    className = className.replaceAll("\\\\", ".");
                    Class<?> aClass = urlClassLoader.loadClass(className);
                    if (aClass.getDeclaredAnnotation(WebBootstrap.class) != null) {
                        Method main = aClass.getMethod("main", String[].class);
                        main.invoke(null, null);
                        return;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
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
     * 扫描所有主机
     * 只有拥有启动类的webapp才会被创建Context容器并加入到服务器中进行管理
     */
    private void scanHost(Engine engine) throws IOException {
        ServerConfigure serverConfigure = configureMap.getConfigure();

        Map<String, File> webapps = new HashMap<>();
        Deque<File> wars = new LinkedList<>();        
        WARDecUtil warDecUtil = WARDecUtil.getInstance(); // 解压工具
        int warCount = 0; // 将被解压的war包数量
        
        // 遍历Host容器
        for (Container container : engine.findChildren()) {
            Host host = (Host) container;
            String path = System.getProperty(SystemProperty.SERVER_BASE) + File.separator + host.getAppBase();
            File repository = new File(path);
            
            if (!repository.exists())
                continue;
            
            if (!repository.isDirectory())
                throw new FilerException("此路径应该是个目录！");
            
            webapps.clear();
            wars.clear();
            
            // 把目录和war文件分离出来
            for (File file : repository.listFiles()) {
                if (file.isDirectory()) {
                    webapps.put(file.getName(), file);
                    scanFiles.push(new ScanFileEntity(file, host));
                } else if (serverConfigure.isUnpackWARS() 
                        && file.getName().endsWith(".war")) {
                    
                    wars.push(file);
                }
            }
            
            // 删除不需要解压的war（未修改的war包就不需要解压）
            Iterator<File> iterator = wars.iterator();
            while (iterator.hasNext()) {
                File file1 = iterator.next();
                File file2 = webapps.get(file1.getName());
                if (file2 != null && file2.lastModified() >= file1.lastModified()) {
                    // war包未修改
                    iterator.remove(); 
                } else {
                    String absolutePath = file1.getAbsolutePath();
                    absolutePath.substring(0, absolutePath.lastIndexOf("."));
                    scanFiles.push(new ScanFileEntity(new File(absolutePath), host));
                    webapps.put(file1.getName(), file1);
                    warCount++;
                }
            }

            // 解压war包，后台解压                      
            warDecUtil.unzip(new ConcurrentLinkedDeque<>(wars), true);

        }

        // 等待war包解压完
        int i = 0;
        while (true) {
            if (warCount == warDecUtil.getCount())
                break;
            
            if (i++ == 10)
                throw new ZipException("StandardServerStartup.scanContext  解压超时！");
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        
    }


    /**
     * 扫描容器
     */
    private void scanContext() {
        int n = scanFiles.size();
        
        if (n <= 0)
            return;
        
        int upper = Math.min(Math.max(1, (int) (n * divisor)), MAX_THREAD);

        for (int i = 0; i < upper; i++) {
            new Thread(new WebappBootstrapScan()).start();            
        }
        
        while (finishCount.get() < upper) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 解析服务器配置
     * 
     * @return
     */
    protected ConfigureMap<Server, ServerConfigure> parseServerConfigure() {
        if (started)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  服务器已经启动，不能再解析配置文件！");
        
        if (this.configureParse == null)
            throw new IllegalArgumentException("ServerStartupBase.parseServerConfigure  配置文件解析实例不能为null！");

        try {
            String filePath = System.getProperty(SystemProperty.SERVER_BASE) + com.ranni.common.Constants.CONF + File.separator;
            
            File file = new File(filePath, Constants.DEFAULT_SERVER_YAML);
            InputStream input = null;
            
            if (!file.exists())
                file = new File(filePath, Constants.DEFAULT_SERVER_YML);
            
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
                }
            }
            
            return configureParse.parse(input);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     */
    @Override
    public void setServerStartup(boolean serverStartup) {
        this.serverStartup = serverStartup;
    }


    /**
     * 从服务器启动标志位
     *
     * @param serverStartup
     * @return
     */
    @Override
    public boolean getServerStartup(boolean serverStartup) {
        return this.serverStartup;
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
        this.serverStartup = false;
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
     * 设置线程数量因子
     * 取值在[0.1, 1]
     * 
     * @param divisor
     */
    @Override
    public void setDivisor(float divisor) {
        if (started)
            throw new IllegalStateException("服务器已启动，不能再更改线程数量因子！"); 
                    
        if (divisor < 0.1f || divisor > 1.0f)
            throw new IllegalArgumentException("参数异常，线程数量因子应在[0.1, 1]范围内");
               
        this.divisor = divisor;
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
    public void initialize() throws IOException {
        if (initialized)
            throw new IllegalStateException("StandardServerStartup.initialize  已经初始化过了！");
        
        synchronized (this) {
            
            if (initialized)
                return;

            // 解析服务器配置信息
            ConfigureMap<Server, ServerConfigure> configureMap = getConfigureMap();

            if (configureMap == null)
                throw new IllegalArgumentException("无可用服务器！");

            // 获取server和engine
            Server server = configureMap.getInstance();
            setDivisor(configureMap.getConfigure().getScanThreadDivisor()); // 设置扫描线程数量因子
            Service[] services = server.findServices();
            Engine engine = (Engine) services[0].getContainer();
            setEngine(engine);
            setServer(server);
        }
        
        // 下面两条支持多线程，所以要在临界区外
        
        // 扫描Host
        scanHost(engine);

        if (serverStartup) {
            // 通过服务器启动，扫描容器
            scanContext();
        }
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
     * 创建Context并初始化webapp
     * 返回Context，并不在此方法中加入Engine
     * 
     * @param applicationConfigure
     * @return
     */
    @Override
    public Context initializeApplication(ApplicationConfigure applicationConfigure) throws Exception {
        if (getServer() == null)
            throw new IllegalStateException("StandardServerStartup.initializeApplication  没有服务器实例！");
        
        if (getEngine() == null)
            throw new IllegalStateException("StandardServerStartup.initializeApplication  没有服务器引擎！");

        Engine engine = getEngine();
        Server server = getServer();
        Host host = (Host) engine.findChild(applicationConfigure.getHost());
        
        if (host == null) {
            host = new StandardHost();            
            host.setAppBase(applicationConfigure.getAppBase());
            host.setName(applicationConfigure.getHost());
            engine.addChild(host);
        }

        StandardContext context = new StandardContext();
    
        context.setDocBase(applicationConfigure.getDocBase());
        context.setPath(applicationConfigure.getPath());
        context.setReloadable(applicationConfigure.isReloadable());
        context.setBackgroundProcessorDelay(applicationConfigure.getBackgroundProcessorDelay());
        context.setLoader(new WebappLoader());
        ContextConfig contextConfig = new ContextConfig();
        context.addLifecycleListener(contextConfig);

        // 连接器
        HttpConnector httpConnector = new HttpConnector();
        httpConnector.setPort(applicationConfigure.getPort());
        httpConnector.setAddress(applicationConfigure.getIp());

        // 加入service
        for (String serviceName : applicationConfigure.getServices()) {
            Service service = server.findService(serviceName);
            service.addConnector(httpConnector);
        }
                
        return context;
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
