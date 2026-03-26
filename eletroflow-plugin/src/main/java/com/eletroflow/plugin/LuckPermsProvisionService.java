package com.eletroflow.plugin;

import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;

public class LuckPermsProvisionService {

    private final LuckPerms luckPerms;

    public LuckPermsProvisionService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public void grant(PaymentRecord payment, PlanRecord plan) throws Exception {
        UUID uniqueId = UUID.fromString(payment.minecraftUuid());
        User user = luckPerms.getUserManager().loadUser(uniqueId).get();
        InheritanceNode node = InheritanceNode.builder(plan.luckPermsGroup())
                .expiry(plan.durationDays(), TimeUnit.DAYS)
                .build();
        user.data().add(node);
        luckPerms.getUserManager().saveUser(user);
    }
}
