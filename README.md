一个仿制Tomcat的HTTP服务器  
---
--- 

#
## 计划
+ Servlet类文件内容读取一次后会存入缓存中，但是缓存内容失效后又要重新将内容从流中取出来，计划后续提供解析一次就永久存入**内存**下来的选项



#
### 版本说明：1.0.2
##### 目录结构
|包|作用|
|----|----|
|com.ranni.util|此目录为工具模块|
|com.ranni.processor|此目录为核心模块|
|com.ranni.connector|此目录为连接器模块|  
|com.ranni.startup|此目录为HTTP服务器启动模块| 

+ [Request模块](./src/main/java/com/ranni/connector/http/request/README.md)
+ [Response模块](./src/main/java/com/ranni/connector/http/response/README.md)
+ [Container模块](./src/main/java/com/ranni/container/README.md)

**说明：**
1. 在此服务器demo中，有使用大量的数组而非ArrayList，而且在删除和增加元素时也使用了大量的
System.arraycopy()。至于为什么不用ArrayList，是因为数组和ArrayList在数据量不超过
1000时，两者的效率差不太多，但是数组却比ArrayList有更少的内存占用。此服务器中如果有数
据量超过1000的集合将会使用ArrayList。 
1. 注意，注释中“资源”指文件夹或文件，而“文件”就是文件，“文件夹”就是文件夹

**更新：**
- 新增HttpProcessorPool
- 新增Container模块结构
- 新增管道和阀机制
- 更新Request类关系
- 更新Response类关系
- 采用异步处理Http请求
  
**已知问题：**
- 服务器不能停下来（服务器没有实现应有的停止功能）
- 连接器线程只有一个
- Request部分方法未能实现（因为暂时还用不上）
- Response部分方法未能实现
- Session没做
- Context没做
- Container每次都得重新创建
- servlet实现类加载功能没有独立出来做个模块
- Wrapper是个空有其表的接口
- keepAlive和100状态码未实现
- CookieTools和DateTool为Tomcat源码文件  

# 
### 版本说明：0.0.1
#####目录结构： 
|包|作用|
|----|----|
|com.ranni.util|此目录为工具模块|
|com.ranni.processor|此目录为核心模块|
|com.ranni.connector|此目录为连接器模块|  
|com.ranni.startup|此目录为HTTP服务器启动模块|  

**更新：**
- 实现简单的启动器模块
- 实现简单的连接器模块
- 实现简单的核心模块

**已知问题：**
- 服务器本身为单线程工作
- 会收到客户端发来的空包（目前做丢弃处理）
- 未实现指定字符编码
- CookieTools和DateTool为Tomcat源码文件
- 被加载的类文件不能有package（即不能把自己打包）
- 对获取静态文件的支持不够完整