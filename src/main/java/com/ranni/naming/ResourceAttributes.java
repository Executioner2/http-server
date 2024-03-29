package com.ranni.naming;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 * 资源文件的属性
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-07 14:23
 */
public class ResourceAttributes implements Attributes {
    /**---------------------------默认属性名---------------------------**/
    public static final String CREATION_DATE = "creationdate"; // 创建日期
    public static final String ALTERNATE_CREATION_DATE = "creation-date"; // 创建日期
    public static final String LAST_MODIFIED = "getlastmodified"; // 最后修改的日期
    public static final String ALTERNATE_LAST_MODIFIED = "last-modified"; // 最后修改的日期
    public static final String NAME = "displayname"; // 名字
    public static final String TYPE = "resourcetype"; // 资源类型
    public static final String ALTERNATE_TYPE = "content-type"; // 资源类型
    public static final String SOURCE = "source"; // 源
    public static final String CONTENT_TYPE = "getcontenttype"; // 内容类型
    public static final String CONTENT_LANGUAGE = "getcontentlanguage"; // 内容语言
    public static final String CONTENT_LENGTH = "getcontentlength"; // 内容长度
    public static final String ALTERNATE_CONTENT_LENGTH = "content-length"; // 内容长度
    public static final String ETAG = "getetag"; // 标签
    public static final String COLLECTION_TYPE = "<collection/>"; // 集合类型

    protected long contentLength = -1; // 属性内容长度
    protected Attributes attributes; // 外部属性
    protected long lastModified = -1L; // 最后修改时间
    protected Date lastModifiedDate; // 最后修改的日期
    protected long creation = -1L; // 创建时间
    protected Date creationDate; // 创建日期
    protected boolean collection; // 集合标志位
    protected String name;

