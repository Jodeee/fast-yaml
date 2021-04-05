package com.pine.fast.plugin.persistent;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.generate.element.Element;

/**
 * name���־û��ļ�������ƿ�����ָ����ͨ���ò�����Ƽ��ɡ�
 * storages ���������ò����ĳ־û�λ�á�����$APP_CONFIG$����ΪIdea��װ��Ĭ�ϵ��û�·�������磺C:\Documents and
 * Settings\10139682\.IdeaIC2017.3\config\options\searchJarPath.xml
 *
 * �������������壨SearchableConfigurable��
 */
@State(name = "config", storages = {@Storage(value ="$APP_CONFIG$/config.xml")})
public class ServerPersistent implements PersistentStateComponent<ServiceConfig> {
    private ServiceConfig serviceConfig = new ServiceConfig();

    public static ServerPersistent getInstance() {
        return ServiceManager.getService(ServerPersistent.class);
    }


    @Nullable
    @Override
    public ServiceConfig getState() {
        return serviceConfig;
    }

    @Override
    public void loadState(ServiceConfig state) {
        XmlSerializerUtil.copyBean(state, serviceConfig);
    }

    @Override
    public void noStateLoaded() {

    }
}
