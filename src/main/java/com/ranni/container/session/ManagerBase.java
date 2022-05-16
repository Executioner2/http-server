package com.ranni.container.session;

import com.ranni.container.Container;
import com.ranni.container.DefaultContext;
import com.ranni.logger.Logger;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * session的管理器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-19 19:08
 */
public abstract class ManagerBase implements Manager {
    protected static final String DEFAULT_ALGORITHM = "SHA-512"; // 默认的session算法
    protected static final int SESSION_ID_BYTES = 16; // session id长度

    protected Container container; // 关联的context容器
    protected int debug = Logger.INFORMATION; // 日志级别
    protected DefaultContext defaultContext; // 默认容器
    protected boolean distributable; // 持久化标志
    protected int maxInactiveInterval = 60; // 最大生存时间，单位秒
    protected static String name = "ManagerBase";
    protected Map<String, Session> sessions = new HashMap(); // session集合
    protected String randomClass = "java.security.SecureRandom"; // session id生成器类名
    protected Deque<Session> recycled = new LinkedList<>(); // 回收的session对象
    protected String entropy; // 生成session id的熵
    protected MessageDigest digest; // 生存session id所使用的算法
    protected volatile Random random; // 随机生成器
    protected String algorithm = DEFAULT_ALGORITHM; // 散列算法


    /**
     * 设置日志输出级别
     *
     * @param debug
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }


    /**
     * 添加到回收队列
     *
     * @param session
     */
    public void recycle(Session session) {
        recycled.push(session);
    }


    /**
     * 返回日志输出级别
     *
     * @return
     */
    public int getDebug() {
        return this.debug;
    }


    /**
     * 返回默认context容器
     *
     * @return
     */
    @Override
    public DefaultContext getDefaultContext() {
        return this.defaultContext;
    }


    /**
     * 设置默认context容器
     *
     * @param defaultContext
     */
    @Override
    public void setDefaultContext(DefaultContext defaultContext) {
        this.defaultContext = defaultContext;
    }


    /**
     * 设置此manager关联的容器
     *
     * @param container
     */
    @Override
    public void setContainer(Container container) {
        this.container = container;
    }


    /**
     * 返回此manager关联的容器
     *
     * @return
     */
    @Override
    public Container getContainer() {
        return this.container;
    }


    /**
     * 返回持久化标志位
     *
     * @return
     */
    @Override
    public boolean getDistributable() {
        return this.distributable;
    }


    /**
     * 设置持久化标志位
     *
     * @param distributable
     */
    @Override
    public void setDistributable(boolean distributable) {
        this.distributable = distributable;
    }


    /**
     * 返回使用的散列算法
     *
     * @return
     */
    public String getAlgorithm() {
        return (this.algorithm);
    }


