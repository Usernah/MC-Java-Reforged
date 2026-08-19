package net.alnv.javareforged.runtime_transform;

import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeTransformationService implements ClassProcessorProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeTransformationService.class);

    @Override
    public void createProcessors(Context context, Collector collector) {
        collector.add(new RuntimeFieldRedirectProcessor());
        collector.add(new TerrainLifecycleProcessor());
        LOGGER.debug("JavaReforged runtime class processors registered");
    }
}
