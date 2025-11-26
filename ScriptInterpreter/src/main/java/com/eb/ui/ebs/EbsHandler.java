package com.eb.ui.ebs;

import com.eb.script.RuntimeContext;
import com.eb.script.interpreter.builtins.Builtins;
import com.eb.script.interpreter.Environment;
import com.eb.script.interpreter.Interpreter;
import com.eb.script.interpreter.InterpreterError;
import com.eb.script.parser.ParseError;
import com.eb.script.parser.Parser;
import com.eb.script.token.ebs.EbsLexer;
import com.eb.script.token.ebs.EbsToken;
import com.eb.ui.cli.Handler;
import com.eb.ui.cli.ScriptArea;
import com.eb.ui.tabs.TabContext;
import com.eb.ui.tabs.TabHandler;
import com.eb.util.Debugger;
import java.io.IOException;
import java.util.List;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 *
 * @author Earl Bosch
 */
public class EbsHandler implements Handler {

    protected final RuntimeContext ctx;
    protected final Environment env;
    protected final Debugger dbg;           // debugger bound to the env
    protected TabHandler tabHandler;
    protected StatusBar statusBar;
    protected final Interpreter interpreter;  // Persistent interpreter to maintain screen state

    public EbsHandler(RuntimeContext ctx) {
        this.ctx = ctx;
        this.env = ctx.environment;
        this.dbg = env.getDebugger();
        this.interpreter = new Interpreter();  // Create persistent interpreter instance
    }

    @Override
    public void setUI_tabPane(EbsConsoleHandler consoleHandler, TabPane mainTabs) {
        this.tabHandler = new TabHandler(consoleHandler, mainTabs);
    }

    @Override
    public void setUI_outputArea(ScriptArea outputArea) {
        this.env.registerOutputArea(outputArea);
    }
    
    /**
     * Set the status bar for this handler
     * @param statusBar The status bar to use
     */
    public void setStatusBar(StatusBar statusBar) {
        this.statusBar = statusBar;
    }
    
    /**
     * Get the status bar associated with this handler
     * @return The status bar, or null if not set
     */
    public StatusBar getStatusBar() {
        return statusBar;
    }
    
    /**
     * Get the interpreter instance for this handler
     * @return The interpreter
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    @Override
    public Object callBuiltin(String builtin, Object... args) throws InterpreterError {
        Object ret = Builtins.callBuiltin(env, builtin.toLowerCase(), args);
        if (env.isEchoOn()) {
            ScriptArea output = env.getOutputArea();
            output.printlnOk(Builtins.getBuiltinCallString(builtin, ret, args));
        }
        return ret;
    }

    /**
     * Process a submitted lines
     */
    @Override
    public void submit(String... lines) throws IOException, ParseError, InterpreterError {
        ScriptArea output = env.getOutputArea();
        
        // Copy debug mode state from current thread (UI thread) to this thread before interpreting
        // This ensures that if debug mode was enabled via Ctrl+D in the editor, it will be active
        // during script interpretation and screen creation
        boolean debugModeEnabled = com.eb.script.interpreter.screen.ScreenFactory.getDebugModeForInheritance();
        com.eb.script.interpreter.screen.ScreenFactory.setDebugModeForThread(debugModeEnabled);
        
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                List<EbsToken> tokens = new EbsLexer().tokenize(line);
                Parser.parse(ctx, line, tokens);
                if (env.isEchoOn() && ctx.statements.length == 1) {
                    EbsStyled.appendStyledText(output, "> " + line + "\n");
                }
                interpreter.interpret(ctx);  // Use persistent interpreter instance
            }
        }
    }

    @Override
    public void submitErrors(String... lines) {
        ScriptArea output = env.getOutputArea();
        for (String l : lines) {
            output.printlnError(l);
        }
    }

    public Tab getSelectedTab() {
        return tabHandler.getSelectedTab();
    }

    public TabContext getSelectedTabContext() {
        return tabHandler.getSelectedTabContext();
    }
    
    /**
     * Close the given tab if it's closable
     * @param tab The tab to close
     */
    public void closeTab(Tab tab) {
        if (tabHandler != null) {
            tabHandler.closeTab(tab);
        }
    }

}
