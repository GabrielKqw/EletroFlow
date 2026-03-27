package com.eletroflow.plugin;

import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import java.time.OffsetDateTime;
import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

public class LuckPermsProvisionService {

    private final LuckPerms luckPerms;

    public LuckPermsProvisionService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public OffsetDateTime grant(PaymentRecord payment, PlanRecord plan, OffsetDateTime activeUntil) throws Exception {
        OffsetDateTime baseTime = activeUntil != null && activeUntil.isAfter(OffsetDateTime.now())
                ? activeUntil
                : OffsetDateTime.now();
        OffsetDateTime expiresAt = baseTime.plusDays(plan.durationDays());
        UUID uniqueId = UUID.fromString(payment.minecraftUuid());
        User user = luckPerms.getUserManager().loadUser(uniqueId).get();
        user.getNodes().stream()
                .filter(InheritanceNode.class::isInstance)
                .map(InheritanceNode.class::cast)
                .filter(InheritanceNode::hasExpiry)
                .filter(node -> node.getGroupName().equalsIgnoreCase(plan.luckPermsGroup()))
                .forEach(node -> user.data().remove(node));
        InheritanceNode node = InheritanceNode.builder(plan.luckPermsGroup()).expiry(expiresAt.toInstant()).build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
        return expiresAt;
    }
}
