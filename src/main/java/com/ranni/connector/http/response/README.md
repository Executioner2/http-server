## com.ranni.connector.http.response包下类的说明

|类型|类名|说明|
|---|---|---|
|接口|Response|定义了一系列通用协议响应所需要的方法|
|接口|HttpResponse|继承于Response接口，然后定义一些仅适用于HTTP协议的方法|
|抽象类|ResponseBase|实现了Response和ServletResponse接口所有方法，此类的主要实现了数据写入以及创建对外访问的输出流和输出处理流，设置的响应参数也仅有Content-Type和Content-Length两个|
|实现类|HttpResponseBase|实现了HttpResponse中所有方法，添加响应行、响应头和响应体的参数以及写入数据在此类中实现，写入数据实际是调用父类的flushBuffer方法，在此类中仅在调用父类flushBuffer时判断是否是第一次提交，如果是就要先把响应行和响应头输出出去|
|继承类|HttpResponseImpl|额，暂无应用|
|包装类|ResponseStream|ServletOutputStream的包装类，增加了数据大小限制以及能且仅能在逻辑上关闭流。此类的对象在ResponseBase实现获取|
|包装类|ResponseWriter|对PrintWriter的包装，仅增加写入后自动刷新流的功能|
|外观类|ResponseFacade|表面上实现了ServletResponse接口，但实际是关联了HttpResponseBase对象然后调用其方法的外观类。此类的对象由HttpResponseBase创建并提供返回方法|
|外观类|HttpResponseFacade|表面上实现了HttpServletResponse接口，实际也是关联了HttpResponseBase然后调用其方法的外观类|

注：  
- 实现类：指对抽象方法（接口、抽象类）的实现
- 继承类：继承父类后增加一些属于自己的方法、属性
- 包装类：根据其设计意义指继承了实现类后对父类方法进行增强以便于其他类使用的类的统称
- 外观类：根据其设计意义指实现类对外开放访问，从而隐藏实现类具体实现的类的统称
  
  
### UML类图
![img](../../../../../../../../uml/v0.0.2/response.png)