    /**
     * 设置要使用的散列算法
     *
     * @param algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }


    /**
     * 返回存活时间，单位秒
     *
     * @return
     */
    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }


    /**
     * 设置存活时间，单位秒
     *
     * @param interval
     */
    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }


    /**
     * 创建session
     * 步骤：
     * 1、生成session id
     * 2、创建session对象并把生成的id传入session对象
     * 3、添加进session集合（在session.setId()中调用此类的add()方法实现 {@link StandardSession#setId(String)}）
     *
     * @return
     */
    @Override
    public Session createSession() {
        Session session = recycled.pollFirst();

        if (session != null) {
            // 复用session
            session.setManager(this);
        } else {
            // 创建session
            session = new StandardSession(this);
        }

        // TODO 组成完整session id的应该还有router id，router id是每个服务器一个。这样做便于实现负载均衡
        String id = getSessionId(); // 取得session id

        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);
        session.setId(id);

        return session;
    }


    /**
     * 生成并返回session id
     *
     * @return
     */
    private String getSessionId() {
        MessageDigest messageDigest = getDigest();
        if (messageDigest == null)
            throw new IllegalStateException("ManagerBase.getSessionId  没有消息摘要生成器！");

        byte[] bytes = new byte[SESSION_ID_BYTES];
        getRandom().nextBytes(bytes);
        bytes = messageDigest.digest(bytes);

        // 转字符
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            byte b1 = (byte) ((bytes[i] & 0xF0) >> 4);
            byte b2 = (byte) (bytes[i] & 0x0F);

            if (b1 < 10)
                sb.append((char) ('0' + b1));
            else
                sb.append((char) ('A' + (b1 - 10)));

            if (b2 < 10)
                sb.append((char) ('0' + b2));
            else
                sb.append((char) ('A' + (b2 - 10)));
        }

        return sb.toString();
    }


    /**
     * 取得消息摘要生成器
     *
     * @return
     */
    private synchronized MessageDigest getDigest() {
        if (this.digest == null) {
            try {
                digest = MessageDigest.getInstance(this.algorithm);
            } catch (NoSuchAlgorithmException e) {
                log("没有" + this.algorithm + "这样的散列算法！");
                try {
                    digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
                } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
                    log("没有" + DEFAULT_ALGORITHM + "这样（默认散列算法）的散列算法！");
                }
            }
        }

        return this.digest;
    }

    /**
     * 如果random为null，则先创建一个随机生成器对象
     *
     * @return
     */
    private Random getRandom() {
        if (random == null) {
            synchronized (this) {
                if (random == null) {
                    // 设置随机种子
                    long seed = System.currentTimeMillis();
                    char[] entropy = getEntropy().toCharArray();
                    for (int i = 0; i < entropy.length; i++) {
                        long update = ((byte) entropy[i]) << ((i % 8) * 8); // 7 * 8 = 56，ascii最大127，127 << 56 = 9151314442816847872。小于Long最大值
                        seed ^= update;
                    }

                    try {
                        Class<?> aClass = Class.forName(this.randomClass);
                        random = (Random) aClass.getConstructor().newInstance();
                        random.setSeed(seed);
                    } catch (Exception e) {
                        log("自定义随机生成器创建失败，采用java.util.Randoms作为生成器对象");
                        random = new Random();
                        random.setSeed(seed);
                    }
                }
            }
        }

        return random;
    }


    /**
     * 设置自定义的随机生成器类名
     *
     * @param randomClass
     */
    public void setRandomClass(String randomClass) {
        if (randomClass == null)
            throw new IllegalStateException("ManagerBase.setRandomClass  传入参数不能为null！");
        this.randomClass = randomClass;
    }


    /**
     * 返回当前的随机生成器类名
     *
     * @return
     */
    public String getRandomClass() {
        return this.randomClass;
    }


    /**
     * 返回随机种子的熵
     *
     * @return
     */
    public String getEntropy() {
        if (entropy == null)
            setEntropy(this.toString());
        return entropy;
    }


    /**
     * 设置随机种子的熵
     *
     * @param entropy
     */
    public void setEntropy(String entropy) {
        this.entropy = entropy;
    }


    /**
     * 根据session id查找session
     *
     * @param id
     * @return
     * @throws IOException
     */
    @Override
    public Session findSession(String id) {
        synchronized (sessions) {
            return sessions.get(id);
        }
    }


    /**
     * 返回所有session
     *
     * @return
     */
    @Override
    public Session[] findSessions() {
        synchronized (sessions) {
            return sessions.values().toArray(new Session[sessions.size()]);
        }
    }


    /**
     * 添加session到集合
     *
     * @param session
     */
    @Override
    public void add(Session session) {
        synchronized (sessions) {
            sessions.put(session.getId(), session);
        }
    }


    /**
     * 移除session
     *
     * @param session
     */
    @Override
    public void remove(Session session) {
        synchronized (sessions) {
            sessions.remove(session.getId());
        }
    }


    /**
     * 当前类名
     * @return
     */
    public String getName() {
        return name;
    }


    /**
     * 日志记录
     *
     * @param message
     */
    void log(String message) {
        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log(getName() + "[" + container.getName() + "]: "
                    + message);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println(getName() + "[" + containerName
                    + "]: " + message);
        }
    }


    /**
     * 日志记录
     *
     * @param message
     * @param throwable
     */
    void log(String message, Throwable throwable) {
        Logger logger = null;
        if (container != null)
            logger = container.getLogger();
        if (logger != null)
            logger.log(getName() + "[" + container.getName() + "] "
                    + message, throwable);
        else {
            String containerName = null;
            if (container != null)
                containerName = container.getName();
            System.out.println(getName() + "[" + containerName
                    + "]: " + message);
            throwable.printStackTrace(System.out);
        }
    }
}
