package com.pine.fast.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.pine.fast.plugin.persistent.ServerPersistent;
import com.pine.fast.plugin.persistent.ServiceConfig;

public class OpenHitAction extends AnAction {

    public OpenHitAction() {
        super("Close Hint");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ServerPersistent serverPersistent = ServerPersistent.getInstance();
        ServiceConfig state = serverPersistent.getState();
        if (state == null) {
            return;
        }
        state.setHint(state.getHint() == null || !state.getHint());
        serverPersistent.loadState(state);
    }

    @Override
    public void update(AnActionEvent e) {
        ServerPersistent serverPersistent = ServerPersistent.getInstance();
        ServiceConfig state = serverPersistent.getState();
        if (state == null) {
            return;
        }
        e.getPresentation().setText(getHintText(state.getHint()));
        System.out.println("��ǰ״̬" + state.getHint());
    }

    private static String getHintText(Boolean hint) {
        return hint == null || hint ? "Close Hint" : "Open Hint";
    }
}
