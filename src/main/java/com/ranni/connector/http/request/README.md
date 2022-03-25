## com.ranni.connector.http.request包下类的说明

|类型|类名|说明|
|---|---|---|
|接口|Request|定义了一系列通用协议请求所需要的方法|
|接口|HttpRequest|继承于Request接口，然后定义一些仅适用于HTTP协议的方法|
|抽象类|RequestBase|实现了Request接口所有方法，实现了ServletRequest中除了操作parameters对象（该对象是在此抽象类的子类HttpRequestBase中定义的）的所有方法|
|实现类|HttpRequestBase|实现了RequestBase中没实现的方法以及HttpRequest中所有方法，解析参数的行为在此类中进行|
|继承类|HttpRequestImpl|额，暂无应用|
|包装类|RequestStream|ServletInputStream的包装类，增加了数据大小限制以及能且仅能在逻辑上关闭流。此类的对象在RequestBase实现获取，在HttpRequestBase实现对数据的读取|
|外观类|RequestFacade|表面上实现了ServletRequest接口，但实际是关联了HttpRequestBase对象然后调用其方法的外观类。此类的对象由HttpRequestBase创建并提供返回方法|
|外观类|HttpRequestFacade|表面上实现了HttpServletRequest接口，实际也是关联了HttpRequestBase然后调用其方法的外观类|

注：  
- 实现类：指对抽象方法（接口、抽象类）的实现
- 继承类：继承父类后增加一些属于自己的方法、属性
- 包装类：根据其设计意义指继承了实现类后对父类方法进行增强以便于其他类使用的类的统称
- 外观类：根据其设计意义指实现类对外开放访问，从而隐藏实现类具体实现的类的统称
  
  
### UML类图
![img](../../../../../../../../uml/v0.0.2/request.png)