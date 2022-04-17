## com.ranni.naming包下类/接口的说明

此包存放了JNDI的实现，其中BaseDirContext、ProxyDirContext、FileDirContext、
WARDirContext是重中之重  
**标记了Deprecated的类暂时不做说明，因为后续有可能移除**
---

|类型|类名|说明|
|---|---|---|
|抽象类|BaseDirContext||
|实现类|ProxyDirContext||
|实现类|FileDirContext||
|实现类|WARDirContext||
|实现类|NameParserImpl||
|实现类|NamingContextEnumeration||
|实现类|NamingEntry||
|实现类|Resource||
|实现类|ResourceAttributes||


注：  
- 普通类：emm，就是普通类
- 实现类：指对抽象方法（接口、抽象类）的实现
- 继承类：继承父类后增加一些属于自己的方法、属性
- 包装类：根据其设计意义指继承了实现类后对父类方法进行增强以便于其他类使用的类的统称
- 外观类：根据其设计意义指实现类对外开放访问，从而隐藏实现类具体实现的类的统称
- 视图类：是集合的一部分（集合中的元素），同时也是一些属性的集合

### UML类图
![img](../../../../../../../uml/v1.0.2/loader.png)
