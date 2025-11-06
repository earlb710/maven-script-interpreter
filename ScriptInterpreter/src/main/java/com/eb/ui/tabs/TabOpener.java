package com.eb.ui.tabs;

public interface TabOpener {

    /**
     * Show (or create) an editor tab for the given file. If a tab for the file
     * already exists, it should be selected & focused. Returns true if a tab
     * was shown or created, false if the operation was cancelled/failed.
     */
    boolean showTab(TabContext context, boolean requestFocus) throws Exception;
    
}
