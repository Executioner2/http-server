## com.ranni.container.context包下类/接口的说明

|类型|类名|说明|
|---|---|---|
|实现类|StandardContext|标准的Context容器，一个WEB应用程序对应一个Context容器|
|实现类|StandardContextMapper|Context容器的映射器，固定为http类型的映射器，主要作用是从请求的URL中取得对应的Mapper容器名字，然后返回给StandardContext查询出该Wrapper容器实例|
|实现类|StandardContextValve|StandardContext的基础阀，即管道中最后执行的阀，有且仅能有一个，该阀会根据请求的协议找到对应的映射器，然后从由映射器解析出处理这个请求的Wrapper容器名|
|实现类|StandardValveContext|阀容器，主要就是把基础阀和其它阀包装到了此类中，提供一个invokeNext()方法自动往下执行阀，由管道对象（StandardPipeline）调用|
  
   
注：  
- 实现类：指对抽象方法（接口、抽象类）的实现
- 继承类：继承父类后增加一些属于自己的方法、属性
- 包装类：根据其设计意义指继承了实现类后对父类方法进行增强以便于其他类使用的类的统称
- 外观类：根据其设计意义指实现类对外开放访问，从而隐藏实现类具体实现的类的统称

### UML类图
![img](../../../../../../../uml/v1.0.2/context.png)