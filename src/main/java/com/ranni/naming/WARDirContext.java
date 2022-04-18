package com.ranni.naming;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Title: HttpServer
 * Description:
 * WAR包容器
 * JNDI中的context
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 19:07
 */
public class WARDirContext extends BaseDirContext {
    protected ZipFile base; // 与此WARDirContext关联的压缩文件
    protected Entry<Entry> entries; // WAR的条目

    public WARDirContext() {

    }

    public WARDirContext(ZipFile base, Entry<Entry> entries) {
        this.base = base;
        this.entries = entries;
    }

    /**
     * 设置根文件
     *
     * @param docBase
     */
    @Override
    public void setDocBase(String docBase) {
        if (docBase == null)
            throw new IllegalArgumentException("资源不能为空！");
        if (docBase.endsWith(".war"))
            throw new IllegalArgumentException("资源文件不是WAR包！");

        File base = new File(docBase);
        if (!base.exists() || !base.canRead() || base.isDirectory())
            throw new IllegalArgumentException("资源文件不是WAR包或资源不可读！");

        super.setDocBase(docBase);
        loadEntries();
    }

    /**
     * 加载WAR包中的所有条目
     * 条目的路径是以WAR包为根目录的相对路径
     */
    protected void loadEntries() {
        try {
            Enumeration<? extends ZipEntry> entryList = base.entries();
            entries = new Entry<Entry>("/", new ZipEntry("/")); // 创建一个根目录

            while (entryList.hasMoreElements()) {
                ZipEntry zipEntry = entryList.nextElement();
                String name = normalize(zipEntry);
                int pos = name.lastIndexOf('/');
                int currentPos;
                int lastPos = 0;

                // 加载子文件夹
                // 按"/"分隔复合名
                while ((currentPos = name.indexOf('/', lastPos)) != -1) {
                    Name parentName = new CompositeName(name.substring(0, lastPos));
                    Name childName = new CompositeName(name.substring(0, currentPos));
                    String entryName = name.substring(lastPos, currentPos); // 当前条目的名字

                    Entry<Entry> parent = treeLookup(parentName); // 父文件夹
                    Entry<Entry> child = treeLookup(childName); // 子文件夹

                    if (child == null) {
                        // 文件路径
                        String zipName = name.substring(1, currentPos + 1);
                        child = new Entry<>(entryName, new ZipEntry(zipName));
                        if (parent != null) parent.addChild(child);
                    }

                    lastPos = currentPos + 1;
                }

                // 将复合名称最后一个名称对应的资源文件载入到属于它的目录下
                // 如 zip条目的名称为 "zip/a/" 调用了normalize()后 规范化为 "/zip/a"
                // 即使当前条目是文件夹，也把最后复合名称最后一个名称（这里是a）当作文件载入到目录 "/zip"下
                // 之所以可以这样做是因为文件和文件夹的视图对象都是一样的（这里为此类的内部类Entry）
                String entryName = name.substring(pos + 1);
                Name compositeName = new CompositeName(name.substring(0, pos));
                Entry<Entry> parent = treeLookup(compositeName);
                Entry<Entry> child = new Entry<>(entryName, zipEntry);
                if (parent != null) parent.addChild(child);
            }
        } catch (Exception e) {
            ;
        }
    }

    /**
     * 取得资源名，并在资源名前加上"/"
     * 如果资源是文件夹，那么把后面的"/"去掉
     *
     * @param entry
     * @return
     */
    protected String normalize(ZipEntry entry) {
        String s = "/" + entry.getName();
        if (entry.isDirectory())
            s = s.substring(0, s.length() - 1);

        return s;
    }

    /**
     * 根据名称取得属性
     *
     * @see CompositeName 复合名字类，有一个或多个名字组成，名字之间由/分隔，绝对路径也是复合名字
     * @param name
     * @param attrIds
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getAttributes(new CompositeName(name), attrIds);
    }

    /**
     * 根据名称取得属性
     *
     * @param name 可以是复合名字
     * @param attrIds
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        Entry entry = null;
        if (name.isEmpty())
            entry = entries;
        else
            entry = treeLookup(name); // 以树型方式解析
        if (entry == null)
            throw new NamingException("资源未不存在！  " + name);

        // 创建资源属性实例并将部分参数传入资源属性实例中
        ZipEntry zipEntry = entry.getEntry();
        ResourceAttributes attrs = new ResourceAttributes();
        attrs.setCreationDate(new Date(zipEntry.getTime()));
        attrs.setContentLength(zipEntry.getSize());
        attrs.setName(zipEntry.getName());
        if (!zipEntry.isDirectory())
            attrs.setResourceType("");
        attrs.setLastModifiedDate(new Date(zipEntry.getTime()));

        return attrs;
    }

    /**
     * 按树型方式解析资源
     *
     * @param name
     * @return
     */
    protected Entry<Entry> treeLookup(Name name) {
        if (name.isEmpty())
            return entries;

        Entry<Entry> currentEntry = entries;

        // 复合名称，一层一层的往下找，当中间某层缺失直接返回null
        for (int i = 0; i < name.size(); i++) {
            if (name.get(i).isEmpty()) continue;
            currentEntry = currentEntry.getChild(name.get(i));
            if (currentEntry == null) return null;
        }

        return currentEntry;
    }

