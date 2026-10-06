package net.alnv.javareforged.runtime_transform;

import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeTransformationService implements ClassProcessorProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeTransformationService.class);

    @Override
    public void createProcessors(Context context, Collector collector) {
        collector.add(new RuntimeFieldRedirectProcessor());
        if (OptionalModPresence.sodiumLoaded()) {
            LOGGER.info("Sodium detected; Java Reforged vanilla terrain class processor is disabled");
            collector.add(new SodiumTerrainStateProcessor());
        } else {
            collector.add(new TerrainLifecycleProcessor());
        }
        collector.add(new BetterCombatCompatProcessor());
        LOGGER.debug("JavaReforged runtime class processors registered");
    }
}
