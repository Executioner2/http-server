## com.ranni.container包下类的说明

|类型|类名|说明|
|---|---|---|
|接口|Container|所有容器的根接口|
|接口|Engine|服务器引擎容器|
|接口|Host|包含一个或多个Context容器的虚拟机主机|
|接口|Context|一个Web应用程序。一个Context可以包含多个Wrapper|
|接口|Wrapper|表示一个独立的servlet|
|抽象类|ContainerBase|基本容器抽象类，由StandardWrapper继承|
|实现类|StandardEngine|Engine接口的标准实现类|
|实现类|StandardHost|Host接口的标准实现类|
|实现类|StandardContext|Context接口的标准实现类|
|实现类|StandardWrapper|Wrapper接口的标准实现类|

注：  
- 实现类：指对抽象方法（接口、抽象类）的实现
- 继承类：继承父类后增加一些属于自己的方法、属性
- 包装类：根据其设计意义指继承了实现类后对父类方法进行增强以便于其他类使用的类的统称
- 外观类：根据其设计意义指实现类对外开放访问，从而隐藏实现类具体实现的类的统称
  
### UML类图
![img](../../../../../../uml/v0.0.2/container.png)