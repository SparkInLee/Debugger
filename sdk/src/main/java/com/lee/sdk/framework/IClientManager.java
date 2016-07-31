package com.lee.sdk.framework;

import com.lee.sdk.service.IClient;

/**
 * @author jiangli
 */
public interface IClientManager {

    IClient getClient(String name);

    void removeClient(String name);

}
