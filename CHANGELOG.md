CheckValve 2.0 Change Log
=========================

**2.0.0**

Bug Fixes
- Added better exception handling
- Fixed a few possible crashes due to database cursors being mishandled
- Trailing spaces in server URLs will no longer cause Unknown Host errors
- Fixed errors when moving servers up or down in the list if other servers have been deleted

Code Changes
- Made compatible with API 11+ (Honeycomb and above)
  - Moved network operations to background threads
  - Created new classes to handle all server queries in the background
- Increased the target SDK version to 19 (KitKat)
- Completely changed the handling of database queries
  - `DatabaseProvider` instances are now opened and closed more cleanly
  - Cursors are only used within methods of the `DatabaseProvider` class
  - SQLite queries are now wrapped in `synchronized()` blocks
- Eliminated the custom `MessageBox` class in favor of using standard `Toast` messages
- Eliminated the custom `ConfirmDelete` class in favor of using standard `AlertDialog` messages
- Redesigned UI elements
  - Added X buttons to dismiss the *Manage Server List* and *Player List* Activities
  - Added a button to the *About CheckValve* Activity
  - Updated the *Manage Server List* screen so it looks better and is easier to use
  - Moved the *Cancel* button to the left side on any screen which has it, to be compliant with the [Android design guidelines](http://developer.android.com/design/building-blocks/dialogs.html) for dialogs and action buttons
    - **Note:** Dialogs which use the built-in `AlertDialog` class will have the *Cancel* button on the right under old Android versions prior to 11.
  - Easier navigation on several dialog screens
- Removed unused code, classes, layouts, and string entires
- Added the `Values` class to standardize constants used throughout the app
- Added `ACCESS_NETWORK_STATE` permission requirement for monitoring the network state while using the Chat Viewer

New Features
- Show/hide RCON passwords on screens where they appear
- RCON command auto-fill for common commands
- Warn before sending unsafe commands via RCON
- View chat
  - View in-game player chat messages in real time
  - Send console chat messages to the server
  - Requires a [CheckValve Chat Relay](https://github.com/daparker/checkvalve-chat-relay)
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

Notes
- While the target SDK version has been increased, the minimum SDK version remains 8 (Froyo).
- I am trying very hard to keep the minimum SDK version at 8 so that CheckValve will continue to work on older devices.  However, changes to the Android API are making it increasingly difficult to stay comaptible with any SDK version below 11 (Honeycomb), and the minimum SDK may need to be rasied in the near future out of necessity.

**2.0.1**
- Added an action bar for devices which do not have a dedicated menu button (Honeycomb and above)

**2.0.2**
- Fixed a bug which prevented RCON from working with GoldSrc servers
- Fixed a bug which caused the "Warn if a command is unsafe" option to not be honored

**2.0.3**
- Fixed a crash when resuming CheckValve after the Chat Viewer was left open for a long time
- Fixed HLTV query issue
- Redesigned About screen with links for support
- Added support for the old GoldSrc query response
- Up/Down arrow keys scroll through RCON command history
- RCON will reconnect automatically when the device changes connections
- Server field on Chat Relay Details screen will auto-fill with previously used hosts

**2.0.4**
- Added UTF-8 support for server information, player names, and chat messages

**2.0.5**
- Added server nicknames
- Added server ping
- Added option for default RCON font size
- Added backup/restore support (Settings > Backups)
- Added READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE permissions for reading/writing backups
- Added debug mode to see more information about server queries and response times
- Fixed NumberFormatException caused by invalid integers in some input fields
- Fixed NullPointerException caused by invalid engine type during RCON connection
- Fixed UI bugs on the Manage Servers screen
- Fixed button text wrapping on the Add Server and Edit Server screens
- Various coding and efficiency improvements
- RCON improvements:
  - Volume keys increase/decrease font size while in a session
  - Added SourceMod commands to the suggestions (must be enabled in Settings)
  - Added the logaddress command to the suggestions (for GoldSrc servers)
