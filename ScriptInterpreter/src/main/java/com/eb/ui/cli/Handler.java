package com.eb.ui.cli;

import com.eb.ui.ebs.EbsConsoleHandler;
import javafx.scene.control.TabPane;

/**
 *
 * @author Earl Bosch
 */
public interface Handler {

    public void submit(String... lines) throws Exception;

    public void submitErrors(String... lines);

    public Object callBuiltin(String builtin, Object... args) throws Exception;
    
    public void setUI_tabPane(EbsConsoleHandler consoleHandler, TabPane mainTabs);

    public void setUI_outputArea(ScriptArea outputArea);
    
}