    // 要转换的日期格式
    protected static final SimpleDateFormat formats[] = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };


    public ResourceAttributes() {
    }

    /**
     * 用其它实现了Attribute的类的实例中的Attribute通用参数创建
     * 一个新的ResourceAttributes对象
     *
     * @param attributes
     */
    public ResourceAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    /**
     * 是否是集合
     * 
     * @return
     */
    public boolean isCollection() {
       if (attributes != null) 
           return getResourceType().equals(COLLECTION_TYPE);
       return collection;
    }

    /**
     * 返回资源类型
     *
     * @return
     */
    public String getResourceType() {
        String result = null;
        if (attributes != null) {
            Attribute attribute = attributes.get(TYPE);
            if (attribute != null) {
                try {
                    result = attribute.get().toString();
                } catch (NamingException e) {
                    ;
                }
            }
        }

        if (result == null) {
            if (collection)
                result = COLLECTION_TYPE;
            else
                result = "";
        }

        return result;
    }


    @Override
    public boolean isCaseIgnored() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Attribute get(String attrID) {
        return null;
    }

    @Override
    public NamingEnumeration<? extends Attribute> getAll() {
        return null;
    }

    @Override
    public NamingEnumeration<String> getIDs() {
        return null;
    }

    @Override
    public Attribute put(String attrID, Object val) {
        return null;
    }

    @Override
    public Attribute put(Attribute attr) {
        return null;
    }

    @Override
    public Attribute remove(String attrID) {
        return null;
    }

    @Override
    public Object clone() {
        return null;
    }

    /**
     * 返回文件创建时间
     *
     * @return
     */
    public long getCreation() {
        if (creation != -1L)
            return creation;
        if (creationDate != null)
            return creationDate.getTime();
        if (attributes != null) {
            Attribute attribute = attributes.get(CREATION_DATE);
            if (attribute != null) {
                try {
                    Object o = attribute.get();
                    if (o instanceof Long) {
                        creation = ((Long) o).longValue();
                    } else if (o instanceof Date) {
                        creationDate = (Date) o;
                        creation = creationDate.getTime();
                    } else {
                        String createDateValue = o.toString();
                        Date result = null;
                        // 依次尝试能够转化为日期的格式
                        for (int i = 0; i < formats.length
                                && result == null; i++) {
                            try {
                                result = formats[i].parse(createDateValue);
                            } catch (ParseException e) {
                                ;
                            }
                        }

                        if (result != null) {
                            creationDate = result;
                            creation = creationDate.getTime();
                        }
                    }
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return creation;
    }

    /**
     * 返回文件创建日期
     *
     * @return
     */
    public Date getCreationDate() {
        if (creationDate != null)
            return creationDate;
        if (creation != -1L) {
            creationDate = new Date(creation);
            return creationDate;
        }

        if (attributes != null) {
            Attribute attribute = attributes.get(CREATION_DATE);
            if (attribute != null) {
                try {
                    Object o = attribute.get();
                    if (o instanceof Long) {
                        creation = ((Long) o).longValue();
                    } else if (o instanceof Date) {
                        creationDate = (Date) o;
                        creation = creationDate.getTime();
                    } else {
                        String createDateValue = o.toString();
                        Date result = null;
                        // 依次尝试能够转化为日期的格式
                        for (int i = 0; i < formats.length
                                && result == null; i++) {
                            try {
                                result = formats[i].parse(createDateValue);
                            } catch (ParseException e) {
                                ;
                            }
                        }

                        if (result != null) {
                            creationDate = result;
                            creation = creationDate.getTime();
                        }
                    }
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return creationDate;
    }

    /**
     * 返回资源文件长度
     *
     * @return
     */
    public long getContentLength() {
        if (contentLength != -1L)
            return contentLength;
        if (attributes != null) {
            Attribute attribute = attributes.get(CONTENT_LENGTH);
            if (attribute != null) {
                try {
                    Object o = attribute.get();
                    if (o instanceof Long) {
                        contentLength = ((Long) o).longValue();
                    } else {
                        try {
                            contentLength = Long.parseLong(o.toString());
                        } catch (NumberFormatException e){
                            ;
                        }
                    }
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return contentLength;
    }

    /**
     * 设置属性内容长度
     *
     * @param length
     */
    public void setContentLength(long length) {
        this.contentLength = length;
        if (attributes != null)
            attributes.put(CONTENT_LENGTH, length);
    }

    /**
     * 取得名字
     *
     * @return
     */
    public String getName() {
        if (name != null)
            return name;

        if (attributes != null) {
            Attribute attribute = attributes.get(NAME);
            if (attribute != null) {
                try {
                    name = attribute.get().toString();
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return name;
    }

    /**
     * 取得最后一次修改的日期
     *
     * @return
     */
    public Date getLastModifiedDate() {
        if (lastModifiedDate != null)
            return lastModifiedDate;
        else if (lastModified != -1L) {
            lastModifiedDate = new Date(lastModified);
            return lastModifiedDate;
        }

        if (attributes != null) {
            Attribute attribute = attributes.get(LAST_MODIFIED);
            if (attribute != null) {
                try {
                    Object o = attribute.get();

                    if (o instanceof Long)
                        lastModified = ((Long) o).longValue();
                    else if (o instanceof Date) {
                        lastModifiedDate = (Date)o;
                        lastModified = lastModifiedDate.getTime();
                    } else {
                        String lastModifiedDateValue = o.toString();
                        Date result = null;

                        for (int i = 0; i < formats.length
                                && result == null; i++) {
                            try {
                                 result = formats[i].parse(lastModifiedDateValue);
                            } catch (ParseException e) {
                                ;
                            }
                        }

                        if (result != null) {
                            lastModifiedDate = result;
                            lastModified = lastModifiedDate.getTime();
                        }
                    }
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return lastModifiedDate;
    }

    /**
     * 属性最后修改时间
     */
    public long getLastModified() {
        if (lastModified != -1L)
            return lastModified;
        if (this.lastModifiedDate != null)
            return lastModifiedDate.getTime();

        if (attributes != null) {
            Attribute attribute = attributes.get(LAST_MODIFIED);
            if (attribute != null) {
                try {
                    Object o = attribute.get();
                    if (o instanceof Long) {
                        lastModified = ((Long) o).longValue();
                    } else if (o instanceof Date) {
                        lastModifiedDate = (Date) o;
                        lastModified = lastModifiedDate.getTime();
                    } else {
                        String lmdv = o.toString(); // 最后修改的日期值（字符串）
                        Date result = null;

                        for (int i = 0; result == null
                            && i < formats.length; i++) {
                            try {
                                result = formats[i].parse(lmdv);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        if (result != null) {
                            lastModified = result.getTime();
                            lastModifiedDate = result;
                        }
                    }
                } catch (NamingException e) {
                    ;
                }
            }
        }

        return lastModified;
    }

    /**
     * 资源创建日期
     *
     * @param date
     */
    public void setCreationDate(Date date) {
        this.creation = date.getTime();
        this.creationDate = date;
        if (attributes != null)
            attributes.put(CREATION_DATE, date);
    }

    /**
     * 设置资源名
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
        if (attributes != null)
            attributes.put(NAME, name);
    }

    /**
     * 设置资源类型
     *
     * @param s
     */
    public void setResourceType(String s) {
        collection = COLLECTION_TYPE.equals(s);
        if (attributes != null)
            attributes.put(TYPE, s);
    }

    /**
     * 设置上次修改日期
     *
     * @param date
     */
    public void setLastModifiedDate(Date date) {
        this.lastModifiedDate = date;
        this.lastModified = date.getTime();
        if (attributes == null)
            attributes.put(LAST_MODIFIED, date);
    }
}
