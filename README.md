# EletroFlow

EletroFlow is a modular payment and provisioning platform for Discord, Minecraft Paper servers, EFI Bank Pix, LuckPerms, and PostgreSQL.

## Modules

- `eletroflow-shared`: shared DTOs and enums
- `eletroflow-backend`: Spring Boot backend, persistence, Pix orchestration, webhook processing
- `eletroflow-discord-bot`: JDA ticket and purchase automation bot
- `eletroflow-plugin`: Paper plugin for reward synchronization and LuckPerms provisioning

## Build

Install Java 21 and Maven 3.9+ before building:

```bash
mvn clean package
```

# EletroFlow
