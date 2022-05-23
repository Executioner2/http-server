package com.ranni.coyote;

/**
 * Title: HttpServer
 * Description:
 * 动作钩子，处理器实现此接口，缓冲流在读取数据触发
 * 相应的动作来做相应处理
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/22 23:36
 */
public interface ActionHook {

    /**
     * 向处理器发送动作
     * 
     * @param actionCode 动作代码
     * @param param 携带参数
     */
    void action(ActionCode actionCode, Object param);
}