    /**
     * 不支持修改属性
     *
     * @param name
     * @param mod_op
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param mods
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param obj
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param obj
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param attrs
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param matchingAttributes
     * @param attributesToReturn
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param matchingAttributes
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param filter
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @param filterExpr
     * @param filterArgs
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 解析资源
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    /**
     * 解析资源
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty())
            return this;
        Entry<Entry> entry = treeLookup(name);
        if (entry == null)
            throw new NamingException("资源不存在！  " + name);

        ZipEntry zipEntry = entry.getEntry();
        if (zipEntry.isDirectory())
            return new WARDirContext(base, entry);
        else
            return new WARResource(entry.getEntry());
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param oldName
     * @param newName
     * @throws NamingException
     */
    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 返回子资源集合的迭代器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return list(new CompositeName(name));
    }

    /**
     * 返回子资源集合的迭代器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        if (name.isEmpty())
            return new NamingContextEnumeration<NameClassPair>(list(entries));

        Entry<Entry> entryEntry = treeLookup(name);
        if (entryEntry == null)
            throw new NamingException("资源不存在！  " + name);

        return new NamingContextEnumeration<NameClassPair>(list(entryEntry));
    }

    /**
     * 取得指定资源下的子资源集合
     *
     * @param entry
     * @return
     */
    protected List<NamingEntry> list(Entry<Entry> entry) {
        List<NamingEntry> list = Collections.synchronizedList(new ArrayList<>());
        List<Entry> children = entry.getChildren();
        Collections.sort(children);
        NamingEntry namingEntry = null;
        Object o = null;

        for (Entry item : children) {
            ZipEntry zipEntry = item.getEntry();
            if (zipEntry.isDirectory()) {
                // 创建WARDirContext实例
                o = new WARDirContext(base, item);
            } else {
                o = new WARResource(zipEntry);
            }
            namingEntry = new NamingEntry(item.getName(), o, NamingEntry.ENTRY);
            list.add(namingEntry);
        }

        return list;
    }

    /**
     * 返回绑定的资源集合的迭代器
     * 迭代器中的元素是{@link Binding}包装类
     * 同 {@link WARDirContext#list(String)} 类似
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return listBindings(new CompositeName(name));
    }

    /**
     * 返回绑定的资源集合的迭代器
     * 迭代器中的元素是{@link Binding}包装类
     * 同 {@link WARDirContext#list(String)} 类似
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        if (name.isEmpty())
            return new NamingContextEnumeration<Binding>(list(entries));

        Entry<Entry> entryEntry = treeLookup(name);
        if (entryEntry == null)
            throw new NamingException("资源不存在！  " + name);

        return new NamingContextEnumeration<Binding>(list(entryEntry));
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持该操作
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 不支持连接，这里直接调用{@link WARDirContext#lookup(String)}
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
     * 返回此容器代表的根目录
     *
     * @return
     * @throws NamingException
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return this.docBase;
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        entries = null;
        if (base != null) {
            try {
                base.close();
            } catch (IOException e) {
                System.out.println("WAR文件关闭失败！ " + base.getName());
                e.printStackTrace(System.out);
            }
        }
        base = null;
    }

    /**
     * WAR资源属性
     */
    protected class WARResource extends Resource {
        protected ZipEntry entry;

        public WARResource(ZipEntry entry) {
            this.entry = entry;
            setContext(entry);
        }

        /**
         * 设置资源文件以二进制流的方式输入
         *
         * @param entry
         */
        public void setContext(ZipEntry entry) {
            try {
                this.inputStream = base.getInputStream(entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 视图类，可进行排序
     */
    protected class Entry<T extends Entry> implements Comparable<T> {
        protected String name; // 资源名称
        protected ZipEntry entry;
        protected List<Entry> children = Collections.synchronizedList(new ArrayList<>()); // 子文件可能超过1000，用List

        public Entry(String name, ZipEntry entry) {
            this.name = name;
            this.entry = entry;
        }

        /**
         * 按名字排序
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(T o) {
            return this.name.compareTo(o.getName());
        }

        public String getName() {
            return name;
        }

        public ZipEntry getEntry() {
            return entry;
        }

        public void addChild(Entry entry) {
            children.add(entry);
        }

        public List<Entry> getChildren() {
            return children;
        }

        public Entry getChild(String name) {
            for (Entry entry : children) {
                if (entry.name.equals(name)) {
                    return entry;
                }
            }

            return null;
        }
    }
}
