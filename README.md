CheckValve 2.0
==============

CheckValve 2.0 is currently under development.  This version of CheckValve includes many changes:

**Bug Fixes**
- Added better exception handling
- Fixed a few possible crashes due to database cursors being mishandled


**Code Changes**
- Made compatible with API 11+ (Honeycomb and above)
  - Moved network operations to background threads
  - Created new classes to handle all server queries in the background
- Increased the target SDK version to 19 (KitKat)
- Completely changed the handling of database queries
  - `DatabaseProvider` instances are now opened and closed more cleanly
  - Cursors are only used within methods of the `DatabaseProvider` class
  - SQLite queries are now wrapped in `synchronized()` blocks
- Eliminated the custom `MessageBox` class in favor of using standard `Toast` messages
- Redesigned some UI elements
  - The *Manage Server List* screen looks better and is easier to use
  - Moved the *Cancel* button to the left side on any screen which has it, to be compliant with the [Android design guidelines](http://developer.android.com/design/building-blocks/dialogs.html) for dialogs and action buttons
    - **Note:** Dialogs which use the built-in `AlertDialog` class will have the *Cancel* button on the right under old Android versions (prior to 11).
  - Easier navigation on several dialog screens
- Removed unused code, classes, layouts, and string entires


**New Features**
- View chat
  - Requires a [CheckValve Chat Relay](https://github.com/daparker/checkvalve-chat-relay)
  - View in-game player chat messages in real time
  - Send console chat messages to the server
- Settings
  - General
    - Show RCON passwords
    - Validate new servers
  - RCON
    - Show suggested commands (auto-fill)
    - Warn before sending unsafe commands
  - Server information
    - Show server name
    - Show IP/Port
    - Show game and version
    - Show number of players
    - Show map name
    - Show server tags
  - Default query options
    - Default port
    - Default timeout
  - Default Chat Relay server
    - IP
    - Port
    - Password


**Notes**
- While the target SDK version has been increased, the minimum SDK version remains 8 (Froyo).
- I am trying very hard to keep the minimum SDK version at 8 so that CheckValve will continue to work on older devices.  However, changes to the Android API are making it increasingly difficult to stay comaptible with any SDK version below 11 (Honeycomb), and the minimum SDK may need to be rasied in the near future out of necessity.
