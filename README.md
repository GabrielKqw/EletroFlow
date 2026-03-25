# EletroFlow

EletroFlow is a modular backend platform for Discord VIP sales, EFI Pix payments, PostgreSQL persistence, and Minecraft Paper provisioning through LuckPerms.

## Modules

- `eletroflow-shared`: shared DTOs and status enums
- `eletroflow-backend`: Spring Boot API, payment orchestration, webhooks, reward dispatch
- `eletroflow-discord-bot`: JDA ticket workflow and payment follow-up
- `eletroflow-plugin`: Paper plugin that polls pending rewards and grants LuckPerms groups

## Core Flow

1. A user opens `/ticket` in Discord.
2. The bot creates a dedicated text channel and shows the configured VIP plans.
3. The user selects a plan and submits Minecraft UUID and username.
4. The bot requests Pix generation from the backend.
5. The backend creates a payment, persists the charge, and returns the Pix payload.
6. The EFI webhook confirms the payment.
7. The backend creates an idempotent provisioning reward.
8. The Paper plugin claims the reward and grants the LuckPerms group.
9. The plugin acknowledges completion to the backend.
10. The bot polls the payment status and can assign the configured Discord role.

## Configuration

- Backend: `eletroflow-backend/src/main/resources/application.yml`
- Discord bot defaults copied to `config/bot-config.yml` and `config/vip-plans.yml`
- Plugin defaults copied to the server plugin folder from `config.yml` and `vip-plans.yml`

## Build

This repository is structured for Java 21 and Maven.

```bash
mvn clean package
```

## Notes

- Java and Maven were not available on the current machine, so the implementation could not be compiled locally in this environment.
- EFI integration is structured around the Pix charge flow and webhook validation, with placeholders for production certificate and credentials.
