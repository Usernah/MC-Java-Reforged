package net.alnv.javareforged.split_transform;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeFieldRedirectTransformationService implements ITransformationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeFieldRedirectTransformationService.class);

    @Override
    public String name() {
        return "javareforged_runtime_field_redirect_bootstrap";
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.debug("JavaReforged runtime field redirect transformation service initialized");
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        LOGGER.debug("JavaReforged runtime field redirect transformation service loaded");
    }

    @Override
    public List<? extends ITransformer<?>> transformers() {
        return List.of();
    }
}
