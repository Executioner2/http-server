package com.ranni.connector.processor;

/**
 * Title: HttpServer
 * Description:
 * 处理器池接口
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 22:46
 */
public interface ProcessorPool {
    int INITIAL_MIN_PROCESSORS = 5; // 初始最小处理器数量
    int INITIAL_MAX_PROCESSORS = 20; // 初始最大处理器数量上限

    // 归还处理器
    void giveBackProcessor(Processor processor);

    // 返回处理器
    Processor getProcessor();

//    // 设置最大处理器数量
//    void setMaxProcessors(int max);
//
//    // 设置最小处理器数量
//    void setMinProcessors(int min);

    // 返回最大处理器数量
    int getMaxProcessors();

    // 返回最小处理器数量
    int getMinProcessors();

    // 设置已创建的处理器数量
    void setCurProcessors(int cur);

    // 已创建的处理器数量自增一
    void incCurProcessors();

    // 返回已创建的处理器数量
    int getCurProcessors();

    // 返回正在工作的处理器数量
    int getWorkingProcessors();
}
