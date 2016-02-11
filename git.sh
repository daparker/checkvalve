#!/bin/bash

git add src/com/github/daparker/checkvalve/CheckValve.java
git add src/com/github/daparker/checkvalve/ServerQuery.java
git commit -m "Added query debug mode"
git add src/com/github/daparker/checkvalve/Values.java
git commit -m "Added values for debug mode"
git add src/com/github/daparker/checkvalve/DebugConsoleActivity.java
git add src/com/github/daparker/checkvalve/QueryDebugLog.java
git commit -m "Added new class for query debugging"
git add res/layout/debug_console.xml
git commit -m "Added layout for debug console"
git add res/menu/main_menu_debug.xml
git add res/menu-v11/main_menu_debug.xml
git commit -m "Added menu option to show debug log"
git add res/values/strings.xml
git commit -m "Added strings for debug mode"
git add AndroidManifext.xml
git commit -m "Added DebugConsoleActivity"
git add .
git commit -m "Update"

exit 0
