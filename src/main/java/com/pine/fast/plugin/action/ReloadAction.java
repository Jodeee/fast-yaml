package com.pine.fast.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.pine.fast.plugin.misc.Icons;

public class ReloadAction extends AnAction {
    public ReloadAction() {
        super("�����Զ�������");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Messages.showMessageDialog("���ܿ����У�", "Fast Yaml", Messages.getWarningIcon());

    }
}
