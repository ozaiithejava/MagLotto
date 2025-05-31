# MagLotto

A simple Minecraft lotto minigame plugin for Spigot/Paper servers.  
Features include automatic lotto start, player join/leave tracking, and detailed event logging in JSON format.

## Features
- Create and manage lotto games with configurable reward, entry cost, and minimum players  
- Automatic game start countdown with cancellation if not enough players join  
- Player join and leave event logging  
- Detailed lotto event logs saved as JSON files for easy tracking and debugging  
- Broadcast messages and player notifications for lotto events

## Installation
1. Download the latest MagLotto plugin `.jar` file  
2. Place the `.jar` in your server's `plugins` folder  
3. Start or reload your server to generate config files  
4. Configure settings in `lotto.yml` as needed  

## Usage
- Use `/lotto admin create` to start a new lotto game  
- Players can join with `/lotto join <id>`  
- Lotto starts automatically after the configured delay if enough players join  



Made with ❤️ by ozaii
