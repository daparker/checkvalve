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
    protected static final int ENGINE_SOURCE = 0;
    protected static final int ENGINE_GOLDSRC = 1;

    // Activity codes
    protected static final int ACTIVITY_ABOUT = 2;
    protected static final int ACTIVITY_ADD_NEW_SERVER = 3;
    protected static final int ACTIVITY_CHAT = 4;
    protected static final int ACTIVITY_CHAT_RELAY_DETAILS_DIALOG = 5;
    protected static final int ACTIVITY_CONFIRM_DELETE = 6;
    protected static final int ACTIVITY_MANAGE_SERVERS = 7;
    protected static final int ACTIVITY_RCON = 8;
    protected static final int ACTIVITY_RCON_PASSWORD_DIALOG = 9;
    protected static final int ACTIVITY_SHOW_PLAYERS = 10;
    protected static final int ACTIVITY_UPDATE_SERVER = 11;
    protected static final int ACTIVITY_CONFIRM_UNSAFE_COMMAND = 12;
    protected static final int ACTIVITY_SETTINGS = 13;

    // Intent extra names
    protected static final String EXTRA_ROW_ID = "rowId";
    protected static final String EXTRA_SERVER = "server";
    protected static final String EXTRA_PORT = "port";
    protected static final String EXTRA_TIMEOUT = "timeout";
    protected static final String EXTRA_PASSWORD = "password";
    protected static final String EXTRA_CHALLENGE_RESPONSE = "challengeResponse";
    protected static final String EXTRA_SEARCH = "search";
    protected static final String EXTRA_PLAYER_LIST = "playerList";
    protected static final String EXTRA_MESSAGE_LIST = "messageList";

    // Bundle keys
    protected static final String SETTING_RCON_SHOW_PASSWORDS = "rconShowPasswords";
    protected static final String SETTING_RCON_WARN_UNSAFE_COMMAND = "rconWarnUnsafeCommand";
    protected static final String SETTING_RCON_SHOW_SUGGESTIONS = "rconShowSuggestions";
    protected static final String SETTING_SHOW_SERVER_IP = "showServerIP";
    protected static final String SETTING_SHOW_SERVER_GAME_INFO = "showServerGameInfo";
    protected static final String SETTING_SHOW_SERVER_MAP_NAME = "showServerMapName";
    protected static final String SETTING_SHOW_SERVER_NUM_PLAYERS = "showServerNumPlayers";
    protected static final String SETTING_SHOW_SERVER_TAGS = "showServerTags";
    protected static final String SETTING_DEFAULT_QUERY_PORT = "defaultQueryPort";
    protected static final String SETTING_DEFAULT_QUERY_TIMEOUT = "defaultQueryTimeout";
    protected static final String SETTING_DEFAULT_RELAY_HOST = "defaultRelayHost";
    protected static final String SETTING_DEFAULT_RELAY_PORT = "defaultRelayPort";
    protected static final String SETTING_DEFAULT_RELAY_PASSWORD = "defaultRelayPassword";
    protected static final String SETTING_VALIDATE_NEW_SERVERS = "validateNewServers";

    // TextView tag values
    protected static final String TAG_SERVER_NAME = "serverName";
    protected static final String TAG_SERVER_IP = "serverIP";
    protected static final String TAG_SERVER_GAME = "serverGame";
    protected static final String TAG_SERVER_MAP = "serverMap";
    protected static final String TAG_SERVER_PLAYERS = "serverPlayers";
    protected static final String TAG_SERVER_TAGS = "serverTags";
    protected static final String TAG_PLAYER_INFO = "playerInfo";
    protected static final String TAG_MESSAGE_INFO = "messageInfo";
    
}