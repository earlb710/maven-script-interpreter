# Help Screen Search Feature

## Overview
This feature adds a search combobox with a search button on top of the help tree view, allowing users to quickly find and navigate to help topics.

## Visual Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  EBS Syntax Help                                                │
├──────────────────────┬──────────────────────────────────────────┤
│ ┌──────────────────┐ │                                          │
│ │ [Search box▼][⚲] │ │                                          │
│ └──────────────────┘ │                                          │
│ ┌──────────────────┐ │                                          │
│ │ Help Topic       │ │         Help Content (WebView)           │
│ ├──────────────────┤ │                                          │
│ │ ▼ Help Topics    │ │                                          │
│ │   ▼ Syntax       │ │                                          │
│ │     ▶ Structure  │ │                                          │
│ │     ▶ Data       │ │                                          │
│ │     ▶ Flow       │ │                                          │
│ │     ▶ Builtins   │ │                                          │
│ │   ▼ Examples     │ │                                          │
│ │   ▼ Guides       │ │                                          │
│ └──────────────────┘ │                                          │
└──────────────────────┴──────────────────────────────────────────┘
```

## Components Added

### 1. Search ComboBox
- **Type**: Editable ComboBox
- **Location**: Top of the tree area (left panel)
- **Features**:
  - Pre-populated with all searchable help topics
  - Editable for custom search queries
  - Placeholder text: "Search help topics..."
  - Width: 180px (grows to fill available space)

### 2. Search Button
- **Type**: Button
- **Label**: "Search"
- **Location**: Next to search combobox
- **Width**: 60px
- **Action**: Triggers search functionality

## Technical Implementation

### File Changes

#### 1. help-screen.json
- Added `searchQuery` screen variable (type: string)
- Added HBox container (`searchBox`) with:
  - Search combobox bound to `searchQuery` variable
  - Search button with `onClick` event handler
- Modified tree area layout to accommodate search UI

#### 2. help.ebs
- Added `collectSearchableTopics()` function:
  - Recursively walks the help tree structure
  - Collects full paths for all topics (e.g., "Help Topics > Syntax > Builtins")
  - Returns array of searchable topic paths

- Added `performSearch()` function:
  - Takes search query as parameter
  - Performs case-insensitive search
  - Matching logic (in order of priority):
    1. Exact match on topic name (leaf node)
    2. Partial match on topic name
    3. Partial match on full path
  - Updates `helpScreen.selectedTopic` with best match
  - Displays matching topic content in WebView
  - Shows "No match found" message if no results

- Modified tree injection code:
  - Populates search combobox options with all collected topics
  - Sorts options alphabetically for easy browsing

## Search Behavior

### Exact Match Example
- Query: "string"
- Finds: "String" topic (exact match, case-insensitive)
- Action: Displays "String" help content immediately

### Partial Match Example
- Query: "array"
- Finds: "Arrays" topic (partial match on topic name)
- Alternative matches: "array.add", "array.sort", etc.
- Action: Selects first matching topic

### Path Match Example
- Query: "flow conditionals"
- Finds: "Conditionals" (matches full path "Help Topics > Syntax > Flow > Conditionals")
- Action: Displays "Conditionals" help content

## User Workflow

1. **Type Search Query**: User types a topic name or keyword in the search combobox
2. **Select from Dropdown**: OR user selects from pre-populated list of topics
3. **Click Search**: User clicks the "Search" button
4. **View Results**: The matching topic is automatically selected in the tree and its content is displayed

## Benefits

1. **Fast Navigation**: Quickly find topics without manually expanding tree nodes
2. **Autocomplete**: Dropdown shows all available topics for easy selection
3. **Flexible Search**: Supports exact and partial matches
4. **Case-Insensitive**: Works regardless of input case
5. **User-Friendly**: Familiar search pattern for help systems

## Code Quality

- ✅ Parse validation successful (no syntax errors)
- ✅ Code review feedback addressed
- ✅ Follows EBS language conventions
- ✅ Uses existing builtins (str.split, str.toLower, str.contains, json operations)
- ✅ Minimal changes to existing code
- ✅ No breaking changes to existing help screen functionality
