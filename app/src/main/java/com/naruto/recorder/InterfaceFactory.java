package com.naruto.recorder;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2019/11/5 0005
 * @Note
 */
public class InterfaceFactory {

    public interface SimpleOperation {
        void done();
    }

    public interface Operation<T> {
        void done(T data);
    }
}
