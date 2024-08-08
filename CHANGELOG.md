CheckValve 2.0 Change Log
=========================

**2.0.15**
- Updated the app to target Android 14 per the new Google Play requirements
- Updated to allow any storage location (local or external) when creating or restoring a backup
- General code cleanup and updates to address deprecated features, etc.

**2.0.14**
- Updated the app to target Android 12 (Snow Cone) per the new Google Play requirements
- Fixed a bug affecting the options to show/hide the game version and current map

**2.0.13**
- Fixed notifications being enabled after a device reboot when disabled in settings
- Made the RCON console text selectable

**2.0.12**
- Updated server queries to support Valve's upcoming protocol changes
- Updated the app to target Android 10 per the new Google Play requirements
- Changed the backup file read and write location to CheckValve's files folder
- Fixed the player search attempting to query disabled servers

**2.0.11**
- Fixed a bug when sending a "say" message from the Chat Viewer to an HLDS server

**2.0.10**
- Fixed background queries not working in Android Oreo and above (issue #11)
- Fixed notifications not working on Android Oreo and above (issue #12)
- Fixed some typos in the source code

**2.0.9**
- Fixed crashes when creating or restoring backup files on Android 6 and above
- Removed support for Android versions prior to 4.0 (Ice Cream Sandwich)

**2.0.8**
- Updated the app to target Android Oreo per the new Google Play requirements
- Removed support for Android versions prior to Honeycomb

**2.0.7**
- Fixed duplicate nickname error when saving changes on the Edit Server screen (issue #10)

**2.0.6**
- Added notifications (Settings > Notifications)
- Added RECEIVE_BOOT_COMPLETED permission
- Added a checkbox on the Manage Servers screen to enable/disable each server
- Fixed a few bugs

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

**2.0.4**
- Added UTF-8 support for server information, player names, and chat messages

**2.0.3**
- Fixed a crash when resuming CheckValve after the Chat Viewer was left open for a long time
- Fixed HLTV query issue
- Redesigned About screen with links for support
- Added support for the old GoldSrc query response
- Up/Down arrow keys scroll through RCON command history
- RCON will reconnect automatically when the device changes connections
- Server field on Chat Relay Details screen will auto-fill with previously used hosts

**2.0.2**
- Fixed a bug which prevented RCON from working with GoldSrc servers
- Fixed a bug which caused the "Warn if a command is unsafe" option to not be honored

**2.0.1**
- Added an action bar for devices which do not have a dedicated menu button (Honeycomb and above)

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
