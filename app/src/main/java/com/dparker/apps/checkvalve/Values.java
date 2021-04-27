/*
 * Copyright 2010-2019 by David A. Parker <parker.david.a@gmail.com>
 *
 * This file is part of CheckValve, an HLDS/SRCDS query app for Android.
 *
 * CheckValve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * CheckValve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the CheckValve source code.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.dparker.apps.checkvalve;

import android.content.Context;
import android.os.Bundle;

/**
 * This class provides static values for standardizing things throughout
 * the CheckValve source code.
 *
 * @author David A. Parker
 */
public final class Values {
    // Engine types
    public static final int ENGINE_SOURCE = 0;
    public static final int ENGINE_GOLDSRC = 1;

    // Activity codes
    public static final int ACTIVITY_ABOUT = 2;
    public static final int ACTIVITY_ADD_NEW_SERVER = 3;
    public static final int ACTIVITY_CHAT = 4;
    public static final int ACTIVITY_CHAT_RELAY_DETAILS_DIALOG = 5;
    public static final int ACTIVITY_CONFIRM_DELETE = 6;
    public static final int ACTIVITY_MANAGE_SERVERS = 7;
    public static final int ACTIVITY_RCON = 8;
    public static final int ACTIVITY_RCON_PASSWORD_DIALOG = 9;
    public static final int ACTIVITY_SHOW_PLAYERS = 10;
    public static final int ACTIVITY_UPDATE_SERVER = 11;
    public static final int ACTIVITY_CONFIRM_UNSAFE_COMMAND = 12;
    public static final int ACTIVITY_SETTINGS = 13;
    public static final int ACTIVITY_SHOW_NOTE = 14;
    public static final int ACTIVITY_DEBUG_CONSOLE = 15;
    public static final int ACTIVITY_CREATE_BACKUP = 16;
    public static final int ACTIVITY_RESTORE_BACKUP = 17;
    public static final int ACTIVITY_FILE_CHOOSER = 18;

    // Intent extra names
    public static final String EXTRA_ROW_ID = "rowId";
    public static final String EXTRA_SERVER = "server";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_TIMEOUT = "timeout";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_CHALLENGE_RESPONSE = "challengeResponse";
    public static final String EXTRA_SEARCH = "search";
    public static final String EXTRA_PLAYER_LIST = "playerList";
    public static final String EXTRA_MESSAGE_LIST = "messageList";
    public static final String EXTRA_FILE_NAME = "filename";
    public static final String EXTRA_NOTE_ID = "noteString";
    public static final String EXTRA_DEBUG_TEXT = "debugText";
    public static final String EXTRA_BACKUP_FILE = "backupFile";
    public static final String EXTRA_NICKNAME = "nickname";
    public static final String EXTRA_REFRESH_SERVERS = "refreshServers";
    public static final String EXTRA_RESTART_SERVICE = "restartService";
    public static final String EXTRA_QUERY_SERVERS = "queryServers";

