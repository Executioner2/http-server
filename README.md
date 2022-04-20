一个仿制Tomcat的HTTP服务器  
---
--- 

#
## 计划
+ Servlet类文件内容读取一次后会存入缓存中，但是缓存内容失效后又要重新将内容从流中取出来，计划后续提供解析一次就永久存入**内存**下来的选项
+ session id目前没有同router id（router id每个服务器一个，用于实现负载均衡）一起组成复合型session id。计划选择性加入这个，因为session可能会被设计为一个独立于服务器的session池。
+ 热部署
+ 日志记录
+ 注解式
+ 图形界面


#
### 版本说明：1.0.2
##### 目录结构
|包|作用|
|----|----|
|com.ranni.util|此目录为工具类存放目录|
|com.ranni.connector|此目录为连接器实现文件存放目录|  
|com.ranni.container|此目录为容器实现文件存放目录| 
|com.ranni.startup|此目录为HTTP服务器启动文件存放目录| 
|com.ranni.logger|此目录为HTTP服务器日志记录器实现文件存放目录| 
|com.ranni.naming|此目录为JNDI命名资源实现文件存放目录| 
|com.ranni.lifecycle|此目录为生命周期定义文件存放目录| 

+ [Request](./src/main/java/com/ranni/connector/http/request/README.md)
+ [Response](./src/main/java/com/ranni/connector/http/response/README.md)
+ [Container](./src/main/java/com/ranni/container/README.md)
+ [Naming](./src/main/java/com/ranni/naming/README.md)
+ [Lifecycle](./src/main/java/com/ranni/lifecycle/README.md)

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
- 新增loader模块
- 新增logger模块
- 新增mapper（映射器）
- 新增lifecycle模块
- 新增naming模块（JNDI）
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