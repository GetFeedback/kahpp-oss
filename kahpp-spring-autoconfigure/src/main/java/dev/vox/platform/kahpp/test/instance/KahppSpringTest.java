package dev.vox.platform.kahpp.test.instance;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Profile("test")
@ExtendWith(SpringExtension.class)
public @interface KahppSpringTest {}