    // Bundle keys
    public static final String MESSAGES = "messages";
    public static final String SERVER_INFO = "serverInfo";
    public static final String SERVER_NAME = "serverName";
    public static final String SERVER_MAP = "serverMap";
    public static final String SERVER_GAME = "serverGame";
    public static final String SERVER_VERSION = "serverVersion";
    public static final String SERVER_NUM_PLAYERS = "numPlayers";
    public static final String SERVER_MAX_PLAYERS = "maxPlayers";
    public static final String SERVER_TAGS = "serverTags";
    public static final String SETTING_ENABLE_NOTIFICATION_LED = "enableNotificationLED";
    public static final String SETTING_ENABLE_NOTIFICATION_SOUND = "enableNotificationSounds";
    public static final String SETTING_ENABLE_NOTIFICATION_VIBRATE = "enableNotificationVibrate";
    public static final String SETTING_ENABLE_NOTIFICATIONS = "enableNotifications";
    public static final String SETTING_BACKGROUND_QUERY_FREQUENCY = "backgroundQueryFrequency";
    public static final String SETTING_RCON_SHOW_PASSWORDS = "rconShowPasswords";
    public static final String SETTING_RCON_WARN_UNSAFE_COMMAND = "rconWarnUnsafeCommand";
    public static final String SETTING_RCON_SHOW_SUGGESTIONS = "rconShowSuggestions";
    public static final String SETTING_RCON_ENABLE_HISTORY = "rconEnableHistory";
    public static final String SETTING_RCON_VOLUME_BUTTONS = "rconVolumeButtons";
    public static final String SETTING_RCON_DEFAULT_FONT_SIZE = "rconDefaultFontSize";
    public static final String SETTING_RCON_INCLUDE_SM = "rconIncludeSM";
    public static final String SETTING_SHOW_SERVER_NAME = "showServerName";
    public static final String SETTING_SHOW_SERVER_IP = "showServerIP";
    public static final String SETTING_SHOW_SERVER_GAME_INFO = "showServerGameInfo";
    public static final String SETTING_SHOW_SERVER_MAP_NAME = "showServerMapName";
    public static final String SETTING_SHOW_SERVER_NUM_PLAYERS = "showServerNumPlayers";
    public static final String SETTING_SHOW_SERVER_TAGS = "showServerTags";
    public static final String SETTING_SHOW_SERVER_PING = "showServerPing";
    public static final String SETTING_USE_SERVER_NICKNAME = "useServerNickname";
    public static final String SETTING_DEFAULT_QUERY_PORT = "defaultQueryPort";
    public static final String SETTING_DEFAULT_QUERY_TIMEOUT = "defaultQueryTimeout";
    public static final String SETTING_DEFAULT_RELAY_HOST = "defaultRelayHost";
    public static final String SETTING_DEFAULT_RELAY_PORT = "defaultRelayPort";
    public static final String SETTING_DEFAULT_RELAY_PASSWORD = "defaultRelayPassword";
    public static final String SETTING_VALIDATE_NEW_SERVERS = "validateNewServers";
    public static final String SETTING_SHOW_CHAT_RELAY_NOTE = "showChatRelayNote";
    public static final String SETTING_SHOW_CONSOLE_RELAY_NOTE = "showConsoleRelayNote";

    // TextView tag values
    public static final String TAG_SERVER_NAME = "serverName";
    public static final String TAG_SERVER_IP = "serverIP";
    public static final String TAG_SERVER_GAME = "serverGame";
    public static final String TAG_SERVER_MAP = "serverMap";
    public static final String TAG_SERVER_PLAYERS = "serverPlayers";
    public static final String TAG_SERVER_TAGS = "serverTags";
    public static final String TAG_SERVER_PING = "serverPing";
    public static final String TAG_SERVER_NICKNAME = "serverNickname";
    public static final String TAG_PLAYER_INFO = "playerInfo";
    public static final String TAG_MESSAGE_INFO = "messageInfo";
    public static final String TAG_ENABLED = "enabled";
    public static final String TAG_DISABLED = "disabled";

    // File names
    public static final String FILE_HIDE_CHAT_RELAY_NOTE = ".hide_chat_relay_note";
    public static final String FILE_HIDE_CONSOLE_RELAY_NOTE = ".hide_console_relay_note";
    public static final String FILE_HIDE_ANDROID_VERSION_NOTE = ".hide_android_version_note";

    // Server query values
    public static final int INT_PACKET_HEADER = 0xFFFFFFFF;
    public static final int INT_SPLIT_HEADER = 0xFFFFFFFE;
    public static final byte BYTE_CHALLENGE_RESPONSE = (byte) 0x41;
    public static final byte BYTE_A2S_PLAYER_RESPONSE = (byte) 0x44;
    public static final byte BYTE_GOLDSRC_INFO = (byte) 0x6D;
    public static final byte BYTE_SOURCE_INFO = (byte) 0x49;
    public static final byte BYTE_A2S_INFO = (byte) 0x54;
    public static final byte BYTE_A2S_PLAYER = (byte) 0x55;
    public static final byte BYTE_A2S_RULES = (byte) 0x56;
    public static final byte[] CHALLENGE_QUERY = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
    public static final String A2S_INFO_QUERY = "Source Engine Query\0";

    // Server query types
    public static final int QUERY_ENGINE = 1;
    public static final int QUERY_INFO = 2;

    // Permission request values
    public static final int PERMISSIONS_REQUEST = 123;

    // Job ID for scheduled jobs
    public static final int JOB_ID = 1;

    // Notification channel values
    public static final String CHANNEL_ID = "CHECKVALVE_NOTIFICATION_CHANNEL";
    public static final String CHANNEL_NAME = "CheckValve Notifications";
    public static final String CHANNEL_DESCRIPTION = "CheckValve notification channel";

    // Byte constants
    public static final byte BYTE_ZERO = (byte) 0x00;
    public static final byte BYTE_ONE = (byte) 0x01;

    public static final Bundle getSettings(Context c) {
        DatabaseProvider d = new DatabaseProvider(c);
        Bundle s = d.getSettingsAsBundle();
        d.close();
        return s;
    }
}