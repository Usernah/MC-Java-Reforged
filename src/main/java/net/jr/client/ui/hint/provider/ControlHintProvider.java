package net.jr.client.ui.hint.provider;

import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ControlHintRequest;

import java.util.List;

public interface ControlHintProvider {
    boolean supports(ControlHintContext context);

    List<ControlHintRequest> buildHints(ControlHintContext context);
}
