package net.alnv.javareforged.split_transform;

import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeFieldRedirectTransformationService implements ClassProcessorProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeFieldRedirectTransformationService.class);

    @Override
    public void createProcessors(Context context, Collector collector) {
        collector.add(new RuntimeFieldRedirectLaunchPlugin());
        LOGGER.debug("JavaReforged runtime field redirect class processor registered");
    }
}
