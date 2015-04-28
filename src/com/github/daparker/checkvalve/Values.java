/*
 * Copyright 2010-2011 by David A. Parker <parker.david.a@gmail.com>
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

package com.github.daparker.checkvalve;

/*
 * Define the Values class
 */
public class Values
{
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

    // Bundle keys
    public static final String SETTING_RCON_SHOW_PASSWORDS = "rconShowPasswords";
    public static final String SETTING_RCON_WARN_UNSAFE_COMMAND = "rconWarnUnsafeCommand";
    public static final String SETTING_RCON_SHOW_SUGGESTIONS = "rconShowSuggestions";
    public static final String SETTING_SHOW_SERVER_IP = "showServerIP";
    public static final String SETTING_SHOW_SERVER_GAME_INFO = "showServerGameInfo";
    public static final String SETTING_SHOW_SERVER_MAP_NAME = "showServerMapName";
    public static final String SETTING_SHOW_SERVER_NUM_PLAYERS = "showServerNumPlayers";
    public static final String SETTING_SHOW_SERVER_TAGS = "showServerTags";
    public static final String SETTING_DEFAULT_QUERY_PORT = "defaultQueryPort";
    public static final String SETTING_DEFAULT_QUERY_TIMEOUT = "defaultQueryTimeout";
    public static final String SETTING_DEFAULT_RELAY_HOST = "defaultRelayHost";
    public static final String SETTING_DEFAULT_RELAY_PORT = "defaultRelayPort";
    public static final String SETTING_DEFAULT_RELAY_PASSWORD = "defaultRelayPassword";
    public static final String SETTING_VALIDATE_NEW_SERVERS = "validateNewServers";

    // TextView tag values
    public static final String TAG_SERVER_NAME = "serverName";
    public static final String TAG_SERVER_IP = "serverIP";
    public static final String TAG_SERVER_GAME = "serverGame";
    public static final String TAG_SERVER_MAP = "serverMap";
    public static final String TAG_SERVER_PLAYERS = "serverPlayers";
    public static final String TAG_SERVER_TAGS = "serverTags";
    public static final String TAG_PLAYER_INFO = "playerInfo";
    public static final String TAG_MESSAGE_INFO = "messageInfo";
}