# UtilityPlus

UtilityPlus is a comprehensive utility plugin for Minecraft servers, designed to work seamlessly on both **Paper** and **Folia**. It provides essential features like spawn management, homes, TPA requests, team systems, and administrative tools with a focus on performance and Folia compatibility.

## 🚀 Features

- **Platform Support:** Fully compatible with Paper and Folia (Regionalized Threading).
- **Spawn System:** Set and teleport to the server spawn.
- **Home System:** Up to 4 home slots per player with a GUI interface.
- **TPA System:** Request to teleport to others or have them teleport to you.
- **Team System:** Create teams, invite members, and use private team chat.
- **Vanish:** Admin invisibility mode.
- **Stats:** Track player kills and deaths with a leaderboard GUI (`/topstats`).
- **Chat Management:** Toggle global, team, or private message preferences.
- **Performance:** Includes custom TPS and CPU monitors (`/tpsmore`).
- **Admin Tools:** Broadcast, Gamemode shortcuts, Summon, and Reload commands.

## 📋 Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/spawn` | Teleport to spawn | `utilityplus.spawn` |
| `/setspawn` | Set server spawn | `utilityplus.setspawn` (OP) |
| `/home [1-4]` | Teleport to or list homes | `utilityplus.home` |
| `/sethome <1-4>` | Set a home slot | `utilityplus.sethome` |
| `/tpa <player>` | Request teleport | `utilityplus.tpa` |
| `/team` | Team management | `utilityplus.team` |
| `/v` | Toggle vanish | `utilityplus.vanish` (OP) |
| `/topstats` | View leaderboard GUI | `utilityplus.topstats` |
| `/tpsmore` | Detailed performance info | `utilityplus.tpsmore` (OP) |

*Full list of commands and permissions can be found in `plugin.yml`.*

## 🛠️ Installation

1. Download the latest release from the [Releases](https://github.com/Deluxeg4/UtilityPlus/releases) page.
2. Choose the JAR corresponding to your platform:
   - `UtilityPlus-Paper-x.x.x.jar` for Paper/Spigot.
   - `UtilityPlus-Folia-x.x.x.jar` for Folia.
3. Place the JAR in your server's `plugins` folder.
4. Restart the server.

## 🔨 Building from Source

To build the plugin yourself, ensure you have JDK 21 installed.

```bash
# Clone the repository
git clone https://github.com/Deluxeg4/UtilityPlus.git
cd UtilityPlus

# Build the JARs
./gradlew build
```

The resulting JARs will be located in `build/libs/`.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details (if applicable).
