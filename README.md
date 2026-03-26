<div align="center">

# EletroFlow

<img src="./assets/eletroflow-banner.gif" alt="EletroFlow Banner" width="720" />

### Discord VIP sales, Pix checkout, PostgreSQL persistence, and LuckPerms delivery inside a single Paper plugin

![Java](https://img.shields.io/badge/Java-21-9cf?style=for-the-badge&labelColor=111111&color=6ee7ff)
![Paper](https://img.shields.io/badge/Paper-1.21.x-9cf?style=for-the-badge&labelColor=111111&color=a7f3d0)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-9cf?style=for-the-badge&labelColor=111111&color=93c5fd)
![LuckPerms](https://img.shields.io/badge/LuckPerms-Required-9cf?style=for-the-badge&labelColor=111111&color=f9a8d4)
![JDA](https://img.shields.io/badge/JDA-5.x-9cf?style=for-the-badge&labelColor=111111&color=c4b5fd)

</div>

![Divider](./assets/neon-divider.svg)

## Overview

EletroFlow is a plugin-first payment system for Minecraft servers. The Paper plugin itself connects to Discord, Efí Pix, PostgreSQL, and LuckPerms to run the full VIP purchase flow without a separate backend runtime.

### What it handles

- VIP purchase panel on Discord
- private purchase threads
- Pix charge generation and reuse
- PostgreSQL persistence
- payment status polling
- LuckPerms VIP delivery
- optional Discord role assignment

![Divider](./assets/neon-divider.svg)

## Runtime

Only one artifact is deployed to the Minecraft server:

- [eletroflow-plugin-1.0.0-SNAPSHOT.jar](C:/Users/Admin/Desktop/pl/eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar)

Repository layout:

- [database](C:/Users/Admin/Desktop/pl/database) contains the PostgreSQL bootstrap script
- [eletroflow-plugin](C:/Users/Admin/Desktop/pl/eletroflow-plugin) contains the Paper plugin
- [eletroflow-shared](C:/Users/Admin/Desktop/pl/eletroflow-shared) contains shared DTOs and enums

![Divider](./assets/neon-divider.svg)

## Purchase Flow

1. A staff member publishes the VIP panel with `/vip-panel`.
2. The player clicks the panel button and receives a private thread.
3. The player selects a VIP plan from the dropdown.
4. The player submits Minecraft UUID and nickname.
5. The plugin creates or reuses a Pix charge through Efí.
6. The payment is stored with the linked Discord identity, Minecraft identity, thread id, and selected plan.
7. The plugin polls Pix confirmation.
8. After confirmation, the plugin records the transaction, persists the VIP grant, applies the LuckPerms group, and can assign the configured Discord role.

![Divider](./assets/neon-divider.svg)

## Stack

- Java 21
- Paper 1.21.x
- PostgreSQL 14+
- JDA 5.x
- Efí Pix API
- LuckPerms
- Maven 3.9+

![Divider](./assets/neon-divider.svg)

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

![Divider](./assets/neon-divider.svg)

## Configuration

Main plugin configuration:

- [config.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/config.yml)

VIP catalog:

- [vip-plans.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/vip-plans.yml)

`config.yml` defines:

- server id
- PostgreSQL connection
- Discord token and guild/channel ids
- Efí credentials
- certificate path
- Pix expiration
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

![Divider](./assets/neon-divider.svg)

## Installation

1. Execute [init.sql](C:/Users/Admin/Desktop/pl/database/init.sql) in PostgreSQL.
2. Adjust [config.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/config.yml) with PostgreSQL, Discord, and Efí credentials.
3. Adjust [vip-plans.yml](C:/Users/Admin/Desktop/pl/eletroflow-plugin/src/main/resources/vip-plans.yml) with the VIP catalog.
4. Build the project with Maven.
5. Place the generated jar inside the server `plugins` folder.
6. Start the Paper server with LuckPerms installed.

![Divider](./assets/neon-divider.svg)

## Build

```bash
mvn clean package -DskipTests
```

Generated artifact:

- [eletroflow-plugin-1.0.0-SNAPSHOT.jar](C:/Users/Admin/Desktop/pl/eletroflow-plugin/target/eletroflow-plugin-1.0.0-SNAPSHOT.jar)

![Divider](./assets/neon-divider.svg)

## Notes

- The plugin does not create the schema automatically.
- LuckPerms is required at startup.
- The PostgreSQL schema must exist before the plugin enables.
