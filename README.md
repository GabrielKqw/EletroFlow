# EletroFlow

EletroFlow is a Paper plugin that centralizes Discord VIP sales, EFI Pix charges, PostgreSQL persistence, and LuckPerms delivery inside the Minecraft server runtime.

## Runtime

Only one runtime artifact is intended to be deployed to the Minecraft server:

- `eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar`

The plugin itself connects to:

- PostgreSQL
- Discord
- EFI Pix
- LuckPerms

## Purchase Flow

1. A staff member uses `/vip-panel` on Discord.
2. The bot integration inside the plugin publishes the purchase panel.
3. The player clicks the button and receives a private thread.
4. Inside the thread, the player selects a VIP from a dropdown fed by the plugin plan catalog.
5. The player submits Minecraft UUID and nickname.
6. The plugin creates or reuses a Pix charge through EFI.
7. The plugin stores plans and payments in PostgreSQL.
8. The plugin polls the payment status.
9. After confirmation, the plugin grants the LuckPerms group and can assign the configured Discord role.

## Configuration

Main configuration:

- [config.yml](/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/config.yml)

VIP catalog:

- [vip-plans.yml](/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/vip-plans.yml)

`config.yml` contains:

- PostgreSQL connection
- Discord token and channel IDs
- EFI credentials and certificate path
- sync interval settings

`vip-plans.yml` contains:

- display name
- amount
- currency
- LuckPerms group
- Discord role id
- duration
- activation flag
- sort order

## Build

```bash
mvn clean package -DskipTests
```

## Output

Generated plugin jar:

- [eletroflow-plugin-1.0.0-SNAPSHOT.jar](/Users/Admin/Desktop/pl/eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar)
