# EletroFlow

![EletroFlow](https://i.pinimg.com/originals/22/81/36/228136787949a85c103a630c753726aa.gif)

EletroFlow is a Paper plugin for VIP sales through Discord with Pix payments via Efí, PostgreSQL persistence, and automatic LuckPerms delivery.

## Stack

- Java 21
- Paper 1.21.x
- PostgreSQL
- JDA
- Efí Pix API
- LuckPerms

## Runtime

Only the plugin jar is deployed to the Minecraft server:

- [eletroflow-plugin-1.0.0-SNAPSHOT.jar](C:/Users/Admin/Desktop/pl/eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar)

The plugin handles:

- Discord purchase panel
- private purchase threads
- Pix charge generation
- PostgreSQL persistence
- payment polling
- LuckPerms delivery
- optional Discord role assignment

## Purchase Flow

1. A staff member publishes the VIP panel with `/vip-panel`.
2. The player clicks the button and receives a private thread.
3. The player selects a VIP plan from the dropdown.
4. The player submits Minecraft UUID and nickname.
5. The plugin creates or reuses a Pix charge through Efí.
6. The payment is stored in PostgreSQL with the Discord thread and player identity.
7. The plugin polls the Pix status.
8. After confirmation, the plugin records the transaction, writes the VIP grant, applies LuckPerms, and can assign the configured Discord role.

## Repository Layout

- [database](C:/Users/Admin/Desktop/pl/database) contains the PostgreSQL bootstrap script
- [eletroflow-plugin](C:/Users/Admin/Desktop/pl/eletroflow-plugin) contains the Paper plugin
- [eletroflow-shared](C:/Users/Admin/Desktop/pl/eletroflow-shared) contains shared DTOs and enums

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 14+
- Paper server with LuckPerms installed
- Discord bot token
- Efí Pix credentials and `.p12` certificate

## Database Setup

Run the bootstrap script before starting the server:

- [init.sql](C:/Users/Admin/Desktop/pl/database/init.sql)

The script creates:

- PostgreSQL user
- database
- `users`
- `vip_plans`
- `payments`
- `payment_transactions`
- `vip_grants`
- `audit_logs`
- indexes

## Configuration

Main plugin configuration:

- [config.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/config.yml)

VIP catalog:

- [vip-plans.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/vip-plans.yml)

`config.yml` defines:

- server id
- PostgreSQL connection
- Discord token and channel ids
- Efí credentials
- certificate path
- payment expiration
- polling intervals

`vip-plans.yml` defines:

- VIP key
- display name
- amount
- currency
- LuckPerms group
- Discord role id
- duration in days
- active flag
- sort order

## Installation

1. Execute [init.sql](C:/Users/Admin/Desktop/pl/database/init.sql) in PostgreSQL.
2. Adjust [config.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/config.yml) with your PostgreSQL, Discord, and Efí data.
3. Adjust [vip-plans.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/vip-plans.yml) with the VIP catalog.
4. Build the project with Maven.
5. Place the generated jar inside the server `plugins` folder.
6. Start the Paper server.

## Build

```bash
mvn clean package -DskipTests
```

Generated artifact:

- [eletroflow-plugin-1.0.0-SNAPSHOT.jar](C:/Users/Admin/Desktop/pl/eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar)

## Notes

- The plugin does not create the schema automatically.
- LuckPerms is required at startup.
- The PostgreSQL schema is expected to exist before the plugin enables.
