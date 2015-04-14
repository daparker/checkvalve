CheckValve 2.0
==============

CheckValve 2.0 is currently under development.  This version of CheckValve includes many changes:

**Bug fixes**
- Added better exception handling
- Fixed a few NPEs.

**Code Changes**
- Completely changed the handling of database queries
  - DatabaseProvider instances are now opened and closed more cleanly within each class that uses it
  - Cursors are only used within methods of the DatabaseProvider class
- Moved all network operations to background threads 

**New Features**
- View chats
- Settings
- Updates for Jelly Bean

