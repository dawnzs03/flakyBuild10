/*
 * Copyright 2018-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.modulith.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An unconditional auto-configuration registering an {@link ImportBeanDefinitionRegistrar} to customize both the entity
 * scan and auto-configuration packages to the packages defined by the {@link ModuleTestExecution} in the application
 * context.
 *
 * @author Oliver Drotbohm
 */
@Configuration(proxyBeanMethods = false)
@Import(ModuleTestAutoConfiguration.AutoConfigurationAndEntityScanPackageCustomizer.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
class ModuleTestAutoConfiguration {

	private static final String AUTOCONFIG_PACKAGES = "org.springframework.boot.autoconfigure.AutoConfigurationPackages";
	private static final String ENTITY_SCAN_PACKAGE = "org.springframework.boot.autoconfigure.domain.EntityScanPackages";

	static class AutoConfigurationAndEntityScanPackageCustomizer implements ImportBeanDefinitionRegistrar {

		private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigurationAndEntityScanPackageCustomizer.class);

		/*
		 * (non-Javadoc)
		 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

			var execution = ((BeanFactory) registry).getBean(ModuleTestExecution.class);
			var basePackages = execution.getBasePackages().toList();

			LOGGER.info("Re-configuring auto-configuration and entity scan packages to: {}.",
					StringUtils.collectionToDelimitedString(basePackages, ", "));

			setBasePackagesOn(registry, AUTOCONFIG_PACKAGES, "BasePackagesBeanDefinition", "basePackages", basePackages);
			setBasePackagesOn(registry, ENTITY_SCAN_PACKAGE, "EntityScanPackagesBeanDefinition", "packageNames",
					basePackages);
		}

		@SuppressWarnings("unchecked")
		private void setBasePackagesOn(BeanDefinitionRegistry registry, String beanName, String definitionType,
				String fieldName, List<String> packages) {

			if (!registry.containsBeanDefinition(beanName)) {
				return;
			}

			var definition = registry.getBeanDefinition(beanName);

			// For Boot 2.4, we deal with a BasePackagesBeanDefinition
			var field = Arrays.stream(definition.getClass().getDeclaredFields())
					.filter(__ -> definition.getClass().getSimpleName().equals(definitionType))
					.filter(it -> it.getName().equals(fieldName))
					.findFirst()
					.orElse(null);

			if (field != null) {

				// Keep all auto-configuration packages from Moduliths

				ReflectionUtils.makeAccessible(field);
				((Set<String>) ReflectionUtils.getField(field, definition)).stream()
						.filter(it -> it.startsWith("org.springframework.modulith"))
						.forEach(packages::add);

				ReflectionUtils.setField(field, definition, new HashSet<>(packages));

			} else {

				ValueHolder holder = definition.getConstructorArgumentValues().getArgumentValue(0, String[].class);
				Arrays.stream((String[]) holder.getValue())
						.filter(it -> it.startsWith("org.springframework.modulith"))
						.forEach(packages::add);

				// Fall back to customize the bean definition in a Boot 2.3 arrangement
				definition.getConstructorArgumentValues().addIndexedArgumentValue(0, packages.toArray(String[]::new));
			}
		}
	}
}
