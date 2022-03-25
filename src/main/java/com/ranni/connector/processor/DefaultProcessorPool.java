package com.ranni.connector.processor;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-25 23:03
 */
public class DefaultProcessorPool implements ProcessorPool {
    @Override
    public Processor getProcessor() {
        return null;
    }

    @Override
    public void setMaxProcessors(int max) {

    }

    @Override
    public void setMinProcessors(int min) {

    }

    @Override
    public int getMaxProcessors() {
        return 0;
    }

    @Override
    public int getMinProcessors() {
        return 0;
    }

    @Override
    public int getCurProcessors() {
        return 0;
    }

    @Override
    public int getWorkingProcessors() {
        return 0;
    }
}
