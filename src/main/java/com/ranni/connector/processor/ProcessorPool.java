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
    // 返回处理器
    Processor getProcessor();

    // 设置最大处理器数量
    void setMaxProcessors(int max);

    // 设置最小处理器数量
    void setMinProcessors(int min);

    // 返回最大处理器数量
    int getMaxProcessors();

    // 返回最小处理器数量
    int getMinProcessors();

    // 返回已创建的处理器数量
    int getCurProcessors();

    // 返回正在工作的处理器数量
    int getWorkingProcessors();
}
