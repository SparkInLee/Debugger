package com.lee.gui;

import java.awt.EventQueue;

public interface CallBack<T> {
    void response(T t);

    void error(Exception e);

    class CallBackAdapter<T> implements CallBack<T> {

        @Override
        public void response(T t) {
            // TODO Auto-generated method stub

        }

        @Override
        public void error(Exception e) {
            // TODO Auto-generated method stub
            e.printStackTrace();
        }
    }

    class CallBackWrapper<T> implements CallBack<T> {
        CallBack<T> delegate;

        public CallBackWrapper(CallBack<T> delegate) {
            // TODO Auto-generated constructor stub
            this.delegate = delegate;
        }

        @Override
        public void response(final T t) {
            // TODO Auto-generated method stub
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    delegate.response(t);
                }
            });
        }

        @Override
        public void error(final Exception e) {
            // TODO Auto-generated method stub
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    delegate.error(e);
                }
            });
        }

    }
}
